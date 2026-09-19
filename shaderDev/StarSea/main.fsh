#version 150

// FrostField 同型的深度投影输入；原型由 WebGL 2 宿主生成两份深度。
uniform sampler2D DepthSampler;
uniform sampler2D BlockDepthSampler;
uniform mat4 InvViewProj;
uniform vec3 FieldCenter;   // 相机相对坐标，单位：格
uniform float FieldRadius; // 领域基准半径，单位：格
uniform float FieldTime;   // 累计秒数，不能直接上传 tick 或原版 GameTime
uniform float FieldAge;    // 激活后的秒数；不与 FieldTime 混用
uniform float FieldOpacity;
uniform float EdgeMotion;  // 边缘起伏的半径比例，0–0.4
uniform float MeteorRate;  // 每个方向星区每秒出生数，0–2；0 关闭
uniform float NebulaClusterGain; // 随机星云簇强度，0–2
uniform float NebulaSeed;        // 星云簇种子，建议 0–9999
uniform vec2 DepthUvScale;
uniform int DebugView;     // 0 正常，1 地形覆盖，2 遮挡排除

in vec2 texCoord0;
out vec4 fragColor;

// @include STARFIELD_SHARED
// @include STARSEA_SKY

vec3 reconstructRelativePosition(vec2 uv, float depth) {
    vec4 p = InvViewProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float w = abs(p.w) < 0.000001 ? (p.w < 0.0 ? -0.000001 : 0.000001) : p.w;
    return p.xyz / w;
}

// 流体范围只控制地形覆盖，不绘制外壳。角向波在上下极点平滑归零。
float coastDistance(vec3 local, float radius, float seconds) {
    vec3 p = local / max(radius, 0.001);
    float motion = clamp(EdgeMotion, 0.0, 0.4);
    vec3 warp = vec3(skyNoise(p * 2.5 + vec3(seconds * 0.13, 17.0, 0.0)),
                     skyNoise(p * 2.5 + vec3(43.0, -seconds * 0.11, 0.0)),
                     skyNoise(p * 2.5 + vec3(7.0, 0.0, seconds * 0.12))) - 0.5;
    vec3 q = p + warp * motion * 0.7;
    float angle = atan(q.z, q.x + 0.000001);
    float planar = length(q.xz) / max(length(q), 0.0001);
    float waves = 0.56 * sin(angle * 3.0 + seconds * 0.47) * pow(planar, 3.0)
        + 0.30 * sin(angle * 5.0 - seconds * 0.33 + 1.2) * pow(planar, 5.0)
        + 0.16 * sin(angle * 7.0 + seconds * 0.21 + 2.6) * pow(planar, 7.0);
    return (length(q) - (1.0 + motion * waves)) * radius;
}

void main() {
    vec2 depthUv = clamp(texCoord0, vec2(0.0), vec2(1.0)) * DepthUvScale;
    float depth = texture(DepthSampler, depthUv).r;
    float blockDepth = texture(BlockDepthSampler, depthUv).r;
    vec3 blockPosition = reconstructRelativePosition(texCoord0, blockDepth);
    vec3 scenePosition = reconstructRelativePosition(texCoord0, depth);
    vec3 rayOrigin = reconstructRelativePosition(texCoord0, 0.0);
    vec3 farPosition = reconstructRelativePosition(texCoord0, 1.0);
    vec3 rayDelta = farPosition - rayOrigin;
    vec3 rayDirection = rayDelta / max(length(rayDelta), 0.000001);
    // 重要：blockPosition 与 FieldCenter 都是相机相对坐标；此处先做相减，
    // 不把数百万格的绝对世界坐标传入程序化噪声，避免远坐标 float 精度损失。
    vec3 local = blockPosition - FieldCenter;
    float radius = max(FieldRadius, 0.001);
    float seconds = max(FieldTime, 0.0);
    // 从中心向外展开同一不规则轮廓；不额外叠加圆形裁切。
    float growth = smoothstep(0.0, 1.8, max(FieldAge, 0.0));
    float coast = coastDistance(local, radius * max(growth, 0.001), seconds);
    // 星图由观察射线决定，与地面法线、UV 和深度无关；墙角两侧连续。
    // 所有导数在 discard 和非一致分支前计算。
    float pixel = max(max(length(dFdx(rayDirection)), length(dFdy(rayDirection))), 0.00001);
    float edgeAA = min(max(fwidth(coast), 0.015), 0.18);

    if (blockDepth >= 0.99999 || FieldRadius <= 0.01 || growth <= 0.0) discard;
    float blockLimit = dot(blockPosition - rayOrigin, rayDirection);
    float sceneLimit = dot(scenePosition - rayOrigin, rayDirection);
    // 沿视线以格为单位比较深度；实体和近处遮挡物保持原材质。
    bool occluded = blockLimit > sceneLimit + 0.025;
    float edgeWidth = max(0.14, radius * 0.035);
    float coverage = (1.0 - smoothstep(-edgeWidth - edgeAA, edgeAA, coast))
        * smoothstep(0.0, 0.25, FieldAge);
    if (DebugView == 2) {
        if (!occluded || coverage <= 0.001) discard;
        fragColor = vec4(vec3(1.0, 0.22, 0.08) * 0.75, 0.75);
        return;
    }
    if (occluded || coverage <= 0.001) discard;
    if (DebugView == 1) {
        fragColor = vec4(vec3(0.12, 0.82, 0.68) * coverage, coverage);
        return;
    }

    // 星云与银河同属观察方向星空；地面深度只决定窗口遮罩，不参与星云坐标。
    vec3 color = continuousSky(rayDirection, pixel, seconds, NebulaSeed, NebulaClusterGain);

    float innerEdge = exp(-pow((coast + edgeWidth * 0.55) / max(0.055, edgeAA), 2.0));
    float edgeGlow = exp(-abs(coast + 0.06) / max(0.15, radius * 0.045));
    float foam = 0.65 + 0.35 * skyNoise(local * 3.0 + vec3(seconds * 0.4, 0.0, -seconds * 0.3));
    vec3 rimColor = mix(vec3(0.22, 0.40, 0.88), vec3(0.57, 0.32, 0.83),
        0.5 + 0.5 * sin(local.x * 0.4 + local.z * 0.6 - seconds * 0.3));
    color += rimColor * (innerEdge * 0.35 + edgeGlow * 0.12) * foam;
    color = pow(vec3(1.0) - exp(-max(color, vec3(0.0)) * EXPOSURE), vec3(1.0 / 2.2));
    float alpha = coverage * clamp(FieldOpacity, 0.0, 1.0);
    // 预乘 Alpha：ONE / ONE_MINUS_SRC_ALPHA，完整覆盖时取代地面颜色。
    fragColor = vec4(color * alpha, alpha);
}

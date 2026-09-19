#version 150

uniform sampler2D DepthSampler;
uniform sampler2D BlockDepthSampler;
uniform mat4 InvViewProj;
uniform vec3 PatternOrigin;
uniform vec3 FieldCenter;
uniform float FieldRadius;
uniform float FieldTime; // 客户端游戏时间，单位 tick；下方乘 0.05 转为秒。
uniform float FieldAge;  // 本次领域激活后的 tick，包含 partial tick。
uniform float PulseRadius; // Java 维护的唯一波峰半径，单位：格；尾部离界才重置。
uniform vec2 DepthUvScale;

in vec2 texCoord0;
out vec4 fragColor;

const vec3 ICE_BLUE = vec3(0.12, 0.46, 0.68);
const vec3 ICE_LIGHT = vec3(0.48, 0.86, 1.0);
const vec3 ICE_WHITE = vec3(0.82, 0.97, 1.0);

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
}

// 固定在世界/球壳坐标上的结晶胞元；动画只改变亮度，不让裂纹像液体一样漂移。
vec2 crystalCell(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    float nearest = 100.0;
    float secondNearest = 100.0;
    for (int z = -1; z <= 1; ++z) {
        for (int y = -1; y <= 1; ++y) {
            for (int x = -1; x <= 1; ++x) {
                vec3 offset = vec3(float(x), float(y), float(z));
                vec3 delta = offset + 0.16 + hash33(cell + offset) * 0.68 - local;
                float d = dot(delta, delta);
                if (d < nearest) {
                    secondNearest = nearest;
                    nearest = d;
                } else {
                    secondNearest = min(secondNearest, d);
                }
            }
        }
    }
    return vec2(sqrt(nearest), sqrt(secondNearest) - sqrt(nearest));
}

vec3 reconstructRelativePosition(vec2 uv, float depth) {
    vec4 p = InvViewProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / (abs(p.w) < 0.000001 ? 0.000001 : p.w);
}

// 单个有限宽度波环，不对空间距离取 fract，任意领域半径都只会出现一波。
vec2 outwardPulse(float radius) {
    float distanceToRing = abs(radius - PulseRadius);
    float envelope = smoothstep(0.0, 0.25, PulseRadius)
            * (1.0 - smoothstep(FieldRadius, FieldRadius + 1.2, PulseRadius))
            * (1.0 - smoothstep(0.85, 1.2, distanceToRing));
    return vec2(exp(-distanceToRing * distanceToRing * 32.0),
            exp(-distanceToRing * distanceToRing * 2.2)) * envelope;
}

float frostBranches(vec2 p, float pixelWidth) {
    float branches = 0.0;
    // 三组互成 60 度的主脉和短侧枝，作为冰花覆霜的细节。
    for (int i = 0; i < 3; ++i) {
        vec2 q = vec2(p.x * 0.5 - p.y * 0.8660254, p.x * 0.8660254 + p.y * 0.5);
        p = q;
        vec2 cell = fract(q * 1.7) - 0.5;
        float stem = abs(cell.x);
        float twig = abs(abs(cell.x) * 0.75 + cell.y - floor(cell.y * 4.0 + 0.5) * 0.25);
        float line = min(stem, max(twig, abs(cell.x) - 0.24));
        branches = max(branches, 1.0 - smoothstep(0.008, 0.024 + pixelWidth * 1.7, line));
    }
    return branches;
}

vec4 groundIce(vec3 position, vec3 normal, float pixelWidth) {
    vec3 local = position - FieldCenter;
    float radius = max(FieldRadius, 0.001);
    float distanceToCenter = length(local);
    float radial = length(local.xz);
    float birthRadius = min(radius, max(FieldAge, 0.0) * 0.05 * 9.0);
    float reveal = 1.0 - smoothstep(birthRadius - 0.45, birthRadius, distanceToCenter);
    if (reveal <= 0.0) return vec4(0.0);

    vec3 pattern = PatternOrigin + position;
    vec2 cell = crystalCell(pattern * 1.15);
    float aa = min(pixelWidth * 1.15, 0.08);
    float crack = 1.0 - smoothstep(0.014, 0.047 + aa, cell.y);
    float crackGlow = exp(-cell.y * 14.0);
    float facets = 1.0 - smoothstep(0.22, 0.85, cell.x);
    float upward = smoothstep(0.45, 0.9, abs(normal.y));
    float fern = frostBranches(pattern.xz, pixelWidth) * upward;
    vec2 pulse = outwardPulse(radial);
    float edge = abs(distanceToCenter - birthRadius);
    float contact = exp(-edge * edge / max(0.018, pixelWidth * pixelWidth * 3.0));
    float contactGlow = exp(-edge * edge * 5.0);
    float underfoot = exp(-radial * radial * 0.8) * upward;

    float alpha = reveal * (0.28 + facets * 0.17 + fern * 0.07) * (0.65 + upward * 0.35);
    vec3 ice = mix(ICE_BLUE, ICE_LIGHT, facets * 0.55 + fern * 0.15);
    vec3 emission = ICE_LIGHT * (crackGlow * 0.10 + fern * 0.065 + pulse.y * 0.09);
    emission += ICE_WHITE * crack * (0.16 + pulse.x * 0.72);
    emission += ICE_LIGHT * pulse.x * (0.12 + fern * 0.18) * upward;
    emission += ICE_WHITE * contact * 0.42 + ICE_LIGHT * contactGlow * 0.17;
    emission += ICE_LIGHT * underfoot * (0.08 + pulse.y * 0.14);
    return vec4(ice * alpha + emission * reveal, alpha);
}

vec4 frozenShell(float rayDistance, float sceneLimit, vec3 rayOrigin, vec3 rayDirection,
        float rayPixelWidth, float time, bool inside) {
    if (rayDistance <= 0.0 || rayDistance > sceneLimit) return vec4(0.0);
    vec3 local = rayOrigin + rayDirection * rayDistance - FieldCenter;
    vec3 normal = local / max(FieldRadius, 0.001);
    float fresnel = pow(clamp(1.0 - abs(dot(normal, rayDirection)), 0.0, 1.0), 2.5);
    // 比原有 0.06 的胞元密度更高，4 格的小领域也能看到完整的冰裂网。
    vec3 pattern = local * 0.82;
    pattern += sin(pattern.yzx * 3.1 + sin(pattern.zxy * 1.9)) * 0.065;
    vec2 cell = crystalCell(pattern);
    float aa = min(rayPixelWidth * rayDistance * 0.82, 0.075);
    float crack = 1.0 - smoothstep(0.012, 0.035 + aa, cell.y);
    float halo = exp(-cell.y * 19.0);
    float facet = 1.0 - smoothstep(0.15, 0.95, cell.x);
    vec2 pulse = outwardPulse(length(local.xz));
    float glint = pow(0.5 + 0.5 * sin(local.y * 3.7 + local.x * 1.1 - time * 2.0943951), 12.0);
    float reveal = smoothstep(0.0, 12.0, FieldAge);
    // 第一人称保留裂纹，同时降低膜面遮盖，内部仍能清楚观察战斗。
    float visibility = inside ? 0.48 : 1.0;
    float alpha = (0.035 + facet * 0.045 + fresnel * 0.15 + crack * 0.10) * visibility * reveal;
    vec3 emission = ICE_LIGHT * (halo * 0.07 + fresnel * 0.20);
    emission += ICE_WHITE * crack * (0.20 + pulse.x * 0.36 + glint * 0.14);
    return vec4(mix(ICE_BLUE, ICE_LIGHT, facet) * alpha + emission * visibility * reveal, alpha);
}

vec4 over(vec4 front, vec4 back) {
    return front + back * (1.0 - front.a);
}

void main() {
    vec2 depthUv = clamp(texCoord0, vec2(0.0), vec2(1.0)) * DepthUvScale;
    float depth = texture(DepthSampler, depthUv).r;
    float blockDepth = texture(BlockDepthSampler, depthUv).r;
    vec3 rayOrigin = reconstructRelativePosition(texCoord0, 0.0);
    vec3 farPosition = reconstructRelativePosition(texCoord0, 1.0);
    vec3 rayDirection = normalize(farPosition - rayOrigin);
    vec3 scenePosition = reconstructRelativePosition(texCoord0, depth);
    vec3 blockPosition = reconstructRelativePosition(texCoord0, blockDepth);
    float sceneLimit = depth < 0.99999 ? max(dot(scenePosition - rayOrigin, rayDirection), 0.0) : 1000000.0;
    float blockLimit = dot(blockPosition - rayOrigin, rayDirection);

    // 导数在任何 discard/非一致分支之前计算，避免边缘像素出现未定义值。
    vec3 dx = dFdx(blockPosition);
    vec3 dy = dFdy(blockPosition);
    vec3 crossNormal = cross(dx, dy);
    vec3 normal = crossNormal / max(length(crossNormal), 0.000001);
    float pixelWidth = max(min(length(dx), length(dy)), 0.002);
    float rayPixelWidth = max(length(dFdx(rayDirection)), length(dFdy(rayDirection)));

    vec3 toCenter = FieldCenter - rayOrigin;
    float projectedCenter = dot(toCenter, rayDirection);
    float radiusSquared = FieldRadius * FieldRadius;
    float intersectionSquared = radiusSquared - (dot(toCenter, toCenter) - projectedCenter * projectedCenter);
    if (intersectionSquared < 0.0 || FieldRadius <= 0.01) discard;
    float offset = sqrt(max(intersectionSquared, 0.0));
    float nearDistance = projectedCenter - offset;
    float farDistance = projectedCenter + offset;
    if (farDistance <= 0.0 || nearDistance > sceneLimit) discard;

    float time = FieldTime * 0.05;
    bool inside = dot(FieldCenter, FieldCenter) < radiusSquared;
    vec4 result = vec4(0.0);
    // 使用以格为单位的遮挡容差，避免原先固定深度差在远处把实体也染成地形冰痕。
    if (blockDepth < 0.99999 && blockLimit <= sceneLimit + 0.025) {
        result = groundIce(blockPosition, normal, pixelWidth);
    }
    vec4 backShell = frozenShell(farDistance, sceneLimit, rayOrigin, rayDirection, rayPixelWidth, time, inside);
    result = over(backShell * 0.65, result);
    result = over(frozenShell(nearDistance, sceneLimit, rayOrigin, rayDirection, rayPixelWidth, time, inside), result);

    if (max(max(result.r, result.g), result.b) < 0.001) discard;
    // 预乘 Alpha 的冰层 + 独立加光项；无需外部 Bloom 即可显示裂纹周围的柔光。
    fragColor = result;
}

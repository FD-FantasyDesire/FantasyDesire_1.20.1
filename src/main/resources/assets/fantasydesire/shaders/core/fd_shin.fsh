#version 150

// ==================== 可调参数：大焰（长焰） ====================
const float LONG_FLAME_WIDTH = 0.55;       // 切向半宽系数，越大越宽；<=0 隐藏。
const float LONG_FLAME_HEIGHT = 0.85;       // 高度倍率，1 为当前高度；<=0 隐藏。
const float LONG_FLAME_Y_OFFSET = 0.0;    // 相对当前位置的竖直偏移，正数向上。
const float LONG_FLAME_COUNT = 3.0;       // 数量，填整数；0 关闭，最多 32。
const vec3 LONG_FLAME_COLOR = vec3(1.0, 0.75, 0.065); // 外焰 RGB，范围 0~1。
const vec3 LONG_FLAME_CORE_COLOR = vec3(1.0, 1, 1); // 内芯 RGB。

// ==================== 可调参数：小焰（短焰） ====================
const float SHORT_FLAME_WIDTH = 0.2;
const float SHORT_FLAME_HEIGHT = 1.5;
const float SHORT_FLAME_Y_OFFSET = 0.0;
const float SHORT_FLAME_COUNT = 6.0;
const vec3 SHORT_FLAME_COLOR = vec3(1.0, 0.75, 0.065);
const vec3 SHORT_FLAME_CORE_COLOR = vec3(1.0, 1, 1);

// 高度围绕各组原锚点缩放，位置偏移不随倍率变化；偏移 0.1 = 实体高度的 10%。
// 代理盒仍固定为局部 [-1,1]，过大的宽高/偏移会在盒边界柔和淡出。
// 修改后游戏内 F3+T 重载；以下为实现，通常无需修改。粒子颜色独立于大小焰。

uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;
uniform sampler2D AuraSampler;
uniform vec2 RenderSize; // 当前绘制目标的像素尺寸，低分辨率 pass 不等于深度纹理尺寸。
uniform int ResolvePass; // 0：体积积分；1：深度一致区域放大，遮挡边缘重新积分。
uniform mat4 InvProjMat;
uniform float EffectTime; // 秒，40 秒周期
uniform float EffectSeed;
uniform float EffectYaw; // 实体 yaw，角度制；Java 按最短路径插值。
uniform float ShinStrength;
uniform int RaySteps;
uniform float LongFlameStrength;
uniform float ShortFlameStrength;
uniform float SparkStrength;
uniform float FogStart;
uniform float FogEnd;
uniform int FogShape;
in vec3 localPosition;
in vec3 viewPosition;
flat in mat4 viewToLocal;
out vec4 fragColor;

const float TAU = 6.28318530718;

// 焰片用固定编号保持连续；光粒子另外混入出生代数，重生时重新取样。
vec4 random4(float key) {
    vec4 p = fract(vec4(key) * vec4(0.1031, 0.1030, 0.0973, 0.1099));
    p += dot(p, p.wzxy + 33.33);
    return fract((p.xxyz + p.yzzw) * p.zywx);
}

// NoiseSampler 必须为循环寻址、线性过滤；显式 LOD 避免分支内隐式导数。
vec3 flowingNoise(vec2 uv) {
    return textureLod(NoiseSampler, uv, 0.0).rgb;
}

// 每片焰有独立的倾斜和扭转；密度、金色柔光、白色内核共用变换后的局部坐标。
vec3 flameSheet(vec3 p, float radius, float offset, float id, float time, float stride, bool shortFlame) {
    float group = shortFlame ? 71.0 : 13.0;
    float motionTime = time * (shortFlame ? 1.0 : 2.0);
    float phase = motionTime * (TAU / 40.0);
    vec4 profile = random4(id * 17.0 + group + EffectSeed * 197.0);
    vec4 shape = random4(id * 23.0 + group + EffectSeed * 293.0);
    if (shortFlame) {
        // 按每片短焰的基础总高度下移一半；在采样域反向平移，保留原形状与流动方向。
        p.y += 0.5 * (mix(0.36, 0.56, profile.y) + mix(0.12, 0.24, profile.z));
    }
    float swing = phase * 4.0 + shape.y * TAU;
    float turn = (shape.x - 0.5) * 1.15 + 0.28 * sin(swing)
               + 0.09 * sin(phase * 7.0 + profile.w * TAU);
    float lean = (profile.x - 0.5) * (shortFlame ? 1.05 : 0.30)
               + sin(swing + shape.z * TAU) * (shortFlame ? 0.20 : 0.08);
    float anchor = shortFlame ? -0.20 + (shape.w - 0.5) * 0.16 : -0.74;
    float tangent = offset * 0.52 - (profile.w - 0.5) * 0.045;
    float vertical = p.y - anchor;
    // 先倾斜生长轴，再绕该轴扭转焰片；旋转相位同时驱动高度和曲率。
    vec2 tilted = vec2(cos(lean) * tangent - sin(lean) * vertical,
                      sin(lean) * tangent + cos(lean) * vertical);
    float heightWave = 1.0 + 0.09 * cos(swing)
                           + 0.04 * sin(phase * 9.0 + shape.z * TAU);
    float h;
    if (shortFlame) {
        // 两端长度不等，纹理贯穿完整焰片，不以 abs(y) 镜像上下半部。
        float upper = mix(0.36, 0.56, profile.y) * heightWave;
        float lower = mix(0.12, 0.24, profile.z)
                    * (1.0 + 0.10 * sin(phase * 6.0 + shape.y * TAU));
        h = (tilted.y + lower) / (upper + lower);
    } else {
        float height = min(mix(1.30, 1.48, profile.y) * heightWave, 1.63);
        h = tilted.y / height;
    }
    if (h <= 0.0 || h >= 1.0) return vec3(0.0);
    float y = clamp((p.y + 0.76) / 1.56, 0.0, 1.0);
    float shell = 0.23 + 0.33 * sin(y * 3.0);
    shell += (shape.w - 0.5) * (shortFlame ? 0.20 : 0.12);
    float radialOffset = radius - shell;
    vec2 sheetPoint = vec2(cos(turn) * tilted.x + sin(turn) * radialOffset,
                         -sin(turn) * tilted.x + cos(turn) * radialOffset);
    float bend = sin(h * 5.0 - swing + shape.z * TAU) * (0.025 + 0.035 * h);
    bend += (profile.z - 0.5) * h * h * 0.12;
    float halfWidth = (shortFlame ? SHORT_FLAME_WIDTH : LONG_FLAME_WIDTH) * pow(sin(h * 3.14159265), 0.70)
                    * mix(1.0, 0.65, h) * mix(0.85, 1.15, profile.w);
    if (abs(sheetPoint.x - bend) > halfWidth * 1.28 + 0.18 || abs(sheetPoint.y) > 0.40) return vec3(0.0);
    // 离线函数噪声携带三种独立频率；纵向拉长并二次扭曲，生成贯通亮纹与孔洞。
    // 显式读取第 0 层保证分支内采样稳定，Lab 与游戏使用同一张周期纹理。
    vec2 noiseUv = vec2(sheetPoint.x * 1.1 + id * 0.173 + group * 0.019,
                        h * 0.38 - motionTime * 0.025 + shape.z);
    vec3 flow = flowingNoise(noiseUv);
    vec3 detail = flowingNoise(noiseUv * 2.0 + (flow.rg - 0.5) * 0.22
                            + vec2(turn * 0.08, -motionTime * 0.025));
    float lateral = halfWidth * mix(0.72, 1.28, flow.g)
                  - abs(sheetPoint.x - bend + (flow.r - 0.5) * 0.18);
    // 用宽密度脊替代窄等值带；沿步进跨度软化侧边，减少薄片上的重复等高线。
    float filterWidth = clamp(stride * 0.65, 0.008, 0.055);
    float sheet = smoothstep(-0.025 - filterWidth, 0.030 + filterWidth, lateral);
    float field = flow.r * 0.82 + detail.g * 0.18;
    float membrane = smoothstep(0.27, 0.68, field);
    float radialWidth = mix(0.050, 0.085, shape.x) * mix(1.0, 0.62, h);
    float filteredWidth = sqrt(radialWidth * radialWidth + stride * stride);
    float radial = (sheetPoint.y - (flow.g - 0.5) * 0.12) / filteredWidth;
    // 两个相邻区域重叠取样，边界只衰减尾部，旋转时不会在分区线突然截断。
    float count = clamp(floor((shortFlame ? SHORT_FLAME_COUNT : LONG_FLAME_COUNT) + 0.5), 1.0, 32.0);
    float support = 1.0 - smoothstep(0.78, 1.0, abs(offset) * count / TAU);
    float envelope = smoothstep(0.0, 0.08, h) * (1.0 - smoothstep(0.85, 1.0, h)) * support
                   * (1.0 - smoothstep(0.30, 0.40, abs(sheetPoint.y)));
    float body = sheet * membrane * mix(0.65, 1.0, detail.b);
    float halo = smoothstep(-0.090, 0.018, lateral) * mix(0.12, 1.0, membrane);
    float core = smoothstep(-filterWidth, 0.055 + filterWidth, lateral)
               * smoothstep(0.40, 0.66, field);
    return vec3(exp(-radial * radial) * body * 1.55,
                exp(-radial * radial / 7.0) * halo * 0.22,
                exp(-radial * radial * 1.35) * core)
                * envelope * (radialWidth / filteredWidth);
}

vec3 flameLayer(vec3 p, float angle, float radius, float time, float stride, bool shortFlame) {
    float heightScale = shortFlame ? SHORT_FLAME_HEIGHT : LONG_FLAME_HEIGHT;
    float width = shortFlame ? SHORT_FLAME_WIDTH : LONG_FLAME_WIDTH;
    float requestedCount = shortFlame ? SHORT_FLAME_COUNT : LONG_FLAME_COUNT;
    if (heightScale <= 0.0 || width <= 0.0 || requestedCount <= 0.0) return vec3(0.0);
    float anchor = shortFlame ? -0.52 : -0.74;
    float yOffset = shortFlame ? SHORT_FLAME_Y_OFFSET : LONG_FLAME_Y_OFFSET;
    p.y = (p.y - yOffset - anchor) / max(heightScale, 0.001) + anchor;
    if (shortFlame && p.y > 0.58) return vec3(0.0);
    if (!shortFlame && p.y <= -0.82) return vec3(0.0);
    float count = clamp(floor(requestedCount + 0.5), 1.0, 32.0);
    float sector = (angle / TAU + 0.5) * count;
    float base = floor(sector);
    float offset = fract(sector) - 0.5;
    float neighbor = offset < 0.0 ? -1.0 : 1.0;
    vec3 density = flameSheet(p, radius, offset * TAU / count, mod(base, count), time, stride, shortFlame)
         + flameSheet(p, radius, (offset - neighbor) * TAU / count,
                      mod(base + neighbor, count), time, stride, shortFlame);
    return density * (shortFlame ? smoothstep(-0.98, -0.86, p.y) : smoothstep(-0.82, -0.73, p.y));
}

vec3 flame(vec3 p, float time, float stride, out vec3 outerColor, out vec3 coreColor) {
    outerColor = LONG_FLAME_COLOR;
    coreColor = LONG_FLAME_CORE_COLOR;
    if (p.y <= -0.98 || p.y >= 0.96) return vec3(0.0);
    float angle = atan(p.z, p.x + 0.00001);
    angle += (EffectSeed - 0.5) * TAU + 0.10 * sin(time * TAU / 40.0);
    float radius = length(p.xz);
    if (radius >= 0.96 || radius < 0.07) return vec3(0.0);
    vec3 longDensity = vec3(0.0), shortDensity = vec3(0.0);
    if (LongFlameStrength > 0.0) longDensity = flameLayer(p, angle, radius, time, stride, false)
        * LongFlameStrength;
    // 下移后的短焰在代理盒底部之前柔和归零，不沿用长焰较高的根部裁剪。
    if (ShortFlameStrength > 0.0) shortDensity = flameLayer(p, angle + 0.27, radius, time, stride, true)
        * ShortFlameStrength;
    // 重叠处按两组消光贡献混色，颜色不影响覆盖率和属性透明度。
    float longWeight = dot(longDensity, vec3(8.0, 2.5, 5.0));
    float shortWeight = dot(shortDensity, vec3(8.0, 2.5, 5.0));
    float shortMix = shortWeight / max(longWeight + shortWeight, 0.00001);
    outerColor = mix(LONG_FLAME_COLOR, SHORT_FLAME_COLOR, shortMix);
    coreColor = mix(LONG_FLAME_CORE_COLOR, SHORT_FLAME_CORE_COLOR, shortMix);
    return (longDensity + shortDensity) * (1.0 - smoothstep(0.82, 0.96, radius))
                   * (1.0 - smoothstep(0.88, 0.96, p.y));
}

// 有界球体沿可见射线段的解析厚度，细小粒子不依赖体积步进是否恰好命中。
float sparkMass(vec3 offset, vec3 direction, float start, float end, float radius) {
    float along = dot(offset, direction);
    float distanceSquared = max(dot(offset, offset) - along * along, 0.0);
    float radiusSquared = radius * radius;
    if (distanceSquared >= radiusSquared) return 0.0;
    float chord = sqrt(radiusSquared - distanceSquared);
    float visibleLength = max(0.0, min(end, along + chord) - max(start, along - chord));
    float falloff = 1.0 - distanceSquared / radiusSquared;
    return visibleLength / (2.0 * radius) * falloff * falloff;
}

vec2 sparks(vec3 origin, vec3 direction, float start, float end, float time) {
    vec2 mass = vec2(0.0);
    if (SparkStrength <= 0.0) return mass;
    for (int i = 0; i < 28; i++) {
        // 随机位置重新出生；寿命末端先归零，避免新位置闪跳。周期为 1.25–2 秒。
        vec4 fixedRandom = random4(float(i) * 9.0 + EffectSeed * 71.0 + 151.0);
        float generations = 20.0 + floor(fixedRandom.x * 13.0);
        float clock = time * (generations / 40.0) + fixedRandom.y;
        float age = fract(clock);
        vec4 birth = random4(float(i) * 19.0 + mod(floor(clock), generations) * 37.0 + EffectSeed * 131.0 + 271.0);
        float angle = birth.x * TAU + (birth.w - 0.5) * age * 0.40;
        float radius = mix(0.22, 0.58, birth.y) + age * 0.20;
        vec3 center = vec3(cos(angle) * radius, mix(-0.66, 0.28, birth.z) + age * 0.42, sin(angle) * radius);
        center.xz += vec2(sin(age * 7.0 + birth.w * TAU), cos(age * 5.0 + birth.x * TAU)) * (0.025 * age);
        float size = mix(0.009, 0.017, birth.w);
        float visibility = smoothstep(0.0, 0.12, age) * (1.0 - smoothstep(0.65, 1.0, age));
        vec3 offset = center - origin;
        mass += vec2(sparkMass(offset, direction, start, end, size),
                     sparkMass(offset, direction, start, end, size * 3.7)) * visibility;
    }
    return mass * SparkStrength;
}
void main() {
    if (ShinStrength <= 0.0) discard;
    // 第 1 至 18 档的不透明度系数从 20% 线性增长到 100%；零强度完全隐藏。
    float strengthAlpha = mix(0.20, 1.0, clamp((ShinStrength - 1.0) / 17.0, 0.0, 1.0));
    vec2 uv = gl_FragCoord.xy / max(RenderSize, vec2(1.0));
    vec2 ndc = uv * 2.0 - 1.0;
    // 从近裁面重建射线，兼容视角摇晃、受伤倾斜等非标准投影。
    vec4 nearView = InvProjMat * vec4(ndc, -1.0, 1.0);
    vec3 localCamera = (viewToLocal * (nearView / nearView.w)).xyz;
    vec3 ray = localPosition - localCamera;
    float surfaceDistance = length(ray);
    vec3 direction = ray / max(surfaceDistance, 0.00001);
    vec3 inverseRay = mix(vec3(-1.0), vec3(1.0), step(vec3(0.0), direction))
                    / max(abs(direction), vec3(0.00001));
    vec3 a = (-vec3(1.0) - localCamera) * inverseRay;
    vec3 b = ( vec3(1.0) - localCamera) * inverseRay;
    vec3 entry = min(a, b), exitPoint = max(a, b);
    float start = max(max(entry.x, max(entry.y, entry.z)), 0.0);
    float end = min(exitPoint.x, min(exitPoint.y, exitPoint.z));
    // 只采样独立深度副本，实体、装备及方块均终止光线积分。
    float depth = texture(DepthSampler, uv).r;
    vec4 sceneView = InvProjMat * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    vec3 sceneLocal = (viewToLocal * (sceneView / sceneView.w)).xyz;
    end = min(end, dot(sceneLocal - localCamera, direction));
    if (end <= start) discard;
    if (ResolvePass == 1) {
        // 只在四邻域与当前表面深度一致时插值；轮廓、薄物体及遮挡交界直接重算。
        // 低分辨率目标存预乘颜色，透明边缘插值不会染黑，也不会再次乘属性强度。
        ivec2 size = textureSize(AuraSampler, 0);
        vec2 pixel = uv * vec2(size) - 0.5;
        ivec2 base = ivec2(floor(pixel));
        vec2 fraction = fract(pixel);
        float sceneZ = abs(sceneView.z / sceneView.w);
        vec4 resolved = vec4(0.0);
        bool compatible = true;
        for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
            ivec2 coord = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            vec2 sampleUv = (vec2(coord) + 0.5) / vec2(size);
            float sampleDepth = textureLod(DepthSampler, sampleUv, 0.0).r;
            vec4 sampleView = InvProjMat * vec4(sampleUv * 2.0 - 1.0, sampleDepth * 2.0 - 1.0, 1.0);
            float sampleZ = abs(sampleView.z / sampleView.w);
            compatible = compatible && abs(sampleZ - sceneZ) <= max(0.002, sceneZ * 0.005);
            vec2 weight = mix(vec2(1.0) - fraction, fraction, vec2(x, y));
            resolved += texelFetch(AuraSampler, coord, 0) * weight.x * weight.y;
        }
        if (compatible) { fragColor = resolved; return; }
    }
    // 先以世界中的实体/地形深度裁剪射线，再逆向旋转到实体朝向空间。
    // 横向密度限制在单位圆内，轴对齐代理盒不必旋转，也不会在转身时裁断效果。
    float yaw = mod(EffectYaw, 360.0) * (TAU / 360.0);
    mat2 yawRotation = mat2(cos(yaw), -sin(yaw), sin(yaw), cos(yaw));
    localCamera.xz = yawRotation * localCamera.xz;
    direction.xz = yawRotation * direction.xz;
    int steps = clamp(RaySteps, 16, 64);
    float stride = (end - start) / float(steps);
    float time = mod(mod(EffectTime, 40.0) + 40.0, 40.0);

    vec3 color = vec3(0.0);
    float transmission = 1.0;
    for (int i = 0; i < 64; i++) {
        if (i >= steps) break;
        vec3 p = localCamera + direction * (start + (float(i) + 0.5) * stride);
        vec3 outerColor, coreColor;
        vec3 density = flame(p, time, stride, outerColor, coreColor);
        float opacity = 1.0 - exp(-(density.x * 8.0 + density.y * 2.5 + density.z * 5.0) * stride);
        float whiteCore = smoothstep(0.12, 0.48, density.z / max(density.x + density.y, 0.00001));
        vec3 emission = mix(outerColor, coreColor, whiteCore);
        color += transmission * opacity * emission;
        transmission *= 1.0 - opacity;
    }
    vec2 spark = sparks(localCamera, direction, start, end, time);
    float sparkOpacity = 1.0 - exp(-spark.x * 3.4 - spark.y * 0.48);
    vec3 sparkColor = mix(vec3(1.0, 0.67, 0.11), vec3(1.0, 0.99, 0.94),
                         spark.x / max(spark.x + spark.y * 0.08, 0.00001));
    color = color * (1.0 - sparkOpacity) + sparkColor * sparkOpacity;
    transmission *= 1.0 - sparkOpacity;
    float mask = 1.0 - transmission;
    float distanceToCamera = FogShape == 0 ? length(viewPosition)
        : max(length(viewPosition.xz), abs(viewPosition.y));
    float fog = 1.0 - smoothstep(FogStart, max(FogEnd, FogStart + 0.001), distanceToCamera);
    // 先整合体积，再乘强度系数一次，低分辨率重建不重复缩放。
    fragColor = vec4(color * strengthAlpha * fog, mask * strengthAlpha * fog);
}

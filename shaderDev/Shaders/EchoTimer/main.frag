// EchoTimer - 星界虚空焰形描边，WebGL 1 / GLSL ES 1.00，无贴图。
// 渲染顺序：焰型内部星云 -> 虚空焰形边缘 -> 实体剪影。
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_mouse;

const vec3 BACKGROUND_COLOR = vec3(0.0015, 0.0025, 0.010);
const vec3 NEBULA_DEEP_BLUE = vec3(0.004, 0.026, 0.120);
const vec3 NEBULA_MID_BLUE = vec3(0.006, 0.080, 0.255);
const vec3 NEBULA_SOFT_BLUE = vec3(0.018, 0.145, 0.380);
const vec3 VOID_DEEP = vec3(0.025, 0.010, 0.095);
const vec3 VOID_BLACK = vec3(0.004, 0.002, 0.016);
const vec3 VOID_STRIKE_COLOR = vec3(0.333333, 0.0, 0.666667); // 0x5500AA
const vec3 ECHO_DAMAGE_COLOR = vec3(0.5, 0.0, 1.0); // 0x8000FF

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float hash11(float value) {
    value = fract(value * 0.1031);
    value *= value + 33.33;
    value *= value + value;
    return fract(value);
}

float hash21(vec2 value) {
    value = fract(value * vec2(123.34, 456.21));
    value += dot(value, value + 45.32);
    return fract(value.x * value.y);
}

float valueNoise(vec2 position) {
    vec2 cell = floor(position);
    vec2 local = fract(position);
    local = local * local * (3.0 - 2.0 * local);

    float lowerLeft = hash21(cell);
    float lowerRight = hash21(cell + vec2(1.0, 0.0));
    float upperLeft = hash21(cell + vec2(0.0, 1.0));
    float upperRight = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(lowerLeft, lowerRight, local.x),
               mix(upperLeft, upperRight, local.x), local.y);
}

float fbm(vec2 position) {
    float result = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; ++octave) {
        result += valueNoise(position) * amplitude;
        position = mat2(1.63, 1.12, -1.12, 1.63) * position
                 + vec2(7.7, -4.3);
        amplitude *= 0.5;
    }
    return result;
}

float roundedBox(vec2 position, vec2 halfSize, float radius) {
    vec2 q = abs(position) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float capsule(vec2 position, vec2 start, vec2 end, float radius) {
    vec2 direction = end - start;
    float projection = clamp(dot(position - start, direction)
                       / max(dot(direction, direction), 0.00001), 0.0, 1.0);
    return length(position - (start + direction * projection)) - radius;
}

float softRing(float radius, float center, float width) {
    float safeWidth = max(width, 0.0005);
    float distanceToRing = (radius - center) / safeWidth;
    return exp(-distanceToRing * distanceToRing);
}

// 中心剪影用于模拟实体，正式迁移时由真实实体模型遮挡底层星空。
float targetBody(vec2 position) {
    float head = length(position - vec2(0.0, 0.56)) - 0.145;
    float torso = roundedBox(position - vec2(0.0, 0.17), vec2(0.205, 0.34), 0.075);
    float leftArm = capsule(position, vec2(-0.17, 0.37), vec2(-0.35, -0.02), 0.068);
    float rightArm = capsule(position, vec2(0.17, 0.37), vec2(0.35, -0.02), 0.068);
    float leftLeg = capsule(position, vec2(-0.085, -0.11), vec2(-0.15, -0.70), 0.082);
    float rightLeg = capsule(position, vec2(0.085, -0.11), vec2(0.15, -0.70), 0.082);
    return min(min(min(head, torso), min(leftArm, rightArm)),
               min(leftLeg, rightLeg));
}

// 分层的固定网格星点，所有星光都在实体填充之前绘制。
vec3 starLayer(vec2 position, float time, float scale, float drift,
               vec3 tint, float cutoff, float size) {
    vec2 scaled = position * scale + vec2(time * drift, -time * drift * 0.63);
    vec2 cell = floor(scaled);
    vec2 local = fract(scaled) - 0.5;
    float seed = hash21(cell);
    vec2 starOffset = vec2(hash11(seed * 31.7), hash11(seed * 53.1)) - 0.5;
    float distanceToStar = length(local - starOffset * 0.72);
    float star = exp(-distanceToStar * distanceToStar / max(size * size, 0.00001));
    float visible = smoothstep(cutoff, cutoff + 0.06, seed);
    float twinkle = 0.70 + 0.30 * sin(time * (1.2 + seed * 2.4) + seed * 44.0);
    return tint * star * visible * twinkle;
}

vec3 proceduralStarfield(vec2 position, float time,
                         float strikeStrength, float damageStrength) {
    float distanceFromCenter = length(position);
    float vignette = 1.0 - smoothstep(0.38, 1.70, distanceFromCenter);
    float largeCloud = fbm(position * 0.92
                         + vec2(time * 0.010, -time * 0.006));
    float middleCloud = fbm(position * 2.15
                           + vec2(-time * 0.014, time * 0.009)
                           + vec2(4.2, -2.8));
    float fineCloud = fbm(position * 5.20
                        + vec2(time * 0.026, -time * 0.019)
                        + vec2(-7.0, 3.4));
    float largeMask = smoothstep(0.28, 0.76, largeCloud);
    float middleMask = smoothstep(0.35, 0.73, middleCloud);
    float fineMask = smoothstep(0.43, 0.70, fineCloud);

    // 这里只返回星云纹理；背景颜色和轮廓遮罩由主函数决定。
    vec3 color = vec3(0.0);
    vec3 strikeStars = mix(vec3(0.10, 0.07, 0.30),
                           VOID_STRIKE_COLOR, strikeStrength);
    vec3 damageStars = mix(vec3(0.20, 0.08, 0.40),
                           ECHO_DAMAGE_COLOR, damageStrength);
    // 三个尺度叠加成深蓝星云，云团比星点更暗，避免盖住双色焰型。
    color += NEBULA_DEEP_BLUE * largeMask * vignette * 0.78;
    color += NEBULA_MID_BLUE * middleMask * (0.28 + largeMask * 0.52)
           * vignette * 0.62;
    color += NEBULA_SOFT_BLUE * fineMask * middleMask * vignette * 0.34;
    color += starLayer(position, time, 8.0, 0.010,
                       strikeStars, 0.73, 0.024)
            * (0.64 + strikeStrength * 0.82);
    color += starLayer(position, time, 14.0, -0.014,
                       mix(vec3(0.18, 0.32, 0.72), damageStars, 0.68),
                       0.81, 0.017) * (0.56 + damageStrength * 1.02);
    color += starLayer(position, time, 23.0, 0.020,
                       damageStars, 0.88, 0.011)
            * (0.42 + damageStrength * 1.18);

    // 目标附近增加一层较密的微型星点，让双焰之间的虚空不会塌成纯色。
    color += starLayer(position + vec2(0.031, -0.047), time * 1.12,
                       31.0, -0.010, damageStars, 0.92, 0.006)
            * (0.30 + damageStrength * 0.72);
    return color;
}

// 通过 SDF 外侧距离和极角噪声生成有上窜趋势的焰舌。
// layerOffset 用来把两层焰型分开，0 为内层，1 为外层。
float voidFlame(vec2 position, float bodyDistance, float time,
                float strength, float layerOffset, float reachScale) {
    vec2 direction = normalize(position + vec2(0.0001, 0.0001));
    float angle = atan(direction.y, direction.x);
    float angularNoise = fbm(vec2(cos(angle), sin(angle)) * 4.8
                           + vec2(time * 0.18 + layerOffset * 3.7,
                                  -time * 0.12 - layerOffset * 2.1));
    float tongues = 0.5 + 0.5 * cos(angle * 8.0
                                  + angularNoise * 4.2
                                  - time * (1.2 + strength * 1.1)
                                  + layerOffset * 1.9);
    tongues = pow(saturate(tongues), 3.0);

    float upwardBias = smoothstep(-0.80, 0.78, position.y);
    float reach = layerOffset * (0.020 + strength * 0.012)
                + 0.014 + angularNoise * 0.026
                + tongues * (0.016 + strength * 0.048) * reachScale;
    reach *= 0.72 + upwardBias * 0.72;

    float width = 0.010 + tongues * 0.010 + strength * 0.006;
    float flameBand = exp(-pow((bodyDistance - reach) / width, 2.0));
    float outside = smoothstep(-0.004, 0.014, bodyDistance);
    float flicker = 0.72 + 0.28 * sin(time * (2.2 + strength * 1.6)
                                      + angularNoise * 10.0 + layerOffset * 2.8);
    return flameBand * outside * (0.56 + tongues * 0.95)
         * flicker;
}

float flameTips(vec2 position, float bodyDistance, float time,
                float strength, float layerOffset, float reachScale) {
    vec2 direction = normalize(position + vec2(0.0001, 0.0001));
    float angle = atan(direction.y, direction.x);
    float pattern = 0.5 + 0.5 * sin(angle * 11.0 - time * 1.8
                                      + layerOffset * 2.4);
    pattern = pow(saturate(pattern), 5.0);
    float upward = smoothstep(-0.30, 0.90, position.y);
    float tipRadius = layerOffset * 0.022
                    + 0.035
                    + pattern * (0.045 + strength * 0.045) * upward * reachScale;
    float tip = exp(-pow((bodyDistance - tipRadius) / 0.012, 2.0));
    return tip * smoothstep(0.0, 0.018, bodyDistance) * pattern;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 position = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float time = mod(u_time, 4096.0);

    // 横轴模拟 VOID_STRIKE 层数（0~50），纵轴模拟存储回响伤害（0~1000）。
    float strikeLayers = u_mouse.x > 0.0
                       ? saturate(u_mouse.x / max(resolution.x, 1.0)) * 50.0
                       : 25.0;
    float storedDamage = u_mouse.y > 0.0
                       ? (1.0 - saturate(u_mouse.y / max(resolution.y, 1.0))) * 1000.0
                       : 500.0;
    storedDamage = min(storedDamage, 1000.0);
    float strikeStrength = saturate(strikeLayers / 50.0);
    float damageStrength = saturate(storedDamage / 1000.0);
    float visualIntensity = saturate(strikeStrength * 0.72 + damageStrength * 0.48);
    float breathing = 0.82 + 0.18 * sin(time * (1.35 + visualIntensity * 0.8));

    float bodyDistance = targetBody(position);

    // 两层焰型独立计算：VOID_STRIKE 在内侧，存储伤害在外侧。
    float strikeFlame = voidFlame(position, bodyDistance, time,
                                  strikeStrength, 0.0, 0.86);
    float strikeTips = flameTips(position, bodyDistance, time,
                                 strikeStrength, 0.0, 0.86);
    float damageFlame = voidFlame(position, bodyDistance, time,
                                  damageStrength, 1.0, 1.08);
    float damageTips = flameTips(position, bodyDistance, time,
                                 damageStrength, 1.0, 1.08);
    float strikeIntensity = strikeStrength * (0.72 + breathing * 0.28);
    float damageIntensity = damageStrength * (0.64 + breathing * 0.36);

    // 第一层：保持画布背景近黑，星云只填充两层焰型的内部。
    vec3 color = BACKGROUND_COLOR;
    float flameSpace = saturate(strikeFlame * strikeIntensity * 1.30
                              + damageFlame * damageIntensity * 1.45);
    float outlineInterior = smoothstep(-0.004, 0.016, bodyDistance)
                          * (1.0 - smoothstep(0.055 + damageStrength * 0.035,
                                              0.145 + damageStrength * 0.065,
                                              bodyDistance));
    flameSpace = saturate(flameSpace
                        + outlineInterior * (0.34 + damageStrength * 0.28));
    vec3 internalNebula = proceduralStarfield(position, time,
                                               strikeStrength, damageStrength);
    color += internalNebula * flameSpace
           * (0.82 + visualIntensity * 0.34);

    // 实体外侧的暗色虚空晕，给焰形描边一个侵蚀边界。
    float outerHaze = exp(-max(bodyDistance, 0.0) * 12.0)
                    * smoothstep(-0.005, 0.055, bodyDistance);
    color += VOID_STRIKE_COLOR * outerHaze
           * strikeIntensity * 0.32;
    color += ECHO_DAMAGE_COLOR * outerHaze
           * damageIntensity * 0.11;

    // 第二层：两种焰型分别着色，不做第三种颜色的混合。
    color += VOID_STRIKE_COLOR * strikeFlame
           * strikeIntensity * (0.72 + strikeIntensity * 0.62);
    color += VOID_STRIKE_COLOR * strikeTips * strikeIntensity * 0.62;
    color += ECHO_DAMAGE_COLOR * damageFlame
           * damageIntensity * (0.72 + damageIntensity * 0.62);
    color += ECHO_DAMAGE_COLOR * damageTips * damageIntensity * 0.62;

    // 第三层：实体填充覆盖星点，模拟星空位于真实实体渲染层下方。
    float body = 1.0 - smoothstep(-0.012, 0.012, bodyDistance);
    float bodyTexture = fbm(position * 4.8 + vec2(time * 0.06, -time * 0.04));
    vec3 bodyColor = VOID_BLACK + VOID_DEEP * bodyTexture * 0.22
                   + ECHO_DAMAGE_COLOR * bodyTexture * damageStrength * 0.045;
    color = mix(color, bodyColor, body);

    // 实体内侧只留一圈很细的 VOID_STRIKE 色边缘，避免盖住模型本身的材质。
    float innerEdge = exp(-abs(bodyDistance) * 92.0)
                    * (1.0 - smoothstep(-0.030, -0.002, bodyDistance));
    color += VOID_STRIKE_COLOR * innerEdge
           * strikeIntensity * 0.40 * breathing;
    color += ECHO_DAMAGE_COLOR * innerEdge * damageStrength * 0.18;

    // 色调映射保留星空暗部，避免边缘焰光冲成纯白。
    color = vec3(1.0) - exp(-color * (1.05 + visualIntensity * 0.18));
    color *= 0.80 + (1.0 - smoothstep(0.32, 1.60, length(position))) * 0.20;
    gl_FragColor = vec4(color, 1.0);
}

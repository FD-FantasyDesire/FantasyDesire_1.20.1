#version 150

uniform sampler2D Sampler0;
uniform float FogStart;
uniform float FogEnd;
uniform float EffectTime;     // 秒，实体 tick + partialTick 在 Java 端除以 20。
uniform float EffectSeed;     // 每实体稳定种子，0~1。
uniform float VoidStrength;   // sqrt(层数 / 50)，0~1。
uniform float EchoStrength;   // 记账伤害 / (记账伤害 + 120)，0~1。
uniform vec4 EchoPulseAges;   // 四道记账脉冲各自经过的秒数。
uniform vec4 EchoPulses;      // 四道脉冲各自的强度，0~1。

in vec2 texCoord0;
in vec3 localPosition;
in float vertexDistance;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash31(cell), hash31(cell + vec3(1, 0, 0)), f.x),
                   mix(hash31(cell + vec3(0, 1, 0)), hash31(cell + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash31(cell + vec3(0, 0, 1)), hash31(cell + vec3(1, 0, 1)), f.x),
                   mix(hash31(cell + vec3(0, 1, 1)), hash31(cell + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float erosionField(vec3 p) {
    // 三个固定频段；场本身不随时间重置，叠层只推动阈值，已经侵蚀的位置保持连续。
    return noise3(p) * 0.57 + noise3(p * 2.03 + 13.7) * 0.29
            + noise3(p * 4.11 + 29.2) * 0.14;
}

void main() {
    vec4 skin = texture(Sampler0, texCoord0);
    if (skin.a < 0.1 || max(VoidStrength, EchoStrength) <= 0.0001) {
        discard;
    }

    vec3 p = localPosition * 3.6 + vec3(EffectSeed * 37.0, 7.3, EffectSeed * 19.0);
    float field = erosionField(p);
    float fieldAA = clamp(fwidth(field), 0.006, 0.055);
    float threshold = mix(0.24, 0.76, VoidStrength);
    float voidPresence = smoothstep(0.0, 0.13, VoidStrength);
    float eroded = (1.0 - smoothstep(threshold - fieldAA, threshold + fieldAA, field)) * voidPresence;
    float front = (1.0 - smoothstep(0.008, 0.028 + fieldAA, abs(field - threshold))) * voidPresence;

    // 扭曲的三维等值线形成分叉裂隙，避免 UV 接缝与规则网格；伤害只增加裂隙密度与亮度。
    vec3 warped = p * 1.7 + vec3(field * 1.9, field * -1.4, field);
    float veinField = noise3(warped);
    float secondaryField = noise3(warped * 1.83 + 21.0);
    float ridge = abs(veinField - 0.5);
    float ridgeAA = clamp(fwidth(veinField), 0.003, 0.035);
    float width = mix(0.009, 0.038, EchoStrength);
    float veins = 1.0 - smoothstep(width, width + ridgeAA, ridge);
    float branches = 1.0 - smoothstep(0.006, 0.015 + ridgeAA, abs(secondaryField - 0.5));
    float echoPresence = smoothstep(0.0, 0.06, EchoStrength);
    float chargedRegion = 1.0 - smoothstep(0.24 + EchoStrength * 0.65,
            0.30 + EchoStrength * 0.65, field);
    veins = max(veins, branches * smoothstep(0.28, 0.85, EchoStrength)) * chargedRegion * echoPresence;

    // 沿局部坐标向上输运能量；新增记账触发一次传播前沿，固定时限后没有脉冲残留。
    float flowPhase = localPosition.y * 13.0 + field * 9.0 + EffectTime * 3.4;
    float flow = pow(0.5 + 0.5 * sin(flowPhase), 5.0);
    float pulse = 0.0;
    for (int i = 0; i < 4; ++i) {
        float age = EchoPulseAges[i];
        float travel = age * 3.4;
        float pulseFront = exp(-pow((localPosition.y - 1.65 + travel) * 4.0, 2.0));
        pulse += pulseFront * EchoPulses[i] * (1.0 - smoothstep(0.7, 1.2, age));
    }
    pulse = min(pulse, 1.25) * echoPresence;
    float ember = noise3(p * 2.4 + vec3(0.0, EffectTime * 0.38, 0.0));
    float innerFlame = smoothstep(0.50, 0.82, ember) * eroded;

    // Alpha 混合压暗原皮肤，紫黑斑块像材质被吞噬；发亮裂隙留在表面内部。
    float skinLuma = dot(skin.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 hollow = vec3(0.012, 0.003, 0.033) + vec3(0.024, 0.008, 0.045) * skinLuma;
    hollow += vec3(0.095, 0.007, 0.21) * innerFlame;
    vec3 voidEdge = vec3(0.333, 0.015, 0.667) * front * (0.65 + innerFlame * 0.35);
    float energy = veins * (0.40 + EchoStrength * 0.55 + flow * 0.32 + pulse * 0.75);
    vec3 echoColor = mix(vec3(0.50, 0.025, 1.0), vec3(0.85, 0.52, 1.0),
            clamp(flow * EchoStrength * 0.55 + pulse * 0.75, 0.0, 1.0));
    vec3 color = hollow + voidEdge + echoColor * energy;
    float alpha = max(eroded * (0.72 + VoidStrength * 0.23), front * 0.80);
    alpha = max(alpha, veins * (0.65 + EchoStrength * 0.30));
    float fogVisibility = 1.0 - smoothstep(FogStart, max(FogStart + 0.001, FogEnd), vertexDistance);
    alpha *= skin.a * fogVisibility;
    if (alpha < 0.003) {
        discard;
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}

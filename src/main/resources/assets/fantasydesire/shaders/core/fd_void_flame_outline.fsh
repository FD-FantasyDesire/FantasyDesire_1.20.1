#version 150

uniform sampler2D SilhouetteSampler;
uniform vec2 ScreenSize;
uniform float FlameTime;
uniform float EffectRadius;
uniform vec2 EffectCenter;
uniform vec3 TargetColor;
uniform float EffectSeed;
uniform float VoidStrikeStrength;
uniform float EchoDamageStrength;

in vec2 texCoord0;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float lower = mix(hash21(cell), hash21(cell + vec2(1.0, 0.0)), local.x);
    float upper = mix(hash21(cell + vec2(0.0, 1.0)), hash21(cell + vec2(1.0)), local.x);
    return mix(lower, upper, local.y);
}

vec2 rotateVector(vec2 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec2(cosine * value.x - sine * value.y, sine * value.x + cosine * value.y);
}

float targetSilhouette(vec2 uv) {
    vec4 maskSample = texture(SilhouetteSampler, clamp(uv, vec2(0.0), vec2(1.0)));
    if (maskSample.a < 0.01) {
        return 0.0;
    }

    vec3 decodedColor = maskSample.rgb / maskSample.a;
    vec3 colorDifference = abs(decodedColor - TargetColor);
    return max(colorDifference.r, max(colorDifference.g, colorDifference.b)) < (1.0 / 510.0)
            ? maskSample.a : 0.0;
}

bool traceSilhouette(vec2 uv, vec2 direction, float distanceLimit,
        out float hitDistance, out vec2 hitUv) {
    vec2 pixel = 1.0 / max(ScreenSize, vec2(1.0));
    float searchDistance = min(EffectRadius * 1.35 + 4.0, distanceLimit);
    float previousDistance = 0.0;

    for (int stepIndex = 1; stepIndex <= 20; ++stepIndex) {
        float distance = searchDistance * float(stepIndex) / 20.0;
        vec2 sampleUv = uv - direction * pixel * distance;
        if (targetSilhouette(sampleUv) < 0.08) {
            previousDistance = distance;
            continue;
        }

        float lower = previousDistance;
        float upper = distance;
        for (int refinementIndex = 0; refinementIndex < 5; ++refinementIndex) {
            float middle = (lower + upper) * 0.5;
            vec2 middleUv = uv - direction * pixel * middle;
            if (targetSilhouette(middleUv) >= 0.08) {
                upper = middle;
            } else {
                lower = middle;
            }
        }

        hitDistance = upper;
        hitUv = uv - direction * pixel * upper;
        return true;
    }

    return false;
}

void main() {
    if (targetSilhouette(texCoord0) > 0.01) {
        discard;
    }

    vec2 centerDirectionPixels = (texCoord0 - EffectCenter) * ScreenSize;
    vec2 baseDirection = length(centerDirectionPixels) > 0.001
            ? normalize(centerDirectionPixels) : vec2(0.0, 1.0);
    float outlineDistance = 1.0e20;
    vec2 nearestUv = texCoord0;
    vec2 outwardDirection = baseDirection;

    // 仅朝效果中心追踪会漏掉手臂下缘等凹轮廓；环形搜索取最近遮罩命中，
    // 使轮廓距离和火焰外向方向不再随观察俯仰角翻转或消失。
    float candidateDistance;
    vec2 candidateUv;
    for (int directionIndex = 0; directionIndex < 12; ++directionIndex) {
        float directionAngle = float(directionIndex) * 0.5235987756;
        vec2 candidateDirection = rotateVector(baseDirection, directionAngle);
        if (traceSilhouette(texCoord0, candidateDirection, outlineDistance,
                candidateDistance, candidateUv) && candidateDistance < outlineDistance) {
            outlineDistance = candidateDistance;
            nearestUv = candidateUv;
            outwardDirection = candidateDirection;
        }
    }

    if (outlineDistance > EffectRadius * 1.35 + 3.5) {
        discard;
    }

    float upwardBias = smoothstep(-0.85, 0.80, outwardDirection.y);
    float time = FlameTime * 0.05;
    vec2 localCoordinates = (nearestUv - EffectCenter) * ScreenSize / max(EffectRadius, 1.0);
    float broadNoise = valueNoise(localCoordinates * vec2(2.4, 1.6)
            + vec2(EffectSeed * 1.7 + time * 0.16, EffectSeed * 2.3 - time * 0.23));
    float fineNoise = valueNoise(localCoordinates * vec2(5.0, 3.4)
            + vec2(EffectSeed * 3.1 - time * 0.31, EffectSeed * 4.7 + time * 0.19));

    float innerTongues = smoothstep(0.24, 0.78, broadNoise * 0.68 + fineNoise * 0.32);
    float innerRadius = EffectRadius * 0.54;
    float innerReach = innerRadius * (0.40 + broadNoise * 0.08
            + innerTongues * (0.30 + VoidStrikeStrength * 0.16))
            * (0.82 + upwardBias * 0.48);

    float outerTongues = smoothstep(0.22, 0.76, fineNoise * 0.62 + broadNoise * 0.38);
    float outerReach = EffectRadius * (0.42 + fineNoise * 0.08
            + outerTongues * (0.38 + EchoDamageStrength * 0.18))
            * (0.82 + upwardBias * 0.52);

    float antialiasWidth = max(fwidth(outlineDistance), 0.75);
    float innerWidth = max(3.50, innerRadius * (0.18 + innerTongues * 0.07)) + antialiasWidth;
    float outerWidth = max(4.50, EffectRadius * (0.17 + outerTongues * 0.08)) + antialiasWidth;
    float innerOffset = (outlineDistance - innerReach) / innerWidth;
    float outerOffset = (outlineDistance - outerReach) / outerWidth;
    float strikeFlame = exp(-innerOffset * innerOffset);
    float damageFlame = exp(-outerOffset * outerOffset);
    float innerBase = exp(-outlineDistance * outlineDistance
            / max(innerRadius * innerRadius * 0.38, 1.0));
    float outerBase = exp(-outlineDistance * outlineDistance
            / max(EffectRadius * EffectRadius * 0.28, 1.0));
    strikeFlame = max(strikeFlame, innerBase * 0.38);
    damageFlame = max(damageFlame, outerBase * 0.30);

    float strikeIntensity = VoidStrikeStrength;
    float damageIntensity = EchoDamageStrength;
    float inner = strikeFlame * strikeIntensity;
    float outer = damageFlame * damageIntensity * (1.0 - inner * 0.82);
    vec3 innerColor = vec3(0.333333, 0.0, 0.666667) * inner * (0.80 + strikeIntensity * 0.52);
    vec3 outerColor = vec3(0.5, 0.0, 1.0) * outer * (0.74 + damageIntensity * 0.62);
    vec3 color = innerColor + outerColor;
    float alpha = clamp(inner * 1.42 + outer * 1.18, 0.0, 1.0);

    if (alpha < 0.002) {
        discard;
    }
    fragColor = vec4(color, alpha);
}

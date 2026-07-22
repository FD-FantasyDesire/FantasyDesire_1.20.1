#ifndef SHADER_DEV_COMMON_GLSL
#define SHADER_DEV_COMMON_GLSL

const float COMMON_PI = 3.141592653589793;
const float COMMON_TAU = 6.283185307179586;

mat2 rotate(float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return mat2(c, -s, s, c);
}

float remap(float value, float inMin, float inMax, float outMin, float outMax) {
    float denominator = inMax - inMin;
    float safeDenominator = abs(denominator) < 0.00001
        ? (denominator < 0.0 ? -0.00001 : 0.00001)
        : denominator;
    return outMin + (value - inMin) * (outMax - outMin) / safeDenominator;
}

float remapClamped(float value, float inMin, float inMax, float outMin, float outMax) {
    float t = clamp(remap(value, inMin, inMax, 0.0, 1.0), 0.0, 1.0);
    return mix(outMin, outMax, t);
}

float pulse(float center, float width, float value) {
    float halfWidth = max(abs(width) * 0.5, 0.00001);
    return 1.0 - smoothstep(halfWidth * 0.65, halfWidth, abs(value - center));
}

float lineGlow(float distanceToLine, float radius, float strength) {
    return strength * radius / max(abs(distanceToLine), 0.001);
}

#endif

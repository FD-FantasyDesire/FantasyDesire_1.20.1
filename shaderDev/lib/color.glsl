#ifndef SHADER_DEV_COLOR_GLSL
#define SHADER_DEV_COLOR_GLSL

vec3 cosinePalette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.283185307179586 * (c * t + d));
}

vec3 magicPalette(float t) {
    return cosinePalette(
        t,
        vec3(0.48, 0.42, 0.55),
        vec3(0.52, 0.48, 0.45),
        vec3(1.00, 0.82, 0.62),
        vec3(0.05, 0.18, 0.42)
    );
}

vec3 hsvToRgb(vec3 hsv) {
    vec3 rgb = clamp(
        abs(mod(hsv.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0,
        0.0,
        1.0
    );
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return hsv.z * mix(vec3(1.0), rgb, hsv.y);
}

vec3 rgbToHsv(vec3 rgb) {
    vec4 k = vec4(0.0, -0.3333333333, 0.6666666667, -1.0);
    vec4 p = mix(vec4(rgb.bg, k.wz), vec4(rgb.gb, k.xy), step(rgb.b, rgb.g));
    vec4 q = mix(vec4(p.xyw, rgb.r), vec4(rgb.r, p.yzx), step(p.x, rgb.r));
    float delta = q.x - min(q.w, q.y);
    float epsilon = 0.0000001;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * delta + epsilon)), delta / (q.x + epsilon), q.x);
}

#endif

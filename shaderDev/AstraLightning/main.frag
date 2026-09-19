// 星座闪电基础预览：WebGL 1 / GLSL ES 1.00，无纹理。
// 起终点与折点在一次生命期内固定；颜色以 0xRRGGBB 输入，MAX_ENDPOINTS 包含首尾。
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
precision highp int;
#else
precision mediump float;
precision mediump int;
#endif
uniform float u_time;       // 秒
uniform vec2 u_resolution;  // 像素

const vec2 START = vec2(-0.88, -0.38);
const vec2 END = vec2(0.86, 0.42);
const int COLOR = 0x8000FF;
const int MAX_ENDPOINTS = 9;
const float SEED = 42.0;
const float RANDOMNESS = 0.24;
const float THICKNESS = 0.012;
const float STAR_RADIUS = 0.080;
const float LIFETIME = 1.2;  // 秒，循环间隔额外留出 0.45 秒

vec3 rgb(int value) {
    float packed = float(value);
    return mod(floor(packed / vec3(65536.0, 256.0, 1.0)), 256.0) / 255.0;
}

float hash(float index, float channel) {
    vec3 p = fract(vec3(index, channel, SEED) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 node(int index, int count) {
    if (index == 0) return START;
    if (index == count - 1) return END;
    vec2 axis = END - START;
    float len = max(length(axis), 0.000001);
    float i = float(index);
    float t = (i + (hash(i, 1.0) - 0.5) * 0.36) / float(count - 1);
    vec2 side = vec2(-axis.y, axis.x) / len;
    float amplitude = min(RANDOMNESS, len / float(count - 1) * 1.3);
    // 二维预览逐折点随机选侧，不按节点奇偶固定左右方向。
    float direction = hash(i, 5.0) < 0.5 ? -1.0 : 1.0;
    float offset = mix(0.45, 1.0, hash(i, 2.0)) * direction;
    return START + axis * t + side * amplitude * sin(t * 3.14159265) * offset;
}

float softCore(float distance, float width, float pixel) {
    float filtered = sqrt(width * width + pixel * pixel);
    return exp(-distance * distance / (filtered * filtered)) * width / filtered;
}

float starRay(vec2 p, vec2 pixel) {
    float along = abs(p.x);
    float width = 0.009 + 0.085 * exp(-along * 12.0);
    return softCore(abs(p.y), width, pixel.y) * exp(-along * 3.8)
        * (1.0 - smoothstep(0.55, 0.92, along));
}

vec3 star(vec2 delta, float size, float pixel, vec3 tint, vec3 core) {
    vec2 p = delta / size;
    vec2 aa = vec2(pixel / size);
    float cross = starRay(p / vec2(0.84, 1.0), aa / vec2(0.84, 1.0))
        + starRay(p.yx / vec2(1.0, 0.84), aa / vec2(1.0, 0.84));
    vec2 diagonal = vec2(p.x + p.y, p.y - p.x) * 1.35;
    vec2 diagonalAA = aa * 2.7;
    float glints = (starRay(diagonal, diagonalAA) + starRay(diagonal.yx, diagonalAA)) * 0.10;
    float nucleus = softCore(length(p), 0.045, length(aa));
    float halo = exp(-length(p) * 8.0) * (1.0 - smoothstep(0.60, 0.96, length(p)));
    return core * (cross * 1.05 + nucleus * 0.65 + glints) + tint * (halo * 0.65 + cross * 0.18);
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = min(resolution.x, resolution.y);
    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float pixel = 1.3 / shortSide;
    float seconds = mod(max(u_time, 0.0), LIFETIME + 0.45);
    float progress = clamp(seconds / LIFETIME, 0.0, 1.0);
    float envelope = smoothstep(0.0, 0.075, seconds) * (1.0 - smoothstep(0.22, 1.0, progress));
    vec3 tint = rgb(COLOR);
    vec3 core = mix(tint, vec3(max(tint.r, max(tint.g, tint.b))), 0.82);
    float len = length(END - START);
    int count = int(min(min(max(float(MAX_ENDPOINTS), 2.0), 33.0),
        max(2.0, ceil(len / max(0.20, STAR_RADIUS * 3.0)) + 1.0)));
    if (len < 0.000001) count = 1;
    vec3 emission = vec3(0.0);
    for (int i = 0; i < 33; i++) {
        if (i >= count) break;
        vec2 a = node(i, count);
        float fi = float(i);
        float wave = (fi / max(1.0, float(count - 1)) - seconds * 2.4) * 5.0;
        float flash = exp(-pow((seconds * 20.0 - 2.0) / 1.3, 2.0));
        float pulse = min(1.0, 0.72 + 0.20 * exp(-wave * wave) + 0.08 * flash);
        float twinkle = 0.91 + 0.09 * sin(seconds * 13.0 + hash(fi, 3.0) * 6.2831853);
        float size = (i == 0 || i == count - 1) ? 1.25 : mix(0.85, 1.10, hash(fi, 4.0));
        emission += star(p - a, STAR_RADIUS * 1.8 * size * (0.94 + 0.06 * twinkle), pixel, tint, core) * twinkle * pulse;
        if (i < count - 1) {
            vec2 b = node(i + 1, count);
            vec2 ab = b - a;
            float t = clamp(dot(p - a, ab) / max(dot(ab, ab), 0.0000001), 0.0, 1.0);
            float d = length(p - a - ab * t) / (THICKNESS * 4.0);
            float aa = pixel / (THICKNESS * 4.0);
            float halo = exp(-d * 5.5) * (1.0 - smoothstep(0.65, 1.0, d));
            emission += (core * softCore(d, 0.065, aa) * 1.05
                + tint * (softCore(d, 0.22, aa) * 0.48 + halo * 0.14)) * pulse;
        }
    }
    vec3 background = mix(vec3(0.012, 0.018, 0.034), vec3(0.003, 0.005, 0.012), smoothstep(0.0, 1.6, length(p)));
    // 与正式管线一样采用加法；不利用全屏曝光掩盖过宽的辉光。
    gl_FragColor = vec4(background + emission * envelope, 1.0);
}

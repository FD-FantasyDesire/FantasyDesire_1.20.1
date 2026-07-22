// Astra Lightning — WebGL 1 / GLSL ES 1.00, self-contained, no textures.
// Custom parameters (edit these constants):
//   OUTER_COLOR = 外缘辉光颜色；CORE_COLOR = 近白核心颜色。
//   CORE_WIDTH = 中心线宽度；OUTER_WIDTH = 外缘线宽度。
//   GLOW_STRENGTH = 辉光强度；FLASH_SIZE / FLASH_STRENGTH = 节点十字闪光。
//   ANIMATION_SPEED = 动画速度；NODE_JITTER = 曲折节点的稳定扰动幅度。
// 未完成内容，预计用于改善line粒子
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float u_time;
uniform vec2 u_resolution;

const vec3 OUTER_COLOR = vec3(0.0, 0.0, 1.0);
const vec3 CORE_COLOR = vec3(0.0, 0.6667, 1.0);
const float CORE_WIDTH = 0.006;
const float OUTER_WIDTH = 0.026;
const float GLOW_STRENGTH = 0.72;
const float FLASH_SIZE = 0.075;
const float FLASH_STRENGTH = 1.20;
const float ANIMATION_SPEED = 0.72;
const float NODE_JITTER = 0.035;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.00001), 0.0, 1.0);
    return length(pa - ba * h);
}

// One fixed point-to-point zigzag path; explicit nodes avoid WebGL 1 dynamic indexing.
vec2 mainNode(int index, float time) {
    vec2 point = vec2(0.0);
    if (index == 0) point = vec2(-0.82, -0.56);
    if (index == 1) point = vec2(-0.57, -0.25);
    if (index == 2) point = vec2(-0.34, -0.39);
    if (index == 3) point = vec2(-0.09, -0.02);
    if (index == 4) point = vec2(0.18, 0.16);
    if (index == 5) point = vec2(0.42, 0.48);
    if (index == 6) point = vec2(0.79, 0.64);
    float fi = float(index);
    point += NODE_JITTER * vec2(
        sin(time * 1.8 + fi * 2.17),
        cos(time * 1.5 - fi * 1.71)
    );
    return point;
}

float lineDistance(vec2 p, float time) {
    float distanceToLine = 10.0;
    for (int i = 0; i < 6; ++i) {
        vec2 a = mainNode(i, time);
        vec2 b = mainNode(i + 1, time);
        distanceToLine = min(distanceToLine, sdSegment(p, a, b));
    }
    return distanceToLine;
}

float nodeFlash(vec2 p, vec2 point, float phase) {
    vec2 delta = p - point;
    float radial = exp(-length(delta) / max(FLASH_SIZE * 0.45, 0.001));
    float crossX = exp(-abs(delta.x) / max(FLASH_SIZE * 1.65, 0.001)) *
        exp(-abs(delta.y) / max(FLASH_SIZE * 0.085, 0.001));
    float crossY = exp(-abs(delta.y) / max(FLASH_SIZE * 1.65, 0.001)) *
        exp(-abs(delta.x) / max(FLASH_SIZE * 0.085, 0.001));
    float pulse = 0.82 + 0.18 * sin(phase);
    return (radial * 0.48 + (crossX + crossY) * 0.72) * pulse;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = min(resolution.x, resolution.y);
    vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / shortSide;

    float time = mod(u_time, 4096.0) * ANIMATION_SPEED;
    vec2 p = uv;

    vec3 color = vec3(0.002, 0.006, 0.024);
    float distanceToLine = lineDistance(p, time);
    float outer = GLOW_STRENGTH * OUTER_WIDTH /
        max(distanceToLine, 0.0015);
    outer *= 1.0 - smoothstep(0.05, 1.25, length(p));
    color += OUTER_COLOR * outer;

    float core = CORE_WIDTH / max(distanceToLine, 0.0012);
    core *= 0.82 + 0.18 * sin(time * 5.0 + distanceToLine * 90.0);
    color += CORE_COLOR * core;

    float flashes = 0.0;
    for (int i = 0; i < 7; ++i) {
        flashes += nodeFlash(p, mainNode(i, time), time * 4.0 + float(i));
    }
    color += (OUTER_COLOR * 0.28 + CORE_COLOR * 0.72) * flashes * FLASH_STRENGTH;

    // Sparse, stable constellation pinpricks add depth without textures.
    for (int k = 0; k < 8; ++k) {
        float fk = float(k);
        vec2 star = vec2(
            mix(-0.92, 0.92, hash21(vec2(fk, 7.1))),
            mix(-0.82, 0.82, hash21(vec2(fk, 9.4)))
        );
        float starGlow = 0.003 / max(length(p - star), 0.006);
        color += OUTER_COLOR * starGlow * 0.06;
    }

    float vignette = 1.0 - smoothstep(0.72, 1.55, length(uv));
    color *= 0.35 + 0.65 * vignette;
    color = 1.0 - exp(-color * 1.10);
    color = pow(max(color, vec3(0.0)), vec3(0.92));
    gl_FragColor = vec4(color, 1.0);
}

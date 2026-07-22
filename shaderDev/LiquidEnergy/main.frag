// Liquid Energy — WebGL 1 / GLSL ES 1.00, self-contained, no textures.
// Custom parameters (edit these constants):
//   DEEP_COLOR / LIQUID_COLOR / CORE_COLOR = 暗部、液体主体与核心颜色。
//   BALL_RADIUS = 中心球半径；BRANCH_WIDTH = 枝条基础宽度。
//   UNION_SOFTNESS = 液体平滑黏连程度；GLOW_STRENGTH = 外部辉光强度。
//   FLOW_SPEED = 流动速度；WOBBLE_AMOUNT = 球面和枝条的呼吸扰动。
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float u_time;
uniform vec2 u_resolution;

const vec3 BACKGROUND_COLOR = vec3(0.002, 0.008, 0.020);
const vec3 DEEP_COLOR = vec3(0.015, 0.16, 0.32);
const vec3 LIQUID_COLOR = vec3(0.02, 0.66, 0.92);
const vec3 CORE_COLOR = vec3(0.82, 1.00, 0.98);

const float BALL_RADIUS = 0.255;
const float BRANCH_WIDTH = 0.036;
const float UNION_SOFTNESS = 0.075;
const float GLOW_STRENGTH = 0.80;
const float FLOW_SPEED = 0.72;
const float WOBBLE_AMOUNT = 0.018;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float smoothUnion(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.0001), 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float sdCapsule(vec2 p, vec2 a, vec2 b, float radius) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.00001), 0.0, 1.0);
    return length(pa - ba * h) - radius;
}

// Explicit branch data avoids arrays and dynamic indexing on WebGL 1 drivers.
void branchData(int index, out vec2 bend, out vec2 tip, out float width) {
    bend = vec2(0.0);
    tip = vec2(0.0);
    width = BRANCH_WIDTH;
    if (index == 0) {
        bend = vec2(0.43, 0.12);
        tip = vec2(0.78, 0.23);
        width *= 1.05;
    }
    if (index == 1) {
        bend = vec2(0.18, 0.40);
        tip = vec2(0.30, 0.73);
        width *= 0.82;
    }
    if (index == 2) {
        bend = vec2(-0.25, 0.36);
        tip = vec2(-0.55, 0.63);
        width *= 0.94;
    }
    if (index == 3) {
        bend = vec2(-0.43, 0.01);
        tip = vec2(-0.83, -0.08);
        width *= 1.10;
    }
    if (index == 4) {
        bend = vec2(-0.19, -0.38);
        tip = vec2(-0.34, -0.76);
        width *= 0.76;
    }
    if (index == 5) {
        bend = vec2(0.31, -0.31);
        tip = vec2(0.67, -0.49);
        width *= 0.92;
    }
}

vec2 branchStart(vec2 bend) {
    return normalize(bend) * (BALL_RADIUS * 0.68);
}

float liquidDistance(vec2 p, float time) {
    float angle = atan(p.y, p.x);
    float ripple = sin(angle * 5.0 + time * 1.7) * 0.55;
    ripple += sin(angle * 8.0 - time * 1.25) * 0.45;
    float distanceField = length(p) - BALL_RADIUS - ripple * WOBBLE_AMOUNT;

    for (int i = 0; i < 6; ++i) {
        vec2 bend;
        vec2 tip;
        float width;
        branchData(i, bend, tip, width);

        float fi = float(i);
        vec2 drift = WOBBLE_AMOUNT * vec2(
            sin(time * 1.31 + fi * 2.43),
            cos(time * 1.17 - fi * 1.79)
        );
        vec2 start = branchStart(bend);
        vec2 movingBend = bend + drift;
        vec2 movingTip = tip + drift * 0.65;

        float root = sdCapsule(p, start, movingBend, width);
        float twig = sdCapsule(p, movingBend, movingTip, width * 0.62);
        float joint = length(p - movingBend) - width * 1.34;
        float tipDrop = length(p - movingTip) - width * (0.84 + 0.15 * sin(time * 2.0 + fi));

        distanceField = smoothUnion(distanceField, root, UNION_SOFTNESS);
        distanceField = smoothUnion(distanceField, twig, UNION_SOFTNESS * 0.56);
        distanceField = smoothUnion(distanceField, joint, UNION_SOFTNESS * 0.50);
        distanceField = smoothUnion(distanceField, tipDrop, UNION_SOFTNESS * 0.34);
    }
    return distanceField;
}

float flowingHighlights(vec2 p, float time) {
    float light = 0.0;
    for (int i = 0; i < 6; ++i) {
        vec2 bend;
        vec2 tip;
        float width;
        branchData(i, bend, tip, width);

        float fi = float(i);
        vec2 drift = WOBBLE_AMOUNT * vec2(
            sin(time * 1.31 + fi * 2.43),
            cos(time * 1.17 - fi * 1.79)
        );
        vec2 start = branchStart(bend);
        vec2 movingBend = bend + drift;
        vec2 movingTip = tip + drift * 0.65;
        float travel = fract(time * 0.23 + fi * 0.173);
        vec2 spot;
        if (travel < 0.52) {
            spot = mix(start, movingBend, travel / 0.52);
        } else {
            spot = mix(movingBend, movingTip, (travel - 0.52) / 0.48);
        }
        float spotDistance = length(p - spot);
        light += exp(-spotDistance * spotDistance / max(width * width * 1.35, 0.0001));
    }
    return light;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = min(resolution.x, resolution.y);
    vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / shortSide;
    float time = mod(u_time, 4096.0) * FLOW_SPEED;

    float distanceField = liquidDistance(uv, time);
    float inside = 1.0 - smoothstep(-0.006, 0.009, distanceField);
    float edge = exp(-abs(distanceField) * 62.0);
    float nearGlow = exp(-max(distanceField, 0.0) * 17.0);
    float farGlow = exp(-max(distanceField, 0.0) * 5.0);

    vec3 color = BACKGROUND_COLOR;
    color += DEEP_COLOR * inside * 0.72;
    color += LIQUID_COLOR * nearGlow * GLOW_STRENGTH * 0.48;
    color += LIQUID_COLOR * farGlow * GLOW_STRENGTH * 0.095;
    color += CORE_COLOR * edge * (0.42 + inside * 0.28);

    float radialCore = exp(-dot(uv, uv) / 0.024);
    float innerPulse = 0.82 + 0.18 * sin(time * 3.6);
    color += CORE_COLOR * radialCore * innerPulse * 1.35;
    color += LIQUID_COLOR * exp(-dot(uv, uv) / 0.095) * 0.34;

    float flow = flowingHighlights(uv, time) * inside;
    color += (LIQUID_COLOR * 0.34 + CORE_COLOR * 0.66) * flow * 1.45;

    // A soft offset reflection makes the central mass read as glossy liquid.
    vec2 reflectionPoint = vec2(-0.075, 0.085);
    float reflection = exp(-dot(uv - reflectionPoint, uv - reflectionPoint) / 0.0065);
    color += CORE_COLOR * reflection * inside * 0.85;

    float vignette = 1.0 - smoothstep(0.72, 1.55, length(uv));
    color *= 0.42 + 0.58 * vignette;
    color = 1.0 - exp(-color * 1.15);
    color = pow(max(color, vec3(0.0)), vec3(0.92));
    gl_FragColor = vec4(color, 1.0);
}

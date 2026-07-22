// WebGL 1 / GLSL ES 1.00 — self-contained procedural magic-rune prototype.
precision highp float;

uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_mouse;

const float PI = 3.141592653589793;
const float TAU = 6.283185307179586;

mat2 rotate2d(float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return mat2(c, -s, s, c);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    vec2 blend = local * local * (3.0 - 2.0 * local);

    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, blend.x), mix(c, d, blend.x), blend.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    mat2 turn = mat2(0.80, -0.60, 0.60, 0.80);
    for (int octave = 0; octave < 5; ++octave) {
        sum += amplitude * valueNoise(p);
        p = turn * p * 2.03 + vec2(17.1, 9.2);
        amplitude *= 0.5;
    }
    return sum;
}

vec3 palette(float t) {
    vec3 a = vec3(0.48, 0.42, 0.55);
    vec3 b = vec3(0.52, 0.48, 0.45);
    vec3 c = vec3(1.00, 0.82, 0.62);
    vec3 d = vec3(0.05, 0.18, 0.42);
    return a + b * cos(TAU * (c * t + d));
}

float sdCircle(vec2 p, float radius) {
    return abs(length(p) - radius);
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.00001), 0.0, 1.0);
    return length(pa - ba * h);
}

float glow(float distanceToShape, float radius, float strength) {
    return strength * radius / max(distanceToShape, 0.0015);
}

float runeSpokes(vec2 p, float phase) {
    float angle = atan(p.y, p.x);
    float radius = length(p);
    float sector = abs(sin(angle * 6.0 + phase));
    float radialBand = abs(radius - mix(0.34, 0.43, sector));
    return radialBand;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / min(resolution.x, resolution.y);
    vec2 mouse = vec2(0.0);
    if (dot(u_mouse, u_mouse) > 0.0) {
        mouse = (2.0 * u_mouse - resolution.xy) / min(resolution.x, resolution.y);
    }
    mouse = clamp(mouse, vec2(-1.5), vec2(1.5));

    float time = u_time;
    float mouseInfluence = exp(-1.8 * dot(uv - mouse, uv - mouse));
    vec2 p = uv - mouse * 0.07;

    float radius = length(p);
    float angle = atan(p.y, p.x);
    float turbulence = fbm(p * 3.2 + vec2(time * 0.11, -time * 0.08));
    float vortex = sin(angle * 7.0 - time * 2.2 + radius * 18.0 + turbulence * 4.0);

    vec3 color = vec3(0.004, 0.007, 0.025);
    color += vec3(0.015, 0.025, 0.080) * (1.0 - smoothstep(0.0, 1.45, radius));
    color += palette(turbulence * 0.7 + time * 0.025) *
             pow(max(vortex * 0.5 + 0.5, 0.0), 5.0) *
             exp(-2.7 * radius) * 0.16;

    vec2 outerP = rotate2d(time * 0.13 + mouse.x * 0.25) * p;
    vec2 innerP = rotate2d(-time * 0.21 + mouse.y * 0.25) * p;
    float pulse = 0.5 + 0.5 * sin(time * 2.4 + turbulence * 3.0);

    float outerRing = sdCircle(outerP, 0.67 + 0.008 * sin(time * 1.7));
    float middleRing = sdCircle(innerP, 0.49);
    float innerRing = sdCircle(p, 0.225 + 0.012 * pulse);
    float spokes = runeSpokes(outerP, time * 0.8);
    spokes = max(spokes, abs(radius - 0.385));

    float glyph = 10.0;
    glyph = min(glyph, sdSegment(innerP, vec2(-0.27, -0.18), vec2(0.0, 0.30)));
    glyph = min(glyph, sdSegment(innerP, vec2(0.0, 0.30), vec2(0.27, -0.18)));
    glyph = min(glyph, sdSegment(innerP, vec2(-0.19, -0.04), vec2(0.19, -0.04)));
    glyph = min(glyph, sdSegment(innerP, vec2(-0.16, -0.22), vec2(0.16, -0.22)));

    float runeMask = glow(outerRing, 0.010, 0.13);
    runeMask += glow(middleRing, 0.008, 0.10);
    runeMask += glow(innerRing, 0.007, 0.08);
    runeMask += glow(spokes, 0.004, 0.06) * smoothstep(0.52, 0.28, radius);
    runeMask += glow(glyph, 0.006, 0.09);

    vec3 runeColor = palette(0.56 + 0.10 * sin(time * 0.4) + radius * 0.25);
    color += runeColor * runeMask * (0.78 + 0.35 * pulse + 0.30 * mouseInfluence);

    float core = 0.018 / max(radius + 0.035, 0.035);
    core *= 0.65 + 0.35 * sin(time * 3.0 + turbulence * 5.0);
    color += vec3(0.38, 0.72, 1.20) * core;

    float spark = 0.0;
    for (int i = 0; i < 9; ++i) {
        float fi = float(i);
        float orbit = 0.29 + 0.055 * mod(fi, 3.0);
        float speed = mix(-0.65, 0.80, hash21(vec2(fi, 2.7)));
        float a = fi * 2.399963 + time * speed;
        vec2 point = vec2(cos(a), sin(a)) * orbit;
        point += 0.025 * vec2(sin(time + fi), cos(time * 1.3 + fi));
        spark += 0.0025 / max(length(p - point), 0.004);
    }
    color += vec3(0.45, 0.82, 1.25) * spark * (0.8 + 0.5 * mouseInfluence);

    float vignette = 1.0 - smoothstep(0.72, 1.65, length(uv));
    color *= 0.42 + 0.58 * vignette;
    color = 1.0 - exp(-color * 1.15);
    color = pow(max(color, vec3(0.0)), vec3(0.90));

    gl_FragColor = vec4(color, 1.0);
}

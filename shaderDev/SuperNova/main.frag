// SuperNova — self-contained WebGL 1 / GLSL ES 1.00 overlay, no textures.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;

// The GLSL Canvas host does not expose project-specific color uniforms.
// Edit these two linear RGB constants (0.0–1.0); they tint the whole effect.
const vec3 CORE_COLOR = vec3(1.00, 0.34, 0.08);
const vec3 CORONA_COLOR = vec3(0.24, 0.48, 1.00);

const float EFFECT_SCALE = 0.82;
const float EXPOSURE = 1.30;
const float PI = 3.14159265359;

float sat(float x) {
    return clamp(x, 0.0, 1.0);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; ++i) {
        sum += valueNoise(p) * amplitude;
        p = mat2(1.62, 1.17, -1.17, 1.62) * p + vec2(8.3, -5.7);
        amplitude *= 0.5;
    }
    return sum;
}

vec2 rotate2D(vec2 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c) * p;
}

float softRing(float radius, float center, float width) {
    float safeWidth = max(width, 0.0005);
    float d = (radius - center) / safeWidth;
    return exp(-d * d);
}

// A rotating polar turbulence field. Different callers use different scale,
// speed and parallax offsets so the corona does not read as one flat disc.
float polarTurbulence(vec2 p, float time, float scale, float speed, float seed) {
    float radius = max(length(p), 0.0005);
    float angle = atan(p.y, p.x);
    vec2 direction = vec2(cos(angle), sin(angle));
    vec2 domain = direction * (scale * 0.18)
                + vec2(radius * scale * 0.72, radius * scale * 0.29)
                + vec2(time * speed, -time * speed * 1.7);
    return fbm(domain + vec2(seed, -seed * 0.73));
}

float rayField(vec2 p, float time, float layer) {
    float radius = max(length(p), 0.001);
    float angle = atan(p.y, p.x);
    vec2 direction = vec2(cos(angle), sin(angle));
    float turn = time * mix(0.08, -0.13, layer);
    float angularNoise = valueNoise(direction * 2.7
                                   + vec2(layer * 11.0, time * 0.35));
    float spokes = pow(0.5 + 0.5 * cos(angle * mix(17.0, 23.0, layer) + turn + angularNoise * 4.0), 14.0);
    float broken = smoothstep(0.38, 0.80,
        valueNoise(direction * 6.2
                 + vec2(layer * 17.0 + radius * 2.3,
                        radius * 8.0 - time * 0.9)));
    float radialFade = exp(-radius * mix(3.2, 4.8, layer));
    float centerCut = smoothstep(0.12, 0.28, radius);
    return spokes * (0.24 + 0.76 * broken) * radialFade * centerCut;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    p /= max(EFFECT_SCALE, 0.001);
    float time = mod(u_time, 4096.0);
    float radius = length(p);
    float pulse = 0.96 + 0.04 * sin(time * 1.65);

    vec3 light = vec3(0.0);
    float opacity = 0.0;

    // Reconstruct the visible hemisphere depth. It offsets front layers toward
    // a moving virtual view direction, creating restrained procedural parallax.
    float sphereRadius = 0.245 * pulse;
    float sphereCoord = radius / max(sphereRadius, 0.001);
    float depth = sqrt(max(1.0 - min(sphereCoord * sphereCoord, 1.0), 0.0));
    vec2 viewDirection = vec2(cos(time * 0.23), sin(time * 0.19)) * 0.018;

    vec2 farCoord = rotate2D(p - viewDirection * depth * 0.35, -time * 0.10);
    vec2 middleCoord = rotate2D(p + viewDirection * depth * 0.70, time * 0.16);
    vec2 nearCoord = rotate2D(p + viewDirection * depth * 1.20, -time * 0.23);

    float farNoise = polarTurbulence(farCoord, time, 7.0, -0.035, 3.7);
    float middleNoise = polarTurbulence(middleCoord, time, 10.0, 0.052, 9.2);
    float nearNoise = polarTurbulence(nearCoord, time, 15.0, -0.075, 15.8);

    float sphereMask = 1.0 - smoothstep(0.82, 1.03, sphereCoord);
    float limb = smoothstep(0.05, 0.92, 1.0 - depth);
    float farShell = sphereMask * smoothstep(0.30, 0.78, farNoise) * (0.30 + 0.70 * limb);
    float middleShell = sphereMask * smoothstep(0.34, 0.76, middleNoise) * (0.38 + 0.62 * depth);
    float nearShell = sphereMask * smoothstep(0.43, 0.82, nearNoise) * (0.18 + 0.82 * depth);

    // Far shell is partially hidden by the denser front layers; this subtle
    // bright/dark ordering is as important to the pseudo-3D read as parallax.
    float frontOcclusion = sat(middleShell * 0.42 + nearShell * 0.68);
    farShell *= 1.0 - frontOcclusion * 0.58;
    float core = exp(-dot(p, p) / max(sphereRadius * sphereRadius * 0.30, 0.00001));
    float hotCore = exp(-dot(p, p) / 0.0042);

    light += CORONA_COLOR * farShell * 0.72;
    light += mix(CORONA_COLOR, CORE_COLOR, 0.48) * middleShell * 1.05;
    light += CORE_COLOR * nearShell * 1.18;
    light += mix(CORE_COLOR, vec3(1.0), 0.76) * core * (1.20 + depth * 0.85);
    light += mix(CORE_COLOR, vec3(1.0), 0.92) * hotCore * 2.15;
    opacity = max(opacity, sat(sphereMask * 0.90 + core));

    // Uneven corona and two counter-rotating ray layers sit around the core.
    float coronaNoise = polarTurbulence(p, time, 8.0, 0.04, 22.1);
    float corona = exp(-radius * 4.6) * (0.34 + 0.66 * coronaNoise);
    corona *= 1.0 - smoothstep(0.70, 1.42, radius);
    float raysBack = rayField(rotate2D(p, time * 0.07), time, 0.0);
    float raysFront = rayField(rotate2D(p + viewDirection * 0.4, -time * 0.11), time, 1.0);
    raysBack *= 1.0 - sphereMask * 0.72;
    light += CORONA_COLOR * corona * 0.50;
    light += CORONA_COLOR * raysBack * 0.64;
    light += mix(CORONA_COLOR, CORE_COLOR, 0.55) * raysFront * 0.82;
    opacity = max(opacity, sat(corona * 0.48 + raysBack * 0.56 + raysFront * 0.70));

    // Breathing shock rings use distinct radii, widths and angular disruption.
    float wave = 0.5 + 0.5 * sin(time * 0.72);
    float ringOneRadius = mix(0.42, 0.70, wave);
    float ringTwoRadius = mix(0.31, 0.57, 0.5 + 0.5 * sin(time * 0.72 + 2.1));
    float ringWarp = (polarTurbulence(p, time, 5.0, -0.025, 31.4) - 0.5) * 0.055;
    float ringOne = softRing(radius + ringWarp, ringOneRadius, 0.018 + wave * 0.012);
    float ringTwo = softRing(radius - ringWarp * 0.6, ringTwoRadius, 0.032) * 0.42;
    float ringFade = 1.0 - smoothstep(0.95, 1.36, radius);
    light += mix(CORE_COLOR, CORONA_COLOR, 0.72) * (ringOne + ringTwo) * ringFade;
    opacity = max(opacity, sat((ringOne + ringTwo) * ringFade * 0.78));

    // Preserve transparent surroundings for overlay use; RGB is tone-mapped.
    light = vec3(1.0) - exp(-max(light, vec3(0.0)) * EXPOSURE);
    opacity *= 1.0 - smoothstep(1.05, 1.48, radius);
    gl_FragColor = vec4(light, sat(opacity));
}

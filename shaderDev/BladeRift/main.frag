// BladeRift — self-contained WebGL 1 / GLSL ES 1.00 single-pass artwork.
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float u_time;
uniform vec2 u_resolution;

const float CYCLE = 1.00;
const float RIFT_LENGTH = 0.9;
const float RIFT_WIDTH = 0.03;
const float DISTORTION = 0.095;
const float EXPOSURE = 1.35;
const float EMISSIVE_STRENGTH = 0.82;
const float SPLIT_STRENGTH = 1.0;
// Independent color entrances for the thin center core and outer energy.
const vec3 CORE_COLOR = vec3(0.0, 0.8667, 1.0);
const vec3 ENERGY_COLOR = vec3(0.0, 0.0157, 0.9765);

float hash21(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
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
    float amp = 0.5;
    for (int i = 0; i < 5; ++i) {
        sum += amp * valueNoise(p);
        p = p * 2.03 + vec2(17.1, -9.4);
        amp *= 0.5;
    }
    return sum;
}

vec2 rotate2(vec2 p, float a) {
    float s = sin(a);
    float c = cos(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

float phaseSpawn(float t) { return smoothstep(0.0, 0.135, t); }
float phaseFade(float t) { return 1.0 - smoothstep(0.68, 1.0, t); }

vec3 spaceBackground(vec2 p, float time) {
    float cloud = fbm(p * 1.65 + vec2(time * 0.035, -time * 0.022));
    float wisps = fbm(p * 3.8 - vec2(time * 0.09, time * 0.055));
    vec3 col = vec3(0.0025, 0.004, 0.014);
    col += vec3(0.018, 0.006, 0.040) * cloud * cloud;
    col += vec3(0.004, 0.018, 0.034) * wisps * 0.55;
    vec2 grid = floor(p * 10.0);
    vec2 cell = fract(p * 10.0) - 0.5;
    float seed = hash21(grid);
    float star = 1.0 - smoothstep(0.002, 0.022, length(cell - (seed - 0.5) * 0.35));
    star *= smoothstep(0.80, 0.84, seed)
        * (0.62 + 0.38 * sin(time * (1.5 + seed * 3.0) + seed * 8.0));
    col += vec3(0.22, 0.42, 0.78) * star;
    return col * (0.42 + 0.58 * (1.0 - smoothstep(0.38, 1.45, length(p))));
}

float riftHalfWidth(float along, float growth) {
    float taper = pow(max(0.0, 1.0 - abs(along) / max(RIFT_LENGTH, 0.001)), 0.58);
    return RIFT_WIDTH * taper * (0.72 + 0.34 * growth);
}

// A signed, finite distance for the tapered diamond.  The old
// max(abs(y) - halfWidth, abs(x) - length) was only a box-like approximation:
// once halfWidth reached zero past a tip, it became abs(y), so glow could run
// indefinitely along the axis.  Combining the two outside components closes
// the distance around each tip.  The inside branch keeps a stable signed
// distance for the narrow seam and body masks (WebGL 1 compatible).
float finiteRiftDistance(vec2 p, float growth) {
    // Keep the finite distance itself left/right symmetric. Animated FBM remains
    // in the interior flow and displacement magnitude, never in tip falloff.
    float halfWidth = max(riftHalfWidth(p.x, growth), 0.0001);
    float axialOutside = max(abs(p.x) - RIFT_LENGTH, 0.0);
    float normalOutside = max(abs(p.y) - halfWidth, 0.0);
    float outsideDistance = length(vec2(axialOutside, normalOutside));
    float insideDistance = -min(RIFT_LENGTH - abs(p.x), halfWidth - abs(p.y));
    return (axialOutside > 0.0 || normalOutside > 0.0) ? outsideDistance : insideDistance;
}

// The core has its own finite tapered diamond, independent of the outer glow.
float finiteCoreDistance(vec2 p) {
    float coreLength = RIFT_LENGTH * 0.94;
    float coreWidth = RIFT_WIDTH * 0.19;
    float taper = pow(max(0.0, 1.0 - abs(p.x) / max(coreLength, 0.001)), 0.72);
    float halfWidth = max(coreWidth * taper, 0.0001);
    float axialOutside = max(abs(p.x) - coreLength, 0.0);
    float normalOutside = max(abs(p.y) - halfWidth, 0.0);
    float outsideDistance = length(vec2(axialOutside, normalOutside));
    float insideDistance = -min(coreLength - abs(p.x), halfWidth - abs(p.y));
    return (axialOutside > 0.0 || normalOutside > 0.0) ? outsideDistance : insideDistance;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / shortSide;
    float time = mod(u_time, 4096.0);
    float phase = mod(time, CYCLE) / CYCLE;
    float spawn = phaseSpawn(phase);
    float active = spawn * phaseFade(phase);
    float growth = mix(0.10, 1.0, spawn);
    float overshoot = 1.0 + 0.55 * exp(-phase * 34.0);
    float flowTime = time * 2.25;

    float angle = -0.48;
    vec2 q = rotate2(uv, -angle);
    float side = q.y >= 0.0 ? 1.0 : -1.0;
    float riftDistance = finiteRiftDistance(q, spawn);
    // The moving front exists only during Spawn. Once Spawn ends every visual
    // mask switches to the complete finite shape, so Hold/Fade stay symmetric.
    float spawnOnly = 1.0 - step(0.135, phase);
    float revealEnd = mix(-RIFT_LENGTH, RIFT_LENGTH, spawn);
    float bodyFront = 1.0 - smoothstep(revealEnd - 0.015, revealEnd + 0.045, q.x);
    float bodyReveal = mix(1.0, bodyFront, spawnOnly);
    float body = (1.0 - smoothstep(-0.008, 0.004, riftDistance)) * bodyReveal * active;

    // Distortion owns a finite, independently feathered band. Its axial ends,
    // normal reach and moving Spawn front all fade before coordinate sampling.
    float splitAxisStart = smoothstep(-RIFT_LENGTH - 0.025, -RIFT_LENGTH + 0.115, q.x);
    float splitAxisEnd = 1.0 - smoothstep(RIFT_LENGTH - 0.115, RIFT_LENGTH + 0.025, q.x);
    float splitFront = 1.0 - smoothstep(revealEnd - 0.105, revealEnd + 0.045, q.x);
    float splitSpawnFront = mix(1.0, splitFront, spawnOnly);
    float splitNormal = 1.0 - smoothstep(RIFT_WIDTH * 1.5, RIFT_WIDTH * 8.5, abs(q.y));
    float splitFalloff = 1.0 - smoothstep(0.0, 0.12, max(riftDistance, 0.0));
    float distortionMask = splitAxisStart * splitAxisEnd * splitSpawnFront * splitNormal
        * splitFalloff * active;
    float animatedFbm = fbm(q * 3.2 + vec2(flowTime * 0.22, -flowTime * 0.18));
    // FBM modulates magnitude only: upper/lower halves always move outward.
    float splitMagnitude = DISTORTION * SPLIT_STRENGTH * distortionMask
        * (0.62 + 0.38 * animatedFbm);
    float displacement = side * splitMagnitude;
    vec2 bgCoord = rotate2(vec2(q.x, q.y + displacement), angle);
    vec3 color = spaceBackground(bgCoord, time);

    // Restrained RGB split belongs to the distortion band, not emissive scope.
    float chroma = exp(-abs(riftDistance) * 38.0) * distortionMask;
    vec2 chromaVec = rotate2(vec2(0.0, 0.010 * side), angle);
    vec3 redSample = spaceBackground(bgCoord + chromaVec, time);
    vec3 blueSample = spaceBackground(bgCoord - chromaVec, time);
    color += vec3(redSample.r, 0.0, blueSample.b) * chroma * 0.34;

    // Emission uses a one-sided opacity only while spawning. Hold/Fade are
    // driven solely by the symmetric finite distance and lifecycle opacity.
    float emissiveFront = 1.0 - smoothstep(revealEnd + 0.075, revealEnd + 0.22, q.x);
    float emissiveReveal = mix(1.0, emissiveFront, spawnOnly);
    float emissiveMask = emissiveReveal * active * EMISSIVE_STRENGTH;
    float glowAxialStart = RIFT_LENGTH + 0.16;
    float glowAxialEnd = RIFT_LENGTH + 0.22;
    float glowNormalStart = RIFT_WIDTH * 7.0;
    float glowNormalEnd = RIFT_WIDTH * 11.0;
    float glowClip = (1.0 - smoothstep(glowAxialStart, glowAxialEnd, abs(q.x)))
        * (1.0 - smoothstep(glowNormalStart, glowNormalEnd, abs(q.y)));
    emissiveMask *= glowClip;
    float outsideDistance = max(riftDistance, 0.0);
    float nearGlow = exp(-outsideDistance * 25.0) * emissiveMask;
    float farGlow = exp(-outsideDistance * 7.0) * emissiveMask;
    float edge = exp(-abs(riftDistance) * 115.0) * emissiveMask;
    vec3 voidColor = vec3(0.004, 0.006, 0.018);
    vec3 energyAccent = mix(ENERGY_COLOR, CORE_COLOR, 0.38);
    color = mix(color, color * 0.20 + voidColor, body * 0.90);
    color += ENERGY_COLOR * farGlow * 0.42;
    color += mix(ENERGY_COLOR, energyAccent, 0.5 + 0.5 * sin(q.x * 7.0 + flowTime)) * nearGlow * 0.90;
    color += energyAccent * edge * 0.55;

    // Internal liquid energy remains below the center diamond in composition.
    float nebula = fbm(vec2(q.x * 7.0 - flowTime * 0.35, q.y * 17.0 + flowTime * 0.22));
    float vein = abs(sin(q.x * 24.0 + flowTime * 1.7 + nebula * 8.0) + 0.42 * sin(q.x * 57.0 - flowTime * 2.4));
    vein = exp(-vein * 8.0) * body;
    color += mix(energyAccent, ENERGY_COLOR, nebula) * (0.35 + 0.65 * nebula) * body * 0.52;
    color += CORE_COLOR * vein * 0.72;

    // The center uses a separate finite SDF and cannot leak as an abs(y) line.
    float coreDistance = finiteCoreDistance(q);
    float coreMask = 1.0 - smoothstep(0.0, 0.0045, max(coreDistance, 0.0));
    float coreReveal = bodyReveal;

    // Keep the spatial fault outside the core body and away from its bright
    // antialiased edge. It is a weak background-side shadow, never a center line.
    float coreExclusion = 1.0 - smoothstep(0.012, 0.040, abs(coreDistance));
    float seamSides = smoothstep(RIFT_WIDTH * 0.20, RIFT_WIDTH * 0.25, abs(q.y))
        * (1.0 - smoothstep(RIFT_WIDTH * 0.34, RIFT_WIDTH * 0.48, abs(q.y)));
    float seam = seamSides * bodyReveal * active * (1.0 - coreExclusion);
    color = mix(color, voidColor, seam * 0.055);

    // Preserve a same-hue core bloom below the final display-color overlay.
    // Cyan outer energy can illuminate its surroundings but cannot tint the core.
    float core = coreMask * coreReveal * active * overshoot;
    color += CORE_COLOR * core * (0.70 + 0.45 * exp(-phase * 30.0));

    color += CORE_COLOR * exp(-dot(uv, uv) / 0.025) * exp(-phase * 28.0) * 1.7;
    color *= 0.50 + 0.50 * (1.0 - smoothstep(0.75, 1.55, length(uv)));
    // Preview tone mapping fits gl_FragColor. Remove for a linear floating HDR target.
    color = 1.0 - exp(-max(color, vec3(0.0)) * EXPOSURE);
    color = pow(max(color, vec3(0.0)), vec3(1.0));
    // Composite the center's configured display color last. This replacement,
    // rather than another additive pass, prevents outer ENERGY_COLOR and tone
    // mapping from shifting CORE_COLOR toward white or cyan during Hold.
    float coreColorCoverage = clamp(coreMask * coreReveal * active, 0.0, 1.0);
    color = mix(color, CORE_COLOR, coreColorCoverage);
    gl_FragColor = vec4(color, 1.0);
}

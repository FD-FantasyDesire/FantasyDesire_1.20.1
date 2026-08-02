// EnergyExplosive — self-contained WebGL 1 / GLSL ES 1.00 single-pass prototype.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;

// Primary/secondary are the two editable entrances for the complete effect.
const vec3 PRIMARY_COLOR = vec3(1.0, 0.9529, 0.0784);
const vec3 SECONDARY_COLOR = vec3(0.0, 0.9176, 1.0);
const float COLOR_BLEND_START = 0.18;
const float COLOR_BLEND_END = 0.78;

const float CYCLE = 1.20;
const float BLAST_RADIUS = 0.88;
const float SHOCK_SPEED = 1.38;
const float PARTICLE_SPEED = 1.26;
const float EXPOSURE = 1.28;

float sat(float x) {
    return clamp(x, 0.0, 1.0);
}

float hash11(float p) {
    return fract(sin(p * 127.1) * 43758.5453123);
}

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

// Bounded polynomial hash keeps useful fractional bits on mediump implementations.
float particleHash(float index, float channel) {
    vec2 p = fract(vec2(index * 0.1031 + channel * 0.11369 + 0.173,
                        index * 0.13787 + channel * 0.10990 + 0.317));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
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
    for (int i = 0; i < 4; ++i) {
        sum += valueNoise(p) * amplitude;
        p = mat2(1.63, 1.12, -1.12, 1.63) * p + vec2(7.7, -4.3);
        amplitude *= 0.5;
    }
    return sum;
}

// Embed the polar angle on a circle before sampling ordinary 2D FBM.  Unlike
// feeding atan() to value noise, both sides of the -PI / PI branch are now the
// same point in the noise domain, while radius and time still advect the field.
float ringFbm(float angle, float radius, float angularScale,
              float radialScale, vec2 drift) {
    vec2 angularDomain = vec2(cos(angle), sin(angle)) * angularScale;
    vec2 radialDomain = vec2(radius * radialScale,
                             radius * radialScale * 0.37);
    return fbm(angularDomain + radialDomain + drift);
}

// The color interval follows normalized radius and receives a small energy bias.
vec3 energyPalette(float normalizedRadius, float energy) {
    float coordinate = normalizedRadius + (1.0 - sat(energy)) * 0.16;
    float blend = smoothstep(COLOR_BLEND_START, COLOR_BLEND_END, coordinate);
    return mix(PRIMARY_COLOR, SECONDARY_COLOR, blend);
}

vec3 quietBackground(vec2 p, float time) {
    float haze = fbm(p * 1.7 + vec2(time * 0.018, -time * 0.013));
    vec3 color = vec3(0.0025, 0.004, 0.014);
    color += mix(PRIMARY_COLOR, SECONDARY_COLOR, haze) * haze * haze * 0.018;

    vec2 grid = floor(p * 9.0);
    vec2 cell = fract(p * 9.0) - 0.5;
    float seed = hash21(grid);
    vec2 offset = vec2(hash11(seed * 31.7), hash11(seed * 53.1)) - 0.5;
    float star = 1.0 - smoothstep(0.010, 0.032, length(cell - offset * 0.55));
    star *= smoothstep(0.91, 0.98, seed) * (0.55 + 0.45 * sin(time * 1.7 + seed * 30.0));
    color += energyPalette(seed, 0.35) * star * 0.32;
    return color * (1.0 - smoothstep(0.72, 1.70, length(p)) * 0.72);
}

float shockRing(float radius, float center, float width) {
    float safeWidth = max(width, 0.0005);
    float d = abs(radius - center);
    float rim = exp(-d * d / (safeWidth * safeWidth));
    float echo = exp(-abs(radius - center + safeWidth * 2.7) / safeWidth) * 0.24;
    return rim + echo;
}

// Curved rotating arms travel from the perimeter into the core before ignition.
float vortexCharge(vec2 p, float phase, float time, float charge) {
    float radius = max(length(p), 0.0005);
    float angle = atan(p.y, p.x);
    float gather = smoothstep(0.0, 0.14, phase);
    float outerRadius = mix(0.86, 0.08, gather);
    float radialEnvelope = exp(-pow((radius - outerRadius) / mix(0.28, 0.075, gather), 2.0));
    float spiralCoordinate = angle * 5.0 - log(radius) * 8.5 + time * 5.2 + gather * 7.0;
    float arms = pow(0.5 + 0.5 * cos(spiralCoordinate), 7.0);
    float armGlow = pow(0.5 + 0.5 * cos(spiralCoordinate + 0.52), 2.0) * 0.24;
    float outerFade = 1.0 - smoothstep(0.92, 1.30, radius);
    float corePull = exp(-radius * mix(5.0, 20.0, gather)) * gather * 0.58;
    return (arms + armGlow) * radialEnvelope * outerFade * charge + corePull * charge;
}

// A clean four-point burst: sharp horizontal/vertical blades, hot core and afterglow.
float crossStar(vec2 p, float flashAge) {
    float horizontal = exp(-abs(p.y) * 150.0) * exp(-abs(p.x) * 8.0);
    float vertical = exp(-abs(p.x) * 150.0) * exp(-abs(p.y) * 8.0);
    float innerCross = exp(-abs(p.y) * 62.0 - abs(p.x) * 17.0)
                     + exp(-abs(p.x) * 62.0 - abs(p.y) * 17.0);
    float core = exp(-dot(p, p) / 0.0018);
    float sharpFlash = exp(-flashAge * 24.0);
    float afterglow = exp(-flashAge * 7.0) * 0.32;
    return (horizontal + vertical) * sharpFlash
         + innerCross * afterglow
         + core * (sharpFlash * 2.4 + afterglow);
}

vec3 particleBurst(vec2 p, float phase, float active) {
    vec3 particles = vec3(0.0);
    for (int i = 0; i < 28; ++i) {
        float index = float(i);
        float angleNoise = particleHash(index, 1.0);
        float speedNoise = particleHash(index, 2.0);
        float delayNoise = particleHash(index, 3.0);
        float lifeNoise = particleHash(index, 4.0);
        float sizeNoise = particleHash(index, 5.0);
        float brightnessNoise = particleHash(index, 6.0);
        float dragNoise = particleHash(index, 7.0);

        float delay = mix(0.145, 0.300, delayNoise);
        float duration = mix(0.34, 0.72, lifeNoise);
        float age = sat((phase - delay) / max(duration, 0.001));
        float born = smoothstep(0.0, mix(0.025, 0.070, delayNoise), age);
        float dead = 1.0 - smoothstep(mix(0.52, 0.72, lifeNoise), 1.0, age);
        float life = born * dead * active;

        // Golden-angle coverage plus broad seeded jitter creates clusters and visible gaps.
        float angle = 0.731 + index * 2.39996323
                    + (angleNoise - 0.5) * 1.38
                    + sin(index * 1.713 + 0.41) * 0.19;
        vec2 direction = vec2(cos(angle), sin(angle));
        float speed = PARTICLE_SPEED * mix(0.38, 1.34, speedNoise * speedNoise);
        float drag = mix(0.55, 2.35, dragNoise);
        float travel = speed * (1.0 - exp(-drag * age)) / max(drag, 0.001);
        vec2 position = direction * (0.035 + travel);

        float size = mix(0.020, 0.006, age) * mix(0.58, 1.48, sizeNoise);
        float head = 1.0 - smoothstep(size * 0.25, size, length(p - position));
        float brightness = mix(0.62, 1.48, brightnessNoise);
        float light = head * 1.75 * life * brightness;
        float radialMix = sat(length(position) / max(BLAST_RADIUS, 0.001));
        particles += energyPalette(radialMix, head) * light;
    }
    return particles;
}

// A layered pseudo-sphere: noisy radius, facing falloff and a dark middle band
// give the blast a luminous front shell instead of a flat circular ring.
float supernovaShell(vec2 p, float blastAge, float layer, float time) {
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float radialNoise = ringFbm(angle, radius, 2.8 + layer * 1.4,
                                7.0 + layer * 3.0,
                                vec2(time * 0.19, -time * (0.7 + layer * 0.25)));
    float clumps = ringFbm(angle, radius, 4.2 + layer, 12.0,
                           vec2(layer * 4.0, -layer * 2.7));
    float shellRadius = mix(0.05, 0.40 + layer * 0.13, pow(blastAge, 0.50));
    shellRadius *= 1.0 + (radialNoise - 0.5) * (0.22 + layer * 0.04);
    float sphere = radius / max(shellRadius, 0.0005);
    float facing = sqrt(max(1.0 - min(sphere * sphere, 0.98), 0.02));
    float shellWidth = mix(0.16, 0.075, facing) + layer * 0.012;
    float shell = exp(-abs(sphere - (0.72 + layer * 0.11)) / max(shellWidth, 0.001));
    float body = exp(-pow(sphere / (0.84 + layer * 0.06), 2.0)) * (0.25 + facing * 0.75);
    float occlusion = smoothstep(0.10, 0.72, sphere) * (0.42 + 0.58 * facing);
    return (shell * (0.58 + clumps * 0.95) + body * 0.22) * occlusion;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float time = mod(u_time, 4096.0);
    float phase = mod(time, CYCLE) / max(CYCLE, 0.001);
    float radius = length(p);

    // Vortex charge -> detonation -> propagation -> decay; both ends return to rest.
    float charge = smoothstep(0.0, 0.035, phase) * (1.0 - smoothstep(0.125, 0.17, phase));
    float detonation = smoothstep(0.12, 0.155, phase) * (1.0 - smoothstep(0.42, 0.74, phase));
    float active = 1.0 - smoothstep(0.76, 0.98, phase);
    float blastAge = sat((phase - 0.12) / 0.62);

    vec3 color = quietBackground(p, time);

    // Rotating logarithmic arms visibly curl inward instead of radially shrinking.
    float vortex = vortexCharge(p, phase, time, charge);
    color += energyPalette(radius / max(BLAST_RADIUS, 0.001), vortex) * vortex * 1.42;

    // Layered, shaded pseudo-sphere: bright core, uneven plasma shell and rim.
    float angular = atan(p.y, p.x);
    float turbulence = ringFbm(angular, radius, 2.4, 8.0,
                               vec2(time * 0.23, -time * 1.8));
    float fireballRadius = mix(0.025, 0.34, pow(blastAge, 0.46));
    float coreSphere = exp(-dot(p, p) / max(fireballRadius * fireballRadius * 0.42, 0.00001));
    float shellNear = supernovaShell(p, blastAge, 0.0, time);
    float shellFar = supernovaShell(p, blastAge * 0.91, 1.0, time + 1.7);
    float fireball = (coreSphere * 1.8 + shellNear * 1.35 + shellFar * 0.78) * detonation;
    float flashAge = max(phase - 0.12, 0.0);
    float starFlash = crossStar(p, flashAge) * step(0.12, phase);
    vec3 blastColor = energyPalette(radius / max(fireballRadius * 1.35, 0.001), fireball);
    color += blastColor * fireball * (0.52 + 0.72 * turbulence);
    color += mix(PRIMARY_COLOR, vec3(1.0), 0.82) * coreSphere * detonation * 2.15;
    color += mix(SECONDARY_COLOR, vec3(1.0), 0.40) * shellFar * detonation * 0.82;
    color += mix(PRIMARY_COLOR, vec3(1.0), 0.76) * starFlash * 1.85;

    // Two soft, differently paced shock layers preserve depth without lightning.
    float ringCenter = SHOCK_SPEED * blastAge;
    float ringWidth = mix(0.022, 0.060, blastAge);
    float ringOne = shockRing(radius, ringCenter, ringWidth) * detonation;
    float ringTwo = shockRing(radius, ringCenter * 0.68, ringWidth * 1.85)
                  * detonation * 0.34;
    float compression = exp(-abs(radius - ringCenter * 0.72) * 18.0)
                      * (1.0 - smoothstep(0.0, max(ringCenter, 0.001), radius))
                      * detonation * 0.18;
    float ringEnergy = sat(ringOne + ringTwo);
    color += energyPalette(radius / max(BLAST_RADIUS, 0.001), ringEnergy)
           * (ringOne * 1.05 + ringTwo + compression);

    // Individually seeded particles travel on fixed rays with radial easing only.
    color += particleBurst(p, phase, active);

    // A subtle expanding wave modulates the background without texture sampling.
    float waveWake = sin((radius - ringCenter) * 46.0) * 0.5 + 0.5;
    waveWake *= exp(-abs(radius - ringCenter) * 6.0) * detonation * 0.055;
    color += energyPalette(radius / max(BLAST_RADIUS, 0.001), 0.35) * waveWake;

    color = vec3(1.0) - exp(-max(color, vec3(0.0)) * EXPOSURE);
    gl_FragColor = vec4(color, 1.0);
}

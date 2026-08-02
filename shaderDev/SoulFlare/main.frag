#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_mouse;

const vec3 WHITE_CYAN = vec3(0.82, 1.00, 1.00);
const vec3 BRIGHT_CYAN = vec3(0.08, 0.84, 1.00);
const vec3 GHOST_BLUE = vec3(0.045, 0.25, 0.95);
const vec3 DEEP_INDIGO = vec3(0.012, 0.022, 0.18);

float sat(float x) { return clamp(x, 0.0, 1.0); }
float hash11(float p) { return fract(sin(p * 127.1) * 43758.5453123); }
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + 1.0), f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += amp * noise2(p);
        p = mat2(1.61, 1.13, -1.13, 1.61) * p + vec2(7.1, 3.7);
        amp *= 0.5;
    }
    return sum;
}

float ridgedFbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.55;
    for (int i = 0; i < 4; i++) {
        float n = 1.0 - abs(noise2(p) * 2.0 - 1.0);
        sum += amp * n * n;
        p = mat2(1.72, 1.08, -1.08, 1.72) * p + vec2(4.3, 9.2);
        amp *= 0.48;
    }
    return sum;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.00001), 0.0, 1.0);
    return length(pa - ba * h);
}

/*
 * Continuous combustion density field.  There are no authored tongues:
 * a thin surface fuel band feeds several upward-advected noise scales.  The
 * height envelope and a moving threshold continuously join, pinch and split
 * whichever temporary peaks the field happens to contain.
 */
vec4 combustionField(vec2 p, float time, float pixelWidth) {
    const float baseY = -0.62;
    float y = p.y - baseY;
    float h = sat(y / 1.48);
    float boundaryAA = max(pixelWidth, 0.0005);
    float aboveSurface = smoothstep(-boundaryAA, boundaryAA, y);
    float belowCeiling = 1.0 - smoothstep(1.58 - boundaryAA,
                                          1.58 + boundaryAA, y);

    /* Slow, non-periodic middle-scale warp; the root is deliberately locked. */
    vec2 slowDomain = vec2(p.x * 2.9, y * 2.25 - time * 0.24);
    float slowA = fbm(slowDomain + vec2(0.0, time * 0.031));
    float slowB = fbm(slowDomain.yx * vec2(0.82, 1.17) + vec2(13.6, -time * 0.027));
    float rootLock = smoothstep(0.025, 0.27, y);
    float warpX = (slowA - 0.5) * (0.10 + 0.13 * h) * rootLock;
    warpX += (slowB - 0.5) * 0.075 * h * h;
    vec2 q = vec2(p.x + warpX, y);

    /* The broad bed narrows statistically, not as a smooth cone silhouette. */
    float edgeNoise = fbm(vec2(q.x * 4.0 + 5.2, y * 3.25 - time * 0.39));
    float halfWidth = mix(0.535, 0.055, pow(h, 0.72));
    halfWidth *= 0.80 + 0.35 * edgeNoise;
    float lateralFuel = 1.0 - abs(q.x) / max(halfWidth, 0.025);

    /* Three crossing transport rates create rolling cells rather than bands. */
    vec2 adv0 = vec2(q.x * 4.8, y * 4.0 - time * 0.82);
    vec2 warp = vec2(fbm(adv0 + vec2(8.1, 1.7)),
                     fbm(adv0 * vec2(0.83, 1.18) + vec2(-3.4, 12.2)));
    warp = (warp - 0.5) * vec2(1.05, 0.72);
    float medium = fbm(adv0 + warp);
    float columns = ridgedFbm(vec2(q.x * 8.2 + warp.x * 1.4,
                                  y * 5.7 - time * 1.27 + warp.y));
    float fine = fbm(vec2(q.x * 15.5 - warp.y * 2.0,
                          y * 11.8 - time * 2.65 + warp.x * 1.2));
    float flicker = noise2(vec2(q.x * 28.0 + slowB * 4.0,
                                y * 18.0 - time * 4.15));

    /* High regions require increasingly rare coincidences, yielding a
       changing count of sharp tips, necks, detached wisps and extinction. */
    float turbulence = 0.43 * medium + 0.35 * columns + 0.22 * fine;
    float threshold = 0.34 + 0.22 * h + 0.16 * h * h;
    threshold += (noise2(vec2(y * 3.7 - time * 0.31, 21.4)) - 0.5)
               * 0.16 * smoothstep(0.35, 1.0, h);
    float density = lateralFuel * (0.82 - 0.27 * h)
                  + turbulence * (0.78 + 0.38 * h)
                  + (flicker - 0.5) * mix(0.10, 0.32, h)
                  - threshold;

    /* Wide, thin and irregular energy injection at y ~= -0.62. */
    float rootWidth = 1.0 - smoothstep(0.43, 0.535,
        abs(p.x) + (noise2(vec2(p.x * 19.0, time * 1.8)) - 0.5) * 0.025);
    float rootHeight = 1.0 - smoothstep(0.012, 0.105, y);
    float rootFlicker = 0.78 + 0.22 * noise2(vec2(p.x * 31.0, time * 5.2));
    float rootDensity = rootWidth * rootHeight * rootFlicker;
    density = max(density, rootDensity * 0.82 - 0.16);

    /* Outside the combustion volume must remain strictly negative: multiplying
       by zero would sit exactly on the threshold and create half-alpha. */
    density = mix(-1.0, density, aboveSurface * belowCeiling);
    return vec4(density, turbulence, fine, h);
}

/* Advected low-density cells carve temporary rolling gaps inside the fuel. */
float darkGaps(vec2 p, float time, float h) {
    float rootSuppression = smoothstep(-0.54, -0.24, p.y);
    vec2 d = vec2(p.x * 7.1, (p.y + 0.62) * 6.0 - time * 1.34);
    vec2 w = vec2(fbm(d * 0.61 + 9.0), fbm(d * 0.73 - 4.0)) - 0.5;
    float cells = fbm(d + w * 1.7);
    float cuts = smoothstep(0.67, 0.82, cells);
    float fastCuts = smoothstep(0.73, 0.90,
        ridgedFbm(vec2(p.x * 12.0 + w.y, p.y * 9.0 - time * 2.25)));
    return sat(cuts * 0.82 + fastCuts * 0.34) * rootSuppression * (0.45 + 0.55 * h);
}

float sixPointStar(vec2 p, float r) {
    vec2 q = abs(p);
    float body = 1.0 - smoothstep(r * 0.16, r * 0.42, length(p));
    float v = exp(-q.x * 120.0 / max(r, 0.001)) * exp(-q.y * 6.0 / max(r, 0.001));
    vec2 a = vec2(0.8660254, 0.5);
    vec2 b = vec2(0.8660254, -0.5);
    float r1 = exp(-abs(dot(p, vec2(-a.y, a.x))) * 120.0 / max(r, 0.001))
             * exp(-abs(dot(p, a)) * 6.0 / max(r, 0.001));
    float r2 = exp(-abs(dot(p, vec2(-b.y, b.x))) * 120.0 / max(r, 0.001))
             * exp(-abs(dot(p, b)) * 6.0 / max(r, 0.001));
    return max(body, max(v, max(r1, r2)));
}

vec4 sparks(vec2 p, float time) {
    vec3 color = vec3(0.0);
    float alpha = 0.0;
    for (int i = 0; i < 8; i++) {
        float seed = float(i) * 27.13 + 4.7;
        float life = mix(0.72, 1.45, hash11(seed + 1.0));
        float cycle = floor(time / life + hash11(seed + 2.0));
        float age = fract(time / life + hash11(seed + 2.0));
        float randomCycle = seed + cycle * 17.31;
        vec2 origin = vec2(mix(-0.42, 0.42, hash11(randomCycle)),
                           mix(-0.48, 0.34, hash11(randomCycle + 3.0)));
        vec2 velocity = vec2((hash11(randomCycle + 5.0) - 0.5) * 0.24,
                             mix(0.28, 0.58, hash11(randomCycle + 7.0)));
        vec2 position = origin + velocity * age;
        position.x += (noise2(vec2(randomCycle, age * 3.0)) - 0.5) * 0.09 * age;
        float fade = smoothstep(0.0, 0.08, age) * (1.0 - smoothstep(0.48, 1.0, age));
        float star = sixPointStar(p - position, mix(0.012, 0.006, age));
        float tail = exp(-segmentDistance(p, position, position - velocity * 0.075) * 180.0)
                   * (1.0 - smoothstep(0.0, 0.075, length(p - position)));
        float light = fade * (star * 1.25 + tail * 0.18);
        color += mix(BRIGHT_CYAN, WHITE_CYAN, star) * light;
        alpha = max(alpha, sat(light));
    }
    return vec4(color, alpha);
}

vec4 soulFragments(vec2 p, float time) {
    vec3 color = vec3(0.0);
    float alpha = 0.0;
    for (int i = 0; i < 4; i++) {
        float seed = float(i) * 39.7 + 2.1;
        float age = fract(time * mix(0.24, 0.39, hash11(seed)) + hash11(seed + 1.0));
        vec2 origin = vec2(mix(-0.39, 0.39, hash11(seed + floor(time * 0.3))),
                           mix(-0.28, 0.52, hash11(seed + 4.0)));
        vec2 position = origin + vec2((hash11(seed + 5.0) - 0.5) * 0.16,
                                      0.22 + hash11(seed + 6.0) * 0.18) * age;
        vec2 q = p - position;
        float shard = 1.0 - smoothstep(0.75, 1.0,
            abs(q.x) / 0.010 + abs(q.y) / 0.027);
        shard *= smoothstep(0.0, 0.08, age) * (1.0 - smoothstep(0.36, 0.82, age));
        color += GHOST_BLUE * shard * 0.62;
        alpha = max(alpha, shard * 0.66);
    }
    return vec4(color, alpha);
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float time = mod(u_time, 4096.0);
    vec2 mouse = vec2(0.0);
    if (dot(u_mouse, u_mouse) > 0.0) {
        mouse = (2.0 * u_mouse - resolution) / shortSide;
    }
    p.x -= mouse.x * 0.025;

    float aa = 2.5 / shortSide;
    vec4 field = combustionField(p, time, aa);
    float density = field.x;
    float body = smoothstep(-aa, aa, density);
    float gaps = darkGaps(p, time, field.w) * body;
    float visible = body * (1.0 - gaps * 0.88);

    /* Density, not authored nested shapes, selects every combustion colour. */
    float low = smoothstep(-0.015, 0.075, density);
    float middle = smoothstep(0.075, 0.31, density);
    float high = smoothstep(0.30, 0.61, density);
    float thinEdge = body * (1.0 - smoothstep(0.0, 0.085, density));
    vec3 flameColor = mix(DEEP_INDIGO, GHOST_BLUE, low);
    flameColor = mix(flameColor, BRIGHT_CYAN, middle);
    flameColor = mix(flameColor, WHITE_CYAN, high);
    flameColor *= visible * (0.82 + 0.38 * field.y + 0.16 * field.z);
    flameColor += DEEP_INDIGO * thinEdge * 1.18;
    flameColor *= 1.0 - gaps * 0.72;

    /* A tight exterior response follows threshold expansion and extinction. */
    float outerGlow = exp(-max(-density, 0.0) * 34.0) * (1.0 - body)
                    * smoothstep(-0.62 - aa, -0.62 + aa, p.y)
                    * (1.0 - smoothstep(0.98 - aa, 0.98 + aa, p.y));
    flameColor += mix(GHOST_BLUE, BRIGHT_CYAN, field.y) * outerGlow * 0.16;

    /* Extremely weak edge-only spectral displacement. */
    float spectral = thinEdge * smoothstep(0.34, 0.88, field.w);
    flameColor += vec3(0.010, 0.0, 0.018) * spectral;

    vec4 fragmentLayer = soulFragments(p, time);
    vec4 sparkLayer = sparks(p, time);
    vec3 color = flameColor + fragmentLayer.rgb + sparkLayer.rgb;
    float alpha = max(visible, max(outerGlow * 0.20,
                      max(fragmentLayer.a, sparkLayer.a)));
    color = vec3(1.0) - exp(-color * (1.0 + mouse.y * 0.035));
    gl_FragColor = vec4(color, sat(alpha));
}

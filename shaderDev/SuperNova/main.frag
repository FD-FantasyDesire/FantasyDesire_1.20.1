// SuperNova - WebGL 1 / GLSL ES 1.00 单 pass 球域体积爆炸原型，无贴图。
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;

const vec3 CORE_COLOR = vec3(1.00, 0.93, 0.79);
const vec3 SHELL_BRIGHT = vec3(0.66, 0.55, 1.00);
const vec3 SHELL_MID = vec3(0.25, 0.17, 0.55);
const vec3 SHELL_DARK = vec3(0.035, 0.035, 0.13);
const vec3 CYAN_STREAM = vec3(0.05, 0.94, 0.88);
const vec3 GOLD_STREAM = vec3(1.00, 0.67, 0.05);
const vec3 MINT_STREAM = vec3(0.52, 1.00, 0.24);
const vec3 PINK_STREAM = vec3(0.92, 0.43, 0.62);
const vec3 RED_STREAM = vec3(1.00, 0.24, 0.08);
const vec3 SPOKE_COLOR = vec3(0.18, 0.39, 1.00);

const float CYCLE = 1.40;
const float SPHERE_RADIUS = 0.88;
const float EXPOSURE = 1.32;
const float PI = 3.14159265359;
const int VOLUME_STEPS = 18;
const int FLOW_TRACE_SEGMENTS = 32;
const int SPOKE_COUNT = 11;

float sat(float value) {
    return clamp(value, 0.0, 1.0);
}

float hash11(float value) {
    value = fract(value * 0.1031);
    value *= value + 33.33;
    value *= value + value;
    return fract(value);
}

float hash21(vec2 value) {
    value = fract(value * vec2(123.34, 456.21));
    value += dot(value, value + 45.32);
    return fract(value.x * value.y);
}

float valueNoise(vec2 position) {
    vec2 cell = floor(position);
    vec2 local = fract(position);
    local = local * local * (3.0 - 2.0 * local);

    float lowerLeft = hash21(cell);
    float lowerRight = hash21(cell + vec2(1.0, 0.0));
    float upperLeft = hash21(cell + vec2(0.0, 1.0));
    float upperRight = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(lowerLeft, lowerRight, local.x),
               mix(upperLeft, upperRight, local.x), local.y);
}

float fbm(vec2 position) {
    float result = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; ++octave) {
        result += valueNoise(position) * amplitude;
        position = mat2(1.63, 1.12, -1.12, 1.63) * position
                 + vec2(7.7, -4.3);
        amplitude *= 0.5;
    }
    return result;
}

vec2 rotate2D(vec2 position, float angle) {
    float cosine = cos(angle);
    float sine = sin(angle);
    return mat2(cosine, -sine, sine, cosine) * position;
}

float softRing(float radius, float center, float width) {
    float safeWidth = max(width, 0.0005);
    float distanceToRing = (radius - center) / safeWidth;
    return exp(-distanceToRing * distanceToRing);
}

float segmentDistance(vec2 position, vec2 start, vec2 end) {
    vec2 segment = end - start;
    float projection = clamp(dot(position - start, segment)
                     / max(dot(segment, segment), 0.00001), 0.0, 1.0);
    return length(position - (start + segment * projection));
}

// 极角先映射到连续圆域，避免噪声在 -PI/PI 处产生接缝。
float ringNoise(float angle, float radius, float angularScale,
                float radialScale, vec2 drift) {
    vec2 angularDomain = vec2(cos(angle), sin(angle)) * angularScale;
    vec2 radialDomain = vec2(radius * radialScale,
                             radius * radialScale * 0.41);
    return fbm(angularDomain + radialDomain + drift);
}

// 每条流线从自己的固定颜色点出发，路径长度不依赖其它流线的半径。
vec2 flowPoint(vec2 source, float progress, float bend,
               float curl, float seed) {
    float t = sat(progress);
    vec2 radial = normalize(source);
    vec2 tangent = vec2(-radial.y, radial.x);
    float variation = mix(0.96, 1.04, hash11(seed * 5.31));
    float clockwiseBend = -abs(bend) * variation;
    float clockwiseCurl = -abs(curl) * (2.0 - variation);
    vec2 controlOne = source * 0.70 + tangent * clockwiseBend;
    vec2 controlTwo = source * 0.24 + tangent * clockwiseCurl;
    float inverse = 1.0 - t;
    return source * (inverse * inverse * inverse)
         + controlOne * (3.0 * inverse * inverse * t)
         + controlTwo * (3.0 * inverse * t * t);
}

// x=历史沉积尾迹，y=移动粒子头，z=粒子抵达中心时的撞击脉冲。
vec3 particleTrace(vec2 position, vec2 source, float bend,
                   float curl, float width, float startDelay,
                   float duration, float seed, float phase) {
    float rawAge = (phase - startDelay) / max(duration, 0.001);
    float particleProgress = pow(sat(rawAge), 0.84);
    float particleLife = smoothstep(0.0, 0.045, rawAge)
                       * (1.0 - smoothstep(0.94, 1.04, rawAge));
    vec2 particlePosition = flowPoint(source, particleProgress,
                                      bend, curl, seed);
    float particleDistanceSquared = dot(position - particlePosition,
                                        position - particlePosition);
    float particleCore = exp(-particleDistanceSquared
                             / max(width * width * 1.35, 0.000001));
    float particleHalo = exp(-particleDistanceSquared
                             / max(width * width * 18.0, 0.000001));
    float particle = (particleCore * 2.35 + particleHalo * 0.78) * particleLife;

    float impactPhase = startDelay + duration;
    float traceClear = 1.0 - smoothstep(impactPhase + 0.035,
                                       impactPhase + 0.155, phase);
    traceClear *= 1.0 - smoothstep(0.47, 0.515, phase);
    float trace = 0.0;

    // 每一段只在粒子真实经过后沉积，并保留在固定空间位置独立衰减。
    for (int segmentIndex = 0; segmentIndex < FLOW_TRACE_SEGMENTS; ++segmentIndex) {
        float index = float(segmentIndex);
        float t0 = index / float(FLOW_TRACE_SEGMENTS);
        float t1 = (index + 1.0) / float(FLOW_TRACE_SEGMENTS);
        float midpoint = (t0 + t1) * 0.5;
        vec2 segmentStart = flowPoint(source, t0, bend, curl, seed);
        vec2 segmentEnd = flowPoint(source, t1, bend, curl, seed);
        float distanceToTrace = segmentDistance(position, segmentStart, segmentEnd);
        float traceShape = exp(-distanceToTrace * distanceToTrace
                               / max(width * width, 0.000001));
        float passageAge = pow(midpoint, 1.0 / 0.84);
        float passagePhase = startDelay + duration * passageAge;
        float timeSincePassage = phase - passagePhase;
        float deposited = smoothstep(-0.010, 0.014, timeSincePassage);
        float localAfterglow = mix(0.34, 1.0,
            exp(-max(timeSincePassage, 0.0) / 0.14));
        float segmentTrace = traceShape * deposited * localAfterglow * traceClear;
        trace = max(trace, segmentTrace);
    }

    float impactAge = phase - impactPhase;
    float impact = smoothstep(0.0, 0.012, impactAge)
                 * (1.0 - smoothstep(0.026, 0.075, impactAge));
    return vec3(trace, particle, impact);
}

vec3 particleTracePair(vec2 position, vec2 source, float bend, float curl,
                       float width, float startDelay, float duration,
                       float seed, float phase) {
    return particleTrace(position, source, bend, curl, width, startDelay,
                         duration, seed, phase)
         + particleTrace(position, -source, bend, curl, width, startDelay,
                         duration, seed, phase);
}

float crossSpark(vec2 position, float size) {
    float safeSize = max(size, 0.0005);
    float horizontal = exp(-abs(position.y) / (safeSize * 0.18))
                     * exp(-abs(position.x) / safeSize);
    float vertical = exp(-abs(position.x) / (safeSize * 0.18))
                   * exp(-abs(position.y) / safeSize);
    return horizontal + vertical;
}

// 由三组不同频率的三角波域组成低成本三维流噪声，供固定步数体积积分使用。
float waveNoise3D(vec3 position) {
    position += sin(position.yzx * 1.17 + position.zxy * 0.73) * 0.34;
    float wave = sin(position.x)
               + sin(position.y * 1.13 + 1.7)
               + sin(position.z * 0.91 - 2.4);
    return 0.5 + wave / 6.0;
}

float volumeTurbulence(vec3 position, float time) {
    vec3 drift = vec3(time * 0.31, -time * 0.27, time * 0.19);
    float broad = waveNoise3D(position * 3.1 + drift);
    float middle = waveNoise3D(position.yzx * 6.3 - drift * 1.7);
    float fine = waveNoise3D(position.zxy * 12.7 + drift * 2.3);
    return broad * 0.54 + middle * 0.31 + fine * 0.15;
}

// 返回 RGB 自发光和 A 密度；所有结构都在单位球坐标中定义。
vec4 plasmaSample(vec3 position, float shellRadius, float shellWidth,
                  float compactness, float cavityRadius,
                  float coreLife, float volumeLife, float time) {
    float radius = length(position);
    float turbulence = volumeTurbulence(position, time);
    float radialWarp = (turbulence - 0.5) * mix(0.075, 0.19, compactness);
    float shellCoordinate = (abs(radius + radialWarp - shellRadius)
                           / max(shellWidth, 0.001));
    float shell = exp(-shellCoordinate * shellCoordinate);
    float clumps = smoothstep(0.44, 0.67, turbulence);
    float fissures = smoothstep(0.43, 0.64,
        volumeTurbulence(position * 1.61 + vec3(2.3, -1.7, 0.8), -time * 0.63));

    float normalizedBody = radius / max(shellRadius, 0.001);
    float body = exp(-pow(normalizedBody, 4.0)) * (1.0 - compactness);
    float cavity = smoothstep(cavityRadius - 0.035,
                              cavityRadius + 0.045, radius);
    float core = exp(-radius * radius / 0.0048) * coreLife;

    float density = (shell * (0.10 + clumps * 1.72) * (0.16 + fissures * 1.18)
                    + body * (0.42 + clumps * 0.92)) * cavity;
    density = density * volumeLife + core * 1.8;

    float frontLight = sat(position.z * 0.52 + 0.52);
    vec3 plasmaColor = mix(SHELL_DARK, SHELL_MID,
                           sat(turbulence * 1.18 + frontLight * 0.18));
    plasmaColor = mix(plasmaColor, SHELL_BRIGHT,
                      sat(shell * clumps * 0.42 + frontLight * 0.14));
    vec3 emission = plasmaColor * density * (0.58 + clumps * 0.88);
    emission += CORE_COLOR * core * 3.4;
    return vec4(emission, density);
}

// 从球体近侧向远侧积分，密度本身同时决定遮挡和透明度。
vec4 integrateSphere(vec2 spherePosition, float shellRadius,
                     float shellWidth, float compactness,
                     float cavityRadius, float coreLife,
                     float volumeLife, float time) {
    float projectedRadiusSquared = dot(spherePosition, spherePosition);
    float halfDepth = sqrt(max(1.0 - projectedRadiusSquared, 0.0));
    float stepLength = halfDepth * 2.0 / float(VOLUME_STEPS);
    vec3 accumulatedLight = vec3(0.0);
    float accumulatedOpacity = 0.0;

    for (int stepIndex = 0; stepIndex < VOLUME_STEPS; ++stepIndex) {
        float stepFraction = (float(stepIndex) + 0.5) / float(VOLUME_STEPS);
        float depth = mix(halfDepth, -halfDepth, stepFraction);
        vec3 samplePosition = vec3(spherePosition, depth);
        vec4 plasma = plasmaSample(samplePosition, shellRadius, shellWidth,
                                   compactness, cavityRadius, coreLife,
                                   volumeLife, time);
        float sampleOpacity = 1.0 - exp(-plasma.a * stepLength * 3.6);
        float transmittance = 1.0 - accumulatedOpacity;
        accumulatedLight += transmittance * plasma.rgb * stepLength * 2.8;
        accumulatedOpacity += transmittance * sampleOpacity;
    }

    return vec4(accumulatedLight, accumulatedOpacity);
}

float ignitionStar(vec2 position, float life) {
    float horizontal = exp(-abs(position.y) * 145.0)
                     * exp(-abs(position.x) * 9.0);
    float vertical = exp(-abs(position.x) * 145.0)
                   * exp(-abs(position.y) * 9.0);
    vec2 diagonalPosition = rotate2D(position, PI * 0.25);
    float diagonal = (exp(-abs(diagonalPosition.y) * 108.0)
                    * exp(-abs(diagonalPosition.x) * 12.0)
                    + exp(-abs(diagonalPosition.x) * 108.0)
                    * exp(-abs(diagonalPosition.y) * 12.0)) * 0.58;
    float core = exp(-dot(position, position) / 0.0011) * 2.2;
    return (horizontal + vertical + diagonal + core) * life;
}

vec4 surfaceSpokes(vec2 position, float shellRadius,
                   float active, float time) {
    vec3 light = vec3(0.0);
    float opacity = 0.0;
    for (int spokeIndex = 0; spokeIndex < SPOKE_COUNT; ++spokeIndex) {
        float index = float(spokeIndex);
        float seed = hash11(index + 4.7);
        float angle = index * 2.399963 + seed * 1.73 + time * 0.08;
        vec2 direction = vec2(cos(angle), sin(angle));
        float spokeLength = mix(0.035, 0.12, hash11(index + 11.3));
        vec2 start = direction * max(shellRadius * 0.93, 0.03);
        vec2 end = direction * min(shellRadius + spokeLength, 0.995);
        float width = mix(0.0025, 0.0060, hash11(index + 19.1));
        float distanceToSpoke = segmentDistance(position, start, end);
        float spoke = exp(-distanceToSpoke * distanceToSpoke
                          / max(width * width, 0.000001));
        spoke *= active * mix(0.38, 1.0, seed);
        light += SPOKE_COLOR * spoke * 0.82;
        opacity = max(opacity, spoke * 0.55);
    }
    return vec4(light, opacity);
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 position = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    vec2 spherePosition = position / max(SPHERE_RADIUS, 0.001);
    float sphereRadius = length(spherePosition);
    float sphereMask = 1.0 - smoothstep(0.985, 1.005, sphereRadius);

    float time = mod(u_time, 4096.0);
    float phase = mod(time, CYCLE) / CYCLE;
    vec3 light = vec3(0.0);
    float opacity = 0.0;

    // 0%-50%：每种颜色从自己的固定外部点生成，独立沿曲线向中心收束。
    vec2 mintSource = vec2(0.0, 0.92);
    vec2 pinkSource = vec2(0.72, 0.68);
    vec2 goldSource = vec2(-0.76, 0.48);
    vec2 cyanSource = vec2(-0.82, -0.18);
    vec2 redSource = vec2(0.90, -0.22);

    float pointLife = smoothstep(0.015, 0.055, phase)
                    * (1.0 - smoothstep(0.11, 0.23, phase));
    float mintPoints = crossSpark(spherePosition - mintSource, 0.032)
                     + crossSpark(spherePosition + mintSource, 0.032);
    float pinkPoints = crossSpark(spherePosition - pinkSource, 0.029)
                     + crossSpark(spherePosition + pinkSource, 0.029);
    float goldPoints = crossSpark(spherePosition - goldSource, 0.034)
                     + crossSpark(spherePosition + goldSource, 0.034);
    float cyanPoints = crossSpark(spherePosition - cyanSource, 0.027)
                     + crossSpark(spherePosition + cyanSource, 0.027);
    float redPoints = crossSpark(spherePosition - redSource, 0.020)
                    + crossSpark(spherePosition + redSource, 0.020);

    vec3 mint = particleTracePair(spherePosition, mintSource, 0.07, 0.07, 0.010,
                                  0.105, 0.265, 1.4, phase);
    vec3 pink = particleTracePair(spherePosition, pinkSource, 0.05, 0.05, 0.008,
                                  0.120, 0.285, 3.1, phase);
    vec3 gold = particleTracePair(spherePosition, goldSource, 0.34, 0.22, 0.013,
                                  0.090, 0.245, 5.8, phase);
    vec3 cyan = particleTracePair(spherePosition, cyanSource, 0.32, 0.24, 0.011,
                                  0.135, 0.300, 7.2, phase);
    vec3 red = particleTracePair(spherePosition, redSource, 0.22, 0.38, 0.005,
                                 0.145, 0.275, 9.6, phase);

    light += MINT_STREAM * (mint.x * 0.74 + mint.y * 1.82
                          + mintPoints * pointLife * 1.05);
    light += PINK_STREAM * (pink.x * 0.66 + pink.y * 1.72
                          + pinkPoints * pointLife * 0.92);
    light += GOLD_STREAM * (gold.x * 0.82 + gold.y * 1.88
                          + goldPoints * pointLife * 1.10);
    light += CYAN_STREAM * (cyan.x * 0.78 + cyan.y * 1.84
                          + cyanPoints * pointLife * 0.88);
    light += RED_STREAM * (red.x * 0.48 + red.y * 1.36
                         + redPoints * pointLife * 0.70);

    float impactCore = exp(-dot(spherePosition, spherePosition) / 0.0018);
    vec3 impactLight = MINT_STREAM * mint.z
                     + PINK_STREAM * pink.z
                     + GOLD_STREAM * gold.z
                     + CYAN_STREAM * cyan.z
                     + RED_STREAM * red.z;
    float impactEnergy = mint.z + pink.z + gold.z + cyan.z + red.z;
    light += mix(impactLight, CORE_COLOR * impactEnergy, 0.66)
           * impactCore * 2.1;

    float traceEnergy = mint.x + pink.x + gold.x + cyan.x + red.x;
    float particleEnergy = mint.y + pink.y + gold.y + cyan.y + red.y;
    light += CORE_COLOR * particleEnergy * 0.92;
    opacity = max(opacity, sat((traceEnergy * 0.54 + particleEnergy * 0.88)
                              * sphereMask));
    opacity = max(opacity, sat(impactCore * impactEnergy * 0.78));
    opacity = max(opacity, sat((mintPoints + pinkPoints + goldPoints
                              + cyanPoints + redPoints) * pointLife
                              * 0.58 * sphereMask));

    float seedLife = 1.0 - smoothstep(0.47, 0.535, phase);
    float seedCore = exp(-dot(spherePosition, spherePosition) / 0.0022) * seedLife;
    float seedStar = ignitionStar(spherePosition,
        (1.0 - smoothstep(0.34, 0.50, phase)) * 0.24);
    light += mix(CYAN_STREAM, CORE_COLOR, 0.76) * (seedCore * 1.9 + seedStar);
    opacity = max(opacity, sat(seedCore + seedStar * 0.54));

    // 52%-100%：爆心在固定球域内点火，体积壳急剧膨胀后掏空并衰减。
    float volumeLife = smoothstep(0.515, 0.548, phase)
                     * (1.0 - smoothstep(0.855, 0.995, phase));
    float expansionKick = smoothstep(0.55, 0.605, phase);
    float expansionDrift = smoothstep(0.605, 0.87, phase);
    float shellRadius3D = mix(0.17, 0.73, expansionKick)
                        + expansionDrift * 0.23;
    float shellWidth = mix(0.15, 0.085, expansionKick)
                     + expansionDrift * 0.025;
    float cavityGrowth = smoothstep(0.56, 0.76, phase);
    float cavityRadius = shellRadius3D * mix(0.0, 0.67, cavityGrowth);
    float coreLife = smoothstep(0.515, 0.548, phase)
                   * (1.0 - smoothstep(0.58, 0.69, phase));

    if (sphereRadius < 1.005 && volumeLife + coreLife > 0.0001) {
        vec4 volume = integrateSphere(spherePosition, shellRadius3D,
                                      shellWidth, expansionKick,
                                      cavityRadius, coreLife,
                                      volumeLife, time);
        // 球壳径向空腔本身仍会投影到视线中央；锥形疏散区让爆心沿视轴破开。
        float openingRadius = shellRadius3D * mix(0.08, 0.58, cavityGrowth);
        float opening = smoothstep(openingRadius - 0.075,
                                   openingRadius + 0.055, sphereRadius);
        float centerTransmission = mix(1.0, 0.14 + opening * 0.86,
                                       cavityGrowth * 0.88);
        volume.rgb *= centerTransmission;
        volume.a *= mix(1.0, 0.24 + opening * 0.76, cavityGrowth * 0.82);
        light += volume.rgb * sphereMask;
        opacity = max(opacity, volume.a * sphereMask);
    }

    float ignitionLife = smoothstep(0.515, 0.545, phase)
                       * (1.0 - smoothstep(0.59, 0.66, phase));
    float star = ignitionStar(spherePosition, ignitionLife);
    light += CORE_COLOR * star * 1.62 * sphereMask;
    opacity = max(opacity, sat(star * 0.72 * sphereMask));

    // 球壳切线方向自然增亮，替代与体积脱节的二维花瓣圆环。
    float angle = atan(spherePosition.y, spherePosition.x);
    float rimNoise = ringNoise(angle, sphereRadius, 4.3, 8.0,
                               vec2(time * 0.12, -time * 0.28));
    float rimRadius = shellRadius3D + (rimNoise - 0.5)
                    * mix(0.018, 0.035, expansionKick);
    float rimBreak = ringNoise(angle, sphereRadius, 7.1, 14.0,
                               vec2(-time * 0.21, time * 0.17));
    float rim = softRing(sphereRadius, rimRadius,
                         mix(0.014, 0.025, expansionKick));
    rim *= (0.28 + rimNoise * 0.48 + smoothstep(0.48, 0.68, rimBreak) * 0.46)
         * volumeLife;
    vec3 rimColor = mix(CORE_COLOR, SHELL_BRIGHT,
                        smoothstep(0.58, 0.76, phase));
    light += rimColor * rim * mix(1.45, 0.58, expansionDrift) * sphereMask;
    opacity = max(opacity, sat(rim * 0.58 * sphereMask));

    float spokeLife = smoothstep(0.56, 0.60, phase)
                    * (1.0 - smoothstep(0.72, 0.86, phase));
    vec4 spokes = surfaceSpokes(spherePosition, shellRadius3D,
                                spokeLife, time);
    light += spokes.rgb * sphereMask;
    opacity = max(opacity, spokes.a * sphereMask);

    // 固定边界只保留极弱的受激辉光，用来确认爆炸始终位于球体空间内。
    float boundary = softRing(sphereRadius, 0.987, 0.010)
                   * volumeLife * smoothstep(0.61, 0.78, phase) * 0.16;
    light += SHELL_MID * boundary;
    opacity = max(opacity, boundary * 0.42);

    light = vec3(1.0) - exp(-max(light, vec3(0.0)) * EXPOSURE);
    gl_FragColor = vec4(light, sat(opacity));
}

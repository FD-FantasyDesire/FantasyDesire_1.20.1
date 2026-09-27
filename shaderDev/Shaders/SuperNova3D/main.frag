// SuperNova - WebGL 1 / GLSL ES 1.00 单 pass 三维风格化星体爆炸原型。
// 画面内部按未来迁移边界模拟多层合成：测试平面、爆闪、体积球壳、宇宙残留和裂痕。
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

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float CYCLE = 8.0;
const float CAMERA_DISTANCE = 7.4;
const float EXPLOSION_HEIGHT = 1.18;
const float MAX_BURST_RADIUS = 3.15;
const float EXPOSURE = 1.28;
const int VOLUME_STEPS = 14;
const int CRACK_COUNT = 7;
const int CRACK_SEGMENTS = 7;

// 修改这两个常量即可测试地面、斜坡或墙面。法线在运行时归一化。
const vec3 TEST_PLANE_POINT = vec3(0.0, 0.0, 0.0);
const vec3 TEST_PLANE_NORMAL = vec3(0.0, 1.0, 0.0);

const vec3 CORE_COLOR = vec3(1.00, 0.94, 0.76);
const vec3 HOT_COLOR = vec3(0.76, 0.96, 1.00);
const vec3 CYAN_COLOR = vec3(0.06, 0.82, 1.00);
const vec3 VIOLET_COLOR = vec3(0.48, 0.16, 0.92);
const vec3 MAGENTA_COLOR = vec3(0.92, 0.20, 0.58);
const vec3 VOID_COLOR = vec3(0.002, 0.003, 0.014);

float sat(float value) {
    return clamp(value, 0.0, 1.0);
}

float easeOutCubic(float value) {
    float inverse = 1.0 - sat(value);
    return 1.0 - inverse * inverse * inverse;
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

float hash31(vec3 value) {
    value = fract(value * 0.1031);
    value += dot(value, value.yzx + 33.33);
    return fract((value.x + value.y) * value.z);
}

vec2 hash22(vec2 value) {
    return vec2(hash21(value + vec2(17.17, 3.11)),
                hash21(value + vec2(43.71, 19.37)));
}

float valueNoise2(vec2 position) {
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

float fbm2(vec2 position) {
    float value = 0.0;
    float amplitude = 0.52;
    for (int octave = 0; octave < 4; ++octave) {
        value += valueNoise2(position) * amplitude;
        position = mat2(1.63, 1.12, -1.12, 1.63) * position
                 + vec2(7.7, -4.3);
        amplitude *= 0.48;
    }
    return value / 0.951;
}

// 三组三角波域组成低成本三维流噪声，避免体积积分内执行昂贵的 3D value noise。
float waveNoise3(vec3 position) {
    position += sin(position.yzx * 1.17 + position.zxy * 0.73) * 0.34;
    float wave = sin(position.x)
               + sin(position.y * 1.13 + 1.7)
               + sin(position.z * 0.91 - 2.4);
    return 0.5 + wave / 6.0;
}

float flowNoise3(vec3 position, float time) {
    vec3 drift = vec3(time * 0.24, -time * 0.19, time * 0.15);
    float broad = waveNoise3(position * 2.7 + drift);
    float middle = waveNoise3(position.yzx * 5.6 - drift * 1.7);
    float fine = waveNoise3(position.zxy * 11.4 + drift * 2.2);
    return broad * 0.54 + middle * 0.31 + fine * 0.15;
}

float wrappedAngle(float angle) {
    return atan(sin(angle), cos(angle));
}

float segmentDistance(vec2 position, vec2 start, vec2 end) {
    vec2 segment = end - start;
    float projection = clamp(dot(position - start, segment)
                     / max(dot(segment, segment), 0.00001), 0.0, 1.0);
    return length(position - (start + segment * projection));
}

void planeBasis(vec3 normal, out vec3 tangent, out vec3 bitangent) {
    if (abs(normal.y) > 0.92) {
        tangent = vec3(1.0, 0.0, 0.0);
    } else {
        tangent = normalize(cross(vec3(0.0, 1.0, 0.0), normal));
    }
    bitangent = normalize(cross(normal, tangent));
}

float rayPlane(vec3 rayOrigin, vec3 rayDirection, vec3 planePoint, vec3 planeNormal) {
    float denominator = dot(rayDirection, planeNormal);
    if (abs(denominator) < 0.0001) {
        return -1.0;
    }
    float distanceAlongRay = dot(planePoint - rayOrigin, planeNormal) / denominator;
    return distanceAlongRay > 0.0 ? distanceAlongRay : -1.0;
}

vec2 raySphere(vec3 rayOrigin, vec3 rayDirection, vec3 center, float radius) {
    vec3 offset = rayOrigin - center;
    float projected = dot(offset, rayDirection);
    float constantTerm = dot(offset, offset) - radius * radius;
    float discriminant = projected * projected - constantTerm;
    if (discriminant < 0.0) {
        return vec2(-1.0);
    }
    float root = sqrt(discriminant);
    return vec2(-projected - root, -projected + root);
}

void buildCamera(vec2 canvasPosition, vec2 resolution,
                 out vec3 rayOrigin, out vec3 rayDirection,
                 out vec3 cameraForward, out vec3 cameraRight, out vec3 cameraUp) {
    vec3 target = TEST_PLANE_POINT + vec3(0.0, 0.92, 0.0);
    float yaw = 0.64;
    float pitch = 0.34;
    if (dot(u_mouse, u_mouse) > 1.0) {
        vec2 mouse = clamp(u_mouse / max(resolution, vec2(1.0)), vec2(0.0), vec2(1.0));
        yaw = mix(-1.35, 1.35, mouse.x);
        pitch = mix(0.10, 0.72, mouse.y);
    }

    vec3 orbit = vec3(sin(yaw) * cos(pitch),
                      sin(pitch),
                      cos(yaw) * cos(pitch));
    rayOrigin = target + orbit * CAMERA_DISTANCE;
    cameraForward = normalize(target - rayOrigin);
    cameraRight = normalize(cross(cameraForward, vec3(0.0, 1.0, 0.0)));
    cameraUp = normalize(cross(cameraRight, cameraForward));
    rayDirection = normalize(cameraForward * 1.72
                           + cameraRight * canvasPosition.x
                           + cameraUp * canvasPosition.y);
}

vec3 proceduralStars(vec2 position, float scale, float threshold,
                     float pointRadius, float time) {
    vec2 cell = floor(position * scale);
    vec2 local = fract(position * scale);
    float seed = hash21(cell + vec2(4.7, 13.1));
    vec2 pointPosition = vec2(0.14) + hash22(cell + vec2(8.3, 27.9)) * 0.72;
    float point = 1.0 - smoothstep(pointRadius * 0.35, pointRadius,
                                   length(local - pointPosition));
    float present = step(threshold, seed);
    float twinkle = 0.68 + 0.32 * sin(time * (1.1 + seed * 2.7) + seed * 31.0);
    vec3 tint = mix(vec3(0.36, 0.58, 1.0), vec3(1.0, 0.84, 0.62),
                    hash21(cell + vec2(31.7, 2.9)));
    return tint * point * present * twinkle * (0.62 + seed * 0.72);
}

vec3 skyColor(vec3 rayDirection, float time) {
    float horizon = sat(rayDirection.y * 0.5 + 0.5);
    vec3 color = mix(vec3(0.028, 0.032, 0.046), vec3(0.003, 0.006, 0.022),
                     pow(horizon, 0.72));
    vec2 sphereUv = vec2(atan(rayDirection.z, rayDirection.x) / TAU,
                         atan(rayDirection.y, length(rayDirection.xz)) / PI);
    color += proceduralStars(sphereUv, 92.0, 0.986, 0.105, time * 0.35) * 0.42;
    color += proceduralStars(sphereUv + vec2(0.13, -0.27), 157.0,
                             0.994, 0.080, time * 0.22) * 0.26;
    return color;
}

// 每个节点在主方向两侧独立偏折，折线连接后形成尖锐转折而不是连续圆弧。
vec2 crackNode(float crackIndex, float baseAngle, float crackLength,
               float nodeIndex) {
    float progress = nodeIndex / float(CRACK_SEGMENTS);
    vec2 forward = vec2(cos(baseAngle), sin(baseAngle));
    vec2 side = vec2(-forward.y, forward.x);
    float lateralSeed = hash11(crackIndex * 31.7 + nodeIndex * 13.1 + 7.3);
    float forwardSeed = hash11(crackIndex * 17.9 + nodeIndex * 29.3 + 3.7);
    float lateral = (lateralSeed - 0.5) * mix(0.08, 0.52, progress);
    float radialJitter = (forwardSeed - 0.5) * crackLength
                       / float(CRACK_SEGMENTS) * 0.30;
    float radial = max(crackLength * progress + radialJitter, 0.0);
    float born = step(0.5, nodeIndex);
    return forward * radial + side * lateral * born;
}

// 生成确定性的折线主裂痕与两段式分支。x=暗裂口，y=发光核心。
vec2 crackField(vec2 position, float growth, float life, float time) {
    float radius = length(position);
    float growthFront = growth * 3.35;
    float dark = 0.0;
    float glow = 0.0;

    for (int crackIndex = 0; crackIndex < CRACK_COUNT; ++crackIndex) {
        float index = float(crackIndex);
        float seed = hash11(index * 9.73 + 4.1);
        float baseAngle = index * TAU / float(CRACK_COUNT)
                        + (seed - 0.5) * 0.52;
        float crackLength = mix(1.75, 3.05, hash11(index + 17.3));
        for (int segmentIndex = 0; segmentIndex < CRACK_SEGMENTS; ++segmentIndex) {
            float segment = float(segmentIndex);
            vec2 segmentStart = crackNode(index, baseAngle, crackLength, segment);
            vec2 segmentTarget = crackNode(index, baseAngle, crackLength, segment + 1.0);
            float startRadius = length(segmentStart);
            float segmentLength = length(segmentTarget - segmentStart);
            float segmentReveal = sat((growthFront - startRadius)
                                / max(segmentLength, 0.001));
            vec2 segmentEnd = mix(segmentStart, segmentTarget, segmentReveal);
            float segmentBorn = step(0.001, segmentReveal);
            float distanceToSegment = segmentDistance(position, segmentStart, segmentEnd);
            float segmentProgress = (segment + 0.5) / float(CRACK_SEGMENTS);
            float taper = mix(0.052, 0.014, segmentProgress);
            float mainDark = (1.0 - smoothstep(taper, taper * 2.30,
                                                distanceToSegment))
                           * segmentBorn;
            float mainCore = (1.0 - smoothstep(taper * 0.16, taper * 0.58,
                                                distanceToSegment))
                           * segmentBorn;
            float frontPulse = exp(-length(position - segmentEnd) * 8.5)
                             * (1.0 - step(0.995, segmentReveal));
            float flowPulse = 0.5 + 0.5 * sin(radius * 8.0 - time * 4.8
                                             + seed * 9.0 + segment * 1.7);
            dark = max(dark, mainDark);
            glow = max(glow, mainCore
                * (0.18 + frontPulse * 1.28 + flowPulse * 0.22));
        }

        float branchNodeIndex = mix(3.0, 4.0, step(0.5, hash11(index + 33.7)));
        vec2 branchPrevious = crackNode(index, baseAngle, crackLength,
                                        branchNodeIndex - 1.0);
        vec2 branchStart = crackNode(index, baseAngle, crackLength,
                                     branchNodeIndex);
        vec2 sourceDirection = normalize(branchStart - branchPrevious + vec2(0.0001));
        float branchSide = mod(float(crackIndex), 2.0) < 0.5 ? -1.0 : 1.0;
        float branchTurn = branchSide * mix(0.42, 0.82, hash11(index + 51.9));
        float branchCosine = cos(branchTurn);
        float branchSine = sin(branchTurn);
        vec2 branchDirection = vec2(
            sourceDirection.x * branchCosine - sourceDirection.y * branchSine,
            sourceDirection.x * branchSine + sourceDirection.y * branchCosine);
        float branchLength = mix(0.46, 0.92, hash11(index + 68.2));
        float branchGrowth = sat((growthFront - length(branchStart))
                            / max(branchLength, 0.001));
        vec2 branchNormal = vec2(-branchDirection.y, branchDirection.x);
        vec2 branchMiddle = branchStart + branchDirection * branchLength * 0.48
                          + branchNormal * branchSide * branchLength * 0.15;
        vec2 branchTarget = branchMiddle + branchDirection * branchLength * 0.52
                          - branchNormal * branchSide * branchLength * 0.26;
        float firstReveal = sat(branchGrowth / 0.48);
        float secondReveal = sat((branchGrowth - 0.48) / 0.52);
        vec2 firstEnd = mix(branchStart, branchMiddle, firstReveal);
        vec2 secondEnd = mix(branchMiddle, branchTarget, secondReveal);
        float firstDistance = segmentDistance(position, branchStart, firstEnd);
        float secondDistance = segmentDistance(position, branchMiddle, secondEnd);
        float branchDistance = min(firstDistance,
            mix(100.0, secondDistance, step(0.001, secondReveal)));
        float branchBorn = step(0.015, branchGrowth);
        float branchDark = (1.0 - smoothstep(0.018, 0.052, branchDistance))
                          * branchBorn;
        float branchCore = (1.0 - smoothstep(0.004, 0.015, branchDistance))
                         * branchBorn;

        float branchFront = exp(-min(length(position - firstEnd),
                                     length(position - secondEnd)) * 9.0);
        dark = max(dark, branchDark);
        glow = max(glow, branchCore * (0.14 + branchFront * 0.96));
    }

    return vec2(dark * life, glow * life);
}

vec3 planeMaterial(vec3 surfacePosition, vec3 rayDirection,
                   vec3 planeNormal, vec3 tangent, vec3 bitangent,
                   float phase, float time, float shellRadius,
                   float burstLife, float distanceAlongRay) {
    vec3 relative = surfacePosition - TEST_PLANE_POINT;
    vec2 local = vec2(dot(relative, tangent), dot(relative, bitangent));
    float radius = length(local);
    float angle = atan(local.y, local.x);

    vec2 gridCell = abs(fract(local + 0.5) - 0.5);
    float fineGrid = 1.0 - smoothstep(0.022, 0.052, min(gridCell.x, gridCell.y));
    vec2 majorCell = abs(fract(local * 0.2 + 0.5) - 0.5);
    float majorGrid = 1.0 - smoothstep(0.006, 0.018,
                                       min(majorCell.x, majorCell.y));
    float gridFade = 1.0 - smoothstep(4.0, 12.0, distanceAlongRay);
    vec3 planeColor = vec3(0.052, 0.058, 0.068);
    planeColor += vec3(0.030, 0.038, 0.050) * fineGrid * gridFade;
    planeColor += vec3(0.055, 0.074, 0.095) * majorGrid * gridFade;

    float scarReveal = easeOutCubic((phase - 0.145) / 0.18);
    float scarLife = smoothstep(0.135, 0.19, phase)
                   * (1.0 - smoothstep(0.88, 0.99, phase));
    float scarRadius = mix(0.10, 2.46, scarReveal);
    float fixedWarp = fbm2(local * 0.62 + vec2(7.1, -3.9)) - 0.5;
    float smallWarp = fbm2(local * 1.84 + vec2(-11.7, 8.3)) - 0.5;
    float radialLobes = sin(angle * 5.0 + fixedWarp * 2.4) * 0.12
                      + sin(angle * 9.0 - smallWarp * 3.1) * 0.055;
    float tendrils = pow(sat(0.5 + 0.5 * sin(angle * 7.0 + fixedWarp * 5.0)), 9.0)
                    * smoothstep(0.55, 1.65, radius) * 0.24;
    float boundaryRadius = scarRadius + fixedWarp * 0.46
                         + smallWarp * 0.17 + radialLobes + tendrils;
    float signedScar = boundaryRadius - radius;
    float scarMask = smoothstep(-0.055, 0.055, signedScar) * scarLife;
    float edgeMask = exp(-abs(signedScar) * 23.0) * scarLife;
    float innerEdge = exp(-abs(signedScar - 0.11) * 15.0) * scarLife;

    vec2 viewParallax = vec2(dot(rayDirection, tangent), dot(rayDirection, bitangent))
                      / max(abs(dot(rayDirection, planeNormal)), 0.15);
    vec2 farSpace = local * 0.52 + viewParallax * 0.10;
    vec2 midSpace = local * 0.88 + viewParallax * 0.32;
    vec2 nearSpace = local * 1.36 + viewParallax * 0.72;
    float nebulaBroad = fbm2(farSpace * 0.72
                            + vec2(time * 0.018, -time * 0.014));
    float nebulaDetail = fbm2(midSpace * 1.18
                             + vec2(-time * 0.031, time * 0.024));
    float nebulaSignal = sat(nebulaBroad * 0.62 + nebulaDetail * 0.38);
    float steppedNebula = floor(nebulaSignal * 5.0) / 4.0;
    float nebulaAngle = atan(midSpace.y, midSpace.x);
    float nebulaRadius = length(midSpace);
    float nebulaWave = 0.5 + 0.5 * sin(nebulaAngle * 3.0
                                     + nebulaRadius * 2.8
                                     - time * 0.055
                                     + nebulaBroad * 4.2);
    float violetRidge = smoothstep(0.48, 0.67,
        nebulaSignal + nebulaWave * 0.16);
    float cyanRidge = smoothstep(0.59, 0.76,
        nebulaDetail * 0.78 + (1.0 - nebulaWave) * 0.24);
    vec3 spaceColor = mix(VOID_COLOR, vec3(0.052, 0.012, 0.115),
                          steppedNebula * 0.86);
    spaceColor += mix(vec3(0.12, 0.025, 0.22), MAGENTA_COLOR, 0.34)
                * violetRidge * (0.11 + steppedNebula * 0.15);
    spaceColor += mix(vec3(0.015, 0.12, 0.20), CYAN_COLOR, 0.28)
                * cyanRidge * (0.08 + nebulaDetail * 0.15);
    spaceColor += proceduralStars(farSpace, 6.0, 0.78, 0.180,
                                  time * 0.45) * 0.72;
    spaceColor += proceduralStars(midSpace, 10.0, 0.86, 0.140,
                                  time * 0.72) * 1.16;
    spaceColor += proceduralStars(nearSpace, 16.0, 0.92, 0.110,
                                  time) * 1.58;
    planeColor = mix(planeColor, spaceColor, scarMask * 0.97);

    float crackGrowth = easeOutCubic((phase - 0.16) / 0.19);
    vec2 cracks = crackField(local, crackGrowth, scarLife, time);
    planeColor = mix(planeColor, vec3(0.004, 0.003, 0.012), cracks.x * 0.92);
    planeColor += mix(VIOLET_COLOR, CYAN_COLOR,
                      0.5 + 0.5 * sin(radius * 2.8 - time * 1.7))
                * cracks.y * 1.15;

    float planeDistance = abs(dot(TEST_PLANE_POINT
                                + planeNormal * EXPLOSION_HEIGHT
                                - TEST_PLANE_POINT, planeNormal));
    float contactRadius = sqrt(max(shellRadius * shellRadius
                                  - planeDistance * planeDistance, 0.0));
    float contactExists = step(planeDistance, shellRadius) * burstLife;
    float ringWarp = (fbm2(local * 1.9 + vec2(4.7, -9.3)) - 0.5) * 0.12;
    float ringDistance = abs(radius - contactRadius + ringWarp);
    float ringSegments = smoothstep(0.18, 0.72,
        0.5 + 0.5 * sin(angle * 13.0 + fixedWarp * 4.2));
    float contactRing = exp(-ringDistance * ringDistance / 0.0045)
                      * contactExists * (0.28 + ringSegments * 0.92);
    float trailingRing = exp(-pow((radius - contactRadius * 0.91) / 0.13, 2.0))
                       * contactExists * 0.30;
    planeColor += HOT_COLOR * contactRing * 1.15;
    planeColor += mix(VIOLET_COLOR, CYAN_COLOR, ringSegments)
                * trailingRing;

    planeColor += CYAN_COLOR * edgeMask * (0.24 + 0.38 * scarReveal);
    planeColor += MAGENTA_COLOR * innerEdge * 0.14;
    float centerShadow = exp(-radius * radius / 1.8) * burstLife;
    planeColor *= 1.0 - centerShadow * 0.16;
    return planeColor;
}

// 返回 RGB 自发光和 A 密度。噪声被量化为色阶，保持风格化块面而非连续烟雾。
vec4 burstSample(vec3 worldPosition, vec3 center, float shellRadius,
                 float progress, float life, float time) {
    vec3 local = worldPosition - center;
    float radius = length(local);
    float noise = flowNoise3(local, time * 0.72);
    float steppedNoise = floor(sat(noise) * 4.0) / 3.0;
    float radialWarp = (noise - 0.5) * mix(0.07, 0.22, progress);
    float shellWidth = mix(0.20, 0.115, progress);
    float shellCoordinate = abs(radius + radialWarp - shellRadius)
                          / max(shellWidth, 0.001);
    float leadingShell = exp(-shellCoordinate * shellCoordinate * 1.35);

    float cloudRadius = shellRadius * 0.84;
    float cloudWidth = mix(0.28, 0.42, progress);
    float cloudCoordinate = abs(radius + radialWarp * 1.45 - cloudRadius)
                          / max(cloudWidth, 0.001);
    float cloudShell = exp(-cloudCoordinate * cloudCoordinate);
    float brokenCloud = smoothstep(0.38, 0.70,
        noise + 0.12 * sin(local.x * 2.1 + local.z * 1.7 - time * 0.8));
    float cavity = smoothstep(shellRadius * mix(0.08, 0.47, progress),
                              shellRadius * mix(0.23, 0.62, progress), radius);
    float density = (leadingShell * (0.14 + steppedNoise * 0.72)
                    + cloudShell * brokenCloud * (0.34 + steppedNoise * 0.78))
                  * cavity * life;

    float stratumPosition = fract((radius + radialWarp) * 3.8 - noise * 1.35);
    float stratum = 1.0 - smoothstep(0.045, 0.14,
                                     abs(stratumPosition - 0.5));
    float hotPatch = smoothstep(0.64, 0.78, noise + leadingShell * 0.10);
    vec3 color = mix(vec3(0.030, 0.020, 0.105), VIOLET_COLOR, steppedNoise);
    color = mix(color, MAGENTA_COLOR, hotPatch * 0.62);
    color = mix(color, HOT_COLOR, leadingShell * (0.28 + hotPatch * 0.42));
    vec3 emission = color * density * (0.72 + steppedNoise * 0.74);
    emission += CYAN_COLOR * stratum * leadingShell * life * 0.30;
    emission += CORE_COLOR * leadingShell * hotPatch * life * 0.42;
    return vec4(emission, density);
}

vec4 integrateBurst(vec3 rayOrigin, vec3 rayDirection, float startDistance,
                    float endDistance, vec3 center, float shellRadius,
                    float progress, float life, float time) {
    float segmentLength = max(endDistance - startDistance, 0.0);
    float stepLength = segmentLength / float(VOLUME_STEPS);
    vec3 accumulatedLight = vec3(0.0);
    float accumulatedOpacity = 0.0;

    for (int stepIndex = 0; stepIndex < VOLUME_STEPS; ++stepIndex) {
        float stepFraction = (float(stepIndex) + 0.5) / float(VOLUME_STEPS);
        float distanceAlongRay = mix(startDistance, endDistance, stepFraction);
        vec3 samplePosition = rayOrigin + rayDirection * distanceAlongRay;
        vec4 plasma = burstSample(samplePosition, center, shellRadius,
                                  progress, life, time);
        float sampleOpacity = 1.0 - exp(-plasma.a * stepLength * 2.8);
        float transmittance = 1.0 - accumulatedOpacity;
        accumulatedLight += transmittance * plasma.rgb * stepLength * 2.4;
        accumulatedOpacity += transmittance * sampleOpacity;
    }
    return vec4(accumulatedLight, accumulatedOpacity);
}

vec3 shellContour(vec3 rayOrigin, vec3 rayDirection, vec3 center,
                  float shellRadius, float life, float time,
                  float sceneLimit) {
    vec2 hit = raySphere(rayOrigin, rayDirection, center, shellRadius);
    float distanceAlongRay = hit.x > 0.0 ? hit.x : hit.y;
    if (distanceAlongRay <= 0.0 || distanceAlongRay >= sceneLimit) {
        return vec3(0.0);
    }
    vec3 normal = normalize(rayOrigin + rayDirection * distanceAlongRay - center);
    float fresnel = pow(sat(1.0 - abs(dot(normal, rayDirection))), 2.6);
    float longitude = atan(normal.z, normal.x);
    float latitude = atan(normal.y, length(normal.xz));
    float broken = smoothstep(0.18, 0.72,
        0.5 + 0.5 * sin(longitude * 11.0 + latitude * 7.0
                      + flowNoise3(normal * 4.8, time) * 5.0));
    float fineArc = pow(sat(0.5 + 0.5 * sin(longitude * 19.0
                                           - latitude * 13.0
                                           + time * 0.75)), 8.0);
    return mix(HOT_COLOR, CYAN_COLOR, broken)
         * life * (fresnel * (0.42 + broken * 0.88) + fineArc * fresnel * 0.38);
}

float finiteRay(float along, float across, float lengthScale, float width) {
    float axial = 1.0 - smoothstep(lengthScale * 0.54, lengthScale,
                                    abs(along));
    return exp(-abs(across) / max(width, 0.0001)) * axial;
}

vec3 crossFlash(vec3 rayOrigin, vec3 rayDirection, vec3 center,
                vec3 cameraForward, vec3 cameraRight, vec3 cameraUp,
                float phase, float sceneLimit) {
    float denominator = dot(rayDirection, cameraForward);
    if (abs(denominator) < 0.0001) {
        return vec3(0.0);
    }
    float distanceAlongRay = dot(center - rayOrigin, cameraForward) / denominator;
    if (distanceAlongRay <= 0.0 || distanceAlongRay >= sceneLimit) {
        return vec3(0.0);
    }

    vec3 hitPosition = rayOrigin + rayDirection * distanceAlongRay - center;
    vec2 position = vec2(dot(hitPosition, cameraRight), dot(hitPosition, cameraUp));
    float entrance = smoothstep(0.045, 0.072, phase);
    float exit = 1.0 - smoothstep(0.135, 0.205, phase);
    float life = entrance * exit;
    float growth = easeOutCubic((phase - 0.045) / 0.075);
    float overshoot = 1.0 + 0.22 * exp(-abs(phase - 0.096) * 38.0);
    float lengthScale = mix(0.12, 1.72, growth) * overshoot;
    float width = mix(0.010, 0.026, growth);
    float horizontal = finiteRay(position.x, position.y, lengthScale, width);
    float vertical = finiteRay(position.y, position.x, lengthScale, width);

    vec2 diagonalPosition = mat2(0.70710678, -0.70710678,
                                  0.70710678, 0.70710678) * position;
    float diagonalDelay = smoothstep(0.074, 0.102, phase);
    float diagonal = (finiteRay(diagonalPosition.x, diagonalPosition.y,
                                lengthScale * 0.44, width * 0.78)
                    + finiteRay(diagonalPosition.y, diagonalPosition.x,
                                lengthScale * 0.44, width * 0.78))
                   * diagonalDelay;
    float coreDistance = dot(position, position);
    float core = exp(-coreDistance / mix(0.0012, 0.009, growth));
    float bloom = exp(-coreDistance / mix(0.012, 0.12, growth));
    float shimmer = 0.94 + 0.06 * sin(u_time * 23.0);
    vec3 light = CORE_COLOR * (horizontal + vertical) * 1.85;
    light += HOT_COLOR * diagonal * 0.92;
    light += CORE_COLOR * core * 3.2;
    light += CYAN_COLOR * bloom * 0.34;
    return light * life * shimmer;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 canvasPosition = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float time = mod(u_time, 4096.0);
    float phase = mod(time, CYCLE) / CYCLE;

    vec3 rayOrigin;
    vec3 rayDirection;
    vec3 cameraForward;
    vec3 cameraRight;
    vec3 cameraUp;
    buildCamera(canvasPosition, resolution, rayOrigin, rayDirection,
                cameraForward, cameraRight, cameraUp);

    vec3 planeNormal = normalize(TEST_PLANE_NORMAL);
    vec3 tangent;
    vec3 bitangent;
    planeBasis(planeNormal, tangent, bitangent);
    vec3 explosionCenter = TEST_PLANE_POINT + planeNormal * EXPLOSION_HEIGHT;

    float burstProgress = easeOutCubic((phase - 0.105) / 0.155);
    float burstLife = smoothstep(0.095, 0.125, phase)
                    * (1.0 - smoothstep(0.30, 0.39, phase));
    float shellRadius = mix(0.14, MAX_BURST_RADIUS, burstProgress);

    float planeDistance = rayPlane(rayOrigin, rayDirection,
                                   TEST_PLANE_POINT, planeNormal);
    float sceneLimit = planeDistance > 0.0 ? planeDistance : 1000.0;
    vec3 color = skyColor(rayDirection, time);

    if (planeDistance > 0.0) {
        vec3 planePosition = rayOrigin + rayDirection * planeDistance;
        vec3 surfaceColor = planeMaterial(planePosition, rayDirection,
            planeNormal, tangent, bitangent, phase, time, shellRadius,
            burstLife, planeDistance);
        float horizonFog = 1.0 - exp(-planeDistance * 0.025);
        color = mix(surfaceColor, skyColor(rayDirection, time), horizonFog * 0.72);
    }

    if (burstLife > 0.0001) {
        float boundRadius = shellRadius + 0.52;
        vec2 volumeHit = raySphere(rayOrigin, rayDirection,
                                   explosionCenter, boundRadius);
        float volumeStart = max(volumeHit.x, 0.0);
        float volumeEnd = min(volumeHit.y, sceneLimit);
        if (volumeHit.y > 0.0 && volumeEnd > volumeStart) {
            vec4 volume = integrateBurst(rayOrigin, rayDirection,
                volumeStart, volumeEnd, explosionCenter, shellRadius,
                burstProgress, burstLife, time);
            color = volume.rgb + color * (1.0 - volume.a * 0.74);
        }
        color += shellContour(rayOrigin, rayDirection, explosionCenter,
                              shellRadius, burstLife, time, sceneLimit);
    }

    color += crossFlash(rayOrigin, rayDirection, explosionCenter,
                        cameraForward, cameraRight, cameraUp,
                        phase, sceneLimit);

    float vignette = 1.0 - smoothstep(0.78, 1.58, length(canvasPosition));
    color *= 0.60 + vignette * 0.40;
    color = vec3(1.0) - exp(-max(color, vec3(0.0)) * EXPOSURE);
    gl_FragColor = vec4(color, 1.0);
}

// VoidRifter — self-contained WebGL 1 / GLSL ES 1.00 single-pass star effect.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform float u_time;
uniform vec2 u_resolution;

const float EXPOSURE = 1.35;
const float RAY_LENGTH = 1.08;
const float SHARP_WIDTH = 150.0;
const float SOFT_WIDTH = 26.0;
const float DIAMOND_RADIUS = 1.0;
const float EDGE_DENT = 1.5;
// The broad volume is deliberately measured in the same signed-clearance space
// as the contour.  The outside reach is intentionally much wider than the
// crisp root, so the silhouette reads as a substantial energy shell rather
// than a one-pixel outline.
const float INNER_GLOW_REACH = 1.5;
const float OUTER_GLOW_REACH = 1.5;

float sat(float value) {
    return clamp(value, 0.0, 1.0);
}

float endFade(float coordinate) {
    return 1.0 - smoothstep(RAY_LENGTH * 0.58, RAY_LENGTH, abs(coordinate));
}

float smallDiagonalEndFade(float coordinate) {
    return 1.0 - smoothstep(0.18, 0.46, abs(coordinate));
}

// The narrow terms define crisp horizontal and vertical rays.  Their length
// fades independently so the effect stays a finite centered star, not an
// infinite pair of lines.
float sharpCross(vec2 p, float pulse) {
    float horizontal = exp(-abs(p.y) * SHARP_WIDTH * pulse)
                     * endFade(p.x);
    float vertical = exp(-abs(p.x) * SHARP_WIDTH * pulse)
                   * endFade(p.y);
    return horizontal + vertical;
}

// A wider companion layer gives the rays a soft edge without washing out the
// bright center or changing the white-only palette.
float softCross(vec2 p) {
    float horizontal = exp(-abs(p.y) * SOFT_WIDTH) * exp(-abs(p.x) * 2.35)
                     * endFade(p.x);
    float vertical = exp(-abs(p.x) * SOFT_WIDTH) * exp(-abs(p.y) * 2.35)
                   * endFade(p.y);
    return horizontal + vertical;
}

// A compact cross on the two diagonal axes adds a restrained 45-degree
// sparkle without extending into the main cross's long horizontal/vertical
// silhouette.
float diagonalCross(vec2 p) {
    vec2 diagonal = vec2((p.x + p.y) * 0.70710678,
                         (p.x - p.y) * 0.70710678);
    float positiveSlope = exp(-abs(diagonal.y) * 96.0)
                        * smallDiagonalEndFade(diagonal.x);
    float negativeSlope = exp(-abs(diagonal.x) * 96.0)
                        * smallDiagonalEndFade(diagonal.y);
    return positiveSlope + negativeSlope;
}

// The four sides bow inward while the axis-aligned tips remain sharp.  In each
// quadrant, abs(p.x) * abs(p.y) is zero at both tips and peaks at the middle of
// the side, so only the side middle receives the inward displacement.
float concaveDiamondClearance(vec2 p) {
    vec2 quadrant = abs(p);
    float sideDent = EDGE_DENT * 4.0 * quadrant.x * quadrant.y
                   / (DIAMOND_RADIUS * DIAMOND_RADIUS);
    return DIAMOND_RADIUS - quadrant.x - quadrant.y - sideDent;
}

// Small deterministic hashes keep the star field texture-free and compatible
// with WebGL 1.  Each grid cell contributes at most one star.
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += vec2(dot(p, p + vec2(45.32)));
    return fract(p.x * p.y);
}

// Bilinear value noise supplies texture-free breakup for the electric edge.
// The moving coordinates make the rim flicker and crawl without introducing
// any sampler or non-WebGL-1 dependency.
float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float lowerLeft = hash21(cell);
    float lowerRight = hash21(cell + vec2(1.0, 0.0));
    float upperLeft = hash21(cell + vec2(0.0, 1.0));
    float upperRight = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(lowerLeft, lowerRight, local.x),
               mix(upperLeft, upperRight, local.x), local.y);
}

// A short fixed fbm stack keeps the rim turbulent without loops, textures, or
// any feature beyond the GLSL ES 1.00 fragment language.
float fbm(vec2 p) {
    float value = valueNoise(p) * 0.50;
    p = p * 2.03 + vec2(17.1, -9.2);
    value += valueNoise(p) * 0.27;
    p = p * 2.01 + vec2(-11.7, 23.4);
    value += valueNoise(p) * 0.15;
    p = p * 2.07 + vec2(8.6, 14.3);
    value += valueNoise(p) * 0.08;
    return value;
}

vec2 hash22(vec2 p) {
    return vec2(
        hash21(p + vec2(17.31, 4.17)),
        hash21(p + vec2(41.73, 9.83))
    );
}

// Re-map the procedural field along radial paths.  As the phase grows, a
// source star appears farther from the center, then wraps back to the origin
// to create a continuous portal-like outward flow.
vec2 portalStarCoordinates(vec2 p, float time, float speed, float offset) {
    float distanceFromCenter = length(p);
    vec2 direction = p / max(distanceFromCenter, 0.0001);
    float phase = mod(time * speed + offset, DIAMOND_RADIUS);
    float sourceDistance = mod(distanceFromCenter - phase + DIAMOND_RADIUS,
                               DIAMOND_RADIUS);
    return direction * sourceDistance;
}

vec3 starLayer(vec2 p, float scale, float threshold, float radius, float time) {
    vec2 cell = floor(p * scale);
    vec2 local = fract(p * scale);
    float seed = hash21(cell + vec2(6.7, 19.1));
    vec2 starPosition = vec2(0.16) + vec2(0.68) * hash22(cell + vec2(2.4, 31.8));
    float distanceToStar = length(local - starPosition);
    float point = 1.0 - smoothstep(0.0, radius, distanceToStar);
    float present = step(threshold, seed);
    float twinkle = 0.65 + 0.35 * sin(time * (1.2 + seed * 4.0) + seed * 37.0);
    float brightness = point * present * twinkle * (0.65 + 0.7 * seed);
    float whiteSeed = hash21(cell + vec2(7.3, 23.7));
    vec3 tint = mix(vec3(0.40, 0.62, 0.98), vec3(0.92, 0.97, 1.0),
                    smoothstep(0.55, 0.92, whiteSeed));
    return tint * brightness;
}

// Each depth plane receives a different amount of rotational parallax and a
// tiny drifting offset.  Sampling the same procedural field through these
// warped coordinates makes the portal feel volumetric instead of like a flat
// grid of dots, while the original fragment position still owns the shape mask.
vec2 depthWarp(vec2 p, float time, float depth) {
    float radius = length(p);
    float twist = depth * (0.10 + 0.18 * radius)
                * sin(time * (0.18 + depth * 0.42)
                      + radius * 4.2 + depth * 5.0);
    float cosine = cos(twist);
    float sine = sin(twist);
    vec2 warped = vec2(p.x * cosine - p.y * sine,
                       p.x * sine + p.y * cosine);
    vec2 drift = vec2(sin(time * (0.13 + depth * 0.24) + depth * 7.0),
                      cos(time * (0.17 + depth * 0.31) - depth * 4.0));
    return warped + drift * (0.004 + 0.016 * depth) * (0.35 + radius);
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = max(min(resolution.x, resolution.y), 1.0);
    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float time = mod(u_time, 4096.0);

    // A restrained pulse keeps the star alive without visibly shifting its
    // centered silhouette or introducing a dependency on mouse input.
    float pulse = 0.96 + 0.06 * sin(time * 2.15);
    float shimmer = 0.97 + 0.03 * sin(time * 7.3 + 0.6);

    float sharp = sharpCross(p, pulse) * shimmer;
    float soft = softCross(p);
    float inner = exp(-abs(p.y) * 64.0 - abs(p.x) * 13.0)
                + exp(-abs(p.x) * 64.0 - abs(p.y) * 13.0);
    float core = exp(-dot(p, p) / 0.0019);
    float centerBloom = exp(-dot(p, p) / 0.050);
    float diagonal = diagonalCross(p);

    float shapeClearance = concaveDiamondClearance(p);
    float insideDiamond = smoothstep(0.0, 0.018, shapeClearance);

    // The signed concave clearance is split at zero so the original inner rim
    // can stay sharp while the spill layers are generated only on the outside.
    // Every layer therefore follows the four dented sides instead of a radial
    // distance from the origin.
    float insideDistance = max(shapeClearance, 0.0);
    float outsideDistance = max(-shapeClearance, 0.0);
    float insideGate = step(0.0, shapeClearance)
                     * (1.0 - smoothstep(0.078, INNER_GLOW_REACH,
                                         shapeClearance));
    float outsideGate = step(0.0, -shapeClearance)
                      * (1.0 - smoothstep(0.078, OUTER_GLOW_REACH,
                                          outsideDistance));
    float angle = atan(p.y + 0.0001, p.x + 0.0001);
    // A local coordinate along each quadrant's concave side lets the slow
    // breakup form long torn sections.  The signed clearance remains the only
    // radial coordinate, so these fields stay attached to the diamond sides.
    vec2 quadrant = abs(p);
    float sideCoordinate = atan(quadrant.y, quadrant.x + 0.0001) / 1.5707963;
    float sideIndex = step(0.0, p.x) + 2.0 * step(0.0, p.y);

    // A quiet interior body gives the portal a deep navy/purple void even
    // between stars.  The clearance fade leaves a clean pocket below the
    // luminous rim, so the procedural sky never muddies the concave outline.
    float spaceMask = insideDiamond * smoothstep(0.012, 0.085, insideDistance);
    float radialDepth = 1.0 - smoothstep(0.10, 0.98, length(p));
    vec2 nebulaPosition = depthWarp(p, time, 0.42);
    float nebulaLarge = valueNoise(nebulaPosition * 2.6
                                   + vec2(time * 0.018, -time * 0.013));
    float nebulaDetail = valueNoise(nebulaPosition * 7.2
                                    + vec2(-time * 0.031, time * 0.024));
    float cloudField = nebulaLarge * 0.68 + nebulaDetail * 0.32;
    float cloud = smoothstep(0.43, 0.78, cloudField);
    float cloudFlow = 0.5 + 0.5 * sin(angle * 2.0 - time * 0.16
                                      + length(p) * 7.0);
    vec3 deepSpace = mix(vec3(0.002, 0.004, 0.018),
                         vec3(0.010, 0.006, 0.040), radialDepth);
    deepSpace += vec3(0.005, 0.004, 0.022) * cloud
               * (0.55 + 0.45 * cloudFlow);
    deepSpace += vec3(0.003, 0.001, 0.014) * nebulaDetail * radialDepth;
    deepSpace *= spaceMask;

    // The contour remains the sole distance source for every rim layer.  The
    // fbm fields only warp thickness, so the spill follows all four concave
    // sides instead of degrading into a circular screen-space halo.
    float coarseEdgeNoise = fbm(p * 17.0
                              + vec2(time * 0.20, -time * 0.16));
    float fineEdgeNoise = fbm(p * 52.0
                            + vec2(-time * 0.67, time * 0.55));
    float highEdgeNoise = valueNoise(p * 116.0
                                   + vec2(time * 1.13, -time * 0.91));
    float contourNoise = fbm(vec2(sideCoordinate * 10.5 + sideIndex * 7.3
                                  + time * 0.15,
                                  time * 0.08 - sideIndex * 3.7));
    float contourDetail = valueNoise(vec2(sideCoordinate * 34.0
                                          + sideIndex * 13.0 - time * 0.42,
                                          outsideDistance * 31.0
                                          + time * 0.29));
    float electricNoise = sat(coarseEdgeNoise * 0.54
                            + fineEdgeNoise * 0.34
                            + highEdgeNoise * 0.12
                            + contourNoise * 0.24);
    float crackle = pow(sat((electricNoise - 0.38) * 1.90), 2.2);
    float tear = pow(sat((fineEdgeNoise * 0.62 + highEdgeNoise * 0.38
                        - 0.46) * 2.15), 2.0);
    float tearPatch = pow(sat((contourNoise * 0.72 + coarseEdgeNoise * 0.28
                             - 0.36) * 2.25), 1.45);
    float fracture = pow(sat((contourDetail * 0.68 + highEdgeNoise * 0.32
                            - 0.48) * 2.8), 2.0);
    tear = max(tear, tearPatch);
    float tremor = 0.5 + 0.5 * sin(time * 18.0 + highEdgeNoise * 9.0
                                  + angle * 17.0 + p.x * 31.0 - p.y * 27.0);
    float flicker = 0.5 + 0.5 * sin(time * 11.5 + fineEdgeNoise * 19.0
                                   + angle * 31.0
                                   + 1.3 * sin(time * 3.2 + angle * 13.0));

    float flowWave = 0.5 + 0.5 * sin(angle * 4.0 - time * 3.7
                                     + 1.4 * sin(angle * 8.0 + time * 1.2)
                                     + coarseEdgeNoise * 4.0);
    float flowPulse = pow(sat(flowWave), 3.6);
    float radialWave = 0.5 + 0.5 * sin(outsideDistance * 43.0 - time * 5.2
                                       + angle * 4.0 + fineEdgeNoise * 6.0);
    float radialPulse = pow(sat(radialWave), 3.0);
    float edgePulse = 0.78 + 0.22 * sin(time * 2.25);
    float edgeActivity = edgePulse * (0.42 + 1.18 * flowPulse
                                    + 0.30 * radialPulse)
                       * (0.58 + 0.72 * electricNoise)
                       * (0.72 + 0.28 * flicker);

    // Sparse displacement makes the wide bands breathe, buckle, and tear while
    // leaving the recognizable dented silhouette readable at their root.
    float insideWarpDistance = max(insideDistance
                                 - 0.012 * (0.30 + 1.55 * crackle), 0.0);
    float outsideWarpDistance = max(outsideDistance
                                   - 0.018 * (0.22 + 1.45 * tear
                                            + 0.42 * fracture + 0.28 * tremor),
                                    0.0);
    // A floor under the smooth attenuation keeps the outer half of the 0.36
    // reach visibly volumetric instead of turning into an imperceptible tail.
    // The contour is still the only spatial source, so this cannot become a
    // circular halo; noise only buckles the thickness around each side.
    float innerVolume = 0.50
                      + 0.50 * (1.0 - smoothstep(0.0, INNER_GLOW_REACH,
                                                  insideWarpDistance));
    float outerVolume = 0.50
                      + 0.50 * (1.0 - smoothstep(0.0, OUTER_GLOW_REACH,
                                                  outsideWarpDistance));
    // This continuous body is the visible width of the outside contour.  It
    // is kept separate from the sharper rim so the star silhouette grows a
    // substantial energy line instead of merely getting a brighter hairline.
    float outerGlowBody = outsideGate
                        * (0.78 + 0.22 * outerVolume)
                        * (0.68 + 0.32 * electricNoise)
                        * (0.74 + 0.34 * tearPatch)
                        * (1.0 - smoothstep(0.0, OUTER_GLOW_REACH,
                                            outsideWarpDistance));
    float innerWide = insideGate * innerVolume
                    * exp(-insideWarpDistance * 7.5)
                    * (0.46 + 0.54 * electricNoise);
    float innerBand = insideGate * innerVolume
                    * exp(-insideWarpDistance * 18.0)
                    * (0.55 + 0.68 * flowPulse + 0.22 * flicker);
    float edgeSoft = insideGate * innerVolume
                   * exp(-insideWarpDistance * 38.0)
                   * (0.60 + 0.62 * electricNoise + 0.25 * tremor);
    float edgeLine = insideGate * exp(-insideWarpDistance * 148.0)
                   * (0.72 + 0.42 * flowPulse + 0.35 * crackle);

    float outerBandFar = outsideGate * outerVolume
                       * exp(-outsideWarpDistance * 3.0)
                       * (0.40 + 0.60 * electricNoise)
                       * (0.72 + 0.38 * tearPatch);
    float outerBandMid = outsideGate * outerVolume
                       * exp(-outsideWarpDistance * 8.0)
                       * (0.38 + 0.78 * flowPulse + 0.46 * tear
                          + 0.30 * fracture);
    float outerBandNear = outsideGate * outerVolume
                        * exp(-outsideWarpDistance * 18.0)
                        * (0.44 + 0.70 * electricNoise + 0.68 * crackle
                           + 0.34 * fracture);
    float outerRim = outsideGate * outerVolume
                   * exp(-outsideWarpDistance * 48.0)
                   * (0.62 + 0.70 * flowPulse + 0.55 * tear);
    float electricRim = outsideGate * outerVolume
                      * exp(-outsideWarpDistance * 108.0)
                      * (0.52 + 0.68 * flicker + 0.95 * crackle);
    float electricNear = outsideGate * outerVolume
                       * exp(-outsideWarpDistance * 26.0)
                       * (0.40 + 0.70 * electricNoise + 0.72 * tear
                          + 0.38 * fracture);

    float spectralShift = 0.5 + 0.5 * sin(angle * 4.0 + time * 1.25
                                           + coarseEdgeNoise * 5.0);
    vec3 innerColor = mix(vec3(0.45, 0.92, 1.0), vec3(0.94, 1.0, 1.0),
                          sat(0.38 + 0.62 * flowPulse));
    vec3 edgeColor = mix(vec3(0.04, 0.38, 1.0), vec3(0.42, 0.10, 0.96),
                         0.22 + 0.46 * spectralShift);
    vec3 glowColor = mix(vec3(0.015, 0.16, 0.78), edgeColor, 0.48);
    vec3 strandColor = mix(glowColor, vec3(0.48, 0.84, 1.0), 0.42);
    vec3 electricHot = mix(innerColor, vec3(1.0), 0.48);

    // Pulses travel in the signed-distance direction, producing a stacked
    // inner/outer boil rather than one sterile outline.
    float spillPhase = angle * 4.0 - time * 4.6
                     + 0.90 * sin(angle * 9.0 + time * 1.5)
                     + fineEdgeNoise * 5.0;
    float spillAlongEdge = pow(sat(0.5 + 0.5 * sin(spillPhase)), 8.0);
    float spillOutward = pow(sat(0.5 + 0.5
                              * sin(spillPhase - outsideDistance * 33.0
                                  + radialPulse * 2.0)), 12.0);
    float outerStrands = outsideGate * outerVolume
                       * exp(-outsideWarpDistance * 10.0)
                       * (0.50 * spillAlongEdge + 0.46 * spillOutward)
                       * (0.26 + 0.70 * fineEdgeNoise + 1.52 * crackle
                          + 0.62 * tearPatch);

    // High-frequency broken waves form short filaments and needle-like spikes.
    float filamentWave = 0.5 + 0.5 * sin(angle * 31.0 - time * 6.2
                         + outsideDistance * 67.0
                         + 1.8 * sin(angle * 12.0 + outsideDistance * 25.0
                                     + time * 1.8));
    float filamentCore = pow(sat(filamentWave), 14.0);
    float filamentSpark = pow(sat(0.5 + 0.5 * sin(angle * 79.0
                                                   + time * 8.0
                                                   + outsideDistance * 37.0
                                                   + highEdgeNoise * 8.0)), 18.0);
    float outwardFilaments = outsideGate * outerVolume
                            * exp(-outsideWarpDistance * 8.0)
                            * (0.30 * filamentCore + 0.50 * filamentSpark)
                            * (0.22 + 0.58 * fineEdgeNoise + 1.70 * crackle
                               + 0.76 * fracture);
    float spikeWave = 0.5 + 0.5 * sin(angle * 18.0 - time * 7.0
                                      + outsideDistance * 86.0
                                      + tear * 5.0);
    float spikes = outsideGate * outerVolume
                 * exp(-outsideWarpDistance * 6.8)
                 * pow(sat(spikeWave), 17.0)
                 * pow(sat(0.28 + 1.35 * tear + 0.72 * crackle
                           + 0.88 * tearPatch), 2.0)
                 * (0.35 + 0.65 * flicker);
    float emberBursts = outsideGate * outerVolume
                       * exp(-outsideWarpDistance * 18.0)
                       * pow(sat(0.5 + 0.5 * sin(angle * 4.0 + time * 4.9
                           + outsideDistance * 42.0 + fineEdgeNoise * 7.0)), 13.0)
                       * pow(sat(0.18 + 1.45 * tear + 0.42 * radialPulse
                                 + 0.70 * fracture), 1.6);

    // The original two layers remain the middle-distance field.  Very fine,
    // slow stars sit farther back, while sparse larger stars move faster in a
    // nearer plane.  Every plane has its own parallax warp and radial speed.
    vec2 farField = depthWarp(p, time, 0.08);
    vec2 nearField = depthWarp(p, time, 0.92);
    vec2 farPortalStars = portalStarCoordinates(farField, time, 0.035, 0.13);
    vec2 portalStars = portalStarCoordinates(p, time, 0.17, 0.0);
    vec2 portalFineStars = portalStarCoordinates(depthWarp(p, time, 0.42),
                                                 time, 0.23, 0.31);
    vec2 nearPortalStars = portalStarCoordinates(nearField, time, 0.39, 0.57);
    vec3 farStars = starLayer(farPortalStars, 31.0, 0.978, 0.045,
                              time * 0.48 + 3.0) * 0.34;
    vec3 midStars = starLayer(portalStars, 11.0, 0.90, 0.105, time) * 0.92
                  + starLayer(portalFineStars, 19.0, 0.955, 0.060,
                              time * 0.82 + 8.0) * 0.50;
    vec3 nearStars = starLayer(nearPortalStars, 7.5, 0.87, 0.15,
                               time * 1.25 + 13.0) * 0.36;
    vec3 stars = farStars + midStars + nearStars;

    // Keep bright points away from the white center and reserve a dark moat
    // inside the signed clearance for the crisp inner edge.  The original
    // diamond mask remains the final authority for all star fragments.
    float starCoreGuard = smoothstep(0.075, 0.20, length(p));
    float starEdgeGuard = smoothstep(0.020, 0.105, insideDistance);
    float starMask = insideDiamond * starCoreGuard * starEdgeGuard;
    stars *= starMask;

    float energy = sharp * 1.16
                 + soft * 0.42
                 + inner * 0.34
                 + diagonal * 0.34
                 + centerBloom * 0.16
                 + core * (2.35 + 0.35 * pulse);
    vec3 color = deepSpace + stars * 1.28;
    // Keep the narrow white-cyan root above the broader chromatic bands, and
    // let the rim add energy without erasing the stars or the central cross.
    color += innerColor * innerWide * (0.20 + 0.32 * edgeActivity);
    color += innerColor * innerBand * (0.28 + 0.48 * edgeActivity);
    color += innerColor * edgeSoft * (0.44 + 0.52 * edgeActivity);
    color += innerColor * edgeLine * (0.82 + 0.72 * edgeActivity);
    color += mix(glowColor, edgeColor, 0.34) * outerGlowBody
           * (0.46 + 0.52 * edgeActivity);
    color += glowColor * outerBandFar * (0.26 + 0.48 * edgeActivity);
    color += glowColor * outerBandMid * (0.30 + 0.56 * edgeActivity);
    color += glowColor * outerBandNear
           * (0.26 + 0.52 * edgeActivity)
           * (0.58 + 0.84 * electricNoise);
    color += edgeColor * outerRim * (0.34 + 0.68 * edgeActivity);
    color += electricHot * electricRim
           * (0.24 + 0.68 * edgeActivity + 1.35 * crackle);
    color += strandColor * outerStrands * (0.30 + 0.70 * edgeActivity)
           * (0.62 + 0.80 * electricNoise);
    color += electricHot * outwardFilaments * (0.38 + 0.92 * edgeActivity);
    color += electricHot * spikes * (0.42 + 1.05 * edgeActivity);
    color += mix(edgeColor, vec3(0.94, 0.98, 1.0), 0.42)
           * emberBursts * (0.24 + 0.82 * tear + 0.42 * edgeActivity);
    color += vec3(energy);

    // Keep the canvas black/transparent outside the procedural glow.  The
    // RGB value remains white so the same shader also reads correctly on a
    // black canvas when the preview host ignores alpha blending.
    float starAlpha = max(max(stars.r, stars.g), stars.b);
    float spaceAlpha = spaceMask * (0.28 + 0.12 * cloud + 0.06 * radialDepth);
    float diamondAlpha = innerWide * 0.10
                       + innerBand * 0.16
                       + edgeLine * (0.62 + 0.20 * edgeActivity)
                       + edgeSoft * 0.16
                        + outerRim * (0.30 + 0.22 * edgeActivity)
                        + outerGlowBody * (0.24 + 0.18 * edgeActivity)
                        + electricNear * (0.10 + 0.12 * crackle)
                        + outerBandNear * 0.18
                        + outerBandMid * 0.17
                        + outerBandFar * 0.15
                        + outerStrands * 0.30
                        + outwardFilaments * 0.34
                        + spikes * 0.28
                        + emberBursts * 0.24;
    float alpha = sat(sharp * 0.58 + soft * 0.46 + inner * 0.22
                    + diagonal * 0.20 + centerBloom * 0.16 + core * 0.74
                    + starAlpha * 0.82 + spaceAlpha + diamondAlpha);

    // Preview tone mapping preserves a white core and a broad, soft falloff.
    color = 1.0 - exp(-max(color, vec3(0.0)) * EXPOSURE);
    gl_FragColor = vec4(color, alpha);
}

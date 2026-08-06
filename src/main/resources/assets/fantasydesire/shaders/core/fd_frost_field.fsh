#version 150

uniform sampler2D DepthSampler;
uniform sampler2D BlockDepthSampler;

uniform mat4 InvViewProj;
uniform vec3 PatternOrigin;
uniform vec3 FieldCenter;
uniform float FieldRadius;
uniform float FieldTime;
uniform vec2 DepthUvScale;

const float FIELD_CRYSTAL_SCALE = 0.06;
const float TERRAIN_CRYSTAL_SCALE = 0.62;

in vec2 texCoord0;

out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    vec3 blend = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, blend.x);
    float nx10 = mix(n010, n110, blend.x);
    float nx01 = mix(n001, n101, blend.x);
    float nx11 = mix(n011, n111, blend.x);
    return mix(mix(nx00, nx10, blend.y), mix(nx01, nx11, blend.y), blend.z);
}

float fbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 3; ++i) {
        value += noise3(p) * amplitude;
        p = p * 2.03 + vec3(13.1, 7.7, 19.3);
        amplitude *= 0.48;
    }
    return value / 0.946;
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
}

vec2 crystalCell(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    float nearest = 100.0;
    float secondNearest = 100.0;

    for (int z = -1; z <= 1; ++z) {
        for (int y = -1; y <= 1; ++y) {
            for (int x = -1; x <= 1; ++x) {
                vec3 offset = vec3(float(x), float(y), float(z));
                vec3 feature = hash33(cell + offset) * 0.72 + vec3(0.14);
                float distanceToFeature = length(offset + feature - local);
                if (distanceToFeature < nearest) {
                    secondNearest = nearest;
                    nearest = distanceToFeature;
                } else if (distanceToFeature < secondNearest) {
                    secondNearest = distanceToFeature;
                }
            }
        }
    }

    return vec2(nearest, secondNearest - nearest);
}

float iceCrystalEnergy(vec3 patternPosition) {
    vec2 crystal = crystalCell(patternPosition * TERRAIN_CRYSTAL_SCALE);
    float facet = 1.0 - smoothstep(0.18, 0.78, crystal.x);
    float crystalEdge = 1.0 - smoothstep(0.025, 0.115, crystal.y);
    return 0.048 + facet * 0.055 + crystalEdge * 0.22;
}

vec3 magneticFlow(vec3 localPosition);

float flowingIceCrystalEnergy(vec3 patternPosition, vec3 localPosition, float time) {
    vec3 flow = magneticFlow(localPosition);
    vec3 randomFlow = vec3(
            sin(time * 0.31 + dot(patternPosition, vec3(0.19, 0.07, 0.13))
                    + sin(patternPosition.z * 0.23)),
            cos(time * 0.27 + dot(patternPosition, vec3(0.11, 0.17, 0.05))
                    + sin(patternPosition.x * 0.19)),
            sin(time * 0.23 + dot(patternPosition, vec3(0.08, 0.14, 0.21))
                    + cos(patternPosition.y * 0.17)));
    vec3 movingPosition = patternPosition + flow * time * 0.11 + randomFlow * 0.26;
    vec2 crystal = crystalCell(movingPosition * FIELD_CRYSTAL_SCALE);
    float facet = 1.0 - smoothstep(0.25, 1.05, crystal.x);
    float crystalEdge = 1.0 - smoothstep(0.008, 0.24, crystal.y);
    float broadFlow = 0.5 + 0.5 * sin(dot(movingPosition, vec3(0.37, 0.19, 0.41)) * 0.88
            - time * 0.30 + facet * 2.7);
    float flowBand = smoothstep(0.28, 0.76, broadFlow);
    float crackFlowPhase = dot(patternPosition, flow) * 4.2 - time * 2.4
            + sin(dot(patternPosition, vec3(0.31, 0.17, 0.29)) * 1.35 - time * 0.65) * 0.55;
    float crackFlowWave = 0.5 + 0.5 * sin(crackFlowPhase);
    float crackFlow = crystalEdge * smoothstep(0.70, 0.96, crackFlowWave);
    float glintWave = 0.5 + 0.5 * sin(dot(movingPosition, vec3(0.74, 0.31, 0.58)) * 1.85
            - time * 0.42 + crystal.x * 4.5);
    float movingGlint = smoothstep(0.46, 0.92, glintWave)
            * (crystalEdge * 0.55 + flowBand * 0.45);
    return 0.080 + facet * 0.20 + crystalEdge * 0.30 + flowBand * 0.07
            + movingGlint * 0.16 + crackFlow * 0.42;
}

vec3 reconstructRelativePosition(vec2 uv, float depth) {
    vec4 clipPosition = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 relativePosition = InvViewProj * clipPosition;
    return relativePosition.xyz / max(abs(relativePosition.w), 0.000001) * sign(relativePosition.w);
}

float sampleDepth(vec2 uv) {
    return texture(DepthSampler, clamp(uv, vec2(0.0), vec2(1.0)) * DepthUvScale).r;
}

float sampleBlockDepth(vec2 uv) {
    return texture(BlockDepthSampler, clamp(uv, vec2(0.0), vec2(1.0)) * DepthUvScale).r;
}

vec3 magneticFlow(vec3 localPosition) {
    vec3 radial = normalize(localPosition + vec3(0.00001));
    vec3 axis = vec3(0.0, 1.0, 0.0);
    vec3 azimuth = cross(axis, radial);
    if (dot(azimuth, azimuth) < 0.0001) {
        azimuth = cross(vec3(1.0, 0.0, 0.0), radial);
    }
    vec3 dipole = 3.0 * radial * dot(radial, axis) - axis;
    return normalize(azimuth * 0.78 + dipole * 0.42);
}

float boundaryEnergy(float distanceAlongRay, float sceneLimit, vec3 rayOrigin, vec3 rayDirection,
        float time, bool cameraInsideField) {
    if (distanceAlongRay < 0.0 || distanceAlongRay > sceneLimit + 0.025) {
        return 0.0;
    }

    vec3 relativePosition = rayOrigin + rayDirection * distanceAlongRay;
    vec3 localPosition = relativePosition - FieldCenter;
    vec3 normal = normalize(localPosition);
    vec3 patternPosition = PatternOrigin + relativePosition;
    float crystalEnergy = flowingIceCrystalEnergy(patternPosition, localPosition, time);
    float fresnel = pow(clamp(1.0 - abs(dot(normal, rayDirection)), 0.0, 1.0), 2.65);
    float interiorRim = cameraInsideField ? fresnel : 1.0;
    return interiorRim * ((0.052 + fresnel * (0.76 + crystalEnergy * 1.45)) * (0.86 + crystalEnergy * 0.32)
            + crystalEnergy * (0.045 + fresnel * 0.16));
}

vec3 surfaceEnergy(vec3 sceneRelativePosition) {
    vec3 localPosition = sceneRelativePosition - FieldCenter;
    float radialDistance = length(localPosition);
    if (radialDistance >= FieldRadius) {
        return vec3(0.0);
    }

    float normalizedRadius = radialDistance / max(FieldRadius, 0.001);

    vec3 patternPosition = PatternOrigin + sceneRelativePosition;
    float crystalEnergy = iceCrystalEnergy(patternPosition);
    float reveal = 1.0 - smoothstep(0.96, 1.0, normalizedRadius);
    float infusion = reveal * crystalEnergy;

    float shellDistance = abs(radialDistance - FieldRadius);
    float pixelWidth = max(fwidth(radialDistance), 0.012);
    float contactCore = 1.0 - smoothstep(pixelWidth * 0.8, pixelWidth * 3.2, shellDistance);
    float contactHaloWidth = max(max(0.32, FieldRadius * 0.035), pixelWidth * 4.0);
    float contactHalo = 1.0 - smoothstep(pixelWidth * 1.5, contactHaloWidth, shellDistance);
    return vec3(infusion, contactCore, contactHalo);
}

void main() {
    float depth = sampleDepth(texCoord0);
    bool hasSurface = depth < 0.99999;
    float blockDepth = sampleBlockDepth(texCoord0);
    bool hasBlockSurface = blockDepth < 0.99999;
    vec3 rayOrigin = reconstructRelativePosition(texCoord0, 0.0);
    vec3 farRelativePosition = reconstructRelativePosition(texCoord0, 1.0);
    vec3 rayDirection = normalize(farRelativePosition - rayOrigin);
    vec3 sceneRelativePosition = hasSurface
            ? reconstructRelativePosition(texCoord0, depth)
            : rayDirection * 1000000.0;
    float sceneLimit = hasSurface
            ? max(dot(sceneRelativePosition - rayOrigin, rayDirection), 0.0)
            : 1000000.0;

    vec3 cameraToCenter = FieldCenter - rayOrigin;
    float projectedCenter = dot(cameraToCenter, rayDirection);
    float centerDistanceSquared = dot(cameraToCenter, cameraToCenter) - projectedCenter * projectedCenter;
    float radiusSquared = FieldRadius * FieldRadius;
    bool cameraInsideField = dot(cameraToCenter, cameraToCenter) < radiusSquared;
    float intersectionSquared = radiusSquared - centerDistanceSquared;
    if (intersectionSquared < 0.0) {
        discard;
    }

    float intersectionOffset = sqrt(intersectionSquared);
    float nearDistance = projectedCenter - intersectionOffset;
    float farDistance = projectedCenter + intersectionOffset;
    if (farDistance <= 0.0) {
        discard;
    }

    float time = FieldTime * 0.05;
    float segmentStart = max(nearDistance, 0.0);
    float segmentEnd = min(farDistance, max(sceneLimit - 0.025, 0.0));
    float volumeEnergy = 0.0;
    if (!cameraInsideField && segmentEnd > segmentStart) {
        float stepLength = (segmentEnd - segmentStart) / 10.0;
        for (int i = 0; i < 10; ++i) {
            float sampleDistance = segmentStart + (float(i) + 0.5) * stepLength;
            vec3 relativePosition = rayOrigin + rayDirection * sampleDistance;
            vec3 localPosition = relativePosition - FieldCenter;
            vec3 flow = magneticFlow(localPosition);
            vec3 patternPosition = PatternOrigin + relativePosition;
            float density = fbm3(patternPosition * 0.18 - flow * time * 0.31);
            float filamentSignal = fract(density * 2.7 + dot(patternPosition, flow) * 0.08 - time * 0.17);
            float filament = smoothstep(0.86, 0.985, 1.0 - abs(filamentSignal * 2.0 - 1.0));
            float radialPosition = length(localPosition) / max(FieldRadius, 0.001);
            float densityEnvelope = 0.32 + 0.68 * (1.0 - smoothstep(0.68, 1.0, radialPosition));
            volumeEnergy += (0.012 + density * 0.022 + filament * 0.052) * densityEnvelope
                    * stepLength / max(FieldRadius, 0.001);
        }
    }

    float boundary = boundaryEnergy(nearDistance, sceneLimit, rayOrigin, rayDirection, time, cameraInsideField);
    if (nearDistance < 0.0 || farDistance <= sceneLimit + 0.025) {
        boundary += boundaryEnergy(farDistance, sceneLimit, rayOrigin, rayDirection, time, cameraInsideField) * 0.72;
    }

    bool blockIsVisible = hasBlockSurface && (!hasSurface || blockDepth <= depth + 0.0005);
    vec3 blockRelativePosition = hasBlockSurface
            ? reconstructRelativePosition(texCoord0, blockDepth)
            : vec3(0.0);
    vec3 surface = blockIsVisible ? surfaceEnergy(blockRelativePosition) : vec3(0.0);
    vec3 volumeColor = vec3(0.20, 0.58, 0.92) * volumeEnergy;
    vec3 boundaryColor = mix(vec3(0.34, 0.76, 1.0), vec3(1.0), clamp(boundary * 1.3, 0.0, 0.88))
            * boundary;
    vec3 surfaceColor = vec3(0.24, 0.76, 1.0) * surface.x * 1.45;
    vec3 contactColor = vec3(0.72, 0.95, 1.0) * surface.y * 1.25
            + vec3(0.24, 0.72, 1.0) * surface.z * 0.54;
    vec3 emission = volumeColor + boundaryColor + surfaceColor + contactColor;

    if (max(max(emission.r, emission.g), emission.b) < 0.001) {
        discard;
    }
    fragColor = vec4(emission, 0.0);
}

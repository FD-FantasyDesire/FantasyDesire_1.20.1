#version 150

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;

uniform float Time;
uniform vec4 CoreColor;
uniform vec4 FlameColor;
uniform float EruptionIntensity;
uniform float NoiseSpeed;
uniform float PulseFrequency;
uniform float Opacity;
uniform float RadiusScale;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
            mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.52;
    for (int i = 0; i < 3; ++i) {
        v += a * noise(p);
        p = p * 1.62 + vec2(11.7, 4.6);
        a *= 0.38;
    }
    return v;
}

float arcSegment(float angle, float radius, float seed, float t) {
    float lanes = 18.0;
    float lane = floor((angle + 3.14159265) / 6.2831853 * lanes + seed);
    float laneHash = hash(vec2(lane, seed));
    float laneCenter = ((lane + 0.5 - seed) / lanes) * 6.2831853 - 3.14159265;
    float angleDelta = abs(atan(sin(angle - laneCenter), cos(angle - laneCenter)));
    float angularLine = smoothstep(0.070, 0.0, angleDelta + (hash(vec2(lane, 7.3 + seed)) - 0.5) * 0.018);
    float radialWindow = smoothstep(0.42 + laneHash * 0.18, 0.55 + laneHash * 0.18, radius)
            * smoothstep(1.16 - laneHash * 0.10, 0.78 + laneHash * 0.08, radius);
    float flicker = step(0.53, noise(vec2(lane * 2.71 + seed, floor(t * 18.0 + laneHash * 9.0))))
            * (0.55 + 0.45 * sin(t * 38.0 + laneHash * 31.0));
    float jagged = 0.72 + 0.28 * sin(radius * 46.0 + laneHash * 12.0 + t * (10.0 + seed));
    return angularLine * radialWindow * flicker * jagged;
}

void main() {
    vec2 centeredUv = texCoord0 * 2.0 - 1.0;
    float uvDist = length(centeredUv);
    float squareExtent = max(abs(centeredUv.x), abs(centeredUv.y));
    float squareBoundaryFade = smoothstep(0.995, 0.78, squareExtent);
    float circularBoundaryFade = smoothstep(1.02, 0.68, uvDist);
    float quadBoundaryFade = pow(squareBoundaryFade, 2.4) * pow(circularBoundaryFade, 1.7);
    if (quadBoundaryFade <= 0.0005) {
        discard;
    }

    vec2 p = centeredUv / max(RadiusScale, 0.001);
    float dist = length(p);
    if (dist > 1.30) {
        discard;
    }

    float t = Time * max(NoiseSpeed, 0.001);
    float angle = atan(p.y, p.x);
    float radial = clamp(dist, 0.0, 1.35);

    float shock = sin(Time * 6.28318 * max(PulseFrequency, 0.001));
    float tremor = 0.020 * sin(Time * 21.0 + sin(angle * 5.0) * 1.4)
            + 0.012 * sin(Time * 37.0 + angle * 3.0);
    float breathing = 1.0 + 0.055 * shock + tremor;

    float roll = angle * 0.25 + t * 0.42;
    vec2 flowUv = vec2(p.x * 1.18 + sin(roll + p.y * 2.20) * 0.14,
            p.y * 1.18 + cos(roll * 1.43 + p.x * 2.05) * 0.14);
    float lowA = fbm(flowUv * 1.55 + vec2(t * 0.95, -t * 0.44));
    float lowB = fbm(flowUv * 0.86 + vec2(-t * 0.58, t * 0.76) + vec2(5.1, 2.7));
    float low = mix(lowA, lowB, 0.55);
    float boundaryNoise = low * 2.0 - 1.0;

    float pulse = 1.0 + 0.070 * shock + 0.036 * sin(Time * 13.0 + low * 5.0);
    float lobe = sin(angle * 4.0 + t * 2.4) * 0.44 + sin(angle * 7.0 - t * 1.9 + 1.7) * 0.26;
    float confinedRadius = (0.84 * breathing) + boundaryNoise * (0.042 + 0.030 * EruptionIntensity) + lobe * 0.030;
    float bodyMask = smoothstep(confinedRadius + 0.12, confinedRadius - 0.075, radial);
    float body = bodyMask * smoothstep(0.10, 0.36, radial);
    float edge = exp(-abs(radial - confinedRadius) * 12.5) * (0.36 + low * 0.24);

    float sphericalShade = pow(clamp(1.0 - dist * dist, 0.0, 1.0), 0.42);
    float coreRadius = 0.40 * breathing + boundaryNoise * 0.018;
    float core = smoothstep(coreRadius + 0.11, coreRadius - 0.10, dist) * (1.38 + 0.14 * shock);
    float hotCore = exp(-(dist * dist) * (13.0 + 2.0 * sin(Time * 17.0))) * (2.15 + 0.30 * shock);
    float whiteCenter = exp(-dist * dist * 42.0) * (4.40 + 0.56 * sin(Time * 31.0));
    float magneticBand = 0.5 + 0.5 * sin(angle * 6.0 + radial * 15.0 - Time * 5.6 + low * 3.0);
    float plasma = (body * (0.44 + low * 0.30 + magneticBand * 0.18) + edge * 0.34) * pulse;
    float halo = exp(-dist * dist * 1.85) * smoothstep(1.06, 0.62, dist) * 0.18;

    float ringArc = arcSegment(angle + sin(radial * 9.0 + t * 4.0) * 0.13, radial, 0.0, Time)
            + arcSegment(angle * 1.07 - cos(radial * 7.0 - t * 3.2) * 0.11, radial, 5.0, Time * 1.13);
    float bandSpark = smoothstep(0.92, 1.0, sin(angle * 22.0 + low * 7.0 - Time * 18.0) * 0.5 + 0.5)
            * smoothstep(0.48, 0.70, radial) * smoothstep(1.13, 0.90, radial)
            * step(0.62, noise(vec2(floor(angle * 9.0 + 19.0), floor(Time * 24.0))));
    float arcs = clamp(ringArc * (1.10 + EruptionIntensity * 0.85) + bandSpark * 0.45, 0.0, 1.8);

    vec3 plasmaGreen = max(FlameColor.rgb, vec3(0.05, 0.95, 0.18));
    plasmaGreen = mix(plasmaGreen, vec3(0.0, 1.0, 0.86), smoothstep(0.52, 1.02, radial));
    vec3 cyanCorona = vec3(0.05, 1.0, 0.82);
    vec3 arcColor = vec3(0.70, 1.0, 0.96);
    vec3 coreTint = mix(vec3(0.78, 1.0, 0.84), CoreColor.rgb, 0.22);
    vec3 whiteHot = mix(coreTint, vec3(1.0), clamp(core * 0.72 + hotCore * 0.18 + whiteCenter * 0.20, 0.0, 1.0));
    float outerPresence = smoothstep(1.00, 0.52, dist) * circularBoundaryFade;
    vec3 color = (plasmaGreen * plasma * 0.94
            + cyanCorona * halo
            + arcColor * arcs * (1.65 + 0.30 * shock) * outerPresence
            + whiteHot * (core * 1.55 + hotCore + whiteCenter) * (0.74 + 0.26 * sphericalShade))
            * vertexColor.a * quadBoundaryFade;

    float sphericalMask = smoothstep(1.10, 0.76, dist);
    float outerAlphaLimiter = mix(0.42, 1.0, smoothstep(0.82, 0.20, dist));
    float alpha = clamp((core * 0.66 + hotCore * 0.22 + plasma * 0.42 + edge * 0.08 + halo * 0.55 + arcs * 0.46)
            * Opacity * vertexColor.a, 0.0, 1.0);
    alpha *= sphericalMask * quadBoundaryFade * outerAlphaLimiter;
    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(color, alpha);
}

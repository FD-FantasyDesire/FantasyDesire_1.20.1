#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform float Time;
uniform float Opacity;
uniform float FlowSpeed;
uniform float LumpScale;
uniform float FilamentIntensity;
uniform vec4 CoreColor;
uniform vec4 FlowColor;
uniform vec4 EdgeColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
            mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 3; ++i) {
        v += a * noise(p);
        p = p * 1.83 + vec2(9.7, 4.1);
        a *= 0.43;
    }
    return v;
}

void main() {
    float u = clamp(texCoord0.x, 0.0, 1.0);
    float side = texCoord0.y * 2.0 - 1.0;
    float edgeDistance = abs(side);

    float feather = smoothstep(1.0, 0.18, edgeDistance);
    float inner = smoothstep(0.58, 0.0, edgeDistance);
    float rim = smoothstep(0.98, 0.48, edgeDistance) * (1.0 - inner * 0.58);
    if (feather <= 0.002) {
        discard;
    }

    float t = Time * max(FlowSpeed, 0.001);
    vec2 flowUv = vec2(u * max(LumpScale, 0.1) - t * 0.42, side * 1.35);
    float low = fbm(flowUv + vec2(0.0, sin(u * 5.3 - t) * 0.18));
    float low2 = fbm(flowUv * 0.53 + vec2(t * 0.13, -t * 0.08) + vec2(4.2, 8.1));
    float lump = smoothstep(0.38, 0.88, low * 0.72 + low2 * 0.48);
    float ferroPulse = 0.5 + 0.5 * sin(u * 18.0 - t * 4.6 + low * 5.2);
    float bead = pow(clamp(0.58 * lump + 0.42 * ferroPulse, 0.0, 1.0), 1.65);

    float strandA = 1.0 - abs(fract(u * 6.8 - t * 0.88 + low * 0.34) * 2.0 - 1.0);
    float strandB = 1.0 - abs(fract(u * 11.0 - t * 1.27 + low2 * 0.25 + side * 0.12) * 2.0 - 1.0);
    float filaments = (pow(strandA, 7.0) * 0.68 + pow(strandB, 10.0) * 0.38) * inner;

    float coreEnd = 1.18 - smoothstep(0.0, 0.16, u) * 0.18;
    float targetGrip = 0.86 + smoothstep(0.78, 1.0, u) * 0.18;
    float endIntensity = coreEnd * targetGrip;

    vec3 whiteHot = mix(CoreColor.rgb, vec3(1.0), 0.28);
    vec3 hotFlow = mix(FlowColor.rgb, vec3(1.0), 0.20);
    vec3 brightFlow = max(FlowColor.rgb, vec3(0.02));
    vec3 edgeTint = max(EdgeColor.rgb, vec3(0.04));
    vec3 filamentTint = mix(FlowColor.rgb, vec3(1.0), 0.42);

    vec3 bodyColor = mix(brightFlow, hotFlow, clamp(lump * 0.85 + inner * 0.35, 0.0, 1.0));
    bodyColor = mix(bodyColor, filamentTint, filaments * 0.55);
    bodyColor = mix(bodyColor, edgeTint, rim * (0.28 + low2 * 0.28));
    vec3 color = bodyColor * (0.72 + bead * 0.72 + rim * 0.24);
    color += whiteHot * filaments * FilamentIntensity * 2.15;
    color += hotFlow * inner * bead * 0.42;
    color *= vertexColor.rgb * endIntensity;

    float alpha = (feather * (0.42 + bead * 0.42) + filaments * 0.38 + rim * 0.18) * vertexColor.a * Opacity;
    alpha *= 0.80 + 0.20 * smoothstep(0.0, 0.08, u);
    alpha *= 0.90 + 0.10 * smoothstep(1.0, 0.86, u);
    alpha = clamp(alpha, 0.0, 1.0);
    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(color, alpha);
}


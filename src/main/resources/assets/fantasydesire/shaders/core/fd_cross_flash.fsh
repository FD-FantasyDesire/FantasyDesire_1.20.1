#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform float GameTime;

out vec4 fragColor;

float crossRay(float along, float across, float lengthScale, float width, float lateralPower) {
    float body = exp(-pow(abs(along) / max(lengthScale, 0.0001), 1.16));
    float edge = exp(-pow(abs(across) / max(width, 0.0001), lateralPower));
    return body * edge;
}

float axisCross(vec2 p, float lengthScale, float width, float lateralPower) {
    float horizontal = crossRay(p.x, p.y, lengthScale, width, lateralPower);
    float vertical = crossRay(p.y, p.x, lengthScale, width, lateralPower);
    return horizontal + vertical;
}

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;

    float dist = length(p);
    float time = GameTime * 24000.0;
    float pulse = 0.92 + 0.08 * sin(time * 0.11) + 0.035 * sin(time * 0.37);
    float shimmer = 0.96 + 0.04 * sin((abs(p.x) + abs(p.y)) * 20.0 - time * 0.23);

    float edgeFade = smoothstep(1.18, 0.92, max(abs(p.x), abs(p.y)));
    float radialFade = smoothstep(1.34, 0.42, dist);
    float centerMask = smoothstep(0.72, 0.02, dist);

    float crispCross = axisCross(p, 1.10, 0.026, 1.72) * 0.92 * radialFade;
    float softCross = axisCross(p, 0.96, 0.082, 1.86) * 0.33 * radialFade;
    float bloomCross = axisCross(p, 0.72, 0.155, 2.05) * 0.17 * centerMask;

    float coreDist = dot(p, p);
    float coreGlow = exp(-coreDist * 18.0) * 0.82;
    float coreHot = exp(-coreDist * 82.0) * 2.20;
    float coreWhite = exp(-coreDist * 210.0) * 1.55;

    float intensity = (crispCross + softCross + bloomCross + coreGlow + coreHot + coreWhite)
            * edgeFade * pulse * shimmer;
    intensity = clamp(intensity, 0.0, 3.15);

    float alpha = clamp(intensity * vertexColor.a, 0.0, 1.0);
    if (alpha < 0.002 || edgeFade < 0.004) {
        discard;
    }

    vec3 tint = max(vertexColor.rgb, vec3(0.02));
    vec3 hotWhite = vec3(1.0, 0.97, 0.88);
    vec3 outerColor = tint * (crispCross + softCross + bloomCross) * 0.95;
    vec3 innerColor = mix(tint, hotWhite, clamp((coreHot + coreWhite) * 0.32, 0.0, 0.92))
            * (coreGlow + coreHot + coreWhite) * 1.42;
    vec3 color = (outerColor + innerColor) * vertexColor.a;

    fragColor = vec4(color, alpha);
}

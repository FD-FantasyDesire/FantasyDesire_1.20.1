#version 150

uniform float FieldRadius;
uniform float PulseRadius; // 格，与地表和球壳共享同一个波峰位置。

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 fieldPosition;
out vec4 fragColor;

void main() {
    float edgeDistance = min(texCoord0.x, 1.0 - texCoord0.x);
    float aa = min(fwidth(texCoord0.x), 0.12);
    float edge = 1.0 - smoothstep(0.015, 0.05 + aa, edgeDistance);
    float halo = exp(-edgeDistance * 10.0);
    float tip = smoothstep(0.76, 1.0, texCoord0.y);
    float radius = length(fieldPosition);
    float boundary = 1.0 - smoothstep(FieldRadius - 0.35, FieldRadius, radius);
    float ring = abs(length(fieldPosition.xz) - PulseRadius);
    float pulse = exp(-ring * ring * 8.0) * smoothstep(0.0, 0.25, PulseRadius)
            * (1.0 - smoothstep(FieldRadius, FieldRadius + 1.2, PulseRadius))
            * (1.0 - smoothstep(0.85, 1.2, ring));
    float strata = pow(0.5 + 0.5 * sin(texCoord0.y * 42.0 + texCoord0.x * 4.0), 14.0);
    float fade = vertexColor.a * boundary;
    float alpha = (0.48 + vertexColor.r * 0.16 + tip * 0.12) * fade;
    vec3 ice = mix(vec3(0.055, 0.27, 0.43), vec3(0.44, 0.82, 0.96), vertexColor.r * 0.7 + tip * 0.25);
    vec3 emission = vec3(0.36, 0.82, 1.0) * (halo * 0.15 + strata * 0.06 + pulse * 0.13);
    emission += vec3(0.78, 0.95, 1.0) * (edge * (0.27 + pulse * 0.30) + tip * 0.24);
    if (fade <= 0.001) discard;
    fragColor = vec4(ice * alpha + emission * fade, alpha);
}

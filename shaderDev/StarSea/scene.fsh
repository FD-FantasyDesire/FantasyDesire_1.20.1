#version 300 es
precision highp float;

in vec3 worldPosition;
in vec3 normal;
in vec3 baseColor;
uniform float Terrain;
out vec4 fragColor;

void main() {
    float illumination = 0.53 + 0.47 * max(dot(normal, normalize(vec3(-0.4, 0.85, 0.45))), 0.0);
    vec3 color = baseColor * illumination;
    // 演示地砖与细小材质颗粒只用于辨认覆盖范围，不采样 Minecraft 资源。
    if (Terrain > 0.5 && abs(normal.y) > 0.5) {
        vec2 cell = abs(fract(worldPosition.xz) - 0.5);
        float edge = max(cell.x, cell.y);
        float aa = max(fwidth(edge), 0.001);
        float grout = smoothstep(0.477 - aa, 0.477 + aa, edge);
        float tile = mod(floor(worldPosition.x) + floor(worldPosition.z), 2.0);
        float grain = fract(sin(dot(floor(worldPosition.xz * 42.0), vec2(12.98, 78.23))) * 4375.85);
        color *= (0.91 + tile * 0.09 + grain * 0.035) * (1.0 - grout * 0.22);
    }
    float fog = 1.0 - exp(-length(worldPosition.xz) * 0.025);
    color = mix(color, vec3(0.045, 0.058, 0.085), fog);
    fragColor = vec4(color, 1.0);
}

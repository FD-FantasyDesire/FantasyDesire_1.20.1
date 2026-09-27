#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec3 Tint;
uniform float Time; // 秒，由独立预览配置绑定。
uniform float Pulse;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 uv;
in vec3 localPosition;
out vec4 fragColor;
void main() {
    vec4 color = texture(Sampler0, uv) * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    float wave = 0.5 + 0.5 * sin(localPosition.y * 8.0 - Time * 2.0);
    color.rgb *= Tint;
    color.rgb += Pulse * wave * vec3(0.12, 0.5, 0.8);
    fragColor = color;
}

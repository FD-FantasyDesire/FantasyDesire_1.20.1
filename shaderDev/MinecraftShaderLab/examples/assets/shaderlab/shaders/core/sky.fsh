#version 150
uniform float Time; // 秒。
uniform vec3 Zenith;
uniform vec3 Horizon;
in vec3 direction;
out vec4 fragColor;
void main() {
    vec3 d = normalize(direction);
    vec3 color = mix(Horizon, Zenith, smoothstep(-0.1, 0.8, d.y));
    float clouds = sin(d.x * 12.0 + Time * 0.1) * sin(d.z * 9.0 - Time * 0.08);
    color += vec3(0.035, 0.04, 0.06) * smoothstep(0.5, 1.0, clouds) * smoothstep(0.0, 0.2, d.y);
    vec3 sunDir = normalize(vec3(-0.6, 0.6, -0.4));
    float sun = pow(max(dot(d, sunDir), 0.0), 180.0);
    fragColor = vec4(color + vec3(0.9, 0.7, 0.4) * sun, 1.0);
}

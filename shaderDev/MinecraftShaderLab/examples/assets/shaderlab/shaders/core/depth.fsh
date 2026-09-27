#version 150
uniform sampler2D DepthSampler;
uniform sampler2D BlockDepthSampler;
uniform mat4 InvViewProj;
uniform float Time; // 秒。
uniform float Radius;
in vec2 uv;
out vec4 fragColor;
void main() {
    float blockDepth = texture(BlockDepthSampler, uv).r;
    float sceneDepth = texture(DepthSampler, uv).r;
    if (blockDepth >= 0.99999 || sceneDepth < blockDepth - 0.00001) discard;
    vec4 p = InvViewProj * vec4(uv * 2.0 - 1.0, blockDepth * 2.0 - 1.0, 1.0);
    vec3 world = p.xyz / p.w;
    float radius = length(world.xz);
    float edge = 1.0 - smoothstep(Radius - 0.5, Radius, radius);
    float rings = pow(0.5 + 0.5 * sin(radius * 8.0 - Time * 3.0), 4.0);
    fragColor = vec4(vec3(0.03, 0.6, 0.95) * edge * (0.2 + rings * 0.8), edge * 0.72);
}

#version 150
in vec3 Position;
uniform mat4 InverseViewProjection;
flat out mat4 viewProjection;

void main() {
    // 每顶点求逆一次，避免每片元、每实例重复反演相机矩阵。
    viewProjection = inverse(InverseViewProjection);
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}

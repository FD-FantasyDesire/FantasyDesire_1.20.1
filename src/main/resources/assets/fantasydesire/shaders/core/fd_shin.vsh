#version 150

in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 EffectLocalMat;
out vec3 localPosition;
out vec3 viewPosition;
flat out mat4 viewToLocal;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPosition = view.xyz;
    localPosition = (EffectLocalMat * vec4(Position, 1.0)).xyz;
    // 游戏顶点已乘 pose，Lab 顶点仍是单位盒；该矩阵统一两端的局部坐标。
    viewToLocal = EffectLocalMat * inverse(ModelViewMat);
}

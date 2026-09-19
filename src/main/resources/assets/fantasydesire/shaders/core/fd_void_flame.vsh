#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 EffectLocalMat;
uniform int FogShape;

out vec2 texCoord0;
out vec3 localPosition;
out float vertexDistance;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;
    // ModelPart 已把姿态写入 Position；逆转实体根矩阵，保留肢体动画，消除相机与实体位移。
    localPosition = (EffectLocalMat * vec4(Position, 1.0)).xyz;
    texCoord0 = UV0;
    vertexDistance = FogShape == 0 ? length(viewPosition.xyz)
            : max(length(viewPosition.xz), abs(viewPosition.y));
}

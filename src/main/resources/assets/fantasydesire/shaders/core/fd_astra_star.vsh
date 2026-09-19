#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out vec2 localCoord;
flat out int shape;
flat out float trailJoinFraction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    // UV.x 分区：拖尾 [0,1]、主光刺 [2,4]、中心 [5,7]、对角光刺 [8,10]。
    shape = UV0.x < 1.5 ? 0 : (UV0.x < 4.5 ? 1 : (UV0.x < 7.5 ? 2 : 3));
    // 拖尾 UV.y 的绝对值为接入过渡比例，符号还原横向 [-1,1]；各顶点比例一致，保持共享批次。
    trailJoinFraction = shape == 0 ? abs(UV0.y) : 1.0;
    localCoord = vec2(UV0.x - float(shape) * 3.0, shape == 0 ? sign(UV0.y) : UV0.y);
}

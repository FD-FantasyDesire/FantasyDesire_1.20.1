#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ViewProj;
uniform vec3 FieldCenter;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 fieldPosition;

void main() {
    gl_Position = ViewProj * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
    fieldPosition = Position - FieldCenter;
}

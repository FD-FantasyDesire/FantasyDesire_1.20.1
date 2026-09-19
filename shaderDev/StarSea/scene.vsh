#version 300 es
precision highp float;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec3 Color;
uniform mat4 ViewProj;
uniform vec3 CameraPosition;
out vec3 worldPosition;
out vec3 normal;
out vec3 baseColor;

void main() {
    worldPosition = Position;
    normal = Normal;
    baseColor = Color;
    gl_Position = ViewProj * vec4(Position - CameraPosition, 1.0);
}

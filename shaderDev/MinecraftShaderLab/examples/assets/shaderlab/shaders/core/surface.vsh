#version 150
#moj_import <shaderlab:lighting.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
out vec4 vertexColor;
out vec4 overlayColor;
out vec2 uv;
out vec3 localPosition;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * vec4(lab_light(Normal, Light0_Direction, Light1_Direction), 1.0)
        * texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    uv = UV0;
    localPosition = Position;
}

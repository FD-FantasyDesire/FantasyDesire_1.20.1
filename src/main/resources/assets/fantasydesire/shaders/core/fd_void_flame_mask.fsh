#version 150

out vec4 fragColor;

uniform vec3 TargetColor;

void main() {
    fragColor = vec4(TargetColor, 1.0);
}

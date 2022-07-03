#version 460 core

in vec3 vertexPosition;
in vec3 vertexColor;

out vec4 fragmentColor;

void main() {
    fragmentColor = vec4(vertexColor, 1.0);
}
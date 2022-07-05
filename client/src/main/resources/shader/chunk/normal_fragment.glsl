#version 460 core

in vec3 vertexPosition;
in vec3 vertexColor;
in vec3 vertexNormal;

out vec4 fragmentColor;

void main() {
    fragmentColor = vec4((vertexNormal + 1) / 2.0, 1.0);
}
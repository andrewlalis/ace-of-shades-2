#version 460 core

layout (location = 0) in vec3 vertexPositionIn;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;

out vec3 vertexPosition;
out vec3 vertexColor;

void main() {
    gl_Position = projectionTransform * viewTransform * vec4(vertexPositionIn, 1.0);
    vertexPosition = vertexPositionIn;
    vertexColor = vec3(1.0, 0.5, 0.5);
}

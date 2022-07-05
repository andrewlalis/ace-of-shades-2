#version 460 core

layout (location = 0) in vec3 vertexPositionIn;
layout (location = 1) in vec3 vertexColorIn;
layout (location = 2) in vec3 vertexNormalIn;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform ivec3 chunkPosition;
uniform int chunkSize;

out vec3 vertexPosition;
out vec3 vertexColor;
out vec3 vertexNormal;

void main() {
    vertexPosition = vertexPositionIn + (chunkPosition * chunkSize);

    gl_Position = projectionTransform * viewTransform * vec4(vertexPosition, 1.0);
    vertexColor = vertexColorIn;
    vertexNormal = vertexNormalIn;
}

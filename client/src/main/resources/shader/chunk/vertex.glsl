#version 460 core

layout (location = 0) in vec3 vertexPositionIn;
layout (location = 1) in vec3 vertexColorIn;
layout (location = 2) in vec3 vertexNormalIn;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat3 normalTransform;
uniform ivec3 chunkPosition;

out vec3 vertexPosition;
out vec3 vertexColor;
out vec3 vertexNormal;

void main() {
    vec3 realVertexPosition = vertexPositionIn + (chunkPosition * 16);

    gl_Position = projectionTransform * viewTransform * vec4(realVertexPosition, 1.0);
    vertexPosition = realVertexPosition;
    vertexColor = vertexColorIn;
    vertexNormal = normalize(normalTransform * vertexNormalIn);
}

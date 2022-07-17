#version 460 core

layout (location = 0) in vec3 vertexPositionIn;
layout (location = 1) in vec3 vertexNormalIn;
layout (location = 2) in vec2 textureCoordsIn;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 modelTransform;

out vec2 textureCoords;
out vec3 vertexNormal;

void main() {
    gl_Position = projectionTransform * viewTransform * modelTransform * vec4(vertexPositionIn, 1.0);
    vertexNormal = vec3(modelTransform * vec4(vertexNormalIn, 1.0));
    textureCoords = textureCoordsIn;
}
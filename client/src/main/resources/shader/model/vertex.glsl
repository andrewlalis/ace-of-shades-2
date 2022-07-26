#version 460 core

layout (location = 0) in vec3 vertexPositionIn;
layout (location = 1) in vec3 vertexNormalIn;
layout (location = 2) in vec2 textureCoordsIn;

uniform mat4 projectionTransform;
uniform mat4 viewTransform;
uniform mat4 modelTransform;
uniform mat3 normalTransform;

out vec2 textureCoords;
out vec3 vertexColor;

void main() {
    gl_Position = projectionTransform * viewTransform * modelTransform * vec4(vertexPositionIn, 1.0);
    vec3 vertexNormal = normalize(normalTransform * vertexNormalIn);

    textureCoords = textureCoordsIn;

    vec3 lightDirection = normalize(vec3(0.5, -1.0, -0.5));// TODO: Add this via a uniform.
    vec3 lightColor = vec3(1.0, 1.0, 0.9); // TODO: Add this via a uniform.

    float ambientComponent = 0.25;
    float diffuseComponent = max(dot(-vertexNormal, lightDirection), 0.0);
    vertexColor = (ambientComponent + diffuseComponent) * lightColor;
}
#version 460 core

in vec2 textureCoords;
in vec3 vertexNormal;

out vec4 fragmentColor;

uniform vec3 aspectColor;
uniform sampler2D textureSampler;

void main() {
    vec4 baseColor = texture(textureSampler, textureCoords);
    vec4 templateColor = vec4(0.0, 1.0, 0.0, 1.0);
    if (baseColor == templateColor) {
        baseColor = vec4(aspectColor, 1.0);
    }
    vec3 lightDirection = normalize(vec3(0.5, -1.0, -0.5));// TODO: Add this via a uniform.
    vec3 lightColor = vec3(1.0, 1.0, 0.9); // TODO: Add this via a uniform.

    vec3 ambientComponent = vec3(0.1, 0.1, 0.1);
    vec3 diffuseComponent = max(dot(vertexNormal * -1, lightDirection), 0.0) * lightColor;
    // TODO: Add shading based on light.
    // fragmentColor = vec4((ambientComponent + diffuseComponent), 1.0) * baseColor;
    fragmentColor = baseColor;
}
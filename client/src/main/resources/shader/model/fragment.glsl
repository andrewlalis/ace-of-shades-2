#version 460 core

in vec2 textureCoords;
in vec3 vertexColor;

out vec4 fragmentColor;

uniform vec3 aspectColor;
uniform sampler2D textureSampler;

void main() {
    vec4 baseColor = texture(textureSampler, textureCoords);
    vec4 templateColor = vec4(0.0, 1.0, 0.0, 1.0);
    if (baseColor == templateColor) {
        baseColor = vec4(aspectColor, 1.0);
    }


    fragmentColor = vec4(vertexColor * vec3(baseColor), 1.0);
}
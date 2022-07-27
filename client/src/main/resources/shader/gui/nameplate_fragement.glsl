#version 460 core

in vec2 texturePosition;
out vec4 fragmentColor;

uniform sampler2D guiTexture;

void main() {
    fragmentColor = texture(guiTexture, texturePosition);
}
#version 460 core

in vec2 position;
out vec2 texturePosition;

uniform mat4 transform;

void main() {
    gl_Position = transform * vec4(position, 0.0, 1.0);
    texturePosition = vec2(
        (position.x + 1.0) / 2.0,
        1 - (position.y + 1.0) / 2.0
    );
}
#version 460 core

in vec2 vertexPosition;

uniform mat4 transform;
uniform mat4 viewTransform;
uniform mat4 perspectiveTransform;

out vec2 texturePosition;

void main() {
    gl_Position = perspectiveTransform * viewTransform * transform * vec4(vertexPosition, 0.0, 1.0);
    texturePosition = vec2(
        (vertexPosition.x + 1.0) / 2.0,
        1 - (vertexPosition.y + 1.0) / 2.0
    );
}
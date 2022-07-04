#version 460 core

in vec3 vertexPosition;
in vec3 vertexColor;
in vec3 vertexNormal;

out vec4 fragmentColor;

void main() {
    vec3 lightDirection = vec3(0.0, -1.0, -0.5);// TODO: Add this via a uniform.
    vec3 lightColor = vec3(1.0, 1.0, 0.9); // TODO: Add this via a uniform.

    vec3 ambientComponent = vec3(0.1, 0.1, 0.1);
    vec3 diffuseComponent = max(dot(vertexNormal, lightDirection), 0.0) * lightColor;
    // No specular component.

    fragmentColor = vec4((ambientComponent + diffuseComponent) * vertexColor, 1.0);
    //fragmentColor = vec4((vertexNormal + 1) / 2.0, 1.0);
}
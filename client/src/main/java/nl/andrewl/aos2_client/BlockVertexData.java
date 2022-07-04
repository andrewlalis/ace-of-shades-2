package nl.andrewl.aos2_client;

import org.joml.Vector3f;

public record BlockVertexData(
		Vector3f position,
		Vector3f color,
		Vector3f normal
) {}

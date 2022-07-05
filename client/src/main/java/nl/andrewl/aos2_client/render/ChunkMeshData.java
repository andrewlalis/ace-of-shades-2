package nl.andrewl.aos2_client.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public record ChunkMeshData(
		FloatBuffer vertexData,
		IntBuffer indices
) {}

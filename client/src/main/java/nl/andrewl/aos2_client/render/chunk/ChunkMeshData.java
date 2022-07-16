package nl.andrewl.aos2_client.render.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public record ChunkMeshData(
		FloatBuffer vertexBuffer,
		IntBuffer indexBuffer
) {}

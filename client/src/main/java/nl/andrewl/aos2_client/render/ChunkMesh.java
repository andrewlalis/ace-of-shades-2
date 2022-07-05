package nl.andrewl.aos2_client.render;

import nl.andrewl.aos_core.model.Chunk;

import java.util.Arrays;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a 3d mesh for a chunk.
 */
public class ChunkMesh {
	private final int vboId;
	private final int vaoId;
	private final int eboId;

	private int indiciesCount;

	private final int[] positionData;
	private final Chunk chunk;

	public ChunkMesh(Chunk chunk) {
		this.chunk = chunk;
		this.positionData = new int[]{chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z};

		this.vboId = glGenBuffers();
		this.eboId = glGenBuffers();
		this.vaoId = glGenVertexArrays();

		loadMesh();

		initVertexArrayAttributes();
	}

	public int[] getPositionData() {
		return positionData;
	}

	private void loadMesh() {
		long start = System.currentTimeMillis();
		var meshData = ChunkMeshGenerator.generateMesh(chunk);
		long dur = System.currentTimeMillis() - start;
		System.out.printf(
				"Generated chunk mesh in %d ms with %d vertices and %d indices, and %d faces. Vertex data size: %d%n",
				dur,
				meshData.vertexData().limit() / 9,
				meshData.indices().limit(),
				meshData.indices().limit() / 4,
				meshData.vertexData().limit()
		);
		this.indiciesCount = meshData.indices().limit();
		int[] data = new int[indiciesCount];
		meshData.indices().get(data);
		meshData.indices().flip();
		System.out.println(Arrays.toString(data));

		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, meshData.vertexData(), GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData.indices(), GL_STATIC_DRAW);

		int size = glGetBufferParameteri(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE);
		System.out.println(size);
	}

	private void initVertexArrayAttributes() {
		glBindVertexArray(vaoId);
		// Vertex position floats.
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0);
		// Vertex color floats.
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		// Vertex normal floats.
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
	}

	public void draw() {
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBindVertexArray(vaoId);
		glDrawElements(GL_TRIANGLES, indiciesCount, GL_UNSIGNED_INT, 0);
	}

	public void free() {
		glDeleteBuffers(vboId);
		glDeleteBuffers(eboId);
		glDeleteVertexArrays(vaoId);
	}
}

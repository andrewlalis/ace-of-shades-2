package nl.andrewl.aos2_client.render;

import nl.andrewl.aos_core.model.Chunk;

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

	/**
	 * Generates and loads this chunk's mesh into the allocated OpenGL buffers.
	 */
	private void loadMesh() {
		var meshData = ChunkMeshGenerator.generateMesh(chunk);
		this.indiciesCount = meshData.indexBuffer().limit();

		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, meshData.vertexBuffer(), GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData.indexBuffer(), GL_STATIC_DRAW);
	}

	/**
	 * Initializes this mesh's vertex array attribute settings.
	 */
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

	/**
	 * Draws the chunk mesh.
	 */
	public void draw() {
		glBindVertexArray(vaoId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glDrawElements(GL_TRIANGLES, indiciesCount, GL_UNSIGNED_INT, 0);
	}

	public void free() {
		glDeleteBuffers(vboId);
		glDeleteBuffers(eboId);
		glDeleteVertexArrays(vaoId);
	}
}

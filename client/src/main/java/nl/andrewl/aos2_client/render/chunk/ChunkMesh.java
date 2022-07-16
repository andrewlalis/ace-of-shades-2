package nl.andrewl.aos2_client.render.chunk;

import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.model.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a 3d mesh for a chunk.
 */
public class ChunkMesh {
	private static final Logger log = LoggerFactory.getLogger(ChunkMesh.class);

	private final int vboId;
	private final int vaoId;
	private final int eboId;

	private int indexCount;

	private final int[] positionData;
	private final Chunk chunk;
	private final World world;

	public ChunkMesh(Chunk chunk, World world, ChunkMeshGenerator meshGenerator) {
		this.chunk = chunk;
		this.world = world;
		this.positionData = new int[]{chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z};

		this.vboId = glGenBuffers();
		this.eboId = glGenBuffers();
		this.vaoId = glGenVertexArrays();

		loadMesh(meshGenerator);

		initVertexArrayAttributes();
	}

	public int[] getPositionData() {
		return positionData;
	}

	public Chunk getChunk() {
		return chunk;
	}

	/**
	 * Generates and loads this chunk's mesh into the allocated OpenGL buffers.
	 */
	private void loadMesh(ChunkMeshGenerator meshGenerator) {
		long start = System.nanoTime();
		var meshData = meshGenerator.generateMesh(chunk, world);
		double dur = (System.nanoTime() - start) / 1_000_000.0;
		this.indexCount = meshData.indexBuffer().limit();
		// Print some debug information.
		log.debug(
				"Generated mesh for chunk ({}, {}, {}) in {} ms. {} vertices and {} indices.",
				chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z,
				dur,
				meshData.vertexBuffer().limit() / 9, indexCount
		);

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
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindVertexArray(0);
	}

	public void free() {
		glDeleteBuffers(vboId);
		glDeleteBuffers(eboId);
		glDeleteVertexArrays(vaoId);
	}
}

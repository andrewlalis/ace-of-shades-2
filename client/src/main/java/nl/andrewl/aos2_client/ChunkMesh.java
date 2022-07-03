package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.model.Chunk;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public class ChunkMesh {
	private final int vboId;
	private final int vaoId;
	private final int eboId;

	private final int indiciesCount;

	public ChunkMesh(Chunk chunk) {
		this.vboId = glGenBuffers();
		this.vaoId = glGenVertexArrays();
		this.eboId = glGenBuffers();

		var meshData = chunk.generateMesh();
		System.out.println(meshData.first().size());
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(3 * meshData.first().size());
		for (var vertex : meshData.first()) {
			vertexBuffer.put(vertex.x);
			vertexBuffer.put(vertex.y);
			vertexBuffer.put(vertex.z);
		}
		vertexBuffer.flip();
		IntBuffer indexBuffer = BufferUtils.createIntBuffer(meshData.second().size());
		for (var index : meshData.second()) {
			indexBuffer.put(index);
		}
		indexBuffer.flip();
		this.indiciesCount = meshData.second().size();

		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
	}

	public void draw() {
		// Bind elements.
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBindVertexArray(vaoId);

		glDrawElements(GL_TRIANGLES, indiciesCount, GL_UNSIGNED_INT, 0);
	}
}

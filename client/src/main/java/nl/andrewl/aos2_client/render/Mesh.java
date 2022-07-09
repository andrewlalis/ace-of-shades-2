package nl.andrewl.aos2_client.render;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL46.*;

public class Mesh {
	private final int vboId;
	private final int vaoId;
	private final int eboId;
	private int indexCount;
	private final Matrix4f transform = new Matrix4f();
	private final float[] transformData = new float[16];

	public Mesh(MeshData initialData) {
		this.vboId = glGenBuffers();
		this.eboId = glGenBuffers();
		this.vaoId = glGenVertexArrays();
		load(initialData);
		initVertexArrayAttributes();
	}

	public void load(MeshData data) {
		indexCount = data.indexBuffer().limit();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, data.vertexBuffer(), GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indexBuffer(), GL_STATIC_DRAW);
	}

	public Matrix4f getTransform() {
		return transform;
	}

	public void updateTransform() {
		transform.set(transformData);
	}

	public float[] getTransformData() {
		return transformData;
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

	public void draw() {
		glBindVertexArray(vaoId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
	}

	public void free() {
		glDeleteBuffers(vboId);
		glDeleteBuffers(eboId);
		glDeleteVertexArrays(vaoId);
	}
}

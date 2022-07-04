package nl.andrewl.aos2_client;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class ChunkRenderer {
	private final ShaderProgram shaderProgram;
	private final int projectionTransformUniform;
	private final int viewTransformUniform;
	private final int normalTransformUniform;

	private final Matrix4f projectionTransform = new Matrix4f().perspective(70, 800 / 600.0f, 0.01f, 100.0f);

	private final List<ChunkMesh> chunkMeshes = new ArrayList<>();

	public ChunkRenderer() {
		this.shaderProgram = new ShaderProgram.Builder()
				.withShader("shader/chunk/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/chunk/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
		shaderProgram.use();
		this.projectionTransformUniform = shaderProgram.getUniform("projectionTransform");
		this.viewTransformUniform = shaderProgram.getUniform("viewTransform");
		this.normalTransformUniform = shaderProgram.getUniform("normalTransform");

		// Preemptively load projection transform, which doesn't change much.
		glUniformMatrix4fv(projectionTransformUniform, false, projectionTransform.get(new float[16]));
	}

	public void addChunkMesh(ChunkMesh mesh) {
		this.chunkMeshes.add(mesh);
	}

	public void draw(Camera cam) {
		glUniformMatrix4fv(viewTransformUniform, false, cam.getViewTransformData());
		Matrix3f normalTransform = new Matrix3f();
		glUniformMatrix3fv(normalTransformUniform, false, normalTransform.get(new float[9]));
		shaderProgram.use();

		for (var mesh : chunkMeshes) mesh.draw();
	}

	public void free() {
		for (var mesh : chunkMeshes) mesh.free();
		shaderProgram.free();
	}
}

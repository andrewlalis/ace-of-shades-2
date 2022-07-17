package nl.andrewl.aos2_client.render.chunk;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.render.ShaderProgram;
import nl.andrewl.aos_core.model.world.Chunk;

import java.util.Collection;

import static org.lwjgl.opengl.GL46.*;

/**
 * The chunk renderer is responsible for managing the shader program, uniforms,
 * and all currently loaded chunk meshes, so that the set of loaded chunks can
 * be rendered each frame.
 */
public class ChunkRenderer {
	private final ShaderProgram shaderProgram;
	private final int projectionTransformUniform;
	private final int viewTransformUniform;
	private final int chunkPositionUniform;

	public ChunkRenderer() {
		this.shaderProgram = new ShaderProgram.Builder()
				.withShader("shader/chunk/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/chunk/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
		shaderProgram.use();
		this.projectionTransformUniform = shaderProgram.getUniform("projectionTransform");
		this.viewTransformUniform = shaderProgram.getUniform("viewTransform");
		this.chunkPositionUniform = shaderProgram.getUniform("chunkPosition");
		int chunkSizeUniform = shaderProgram.getUniform("chunkSize");
		// Set constant uniforms that don't change during runtime.
		glUniform1i(chunkSizeUniform, Chunk.SIZE);
		shaderProgram.stopUsing();
	}

	public void setPerspective(float[] data) {
		shaderProgram.use();
		glUniformMatrix4fv(projectionTransformUniform, false, data);
		shaderProgram.stopUsing();
	}

	public void draw(Camera cam, Collection<ChunkMesh> chunkMeshes) {
		shaderProgram.use();
		glUniformMatrix4fv(viewTransformUniform, false, cam.getViewTransformData());
		for (var mesh : chunkMeshes) {
			glUniform3iv(chunkPositionUniform, mesh.getPositionData());
			mesh.draw();
		}
		shaderProgram.stopUsing();
	}

	public void free() {
		shaderProgram.free();
	}
}

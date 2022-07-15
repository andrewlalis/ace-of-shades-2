package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.model.World;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL46.*;

/**
 * The chunk renderer is responsible for managing the shader program, uniforms,
 * and all currently loaded chunk meshes, so that the set of loaded chunks can
 * be rendered each frame.
 */
public class ChunkRenderer {
	private final ChunkMeshGenerator chunkMeshGenerator = new ChunkMeshGenerator();
	private final Queue<Chunk> meshGenerationQueue = new ConcurrentLinkedQueue<>();

	private ShaderProgram shaderProgram;
	private int projectionTransformUniform;
	private int viewTransformUniform;
	private int chunkPositionUniform;

	private final Map<Vector3i, ChunkMesh> chunkMeshes = new HashMap<>();

	public void setupShaderProgram() {
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
	}

	public void queueChunkMesh(Chunk chunk) {
		meshGenerationQueue.add(chunk);
	}

	public void setPerspective(Matrix4f projectionTransform) {
		glUniformMatrix4fv(projectionTransformUniform, false, projectionTransform.get(new float[16]));
	}

	public void draw(Camera cam, World world) {
		while (!meshGenerationQueue.isEmpty()) {
			Chunk chunk = meshGenerationQueue.remove();
			ChunkMesh mesh = new ChunkMesh(chunk, world, chunkMeshGenerator);
			ChunkMesh existingMesh = chunkMeshes.get(chunk.getPosition());
			if (existingMesh != null) existingMesh.free();
			chunkMeshes.put(chunk.getPosition(), mesh);
		}
		shaderProgram.use();
		glUniformMatrix4fv(viewTransformUniform, false, cam.getViewTransformData());
		for (var mesh : chunkMeshes.values()) {
			glUniform3iv(chunkPositionUniform, mesh.getPositionData());
			mesh.draw();
		}
	}

	public void free() {
		for (var mesh : chunkMeshes.values()) mesh.free();
		chunkMeshes.clear();
		meshGenerationQueue.clear();
		shaderProgram.free();
	}
}

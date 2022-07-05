package nl.andrewl.aos2_client.render;

import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;


public final class ChunkMeshGenerator {
	private ChunkMeshGenerator() {}

	public static ChunkMeshData generateMesh(Chunk chunk) {
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(20000);
		IntBuffer indexBuffer = BufferUtils.createIntBuffer(5000);
		int idx = 0;
		for (int i = 0; i < Chunk.TOTAL_SIZE; i++) {
			var pos = Chunk.idxToXyz(i);
			int x = pos.x;
			int y = pos.y;
			int z = pos.z;
			byte block = chunk.getBlocks()[i];
			if (block <= 0) {
				continue; // Don't render empty blocks.
			}

			Vector3f color = Chunk.getColor(block);

			// See /design/block_rendering.svg for a diagram of how these vertices are defined.
			var a = new Vector3f(x, y + 1, z + 1);
			var b = new Vector3f(x, y + 1, z);
			var c = new Vector3f(x, y, z);
			var d = new Vector3f(x, y, z + 1);
			var e = new Vector3f(x + 1, y + 1, z);
			var f = new Vector3f(x + 1, y + 1, z + 1);
			var g = new Vector3f(x + 1, y, z + 1);
			var h = new Vector3f(x + 1, y, z);

			// Top
			if (chunk.getBlockAt(x, y + 1, z) == 0) {
				var norm = new Vector3f(0, 1, 0);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(a, f, e, b));
				idx += 4;
			}
			// Bottom
			if (chunk.getBlockAt(x, y - 1, z) == 0) {
				var norm = new Vector3f(0, -1, 0);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(c, h, g, d));
				idx += 4;
			}
			// Positive z
			if (chunk.getBlockAt(x, y, z + 1) == 0) {
				var norm = new Vector3f(0, 0, 1);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(f, a, d, g));
				idx += 4;
			}
			// Negative z
			if (chunk.getBlockAt(x, y, z - 1) == 0) {
				var norm = new Vector3f(0, 0, -1);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(b, e, h, c));
				idx += 4;
			}
			// Positive x
			if (chunk.getBlockAt(x + 1, y, z) == 0) {
				var norm = new Vector3f(1, 0, 0);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(e, f, g, h));
				idx += 4;
			}
			// Negative x
			if (chunk.getBlockAt(x - 1, y, z) == 0) {
				var norm = new Vector3f(-1, 0, 0);
				genFace(vertexBuffer, indexBuffer, idx, color, norm, List.of(a, b, c, d));
				idx += 4;
			}
		}

		return new ChunkMeshData(vertexBuffer.flip(), indexBuffer.flip());
	}

	private static void genFace(FloatBuffer vertexBuffer, IntBuffer indexBuffer, int currentIndex, Vector3f color, Vector3f norm, List<Vector3f> vertices) {
		for (var vertex : vertices) {
			vertexBuffer.put(vertex.x);
			vertexBuffer.put(vertex.y);
			vertexBuffer.put(vertex.z);
			vertexBuffer.put(color.x);
			vertexBuffer.put(color.y);
			vertexBuffer.put(color.z);
			vertexBuffer.put(norm.x);
			vertexBuffer.put(norm.y);
			vertexBuffer.put(norm.z);
		}
		// Top-left triangle.
		indexBuffer.put(currentIndex);
		indexBuffer.put(currentIndex + 1);
		indexBuffer.put(currentIndex + 2);
		// Bottom-right triangle.
		indexBuffer.put(currentIndex + 2);
		indexBuffer.put(currentIndex + 3);
		indexBuffer.put(currentIndex);
	}
}

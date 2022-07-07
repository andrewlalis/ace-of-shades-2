package nl.andrewl.aos2_client.render;

import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Highly-optimized class for generating chunk meshes, without any heap
 * allocations at runtime. Not thread safe.
 */
public final class ChunkMeshGenerator {
	private final FloatBuffer vertexBuffer;
	private final IntBuffer indexBuffer;

	private final Vector3i pos = new Vector3i();
	private final Vector3f color = new Vector3f();
	private final Vector3f norm = new Vector3f();

	public ChunkMeshGenerator() {
		vertexBuffer = BufferUtils.createFloatBuffer(300_000);
		indexBuffer = BufferUtils.createIntBuffer(100_000);
	}

	public ChunkMeshData generateMesh(Chunk chunk) {
		vertexBuffer.clear();
		indexBuffer.clear();
		int idx = 0;
		for (int i = 0; i < Chunk.TOTAL_SIZE; i++) {
			Chunk.idxToXyz(i, pos);
			int x = pos.x;
			int y = pos.y;
			int z = pos.z;
			byte block = chunk.getBlocks()[i];
			if (block <= 0) {
				continue; // Don't render empty blocks.
			}

			Chunk.getColor(block, color);

			// See /design/block_rendering.svg for a diagram of how these vertices are defined.
//			var a = new Vector3f(x, y + 1, z + 1);
//			var b = new Vector3f(x, y + 1, z);
//			var c = new Vector3f(x, y, z);
//			var d = new Vector3f(x, y, z + 1);
//			var e = new Vector3f(x + 1, y + 1, z);
//			var f = new Vector3f(x + 1, y + 1, z + 1);
//			var g = new Vector3f(x + 1, y, z + 1);
//			var h = new Vector3f(x + 1, y, z);

			// Top
			if (chunk.getBlockAt(x, y + 1, z) == 0) {
				norm.set(0, 1, 0);
				genFace(idx,
						x,		y+1,	z+1,	// a
						x+1,	y+1,	z+1,	// f
						x+1,	y+1,	z,		// e
						x,		y+1,	z		// b
				);
				idx += 4;
			}
			// Bottom
			if (chunk.getBlockAt(x, y - 1, z) == 0) {
				norm.set(0, -1, 0);// c h g d
				genFace(idx,
						x,		y,		z,		// c
						x+1,	y,		z,		// h
						x+1,	y,		z+1,	// g
						x,		y,		z+1		// d
				);
				idx += 4;
			}
			// Positive z
			if (chunk.getBlockAt(x, y, z + 1) == 0) {
				norm.set(0, 0, 1);
				genFace(idx,
						x+1,	y+1,	z+1,	// f
						x,		y+1,	z+1,	// a
						x,		y,		z+1,	// d
						x+1,	y,		z+1		// g
				);
				idx += 4;
			}
			// Negative z
			if (chunk.getBlockAt(x, y, z - 1) == 0) {
				norm.set(0, 0, -1);
				genFace(idx,
						x,		y+1,	z,		// b
						x+1,	y+1,	z,		// e
						x+1,	y,		z,		// h
						x,		y,		z		// c
				);
				idx += 4;
			}
			// Positive x
			if (chunk.getBlockAt(x + 1, y, z) == 0) {
				norm.set(1, 0, 0);
				genFace(idx,
						x+1,	y+1,	z,		// e
						x+1,	y+1,	z+1,	// f
						x+1,	y,		z+1,	// g
						x+1,	y,		z		// h
				);
				idx += 4;
			}
			// Negative x
			if (chunk.getBlockAt(x - 1, y, z) == 0) {
				norm.set(-1, 0, 0);
				genFace(idx,
						x,		y+1,	z+1,	// a
						x,		y+1,	z,		// b
						x,		y,		z,		// c
						x,		y,		z+1		// d
				);
				idx += 4;
			}
		}

		return new ChunkMeshData(vertexBuffer.flip(), indexBuffer.flip());
	}

	private void genFace(int currentIndex, float... vertices) {
		for (int i = 0; i < 12; i += 3) {
			vertexBuffer.put(vertices[i]);
			vertexBuffer.put(vertices[i+1]);
			vertexBuffer.put(vertices[i+2]);
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

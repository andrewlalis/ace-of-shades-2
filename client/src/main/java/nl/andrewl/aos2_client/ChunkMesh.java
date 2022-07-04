package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.Pair;
import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

		long start = System.currentTimeMillis();
		var meshData = generateMesh(chunk);
		long dur = System.currentTimeMillis() - start;
		System.out.println("Generated chunk mesh in " + dur + " ms");
		this.indiciesCount = meshData.second().length;

		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, meshData.first(), GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData.second(), GL_STATIC_DRAW);

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

	private Pair<float[], int[]> generateMesh(Chunk c) {
		List<BlockVertexData> vertexList = new ArrayList<>();
		List<Integer> indexList = new ArrayList<>();
		int idx = 0;
		for (int x = 0; x < Chunk.SIZE; x++) {
			for (int y = 0; y < Chunk.SIZE; y++) {
				for (int z = 0; z < Chunk.SIZE; z++) {
					byte block = c.getBlockAt(x, y, z);
					if (block == 0) continue;
					Vector3f color = Chunk.getColor(block);
					var A = new Vector3f(x, y + 1, z);
					var B = new Vector3f(x, y + 1, z + 1);
					var C = new Vector3f(x + 1, y + 1, z + 1);
					var D = new Vector3f(x + 1, y + 1, z);
					var E = new Vector3f(x, y, z);
					var F = new Vector3f(x, y, z + 1);
					var G = new Vector3f(x + 1, y, z + 1);
					var H = new Vector3f(x + 1, y, z);

					// Top
					if (c.getBlockAt(x, y + 1, z) == 0) {
						var norm = new Vector3f(0, 1, 0);
						vertexList.addAll(Stream.of(A, B, C, D).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx, idx + 1, idx + 3,
								idx + 3, idx + 1, idx + 2
						));
						idx += 4;
					}
					// Bottom
					if (c.getBlockAt(x, y - 1, z) == 0) {
						var norm = new Vector3f(0, -1, 0);
						vertexList.addAll(Stream.of(E, F, G, H).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx + 3, idx + 1, idx,
								idx + 1, idx + 3, idx + 2
						));
						idx += 4;
					}
					// Positive z
					if (c.getBlockAt(x, y, z + 1) == 0) {
						var norm = new Vector3f(0, 0, 1);
						vertexList.addAll(Stream.of(B, F, G, C).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx + 3, idx, idx + 1,
								idx + 3, idx + 1, idx + 2
						));
						idx += 4;
					}
					// Negative z
					if (c.getBlockAt(x, y, z - 1) == 0) {
						var norm = new Vector3f(0, 0, -1);
						vertexList.addAll(Stream.of(A, E, H, D).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx, idx + 3, idx + 2,
								idx + 2, idx + 1, idx
						));
						idx += 4;
					}
					// Positive x
					if (c.getBlockAt(x + 1, y, z) == 0) {
						var norm = new Vector3f(1, 0, 0);
						vertexList.addAll(Stream.of(C, G, H, D).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx + 3, idx, idx + 1,
								idx + 3, idx + 1, idx + 2
						));
						idx += 4;
					}
					// Negative x
					if (c.getBlockAt(x - 1, y, z) == 0) {
						var norm = new Vector3f(-1, 0, 0);
						vertexList.addAll(Stream.of(A, E, F, B).map(v -> new BlockVertexData(v, color, norm)).toList());
						indexList.addAll(List.of(
								idx + 3, idx, idx + 1,
								idx + 3, idx + 1, idx + 2
						));
						idx += 4;
					}
				}
			}
		}

		float[] vertexData = new float[9 * vertexList.size()];
		int vertexDataIdx = 0;
		for (var vertex : vertexList) {
			vertexData[vertexDataIdx++] = vertex.position().x;
			vertexData[vertexDataIdx++] = vertex.position().y;
			vertexData[vertexDataIdx++] = vertex.position().z;
			vertexData[vertexDataIdx++] = vertex.color().x;
			vertexData[vertexDataIdx++] = vertex.color().y;
			vertexData[vertexDataIdx++] = vertex.color().z;
			vertexData[vertexDataIdx++] = vertex.normal().x;
			vertexData[vertexDataIdx++] = vertex.normal().y;
			vertexData[vertexDataIdx++] = vertex.normal().z;
		}
		int[] indexData = indexList.stream().mapToInt(v -> v).toArray();
		System.out.printf("Generated chunk mesh: %d vertices, %d indexes%n", vertexList.size(), indexData.length);

		return new Pair<>(vertexData, indexData);
	}

	public void draw() {
		// Bind elements.
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
		glBindVertexArray(vaoId);

		glDrawElements(GL_TRIANGLES, indiciesCount, GL_UNSIGNED_INT, 0);
	}
}

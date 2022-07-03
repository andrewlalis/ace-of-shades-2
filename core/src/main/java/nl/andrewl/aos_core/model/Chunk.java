package nl.andrewl.aos_core.model;

import nl.andrewl.aos_core.Pair;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Chunk {
	/**
	 * The size of a chunk, in terms of the number of blocks on one axis of the cube.
	 */
	public static final byte SIZE = 16;

	private final byte[] blocks = new byte[SIZE * SIZE * SIZE];

	public byte getBlockAt(int x, int y, int z) {
		int idx = x * SIZE * SIZE + y * SIZE + z;
		if (idx < 0 || idx >= SIZE * SIZE * SIZE) return 0;
		return blocks[x * SIZE * SIZE + y * SIZE + z];
	}

	public byte getBlockAt(Vector3i localPosition) {
		return getBlockAt(localPosition.x, localPosition.y, localPosition.z);
	}

	public byte[] getBlocks() {
		return blocks;
	}

	public Pair<List<Vector3f>, List<Integer>> generateMesh() {
		List<Vector3f> vertexList = new ArrayList<>();
		List<Integer> indexList = new ArrayList<>();
		int elementIdx = 0;
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				for (int z = 0; z < SIZE; z++) {
					byte block = getBlockAt(x, y, z);
					if (block == 0) continue;
					// Top
					if (getBlockAt(x, y + 1, z) == 0) {
						vertexList.add(new Vector3f(x + 1, y, z));       // 0
						vertexList.add(new Vector3f(x, y, z));              // 1
						vertexList.add(new Vector3f(x, y, z + 1));       // 2
						vertexList.add(new Vector3f(x + 1, y, z + 1));// 3

						indexList.add(elementIdx);
						indexList.add(elementIdx + 1);
						indexList.add(elementIdx + 2);

						indexList.add(elementIdx + 2);
						indexList.add(elementIdx + 3);
						indexList.add(elementIdx);

						elementIdx += 4;
					}
					// Bottom
					if (getBlockAt(x, y - 1, z) == 0) {
						vertexList.add(new Vector3f(x + 1, y - 1, z));       // 0
						vertexList.add(new Vector3f(x, y - 1, z));              // 1
						vertexList.add(new Vector3f(x, y - 1, z + 1));       // 2
						vertexList.add(new Vector3f(x + 1, y - 1, z + 1));// 3

						indexList.add(elementIdx);
						indexList.add(elementIdx + 2);
						indexList.add(elementIdx + 1);

						indexList.add(elementIdx);
						indexList.add(elementIdx + 3);
						indexList.add(elementIdx + 2);

						elementIdx += 4;
					}
					// Positive z
					// Negative z
					// Positive x
					// Negative x
				}
			}
		}

		return new Pair<>(vertexList, indexList);
	}

	public static Chunk of(byte value) {
		Chunk c = new Chunk();
		Arrays.fill(c.blocks, value);
		return c;
	}

	public static Vector3f getColor(byte blockValue) {
		float v = blockValue / 128.0f;
		return new Vector3f(v);
	}
}

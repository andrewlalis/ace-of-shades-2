package nl.andrewl.aos_core.model;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Arrays;
import java.util.Random;

public class Chunk {
	/**
	 * The size of a chunk, in terms of the number of blocks on one axis of the cube.
	 */
	public static final int SIZE = 16;
	public static final int TOTAL_SIZE = SIZE * SIZE * SIZE;

	private final byte[] blocks = new byte[TOTAL_SIZE];

	public byte getBlockAt(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return 0;
		int idx = x * SIZE * SIZE + y * SIZE + z;
		return blocks[idx];
	}

	public byte getBlockAt(Vector3i localPosition) {
		return getBlockAt(localPosition.x, localPosition.y, localPosition.z);
	}

	public void setBlockAt(int x, int y, int z, byte value) {
		if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return;
		int idx = x * SIZE * SIZE + y * SIZE + z;
		blocks[idx] = value;
	}

	public byte[] getBlocks() {
		return blocks;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < SIZE; y++) {
			sb.append("y=").append(y).append('\n');
			for (int z = 0; z < SIZE; z++) {
				for (int x = 0; x < SIZE; x++) {
					sb.append(String.format("%02X ", getBlockAt(x, y, z)));
				}
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	public static Chunk of(byte value) {
		Chunk c = new Chunk();
		Arrays.fill(c.blocks, value);
		return c;
	}

	public static Chunk random(Random rand) {
		Chunk c = new Chunk();
		for (int i = 0; i < TOTAL_SIZE; i++) {
			c.blocks[i] = (byte) rand.nextInt(1, 128);
		}
		return c;
	}

	public static Vector3f getColor(byte blockValue) {
		float v = blockValue / 127.0f;
		return new Vector3f(v);
	}
}

package nl.andrewl.aos_core.model;

import net.openhft.hashing.LongHashFunction;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Holds information about a uniform "chunk" of the voxel world, with a cubic
 * shape. Each chunk contains a flattened 1-dimensional array that holds the
 * information about each "block" in the chunk.
 */
public class Chunk {
	/**
	 * The size of a chunk, in terms of the number of blocks on one axis of the cube.
	 */
	public static final int SIZE = 16;

	/**
	 * The total size of the chunk's {@link Chunk#blocks} array.
	 */
	public static final int TOTAL_SIZE = SIZE * SIZE * SIZE;

	private final byte[] blocks = new byte[TOTAL_SIZE];
	private final Vector3i position;

	public Chunk(int cx, int cy, int cz) {
		this.position = new Vector3i(cx, cy, cz);
	}

	public Chunk(int cx, int cy, int cz, byte[] blocks) {
		this(cx, cy, cz);
		System.arraycopy(blocks, 0, this.blocks, 0, TOTAL_SIZE);
	}

	public Chunk(Vector3i position) {
		this.position = new Vector3i(position);
	}

	public Chunk(Chunk other) {
		this(other.position);
		System.arraycopy(other.blocks, 0, this.blocks, 0, TOTAL_SIZE);
	}

	public Vector3i getPosition() {
		return position;
	}

	/**
	 * Converts the given 3D coordinate to a 1D index which points to the block
	 * with that coordinate within the chunk.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 * @return The 1D index, or -1 if out of bounds.
	 */
	public static int xyzToIdx(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return -1;
		return x * SIZE * SIZE + y * SIZE + z;
	}

	/**
	 * Converts the given 1D index to a 3D coordinate that points to the block
	 * with that index.
	 * @param idx The index.
	 * @return The 3D coordinate, or -1, -1, -1 if the index is out of bounds.
	 */
	public static Vector3i idxToXyz(int idx) {
		Vector3i vec = new Vector3i(-1, -1, -1);
		idxToXyz(idx, vec);
		return vec;
	}

	public static void idxToXyz(int idx, Vector3i vec) {
		if (idx < 0 || idx >= TOTAL_SIZE) return;
		vec.x = idx / (SIZE * SIZE);
		int remainder = idx % (SIZE * SIZE);
		vec.y = remainder / SIZE;
		vec.z = remainder % SIZE;
	}

	public byte getBlockAt(int x, int y, int z) {
		int idx = xyzToIdx(x, y, z);
		if (idx < 0) return 0;
		return blocks[idx];
	}

	public byte getBlockAt(Vector3i localPosition) {
		return getBlockAt(localPosition.x, localPosition.y, localPosition.z);
	}

	public void setBlockAt(int x, int y, int z, byte value) {
		int idx = xyzToIdx(x, y, z);
		if (idx < 0) return;
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

	public long blockHash() {
		return LongHashFunction.xx3(0).hashBytes(blocks);
	}

	public static Chunk random(Vector3i position, Random rand) {
		Chunk c = new Chunk(position);
		for (int i = 0; i < TOTAL_SIZE; i++) {
			c.blocks[i] = (byte) rand.nextInt(1, 128);
		}
		return c;
	}

	public static Vector3f getColor(byte blockValue) {
		float v = blockValue / 127.0f;
		return new Vector3f(v);
	}

	public static void getColor(byte blockValue, Vector3f vec) {
		float v = blockValue / 127.0f;
		vec.set(v);
	}
}

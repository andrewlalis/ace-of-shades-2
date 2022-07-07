package nl.andrewl.aos_core.model;

import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.HashMap;
import java.util.Map;

/**
 * A world is just a collection of chunks that together form the environment
 * that players can interact in.
 */
public class World {
	private final Map<Vector3ic, Chunk> chunkMap = new HashMap<>();

	public void addChunk(Chunk chunk) {
		chunkMap.put(chunk.getPosition(), chunk);
	}

	public Map<Vector3ic, Chunk> getChunkMap() {
		return chunkMap;
	}

	public byte getBlockAt(Vector3f pos) {
		Vector3i chunkPos = getChunkPosAt(pos);
		Chunk chunk = chunkMap.get(chunkPos);
		if (chunk == null) return 0;
		Vector3i blockPos = new Vector3i(
				(int) Math.floor(pos.x - chunkPos.x * Chunk.SIZE),
				(int) Math.floor(pos.y - chunkPos.y * Chunk.SIZE),
				(int) Math.floor(pos.z - chunkPos.z * Chunk.SIZE)
		);
		return chunk.getBlockAt(blockPos);
	}

	public void setBlockAt(Vector3f pos, byte block) {
		Vector3i chunkPos = getChunkPosAt(pos);
		Chunk chunk = chunkMap.get(chunkPos);
		if (chunk == null) return;
		Vector3i blockPos = new Vector3i(
				(int) Math.floor(pos.x - chunkPos.x * Chunk.SIZE),
				(int) Math.floor(pos.y - chunkPos.y * Chunk.SIZE),
				(int) Math.floor(pos.z - chunkPos.z * Chunk.SIZE)
		);
		chunk.setBlockAt(blockPos.x, blockPos.y, blockPos.z, block);
	}

	public byte getBlockAt(int x, int y, int z) {
		int chunkX = x / Chunk.SIZE;
		int localX = x % Chunk.SIZE;
		int chunkY = y / Chunk.SIZE;
		int localY = y % Chunk.SIZE;
		int chunkZ = z / Chunk.SIZE;
		int localZ = z % Chunk.SIZE;
		Vector3i chunkPos = new Vector3i(chunkX, chunkY, chunkZ);
		Chunk chunk = chunkMap.get(chunkPos);
		if (chunk == null) return 0;
		return chunk.getBlockAt(localX, localY, localZ);
	}

	public Chunk getChunkAt(Vector3i chunkPos) {
		return chunkMap.get(chunkPos);
	}

	/**
	 * Gets the coordinates of a chunk at a given world position.
	 * @param worldPos The world position.
	 * @return The chunk position. Note that this may not correspond to any existing chunk.
	 */
	public static Vector3i getChunkPosAt(Vector3f worldPos) {
		return new Vector3i(
				(int) Math.floor(worldPos.x / Chunk.SIZE),
				(int) Math.floor(worldPos.y / Chunk.SIZE),
				(int) Math.floor(worldPos.z / Chunk.SIZE)
		);
	}
}

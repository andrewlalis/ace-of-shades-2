package nl.andrewl.aos_core.model;

import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.HashMap;
import java.util.Map;

public class World {
	private final Map<Vector3ic, Chunk> chunkMap = new HashMap<>();

	public void addChunk(Chunk chunk) {
		chunkMap.put(chunk.getPosition(), chunk);
	}

	public Map<Vector3ic, Chunk> getChunkMap() {
		return chunkMap;
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
}

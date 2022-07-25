package nl.andrewl.aos_core.model.world;

import nl.andrewl.aos_core.Directions;
import nl.andrewl.aos_core.MathUtils;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A world is just a collection of chunks that together form the environment
 * that players can interact in.
 */
public class World {
	private static final float DELTA = 0.0001f;

	protected final Map<Vector3ic, Chunk> chunkMap = new HashMap<>();
	protected ColorPalette palette;
	protected final Map<String, Vector3f> spawnPoints = new HashMap<>();

	public World(ColorPalette palette, Collection<Chunk> chunks) {
		this.palette = palette;
		for (var chunk : chunks) addChunk(chunk);
	}

	public World(ColorPalette palette) {
		this(palette, Collections.emptyList());
	}

	public World() {
		this(ColorPalette.rainbow());
	}

	public void addChunk(Chunk chunk) {
		chunkMap.put(chunk.getPosition(), chunk);
	}

	public void removeChunk(Vector3i chunkPos) {
		chunkMap.remove(chunkPos);
	}

	public Map<Vector3ic, Chunk> getChunkMap() {
		return chunkMap;
	}

	public ColorPalette getPalette() {
		return palette;
	}

	public void setPalette(ColorPalette palette) {
		this.palette = palette;
	}

	public byte getBlockAt(Vector3f pos) {
		return getBlockAt(pos, new Vector3i());
	}

	public byte getBlockAt(Vector3f pos, Vector3i util) {
		getChunkPosAt(pos, util);
		Chunk chunk = chunkMap.get(util);
		if (chunk == null) return 0;
		getLocalPosAt(pos.x, pos.y, pos.z, util);
		return chunk.getBlockAt(util);
	}

	public byte getBlockAt(float x, float y, float z) {
		return getBlockAt(new Vector3f(x, y, z));
	}

	public void setBlockAt(Vector3f pos, byte block) {
		Vector3i chunkPos = getChunkPosAt(pos);
		Chunk chunk = chunkMap.get(chunkPos);
		if (chunk == null) return;
		Vector3i blockPos = getLocalPosAt(pos.x, pos.y, pos.z, chunkPos);
		chunk.setBlockAt(blockPos.x, blockPos.y, blockPos.z, block);
	}

	public void setBlockAt(int x, int y, int z, byte block) {
		setBlockAt(new Vector3f(x, y, z), block);
	}

	public Chunk getChunkAt(Vector3i chunkPos) {
		return chunkMap.get(chunkPos);
	}

	public Chunk getChunkAt(int x, int y, int z) {
		return chunkMap.get(new Vector3i(x, y, z));
	}

	public Vector3f getSpawnPoint(String name) {
		return spawnPoints.get(name);
	}

	public void setSpawnPoint(String name, Vector3f location) {
		spawnPoints.put(name, location);
	}

	public void removeSpawnPoint(String name) {
		spawnPoints.remove(name);
	}

	public Map<String, Vector3f> getSpawnPoints() {
		return Collections.unmodifiableMap(spawnPoints);
	}

	/**
	 * Clears all data from the world.
	 */
	public void clear() {
		chunkMap.clear();
		spawnPoints.clear();
	}

	/**
	 * Gets the position that a system is looking at, within a distance limit.
	 * Usually used to determine where a player has interacted/clicked in the
	 * world.
	 * @param eyePos The origin point to look from.
	 * @param eyeDir The direction to look towards. This should be normalized.
	 * @param limit The radius out from the origin to look. Blocks outside this
	 *              limit will not be returned.
	 * @return The location of the block that is looked at, or null if none
	 * could be found.
	 */
	public Hit getLookingAtPos(Vector3f eyePos, Vector3f eyeDir, float limit) {
		if (eyeDir.lengthSquared() == 0 || limit <= 0) return null;
		Vector3f pos = new Vector3f(eyePos);
		while (pos.distance(eyePos) < limit) {
			stepToNextBlock(pos, eyeDir);
			if (getBlockAt(pos) > 0) {
				Vector3i hitPos = new Vector3i(
						(int) Math.floor(pos.x),
						(int) Math.floor(pos.y),
						(int) Math.floor(pos.z)
				);
				Vector3ic hitNorm;

				// Determine the face that was hit based on which face was closest to the hit point.
				float minYDist = Math.abs(pos.y - hitPos.y);
				float maxYDist = Math.abs(pos.y - (hitPos.y + 1));
				float minXDist = Math.abs(pos.x - hitPos.x);
				float maxXDist = Math.abs(pos.x - (hitPos.x + 1));
				float minZDist = Math.abs(pos.z - hitPos.z);
				float maxZDist = Math.abs(pos.z - (hitPos.z + 1));
				float minDist = MathUtils.min(minYDist, maxYDist, minXDist, maxXDist, minZDist, maxZDist);

				if (minDist == maxYDist) hitNorm = Directions.UP;
				else if (minDist == minYDist) hitNorm = Directions.DOWN;
				else if (minDist == maxXDist) hitNorm = Directions.POSITIVE_X;
				else if (minDist == minXDist) hitNorm = Directions.NEGATIVE_X;
				else if (minDist == maxZDist) hitNorm = Directions.POSITIVE_Z;
				else if (minDist == minZDist) hitNorm = Directions.NEGATIVE_Z;
				else {
					hitNorm = Directions.UP;
					System.err.println("Invalid hit!");
				}

				return new Hit(hitPos, hitNorm, pos);
			}
		}
		return null;
	}

	/**
	 * Increments the given position until it hits a new block space, moving in
	 * the specified direction. Note that we move slightly into a block if
	 * needed, to ensure accuracy, so the position may not be a whole number.
	 * @param pos The position.
	 * @param dir The direction to move in.
	 */
	private static void stepToNextBlock(Vector3f pos, Vector3f dir) {
		if (dir.lengthSquared() == 0) return;
		// Find the amount we'd have to multiply dir by to get pos to move to the next block on that axis.
		float factorX = Float.MAX_VALUE;
		float factorY = Float.MAX_VALUE;
		float factorZ = Float.MAX_VALUE;

		if (dir.x != 0) factorX = factorToNextValue(pos.x, dir.x);
		if (dir.y != 0) factorY = factorToNextValue(pos.y, dir.y);
		if (dir.z != 0) factorZ = factorToNextValue(pos.z, dir.z);

		float minFactor = Math.min(factorX, Math.min(factorY, factorZ));
		dir.mulAdd(minFactor, pos, pos);
	}

	private static float factorToNextValue(float n, float dir) {
		if (dir == 0) return Float.MAX_VALUE;
		float nextValue;
		if (dir > 0) {
			nextValue = (Math.ceil(n) == n) ? n + 1 : Math.ceil(n);
		} else {
			nextValue = Math.floor(n) - DELTA;
		}
		float diff = nextValue - n;
		// Testing code!
		if (diff == 0) {
			System.out.printf("n = %.8f, nextValue = %.8f, floor(n) - DELTA = %.8f%n", n, nextValue, Math.floor(n) - DELTA);
			throw new RuntimeException("EEK");
		}
		return Math.abs(diff / dir);
	}

	/**
	 * Gets the chunk position at the specified world position.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 * @param dest The destination vector to place the chunk position in.
	 * @return The destination vector, for method chaining.
	 */
	public static Vector3i getChunkPosAt(float x, float y, float z, Vector3i dest) {
		dest.x = (int) Math.floor(x / Chunk.SIZE);
		dest.y = (int) Math.floor(y / Chunk.SIZE);
		dest.z = (int) Math.floor(z / Chunk.SIZE);
		return dest;
	}

	public static Vector3i getChunkPosAt(Vector3f worldPos, Vector3i dest) {
		return getChunkPosAt(worldPos.x, worldPos.y, worldPos.z, dest);
	}

	public static Vector3i getChunkPosAt(Vector3f worldPos) {
		return getChunkPosAt(worldPos, new Vector3i());
	}

	public static Vector3i getChunkPosAt(Vector3i worldPos) {
		return getChunkPosAt(worldPos.x, worldPos.y, worldPos.z, new Vector3i());
	}

	/**
	 * Gets the chunk-local position at the specified world position.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 * @param dest The destination vector to place the local position in.
	 * @return The destination vector, for method chaining.
	 */
	public static Vector3i getLocalPosAt(float x, float y, float z, Vector3i dest) {
		getChunkPosAt(x, y, z, dest);
		float chunkX = dest.x;
		float chunkY = dest.y;
		float chunkZ = dest.z;
		dest.x = (int) Math.floor(x - chunkX * Chunk.SIZE);
		dest.y = (int) Math.floor(y - chunkY * Chunk.SIZE);
		dest.z = (int) Math.floor(z - chunkZ * Chunk.SIZE);
		return dest;
	}

	public static Vector3i getLocalPosAt(Vector3i worldPos) {
		return getLocalPosAt(worldPos.x, worldPos.y, worldPos.z, new Vector3i());
	}
}

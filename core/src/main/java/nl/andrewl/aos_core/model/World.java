package nl.andrewl.aos_core.model;

import nl.andrewl.aos_core.Directions;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.*;

/**
 * A world is just a collection of chunks that together form the environment
 * that players can interact in.
 */
public class World {
	protected final Map<Vector3ic, Chunk> chunkMap = new HashMap<>();
	protected ColorPalette palette;

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
		Vector3f movement = new Vector3f(); // Pre-allocate this vector.
		while (pos.distance(eyePos) < limit) {
			// Find the coordinates of the next blocks on the x, y, and z axes.
			float stepX = getNextStep(pos.x, eyeDir.x);
			float stepY = getNextStep(pos.y, eyeDir.y);
			float stepZ = getNextStep(pos.z, eyeDir.z);
			// Get the distance from our current position to the next block on the x, y, and z axes.
			float distX = Math.abs(pos.x - stepX);
			float distY = Math.abs(pos.y - stepY);
			float distZ = Math.abs(pos.z - stepZ);
			// Get the factor required to multiply each component by to get to its next step.
			float factorX = Math.abs(distX / eyeDir.x);
			float factorY = Math.abs(distY / eyeDir.y);
			float factorZ = Math.abs(distZ / eyeDir.z);
			float minFactor = Float.MAX_VALUE;
			if (factorX > 0 && factorX < minFactor) minFactor = factorX;
			if (factorY > 0 && factorY < minFactor) minFactor = factorY;
			if (factorZ > 0 && factorZ < minFactor) minFactor = factorZ;
			// We should add dir * lowest factor to step to the first next block.
			movement.set(eyeDir).mul(minFactor);
			pos.add(movement);
			if (getBlockAt(pos) > 0) {
				Vector3f prevPos = new Vector3f(pos).sub(movement);
				Vector3i hitPos = new Vector3i(
						(int) Math.floor(pos.x),
						(int) Math.floor(pos.y),
						(int) Math.floor(pos.z)
				);
				Vector3ic hitNorm = null;

				if (prevPos.y > hitPos.y + 1) hitNorm = Directions.UP;
				else if (prevPos.y < hitPos.y) hitNorm = Directions.DOWN;
				else if (prevPos.x > hitPos.x + 1) hitNorm = Directions.POSITIVE_X;
				else if (prevPos.x < hitPos.x) hitNorm = Directions.NEGATIVE_X;
				else if (prevPos.z > hitPos.z + 1) hitNorm = Directions.POSITIVE_Z;
				else if (prevPos.z < hitPos.z) hitNorm = Directions.NEGATIVE_Z;

				return new Hit(hitPos, hitNorm);
			}
		}
		return null;
	}

	/**
	 * Helper function to find the next whole number, given a current number and
	 * an indication of which direction the number is increasing.
	 * @param n The current number.
	 * @param sign An indication of which way the number is increasing.
	 * @return The next whole number up from the current number.
	 */
	private static float getNextStep(float n, float sign) {
		if (sign > 0) {
			if (Math.ceil(n) == n) {
				return n + 1;
			} else {
				return Math.ceil(n);
			}
		} else if (sign < 0) {
			if (Math.floor(n) == n) {
				return n - 1;
			} else {
				return Math.floor(n);
			}
		}
		return n;
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

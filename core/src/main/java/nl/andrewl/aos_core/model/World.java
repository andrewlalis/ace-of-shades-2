package nl.andrewl.aos_core.model;

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
		util.x = (int) Math.floor(pos.x - util.x * Chunk.SIZE);
		util.y = (int) Math.floor(pos.y - util.y * Chunk.SIZE);
		util.z = (int) Math.floor(pos.z - util.z * Chunk.SIZE);
		return chunk.getBlockAt(util);
	}

	public byte getBlockAt(float x, float y, float z) {
		return getBlockAt(new Vector3f(x, y, z));
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

	public void setBlockAt(int x, int y, int z, byte block) {
		setBlockAt(new Vector3f(x, y, z), block);
	}

	public Chunk getChunkAt(Vector3i chunkPos) {
		return chunkMap.get(chunkPos);
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
	public Vector3i getLookingAtPos(Vector3f eyePos, Vector3f eyeDir, float limit) {
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
				return new Vector3i(
						(int) Math.floor(pos.x),
						(int) Math.floor(pos.y),
						(int) Math.floor(pos.z)
				);
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
	 * Gets the coordinates of a chunk at a given world position.
	 * @param worldPos The world position.
	 * @return The chunk position. Note that this may not correspond to any existing chunk.
	 */
	public static Vector3i getChunkPosAt(Vector3f worldPos) {
		return getChunkPosAt(worldPos, new Vector3i());
	}

	public static Vector3i getChunkPosAt(Vector3f worldPos, Vector3i dest) {
		dest.x = (int) Math.floor(worldPos.x / Chunk.SIZE);
		dest.y = (int) Math.floor(worldPos.y / Chunk.SIZE);
		dest.z = (int) Math.floor(worldPos.z / Chunk.SIZE);
		return dest;
	}
}

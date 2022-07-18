package nl.andrewl.aos_core.net.world;

import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.record_net.Message;
import org.joml.Vector3i;

/**
 * A message that's sent to clients when a block in a chunk is updated.
 * @param cx The chunk x coordinate.
 * @param cy The chunk y coordinate.
 * @param cz The chunk z coordinate.
 * @param lx The local x coordinate in the chunk.
 * @param ly The local y coordinate in the chunk.
 * @param lz The local z coordinate in the chunk.
 * @param newBlock The new block data in the specified position.
 */
public record ChunkUpdateMessage(
		int cx, int cy, int cz,
		int lx, int ly, int lz,
		byte newBlock
) implements Message {
	public static ChunkUpdateMessage fromWorld(Vector3i worldPos, World world) {
		Vector3i chunkPos = World.getChunkPosAt(worldPos);
		Vector3i localPos = World.getLocalPosAt(worldPos);
		return new ChunkUpdateMessage(
				chunkPos.x, chunkPos.y, chunkPos.z,
				localPos.x, localPos.y, localPos.z,
				world.getBlockAt(worldPos.x, worldPos.y, worldPos.z)
		);
	}

	public Vector3i getChunkPos() {
		return new Vector3i(cx, cy, cz);
	}

	public Vector3i getLocalPos() {
		return new Vector3i(lx, ly, lz);
	}
}

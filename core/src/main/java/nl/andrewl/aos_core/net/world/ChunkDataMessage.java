package nl.andrewl.aos_core.net.world;

import nl.andrewl.aos_core.model.world.Chunk;
import nl.andrewl.record_net.Message;

/**
 * A message containing all the information about a chunk, to send to a client.
 */
public record ChunkDataMessage(
		int cx, int cy, int cz,
		byte[] blocks
) implements Message {
	public ChunkDataMessage(Chunk chunk) {
		this(chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z, chunk.getBlocks());
	}

	public Chunk toChunk() {
		return new Chunk(cx, cy, cz, blocks);
	}
}

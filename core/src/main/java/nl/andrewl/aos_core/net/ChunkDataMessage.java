package nl.andrewl.aos_core.net;

import nl.andrewl.aos_core.model.Chunk;
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
}

package nl.andrewl.aos_core.net;

import nl.andrewl.aos_core.model.world.Chunk;
import nl.andrewl.record_net.Message;

/**
 * A message sent by the client, which contains a hash of a chunk, so that the
 * server can determine if it's up-to-date, or if the server needs to send the
 * latest chunk data to the user.
 * @param cx The chunk x coordinate.
 * @param cy The chunk y coordinate.
 * @param cz The chunk z coordinate.
 * @param hash The hash value of the chunk.
 */
public record ChunkHashMessage(
		int cx, int cy, int cz,
		long hash
) implements Message {
	public ChunkHashMessage(Chunk chunk) {
		this(chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z, chunk.blockHash());
	}
}

package nl.andrewl.aos_core.net.udp;

import nl.andrewl.record_net.Message;

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
) implements Message {}

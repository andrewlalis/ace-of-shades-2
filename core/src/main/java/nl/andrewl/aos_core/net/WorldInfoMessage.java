package nl.andrewl.aos_core.net;

import nl.andrewl.aos_core.model.World;
import nl.andrewl.record_net.Message;

/**
 * Message that the server sends to connecting clients with some metadata about
 * the world.
 */
public record WorldInfoMessage(
		float[] palette
) implements Message {
	public WorldInfoMessage(World world) {
		this(world.getPalette().toArray());
	}
}

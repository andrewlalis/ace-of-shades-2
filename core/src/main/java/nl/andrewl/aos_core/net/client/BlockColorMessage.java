package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent when a client is holding a block item stack, and
 * selects a different color. It's also sent to other clients to tell them that
 * the player with the given id has selected a different block color.
 */
public record BlockColorMessage(
		int clientId,
		byte block
) implements Message {}

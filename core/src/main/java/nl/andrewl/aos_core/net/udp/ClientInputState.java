package nl.andrewl.aos_core.net.udp;

import nl.andrewl.record_net.Message;

/**
 * A message that' sent periodically by the client when the player's input
 * changes.
 */
public record ClientInputState(
		int clientId,
		boolean forward,
		boolean backward,
		boolean left,
		boolean right,
		boolean jumping,
		boolean crouching,
		boolean sprinting
) implements Message {}

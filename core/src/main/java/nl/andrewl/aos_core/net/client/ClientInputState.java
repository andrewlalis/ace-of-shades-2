package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent periodically by the client when the player's input
 * changes.
 */
public record ClientInputState(
		int clientId,
		// Movement
		boolean forward,
		boolean backward,
		boolean left,
		boolean right,
		boolean jumping,
		boolean crouching,
		boolean sprinting,

		// Interaction
		boolean hitting, // Usually a "left-click" action.
		boolean interacting, // Usually a "right-click" action.
		int selectedInventoryIndex // The selected index in the player's inventory.
) implements Message {}

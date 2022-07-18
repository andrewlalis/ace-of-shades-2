package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent when a player's selected inventory stack changes.
 * @param index The selected index.
 */
public record InventorySelectedStackMessage(
		int index
) implements Message {}

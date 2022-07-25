package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent to update a client with their player's latest health
 * information.
 * @param health The player's health.
 */
public record ClientHealthMessage(
		float health
) implements Message {}

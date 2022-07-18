package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message sent by clients when they update their player's orientation.
 * @param clientId The client's id.
 * @param x The rotation about the vertical axis.
 * @param y The rotation about the horizontal axis.
 */
public record ClientOrientationState(
		int clientId,
		float x, float y
) implements Message {}

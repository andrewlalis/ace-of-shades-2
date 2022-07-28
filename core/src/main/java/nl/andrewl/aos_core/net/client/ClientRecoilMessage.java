package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that the server sends to clients, to tell them to update their
 * player's orientation according to a recoil event.
 */
public record ClientRecoilMessage(
		float dx, float dy
) implements Message {}

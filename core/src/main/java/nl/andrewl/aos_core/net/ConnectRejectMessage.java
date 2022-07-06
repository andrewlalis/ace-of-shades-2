package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent by the server when a connecting client is rejected.
 * @param reason The reason for the rejection.
 */
public record ConnectRejectMessage(
		String reason
) implements Message {}

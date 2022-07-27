package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message sent by clients when they write a chat message for others to see.
 */
public record ChatWrittenMessage(
		String message
) implements Message {}

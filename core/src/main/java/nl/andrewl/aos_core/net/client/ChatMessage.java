package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent from the server to clients about a message that has
 * appeared in chat.
 */
public record ChatMessage(
		long sentAt,
		String author,
		String message
) implements Message {
	public static ChatMessage announce(String message) {
		return new ChatMessage(System.currentTimeMillis(), "_ANNOUNCE", message);
	}

	public static ChatMessage privateMessage(String message) {
		return new ChatMessage(System.currentTimeMillis(), "_PRIVATE", message);
	}
}

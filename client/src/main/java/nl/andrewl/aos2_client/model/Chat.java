package nl.andrewl.aos2_client.model;

import nl.andrewl.aos_core.net.client.ChatMessage;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Represents the set of all chat messages that this client is aware of.
 */
public class Chat {
	public static final int MAX_HISTORY = 20;
	private final Deque<ChatMessage> messages = new ConcurrentLinkedDeque<>();

	public void chatReceived(ChatMessage msg) {
		messages.addFirst(msg);
		while (messages.size() > MAX_HISTORY) {
			messages.removeLast();
		}
	}

	public List<ChatMessage> getMessages() {
		return new ArrayList<>(messages);
	}
}

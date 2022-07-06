package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

/**
 * The message that's sent by the server to indicate that a connecting client
 * has been accepted and can join the server.
 * @param clientId The client's id.
 */
public record ConnectAcceptMessage (
		int clientId
) implements Message {}

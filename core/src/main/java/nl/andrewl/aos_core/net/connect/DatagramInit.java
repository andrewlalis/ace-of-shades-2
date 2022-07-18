package nl.andrewl.aos_core.net.connect;

import nl.andrewl.record_net.Message;

/**
 * The message that's sent initially by the client, and responded to by the
 * server, when a client is establishing a UDP "connection" to the server.
 * @param clientId The client's id.
 */
public record DatagramInit(
		int clientId
) implements Message {}

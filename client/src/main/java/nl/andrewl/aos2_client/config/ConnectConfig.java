package nl.andrewl.aos2_client.config;

/**
 * The data that's needed by the client to initially establish a connection.
 */
public record ConnectConfig(
		String host,
		int port,
		String username,
		boolean spectator
) {}

package nl.andrewl.aos2_server.cli.ingame;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.model.ServerPlayer;

/**
 * Represents a component for handling a certain type of command.
 */
public interface PlayerCommand {
	void handle(String[] args, ServerPlayer player, ClientCommunicationHandler handler, Server server);
}

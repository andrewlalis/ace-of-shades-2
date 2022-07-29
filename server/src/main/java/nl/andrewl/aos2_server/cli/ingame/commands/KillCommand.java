package nl.andrewl.aos2_server.cli.ingame.commands;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.cli.ingame.PlayerCommand;
import nl.andrewl.aos2_server.model.ServerPlayer;

public class KillCommand implements PlayerCommand {
	@Override
	public void handle(String[] args, ServerPlayer player, ClientCommunicationHandler handler, Server server) {
		server.getPlayerManager().playerKilled(player, null);
	}
}

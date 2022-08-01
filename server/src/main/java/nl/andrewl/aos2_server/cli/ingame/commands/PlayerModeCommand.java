package nl.andrewl.aos2_server.cli.ingame.commands;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.cli.ingame.PlayerCommand;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.PlayerMode;
import nl.andrewl.aos_core.net.client.ChatMessage;
import nl.andrewl.aos_core.net.client.ClientInventoryMessage;

public class PlayerModeCommand implements PlayerCommand {
	@Override
	public void handle(String[] args, ServerPlayer player, ClientCommunicationHandler handler, Server server) {
		if (args.length < 1) {
			handler.sendTcpMessage(ChatMessage.privateMessage("Missing required mode argument."));
			return;
		}
		String modeText = args[0].trim().toUpperCase();
		try {
			PlayerMode mode = PlayerMode.valueOf(modeText);
			server.getPlayerManager().setMode(player, mode);
			handler.sendTcpMessage(new ClientInventoryMessage(player.getInventory()));
			server.getPlayerManager().broadcastUdpMessage(player.getUpdateMessage(System.currentTimeMillis()));
			handler.sendTcpMessage(ChatMessage.privateMessage("Your mode has been updated to " + mode.name() + "."));
		} catch (IllegalArgumentException e) {
			handler.sendTcpMessage(ChatMessage.privateMessage("Invalid mode. Should be NORMAL, CREATIVE, or SPECTATOR."));
		}
	}
}

package nl.andrewl.aos2_server.cli.ingame.commands;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.cli.ingame.PlayerCommand;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.net.client.ChatMessage;

public class KillDeathRatioCommand implements PlayerCommand {
	@Override
	public void handle(String[] args, ServerPlayer player, ClientCommunicationHandler handler, Server server) {
		float killCount = player.getKillCount();
		float deathCount = player.getDeathCount();
		float kd = 0;
		if (deathCount > 0) kd = killCount / deathCount;
		handler.sendTcpMessage(ChatMessage.privateMessage("Your kill/death ratio is %.2f.".formatted(kd)));
	}
}

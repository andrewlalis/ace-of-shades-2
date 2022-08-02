package nl.andrewl.aos2_server.cli.ingame;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.cli.ingame.commands.KillCommand;
import nl.andrewl.aos2_server.cli.ingame.commands.KillDeathRatioCommand;
import nl.andrewl.aos2_server.cli.ingame.commands.PlayerModeCommand;
import nl.andrewl.aos2_server.cli.ingame.commands.TeamsCommand;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.net.client.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerCommandHandler {
	private static final Pattern commandSplitter = Pattern.compile("\"(.*)\"|'(.*)'|(\\S+)");

	private final Server server;
	private final Map<String, PlayerCommand> commands;

	public PlayerCommandHandler(Server server) {
		this.server = server;
		commands = new HashMap<>();
		commands.put("kd", new KillDeathRatioCommand());
		commands.put("kill", new KillCommand());
		commands.put("mode", new PlayerModeCommand());
		commands.put("teams", new TeamsCommand());
	}

	public void handle(String rawCommand, ServerPlayer player, ClientCommunicationHandler handler) {
		Matcher matcher = commandSplitter.matcher(rawCommand);
		List<String> matches = new ArrayList<>();
		while (matcher.find()) {
			for (int i = matcher.groupCount() - 1; i >= 0; i--) {
				String group = matcher.group(i);
				if (group != null) {
					matches.add(group);
					break;
				}
			}
		}
		String mainCommandString = matches.get(0).substring(1).trim().toLowerCase();
		if (!mainCommandString.isBlank()) {
			PlayerCommand command = commands.get(mainCommandString);
			if (command != null) {
				String[] args = new String[matches.size() - 1];
				matches.subList(1, matches.size()).toArray(args);
				command.handle(args, player, handler, server);
			} else {
				handler.sendTcpMessage(ChatMessage.privateMessage("Unknown command: \"%s\".".formatted(mainCommandString)));
			}
		} else {
			handler.sendTcpMessage(ChatMessage.privateMessage("Invalid or missing command."));
		}
	}
}

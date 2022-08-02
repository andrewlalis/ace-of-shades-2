package nl.andrewl.aos2_server.cli.ingame.commands;

import nl.andrewl.aos2_server.ClientCommunicationHandler;
import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.cli.ingame.PlayerCommand;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.net.client.ChatMessage;

import java.util.stream.Collectors;

public class TeamsCommand implements PlayerCommand {
	@Override
	public void handle(String[] args, ServerPlayer player, ClientCommunicationHandler handler, Server server) {
		if (args.length == 0) {
			String teamsString = server.getTeamManager().getTeams().stream()
					.map(Team::getName).collect(Collectors.joining(", "));
			handler.sendTcpMessage(ChatMessage.privateMessage(teamsString));
		} else {
			String cmd = args[0].trim().toLowerCase();
			if (cmd.equals("set")) {
				if (args.length >= 2) {
					String teamIdent = args[1].trim();
					server.getTeamManager().findByIdOrName(teamIdent)
							.ifPresentOrElse(
									team -> server.getPlayerManager().setTeam(player, team),
									() -> handler.sendTcpMessage(ChatMessage.privateMessage("Unknown team."))
							);
				} else {
					handler.sendTcpMessage(ChatMessage.privateMessage("Missing required team identifier."));
				}
			} else {
				handler.sendTcpMessage(ChatMessage.privateMessage("Unknown subcommand."));
			}
		}
	}
}

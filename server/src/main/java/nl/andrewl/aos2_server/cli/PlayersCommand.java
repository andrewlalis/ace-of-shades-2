package nl.andrewl.aos2_server.cli;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.Player;
import picocli.CommandLine;

import java.util.Collection;
import java.util.Comparator;

@CommandLine.Command(
		name = "players",
		description = "Commands for interacting with the players on the server.",
		mixinStandardHelpOptions = true
)
public class PlayersCommand {
	@CommandLine.ParentCommand ServerCli cli;

	@CommandLine.Command(name = "list", description = "Lists all online players.")
	public void list() {
		var playerManager = cli.server.getPlayerManager();
		Collection<ServerPlayer> players = playerManager.getPlayers();
		if (players.isEmpty()) {
			cli.out.println("There are no players connected to the server.");
		} else {
			TablePrinter tp = new TablePrinter(cli.out)
					.drawBorders(true)
					.addLine("Id", "Username", "Health", "Position", "Held Item", "Team");
			players.stream().sorted(Comparator.comparing(Player::getId)).forEachOrdered(player -> {
				tp.addLine(
						player.getId(),
						player.getUsername(),
						String.format("%.2f / 1.00", player.getHealth()),
						String.format("x=%.2f, y=%.2f, z=%.2f", player.getPosition().x, player.getPosition().y, player.getPosition().z),
						player.getInventory().getSelectedItemStack().getType().getName(),
						player.getTeam() == null ? "None" : player.getTeam().getName()
				);
			});
			tp.println();
		}
	}

	@CommandLine.Command(name = "kick", description = "Kicks a player from the server.")
	public void kick(
			@CommandLine.Parameters(paramLabel = "player", description = "The id or name of the player to kick.") String playerIdent
	) {
		cli.server.getPlayerManager().findByIdOrName(playerIdent)
				.ifPresentOrElse(player -> {
					cli.server.getPlayerManager().deregister(player);
				}, () -> cli.out.println("Player not found."));
	}

	@CommandLine.Command(name = "set-team", description = "Sets a player's team.")
	public void setTeam(
			@CommandLine.Parameters(paramLabel = "player", description = "The id or name of the player to set the team of.", index = "0") String playerIdent,
			@CommandLine.Parameters(paramLabel = "team", description = "The id or name of the team to move the player to.", index = "1") String teamIdent
	) {
		cli.server.getPlayerManager().findByIdOrName(playerIdent)
				.ifPresentOrElse(player -> {
					cli.server.getTeamManager().findByIdOrName(teamIdent)
							.ifPresentOrElse(team -> {
								cli.out.println("Not yet implemented.");
							}, () -> cli.out.println("Team not found."));
				}, () -> cli.out.println("Player not found."));
	}
}

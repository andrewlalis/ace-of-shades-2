package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.Team;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

/**
 * Component that manages the teams on a server.
 */
public class TeamManager {
	private int nextTeamId = 1;
	private final Server server;
	private final Map<Integer, Team> teams;

	public TeamManager(Server server) {
		this.server = server;
		this.teams = new HashMap<>();
	}

	public synchronized void addTeam(String name, Vector3f color, String spawnPoint) {
		int id = nextTeamId++;
		teams.put(id, new Team(id, name, color, server.getWorld().getSpawnPoint(spawnPoint)));
	}

	public Team getTeam(int id) {
		return teams.get(id);
	}

	public Optional<Team> findByIdOrName(String ident) {
		for (var team : teams.values()) {
			if (team.getName().equals(ident)) return Optional.of(team);
		}
		for (var team : teams.values()) {// Try again ignoring case.
			if (team.getName().equalsIgnoreCase(ident)) return Optional.of(team);
		}
		try {
			int id = Integer.parseInt(ident);
			for (var team : teams.values()) {
				if (team.getId() == id) return Optional.of(team);
			}
			return Optional.empty();
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public Collection<Team> getTeams() {
		return Collections.unmodifiableCollection(teams.values());
	}

	public Collection<ServerPlayer> getPlayers(Team team) {
		return server.getPlayerManager().getPlayers().stream()
				.filter(p -> p.getTeam().equals(team))
				.toList();
	}

	public Collection<ServerPlayer> getPlayers(int id) {
		Team team = getTeam(id);
		if (team == null) return Collections.emptyList();
		return getPlayers(team);
	}

	public boolean isProtected(Vector3i pos) {
		float prot = server.getConfig().actions.teamSpawnProtection;
		return prot > 0 &&
				getTeams().stream().anyMatch(t -> t.getSpawnPoint().distance(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f) <= prot);
	}

	public boolean isProtected(ServerPlayer player) {
		float prot = server.getConfig().actions.teamSpawnProtection;
		return prot > 0 &&
				getTeams().stream().anyMatch(t -> t.equals(player.getTeam()) && player.getPosition().distance(t.getSpawnPoint()) <= prot);
	}
}

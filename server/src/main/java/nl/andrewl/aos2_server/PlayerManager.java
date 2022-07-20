package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.net.client.PlayerJoinMessage;
import nl.andrewl.aos_core.net.client.PlayerLeaveMessage;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;

/**
 * This component is responsible for managing the set of players connected to
 * the server, and components related to that.
 */
public class PlayerManager {
	private static final Logger log = LoggerFactory.getLogger(PlayerManager.class);

	private final Server server;
	private final Map<Integer, ServerPlayer> players = new HashMap<>();
	private final Map<Integer, ClientCommunicationHandler> clientHandlers = new HashMap<>();
	private int nextClientId = 1;

	public PlayerManager(Server server) {
		this.server = server;
	}

	public synchronized ServerPlayer register(ClientCommunicationHandler handler, String username) {
		ServerPlayer player = new ServerPlayer(nextClientId++, username);
		log.info("Registered player \"{}\" with id {}", player.getUsername(), player.getId());
		players.put(player.getId(), player);
		clientHandlers.put(player.getId(), handler);
		Team team = findBestTeamForNewPlayer();
		if (team != null) {
			player.setTeam(team);
			log.info("Player \"{}\" joined the \"{}\" team.", player.getUsername(), team.getName());
		}
		player.setPosition(getBestSpawnPoint(player));
		// Tell all other players that this one has joined.
		broadcastTcpMessageToAllBut(new PlayerJoinMessage(player), player);
		return player;
	}

	public synchronized void deregister(ServerPlayer player) {
		ClientCommunicationHandler handler = clientHandlers.get(player.getId());
		if (handler != null) handler.shutdown();
		players.remove(player.getId());
		clientHandlers.remove(player.getId());
		broadcastTcpMessage(new PlayerLeaveMessage(player.getId()));
		log.info("Deregistered player \"{}\" with id {}", player.getUsername(), player.getId());
	}

	public synchronized void deregisterAll() {
		Set<ServerPlayer> playersToDeregister = new HashSet<>(getPlayers());
		for (var player : playersToDeregister) {
			deregister(player);
		}
	}

	public ServerPlayer getPlayer(int id) {
		return players.get(id);
	}

	public Collection<ServerPlayer> getPlayers() {
		return Collections.unmodifiableCollection(players.values());
	}

	public ClientCommunicationHandler getHandler(int id) {
		return clientHandlers.get(id);
	}

	public Collection<ClientCommunicationHandler> getHandlers() {
		return Collections.unmodifiableCollection(clientHandlers.values());
	}

	/**
	 * Finds the team that's best suited for adding a new player. This is the
	 * team that has the minimum (or tied for minimum) number of players.
	 * @return The best team to add a player to.
	 */
	private Team findBestTeamForNewPlayer() {
		Team minTeam = null;
		int minCount = Integer.MAX_VALUE;
		for (var team : server.getTeams().values()) {
			int playerCount = (int) players.values().stream()
							.filter(p -> Objects.equals(p.getTeam(), team))
							.count();
			if (playerCount < minCount) {
				minCount = playerCount;
				minTeam = team;
			}
		}
		return minTeam;
	}

	/**
	 * Determines the best location to spawn the given player at. This is
	 * usually the player's team spawn point, if they have a team. Otherwise, a
	 * spawn point is randomly chosen from the world. If no spawnpoint exists in
	 * the world, we resort to 0, 0, 0 as the last option.
	 * @param player The player to spawn.
	 * @return The best location to spawn the player at.
	 */
	private Vector3f getBestSpawnPoint(ServerPlayer player) {
		if (player.getTeam() != null) return player.getTeam().getSpawnPoint();
		return server.getWorld().getSpawnPoints().values().stream().findAny().orElse(new Vector3f(0, 0, 0));
	}

	public void handleUdpInit(DatagramInit init, DatagramPacket packet) {
		var handler = getHandler(init.clientId());
		if (handler != null) {
			handler.setClientUdpPort(packet.getPort());
			handler.sendDatagramPacket(init);
			log.debug("Echoed player \"{}\"'s UDP init packet.", getPlayer(init.clientId()).getUsername());
		}
	}

	public void broadcastTcpMessage(Message msg) {
		for (var handler : getHandlers()) {
			handler.sendTcpMessage(msg);
		}
	}

	public void broadcastTcpMessageToAllBut(Message msg, ServerPlayer player) {
		for (var entry : clientHandlers.entrySet()) {
			if (entry.getKey() != player.getId()) {
				entry.getValue().sendTcpMessage(msg);
			}
		}
	}

	public void broadcastUdpMessage(Message msg) {
		try {
			byte[] data = Net.write(msg);
			DatagramPacket packet = new DatagramPacket(data, data.length);
			for (var handler : getHandlers()) {
				handler.sendDatagramPacket(packet);
			}
		} catch (IOException e) {
			log.warn("An error occurred while broadcasting a UDP message.", e);
		}
	}

	public void broadcastUdpMessageToAllBut(Message msg, ServerPlayer player) {
		try {
			byte[] data = Net.write(msg);
			DatagramPacket packet = new DatagramPacket(data, data.length);
			for (var entry : clientHandlers.entrySet()) {
				if (entry.getKey() != player.getId()) {
					entry.getValue().sendDatagramPacket(packet);
				}
			}
		} catch (IOException e) {
			log.warn("An error occurred while broadcasting a UDP message.", e);
		}
	}
}

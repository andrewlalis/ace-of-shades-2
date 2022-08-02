package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.model.PlayerMode;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.model.item.*;
import nl.andrewl.aos_core.net.client.*;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This component is responsible for managing the set of players connected to
 * the server, and components related to that.
 */
public class PlayerManager {
	private final Server server;
	private final Map<Integer, ServerPlayer> players = new HashMap<>();
	private final Map<Integer, ClientCommunicationHandler> clientHandlers = new HashMap<>();
	private int nextClientId = 1;

	public PlayerManager(Server server) {
		this.server = server;
	}

	public synchronized ServerPlayer register(ClientCommunicationHandler handler, String username, boolean spectator) {
		PlayerMode mode = spectator ? PlayerMode.SPECTATOR : PlayerMode.NORMAL;
		Team team = mode != PlayerMode.NORMAL ? null : findBestTeamForNewPlayer();
		ServerPlayer player = new ServerPlayer(nextClientId++, username, team, mode);
		if (player.getMode() == PlayerMode.NORMAL || player.getMode() == PlayerMode.CREATIVE) {
			var inv = player.getInventory();
			inv.getItemStacks().add(new GunItemStack(ItemTypes.RIFLE));
			inv.getItemStacks().add(new GunItemStack(ItemTypes.AK_47));
			inv.getItemStacks().add(new GunItemStack(ItemTypes.WINCHESTER));
			inv.getItemStacks().add(new BlockItemStack(ItemTypes.BLOCK, 50, (byte) 1));
			inv.setSelectedIndex(0);
		}

		System.out.printf("Registered player \"%s\" with id %d.%n", player.getUsername(), player.getId());
		players.put(player.getId(), player);
		clientHandlers.put(player.getId(), handler);
		String joinMessage;
		if (player.getTeam() != null) {
			System.out.printf("Player \"%s\" joined the \"%s\" team.%n", player.getUsername(), player.getTeam().getName());
			joinMessage = String.format("%s joined the %s team.", username, player.getTeam().getName());
		} else {
			joinMessage = username + " joined the game.";
		}
		player.setPosition(getBestSpawnPoint(player));
		// Tell all other players that this one has joined.
		broadcastTcpMessageToAllBut(new PlayerJoinMessage(
				player.getId(), player.getUsername(), player.getTeam() == null ? -1 : player.getTeam().getId(),
				player.getPosition().x(), player.getPosition().y(), player.getPosition().z(),
				player.getVelocity().x(), player.getVelocity().y(), player.getVelocity().z(),
				player.getOrientation().x(), player.getOrientation().y(),
				player.isCrouching(),
				player.getInventory().getSelectedItemStack() == null ? -1 : player.getInventory().getSelectedItemStack().getType().getId(),
				player.getInventory().getSelectedBlockValue(),
				player.getMode()
		), player);
		if (player.getMode() != PlayerMode.SPECTATOR) {
			broadcastTcpMessageToAllBut(ChatMessage.announce(joinMessage), player);
		}
		return player;
	}

	public synchronized void deregister(ServerPlayer player) {
		ClientCommunicationHandler handler = clientHandlers.get(player.getId());
		if (handler != null) handler.shutdown();
		players.remove(player.getId());
		clientHandlers.remove(player.getId());
		broadcastTcpMessage(new PlayerLeaveMessage(player.getId()));
		System.out.printf("Deregistered player \"%s\" with id %d.%n", player.getUsername(), player.getId());
		broadcastTcpMessage(ChatMessage.announce(player.getUsername() + " left the game."));
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

	public Optional<ServerPlayer> findByIdOrName(String text) {
		Pattern p = Pattern.compile("\\d+");
		if (p.matcher(text).matches() && text.length() < 8) {
			int id = Integer.parseInt(text);
			return Optional.ofNullable(getPlayer(id));
		} else {
			String finalText = text.trim().toLowerCase();
			List<ServerPlayer> matches = getPlayers().stream()
					.filter(player -> player.getUsername().trim().toLowerCase().equals(finalText))
					.toList();
			if (matches.size() == 1) return Optional.of(matches.get(0));
			return Optional.empty();
		}
	}

	public Collection<ServerPlayer> getPlayers() {
		return Collections.unmodifiableCollection(players.values());
	}

	public ClientCommunicationHandler getHandler(int id) {
		return clientHandlers.get(id);
	}

	public ClientCommunicationHandler getHandler(ServerPlayer player) {
		return clientHandlers.get(player.getId());
	}

	public Collection<ClientCommunicationHandler> getHandlers() {
		return Collections.unmodifiableCollection(clientHandlers.values());
	}

	public void tick(long currentTimeMillis, float dt) {
		for (var player : players.values()) {
			player.getActionManager().tick(currentTimeMillis, dt, server.getWorld(), server);
			if (player.getActionManager().isUpdated()) {
				broadcastUdpMessage(player.getUpdateMessage(currentTimeMillis));
			}
		}
	}

	/**
	 * Finds the team that's best suited for adding a new player. This is the
	 * team that has the minimum (or tied for minimum) number of players.
	 * @return The best team to add a player to.
	 */
	private Team findBestTeamForNewPlayer() {
		Team minTeam = null;
		int minCount = Integer.MAX_VALUE;
		for (var team : server.getTeamManager().getTeams()) {
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

	/**
	 * This method is invoked by the server's logic if a player has been
	 * determined to be killed somehow. We will reset their inventory, health,
	 * and respawn them.
	 * @param player The player that died.
	 * @param killedBy The player that killed them. Can be null.
	 */
	public void playerKilled(ServerPlayer player, ServerPlayer killedBy) {
		Vector3f deathPosition = new Vector3f(player.getPosition());
		player.incrementDeathCount();
		broadcastUdpMessage(new SoundMessage("death", 1, deathPosition));
		respawn(player);
		String deathMessage;
		if (killedBy != null) {
			killedBy.incrementKillCount();
			deathMessage = player.getUsername() + " was killed by " + killedBy.getUsername() + ".";
		} else {
			deathMessage = player.getUsername() + " died.";
		}
		broadcastTcpMessage(ChatMessage.announce(deathMessage));
		// TODO: Team points or something.
	}

	public void resupply(ServerPlayer player) {
		var handler = getHandler(player.getId());
		player.setHealth(1);
		for (int i = 0; i < player.getInventory().getItemStacks().size(); i++) {
			ItemStack stack = player.getInventory().getItemStacks().get(i);
			if (stack instanceof GunItemStack g) {
				Gun gun = (Gun) g.getType();
				g.setBulletCount(gun.getMaxBulletCount());
				g.setClipCount(gun.getMaxClipCount());
			} else if (stack instanceof BlockItemStack b) {
				b.setAmount(50);
			}
			handler.sendTcpMessage(new ItemStackMessage(i, stack));
		}
		handler.sendDatagramPacket(new ClientHealthMessage(player.getHealth()));
		handler.sendTcpMessage(ChatMessage.privateMessage("You've been resupplied at your team base."));
	}

	public void respawn(ServerPlayer player) {
		player.setPosition(getBestSpawnPoint(player));
		player.setVelocity(new Vector3f(0));
		broadcastUdpMessage(player.getUpdateMessage(System.currentTimeMillis()));
		resupply(player);
	}

	public void setMode(ServerPlayer player, PlayerMode mode) {
		player.setMode(mode);
		var handler = getHandler(player);
		var inv = player.getInventory();
		inv.clear();
		if (mode == PlayerMode.NORMAL || mode == PlayerMode.CREATIVE) {
			inv.getItemStacks().add(new GunItemStack(ItemTypes.RIFLE));
			inv.getItemStacks().add(new GunItemStack(ItemTypes.AK_47));
			inv.getItemStacks().add(new GunItemStack(ItemTypes.WINCHESTER));
			inv.getItemStacks().add(new BlockItemStack(ItemTypes.BLOCK, 50, (byte) 1));
			inv.setSelectedIndex(0);
			handler.sendTcpMessage(new ClientInventoryMessage(inv));
			broadcastUdpMessage(player.getUpdateMessage(System.currentTimeMillis()));
		}
		if (mode != PlayerMode.NORMAL) {
			player.setTeam(null);
			broadcastTcpMessage(new PlayerTeamUpdateMessage(player.getId(), -1));
		} else {
			player.setTeam(findBestTeamForNewPlayer());
			broadcastTcpMessage(new PlayerTeamUpdateMessage(player.getId(), player.getTeam() == null ? -1 : player.getTeam().getId()));
		}
		handler.sendTcpMessage(ChatMessage.privateMessage("Your mode has been updated to " + mode.name() + "."));
	}

	public void setTeam(ServerPlayer player, Team team) {
		if (Objects.equals(team, player.getTeam()) || player.getMode() != PlayerMode.NORMAL) return;
		player.setTeam(team);
		broadcastUdpMessage(new PlayerTeamUpdateMessage(player.getId(), team == null ? -1 : team.getId()));
		respawn(player);
		String chatMessage;
		if (team != null) {
			chatMessage = "%s has changed to the %s team.".formatted(player.getUsername(), team.getName());
		} else {
			chatMessage = "%s has changed to not be on a team.".formatted(player.getUsername());
		}
		broadcastTcpMessage(ChatMessage.announce(chatMessage));
	}

	public void handleUdpInit(DatagramInit init, DatagramPacket packet) {
		var handler = getHandler(init.clientId());
		if (handler != null) {
			handler.setClientUdpPort(packet.getPort());
			handler.sendDatagramPacket(init);
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
}

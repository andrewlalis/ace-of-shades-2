package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.net.udp.DatagramInit;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;

/**
 * This component is responsible for managing the set of players connected to
 * the server.
 */
public class PlayerManager {
	private static final Logger log = LoggerFactory.getLogger(PlayerManager.class);

	private final Map<Integer, ServerPlayer> players = new HashMap<>();
	private final Map<Integer, ClientCommunicationHandler> clientHandlers = new HashMap<>();
	private int nextClientId = 1;

	public synchronized ServerPlayer register(ClientCommunicationHandler handler, String username) {
		ServerPlayer player = new ServerPlayer(nextClientId++, username);
		players.put(player.getId(), player);
		clientHandlers.put(player.getId(), handler);
		log.info("Registered player \"{}\" with id {}", player.getUsername(), player.getId());
		player.setPosition(new Vector3f(0, 64, 0));
		broadcastUdpMessage(new PlayerUpdateMessage(player));
		return player;
	}

	public synchronized void deregister(ServerPlayer player) {
		ClientCommunicationHandler handler = clientHandlers.get(player.getId());
		if (handler != null) handler.shutdown();
		players.remove(player.getId());
		clientHandlers.remove(player.getId());
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

	public void handleUdpInit(DatagramInit init, DatagramPacket packet) {
		var handler = getHandler(init.clientId());
		if (handler != null) {
			handler.setClientUdpPort(packet.getPort());
			handler.sendDatagramPacket(init);
			log.debug("Echoed player \"{}\"'s UDP init packet.", getPlayer(init.clientId()).getUsername());
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
}

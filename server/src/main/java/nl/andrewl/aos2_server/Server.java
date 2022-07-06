package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.udp.DatagramInit;
import nl.andrewl.record_net.Message;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class Server implements Runnable {
	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;

	private int nextClientId = 1;
	private final Map<Integer, Player> players;
	private final Map<Integer, ClientCommunicationHandler> playerClientHandlers;
	private final World world;

	public Server() throws IOException {
		this.serverSocket = new ServerSocket(24464, 5);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(24464);
		this.datagramSocket.setReuseAddress(true);
		this.players = new HashMap<>();
		this.playerClientHandlers = new HashMap<>();

		// Generate world. TODO: do this elsewhere.
		Random rand = new Random(1);
		this.world = new World();
		for (int x = -5; x <= 5; x++) {
			for (int y = 0; y <= 3; y++) {
				for (int z = -3; z <= 3; z++) {
					Chunk chunk = new Chunk(x, y, z);
					for (int i = 0; i < Chunk.TOTAL_SIZE; i++) {
						chunk.getBlocks()[i] = (byte) rand.nextInt(20, 40);
					}
					world.addChunk(chunk);
				}
			}
		}
	}

	@Override
	public void run() {
		running = true;
		new Thread(new UdpReceiver(datagramSocket, this::handleUdpMessage)).start();
		System.out.println("Started AOS2-Server on TCP/UDP port " + serverSocket.getLocalPort() + "; now accepting connections.");
		while (running) {
			acceptClientConnection();
		}
		for (var player : players.values()) {
			deregisterPlayer(player);
		}
		datagramSocket.close();
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleUdpMessage(Message msg, DatagramPacket packet) {
		// Echo any init message from known clients.
		if (msg instanceof DatagramInit init) {
			var handler = getHandler(init.clientId());
			if (handler != null) {
				handler.setClientUdpPort(packet.getPort());
				handler.sendDatagramPacket(msg);
			}
		}
	}

	public synchronized Player registerPlayer(ClientCommunicationHandler handler, String username) {
		Player player = new Player(nextClientId++, username);
		players.put(player.getId(), player);
		playerClientHandlers.put(player.getId(), handler);
		System.out.println("Registered player " + username + " with id " + player.getId());
		return player;
	}

	public synchronized void deregisterPlayer(Player player) {
		ClientCommunicationHandler handler = playerClientHandlers.get(player.getId());
		handler.shutdown();
		players.remove(player.getId());
		playerClientHandlers.remove(player.getId());
		System.out.println("Deregistered player " + player.getUsername() + " with id " + player.getId());
	}

	public ClientCommunicationHandler getHandler(int id) {
		return playerClientHandlers.get(id);
	}

	public World getWorld() {
		return world;
	}

	private void acceptClientConnection() {
		try {
			Socket clientSocket = serverSocket.accept();
			var handler = new ClientCommunicationHandler(this, clientSocket, datagramSocket);
			ForkJoinPool.commonPool().submit(() -> {
				try {
					handler.establishConnection();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			if (e instanceof SocketException && !this.running && e.getMessage().equalsIgnoreCase("Socket closed")) {
				return; // Ignore this exception, since it is expected on shutdown.
			}
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		new Server().run();
	}
}

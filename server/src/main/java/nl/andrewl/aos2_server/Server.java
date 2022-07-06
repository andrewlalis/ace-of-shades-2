package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.udp.DatagramInit;
import nl.andrewl.record_net.Message;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server implements Runnable {
	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;

	private int nextClientId = 1;
	private final Set<ClientCommunicationHandler> clientHandlers;
	private final Map<Integer, Player> players;
	private final Map<Integer, ClientCommunicationHandler> playerClientHandlers;

	public Server() throws IOException {
		this.serverSocket = new ServerSocket(24464, 5);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(24464);
		this.datagramSocket.setReuseAddress(true);
		this.clientHandlers = new HashSet<>();
		this.players = new HashMap<>();
		this.playerClientHandlers = new HashMap<>();
	}

	@Override
	public void run() {
		running = true;
		new Thread(new UdpReceiver(datagramSocket, this::handleUdpMessage)).start();
		System.out.println("Started AOS2-Server on TCP/UDP port " + serverSocket.getLocalPort() + "; now accepting connections.");
		while (running) {
			acceptClientConnection();
		}
		datagramSocket.close();
		for (var handler : clientHandlers) handler.shutdown();
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

	public ClientCommunicationHandler getHandler(int id) {
		return playerClientHandlers.get(id);
	}

	private void acceptClientConnection() {
		try {
			Socket clientSocket = serverSocket.accept();
			ClientCommunicationHandler handler = new ClientCommunicationHandler(this, clientSocket, datagramSocket);
			clientHandlers.add(handler);
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

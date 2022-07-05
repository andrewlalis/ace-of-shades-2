package nl.andrewl.aos2_server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class Server implements Runnable {
	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;
	private Set<ClientHandler> clientHandlers;

	public static void main(String[] args) throws IOException {
		new Server().run();
	}

	public Server() throws IOException {
		this.serverSocket = new ServerSocket(24464, 5);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(24464);
		this.datagramSocket.setReuseAddress(true);
		this.clientHandlers = new HashSet<>();
	}

	@Override
	public void run() {
		running = true;
		System.out.println("Started AOS2-Server on TCP/UDP port " + serverSocket.getLocalPort() + "; now accepting connections.");
		while (running) {
			acceptClientConnection();
		}
	}

	private void acceptClientConnection() {
		try {
			Socket clientSocket = serverSocket.accept();
			ClientHandler handler = new ClientHandler(this, clientSocket, datagramSocket);
			handler.start();
			clientHandlers.add(handler);
		} catch (IOException e) {
			if (e instanceof SocketException && !this.running && e.getMessage().equalsIgnoreCase("Socket closed")) {
				return; // Ignore this exception, since it is expected on shutdown.
			}
			e.printStackTrace();
		}
	}
}

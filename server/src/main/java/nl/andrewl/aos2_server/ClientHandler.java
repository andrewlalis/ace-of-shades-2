package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.net.ConnectRejectMessage;
import nl.andrewl.aos_core.net.ConnectRequestMessage;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.*;

public class ClientHandler extends Thread {
	private static int nextThreadId = 1;

	private final Server server;
	private final Socket socket;
	private final DatagramSocket datagramSocket;
	private final ExtendedDataInputStream in;
	private final ExtendedDataOutputStream out;

	private volatile boolean running;
	private InetAddress clientAddress;
	private int clientUdpPort;

	public ClientHandler(Server server, Socket socket, DatagramSocket datagramSocket) throws IOException {
		super("aos-client-handler-" + nextThreadId++);
		this.server = server;
		this.socket = socket;
		this.datagramSocket = datagramSocket;
		this.in = Net.getInputStream(socket.getInputStream());
		this.out = Net.getOutputStream(socket.getOutputStream());
	}

	public void shutdown() {
		running = false;
	}

	@Override
	public void run() {
		running = true;
		establishConnection();
		while (running) {
			try {
				Message msg = Net.read(in);
			} catch (SocketException e) {
				if (e.getMessage().equals("Socket closed") | e.getMessage().equals("Connection reset")) {
					shutdown();
				} else {
					e.printStackTrace();
				}
			} catch (EOFException e) {
				shutdown();
			} catch (IOException e) {
				e.printStackTrace();
				shutdown();
			}
		}
	}

	private void establishConnection() {
		try {
			socket.setSoTimeout(1000);
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		boolean connectionEstablished = false;
		int attempts = 0;
		while (!connectionEstablished && attempts < 100) {
			try {
				Message msg = Net.read(in);
				if (msg instanceof ConnectRequestMessage connectMsg) {
					this.clientAddress = socket.getInetAddress();
					this.clientUdpPort = connectMsg.udpPort();
					System.out.println("Player connected: " + connectMsg.username());
					connectionEstablished = true;
					try {
						socket.setSoTimeout(0);
					} catch (SocketException e) {
						throw new RuntimeException(e);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			attempts++;
		}
		if (!connectionEstablished) {
			try {
				Net.write(new ConnectRejectMessage("Too many connect attempts failed."), out);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Player couldn't connect after " + attempts + " attempts. Aborting.");
			shutdown();
		}
	}

	private void sendDatagramPacket(Message msg) {
		try {
			sendDatagramPacket(Net.write(msg));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendDatagramPacket(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientUdpPort);
		sendDatagramPacket(packet);
	}

	private void sendDatagramPacket(DatagramPacket packet) {
		try {
			packet.setAddress(clientAddress);
			packet.setPort(clientUdpPort);
			datagramSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

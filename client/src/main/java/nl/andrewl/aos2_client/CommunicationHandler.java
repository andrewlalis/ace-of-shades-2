package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.net.*;
import nl.andrewl.aos_core.net.udp.DatagramInit;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Class which handles the client's communication with the server. This
 * involves establishing a TCP and UDP connection, and providing generic
 * methods for sending messages and processing those we receive.
 */
public class CommunicationHandler {
	private Socket socket;
	private DatagramSocket datagramSocket;
	private ExtendedDataInputStream in;
	private ExtendedDataOutputStream out;
	private int clientId;

	public int establishConnection(InetAddress address, int port, String username) throws IOException {
		System.out.printf("Connecting to server at %s, port %d, with username \"%s\"...%n", address, port, username);
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
		socket = new Socket(address, port);
		socket.setSoTimeout(1000);
		in = Net.getInputStream(socket.getInputStream());
		out = Net.getOutputStream(socket.getOutputStream());
		Net.write(new ConnectRequestMessage(username), out);
		Message response = Net.read(in);
		socket.setSoTimeout(0);
		if (response instanceof ConnectRejectMessage rejectMessage) {
			throw new IOException("Attempt to connect rejected: " + rejectMessage.reason());
		}
		if (response instanceof ConnectAcceptMessage acceptMessage) {
			this.clientId = acceptMessage.clientId();
			new Thread(new TcpReceiver(in, this::handleMessage)).start();
			establishDatagramConnection();
			new Thread(new UdpReceiver(datagramSocket, this::handleUdpMessage)).start();
			return acceptMessage.clientId();
		} else {
			throw new IOException("Server returned an unexpected message: " + response);
		}
	}

	public void shutdown() {
		try {
			socket.close();
			datagramSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(Message msg) {
		try {
			Net.write(msg, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendDatagramPacket(Message msg) {
		try {
			byte[] data = Net.write(msg);
			DatagramPacket packet = new DatagramPacket(data, data.length, socket.getRemoteSocketAddress());
			datagramSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Establishes a UDP "connection" to the server, after we've already
	 * obtained our {@link CommunicationHandler#clientId} from our TCP
	 * connection. This continuously sends {@link DatagramInit} packets until
	 * the server responds with an echo of that packet.
	 * @throws IOException If an error occurs.
	 */
	private void establishDatagramConnection() throws IOException {
		datagramSocket = new DatagramSocket();
		boolean connectionEstablished = false;
		int attempts = 0;
		while (!connectionEstablished && attempts < 100) {
			sendDatagramPacket(new DatagramInit(clientId));
			byte[] buffer = new byte[UdpReceiver.MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			datagramSocket.receive(packet);
			Message msg = Net.read(buffer);
			if (msg instanceof DatagramInit echo && echo.clientId() == clientId) {
				connectionEstablished = true;
			} else {
				attempts++;
			}
		}
		if (!connectionEstablished) {
			throw new IOException("Could not establish a datagram connection to the server after " + attempts + " attempts.");
		}
		System.out.println("Established datagram communication with the server.");
	}

	private void handleMessage(Message msg) {
		System.out.println("Received message: " + msg);
	}

	private void handleUdpMessage(Message msg, DatagramPacket packet) {
		System.out.println("Received udp message: " + msg);
	}
}

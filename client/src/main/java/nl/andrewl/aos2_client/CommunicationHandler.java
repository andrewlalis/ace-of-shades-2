package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.net.*;
import nl.andrewl.aos_core.net.connect.ConnectAcceptMessage;
import nl.andrewl.aos_core.net.connect.ConnectRejectMessage;
import nl.andrewl.aos_core.net.connect.ConnectRequestMessage;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(CommunicationHandler.class);

	private final Client client;
	private Socket socket;
	private DatagramSocket datagramSocket;
	private ExtendedDataOutputStream out;
	private int clientId;

	public CommunicationHandler(Client client) {
		this.client = client;
	}
	
	public void establishConnection() throws IOException {
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
		InetAddress address = InetAddress.getByName(client.getConfig().serverHost);
		int port = client.getConfig().serverPort;
		String username = client.getConfig().username;
		log.info("Connecting to server at {}, port {}, with username \"{}\"...", address, port, username);

		socket = new Socket(address, port);
		socket.setSoTimeout(1000);
		ExtendedDataInputStream in = Net.getInputStream(socket.getInputStream());
		out = Net.getOutputStream(socket.getOutputStream());
		Net.write(new ConnectRequestMessage(username), out);
		Message response = Net.read(in);
		socket.setSoTimeout(0);
		if (response instanceof ConnectRejectMessage rejectMessage) {
			throw new IOException("Attempt to connect rejected: " + rejectMessage.reason());
		}
		if (response instanceof ConnectAcceptMessage acceptMessage) {
			this.clientId = acceptMessage.clientId();
			client.setPlayer(new ClientPlayer(clientId, username));
			establishDatagramConnection();
			log.info("Connection to server established. My client id is {}.", clientId);
			new Thread(new TcpReceiver(in, client::onMessageReceived)).start();
			new Thread(new UdpReceiver(datagramSocket, (msg, packet) -> client.onMessageReceived(msg))).start();
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
		log.debug("Established datagram communication with the server.");
	}

	public int getClientId() {
		return clientId;
	}
}

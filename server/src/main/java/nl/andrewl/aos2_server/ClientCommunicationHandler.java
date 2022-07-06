package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.net.ConnectAcceptMessage;
import nl.andrewl.aos_core.net.ConnectRejectMessage;
import nl.andrewl.aos_core.net.ConnectRequestMessage;
import nl.andrewl.aos_core.net.TcpReceiver;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ClientCommunicationHandler {
	private final Server server;
	private final Socket socket;
	private final DatagramSocket datagramSocket;
	private final ExtendedDataInputStream in;
	private final ExtendedDataOutputStream out;

	private InetAddress clientAddress;
	private int clientUdpPort;
	private Player player;

	public ClientCommunicationHandler(Server server, Socket socket, DatagramSocket datagramSocket) throws IOException {
		this.server = server;
		this.socket = socket;
		this.datagramSocket = datagramSocket;
		this.in = Net.getInputStream(socket.getInputStream());
		this.out = Net.getOutputStream(socket.getOutputStream());
		establishConnection();
		new Thread(new TcpReceiver(in, this::handleTcpMessage)).start();
	}

	public void shutdown() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setClientUdpPort(int port) {
		this.clientUdpPort = port;
	}

	private void handleTcpMessage(Message msg) {
		System.out.println("Message received from client " + player.getUsername() + ": " + msg);
	}

	private void establishConnection() throws IOException {
		socket.setSoTimeout(1000);
		boolean connectionEstablished = false;
		int attempts = 0;
		while (!connectionEstablished && attempts < 100) {
			try {
				Message msg = Net.read(in);
				if (msg instanceof ConnectRequestMessage connectMsg) {
					// Try to set the TCP timeout back to 0 now that we've got the correct request.
					socket.setSoTimeout(0);
					this.clientAddress = socket.getInetAddress();
					System.out.println("Player connected: " + connectMsg.username());
					connectionEstablished = true;
					this.player = server.registerPlayer(this, connectMsg.username());
					Net.write(new ConnectAcceptMessage(player.getId()), out);
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
			socket.close();
		}
	}

	public void sendDatagramPacket(Message msg) {
		try {
			sendDatagramPacket(Net.write(msg));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendDatagramPacket(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientUdpPort);
		sendDatagramPacket(packet);
	}

	public void sendDatagramPacket(DatagramPacket packet) {
		try {
			packet.setAddress(clientAddress);
			packet.setPort(clientUdpPort);
			datagramSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.net.*;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Component which manages the establishing and maintenance of a connection
 * to a single client. This involves waiting for the client to send their
 * first {@link ConnectRequestMessage}, so that we can respond with either a
 * {@link ConnectRejectMessage} or {@link ConnectAcceptMessage}. If the player
 * is accepted, we proceed to register the player and begin receiving messages
 * from them.
 */
public class ClientCommunicationHandler {
	private static final Logger log = LoggerFactory.getLogger(ClientCommunicationHandler.class);

	private final Server server;
	private final Socket socket;
	private final DatagramSocket datagramSocket;
	private final ExtendedDataInputStream in;
	private final ExtendedDataOutputStream out;

	private InetAddress clientAddress;
	private int clientUdpPort = -1;
	private ServerPlayer player;

	public ClientCommunicationHandler(Server server, Socket socket, DatagramSocket datagramSocket) throws IOException {
		this.server = server;
		this.socket = socket;
		this.datagramSocket = datagramSocket;
		this.in = Net.getInputStream(socket.getInputStream());
		this.out = Net.getOutputStream(socket.getOutputStream());
	}

	public void shutdown() {
		try {
			if (!socket.isClosed()) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used to set UDP port once we know it, since the client first sends their
	 * connection request, then we accept, and <em>then</em> the client begins
	 * the UDP communication.
	 * @param port The client's port.
	 */
	public void setClientUdpPort(int port) {
		this.clientUdpPort = port;
	}

	private void handleTcpMessage(Message msg) {
		log.debug("Received TCP message from client \"{}\": {}", player.getUsername(), msg.toString());
		if (msg instanceof ChunkHashMessage hashMessage) {
			Chunk chunk = server.getWorld().getChunkAt(new Vector3i(hashMessage.cx(), hashMessage.cy(), hashMessage.cz()));
			if (chunk != null && hashMessage.hash() != chunk.blockHash()) {
				sendTcpMessage(new ChunkDataMessage(chunk));
			}
		}
	}

	public void establishConnection() throws IOException {
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
					connectionEstablished = true;
					this.player = server.getPlayerManager().register(this, connectMsg.username());
					Net.write(new ConnectAcceptMessage(player.getId()), out);
					log.debug("Sent connect accept message.");

					for (var chunk : server.getWorld().getChunkMap().values()) {
						sendTcpMessage(new ChunkDataMessage(chunk));
					}

					// Initiate a TCP receiver thread to accept incoming messages from the client.
					TcpReceiver tcpReceiver = new TcpReceiver(in, this::handleTcpMessage)
							.withShutdownHook(() -> server.getPlayerManager().deregister(this.player));
					new Thread(tcpReceiver).start();
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
			log.warn("Player couldn't connect after {} attempts. Aborting connection.", attempts);
			socket.close();
		}
	}

	public void sendTcpMessage(Message msg) {
		try {
			Net.write(msg, out);
		} catch (IOException e) {
			e.printStackTrace();
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
			if (clientUdpPort != -1) {
				packet.setAddress(clientAddress);
				packet.setPort(clientUdpPort);
				datagramSocket.send(packet);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

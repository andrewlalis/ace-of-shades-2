package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.UsernameChecker;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.aos_core.model.world.Chunk;
import nl.andrewl.aos_core.model.world.WorldIO;
import nl.andrewl.aos_core.net.TcpReceiver;
import nl.andrewl.aos_core.net.client.ChatMessage;
import nl.andrewl.aos_core.net.client.ChatWrittenMessage;
import nl.andrewl.aos_core.net.connect.ConnectAcceptMessage;
import nl.andrewl.aos_core.net.connect.ConnectRejectMessage;
import nl.andrewl.aos_core.net.connect.ConnectRequestMessage;
import nl.andrewl.aos_core.net.world.ChunkDataMessage;
import nl.andrewl.aos_core.net.world.ChunkHashMessage;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;
import org.joml.Vector3i;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;

/**
 * Component which manages the establishing and maintenance of a connection
 * to a single client. This involves waiting for the client to send their
 * first {@link ConnectRequestMessage}, so that we can respond with either a
 * {@link ConnectRejectMessage} or {@link ConnectAcceptMessage}. If the player
 * is accepted, we proceed to register the player and begin receiving messages
 * from them.
 */
public class ClientCommunicationHandler {
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
		if (msg instanceof ChunkHashMessage hashMessage) {
			Chunk chunk = server.getWorld().getChunkAt(new Vector3i(hashMessage.cx(), hashMessage.cy(), hashMessage.cz()));
			if (chunk != null && hashMessage.hash() != chunk.blockHash()) {
				sendTcpMessage(new ChunkDataMessage(chunk));
			}
		} else if (msg instanceof ChatWrittenMessage chatWrittenMessage) {
			if (chatWrittenMessage.message().startsWith("/")) {
				server.handleCommand(chatWrittenMessage.message(), player, this);
			} else {
				server.getPlayerManager().broadcastTcpMessage(new ChatMessage(
						System.currentTimeMillis(),
						player.getUsername(),
						chatWrittenMessage.message()
				));
			}
		}
	}

	public void establishConnection() throws IOException {
		socket.setSoTimeout(1000);
		boolean connectionEstablished = false;
		int attempts = 0;
		while (!connectionEstablished && attempts < 10) {
			try {
				Message msg = Net.read(in);
				if (msg instanceof ConnectRequestMessage connectMsg) {
					// Ensure the connection is valid.
					if (!UsernameChecker.isValid(connectMsg.username())) {
						Net.write(new ConnectRejectMessage("Invalid username."), out);
						socket.close();
						return;
					}
					if (server.getPlayerManager().getPlayers().stream().anyMatch(p -> p.getUsername().equals(connectMsg.username()))) {
						Net.write(new ConnectRejectMessage("Username is already taken."), out);
						socket.close();
						return;
					}
					if (server.getPlayerManager().getPlayers().size() >= server.getConfig().maxPlayers) {
						Net.write(new ConnectRejectMessage("Server is full."), out);
						socket.close();
						return;
					}

					// Try to set the TCP timeout back to 0 now that we've got the correct request.
					socket.setSoTimeout(0);
					this.clientAddress = socket.getInetAddress();
					this.player = server.getPlayerManager().register(this, connectMsg.username(), connectMsg.spectator());
					Net.write(new ConnectAcceptMessage(player.getId()), out);
					sendInitialData();
					sendTcpMessage(ChatMessage.privateMessage("Welcome to the server, " + player.getUsername() + "."));
					if (player.getTeam() != null) {
						sendTcpMessage(ChatMessage.privateMessage("You've joined the " + player.getTeam().getName() + " team."));
					}
					// Initiate a TCP receiver thread to accept incoming messages from the client.
					TcpReceiver tcpReceiver = new TcpReceiver(in, this::handleTcpMessage)
							.withShutdownHook(() -> server.getPlayerManager().deregister(this.player));
					new Thread(tcpReceiver).start();
					connectionEstablished = true;
				}
			} catch (SocketTimeoutException e) {
				// Ignore this one, since this will happen if the client doesn't send data properly.
			} catch (IOException e) {
				System.err.println("An IOException occurred while attempting to establish a connection to a client: " + e.getMessage());
			}
			attempts++;
		}
		if (!connectionEstablished) {
			try {
				Net.write(new ConnectRejectMessage("Too many connect attempts failed."), out);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.err.printf("Player couldn't connect after %d attempts. Aborting connection.%n", attempts);
			socket.close();
		}
	}

	public void sendTcpMessage(Message msg) {
		ForkJoinPool.commonPool().submit(() -> {
			synchronized (out) {
				try {
					Net.write(msg, out);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void sendDatagramPacket(Message msg) {
		try {
			sendDatagramPacket(Net.write(msg));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendDatagramPacket(byte[] data) {
		if (clientUdpPort != -1) {
			DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientUdpPort);
			sendDatagramPacket(packet);
		} else {
			System.err.println("Can't send datagram packet because we don't know the client's UDP port yet.");
		}
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

	/**
	 * Dedicated method to send all initial data to the client when they first
	 * connect. We don't use record-net for this, but a custom stream writing
	 * operation to improve efficiency.
	 */
	private void sendInitialData() throws IOException {
		// First world data. We send this in the same format that we'd use for files.
		WorldIO.write(server.getWorld(), out);

		// Team data.
		var teams = server.getTeamManager().getTeams();
		out.writeInt(teams.size());
		for (var team : teams) {
			out.writeInt(team.getId());
			out.writeString(team.getName());
			out.writeFloat(team.getColor().x());
			out.writeFloat(team.getColor().y());
			out.writeFloat(team.getColor().z());
			out.writeFloat(team.getSpawnPoint().x());
			out.writeFloat(team.getSpawnPoint().y());
			out.writeFloat(team.getSpawnPoint().z());
		}

		// Player data.
		var otherPlayers = new LinkedList<>(server.getPlayerManager().getPlayers());
		otherPlayers.remove(player);
		out.writeInt(otherPlayers.size());
		for (var player : otherPlayers) {
			out.writeInt(player.getId());
			out.writeString(player.getUsername());
			if (player.getTeam() == null) {
				out.writeInt(-1);
			} else {
				out.writeInt(player.getTeam().getId());
			}

			out.writeFloat(player.getPosition().x());
			out.writeFloat(player.getPosition().y());
			out.writeFloat(player.getPosition().z());

			out.writeFloat(player.getVelocity().x());
			out.writeFloat(player.getVelocity().y());
			out.writeFloat(player.getVelocity().z());

			out.writeFloat(player.getOrientation().x());
			out.writeFloat(player.getOrientation().y());

			out.writeBoolean(player.isCrouching());
			out.writeInt(player.getInventory().getSelectedItemStack() == null ? -1 : player.getInventory().getSelectedItemStack().getType().getId());
			out.writeByte(player.getInventory().getSelectedBlockValue());
			out.writeInt(player.getMode().ordinal());
		}

		// Send the player's own inventory data.
		out.writeInt(player.getInventory().getItemStacks().size());
		for (var stack : player.getInventory().getItemStacks()) {
			ItemStack.write(stack, out);
		}
		out.writeInt(player.getInventory().getSelectedIndex());

		// Send the player's own player data.
		if (player.getTeam() == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(player.getTeam().getId());
		}
		out.writeFloat(player.getPosition().x());
		out.writeFloat(player.getPosition().y());
		out.writeFloat(player.getPosition().z());

		out.writeInt(player.getMode().ordinal());
	}
}

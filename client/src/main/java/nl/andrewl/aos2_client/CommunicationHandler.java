package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.model.OtherPlayer;
import nl.andrewl.aos_core.Net;
import nl.andrewl.aos_core.model.PlayerMode;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.model.world.WorldIO;
import nl.andrewl.aos_core.net.*;
import nl.andrewl.aos_core.net.connect.ConnectAcceptMessage;
import nl.andrewl.aos_core.net.connect.ConnectRejectMessage;
import nl.andrewl.aos_core.net.connect.ConnectRequestMessage;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;
import org.joml.Vector3f;

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
	private final Client client;
	private Socket socket;
	private DatagramSocket datagramSocket;
	private ExtendedDataOutputStream out;
	private ExtendedDataInputStream in;
	private int clientId;
	private boolean done;

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
		System.out.printf("Connecting to server at %s, port %d, with username \"%s\"...%n", address, port, username);

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
			client.setMyPlayer(new ClientPlayer(clientId, username));
			receiveInitialData();
			establishDatagramConnection();
			new Thread(new TcpReceiver(in, client::onMessageReceived).withShutdownHook(this::shutdown)).start();
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
		} finally {
			done = true;
		}
	}

	public boolean isDone() {
		return done;
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
	}

	public int getClientId() {
		return clientId;
	}

	private void receiveInitialData() throws IOException {
		// Read the world data.
		World world = WorldIO.read(in);
		ClientWorld clientWorld = new ClientWorld();
		clientWorld.setPalette(world.getPalette());
		for (var chunk : world.getChunkMap().values()) {
			clientWorld.addChunk(chunk);
		}
		for (var spawnPoint : world.getSpawnPoints().entrySet()) {
			clientWorld.setSpawnPoint(spawnPoint.getKey(), spawnPoint.getValue());
		}
		client.setWorld(clientWorld);

		// Read the team data.
		int teamCount = in.readInt();
		for (int i = 0; i < teamCount; i++) {
			int id = in.readInt();
			client.getTeams().put(id, new Team(
					id, in.readString(),
					new Vector3f(in.readFloat(), in.readFloat(), in.readFloat()),
					new Vector3f(in.readFloat(), in.readFloat(), in.readFloat())
			));
		}

		// Read player data.
		int playerCount = in.readInt();
		for (int i = 0; i < playerCount; i++) {
			OtherPlayer player = new OtherPlayer(in.readInt(), in.readString());
			int teamId = in.readInt();
			if (teamId != -1) player.setTeam(client.getTeams().get(teamId));
			System.out.println(teamId);
			player.getPosition().set(in.readFloat(), in.readFloat(), in.readFloat());
			player.getVelocity().set(in.readFloat(), in.readFloat(), in.readFloat());
			player.getOrientation().set(in.readFloat(), in.readFloat());
			player.setCrouching(in.readBoolean());
			player.setHeldItemId(in.readInt());
			player.setSelectedBlockValue(in.readByte());
			player.setMode(PlayerMode.values()[in.readInt()]);
			client.getPlayers().put(player.getId(), player);
		}

		// Read inventory data.
		int itemStackCount = in.readInt();
		var inv = client.getMyPlayer().getInventory();
		for (int i = 0; i < itemStackCount; i++) {
			inv.getItemStacks().add(ItemStack.read(in));
		}
		inv.setSelectedIndex(in.readInt());

		// Read our own player data.
		int teamId = in.readInt();
		if (teamId != -1) client.getMyPlayer().setTeam(client.getTeams().get(teamId));
		client.getMyPlayer().getPosition().set(in.readFloat(), in.readFloat(), in.readFloat());
		client.getMyPlayer().setMode(PlayerMode.values()[in.readInt()]);
	}
}

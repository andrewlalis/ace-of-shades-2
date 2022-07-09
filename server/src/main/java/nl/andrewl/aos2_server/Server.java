package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.model.WorldIO;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.udp.ClientInputState;
import nl.andrewl.aos_core.net.udp.ClientOrientationState;
import nl.andrewl.aos_core.net.udp.DatagramInit;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class Server implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;

	private final PlayerManager playerManager;
	private final World world;
	private final WorldUpdater worldUpdater;

	public Server() throws IOException {
		this.serverSocket = new ServerSocket(24464, 5);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(24464);
		this.datagramSocket.setReuseAddress(true);
		this.playerManager = new PlayerManager();
		this.worldUpdater = new WorldUpdater(this, 20);

		// Generate world. TODO: do this elsewhere.
		Random rand = new Random(1);
		this.world = new World();
		for (int x = -5; x <= 5; x++) {
			for (int y = 0; y <= 5; y++) {
				for (int z = -3; z <= 3; z++) {
					Chunk chunk = new Chunk(x, y, z);
					if (y <= 3) {
						for (int i = 0; i < Chunk.TOTAL_SIZE; i++) {
							chunk.getBlocks()[i] = (byte) rand.nextInt(20, 40);
						}
					}
					world.addChunk(chunk);
				}
			}
		}
		world.setBlockAt(new Vector3f(5, 64, 5), (byte) 50);
		world.setBlockAt(new Vector3f(5, 64, 6), (byte) 50);
		world.setBlockAt(new Vector3f(5, 64, 7), (byte) 50);
		world.setBlockAt(new Vector3f(5, 65, 6), (byte) 50);
		world.setBlockAt(new Vector3f(5, 66, 7), (byte) 50);
		world.setBlockAt(new Vector3f(5, 65, 7), (byte) 50);
		world.setBlockAt(new Vector3f(5, 67, 8), (byte) 50);
		world.setBlockAt(new Vector3f(6, 67, 8), (byte) 50);
		world.setBlockAt(new Vector3f(7, 67, 8), (byte) 50);
		world.setBlockAt(new Vector3f(5, 67, 9), (byte) 50);
		world.setBlockAt(new Vector3f(6, 67, 9), (byte) 50);
		world.setBlockAt(new Vector3f(7, 67, 9), (byte) 50);

		for (int z = 0; z > -20; z--) {
			world.setBlockAt(new Vector3f(0, 63, z), (byte) 120);
		}

		for (int x = 0; x < 10; x++) {
			world.setBlockAt(new Vector3f(x - 5, 64, 3), (byte) 80);
			world.setBlockAt(new Vector3f(x - 5, 65, 3), (byte) 80);
			world.setBlockAt(new Vector3f(x - 5, 66, 3), (byte) 80);
		}

		for (int z = 0; z < 10; z++) {
			world.setBlockAt(new Vector3f(20, 64, z), (byte) 80);
			world.setBlockAt(new Vector3f(20, 65, z), (byte) 80);
			world.setBlockAt(new Vector3f(20, 66, z), (byte) 80);
		}
		world.setBlockAt(new Vector3f(21, 64, 6), (byte) 1);

		for (int x = 0; x < 127; x++) {
			world.setBlockAt(new Vector3f(x - 50, 63, -15), (byte) x);
		}

		WorldIO.write(world, Path.of("testworld"));
//		this.world = WorldIO.read(Path.of("testworld"));
	}

	@Override
	public void run() {
		running = true;
		new Thread(new UdpReceiver(datagramSocket, this::handleUdpMessage)).start();
		new Thread(worldUpdater).start();
		log.info("Started AoS2 Server on TCP/UDP port {}; now accepting connections.", serverSocket.getLocalPort());
		while (running) {
			acceptClientConnection();
		}
		playerManager.deregisterAll();
		worldUpdater.shutdown();
		datagramSocket.close(); // Shuts down the UdpReceiver.
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleUdpMessage(Message msg, DatagramPacket packet) {
		if (msg instanceof DatagramInit init) {
			playerManager.handleUdpInit(init, packet);
		} else if (msg instanceof ClientInputState inputState) {
			ServerPlayer player = playerManager.getPlayer(inputState.clientId());
			if (player != null) {
				player.setLastInputState(inputState);
			}
		} else if (msg instanceof ClientOrientationState orientationState) {
			ServerPlayer player = playerManager.getPlayer(orientationState.clientId());
			if (player != null) {
				player.setOrientation(orientationState.x(), orientationState.y());
				playerManager.broadcastUdpMessageToAllBut(new PlayerUpdateMessage(player), player);
			}
		}
	}

	private void acceptClientConnection() {
		try {
			Socket clientSocket = serverSocket.accept();
			var handler = new ClientCommunicationHandler(this, clientSocket, datagramSocket);
			// Establish the connection in a separate thread so that we can continue accepting clients.
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

	public World getWorld() {
		return world;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public static void main(String[] args) throws IOException {
		new Server().run();
	}
}

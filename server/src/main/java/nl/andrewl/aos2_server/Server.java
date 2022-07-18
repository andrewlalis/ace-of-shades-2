package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.logic.WorldUpdater;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.model.world.Worlds;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.client.ClientInputState;
import nl.andrewl.aos_core.net.client.ClientOrientationState;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Server implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;
	private final ServerConfig config;
	private final PlayerManager playerManager;
	private final World world;
	private final WorldUpdater worldUpdater;

	public Server(ServerConfig config) throws IOException {
		this.config = config;
		this.serverSocket = new ServerSocket(config.port, config.connectionBacklog);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(config.port);
		this.datagramSocket.setReuseAddress(true);
		this.playerManager = new PlayerManager();
		this.worldUpdater = new WorldUpdater(this, config.ticksPerSecond);
		this.world = Worlds.testingWorld();
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
				if (player.getActionManager().setLastInputState(inputState)) {
					playerManager.broadcastUdpMessage(player.getUpdateMessage());
				}
			}
		} else if (msg instanceof ClientOrientationState orientationState) {
			ServerPlayer player = playerManager.getPlayer(orientationState.clientId());
			if (player != null) {
				player.setOrientation(orientationState.x(), orientationState.y());
				playerManager.broadcastUdpMessageToAllBut(player.getUpdateMessage(), player);
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
				} catch (Exception e) {
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

	public ServerConfig getConfig() {
		return config;
	}

	public World getWorld() {
		return world;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public static void main(String[] args) throws IOException {
		List<Path> configPaths = Config.getCommonConfigPaths();
		if (args.length > 0) {
			configPaths.add(Path.of(args[0].trim()));
		}
		ServerConfig cfg = Config.loadConfig(ServerConfig.class, configPaths, new ServerConfig());
		new Server(cfg).run();
	}
}

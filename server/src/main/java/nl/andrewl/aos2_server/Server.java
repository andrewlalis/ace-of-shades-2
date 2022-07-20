package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.logic.WorldUpdater;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.model.world.Worlds;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.client.ClientInputState;
import nl.andrewl.aos_core.net.client.ClientOrientationState;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class Server implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;
	private final ServerConfig config;
	private final PlayerManager playerManager;
	private final Map<Integer, Team> teams;
	private final Map<Team, String> teamSpawnPoints;
	private final World world;
	private final WorldUpdater worldUpdater;

	public Server(ServerConfig config) throws IOException {
		this.config = config;
		this.serverSocket = new ServerSocket(config.port, config.connectionBacklog);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(config.port);
		this.datagramSocket.setReuseAddress(true);
		this.playerManager = new PlayerManager(this);
		this.worldUpdater = new WorldUpdater(this, config.ticksPerSecond);
		this.world = Worlds.testingWorld();
		this.teams = new HashMap<>();
		this.teamSpawnPoints = new HashMap<>();
		// TODO: Add some way to configure teams with config files.
		teams.put(1, new Team(1, "Red", new Vector3f(0.8f, 0, 0), world.getSpawnPoint("first")));
		teams.put(2, new Team(2, "Blue", new Vector3f(0, 0, 0.8f), world.getSpawnPoint("first")));
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
		long now = System.currentTimeMillis();
		if (msg instanceof DatagramInit init) {
			playerManager.handleUdpInit(init, packet);
		} else if (msg instanceof ClientInputState inputState) {
			ServerPlayer player = playerManager.getPlayer(inputState.clientId());
			if (player != null) {
				if (player.getActionManager().setLastInputState(inputState)) {
					playerManager.broadcastUdpMessage(player.getUpdateMessage(now));
				}
			}
		} else if (msg instanceof ClientOrientationState orientationState) {
			ServerPlayer player = playerManager.getPlayer(orientationState.clientId());
			if (player != null) {
				player.setOrientation(orientationState.x(), orientationState.y());
				playerManager.broadcastUdpMessageToAllBut(player.getUpdateMessage(now), player);
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

	public Map<Integer, Team> getTeams() {
		return teams;
	}

	public String getSpawnPoint(Team team) {
		return teamSpawnPoints.get(team);
	}

	public Collection<ServerPlayer> getPlayersInTeam(Team team) {
		return playerManager.getPlayers().stream().filter(p -> Objects.equals(p.getTeam(), team)).toList();
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

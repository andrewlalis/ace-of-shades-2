package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.cli.ServerCli;
import nl.andrewl.aos2_server.cli.ingame.PlayerCommandHandler;
import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.logic.WorldUpdater;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.model.world.WorldIO;
import nl.andrewl.aos_core.model.world.Worlds;
import nl.andrewl.aos_core.net.UdpReceiver;
import nl.andrewl.aos_core.net.client.BlockColorMessage;
import nl.andrewl.aos_core.net.client.ClientInputState;
import nl.andrewl.aos_core.net.client.ClientOrientationState;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The central server, which mainly contains all the different managers and
 * other components that make up the server's state and logic.
 */
public class Server implements Runnable {
	private final ServerSocket serverSocket;
	private final DatagramSocket datagramSocket;
	private volatile boolean running;
	private final ServerConfig config;
	private final PlayerManager playerManager;
	private final TeamManager teamManager;
	private final ProjectileManager projectileManager;
	private final PlayerCommandHandler commandHandler;
	private final World world;
	private final WorldUpdater worldUpdater;

	public Server(ServerConfig config) throws IOException {
		this.config = config;
		this.serverSocket = new ServerSocket(config.port, config.connectionBacklog);
		this.serverSocket.setReuseAddress(true);
		this.datagramSocket = new DatagramSocket(config.port);
		this.datagramSocket.setReuseAddress(true);
		this.playerManager = new PlayerManager(this);
		this.teamManager = new TeamManager(this);
		this.projectileManager = new ProjectileManager(this);
		this.commandHandler = new PlayerCommandHandler(this);
		this.worldUpdater = new WorldUpdater(this, config.ticksPerSecond);

		if (config.world.startsWith("worlds.")) {
			String worldName = config.world.substring("worlds.".length());
			this.world = switch (worldName) {
				case "testing" -> Worlds.testingWorld();
				case "flat" -> Worlds.flatWorld();
				case "cube" -> Worlds.smallCube();
				case "arena" -> Worlds.arena();
				default -> WorldIO.read(FileUtils.getClasspathResource("redfort.wld"));
			};
		} else {
			Path worldFile = Path.of(config.world);
			if (Files.isReadable(worldFile)) {
				this.world = WorldIO.read(worldFile);
			} else {
				System.err.println("Cannot read world file: " + worldFile.toAbsolutePath());
				this.world = Worlds.arena();
			}
		}

		for (var teamConfig : config.teams) {
			teamManager.addTeam(teamConfig.name, new Vector3f(teamConfig.color), teamConfig.spawnPoint);
		}
	}

	@Override
	public void run() {
		running = true;
		new Thread(new UdpReceiver(datagramSocket, this::handleUdpMessage)).start();
		new Thread(worldUpdater).start();
		ScheduledExecutorService executorService = null;
		if (config.registries != null && config.registries.length > 0) {
			executorService = Executors.newSingleThreadScheduledExecutor();
			var registryUpdater = new RegistryUpdater(this);
			executorService.scheduleAtFixedRate(registryUpdater::sendUpdates, 0, 30, TimeUnit.SECONDS);
		}
		System.out.printf("Started AoS2 Server on TCP/UDP port %d; now accepting connections.%n", serverSocket.getLocalPort());
		while (running) {
			acceptClientConnection();
		}
		System.out.println("Shutting down the server.");
		if (executorService != null) executorService.shutdown();
		playerManager.deregisterAll();
		worldUpdater.shutdown();
		datagramSocket.close(); // Shuts down the UdpReceiver.
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void shutdown() {
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		} else if (msg instanceof BlockColorMessage blockColorMessage) {
			ServerPlayer player = playerManager.getPlayer(blockColorMessage.clientId());
			if (player != null && player.getInventory().getSelectedItemStack() instanceof BlockItemStack stack) {
				stack.setSelectedValue(blockColorMessage.block());
				playerManager.broadcastUdpMessageToAllBut(blockColorMessage, player);
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

	public TeamManager getTeamManager() {
		return teamManager;
	}

	public ProjectileManager getProjectileManager() {
		return projectileManager;
	}

	public void handleCommand(String cmd, ServerPlayer player, ClientCommunicationHandler handler) {
		commandHandler.handle(cmd, player, handler);
	}

	public static void main(String[] args) throws IOException {
		List<Path> configPaths = Config.getCommonConfigPaths();
		configPaths.add(0, Path.of("server.yaml"));
		if (args.length > 0) {
			configPaths.add(Path.of(args[0].trim()));
		}
		ServerConfig cfg = Config.loadConfig(ServerConfig.class, configPaths, new ServerConfig(), "default-config.yaml");
		Server server = new Server(cfg);
		new Thread(server).start();
		ServerCli.start(server);
	}
}

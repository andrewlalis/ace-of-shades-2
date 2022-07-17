package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.config.ClientConfig;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.control.PlayerInputKeyCallback;
import nl.andrewl.aos2_client.control.PlayerInputMouseClickCallback;
import nl.andrewl.aos2_client.control.PlayerViewCursorCallback;
import nl.andrewl.aos2_client.render.GameRenderer;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.world.ColorPalette;
import nl.andrewl.aos_core.net.*;
import nl.andrewl.aos_core.net.udp.ChunkUpdateMessage;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import nl.andrewl.record_net.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Client implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Client.class);
	public static final double FPS = 60;

	private final ClientConfig config;
	private final CommunicationHandler communicationHandler;
	private final InputHandler inputHandler;
	private final GameRenderer gameRenderer;

	private int clientId;
	private final ClientWorld world;

	public Client(ClientConfig config) {
		this.config = config;
		this.communicationHandler = new CommunicationHandler(this);
		this.inputHandler = new InputHandler(communicationHandler);
		this.world = new ClientWorld();
		this.gameRenderer = new GameRenderer(config.display, world);
	}

	public ClientConfig getConfig() {
		return config;
	}

	@Override
	public void run() {
		try {
			this.clientId = communicationHandler.establishConnection();
		} catch (IOException e) {
			log.error("Couldn't connect to the server: {}", e.getMessage());
			return;
		}

		gameRenderer.setupWindow(
				new PlayerViewCursorCallback(config.input, gameRenderer.getCamera(), communicationHandler),
				new PlayerInputKeyCallback(inputHandler),
				new PlayerInputMouseClickCallback(inputHandler)
		);

		long lastFrameAt = System.currentTimeMillis();
		while (!gameRenderer.windowShouldClose()) {
			long now = System.currentTimeMillis();
			float dt = (now - lastFrameAt) / 1000f;
			world.processQueuedChunkUpdates();
			gameRenderer.getCamera().interpolatePosition(dt);
			world.interpolatePlayers(dt);
			gameRenderer.draw();
			lastFrameAt = now;
		}
		gameRenderer.freeWindow();
		communicationHandler.shutdown();
	}

	public void onMessageReceived(Message msg) {
		if (msg instanceof WorldInfoMessage worldInfo) {
			world.setPalette(ColorPalette.fromArray(worldInfo.palette()));
		}
		if (msg instanceof ChunkDataMessage chunkDataMessage) {
			world.addChunk(chunkDataMessage);
		}
		if (msg instanceof ChunkUpdateMessage u) {
			world.updateChunk(u);
			// If we received an update for a chunk we don't have, request it!
			if (world.getChunkAt(u.getChunkPos()) == null) {
				communicationHandler.sendMessage(new ChunkHashMessage(u.cx(), u.cy(), u.cz(), -1));
			}
		}
		if (msg instanceof PlayerUpdateMessage playerUpdate) {
			if (playerUpdate.clientId() == clientId) {
				float eyeHeight = playerUpdate.crouching() ? 1.3f : 1.7f;
				gameRenderer.getCamera().setPosition(playerUpdate.px(), playerUpdate.py() + eyeHeight, playerUpdate.pz());
				gameRenderer.getCamera().setVelocity(playerUpdate.vx(), playerUpdate.vy(), playerUpdate.vz());
			} else {
				world.playerUpdated(playerUpdate);
			}
		}
		if (msg instanceof PlayerJoinMessage joinMessage) {
			world.playerJoined(joinMessage);
		}
		if (msg instanceof PlayerLeaveMessage leaveMessage) {
			world.playerLeft(leaveMessage);
		}
	}


	public static void main(String[] args) throws IOException {
		List<Path> configPaths = Config.getCommonConfigPaths();
		if (args.length > 0) {
			configPaths.add(Path.of(args[0].trim()));
		}
		ClientConfig clientConfig = Config.loadConfig(ClientConfig.class, configPaths, new ClientConfig());
		Client client = new Client(clientConfig);
		client.run();
	}
}

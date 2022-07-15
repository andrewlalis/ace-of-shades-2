package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.control.PlayerInputKeyCallback;
import nl.andrewl.aos2_client.control.PlayerInputMouseClickCallback;
import nl.andrewl.aos2_client.control.PlayerViewCursorCallback;
import nl.andrewl.aos2_client.render.GameRenderer;
import nl.andrewl.aos_core.model.Chunk;
import nl.andrewl.aos_core.model.ColorPalette;
import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.net.ChunkDataMessage;
import nl.andrewl.aos_core.net.ChunkHashMessage;
import nl.andrewl.aos_core.net.WorldInfoMessage;
import nl.andrewl.aos_core.net.udp.ChunkUpdateMessage;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import nl.andrewl.record_net.Message;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class Client implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Client.class);
	public static final double FPS = 60;

	private final InetAddress serverAddress;
	private final int serverPort;
	private final String username;

	private final CommunicationHandler communicationHandler;
	private final InputHandler inputHandler;
	private final GameRenderer gameRenderer;

	private int clientId;
	private final ClientWorld world;

	public Client(InetAddress serverAddress, int serverPort, String username) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.username = username;
		this.communicationHandler = new CommunicationHandler(this);
		this.inputHandler = new InputHandler(communicationHandler);
		this.world = new ClientWorld();
		this.gameRenderer = new GameRenderer(world);
	}

	@Override
	public void run() {
		try {
			log.debug("Connecting to server at {}, port {}, with username \"{}\"...", serverAddress, serverPort, username);
			this.clientId = communicationHandler.establishConnection(serverAddress, serverPort, username);
			log.info("Established a connection to the server.");
		} catch (IOException e) {
			log.error("Couldn't connect to the server: {}", e.getMessage());
			return;
		}

		gameRenderer.setupWindow(
				new PlayerViewCursorCallback(gameRenderer.getCamera(), communicationHandler),
				new PlayerInputKeyCallback(inputHandler),
				new PlayerInputMouseClickCallback(inputHandler)
		);

		long lastFrameAt = System.currentTimeMillis();
		while (!gameRenderer.windowShouldClose()) {
			long now = System.currentTimeMillis();
			float dt = (now - lastFrameAt) / 1000f;
			gameRenderer.getCamera().interpolatePosition(dt);
			gameRenderer.draw();
			lastFrameAt = now;
		}
		gameRenderer.freeWindow();
		communicationHandler.shutdown();
	}

	public int getClientId() {
		return clientId;
	}

	public World getWorld() {
		return world;
	}

	public void onMessageReceived(Message msg) {
		if (msg instanceof WorldInfoMessage worldInfo) {
			world.setPalette(ColorPalette.fromArray(worldInfo.palette()));
		}
		if (msg instanceof ChunkDataMessage chunkDataMessage) {
			Chunk chunk = chunkDataMessage.toChunk();
			world.addChunk(chunk);
			gameRenderer.getChunkRenderer().queueChunkMesh(chunk);
		}
		if (msg instanceof ChunkUpdateMessage u) {
			Vector3i chunkPos = new Vector3i(u.cx(), u.cy(), u.cz());
			Chunk chunk = world.getChunkAt(chunkPos);
			System.out.println(u);
			if (chunk != null) {
				chunk.setBlockAt(u.lx(), u.ly(), u.lz(), u.newBlock());
				gameRenderer.getChunkRenderer().queueChunkMesh(chunk);
			} else {
				communicationHandler.sendMessage(new ChunkHashMessage(u.cx(), u.cy(), u.cz(), -1));
			}
		}
		if (msg instanceof PlayerUpdateMessage playerUpdate) {
			if (playerUpdate.clientId() == clientId) {
				float eyeHeight = playerUpdate.crouching() ? 1.3f : 1.7f;
				gameRenderer.getCamera().setPosition(playerUpdate.px(), playerUpdate.py() + eyeHeight, playerUpdate.pz());
				gameRenderer.getCamera().setVelocity(playerUpdate.vx(), playerUpdate.vy(), playerUpdate.vz());
				// TODO: Unload far away chunks and request close chunks we don't have.
			}
		}
	}


	public static void main(String[] args) throws IOException {
		InetAddress serverAddress = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		String username = args[2].trim();

		Client client = new Client(serverAddress, serverPort, username);
		client.run();
	}
}

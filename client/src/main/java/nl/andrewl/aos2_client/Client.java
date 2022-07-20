package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.config.ClientConfig;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.control.PlayerInputKeyCallback;
import nl.andrewl.aos2_client.control.PlayerInputMouseClickCallback;
import nl.andrewl.aos2_client.control.PlayerViewCursorCallback;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.model.OtherPlayer;
import nl.andrewl.aos2_client.render.GameRenderer;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.net.client.*;
import nl.andrewl.aos_core.net.world.ChunkDataMessage;
import nl.andrewl.aos_core.net.world.ChunkHashMessage;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Client.class);
	public static final double FPS = 60;

	private final ClientConfig config;
	private final CommunicationHandler communicationHandler;
	private final InputHandler inputHandler;
	private GameRenderer gameRenderer;
	private long lastPlayerUpdate = 0;

	private ClientWorld world;
	private ClientPlayer myPlayer;
	private final Map<Integer, OtherPlayer> players;
	private Map<Integer, Team> teams;

	public Client(ClientConfig config) {
		this.config = config;
		this.players = new ConcurrentHashMap<>();
		this.teams = new ConcurrentHashMap<>();
		this.communicationHandler = new CommunicationHandler(this);
		this.inputHandler = new InputHandler(this, communicationHandler);
	}

	public ClientConfig getConfig() {
		return config;
	}

	public ClientPlayer getMyPlayer() {
		return myPlayer;
	}

	/**
	 * Called by the {@link CommunicationHandler} when a connection is
	 * established, and we need to begin tracking the player's state.
	 * @param myPlayer The player.
	 */
	public void setMyPlayer(ClientPlayer myPlayer) {
		this.myPlayer = myPlayer;
	}

	@Override
	public void run() {
		try {
			communicationHandler.establishConnection();
		} catch (IOException e) {
			log.error("Couldn't connect to the server: {}", e.getMessage());
			return;
		}

		gameRenderer = new GameRenderer(config.display, this);
		gameRenderer.setupWindow(
				new PlayerViewCursorCallback(config.input, this, gameRenderer.getCamera(), communicationHandler),
				new PlayerInputKeyCallback(inputHandler),
				new PlayerInputMouseClickCallback(inputHandler)
		);

		long lastFrameAt = System.currentTimeMillis();
		while (!gameRenderer.windowShouldClose()) {
			long now = System.currentTimeMillis();
			float dt = (now - lastFrameAt) / 1000f;
			world.processQueuedChunkUpdates();
			gameRenderer.getCamera().interpolatePosition(dt);
			interpolatePlayers(dt);
			gameRenderer.draw();
			lastFrameAt = now;
		}
		gameRenderer.freeWindow();
		communicationHandler.shutdown();
	}

	public void onMessageReceived(Message msg) {
		if (msg instanceof ChunkDataMessage chunkDataMessage) {
			world.addChunk(chunkDataMessage);
		} else if (msg instanceof ChunkUpdateMessage u) {
			world.updateChunk(u);
			// If we received an update for a chunk we don't have, request it!
			if (world.getChunkAt(u.getChunkPos()) == null) {
				communicationHandler.sendMessage(new ChunkHashMessage(u.cx(), u.cy(), u.cz(), -1));
			}
		} else if (msg instanceof PlayerUpdateMessage playerUpdate) {
			if (playerUpdate.clientId() == myPlayer.getId() && playerUpdate.timestamp() > lastPlayerUpdate) {
				myPlayer.getPosition().set(playerUpdate.px(), playerUpdate.py(), playerUpdate.pz());
				myPlayer.getVelocity().set(playerUpdate.vx(), playerUpdate.vy(), playerUpdate.vz());
				myPlayer.setCrouching(playerUpdate.crouching());
				if (gameRenderer != null) {
					gameRenderer.getCamera().setToPlayer(myPlayer);
				}
				lastPlayerUpdate = playerUpdate.timestamp();
			} else {
				OtherPlayer p = players.get(playerUpdate.clientId());
				if (p != null) {
					playerUpdate.apply(p);
					p.setHeldItemId(playerUpdate.selectedItemId());
					p.updateModelTransform();
				}
			}
		} else if (msg instanceof ClientInventoryMessage inventoryMessage) {
			myPlayer.setInventory(inventoryMessage.inv());
		} else if (msg instanceof InventorySelectedStackMessage selectedStackMessage) {
			myPlayer.getInventory().setSelectedIndex(selectedStackMessage.index());
		} else if (msg instanceof ItemStackMessage itemStackMessage) {
			myPlayer.getInventory().getItemStacks().set(itemStackMessage.index(), itemStackMessage.stack());
		} else if (msg instanceof PlayerJoinMessage joinMessage) {
			Player p = joinMessage.toPlayer();
			OtherPlayer op = new OtherPlayer(p.getId(), p.getUsername());
			if (joinMessage.teamId() != -1) {
				op.setTeam(teams.get(joinMessage.teamId()));
			}
			op.getPosition().set(p.getPosition());
			op.getVelocity().set(p.getVelocity());
			op.getOrientation().set(p.getOrientation());
			op.setHeldItemId(joinMessage.selectedItemId());
			players.put(op.getId(), op);
		} else if (msg instanceof PlayerLeaveMessage leaveMessage) {
			players.remove(leaveMessage.id());
		}
	}

	public void setWorld(ClientWorld world) {
		this.world = world;
	}

	public ClientWorld getWorld() {
		return world;
	}

	public Map<Integer, Team> getTeams() {
		return teams;
	}

	public void setTeams(Map<Integer, Team> teams) {
		this.teams = teams;
	}

	public Map<Integer, OtherPlayer> getPlayers() {
		return players;
	}

	public void interpolatePlayers(float dt) {
		Vector3f movement = new Vector3f();
		for (var player : players.values()) {
			movement.set(player.getVelocity()).mul(dt);
			player.getPosition().add(movement);
			player.updateModelTransform();
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

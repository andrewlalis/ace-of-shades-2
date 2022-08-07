package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.config.ClientConfig;
import nl.andrewl.aos2_client.config.ConnectConfig;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.model.Chat;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.model.OtherPlayer;
import nl.andrewl.aos2_client.render.GameRenderer;
import nl.andrewl.aos2_client.sound.SoundManager;
import nl.andrewl.aos_core.config.Config;
import nl.andrewl.aos_core.model.Projectile;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.net.client.*;
import nl.andrewl.aos_core.net.world.ChunkDataMessage;
import nl.andrewl.aos_core.net.world.ChunkHashMessage;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
import nl.andrewl.record_net.Message;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client implements Runnable {
	public final ConnectConfig connectConfig;
	public final ClientConfig config;

	private final CommunicationHandler communicationHandler;
	private final InputHandler inputHandler;
	private final Camera camera;
	private GameRenderer gameRenderer;
	private SoundManager soundManager;
	private long lastPlayerUpdate = 0;

	private ClientWorld world;
	private ClientPlayer myPlayer;
	private final Map<Integer, OtherPlayer> players;
	private final Map<Integer, Projectile> projectiles;
	private final Map<Integer, Team> teams;
	private final Chat chat;
	private final Queue<Runnable> mainThreadActions;

	public Client(ClientConfig config, ConnectConfig connectConfig) {
		this.config = config;
		this.connectConfig = connectConfig;
		this.camera = new Camera();
		this.players = new ConcurrentHashMap<>();
		this.teams = new ConcurrentHashMap<>();
		this.projectiles = new ConcurrentHashMap<>();
		this.communicationHandler = new CommunicationHandler(this);
		this.inputHandler = new InputHandler(this, communicationHandler, camera);
		this.chat = new Chat();
		this.mainThreadActions = new ConcurrentLinkedQueue<>();
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
			System.err.println("Couldn't connect to the server: " + e.getMessage());
			return;
		}

		gameRenderer = new GameRenderer(this, inputHandler, camera);
		soundManager = new SoundManager();

		long lastFrameAt = System.currentTimeMillis();
		while (!gameRenderer.windowShouldClose() && !communicationHandler.isDone()) {
			long now = System.currentTimeMillis();
			float dt = (now - lastFrameAt) / 1000f;

			world.processQueuedChunkUpdates();
			while (!mainThreadActions.isEmpty()) {
				mainThreadActions.remove().run();
			}
			soundManager.updateListener(myPlayer.getPosition(), myPlayer.getVelocity());
			gameRenderer.getCamera().interpolatePosition(dt);
			interpolatePlayers(now, dt);
			interpolateProjectiles(dt);
			soundManager.playWalkingSounds(myPlayer, world, now);

			gameRenderer.draw();
			lastFrameAt = now;
		}
		soundManager.free();
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
			runLater(() -> {
				if (playerUpdate.clientId() == myPlayer.getId() && playerUpdate.timestamp() > lastPlayerUpdate) {
					myPlayer.getPosition().set(playerUpdate.px(), playerUpdate.py(), playerUpdate.pz());
					myPlayer.getVelocity().set(playerUpdate.vx(), playerUpdate.vy(), playerUpdate.vz());
					myPlayer.setCrouching(playerUpdate.crouching());
					myPlayer.setMode(playerUpdate.mode());
					if (gameRenderer != null) {
						gameRenderer.getCamera().setToPlayer(myPlayer);
					}
					if (soundManager != null) {
						soundManager.updateListener(myPlayer.getEyePosition(), myPlayer.getVelocity());
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
			});
		} else if (msg instanceof ClientInventoryMessage inventoryMessage) {
			runLater(() -> myPlayer.setInventory(inventoryMessage.inv()));
		} else if (msg instanceof InventorySelectedStackMessage selectedStackMessage) {
			myPlayer.getInventory().setSelectedIndex(selectedStackMessage.index());
		} else if (msg instanceof ItemStackMessage itemStackMessage) {
			myPlayer.getInventory().getItemStacks().set(itemStackMessage.index(), itemStackMessage.stack());
		} else if (msg instanceof BlockColorMessage blockColorMessage) {
			OtherPlayer player = players.get(blockColorMessage.clientId());
			if (player != null) {
				player.setSelectedBlockValue(blockColorMessage.block());
			}
		} else if (msg instanceof PlayerJoinMessage joinMessage) {
			runLater(() -> {
				OtherPlayer op = OtherPlayer.fromJoinMessage(joinMessage, this);
				players.put(op.getId(), op);
			});
		} else if (msg instanceof PlayerLeaveMessage leaveMessage) {
			runLater(() -> players.remove(leaveMessage.id()));
		} else if (msg instanceof PlayerTeamUpdateMessage teamUpdateMessage) {
			runLater(() -> {
				OtherPlayer op = players.get(teamUpdateMessage.playerId());
				Team team = teamUpdateMessage.teamId() == -1 ? null : teams.get(teamUpdateMessage.teamId());
				if (op != null) {
					op.setTeam(team);
				}
			});
		} else if (msg instanceof SoundMessage soundMessage) {
			if (soundManager != null) {
				soundManager.play(
						soundMessage.name(),
						soundMessage.gain(),
						new Vector3f(soundMessage.px(), soundMessage.py(), soundMessage.pz()),
						new Vector3f(soundMessage.vx(), soundMessage.vy(), soundMessage.vz())
				);
			}
		} else if (msg instanceof ProjectileMessage pm) {
			runLater(() -> {
				Projectile p = projectiles.get(pm.id());
				if (p == null && !pm.destroyed()) {
					p = new Projectile(pm.id(), new Vector3f(pm.px(), pm.py(), pm.pz()), new Vector3f(pm.vx(), pm.vy(), pm.vz()), pm.type());
					projectiles.put(p.getId(), p);
				} else if (p != null) {
					p.getPosition().set(pm.px(), pm.py(), pm.pz()); // Don't update position, it's too short of a timeframe to matter.
					p.getVelocity().set(pm.vx(), pm.vy(), pm.vz());
					if (pm.destroyed()) {
						projectiles.remove(p.getId());
					}
				}
			});
		} else if (msg instanceof ClientHealthMessage healthMessage) {
			myPlayer.setHealth(healthMessage.health());
		} else if (msg instanceof ChatMessage chatMessage) {
			chat.chatReceived(chatMessage);
			if (soundManager != null) {
				soundManager.play("chat", 1, myPlayer.getEyePosition(), myPlayer.getVelocity());
			}
		} else if (msg instanceof ClientRecoilMessage recoil) {
			runLater(() -> {
				myPlayer.setOrientation(myPlayer.getOrientation().x + recoil.dx(), myPlayer.getOrientation().y + recoil.dy());
				if (gameRenderer != null) {
					gameRenderer.getCamera().setOrientationToPlayer(myPlayer);
				}
				communicationHandler.sendDatagramPacket(ClientOrientationState.fromPlayer(myPlayer));
			});
		}
	}

	public void setWorld(ClientWorld world) {
		this.world = world;
	}

	public ClientWorld getWorld() {
		return world;
	}

	public InputHandler getInputHandler() {
		return inputHandler;
	}

	public CommunicationHandler getCommunicationHandler() {
		return communicationHandler;
	}

	public Map<Integer, Team> getTeams() {
		return teams;
	}

	public Map<Integer, OtherPlayer> getPlayers() {
		return players;
	}

	public Map<Integer, Projectile> getProjectiles() {
		return projectiles;
	}

	public Chat getChat() {
		return chat;
	}

	public SoundManager getSoundManager() {
		return soundManager;
	}

	public void interpolatePlayers(long now, float dt) {
		Vector3f movement = new Vector3f();
		for (var player : players.values()) {
			movement.set(player.getVelocity()).mul(dt);
			player.getPosition().add(movement);
			player.updateModelTransform();
			soundManager.playWalkingSounds(player, world, now);
		}
		gameRenderer.getGuiRenderer().updateNamePlates(players.values());
	}

	public void interpolateProjectiles(float dt) {
		Vector3f movement = new Vector3f();
		for (var proj : projectiles.values()) {
			movement.set(proj.getVelocity()).mul(dt);
			proj.getPosition().add(movement);
		}
	}

	public void runLater(Runnable runnable) {
		mainThreadActions.add(runnable);
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Missing required host, port, username args.");
			System.exit(1);
		}
		String host = args[0].trim();
		int port = Integer.parseInt(args[1]);
		String username = args[2].trim();
		ConnectConfig connectCfg = new ConnectConfig(host, port, username, false);

		List<Path> configPaths = Config.getCommonConfigPaths();
		configPaths.add(0, Path.of("client.yaml")); // Add this first so we create client.yaml if needed.
		if (args.length > 3) {
			configPaths.add(Path.of(args[3].trim()));
		}
		ClientConfig clientConfig = Config.loadConfig(ClientConfig.class, configPaths, new ClientConfig(), "default-config.yaml");
		Client client = new Client(clientConfig, connectCfg);
		client.run();
	}
}

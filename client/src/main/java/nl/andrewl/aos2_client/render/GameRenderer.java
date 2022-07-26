package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.config.ClientConfig;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.render.chunk.ChunkRenderer;
import nl.andrewl.aos2_client.render.gui.GUIRenderer;
import nl.andrewl.aos2_client.render.gui.GUITexture;
import nl.andrewl.aos2_client.render.model.Model;
import nl.andrewl.aos_core.model.Team;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.item.ItemTypes;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

/**
 * This component manages all the view-related aspects of the client, such as
 * chunk rendering, window setup and removal, and other OpenGL functions. It
 * should generally only be invoked on the main thread, since this is where the
 * OpenGL context exists.
 */
public class GameRenderer {
	private static final Logger log = LoggerFactory.getLogger(GameRenderer.class);
	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 500f;

	private final ClientConfig.DisplayConfig config;
	private ChunkRenderer chunkRenderer;
	private GUIRenderer guiRenderer;
	private ModelRenderer modelRenderer;
	private final Camera camera;
	private final Client client;

	// Standard models for various game components.
	private Model playerModel;
	private Model rifleModel;
	private Model blockModel;
	private Model bulletModel;
	private Model smgModel;
	private Model shotgunModel;
	private Model flagModel;

	// Standard GUI textures.
	private GUITexture crosshairTexture;
	private GUITexture clipTexture;
	private GUITexture bulletTexture;
	private GUITexture healthBarRedTexture;
	private GUITexture healthBarGreenTexture;

	private long windowHandle;
	private int screenWidth = 800;
	private int screenHeight = 600;

	private final Matrix4f perspectiveTransform;

	public GameRenderer(ClientConfig.DisplayConfig config, Client client) {
		this.config = config;
		this.client = client;
		this.camera = new Camera();
		camera.setToPlayer(client.getMyPlayer());
		this.perspectiveTransform = new Matrix4f();
	}

	public void setupWindow(
			GLFWCursorPosCallbackI viewCursorCallback,
			GLFWKeyCallbackI inputKeyCallback,
			GLFWMouseButtonCallbackI mouseButtonCallback,
			GLFWScrollCallbackI scrollCallback
	) {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW.");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		long monitorId = glfwGetPrimaryMonitor();
		GLFWVidMode primaryMonitorSettings = glfwGetVideoMode(monitorId);
		if (primaryMonitorSettings == null) throw new IllegalStateException("Could not get information about the primary monitory.");
		log.debug("Primary monitor settings: Width: {}, Height: {}, FOV: {}", primaryMonitorSettings.width(), primaryMonitorSettings.height(), config.fov);
		if (config.fullscreen) {
			screenWidth = primaryMonitorSettings.width();
			screenHeight = primaryMonitorSettings.height();
			windowHandle = glfwCreateWindow(screenWidth, screenHeight, "Ace of Shades 2", monitorId, 0);
		} else {
			screenWidth = 1000;
			screenHeight = 800;
			windowHandle = glfwCreateWindow(screenWidth, screenHeight, "Ace of Shades 2", 0, 0);
		}
		if (windowHandle == 0) throw new RuntimeException("Failed to create GLFW window.");
		log.debug("Initialized GLFW window.");

		// Setup callbacks.
		glfwSetKeyCallback(windowHandle, inputKeyCallback);
		glfwSetCursorPosCallback(windowHandle, viewCursorCallback);
		glfwSetMouseButtonCallback(windowHandle, mouseButtonCallback);
		glfwSetScrollCallback(windowHandle, scrollCallback);
		if (config.captureCursor) {
			glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		}
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
		glfwSetCursorPos(windowHandle, 0, 0);
		log.debug("Set up window callbacks.");

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);

		GL.createCapabilities();
//		GLUtil.setupDebugMessageCallback(System.out);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		log.debug("Initialized OpenGL context.");

		this.chunkRenderer = new ChunkRenderer();
		log.debug("Initialized chunk renderer.");

		this.guiRenderer = new GUIRenderer();
		crosshairTexture = new GUITexture("gui/crosshair.png");
		clipTexture = new GUITexture("gui/clip.png");
		bulletTexture = new GUITexture("gui/bullet.png");
		healthBarRedTexture = new GUITexture("gui/health-red.png");
		healthBarGreenTexture = new GUITexture("gui/health-green.png");
		guiRenderer.addTexture("crosshair", crosshairTexture);
		guiRenderer.addTexture("clip", clipTexture);
		guiRenderer.addTexture("bullet", bulletTexture);
		guiRenderer.addTexture("health-red", healthBarRedTexture);
		guiRenderer.addTexture("health-green", healthBarGreenTexture);
		log.debug("Initialized GUI renderer.");

		this.modelRenderer = new ModelRenderer();
		try {
			playerModel = new Model("model/player_simple.obj", "model/simple_player.png");
			rifleModel = new Model("model/rifle.obj", "model/rifle.png");
			smgModel = new Model("model/smg.obj", "model/smg.png");
			blockModel = new Model("model/block.obj", "model/block.png");
			bulletModel = new Model("model/bullet.obj", "model/bullet.png");
			flagModel = new Model("model/flag.obj", "model/flag.png");
			shotgunModel = new Model("model/shotgun.obj", "model/shotgun.png");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.debug("Initialized model renderer.");
		updatePerspective();
	}

	public float getAspectRatio() {
		return (float) screenWidth / (float) screenHeight;
	}

	/**
	 * Updates the rendering perspective used to render the game.
	 */
	private void updatePerspective() {
		float fovRad = (float) Math.toRadians(config.fov);
		if (fovRad >= Math.PI) {
			fovRad = (float) (Math.PI - 0.01f);
		} else if (fovRad <= 0) {
			fovRad = 0.01f;
		}
		perspectiveTransform.setPerspective(fovRad, getAspectRatio(), Z_NEAR, Z_FAR);
		float[] data = new float[16];
		perspectiveTransform.get(data);
		if (chunkRenderer != null) chunkRenderer.setPerspective(data);
		if (modelRenderer != null) modelRenderer.setPerspective(data);
	}

	public Matrix4f getPerspectiveTransform() {
		return perspectiveTransform;
	}

	public boolean windowShouldClose() {
		return glfwWindowShouldClose(windowHandle);
	}

	public Camera getCamera() {
		return camera;
	}

	public void draw() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		chunkRenderer.draw(camera, client.getWorld().getChunkMeshesToDraw());

		ClientPlayer myPlayer = client.getMyPlayer();

		// Draw models. Use one texture at a time for efficiency.
		modelRenderer.start(camera.getViewTransformData());
		myPlayer.updateHeldItemTransform(camera);

		playerModel.bind();
		for (var player : client.getPlayers().values()) {
			if (player.getTeam() != null) {
				modelRenderer.setAspectColor(player.getTeam().getColor());
			} else {
				modelRenderer.setAspectColor(new Vector3f(0.3f, 0.3f, 0.3f));
			}
			modelRenderer.render(playerModel, player.getModelTransformData(), player.getNormalTransformData());
		}
		playerModel.unbind();

		// Render guns!
		rifleModel.bind();
		if (myPlayer.getInventory().getSelectedItemStack().getType().getId() == ItemTypes.RIFLE.getId()) {
			modelRenderer.render(rifleModel, myPlayer.getHeldItemTransformData(), myPlayer.getHeldItemNormalTransformData());
		}
		for (var player : client.getPlayers().values()) {
			if (player.getHeldItemId() == ItemTypes.RIFLE.getId()) {
				modelRenderer.render(rifleModel, player.getHeldItemTransformData(), player.getHeldItemNormalTransformData());
			}
		}
		rifleModel.unbind();
		smgModel.bind();
		if (myPlayer.getInventory().getSelectedItemStack().getType().getId() == ItemTypes.AK_47.getId()) {
			modelRenderer.render(smgModel, myPlayer.getHeldItemTransformData(), myPlayer.getHeldItemNormalTransformData());
		}
		for (var player : client.getPlayers().values()) {
			if (player.getHeldItemId() == ItemTypes.AK_47.getId()) {
				modelRenderer.render(smgModel, player.getHeldItemTransformData(), player.getHeldItemNormalTransformData());
			}
		}
		smgModel.unbind();
		shotgunModel.bind();
		if (myPlayer.getInventory().getSelectedItemStack().getType().getId() == ItemTypes.WINCHESTER.getId()) {
			modelRenderer.render(shotgunModel, myPlayer.getHeldItemTransformData(), myPlayer.getHeldItemNormalTransformData());
		}
		for (var player : client.getPlayers().values()) {
			if (player.getHeldItemId() == ItemTypes.WINCHESTER.getId()) {
				modelRenderer.render(shotgunModel, player.getHeldItemTransformData(), player.getHeldItemNormalTransformData());
			}
		}
		shotgunModel.unbind();

		blockModel.bind();
		if (myPlayer.getInventory().getSelectedItemStack().getType().getId() == ItemTypes.BLOCK.getId()) {
			BlockItemStack stack = (BlockItemStack) myPlayer.getInventory().getSelectedItemStack();
			modelRenderer.setAspectColor(client.getWorld().getPalette().getColor(stack.getSelectedValue()));
			modelRenderer.render(blockModel, myPlayer.getHeldItemTransformData(), myPlayer.getHeldItemNormalTransformData());
		}
		modelRenderer.setAspectColor(new Vector3f(0.5f, 0.5f, 0.5f));
		for (var player : client.getPlayers().values()) {
			if (player.getHeldItemId() == ItemTypes.BLOCK.getId()) {
				modelRenderer.setAspectColor(client.getWorld().getPalette().getColor(player.getSelectedBlockValue()));
				modelRenderer.render(blockModel, player.getHeldItemTransformData(), player.getHeldItemNormalTransformData());
			}
		}
		blockModel.unbind();

		bulletModel.bind();
		Matrix4f modelTransform = new Matrix4f();
		Matrix3f normalTransform = new Matrix3f();
		for (var projectile : client.getProjectiles().values()) {
			modelTransform.identity()
					.translate(projectile.getPosition())
					.rotateTowards(projectile.getVelocity(), Camera.UP)
					.scale(1, 1, projectile.getVelocity().length() / 5);
			modelTransform.normal(normalTransform);
			modelRenderer.render(bulletModel, modelTransform, normalTransform);
		}
		bulletModel.unbind();
		// Draw team bases.
		flagModel.bind();
		for (Team team : client.getTeams().values()) {
			modelTransform.identity()
					.translate(team.getSpawnPoint());
			modelTransform.normal(normalTransform);
			modelRenderer.setAspectColor(team.getColor());
			modelRenderer.render(flagModel, modelTransform, normalTransform);
		}
		flagModel.unbind();

		modelRenderer.end();

		// GUI rendering
		guiRenderer.start();
		guiRenderer.draw(crosshairTexture, crosshairTexture.getIdealScaleX(32, screenWidth), crosshairTexture.getIdealScaleY(32, screenHeight), 0, 0);
		// If we're holding a gun, draw clip and bullet graphics.
		if (client.getMyPlayer().getInventory().getSelectedItemStack().getType() instanceof Gun) {
			GunItemStack stack = (GunItemStack) client.getMyPlayer().getInventory().getSelectedItemStack();
			for (int i = 0; i < stack.getClipCount(); i++) {
				guiRenderer.draw(
						clipTexture,
						clipTexture.getIdealScaleX(64, screenWidth),
						clipTexture.getIdealScaleY(clipTexture.getIdealHeight(64), screenHeight),
						0.90f,
						-0.90f + (i * 0.15f)
				);
			}
			for (int i = 0; i < stack.getBulletCount(); i++) {
				guiRenderer.draw(
						bulletTexture,
						bulletTexture.getIdealScaleX(16, screenWidth),
						bulletTexture.getIdealScaleY(bulletTexture.getIdealHeight(16), screenHeight),
						0.80f - (i * 0.05f),
						-0.90f
				);
			}
		}
		// Render the player's health.
		guiRenderer.draw(
				healthBarRedTexture,
				healthBarRedTexture.getIdealScaleX(64, screenWidth),
				healthBarRedTexture.getIdealScaleY(16, screenHeight),
				-0.90f,
				-0.90f
		);
		guiRenderer.draw(
				healthBarGreenTexture,
				healthBarGreenTexture.getIdealScaleX(64 * client.getMyPlayer().getHealth(), screenWidth),
				healthBarGreenTexture.getIdealScaleY(16, screenHeight),
				-0.90f,
				-0.90f
		);
		guiRenderer.end();

		glfwSwapBuffers(windowHandle);
		glfwPollEvents();
	}

	public void freeWindow() {
		if (rifleModel != null) rifleModel.free();
		if (smgModel != null) smgModel.free();
		if (flagModel != null) flagModel.free();
		if (bulletModel != null) bulletModel.free();
		if (playerModel != null) playerModel.free();
		if (blockModel != null) blockModel.free();
		if (modelRenderer != null) modelRenderer.free();
		if (guiRenderer != null) guiRenderer.free();
		if (chunkRenderer != null) chunkRenderer.free();
		GL.destroy();
		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwSetErrorCallback(null);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
	}
}

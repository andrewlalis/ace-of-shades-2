package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.ClientWorld;
import nl.andrewl.aos2_client.render.chunk.ChunkRenderer;
import nl.andrewl.aos2_client.render.gui.GUIRenderer;
import nl.andrewl.aos2_client.render.gui.GUITexture;
import nl.andrewl.aos2_client.render.model.Model;
import org.joml.Matrix4f;
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

	private ChunkRenderer chunkRenderer;
	private GUIRenderer guiRenderer;
	private ModelRenderer modelRenderer;
	private final Camera camera;
	private final ClientWorld world;
	private Model playerModel; // Standard player model used to render all players.
	private Model rifleModel;

	private long windowHandle;
	private int screenWidth = 800;
	private int screenHeight = 600;
	private float fov = 90f;

	private final Matrix4f perspectiveTransform;

	public GameRenderer(ClientWorld world) {
		this.world = world;
		this.camera = new Camera();
		this.perspectiveTransform = new Matrix4f();
	}

	public void setupWindow(
			GLFWCursorPosCallbackI viewCursorCallback,
			GLFWKeyCallbackI inputKeyCallback,
			GLFWMouseButtonCallbackI mouseButtonCallback,
			boolean fullscreen,
			boolean grabCursor
	) {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW.");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		long monitorId = glfwGetPrimaryMonitor();
		GLFWVidMode primaryMonitorSettings = glfwGetVideoMode(monitorId);
		if (primaryMonitorSettings == null) throw new IllegalStateException("Could not get information about the primary monitory.");
		log.debug("Primary monitor settings: Width: {}, Height: {}", primaryMonitorSettings.width(), primaryMonitorSettings.height());
		if (fullscreen) {
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
		if (grabCursor) {
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
		// TODO: More organized way to load textures for GUI.
		try {
			var crosshairTexture = new GUITexture("gui/crosshair.png");
			float size = 32;
			crosshairTexture.getScale().set(size / screenWidth, size / screenHeight);
			guiRenderer.addTexture(crosshairTexture);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.debug("Initialized GUI renderer.");
		this.modelRenderer = new ModelRenderer();
		try {
			playerModel = new Model("model/player_simple.obj", "model/simple_player.png");
			rifleModel = new Model("model/rifle.obj", "model/rifle.png");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.debug("Initialized model renderer.");
		updatePerspective();
	}

	public void setFov(float fov) {
		this.fov = fov;
		updatePerspective();
	}

	public float getAspectRatio() {
		return (float) screenWidth / (float) screenHeight;
	}

	/**
	 * Updates the rendering perspective used to render the game.
	 */
	private void updatePerspective() {
		perspectiveTransform.setPerspective(fov, getAspectRatio(), Z_NEAR, Z_FAR);
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
		chunkRenderer.draw(camera, world.getChunkMeshesToDraw());

		// Draw players.
		modelRenderer.setView(camera.getViewTransformData());
		playerModel.bind();
		Matrix4f modelTransform = new Matrix4f();
		for (var player : world.getPlayers()) {
			modelTransform.identity().translate(player.getPosition());
			modelRenderer.render(playerModel, modelTransform);
		}
		playerModel.unbind();
		rifleModel.bind();
		for (var player : world.getPlayers()) {
			modelTransform.identity()
					.translate(player.getPosition())
					.rotate((float) (player.getOrientation().x - Math.PI / 2), Camera.UP)
					.translate(0, 0, -0.45f);
			modelRenderer.render(rifleModel, modelTransform);
		}
		rifleModel.unbind();

		guiRenderer.draw();

		glfwSwapBuffers(windowHandle);
		glfwPollEvents();
	}

	public void freeWindow() {
		if (playerModel != null) playerModel.free();
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

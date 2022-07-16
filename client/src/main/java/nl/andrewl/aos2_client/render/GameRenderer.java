package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.render.chunk.ChunkRenderer;
import nl.andrewl.aos2_client.render.gui.GUIRenderer;
import nl.andrewl.aos2_client.render.gui.GUITexture;
import nl.andrewl.aos_core.model.World;
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

	private final ChunkRenderer chunkRenderer;
	private final GUIRenderer guiRenderer;
	private final Camera camera;
	private final World world;

	private long windowHandle;
	private GLFWVidMode primaryMonitorSettings;
	private boolean fullscreen;
	private int screenWidth = 800;
	private int screenHeight = 600;
	private float fov = 70f;

	private final Matrix4f perspectiveTransform;

	public GameRenderer(World world) {
		this.world = world;
		this.chunkRenderer = new ChunkRenderer();
		this.guiRenderer = new GUIRenderer();
		this.camera = new Camera();
		this.perspectiveTransform = new Matrix4f();

	}

	public void setupWindow(GLFWCursorPosCallbackI viewCursorCallback, GLFWKeyCallbackI inputKeyCallback, GLFWMouseButtonCallbackI mouseButtonCallback) {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW.");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		primaryMonitorSettings = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if (primaryMonitorSettings == null) throw new IllegalStateException("Could not get information about the primary monitory.");
		log.debug("Primary monitor settings: Width: {}, Height: {}", primaryMonitorSettings.width(), primaryMonitorSettings.height());
		windowHandle = glfwCreateWindow(screenWidth, screenHeight, "Ace of Shades 2", 0, 0);
		if (windowHandle == 0) throw new RuntimeException("Failed to create GLFW window.");
		fullscreen = false;
		log.debug("Initialized GLFW window.");

		// Setup callbacks.
		glfwSetKeyCallback(windowHandle, inputKeyCallback);
		glfwSetCursorPosCallback(windowHandle, viewCursorCallback);
		glfwSetMouseButtonCallback(windowHandle, mouseButtonCallback);
		glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
		glfwSetCursorPos(windowHandle, 0, 0);
		log.debug("Set up window callbacks.");

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);
		log.debug("Made window visible.");

		GL.createCapabilities();
//		GLUtil.setupDebugMessageCallback(System.out);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		log.debug("Initialized OpenGL context.");

		chunkRenderer.setupShaderProgram();
		log.debug("Initialized chunk renderer.");
		guiRenderer.setup();
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
		updatePerspective();
	}

	public void setFullscreen(boolean fullscreen) {
		if (windowHandle == 0) throw new IllegalStateException("Window not setup.");
		long monitor = glfwGetPrimaryMonitor();
		if (!this.fullscreen && fullscreen) {
			glfwSetWindowMonitor(windowHandle, monitor, 0, 0, primaryMonitorSettings.width(), primaryMonitorSettings.height(), primaryMonitorSettings.refreshRate());
			screenWidth = primaryMonitorSettings.width();
			screenHeight = primaryMonitorSettings.height();
			updatePerspective();
		} else if (this.fullscreen && !fullscreen) {
			screenWidth = 800;
			screenHeight = 600;
			int left = primaryMonitorSettings.width() / 2;
			int top = primaryMonitorSettings.height() / 2;
			glfwSetWindowMonitor(windowHandle, 0, left, top, screenWidth, screenHeight, primaryMonitorSettings.refreshRate());
			updatePerspective();
		}
		this.fullscreen = fullscreen;
	}

	public void setSize(int width, int height) {
		glfwSetWindowSize(windowHandle, width, height);
		this.screenWidth = width;
		this.screenHeight = height;
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
	 * Updates the rendering perspective used to render the game. Note: only
	 * call this after calling {@link ChunkRenderer#setupShaderProgram()}.
	 */
	private void updatePerspective() {
		perspectiveTransform.setPerspective(fov, getAspectRatio(), Z_NEAR, Z_FAR);
		chunkRenderer.setPerspective(perspectiveTransform);
	}

	public boolean windowShouldClose() {
		return glfwWindowShouldClose(windowHandle);
	}

	public Camera getCamera() {
		return camera;
	}

	public ChunkRenderer getChunkRenderer() {
		return chunkRenderer;
	}

	public void draw() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		chunkRenderer.draw(camera, world);
		guiRenderer.draw();

		glfwSwapBuffers(windowHandle);
		glfwPollEvents();
	}

	public void freeWindow() {
		guiRenderer.free();
		chunkRenderer.free();
		GL.destroy();
		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwSetErrorCallback(null);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
	}
}

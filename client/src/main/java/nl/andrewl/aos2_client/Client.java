package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.render.ChunkMesh;
import nl.andrewl.aos2_client.render.ChunkRenderer;
import nl.andrewl.aos2_client.render.WindowInfo;
import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3i;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Client {
	public static void main(String[] args) {
		var windowInfo = initUI();
		long windowHandle = windowInfo.windowHandle();

		Camera cam = new Camera();
		cam.setOrientationDegrees(90, 90);
		cam.setPosition(-3, 3, 0);
		glfwSetCursorPosCallback(windowHandle, cam);

		Chunk chunk = Chunk.random(new Vector3i(0, 0, 0), new Random(1));
		Chunk chunk2 = Chunk.random(new Vector3i(1, 0, 0), new Random(1));
		Chunk chunk3 = Chunk.random(new Vector3i(1, 0, 1), new Random(1));
		Chunk chunk4 = Chunk.random(new Vector3i(0, 0, 1), new Random(1));

		chunk.setBlockAt(0, 0, 0, (byte) 0);

		for (int x = 0; x < Chunk.SIZE; x++) {
			for (int z = 0; z < Chunk.SIZE; z++) {
				chunk.setBlockAt(x, Chunk.SIZE - 1, z, (byte) 0);
			}
		}

		ChunkRenderer chunkRenderer = new ChunkRenderer(windowInfo.width(), windowInfo.height());
		chunkRenderer.addChunkMesh(new ChunkMesh(chunk));
		chunkRenderer.addChunkMesh(new ChunkMesh(chunk2));
		chunkRenderer.addChunkMesh(new ChunkMesh(chunk3));
		chunkRenderer.addChunkMesh(new ChunkMesh(chunk4));

		while (!glfwWindowShouldClose(windowHandle)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			chunkRenderer.draw(cam);

			glfwSwapBuffers(windowHandle);
			glfwPollEvents();

			if (glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS) cam.move(Camera.FORWARD);
			if (glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS) cam.move(Camera.BACKWARD);
			if (glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS) cam.move(Camera.LEFT);
			if (glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS) cam.move(Camera.RIGHT);
			if (glfwGetKey(windowHandle, GLFW_KEY_SPACE) == GLFW_PRESS) cam.move(Camera.UP);
			if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) cam.move(Camera.DOWN);
		}

		chunkRenderer.free();

		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private static WindowInfo initUI() {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW.");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		var vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if (vidMode == null) throw new IllegalStateException("Could not get information about the primary monitory.");
		long windowHandle = glfwCreateWindow(vidMode.width(), vidMode.height(), "Ace of Shades 2", glfwGetPrimaryMonitor(), 0);
		if (windowHandle == 0) throw new RuntimeException("Failed to create GLFW window.");

		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(windowHandle, true);
			}
		});

		glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

		glfwSetWindowPos(windowHandle, 0, 0);
		glfwSetCursorPos(windowHandle, 0, 0);

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);

		GL.createCapabilities();
//		GLUtil.setupDebugMessageCallback(System.out);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);

		return new WindowInfo(windowHandle, vidMode.width(), vidMode.height());
	}
}

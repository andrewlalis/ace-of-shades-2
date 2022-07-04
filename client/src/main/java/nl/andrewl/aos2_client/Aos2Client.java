package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3i;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.MemoryUtil;

import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Aos2Client {
	public static void main(String[] args) {
		long windowHandle = initUI();

		Chunk chunk = Chunk.random(new Vector3i(0, 0, 0), new Random(1));
		Camera cam = new Camera();
		glfwSetCursorPosCallback(windowHandle, cam);

		for (int i = 0; i < 16; i++) {
			chunk.setBlockAt(i, 0, 0, (byte) 8);
			chunk.setBlockAt(0, i, 0, (byte) 40);
			chunk.setBlockAt(0, 0, i, (byte) 120);
		}
		chunk.setBlockAt(0, 15, 0, (byte) 0);
		chunk.setBlockAt(1, 15, 0, (byte) 0);
		chunk.setBlockAt(2, 15, 0, (byte) 0);
		chunk.setBlockAt(2, 15, 1, (byte) 0);
		chunk.setBlockAt(0, 0, 0, (byte) 0);

		ChunkRenderer chunkRenderer = new ChunkRenderer();
		ChunkMesh mesh = new ChunkMesh(chunk);
		chunkRenderer.addChunkMesh(mesh);

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

	private static long initUI() {
		System.out.println("LWJGL Version: " + Version.getVersion());
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		long windowHandle = glfwCreateWindow(800, 600, "Ace of Shades 2", MemoryUtil.NULL, MemoryUtil.NULL);
		if (windowHandle == MemoryUtil.NULL) throw new RuntimeException("Failed to create GLFW window.");
		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(windowHandle, true);
			}
		});

		glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

		glfwSetWindowPos(windowHandle, 50, 50);
		glfwSetCursorPos(windowHandle, 0, 0);

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);

		GL.createCapabilities();
		GLUtil.setupDebugMessageCallback(System.out);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);

		return windowHandle;
	}
}

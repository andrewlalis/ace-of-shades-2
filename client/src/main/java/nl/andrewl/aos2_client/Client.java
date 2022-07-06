package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.render.ChunkMesh;
import nl.andrewl.aos2_client.render.ChunkRenderer;
import nl.andrewl.aos2_client.render.WindowUtils;
import nl.andrewl.aos_core.model.World;

import java.io.IOException;
import java.net.InetAddress;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Client implements Runnable {
	public static void main(String[] args) throws IOException {
		InetAddress serverAddress = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		String username = args[2].trim();

		Client client = new Client(serverAddress, serverPort, username);
		client.run();
	}

	private InetAddress serverAddress;
	private int serverPort;
	private String username;
	private CommunicationHandler communicationHandler;
	private ChunkRenderer chunkRenderer;

	private World world;

	public Client(InetAddress serverAddress, int serverPort, String username) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.username = username;
		this.communicationHandler = new CommunicationHandler(this);
		this.world = new World();
	}

	@Override
	public void run() {
		var windowInfo = WindowUtils.initUI();
		long windowHandle = windowInfo.windowHandle();
		chunkRenderer = new ChunkRenderer(windowInfo.width(), windowInfo.height());

		try {
			communicationHandler.establishConnection(serverAddress, serverPort, username);
			System.out.println("Established connection to the server.");
		} catch (IOException e) {
			e.printStackTrace();
			return; // Exit without starting the game.
		}

		System.out.println("Waiting for all world data to arrive...");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (var chunk : world.getChunkMap().values()) {
			chunkRenderer.addChunkMesh(new ChunkMesh(chunk));
		}

		Camera cam = new Camera();
		cam.setOrientationDegrees(90, 90);
		cam.setPosition(0, 48, 0);
		glfwSetCursorPosCallback(windowHandle, cam);


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
		WindowUtils.clearUI(windowHandle);
	}

	public World getWorld() {
		return world;
	}

	public ChunkRenderer getChunkRenderer() {
		return chunkRenderer;
	}
}

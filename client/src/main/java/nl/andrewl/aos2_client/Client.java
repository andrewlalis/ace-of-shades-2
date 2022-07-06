package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.render.ChunkMesh;
import nl.andrewl.aos2_client.render.ChunkRenderer;
import nl.andrewl.aos2_client.render.WindowUtils;
import nl.andrewl.aos_core.model.Chunk;
import org.joml.Vector3i;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Client implements Runnable {
	public static void main(String[] args) throws IOException {
		InetAddress serverAddress = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		String username = args[2].trim();

		Client client = new Client(serverAddress, serverPort, username);
		client.run();


//		var windowInfo = WindowUtils.initUI();
//		long windowHandle = windowInfo.windowHandle();
//
//		Camera cam = new Camera();
//		cam.setOrientationDegrees(90, 90);
//		cam.setPosition(-3, 3, 0);
//		glfwSetCursorPosCallback(windowHandle, cam);
//
//		Chunk chunk = Chunk.random(new Vector3i(0, 0, 0), new Random(1));
//		Chunk chunk2 = Chunk.random(new Vector3i(1, 0, 0), new Random(1));
//		Chunk chunk3 = Chunk.random(new Vector3i(1, 0, 1), new Random(1));
//		Chunk chunk4 = Chunk.random(new Vector3i(0, 0, 1), new Random(1));
//
//		chunk.setBlockAt(0, 0, 0, (byte) 0);
//
//		for (int x = 0; x < Chunk.SIZE; x++) {
//			for (int z = 0; z < Chunk.SIZE; z++) {
//				chunk.setBlockAt(x, Chunk.SIZE - 1, z, (byte) 0);
//			}
//		}
//
//		ChunkRenderer chunkRenderer = new ChunkRenderer(windowInfo.width(), windowInfo.height());
//		chunkRenderer.addChunkMesh(new ChunkMesh(chunk));
//		chunkRenderer.addChunkMesh(new ChunkMesh(chunk2));
//		chunkRenderer.addChunkMesh(new ChunkMesh(chunk3));
//		chunkRenderer.addChunkMesh(new ChunkMesh(chunk4));
//
//		while (!glfwWindowShouldClose(windowHandle)) {
//			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//
//			chunkRenderer.draw(cam);
//
//			glfwSwapBuffers(windowHandle);
//			glfwPollEvents();
//
//			if (glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS) cam.move(Camera.FORWARD);
//			if (glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS) cam.move(Camera.BACKWARD);
//			if (glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS) cam.move(Camera.LEFT);
//			if (glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS) cam.move(Camera.RIGHT);
//			if (glfwGetKey(windowHandle, GLFW_KEY_SPACE) == GLFW_PRESS) cam.move(Camera.UP);
//			if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) cam.move(Camera.DOWN);
//		}
//
//		chunkRenderer.free();
//		WindowUtils.clearUI(windowHandle);
	}

	private InetAddress serverAddress;
	private int serverPort;
	private String username;
	private CommunicationHandler communicationHandler;
	private volatile boolean running;

	public Client(InetAddress serverAddress, int serverPort, String username) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.username = username;
		this.communicationHandler = new CommunicationHandler();
	}

	@Override
	public void run() {
		running = false;
		try {
			communicationHandler.establishConnection(serverAddress, serverPort, username);
			System.out.println("Established connection to the server.");
		} catch (IOException e) {
			e.printStackTrace();
			running = false;
		}
		while (running) {
			// Do game stuff
			System.out.println("Running!");
		}
	}
}

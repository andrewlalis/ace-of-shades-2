package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos2_client.config.ClientConfig;
import nl.andrewl.aos_core.net.udp.ClientOrientationState;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;

import java.util.concurrent.ForkJoinPool;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

/**
 * Callback that's called when the player's cursor position updates. This means
 * the player is looking around.
 */
public class PlayerViewCursorCallback implements GLFWCursorPosCallbackI {
	/**
	 * The number of milliseconds to wait before sending orientation updates,
	 * to prevent overloading the server.
	 */
	private static final int ORIENTATION_UPDATE_LIMIT = 20;

	private final ClientConfig.InputConfig config;
	private final Camera camera;
	private final CommunicationHandler comm;
	private float lastMouseCursorX;
	private float lastMouseCursorY;
	private long lastOrientationUpdateSentAt = 0L;

	public PlayerViewCursorCallback(ClientConfig.InputConfig config, Camera camera, CommunicationHandler comm) {
		this.config = config;
		this.camera = camera;
		this.comm = comm;
	}

	@Override
	public void invoke(long window, double xpos, double ypos) {
		double[] xb = new double[1];
		double[] yb = new double[1];
		glfwGetCursorPos(window, xb, yb);
		float x = (float) xb[0];
		float y = (float) yb[0];
		float dx = x - lastMouseCursorX;
		float dy = y - lastMouseCursorY;
		lastMouseCursorX = x;
		lastMouseCursorY = y;
		camera.setOrientation(
				camera.getOrientation().x - dx * config.mouseSensitivity,
				camera.getOrientation().y - dy * config.mouseSensitivity
		);
		long now = System.currentTimeMillis();
		if (lastOrientationUpdateSentAt + ORIENTATION_UPDATE_LIMIT < now) {
			ForkJoinPool.commonPool().submit(() -> comm.sendDatagramPacket(new ClientOrientationState(comm.getClientId(), camera.getOrientation().x, camera.getOrientation().y)));
			lastOrientationUpdateSentAt = now;
		}
	}
}

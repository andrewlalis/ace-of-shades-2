package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos_core.net.udp.ClientInputState;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import static org.lwjgl.glfw.GLFW.*;

public class PlayerInputKeyCallback implements GLFWKeyCallbackI {
	private ClientInputState lastInputState = null;
	private final CommunicationHandler comm;

	public PlayerInputKeyCallback(CommunicationHandler comm) {
		this.comm = comm;
	}

	@Override
	public void invoke(long window, int key, int scancode, int action, int mods) {
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
			glfwSetWindowShouldClose(window, true);
		}

		ClientInputState inputState = new ClientInputState(
				comm.getClientId(),
				glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
		);
		if (!inputState.equals(lastInputState)) {
			comm.sendDatagramPacket(inputState);
			lastInputState = inputState;
		}
	}
}

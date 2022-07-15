package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos_core.net.udp.ClientInputState;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Class which manages the player's input, and sending it to the server.
 */
public class InputHandler {
	private final CommunicationHandler comm;

	private ClientInputState lastInputState = null;

	public InputHandler(CommunicationHandler comm) {
		this.comm = comm;
	}

	public void updateInputState(long window) {
		// TODO: Allow customized keybindings.
		ClientInputState currentInputState = new ClientInputState(
				comm.getClientId(),
				glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS,
				glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS,
				glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS
		);
		if (!currentInputState.equals(lastInputState)) {
			comm.sendDatagramPacket(currentInputState);
			lastInputState = currentInputState;
		}
	}
}

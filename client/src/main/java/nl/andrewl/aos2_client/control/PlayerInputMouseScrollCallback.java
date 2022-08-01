package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWScrollCallbackI;

public class PlayerInputMouseScrollCallback implements GLFWScrollCallbackI {
	private final InputHandler inputHandler;

	public PlayerInputMouseScrollCallback(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void invoke(long window, double xoffset, double yoffset) {
		inputHandler.getActiveContext().mouseScroll(window, xoffset, yoffset);
	}
}

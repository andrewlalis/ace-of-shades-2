package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;

public class PlayerViewCursorCallback implements GLFWCursorPosCallbackI {

	private final InputHandler inputHandler;

	public PlayerViewCursorCallback(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void invoke(long window, double xpos, double ypos) {
		inputHandler.getActiveContext().mouseCursorPos(window, xpos, ypos);
	}
}

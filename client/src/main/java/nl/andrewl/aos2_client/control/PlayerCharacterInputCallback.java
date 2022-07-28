package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWCharCallbackI;

public class PlayerCharacterInputCallback implements GLFWCharCallbackI {
	private final InputHandler inputHandler;

	public PlayerCharacterInputCallback(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void invoke(long window, int codepoint) {
		if (inputHandler.isChatting()) {
			inputHandler.appendToChat(codepoint);
		}
	}
}

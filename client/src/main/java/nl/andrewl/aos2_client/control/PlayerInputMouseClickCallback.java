package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

/**
 * Callback that's called when the player clicks with their mouse.
 */
public class PlayerInputMouseClickCallback implements GLFWMouseButtonCallbackI {
	private final InputHandler inputHandler;

	public PlayerInputMouseClickCallback(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void invoke(long window, int button, int action, int mods) {
		System.out.println("Click: " + button);
		inputHandler.updateInputState(window);
	}
}

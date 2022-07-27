package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

import static org.lwjgl.glfw.GLFW.*;

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
		if (action == GLFW_PRESS) {
			switch (button) {
				case GLFW_MOUSE_BUTTON_1 -> inputHandler.setHitting(true);
				case GLFW_MOUSE_BUTTON_2 -> inputHandler.setInteracting(true);
			}
		} else if (action == GLFW_RELEASE) {
			switch (button) {
				case GLFW_MOUSE_BUTTON_1 -> inputHandler.setHitting(false);
				case GLFW_MOUSE_BUTTON_2 -> inputHandler.setInteracting(false);
			}
		}
	}
}

package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

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
		switch (action) {
			case GLFW_PRESS -> inputHandler.getActiveContext().mouseButtonPress(window, button, mods);
			case GLFW_RELEASE -> inputHandler.getActiveContext().mouseButtonRelease(window, button, mods);
		}
	}
}

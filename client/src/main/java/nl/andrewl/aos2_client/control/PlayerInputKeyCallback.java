package nl.andrewl.aos2_client.control;

import org.lwjgl.glfw.GLFWKeyCallbackI;

import static org.lwjgl.glfw.GLFW.*;

public class PlayerInputKeyCallback implements GLFWKeyCallbackI {
	private final InputHandler inputHandler;

	public PlayerInputKeyCallback(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void invoke(long window, int key, int scancode, int action, int mods) {
		switch (action) {
			case GLFW_PRESS -> inputHandler.getActiveContext().keyPress(window, key, mods);
			case GLFW_RELEASE -> inputHandler.getActiveContext().keyRelease(window, key, mods);
			case GLFW_REPEAT -> inputHandler.getActiveContext().keyRepeat(window, key, mods);
		}
	}
}

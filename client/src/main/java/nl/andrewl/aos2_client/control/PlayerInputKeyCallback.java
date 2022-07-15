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
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
			glfwSetWindowShouldClose(window, true);
		}
		inputHandler.updateInputState(window);
	}
}

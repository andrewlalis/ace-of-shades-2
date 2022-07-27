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
		if (action == GLFW_PRESS) {
			switch (key) {
				case GLFW_KEY_W -> inputHandler.setForward(true);
				case GLFW_KEY_A -> inputHandler.setLeft(true);
				case GLFW_KEY_S -> inputHandler.setBackward(true);
				case GLFW_KEY_D -> inputHandler.setRight(true);
				case GLFW_KEY_SPACE -> inputHandler.setJumping(true);
				case GLFW_KEY_LEFT_CONTROL -> inputHandler.setCrouching(true);
				case GLFW_KEY_LEFT_SHIFT -> inputHandler.setSprinting(true);
				case GLFW_KEY_R -> inputHandler.setReloading(true);

				case GLFW_KEY_1 -> inputHandler.setSelectedInventoryIndex(0);
				case GLFW_KEY_2 -> inputHandler.setSelectedInventoryIndex(1);
				case GLFW_KEY_3 -> inputHandler.setSelectedInventoryIndex(2);
				case GLFW_KEY_4 -> inputHandler.setSelectedInventoryIndex(3);

				case GLFW_KEY_F3 -> inputHandler.toggleDebugEnabled();
			}
		} else if (action == GLFW_RELEASE) {
			switch (key) {
				case GLFW_KEY_W -> inputHandler.setForward(false);
				case GLFW_KEY_A -> inputHandler.setLeft(false);
				case GLFW_KEY_S -> inputHandler.setBackward(false);
				case GLFW_KEY_D -> inputHandler.setRight(false);
				case GLFW_KEY_SPACE -> inputHandler.setJumping(false);
				case GLFW_KEY_LEFT_CONTROL -> inputHandler.setCrouching(false);
				case GLFW_KEY_LEFT_SHIFT -> inputHandler.setSprinting(false);
				case GLFW_KEY_R -> inputHandler.setReloading(false);

				case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
			}
		}
	}
}

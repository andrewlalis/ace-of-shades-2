package nl.andrewl.aos2_client.control.context;

import nl.andrewl.aos2_client.control.InputContext;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.util.WindowUtils;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public class ExitMenuContext implements InputContext {
	private final InputHandler inputHandler;

	public ExitMenuContext(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void onEnable() {
		if (inputHandler.getClient().getConfig().display.captureCursor) {
			WindowUtils.freeCursor(inputHandler.getWindowId());
		}
	}

	@Override
	public void onDisable() {
		if (inputHandler.getClient().getConfig().display.captureCursor) {
			WindowUtils.captureCursor(inputHandler.getWindowId());
		}
	}

	@Override
	public void keyPress(long window, int key, int mods) {
		switch (key) {
			case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
			default -> inputHandler.switchToNormalContext();
		}
	}
}

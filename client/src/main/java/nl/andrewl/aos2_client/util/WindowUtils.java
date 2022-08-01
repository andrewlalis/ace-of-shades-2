package nl.andrewl.aos2_client.util;

import nl.andrewl.aos_core.Pair;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class WindowUtils {
	public static Pair<Integer, Integer> getSize(long id) {
		IntBuffer wBuf = BufferUtils.createIntBuffer(1);
		IntBuffer hBuf = BufferUtils.createIntBuffer(1);
		glfwGetWindowSize(id, wBuf, hBuf);
		return new Pair<>(wBuf.get(0), hBuf.get(0));
	}

	public static void captureCursor(long id) {
		glfwSetInputMode(id, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
	}

	public static void freeCursor(long id) {
		glfwSetInputMode(id, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		var size = WindowUtils.getSize(id);
		glfwSetCursorPos(id, size.first() / 2.0, size.second() / 2.0);
	}
}

package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.model.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Aos2Client {
	public static void main(String[] args) throws IOException {
		System.out.println("LWJGL Version: " + Version.getVersion());
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Could not initialize GLFW");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		long windowHandle = glfwCreateWindow(800, 600, "Ace of Shades 2", MemoryUtil.NULL, MemoryUtil.NULL);
		if (windowHandle == MemoryUtil.NULL) throw new RuntimeException("Failed to create GLFW window.");
		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(windowHandle, true);
			}
		});

		glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

		glfwSetWindowPos(windowHandle, 50, 50);
		glfwSetCursorPos(windowHandle, 0, 0);

		glfwMakeContextCurrent(windowHandle);
		glfwSwapInterval(1);
		glfwShowWindow(windowHandle);

		GL.createCapabilities();
		GLUtil.setupDebugMessageCallback(System.out);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);

		Chunk chunk = Chunk.of((byte) 64);
		Matrix4f projectionTransform = new Matrix4f().perspective(70, 800 / 600.0f, 0.01f, 100.0f);
		Matrix4f viewTransform = new Matrix4f()
				.lookAt(new Vector3f(-5, 50, -10), new Vector3f(8, 0, 8), new Vector3f(0, 1, 0));
		ChunkMesh mesh = new ChunkMesh(chunk);

		int shaderProgram = createShaderProgram();
		int projectionTransformUniform = glGetUniformLocation(shaderProgram, "projectionTransform");
		int viewTransformUniform = glGetUniformLocation(shaderProgram, "viewTransform");

		glUniformMatrix4fv(projectionTransformUniform, false, projectionTransform.get(new float[16]));

		while (!glfwWindowShouldClose(windowHandle)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glUniformMatrix4fv(viewTransformUniform, false, viewTransform.get(new float[16]));

			mesh.draw();

			glfwSwapBuffers(windowHandle);
			glfwPollEvents();
		}

		Callbacks.glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private static int createShaderProgram() throws IOException {
		int prog = glCreateProgram();
		int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragShader, FileUtils.readClasspathFile("shader/fragment.glsl"));
		glCompileShader(fragShader);
		glAttachShader(prog, fragShader);
		int vertShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertShader, FileUtils.readClasspathFile("shader/vertex.glsl"));
		glCompileShader(vertShader);
		glAttachShader(prog, vertShader);

		glValidateProgram(prog);
		glLinkProgram(prog);
		glUseProgram(prog);
		return prog;
	}
}

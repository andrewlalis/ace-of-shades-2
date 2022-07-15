package nl.andrewl.aos2_client.render.font;

import nl.andrewl.aos2_client.render.ShaderProgram;

import static org.lwjgl.opengl.GL46.*;

public class FontRenderer {
	private final ShaderProgram shaderProgram;

	public FontRenderer() {
		shaderProgram = new ShaderProgram.Builder()
				.withShader("/shader/text/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("/shader/text/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
	}

	public void free() {
		shaderProgram.free();
	}
}

package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos2_client.render.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

/**
 * Manages rendering of 2D GUI components like cross-hairs, inventory stuff, etc.
 */
public class GUIRenderer {
	private int vaoId;
	private int vboId;
	private int vertexCount;
	private ShaderProgram shaderProgram;
	private int transformUniformLocation;


	private final List<GUITexture> guiTextures = new ArrayList<>();

	public void addTexture(GUITexture texture) {
		guiTextures.add(texture);
	}

	public void setup() {
		vaoId = glGenVertexArrays();
		vboId = glGenBuffers();
		FloatBuffer buffer = BufferUtils.createFloatBuffer(8);
		buffer.put(new float[]{
				-1, 1,
				-1, -1,
				1, 1,
				1, -1
		});
		buffer.flip();
		vertexCount = buffer.limit();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
		shaderProgram = new ShaderProgram.Builder()
				.withShader("shader/gui/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/gui/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
		transformUniformLocation = shaderProgram.getUniform("transform");
		shaderProgram.bindAttribute(0, "position");
	}

	public void draw() {
		shaderProgram.use();
		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_DEPTH_TEST);
		for (var texture : guiTextures) {
			glActiveTexture(GL_TEXTURE0);
			Matrix4f transform = new Matrix4f()
					.translate(texture.getPosition().x, texture.getPosition().y, 0)
					.scale(texture.getScale().x, texture.getScale().y, 1);
			float[] transformData = new float[16];
			transform.get(transformData);
			glUniformMatrix4fv(transformUniformLocation, false, transformData);
			glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
			glDrawArrays(GL_TRIANGLE_STRIP, 0, vertexCount);
		}
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
		glDisableVertexAttribArray(0);
		glBindVertexArray(0);
	}

	public void free() {
		for (var tex : guiTextures) tex.free();
		glDeleteBuffers(vboId);
		glDeleteVertexArrays(vaoId);
		shaderProgram.free();
	}
}

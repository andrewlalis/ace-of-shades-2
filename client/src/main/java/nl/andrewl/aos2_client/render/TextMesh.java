package nl.andrewl.aos2_client.render;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBEasyFont.stb_easy_font_print;

public class TextMesh {
	private final int vbo;
	private final int vao;
	private int quadCount;

	private String text;
	private final Vector2f position;

	public TextMesh() {
		this.vbo = glGenBuffers();
		this.vao = glGenVertexArrays();
		this.position = new Vector2f();
		this.text = "Hello world";

		glBindVertexArray(vao);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 7, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, false, 7, 3);
	}

	public void updateMesh() {
		ByteBuffer colorBuffer = ByteBuffer.allocate(4);
		colorBuffer.putInt(Color.WHITE.getRGB());
		colorBuffer.flip();
		ByteBuffer vertexBuffer = BufferUtils.createByteBuffer(text.length() * 500);
		quadCount = stb_easy_font_print(position.x, position.y, text, colorBuffer, vertexBuffer);
		vertexBuffer.flip();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
	}

	public void draw() {
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBindVertexArray(vao);

		glDrawArrays(GL_QUADS, 0, quadCount);
	}
}

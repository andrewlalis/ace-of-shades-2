package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos2_client.render.ShaderProgram;
import nl.andrewl.aos2_client.util.ResourceUtils;
import nl.andrewl.aos_core.FileUtils;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Manages rendering of 2D GUI components like cross-hairs, inventory stuff, etc.
 */
public class GuiRenderer {
	private final long vgId;
	private final int jetbrainsMonoFont;
	private final ByteBuffer jetbrainsMonoFontData;

	private static final NVGColor colorA = NVGColor.create();
	private static final NVGColor colorB = NVGColor.create();
	private static final NVGColor colorC = NVGColor.create();

	private static final NVGPaint paintA = NVGPaint.create();
	private static final NVGPaint paintB = NVGPaint.create();
	private static final NVGPaint paintC = NVGPaint.create();

	private final int vaoId;
	private final int vboId;
	private final int vertexCount;
	private final ShaderProgram shaderProgram;
	private final int transformUniformLocation;
	private final int textureSamplerUniform;
	private final Matrix4f transformMatrix;
	private final float[] transformMatrixData;

	private final Map<String, GUITexture> textures = new HashMap<>();

	public GuiRenderer() throws IOException {
		vgId = nvgCreate(NVG_ANTIALIAS);
		jetbrainsMonoFontData = FileUtils.readClasspathResourceAsDirectByteBuffer("text/JetBrainsMono-Regular.ttf");
		jetbrainsMonoFont = nvgCreateFontMem(
				vgId,
				"jetbrains-mono",
				jetbrainsMonoFontData,
				0
		);

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
		textureSamplerUniform = shaderProgram.getUniform("guiTexture");
		shaderProgram.bindAttribute(0, "position");
		this.transformMatrix = new Matrix4f();
		this.transformMatrixData = new float[16];
	}

	public void loadTexture(String name, String resource) {
		textures.put(name, new GUITexture(resource));
	}

	public void addTexture(String name, GUITexture texture) {
		textures.put(name, texture);
	}

	public void start() {
		shaderProgram.use();
		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_DEPTH_TEST);
		glUniform1i(textureSamplerUniform, 0);
	}

	public void draw(String name, float scaleX, float scaleY, float x, float y) {
		draw(textures.get(name), scaleX, scaleY, x, y);
	}

	public void draw(GUITexture texture, float scaleX, float scaleY, float x, float y) {
		glActiveTexture(0);
		transformMatrix.identity()
						.translate(x, y, 0)
						.scale(scaleX, scaleY, 1)
						.get(transformMatrixData);
		glUniformMatrix4fv(transformUniformLocation, false, transformMatrixData);
		glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
		glDrawArrays(GL_TRIANGLE_STRIP, 0, vertexCount);
	}

	public void drawNvg(float width, float height) {
		nvgBeginFrame(vgId, width, height, width / height);
		nvgSave(vgId);
		nvgFontSize(vgId, 60f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgText(vgId, 50, 50, "Hello world!");
		nvgRestore(vgId);
		nvgEndFrame(vgId);
	}

	public void end() {
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
		glDisableVertexAttribArray(0);
		glBindVertexArray(0);
		shaderProgram.stopUsing();
	}

	public void free() {
		memFree(jetbrainsMonoFontData);
		nvgDelete(vgId);
		for (var tex : textures.values()) tex.free();
		glDeleteBuffers(vboId);
		glDeleteVertexArrays(vaoId);
		shaderProgram.free();
	}


}

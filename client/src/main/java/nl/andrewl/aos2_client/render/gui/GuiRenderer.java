package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.render.ShaderProgram;
import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.model.item.BlockItem;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.item.GunItemStack;
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

	// Simple 2d texture quad information.
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
		try {
			textures.put(name, new GUITexture(resource));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	public void drawNvg(float width, float height, ClientPlayer player) {
		nvgBeginFrame(vgId, width, height, width / height);
		nvgSave(vgId);
		nvgFontSize(vgId, 60f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgFillColor(vgId, GuiUtils.rgba(1, 0, 0, 1, colorA));
		nvgText(vgId, 5, 5, "Hello world!");

		drawCrosshair(width, height);
		drawHealthBar(width, height, player);
		drawHeldItemStackInfo(width, height, player);

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

	private void drawCrosshair(float w, float h) {
		float cx = w / 2f;
		float cy = h / 2f;

		nvgStrokeColor(vgId, GuiUtils.rgba(1, 1, 1, 0.25f, colorA));
		nvgBeginPath(vgId);
		nvgMoveTo(vgId, cx - 10, cy);
		nvgLineTo(vgId, cx + 10, cy);
		nvgMoveTo(vgId, cx, cy - 10);
		nvgLineTo(vgId, cx, cy + 10);
		nvgStroke(vgId);
	}

	private void drawHealthBar(float w, float h, ClientPlayer player) {
		nvgFillColor(vgId, GuiUtils.rgba(1, 0, 0, 1, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, 20, h - 60, 100, 20);
		nvgFill(vgId);
		nvgFillColor(vgId, GuiUtils.rgba(0, 1, 0, 1, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, 20, h - 60, 100 * player.getHealth(), 20);
		nvgFill(vgId);

		nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgText(vgId, 20, h - 30, String.format("%.2f / 1.00 HP", player.getHealth()));
	}

	private void drawHeldItemStackInfo(float w, float h, ClientPlayer player) {
		var stack = player.getInventory().getSelectedItemStack();
		if (stack instanceof GunItemStack g) {
			drawGunInfo(w, h, g);
		} else if (stack instanceof BlockItemStack b) {
			drawBlockInfo(w, h, b);
		}
	}

	private void drawGunInfo(float w, float h, GunItemStack stack) {
		Gun gun = (Gun) stack.getType();
		float y = h - 50;
		for (int i = 0; i < gun.getMaxClipCount(); i++) {
			float alpha = i < stack.getClipCount() ? 0.75f : 0.25f;
			nvgFillColor(vgId, GuiUtils.rgba(0.2f, 0.2f, 0.1f, alpha, colorA));
			nvgBeginPath(vgId);
			nvgRect(vgId, w - 60, y, 40, 30);
			nvgFill(vgId);
			y -= 35;
		}
		float x = w - 80;
		for (int i = 0; i < gun.getMaxBulletCount(); i++) {
			float alpha = i < stack.getBulletCount() ? 0.75f : 0.1f;
			nvgFillColor(vgId, GuiUtils.rgba(0.7f, 0.3f, 0, alpha, colorA));
			nvgBeginPath(vgId);
			nvgRect(vgId, x, h - 60, 10, 40);
			nvgFill(vgId);
			x -= 15;
		}
	}

	private void drawBlockInfo(float w, float h, BlockItemStack stack) {
		BlockItem block = (BlockItem) stack.getType();
		nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 0.75f, colorA));
		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgText(vgId, w - 140, h - 30, String.format("%d / %d Blocks", stack.getAmount(), block.getMaxAmount()));
	}
}

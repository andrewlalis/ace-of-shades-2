package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.model.Chat;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.model.OtherPlayer;
import nl.andrewl.aos2_client.render.ShaderProgram;
import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.BlockItem;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.item.GunItemStack;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

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

	private final ShaderProgram namePlateShaderProgram;
	private final int namePlateTransformUniform;
	private final int namePlateViewTransformUniform;
	private final int namePlatePerspectiveTransformUniform;
	private final int namePlateTextureSamplerUniform;
	private final Font namePlateFont;
	private final Map<OtherPlayer, GuiTexture> playerNamePlates = new HashMap<>();

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

		// Shader program for rendering name plates.
		namePlateShaderProgram = new ShaderProgram.Builder()
				.withShader("shader/gui/nameplate_vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/gui/nameplate_fragement.glsl", GL_FRAGMENT_SHADER)
				.build();
		namePlateTransformUniform = namePlateShaderProgram.getUniform("transform");
		namePlateViewTransformUniform = namePlateShaderProgram.getUniform("viewTransform");
		namePlatePerspectiveTransformUniform = namePlateShaderProgram.getUniform("perspectiveTransform");
		namePlateTextureSamplerUniform = namePlateShaderProgram.getUniform("guiTexture");
		namePlateShaderProgram.bindAttribute(0, "vertexPosition");

		try (var in = FileUtils.getClasspathResource("text/JetBrainsMono-Regular.ttf")) {
			namePlateFont = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(84f);
		} catch (FontFormatException e) {
			throw new RuntimeException(e);
		}

		this.transformMatrix = new Matrix4f();
		this.transformMatrixData = new float[16];
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

	public void draw(GuiTexture texture, float scaleX, float scaleY, float x, float y) {
		glActiveTexture(0);
		transformMatrix.identity()
						.translate(x, y, 0)
						.scale(scaleX, scaleY, 1)
						.get(transformMatrixData);
		glUniformMatrix4fv(transformUniformLocation, false, transformMatrixData);
		glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
		glDrawArrays(GL_TRIANGLE_STRIP, 0, vertexCount);
	}

	private void addNamePlate(OtherPlayer player) {
		GuiTexture texture = new GuiTexture(generateNameplateImage(player.getUsername()));
		playerNamePlates.put(player, texture);
	}

	private void removeNamePlate(OtherPlayer player) {
		GuiTexture texture = playerNamePlates.remove(player);
		texture.free();
	}

	private void retainNamePlates(Collection<OtherPlayer> players) {
		Set<OtherPlayer> removalSet = new HashSet<>(playerNamePlates.keySet());
		removalSet.removeAll(players);
		for (OtherPlayer playerToRemove : removalSet) {
			removeNamePlate(playerToRemove);
		}
	}

	public void updateNamePlates(Collection<OtherPlayer> players) {
		for (OtherPlayer player : players) {
			if (!playerNamePlates.containsKey(player)) {
				addNamePlate(player);
			}
		}
		retainNamePlates(players);
	}

	private BufferedImage generateNameplateImage(String username) {
		BufferedImage testImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D testGraphics = testImg.createGraphics();
		testGraphics.setFont(namePlateFont);
		int textWidth = testGraphics.getFontMetrics(namePlateFont).stringWidth(username);
		int textHeight = testGraphics.getFontMetrics(namePlateFont).getHeight();

		int w = textWidth + 20;
		int h = textHeight + 10;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setBackground(new Color(0, 0, 0, 0.5f));
		g.clearRect(0, 0, w, h);
		g.setColor(Color.WHITE);
		g.setFont(namePlateFont);
		g.drawString(username, 10, h - 15);
		return img;
	}

	public void drawNameplates(ClientPlayer myPlayer, float[] viewTransformData, float[] perspectiveTransformData) {
		shaderProgram.stopUsing();
		namePlateShaderProgram.use();
		glEnable(GL_DEPTH_TEST);
		glActiveTexture(0);
		glUniform1i(namePlateTextureSamplerUniform, 0);
		glUniformMatrix4fv(namePlateViewTransformUniform, false, viewTransformData);
		glUniformMatrix4fv(namePlatePerspectiveTransformUniform, false, perspectiveTransformData);
		for (var entry : playerNamePlates.entrySet()) {
			OtherPlayer player = entry.getKey();
			// Skip rendering far-away nameplates.
			if (player.getPosition().distance(myPlayer.getPosition()) > 50) continue;
			GuiTexture texture = entry.getValue();
			float aspectRatio = (float) texture.getHeight() / (float) texture.getWidth();
			transformMatrix.identity()
					.translate(player.getPosition().x(), player.getPosition().y() + Player.HEIGHT + 1f, player.getPosition().z())
					.rotate(myPlayer.getOrientation().x, Camera.UP)
					.scale(1f, aspectRatio, 0f)
					.get(transformMatrixData);
			glUniformMatrix4fv(namePlateTransformUniform, false, transformMatrixData);
			glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
			glDrawArrays(GL_TRIANGLE_STRIP, 0, vertexCount);
		}

		glBindTexture(GL_TEXTURE_2D, 0);
		glDisable(GL_DEPTH_TEST);
		namePlateShaderProgram.stopUsing();
		shaderProgram.use();
	}

	public void drawNvg(float width, float height, ClientPlayer player, Chat chat) {
		nvgBeginFrame(vgId, width, height, width / height);
		nvgSave(vgId);

		drawCrosshair(width, height);
		drawChat(width, height, chat);
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
		for (var texture : playerNamePlates.values()) {
			texture.free();
		}
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

	private void drawChat(float w, float h, Chat chat) {
		float chatWidth = w / 3;
		float chatHeight = h / 4;

		nvgFillColor(vgId, GuiUtils.rgba(0, 0, 0, 0.25f, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, 0, 0, chatWidth, chatHeight);
		nvgFill(vgId);

		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		float y = chatHeight - 12;
		for (var msg : chat.getMessages()) {
			if (msg.author().equals("_ANNOUNCE")) {
				nvgFillColor(vgId, GuiUtils.rgba(0.7f, 0, 0, 1, colorA));
				nvgText(vgId, 5, y, msg.message());
			} else if (msg.author().equals("_PRIVATE")) {
				nvgFillColor(vgId, GuiUtils.rgba(0.3f, 0.3f, 0.3f, 1, colorA));
				nvgText(vgId, 5, y, msg.message());
			} else {
				nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
				nvgText(vgId, 5, y, msg.author() + ": " + msg.message());
			}

			y -= 16;
		}
	}
}

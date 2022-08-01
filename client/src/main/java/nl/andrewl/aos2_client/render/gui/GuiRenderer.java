package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos2_client.model.OtherPlayer;
import nl.andrewl.aos2_client.render.ShaderProgram;
import nl.andrewl.aos2_client.sound.SoundSource;
import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.PlayerMode;
import nl.andrewl.aos_core.model.item.BlockItem;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.world.Hit;
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
		// Show nameplates from farther away if we're in creative/spectator.
		float nameplateRadius = myPlayer.getMode() == PlayerMode.NORMAL ? 50 : 200;
		for (var entry : playerNamePlates.entrySet()) {
			OtherPlayer player = entry.getKey();
			// There are some scenarios where we skip rendering the name.
			if (player.getPosition().distance(myPlayer.getPosition()) > nameplateRadius || player.getMode() == PlayerMode.SPECTATOR) continue;
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

	public void drawNvg(float width, float height, Client client) {
		nvgBeginFrame(vgId, width, height, width / height);
		nvgSave(vgId);

		boolean scopeEnabled = client.getInputHandler().getNormalContext().isScopeEnabled();
		PlayerMode mode = client.getMyPlayer().getMode();
		drawCrosshair(width, height, scopeEnabled);
		drawHeldItemStackInfo(width, height, client.getMyPlayer());
		if (!scopeEnabled) {
			drawChat(width, height, client);
			if (mode == PlayerMode.NORMAL) drawHealthBar(width, height, client.getMyPlayer());
		}
		if (client.getInputHandler().getNormalContext().isDebugEnabled()) {
			drawDebugInfo(width, height, client);
		}
		if (client.getInputHandler().isExitMenuContextActive()) {
			drawExitMenu(width, height);
		}

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

	private void drawCrosshair(float w, float h, boolean scopeEnabled) {
		float cx = w / 2f;
		float cy = h / 2f;
		float size = 20f;
		if (scopeEnabled) {
			size = 3f;
			nvgStrokeColor(vgId, GuiUtils.rgba(1, 0, 0, 0.5f, colorA));
		} else {
			nvgStrokeColor(vgId, GuiUtils.rgba(1, 1, 1, 0.25f, colorA));
		}

		nvgBeginPath(vgId);
		nvgMoveTo(vgId, cx - size / 2, cy);
		nvgLineTo(vgId, cx + size / 2, cy);
		nvgMoveTo(vgId, cx, cy - size / 2);
		nvgLineTo(vgId, cx, cy + size / 2);
		nvgStroke(vgId);
	}

	private void drawHealthBar(float w, float h, ClientPlayer player) {
		nvgFillColor(vgId, GuiUtils.rgba(0.6f, 0, 0, 1, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, w - 170, h - 100, 100, 20);
		nvgFill(vgId);
		nvgFillColor(vgId, GuiUtils.rgba(0, 0.6f, 0, 1, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, w - 170, h - 100, 100 * player.getHealth(), 20);
		nvgFill(vgId);

		nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgText(vgId, w - 165, h - 95, String.format("%.2f / 1.00", player.getHealth()));
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
		nvgText(vgId, w - 140, h - 14, String.format("Selected value: %d", stack.getSelectedValue()));
	}

	private void drawChat(float w, float h, Client client) {
		var chat = client.getChat();
		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		float y = h - 16 - 12;
		for (var msg : chat.getMessages()) {
			if (msg.author().equals("_ANNOUNCE")) {
				nvgFillColor(vgId, GuiUtils.rgba(0.7f, 0, 0, 1, colorA));
				nvgText(vgId, 5, y, msg.message());
			} else if (msg.author().equals("_PRIVATE")) {
				nvgFillColor(vgId, GuiUtils.rgba(0.6f, 0.6f, 0.6f, 1, colorA));
				nvgText(vgId, 5, y, msg.message());
			} else {
				nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
				nvgText(vgId, 5, y, msg.author() + ": " + msg.message());
			}
			y -= 16;
		}
		var input = client.getInputHandler();
		if (input.isChattingContextActive()) {
			nvgFillColor(vgId, GuiUtils.rgba(0, 0, 0, 0.5f, colorA));
			nvgBeginPath(vgId);
			nvgRect(vgId, 0, h - 16, w, 16);
			nvgFill(vgId);
			nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
			nvgText(vgId, 5, h - 14, "> " + input.getChattingContext().getChatBufferText() + "_");
		}
	}

	private void drawDebugInfo(float w, float h, Client client) {
		float y = h / 4 + 10;
		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
		var pos = client.getMyPlayer().getPosition();
		nvgText(vgId, 5, y, String.format("Pos: x=%.3f, y=%.3f, z=%.3f", pos.x, pos.y, pos.z));
		y += 12;
		var vel = client.getMyPlayer().getVelocity();
		nvgText(vgId, 5, y, String.format("Vel: x=%.3f, y=%.3f, z=%.3f, speed=%.3f", vel.x, vel.y, vel.z, vel.length()));
		y += 12;
		var view = client.getMyPlayer().getOrientation();
		nvgText(vgId, 5, y, String.format("View: horizontal=%.3f, vertical=%.3f", Math.toDegrees(view.x), Math.toDegrees(view.y)));
		y += 12;
		var soundSources = client.getSoundManager().getSources();
		int activeCount = (int) soundSources.stream().filter(SoundSource::isPlaying).count();
		nvgText(vgId, 5, y, String.format("Sounds: %d / %d playing", activeCount, soundSources.size()));
		y += 12;
		nvgText(vgId, 5, y, String.format("Projectiles: %d", client.getProjectiles().size()));
		y += 12;
		nvgText(vgId, 5, y, String.format("Players: %d", client.getPlayers().size()));
		y += 12;
		nvgText(vgId, 5, y, String.format("Chunks: %d", client.getWorld().getChunkMap().size()));
		y += 12;

		Hit hit = client.getWorld().getLookingAtPos(client.getMyPlayer().getEyePosition(), client.getMyPlayer().getViewVector(), 50);
		if (hit != null) {
			nvgText(vgId, 5, y, String.format("Looking at: x=%d, y=%d, z=%d", hit.pos().x, hit.pos().y, hit.pos().z));
		}
	}

	private void drawExitMenu(float width, float height) {
		nvgFillColor(vgId, GuiUtils.rgba(0, 0, 0, 0.5f, colorA));
		nvgBeginPath(vgId);
		nvgRect(vgId, 0, 0, width, height);
		nvgFill(vgId);

		nvgFontSize(vgId, 12f);
		nvgFontFaceId(vgId, jetbrainsMonoFont);
		nvgTextAlign(vgId, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		nvgFillColor(vgId, GuiUtils.rgba(1, 1, 1, 1, colorA));
		nvgText(vgId, width / 2f, height / 2f, "Press ESC to quit. Press any other key to return to the game.");
	}
}

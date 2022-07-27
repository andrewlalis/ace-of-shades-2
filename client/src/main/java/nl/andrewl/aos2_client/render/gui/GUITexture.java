package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos_core.FileUtils;
import nl.andrewl.aos_core.ImageUtils;
import org.joml.Vector2f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a texture loaded onto the GPU.
 */
public class GUITexture {
	private final int textureId;
	/**
	 * The original image's width.
	 */
	private final int width;
	/**
	 * The original image's height.
	 */
	private final int height;

	public GUITexture(String location) throws IOException {
		this(FileUtils.readClasspathImage(location));
	}

	public GUITexture(BufferedImage img) {
		width = img.getWidth();
		height = img.getHeight();

		textureId = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureId);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		var buf = ImageUtils.decodePng(img);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float getAspectRatio() {
		return (float) width / height;
	}

	public float getIdealHeight(float width) {
		return width / getAspectRatio();
	}

	public float getIdealWidth(float height) {
		return height * getAspectRatio();
	}

	public float getIdealScaleX(float desiredWidth, float screenWidth) {
		return desiredWidth / screenWidth;
	}

	public float getIdealScaleY(float desiredHeight, float screenHeight) {
		return desiredHeight / screenHeight;
	}

	public int getTextureId() {
		return textureId;
	}

	public void free() {
		glDeleteTextures(textureId);
	}
}

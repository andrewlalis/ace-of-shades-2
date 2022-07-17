package nl.andrewl.aos2_client.render.gui;

import nl.andrewl.aos_core.ImageUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.lwjgl.opengl.GL46.*;

public class GUITexture {
	private final int textureId;
	private final int width;
	private final int height;
	private final Vector2f position = new Vector2f(0, 0);
	private final Vector2f scale = new Vector2f(1, 1);

	public GUITexture(String location) throws IOException {
		try (var in = GUITexture.class.getClassLoader().getResourceAsStream(location)) {
			if (in == null) throw new IOException("Couldn't load texture image from " + location);
			BufferedImage img = ImageIO.read(in);
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
	}

	public int getTextureId() {
		return textureId;
	}

	public Vector2f getPosition() {
		return position;
	}

	public Vector2f getScale() {
		return scale;
	}

	public void free() {
		glDeleteTextures(textureId);
	}
}

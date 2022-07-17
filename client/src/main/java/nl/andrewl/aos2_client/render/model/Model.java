package nl.andrewl.aos2_client.render.model;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import nl.andrewl.aos_core.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a 3D model with a texture, which can be used to render one or
 * more entities.
 */
public class Model {
	private final int vaoId;
	private final List<Integer> vboIds;
	private final int eboId;
	private final int indexCount;
	private final int textureId;

	public Model(String resource, String textureResource) throws IOException {
		try (
				var in = Model.class.getClassLoader().getResourceAsStream(resource);
				var imageIn = Model.class.getClassLoader().getResourceAsStream(textureResource)
		) {
			if (in == null) throw new IOException("Could not load resource: " + resource);
			if (imageIn == null) throw new IOException("Could not load texture image: " + textureResource);
			Obj obj = ObjReader.read(in);
			obj = ObjUtils.convertToRenderable(obj);
			IntBuffer indices = ObjData.getFaceVertexIndices(obj, 3);
			FloatBuffer vertices = ObjData.getVertices(obj);
			FloatBuffer texCoords = ObjData.getTexCoords(obj, 2, true);
			FloatBuffer normals = ObjData.getNormals(obj);
			indexCount = indices.limit();

			vboIds = new ArrayList<>(4);

			vaoId = glGenVertexArrays();
			glBindVertexArray(vaoId);

			// Position data
			int vboId = glGenBuffers();
			vboIds.add(vboId);
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

			// Normal data
			vboId = glGenBuffers();
			vboIds.add(vboId);
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW);
			glEnableVertexAttribArray(1);
			glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

			// Texture data
			vboId = glGenBuffers();
			vboIds.add(vboId);
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, texCoords, GL_STATIC_DRAW);
			glEnableVertexAttribArray(2); // Texture coordinates
			glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);

			// Index data
			eboId = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

			textureId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, textureId);
			BufferedImage img = ImageIO.read(imageIn);
			int w = img.getWidth();
			int h = img.getHeight();
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			ByteBuffer imageBuffer = ImageUtils.decodePng(img);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
		}
	}

	public void bind() {
		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
	}

	public void draw() {
		glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
	}

	public void unbind() {
		glBindVertexArray(0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void free() {
		glDeleteTextures(textureId);
		glDeleteBuffers(eboId);
		for (var vboId : vboIds) {
			glDeleteBuffers(vboId);
		}
		glDeleteVertexArrays(vaoId);
	}
}

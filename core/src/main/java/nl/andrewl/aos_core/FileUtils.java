package nl.andrewl.aos_core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class FileUtils {
	private FileUtils() {}

	/**
	 * Reads a classpath resource as a string.
	 * @param resource The resource to read.
	 * @return The string contents of the resource.
	 * @throws IOException If the resource can't be found or read.
	 */
	public static String readClasspathFile(String resource) throws IOException {
		try (InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) throw new IOException("Could not load classpath resource: " + resource);
			return new String(in.readAllBytes());
		}
	}

	public static BufferedImage readClasspathImage(String resource) throws IOException {
		try (var in = FileUtils.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) throw new IOException("Couldn't load texture image from " + resource);
			return ImageIO.read(in);
		}
	}

	/**
	 * Reads a classpath resource into a directly-allocated byte buffer that
	 * must be deallocated manually.
	 * @param resource The resource to read.
	 * @return The byte buffer containing the resource.
	 * @throws IOException If the resource can't be found or read.
	 */
	public static ByteBuffer readClasspathResourceAsDirectByteBuffer(String resource) throws IOException {
		try (InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) throw new IOException("Could not load classpath resource: " + resource);
			byte[] bytes = in.readAllBytes();
			ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
			buffer.put(bytes);
			buffer.flip();
			return buffer;
		}
	}
}

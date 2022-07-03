package nl.andrewl.aos_core;

import java.io.IOException;
import java.io.InputStream;

public final class FileUtils {
	private FileUtils() {}

	public static String readClasspathFile(String resource) throws IOException {
		try (InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) throw new IOException("Could not load classpath resource: " + resource);
			return new String(in.readAllBytes());
		}
	}
}

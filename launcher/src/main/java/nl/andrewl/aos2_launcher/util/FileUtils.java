package nl.andrewl.aos2_launcher.util;

import nl.andrewl.aos2_launcher.model.ProgressReporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class FileUtils {
	public static String humanReadableByteCountSI(long bytes) {
		if (-1000 < bytes && bytes < 1000) {
			return bytes + " B";
		}
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	public static String humanReadableByteCountBin(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format("%.1f %ciB", value / 1024.0, ci.current());
	}

	public static void deleteRecursive(Path p) throws IOException {
		Files.walkFileTree(p, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void downloadWithProgress(Path outputFile, HttpResponse<InputStream> resp, ProgressReporter reporter) throws IOException {
		reporter.setProgress(0);
		long size = resp.headers().firstValueAsLong("Content-Length").orElse(1);
		try (var out = Files.newOutputStream(outputFile); var in = resp.body()) {
			byte[] buffer = new byte[8192];
			long bytesRead = 0;
			while (bytesRead < size) {
				int readCount = in.read(buffer);
				out.write(buffer, 0, readCount);
				bytesRead += readCount;
				reporter.setProgress((double) bytesRead / size);
			}
		}
	}
}

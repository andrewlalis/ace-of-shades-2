package nl.andrewl.aos2_launcher;

import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.util.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

public class SystemVersionValidator {
	private static final String os = System.getProperty("os.name").trim().toLowerCase();
	private static final String arch = System.getProperty("os.arch").trim().toLowerCase();

	private static final boolean OS_WINDOWS = os.contains("win");
	private static final boolean OS_MAC = os.contains("mac");
	private static final boolean OS_LINUX = os.contains("nix") || os.contains("nux") || os.contains("aix");

	private static final boolean ARCH_X86 = arch.equals("x86");
	private static final boolean ARCH_X86_64 = arch.equals("x86_64");
	private static final boolean ARCH_AMD64 = arch.equals("amd64");
	private static final boolean ARCH_AARCH64 = arch.equals("aarch64");
	private static final boolean ARCH_ARM = arch.equals("arm");
	private static final boolean ARCH_ARM32 = arch.equals("arm32");

	private static final String JRE_DOWNLOAD_URL = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4+8/";

	public static String getPreferredVersionSuffix() {
		if (OS_LINUX) {
			if (ARCH_AARCH64) return "linux-aarch64";
			if (ARCH_AMD64) return "linux-amd64";
			if (ARCH_ARM) return "linux-arm";
			if (ARCH_ARM32) return "linux-arm32";
		} else if (OS_MAC) {
			if (ARCH_AARCH64) return "macos-aarch64";
			if (ARCH_X86_64) return "macos-x86_64";
		} else if (OS_WINDOWS) {
			if (ARCH_AARCH64) return "windows-aarch64";
			if (ARCH_AMD64) return "windows-amd64";
			if (ARCH_X86) return "windows-x86";
		}
		System.err.println("Couldn't determine the preferred OS/ARCH version. Defaulting to windows-amd64.");
		return "windows-amd64";
	}

	public static CompletableFuture<Path> getJreExecutablePath(ProgressReporter progressReporter) {
		Optional<Path> optionalExecutablePath = findJreExecutable();
		return optionalExecutablePath.map(CompletableFuture::completedFuture)
				.orElseGet(() -> downloadAppropriateJre(progressReporter));
	}

	public static CompletableFuture<Path> downloadAppropriateJre(ProgressReporter progressReporter) {
		progressReporter.enableProgress();
		progressReporter.setActionText("Downloading JRE...");
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().GET().timeout(Duration.ofMinutes(5));
		String jreArchiveName = getPreferredJreName();
		String url = JRE_DOWNLOAD_URL + jreArchiveName;
		HttpRequest req = requestBuilder.uri(URI.create(url)).build();
		return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						// Download sequentially, and update the progress.
						try {
							if (Files.exists(Launcher.JRE_PATH)) {
								FileUtils.deleteRecursive(Launcher.JRE_PATH);
							}
							Files.createDirectory(Launcher.JRE_PATH);
							Path jreArchiveFile = Launcher.JRE_PATH.resolve(jreArchiveName);
							FileUtils.downloadWithProgress(jreArchiveFile, resp, progressReporter);
							progressReporter.setProgress(-1); // Indefinite progress.
							progressReporter.setActionText("Unpacking JRE...");
							ProcessBuilder pb = new ProcessBuilder().inheritIO();
							if (OS_LINUX || OS_MAC) {
								pb.command("tar", "-xzf", jreArchiveFile.toAbsolutePath().toString(), "-C", Launcher.JRE_PATH.toAbsolutePath().toString());
							} else if (OS_WINDOWS) {
								pb.command("powershell", "-command", "\"Expand-Archive -Force '" + jreArchiveFile.toAbsolutePath() + "' '" + Launcher.JRE_PATH.toAbsolutePath() + "'\"");
							}
							Process process = pb.start();
							int result = process.waitFor();
							if (result != 0) throw new IOException("Archive extraction process exited with non-zero code: " + result);
							Files.delete(jreArchiveFile);
							progressReporter.setActionText("Looking for java executable...");
							Optional<Path> optionalExecutablePath = findJreExecutable();
							if (optionalExecutablePath.isEmpty()) throw new IOException("Couldn't find java executable.");
							progressReporter.disableProgress();
							return optionalExecutablePath.get();
						} catch (IOException | InterruptedException e) {
							throw new RuntimeException(e);
						}
					} else {
						throw new RuntimeException("JRE download failed: " + resp.statusCode());
					}
				});
	}

	private static Optional<Path> findJreExecutable() {
		if (!Files.exists(Launcher.JRE_PATH)) return Optional.empty();
		BiPredicate<Path, BasicFileAttributes> pred = (path, basicFileAttributes) -> {
			String filename = path.getFileName().toString();
			return Files.isExecutable(path) && (filename.equals("java") || filename.equals("java.exe"));
		};
		try (var s = Files.find(Launcher.JRE_PATH, 3, pred)) {
			return s.findFirst();
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	private static String getPreferredJreName() {
		if (OS_LINUX) {
			if (ARCH_AARCH64) return "OpenJDK17U-jre_aarch64_linux_hotspot_17.0.4_8.tar.gz";
			if (ARCH_AMD64) return "OpenJDK17U-jre_x64_linux_hotspot_17.0.4_8.tar.gz";
			if (ARCH_ARM || ARCH_ARM32) return "OpenJDK17U-jre_arm_linux_hotspot_17.0.4_8.tar.gz";
		} else if (OS_MAC) {
			if (ARCH_AARCH64) return "OpenJDK17U-jre_aarch64_mac_hotspot_17.0.4_8.tar.gz";
			if (ARCH_X86_64) return "OpenJDK17U-jre_x64_mac_hotspot_17.0.4_8.tar.gz";
		} else if (OS_WINDOWS) {
			if (ARCH_AARCH64 || ARCH_AMD64) return "OpenJDK17U-jre_x64_windows_hotspot_17.0.4_8.zip";
			if (ARCH_X86) return "OpenJDK17U-jre_x86-32_windows_hotspot_17.0.4_8.zip";
		}
		System.err.println("Couldn't determine the preferred JRE version. Defaulting to x64_windows.");
		return "OpenJDK17U-jre_x64_windows_hotspot_17.0.4_8.zip";
	}
}

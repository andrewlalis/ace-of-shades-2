package nl.andrewl.aos2_launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.andrewl.aos2_launcher.model.ClientVersionRelease;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.util.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionFetcher {
	private static final String BASE_GITHUB_URL = "https://api.github.com/repos/andrewlalis/ace-of-shades-2";

	public static final VersionFetcher INSTANCE = new VersionFetcher();

	private final List<ClientVersionRelease> availableReleases;

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
	private boolean loaded = false;
	private CompletableFuture<List<ClientVersionRelease>> activeReleaseFetchFuture;

	public VersionFetcher() {
		this.availableReleases = new ArrayList<>();
	}

	public CompletableFuture<ClientVersionRelease> getRelease(String versionTag) {
		return getAvailableReleases().thenApply(releases -> releases.stream()
				.filter(r -> r.tag().equals(versionTag))
				.findFirst().orElse(null));
	}

	public CompletableFuture<List<ClientVersionRelease>> getAvailableReleases() {
		if (loaded) {
			return CompletableFuture.completedFuture(Collections.unmodifiableList(availableReleases));
		}
		return fetchReleasesFromGitHub();
	}

	private CompletableFuture<List<ClientVersionRelease>> fetchReleasesFromGitHub() {
		if (activeReleaseFetchFuture != null) return activeReleaseFetchFuture;
		HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_GITHUB_URL + "/releases"))
				.timeout(Duration.ofSeconds(3))
				.GET()
				.build();
		activeReleaseFetchFuture = httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						JsonArray releasesArray = new Gson().fromJson(new InputStreamReader(resp.body()), JsonArray.class);
						availableReleases.clear();
						for (var element : releasesArray) {
							if (element.isJsonObject()) {
								JsonObject obj = element.getAsJsonObject();
								String tag = obj.get("tag_name").getAsString();
								String apiUrl = obj.get("url").getAsString();
								String assetsUrl = obj.get("assets_url").getAsString();
								OffsetDateTime publishedAt = OffsetDateTime.parse(obj.get("published_at").getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
								LocalDateTime localPublishedAt = publishedAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
								availableReleases.add(new ClientVersionRelease(tag, apiUrl, assetsUrl, localPublishedAt));
							}
						}
						availableReleases.sort(Comparator.comparing(ClientVersionRelease::publishedAt).reversed());
						loaded = true;
						return availableReleases;
					} else {
						throw new RuntimeException("Error while requesting releases.");
					}
				});
		return activeReleaseFetchFuture;
	}

	public List<String> getDownloadedVersions() {
		try (var s = Files.list(Launcher.VERSIONS_DIR)) {
			return s.filter(this::isVersionFile)
					.map(this::extractVersion)
					.toList();
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public CompletableFuture<Path> ensureVersionIsDownloaded(String versionTag, ProgressReporter progressReporter) {
		try (var s = Files.list(Launcher.VERSIONS_DIR)) {
			Optional<Path> optionalFile = s.filter(f -> isVersionFile(f) && versionTag.equals(extractVersion(f)))
					.findFirst();
			if (optionalFile.isPresent()) return CompletableFuture.completedFuture(optionalFile.get());
		} catch (IOException e) {
			return CompletableFuture.failedFuture(e);
		}
		progressReporter.enableProgress();
		progressReporter.setActionText("Downloading client " + versionTag + "...");
		var future = getRelease(versionTag)
				.thenComposeAsync(release -> downloadVersion(release, progressReporter));
		future.thenRun(progressReporter::disableProgress);
		return future;
	}

	private CompletableFuture<Path> downloadVersion(ClientVersionRelease release, ProgressReporter progressReporter) {
		System.out.println("Downloading version " + release.tag());
		HttpRequest req = HttpRequest.newBuilder(URI.create(release.assetsUrl()))
				.GET().timeout(Duration.ofSeconds(3)).build();
		CompletableFuture<JsonObject> downloadUrlFuture = httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						JsonArray assetsArray = new Gson().fromJson(new InputStreamReader(resp.body()), JsonArray.class);
						String preferredVersionSuffix = SystemVersionValidator.getPreferredVersionSuffix();
						String regex = "aos2-client-\\d+\\.\\d+\\.\\d+-" + preferredVersionSuffix + "\\.jar";
						for (var asset : assetsArray) {
							JsonObject assetObj = asset.getAsJsonObject();
							String name = assetObj.get("name").getAsString();
							if (name.matches(regex)) {
								return assetObj;
							}
						}
						throw new RuntimeException("Couldn't find a matching release asset for this system.");
					} else {
						throw new RuntimeException("Error while requesting release assets from GitHub: " + resp.statusCode());
					}
				});
		return downloadUrlFuture.thenComposeAsync(asset -> {
			String url = asset.get("browser_download_url").getAsString();
			String fileName = asset.get("name").getAsString();
			HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(url))
					.GET().timeout(Duration.ofMinutes(5)).build();
			Path file = Launcher.VERSIONS_DIR.resolve(fileName);
			return httpClient.sendAsync(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())
					.thenApplyAsync(resp -> {
						if (resp.statusCode() == 200) {
							// Download sequentially, and update the progress.
							try {
								FileUtils.downloadWithProgress(file, resp, progressReporter);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
							return file;
						} else {
							throw new RuntimeException("Error while downloading release asset from GitHub: " + resp.statusCode());
						}
					});
		});
	}

	private boolean isVersionDownloaded(String versionTag) {
		return getDownloadedVersions().contains(versionTag);
	}

	private boolean isVersionFile(Path p) {
		return Files.isRegularFile(p) && p.getFileName().toString()
				.matches("aos2-client-\\d+\\.\\d+\\.\\d+-.+\\.jar");
	}

	private String extractVersion(Path file) {
		Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
		Matcher matcher = pattern.matcher(file.getFileName().toString());
		if (matcher.find()) {
			return "v" + matcher.group();
		}
		throw new IllegalArgumentException("File doesn't contain a valid version pattern.");
	}
}

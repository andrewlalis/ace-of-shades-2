package nl.andrewl.aos_core.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Config {
	private static final Yaml yaml = new Yaml();

	private Config() {}

	/**
	 * Loads a configuration YAML object from the first available path.
	 * @param configType The type of the configuration object.
	 * @param paths The paths to load from.
	 * @param fallback A default configuration object to use if no config could
	 *                 be loaded from any of the paths.
	 * @return The configuration object.
	 * @param <T> The type of the configuration object.
	 */
	public static <T> T loadConfig(Class<T> configType, List<Path> paths, T fallback) {
		for (var path : paths) {
			if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
				try (var reader = Files.newBufferedReader(path)) {
					return yaml.loadAs(reader, configType);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return fallback;
	}

	public static <T> T loadConfig(Class<T> configType, List<Path> paths) {
		var cfg = loadConfig(configType, paths, null);
		if (cfg == null) {
			throw new RuntimeException("Could not load config from any of the supplied paths.");
		}
		return cfg;
	}

	public static <T> T loadConfig(Class<T> configType, T fallback, Path... paths) {
		return loadConfig(configType, List.of(paths), fallback);
	}

	public static List<Path> getCommonConfigPaths() {
		List<Path> paths = new ArrayList<>();
		paths.add(Path.of("config.yaml"));
		paths.add(Path.of("config.yml"));
		paths.add(Path.of("cfg.yaml"));
		paths.add(Path.of("cfg.yml"));
		return paths;
	}
}

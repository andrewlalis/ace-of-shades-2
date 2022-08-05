package nl.andrewl.aos2_server;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Component that sends regular updates to any configured server registries.
 */
public class RegistryUpdater {
	private final Server server;
	private final HttpClient httpClient;

	public RegistryUpdater(Server server) {
		this.server = server;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.build();
	}

	public void sendUpdates() {
		var cfg = server.getConfig();
		if (
				cfg.registries != null &&
				cfg.registries.length > 0 &&
				cfg.name != null && !cfg.name.isBlank()
		) {
			Map<String, Object> data = new HashMap<>();
			data.put("port", cfg.port);
			data.put("name", cfg.name);
			data.put("description", cfg.description);
			data.put("maxPlayers", cfg.maxPlayers);
			data.put("currentPlayers", server.getPlayerManager().getPlayers().size());
			String json = new Gson().toJson(data);
			for (String registryUrl : server.getConfig().registries) {
				HttpRequest req = HttpRequest.newBuilder(URI.create(registryUrl + "/servers"))
						.POST(HttpRequest.BodyPublishers.ofString(json))
						.header("Content-Type", "application/json")
						.timeout(Duration.ofSeconds(3))
						.build();
				try {
					var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
					if (resp.statusCode() != 200) {
						System.err.println("Error response when sending registry update to " + registryUrl + ": " + resp.statusCode() + " " + resp.body());
					}
				} catch (IOException | InterruptedException e) {
					System.err.println("An error occurred while sending registry update to " + registryUrl + ": " + e.getMessage());
				}
			}
		}
	}
}

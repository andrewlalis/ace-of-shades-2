package nl.andrewl.aos2_launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nl.andrewl.aos2_launcher.model.Server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class ServersFetcher {
	private static final String registryURL = "http://localhost:8080";

	private final HttpClient httpClient;
	private final Gson gson;

	public ServersFetcher() {
		httpClient = HttpClient.newBuilder().build();
		gson = new Gson();
	}

	public CompletableFuture<List<Server>> fetchServers() {
		HttpRequest req = HttpRequest.newBuilder(URI.create(registryURL + "/servers"))
				.GET()
				.timeout(Duration.ofSeconds(3))
				.header("Accept", "application/json")
				.build();
		return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						JsonArray serversArray = gson.fromJson(resp.body(), JsonArray.class);
						List<Server> servers = new ArrayList<>(serversArray.size());
						for (JsonElement serverJson : serversArray) {
							if (serverJson instanceof JsonObject obj) {
								servers.add(new Server(
										obj.get("host").getAsString(),
										obj.get("port").getAsInt(),
										obj.get("name").getAsString(),
										obj.get("description").getAsString(),
										obj.get("maxPlayers").getAsInt(),
										obj.get("currentPlayers").getAsInt(),
										obj.get("lastUpdatedAt").getAsLong()
								));
							}
						}
						return servers;
					} else {
						throw new RuntimeException("Invalid response: " + resp.statusCode());
					}
				});
	}
}

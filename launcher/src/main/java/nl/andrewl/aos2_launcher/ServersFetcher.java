package nl.andrewl.aos2_launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.stage.Window;
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
	private final HttpClient httpClient;
	private final Gson gson;
	private final StringProperty registryUrl;

	public ServersFetcher(StringProperty registryUrlProperty) {
		httpClient = HttpClient.newBuilder().build();
		gson = new Gson();
		this.registryUrl = new SimpleStringProperty("http://localhost:8080");
		registryUrl.bind(registryUrlProperty);
	}

	public CompletableFuture<List<Server>> fetchServers(Window owner) {
		if (registryUrl.get() == null || registryUrl.get().isBlank()) {
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setContentText("Invalid or missing registry URL. Can't fetch the list of servers.");
				alert.initOwner(owner);
				alert.show();
			});
			return CompletableFuture.completedFuture(new ArrayList<>());
		}
		HttpRequest req = HttpRequest.newBuilder(URI.create(registryUrl.get() + "/servers"))
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

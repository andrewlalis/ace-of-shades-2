package nl.andrewl.aos2_launcher.model;

import javafx.beans.property.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Server {
	private final StringProperty host;
	private final IntegerProperty port;
	private final StringProperty name;
	private final StringProperty description;
	private final IntegerProperty maxPlayers;
	private final IntegerProperty currentPlayers;
	private final ObjectProperty<LocalDateTime> lastUpdatedAt;

	public Server(String host, int port, String name, String description, int maxPlayers, int currentPlayers, long lastUpdatedAt) {
		this.host = new SimpleStringProperty(host);
		this.port = new SimpleIntegerProperty(port);
		this.name = new SimpleStringProperty(name);
		this.description = new SimpleStringProperty(description);
		this.maxPlayers = new SimpleIntegerProperty(maxPlayers);
		this.currentPlayers = new SimpleIntegerProperty(currentPlayers);
		LocalDateTime ts = Instant.ofEpochMilli(lastUpdatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime();
		this.lastUpdatedAt = new SimpleObjectProperty<>(ts);
	}

	public String getHost() {
		return host.get();
	}

	public StringProperty hostProperty() {
		return host;
	}

	public int getPort() {
		return port.get();
	}

	public IntegerProperty portProperty() {
		return port;
	}

	public String getName() {
		return name.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public String getDescription() {
		return description.get();
	}

	public StringProperty descriptionProperty() {
		return description;
	}

	public int getMaxPlayers() {
		return maxPlayers.get();
	}

	public IntegerProperty maxPlayersProperty() {
		return maxPlayers;
	}

	public int getCurrentPlayers() {
		return currentPlayers.get();
	}

	public IntegerProperty currentPlayersProperty() {
		return currentPlayers;
	}

	public LocalDateTime getLastUpdatedAt() {
		return lastUpdatedAt.get();
	}

	public Property<LocalDateTime> lastUpdatedAtProperty() {
		return lastUpdatedAt;
	}
}

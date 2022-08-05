package nl.andrewl.aos2registryapi.model;

import java.time.Instant;

public class ServerInfo {
	private String name;
	private String description;
	private int maxPlayers;
	private int currentPlayers;
	private Instant lastUpdatedAt;

	public ServerInfo(String name, String description, int maxPlayers, int currentPlayers) {
		this.name = name;
		this.description = description;
		this.maxPlayers = maxPlayers;
		this.currentPlayers = currentPlayers;
		this.lastUpdatedAt = Instant.now();
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public int getCurrentPlayers() {
		return currentPlayers;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public void setCurrentPlayers(int currentPlayers) {
		this.currentPlayers = currentPlayers;
	}

	public void setLastUpdatedAt(Instant lastUpdatedAt) {
		this.lastUpdatedAt = lastUpdatedAt;
	}
}

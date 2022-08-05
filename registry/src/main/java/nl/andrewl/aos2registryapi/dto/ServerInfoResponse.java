package nl.andrewl.aos2registryapi.dto;

public record ServerInfoResponse (
		String host,
		int port,
		String name,
		String description,
		int maxPlayers,
		int currentPlayers,
		long lastUpdatedAt
) {}

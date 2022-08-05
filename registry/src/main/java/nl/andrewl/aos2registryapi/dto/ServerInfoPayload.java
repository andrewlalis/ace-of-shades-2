package nl.andrewl.aos2registryapi.dto;

public record ServerInfoPayload (
		int port,
		String name,
		String description,
		int maxPlayers,
		int currentPlayers
) {}

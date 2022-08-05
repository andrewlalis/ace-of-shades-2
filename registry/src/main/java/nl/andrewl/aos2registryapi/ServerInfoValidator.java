package nl.andrewl.aos2registryapi;

import nl.andrewl.aos2registryapi.dto.ServerInfoPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerInfoValidator {

	public boolean validateName(String name) {
		return name != null && !name.isBlank() && name.length() <= 64;
	}

	public boolean validateDescription(String description) {
		return description == null ||
				(!description.isBlank() && description.length() <= 256);
	}

	public boolean validatePlayerCounts(int max, int current) {
		return max > 0 && current >= 0 && current <= max && max < 1000;
	}

	public Optional<List<String>> validatePayload(ServerInfoPayload payload) {
		List<String> messages = new ArrayList<>(3);
		if (payload.port() < 0 || payload.port() > 65535) messages.add("Invalid port.");
		if (!validateName(payload.name())) messages.add("Invalid name.");
		if (!validateDescription(payload.description())) messages.add("Invalid description.");
		if (!validatePlayerCounts(payload.maxPlayers(), payload.currentPlayers())) messages.add("Invalid player counts.");
		if (messages.size() > 0) return Optional.of(messages);
		return Optional.empty();
	}
}

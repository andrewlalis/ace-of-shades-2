package nl.andrewl.aos2registryapi;

import nl.andrewl.aos2registryapi.dto.ServerInfoPayload;
import nl.andrewl.aos2registryapi.dto.ServerInfoResponse;
import nl.andrewl.aos2registryapi.model.ServerIdentifier;
import nl.andrewl.aos2registryapi.model.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class ServerRegistry {
	private static final Logger log = LoggerFactory.getLogger(ServerRegistry.class);

	public static final Duration SERVER_TIMEOUT = Duration.ofMinutes(3);
	public static final Duration SERVER_MIN_UPDATE = Duration.ofSeconds(5);

	private final Map<ServerIdentifier, ServerInfo> servers = new ConcurrentHashMap<>();
	private final ServerInfoValidator infoValidator = new ServerInfoValidator();

	public Flux<ServerInfoResponse> getServers() {
		Stream<ServerInfoResponse> stream = servers.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getValue().getLastUpdatedAt()))
				.map(entry -> new ServerInfoResponse(
						entry.getKey().host(),
						entry.getKey().port(),
						entry.getValue().getName(),
						entry.getValue().getDescription(),
						entry.getValue().getMaxPlayers(),
						entry.getValue().getCurrentPlayers(),
						entry.getValue().getLastUpdatedAt().toEpochMilli()
				));
		return Flux.fromStream(stream);
	}

	public void acceptInfo(ServerIdentifier ident, ServerInfoPayload payload) {
		var result = infoValidator.validatePayload(payload);
		if (result.isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", result.get()));
		}
		ServerInfo info = servers.get(ident);
		if (info != null) {
			Instant now = Instant.now();
			// Check if this update was sent too fast.
			if (Duration.between(info.getLastUpdatedAt(), now).compareTo(SERVER_MIN_UPDATE) < 0) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Server update rate limit exceeded.");
			}
			// Update existing server.
			info.setName(payload.name());
			info.setDescription(payload.description());
			info.setMaxPlayers(payload.maxPlayers());
			info.setCurrentPlayers(payload.currentPlayers());
			info.setLastUpdatedAt(now);
		} else {
			// Save new server.
			servers.put(ident, new ServerInfo(payload.name(), payload.description(), payload.maxPlayers(), payload.currentPlayers()));
		}
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
	public void purgeOldServers() {
		Queue<ServerIdentifier> removalQueue = new LinkedList<>();
		final Instant cutoff = Instant.now().minus(SERVER_TIMEOUT);
		for (var entry : servers.entrySet()) {
			var ident = entry.getKey();
			var server = entry.getValue();
			if (server.getLastUpdatedAt().isBefore(cutoff)) {
				removalQueue.add(ident);
			}
		}
		while (!removalQueue.isEmpty()) {
			servers.remove(removalQueue.remove());
		}
	}
}

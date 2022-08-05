package nl.andrewl.aos2registryapi.api;

import nl.andrewl.aos2registryapi.ServerRegistry;
import nl.andrewl.aos2registryapi.dto.ServerInfoPayload;
import nl.andrewl.aos2registryapi.dto.ServerInfoResponse;
import nl.andrewl.aos2registryapi.model.ServerIdentifier;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/servers")
public class ServersController {
	private final ServerRegistry serverRegistry;

	public ServersController(ServerRegistry serverRegistry) {
		this.serverRegistry = serverRegistry;
	}

	@GetMapping
	public Flux<ServerInfoResponse> getServers() {
		return serverRegistry.getServers();
	}

	@PostMapping
	public Mono<ResponseEntity<Object>> updateServer(ServerHttpRequest req, @RequestBody Mono<ServerInfoPayload> payloadMono) {
		String host = req.getRemoteAddress().getAddress().getHostAddress();
		return payloadMono.mapNotNull(payload -> {
			ServerIdentifier ident = new ServerIdentifier(host, payload.port());
			serverRegistry.acceptInfo(ident, payload);
			return ResponseEntity.ok(null);
		});
	}
}

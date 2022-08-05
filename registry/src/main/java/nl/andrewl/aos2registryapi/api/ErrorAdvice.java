package nl.andrewl.aos2registryapi.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorAdvice {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<?> handleRSE(ResponseStatusException e) {
		Map<String, Object> data = new HashMap<>();
		data.put("code", e.getRawStatusCode());
		data.put("message", e.getReason());
		return ResponseEntity.status(e.getStatus()).body(data);
	}
}

package nl.andrewl.aos2_launcher.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record ClientVersionRelease (
		String tag,
		String apiUrl,
		String assetsUrl,
		LocalDateTime publishedAt
) {}

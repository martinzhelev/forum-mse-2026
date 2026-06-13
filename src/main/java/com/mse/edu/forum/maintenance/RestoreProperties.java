package com.mse.edu.forum.maintenance;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.restore")
public record RestoreProperties(boolean enabled, long retryAfterSeconds, List<String> allowlist) {

	public RestoreProperties {
		retryAfterSeconds = Math.max(1L, retryAfterSeconds);
		allowlist = allowlist == null ? List.of() : List.copyOf(allowlist);
	}
}

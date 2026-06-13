package com.mse.edu.forum.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceModeState {

	private final long defaultRetryAfterSeconds;

	private volatile boolean restoreInProgress;
	private volatile Instant restoreStartedAt;
	private volatile Instant estimatedCompletionAt;

	public MaintenanceModeState(
			@Value("${forum.maintenance.default-retry-after-seconds:120}")
			long defaultRetryAfterSeconds) {
		this.defaultRetryAfterSeconds = Math.max(1, defaultRetryAfterSeconds);
	}

	public synchronized StatusSnapshot startRestore(Long estimatedDurationSeconds) {
		restoreInProgress = true;
		restoreStartedAt = Instant.now();
		if (estimatedDurationSeconds != null && estimatedDurationSeconds > 0) {
			estimatedCompletionAt = restoreStartedAt.plusSeconds(estimatedDurationSeconds);
		} else {
			estimatedCompletionAt = null;
		}
		return snapshot();
	}

	public synchronized StatusSnapshot finishRestore() {
		restoreInProgress = false;
		restoreStartedAt = null;
		estimatedCompletionAt = null;
		return snapshot();
	}

	public synchronized StatusSnapshot snapshot() {
		long retryAfter = defaultRetryAfterSeconds;
		if (restoreInProgress && estimatedCompletionAt != null) {
			long remaining = estimatedCompletionAt.getEpochSecond() - Instant.now().getEpochSecond();
			retryAfter = Math.max(1, remaining);
		}
		return new StatusSnapshot(
				restoreInProgress, retryAfter, restoreStartedAt, estimatedCompletionAt);
	}

	public record StatusSnapshot(
			boolean restoreInProgress,
			long retryAfterSeconds,
			Instant restoreStartedAt,
			Instant estimatedCompletionAt) {}
}

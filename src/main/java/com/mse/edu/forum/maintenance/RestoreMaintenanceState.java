package com.mse.edu.forum.maintenance;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class RestoreMaintenanceState {

	private final RestoreProperties properties;
	private final AtomicBoolean restoreInProgress;

	public RestoreMaintenanceState(RestoreProperties properties) {
		this.properties = properties;
		this.restoreInProgress = new AtomicBoolean(properties.enabled());
	}

	public boolean isRestoreInProgress() {
		return restoreInProgress.get();
	}

	public long getRetryAfterSeconds() {
		return properties.retryAfterSeconds();
	}

	public void startRestore() {
		restoreInProgress.set(true);
	}

	public void finishRestore() {
		restoreInProgress.set(false);
	}

	public boolean setRestoreInProgress(boolean enabled) {
		restoreInProgress.set(enabled);
		return restoreInProgress.get();
	}
}

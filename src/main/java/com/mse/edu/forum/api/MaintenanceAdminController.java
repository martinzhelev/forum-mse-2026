package com.mse.edu.forum.api;

import com.mse.edu.forum.service.MaintenanceModeState;
import com.mse.edu.forum.service.MaintenanceModeState.StatusSnapshot;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceAdminController {

	private final MaintenanceModeState maintenanceModeState;

	public MaintenanceAdminController(MaintenanceModeState maintenanceModeState) {
		this.maintenanceModeState = maintenanceModeState;
	}

	@GetMapping("/status")
	public ResponseEntity<MaintenanceStatusResponse> status() {
		return ResponseEntity.ok(MaintenanceStatusResponse.from(maintenanceModeState.snapshot()));
	}

	@PostMapping("/restore/start")
	public ResponseEntity<MaintenanceStatusResponse> startRestore(
			@RequestBody(required = false) StartRestoreRequest request) {
		Long estimatedDurationSeconds = request == null ? null : request.estimatedDurationSeconds();
		StatusSnapshot snapshot = maintenanceModeState.startRestore(estimatedDurationSeconds);
		return ResponseEntity.ok(MaintenanceStatusResponse.from(snapshot));
	}

	@PostMapping("/restore/finish")
	public ResponseEntity<MaintenanceStatusResponse> finishRestore() {
		return ResponseEntity.ok(MaintenanceStatusResponse.from(maintenanceModeState.finishRestore()));
	}

	public record StartRestoreRequest(Long estimatedDurationSeconds) {}

	public record MaintenanceStatusResponse(
			boolean restoreInProgress,
			long retryAfterSeconds,
			Instant restoreStartedAt,
			Instant estimatedCompletionAt) {
		static MaintenanceStatusResponse from(StatusSnapshot snapshot) {
			return new MaintenanceStatusResponse(
					snapshot.restoreInProgress(),
					snapshot.retryAfterSeconds(),
					snapshot.restoreStartedAt(),
					snapshot.estimatedCompletionAt());
		}
	}
}

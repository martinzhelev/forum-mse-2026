package com.mse.edu.forum.maintenance;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/restore")
@PreAuthorize("hasRole('ADMIN')")
public class RestoreMaintenanceController {

	private final RestoreMaintenanceState maintenanceState;

	public RestoreMaintenanceController(RestoreMaintenanceState maintenanceState) {
		this.maintenanceState = maintenanceState;
	}

	@GetMapping("/status")
	public RestoreMaintenanceStatusResponse status() {
		return new RestoreMaintenanceStatusResponse(
				maintenanceState.isRestoreInProgress(),
				maintenanceState.getRetryAfterSeconds(),
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	@PostMapping("/enable")
	public RestoreMaintenanceStatusResponse enable(@RequestBody(required = false) RestoreMaintenanceToggleRequest request) {
		maintenanceState.setRestoreInProgress(true);
		return status();
	}

	@PostMapping("/disable")
	public RestoreMaintenanceStatusResponse disable(
			@RequestBody(required = false) RestoreMaintenanceToggleRequest request) {
		maintenanceState.setRestoreInProgress(false);
		return status();
	}
}

package com.mse.edu.forum.maintenance;

import java.time.OffsetDateTime;

public record RestoreMaintenanceStatusResponse(
		boolean restoreInProgress, long retryAfterSeconds, OffsetDateTime timestamp) {}

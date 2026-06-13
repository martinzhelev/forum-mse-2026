package com.mse.edu.forum.maintenance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RestoreLifecycleServiceTest {

	@Test
	void runWithRestoreWindow_resetsStateWhenActionFails() {
		RestoreProperties properties = new RestoreProperties(false, 120, java.util.List.of());
		RestoreMaintenanceState state = new RestoreMaintenanceState(properties);
		RestoreLifecycleService service = new RestoreLifecycleService(state);

		assertThrows(IllegalStateException.class, () -> service.runWithRestoreWindow(() -> {
			throw new IllegalStateException("boom");
		}));
		assertFalse(state.isRestoreInProgress());
	}
}

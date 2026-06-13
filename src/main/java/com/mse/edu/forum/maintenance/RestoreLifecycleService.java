package com.mse.edu.forum.maintenance;

import org.springframework.stereotype.Service;

@Service
public class RestoreLifecycleService {

	private final RestoreMaintenanceState maintenanceState;

	public RestoreLifecycleService(RestoreMaintenanceState maintenanceState) {
		this.maintenanceState = maintenanceState;
	}

	public void runWithRestoreWindow(Runnable restoreAction) {
		maintenanceState.startRestore();
		try {
			restoreAction.run();
		} finally {
			maintenanceState.finishRestore();
		}
	}
}

package com.etteplan.servicemanual.maintenancetask;

class MaintenanceTaskNotFoundException extends RuntimeException {
    
    MaintenanceTaskNotFoundException(Long id) {
        super("Could not find maintenance task " + id);
    }
}

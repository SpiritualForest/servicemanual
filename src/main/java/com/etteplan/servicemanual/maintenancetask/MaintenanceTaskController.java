package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping; // Might use this for editing, we'll see
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

import java.util.List;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;

    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
    }

    @GetMapping("/tasks")
    List<MaintenanceTask> getAllTasks() {
        // No filter applied
        // TODO: sort the results by severity and registration time.
        return taskRepository.findAllByOrderBySeverityDescRegistered();
    }

    @GetMapping("/tasks/{deviceId}")
    List<MaintenanceTask> getTaskById(@PathVariable Long deviceId) {
        // Return all the tasks associated with <deviceId>
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdOrderBySeverityDescRegistered(deviceId);
        return tasks;
    }
}

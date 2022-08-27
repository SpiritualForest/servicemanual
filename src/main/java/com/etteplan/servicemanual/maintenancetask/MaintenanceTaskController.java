package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.ResponseEntity;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

import javax.validation.Valid;

// MaintenanceTask and its Repository already exist in this package, that's why we don't have to import them.

import java.util.List;
import java.util.stream.Collectors;

// TODO: Clean this stuff a little bit. Look at this mess!
// FIXME: we have to do data validation and other error checks. Make sure to implement this.

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;

    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
    }

    // Show all tasks with default sorting - severity first, then registration time
    // TODO: GET request parameters
    @GetMapping("/tasks")
    CollectionModel<EntityModel<MaintenanceTask>> getAllTasks() {
        List<EntityModel<MaintenanceTask>> tasks = taskRepository.findAllByOrderBySeverityAscRegistered().stream()
            .map(task -> EntityModel.of(task, 
                        // self: { ... }
                        linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
                        // deviceId: { ... }
                        linkTo(methodOn(MaintenanceTaskController.class).getTasksByDeviceId(task.getDeviceId())).withRel("deviceId"),
                        // tasks: { ... }
                        linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withRel("tasks")))
            .collect(Collectors.toList());
        
        return CollectionModel.of(tasks, linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withSelfRel());
    }

    // Delete all tasks
    @DeleteMapping("/tasks")
    void deleteAllTasks() {
        taskRepository.deleteAll();
    }

    // Show a unique task by its id
    @GetMapping("/tasks/{taskId}")
    EntityModel<MaintenanceTask> getTaskById(@PathVariable Long taskId) {
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));

        return EntityModel.of(task,
                linkTo(methodOn(MaintenanceTaskController.class).getTaskById(taskId)).withSelfRel(),
                linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withRel("tasks")
            );
    }

    // Delete a single task based on its id
    @DeleteMapping("/tasks/{taskId}")
    ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        if (taskRepository.existsById(taskId)) {
            taskRepository.deleteById(taskId);
            return ResponseEntity.ok("Task deleted successfully.");
        }
        // If we reached here, there was an error
        return ResponseEntity.unprocessableEntity().body("Task deletion failed: no such task: " + taskId);
    }

    // Update a single task
    @PutMapping("/tasks/{taskId}")
    MaintenanceTask updateTask(@RequestBody MaintenanceTask modifiedTask, @PathVariable Long taskId) {
        return taskRepository.findById(taskId)
            .map(task -> {
                // Modify the data in the original task entity
                // Severity
                TaskSeverity severity = modifiedTask.getSeverity();
                if (severity != null) {
                    task.setSeverity(severity);
                }
                // Status
                TaskStatus status = modifiedTask.getStatus();
                if (status != null) {
                    task.setStatus(status);
                }
                // Desc
                String description = modifiedTask.getDescription();
                if (description != null) {
                    task.setDescription(description);
                }
                // Device ID. Need to be careful with this one.
                Long deviceId = modifiedTask.getDeviceId();
                if (deviceId != null) {
                    task.setDeviceId(deviceId);
                }
                return taskRepository.save(task);
            }).orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));
    }

    // Show all tasks performed on <deviceId> sorted by severity and then registration time.
    @GetMapping("/tasks/device/{deviceId}")
    CollectionModel<EntityModel<MaintenanceTask>> getTasksByDeviceId(@PathVariable Long deviceId) {
        // Return all the tasks associated with <deviceId>
        List<EntityModel<MaintenanceTask>> tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId).stream()
            .map(task -> EntityModel.of(task,
                        linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
                        linkTo(methodOn(MaintenanceTaskController.class).getTasksByDeviceId(deviceId)).withRel("deviceId"),
                        linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withRel("tasks")))
            .collect(Collectors.toList());
        
        return CollectionModel.of(tasks, linkTo(methodOn(MaintenanceTaskController.class).getTasksByDeviceId(deviceId)).withSelfRel());
    }
    // Delete all tasks for this deviceId
    @DeleteMapping("/tasks/device/{deviceId}")
    void deleteDeviceTasks(@PathVariable Long deviceId) {
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
    }
    
    // Create a new task
    @PostMapping("/tasks/new")
    MaintenanceTask createTask(@RequestBody @Valid MaintenanceTask task) {
        if (!deviceRepository.existsById(task.getDeviceId())) {
            // Error, no such device.
            throw new FactoryDeviceNotFoundException(task.getDeviceId());
        }
        return taskRepository.save(task);
    }
}

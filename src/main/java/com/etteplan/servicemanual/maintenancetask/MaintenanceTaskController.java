package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

import javax.validation.Valid;

// MaintenanceTask and its Repository already exist in this package, that's why we don't have to import them.

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

// TODO: Clean this stuff a little bit. Look at this mess!
// FIXME: we have to do data validation and other error checks. Make sure to implement this.

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;
    private final MaintenanceTaskModelAssembler assembler;

    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository, MaintenanceTaskModelAssembler assembler) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
        this.assembler = assembler;
    }

    // MAPPING: /api/tasks

    CollectionModel<EntityModel<MaintenanceTask>> query(Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Helper function to organize our code better.
        // Filters the tasks based on the given parameters.
        /* NOTE: to the gods of programming, I'm sorry for all these conditionals.
         * It's not my fault that this is how this framework was designed and how Java is designed.
         * Forgive me. */
        List<MaintenanceTask> tasks = new ArrayList<>();
        if (status != null && severity != null) {
            // Status and severity
            if (deviceId == null) {
                tasks = taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(status, severity);
            }
            else {
                tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(deviceId, status, severity);
            }
        }
        else if (status != null) {
            // Status only
            if (deviceId == null) {
                tasks = taskRepository.findAllByStatusOrderBySeverityAscRegistered(status);
            }
            else {
                tasks = taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(deviceId, status);
            }
        }
        else if (severity != null) {
            // Severity only
            if (deviceId == null) {
                tasks = taskRepository.findAllBySeverityOrderBySeverityAscRegistered(severity);
            }
            else {
                tasks = taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(deviceId, severity);
            }
        }
        else {
            // Everything, no filter.
            if (deviceId == null) {
                tasks = taskRepository.findAllByOrderBySeverityAscRegistered();
            }
            else {
                tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
            }
        }
        if (deviceId == null) {
            // Add links to /api/tasks
            List<EntityModel<MaintenanceTask>> tasksModel = tasks.stream().map(assembler::toModel).collect(Collectors.toList());
            return CollectionModel.of(tasksModel, linkTo(methodOn(MaintenanceTaskController.class).all()).withSelfRel());
        }
        else {
            // Add links to /api/tasks and the /api/tasks/device/deviceId
            List<EntityModel<MaintenanceTask>> tasksModel = tasks.stream()
                    .map(assembler::toModelWithDevice)
                    .collect(Collectors.toList());
            return CollectionModel.of(tasksModel, 
                    linkTo(methodOn(MaintenanceTaskController.class).all(deviceId)).withRel("device"),
                    linkTo(methodOn(MaintenanceTaskController.class).all()).withRel("tasks"));
        }
    }
    
    @GetMapping(path = "/api/tasks", params = { "status", "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskStatus status, @RequestParam TaskSeverity severity) {
        // Filter by both, no device
        return query(null, status, severity);
    }

    @GetMapping(path = "/api/tasks", params = { "status" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskStatus status) {
        // Filter by status, no device
        return query(null, status, null);
    }

    @GetMapping(path = "/api/tasks", params = { "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskSeverity severity) {
        // Filters by severity, no device
        return query(null, null, severity);
    }
    
    @GetMapping("/api/tasks")
    CollectionModel<EntityModel<MaintenanceTask>> all() {
        // Query with no filter, fetches all existing tasks
        return query(null, null, null);
    }

    // Delete all tasks
    @DeleteMapping("/api/tasks")
    void deleteAllTasks() {
        taskRepository.deleteAll();
    }
    
    // MAPPING: /api/tasks/{taskId}

    // Show a unique task by its id
    @GetMapping("/api/tasks/{taskId}")
    EntityModel<MaintenanceTask> getTaskById(@PathVariable Long taskId) {
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));

        return assembler.toModel(task);
    }

    // Delete a single task based on its id
    @DeleteMapping("/api/tasks/{taskId}")
    ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        if (taskRepository.existsById(taskId)) {
            taskRepository.deleteById(taskId);
            return ResponseEntity.ok("Task deleted successfully.");
        }
        // If we reached here, there was an error
        throw new MaintenanceTaskNotFoundException(taskId);
    }

    // Update a single task
    @PutMapping("/api/tasks/{taskId}")
    MaintenanceTask updateTask(@RequestBody @Valid MaintenanceTask modifiedTask, @PathVariable Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            // No such task
            throw new MaintenanceTaskNotFoundException(taskId);
        }
        if (!deviceRepository.existsById(modifiedTask.getDeviceId())) {
            // The supplied deviceId doesn't actually exist in the database. Abort.
            throw new FactoryDeviceNotFoundException(modifiedTask.getDeviceId());
        }
        // Will return BadRequest if it contains null values
        
        /* We must load up the appropriate MaintenanceTask entity
         * and update all of its relevant fields ourselves, otherwise
         * a new one will be created in its place with these fields,
         * and the original one will remain unmodified.
         * God bless the people who developed this. */

        MaintenanceTask task = taskRepository.findById(taskId).get();
        task.setStatus(modifiedTask.getStatus());
        task.setSeverity(modifiedTask.getSeverity());
        task.setDescription(modifiedTask.getDescription());
        task.setDeviceId(modifiedTask.getDeviceId());
        return taskRepository.save(task);
    }

    // MAPPING: /api/tasks/device

    // Show all tasks associated with <deviceId>
    @GetMapping("/api/tasks/device/{deviceId}")
    CollectionModel<EntityModel<MaintenanceTask>> all(@PathVariable Long deviceId) {
        // Fetch all associated with this device
        return query(deviceId, null, null);
    }

    @GetMapping(path = "/api/tasks/device/{deviceId}", params = { "status", "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@PathVariable Long deviceId, @RequestParam TaskStatus status, @RequestParam TaskSeverity severity) {
        // Device, status, severity
        return query(deviceId, status, severity);
    }

    @GetMapping(path = "/api/tasks/device/{deviceId}", params = { "status" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@PathVariable Long deviceId, @RequestParam TaskStatus status) {
        // Device and status
        return query(deviceId, status, null);
    }

    @GetMapping(path = "/api/tasks/device/{deviceId}", params = { "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@PathVariable Long deviceId, @RequestParam TaskSeverity severity) {
        // Device and severity
        return query(deviceId, null, severity);
    }

    // Delete all tasks for this deviceId
    @DeleteMapping("/api/tasks/device/{deviceId}")
    void deleteDeviceTasks(@PathVariable Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new FactoryDeviceNotFoundException(deviceId);
        }
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
    }
    
    // Create a new task
    @PostMapping("/api/tasks/create")
    @ResponseStatus(HttpStatus.CREATED)
    MaintenanceTask createTask(@RequestBody @Valid MaintenanceTask task) {
        if (!deviceRepository.existsById(task.getDeviceId())) {
            // Error, no such device.
            throw new FactoryDeviceNotFoundException(task.getDeviceId());
        }
        return taskRepository.save(task);
    }
}

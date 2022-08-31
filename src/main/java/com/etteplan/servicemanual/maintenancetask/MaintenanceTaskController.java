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

    CollectionModel<EntityModel<MaintenanceTask>> addHyperlinks(Long deviceId, List<MaintenanceTask> tasks) {
        // Helper function to organize our code better.
        // Filters the tasks based on the given parameters.
        // The tasks list comes from the various mapped methods that call this function.
        // This function just serves to add the necessary hyperlinks to all the task objects.
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

    // MAPPING: /api/tasks
    
    @GetMapping(path = "/api/tasks", params = { "status", "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskStatus status, @RequestParam TaskSeverity severity) {
        // Filter by both, no device
        List<MaintenanceTask> tasks = taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(status, severity);
        return addHyperlinks(null, tasks);
    }

    @GetMapping(path = "/api/tasks", params = { "status" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskStatus status) {
        // Filter by status, no device
        List<MaintenanceTask> tasks = taskRepository.findAllByStatusOrderBySeverityAscRegistered(status);
        return addHyperlinks(null, tasks);
    }

    @GetMapping(path = "/api/tasks", params = { "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam TaskSeverity severity) {
        // Filters by severity, no device
        List<MaintenanceTask> tasks = taskRepository.findAllBySeverityOrderBySeverityAscRegistered(severity);
        return addHyperlinks(null, tasks);
    }
    
    @GetMapping("/api/tasks")
    CollectionModel<EntityModel<MaintenanceTask>> all() {
        // Query with no filter, fetches all existing tasks
        List<MaintenanceTask> tasks = taskRepository.findAllByOrderBySeverityAscRegistered();
        return addHyperlinks(null, tasks);
    }

    // Filter tasks first by deviceId, then status and severity according to other parameters passed
    
    @GetMapping(path = "/api/tasks", params = { "deviceId" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam Long deviceId) {
        // Fetch all associated with this device
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
        return addHyperlinks(deviceId, tasks);
    }

    @GetMapping(path = "/api/tasks", params = { "deviceId", "status", "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam Long deviceId, @RequestParam TaskStatus status, @RequestParam TaskSeverity severity) {
        // Device, status, severity
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(deviceId, status, severity);
        return addHyperlinks(deviceId, tasks);
    }

    @GetMapping(path = "/api/tasks", params = { "deviceId", "status" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam Long deviceId, @RequestParam TaskStatus status) {
        // Device and status
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(deviceId, status);
        return addHyperlinks(deviceId, tasks);
    }

    @GetMapping(path = "/api/tasks", params = { "deviceId", "severity" })
    CollectionModel<EntityModel<MaintenanceTask>> all(@RequestParam Long deviceId, @RequestParam TaskSeverity severity) {
        // Device and severity
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(deviceId, severity);
        return addHyperlinks(deviceId, tasks);
    }

    // Delete all tasks for this deviceId
    @DeleteMapping(path = "/api/tasks", params = { "deviceId" })
    void deleteDeviceTasks(@RequestParam Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new FactoryDeviceNotFoundException(deviceId);
        }
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
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

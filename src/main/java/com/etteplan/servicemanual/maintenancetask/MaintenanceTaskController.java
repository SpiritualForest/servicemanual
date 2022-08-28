package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
    private final MaintenanceTaskModelAssembler assembler;

    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository, MaintenanceTaskModelAssembler assembler) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
        this.assembler = assembler;
    }

    // MAPPING: /api/tasks

    // Show all tasks
    @GetMapping("/api/tasks")
    CollectionModel<EntityModel<MaintenanceTask>> all() {
        // Get all
        List<EntityModel<MaintenanceTask>> tasks = taskRepository.findAllByOrderBySeverityAscRegistered().stream()
            .map(assembler::toModel)
            .collect(Collectors.toList());
        return CollectionModel.of(tasks, linkTo(methodOn(MaintenanceTaskController.class).all()).withSelfRel());
    }
    // Delete all tasks
    @DeleteMapping("/api/tasks")
    void deleteAllTasks() {
        taskRepository.deleteAll();
    }

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

    // MAPPING: /api/tasks/deviceId

    // Show all tasks associated with <deviceId>
    @GetMapping("/api/tasks/deviceId/{deviceId}")
    CollectionModel<EntityModel<MaintenanceTask>> all(@PathVariable Long deviceId) {
        List<EntityModel<MaintenanceTask>> tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId).stream()
                .map(assembler::toModelWithDevice)
                .collect(Collectors.toList());
        return CollectionModel.of(tasks, 
                linkTo(methodOn(MaintenanceTaskController.class).all(deviceId)).withRel("deviceId"),
                linkTo(methodOn(MaintenanceTaskController.class).all()).withRel("tasks"));
    }

    // Delete all tasks for this deviceId
    @DeleteMapping("/api/tasks/deviceId/{deviceId}")
    void deleteDeviceTasks(@PathVariable Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new FactoryDeviceNotFoundException(deviceId);
        }
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
    }
    
    // Create a new task
    @PostMapping("/api/tasks/create")
    MaintenanceTask createTask(@RequestBody @Valid MaintenanceTask task) {
        if (!deviceRepository.existsById(task.getDeviceId())) {
            // Error, no such device.
            throw new FactoryDeviceNotFoundException(task.getDeviceId());
        }
        return taskRepository.save(task);
    }
}

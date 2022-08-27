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
    @GetMapping("/api/tasks")
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
    @DeleteMapping("/api/tasks")
    void deleteAllTasks() {
        taskRepository.deleteAll();
    }

    // Show a unique task by its id
    @GetMapping("/api/tasks/{taskId}")
    EntityModel<MaintenanceTask> getTaskById(@PathVariable Long taskId) {
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));

        return EntityModel.of(task,
                linkTo(methodOn(MaintenanceTaskController.class).getTaskById(taskId)).withSelfRel(),
                linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withRel("tasks")
            );
    }

    // Delete a single task based on its id
    @DeleteMapping("/api/tasks/{taskId}")
    ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        if (taskRepository.existsById(taskId)) {
            taskRepository.deleteById(taskId);
            return ResponseEntity.ok("Task deleted successfully.");
        }
        // If we reached here, there was an error
        return ResponseEntity.unprocessableEntity().body("Task deletion failed: no such task: " + taskId);
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

    // Show all tasks performed on <deviceId> sorted by severity and then registration time.
    @GetMapping("/api/tasks/device/{deviceId}")
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
    @DeleteMapping("/api/tasks/device/{deviceId}")
    void deleteDeviceTasks(@PathVariable Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new FactoryDeviceNotFoundException(deviceId);
        }
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
    }
    
    // Create a new task
    @PostMapping("/api/tasks/new")
    MaintenanceTask createTask(@RequestBody @Valid MaintenanceTask task) {
        if (!deviceRepository.existsById(task.getDeviceId())) {
            // Error, no such device.
            throw new FactoryDeviceNotFoundException(task.getDeviceId());
        }
        return taskRepository.save(task);
    }
}

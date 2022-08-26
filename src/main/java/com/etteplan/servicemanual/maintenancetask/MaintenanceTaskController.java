package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping; // Might use this for editing, we'll see
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

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
    @GetMapping("/tasks/{id}")
    EntityModel<MaintenanceTask> getTaskById(@PathVariable Long id) {
        MaintenanceTask task = taskRepository.findById(id)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(id));

        return EntityModel.of(task,
                linkTo(methodOn(MaintenanceTaskController.class).getTaskById(id)).withSelfRel(),
                linkTo(methodOn(MaintenanceTaskController.class).getAllTasks()).withRel("tasks")
            );
    }

    // Delete a single task based on its id
    @DeleteMapping("/tasks/{id}")
    ResponseEntity<String> deleteTask(@PathVariable Long id) {
        
        // TODO: return JSON here with a status and some links maybe

        if (taskRepository.findById(id).isPresent()) {
            taskRepository.deleteById(id);
            return ResponseEntity.ok().body("success");
        }
        // If we reached here, there was an error
        return ResponseEntity.unprocessableEntity().body("failed");
    }

    // Update a single task
    @PutMapping("/tasks/{id}")
    MaintenanceTask updateTask(@RequestBody MaintenanceTask modifiedTask, @PathVariable Long taskId) {
        return taskRepository.findById(taskId)
            .map(task -> {
                // Modify the data in the original task entity
                task.setSeverity(modifiedTask.getSeverity());
                task.setStatus(modifiedTask.getStatus());
                task.setDeviceId(modifiedTask.getDeviceId());
                task.setDescription(modifiedTask.getDescription());
                task.setRegistered(modifiedTask.getRegistered());
                return taskRepository.save(task);
            }).orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));
    }

    // Show all tasks performed on <deviceId>
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
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
        taskRepository.deleteAll(tasks);
    }
    
    // Create a new task
    @PostMapping("/tasks/new")
    MaintenanceTask createTask(@RequestBody MaintenanceTask task) {
        return taskRepository.save(task);
    }
}

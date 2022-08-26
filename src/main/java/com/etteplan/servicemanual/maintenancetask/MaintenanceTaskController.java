package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping; // Might use this for editing, we'll see
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

// MaintenanceTask and its Repository already exist in this package, that's why we don't have to import them.

import java.util.List;
import java.util.stream.Collectors;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;

    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
    }
    // TODO: add hyperlinks to all with HAL.

    // Show all tasks, GET request
    // Aggregate root
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
    
    // Delete a single task based on its id
    @DeleteMapping("/tasks/{taskId}/delete")
    void deleteTask(@PathVariable Long taskId) {
        taskRepository.deleteById(taskId);
    }

    // Create a new task
    @PostMapping("/tasks/new")
    MaintenanceTask createTask(@RequestBody MaintenanceTask task) {
        return taskRepository.save(task);
    }
}

package com.etteplan.servicemanual.maintenancetask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;
    private final MaintenanceTaskModelAssembler assembler;
    
    // Our constructor
    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository, MaintenanceTaskModelAssembler assembler) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
        this.assembler = assembler;
        TaskFetcher.setTaskRepository(taskRepository); // TaskFetcher.java
        TaskEditor.setTaskRepository(taskRepository); // TaskEditor.java
        TaskEditor.setDeviceRepository(deviceRepository);
    }

    CollectionModel<EntityModel<MaintenanceTask>> addHyperlinks(List<MaintenanceTask> tasks) {
        // Helper function to add links to /api/tasks, and /api/tasks/{taskId} to each task obj
        // prior to sending it as a response to the client
        List<EntityModel<MaintenanceTask>> tasksModel = tasks.stream().map(assembler::toModel).collect(Collectors.toList());
        return CollectionModel.of(tasksModel, linkTo(methodOn(MaintenanceTaskController.class).all(assembler.params)).withSelfRel());
    }

    // MAPPING: /api/tasks
    
    /* We do our own resolution of queries to validate the request.
     * If a query parameter is bad in some way, such as not convertable to its required type, or unknown,
     * then QueryParameterException is thrown, and our response status is 400 bad request. */
    
    // Fetch tasks
    
    @GetMapping("/api/tasks")
    ResponseEntity<Object> all(@RequestParam Map<String, String> queryParameters) {
        List<MaintenanceTask> tasks = new ArrayList<>();
        try {
            tasks = TaskFetcher.fetchTasks(queryParameters);
        }
        catch (QueryParameterException ex) {
            // Got a bad parameter. We do not proceed.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // Query was ok, return whatever tasks were found.
        return ResponseEntity.ok().body(addHyperlinks(tasks));
    }
    
    // Delete tasks

    @DeleteMapping("/api/tasks")
    ResponseEntity<String> deleteTasks(@RequestParam Map<String, String> queryParameters) {
        // Delete tasks
        List<MaintenanceTask> tasks = new ArrayList<>();
        try {
            // Try to fetch the tasks
            tasks = TaskFetcher.fetchTasks(queryParameters);
        }
        catch (QueryParameterException ex) {
            // Got a bad parameter.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // Query was ok, delete whatever tasks were found
        taskRepository.deleteAll(tasks);
        return ResponseEntity.ok().body(String.format("Deleted %d tasks", tasks.size()));
    }
    
    // Create a new task

    @PostMapping("/api/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    EntityModel<MaintenanceTask> createTask(@RequestBody @Valid MaintenanceTask task) {
        // Returns 400 bad request if the supplied task object is not valid in some way.
        if (!deviceRepository.existsById(task.getDeviceId())) {
            // Error, no such device.
            throw new FactoryDeviceNotFoundException(task.getDeviceId());
        }
        String escapedDesc = task.getDescription();
        // Escape HTML in the description to prevent potential XSS attacks
        escapedDesc = escapedDesc.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        task.setDescription(escapedDesc);
        // Save and return the created task object
        task = taskRepository.save(task);
        return assembler.toModel(task);
    }

    // MAPPING: /api/tasks/{taskId}

    // Show a unique task by its id
    
    @GetMapping("/api/tasks/{taskId}")
    EntityModel<MaintenanceTask> getTaskById(@PathVariable Long taskId) {
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));
        
        // Task was found, show it
        return assembler.toModel(task);
    }

    // Delete a single task based on its id
    
    @DeleteMapping("/api/tasks/{taskId}")
    ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));
        
        // The task was found. Delete it.
        taskRepository.delete(task);
        return ResponseEntity.ok(String.format("Task %d deleted", taskId));
    }

    // Update a task object's fields.
    
    @PatchMapping("/api/tasks/{taskId}")
    ResponseEntity<Object> updateTask(@RequestBody Map<String, String> requestBody, @PathVariable Long taskId) {
        // Returns 400 bad request if the request body is empty, contains an unknown property,
        // or the value of a property cannot be correctly converted to a MaintenanceTask field that represents it.

        /* We allow the modification of as many or as few of the fields as desired. */
        
        MaintenanceTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MaintenanceTaskNotFoundException(taskId));
        
        // Task exists. Try to edit it according to the given request body
        try {
            task = TaskEditor.editTask(task, requestBody);
        }
        catch (RequestBodyException ex) {
            // Error in the request body
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // Task edited successfully
        return ResponseEntity.ok().body(assembler.toModel(task));
    } 
}

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

// MaintenanceTask and its Repository already exist in this package, that's why we don't have to import them.
// TaskFetcher is a static class used to resolve query parameters and fetch tasks accordingly. View TaskFetcher.java
// TaskEditor is a static class similar to TaskFetcher, but its purpose and validation mechanism are different.

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
    
    // We need these param HashMaps for creating hyperlinks in responses
    // They serve literally no other purpose.
    private final Map<String, String> emptyParams = new HashMap<String, String>();
    private Map<String, String> deviceParam = new HashMap<String, String>();
    private final String DEVICEID = "deviceId";

    // Our constructor
    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository, MaintenanceTaskModelAssembler assembler) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
        this.assembler = assembler;
        TaskFetcher.setTaskRepository(taskRepository);
        TaskEditor.setTaskRepository(taskRepository);
        TaskEditor.setDeviceRepository(deviceRepository);
    }

    CollectionModel<EntityModel<MaintenanceTask>> addHyperlinks(Long deviceId, List<MaintenanceTask> tasks) {
        // Helper function to create hyperlinks to the supplied MaintenanceTask objects
        if (deviceId == null) {
            // Add links to /api/tasks
            List<EntityModel<MaintenanceTask>> tasksModel = tasks.stream().map(assembler::toModel).collect(Collectors.toList());
            return CollectionModel.of(tasksModel, linkTo(methodOn(MaintenanceTaskController.class).all(this.emptyParams)).withSelfRel());
        }
        else {
            // Add links to /api/tasks and the /api/tasks?deviceId=
            deviceParam.put(DEVICEID, Long.toString(deviceId));
            List<EntityModel<MaintenanceTask>> tasksModel = tasks.stream()
                    .map(assembler::toModelWithDevice)
                    .collect(Collectors.toList());
            return CollectionModel.of(tasksModel, 
                    linkTo(methodOn(MaintenanceTaskController.class).all(deviceParam)).withRel("device"),
                    linkTo(methodOn(MaintenanceTaskController.class).all(this.emptyParams)).withRel("tasks"));
        }
    }

    // MAPPING: /api/tasks
    
    /* We do our own resolution of queries to validate the request.
     * If a query parameter is bad in some way, such as not convertable to its required type, or unknown,
     * then QueryParameterException is thrown, and our response status is 400 bad request. */
    
    @GetMapping("/api/tasks")
    ResponseEntity<Object> all(@RequestParam Map<String, String> queryParameters) {
        // Fetch tasks
        try {
            List<MaintenanceTask> tasks = TaskFetcher.fetchTasks(queryParameters);
            return ResponseEntity.ok().body(addHyperlinks(TaskFetcher.getDeviceId(), tasks));
        }
        catch (QueryParameterException ex) {
            // Got a bad parameter. We do not proceed.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
    
    @DeleteMapping("/api/tasks")
    ResponseEntity<String> deleteTasks(@RequestParam Map<String, String> queryParameters) {
        // Delete tasks
        try {
            List<MaintenanceTask> tasks = TaskFetcher.fetchTasks(queryParameters);
            taskRepository.deleteAll(tasks);
            return ResponseEntity.ok().body("Tasks deleted.");
        }
        catch (QueryParameterException ex) {
            // Got a bad parameter.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
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
        return assembler.toModel(taskRepository.save(task));
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
        // If we reached here, the task was not found
        throw new MaintenanceTaskNotFoundException(taskId);
    }

    // Update a task's fields.
    @PatchMapping("/api/tasks/{taskId}")
    ResponseEntity<Object> updateTask(@RequestBody Map<String, String> body, @PathVariable Long taskId) {
        // Returns 400 bad request if the request body is empty, contains an unknown property,
        // or the value of a property cannot be correctly converted to a MaintenanceTask field that represents it.

        /* We allow the modification of as many or as few of the fields as desired. */

        if (!taskRepository.existsById(taskId)) {
            // No such task
            throw new MaintenanceTaskNotFoundException(taskId);
        }
        if (body.isEmpty()) {
            // Empty body supplied, return 400
            return ResponseEntity.badRequest().body("Error: empty request body");
        }

        // The request body is not empty, let's parse it.
        MaintenanceTask task = taskRepository.findById(taskId).get();
        try {
            task = TaskEditor.editTask(task, body);
        }
        catch (RequestBodyException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        return ResponseEntity.ok().body(assembler.toModel(task));
    } 
}

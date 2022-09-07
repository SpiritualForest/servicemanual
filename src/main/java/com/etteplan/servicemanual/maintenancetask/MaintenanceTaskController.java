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
// TaskFetcher is a static class used to resolve query parameters and fetch task accordingly. View TaskFetcher.java

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;
    private final MaintenanceTaskModelAssembler assembler;
    
    // We need these param HashMaps for creating hyperlinks in responses
    // They serve literally no other purpose.
    private final Map<String, String> emptyParams = new HashMap<String, String>();
    private Map<String, String> deviceParam = new HashMap<String, String>();
    
    // Request body property names
    private final String RP_DEVICEID = "deviceId";
    private final String RP_STATUS = "status";
    private final String RP_SEVERITY = "severity";
    private final String RP_DESCRIPTION = "description";
    private final String RP_REGISTERED = "registered";

    // Our constructor
    public MaintenanceTaskController(MaintenanceTaskRepository taskRepository, FactoryDeviceRepository deviceRepository, MaintenanceTaskModelAssembler assembler) {
        this.taskRepository = taskRepository;
        this.deviceRepository = deviceRepository;
        this.assembler = assembler;
        TaskFetcher.setTaskRepository(taskRepository);
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
            deviceParam.put(RP_DEVICEID, Long.toString(deviceId));
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
        // Escape HTML in the description
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

    // Update a single task - we validate the request body ourselves
    @PutMapping("/api/tasks/{taskId}")
    ResponseEntity<Object> updateTask(@RequestBody Map<String, String> body, @PathVariable Long taskId) {
        // Returns 400 bad request if the request body is empty, contains an unknown property,
        // or the value of a property cannot be correctly converted to a MaintenanceTask field that represents it.

        /* We allow the modification of all, or just some, of the fields. */

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
        for(String param : body.keySet()) {
            String value = body.get(param);
            // Wrap the switch in a try, so that we can catch IllegalArgumentException only once
            // instead of catching it in every case.
            try {
                switch (param) {
                    case RP_DEVICEID:
                        // Throws IllegalArgumentException if parsing fails
                        Long deviceId = Long.parseLong(value);
                        if (!deviceRepository.existsById(deviceId)) {
                            // No such device
                            throw new FactoryDeviceNotFoundException(deviceId);
                        }
                        task.setDeviceId(deviceId);
                        break;

                    case RP_STATUS:
                        // Throws IllegalArgumentException if parsing fails
                        task.setStatus(TaskStatus.valueOf(value));
                        break;

                    case RP_SEVERITY:
                        // Throws IllegalArgumentException if parsing fails
                        task.setSeverity(TaskSeverity.valueOf(value));
                        break;

                    case RP_DESCRIPTION:
                        // We validate that there are no constraint violations on NotNull and NotEmpty
                        if (value == null || value.isEmpty()) {
                            return ResponseEntity.badRequest().body("Error in description: can't be null or empty");
                        }
                        // Valid desc. Escape the HTML, we don't want XSS attacks, do we?
                        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                        task.setDescription(value);
                        break;

                    case RP_REGISTERED:
                        // Throws DateTimeParseException if parsing fails
                        try {
                            task.setRegistered(LocalDateTime.parse(value));
                        }
                        catch (DateTimeParseException ex) {
                            return ResponseEntity.badRequest().body("Could not parse registration time: must be yyyy-dd-mmThh:mm:ss");
                        }
                        break;

                    default:
                        // Unknown parameter
                        return ResponseEntity.badRequest().body(String.format("Unknown property: %s", param));
                }
            }
            catch (IllegalArgumentException ex) {
                // Couldn't convert deviceId, status, or severity
                return ResponseEntity.badRequest().body(String.format("Error in request body properties: %s", ex.getMessage()));
            }
        }
        // Save changes and return
        task = taskRepository.save(task);
        return ResponseEntity.ok().body(assembler.toModel(task));
    } 
}

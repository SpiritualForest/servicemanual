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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;
    private final MaintenanceTaskModelAssembler assembler;
    
    // We need these param objects for creating hyperlinks in responses
    private final Map<String, String> emptyParams = new HashMap<String, String>();
    private Map<String, String> deviceParam = new HashMap<String, String>();
    private final String Q_DEVICEID = "deviceId";

    // Our constructor
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
            return CollectionModel.of(tasksModel, linkTo(methodOn(MaintenanceTaskController.class).all(this.emptyParams)).withSelfRel());
        }
        else {
            // Add links to /api/tasks and the /api/tasks/device/deviceId
            deviceParam.put(Q_DEVICEID, Long.toString(deviceId));
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
        if (queryParameters.size() == 0) {
            // No query parameters supplied. Return all tasks.
            return ResponseEntity.ok().body(addHyperlinks(null, taskRepository.findAllByOrderBySeverityAscRegistered()));
        }
        
        // There are query parameters
        // Create a new QueryResolver object with our task repository and supplied query parameters
        List<MaintenanceTask> tasks = new ArrayList<>();
        QueryResolver resolver = new QueryResolver(taskRepository, queryParameters);
        try {
            tasks = resolver.resolveQuery();
        }
        catch(QueryParameterException ex) {
            // Got a bad parameter. We do not proceed.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        return ResponseEntity.ok().body(addHyperlinks(resolver.getDeviceId(), tasks));
    }
    
    @DeleteMapping("/api/tasks")
    ResponseEntity<String> deleteTasks(@RequestParam Map<String, String> queryParameters) {
        if (queryParameters.size() == 0) {
            // No query parameters.
            // NOTE: DANGER, DANGER, DANGER!!!
            // We delete ALL tasks from the database here.
            taskRepository.deleteAll();
            return ResponseEntity.ok().body("All tasks deleted.");
        }
        // Resolve our query parameters.
        List<MaintenanceTask> tasks = new ArrayList<>();
        QueryResolver resolver = new QueryResolver(taskRepository, queryParameters);
        try {
            tasks = resolver.resolveQuery();
        }
        catch(QueryParameterException ex) {
            // Got a bad parameter.
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // Parameters have been validated, proceed with deletion
        taskRepository.deleteAll(tasks);
        return ResponseEntity.ok().body("Tasks deleted.");
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

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
import java.lang.reflect.Array;

@RestController
class MaintenanceTaskController {
    
    private final MaintenanceTaskRepository taskRepository;
    private final FactoryDeviceRepository deviceRepository;
    private final MaintenanceTaskModelAssembler assembler;
    // We need these param objects for creating hyperlinks in responses
    private final Map<String, String> emptyParams = new HashMap<String, String>();
    private Map<String, String> deviceParam = new HashMap<String, String>();

    // Accepted query parameter names
    // We need to know these so that we can return 400 in case we encounter unknown params
    private final String Q_DEVICEID = "deviceId";
    private final String Q_STATUS = "status";
    private final String Q_SEVERITY = "severity";
    private final List<String> acceptedQueryParameters = new ArrayList<String>(List.of(Q_DEVICEID, Q_STATUS, Q_SEVERITY));
    
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
    // TODO: clean this mess up. This is horrible.
    
    @GetMapping("/api/tasks")
    ResponseEntity<Object> all(@RequestParam Map<String, String> params) {
        if (params.size() == 0) {
            // No query parameters supplied. Return all tasks.
            return ResponseEntity.ok().body(addHyperlinks(null, taskRepository.findAllByOrderBySeverityAscRegistered()));
        }
        
        // There are query parameters
        TaskStatus status = null;
        TaskSeverity severity = null;
        Long deviceId = null;
        List<MaintenanceTask> response = new ArrayList<>();

        for(String param : params.keySet()) {
            if (!this.acceptedQueryParameters.contains(param)) {
                // No such parameter, return 400 bad request
                return ResponseEntity.badRequest().body(String.format("Bad request: unknown parameter '%s'", param));
            }
            else {
                // Parameter name exists, now let's check that the data is valid
                String value = params.get(param);
                switch(param) {
                    case Q_STATUS:
                        try {
                            status = TaskStatus.valueOf(value);
                        }
                        catch(IllegalArgumentException ex) {
                            // Bad task status, return 400
                            return ResponseEntity.badRequest().body(
                                    String.format("Bad request: could not covert status '%s'. Available values are 'OPEN' and 'CLOSED'", value));
                        }
                        break;
                    case Q_SEVERITY:
                        try { 
                            severity = TaskSeverity.valueOf(value);
                        }
                        catch(IllegalArgumentException ex) {
                            // Bad severity
                            return ResponseEntity.badRequest().body(
                                    String.format("Bad request: could not convert severity '%s'. Available values are 'UNIMPORTANT', 'IMPORTANT', 'CRITICAL'", value));
                        }
                        break;
                    case Q_DEVICEID:
                        try {
                            deviceId = Long.parseLong(value);
                        }
                        catch(IllegalArgumentException ex) {
                            // Not a number
                            return ResponseEntity.badRequest().body(String.format("Bad request: could not convert deviceId '%s'. Must be an integer.", value));
                        }
                        break;
                }
            }
        }
        // Now that we've initialized our query parameters, we need to choose which method to call.
        if (deviceId == null) {
            // No device
            if (status != null && severity != null) {
                response = all(status, severity);
            }
            else if (status != null) {
                response = all(status);
            }
            else {
                response = all(severity);
            }
        }
        else {
            if (status != null && severity != null) {
                response = all(deviceId, status, severity);
            }
            else if (status != null) {
                response = all(deviceId, status);
            }
            else {
                response = all(deviceId, severity);
            }
        }
        return ResponseEntity.ok().body(addHyperlinks(deviceId, response));
    }

    List<MaintenanceTask> all(TaskStatus status, TaskSeverity severity) {
        // Filter by both, no device 
        return taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(status, severity);
    }

    List<MaintenanceTask> all(TaskStatus status) {
        // Filter by status, no device
        return taskRepository.findAllByStatusOrderBySeverityAscRegistered(status);
    }

    List<MaintenanceTask> all(TaskSeverity severity) {
        // Filters by severity, no device
        return taskRepository.findAllBySeverityOrderBySeverityAscRegistered(severity);
    }
    
    List<MaintenanceTask> all() {
        // Query with no filter, fetches all existing tasks
        return taskRepository.findAllByOrderBySeverityAscRegistered();
    }

    // Filter tasks first by deviceId, then status and severity according to other parameters passed
    
    List<MaintenanceTask> all(Long deviceId) {
        // Fetch all associated with this device 
        return taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
    }

    List<MaintenanceTask> all(Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Device, status, severity
        return taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(deviceId, status, severity);
    }

    List<MaintenanceTask> all(Long deviceId, TaskStatus status) {
        // Device and status
        return taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(deviceId, status);
    }

    List<MaintenanceTask> all(Long deviceId, TaskSeverity severity) {
        // Device and severity
        return taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(deviceId, severity);
    }

    /* NOTE: all DELETE requests always return 200, even if nothing was actually removed.
     * This is done to maintain consistency across the API. Rather than treating the device as a special parameter,
     * it is treated as just another filtration parameter like status and severity.
     * If no tasks attached to the device are found, the result is simply that nothing happens.
     * This is consistent with deletion requests filtering without a deviceId, only status and/or severity. */

    // Delete all tasks for this deviceId
    @DeleteMapping(path = "/api/tasks", params = { "deviceId" })
    void deleteTasks(@RequestParam Long deviceId) {
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceId(deviceId);
        taskRepository.deleteAll(tasks);
    }

    // Delete all tasks for deviceId with status
    @DeleteMapping(path = "/api/tasks", params = { "deviceId", "status" })
    void deleteTasks(@RequestParam Long deviceId, @RequestParam TaskStatus status) {
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndStatus(deviceId, status);
        taskRepository.deleteAll(tasks);
    }
    
    // Delete by deviceId, status, severity
    @DeleteMapping(path = "/api/tasks", params = { "deviceId", "status", "severity" })
    void deleteTasks(@RequestParam Long deviceId, TaskStatus status, TaskSeverity severity) {
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverity(deviceId, status, severity);
        taskRepository.deleteAll(tasks);
    }

    // Delete by deviceId and severity
    @DeleteMapping(path = "/api/tasks", params = { "deviceId", "severity" })
    void deleteTasks(@RequestParam Long deviceId, @RequestParam TaskSeverity severity) {
        List<MaintenanceTask> tasks = taskRepository.findAllByDeviceIdAndSeverity(deviceId, severity);
        taskRepository.deleteAll(tasks);
    }

    // Delete by status and severity (no device)
    @DeleteMapping(path = "/api/tasks", params = { "status", "severity" })
    void deleteTasks(@RequestParam TaskStatus status, @RequestParam TaskSeverity severity) {
        List<MaintenanceTask> tasks = taskRepository.findAllByStatusAndSeverity(status, severity);
        taskRepository.deleteAll(tasks);
    }

    // Delete by status only
    @DeleteMapping(path = "/api/tasks", params = { "status" })
    void deleteTasks(@RequestParam TaskStatus status) {
        taskRepository.deleteAll(taskRepository.findAllByStatus(status));
    }

    // Delete by severity only
    @DeleteMapping(path = "/api/tasks", params = { "severity" })
    void deleteTasks(@RequestParam TaskSeverity severity) {
        taskRepository.deleteAll(taskRepository.findAllBySeverity(severity));
    }

    // Delete all tasks from the database, no filter
    @DeleteMapping("/api/tasks/all")
    void deleteTasks() {
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

package com.etteplan.servicemanual.maintenancetask;

import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import java.util.Map;

/* Static class to edit existing MaintenanceTask entites based on the given request body.
 * This class is used by the controller when handling PATCH requests.
 * If the request body is empty, malformed, or contains unknown properties, RequestBodyException is thrown.
 * If the deviceId is supplied in the body, but doesn't exist, FactoryDeviceNotFound is thrown.
 * FactoryDeviceNotFound is handled implicitly by the controller because it has an attached
 * FactoryDeviceNotFoundAdvice class that tells the controller how to handle it.
 * Therefore we don't need to handle it manually ourselves in the controller.
 * We only handle RequestBodyException there. */

public final class TaskEditor {
    
    private static FactoryDeviceRepository deviceRepository;
    private static MaintenanceTaskRepository taskRepository;

    // Request body property names
    private static final String RP_DEVICEID = "deviceId";
    private static final String RP_STATUS = "status";
    private static final String RP_SEVERITY = "severity";
    private static final String RP_DESCRIPTION = "description";
    private static final String RP_REGISTERED = "registered";

    // Error messages
    private static final String ERR_DEVICEID = "Error in request body: deviceId must be an integer.";
    private static final String ERR_STATUS = "Error in request body: status must be either 'OPEN' or 'CLOSED'.";
    private static final String ERR_SEVERITY = "Error in request body: severity must be 'UNIMPORTANT', 'IMPORTANT', or 'CRITICAL'.";
    private static final String ERR_DESCRIPTION = "Error in request body: description can't be null or empty.";
    private static final String ERR_REGISTERED = "Could not parse registration time: must be java.time.LocalDateTime format: yyyy-dd-mmThh:mm:ss";
    private static final String ERR_UNKNOWN = "Unknown property '%s'. Available properties: deviceId, status, severity, description, registered.";

    private TaskEditor() {} // Private constructor for static class

    protected static void setTaskRepository(MaintenanceTaskRepository repository) {
        taskRepository = repository;
    }

    protected static void setDeviceRepository(FactoryDeviceRepository repository) {
        deviceRepository = repository;
    }

    protected static MaintenanceTask editTask(MaintenanceTask task, Map<String, String> requestBody) throws RequestBodyException, FactoryDeviceNotFoundException {
        // Edits the task according to the given request body.
        // If successful, returns the edited and saved task object.
        // If the request body is empty, contains unknown properties, or incorrect values for a property,
        // RequestBodyException will be thrown and the task won't be editd.
        if (requestBody.isEmpty()) {
            throw new RequestBodyException("Error: empty request body");
        }
        for(String param : requestBody.keySet()) {
            String value = requestBody.get(param);
            switch (param) {
                case RP_DEVICEID:
                    // deviceId
                    Long deviceId;
                    try {    
                        deviceId = Long.parseLong(value);
                    }
                    catch (IllegalArgumentException ex) {
                        throw new RequestBodyException(ERR_DEVICEID);
                    }
                    // Now check that the device exists
                    if (!deviceRepository.existsById(deviceId)) {
                        // No such device
                        // We don't need to handle this in the controller
                        // because of the exception advice.
                        // View FactoryDeviceNotFoundAdvice.java in the factorydevice package
                        throw new FactoryDeviceNotFoundException(deviceId);
                    }
                    // Checks passed
                    task.setDeviceId(deviceId);
                    break;

                case RP_STATUS:
                    // status
                    try {
                        task.setStatus(TaskStatus.valueOf(value));
                    }
                    catch (IllegalArgumentException | NullPointerException ex) {
                        throw new RequestBodyException(ERR_STATUS);
                    }
                    break;

                case RP_SEVERITY:
                    // severity
                    try {
                        task.setSeverity(TaskSeverity.valueOf(value));
                    }
                    catch (IllegalArgumentException | NullPointerException ex) {
                        throw new RequestBodyException(ERR_SEVERITY);
                    }
                    break;

                case RP_DESCRIPTION:
                    // We validate that there are no constraint violations on NotNull and NotEmpty
                    if (value == null || value.isEmpty()) {
                        throw new RequestBodyException(ERR_DESCRIPTION);
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
                    catch (DateTimeParseException | NullPointerException ex) {
                        throw new RequestBodyException(ERR_REGISTERED);
                    }
                    break;

                default:
                    // Unknown parameter
                    throw new RequestBodyException(String.format(ERR_UNKNOWN, param));
            }
        }
        // Task object edited. Save and return it.
        return taskRepository.save(task);
    }
}

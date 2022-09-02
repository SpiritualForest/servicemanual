package com.etteplan.servicemanual.maintenancetask;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/* This class resolves which database query method must be called based
 * on the given query parameter values.
 * This class is static. It cannot be instantiated or extended, and has no instance members, only static
 * class members. I decided to design it this way because we don't need a QueryResolver instance
 * in the controller. We just need to call the function that resolves the query parameters
 * and fetches the appropriate tasks from the database. An instance is not needed for this.
 * Plus, when this class needed to be instantiated, the unit tests failed
 * when I tried to instantiate it in the controller's constructor. */

public final class QueryResolver {
 
    // Query parameter names
    // Any query parameter we encounter which is not listed here is rejected, with a QueryParameterException thrown.
    private static final String Q_DEVICEID = "deviceId";
    private static final String Q_STATUS = "status";
    private static final String Q_SEVERITY = "severity";
    private static final List<String> acceptedQueryParameters = Arrays.asList(Q_DEVICEID, Q_STATUS, Q_SEVERITY);
    
    // Some informational messages in case of an exception.
    private static final String unknownParam = "Bad request: unknown parameter '%s'. %s";
    private static final String availableParams = String.format("Available query parameters: '%s', '%s', '%s'", Q_DEVICEID, Q_STATUS, Q_SEVERITY);
    private static final String notConvertable = "Bad request: could not convert parameter '%s'. %s";
    private static final String availableStatus = "Available values for status: 'OPEN', 'CLOSED'";
    private static final String availableSeverity = "Available values for severity: 'UNIMPORTANT', 'IMPORTANT', 'CRITICAL'";

    // We store this field because we need it for creating "/api/tasks?deviceId=<id>" hyperlinks
    // in the controller that uses this class, when responding to GET requests.
    private static Long deviceId = null;

    protected static Long getDeviceId() {
        return deviceId;
    }
    
    // Private constructor because we want a static class
    private QueryResolver() {}

    // Input parameters: the MaintenanceTaskRepository, Map<String, String> of query parameters.
    // Output: a list of MaintenanceTask objects retrieved from the database
    // according to the query parameters.
    protected static List<MaintenanceTask> resolveQuery(MaintenanceTaskRepository taskRepository, Map<String, String> parameters) throws QueryParameterException {
        if (parameters.size() == 0) {
            // No parameters were actually supplied. Fetch all tasks.
            return taskRepository.findAllByOrderBySeverityAscRegistered();
        }
        // Parameters were supplied. Resolve.
         
        Long deviceId = null;
        TaskStatus status = null;
        TaskSeverity severity = null;

        for(String param : parameters.keySet()) {
            if (!acceptedQueryParameters.contains(param)) {
                // "Bad request, unknown param '%s'. Available params: ..."
                throw new QueryParameterException(String.format(unknownParam, param, availableParams));
            }
            else {
                // Parameter is correct, validate the data
                String value = parameters.get(param);
                if (param.equals(Q_STATUS)) {
                    try { 
                        status = TaskStatus.valueOf(value);
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for status. Throw exception, show which statuses are available
                        throw new QueryParameterException(String.format(notConvertable, value, availableStatus));
                    }
                }
                else if (param.equals(Q_SEVERITY)) {
                    try {
                        severity = TaskSeverity.valueOf(value);
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for severity.
                        throw new QueryParameterException(String.format(notConvertable, value, availableSeverity));
                    }
                }
                else if (param.equals(Q_DEVICEID)) {
                    try {
                        deviceId = Long.parseLong(value);
                        QueryResolver.deviceId = deviceId;
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for deviceId
                        throw new QueryParameterException(String.format(notConvertable, value, "Must be an integer."));
                    }
                }
            }
        }
        // Query the database
        return queryDatabase(taskRepository, deviceId, status, severity);
    }

    private static List<MaintenanceTask> queryDatabase(MaintenanceTaskRepository taskRepository, Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Here we check which database query we should perform, based on the values
        // of our private fields, which were set by our resolve() method.
        List<MaintenanceTask> tasks = new ArrayList<>();
        if (deviceId == null) {
            // Only status and severity
            if (status != null && severity != null) {
                tasks = taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(status, severity);
            }
            else if (status != null) {
                tasks = taskRepository.findAllByStatusOrderBySeverityAscRegistered(status);
            }
            else if (severity != null) {
                tasks = taskRepository.findAllBySeverityOrderBySeverityAscRegistered(severity);
            }
        }
        else {
            // device, status, severity
            if (status != null && severity != null) {
                tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(deviceId, status, severity);
            }
            else if (status != null) {
                tasks = taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(deviceId, status);
            }
            else if (severity != null) {
                tasks = taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(deviceId, severity);
            }
            else {
                // Only device
                tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
            }
        }
        return tasks;
    }
}

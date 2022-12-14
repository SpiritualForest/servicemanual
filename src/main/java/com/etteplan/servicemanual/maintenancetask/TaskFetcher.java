package com.etteplan.servicemanual.maintenancetask;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/* Static class to fetch a list of tasks based on given query parameters.
 * Encountering unknown or malformed query parameters will
 * throw QueryParameterException.
 * If the query is correct but no tasks match the supplied parameters,
 * an empty list will be returned. */

public final class TaskFetcher {
 
    // Query parameter names
    // Any query parameter we encounter which is not listed here is rejected, with a QueryParameterException thrown.
    private static final String Q_DEVICEID = "deviceId";
    private static final String Q_STATUS = "status";
    private static final String Q_SEVERITY = "severity";
    
    // Some informational messages in case of an exception.
    private static final String unknownParam = "Bad request: unknown parameter '%s'. %s";
    private static final String availableParams = String.format("Available query parameters: '%s', '%s', '%s'", Q_DEVICEID, Q_STATUS, Q_SEVERITY);
    private static final String notConvertable = "Bad request: could not convert parameter '%s'. %s";
    private static final String availableStatus = "Available values for status: 'OPEN', 'CLOSED'";
    private static final String availableSeverity = "Available values for severity: 'UNIMPORTANT', 'IMPORTANT', 'CRITICAL'";
    
    /* Database Query Parameters represent a combined value between 1 and 7.
     * Based on this value, we decide which database query function to call
     * in order to retrieve tasks.
     * Each query parameter has been assigned its own numerical value.
     * The combination of these values tells us which parameters we need
     * to pass to the JPA repository function.
     * So for example, if we encounter the deviceId and status parameters in our query,
     * the combined value will become 3. So we know then to call the function
     * that retrieves tasks by deviceId and status, and pass to it only the parameters deviceId and status.
     * So if we got a total of 6, filter tasks by status and severity, and only supply those parameters.
     * If we got 7, filter by all three parameters. And so forth. */
    
    private static final int DQP_DEVICEID = 1;
    private static final int DQP_STATUS = 2;
    private static final int DQP_SEVERITY = 4;

    private static MaintenanceTaskRepository taskRepository;

    protected static void setTaskRepository(MaintenanceTaskRepository repository) {
        TaskFetcher.taskRepository = repository;
    }
 
    // Private constructor because we want a static class
    private TaskFetcher() {}

    // Input parameters: Map<String, String> of query parameters.
    // Output: a list of MaintenanceTask objects retrieved from the database
    // according to the query parameters.
    
    protected static List<MaintenanceTask> fetchTasks(Map<String, String> parameters) throws QueryParameterException {
        if (parameters.isEmpty()) {
            // No parameters were actually supplied. Fetch all tasks.
            return taskRepository.findAllByOrderBySeverityAscRegistered();
        }
        // Parameters were supplied. Parse the query. 
        
        int databaseMethod = 0; // Which JPA repository method to call, based on the parameters we encountered
        Long deviceId = null;
        TaskStatus status = null;
        TaskSeverity severity = null;

        for (String param : parameters.keySet()) {
            String value = parameters.get(param);
            switch (param) {
                
                case Q_DEVICEID:
                    try {
                        deviceId = Long.parseLong(value);
                        databaseMethod += DQP_DEVICEID; // Indicate that we found the deviceId parameter
                    }
                    catch (IllegalArgumentException ex) {
                        // Bad parameter for deviceId.
                        // No "NullPointerException" check here because in case of null
                        // it will throw "NumberFormatException", which is a subclass of "IllegalArgumentException"
                        throw new QueryParameterException(String.format(notConvertable, value, "Must be an integer."));
                    }
                    break;

                case Q_STATUS:
                    try { 
                        status = TaskStatus.valueOf(value);
                        databaseMethod += DQP_STATUS; // Indicate that we found the status parameter
                    }
                    catch (IllegalArgumentException | NullPointerException ex) {
                        // Bad parameter for status. Throw exception, show which statuses are available
                        throw new QueryParameterException(String.format(notConvertable, value, availableStatus));
                    }
                    break;
                
                case Q_SEVERITY:
                    try {
                        severity = TaskSeverity.valueOf(value);
                        databaseMethod += DQP_SEVERITY; // Indicate that we found the severity parameter
                    }
                    catch (IllegalArgumentException | NullPointerException ex) {
                        // Bad parameter for severity.
                        throw new QueryParameterException(String.format(notConvertable, value, availableSeverity));
                    }
                    break;
                
                default:
                    // Unknown parameter, throw an exception
                    throw new QueryParameterException(String.format(unknownParam, param, availableParams));
            }
        }
        // Now we call the database query resolution function with all 3 parameters, and pass our databaseMethod integer
        // to tell it which query it should use. Based on this, it will know which JPA repository function to call
        // and which query parameters to pass along.
        return queryDatabase(databaseMethod, deviceId, status, severity);
    }

    private static List<MaintenanceTask> queryDatabase(int databaseMethod, Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Query the database based on the parameters we received
        // The databaseMethod integer has a value between 1 and 7
        // DQP_DEVICEID = 1, DQP_STATUS = 2, DQP_SEVERITY = 4.
        // The combination of these values determine which database query method to call
        // from our task repository. This way we don't have to perform null checks on parameters :)
        List<MaintenanceTask> tasks = new ArrayList<>();
        switch (databaseMethod) {
            case DQP_DEVICEID:
                // 1: only device
                tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(deviceId);
                break;
            case DQP_STATUS:
                // 2: only status
                tasks = taskRepository.findAllByStatusOrderBySeverityAscRegistered(status);
                break;
            case DQP_DEVICEID + DQP_STATUS:
                // 3: device and status
                tasks = taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(deviceId, status);
                break;
            case DQP_SEVERITY:
                // 4: only severity
                tasks = taskRepository.findAllBySeverityOrderBySeverityAscRegistered(severity);
                break;
            case DQP_DEVICEID + DQP_SEVERITY:
                // 5: device and severity
                tasks = taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(deviceId, severity);
                break;
            case DQP_STATUS + DQP_SEVERITY:
                // 6: status and severity
                tasks = taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(status, severity);
                break;
            case DQP_DEVICEID + DQP_STATUS + DQP_SEVERITY:
                // 7: device, status, severity
                tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(deviceId, status, severity);
                break;
        }
        return tasks;
    }
}

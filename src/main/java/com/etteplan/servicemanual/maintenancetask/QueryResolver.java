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
    
    /* Database Query Parameters represent a combined value between 1 and 7.
     * Based on this value, we decide which database query function to call
     * in order to retrieve tasks.
     * Each query parameter has been assigned its own numerical value:
     * deviceId: 1, status: 2, severity: 4. The combination of these values tells us
     * which parameters we need to pass to the JPA repository function.
     * So for example, if we encounter the deviceId and status parameters in our query,
     * the combined value will become 3. So we know then to call the function
     * that retrieves tasks by deviceId and status, and pass to it only the parameters deviceId and status.
     * This works similarly to Linux's filesystem permissions numbering system.
     * So if we got a total combined value of 7, it means we filter with all 3 parameters. */
    
    private static final int DQP_DEVICEID = 1;
    private static final int DQP_STATUS = 2;
    private static final int DQP_SEVERITY = 4;

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
        
        int databaseMethod = 0; // Which JPA repository method to call, based on the parameters we encountered
        Long deviceId = null;
        TaskStatus status = null;
        TaskSeverity severity = null;

        for(String param : parameters.keySet()) {
            if (!acceptedQueryParameters.contains(param)) {
                // "Bad request, unknown param '%s'. Available params: ..."
                throw new QueryParameterException(String.format(unknownParam, param, availableParams));
            }
            // If we reached here, no exception was thrown.
            // Parameter is correct, validate the data

            // We perform the bitwise AND operations here
            // to ensure that we don't get duplicate parameters.
            
            String value = parameters.get(param);
            if (param.equals(Q_DEVICEID) && ((databaseMethod & DQP_DEVICEID) == 0)) {
                try {
                    deviceId = Long.parseLong(value);
                    databaseMethod += DQP_DEVICEID; // Indicate that we found the deviceId parameter
                    QueryResolver.deviceId = deviceId; // Stored for hyperlink creation in the controller
                }
                catch(IllegalArgumentException ex) {
                    // Bad parameter for deviceId
                    throw new QueryParameterException(String.format(notConvertable, value, "Must be an integer."));
                }
            }
            else if (param.equals(Q_STATUS) && ((databaseMethod & DQP_STATUS) == 0)) {
                try { 
                    status = TaskStatus.valueOf(value);
                    databaseMethod += DQP_STATUS; // Indicate that we found the status parameter
                }
                catch(IllegalArgumentException ex) {
                    // Bad parameter for status. Throw exception, show which statuses are available
                    throw new QueryParameterException(String.format(notConvertable, value, availableStatus));
                }
            }
            else if (param.equals(Q_SEVERITY) && ((databaseMethod & DQP_SEVERITY) == 0)) {
                try {
                    severity = TaskSeverity.valueOf(value);
                    databaseMethod += DQP_SEVERITY; // Indicate that we found the severity parameter
                }
                catch(IllegalArgumentException ex) {
                    // Bad parameter for severity.
                    throw new QueryParameterException(String.format(notConvertable, value, availableSeverity));
                }
            }
        }
        // Now we call the database query resolution function with all 3 parameters, and pass our databaseMethod variable
        // to tell it which query it should use. Based on this, it will know which JPA repository function to call
        // and which query parameters to pass along.
        return queryDatabase(taskRepository, databaseMethod, deviceId, status, severity);
    }

    private static List<MaintenanceTask> queryDatabase(MaintenanceTaskRepository taskRepository, int databaseMethod, Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Query the database based on the parameters we received
        // The databaseMethod parameter is a value between 1 and 7
        // DQP_DEVICEID = 1, DQP_STATUS = 2, DQP_SEVERITY = 4.
        // The combination of these values determine which database query method to call
        // from our task repository. This way we don't have to perform null checks on parameters :)
        List<MaintenanceTask> tasks = new ArrayList<>();
        switch(databaseMethod) {
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

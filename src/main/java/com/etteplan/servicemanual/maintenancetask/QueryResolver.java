package com.etteplan.servicemanual.maintenancetask;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

// This class resolves which database query method must be called based
// on the given query parameter values.

/* The class is instantiated with a MaintenanceTaskRepository and a Map<String, String>
 * of query parameters for resolution.
 * After instantiating, we need to explicitly call resolveQuery(), which will resolve the query
 * and fetch tasks from the database based on the parameters, OR throw a QueryParameterException
 * in case there was a problem with the parameters. */

class QueryResolver {
    private final MaintenanceTaskRepository taskRepository;
    
    // Query parameter names
    // Any query parameter we encounter which is not listed here is rejected, with a QueryParameterException thrown.
    private final String Q_DEVICEID = "deviceId";
    private final String Q_STATUS = "status";
    private final String Q_SEVERITY = "severity";
    private final List<String> acceptedQueryParameters = Arrays.asList(Q_DEVICEID, Q_STATUS, Q_SEVERITY);

    private Long deviceId = null;
    private TaskStatus status = null;
    private TaskSeverity severity = null;

    private final Map<String, String> parameters;

    public Long getDeviceId() {
        return this.deviceId;
    }

    public QueryResolver(MaintenanceTaskRepository taskRepository, Map<String, String> parameters) { 
        this.taskRepository = taskRepository;
        this.parameters = parameters;
    }

    public List<MaintenanceTask> resolveQuery() throws QueryParameterException {
        if (parameters.size() == 0) {
            // No parameters were actually supplied. Fetch all tasks.
            return taskRepository.findAllByOrderBySeverityAscRegistered();
        }
        // Parameters were supplied. Resolve.
        
        // Some informational messages in case of an exception.
        String unknown = "Bad request: unknown parameter '%s'. %s";
        String availableParams = String.format("Available query parameters: '%s', '%s', '%s'", Q_DEVICEID, Q_STATUS, Q_SEVERITY);
        String notConvertable = "Bad request: could not convert parameter '%s'. %s";
        String availableStatus = "Available values for status: 'OPEN', 'CLOSED'";
        String availableSeverity = "Available values for severity: 'UNIMPORTANT', 'IMPORTANT', 'CRITICAL'";

        for(String param : parameters.keySet()) {
            if (!acceptedQueryParameters.contains(param)) {
                // "Bad request, unknown param '%s'. Available params: ..."
                throw new QueryParameterException(String.format(unknown, param, availableParams));
            }
            else {
                // Parameter is correct, validate the data
                String value = parameters.get(param);
                if (param.equals(Q_STATUS)) {
                    try { 
                        this.status = TaskStatus.valueOf(value);
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for status. Throw exception, show which statuses are available
                        throw new QueryParameterException(String.format(notConvertable, value, availableStatus));
                    }
                }
                else if (param.equals(Q_SEVERITY)) {
                    try {
                        this.severity = TaskSeverity.valueOf(value);
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for severity.
                        throw new QueryParameterException(String.format(notConvertable, value, availableSeverity));
                    }
                }
                else if (param.equals(Q_DEVICEID)) {
                    try {
                        this.deviceId = Long.parseLong(value);
                    }
                    catch(IllegalArgumentException ex) {
                        // Bad parameter for deviceId
                        throw new QueryParameterException(String.format(notConvertable, value, "Must be an integer."));
                    }
                }
            }
        }
        // Query the database
        return queryDatabase();
    }

    private List<MaintenanceTask> queryDatabase() {
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

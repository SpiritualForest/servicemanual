package com.etteplan.servicemanual.maintenancetask;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

// This class resolves which database query method must be called based
// on the given query parameter values.

class QueryResolver {
    
    private final MaintenanceTaskRepository taskRepository;

    // Query parameter names
    private final String Q_DEVICEID = "deviceId";
    private final String Q_STATUS = "status";
    private final String Q_SEVERITY = "severity";
    private final List<String> acceptedQueryParameters = Arrays.asList(Q_DEVICEID, Q_STATUS, Q_SEVERITY);
    private Map<String, String> parameters;

    private Long deviceId = null;
    private TaskStatus status = null;
    private TaskSeverity severity = null;

    public Long getDeviceId() {
        return this.deviceId;
    }

    public QueryResolver(MaintenanceTaskRepository taskRepository, Map<String, String> queryParameters) {
        this.taskRepository = taskRepository;
        this.parameters = queryParameters;
    }

    public List<MaintenanceTask> resolveQuery() throws QueryParameterException {
        // Resolve the query parameters.
        
        // Some informational messages in case of an exception.
        String unknown = "Bad request: unknown parameter '%s'";
        String notConvertable = "Bad request: could not convert parameter '%s'. %s";
        String availableStatus = "Available parameters for status: 'OPEN', 'CLOSED'";
        String availableSeverity = "Available parameters for severity: 'UNIMPORTANT', 'IMPORTANT', 'CRITICAL'";

        for(String param : parameters.keySet()) {
            if (!acceptedQueryParameters.contains(param)) {
                throw new QueryParameterException(String.format(unknown, param));
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
        return queryDatabase();
    }

    private List<MaintenanceTask> queryDatabase() {
        // Here we check which database query we should perform, based on the values
        // of our private fields, which were set by our resolve() method.
        List<MaintenanceTask> tasks = new ArrayList<>();
        if (this.deviceId == null) {
            // Only status and severity
            if (this.status != null && this.severity != null) {
                tasks = taskRepository.findAllByStatusAndSeverityOrderBySeverityAscRegistered(this.status, this.severity);
            }
            else if (this.status != null) {
                tasks = taskRepository.findAllByStatusOrderBySeverityAscRegistered(this.status);
            }
            else if (this.severity != null) {
                tasks = taskRepository.findAllBySeverityOrderBySeverityAscRegistered(this.severity);
            }
        }
        else {
            // device, status, severity
            if (this.status != null && this.severity != null) {
                tasks = taskRepository.findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(this.deviceId, this.status, this.severity);
            }
            else if (status != null) {
                tasks = taskRepository.findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(this.deviceId, this.status);
            }
            else if (severity != null) {
                tasks = taskRepository.findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(this.deviceId, this.severity);
            }
            else {
                // Only device
                tasks = taskRepository.findAllByDeviceIdOrderBySeverityAscRegistered(this.deviceId);
            }
        }
        return tasks;
    }
}

package com.etteplan.servicemanual.maintenancetask;

// This exception is thrown when our QueryResolver encounters
// an unconvertable parameter.

public class QueryParameterException extends Exception {
    public QueryParameterException(String message) {
        super(message);
    }
}

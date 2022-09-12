package com.etteplan.servicemanual.maintenancetask;

// This exception is thrown when the TaskEditor encounters a bad request body
// in the PATCH request from controller

public class RequestBodyException extends Exception {
    public RequestBodyException(String message) {
        super(message);
    }
}


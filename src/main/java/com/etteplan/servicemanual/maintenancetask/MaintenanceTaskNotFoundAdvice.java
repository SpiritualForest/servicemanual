package com.etteplan.servicemanual.maintenancetask;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class MaintenanceTaskNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(MaintenanceTaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String maintenanceTaskNotFoundHandler(MaintenanceTaskNotFoundException ex) {
        return ex.getMessage();
    }
}

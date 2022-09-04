package com.etteplan.servicemanual.maintenancetask;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
public class MaintenanceTask {
    
    /* Fields */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private Long deviceId; // The device that underwent this maintenance task

    @Enumerated(EnumType.STRING)
    @NotNull
    private TaskSeverity severity; // Severity
    
    @Enumerated(EnumType.ORDINAL)
    @NotNull
    private TaskStatus status; // Open or closed
    
    @NotNull
    @NotEmpty
    private String description;
    
    private LocalDateTime registered;

    protected MaintenanceTask() {
        // Default constructor
        this.registered = LocalDateTime.now();
    }

    public MaintenanceTask(Long deviceId, TaskSeverity severity, TaskStatus status, String description) {
        // Overloaded constructor for creating new tasks: registeration time will automatically be right now
        this.deviceId = deviceId;
        this.severity = severity;
        this.status = status;
        this.description = description;
        this.registered = LocalDateTime.now();
    }
    
    /**
     * @param deviceId Id of the device this task is tied to
     * @param severity Severity of the task, unimportant / important / critical
     * @param status Status of the task
     * @param description Description of the task
     * @param registered Registration time of the task
     */
    public MaintenanceTask(Long deviceId, TaskSeverity severity, TaskStatus status, String description, LocalDateTime registered) {
        this.deviceId = deviceId;
        this.severity = severity;
        this.status = status;
        this.description = description;
        this.registered = registered;
    }

    public Long getId() {
        return this.id;
    }

    public void setId() {
        this.id = id;
    }

    public Long getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public TaskSeverity getSeverity() {
        return this.severity;
    }

    public void setSeverity(TaskSeverity severity) {
        this.severity = severity;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRegistered() {
        return this.registered;
    }

    public void setRegistered(LocalDateTime registered) {
        this.registered = registered;
    }
}

package com.etteplan.servicemanual.maintenancetask;

import com.etteplan.servicemanual.factorydevice.FactoryDevice;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.EnumType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class MaintenanceTask {
    
    /* Fields */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long deviceId; // The device that underwent this maintenance task

    @Enumerated(EnumType.STRING)
    private TaskSeverity severity; // Severity
    
    @Enumerated(EnumType.ORDINAL)
    private TaskStatus status; // Open or closed
    private String description;
    private LocalDateTime registered; // FIXME: we might have to use int or some other type here, depending on the database

    protected MaintenanceTask() {} // Default constructor because... satan.

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

    public Long getDeviceId() {
        return this.deviceId;
    }

    /* public FactoryDevice getDevice() { } */


    public TaskSeverity getSeverity() {
        return this.severity;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getRegistered() {
        return this.registered;
    }
}

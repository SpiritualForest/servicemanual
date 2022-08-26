package com.etteplan.servicemanual;

import com.etteplan.servicemanual.maintenancetask.MaintenanceTask;
import com.etteplan.servicemanual.maintenancetask.TaskSeverity;
import com.etteplan.servicemanual.maintenancetask.TaskStatus;

import com.etteplan.servicemanual.factorydevice.FactoryDevice;

import java.util.Random;
import java.util.List;
import java.util.Arrays;

public class DatabaseInitializer {
    /* Database initialization stuff. */
    
    private final Random random = new Random();

    /* Task values */
    private List<String> taskDescriptions = Arrays.asList(
            "Fixing CPU cooling mechanism",
            "Cleaning",
            "Bug fixes",
            "Glueing everything back together",
            "General fixes",
            "Casual cleanup",
            "A bad description because the employee was annoyed muahahahahaha >:D",
            "Fix nuclear meltdown",
            "Device was overheating",
            "Replaced a transistor"
        );

    private List<TaskSeverity> severities = Arrays.asList(
            TaskSeverity.CRITICAL,
            TaskSeverity.IMPORTANT,
            TaskSeverity.UNIMPORTANT
        );

    private List<TaskStatus> statuses = Arrays.asList(
            TaskStatus.OPEN,
            TaskStatus.CLOSED
        );

    /* Device values */
    private List<String> deviceTypes = Arrays.asList(
            "Computer",
            "Monitor",
            "Electric vehicle",
            "Gym equipment",
            "Temperature sensor",
            "Refrigerator",
            "Oven",
            "Fan"
        );
    
    private List<String> deviceNames = Arrays.asList(
            "localhostComp",
            "John's device",
            "Old runner",
            "Baker",
            "Charlie",
            "The Tank",
            "Bobby the stimulator"
        );

    public MaintenanceTask createRandomTask(Long deviceId) {
        // Create a random task, associated with <deviceId>
        String desc = taskDescriptions.get(random.nextInt(taskDescriptions.size())); // Random description
        TaskSeverity severity = severities.get(random.nextInt(severities.size()));
        TaskStatus status = statuses.get(random.nextInt(statuses.size()));

        // deviceId, severity, status, description
        MaintenanceTask task = new MaintenanceTask(deviceId, severity, status, desc);
        return task;
    }

    public FactoryDevice createRandomDevice() {
        // Creates a new random device whose Year param is between 1975-2022 (inclusive)
        String name = deviceNames.get(random.nextInt(deviceNames.size()));
        String type = deviceTypes.get(random.nextInt(deviceTypes.size()));
        int year = random.nextInt(2023-1975)+1975;
        return new FactoryDevice(name, year, type);
    }
}

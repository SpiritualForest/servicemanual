package com.etteplan.servicemanual;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.etteplan.servicemanual.factorydevice.FactoryDevice;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;
import com.etteplan.servicemanual.maintenancetask.MaintenanceTask;
import com.etteplan.servicemanual.maintenancetask.MaintenanceTaskRepository;

import com.etteplan.servicemanual.DatabaseInitializer;

@SpringBootApplication
public class ServiceManualApplication {
    
    @Autowired
    private FactoryDeviceRepository deviceRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    private DatabaseInitializer db = new DatabaseInitializer();

    public static void main(final String[] args) {
        SpringApplication.run(ServiceManualApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(String... args) {
        // FIXME: command line arguments.
        if (deviceRepository.findAll().isEmpty() && taskRepository.findAll().isEmpty()) {
            // Create 100 random devices
            List<FactoryDevice> devices = new ArrayList<>();
            List<MaintenanceTask> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                FactoryDevice device = db.createRandomDevice();
                device = deviceRepository.save(device); // Save individual to generate an ID for it
                // Create 3 tasks per device
                for (int x = 0; x < 3; x++) {
                    MaintenanceTask task = db.createRandomTask(device.getId());
                    tasks.add(task);
                }
            }
            taskRepository.saveAll(tasks);
            System.out.println("Database initialized");
        }
        return (params) -> {
            System.out.println("Running on http://localhost:8080/");
        };
    }
}

package com.etteplan.servicemanual;

import java.util.Arrays;
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
    public CommandLineRunner populateDeviceTable(String... args) {
        if (deviceRepository.findAll().isEmpty()) {
            // Create 100 random devices
            List<FactoryDevice> devices = new ArrayList<>();
            for(int i = 0; i < 100; i++) {
                devices.add(db.createRandomDevice());
            }
            deviceRepository.saveAll(devices);
        }
        return (params) -> {
            System.out.println("Running on http://localhost:8080/");
        };
    }

    @Bean
    public CommandLineRunner populateMaintenanceTable(String... args) {
        if (taskRepository.findAll().isEmpty()) {
            // Now find all the devices and create 3 random tasks for each device
            List<MaintenanceTask> tasks = new ArrayList<>();
            List<FactoryDevice> devices = deviceRepository.findAll();
            for(FactoryDevice device : devices) {
                for(int i = 0; i < 3; i++) {
                    MaintenanceTask task = db.createRandomTask(device.getId());
                    tasks.add(task);
                }
            }
            taskRepository.saveAll(tasks);
        }
        return (params) -> {};
    }
}

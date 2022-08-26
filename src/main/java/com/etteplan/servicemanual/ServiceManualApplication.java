package com.etteplan.servicemanual;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.etteplan.servicemanual.factorydevice.FactoryDevice;
import com.etteplan.servicemanual.factorydevice.FactoryDeviceRepository;

@SpringBootApplication
public class ServiceManualApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ServiceManualApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(final FactoryDeviceRepository repository) {
        return (args) -> {

            System.out.println("Running on http://localhost:8080/");

            /**
             * Remove this. Temporary device storage before proper data storage is implemented.
             */
            final List<FactoryDevice> devices = Arrays.asList(
                new FactoryDevice("Device X", 2001, "type 10"),
                new FactoryDevice("Device Y", 2012, "type 3"),
                new FactoryDevice("Device Z", 1985, "type 1")
            );

            repository.saveAll(devices);
        };
    }

}

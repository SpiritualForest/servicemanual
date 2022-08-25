package com.etteplan.servicemanual.maintenancetask;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Query creation: https://docs.spring.io/spring-data/jpa/docs/1.4.2.RELEASE/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
// All single item fetching method name start with "findBy".
// Multiple row fetching methods start with "findAll".

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> { 
    
    // ... where deviceId = <deviceId> order by severity desc, registered
    List<MaintenanceTask> findAllByDeviceIdOrderBySeverityDescRegistered(Long deviceId);

    // ... order by severity desc, registered
    List<MaintenanceTask> findAllByOrderBySeverityDescRegistered();
}

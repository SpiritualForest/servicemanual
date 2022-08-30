package com.etteplan.servicemanual.maintenancetask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Query creation: https://docs.spring.io/spring-data/jpa/docs/1.4.2.RELEASE/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
// All single item fetching method name start with "findBy".
// Multiple row fetching methods start with "findAll".

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> { 
    
    // ... where deviceId = <deviceId> order by severity desc, registered
    List<MaintenanceTask> findAllByDeviceIdOrderBySeverityAscRegistered(Long deviceId);

    // ... order by severity desc, registered
    List<MaintenanceTask> findAllByOrderBySeverityAscRegistered();
    
    // ... where deviceId = <deviceId>
    List<MaintenanceTask> findAllByDeviceId(Long deviceId);

    // where status = <status> and severity = <severity> order by severity
    List<MaintenanceTask> findAllByStatusAndSeverityOrderBySeverityAscRegistered(TaskStatus status, TaskSeverity severity);

    // where status = <status>
    List<MaintenanceTask> findAllByStatusOrderBySeverityAscRegistered(TaskStatus status);

    // where severity = <severity>
    List<MaintenanceTask> findAllBySeverityOrderBySeverityAscRegistered(TaskSeverity severity);

    // Same as the above methods with severity and status, just including deviceId
    List<MaintenanceTask> findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(Long deviceId, TaskStatus status, TaskSeverity severity);

    List<MaintenanceTask> findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(Long deviceId, TaskStatus status);

    List<MaintenanceTask> findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(Long deviceId, TaskSeverity severity);
}

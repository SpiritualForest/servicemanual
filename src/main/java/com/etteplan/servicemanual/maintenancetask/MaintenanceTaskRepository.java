package com.etteplan.servicemanual.maintenancetask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Query creation: https://docs.spring.io/spring-data/jpa/docs/1.4.2.RELEASE/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
// All single item fetching method name start with "findBy".
// Multiple row fetching methods start with "findAll".

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> { 
    
    // ... where deviceId = <deviceId> order by severity asc, registered
    List<MaintenanceTask> findAllByDeviceIdOrderBySeverityAscRegistered(Long deviceId);

    // ... order by severity asc, registered
    List<MaintenanceTask> findAllByOrderBySeverityAscRegistered();
    
    // ... where deviceId = <deviceId>
    List<MaintenanceTask> findAllByDeviceId(Long deviceId);

    // where status = <status> and severity = <severity> order by severity
    List<MaintenanceTask> findAllByStatusAndSeverityOrderBySeverityAscRegistered(TaskStatus status, TaskSeverity severity);

    // where status = <status>
    List<MaintenanceTask> findAllByStatusOrderBySeverityAscRegistered(TaskStatus status);

    // where severity = <severity>
    List<MaintenanceTask> findAllBySeverityOrderBySeverityAscRegistered(TaskSeverity severity);

    // where deviceId = <deviceId> AND status = <status> AND severity = <severity> order by severity ASC, registered
    List<MaintenanceTask> findAllByDeviceIdAndStatusAndSeverityOrderBySeverityAscRegistered(Long deviceId, TaskStatus status, TaskSeverity severity);
    
    // where deviceId = <deviceId> AND status = <status> order by [...]
    List<MaintenanceTask> findAllByDeviceIdAndStatusOrderBySeverityAscRegistered(Long deviceId, TaskStatus status);

    // where deviceId = <deviceId> AND severity = <severity> order by [...]
    List<MaintenanceTask> findAllByDeviceIdAndSeverityOrderBySeverityAscRegistered(Long deviceId, TaskSeverity severity);

    // where deviceId = <deviceId> AND status = <status> (NO sorting)
    List<MaintenanceTask> findAllByDeviceIdAndStatus(Long deviceId, TaskStatus status);
    
    // where deviceId = <deviceId> AND severity = <severity> (NO sorting)
    List<MaintenanceTask> findAllByDeviceIdAndSeverity(Long deviceId, TaskSeverity severity);

    // where deviceId = <deviceId> AND status = <status> AND severity = <severity> (NO sorting)
    List<MaintenanceTask> findAllByDeviceIdAndStatusAndSeverity(Long deviceId, TaskStatus status, TaskSeverity severity);

    // where status = <status> (NO sorting)
    List<MaintenanceTask> findAllByStatus(TaskStatus status);

    // where severity = <severity> (NO sorting)
    List<MaintenanceTask> findAllBySeverity(TaskSeverity severity);

    // where status = <status> AND severity = <severity> (NO sorting)
    List<MaintenanceTask> findAllByStatusAndSeverity(TaskStatus status, TaskSeverity severity);
}

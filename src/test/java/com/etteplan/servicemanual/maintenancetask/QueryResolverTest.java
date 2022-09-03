package com.etteplan.servicemanual.maintenancetask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

@SpringBootTest
public class QueryResolverTest {

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    private Map<String, String> params = new HashMap<String, String>();
    
    private final Random random = new Random();

    private List<String> taskDescriptions = Arrays.asList(
            "Fixing CPU cooling mechanism",
            "Cleaning",
            "Bug fixes",
            "Glueing everything back together",
            "General fixes",
            "Casual cleanup",
            "A bad description",
            "Fix nuclear meltdown",
            "Device was overheating",
            "Replaced a transistor"
        );

    private void createTasks(int amount, Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Creates a list of <amount> of maintenance tasks
        String desc = taskDescriptions.get(random.nextInt(taskDescriptions.size())); // Random description 
        List<MaintenanceTask> tasks = new ArrayList<>();
        for(int i = 0; i < amount; i++) {
            MaintenanceTask task = new MaintenanceTask();
            task.setDeviceId(deviceId);
            task.setStatus(status);
            task.setSeverity(severity);
            task.setDescription(desc);
            tasks.add(task);
        }
        taskRepository.saveAll(tasks);
    }

    @BeforeEach
    private void setUp() {
        // Before each test, clear the parameters hashmap
        params.clear();
    }

    @Test
    public void resolveQueryUnknownParameter() throws Exception {
        params.put("badparam", "badvalue");
        assertThrows(QueryParameterException.class, () -> {
            QueryResolver.resolveQuery(params);
        });
    }

    @Test
    public void resolveQueryBadValue() throws Exception {
        params.put("deviceId", "hello");
        assertThrows(QueryParameterException.class, () -> {
            QueryResolver.resolveQuery(params);
        });
    }

    @Test
    public void resolveQueryGoodValueBadValue() throws Exception {
        params.put("deviceId", "1");
        params.put("status", "hello");
        assertThrows(QueryParameterException.class, () -> {
            QueryResolver.resolveQuery(params);
        });
    }

    @Test
    public void resolveQueryKnownAndUnknownParams() throws Exception {
        params.put("deviceId", "1");
        params.put("imaginary", "value");
        assertThrows(QueryParameterException.class, () -> {
            QueryResolver.resolveQuery(params);
        });
    }

    @Test
    public void resolveQueryFetchByStatus() throws Exception {
        createTasks(5, 2L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        params.put("status", "OPEN");
        List<MaintenanceTask> tasks = QueryResolver.resolveQuery(params);
        // Assert that each task's status is OPEN
        for(MaintenanceTask task : tasks) {
            assertEquals(TaskStatus.OPEN, task.getStatus());
        }
    }

    @Test
    public void resolveQueryFetchBySeverity() throws Exception {
        createTasks(3, 5L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("severity", "IMPORTANT");
        List<MaintenanceTask> tasks = QueryResolver.resolveQuery(params);
        for(MaintenanceTask task : tasks) {
            assertEquals(TaskSeverity.IMPORTANT, task.getSeverity());
        }
    }

    @Test
    public void resolveQueryFetchByDeviceId() throws Exception {
        createTasks(2, 1L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        params.put("deviceId", "1");
        List<MaintenanceTask> tasks = QueryResolver.resolveQuery(params);
        for(MaintenanceTask task : tasks) {
            assertEquals(1L, task.getDeviceId());
        }
    }

    @Test
    public void resolveQueryFetchByStatusAndSeverity() throws Exception {
        createTasks(2, 2L, TaskStatus.OPEN, TaskSeverity.CRITICAL);
        params.put("status", "OPEN");
        params.put("severity", "CRITICAL");
        for(MaintenanceTask task : QueryResolver.resolveQuery(params)) {
            // Assert the status and severity
            assertEquals(TaskStatus.OPEN, task.getStatus());
            assertEquals(TaskSeverity.CRITICAL, task.getSeverity());
        }
    }

    @Test
    public void resolveQueryFetchByDeviceAndStatusAndSeverity() throws Exception {
        createTasks(2, 6L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("deviceId", "6");
        params.put("status", "CLOSED");
        params.put("severity", "CRITICAL");
        for(MaintenanceTask task : QueryResolver.resolveQuery(params)) {
            assertEquals(6L, task.getDeviceId());
            assertEquals(TaskStatus.CLOSED, task.getStatus());
            assertEquals(TaskSeverity.IMPORTANT, task.getSeverity());
        }
    }

    @Test
    public void resolveQueryFetchByDeviceAndStatus() throws Exception {
        createTasks(2, 7L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        params.put("deviceId", "7");
        params.put("status", "OPEN");
        for(MaintenanceTask task : QueryResolver.resolveQuery(params)) {
            assertEquals(TaskStatus.OPEN, task.getStatus());
            assertEquals(7L, task.getDeviceId());
        }
    }

    @Test
    public void resolveQueryFetchByDeviceAndSeverity() throws Exception {
        createTasks(2, 4L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        params.put("deviceId", "4");
        params.put("severity", "CRITICAL");
        for(MaintenanceTask task : QueryResolver.resolveQuery(params)) {
            assertEquals(4L, task.getDeviceId());
            assertEquals(TaskSeverity.CRITICAL, task.getSeverity());
        }
    }
}

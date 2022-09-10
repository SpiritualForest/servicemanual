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
public class TaskEditorTest {

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

    private MaintenanceTask createTask(Long deviceId, TaskStatus status, TaskSeverity severity) {
        String desc = taskDescriptions.get(random.nextInt(taskDescriptions.size())); // Random description 
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(deviceId);
        task.setStatus(status);
        task.setSeverity(severity);
        task.setDescription(desc);
        return taskRepository.save(task);
    }

    @BeforeEach
    private void setUp() {
        // Before each test, clear the parameters hashmap
        params.clear();
    }

    @Test
    public void editTaskDescription() throws Exception {
        MaintenanceTask task = createTask(3L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        params.put("description", "Hello!");
        task = TaskEditor.editTask(task, params);
        assertEquals("Hello!", task.getDescription());
    }

    @Test
    public void editTaskDescriptionAndStatus() throws Exception {
        MaintenanceTask task = createTask(3L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        params.put("description", "A great description");
        params.put("status", "CLOSED");
        task = TaskEditor.editTask(task, params);
        assertEquals("A great description", task.getDescription());
        assertEquals(TaskStatus.CLOSED, task.getStatus());
    }

    @Test
    public void editTaskDeviceIdAndSeverity() throws Exception {
        MaintenanceTask task = createTask(3L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        params.put("deviceId", "4");
        params.put("severity", "IMPORTANT");
        task = TaskEditor.editTask(task, params);
        assertEquals(4L, task.getDeviceId());
        assertEquals(TaskSeverity.IMPORTANT, task.getSeverity());
    }

    @Test
    public void editTaskEverything() throws Exception {
        MaintenanceTask task = createTask(4L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("deviceId", "5");
        params.put("status", "OPEN");
        params.put("severity", "CRITICAL");
        params.put("description", "Hello world");
        params.put("registered", "2022-09-09T14:34:01");
        task = TaskEditor.editTask(task, params);
        // Assert equalities
        assertEquals(5L, task.getDeviceId());
        assertEquals(TaskStatus.OPEN, task.getStatus());
        assertEquals(TaskSeverity.CRITICAL, task.getSeverity());
        assertEquals("Hello world", task.getDescription());
        assertEquals("2022-09-09T14:34:01", task.getRegistered().toString());
    }

    @Test
    public void editTaskEmptyDesc() throws Exception {
        MaintenanceTask task = createTask(7L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        params.put("description", "");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }

    @Test
    public void editTaskNullDesc() throws Exception {
        MaintenanceTask task = createTask(7L, TaskStatus.OPEN, TaskSeverity.CRITICAL);
        params.put("description", null);
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }

    @Test
    public void editTaskBadRegistrationTime() throws Exception {
        MaintenanceTask task = createTask(8L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("registered", "lulz");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }
    
    @Test
    public void editTaskUnknownParam() throws Exception {
        MaintenanceTask task = createTask(8L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("some_unknown_param", "lulz");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }  
    
    @Test
    public void editTaskEmptyBody() throws Exception {
        // Empty request body
        MaintenanceTask task = createTask(8L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }  
    
    @Test
    public void editTaskBadDeviceId() throws Exception {
        MaintenanceTask task = createTask(8L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("deviceId", "lulz");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }

    @Test
    public void editTaskBadStatus() throws Exception {
        MaintenanceTask task = createTask(9L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("status", "lulz");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }
    
    @Test
    public void editTaskBadSeverity() throws Exception {
        MaintenanceTask task = createTask(9L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        params.put("severity", "lulz");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }

    @Test
    public void editTaskGoodParamBadParam() throws Exception {
        MaintenanceTask task = createTask(10L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        params.put("deviceId", "1");
        params.put("status", "lulzies");
        assertThrows(RequestBodyException.class, () -> {
            TaskEditor.editTask(task, params);
        });
    }
}


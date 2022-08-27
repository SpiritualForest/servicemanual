package com.etteplan.servicemanual.maintenancetask;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.MvcResult;

import com.etteplan.servicemanual.maintenancetask.MaintenanceTaskRepository;
import com.etteplan.servicemanual.maintenancetask.TaskStatus;
import com.etteplan.servicemanual.maintenancetask.TaskSeverity;

import java.util.ArrayList;
import java.util.List;

// MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/endpoint").accept(MediaType.APPLICATION_JSON))
// .andExcept(status().isOk()).andReturn();
//
// result.getResponse().getContentAsString();

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MaintenanceTaskControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    // GET tests
    
    @Test
    public void getMaintenanceTasks() throws Exception {
        // All tasks
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getMaintenanceTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getSingleTask() throws Exception {
        MaintenanceTask task = new MaintenanceTask();
        task.setSeverity(TaskSeverity.UNIMPORTANT);
        task.setStatus(TaskStatus.CLOSED);
        task.setDescription("Some description");
        task.setDeviceId(1L);
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.get(String.format("/api/tasks/%d", task.getId())).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getSingleTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // POST

    @Test
    public void addTask() throws Exception {
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn();
    }
    
    @Test
    public void addTaskNullValue() throws Exception {
        // Try adding a task with a null device ID. Should return bad request
        String json = "{\"deviceId\": null, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void addTaskBadRegistrationTime() throws Exception {
        // Add it with a malformed registration time
        // Result should a disregard of this value altogether, with a replacement by LocalDateTime.now()
        // in the TaskMaintenance's default constructor.
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\", \"registered\": \"some random value lol\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void addTaskDeviceNotFound() throws Exception {
        // Add a task with a deviceId that doesn't exist.
        String json = "{\"deviceId\": 123456789, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isNotFound()).andReturn();
    }

    // PUT

    @Test
    public void modifyTask() throws Exception {
        // Modify a task, set its status to closed
        MaintenanceTask newTask = new MaintenanceTask();
        newTask.setSeverity(TaskSeverity.IMPORTANT);
        newTask.setStatus(TaskStatus.OPEN);
        newTask.setDescription("A test task");
        newTask.setDeviceId(1L);
        newTask = taskRepository.save(newTask); // to get the id
        // Now modify it
        String json = "{\"status\": \"CLOSED\", \"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\"}"; 
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(String.format("/api/tasks/%d", newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
        // Now also load up the task object from the repository and compare the status
        MaintenanceTask modifiedTask = taskRepository.findById(newTask.getId()).get();
        assertEquals(newTask.getId(), modifiedTask.getId());
        assertEquals(TaskStatus.CLOSED, modifiedTask.getStatus());
    }

    @Test
    public void modifyTaskNotFound() throws Exception {
        // Try to modify a non existent task. Should return 404
        String json = "{\"status\": \"CLOSED\", \"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\"}"; 
        mvc.perform(MockMvcRequestBuilders.put("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isNotFound());
    }

    @Test
    public void modifyTaskNullValue() throws Exception {
        // Try to modify a task, but pass null values. Should return BadRequest.
        String json = "{\"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\"}"; 
        MaintenanceTask newTask = new MaintenanceTask();
        newTask.setSeverity(TaskSeverity.IMPORTANT);
        newTask.setStatus(TaskStatus.OPEN);
        newTask.setDescription("A test task");
        newTask.setDeviceId(1L);
        newTask = taskRepository.save(newTask); // to get the id
        mvc.perform(MockMvcRequestBuilders.put(String.format("/api/tasks/%d", newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    // DELETE

    @Test
    public void deleteSingleTask() throws Exception {
        MaintenanceTask task = new MaintenanceTask();
        task.setSeverity(TaskSeverity.UNIMPORTANT);
        task.setStatus(TaskStatus.CLOSED);
        task.setDescription("Meaningless drivel");
        task.setDeviceId(1L);
        task = taskRepository.save(task);
        // Confirm its existence in the database
        assertTrue(taskRepository.findById(task.getId()).isPresent());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(String.format("/api/tasks/%d", task.getId())).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertFalse(taskRepository.findById(task.getId()).isPresent());
    }

    @Test
    public void deleteTaskNotFound() throws Exception {
        // Try to delete a non existent task
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteTasksByDeviceId() throws Exception {
        // Delete all the tasks associated with a given device.
        
        // First, create a bunch of new tasks
        List<MaintenanceTask> tasks = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            MaintenanceTask task = new MaintenanceTask();
            task.setSeverity(TaskSeverity.IMPORTANT);
            task.setStatus(TaskStatus.CLOSED);
            task.setDeviceId(1L);
            task.setDescription("This task is about to be deleted lulz");
            tasks.add(task);
        }
        taskRepository.saveAll(tasks);
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByDeviceId(1L).isEmpty());
        // Now delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks/device/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceId(1L).isEmpty());
    }

    @Test
    public void deleteTasksDeviceNotFound() throws Exception {
        // Should return isNotFound()
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks/device/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}


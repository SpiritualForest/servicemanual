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
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/102").accept(MediaType.APPLICATION_JSON))
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
        System.out.println(result.getResponse().getContentAsString());
    }
    
    @Test
    public void addTaskNullValue() throws Exception {
        // Try adding a task with a null device ID. Should return bad request
        String json = "{\"deviceId\": null, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
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
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void addTaskDeviceNotFound() throws Exception {
        // Add a task with a deviceId that doesn't exist.
        String json = "{\"deviceId\": 123456789, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isNotFound()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
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
}


package com.etteplan.servicemanual.maintenancetask;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.MvcResult;

import com.etteplan.servicemanual.maintenancetask.MaintenanceTaskRepository;

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
    private MaintenanceTaskRepository repository;

    // GET tests
    
    @Test
    public void getMaintenanceTasks() throws Exception {
        // All tasks
        mvc.perform(MockMvcRequestBuilders.get("/tasks").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getMaintenanceTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getSingleTask() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/tasks/102").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getSingleTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // POST

    @Test
    public void addTask() throws Exception {
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }
    
    @Test
    public void addTaskNullValue() throws Exception {
        String json = "{\"deviceId\": null, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/tasks/new").accept(MediaType.APPLICATION_JSON)
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
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void addTaskDeviceNotFound() throws Exception {
        // Add a task with a deviceId that doesn't exist.
        String json = "{\"deviceId\": 123456789, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/tasks/new").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isNotFound()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }
}


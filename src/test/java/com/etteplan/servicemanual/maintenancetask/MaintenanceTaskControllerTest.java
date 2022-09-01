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

import org.json.*; // For parsing specific JSON results when filtering tasks

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
    public void getMaintenanceTaskFilterByStatusAndSeverity() throws Exception {
        // All tasks where status == <status> and severity == <severity>
        // First let's create some tasks
        List<MaintenanceTask> tasks = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            // Status closed, severity critical
            MaintenanceTask task = new MaintenanceTask();
            task.setDeviceId(1L);
            task.setSeverity(TaskSeverity.CRITICAL);
            task.setStatus(TaskStatus.CLOSED);
            task.setDescription("A description");
            tasks.add(task);
        }
        for(int i = 0; i < 5; i++) {
            // Status open, severity unimportant
            MaintenanceTask task = new MaintenanceTask();
            task.setDeviceId(1L);
            task.setSeverity(TaskSeverity.UNIMPORTANT);
            task.setStatus(TaskStatus.OPEN);
            task.setDescription("A description");
            tasks.add(task);
        }
        taskRepository.saveAll(tasks);
        assertFalse(taskRepository.findAllByStatus(TaskStatus.CLOSED).isEmpty());
        // Make the request, status closed, severity critical
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("status", "CLOSED").param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status and critical severity have been returned
        JSONObject jsonResult = new JSONObject(result.getResponse().getContentAsString());
        JSONObject em = jsonResult.getJSONObject("_embedded");
        JSONArray taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String severity = taskObj.getString("severity");
            String status = taskObj.getString("status");
            assertEquals("CRITICAL", severity);
            assertEquals("CLOSED", status);
        }
        // Make the request, status open
        result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("status", "OPEN")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status has been returned
        jsonResult = new JSONObject(result.getResponse().getContentAsString());
        em = jsonResult.getJSONObject("_embedded");
        taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String status = taskObj.getString("status");
            assertEquals("OPEN", status);
        }
        // Make the request, severity unimportant
        result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("severity", "UNIMPORTANT")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only unimportant severity has been returned
        jsonResult = new JSONObject(result.getResponse().getContentAsString());
        em = jsonResult.getJSONObject("_embedded");
        taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String severity = taskObj.getString("severity");
            assertEquals("UNIMPORTANT", severity);
        }
    }

    @Test
    public void getMaintenanceTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getMaintenanceTasksGarbageParam() throws Exception {
        // Try to get it with a param that can't be converted to its type
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("status", "NO_SUCH_STATUS").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getMaintenanceTasksNonExistentParams() throws Exception {
        // Get tasks with query params that don't match anything.
        // Should fail, return status 400
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("no_such_param", "no_such_value").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
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
    public void getSingleTaskWithParams() throws Exception {
        // Getting a single task doesn't support any query parameters, so they should just be discarded.
        // The task should be fetched, status 200.
        MaintenanceTask task = new MaintenanceTask();
        task.setSeverity(TaskSeverity.IMPORTANT);
        task.setStatus(TaskStatus.CLOSED);
        task.setDescription("Some description");
        task.setDeviceId(1L);
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.get(String.format("/api/tasks/%d", task.getId())).param("nosuchparam", "nosuchvalue").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getSingleTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getSingleTaskNotFoundWithParams() throws Exception {
        // Should return isNotFound
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").param("param", "value").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getTasksByDeviceId() throws Exception {
        // Get all the tasks associated with a deviceId.
        // This should always return status 200.
        // If the device doesn't exist, it should just return an empty collection.
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getTasksByDeviceIdFilter() throws Exception {
        // Get all the tasks associated with a device, while applying filters to the query
        // Filters: severity and status
        List<MaintenanceTask> tasks = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            // Status closed, severity critical
            MaintenanceTask task = new MaintenanceTask();
            task.setDeviceId(1L);
            task.setSeverity(TaskSeverity.CRITICAL);
            task.setStatus(TaskStatus.CLOSED);
            task.setDescription("A description");
            tasks.add(task);
        }
        for(int i = 0; i < 5; i++) {
            // Status open, severity unimportant
            MaintenanceTask task = new MaintenanceTask();
            task.setDeviceId(1L);
            task.setSeverity(TaskSeverity.UNIMPORTANT);
            task.setStatus(TaskStatus.OPEN);
            task.setDescription("A description");
            tasks.add(task);
        }
        taskRepository.saveAll(tasks);
        // Make the request, status closed, severity critical
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").param("status", "CLOSED").param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status and critical severity have been returned
        JSONObject jsonResult = new JSONObject(result.getResponse().getContentAsString());
        JSONObject em = jsonResult.getJSONObject("_embedded");
        JSONArray taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String severity = taskObj.getString("severity");
            String status = taskObj.getString("status");
            assertEquals("CRITICAL", severity);
            assertEquals("CLOSED", status);
        }
        // Make the request, status open
        result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").param("status", "OPEN")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status has been returned
        jsonResult = new JSONObject(result.getResponse().getContentAsString());
        em = jsonResult.getJSONObject("_embedded");
        taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String status = taskObj.getString("status");
            assertEquals("OPEN", status);
        }
        // Make the request, severity unimportant
        result = mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").param("severity", "UNIMPORTANT")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only unimportant severity has been returned
        jsonResult = new JSONObject(result.getResponse().getContentAsString());
        em = jsonResult.getJSONObject("_embedded");
        taskArray = em.getJSONArray("maintenanceTaskList");
        for(int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            String severity = taskObj.getString("severity");
            assertEquals("UNIMPORTANT", severity);
        }
    }

    @Test
    public void getTasksFilterNonExistentParam() throws Exception {
        // Filter tasks by one good parameter, and one garbage parameter that can't be correctly mapped.
        // Should return 400
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").param("nosuchparam", "nosuchval").accept(MediaType.APPLICATION_JSON)).
            andExpect(status().isBadRequest());
    }

    @Test
    public void getTasksFilterGarbageParam() throws Exception {
        // One good param, one garbage param that can't be converted to its correct type. Should return 400
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks").param("deviceId", "1").param("status", "nosuchstatus").accept(MediaType.APPLICATION_JSON)).
            andExpect(status().isBadRequest());
    }

    // POST

    @Test
    public void addTask() throws Exception {
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andReturn();
    }
    
    @Test
    public void addTaskNullValue() throws Exception {
        // Try adding a task with a null device ID. Should return bad request
        String json = "{\"deviceId\": null, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void addTaskBadRegistrationTime() throws Exception {
        // Add it with a malformed registration time
        // Result should a disregard of this value altogether, with a replacement by LocalDateTime.now()
        // in the TaskMaintenance's default constructor.
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\", \"registered\": \"some random value lol\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void addTaskNonExistentKeyValuePair() throws Exception {
        // Add it with a non existent key/value pair - should return 400
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\", \"randomkey\": \"randomvalue\"}";
        mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskEmptyDescription() throws Exception {
        // Try to add a task with an empty string as a description.
        // Should return bad request due to the NotEmpty constraint we set on it.
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"\"}";
        mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskDeviceNotFound() throws Exception {
        // Add a task with a deviceId that doesn't exist.
        String json = "{\"deviceId\": 123456789, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/tasks/create").accept(MediaType.APPLICATION_JSON)
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
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(String.format("/api/tasks/%d", newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void modifyTaskNonExistentProperty() throws Exception {    
        // Create a task and try to modify it, but pass an unknown property in the body
        // Should return 400 bad request
        MaintenanceTask newTask = new MaintenanceTask();
        newTask.setSeverity(TaskSeverity.IMPORTANT);
        newTask.setStatus(TaskStatus.OPEN);
        newTask.setDescription("A test task");
        newTask.setDeviceId(1L);
        newTask = taskRepository.save(newTask); // to get the id
        // Now try to modify it
        String json = "{\"status\": \"CLOSED\", \"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\", \"unknownKey\": \"unknownValue\"}"; 
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(String.format("/api/tasks/%d", newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
        // Now also load up the task object from the repository and compare the status
        // It should still be OPEN, as the modification should have been aborted
        MaintenanceTask modifiedTask = taskRepository.findById(newTask.getId()).get();
        assertEquals(newTask.getId(), modifiedTask.getId());
        assertEquals(TaskStatus.OPEN, modifiedTask.getStatus());
    }

    // DELETE

    @Test
    public void deleteAllTasks() throws Exception {
        MaintenanceTask task = new MaintenanceTask();
        task.setSeverity(TaskSeverity.IMPORTANT);
        task.setStatus(TaskStatus.CLOSED);
        task.setDescription("Some description");
        task.setDeviceId(1L);
        task = taskRepository.save(task);
        assertTrue(taskRepository.existsById(task.getId()));
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert that no tasks exist
        assertTrue(taskRepository.findAll().isEmpty());
    }
    
    @Test
    public void deleteSingleTask() throws Exception {
        MaintenanceTask task = new MaintenanceTask();
        task.setSeverity(TaskSeverity.UNIMPORTANT);
        task.setStatus(TaskStatus.CLOSED);
        task.setDescription("Meaningless drivel");
        task.setDeviceId(1L);
        task = taskRepository.save(task);
        // Confirm its existence in the database
        assertTrue(taskRepository.existsById(task.getId()));
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
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceId(1L).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndStatusAndSeverity() throws Exception {
        // Delete all the tasks associated with the deviceId, where status is <status> and severity is <severity>
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
        assertFalse(taskRepository.findAllByDeviceIdAndStatusAndSeverity(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").param("status", "CLOSED").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndStatusAndSeverity(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndStatus() throws Exception {
        // Delete all the tasks associated with the deviceId, where status is <status>
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
        assertFalse(taskRepository.findAllByDeviceIdAndStatus(1L, TaskStatus.CLOSED).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").param("status", "CLOSED").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndStatus(1L, TaskStatus.CLOSED).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndSeverity() throws Exception {
        // Delete all the tasks associated with the deviceId, where severity is <severity>
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
        assertFalse(taskRepository.findAllByDeviceIdAndSeverity(1L, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndSeverity(1L, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByStatusAndSeverity() throws Exception {
        // Delete all the tasks where status is <status> and severity is <severity>
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
        assertFalse(taskRepository.findAllByStatusAndSeverity(TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("status", "CLOSED").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByStatusAndSeverity(TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByStatus() throws Exception {
        // Delete all the tasks where status is <status>
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
        assertFalse(taskRepository.findAllByStatus(TaskStatus.CLOSED).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("status", "CLOSED").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByStatus(TaskStatus.CLOSED).isEmpty());
    }

    @Test
    public void deleteTasksBySeverity() throws Exception {
        // Delete all the tasks where severity is <severity>
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
        assertFalse(taskRepository.findAllBySeverity(TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllBySeverity(TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTaskGarbageParams() throws Exception {
        // Delete tasks with garbage parameters - should return 400 bad request
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(1L);
        task.setStatus(TaskStatus.CLOSED);
        task.setSeverity(TaskSeverity.CRITICAL);
        task.setDescription("A task created in our tests :)");
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "lulzbad").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that nothing was deleted
        assertFalse(taskRepository.findAll().isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskNonExistentParam() throws Exception {
        // Delete a task, but make the query with a non existent parameter name.
        // Should return 400 with the request not being mapped anywhere.
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(1L);
        task.setStatus(TaskStatus.CLOSED);
        task.setSeverity(TaskSeverity.CRITICAL);
        task.setDescription("A task created in our tests :)"); 
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("lolshit", "lulzcrap").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks were deleted and that our created task still exists
        assertFalse(taskRepository.findAll().isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskGoodAndGarbageParam() throws Exception {
        // Delete a task, pass one valid parameter, and one garbage parameter
        // It should just delete the tasks with the deviceId.
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(1L);
        task.setStatus(TaskStatus.CLOSED);
        task.setSeverity(TaskSeverity.CRITICAL);
        task.setDescription("A task created in our tests :)");
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").param("lolshit", "lulzies").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks were deleted and that our created task specifically still exists
        assertFalse(taskRepository.findAllByDeviceId(1L).isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskValidAndInvalidParams() throws Exception {
        // Delete a task, pass one valid parameter, and one invalid parameter (that can't be converted to its type)
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(1L);
        task.setStatus(TaskStatus.CLOSED);
        task.setSeverity(TaskSeverity.CRITICAL);
        task.setDescription("A task created in our tests :)");
        task = taskRepository.save(task);
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "1").param("status", "FUCKITHAHAHAHAHA").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks for device ID 1 were deleted
        assertFalse(taskRepository.findAllByDeviceId(1L).isEmpty());
        // Assert that our newly created task still exists
        assertTrue(taskRepository.existsById(task.getId()));
    }
    
    @Test
    public void deleteTasksDeviceNotFound() throws Exception {
        // Should return isOk(), even though nothing happens
        mvc.perform(MockMvcRequestBuilders.delete("/api/tasks").param("deviceId", "123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert that no tasks were deleted
        assertFalse(taskRepository.findAll().isEmpty());
    }
}


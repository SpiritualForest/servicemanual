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
import java.util.Arrays;
import java.util.Random; // For random task descriptions


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
    // Endpoint constants

    private final String API_TASKS = "/api/tasks";
    private final String API_TASKID = "/api/tasks/%d";

    private MaintenanceTask createMaintenanceTask(Long deviceId, TaskStatus status, TaskSeverity severity) {
        // Create a new task, save it, and return it.
        String desc = taskDescriptions.get(random.nextInt(taskDescriptions.size())); // Random description
        MaintenanceTask task = new MaintenanceTask();
        task.setDeviceId(deviceId);
        task.setStatus(status);
        task.setSeverity(severity);
        task.setDescription(desc);
        return taskRepository.save(task);
    }

    private JSONArray getTaskArray(JSONObject jsonResult) throws JSONException {
        JSONObject em = jsonResult.getJSONObject("_embedded");
        JSONArray taskArray = em.getJSONArray("maintenanceTaskList");
        return taskArray;
    }

    // GET tests
    
    @Test
    public void getMaintenanceTasks() throws Exception {
        // All tasks
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getMaintenanceTaskFilterByStatusAndSeverity() throws Exception {
        // All tasks where status == <status> and severity == <severity>
        // First let's create some tasks
        Long deviceId = 1L;
        for (int i = 0; i < 5; i++) {
            // Status closed, severity critical
            createMaintenanceTask(deviceId, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        }
        // Make the request, status closed, severity critical
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("status", "CLOSED").param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status and critical severity have been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals("CRITICAL", taskObj.getString("severity"));
            assertEquals("CLOSED", taskObj.getString("status"));
        }
    }

    @Test
    public void getMaintenanceTaskFilterByStatus() throws Exception {
        for (int i = 0; i < 5; i++) {
            // Status open, severity unimportant
            createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        }
        
        // Make the request, status open
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("status", "OPEN")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status has been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals("OPEN", taskObj.getString("status"));
        }
    }

    @Test
    public void getMaintenanceTaskFilterBySeverity() throws Exception {
        for (int i = 0; i < 5; i++) {
            // Status open, severity unimportant
            createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.UNIMPORTANT);
        }
        // Make the request, severity unimportant
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("severity", "UNIMPORTANT")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only unimportant severity has been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals("UNIMPORTANT", taskObj.getString("severity"));
        }
    }

    @Test
    public void getMaintenanceTasksNoSuchDevice() throws Exception {
        // Get a task for a device that doesn't exist.
        // Should just return 200 and no task objects.
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getMaintenanceTaskNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getMaintenanceTasksGarbageParam() throws Exception {
        // Try to get it with a param that can't be converted to its type
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("status", "NO_SUCH_STATUS").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getMaintenanceTasksNonExistentParams() throws Exception {
        // Get tasks with query params that don't match anything.
        // Should fail, return status 400
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("no_such_param", "no_such_value").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getMaintenanceTasksEmptyParam() throws Exception {
        // Pass an query parameter. Should return 400
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("status", "").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getSingleTask() throws Exception {
        MaintenanceTask task = createMaintenanceTask(2L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        mvc.perform(MockMvcRequestBuilders.get(String.format(API_TASKID, task.getId())).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getSingleTaskWithParams() throws Exception {
        // Getting a single task doesn't support any query parameters, so they should just be discarded.
        // The task should be fetched, status 200.
        MaintenanceTask task = createMaintenanceTask(2L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        mvc.perform(MockMvcRequestBuilders.get(String.format(API_TASKID, task.getId())).param("nosuchparam", "nosuchvalue").accept(MediaType.APPLICATION_JSON))
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
    public void getSingleTaskBadTaskId() throws Exception {
        // Try to get a task with a taskID that's not an integer.
        // Should return bad request
        mvc.perform(MockMvcRequestBuilders.get("/api/tasks/hellolulz").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getTasksByDeviceId() throws Exception {
        // Get all the tasks associated with a deviceId.
        // This should always return status 200.
        // If the device doesn't exist, it should just return an empty collection.
        for (int i = 0; i < 5; i++) {
            createMaintenanceTask(5L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        }
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "5").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals(5L, taskObj.getLong("deviceId"));
        }
    }

    @Test
    public void getTasksByDeviceIdAndStatusAndSeverity() throws Exception {
        // Get all the tasks associated with a device, while applying filters to the query
        // Filters: severity and status
        for (int i = 0; i < 5; i++) {
            // Status closed, severity critical
            createMaintenanceTask(3L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        }
        // Make the request, status closed, severity critical
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "3").param("status", "CLOSED").param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status and critical severity have been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals(3L, taskObj.getLong("deviceId"));
            assertEquals("CRITICAL", taskObj.getString("severity"));
            assertEquals("CLOSED", taskObj.getString("status"));
        }
    }

    @Test
    public void getTasksByDeviceIdAndStatus() throws Exception {
        for (int i = 0; i < 5; i++) {
            createMaintenanceTask(6L, TaskStatus.OPEN, TaskSeverity.CRITICAL);
        }
        // Make the request, status open
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "6").param("status", "OPEN")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only open status has been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals(6L, taskObj.getLong("deviceId"));
            assertEquals("OPEN", taskObj.getString("status"));
        }
    }

    @Test
    public void getTasksByDeviceIdAndSeverity() throws Exception {
        for (int i = 0; i < 5; i++) {
            createMaintenanceTask(4L, TaskStatus.CLOSED, TaskSeverity.UNIMPORTANT);
        }
        // Make the request, severity unimportant
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "4").param("severity", "UNIMPORTANT")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        // Parse JSON so that we can assert that only unimportant severity has been returned
        JSONArray taskArray = getTaskArray(new JSONObject(result.getResponse().getContentAsString()));
        for (int i = 0; i < taskArray.length(); i++) {
            JSONObject taskObj = taskArray.getJSONObject(i);
            assertEquals(4L, taskObj.getLong("deviceId"));
            assertEquals("UNIMPORTANT", taskObj.getString("severity"));
        }
    }

    @Test
    public void getTasksFilterNonExistentParam() throws Exception {
        // Filter tasks by one good parameter, and one garbage parameter that can't be correctly mapped.
        // Should return 400
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "1").param("nosuchparam", "nosuchval").accept(MediaType.APPLICATION_JSON)).
            andExpect(status().isBadRequest());
    }

    @Test
    public void getTasksFilterGarbageParam() throws Exception {
        // One good param, one garbage param that can't be converted to its correct type. Should return 400
        mvc.perform(MockMvcRequestBuilders.get(API_TASKS).param("deviceId", "1").param("status", "nosuchstatus").accept(MediaType.APPLICATION_JSON)).
            andExpect(status().isBadRequest());
    }

    // POST

    @Test
    public void addTask() throws Exception {
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated());
    }
    
    @Test
    public void addTaskNullValue() throws Exception {
        // Try adding a task with a null device ID. Should return bad request
        String json = "{\"deviceId\": null, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskBadRegistrationTime() throws Exception {
        // Add it with a malformed registration time
        // Result should a disregard of this value altogether, with a replacement by LocalDateTime.now()
        // in the TaskMaintenance's default constructor.
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\", \"registered\": \"some random value lol\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskNonExistentKeyValuePair() throws Exception {
        // Add it with a non existent key/value pair - should return 400
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\", \"randomkey\": \"randomvalue\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    public void addTaskGarbageStatus() throws Exception {
        // Add it with an invalid status - should return 400
        String json = "{\"deviceId\": 1, \"status\": \"NOSUCHSTATUS\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskEmptyDescription() throws Exception {
        // Try to add a task with an empty string as a description.
        // Should return bad request due to the NotEmpty constraint we set on it.
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskDeviceNotFound() throws Exception {
        // Add a task with a deviceId that doesn't exist.
        String json = "{\"deviceId\": 123456789, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isNotFound());
    }

    @Test
    public void addTaskWrongEndpoint() throws Exception {
        // Try to add a task through /api/tasks/{taskId}
        // Should say that the method is not allowed (405)
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        String json = "{\"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        mvc.perform(MockMvcRequestBuilders.post("/api/tasks/" + task.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void addTaskEmptyBody() throws Exception {
        // Try to add a task without providing a task object body in the request
        // Should be 400 bad request
        mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addTaskHTMLDescription() throws Exception {
        // Add a task with HTML in the description. This should be escaped.
        String json = "{ \"status\": \"CLOSED\", \"severity\": \"CRITICAL\", \"deviceId\": 1, \"description\": \"<script type=\\\"text/javascript\\\">alert(\\\"XSS!\\\");</script>\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andReturn();
        JSONObject response = new JSONObject(result.getResponse().getContentAsString());
        Long taskId = response.getLong("id");
        MaintenanceTask task = taskRepository.findById(taskId).get();
        String desc = task.getDescription();
        assertTrue(desc.contains("&gt;"));
        assertTrue(desc.contains("&lt;"));
        assertFalse(desc.contains("<"));
        assertFalse(desc.contains(">"));
    }

    @Test
    public void addTaskWithTaskId() throws Exception {
        // Create a new task, but explicitly provide an id for it in the body.
        // The ID should be discarded.
        String json = "{\"id\": 1, \"deviceId\": 1, \"status\": \"OPEN\", \"severity\": \"CRITICAL\", \"description\": \"Major fixes of security holes\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andReturn();
        JSONObject response = new JSONObject(result.getResponse().getContentAsString());
        Long taskId = response.getLong("id");
        // Assert that the taskId is NOT 1
        assertNotEquals(1L, taskId);
        // Assert that there is no task with ID 1
        assertFalse(taskRepository.existsById(1L));
    }

    // PUT

    @Test
    public void modifyTask() throws Exception {
        // Modify a task, set its status to closed
        MaintenanceTask newTask = createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        // Now modify it
        String json = "{\"status\": \"CLOSED\", \"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\"}"; 
        mvc.perform(MockMvcRequestBuilders.put(String.format(API_TASKID, newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk());
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
    public void modifyTaskNoStatus() throws Exception {
        // Try to modify a task, but don't pass a status in the body. Should return BadRequest.
        String json = "{\"description\": \"A test task\", \"deviceId\": 1, \"severity\": \"IMPORTANT\"}"; 
        MaintenanceTask newTask = createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        // Now try to modify it
        mvc.perform(MockMvcRequestBuilders.put(String.format(API_TASKID, newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
        // Now also load up the task object from the repository and compare the status
        // It should still be OPEN, as the modification should have been aborted
        MaintenanceTask modifiedTask = taskRepository.findById(newTask.getId()).get();
        assertEquals(newTask.getId(), modifiedTask.getId());
        assertEquals(TaskStatus.OPEN, modifiedTask.getStatus());
    }
    
    @Test
    public void modifyTaskEmptyDescription() throws Exception {
        // Try to modify a task, pass empty description. Should return BadRequest.
        String json = "{\"description\": \"\", \"deviceId\": 1, \"severity\": \"IMPORTANT\", \"status\": \"OPEN\"}"; 
        MaintenanceTask newTask = createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        mvc.perform(MockMvcRequestBuilders.put(String.format(API_TASKID, newTask.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void modifyTaskWrongEndpoint() throws Exception {
        // Make the PUT request on /api/tasks/create
        // Should return 400 bad request because it tries to convert "create" into an integer as a taskId
        String json = "{\"description\": \"testing\", \"deviceId\": 1, \"severity\": \"IMPORTANT\", \"status\": \"OPEN\"}"; 
        mvc.perform(MockMvcRequestBuilders.put(API_TASKS).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isMethodNotAllowed());
    }
    
    @Test
    public void modifyTaskHTMLDescription() throws Exception {
        // Modify a task with HTML in the description. This should be escaped.
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.IMPORTANT);
        String json = "{ \"status\": \"CLOSED\", \"severity\": \"CRITICAL\", \"deviceId\": 1, \"description\": \"<script type=\\\"text/javascript\\\">alert(\\\"XSS!\\\");</script>\"}";
        MvcResult result = mvc.perform(MockMvcRequestBuilders.put(String.format(API_TASKID, task.getId())).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn();
        JSONObject response = new JSONObject(result.getResponse().getContentAsString());
        // Assert that HTML was escaped
        task = taskRepository.findById(task.getId()).get();
        String desc = task.getDescription();
        assertTrue(desc.contains("&gt;"));
        assertTrue(desc.contains("&lt;"));
        assertFalse(desc.contains("<"));
        assertFalse(desc.contains(">"));
    }

    // DELETE

    @Test
    public void deleteAllTasks() throws Exception {
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        assertTrue(taskRepository.existsById(task.getId()));
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert that no tasks exist
        assertTrue(taskRepository.findAll().isEmpty());
    }
    
    @Test
    public void deleteSingleTask() throws Exception {
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.UNIMPORTANT);
        // Confirm its existence in the database
        assertTrue(taskRepository.existsById(task.getId()));
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(String.format(API_TASKID, task.getId())).accept(MediaType.APPLICATION_JSON))
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
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        }
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByDeviceId(1L).isEmpty());
        // Now delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceId(1L).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndStatusAndSeverity() throws Exception {
        // Delete all the tasks associated with the deviceId, where status is <status> and severity is <severity>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        } 
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByDeviceIdAndStatusAndSeverity(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "1").param("status", "CLOSED").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndStatusAndSeverity(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndStatus() throws Exception {
        // Delete all the tasks associated with the deviceId, where status is <status>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        } 
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByDeviceIdAndStatus(1L, TaskStatus.CLOSED).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "1").param("status", "CLOSED").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndStatus(1L, TaskStatus.CLOSED).isEmpty());
    }

    @Test
    public void deleteTasksByDeviceIdAndSeverity() throws Exception {
        // Delete all the tasks associated with the deviceId, where severity is <severity>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        } 
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByDeviceIdAndSeverity(1L, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "1").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByDeviceIdAndSeverity(1L, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByStatusAndSeverity() throws Exception {
        // Delete all the tasks where status is <status> and severity is <severity>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        } 
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByStatusAndSeverity(TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("status", "CLOSED").param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByStatusAndSeverity(TaskStatus.CLOSED, TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTasksByStatus() throws Exception {
        // Delete all the tasks where status is <status>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        }
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllByStatus(TaskStatus.CLOSED).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("status", "CLOSED").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllByStatus(TaskStatus.CLOSED).isEmpty());
    }

    @Test
    public void deleteTasksBySeverity() throws Exception {
        // Delete all the tasks where severity is <severity>
        // First, create a bunch of new tasks
        for (int i = 0; i < 10; i++) {
            createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.IMPORTANT);
        } 
        // Assert the existence of the newly created tasks
        assertFalse(taskRepository.findAllBySeverity(TaskSeverity.IMPORTANT).isEmpty());
        // Delete
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("severity", "IMPORTANT").
                accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert the deletion
        assertTrue(taskRepository.findAllBySeverity(TaskSeverity.IMPORTANT).isEmpty());
    }

    @Test
    public void deleteTaskGarbageParams() throws Exception {
        // Delete tasks with garbage parameters - should return 400 bad request
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.CLOSED, TaskSeverity.CRITICAL);
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "lulzbad").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that nothing was deleted
        assertFalse(taskRepository.findAll().isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskNonExistentParam() throws Exception {
        // Delete a task, but make the query with a non existent parameter name.
        // Should return 400 due to a bad parameter
        MaintenanceTask task = createMaintenanceTask(2L, TaskStatus.OPEN, TaskSeverity.CRITICAL);
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("lolshit", "lulzcrap").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks were deleted and that our created task still exists
        assertFalse(taskRepository.findAll().isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskGoodAndGarbageParam() throws Exception {
        // Delete a task, pass one valid parameter, and one garbage parameter
        // It should return 400 bad request and not delete anything
        MaintenanceTask task = createMaintenanceTask(2L, TaskStatus.OPEN, TaskSeverity.CRITICAL); 
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "2").param("lolshit", "lulzies").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks were deleted and that our created task specifically still exists
        assertFalse(taskRepository.findAllByDeviceId(2L).isEmpty());
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTaskValidAndInvalidParams() throws Exception {
        // Delete a task, pass one valid parameter, and one invalid parameter (that can't be converted to its type)
        // Should return 400
        MaintenanceTask task = createMaintenanceTask(1L, TaskStatus.OPEN, TaskSeverity.CRITICAL);  
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "1").param("status", "FUCKITHAHAHAHAHA").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        // Assert that no tasks for device ID 1 were deleted
        assertFalse(taskRepository.findAllByDeviceId(1L).isEmpty());
        // Assert that our newly created task still exists
        assertTrue(taskRepository.existsById(task.getId()));
    }

    @Test
    public void deleteTasksEmptyQueryParam() throws Exception {
        // Pass an empty query parameter. Should return 400
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("status", "").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    public void deleteTasksDeviceNotFound() throws Exception {
        // Should return isOk(), even though nothing happens
        MaintenanceTask task = createMaintenanceTask(3L, TaskStatus.CLOSED, TaskSeverity.UNIMPORTANT);
        mvc.perform(MockMvcRequestBuilders.delete(API_TASKS).param("deviceId", "123456789").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        // Assert that no tasks were deleted
        assertFalse(taskRepository.findAll().isEmpty());
        // Assert that our new task still exists
        assertTrue(taskRepository.existsById(task.getId()));
    }
}

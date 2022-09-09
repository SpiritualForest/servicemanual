// All of the front-end's interactions with the REST API are handled here.

/* Add task modal functions */

function configureSaveTaskModalButtons() {
    // Configures the click events for the save task modal and its relevant buttons
    let addTaskBtn = document.getElementById("open-modal-btn");
    let saveTaskModal = document.getElementById("save-task-modal");
    let cancelBtn = document.getElementById("save-task-modal-cancel-btn");
    let saveBtn = document.getElementById("save-task-modal-save-btn");
    addTaskBtn.onclick = function() {
        // Open the saveTaskModal and set the save button
        // onclick function to call saveTask() with no parameters.
        // No parameters on saveTask() means create a new one
        saveTaskModal.style.display = "block";
        saveBtn.onclick = function() { saveTask(); }
        // Also hide the task ID div
        document.getElementById("task-id-div").style.display = "none";
        // Set the save task header to "Add task"
        document.getElementById("save-task-header").innerHTML = "Add task";
    }
    // Cancel button hides the div
    cancelBtn.onclick = function() { saveTaskModal.style.display = "none"; }
}

function openEditTaskDiv(id, deviceId, taskStatus, taskSeverity, description) {
    // Open the task editing div
    // This function is hooked up to all the "Edit" buttons, for each task.
    // The parameters come from each table row.
    // View the fillTasksTable() function further down to see the hooking.
    
    let saveTaskModal = document.getElementById("save-task-modal");
    saveTaskModal.style.display = "block";
    
    // Also display the TaskID div and set the save task header to "Edit task"
    document.getElementById("task-id-div").style.display = "block";
    document.getElementById("save-task-header").innerHTML = `Edit task ${id}`;
    
    // Now we populate the various input elements with the task's information
    // TaskID
    document.getElementById("save-task-task-id").value = id;
    // DeviceID
    document.getElementById("save-task-select-device").value = deviceId;
    // Task status
    document.getElementById("save-task-select-status").value = taskStatus;
    // Task severity
    document.getElementById("save-task-select-severity").value = taskSeverity;
    // Description
    document.getElementById("save-task-description").value = description;

    // Set the save task button's action to edit tasks
    // Pass true to indicate that we need to edit a task, rather than create a new one
    let saveTaskBtn = document.getElementById("save-task-modal-save-btn");
    saveTaskBtn.onclick = function() { saveTask(editExisting=true); }
}

function saveTask(editExisting=false) {
    // Saves a task.
    // If editExisting is true, it will use the PUT method and modify an existing task.
    // Otherwise, creates a new task

    // Our div that contains the various input elements
    let saveTaskModal = document.getElementById("save-task-modal");
    
    let deviceId = document.getElementById("save-task-select-device").value; // Select
    let taskStatus = document.getElementById("save-task-select-status").value; // Select
    let taskSeverity = document.getElementById("save-task-select-severity").value; // Select
    let taskDescription = document.getElementById("save-task-description").value; // Textarea
     
    // Escape the HTML in the description - we don't want XSS attacks :)
    taskDescription = taskDescription.replace(/</g, "&lt;");
    taskDescription = taskDescription.replace(/>/g, "&gt");
    
    if (taskDescription == "") {
        errorMsg("Error creating task: description is required");
        return;
    }
    
    let taskObj = {
        deviceId: deviceId,
        status: taskStatus,
        severity: taskSeverity,
        description: taskDescription,
    }
    
    // Method is set to creating new tasks by default
    let requestMethod = "POST";
    let endpoint = "/api/tasks";
    
    if (editExisting) {
        // We're actually editing a task, so set the taskId in the request body,
        // set the HTTP method to PUT, and set the endpoint to /api/tasks/<taskId>
        taskId = document.getElementById("save-task-task-id").value;
        requestMethod = "PATCH";
        endpoint = `/api/tasks/${taskId}`;
    }
    
    // Perform the request
    fetch(endpoint,  {
        method: requestMethod,
        body: JSON.stringify(taskObj),
        headers: { "Content-type": "application/json; charset=UTF-8" }
    }).then(response => {
        if (response.status === 201) {
            // Successfully created the resource
            successMsg("Task created successfully");
            return response.json();
        }
        else if (response.ok) {
            // Successfully edited a task
            successMsg("Changes saved successfully");
            return response.json();
        }
        else {
            // Error
            throw response;
        }
    }).then(() => {
        // Close the save task modal and fetch all the tasks again to refresh the tasks lists.
        saveTaskModal.style.display = "none";
        fetchTasks();
    })
    .catch(err => {
        errorMsg("Error saving task");
        console.log(err);
    })
}

function fetchDevices() {
    /* This function fetches all the devices and populates the Device ID selection menus
     * for filtering, adding, and editing tasks.
     * Since we don't provide the option to add, edit, or remove devices,
     * this function only gets called once. */
    let saveTaskSelectDevice = document.getElementById("save-task-select-device"); // Device ID menu for the adding new tasks
    let selectFilterTasksDevice = document.getElementById("select-filter-tasks-device"); // Device ID menu for filter tasks by device ID
    fetch("/factorydevices").then(response => {
        if (response.ok) {
            return response.json();
        }
        else {
            throw response;
        }
    }).then(devices => {
        for(let device of devices) {
            let deviceId = device.id;
            let name = device.name;
            let type = device.type;
            let year = device.year;
            let optionElementAddTask = document.createElement("option"); // Add task select
            let fullDeviceData = `${deviceId} (${name}/${type}/${year})`;
            optionElementAddTask.value = deviceId;
            optionElementAddTask.innerHTML = fullDeviceData
            saveTaskSelectDevice.appendChild(optionElementAddTask);
            let optionElementFilterTasks = document.createElement("option"); // Filter select
            optionElementFilterTasks.value = deviceId;
            optionElementFilterTasks.innerHTML = fullDeviceData;
            selectFilterTasksDevice.appendChild(optionElementFilterTasks);
        }
    }).catch(err => {
        errorMsg(err);
        console.log(err);
    })
}

function fetchTasks() {
    let applyFilters = document.getElementById("filter-tasks").checked;
    let endpoint = "/api/tasks"
    let searchParams = new URLSearchParams();
    if (applyFilters) {
        // If a filter parameter's value is -1, it is NOT applied to the request
        let deviceId = document.getElementById("select-filter-tasks-device").value;
        if (deviceId != -1) {
            // Apply the device
            searchParams.append("deviceId", deviceId);
        }
        let selectStatus = document.getElementById("select-filter-tasks-status").value;
        let selectSeverity = document.getElementById("select-filter-tasks-severity").value;
        if (selectStatus != -1) {
            searchParams.append("status", selectStatus);
        }
        if (selectSeverity != -1) {
            searchParams.append("severity", selectSeverity);
        }
        // Add the search parameters to the endpoint
        endpoint += "?" + searchParams;
    }
    fetch(endpoint).then(response => {
        if (response.ok) {
            return response.json();
        }
        else {
            throw response;
        }
    })
    .then(responseTasks => {
        fillTasksTable(responseTasks);
    }).catch(err => {
        errorMsg(err);
        console.log(err);
    })
}

function fillTasksTable(tasks) {
    let tableElement = document.getElementById("tasks-table");
    // First, clear the table of the previous rows, so that we can add the new ones
    while(tableElement.rows.length > 1) {
        // 1, not 0, because the first row is all the headers and we want to skip that
        tableElement.deleteRow(1);
    }
    
    if (!("_embedded" in tasks)) {
        // If _embedded is not found in the object, it means no tasks were returned.
        // We do not proceed.
        return;
    }
    
    let severityCell; // We need this to set the red bold font in case severity is critical
    let headerRowCells = tableElement.rows[0].cells;
    for(let i = 0; i < headerRowCells.length; i++) {
        if (headerRowCells[i].innerHTML.toLowerCase() == "severity") {
            severityCell = i;
            break;
        }
    }
    // Forgive me for setting styles through JS :(
    for(let task of tasks["_embedded"]["maintenanceTaskList"]) {

        let id = task.id;
        let deviceId = task.deviceId;
        let taskStatus = task.status;
        let taskSeverity = task.severity;
        let description = task.description;
        let registered = task.registered;
        
        // Escape the HTML in the description - we don't want XSS attacks :)
        description = description.replace(/</g, "&lt;");
        description = description.replace(/>/g, "&gt");
        
        // Task, device, status, severity, description, registered, action
        let row = tableElement.insertRow();
        let allData = [id, deviceId, taskStatus, taskSeverity, description, registered];
        
        for(let i in allData) {
            let cell = row.insertCell(i);
            cell.innerHTML = allData[i];
        }
        if (row.cells[severityCell].innerHTML.toLowerCase() == "critical") {
            // Set bold red font if the severity is critical
            row.cells[severityCell].className = "td-severity-critical";
        }
        // Now add a cell that contains the actions
        let actionCell = row.insertCell(allData.length);
        let editBtn = document.createElement("button");
        let deleteBtn = document.createElement("button");
        editBtn.id = `edit-task-btn-${id}`; // edit-btn-task-112
        editBtn.innerHTML = "Edit";
        editBtn.className = "task-btn";
        deleteBtn.id = `delete-task-btn-${id}`; // delete-btn-task-112
        deleteBtn.innerHTML = "Delete";
        deleteBtn.className = "task-btn";
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
        // Add the functions to edit and delete the task
        editBtn.onclick = function() { openEditTaskDiv(id, deviceId, taskStatus, taskSeverity, description); }
        deleteBtn.onclick = function() { deleteTask(id); }
    }
}

function deleteTask(id) {
    // Delete single task.
    if (!confirm(`Are you sure you want to delete task ${id}?`)) {
        return;
    }
    let endpoint = `/api/tasks/${id}`;
    fetch(endpoint, {
        method: "DELETE"
    }).then(response => {
        if (response.ok) {
            // Task delete, fetch tasks.
            successMsg(`Task ${id} deleted`);
            fetchTasks();
        }
        else {
            throw response;
        }
    }).catch(err => {
        errorMsg(err);
        console.log(err);
    })
}

function deleteAllTasks() {
    // Deletes all tasks
    // Filter parameters apply (if the Filter checkbox is checked), and will affect what tasks are removed.
    if (!confirm("Are you sure you want to delete all these tasks?")) {
        return;
    }
    // Confirmed that the user wants to delete.
    let queryParams = new URLSearchParams();
    let endpoint = "/api/tasks";
    if (document.getElementById("filter-tasks").checked) {
        // Parameters apply to the deletion request
        let deviceId = document.getElementById("select-filter-tasks-device").value;
        let taskStatus = document.getElementById("select-filter-tasks-status").value;
        let taskSeverity = document.getElementById("select-filter-tasks-severity").value;
        if (deviceId != -1) {
            queryParams.append("deviceId", deviceId);
        }
        if (taskStatus != -1) {
            queryParams.append("status", taskStatus);
        }
        if (taskSeverity != -1) {
            queryParams.append("severity", taskSeverity);
        }
        endpoint += "?" + queryParams;
    }
    // Perform the request
    fetch(endpoint, {
        method: "DELETE"
    }).then(response => {
        if (response.ok) {
            // Deletion successful.
            successMsg("Tasks deleted");
            fetchTasks();
        }
        else {
            throw response;
        }
    }).catch(err => {
        errorMsg(err);
        console.log(err);
    })
}

function successMsg(message) {
    // Call actionMsg with isError set to false
    actionMsg(message, false);
}

function errorMsg(message) {
    // Call actionMsg with isError set to true
    actionMsg(message, true);
}

function actionMsg(message, isError) {
    // Shows the information text about a given action that was performed.
    // Also shows errors.
    // Displays the check icon or error icon depending on whether the operation
    // succeeded or failed.
    document.getElementById("information-text").innerHTML = message; // <p> element
    if (!isError) {
        // Success
        document.getElementById("check-img").style.display = "inline";
        document.getElementById("error-img").style.display = "none";
    }
    else {
        // Error
        document.getElementById("error-img").style.display = "inline";
        document.getElementById("check-img").style.display = "none";
    }
}

function configureFilters() {
    // Hook the checkbox onchange event
    let filterCheckbox = document.getElementById("filter-tasks");
    filterCheckbox.onchange = function() {
        // Triggered when the "Filter" checkbox is checked or unchecked
        if (filterCheckbox.checked) {
            // Filters apply to the Delete tasks function now
            document.getElementById("delete-tasks-btn").innerHTML = "Delete tasks (filters apply)";
        }
        else {
            // Filters don't apply to deletion: ALL tasks will be removed
            document.getElementById("delete-tasks-btn").innerHTML = "Delete all tasks";
            // Fetch all
        }
        fetchTasks();
    }
    // Filtration select menus
    let filterDevice = document.getElementById("select-filter-tasks-device");
    let filterSeverity = document.getElementById("select-filter-tasks-severity");
    let filterStatus = document.getElementById("select-filter-tasks-status");

    // Now we hook up the onchange event to all these bastards - all it does is call fetchTasks with the filterDevice's value
    // fetchTasks() issues the fetch() request with the appropriate URLSearchParams based on the values
    // obtained from these select menus.
    filterDevice.onchange = function() { fetchTasks(); }
    filterSeverity.onchange = function() { fetchTasks(); }
    filterStatus.onchange = function() { fetchTasks(); }
}

// Hook up the Delete tasks button's onclick event
let deleteTasksBtn = document.getElementById("delete-tasks-btn");
deleteTasksBtn.onclick = function() { deleteAllTasks(); }

// Configure filters and buttons
configureFilters();
configureSaveTaskModalButtons();
fetchDevices(); // Populate our DeviceID select menus
fetchTasks(); // Populate the tasks table with all tasks

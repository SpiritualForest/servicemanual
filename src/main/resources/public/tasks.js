// All of the front-end's interactions with the REST API are handled here.

/* Add task modal functions */

function configureAddTaskModalButtons() {
    // Configures the click events for the Add task modal and its relevant buttons
    let addTaskBtn = document.getElementById("open-modal-btn");
    let modalDiv = document.getElementById("add-task-modal");
    let cancelBtn = document.getElementById("add-task-modal-cancel-btn");
    let saveBtn = document.getElementById("add-task-modal-save-btn");
    addTaskBtn.onclick = function() { modalDiv.style.display = "block"; }
    cancelBtn.onclick = function() { modalDiv.style.display = "none"; }
    saveBtn.onclick = function() { saveNewTask(modalDiv); }
}

function saveNewTask(modalDiv) {
    let deviceId = document.getElementById("add-task-select-device").value; // Select
    let taskStatus = document.getElementById("add-task-select-status").value; // Select
    let taskSeverity = document.getElementById("add-task-select-severity").value; // Select
    let taskDescription = document.getElementById("add-task-description").value; // Textarea

    // Escape the HTML - we don't want XSS attacks :)
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
        // We don't add task ID or registration time, those are added automatically in the backend.
    }
    // Perform the request
    let endpoint = "/api/tasks/create"
    fetch(endpoint,  {
        method: "POST",
        body: JSON.stringify(taskObj),
        headers: { "Content-type": "application/json; charset=UTF-8" }
    }).then(response => {
        if (response.status === 201) {
            // Successfully created the resource
            successMsg("Task created successfully");
            return response.json();
        }
        else {
            throw response;
        }
    }).then(() => {
        // Close the modal and fetch all the tasks again to refresh the tasks lists.
        modalDiv.style.display = "none";
        fetchTasks();
    })
    .catch(err => {
        errorMsg("Error creating task");
        console.log(err);
    })
}

function fetchDevices() {
    /* This function fetches all the devices and populates the Device ID selection menus
     * for filtering, adding, and editing tasks.
     * Since we don't provide the option to add, edit, or remove devices,
     * this function only gets called once. */
    let addTaskSelectDevice = document.getElementById("add-task-select-device"); // Device ID menu for the adding new tasks
    let selectFilterTasksDevice = document.getElementById("select-filter-tasks-device"); // Device ID menu for filter tasks by device ID
    let editTaskSelectDevice = document.getElementById("edit-task-select-device"); // Device ID menu for editing existing tasks
    fetch("/factorydevices").then(response => {
        if (response.ok) {
            return response.json();
        }
        else {
            throw response;
        }
    })
    .then(devices => {
        for(let device of devices) {
            let deviceId = device.id;
            let name = device.name;
            let type = device.type;
            let year = device.year;
            let optionElementAddTask = document.createElement("option"); // Add task select
            let fullDeviceData = `${deviceId} (${name}/${type}/${year})`;
            optionElementAddTask.value = deviceId;
            optionElementAddTask.innerHTML = fullDeviceData
            addTaskSelectDevice.appendChild(optionElementAddTask);
            let optionElementFilterTasks = document.createElement("option"); // Filter select
            optionElementFilterTasks.value = deviceId;
            optionElementFilterTasks.innerHTML = fullDeviceData;
            selectFilterTasksDevice.appendChild(optionElementFilterTasks);
            let optionElementEditTask = document.createElement("option"); // Edit task select
            optionElementEditTask.value = deviceId;
            optionElementEditTask.innerHTML = fullDeviceData;
            editTaskSelectDevice.appendChild(optionElementEditTask);
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
        
        // Task, device, status, severity, description, registered, action
        let row = tableElement.insertRow();
        let allData = [id, deviceId, taskStatus, taskSeverity, description, registered];
        
        for(let i in allData) {
            let cell = row.insertCell(i);
            cell.innerHTML = allData[i];
        }
        if (row.cells[severityCell].innerHTML.toLowerCase() == "critical") {
            // Set bold red font if the severity is critical
            row.cells[severityCell].style.color = "red";
            row.cells[severityCell].style.fontWeight = "bold";
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
        // Add the functions to edit and delete the tasks
        editBtn.onclick = function() { openEditTaskDiv(id, deviceId, taskStatus, taskSeverity, description); }
        deleteBtn.onclick = function() { deleteTask(id); }
    }
}

function configureEditTaskModalSaveAndCancelButtons() {
    let editTaskDiv = document.getElementById("edit-task");
    let cancelEditBtn = document.getElementById("edit-task-modal-cancel-btn");
    cancelEditBtn.onclick = function() { editTaskDiv.style.display = "none"; }
    let saveEditedTaskBtn = document.getElementById("edit-task-modal-save-btn");
    saveEditedTaskBtn.onclick = function() { saveEditedTask(); }
}

function openEditTaskDiv(id, deviceId, taskStatus, taskSeverity, description) {
    // Open the task editing div
    let editTaskDiv = document.getElementById("edit-task");
    editTaskDiv.style.display = "block";
    
    // Now we populate the various input elements with the task's information
    // TaskID
    document.getElementById("edit-task-id").value = id;
    // DeviceID
    document.getElementById("edit-task-select-device").value = deviceId;
    // Task status
    document.getElementById("edit-task-select-status").value = taskStatus;
    // Task severity
    document.getElementById("edit-task-select-severity").value = taskSeverity;
    // Description
    document.getElementById("edit-task-description").value = description;
}

function saveEditedTask() {
    let editTaskDiv = document.getElementById("edit-task");
    let taskId = document.getElementById("edit-task-id").value;
    let deviceId = document.getElementById("edit-task-select-device").value;
    let taskStatus = document.getElementById("edit-task-select-status").value;
    let taskSeverity = document.getElementById("edit-task-select-severity").value;
    let description = document.getElementById("edit-task-description").value;
    
    // Escape the HTML - we don't want XSS attacks :)
    description = description.replace(/</g, "&lt;");
    description = description.replace(/>/g, "&gt");
    
    if (description == "") {
        errorMsg("Error saving changes: description is required");
        return;
    }
    
    let endpoint = `/api/tasks/${taskId}`;

    let taskObj = {
        // We provide the task ID as an endpoint path variable, not in the body object.
        deviceId: deviceId,
        status: taskStatus,
        severity: taskSeverity,
        description: description,
    }
    // Perform the request
    fetch(endpoint,  {
        method: "PUT",
        body: JSON.stringify(taskObj),
        headers: { "Content-type": "application/json; charset=UTF-8" }
    }).then(response => {
        if (response.status === 200) {
            // Successfully modified the resource
            successMsg("Changes saved successfully");
            return response.json();
        }
        else {
            throw response;
        }
    }).then(() => {
        // Close the modal and fetch all the tasks again to refresh the tasks lists.
        editTaskDiv.style.display = "none";
        fetchTasks();
    })
    .catch(err => {
        errorMsg(err);
        console.log(err);
    })
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
configureAddTaskModalButtons();
configureEditTaskModalSaveAndCancelButtons();
fetchDevices(); // Populate our DeviceID select menus
fetchTasks(); // Populate the tasks table with all tasks

// This JavaScript file interacts with the API using fetch()
// and whatever else. Coming soon.

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
    let endpoint = "/api/tasks/create"

    let taskObj = {
        deviceId: deviceId,
        status: taskStatus,
        severity: taskSeverity,
        description: taskDescription,
        // We don't add task ID or registration time, those are added automatically in the backend.
    }
    // Perform the request
    fetch(endpoint,  {
        method: "POST",
        body: JSON.stringify(taskObj),
        headers: {"Content-type": "application/json; charset=UTF-8"}
    }).then(response => {
        if (response.status === 201) {
            // Successfully created the resource
            return response.json();
        }
    }).then(() => {
        // Close the modal and fetch all the tasks again to refresh the tasks lists.
        modalDiv.style.display = "none";
        fetchTasks(-1);
    })
    .catch(err => {
        console.log("Error: " + err);
    })
}

// TODO: error checks here?
function fetchDevices() {
    let addTaskSelectDevice = document.getElementById("add-task-select-device"); // Device ID menu for the adding new tasks
    let selectFilterTasksDevice = document.getElementById("select-filter-tasks-device"); // Device ID menu for filter tasks by device ID
    fetch("/factorydevices").then(response => {
        if (response.ok) {
            return response.json();
        }
    })
    .then(devices => {
        for(let device of devices) {
            let deviceId = device.id;
            let name = device.name;
            let type = device.type;
            let year = device.year;
            let optionElementAddTask = document.createElement("option");
            let fullDeviceData = `${deviceId} (${name}/${type}/${year})`;
            optionElementAddTask.value = deviceId;
            optionElementAddTask.innerHTML = fullDeviceData
            addTaskSelectDevice.appendChild(optionElementAddTask);
            let optionElementFilterTasks = document.createElement("option");
            optionElementFilterTasks.value = deviceId;
            optionElementFilterTasks.innerHTML = fullDeviceData;
            selectFilterTasksDevice.appendChild(optionElementFilterTasks);
        }
    })
}

function fetchTasks(deviceId) {
    // if deviceId is -1, fetch all from /api/tasks
    // Otherwise, fetch from /api/tasks/device/deviceId
    if (deviceId === -1) {
        fetch("/api/tasks").then(response => {
            if (response.ok) {
                return response.json();
            }
        })
        .then(tasks => {
            fillTasksTable(tasks);
        })
    }
}

function fillTasksTable(tasks) {
    let tableElement = document.getElementById("tasks-table");
    // First, clear the table of the previous rows, so that we can add the new ones
    while(tableElement.rows.length > 1) {
        // 1, not 0, because the first row is all the headers and we want to skip that
        tableElement.deleteRow(1);
    }
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
        for(let x in allData) {
            let cell = row.insertCell(x);
            cell.innerHTML = allData[x];
        }
        // Now add a cell that contains the actions
        let actionCell = row.insertCell(allData.length);
        let editBtn = document.createElement("button");
        let deleteBtn = document.createElement("button");
        editBtn.id = `edit-task-btn-${id}`; // edit-btn-task-112
        editBtn.innerHTML = "Edit";
        editBtn.style.fontSize = "14px";
        editBtn.style.margin = "5px";
        deleteBtn.id = `delete-task-btn-${id}`; // delete-btn-task-112
        deleteBtn.innerHTML = "Delete";
        deleteBtn.style.fontSize = "14px";
        deleteBtn.style.margin = "5px";
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
        // Add the functions to edit and delete the tasks
        editBtn.onclick = function() { editTask(id); }
        deleteBtn.onclick = function() { deleteTask(id); }
    }
}

// TODO
function editTask(id) { }
function deleteTask(id) { }

configureAddTaskModalButtons();
fetchDevices();
fetchTasks(-1);


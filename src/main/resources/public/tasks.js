// This JavaScript file interacts with the API using fetch()
// and whatever else. Coming soon.

/* Add task modal functions */

let addTaskBtn = document.getElementById("open-modal-btn");
let modalDiv = document.getElementById("add-task-modal");
let cancelBtn = document.getElementById("add-task-modal-cancel-btn");
addTaskBtn.onclick = function() { modalDiv.style.display = "block"; }
cancelBtn.onclick = function() { modalDiv.style.display = "none"; }

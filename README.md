### To run the application: 
* Install Maven.
* Navigate to the application's directory and execute the command "mvn install" to install the dependencies.
* Execute the command "mvn spring-boot: run" to run the application.

The application listens on localhost:8080. A front-end web interface will be made available in the coming days.

Please view the api.yml file for a detailed documentation of the API.

Short version:

Endpoints:

/api/tasks - GET, DELETE to get all or delete all tasks.  
/api/tasks/device/deviceId - GET, DELETE, parameter deviceId is an integer. Gets or deletes all tasks associated with the particular device.  
/api/tasks/create - POST. Create a new task. View the api.yml file's definition for MaintenanceTask to see the body content to pass in the request.  
/api/tasks/taskId - GET, PUT, DELETE. Retrieve, update, or delete the task with the given taskId. taskId is an integer. The body for the PUT request is the same as in the /api/tasks/create POST request. View the definition for 'MaintenanceTask' in api.yml.  

MaintenanceTask object example:
```
{
    id: 800,
    deviceId: 1,
    severity: "IMPORTANT",
    status: "CLOSED",
    description: "CPU welding task",
    registered: "2022-08-29T15:10:06.042637",
    _links: {
        self: {
                href: "http://localhost:8080/api/tasks/855"
        }
        tasks: {
                href: "http://localhost:8080/api/tasks"
        }
        device: {
                href: "http://localhost:8080/api/tasks/device/1"
        }
    }
}


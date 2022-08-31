### To run the application: 
* Install Maven.
* Navigate to the application's directory and execute the command "mvn install" to install the dependencies.
* Execute the command "mvn spring-boot:run" to run the application.
* Additionally, you may be interested in running all the unit tests through "mvn test"

The application listens on localhost:8080. A front-end web interface will be made available in the coming days.

Please view the api.yml file for a detailed documentation of the API.

Short version:

Endpoints:

/api/tasks - GET, DELETE to get all or delete all tasks according to the given parameters. Accepted query parameters: deviceId=integer, status=string, severity=string.   
/api/tasks/create - POST. Create a new task. View the api.yml file's definition for MaintenanceTask to see the body content to pass in the request.  
/api/tasks/taskId - GET, PUT, DELETE. Retrieve, update, or delete the task with the given taskId. taskId is an integer. The body for the PUT request is the same as in the /api/tasks/create POST request. View the definition for 'MaintenanceTask' in api.yml.  

MaintenanceTask object example:
```
{
    id: 855,
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
                href: "http://localhost:8080/api/tasks?deviceId=1"
        }
    }
}
```
Object returned by GET on /api/tasks, which returns an array of MaintenanceTask objects:
```
{
    _embedded: {
        maintenanceTaskList: [ array of MaintenanceTask objects ],
    }
    _links: {
        tasks: {
            href: "http://localhost:8080/api/tasks"
        }
    }
}
```
Object returned by GET on /api/task/{taskId}: see the MaintenanceTask object example above.

### To run the application: 
* Install Maven.
* Navigate to the application's directory and execute the command "mvn install" to install the dependencies.
* Execute the command "mvn spring-boot:run" to run the application.
* Additionally, you may be interested in running all the unit tests through "mvn test"

The application listens on localhost:8080. A front-end web interface is available at http://localhost:8080/ when the application is running.  

The application will initialize the database with some randomly created tasks on the first run.  

Please view the api.yml file for a detailed documentation of the API.

Short version:

Endpoints:

/api/tasks - GET, DELETE to get all or delete all tasks according to the given parameters. Accepted query parameters: deviceId=integer, status=string, severity=string.   
/api/tasks/create - POST. Create a new task. View the api.yml file's definition for MaintenanceTask to see the body content to pass in the request.  
/api/tasks/taskId - GET, PUT, DELETE. Retrieve, update, or delete the task with the given taskId. taskId is an integer. The body for the PUT request is the same as in the /api/tasks/create POST request. View the definition for 'MaintenanceTask' in api.yml.  
/api/tasks/all - DELETE. Special endpoint to delete all tasks from the database.

#### Note on query parameters and request mapping:
Requests are mapped according to the best matching path and parameter combination.  
If a request has one good parameter (such as deviceId), and several parameters that don't exist,  
then the function that handles only requests with the deviceId parameter present will be called.  

If none of the parameters match anything, or if the framework can't convert a validly name parameter's data into the entity's required data type, HTTP 400 "bad request" is returned.

Query examples and resulting status responses:
```
GET http://localhost:8080/api/tasks?deviceId=1 - results in 200.
Returns the object that contains an array of MaintenanceTask objects, links, etc.

GET http://localhost:8080/api/tasks?status=NO_SUCH_STATUS - reuslts in 400 BAD REQUEST,
because Hibernate can't map this value to a correct TaskStatus enum value.

DELETE http://localhost:8080/api/tasks?deviceId=1&no_such_param=no_such_value - 200, 
all tasks attached to deviceId 1 are deleted. This happens because Spring Boot doesn't find a parameter mapping for "no_such_param", but "deviceId" is correct, so it executes the function that accepts only "deviceId" as a query parameter.

DELETE http://localhost:8080/api/tasks?status=CLOSED&severity=NO_SUCH_SEVERITY - 400 BAD REQUEST,
because "NO_SUCH_SEVERITY" cannot be correctly converted to a TaskSeverity enum value.
```

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
If the query didn't match anything, only the links portion of the object will exist.  

Object returned by GET on /api/task/{taskId}: see the MaintenanceTask object example above.

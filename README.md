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

/api/tasks - _**GET, DELETE**_ to get all or delete all tasks according to the given parameters.  
Accepted query parameters:
```
deviceId=integer
status=string (OPEN/CLOSED)
severity=string (UNIMPORTANT, IMPORTANT, CRITICAL)  
```
If no query parameters are supplied to the request, it will either fetch all, or _**DELETE ALL TASKS**_.  
_**Exercise caution**_ with the _**DELETE**_ method on this endpoint.  

Supplying unknown or malformed query parameters with _**GET**_ or _**DELETE**_ to this endpoint will result in a 400 "bad request" response.  
Example queries:
```
GET /api/tasks?deviceId=1
GET /api/tasks?status=OPEN&severity=CRITICAL
DELETE /api/tasks?status=CLOSED
DELETE /api/tasks?deviceId=10
```
  
/api/tasks - _**POST**_. Create a new task. View the api.yml file's definition for MaintenanceTask to see the body content to pass in the request.  
**NOTE**: it is _not_ required to pass an explicit taskId in the request body for this method. If an "id" property is present in the request body, it is discarded.  
The database automatically generates an ID, and logs the current time as the task's registration time, when a new task is created.  
It _is_ possible to explicitly pass a registration time if desired, but this property is also **not** required. See the MaintenanceTask object's "registered" property example for the appropriate format to pass a registration time in the request body. Passing a malformed registration time will result in an HTTP 400 "bad request" response.  
Passing an empty request body, unknown properties, or malformed values in the request body, will result in a 400 "bad request" response, and the task will _**NOT**_ be created.  
Query parameters passed to this endpoint are **discarded**, and have no effect on the request or response.  

/api/tasks/{taskId} - _**GET, PUT, DELETE**_. Retrieve, update, or delete the task with the given _taskId_. _taskId_ is an integer. The body for the PUT request is the same as in the /api/tasks POST request. View the definition for 'MaintenanceTask' in api.yml.  
Query parameters passed to this endpoint are **discarded**, and have no effect on the request or response. 
Passing a taskId value that is not an integer will result in a 400 "bad request" response.  

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

Object returned by GET on /api/tasks/{taskId}: see the MaintenanceTask object example above.

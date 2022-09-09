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

### /api/tasks - _GET, DELETE_
Get all or delete all tasks according to the given parameters.  
Accepted query parameters:
```
deviceId=integer
status=string (OPEN/CLOSED)
severity=string (UNIMPORTANT, IMPORTANT, CRITICAL)  
```
If no query parameters are supplied to the request, it will either fetch all, or _**DELETE ALL TASKS**_.  
_**Exercise caution**_ with the _**DELETE**_ method on this endpoint.  

Supplying unknown or malformed query parameters with _**GET**_ or _**DELETE**_ to this endpoint will result in a **400 "bad request"** response.  
Example queries:
```
GET /api/tasks?deviceId=1
GET /api/tasks?status=OPEN&severity=CRITICAL
DELETE /api/tasks?status=CLOSED
DELETE /api/tasks?deviceId=10
```
  
### /api/tasks - _POST_
Create a new task. View the api.yml file's definition for MaintenanceTask to see the body content to pass in the request.  
**NOTE:** the request body should **NOT** contain an explicit taskId. If an "id" property is present in the request body, it is discarded.  
The database automatically generates an ID, and logs the current time as the task's registration time, when a new task is created.  
  
It **_is_** possible to explicitly pass a registration time if desired, but this property is also **NOT** required. See the MaintenanceTask object's "registered" property example for the appropriate format to pass a registration time in the request body.  
Passing a malformed registration time will result in an **HTTP 400 "bad request"** response.  
  
Passing an empty request body, unknown properties, or malformed values in the request body, will result in a **400 "bad request"** response, and the task will _**NOT**_ be created.  
  
Since all tasks are attached to a deviceId, passing deviceId which does not exist in the database will result in a **404 "device not found"** response.  
Query parameters passed with this method are **discarded**, and have no effect on the request or response.  

Example **POST** request with no registration time:
```
POST /api/tasks
Host: localhost
Content-Type: application/json
{ 
    "deviceId": 1,
    "status": "OPEN",
    "severity": "IMPORTANT",
    "description": "Clean power supply fan"
}
```
Example **POST** with registration time:
```
POST /api/tasks
Host: localhost
Content-Type: application/json
{ 
    "deviceId": 1,
    "status": "OPEN",
    "severity": "IMPORTANT",
    "description": "Clean power supply fan",
    "registered": "2022-09-01T10:05:35"
}
```


### /api/tasks/{taskId} - _GET, PATCH, DELETE_
Retrieve, update, or delete the task with the given _taskId_. _taskId_ is an integer. The body for the **PATCH** request is the same as in the **/api/tasks POST** request.  

Example **GET** and **DELETE** requests:
```
GET /api/tasks/750
DELETE /api/tasks/800
```
Query parameters passed to this endpoint are **discarded**, and have no effect on the request or response. 
Passing a _taskId_ value that is not an integer will result in a **400 "bad request"** response.  
If the task doesn't exist in the database, a **404 "task not found"** response will be returned.  

All properties in the request body for the **PATCH** request are **_optional_**. It is possible to modify as many, or as few, fields as desired.  

Passing an empty request body will result in a **400 "bad request"** response.  

Passing unknown properties, or properties with malformed or incorrect values, in the request body, will result in a **400 "bad request"** response. 

When attempting to modify the _deviceId_ of a task, if the device doesn't exist in the database, a **404 "device not found"** response will be retured.  

**NOTE:** in this case, attempting to pass a task ID in the request body will result in a **400 "bad request"** response, as it is not allowed. The ID is passed only as a path variable.  

Example **PATCH** request to change the description on task _700_:
```
PATCH /api/tasks/700
Host: localhost
Content-Type: application/json
{ 
    "description": "Fixed a transistor"
}
```
Example **PATCH** to change the status and severity on task _651_:
```
PATCH /api/tasks/651
Host: localhost
Content-Type: application/json
{
    "status": "CLOSED",
    "severity": "UNIMPORTANT"
}
```

### MaintenanceTask object response example:
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
### Object returned by GET on /api/tasks:
Returns an array of MaintenanceTask objects:
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

### Object returned by GET on /api/tasks/{taskId}:
See the MaintenanceTask object response example above.

package com.etteplan.servicemanual.maintenancetask;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class MaintenanceTaskModelAssembler implements RepresentationModelAssembler<MaintenanceTask, EntityModel<MaintenanceTask>> {
  Map<String, String> params = new HashMap<String, String>();
  Map<String, String> deviceParam = new HashMap<String, String>();

  @Override
  public EntityModel<MaintenanceTask> toModel(MaintenanceTask task) {
    // Add the deviceId to the hashmap 
    deviceParam.put("deviceId", Long.toString(task.getDeviceId()));
    return EntityModel.of(task, //
        // self { ... }
        linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
        // tasks { ... }
        linkTo(methodOn(MaintenanceTaskController.class).all(params)).withRel("tasks"),
        // device
        linkTo(methodOn(MaintenanceTaskController.class).all(deviceParam)).withRel("device"));
    }

  public EntityModel<MaintenanceTask> toModelWithDevice(MaintenanceTask task) {
      return EntityModel.of(task,
              linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
              linkTo(methodOn(MaintenanceTaskController.class).all(params)).withRel("tasks"),
              linkTo(methodOn(MaintenanceTaskController.class).all(deviceParam)).withRel("device"));
  }
}

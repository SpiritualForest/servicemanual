package com.etteplan.servicemanual.maintenancetask;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class MaintenanceTaskModelAssembler implements RepresentationModelAssembler<MaintenanceTask, EntityModel<MaintenanceTask>> {

  @Override
  public EntityModel<MaintenanceTask> toModel(MaintenanceTask task) {

    return EntityModel.of(task, //
        // self { ... }
        linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
        // tasks { ... }
        linkTo(methodOn(MaintenanceTaskController.class).all()).withRel("tasks"),
        // device
        linkTo(methodOn(MaintenanceTaskController.class).all(task.getDeviceId())).withRel("deviceId"));
    }

  public EntityModel<MaintenanceTask> toModelWithDevice(MaintenanceTask task) {
      return EntityModel.of(task,
              linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
              linkTo(methodOn(MaintenanceTaskController.class).all()).withRel("tasks"),
              linkTo(methodOn(MaintenanceTaskController.class).all(task.getDeviceId())).withRel("deviceId"));
  }
}

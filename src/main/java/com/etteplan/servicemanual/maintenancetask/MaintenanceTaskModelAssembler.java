package com.etteplan.servicemanual.maintenancetask;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class MaintenanceTaskModelAssembler implements RepresentationModelAssembler<MaintenanceTask, EntityModel<MaintenanceTask>> {
    
    // We need this map because the all() method accepts it as a parameter
    // Set to public so that the controller can use this field as well, without
    // having to define its own separate maps for its addHyperlinks function
    protected final Map<String, String> params = new HashMap<String, String>();

  @Override
  public EntityModel<MaintenanceTask> toModel(MaintenanceTask task) {
    return EntityModel.of(task, //
        // self { ... }
        linkTo(methodOn(MaintenanceTaskController.class).getTaskById(task.getId())).withSelfRel(),
        // tasks { ... }
        linkTo(methodOn(MaintenanceTaskController.class).all(params)).withRel("tasks"));
    }
}

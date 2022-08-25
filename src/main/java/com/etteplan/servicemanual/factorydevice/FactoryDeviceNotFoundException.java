package com.etteplan.servicemanual.factorydevice;

class FactoryDeviceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    FactoryDeviceNotFoundException(Long id) {
        super("Could not find factory device " + id);
    }
}
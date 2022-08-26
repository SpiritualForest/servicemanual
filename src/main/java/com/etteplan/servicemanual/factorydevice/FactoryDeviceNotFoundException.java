package com.etteplan.servicemanual.factorydevice;

public class FactoryDeviceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FactoryDeviceNotFoundException(Long id) {
        super("Could not find factory device " + id);
    }
}

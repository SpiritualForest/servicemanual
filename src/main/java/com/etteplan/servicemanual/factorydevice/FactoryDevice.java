package com.etteplan.servicemanual.factorydevice;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class FactoryDevice {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private int year;
    private String type;
    
    protected FactoryDevice() {}

    /**
     * Constructor of FactoryDevice class
     * 
     * @param name Name of the device
     * @param year Year of the device
     * @param type Type of the device
     */
    public FactoryDevice(String name, int year, String type) {
        this.name = name;
        this.year = year;
        this.type = type;
    }

    public Long getId() {
        return this.id;
    }

    /**
     * 
     * @return name of the device
     */
    public String getName() {
        return this.name;
    }

    public int getYear() {
        return this.year;
    }

    public String getType() {
        return this.type;
    }
}

package com.etteplan.servicemanual.factorydevice;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class FactoryDeviceControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void getFactoryDevices() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/factorydevices").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void getFactoryDeviceNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/factorydevices/999").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
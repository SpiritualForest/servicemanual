package com.etteplan.servicemanual.maintenancetask;

import java.util.HashMap;

public class QueryParameters {
    // We need these param objects for creating hyperlinks in responses
    public final HashMap<String, String> emptyParams = new HashMap<String, String>();
    public final HashMap<String, String> deviceParam = new HashMap<String, String>();

    public QueryParameters(Long deviceId) {
        this.deviceParam.put("deviceId", Long.toString(deviceId));
    }
}

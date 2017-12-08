package com.pagatodo.ptscreen.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by mcardenas on 20/06/2017.
 */

public class Endpoint implements Serializable {

    private String deviceId;
    private String deviceName;
    private String serviceType;
    private String hostname;
    private int port;
    private boolean connected;


    public Endpoint(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.hostname = null;
        this.port = -1;
    }

    public Endpoint(String deviceId, String deviceName, String hostname, int port) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.hostname = hostname;
        this.port = port;
    }

    public String getURLEndpointString(){
        return "http://" + hostname + ":" + port + "/";
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toJSON(){

        JSONObject object = new JSONObject();

        try {

            object.put("deviceId", deviceId);
            object.put("deviceName", deviceName);
            object.put("serviceType", serviceType);
            object.put("hostname", hostname);
            object.put("port", port);

            return object.toString();
        }catch( JSONException jsone){
            jsone.printStackTrace();
        }

        return null;
    }

    public Endpoint fromJSON( String json ){
        try {

            String deviceId = null;
            String deviceName = null;
            String serviceType = null;
            String hostname = null;
            int port = -1;

            JSONObject jsonobject = new JSONObject(json);
            Endpoint endpoint = new Endpoint(deviceId, deviceName);
            endpoint.setHostname(hostname);
            endpoint.setServiceType(serviceType);
            endpoint.setPort(port);

            return endpoint;
        }catch(JSONException jsone){
            jsone.printStackTrace();
        }

        return null;
    }
}

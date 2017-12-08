package com.zobot.androidtoandroid.model;

import com.zobot.androidtoandroid.model.Model;

/**
 * Created by jvazquez on 06/12/2017.
 */

public class AndroidDevice extends Model {

    private String manufacturer = "";
    private String model        = "";
    private String description  = "";
    private String version      = "";
    private String uri          = "";
    private String serial       = "";

    public AndroidDevice() {
    }

    public AndroidDevice(String manufacturer, String model, String description, String version, String uri, String serial) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.description = description;
        this.version = version;
        this.uri = uri;
        this.serial = serial;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}

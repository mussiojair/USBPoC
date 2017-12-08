package com.zobot.androidtoandroid;

/**
 * Created by jvazquez on 06/12/2017.
 */

public enum CommunicationType {
    USB_HOST(0),
    USB_ACCESSORY(1),
    WIFI_HOST(2),
    WIFI_ACCESSORY(3);

    private int mType = -1;
    CommunicationType(int id){
        this.mType = id;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }
}
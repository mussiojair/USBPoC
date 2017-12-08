package com.zobot.androidtoandroid;

import com.zobot.androidtoandroid.model.AndroidDevice;

/**
 * Created by jvazquez on 06/12/2017.
 */

public interface OnConnectListener {
    void onStatusConnection(AndroidDevice androidDevice, boolean connected);
    void onConnected(AndroidDevice androidDevice);
    void onDisconnected(AndroidDevice androidDevice);
    void onTryConnect(AndroidDevice androidDevice);
}

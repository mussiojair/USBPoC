package com.zobot.androidtoandroid;

import android.content.Context;

import com.zobot.androidtoandroid.common.AbstractCommunication;
import com.zobot.androidtoandroid.model.AndroidDevice;
import com.zobot.androidtoandroid.usb.USBCommunication;
import com.zobot.androidtoandroid.wifi.WIFICommunication;

/**
 * Created by jvazquez on 06/12/2017.
 */

public class CommunicationConfig implements OnConnectListener {
    private CommunicationType mType;
    private Context mContext;
    private AbstractCommunication mCommunication = null;

    private static final CommunicationConfig ourInstance = new CommunicationConfig();

    private static CommunicationConfig getInstance() {
        return ourInstance;
    }

    private CommunicationConfig() {
    }

    public static void init(Context ctx, CommunicationType type){
        getInstance().start(ctx, type);
    }
    public static CommunicationType getType(){
        return getInstance().mType;
    }
    public static void openCommunication(){
        getInstance().open();
    }

    private void open() {
        mCommunication.tryConnection();
    }

    private void start(Context ctx, CommunicationType type) {
        this.mContext = ctx;
        this.mType = type;
        mCommunication = getCommunication();
    }


    private AbstractCommunication getCommunication() {
        switch (mType){
            case WIFI_ACCESSORY:
            case WIFI_HOST:
                return new WIFICommunication(mContext, this);
            case USB_ACCESSORY:
            case USB_HOST:
            default:
                return new USBCommunication(mContext, this);
        }
    }


    @Override
    public void onStatusConnection(AndroidDevice androidDevice, boolean connected) {

    }

    @Override
    public void onConnected(AndroidDevice androidDevice) {

    }

    @Override
    public void onDisconnected(AndroidDevice androidDevice) {

    }

    @Override
    public void onTryConnect(AndroidDevice androidDevice) {

    }
}
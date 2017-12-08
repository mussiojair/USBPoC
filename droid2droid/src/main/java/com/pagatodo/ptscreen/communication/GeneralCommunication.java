package com.pagatodo.ptscreen.communication;

import com.pagatodo.ptscreen.data.Endpoint;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by mcardenas on 19/06/2017.
 */

public abstract class GeneralCommunication {


    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_DISCONNECTED = 2;
    public static final int STATUS_DISCOVERING = 3;
    public static final int STATUS_CONNECTION_FAILED = 4;
    public static final int STATUS_DISCOVERING_FAILED = 5;
    public static final int STATUS_NOT_CONNECTED_TO_WIFI = 6;
    public static final int STATUS_ENDPOINT_FOUND = 7;
    public static final int STATUS_ENDPOINT_LOST = 8;
    public static final int STATUS_ENDPOINT_CONNECTED = 9;


    protected String mDeviceName;
    protected String mServiceName;
    protected String mServiceType;
    protected String mServiceId;
    protected int mLocalPort;
    protected String mHostname;

    protected HashMap<String, Endpoint> mDetectedDevices;
    protected StatusListener mStatusListener;
    protected MessagesListener mMessageListener;
    protected PeerListener mPeerListener;

    public GeneralCommunication(){
        mDetectedDevices = new HashMap<String, Endpoint>();
    }

    public String getServiceId() {
        return mServiceId;
    }

    public void setServiceId(String mServiceId) {
        this.mServiceId = mServiceId;
    }

    public HashMap<String, Endpoint> getConnectedDevices() {
        return mDetectedDevices;
    }

    public void updateEndpointStatus(Endpoint endpoint, boolean status){
        endpoint.setConnected(status);
        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
    }

    public void deleteConnectedDevice(String endpointId){
        mDetectedDevices.remove(endpointId);
    }


    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String mDeviceName) {
        this.mDeviceName = mDeviceName;
    }

    public String getServiceName() {
        return mServiceName;
    }

    public void setServiceName(String mServiceName) {
        this.mServiceName = mServiceName;
    }

    public String getServiceType() {
        return mServiceType;
    }

    public void setServiceType(String mServiceType) {
        this.mServiceType = mServiceType;
    }

    public abstract void startService();
    public abstract void stopService();
    public abstract void startAdvertising();
    public abstract void stopAdvertising();
    public abstract void connectTo(Endpoint endpoint);
    public abstract void disconnectDevice(Endpoint endpoint);
    public abstract void disconnect();
    public abstract void startDiscovering();
    public abstract void stopDiscovering();
    public abstract void setLocalPort(int port);
    public abstract void configureLocalPort();
    public abstract void sendMessageToAll(String message);
    public abstract void sendMessageToDevice(Endpoint endpoint, String message);





    public void setStatusListener( StatusListener statusListener ){
        this.mStatusListener = statusListener;
    }

    public void setPeerListener(PeerListener peerListener){
        this.mPeerListener = peerListener;
    }

    public void setMessagesListener(MessagesListener messageListener){
        this.mMessageListener = messageListener;
    }

    public interface StatusListener extends Serializable{
        public static final int SERVICE_CONNECTED = 1;
        public static final int SERVICE_DISCONNECTED = 2;
        void statusChanged(String status, int statuscode);
    }

    public interface PeerListener extends Serializable{

        public static final int PEER_CONNECTED = 1;
        public static final int PEER_DISCONNECTED  = 2;
        public static final int PEER_FOUND = 3;
        public static final int PEER_LOST = 4;

        void peerStatusChanged(String status, int statuscode, Endpoint endpoint);
        void peerStatusOnLost(String status, int statuscode, Endpoint endpoint);
        void peerStatusOnFound(String status, int statuscode, Endpoint endpoint);
        void peerConnected(String status, int statuscode, Endpoint endpoint);
        void peerDisconnected(String status, int statuscode, Endpoint endpoint);
    }

    public interface MessagesListener extends Serializable{
        void messageReceived(Endpoint endpoint, String message);
    }
}

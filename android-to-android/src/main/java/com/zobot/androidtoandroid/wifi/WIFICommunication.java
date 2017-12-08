package com.zobot.androidtoandroid.wifi;

import android.content.Context;

import com.pagatodo.ptscreen.Connections;
import com.pagatodo.ptscreen.D2D;
import com.pagatodo.ptscreen.Rol;
import com.pagatodo.ptscreen.data.Endpoint;
import com.zobot.androidtoandroid.model.AndroidDevice;
import com.zobot.androidtoandroid.CommunicationConfig;
import com.zobot.androidtoandroid.CommunicationType;
import com.zobot.androidtoandroid.OnConnectListener;
import com.zobot.androidtoandroid.common.AbstractCommunication;

import static com.zobot.androidtoandroid.Logger.logger;

/**
 * Created by jvazquez on 06/12/2017.
 */

public class WIFICommunication extends AbstractCommunication implements D2D.D2DMessagesListener, D2D.D2DPeersListener{
    private D2D mServer = null;
    private static final String SERVICE_ID = "com.zobot.androidtoandroid";

    public WIFICommunication(Context mContext, OnConnectListener onConnectListener) {
        super(mContext, onConnectListener);
    }


    @Override
    public void tryConnection() {
        logger(getContext(), "Iniciando la conexión WIFI como: " + (CommunicationConfig.getType() == CommunicationType.WIFI_HOST ? "HOST" : "ACCESSORY"));
        getServer().startRol();
    }

    @Override
    public void retryConnection() {

    }
    @Override
    public void abortConnection() {

    }

    @Override
    public void sendMessage(AndroidDevice device, String message) {

    }

    @Override
    public void sendBytes(AndroidDevice device, byte[] bytes) {

    }


    @Override
    public void onMessageReceived(Endpoint endpoint, String s) {
        logger(getContext(), "Se ha recibido un mensaje");
    }

    @Override
    public void onPeerFound(Endpoint endpoint) {
        logger(getContext(), "Se ha encontrado un dispositivo");

    }

    @Override
    public void onPeerLost(Endpoint endpoint) {
        logger(getContext(), "Se ha perdido la conexión con un dispositivo");

    }

    @Override
    public void onPeerConnected(Endpoint endpoint) {
        logger(getContext(), "Se ha iniciado la conexión");

    }

    @Override
    public void onPeerDisconnected(Endpoint endpoint) {
        logger(getContext(), "Se ha desconectado");

    }
    private D2D getServer(){
        if(mServer == null){
            int rol = -1;
            switch (CommunicationConfig.getType()){
                case WIFI_ACCESSORY:
                    rol = Rol.ADVERTISER;
                    break;
                case WIFI_HOST:
                    rol = Rol.DISCOVERER;
                    break;
                case USB_HOST:
                case USB_ACCESSORY:
                    throw new RuntimeException("This class need implement type wifi");
            }
            mServer = new D2D.Builder()
                    .setRol(rol)
                    .setServiceId(SERVICE_ID)
                    .setContext(getContext())
                    .setConnectionType(Connections.CONNECTION_SOCKET)
                    .setD2DMessagesListener(this)
                    .setD2DPeersListener(this)
                    .build();
        }
        return mServer;
    }
}

package com.zobot.androidtoandroid.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.zobot.androidtoandroid.UsbHandler;
import com.zobot.androidtoandroid.common.AbstractCommunication;
import com.zobot.androidtoandroid.model.AndroidDevice;
import com.zobot.androidtoandroid.CommunicationConfig;
import com.zobot.androidtoandroid.OnConnectListener;

import java.util.HashMap;

import static com.zobot.androidtoandroid.Logger.logger;
import static com.zobot.androidtoandroid.UsbHandler.ACTION_USB_PERMISSION_ACCESSORY;
import static com.zobot.androidtoandroid.UsbHandler.ACTION_USB_PERMISSION_HOST;

/**
 * Created by jvazquez on 06/12/2017.
 */

public class USBCommunication extends AbstractCommunication implements UsbHostThread.HostStatusListener, UsbHandler.UsbPermissionListener {

    private UsbManager mUsbManager                  = null;
    private UsbAccesoryThread mThreadAccessory      = null;
    private UsbHostThread mUsbHostThread            = null;
    private UsbHandler mUsbPermissionHandler        = null;

    private PendingIntent mPermissionIntent = null;



    public USBCommunication(Context mContext, OnConnectListener onConnectListener) {
        super(mContext, onConnectListener);
        mUsbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        mUsbPermissionHandler = new UsbHandler(this);

        mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(mType.getType() == 0 ? ACTION_USB_PERMISSION_HOST : ACTION_USB_PERMISSION_ACCESSORY ), 0 );

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION_HOST);
        filter.addAction(ACTION_USB_PERMISSION_ACCESSORY);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getContext().registerReceiver(mUsbPermissionHandler.getUsbReceiver(), filter);
    }

    @Override
    public void tryConnection() {
        logger(getContext(), "Intentando la comunicación USB, dispositivos: " + mUsbManager.getDeviceList().size());
        switch (CommunicationConfig.getType()){
            case USB_HOST:
                handleUSBHost();
                break;
            case USB_ACCESSORY:
                handleUSBAccessory();
                break;
            default:
                throw new RuntimeException("only usb");
        }
    }

    private void handleUSBAccessory() {
        UsbAccessory[] usbAccessories = mUsbManager.getAccessoryList();
        if(usbAccessories != null && usbAccessories.length > 0){
            for(UsbAccessory usbAccessory: usbAccessories){
                //                    if(usbDevice.getDeviceId() == SERIAL_ACCESSORY){
                openAccessoryDevice(usbAccessory);
//                    }
            }
        }else{
            logger(getContext(),"No hay dispositivos conectados...");
        }
    }

    private void handleUSBHost() {
        HashMap<String, UsbDevice> listDevices = mUsbManager.getDeviceList();
        if(listDevices != null && listDevices.size() > 0){
            if(!isConnect()){
                for(final UsbDevice usbDevice: listDevices.values()){
//                    if(usbDevice.getDeviceId() == SERIAL_ACCESSORY){
                        openHostDevice(usbDevice);
//                    }
                }
            }else{
                logger(getContext(), "Listo para comunicarse...");
            }
        }else{
            setConnect(false);
            logger(getContext(),"No hay dispositivos conectados...");
        }
    }

    private void openHostDevice(UsbDevice usbDevice) {
        if(mUsbManager.hasPermission(usbDevice)){
            logger(getContext(),"Abriendo el dispositivo...");

            if(mUsbHostThread == null){
                mUsbHostThread = new UsbHostThread(getContext(), mUsbManager, this);
                mUsbHostThread.setUsbDevice(usbDevice);
                mUsbHostThread.start();
            } else if(!mUsbHostThread.isAlive()){
                mUsbHostThread.setUsbDevice(usbDevice);
                mUsbHostThread.start();
            } else{
                logger(getContext(), "Ya hay una conexión activa");
            }
        }else{
            logger(getContext(),"No tienes permisos para conectar...");
            mUsbManager.requestPermission(usbDevice, mPermissionIntent);
        }
    }

    private void openAccessoryDevice(UsbAccessory usbAccessory){
        if(mUsbManager.hasPermission(usbAccessory)){
            logger(getContext(),"Abriendo el dispositivo...");
            if(mThreadAccessory == null){
                mThreadAccessory = new UsbAccesoryThread(getContext(), mUsbManager);
                mThreadAccessory.setUsbAccessory(usbAccessory);
                mThreadAccessory.start();
            } else if(!mThreadAccessory.isAlive()){
                mThreadAccessory.setUsbAccessory(usbAccessory);
                mThreadAccessory.start();
            } else{
                logger(getContext(), "Ya hay una conexión activa");
            }
        }else{
            logger(getContext(),"No tienes permisos para conectar...");
            mUsbManager.requestPermission(usbAccessory, mPermissionIntent);
        }
    }
    @Override
    public void retryConnection() {

    }
    @Override
    public void abortConnection() {
        switch (mType){
            case USB_HOST:
                clearHost();
                break;
            case USB_ACCESSORY:

                break;
        }

    }

    @Override
    public void sendMessage(AndroidDevice device, String message) {

    }

    @Override
    public void sendBytes(AndroidDevice device, byte[] bytes) {

    }

    @Override
    public void onConnected() {
        logger(getContext(), "Conectado");
        setConnect(true);
//        startPinning();
    }

    @Override
    public void onDisconnected() {
        setConnect(false);
        clearHost();
    }

    @Override
    public void onErrorConnected() {
        switch (mType){
            case USB_ACCESSORY:
                break;
            case USB_HOST:
                clearHost();
                break;
        }
    }

    @Override
    public void onSuccessHost(boolean granted, UsbDevice usbDevice) {
        openHostDevice(usbDevice);
    }

    @Override
    public void onFailedHost(String message) {

    }

    @Override
    public void onSuccessAccessory(boolean granted, UsbAccessory usbAccessory) {
        openAccessoryDevice(usbAccessory);
    }

    @Override
    public void onFailedAccessory(String message) {

    }

    @Override
    public void onAttached(UsbDevice usbDevice) {
        switch (mType){
            case USB_HOST:
                openHostDevice(usbDevice);
                break;
            case USB_ACCESSORY:
                break;
        }
    }

    @Override
    public void onDettached(UsbDevice usbDevice) {
        switch (mType){
            case USB_ACCESSORY:

                break;
            case USB_HOST:
                clearHost();
                break;
        }
    }

    private void clearHost() {
        if(mUsbHostThread != null){
            mUsbHostThread.interrupt();
        }
        mUsbHostThread = null;
        setConnect(false);
    }
}
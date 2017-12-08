package com.zobot.androidtoandroid.usb;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zobot.androidtoandroid.Constants.BUFFER_SIZE_IN_BYTES;
import static com.zobot.androidtoandroid.Constants.USB_TIMEOUT_IN_MS;
import static com.zobot.androidtoandroid.Logger.logger;

/**
 * Created by jvazquez on 07/12/2017.
 */

public class UsbHostThread extends Thread{

    private Context mContext = null;

    private UsbDevice mUsbDevice = null;
    private UsbManager mUsbManager = null;
    private UsbEndpoint endpointIn;
    private UsbEndpoint endpointOut;
    private final AtomicBoolean keepThreadAlive = new AtomicBoolean(true);
    private final List<String> sendBuffer = new ArrayList<>();
    private HostStatusListener mHostStatusListener = null;

    public UsbHostThread(Context context, UsbManager mUsbManager, HostStatusListener hostStatusListener) {
        this.mUsbManager = mUsbManager;
        this.mContext = context;
        this.mHostStatusListener = hostStatusListener;
    }
    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }

    public void setUsbDevice(UsbDevice mUsbDevice) {
        this.mUsbDevice = mUsbDevice;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void run() {
        connectAndSend(mUsbDevice);
    }


    private void connectAndSend(UsbDevice device) {
        logger(getContext(), "Iniciando la interfaz...");
        if(device == null){
            logger(getContext(), "El dispositivo es nulo...");
            return;
        }
        final int TIMEOUT = 1000;

        UsbInterface intf = device.getInterface(1);

        final UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if(connection == null){
            if(mHostStatusListener != null){
                mHostStatusListener.onErrorConnected();
            }
            return;
        }
        final boolean claimResult = connection.claimInterface(intf, true);
        initStringControlTransfer(connection, 0, "pagatodo", TIMEOUT); // MANUFACTURER
        initStringControlTransfer(connection, 1, "usb2musb", TIMEOUT); // MODEL
        initStringControlTransfer(connection, 2, "USB communication", TIMEOUT); // DESCRIPTION
        initStringControlTransfer(connection, 3, "0.23", TIMEOUT); // VERSION
        initStringControlTransfer(connection, 4, "http://pagatodo.com", TIMEOUT); // URI
        initStringControlTransfer(connection, 5, "42", TIMEOUT); // SERIAL
        connection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, TIMEOUT);


        UsbInterface usbInterface = device.getInterface(0);

        for( int i = 0 ; i < usbInterface.getEndpointCount(); i++){
            final UsbEndpoint ep = device.getInterface(0).getEndpoint(i);


            if( ep.getDirection() == UsbConstants.USB_DIR_IN ) {
                endpointIn = ep;
            } else if ( ep.getDirection() == UsbConstants.USB_DIR_OUT ){
                endpointOut = ep;
            }
        }

        if (endpointIn == null) {
            return;
        }

        if (endpointOut == null) {
            return;
        }
        if(mHostStatusListener != null){
            mHostStatusListener.onConnected();
        }
        if( !claimResult ){
            logger(getContext(), "No se pudo reclamar la interfaz");
            connection.close();
        }else{
            logger(getContext(), "Listo para comunicarse...");
            final byte buff[] = new byte[BUFFER_SIZE_IN_BYTES];
            while (keepThreadAlive.get()) {

                final int bytesTransferred = connection.bulkTransfer(endpointIn, buff, buff.length, USB_TIMEOUT_IN_MS);
                if (bytesTransferred > 0) {
                    logger(getContext(),"device> "+new String(buff, 0, bytesTransferred));
                }

                synchronized (sendBuffer) {
                    if (sendBuffer.size() > 0 ) {
                        final byte[] sendBuff = sendBuffer.get(0).toString().getBytes();
                        connection.bulkTransfer(endpointOut, sendBuff, sendBuff.length, USB_TIMEOUT_IN_MS);
                        logger(getContext(), "Enviado... " + new String(sendBuff));
                        sendBuffer.remove(0);
                    }
                }
            }
        }
        connection.releaseInterface(intf);
        connection.close();
        if(mHostStatusListener != null){
            mHostStatusListener.onDisconnected();
        }

    }
    private void initStringControlTransfer(final UsbDeviceConnection deviceConnection,
                                           final int index,
                                           final String string, final int TIMEOUT) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), TIMEOUT);
    }

    public interface HostStatusListener{
        void onConnected();
        void onDisconnected();
        void onErrorConnected();
    }
}

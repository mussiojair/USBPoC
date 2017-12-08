package com.zobot.androidtoandroid.usb;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.zobot.androidtoandroid.Constants.BUFFER_SIZE_IN_BYTES;
import static com.zobot.androidtoandroid.Logger.logger;

/**
 * Created by jvazquez on 07/12/2017.
 */

public class UsbAccesoryThread extends Thread {

    private UsbAccessory mUsbAccessory = null;
    private ParcelFileDescriptor fileDescriptor = null;
    private UsbManager mUsbManager = null;
    private FileInputStream inStream = null;
    private FileOutputStream outStream = null;
    private Handler sendHandler = null;
    private Context mContext = null;


    public UsbAccesoryThread(Context context, UsbManager mUsbManager) {
        this.mContext = context;
        this.mUsbManager = mUsbManager;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public UsbManager getUsbManager() {
        return mUsbManager;
    }

    public void setUsbManager(UsbManager mUsbManager) {
        this.mUsbManager = mUsbManager;
    }

    public UsbAccessory getUsbAccessory() {
        return mUsbAccessory;
    }

    public void setUsbAccessory(UsbAccessory mUsbAccessory) {
        this.mUsbAccessory = mUsbAccessory;
    }

    @Override
    public void run() {
        openAccessory(getUsbAccessory());
    }

    private void openAccessory(UsbAccessory accessory) {
        fileDescriptor = mUsbManager.openAccessory(accessory);

        if(fileDescriptor != null){
            FileDescriptor fd = fileDescriptor.getFileDescriptor();

            inStream = new FileInputStream(fd);
            outStream = new FileOutputStream(fd);

//            Toast.makeText(this, "USB OK: conectado", Toast.LENGTH_SHORT).show();

//            new CommunicationThread().start();
//
//            sendHandler = new Handler(){
//                public void handleMessage(Message msg){
//                    try {
//
//                        outStream.write((byte[]) msg.obj);
//                        logger(getContext(), "Enviando mensaje: " + new String((byte[])msg.obj));
//
//                    }catch(Exception e){
//                        logger(getContext(), "USB Send Error: " + e.getMessage());
//                    }
//                }
//            };
        }else{
            logger(getContext(), "Error al conectar por Usb");
        }
    }

    private class CommunicationThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            while (running) {
                byte[] msg = new byte[BUFFER_SIZE_IN_BYTES];
                try {
                    //Handle incoming messages
                    int len = inStream.read(msg);
                    while (inStream != null && len > 0 && true) {
                        receive(msg, len);
                        Thread.sleep(10);
                        len = inStream.read(msg);
                    }
                } catch (final Exception e) {
                    running = false;
                    logger(getContext(), "USB receive Failed " + e.toString());
                    closeAccessory();
                    interrupt();
                }
            }
//            logger(getContext(), "USB communication closes");
        }

        public void closeAccessory() {
            try {
                if (fileDescriptor != null) {
                    fileDescriptor.close();
                }
            } catch (IOException e) {

            } finally {
                fileDescriptor = null;
            }
            // onDisconnected();
        }
    }
    private void receive(byte[] payload, final int length){
        logger(getContext(), new String(payload));
    }
}

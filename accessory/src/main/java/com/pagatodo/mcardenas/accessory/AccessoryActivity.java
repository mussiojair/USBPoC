package com.pagatodo.mcardenas.accessory;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zobot.androidtoandroid.CommunicationConfig;
import com.zobot.androidtoandroid.CommunicationType;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import static com.pagatodo.mcardenas.accessory.UsbObservable.ACTION_USB_PERMISSION;

public class AccessoryActivity extends AppCompatActivity {

    private static final String TAG = "AccessoryDeb";
    public static final int USB_TIMEOUT_IN_MS = 100;
    public static final int BUFFER_SIZE_IN_BYTES = 256;

    private UsbObservable usbObservable;
    private UsbManager mUsbManager;
    private ParcelFileDescriptor fileDescriptor;
    private FileInputStream inStream;
    private FileOutputStream outStream;
    private PendingIntent mPermissionIntent;
    private Handler sendHandler;
    private boolean running;

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CommunicationConfig.init(this, CommunicationType.USB_ACCESSORY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessory);

        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send(new Date().toString().getBytes());
            }
        });
        findViewById(R.id.textMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommunicationConfig.openCommunication();
            }
        });
//        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();
//
//        if(accessoryList == null || accessoryList.length == 0){
//            Log.d(TAG, "No hay dispositivos conectados");
//        }else{
//
//            if (mUsbManager.hasPermission(accessoryList[0])) {
//                openAccessory(accessoryList[0]);
//            } else {
//                onDebug("No tienes permiso para usar usb");
//                /*
//                synchronized (mUsbReceiver) {
//                    if (!mPermissionRequestPending) {
//                        mUsbManager.requestPermission(accessory,
//                                mPermissionIntent);
//                        mPermissionRequestPending = true;
//                    }
//                }*/
//            }
//        }
    }

    private void openAccessory(UsbAccessory accessory) {
        fileDescriptor = mUsbManager.openAccessory(accessory);

        if(fileDescriptor != null){
            FileDescriptor fd = fileDescriptor.getFileDescriptor();

            inStream = new FileInputStream(fd);
            outStream = new FileOutputStream(fd);

            Toast.makeText(this, "USB OK: conectado", Toast.LENGTH_SHORT).show();

            new CommunicationThread().start();

            sendHandler = new Handler(){
                public void handleMessage(Message msg){
                    try {

                        outStream.write((byte[]) msg.obj);
                        onDebug("Enviando mensaje: " + new String((byte[])msg.obj));

                    }catch(Exception e){
                        Log.d(TAG, "USB Send Error: " + e.getMessage());
                        onDebug("USB Send Error: " + e.getMessage());
                    }
                }
            };
        }else{
            Toast.makeText(this, "USB Error: no se pudo conectar", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "USB Error: no se pudo conectar");
        }
    }

    private void send(byte[] payload){
        if(sendHandler != null){
            Message msg = sendHandler.obtainMessage();
            msg.obj = payload;
            sendHandler.sendMessage(msg);
        }else{
            onDebug("Handler es null");
        }
    }

    private void receive(byte[] payload, final int length){
        Toast.makeText(AccessoryActivity.this, new String(payload), Toast.LENGTH_SHORT);
    }

    private class CommunicationThread extends Thread {
        @Override
        public void run() {
            running = true;

            while (running) {
                byte[] msg = new byte[BUFFER_SIZE_IN_BYTES];
                try {
                    //Handle incoming messages
                    int len = inStream.read(msg);
                    while (inStream != null && len > 0 && running) {
                        receive(msg, len);
                        Thread.sleep(10);
                        len = inStream.read(msg);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "USB receive Failed " + e.toString() + "\n");
                    closeAccessory();
                }
            }
            Log.d(TAG, "USB communication closes");
        }

        public void closeAccessory() {
            running = false;
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


    private void onDebug(final String msg){

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AccessoryActivity.this, msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);
            }
        });
    }

}

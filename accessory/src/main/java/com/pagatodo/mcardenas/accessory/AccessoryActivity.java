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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import static com.pagatodo.mcardenas.accessory.UsbObservable.ACTION_USB_PERMISSION;

public class AccessoryActivity extends AppCompatActivity implements Observer {

    private final static String TAG = "AccessoryAct";

    private UsbObservable usbObservable;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessory);

        usbObservable = new UsbObservable();
        usbObservable.addObserver(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(usbObservable.getUsbReceiver(), filter);
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(usbObservable.getUsbReceiver());

        super.onDestroy();
    }

    @Override
    public void update(Observable observable, Object o) {
        UsbObservable.UsbState usbState = (UsbObservable.UsbState)o;


        if( usbState.state.equals(UsbObservable.USB_STATE_ATTACHED)){

            // Solicita permiso para utilizar el dispositivo conectado via usb
            Log.d(TAG, UsbObservable.USB_STATE_ATTACHED );
            mUsbManager.requestPermission( usbState.device , mPermissionIntent);

        }else if(usbState.state.equals(UsbObservable.USB_STATE_DETACHED)) {

            Log.d(TAG, UsbObservable.USB_STATE_DETACHED );

        }else if(usbState.state.equals(UsbObservable.USB_STATE_GRANTED)) {

            Log.d(TAG, UsbObservable.USB_STATE_GRANTED );
            connectAndSend(usbState.device);

        }else if(usbState.state.equals(UsbObservable.USB_STATE_DENIED)) {

            Log.d(TAG, UsbObservable.USB_STATE_DENIED );

        }
    }


    private void connectAndSend(UsbAccessory device) {

    }
}

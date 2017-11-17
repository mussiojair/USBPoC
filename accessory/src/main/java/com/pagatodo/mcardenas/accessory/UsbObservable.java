package com.pagatodo.mcardenas.accessory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.Observable;

/**
 * Created by mcardenas on 17/11/2017.
 */

public class UsbObservable extends Observable {

    // Debug Tag
    private final static String TAG = "UsbObservable";

    // Action of interest
    public final static String ACTION_USB_PERMISSION = "com.pagatodo.ACTION_USB_PERMISSION";
    public final static String USB_STATE_ATTACHED = "com.pagatodo.ACTION_USB_ATTACHED";
    public final static String USB_STATE_DETACHED = "com.pagatodo.ACTION_USB_DETACHED";
    public final static String USB_STATE_GRANTED = "com.pagatodo.ACTION_USB_GRANTED";
    public final static String USB_STATE_DENIED = "com.pagatodo.ACTION_USB_DENIED";

    /* Detecta la conexión y desconexión de un dispositivo a través del puerto USB */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(accessory != null){
                            //call method to set up accessory communication
                            UsbState usbState = new UsbState();
                            usbState.device = accessory;
                            usbState.state = USB_STATE_GRANTED;
                            setChanged();
                            notifyObservers(usbState);
                        }
                    }
                    else {
                        UsbState usbState = new UsbState();
                        usbState.device = accessory;
                        usbState.state = USB_STATE_DENIED;
                        setChanged();
                        notifyObservers(usbState);
                    }
                }
            }
        }
    };

    public BroadcastReceiver getUsbReceiver(){
        return mUsbReceiver;
    }



    public class UsbState{

        public String state;
        public UsbAccessory device;

    }
}


package com.pagatodo.mcardenas.usbpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (ACTION_USB_PERMISSION.equals(action)) {
                // Permission requested

                synchronized (this) {

                    UsbState usbState = new UsbState();
                    usbState.device = usbDevice;

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // User has granted permission
                        usbState.state = USB_STATE_GRANTED;
                    } else {
                        // User has denied permission
                        usbState.state = USB_STATE_DENIED;
                    }
                    setChanged();
                    notifyObservers( usbState );
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                // Device removed
                Log.d(TAG, "USB DETACHED");
                synchronized (this) {
                    // ... Check to see if usbDevice is yours and cleanup ...
                    UsbState usbState = new UsbState();
                    usbState.device = usbDevice;
                    usbState.state = USB_STATE_DETACHED;

                    setChanged();
                    notifyObservers( usbState );
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // Device attached
                // Log.d(TAG, "USB ATTACHED");

                synchronized (this) {
                    // Qualify the new device to suit your needs and request permission
                    // if ((usbDevice.getVendorId() == VID) && (usbDevice.getProductId() == MY_PID)) {
                    // mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                    //}
                    UsbState usbState = new UsbState();
                    usbState.device = usbDevice;
                    usbState.state = USB_STATE_ATTACHED;


                    setChanged();
                    notifyObservers( usbState );
                }
            }

        }
    };

    public BroadcastReceiver getUsbReceiver(){
        return mUsbReceiver;
    }



    public class UsbState{

        public String state;
        public UsbDevice device;

    }
}

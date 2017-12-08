package com.zobot.androidtoandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * Created by jvazquez on 07/12/2017.
 */

public class UsbHandler {
    // Action of interest
    public final static String ACTION_USB_PERMISSION_HOST       = "com.pagatodo.ACTION_USB_PERMISSION_HOST";
    public final static String ACTION_USB_PERMISSION_ACCESSORY  = "com.pagatodo.ACTION_USB_PERMISSION_ACCESSORY";

    private UsbPermissionListener mUsbPermissionListener = null;

    public UsbHandler(UsbPermissionListener mUsbPermissionListener) {
        this.mUsbPermissionListener = mUsbPermissionListener;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case ACTION_USB_PERMISSION_HOST:
                    UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    synchronized (this) {
                        if(mUsbPermissionListener != null){
                            mUsbPermissionListener.onSuccessHost(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false), usbDevice);
                        }
                    }
                    break;
                case ACTION_USB_PERMISSION_ACCESSORY:
                    UsbAccessory usbAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    synchronized (this) {
                        if(mUsbPermissionListener != null){
                            mUsbPermissionListener.onSuccessAccessory(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false), usbAccessory);
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    UsbDevice usbDevices = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    synchronized (this){
                        if(mUsbPermissionListener != null){
                            mUsbPermissionListener.onAttached(usbDevices);
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    UsbDevice usbDevicess = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    synchronized (this){
                        if(mUsbPermissionListener != null){
                            mUsbPermissionListener.onDettached(usbDevicess);
                        }
                    }
                    break;
            }
        }
    };

    public BroadcastReceiver getUsbReceiver() {
        return mUsbReceiver;
    }

    public interface UsbPermissionListener{
        void onSuccessHost(boolean granted, UsbDevice usbDevice);
        void onFailedHost(String message);
        void onSuccessAccessory(boolean granted, UsbAccessory usbAccessory);
        void onFailedAccessory(String message);
        void onAttached(UsbDevice usbDevice);
        void onDettached(UsbDevice usbDevice);
    }
}

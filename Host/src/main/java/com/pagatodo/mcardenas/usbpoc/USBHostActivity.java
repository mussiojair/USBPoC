package com.pagatodo.mcardenas.usbpoc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Parcel;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

public class USBHostActivity extends AppCompatActivity implements Observer {

    private final static String TAG = "USBHostTAG";

    private final String ACTION_USB_PERMISSION = "com.pagatodo.ACTION_USB_PERMISSION";
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private UsbObservable usbObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbhost);


        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this,0, new Intent(UsbObservable.ACTION_USB_PERMISSION), 0 );
        usbObservable = new UsbObservable();
        usbObservable.addObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registra receiver para detectar conexion y desconexion vía puerto usb
        IntentFilter filter = new IntentFilter(UsbObservable.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
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
        Log.d(TAG, usbState.device.getProductName() + " - " +
                usbState.device.getVendorId() + " - " +
                usbState.device.getManufacturerName() + " - " +
                usbState.device.getSerialNumber()
        );

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

    private void connectAndSend(UsbDevice device) {
        final byte[] bytes = new byte[]{0x2, 0xd,0x0, 0x0};
        final int TIMEOUT = 1000;
        final boolean forceClaim = true;

        UsbInterface intf = device.getInterface(1);
        Log.d(TAG, "InterfaceCount = " + device.getInterfaceCount());
        final UsbEndpoint endpoint = intf.getEndpoint(0);
        final UsbDeviceConnection connection = mUsbManager.openDevice(device);
        // connection.claimInterface(intf, forceClaim);
        initStringControlTransfer(connection, 0, "quandoo", TIMEOUT); // MANUFACTURER
        initStringControlTransfer(connection, 1, "Android2AndroidAccessory", TIMEOUT); // MODEL
        initStringControlTransfer(connection, 2, "showcasing android2android USB communication", TIMEOUT); // DESCRIPTION
        initStringControlTransfer(connection, 3, "0.1", TIMEOUT); // VERSION
        initStringControlTransfer(connection, 4, "http://quandoo.de", TIMEOUT); // URI
        initStringControlTransfer(connection, 5, "42", TIMEOUT); // SERIAL

        connection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, TIMEOUT);

        connection.close();

    }

    private void initStringControlTransfer(final UsbDeviceConnection deviceConnection,
                                           final int index,
                                           final String string, final int TIMEOUT) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), TIMEOUT);
    }
}
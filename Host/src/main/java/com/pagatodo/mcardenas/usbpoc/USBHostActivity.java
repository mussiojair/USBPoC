package com.pagatodo.mcardenas.usbpoc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.hardware.usb.UsbConstants.USB_DIR_IN;

public class USBHostActivity extends AppCompatActivity implements Observer {

    private final static String TAG = "USBHostTAG";
    private final static int BUFFER_SIZE_IN_BYTES = 256;
    private final static int USB_TIMEOUT_IN_MS = 100;

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private UsbObservable usbObservable;

    private UsbEndpoint endpointIn;
    private UsbEndpoint endpointOut;

    private final AtomicBoolean keepThreadAlive = new AtomicBoolean(true);
    private final List<String> sendBuffer = new ArrayList<>();

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
        final UsbObservable.UsbState usbState = (UsbObservable.UsbState)o;
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectAndSend(usbState.device);
                }
            }).start();


        }else if(usbState.state.equals(UsbObservable.USB_STATE_DENIED)) {

            Log.d(TAG, UsbObservable.USB_STATE_DENIED );

        }
    }

    private void connectAndSend(UsbDevice device) {
        final int TIMEOUT = 1000;

        UsbInterface intf = device.getInterface(1);

        Log.d(TAG, "InterfaceCount = " + device.getInterfaceCount());

        final UsbDeviceConnection connection = mUsbManager.openDevice(device);
        final boolean claimResult = connection.claimInterface(intf, true);

        if (connection == null) {
            onDebug("No se pudo establecer la conexión usb");
            return;
        }
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
            onDebug("Input Endpoint not found");
            return;
        }

        if (endpointOut == null) {
            onDebug("Output Endpoint not found");
            return;
        }

        if( !claimResult ){
            onDebug("No se pudo reclamar la interfaz");
            connection.close();
        }else{

            final byte buff[] = new byte[BUFFER_SIZE_IN_BYTES];
            onDebug("Claimed interface - ready to communicate");

            while (keepThreadAlive.get()) {

                final int bytesTransferred = connection.bulkTransfer(endpointIn, buff, buff.length, USB_TIMEOUT_IN_MS);
                if (bytesTransferred > 0) {
                    onDebug("device> "+new String(buff, 0, bytesTransferred));
                }

                synchronized (sendBuffer) {
                    if ( sendBuffer.size() > 0 ) {
                        final byte[] sendBuff = sendBuffer.get(0).toString().getBytes();
                        connection.bulkTransfer(endpointOut, sendBuff, sendBuff.length, USB_TIMEOUT_IN_MS);
                        onDebug("Enviado... " + new String(sendBuff));
                        sendBuffer.remove(0);
                    }
                }
            }
        }

        connection.releaseInterface(intf);
        connection.close();

    }

    private void initStringControlTransfer(final UsbDeviceConnection deviceConnection,
                                           final int index,
                                           final String string, final int TIMEOUT) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), TIMEOUT);
    }

    private void onDebug(final String msg){

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(USBHostActivity.this, msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);
            }
        });
    }
}
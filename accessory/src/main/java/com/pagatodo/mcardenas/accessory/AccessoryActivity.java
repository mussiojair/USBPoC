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
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import static com.pagatodo.mcardenas.accessory.UsbObservable.ACTION_USB_PERMISSION;

public class AccessoryActivity extends AppCompatActivity {

    private final static String TAG = "AccessoryAct";

    private UsbObservable usbObservable;
    private UsbManager mUsbManager;
    private ParcelFileDescriptor fileDescriptor;
    private FileInputStream inStream;
    private FileOutputStream outStream;
    private PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessory);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();

        if(accessoryList == null || accessoryList.length == 0){
            Log.d(TAG, "No hay dispositivos conectados");
        }else{
            openAccessory(accessoryList[0]);
        }

    }

    private void openAccessory(UsbAccessory accessory) {

        fileDescriptor = mUsbManager.openAccessory(accessory);

        if(fileDescriptor != null){
            FileDescriptor fd = fileDescriptor.getFileDescriptor();
            inStream = new FileInputStream(fd);
            outStream = new FileOutputStream(fd);
            Toast.makeText(this, "USB OK: conectado", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "USB Error: no se pudo conectar", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "USB Error: no se pudo conectar");
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

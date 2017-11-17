package com.pagatodo.mcardenas.usbpoc;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class USBHostActivity extends AppCompatActivity {

    private final static String TAG = "USBHostTAG";

    private final static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        private final static String TAG = "BroadcastReceiverTAG";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Toast.makeText( USBHostActivity.this, "Intent received" , Toast.LENGTH_SHORT);



            if( ACTION_USB_PERMISSION.equals(action) ){
                synchronized (this){
                    usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        if(usbDevice != null ){
                            Log.d(TAG, "permission on USB device granted...");
                        }
                    }else{
                        Log.d(TAG, "permission denied for device");
                    }
                }
            }
        }
    };

    private UsbDevice usbDevice;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbhost);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        filter = new IntentFilter(ACTION_USB_PERMISSION);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mUsbReceiver, filter);
        mUsbManager.requestPermission(usbDevice, mPermissionIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.w(TAG, "new Intent: " + intent.getAction());
        Toast.makeText(this, "new Intent: " + intent.getAction(), Toast.LENGTH_SHORT);
        usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    }
}
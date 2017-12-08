package com.zobot.androidtoandroid;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jvazquez on 07/12/2017.
 */

public class Logger {
    public static void logger(final Context context, final String message){
        try{
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e){

        }
        Log.d("AndroidToAndroid", message);
    }
}

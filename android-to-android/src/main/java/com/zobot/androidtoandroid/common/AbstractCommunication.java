package com.zobot.androidtoandroid.common;

import android.content.Context;

import com.zobot.androidtoandroid.CommunicationConfig;
import com.zobot.androidtoandroid.model.AndroidDevice;
import com.zobot.androidtoandroid.CommunicationType;
import com.zobot.androidtoandroid.OnConnectListener;
import com.zobot.androidtoandroid.PinningRunnable;

/**
 * Created by jvazquez on 06/12/2017.
 */

public abstract class AbstractCommunication {

    protected Context mContext = null;
    protected OnConnectListener mOnConnectListener = null;
    protected Thread mPinningThread = null;
    protected PinningRunnable mPinningRunnable = null;
    protected boolean mConnect = false;
    protected CommunicationType mType = null;


    public AbstractCommunication(Context mContext, OnConnectListener onConnectListener) {
        this.mContext = mContext;
        this.mOnConnectListener = onConnectListener;
        mPinningRunnable = new PinningRunnable(onConnectListener);
        mPinningThread = new Thread(mPinningRunnable);
        this.mType = CommunicationConfig.getType();
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public OnConnectListener getOnConnectListener() {
        return mOnConnectListener;
    }

    public void setOnConnectListener(OnConnectListener mOnConnectListener) {
        this.mOnConnectListener = mOnConnectListener;
    }


    protected void startPinning() {
        if(mPinningThread.isAlive()){
            mPinningRunnable.setPinning(mConnect);
        }else{
            mPinningThread.start();
        }
    }
    protected void stopPinning(){
        mConnect = false;
    }
    public abstract void tryConnection();
    public abstract void retryConnection();
    public abstract void abortConnection();
    public abstract void sendMessage(AndroidDevice device, String message);
    public abstract void sendBytes(AndroidDevice device, byte[] bytes);

    public boolean isConnect() {
        return mConnect;
    }

    public void setConnect(boolean mConnect) {
        this.mConnect = mConnect;
    }
}

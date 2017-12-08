package com.zobot.androidtoandroid;

/**
 * Created by jvazquez on 06/12/2017.
 */

public class PinningRunnable implements Runnable {

    private OnConnectListener mOnConnectListener = null;
    private boolean mPinning = true;

    public PinningRunnable(OnConnectListener mOnConnectListener) {
        this.mOnConnectListener = mOnConnectListener;
    }

    public void run() {
        while (true) {
            if(mOnConnectListener != null) {
                if (mPinning) {

                } else {

                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public boolean isPinning() {
        return mPinning;
    }

    public void setPinning(boolean pinning) {
        this.mPinning = pinning;
    }
}
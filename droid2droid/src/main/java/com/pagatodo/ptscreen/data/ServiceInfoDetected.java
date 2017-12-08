package com.pagatodo.ptscreen.data;

import android.content.pm.ServiceInfo;
import android.net.nsd.NsdServiceInfo;

/**
 * Created by mcardenas on 03/07/2017.
 */

public class ServiceInfoDetected {

    private NsdServiceInfo serviceInfo;
    private boolean fail;


    public NsdServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(NsdServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}

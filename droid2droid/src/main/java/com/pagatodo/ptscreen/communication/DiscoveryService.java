package com.pagatodo.ptscreen.communication;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.pagatodo.ptscreen.data.Endpoint;
import com.pagatodo.ptscreen.data.ServiceInfoDetected;
import com.pagatodo.ptscreen.utils.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Created by mcardenas on 19/06/2017.
 */

public class DiscoveryService implements Serializable {

    private static final String TAG  = "DiscoveryService";
    private NsdServiceInfo serviceInfo;
    private String mServiceName;
    private String mServiceType;
    private int mLocalPort;
    private Context mContext;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager mNsdManager;
    private String mDeviceId;

    private HashMap<String, ServiceInfoDetected> mDetectedServices;



    public DiscoveryService(Context context, String serviceName, String serviceType, String deviceId){
        mContext = context;
        mServiceName = serviceName;
        mServiceType = serviceType;
        mDeviceId = deviceId;
        mDetectedServices = new HashMap<>();

        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
    }

    public void setLocalPort(int port){
        mLocalPort = port;
    }

    public int getLocalPort(){
        return mLocalPort;
    }

    public void startDiscovering(){

        initializeDiscoveryListener();
        if(mNsdManager != null)
            mNsdManager.discoverServices(mServiceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void stopDiscovering(){
        if(mDiscoveryListener != null)
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        mDiscoveryListener = null;
    }

    public void startAdvertising(){

        initializeRegistrationListener();

        Log.d(TAG, mDeviceId + " : " + mServiceName + " : " + mServiceType + " : " + mLocalPort);
        serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName( mServiceName);
        serviceInfo.setServiceType( mServiceType );
        serviceInfo.setPort( mLocalPort );

        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void stopAdvertising(){
        if(mRegistrationListener != null)
            mNsdManager.unregisterService(mRegistrationListener);
        mRegistrationListener = null;
    }

    public void regenerateLocalPort(){
        initializeServerSocket();
    }

    /* internal operations */
    private void initializeServerSocket() {
        // Initialize a server socket on the next available port.
        try {
            ServerSocket mServerSocket = new ServerSocket(0);
            // Store the chosen port.
            mLocalPort = mServerSocket.getLocalPort();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void initializeDiscoveryListener(){

        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStartDiscoveryFailed");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStopDiscoveryFailed");
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Service discovery stopped");
            }

            @Override
            public void onServiceFound(final NsdServiceInfo service) {

                Log.d(TAG, "Service discovery success " + service);

                ServiceInfoDetected sid = mDetectedServices.get(service.getServiceName());
                if(sid == null ) {
                    ServiceInfoDetected sif = new ServiceInfoDetected();
                    sif.setServiceInfo(service);
                    sif.setFail(false);
                    mDetectedServices.put(service.getServiceName(), sif);
                }else if(sid.isFail()){
                    return ;
                }

                if (!service.getServiceType().equals(mServiceType)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains(mServiceName)){
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {

                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            // Called when the resolve fails.  Use the error code to debug.
                            Log.e(TAG, "Resolve failed " + errorCode);

                            ServiceInfoDetected sid = mDetectedServices.get( serviceInfo.getServiceName() );
                            sid.setFail( true );
                            mDetectedServices.put( serviceInfo.getServiceName(), sid );


                            if(( errorCode == NsdManager.FAILURE_ALREADY_ACTIVE)){
                                Log.d(TAG, serviceInfo.getServiceName());
                            }
                        }

                        @Override
                        public void onServiceResolved(final NsdServiceInfo serviceInfo) {

                            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

                            Endpoint endpoint = new Endpoint(Utils.generateDeviceid(serviceInfo.getHost().getHostAddress()), serviceInfo.getServiceName());
                            endpoint.setServiceType(serviceInfo.getServiceType());
                            endpoint.setHostname(serviceInfo.getHost().getHostAddress());
                            endpoint.setPort(serviceInfo.getPort());

                            mServiceDiscoveryListener.serviceDetected(endpoint);

                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "service lost" + serviceInfo);
            }
        };
    }

    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                Log.d(TAG, "onServiceRegistered");
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.d(TAG, "onRegistrationFailed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d(TAG, "onServiceUnregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                Log.d(TAG, "onUnregistrationFailed");
            }
        };
    }

    /* Interface definition: ServiceDiscoveryListener */

    private ServiceDiscoveryListener mServiceDiscoveryListener;

    public void setServiceDiscoveryListener( ServiceDiscoveryListener serviceDiscoveryListener){
        this.mServiceDiscoveryListener = serviceDiscoveryListener;
    }

    public interface ServiceDiscoveryListener {
        void serviceDetected(Endpoint endpoint);
    }

}

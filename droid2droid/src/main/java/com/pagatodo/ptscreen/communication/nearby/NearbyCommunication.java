package com.pagatodo.ptscreen.communication.nearby;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.pagatodo.ptscreen.communication.GeneralCommunication;
import com.pagatodo.ptscreen.data.Endpoint;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by mcardenas on 19/06/2017.
 */

public class NearbyCommunication extends GeneralCommunication implements
        Serializable,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
        {

    private static final String TAG = "NearbyComm";
    private static int[] NETWORK_TYPES = {ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_ETHERNET};
    private GoogleApiClient mGoogleApiClient;
    private EndpointDiscoveryCallback mEndpointDiscoveryCallback;
    private ConnectionLifecycleCallback mConnectionLifecycleCallback;
    private PayloadCallback mPayloadCallback;
    private Context mContext;
    private boolean isAdvertiser;



    public NearbyCommunication( Context context, String service_id ){

        mContext = context;
        mServiceId = service_id;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d(TAG, "onEndpointFound " + endpointId + ", " + discoveredEndpointInfo.getEndpointName() + ", " + discoveredEndpointInfo.getServiceId());
                mPeerListener.peerStatusOnFound("onEndpointFound", PeerListener.PEER_FOUND, new Endpoint( endpointId, discoveredEndpointInfo.getEndpointName() ));
                // se añade a la lista de dispositivos conectados
                Endpoint ep = new Endpoint(endpointId, discoveredEndpointInfo.getEndpointName());
                mDetectedDevices.put(endpointId, ep);
            }

            @Override
            public void onEndpointLost(String endpointId) {
                Log.d(TAG, "onEndpointLost " + endpointId);
                // mPeerListener.peerStatusChanged("onEndpointLost", PeerListener.PEER_LOST, new Endpoint( endpointId, null ));
                mPeerListener.peerStatusOnLost("onEndpointLost", PeerListener.PEER_LOST, new Endpoint( endpointId, null ));
            }
        };

        mPayloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(String endointId, Payload payload) {
                Log.d(TAG, "onPayloadReceived: " + new String(payload.asBytes()));
                Endpoint ep = mDetectedDevices.get(endointId);
                mMessageListener.messageReceived(ep, new String(payload.asBytes()));
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d(TAG, "onPayloadTransferUpdate");
            }
        };


        mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d(TAG, "onConnectionInitiated para" + endpointId + ", esperando confirmación de la conexión...");
                // Automatically accept the connection on both sides.
                Nearby.Connections.acceptConnection(
                        mGoogleApiClient, endpointId, mPayloadCallback);

                Endpoint ep = mDetectedDevices.get(endpointId);
                if( ep == null) {
                    ep  = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mDetectedDevices.put(ep.getDeviceId(), ep);
                }
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                Log.d(TAG, "onConnectionResult conexión a " + endpointId + "exitosa.");

                mStatusListener.statusChanged("Conexión a " + endpointId + " exitosa.", STATUS_ENDPOINT_CONNECTED);
                Endpoint ep = mDetectedDevices.get(endpointId);
                updateEndpointStatus(ep, true);
                mPeerListener.peerConnected("Peer Conectado", PeerListener.PEER_CONNECTED, ep);
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d(TAG, "onDisconnected "  + endpointId);
                Endpoint ep = mDetectedDevices.get(endpointId);
                updateEndpointStatus(ep, false);
                mPeerListener.peerDisconnected("Peer Desconectado: " + ep.getDeviceName(), PeerListener.PEER_DISCONNECTED, ep);
            }
        };



    }

    @Override
    public void startService() {
        Log.d(TAG, "startNearbyService");
        if( mGoogleApiClient.isConnected() ) {

            if( isAdvertiser ){
                startNearbyAdvertising();
            }else{
                startNearbyDiscovering();
            }

        } else {

            Log.d(TAG, "Start Google Api Cliente");
            mGoogleApiClient.connect();

        }
    }

    @Override
    public void stopService() {
        Log.d(TAG, "stopNearbyService");
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void startAdvertising() {
        isAdvertiser = true;
        startService();
    }

    private void startNearbyAdvertising(){
        Log.d(TAG, "getServiceId(): " + getServiceId());
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getDeviceName(),
                getServiceId(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    // We're advertising!
                                    Log.d(TAG, "startAdvertising success!" );
                                } else {
                                    // We were unable to start advertising.
                                    Log.d(TAG, "startAdvertising fails!" + result.getStatus().getStatusCode() );
                                }
                            }
                        });
    }
    @Override
    public void stopAdvertising() {
        Log.d(TAG, "stopAdvertising");
        if( mGoogleApiClient.isConnected()) {
            Nearby.Connections.stopAdvertising(mGoogleApiClient);
        }
    }

    @Override
    public void connectTo(final Endpoint endpoint) {

        Nearby.Connections.requestConnection(
                mGoogleApiClient,
                getDeviceName(),
                endpoint.getDeviceId(),
                mConnectionLifecycleCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    // We successfully requested a connection. Now both sides
                                    // must accept before the connection is established.
                                    Log.d(TAG, "Solicita conexión a " + endpoint.getDeviceName() + " exitosa.");
                                    mStatusListener.statusChanged("Solicita conexión a " + endpoint.getDeviceName() + " exitosa.", STATUS_ENDPOINT_CONNECTED);

                                } else {
                                    // Nearby Connections failed to request the connection.
                                    Log.d(TAG, "Solicitud de conexión a " + endpoint.getDeviceName() + ", codeerror = " + status.getStatusCode() );
                                    mStatusListener.statusChanged("Solicitud de conexión a " + endpoint.getDeviceName(), STATUS_CONNECTION_FAILED);
                                }
                            }
                        });
    }

    @Override
    public void disconnectDevice(Endpoint endpoint) {
        Log.d(TAG, "disconnectDevice: " + endpoint.getDeviceName());
        if(mGoogleApiClient.isConnected()) {
            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getDeviceId());
        }
        mPeerListener.peerDisconnected("Peer Desconectado: " + endpoint.getDeviceName(), PeerListener.PEER_DISCONNECTED, endpoint);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void startDiscovering() {
        isAdvertiser = false;
        startService();
    }

    @Override
    public void stopDiscovering() {
        Log.d(TAG , "StopDiscovering()...");
        if( mGoogleApiClient.isConnected()) {
            Nearby.Connections.stopDiscovery(mGoogleApiClient);
        }
    }

    @Override
    public void setLocalPort(int port) {

    }

    @Override
    public void configureLocalPort() {

    }

    @Override
    public void sendMessageToAll(String message) {

        Object[] devs = mDetectedDevices.keySet().toArray();
        for(Object d : devs){
            Nearby.Connections.sendPayload(mGoogleApiClient, (String)d, Payload.fromBytes(message.getBytes()));
        }
    }

    @Override
    public void sendMessageToDevice(Endpoint endpoint, String message) {
        Log.d(TAG, "Attempt to send message to " + endpoint.getDeviceId() + ": " + message );
        // Nearby.Connections.sendReliableMessage(mGoogleApiClient, currentEndpointId, cmd.getBytes() );
        Nearby.Connections.sendPayload(mGoogleApiClient, endpoint.getDeviceId(), Payload.fromBytes(message.getBytes()));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        mStatusListener.statusChanged("onConnected", STATUS_CONNECTED);
        if(isAdvertiser) {
            Log.d(TAG, "Start Advertising...");
            startNearbyAdvertising();
        } else {
            Log.d(TAG, "Start Discovering...");
            startNearbyDiscovering();
        }
    }

    private void startNearbyDiscovering(){
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                getServiceId(),
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    // We're discovering!
                                    Log.d(TAG, "onResult Start discovery success!. Searching for service: " + getServiceId());
                                    mStatusListener.statusChanged("onResult Start discovery success!", STATUS_DISCOVERING);
                                } else {
                                    // We were unable to start discovering.
                                    Log.d(TAG, "onResult Start discovery failed! " + status.getStatusCode() );
                                    mStatusListener.statusChanged("onResult start discovery failed!", STATUS_DISCOVERING_FAILED);
                                }
                            }
                        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended " + i);
        mStatusListener.statusChanged("onConnectionSuspended. Error no. " + i, STATUS_DISCONNECTED);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed " + connectionResult.getErrorCode());
        mStatusListener.statusChanged("onConnectionFailed", STATUS_DISCONNECTED);
    }
}

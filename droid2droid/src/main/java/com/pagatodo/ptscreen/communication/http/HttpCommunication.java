package com.pagatodo.ptscreen.communication.http;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.pagatodo.ptscreen.communication.DiscoveryService;
import com.pagatodo.ptscreen.communication.GeneralCommunication;
import com.pagatodo.ptscreen.data.Endpoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by mcardenas on 19/06/2017.
 */

public class HttpCommunication extends GeneralCommunication implements Serializable, DiscoveryService.ServiceDiscoveryListener, WebServer.WebServerRequestListener {

    private static final String TAG = "HttpComm";
    private DiscoveryService mDiscovery;
    private WebServer mWebServer;
    private Context context;
    private String mDeviceId = "";
    private Endpoint thisEndpoint;

    public HttpCommunication(Context context){
        this.context = context;
        setServiceType("_http._tcp.");
        startService();
    }


    @Override
    public void startService() {
        try{
            configureLocalPort();
            Log.d(TAG, "local port: " + mLocalPort);
            mDeviceId  = generateDeviceid();

            Log.d(TAG, "deviceId: " + mDeviceId);
            mWebServer = new WebServer( mDeviceId, mLocalPort );
            mWebServer.setWebServerRequestListener(this);
            mWebServer.start();

            // recupera datos de este endpoint
            configureThisEndpoint();


        } catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void configureThisEndpoint(){
        thisEndpoint = new Endpoint(mDeviceId, getDeviceName() == null ? Build.MODEL : getDeviceName() );
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        int ipAddress = info.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        thisEndpoint.setHostname(ip);
        thisEndpoint.setServiceType(getServiceType());
        thisEndpoint.setPort(mWebServer.getListeningPort());
    }

    @Override
    public void stopService() {
        if(mWebServer != null)
            mWebServer.stop();
    }

    @Override
    public void startAdvertising() {
        Log.d(TAG, "startAdvertising");
        mDiscovery = new DiscoveryService(context, mServiceId , mServiceType, mDeviceId);
        mDiscovery.setLocalPort(mLocalPort);
        mDiscovery.setServiceDiscoveryListener(this);
        mDiscovery.startAdvertising();
    }

    @Override
    public void stopAdvertising() {
        mDiscovery.stopAdvertising();
    }

    @Override
    public void connectTo(final Endpoint endpoint) {

        /*
        * Hay que enviar los datos del discoverer al advertising.
        * Esto permite una comunicación de 2 vías asíncrona.
        */

        HashMap<String, String> params = new HashMap<String, String>();

        params.put("deviceId", thisEndpoint.getDeviceId());
        params.put("deviceName", thisEndpoint.getDeviceName());
        params.put("serviceType", thisEndpoint.getServiceType());
        params.put("hostname", thisEndpoint.getHostname());
        params.put("port", "" + thisEndpoint.getPort());

        JSONObject postparameters = null;

        try {
            postparameters = new JSONObject(thisEndpoint.toJSON());
            Log.d(TAG, postparameters.toString());
        }catch(JSONException jsone){
            jsone.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        Log.d(TAG, "Connecting to " + endpoint.getURLEndpointString() + "registrar/" + " using Volley");
        JsonObjectRequest jsonRequest = new JsonObjectRequest(endpoint.getURLEndpointString() + "registrar/", postparameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                try {
                    if (response.getString("status").toLowerCase().equals("ok")) {
                        endpoint.setConnected(true);
                        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
                        mPeerListener.peerConnected("Peer Conectado", PeerListener.PEER_CONNECTED, endpoint);
                    }
                }catch(JSONException jsone){
                    jsone.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        });
        queue.add(jsonRequest);

    }


    private void sendMessageViaHttp(Endpoint endpoint, final String jsonmessage ){
        JSONObject postparameters = null;

        try {
            postparameters = new JSONObject(jsonmessage);
            Log.d(TAG, postparameters.toString());
        }catch(JSONException jsone){
            jsone.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(endpoint.getURLEndpointString() + "mensaje/", postparameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                try {
                    if (response.getString("status").toLowerCase().equals("ok")) {
                        Log.d(TAG, "mensaje enviado: " + jsonmessage);
                        Log.d(TAG, "respuesta: " + response.toString() );
                    }
                }catch(JSONException jsone){
                    jsone.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error al enviar mensajes: " + error.toString());
            }
        });
        queue.add(jsonRequest);
    }
    @Override
    public void disconnectDevice(Endpoint endpoint) {
        endpoint.setConnected(false);

        HashMap<String, String> params = new HashMap<String, String>();

        params.put("deviceId", thisEndpoint.getDeviceId());
        params.put("deviceName", thisEndpoint.getDeviceName());
        params.put("serviceType", thisEndpoint.getServiceType());
        params.put("hostname", thisEndpoint.getHostname());
        params.put("port", "" + thisEndpoint.getPort());

        JSONObject postparameters = null;

        try {
            postparameters = new JSONObject(thisEndpoint.toJSON());
            Log.d(TAG, postparameters.toString());
        }catch(JSONException jsone){
            jsone.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(endpoint.getURLEndpointString() + "desconectar/", postparameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
                try {
                    if (response.getString("status").toLowerCase().equals("ok")) {
                        Log.d(TAG, "mensaje enviado: " + response);
                        Log.d(TAG, "respuesta: " + response.toString() );
                    }
                }catch(JSONException jsone){
                    jsone.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error al enviar mensajes: " + error.toString());
            }
        });
        queue.add(jsonRequest);



        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void startDiscovering() {
        mDiscovery = new DiscoveryService(context, mServiceId , mServiceType, mDeviceId);
        mDiscovery.setLocalPort(mLocalPort);
        mDiscovery.setServiceDiscoveryListener(this);
        mDiscovery.startDiscovering();
    }

    @Override
    public void stopDiscovering() {
        mDiscovery.stopDiscovering();
    }


    @Override
    public void setLocalPort(int port) {

    }

    @Override
    public void configureLocalPort() {

        // Initialize a server socket on the next available port.
        try {
            ServerSocket mServerSocket = new ServerSocket(0);
            // Store the chosen port.
            mLocalPort = mServerSocket.getLocalPort();
            // libera el puerto con la dirección
            mServerSocket.close();
        } catch (IOException ioe) {

            ioe.printStackTrace();

        }
    }

    @Override
    public void sendMessageToAll(String message) {
        Set<String> keys = mDetectedDevices.keySet();
        for( String k : keys) {
            Endpoint ep = mDetectedDevices.get(k);
            if( ep.isConnected() ){
                sendMessageToDevice(ep, message);
            }
        }
    }

    @Override
    public void sendMessageToDevice(Endpoint ep, String message) {

        try {

            JSONObject msg = new JSONObject();
            msg.put("msg", message);
            msg.put("endpoint", thisEndpoint.toJSON());
            sendMessageViaHttp(ep, msg.toString());

        }catch(JSONException jsone){
            jsone.printStackTrace();
        }
    }

    @Override
    public void setServiceName(String serviceName) {
        mServiceName = serviceName;
    }

    @Override
    public void setServiceType(String serviceType) {
        mServiceType = serviceType;
    }



    @Override
    public void serviceDetected(Endpoint endpoint) {
        Log.d(TAG, "serviceDetected. deviceId: " + endpoint.getDeviceId() + ", deviceName: " + endpoint.getDeviceName());
        Log.d(TAG, "hostname:" + endpoint.getHostname());
        Log.d(TAG, "port: " + endpoint.getPort());
        Log.d(TAG, "serviceType: " + endpoint.getServiceType());

        mPeerListener.peerStatusOnFound("onEndpointFound", PeerListener.PEER_FOUND, endpoint);
        // se añade a la lista de dispositivos conectados
        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
    }

    private String generateDeviceid(){

        String deviceId = "";
        try{
            MessageDigest md = MessageDigest.getInstance("SHA");
            String sha = convertByteArrayToHexString(md.digest(new Date().toString().getBytes()));
            deviceId = sha.substring(0,5);
        }catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return deviceId;
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    @Override
    public void registerRequest(Endpoint endpoint) {
        Log.d(TAG, endpoint.getDeviceId() + " solicita registrarse.");
        endpoint.setConnected(true);
        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
        mPeerListener.peerConnected("Peer Conectado", PeerListener.PEER_CONNECTED, endpoint);
    }

    @Override
    public void deregisterRequest(Endpoint endpoint) {
        Log.d(TAG, endpoint.getDeviceId() + " se desconecta.");
        endpoint.setConnected(false);
        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
        mPeerListener.peerDisconnected("Peer Desconectado: " + endpoint.getDeviceName(), PeerListener.PEER_DISCONNECTED, endpoint);
    }

    @Override
    public void messageRequest( String jsonmessage ) {
        // mMessageListener.messageReceived(jsonmessage);
        try {

            JSONObject json = new JSONObject(jsonmessage);
            JSONObject endpoint = new JSONObject(json.getString("endpoint"));

            String deviceId = endpoint.getString("deviceId");
            String deviceName = endpoint.getString("deviceName");
            String serviceType = endpoint.getString("serviceType");
            String hostname = endpoint.getString("hostname");
            int port = endpoint.getInt("port");

            String msg = json.getString("msg");

            Endpoint ep = new Endpoint(deviceId, deviceName);
            ep.setServiceType(serviceType);
            ep.setHostname(hostname);
            ep.setPort(port);

            mMessageListener.messageReceived(ep, msg);

        }catch( JSONException jsone){

            jsone.printStackTrace();

        }


    }
}
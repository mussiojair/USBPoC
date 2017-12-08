package com.pagatodo.ptscreen.communication.http;

import android.util.Log;

import com.pagatodo.ptscreen.data.Endpoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by mcardenas on 19/06/2017.
 */

public class WebServer extends NanoHTTPD implements Serializable {

    private final static String TAG = "WebServer";
    private String mDeviceId;

    public WebServer(String deviceId, int port) {
        super(port);
        mDeviceId = deviceId;
        Log.d(TAG, "Starting WebServer");
    }

    public WebServer(String deviceId, String hostname, int port) {
        super(hostname, port);
        mDeviceId = deviceId;
        Log.d(TAG, "Starting WebServer");
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "on server() method: " + session.getUri());
        String msg = mDeviceId;

        Map<String, String> parms = new HashMap<String, String>();
        try {
            session.parseBody(parms);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"IOException\"}");
        } catch (ResponseException e) {
            e.printStackTrace();
            return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"ResponseException\"}");
        } catch(Exception e){
            e.printStackTrace();
            return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"Exception\"}");
        }

        if( session.getUri().contains("mensaje/")) {

            Set<String> keys = parms.keySet();
            String jsonpostmsg = parms.get("postData");
            Log.d(TAG, "mensaje recibido: " + jsonpostmsg );

            mRequestListener.messageRequest(jsonpostmsg);

            return newFixedLengthResponse("{ \"status\" : \"ok\"}");

        }else if( session.getUri().contains("desconectar/") ){

            Set<String> keys = parms.keySet();
            String jsonpostdata = parms.get("postData");
            Log.d(TAG, "Desconectar: " + jsonpostdata );
            Endpoint endpoint = null;

            try {
                JSONObject jsonendpoint = new JSONObject(jsonpostdata);
                String serviceType = jsonendpoint.getString("serviceType");
                String deviceName = jsonendpoint.getString("deviceName");
                String deviceId = jsonendpoint.getString("deviceId");
                String hostname = jsonendpoint.getString("hostname");
                int port = jsonendpoint.getInt("port");

                endpoint = new Endpoint(deviceId, deviceName);
                endpoint.setServiceType(serviceType);
                endpoint.setHostname(hostname);
                endpoint.setPort(port);

                Log.d(TAG, "deregisterRequest");
                mRequestListener.deregisterRequest(endpoint);


            }catch(JSONException jsone){
                jsone.printStackTrace();
                return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"JSONException\"}");
            }catch(Exception e){
                e.printStackTrace();
                return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"Exception\"}");
            }

            return newFixedLengthResponse("{ \"status\" : \"ok\"}");

        }else if( session.getUri().contains("registrar/") ){

            Set<String> keys = parms.keySet();
            String jsonpostdata = parms.get("postData");
            Endpoint endpoint = null;

            try {
                JSONObject jsonendpoint = new JSONObject(jsonpostdata);
                String serviceType = jsonendpoint.getString("serviceType");
                String deviceName = jsonendpoint.getString("deviceName");
                String deviceId = jsonendpoint.getString("deviceId");
                String hostname = jsonendpoint.getString("hostname");
                int port = jsonendpoint.getInt("port");

                endpoint = new Endpoint(deviceId, deviceName);
                endpoint.setServiceType(serviceType);
                endpoint.setHostname(hostname);
                endpoint.setPort(port);

                mRequestListener.registerRequest(endpoint);

            }catch(JSONException jsone){
                jsone.printStackTrace();
                return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"JSONException\"}");
            }catch(Exception e){
                e.printStackTrace();
                return newFixedLengthResponse("{ \"status\" : \"error\", \"msg\": \"Exception\"}");
            }
            return newFixedLengthResponse("{ \"status\" : \"ok\"}");
        }

        return newFixedLengthResponse(msg);
    }


    private WebServerRequestListener mRequestListener;

    public void setWebServerRequestListener(WebServerRequestListener requestListener){
        mRequestListener = requestListener;
    }

    public interface WebServerRequestListener{
        void registerRequest(Endpoint endpoint);
        void deregisterRequest(Endpoint endpoint);
        void messageRequest(String jsonmessage);
    }
}

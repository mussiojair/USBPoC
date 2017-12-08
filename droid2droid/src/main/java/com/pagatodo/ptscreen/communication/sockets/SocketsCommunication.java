package com.pagatodo.ptscreen.communication.sockets;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.pagatodo.ptscreen.communication.DiscoveryService;
import com.pagatodo.ptscreen.communication.GeneralCommunication;
import com.pagatodo.ptscreen.data.Endpoint;
import com.pagatodo.ptscreen.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Set;

import static android.content.Context.WIFI_SERVICE;


/**
 * Created by mcardenas on 02/08/2017.
 */

public class SocketsCommunication extends GeneralCommunication implements DiscoveryService.ServiceDiscoveryListener,
        EndpointSocket.EndpointSocketListener {


    /*
    * ServerSocketOperations controla la comunicación con el endpoint que solicita la conexión
    * El endpoint que funje como Advertiser utiliza un server socket para esperar solicitudes de conexión entrantes
    * Luego de recibir esta solicitud
    *
    */

    private static final String TAG = "SocketsComm";
    public static final int SOCKET_SERVER = 1;
    public static final int SOCKET_CLIENT = 2;


    private Context context;
    private DiscoveryService mDiscovery;
    private String mDeviceId = "";
    private ServerSocket mServerSocket;
    private EndpointSocket mEPSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String mHostname = null;
    private Endpoint thisEndpoint, remoteEndpoint;




    public SocketsCommunication(Context context, int connectionType){

        this.context = context;
        setServiceType("_socket._tcp.");
        mDeviceId  = Utils.generateDeviceid( new Date().toString() );

        if(connectionType == SOCKET_SERVER)
            startService();
    }

    @Override
    public void startService() {

        try {

            initServer();

        } catch ( IOException ioe ){
            ioe.printStackTrace();
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private void initServer() throws IOException {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try{


                    while( true ) {

                        mServerSocket = new ServerSocket(0);
                        mLocalPort = mServerSocket.getLocalPort();
                        Socket s = mServerSocket.accept(); // Espera hasta recibir petición de conexión

                        if( remoteEndpoint != null && remoteEndpoint.isConnected() ) {
                            s.close();
                            continue;
                        }

                        mEPSocket = new EndpointSocket(s);
                        mEPSocket.setEndpointSocketListener(SocketsCommunication.this);

                        Endpoint remote = new Endpoint(Utils.generateDeviceid(s.getInetAddress().getHostAddress()), s.getInetAddress().getHostName());
                        remote.setHostname(s.getInetAddress().getHostAddress());
                        remote.setPort(s.getPort());

                        // Registrar el dispositivo con estatus: CONNECTED
                        remoteEndpoint = remote;
                        mStatusListener.statusChanged("Conexión a " + remoteEndpoint.getDeviceId() + " exitosa.", STATUS_ENDPOINT_CONNECTED);
                        mDetectedDevices.put(remoteEndpoint.getDeviceId(), remoteEndpoint);

                        updateEndpointStatus(remoteEndpoint, true);

                        mPeerListener.peerConnected("Peer Conectado", PeerListener.PEER_CONNECTED, remoteEndpoint);
                    }

                } catch ( IOException  ioe ){
                    ioe.printStackTrace();
                }
            }
        });

        t.start();
    }

    private void configureThisEndpoint() {
        thisEndpoint = new Endpoint(mDeviceId, getDeviceName() == null ? Build.MODEL : getDeviceName() );
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        int ipAddress = info.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        thisEndpoint.setHostname(ip);
        thisEndpoint.setServiceType(getServiceType());
        thisEndpoint.setPort(mLocalPort);
    }

    @Override
    public void stopService() {
        try {

            if(in != null)
                in.close();

            if( out != null )
                out.close();

            if( mEPSocket != null )
                mEPSocket.close();

            if ( mServerSocket != null)
                mServerSocket.close();

        } catch ( IOException ioe ){

            ioe.printStackTrace();

        } catch ( Exception e ){

            e.printStackTrace();

        }
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

        new Thread(new Runnable() {
            @Override
            public void run() {

                try{

                    mHostname = endpoint.getHostname();
                    mLocalPort = endpoint.getPort();

                    if(mHostname == null){
                        throw new Exception("Hostname cannot be null");
                    }

                    if( mLocalPort == -1 ){
                        throw new Exception("Port not specified");
                    }

                    // Establece comunicación con el servidor
                    Socket s = new Socket(mHostname, mLocalPort);
                    mEPSocket = new EndpointSocket(s);
                    mEPSocket.setEndpointSocketListener(SocketsCommunication.this);

                    // Registrar el dispositivo con estatus: CONNECTED
                    mStatusListener.statusChanged("Conexión a " + endpoint.getDeviceId() + " exitosa.", STATUS_ENDPOINT_CONNECTED);
                    Endpoint ep = mDetectedDevices.get(endpoint.getDeviceId());
                    updateEndpointStatus(ep, true);
                    remoteEndpoint = ep;
                    mPeerListener.peerConnected("Peer Conectado", PeerListener.PEER_CONNECTED, ep);

                } catch (ConnectException ce ) {

                    ce.printStackTrace();
                    mPeerListener.peerStatusOnLost("Peer Lost", PeerListener.PEER_LOST, remoteEndpoint);

                } catch( IOException ioe) {

                    ioe.printStackTrace();
                    mPeerListener.peerStatusOnLost("Peer Lost", PeerListener.PEER_LOST, remoteEndpoint);

                }catch( Exception e){

                    e.printStackTrace();
                    mPeerListener.peerStatusOnLost("Peer Lost", PeerListener.PEER_LOST, remoteEndpoint);

                }
            }
        }).start();
    }

    @Override
    public void disconnectDevice(Endpoint endpoint) {
        if( mEPSocket != null )
            mEPSocket.send("Bye.");
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void startDiscovering() {
        if( mDiscovery == null ) {
            mDiscovery = new DiscoveryService(context, mServiceId, mServiceType, mDeviceId);
            mDiscovery.setLocalPort(mLocalPort);
            mDiscovery.setServiceDiscoveryListener(this);
        }
        mDiscovery.startDiscovering();
    }


    @Override
    public void stopDiscovering() {
        if( mDiscovery != null ) {
            mDiscovery.stopDiscovering();
            mDiscovery = null;
        }
    }

    @Override
    public void setLocalPort(int port) {
        mLocalPort = port;
    }


    @Override
    public void configureLocalPort() {
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
    public void sendMessageToDevice(final Endpoint endpoint, final String message) {

        mEPSocket.send(message);

        //try {
            //JSONObject msg = new JSONObject();
            //msg.put("msg", message);
            // msg.put("endpoint", thisEndpoint.toJSON());
        //}catch( JSONException jsone ){
        //    jsone.printStackTrace();
        //}
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

        Log.d(TAG, "ServiceDetected. deviceId: " + endpoint.getDeviceId() + ", deviceName: " + endpoint.getDeviceName());
        Log.d(TAG, "Hostname:" + endpoint.getHostname());
        Log.d(TAG, "Port: " + endpoint.getPort());
        Log.d(TAG, "ServiceType: " + endpoint.getServiceType());

        mPeerListener.peerStatusOnFound("onEndpointFound", PeerListener.PEER_FOUND, endpoint);
        // se añade a la lista de dispositivos conectados
        mDetectedDevices.put(endpoint.getDeviceId(), endpoint);
    }

    @Override
    public void endpointDisconnected() {
        Log.d(TAG, "Endpoint disconnected");
        if( remoteEndpoint != null ) {
            mPeerListener.peerDisconnected("Peer Desconectado: " + remoteEndpoint.getDeviceName(), PeerListener.PEER_DISCONNECTED, remoteEndpoint);
            mDetectedDevices.remove(remoteEndpoint.getDeviceId());
            remoteEndpoint = null;
        }
    }

    @Override
    public void receivedMessage(String msg) {
        Log.d( TAG , "Mensaje recibido: " + msg );

        mMessageListener.messageReceived( remoteEndpoint, msg );
    }
}

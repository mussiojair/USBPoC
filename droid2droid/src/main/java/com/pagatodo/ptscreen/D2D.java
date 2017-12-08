package com.pagatodo.ptscreen;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.pagatodo.ptscreen.communication.GeneralCommunication;
import com.pagatodo.ptscreen.communication.http.HttpCommunication;
import com.pagatodo.ptscreen.communication.nearby.NearbyCommunication;
import com.pagatodo.ptscreen.communication.sockets.SocketsCommunication;
import com.pagatodo.ptscreen.data.Endpoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * D2D es la clase de alto nivel que permite a 2 dispositivos android cercanos detectarse y comunicarse entre ellos.
 * D2D puede inicializarse con un rol, ya sea <b>advertiser</b> o <b>discoverer</b>. Posee una clase interna Builder que facilita la creación de un objeto de esta clase.
 *
 * @author Mussio Cárdenas
 * @version 1.0
 */

public class D2D
        implements GeneralCommunication.StatusListener, GeneralCommunication.MessagesListener, GeneralCommunication.PeerListener, Serializable{

    private static final String TAG = "D2D";
    private int mConnectionType; // Nearby or HTTP
    private int mRol;
    private int mListenPort; // optional
    private GeneralCommunication comm;
    private Context context;
    private String mServiceId;
    private GeneralCommunication.MessagesListener mMessagesListener;
    private GeneralCommunication.PeerListener mPeerListener;

    private D2DMessagesListener mD2DMessageListener;
    private D2DPeersListener mD2DPeersListener;
    private HashMap<String, Endpoint> mNearbyDevices;

    public void removeD2DPeersListener(){
        mD2DPeersListener = null;
    }

    public void addD2DPeersListener(D2DPeersListener d2dPeersListener){
        mD2DPeersListener = d2dPeersListener;
    }

    public D2D(Context context){
        mNearbyDevices = new HashMap<String, Endpoint>();
        mConnectionType = -1;
        mServiceId = null;
        mRol = -1;
        mListenPort = -1;
        comm = null;
        this.context = context;
        mServiceId = null;
        mMessagesListener = null;
    }

    /**
     * Anuncia o busca dispositivos dependiendo de la configuración elegida.
     * Si se eligió Google Nearby la app se anunciará a los dispositivos cercano con bluetooth/ble activo.
     * Si la app se configuró para usar comunicación HTTP o Sockets se anunciará o buscará dispositivos dentro de la red local.
     */

    public void startRol(){

        if(mRol == Rol.ADVERTISER) {
            comm.startAdvertising();
        }else if (mRol == Rol.DISCOVERER ){
            comm.startDiscovering();
        }
    }

    /**
     * Deja de anunciar o buscar dispositivos. Las conexiones establecidas permanecerán activas.
     */
    public void stopRol(){

        if(mRol == Rol.ADVERTISER) {

            comm.stopAdvertising();

        } else if (mRol == Rol.DISCOVERER ){

            comm.stopDiscovering();

        }

        // comm.stopService();
    }

    /**
     * Detiene completamente el servicio.
     */
    public void stopService(){
        comm.stopService();
    }

    /**
     * Si el tipo de servicio de comunicación fue establecido como Connections.CONNECTION_HTTP es posible especificar el puerto de escucha.
     * Si no se establece algún valor se utilizará el primer puerto disponible en el dispositivo.
     * @param port Puerto de escucha. Ejemplo: 8080, 80, 5000
     */
    public void setListenPort( int port ){
        mListenPort = port;
    }

    public void connectToDevice(Endpoint endpoint ){
        comm.connectTo(endpoint);
    }

    public void disconnectDevice( Endpoint endpoint ){
        comm.deleteConnectedDevice(endpoint.getDeviceId());
    }

    public void disconnectAllDevices(){
        Log.d(TAG, "Dispositivos conectados: " + mNearbyDevices.size() );
        for(String endpointId : mNearbyDevices.keySet() ) {
            Endpoint ep = mNearbyDevices.get(endpointId);
            Log.d(TAG, "Solicitando desconexión de: " + ep.getDeviceName() + " : " + ep.getURLEndpointString());
            comm.disconnectDevice(ep);
        }
        // Elimina todos los dispositivos detectados de la lista.
        mNearbyDevices.clear();
    }

    /**
     * Envía un mensaje en una String a un Endpoint específico. Se debe establecer previamente la conexión antes de intentar enviar el mensaje.
     * @param endpoint Endpoint remoto.
     * @param message Mensaje enviado.
     */
    public void sendMessageToDevice(Endpoint endpoint, String message){
        Log.d(TAG, "Enviando mensajes a: " + endpoint.getDeviceId() + ", mensaje: " + message);
        comm.sendMessageToDevice(endpoint, message);
    }

    /**
     * Envía un mensaje de texto a todos los dispositivos conectados.
     * @param message Cadena de caracteres que contiene el mensaje.
     */
    public void sendMessageToAllDevices(String message){
        Log.d(TAG, message);
        comm.sendMessageToAll(message);
    }


    /**
     * Establece en una String el identificador del servicio utilizado para encontrar dispositivos en la red.
     * @param serviceId
     */
    public void setServiceId(String serviceId){
        mServiceId = serviceId;
    }

    @Override
    public void statusChanged(String status, int statuscode) {
        Log.d(TAG, status);
    }

    @Override
    public void peerStatusChanged(String status, int statuscode, Endpoint endpoint) {
        Log.d(TAG, status);

        switch( statuscode ){
            case PEER_FOUND :
                Log.d(TAG, "peer found! : " + endpoint.getDeviceId() + ", " + endpoint.getDeviceName());
                break;
            case PEER_LOST :
                Log.d(TAG, "peer lost! : " + endpoint.getDeviceId());
                break;
        }
    }

    @Override
    public void peerStatusOnFound(String status, int statuscode, Endpoint endpoint) {
        Log.d(TAG, "Peer Found! : " + endpoint.getDeviceId() + ", " + endpoint.getDeviceName());

        if( !endpoint.isConnected() && mD2DPeersListener != null )
            mD2DPeersListener.onPeerFound(endpoint);

        mNearbyDevices.put(endpoint.getDeviceId(), endpoint);
    }

    private boolean peerAlreadyDetected(Endpoint endpoint){
        Endpoint check  = mNearbyDevices.get(endpoint.getDeviceId());
        if(check != null){
            return true;
        }
        return false;
    }

    @Override
    public void peerConnected(String status, int statuscode, final Endpoint endpoint) {
        endpoint.setConnected(true);
        mNearbyDevices.put(endpoint.getDeviceId(), endpoint);
        if(mD2DPeersListener != null) {
            if(context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mD2DPeersListener.onPeerConnected(endpoint);
                    }
                });
            }else{
                mD2DPeersListener.onPeerConnected(endpoint);
            }
        }
    }

    @Override
    public void peerDisconnected(String status, int statuscode, final Endpoint endpoint) {
        if(mD2DPeersListener != null) {
            if(context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mD2DPeersListener.onPeerDisconnected(endpoint);
                    }
                });

            }else{
                mD2DPeersListener.onPeerDisconnected(endpoint);
            }
        }
    }

    @Override
    public void peerStatusOnLost(String status, int statuscode, Endpoint endpoint) {

        if( endpoint != null) {
            Log.d(TAG, "peer lost! : " + endpoint.getDeviceId());
            if (mD2DPeersListener != null)
                mD2DPeersListener.onPeerLost(endpoint);
        }

    }
    /**
     * Establece la referencia al objecto D2DMessageListener que recibirá los mensajes enviados desde
     * otro peer con el que se tenga una conexión establecida.
     * @param d2dMessagesListener
     */
    public void setD2DMessageListener(D2DMessagesListener d2dMessagesListener){
        mD2DMessageListener = d2dMessagesListener;
    }

    @Override
    public void messageReceived(final Endpoint endpoint, final String message) {
        Log.d(TAG, message);
        if(mD2DMessageListener != null) {
            if(context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mD2DMessageListener.onMessageReceived(endpoint, message);
                    }
                });
            }else{
                mD2DMessageListener.onMessageReceived(endpoint, message);
            }
        }

    }

    /**
     * Cuando una conexión está activa D2DMessagesListener proporciona un método a través del cual se recibirán los mensajes enviados desde el otro extremo de la conexión.
     */
    public interface D2DMessagesListener{
        /**
         * El método onMessageReceived recibe un mensaje de tipo String desde el otro extremo de la conexión.
         * @param endpoint objeto Endpoint que manda el mensaje.
         * @param msg objeto de tipo String que contiene el mensaje enviado.
         */
        void onMessageReceived(Endpoint endpoint, String msg);
    }

    /**
     * Establece la referencia a un objeto que implemente la interfaz D2DPeersListener. Es obligatorio utilizarlo al momento de crear un objecto de tipo D2D.
     * @param d2DPeersListener Referencia al objeto que implementa la interfaz D2DPeersListener
     */
    public void setD2DPeersListener(D2DPeersListener d2DPeersListener){
        mD2DPeersListener = d2DPeersListener;
    }

    /**
     * La interfaz D2DPeersListener proporciona cuatro métodos para notificar a nuestra aplicación acerca de los posibles estados de un endpoint remoto.
     */
    public interface D2DPeersListener{
        /**
         * Notifica cuando un Endpoint ha sido detectado.
         * @param endpoint Endpoint remoto.
         */
        void onPeerFound(Endpoint endpoint);

        /**
         * Notifica cuando un Endpoint ya no es detectado en la red.
         * @param endpoint Endpoint remoto.
         */
        void onPeerLost(Endpoint endpoint);

        /**
         * Notifica cuando la conexión con un Endpoint se ha realizado satifactoriamente.
         * @param endpoint Endpoint remoto.
         */
        void onPeerConnected(Endpoint endpoint);

        /**
         * Notifica cuando nos hemos desconectado del Endpoint remoto.
         * @param endpoint Endpoint remoto.
         */
        void onPeerDisconnected(Endpoint endpoint);
    }
    /**
     * D2D.Builder es una clase interna usada para definir el rol de la app que implemente la librería D2D.
     * Solo existen 2 roles para los que implementan la interfaz. Advertisers y discoverers.
     * Advertiser es la app que se anuncia ofreciendo un servicio en la red.
     * Discoverer es la app que busca otros dispositivos en la red para conectarse.
     * @author Mussio Cárdenas
     * @version 1.0
     */

    public static class Builder {

        private int mConnectionType;
        private String mServiceId;
        private int mRol;
        private GeneralCommunication comm;
        private int mListenPort;
        private Context context;
        private D2DMessagesListener mD2DMessagesListener;
        private D2DPeersListener mD2DPeersListener;

        public Builder(){
            mConnectionType = -1;
            mServiceId = null;
            comm = null;
            mListenPort = -1;
            context = null;
            mRol = -1;
            mD2DMessagesListener = null;
        }

        /**
         * Establece el tipo de comunicación subyacente por el que se establecerá la conexión e
         * intercambiarán los mensajes. Actualmente tiene soporte para Google Nearby y HTTP.
         * Las constantes del parámetro @param serviceType están definidas en la clase @see Connections
         *
         * @author Mussio Cárdenas
         * @version 1.0
         * @param connectionType
         * @return Builder
         */
        public Builder setConnectionType(int connectionType ){

            mConnectionType = connectionType;

            switch( connectionType ){
                case Connections.CONNECTION_HTTP :
                    comm = new HttpCommunication(context);
                    break;
                case Connections.CONNECTION_NEARBY :
                    comm = new NearbyCommunication(context, mServiceId);
                    break;
                case Connections.CONNECTION_SOCKET :
                    comm = new SocketsCommunication(context, mRol == Rol.ADVERTISER ? SocketsCommunication.SOCKET_SERVER : SocketsCommunication.SOCKET_CLIENT );
            }

            return this;
        }


        public Builder setRol( int rol ){
            mRol = rol;
            return this;
        }


        public Builder setServiceId(String serviceId){
            mServiceId = serviceId;
            return this;
        }

        /**
         * Establece el contexto de la app. Es obligatorio para el funcionamiento de las actividades de red.
         * @param context
         * @return Builder
         */
        public Builder setContext(Context context){
            this.context = context;
            return this;
        }



        public Builder setD2DMessagesListener(D2DMessagesListener d2dMessagesListener){
            mD2DMessagesListener = d2dMessagesListener;
            return this;
        }

        public Builder setD2DPeersListener(D2DPeersListener d2dPeersListener){
            mD2DPeersListener = d2dPeersListener;
            return this;
        }

        /**
         * Construye el objeto D2D con el que se controla la comunicación entre dispositivos.
         * El contexto debe ser establecido utilizando la función Builder.setContext() sino lanzará una excepción
         *
         * @return D2D object. null si el objeto no pudo ser creado.
         */

        public D2D build() {

            try {
                if (context == null)
                    throw new Exception("No has especificado el contexto de la app. Builder.setContext(Context c).");

                if (mRol == -1)
                    throw new Exception("No has especificado el rol. Builder.setRol( int rol ). Puede ser Rol.ADVERTISER o Rol.DISCOVERER.");

                if (mServiceId == null)
                    throw new Exception("No has especificado el serviceId. Builder.setServiceId(String serviceName). Ej: com.domain.appname");

                if (mD2DMessagesListener == null)
                    throw new Exception("No has especificado un receptor D2DMessagesListener para los mensajes recibidos. Builder.setMessagesListener(D2DMessagesListener listener).");

                if (mD2DPeersListener == null)
                    throw new Exception("No has especificado un receptor D2DPeersListener para los dispositivos encontrados. Builder.setPeersListener(D2DPeersListener listener).");

                D2D nuevo = new D2D(context);
                comm.setStatusListener( nuevo );
                comm.setMessagesListener( nuevo );
                comm.setPeerListener( nuevo );
                comm.setServiceId(mServiceId);
                nuevo.mConnectionType = mConnectionType;
                nuevo.mListenPort = mListenPort;
                nuevo.comm = comm;
                nuevo.mRol = mRol;
                nuevo.mServiceId = mServiceId;
                nuevo.setD2DMessageListener(mD2DMessagesListener);
                nuevo.setD2DPeersListener(mD2DPeersListener);

                return nuevo;

            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }
}

package com.pagatodo.ptscreen.communication.sockets;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by mcardenas on 07/08/2017.
 */

class EndpointSocket {

    private static final String TAG = "EndpointSocket";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean close = false;
    private MessageReceiver mMessageReceiver;
    private PingSocket mPing;


    public EndpointSocket(Socket s){
        try {

            socket = s;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader( new InputStreamReader( socket.getInputStream() ));

            // Comienza a escuchar mensajes entrantes.
            mMessageReceiver = new MessageReceiver(in);
            mMessageReceiver.start();

            // ping socket
            mPing = new PingSocket(out);
            mPing.start();

        }catch( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    public void send( final String message ){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Enviando mensaje: " + message);
                out.println(message);
            }
        }).start();
    }

    public void close(){
        close = true;
        mMessageReceiver.interrupt();
        mPing.interrupt();
        // Cerrar los streams
        try {
            in.close();
            socket.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    private EndpointSocketListener epslistener;

    public void setEndpointSocketListener( EndpointSocketListener l ){
        this.epslistener = l;
    }

    /*
    *  Reporta estados y comunica mensajes a
    *  el objeto usuario de las instancias de esta clase
    */
    public interface EndpointSocketListener{
        void endpointDisconnected();
        void receivedMessage( String msg );
    }

    /*
    * Hilo secundario que espera mensajes entrantes
    */

    private class MessageReceiver extends  Thread{

        private BufferedReader in;

        public MessageReceiver( BufferedReader in ){
            this.in = in;
        }

        @Override
        public void run(){
            try {
                String line = "";
                while ( (line = in.readLine()) != null) {
                    Log.d(TAG, "recv: " + line);

                    if( line.equals( "Bye." )){
                        if( epslistener != null)
                            epslistener.endpointDisconnected();
                        break;
                    }

                    if ( line.equals( "ping" )){
                        continue;
                    }

                    if( epslistener != null)
                        epslistener.receivedMessage(line);
                }
            }catch( IOException ioe ){

                ioe.printStackTrace();

            } finally{
                try {
                    in.close();
                    socket.close();
                    // notificar desconexión
                    epslistener.endpointDisconnected();
                }catch(IOException ioe){
                    ioe.printStackTrace();
                }catch( Exception e ){
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Desconectado...");

        }

    }


    private class PingSocket extends Thread{

        PrintWriter out;

        public PingSocket( PrintWriter out ){
            this.out = out;
        }
        @Override
        public void run(){

            while( true ) {
                try {

                    out.println("ping");
                    Thread.sleep(3000);

                } catch (InterruptedException ie ){
                    ie.printStackTrace();
                } catch( Exception e ){
                    epslistener.endpointDisconnected();
                }
            }
        }
    }
}
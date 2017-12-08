package com.pagatodo.ptscreen;

import java.io.Serializable;

/**
 * Created by mcardenas on 20/06/2017.
 * Catalogo de los tipos de conexiones soportadas por la librería
 */

public class Connections implements Serializable {

    /**
     * Especifica que el tipo de servicio de comunicación es por Google Nearby.
     * Este tipo de conexión funciona por Bluetooth/BLE y no necesita que los dispositivos estén conectados en una red o a internet.
     */
    public final static int CONNECTION_NEARBY = 1;


    /**
     * Especifica que el tipo de servicio de comunicación es por un servidor HTTP corriendo en la misma red local
     */
    public final static int CONNECTION_HTTP = 2;



    public final static int CONNECTION_SOCKET = 3;

}

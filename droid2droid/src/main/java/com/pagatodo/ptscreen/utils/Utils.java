package com.pagatodo.ptscreen.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by mcardenas on 04/08/2017.
 */

public class Utils {

    public static String generateDeviceid( String seed ){

        String deviceId = "";
        try{
            MessageDigest md = MessageDigest.getInstance("SHA");
            String sha = convertByteArrayToHexString(md.digest(seed.getBytes()));
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
}

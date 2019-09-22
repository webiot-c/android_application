package com.webiot_c.cprss_notifi_recv.connect;

import android.content.Context;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CPRSS_WebSocketClient extends WebSocketClient {

    CPRSS_WebSocketClientListener listener;

    AEDInformationDatabaseHelper dbhelper;

    public CPRSS_WebSocketClient(URI uri, CPRSS_WebSocketClientListener listener){
        this(uri, listener, null);
    }

    public CPRSS_WebSocketClient(URI uri, CPRSS_WebSocketClientListener listener, Context context){
        super(uri);
        this.listener = listener;
        dbhelper = AEDInformationDatabaseHelper.getInstance(context);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        listener.onOpen(handshakedata);
    }

    @Override
    public void onMessage(String message) {
        Log.d("WSC", message);
        String[] mes_formatted = message.split("#");
        if(mes_formatted.length != 4){
            listener.onOtherMessage(message);
            return;
        }

        AEDInformation aedInfo;
        try {
            aedInfo = new AEDInformation(
                    mes_formatted[1],
                    Double.valueOf(mes_formatted[2]),
                    Double.valueOf(mes_formatted[3]),
                    new Date()
            );
        } catch (NumberFormatException e){
            listener.onOtherMessage(message);
            return;
        }

        switch(mes_formatted[0]){
            case "AED-OPEN":
                listener.onAEDUseStarted(aedInfo); break;

            case "AED-POLLING":
                if(!dbhelper.isAlreadyRegistred(aedInfo.getAed_id())) {
                    Log.i("WSC", "Received Missed aed data! UseStarted will be called.");
                    listener.onAEDUseStarted(aedInfo);
                } else {
                    Log.i("WSC", "Location update: " + aedInfo.toString());
                    listener.onAEDLocationUpdated(aedInfo);
                }
                break;

            case "AED-CLOSE":
                listener.onAEDUseFinished(aedInfo); break;

            default:
                listener.onOtherMessage(message);
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        listener.onClose(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        listener.onError(ex);
    }
}

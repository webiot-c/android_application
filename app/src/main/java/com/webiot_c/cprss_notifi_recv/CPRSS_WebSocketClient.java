package com.webiot_c.cprss_notifi_recv;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class CPRSS_WebSocketClient extends WebSocketClient {

    CPRSS_WebSocketClientListener listener;

    CPRSS_WebSocketClient(URI uri, CPRSS_WebSocketClientListener listener){
        super(uri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        listener.onOpen(handshakedata);
    }

    @Override
    public void onMessage(String message) {
        Log.e("SKT", message);
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
                    Double.valueOf(mes_formatted[3])
            );
        } catch (NumberFormatException e){
            listener.onOtherMessage(message);
            return;
        }

        switch(mes_formatted[0]){
            case "AED-OPEN":
                listener.onAEDUseStarted(aedInfo); break;
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

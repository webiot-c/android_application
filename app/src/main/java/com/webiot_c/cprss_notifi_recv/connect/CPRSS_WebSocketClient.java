package com.webiot_c.cprss_notifi_recv.connect;

import android.content.Context;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ClientEndpoint
public class CPRSS_WebSocketClient {

    CPRSS_WebSocketClientListener listener;

    AEDInformationDatabaseHelper dbhelper;

    WebSocketContainer container;

    URI uri_info;



    public CPRSS_WebSocketClient(URI uri, CPRSS_WebSocketClientListener listener) throws IOException, DeploymentException {
        this(uri, listener, null);
    }

    public CPRSS_WebSocketClient(URI uri, CPRSS_WebSocketClientListener listener, Context context) throws IOException, DeploymentException {

        uri_info = uri;
        this.listener = listener;
        reconnect();

        dbhelper = AEDInformationDatabaseHelper.getInstance(context);
    }

    public void reconnect(){
        final CPRSS_WebSocketClient wrapped_this = this;
        new Thread(new Runnable() {
            @Override
            public void run() {

                container = ContainerProvider.getWebSocketContainer();
                try {
                    container.connectToServer(wrapped_this, uri_info);
                } catch (DeploymentException | IOException e) {
                    Log.e("WS Connection", "Error occured when trying to connect.", e);
                    listener.onClose(null);
                }

            }
        }).start();
    }

    @OnOpen
    public void onOpen(Session session) {
        Log.d("WSC", "Websocket Session Opened!");
        listener.onOpen(session);
    }

    @OnMessage
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

    @OnClose
    public void onClose(Session session) {
        listener.onClose(session);
    }

    @OnError
    public void onError(Throwable ex) {
        listener.onError(ex);
    }
}

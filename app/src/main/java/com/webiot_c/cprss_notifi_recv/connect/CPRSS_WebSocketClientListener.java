package com.webiot_c.cprss_notifi_recv.connect;

import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;

import org.java_websocket.handshake.ServerHandshake;

public interface CPRSS_WebSocketClientListener {

    void onOpen(ServerHandshake handshakedata);
    void onClose(int code, String reason, boolean remote);
    void onAEDUseStarted(AEDInformation aedInfo);
    void onAEDLocationUpdated(AEDInformation aedInfo);
    void onAEDUseFinished(AEDInformation aedInfo);
    void onOtherMessage(String message);
    void onError(Exception e);

}

package com.webiot_c.cprss_notifi_recv.connect;

import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;

import javax.websocket.Session;

public interface CPRSS_WebSocketClientListener {

    void onOpen(Session session);
    void onClose(Session session);
    void onAEDUseStarted(AEDInformation aedInfo);
    void onAEDLocationUpdated(AEDInformation aedInfo);
    void onAEDUseFinished(AEDInformation aedInfo);
    void onOtherMessage(String message);
    void onError(Throwable e);

}

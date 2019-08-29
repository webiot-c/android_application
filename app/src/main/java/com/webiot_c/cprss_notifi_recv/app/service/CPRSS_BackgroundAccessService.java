package com.webiot_c.cprss_notifi_recv.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.app.MainActivity;
import com.webiot_c.cprss_notifi_recv.connect.CPRSS_WebSocketClient;
import com.webiot_c.cprss_notifi_recv.connect.CPRSS_WebSocketClientListener;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;
import com.webiot_c.cprss_notifi_recv.utility.LocationGetter;
import com.webiot_c.cprss_notifi_recv.utility.NotificationUtility;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class CPRSS_BackgroundAccessService extends Service implements CPRSS_WebSocketClientListener{

    public class BroadcastConstant {
        public static final String AED_STARTED          = "AED_STARTED";
        public static final String AED_LOCATION_UPDATED = "AED_LOCATION_UPDATED";
        public static final String AED_FINISHED         = "AED_FINISHED";
        public static final String DATABASE_UPDATED     = "DATABASE_UPDATED";
    }

    class ActivityMessageReceiver extends Handler {
        @Override
        public void handleMessage(Message mes){
            distance = mes.what;
            Log.e("DistanceUpdated", distance + " Km");
        }
    }

    /**
     * CPRSSのWebサーバーと通信するときに使うクライアント。
     */
    private CPRSS_WebSocketClient wsclient;

    /**
     * {@link AEDInformation}を保存するデータベースとの通信に使用する
     */
    AEDInformationDatabaseHelper dbhelper;

    LocationGetter locationGetter;

    Messenger mServiceMessenger = new Messenger( new ActivityMessageReceiver());

    int distance;

    @Override
    public void onCreate() {

        try {
            wsclient = new CPRSS_WebSocketClient(new URI("ws://cprss-notificator.herokuapp.com/"), CPRSS_BackgroundAccessService.this );
        } catch(Exception e){

        }
        wsclient.connect();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notifyman = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notify = new Notification.Builder(CPRSS_BackgroundAccessService.this,
                    NotificationUtility.NOTIFICATION_CHANNEL_BACKGROUND)
                            .setContentTitle(String.format(getString(R.string.notify_background), getString(R.string.app_name)))
                            .setSmallIcon(R.drawable.ic_server_connection)
                    .build();

            startForeground(1, notify);
        }

        dbhelper = AEDInformationDatabaseHelper.getInstance(getApplicationContext());
        locationGetter = new LocationGetter(this);

        if (wsclient.isClosed()){
            wsclient.reconnect();
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceMessenger.getBinder();
    }

    // ----- サーバー接続関連 ----- //

    /**
     * 接続に失敗した際に再試行した回数。
     */
    int retryCount = 0;

    /**
     * 次に接続試行するまでの時間。(5秒 + {@code interval}分)
     */
    int interval = 0;

    /**
     * サーバーとの接続に成功したときに呼び出される。
     * @param handshakedata 接続成功に関する詳しい情報。
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        retryCount = 0;
        Log.e("WS", "could access to server!");
    }

    /**
     * サーバーとの接続が切断されたときに呼び出される。
     * 処理としては、10回接続を試行し、失敗した場合は通知を送信して、しばらく時間を置いてから試行する。
     * @param code 切断された理由。
     * @param reason 切断された理由。
     * @param remote サーバー側から切断された理由は True。
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(retryCount > 10){
                    NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_SERVER,
                            CPRSS_BackgroundAccessService.this,
                            android.R.drawable.ic_dialog_info,
                            getString(R.string.notify_disconnected),
                            getString(R.string.notify_disconnection_detail),
                            getString(R.string.notify_disconnection_detail) + "\n" +
                            getString(R.string.notify_disconnected_context)
                    );
                    retryCount = 0;
                    interval += 1;
                }
                try{
                    Thread.sleep(500 + (1000 * 60 * interval));
                } catch (InterruptedException e) {
                    Log.e("WebSocket Ret.", "Couldn't wait enough time due to interruption.");
                }
                Log.e("WebSocket Ret.", "Cannnot access to server! trying. attempt " + String.valueOf(retryCount) + "/10");
                wsclient.reconnect();
                retryCount++;
            }
        }).start();
    }

    /**
     * AED使用が開始されたときに通知される。
     * @param aedInfo AED情報。
     */
    @Override
    public void onAEDUseStarted(AEDInformation aedInfo) {

        if(dbhelper.isAlreadyRegistred(aedInfo.getAed_id())) return;

        // 距離で識別する
        if(locationGetter.getCurrentLocation() != null) {
            double req_distance = MainActivity.notification_distance;

            float[] results = new float[3];
            Location.distanceBetween(
                    aedInfo.getLatitude(), aedInfo.getLongitude(),
                    locationGetter.getCurrentLocation().getLatitude(), locationGetter.getCurrentLocation().getLongitude(), results);
            if (req_distance < results[0] / 1000) return;
        }


        NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_AED_START,
                this,
                android.R.drawable.ic_dialog_info,
                getString(R.string.notify_aed_open),
                getString(R.string.notify_aed_open_detail),
                getString(R.string.notify_aed_open_context));

        dbhelper.saveData(aedInfo);

        Intent intent = new Intent();
        intent.setAction(BroadcastConstant.AED_STARTED);
        sendBroadcast(intent);

        requestDatabaseUpdate();

    }

    /**
     * AED使用が完了した際に通知される。
     * @param aedInfo AED情報。
     */
    @Override
    public void onAEDUseFinished(AEDInformation aedInfo) {
        NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_AED_FINISH,
                this,
                android.R.drawable.ic_dialog_info,
                getString(R.string.notify_aed_close),
                getString(R.string.notify_aed_close_detail));
        dbhelper.deleteData(aedInfo.getAed_id());

        Intent intent = new Intent();
        intent.setAction(BroadcastConstant.AED_FINISHED);
        sendBroadcast(intent);

        requestDatabaseUpdate();

    }

    /**
     * メッセージを受信したが、{@link CPRSS_WebSocketClient}が解釈できなかった場合に通知される。
     * @param message 受信したメッセージ。
     */
    @Override
    public void onOtherMessage(String message) {
        Log.e("CPRSS", "Unparsed Message: " + message);

    }

    /**
     * サーバーとの通信において、例外が発生した場合に通知される。
     * @param e 発生した例外
     */
    @Override
    public void onError(Exception e) {

        Log.e("CPRSS", "Error occured in WS", e);
    }


    public void requestDatabaseUpdate(){
        sendBroadcast(new Intent(BroadcastConstant.DATABASE_UPDATED));
    }

}

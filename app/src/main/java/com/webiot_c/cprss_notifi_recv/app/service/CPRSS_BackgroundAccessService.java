package com.webiot_c.cprss_notifi_recv.app.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.DialogActivity;
import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.app.AEDLocationActivity;
import com.webiot_c.cprss_notifi_recv.app.MainActivity;
import com.webiot_c.cprss_notifi_recv.connect.CPRSS_WebSocketClient;
import com.webiot_c.cprss_notifi_recv.connect.CPRSS_WebSocketClientListener;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;
import com.webiot_c.cprss_notifi_recv.utility.DateCompareUtility;
import com.webiot_c.cprss_notifi_recv.utility.LocationGetter;
import com.webiot_c.cprss_notifi_recv.utility.NotificationUtility;
import com.webiot_c.cprss_notifi_recv.utility.PreferencesUtility;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        }
    }

    public static final String WS_SERVER_ADDRESS = "ws://192.168.10.8:6789/";

    /**
     * CPRSSのWebサーバーと通信するときに使うクライアント。
     */
    private CPRSS_WebSocketClient wsclient;

    /**
     * {@link AEDInformation}を保存するデータベースとの通信に使用する
     */
    AEDInformationDatabaseHelper dbhelper;

    /**
     * ユーザーによって無視されたAED情報のID。
     * AED-FINISH受信時か、10分経過後に自動的に削除されるようになっている。
     */
    static Map<String, Date> ignoredAEDID = new HashMap<>();

    LocationGetter locationGetter;

    Messenger mServiceMessenger = new Messenger( new ActivityMessageReceiver());

    int distance;

    boolean isErrorOccured = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if(!isLocationPermissionGranted()){

            Intent intent = new Intent(this, DialogActivity.class);
            intent.putExtra("title", String.format(getString(R.string.loc_permission_turnoff), getString(R.string.service_name)));
            intent.putExtra("mes", getString(R.string.loc_permission_turnoff_context));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            isErrorOccured = true;
            return;
        }

        try {
            wsclient = new CPRSS_WebSocketClient(new URI(WS_SERVER_ADDRESS), CPRSS_BackgroundAccessService.this, this );
            Log.e("WSC", ( wsclient == null ? null : wsclient.toString()));
        } catch(Exception e){
            Log.e("WSC", "Error occured in creating instance", e);
        }
        wsclient.connect();
        PreferencesUtility.initialize(this);
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

        if(isErrorOccured){
            stopSelf();
            return Service.START_NOT_STICKY;
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
     * 一度接続失敗の通知を表示しているか
     */
    boolean isDisconnedtedNoticed = false;

    /**
     * サーバーとの接続に成功したときに呼び出される。
     * @param handshakedata 接続成功に関する詳しい情報。
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        retryCount = 0;
        if(isDisconnedtedNoticed){
            isDisconnedtedNoticed = false;
            NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_SERVER,
                    this,
                    android.R.drawable.ic_dialog_info,
                    getString(R.string.reconnected),
                    getString(R.string.reconnected));
        }
        Log.e("WebSokcet Ret.", "Accessed to server!");
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
                if(retryCount > 10 && !isDisconnedtedNoticed){
                    NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_SERVER,
                            CPRSS_BackgroundAccessService.this,
                            android.R.drawable.ic_dialog_alert,
                            getString(R.string.notify_disconnected),
                            getString(R.string.notify_disconnection_detail),
                            getString(R.string.notify_disconnection_detail) + "\n" +
                            getString(R.string.notify_disconnected_context)
                    );
                    isDisconnedtedNoticed = true;
                    retryCount = 0;

                    if(interval < 30)
                        interval += 1;

                    Log.e("WebSocket Ret.", "Server seems temporally unavailable. Wait for " + ((500 + (1000 * 60 * interval)) / 1000.0) + "seconds.");
                }
                Log.e("WebSocket Ret.", "Cannot access to server! trying. attempt " + String.valueOf(retryCount + 1) + "/10");
                try{
                    Thread.sleep(500 + (1000 * 60 * interval));
                } catch (InterruptedException e) {
                    Log.e("WebSocket Ret.", "Couldn't wait enough time due to interruption.");
                }
                wsclient.reconnect();
                retryCount++;
            }
        }).start();
    }

    /**
     * AED使用が開始されたときに通知される。
     * AED-OPENを受信していないノードからAED-POLLINGを受信した場合もこのメソッドが呼ばれる。
     * @param aedInfo AED情報。
     */
    @Override
    public void onAEDUseStarted(AEDInformation aedInfo) {

        Log.e("IgnoreAED", ignoredAEDID.toString());

        if(dbhelper.isAlreadyRegistred(aedInfo.getAed_id())) return;
        if(ignoredAEDID.containsKey(aedInfo.getAed_id())) return;

        Log.e("Current", (locationGetter.getCurrentLocation() == null ? "null" : locationGetter.getCurrentLocation().toString()));
        // 距離で識別する
        if(locationGetter.getCurrentLocation() != null) {
            float req_distance = PreferencesUtility.getCastedFloatValue("maximum_notification_range");

            // 何も入力されていない場合、MainActivity.notification_distanceの値はInteger.MAX_VALUEになる。
            if(req_distance != 0 && req_distance != Integer.MAX_VALUE) {

                float[] results = new float[3];
                Location.distanceBetween(
                        aedInfo.getLatitude(), aedInfo.getLongitude(),
                        locationGetter.getCurrentLocation().getLatitude(), locationGetter.getCurrentLocation().getLongitude(), results);
                if (req_distance < results[0] / 1000) return;
            }
        }


        int unique_id = 48971;

        Intent notify_intent = new Intent(this, AEDLocationActivity.class);
        notify_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notify_intent.setAction(BroadcastConstant.AED_STARTED);
        notify_intent.putExtra("aed-id", aedInfo.getAed_id());
        notify_intent.putExtra("lat", aedInfo.getLatitude());
        notify_intent.putExtra("lon", aedInfo.getLongitude());

        PendingIntent pi = PendingIntent.getActivity(this, 0, notify_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationUtility.NOTIFICATION_CHANNEL_AED_START)
                .setContentIntent(pi)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.notify_aed_open))
                .setContentText(getString(R.string.notify_aed_open_detail))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notify_aed_open_context)));

        NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(unique_id, builder.build());

        sendBroadcast(notify_intent);

        dbhelper.saveData(aedInfo);

        requestDatabaseUpdate();

    }

    @Override
    public void onAEDLocationUpdated(AEDInformation aedInfo){

        Log.e("AEDLocationUpdateLis", aedInfo.toString());
        dbhelper.updateData(aedInfo);

        Intent intent = new Intent();
        intent.setAction(BroadcastConstant.AED_LOCATION_UPDATED);
        intent.putExtra("aed-id", aedInfo.getAed_id());
        intent.putExtra("lat", aedInfo.getLatitude());
        intent.putExtra("lon", aedInfo.getLongitude());
        sendBroadcast(intent);

        requestDatabaseUpdate();

    }

    /**
     * AED使用が完了した際に通知される。
     * @param aedInfo AED情報。
     */
    @Override
    public void onAEDUseFinished(AEDInformation aedInfo) {

        deleteIgnoreAEDID(aedInfo.getAed_id());

        NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_AED_FINISH,
                this,
                android.R.drawable.ic_dialog_info,
                getString(R.string.notify_aed_close),
                getString(R.string.notify_aed_close_detail));
        dbhelper.deleteData(aedInfo.getAed_id());

        Intent intent = new Intent();
        intent.setAction(BroadcastConstant.AED_FINISHED);
        intent.putExtra("aed-id", aedInfo.getAed_id());
        intent.putExtra("lat", aedInfo.getLatitude());
        intent.putExtra("lon", aedInfo.getLongitude());
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

        Log.e("CPRSS", "Error occured in WS_SERVER_ADDRESS", e);
    }


    public void requestDatabaseUpdate(){
        sendBroadcast(new Intent(BroadcastConstant.DATABASE_UPDATED));
    }

    private boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionGranted() {
        return (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    /**
     * 通知が来ても無視する対象となるノードIDを登録する。
     * @param aedid 無視対象とするノードのID。
     */
    public static void addIgnoreAEDID(String aedid){
        ignoredAEDID.put(aedid, new Date());
    }

    /**
     * 無視対象のノードIDを登録解除する。
     * @param aedid 登録解除の対象となるノードのID
     */
    public static void deleteIgnoreAEDID(String aedid){
        ignoredAEDID.remove(aedid);
    }

    /**
     * 登録から10分以上経過している無視対象のノードIDを登録解除する。
     */
    public static void deleteExpiredIgnoreAEDID(){
        Set<String> keys = ignoredAEDID.keySet();

        Log.e("IgnoredAED", ignoredAEDID.toString());

        for(String key : keys){
            Date registredTime = ignoredAEDID.get(key);
            long dayDiff_minute = DateCompareUtility.Diff(new Date(), registredTime) / 1000 / 60;

            if(dayDiff_minute > 10){
                deleteIgnoreAEDID(key);
            }
        }
    }

}

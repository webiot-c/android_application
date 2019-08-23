package com.webiot_c.cprss_notifi_recv;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * アプロケーションを起動したときに表示される最初の画面の挙動を記述する。
 * @author loxygenK
 */
public class MainActivity extends AppCompatActivity implements LocationListener, CPRSS_WebSocketClientListener {

    ////////////////////
    ///// 定数たち /////
    ////////////////////

    /**
     * 位置情報サービスに関する通知チャンネル。
     */
    private static final String NOTIFICATION_CHANNEL_LOCATION   = "cprss_notifychan_loc";

    /**
     * AED使用開始に関する通知チャンネル。
     */
    private static final String NOTIFICATION_CHANNEL_AED_START  = "cprss_notifychan_aedst";

    /**
     * AED使用終了に関する通知チャンネル。
     */
    private static final String NOTIFICATION_CHANNEL_AED_FINISH = "cprss_notifychan_aedfn";

    /**
     * サーバーとの接続に関する通知チャンネル。
     */
    private static final String NOTIFICATION_CHANNEL_SERVER = "cprss_notifychan_svr";

    /**
     *  {重要度, 通知チャンネルのID, 通知チャンネル名, 通知チャンネルの説明}
     *  重要度は、レベルに応じて 1～4を選択する。
     */
    private static final Object[][] NOTIFICATION_CHANNEL_INFORMATIONS = {
            {NotificationManager.IMPORTANCE_HIGH    , NOTIFICATION_CHANNEL_LOCATION   , "位置情報サービス関連の通知", "位置情報サービスの状態に変化が発生したときに通知されます。"},
            {NotificationManager.IMPORTANCE_MAX     , NOTIFICATION_CHANNEL_AED_START  , "AED使用開始通知",            "AEDの使用が始まったときに通知されます。"},
            {NotificationManager.IMPORTANCE_HIGH    , NOTIFICATION_CHANNEL_AED_FINISH , "AED使用終了通知",            "AEDの使用が終了したときに通知されます。"},
            {NotificationManager.IMPORTANCE_DEFAULT , NOTIFICATION_CHANNEL_SERVER     , "サーバー接続状態通知",       "CPRSSサーバーとの接続状態に変化が発生したときに通知します。"}
    };

    //////////////////////////
    ///// フィールドたち /////
    //////////////////////////

    /**
     * 位置情報の取得に必要なシステムサービス。
     */
    private LocationManager locMan;
    /**
     * {@link MainActivity#locMan locMan}により決定された、最も効率的な位置情報の取得方法。
     */
    private String bestProvider;
    /**
     * 現在の位置情報。
     */
    private Location currentLocation;

    /**
     * UIアップデートに使うハンドラ。
     * {@link Handler#post(Runnable)} で投げた{@link Runnable}は、絶対にUIスレッドで実行される。
     */
    private Handler handler;

    /**
     * 現在の位置情報サービスの状態を示す{@link TextView}。
     */
    private TextView status;
    /**
     * 現在の緯度を示す{@link TextView}。making-UIブランチで消える。
     */
    private TextView lon;
    /**
     * 現在の経度を示す{@link TextView}。making-UIブランチで消える。
     */
    private TextView lat;

    /**
     * CPRSSのWebサーバーと通信するときに使うクライアント。
     */
    private CPRSS_WebSocketClient wsclient;

    /**
     * 現在使用中のAED。
     */
    private ArrayList<AEDInformation> aed_infos;
    /**
     * {@link ListView}に{@link AEDInformation}の情報を表示するのに必要な{@link Adapter}。
     */
    private AEDInformationAdapter adapter;

    /**
     * このインスタンス。別のスレッドから操作するときに使う。
     */
    MainActivity virtual_this = this;

    /**
     * {@link MainActivity#onResume()} 時点で、アプリケーションを起動した直後か。
     */
    boolean isStarting = false;

    /**
     * {@link AEDInformation}を保存するデータベースとの通信に使用する
     */
    AEDInformationDatabaseHelper dbhelper;

    ////////////////////
    ///// メソッド /////
    ////////////////////

    // ----- アプリ起動・UI ----- //

    /**
     * アプリケーションを起動した際に呼び出される。
     * @param savedInstanceState アプリケーションを復元するために必要な情報。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        lon    = findViewById(R.id.lon);
        lat    = findViewById(R.id.lat);

        dbhelper = new AEDInformationDatabaseHelper(getApplicationContext());

        aed_infos = new ArrayList<>();
        adapter = new AEDInformationAdapter(this, aed_infos);
        ((ListView)findViewById(R.id.list)).setAdapter(adapter);

        notify(  NOTIFICATION_CHANNEL_AED_START,
                 android.R.drawable.ic_dialog_info,
                "CPRSSが起動しました。",
                "ご協力ありがとうございます。");

        handler = new Handler();

        createNotificationChannel();
        initializeLocationManager();

        startUpdateLocation();
        updateAEDList();

        try {
            wsclient = new CPRSS_WebSocketClient(new URI("ws://cprss-notificator.herokuapp.com/"), virtual_this);
        } catch(Exception e){

        }
        wsclient.connect();
        isStarting = true;

    }

    /**
     * アプリケーションに帰ってきたときに呼び出される。
     */
    public void onResume(){
        super.onResume();
        if(!isStarting) {
            wsclient.reconnect();
            isStarting = false;
        }
    }

    public void updateAEDList(){
        AEDInformation[] aeds = dbhelper.getAEDInformationsFromDatabase();

        aed_infos.clear();
        aed_infos.addAll(Arrays.asList(aeds));

        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }

    // ----- 位置情報関連 ----- //

    /**
     * 位置情報サービスを利用する旨をOSに報告する。
     */
    private void initializeLocationManager(){
        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);

        bestProvider = locMan.getBestProvider(criteria, true);
    }

    /**
     * 指定された権限がユーザーによって許可されているかを確認する。
     * @param permission 権限の名前
     * @return 権限が許可されていた場合は true。
     */
    private boolean isPermissionGranted(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 位置情報関連の権限を確認する。
     */
    private void checkLocationServicePermission() {
        if (!(isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }

    /**
     * 位置情報の取得を開始する。
     */
    private void startUpdateLocation() {
        checkLocationServicePermission();
        locMan.requestLocationUpdates(bestProvider, 60000, 3, this);
    }

    /**
     * 位置情報の取得を終了する。
     */
    private void locationStop() {
        locMan.removeUpdates(this);
    }

    /**
     * 位置情報が更新されたときに呼び出される。
     * @param location 新しい位置情報。
     */
    @Override
    public void onLocationChanged(Location location){
        currentLocation = location;
        lat.setText(String.format(getString(R.string.latitude), currentLocation.getLatitude()));
        lon.setText(String.format(getString(R.string.longitude), currentLocation.getLongitude()));
    }

    /**
     * 位置情報サービスの状態が変化したときに呼び出される。
     * @param provider 対象の位置情報プロバイダー
     * @param service_st 新しいステータス
     * @param extras おそらく、補足情報。
     */
    @Override
    public void onStatusChanged(String provider, int service_st, Bundle extras) {
        switch (service_st) {
            case LocationProvider.AVAILABLE:
                status.setText("");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                status.setText(R.string.locservice_outofservice);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                status.setText(R.string.locservice_temp);
                break;
            default:
                status.setText(R.string.locservice_unknown);
                break;
        }
    }

    /**
     * 位置情報ブロバイダが無効になったときに呼び出される。
     * @param provider 無効になったプロバイダ
     */
    @Override
    public void onProviderDisabled(String provider){
        notify(NOTIFICATION_CHANNEL_LOCATION
                ,android.R.drawable.ic_dialog_info,
                getString(R.string.notify_locservice_disable),
                getString(R.string.notify_locservice_disabled_detail));
    }

    /**
     * 位置情報ブロバイダが有効になったときに呼び出される。
     * @param provider 有効になったプロバイダ
     */
    @Override
    public void onProviderEnabled(String provider){
        notify(  NOTIFICATION_CHANNEL_LOCATION,
                 android.R.drawable.ic_dialog_info,
                getString(R.string.notify_locservice_enabled),
                getString(R.string.notify_locservice_enabled_detail));
    }

    // ----- 通知 ----- //

    /**
     * Android 8 (Oreo／APIレベル26以上) 用に、通知チャンネルを有効化する。
     */
    private void createNotificationChannel() {
        // AndroidのバージョンがOreo以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // NOTIFICATION_CHANNEL_INFORMATIONS のとおりにチャンネルを生成する。
            for(Object[] channelInfo : NOTIFICATION_CHANNEL_INFORMATIONS){
                NotificationChannel channel = new NotificationChannel((String) channelInfo[1], (String) channelInfo[2], (int) channelInfo[0]);
                channel.setDescription((String)channelInfo[3]);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 詳しいメッセージなしで通知を送信する
     * @param channel 通知チャンネル名
     * @param icon_res アイコンのリソースID
     * @param title 通知タイトル
     * @param text 通知内容
     */
    private void notify(String channel, int icon_res, String title, String text){
        notify(channel, icon_res, title, text, null);
    }

    /**
     * 詳しいメッセージ付きで通知を送信する
     * @param channel 通知チャンネル名
     * @param icon_res アイコンのリソースID
     * @param title 通知タイトル
     * @param text 通知内容
     * @param content 通知の詳細な内容
     */
    private void notify(String channel, int icon_res, String title, String text, String content){

        // 通知を識別するためのID。なんでもいい。適当に48971になっている。
        int unique_id = 48971;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(icon_res)
                .setContentTitle(title)
                .setContentText(text);

        if(content != null){
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(unique_id, builder.build());

    }

    // ----- サーバーとの通信 ----- //

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
                    virtual_this.notify(NOTIFICATION_CHANNEL_SERVER,
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
                    Thread.sleep(5000 + (1000 * 60 * interval));
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

        // 距離で識別する
        if(currentLocation != null) {
            String distance_raw = ((EditText) findViewById(R.id.dist)).getText().toString();
            if (!distance_raw.equals("")) {
                double req_distance = Float.parseFloat(distance_raw);

                float[] results = new float[3];
                Location.distanceBetween(
                        aedInfo.getLatitude(), aedInfo.getLongitude(),
                        currentLocation.getLatitude(), currentLocation.getLongitude(), results);
                if (req_distance < results[0] / 1000) return;

            }
        }


        notify(NOTIFICATION_CHANNEL_AED_START,
                android.R.drawable.ic_dialog_info,
                getString(R.string.notify_aed_open),
                getString(R.string.notify_aed_open_detail),
                getString(R.string.notify_aed_open_context));

        dbhelper.saveData(aedInfo);

        updateAEDList();

    }

    /**
     * AED使用が完了した際に通知される。
     * @param aedInfo AED情報。
     */
    @Override
    public void onAEDUseFinished(AEDInformation aedInfo) {
        notify(NOTIFICATION_CHANNEL_AED_FINISH,
                android.R.drawable.ic_dialog_info,
                getString(R.string.notify_aed_close),
                getString(R.string.notify_aed_close_detail));
        dbhelper.deleteData(aedInfo.getAed_id());
        updateAEDList();

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
}

package com.webiot_c.cprss_notifi_recv;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.tan;
import static java.lang.Math.acos;

/**
 * アプロケーションを起動したときに表示される最初の画面の挙動を記述する。
 * @author loxygenK
 */
public class MainActivity extends AppCompatActivity implements LocationListener, CPRSS_WebSocketClientListener {

    private LocationManager locMan;
    private String bestProvider;

    private Handler handler;

    private Location currentLocation;

    private TextView status, lon, lat, ade_lon, ade_lat;
    private Button invoke, close;

    private CPRSS_WebSocketClient wsclient;

    private ArrayList<AEDInformation> aed_infos;
    private AEDInformationAdapter adapter;

    private static final String NOTIFICATION_CHANNEL_LOCATION   = "cprss_notifychan_loc";
    private static final String NOTIFICATION_CHANNEL_AED_START  = "cprss_notifychan_aedst";
    private static final String NOTIFICATION_CHANNEL_AED_FINISH = "cprss_notifychan_aedfn";
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

    MainActivity virtual_this = this;

    boolean isStarting = false;

    /**
     * アプリケーションを起動した際に呼び出される。
     * @param savedInstanceState アプリケーションを復元するために必要な情報。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);  // 位置情報サービスの情報
        lon    = findViewById(R.id.lon);     // 緯度
        lat    = findViewById(R.id.lat);     // 経度

        aed_infos = new ArrayList<>();
        adapter = new AEDInformationAdapter(this, aed_infos);
        ((ListView)findViewById(R.id.list)).setAdapter(adapter);

        notify(  NOTIFICATION_CHANNEL_AED_START,
                 android.R.drawable.ic_dialog_info,
                "CPRSSが起動しました。",
                "ご協力ありがとうございます。");

        createNotificationChannel();
        initializeLocationManager();

        startUpdateLocation();

        try {
            wsclient = new CPRSS_WebSocketClient(new URI("ws://cprss-notificator.herokuapp.com/"), virtual_this);
        } catch(Exception e){

        }
        wsclient.connect();
        isStarting = true;

    }

    public void onResume(){
        super.onResume();
        handler = new Handler();
        if(!isStarting) {
            wsclient.reconnect();
            isStarting = false;
        }
    }


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

    private boolean isPermissionGranted(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermission() {
        if (!(isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }

    private void startUpdateLocation() {
        checkPermission();
        locMan.requestLocationUpdates(bestProvider, 60000, 3, this);
    }

    private void locationStop() {
        locMan.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location){
        currentLocation = location;
        lat.setText("緯度: " + currentLocation.getLatitude());
        lon.setText("経度: " + currentLocation.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int service_st, Bundle extras) {
        switch (service_st) {
            case LocationProvider.AVAILABLE:
                status.setText("位置情報を利用できます。");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                status.setText("圏外です。");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                status.setText("一時的に利用できません。");
                break;
            default:
                status.setText("ステータスが不明です。");
                break;
        }
    }

    String PROV_DISABLED_CHANNEL = "cprss_provdisabled";
    @Override
    public void onProviderDisabled(String provider){


        notify(NOTIFICATION_CHANNEL_LOCATION
                ,android.R.drawable.ic_dialog_info,
                "位置情報サービスが無効になりました。",
                "位置情報サービスが無効の状態だと、通知を受け取れません。\n位置情報サービスの有効化にご協力ください。");
    }

    @Override
    public void onProviderEnabled(String provider){
        notify(  NOTIFICATION_CHANNEL_LOCATION,
                 android.R.drawable.ic_dialog_info,
                "位置情報サービスが有効になりました。",
                "ご協力ありがとうございます。");
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            for(Object[] channelInfo : NOTIFICATION_CHANNEL_INFORMATIONS){
                NotificationChannel channel = new NotificationChannel((String) channelInfo[1], (String) channelInfo[2], (int) channelInfo[0]);
                channel.setDescription((String)channelInfo[3]);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void notify(String channel, int icon_res, String title, String text){
        notify(channel, icon_res, title, text, null);
    }

    private void notify(String channel, int icon_res, String title, String text, String content){

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

    int retryCount = 0;
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        retryCount = 0;
        Log.e("WS", "could access to server!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(retryCount > 10){
                    virtual_this.notify(NOTIFICATION_CHANNEL_SERVER,
                            android.R.drawable.ic_dialog_info,
                            "サーバーとの接続が切断されました。",
                            "あらゆる通知が受信できません。接続の復帰にご協力ください。",
                            "あらゆる通知が受信できません。接続の復帰にご協力ください。\n" +
                                    "サーバー側の問題である可能性がある場合は、開発者にご連絡ください。");
                }
                try{
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.e("WebSocket Ret.", "Couldn't wait enough time due to interruption.");
                }
                Log.e("WebSocket Ret.", "Cannnot access to server! trying. attempt " + String.valueOf(retryCount) + "/10");
                wsclient.reconnect();
                retryCount++;
            }
        }).start();
    }

    @Override
    public void onAEDUseStarted(AEDInformation aedInfo) {

        String distance_raw = ((EditText) findViewById(R.id.dist)).getText().toString();
        if(!distance_raw.equals("")){
            double req_distance = Float.parseFloat(distance_raw);

            float[] results = new float[3];
            Location.distanceBetween(
                    aedInfo.getLatitude(), aedInfo.getLongitude(),
                    currentLocation.getLatitude(), currentLocation.getLongitude(), results);
            if(req_distance < results[0] / 1000) return;

        }


        notify(NOTIFICATION_CHANNEL_AED_START,
                android.R.drawable.ic_dialog_info,
                "AED使用が開始されました。人命救助の応援が必要な可能性があります。",
                "タップして場所を確認してください。",
                "設定した範囲内で、人命救助の応援を必要としている人がいます。\n" +
                         "応援にご協力ください。\n" +
                         "位置をタップして確認してください。");
        aed_infos.add(0, aedInfo);
        ArrayList<AEDInformation> temp_aeds = new ArrayList<>(aed_infos);

        aed_infos.clear();
        aed_infos.addAll(temp_aeds);

        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }


    @Override
    public void onAEDUseFinished(AEDInformation aedInfo) {
        notify(NOTIFICATION_CHANNEL_AED_FINISH,
                android.R.drawable.ic_dialog_info,
                "AED使用が終了しました。",
                "これ以上応援は必要ないと思われます。ご協力ありがとうございました。");
        for(int i = 0;i<aed_infos.size();i++) {
            if (aed_infos.get(i).getAed_id().equals(aedInfo.getAed_id())) {
                aed_infos.remove(i);
                break;
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onOtherMessage(String message) {
        Log.e("CPRSS", "Unparsed Message: " + message);

    }

    @Override
    public void onError(Exception e) {

        Log.e("CPRSS", "Error occured in WS", e);
    }
}

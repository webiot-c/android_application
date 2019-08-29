package com.webiot_c.cprss_notifi_recv.app;

import android.app.ActivityManager;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.TextView;

import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.app.service.CPRSS_BackgroundAccessService;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationAdapter;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;
import com.webiot_c.cprss_notifi_recv.utility.LocationGetter;
import com.webiot_c.cprss_notifi_recv.utility.NotificationUtility;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * アプロケーションを起動したときに表示される最初の画面の挙動を記述する。
 * @author loxygenK
 */
public class MainActivity extends AppCompatActivity {

    ////////////////////////////////////////////
    // MainActivityに付随するBroacastReceiver //
    ////////////////////////////////////////////
    public class DatabaseUpdateReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            updateUIProperties();
        }
    }

    //////////////////////////
    ///// フィールドたち /////
    //////////////////////////

    /**
     * UIアップデートに使うハンドラ。
     * {@link Handler#post(Runnable)} で投げた{@link Runnable}は、絶対にUIスレッドで実行される。
     */
    private Handler handler;

    /**
     * 現在使用中のAED。
     */
    private ArrayList<AEDInformation> aed_infos;
    /**
     * {@link ListView}に{@link AEDInformation}の情報を表示するのに必要な{@link Adapter}。
     */
    private AEDInformationAdapter adapter;

    /**
     * {@link AEDInformation}を保存するデータベースとの通信に使用する
     */
    AEDInformationDatabaseHelper dbhelper;

    /**
     * バックグラウンドサービスの実行管理に使用される。
     */
    JobScheduler js;

    BroadcastReceeiveManager broadcastReceeiveManager;
    DatabaseUpdateReceiver databaseUpdateReceiver;

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

        dbhelper = new AEDInformationDatabaseHelper(getApplicationContext());

        aed_infos = new ArrayList<>();
        adapter = new AEDInformationAdapter(this, aed_infos);
        ((ListView)findViewById(R.id.list)).setAdapter(adapter);

        handler = new Handler();

        NotificationUtility.deleteNotificationChannel(this);
        NotificationUtility.createNotificationChannel(this);

        LocationGetter.checkLocationServicePermission(this);

        startConnectionService();

    }

    /**
     * アプリケーションに帰ってきたときに呼び出される。
     */
    public void onResume(){
        super.onResume();
        js = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);

        updateUIProperties();
        registerBroadcastReceivers();
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterBroadcastReceivers();
    }

    public void updateUIProperties(){
        AEDInformation[] aeds = dbhelper.getAEDInformationsFromDatabase();

        aed_infos.clear();
        aed_infos.addAll(Arrays.asList(aeds));

        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

        int forecol, backcol;

        if(aeds.length > 0){
            forecol = getResources().getColor(R.color.aed_enabled_fore);
            backcol = getResources().getColor(R.color.aed_enabled);
        } else {
            forecol = getResources().getColor(R.color.noaedinfo_fore);
            backcol = getResources().getColor(R.color.noaedinfo);
        }

        findViewById(R.id.title_area).setBackgroundColor(backcol);
        ((TextView)findViewById(R.id.service_name)).setTextColor(forecol);
        ((TextView)findViewById(R.id.app_title)).setTextColor(forecol);

    }


    // ----- サービス連携 ----- //

    /**
     * サービスが起動していなかった場合、サービスを起動する
     */
    public void startConnectionService(){

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CPRSS_BackgroundAccessService.class.getName().equals(serviceInfo.service.getClassName())) {
                // 実行中なら起動しない
                return;
            }
        }

        Intent intent = new Intent(this, CPRSS_BackgroundAccessService.class);

        // AndroidのバージョンがOreo以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android Oreo以上と以下で処理を変えなければならない。
            startForegroundService(intent);
        } else {
            startService(intent);
        }

    }

    /**
     * サービスを停止する。本プログラムでは永続的にサービスを実行し続けるため、使用していない
     */
    public void stopConnectionService(){
        // こっちはいいらしい。
        stopService(new Intent(this, CPRSS_BackgroundAccessService.class));
    }

    /**
     * {@link BroadcastReceiver}を登録する
     */
    public void registerBroadcastReceivers(){


        databaseUpdateReceiver = new DatabaseUpdateReceiver();
        IntentFilter dur_filter = new IntentFilter();

        dur_filter.addAction("DATABASE_UPDATED");
        registerReceiver(databaseUpdateReceiver, dur_filter);


    }

    /**
     * {@link MainActivity#registerBroadcastReceivers()}で登録した{@link BroadcastReceiver}を登録解除する
     */
    public void unregisterBroadcastReceivers(){
        unregisterReceiver(databaseUpdateReceiver);
    }
}

package com.webiot_c.cprss_notifi_recv.app;

import android.Manifest;
import android.app.ActivityManager;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.app.service.CPRSS_BackgroundAccessService;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformation;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationAdapter;
import com.webiot_c.cprss_notifi_recv.data_struct.AEDInformationDatabaseHelper;
import com.webiot_c.cprss_notifi_recv.utility.NotificationUtility;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * アプロケーションを起動したときに表示される最初の画面の挙動を記述する。
 * @author loxygenK
 */
public class MainActivity extends AppCompatActivity implements TextWatcher, View.OnClickListener {

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

    Messenger mServiceMessenger;

    public static float notification_distance;

    SharedPreferences sharedPreferences;

    Thread uiUpdateThread;

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

        sharedPreferences = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        dbhelper = AEDInformationDatabaseHelper.getInstance(getApplicationContext());

        NotificationUtility.createNotificationChannel(this);

        requestPermission();

        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_main);

        ((EditText) findViewById(R.id.dist)).setText(String.valueOf(sharedPreferences.getFloat("Notification_Distance", 0)));

        final RecyclerView list = findViewById(R.id.list);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        aed_infos = new ArrayList<>();
        adapter = new AEDInformationAdapter(this, aed_infos, this);

        list.setHasFixedSize(true);
        list.addItemDecoration(itemDecoration);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {

                return false;

            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                int swipedPosition = viewHolder.getAdapterPosition();
                AEDInformationAdapter adapter = (AEDInformationAdapter) list.getAdapter();
                AEDInformation deleted = adapter.remove(swipedPosition);

                dbhelper.deleteData(deleted.getAed_id());

                adapter.notifyItemRemoved(swipedPosition);

                updateUIProperties();

            }
        };

        (new ItemTouchHelper(callback)).attachToRecyclerView(list);

        handler = new Handler();

        ((FloatingActionButton)findViewById(R.id.updateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIProperties();
            }
        });

        ((EditText) findViewById(R.id.dist)).addTextChangedListener(this);




    }

    @Override
    public void onClick(View v) {

        AEDInformation aed_Info = dbhelper.getAEDInformation(((TextView)v.findViewById(R.id.adeid)).getText().toString());

        Intent intent = new Intent(this, AEDLocationActivity.class);
        intent.putExtra("aed-id", aed_Info.getAed_id());
        intent.putExtra("lat", aed_Info.getLatitude());
        intent.putExtra("lon", aed_Info.getLongitude());

        startActivity(intent);
    }


    /**
     * 指定された権限がユーザーによって許可されているかを確認する。
     * @param permission 権限の名前
     * @return 権限が許可されていた場合は true。
     */
    private boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionGranted() {
        return (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    /**
     * 位置情報関連の権限を確認する。
     */
    public void checkLocationServicePermission() {
        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }

    /**
     * 権限をユーザーに許諾してもらえるように頑張って、ダメだったらアプリケーションを終了する。
     */
    public void requestPermission() {
        if (!isLocationPermissionGranted()) {

            checkLocationServicePermission();

        } else {
            startConnectionService();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1000: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startConnectionService();
                } else {
                    this.finish();
                }
            }
        }
    }

    /**
     * アプリケーションに帰ってきたときに呼び出される。
     */
    public void onResume(){
        super.onResume();
        js = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);

        updateUIProperties();
        registerBroadcastReceivers();

        if(uiUpdateThread != null &&uiUpdateThread.isAlive()) uiUpdateThread.interrupt();

        uiUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUIProperties();
                        }
                    });

                    try {
                        Thread.sleep(1000 * 15);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

            }
        });

        uiUpdateThread.start();

    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterBroadcastReceivers();
        if(uiUpdateThread.isAlive()) uiUpdateThread.interrupt();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(uiUpdateThread.isAlive()) uiUpdateThread.interrupt();

    }

    public void updateUIProperties(){
        AEDInformation[] aeds = dbhelper.getAEDInformationsFromDatabase();

        aed_infos.clear();
        aed_infos.addAll(Arrays.asList(aeds));

        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                adapter.updateList(aed_infos);
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        float distance = Float.MAX_VALUE;
        if(s.length() > 0){
            String raw_stsring = s.toString();
            try {
                distance = Float.valueOf(raw_stsring);
            } catch (NumberFormatException e){
                return;
            }
        }
        notification_distance = distance;

        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putFloat("Notification_Distance", notification_distance);
        editor.apply();

    }

    // ----- サービス管理 ----- //

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
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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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


        findViewById(R.id.mainui_setting_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        final RecyclerView list = findViewById(R.id.list);
        final RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
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

                CPRSS_BackgroundAccessService.addIgnoreAEDID(deleted.getAed_id());
                updateUIProperties();

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;

                    Paint p = new Paint();
                    Drawable d = getResources().getDrawable(R.drawable.ic_delete, null);

                    if (dX > 0) {
                        p.setARGB(255, 255, 0, 0);
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), p);

                        p.setARGB(255, 255, 255, 255);

                        d.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + itemView.getHeight(), itemView.getBottom());

                    }
                    else {
                        p.setARGB(255, 255, 0, 2);
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), p);
                        p.setARGB(255, 255, 255, 255);
                        c.drawText("削除", (float) itemView.getLeft(), (float) itemView.getTop(), p);
                        d.setBounds(itemView.getRight()  - itemView.getHeight(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    }

                    d.draw(c);

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

        };

        (new ItemTouchHelper(callback)).attachToRecyclerView(list);

        handler = new Handler();

        final SwipeRefreshLayout refresh = ((SwipeRefreshLayout)findViewById(R.id.refresh));
        Resources res = getResources();
        refresh.setColorSchemeColors(res.getColor(R.color.red), res.getColor(R.color.green), res.getColor(R.color.blue), res.getColor(R.color.yellow));
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateUIProperties();
                refresh.setRefreshing(false);
            }
        });


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
                        Thread.sleep(1000 * 5);
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

        CPRSS_BackgroundAccessService.deleteExpiredIgnoreAEDID();

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
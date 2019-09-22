package com.webiot_c.cprss_notifi_recv.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.app.service.CPRSS_BackgroundAccessService;

public class BroadcastReceeiveManager extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Recv", intent.getAction());
        if(isBootCompleted(context, intent) || isApplicationUpdated(context, intent)){
            Intent service_intent = new Intent(context, CPRSS_BackgroundAccessService.class);

            // AndroidのバージョンがOreo以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android Oreo以上と以下で処理を変えなければならない。
                context.startForegroundService(service_intent);
            } else {
                context.startService(service_intent);
            }
        }
    }

    public static boolean isBootCompleted(Context context, Intent intent){
        if(context == null){
            throw new IllegalArgumentException("Context must not be null.");
        }
        if(intent == null){
            throw new IllegalArgumentException("Intent must not be null.");
        }
        String action = intent.getAction();
        return Intent.ACTION_BOOT_COMPLETED.equals(action);
    }

    public static boolean isApplicationUpdated(Context context, Intent intent){
        if(context == null){
            throw new IllegalArgumentException("Context must not be null.");
        }
        if(intent == null){
            throw new IllegalArgumentException("Intent must not be null.");
        }
        String action = intent.getAction();
        String pack = intent.getDataString();
        return Intent.ACTION_PACKAGE_REPLACED.equals(action) &&
                pack.equals("package:" + context.getPackageName());
    }
}

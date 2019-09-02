package com.webiot_c.cprss_notifi_recv.utility;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.webiot_c.cprss_notifi_recv.R;

public class NotificationUtility {


    ////////////////////
    ///// 定数たち /////
    ////////////////////

    /**
     * 位置情報サービスに関する通知チャンネル。
     */
    public static final String NOTIFICATION_CHANNEL_LOCATION   = "cprss_notifychan_loc";

    /**
     * AED使用開始に関する通知チャンネル。
     */
    public static final String NOTIFICATION_CHANNEL_AED_START  = "cprss_notifychan_aedst";

    /**
     * AED使用終了に関する通知チャンネル。
     */
    public static final String NOTIFICATION_CHANNEL_AED_FINISH = "cprss_notifychan_aedfn";

    /**
     * サーバーとの接続に関する通知チャンネル。
     */
    public static final String NOTIFICATION_CHANNEL_SERVER = "cprss_notifychan_svr";

    /**
     * バックグラウンドサービスの通知。
     */
    public static final String NOTIFICATION_CHANNEL_BACKGROUND = "cprss_notifychan_background";

    /**
     *  {重要度, 通知チャンネルのID, 通知音リソースID, 通知チャンネル名, 通知チャンネルの説明}
     *  重要度は、レベルに応じて 1～4を選択する。
     */
    public static final Object[][] NOTIFICATION_CHANNEL_INFORMATIONS = {
            {NotificationManager.IMPORTANCE_HIGH    , NOTIFICATION_CHANNEL_LOCATION   , 0,                  "位置情報サービス関連の通知", "位置情報サービスの状態に変化が発生したときに通知されます。"},
            {NotificationManager.IMPORTANCE_MAX     , NOTIFICATION_CHANNEL_AED_START  , R.raw.aed_started,  "AED使用開始通知",            "AEDの使用が始まったときに通知されます。"},
            {NotificationManager.IMPORTANCE_HIGH    , NOTIFICATION_CHANNEL_AED_FINISH , R.raw.aed_finished, "AED使用終了通知",            "AEDの使用が終了したときに通知されます。"},
            {NotificationManager.IMPORTANCE_DEFAULT , NOTIFICATION_CHANNEL_SERVER     , 0,                  "サーバー接続状態通知",       "CPRSSサーバーとの接続状態に変化が発生したときに通知します。"},
            {NotificationManager.IMPORTANCE_LOW     , NOTIFICATION_CHANNEL_BACKGROUND , 0,                  "実行確認通知",               "このアプリケーションでは、常に通知を受信できるよう、バックグラウンドでで通信を行っています。\n通信が行われている間、この通知が表示されています。"}
    };



    /**
     * デバッグ目的で、一度チャンネルを全て消去する必要がある際に呼び出す。通常使わない。
     */
    public static void deleteNotificationChannel(Context context) {
        // AndroidのバージョンがOreo以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            for (Object[] channelInfo : NOTIFICATION_CHANNEL_INFORMATIONS) {
                nManager.deleteNotificationChannel((String)channelInfo[1]);
            }
            Toast.makeText(context.getApplicationContext(), "通知チャンネルがすべて削除されました。", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Android 8 (Oreo／APIレベル26以上) 用に、通知チャンネルを有効化する。
     * Android 8 未満の場合は、何も実行されない。
     */
    public static void createNotificationChannel(Context context) {
        // AndroidのバージョンがOreo以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // NOTIFICATION_CHANNEL_INFORMATIONS のとおりにチャンネルを生成する。
            for(Object[] channelInfo : NOTIFICATION_CHANNEL_INFORMATIONS){
                NotificationChannel channel = new NotificationChannel((String) channelInfo[1], (String) channelInfo[3], (int) channelInfo[0]);
                channel.setDescription((String)channelInfo[4]);

                if((int)channelInfo[2] != 0){
                    AudioAttributes attr = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    AssetManager act = context.getResources().getAssets();

                    Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + (int)channelInfo[2]);
                    channel.setSound(uri, attr);
                }

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
    public static void notify(String channel, Context context, int icon_res, String title, String text){
        notify(channel, context, icon_res, title, text, null);
    }

    /**
     * 詳しいメッセージ付きで通知を送信する
     * @param channel 通知チャンネル名
     * @param icon_res アイコンのリソースID
     * @param title 通知タイトル
     * @param text 通知内容
     * @param content 通知の詳細な内容
     */
    public static void notify(String channel, Context context, int icon_res, String title, String text, String content){

        // 通知を識別するためのID。なんでもいい。適当に48971になっている。
        int unique_id = 48971;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(icon_res)
                .setContentTitle(title)
                .setContentText(text);

        if(content != null){
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(unique_id, builder.build());

    }

}

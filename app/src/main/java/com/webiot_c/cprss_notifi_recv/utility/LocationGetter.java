package com.webiot_c.cprss_notifi_recv.utility;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.R;

import static android.content.Context.LOCATION_SERVICE;

public class LocationGetter implements LocationListener {

    public interface LocationStatusChangedListener {
        void onLocationChanged(Location location);
        void onStatusChanged(int statusCode);
    }

    LocationManager locationManager;
    String bestProvider;

    Context context;

    Location currentLocation;

    public LocationGetter(Context context){
        initializeLocationManager(context);
        startUpdateLocation();
        this.context = context;
    }

    /**
     * 位置情報サービスを利用する旨をOSに報告する。
     */
    private void initializeLocationManager(Context context){
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);

        bestProvider = locationManager.getBestProvider(criteria, true);
    }

    /**
     * 指定された権限がユーザーによって許可されているかを確認する。
     * @param permission 権限の名前
     * @return 権限が許可されていた場合は true。
     */
    private static boolean isPermissionGranted(String permission, Activity activity){
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 位置情報関連の権限を確認する。
     */
    public static void checkLocationServicePermission(Activity activity) {
        if (!(isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, activity) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, activity))) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
    }
    /**
     * 位置情報ブロバイダが無効になったときに呼び出される。
     * @param provider 無効になったプロバイダ
     */
    @Override
    public void onProviderDisabled(String provider){
        NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_LOCATION,
                context,
                android.R.drawable.ic_dialog_info,
                context.getString(R.string.notify_locservice_disable),
                context.getString(R.string.notify_locservice_disabled_detail));
    }

    /**
     * 位置情報ブロバイダが有効になったときに呼び出される。
     * @param provider 有効になったプロバイダ
     */
    @Override
    public void onProviderEnabled(String provider){
        NotificationUtility.notify(NotificationUtility.NOTIFICATION_CHANNEL_LOCATION,
                context,
                android.R.drawable.ic_dialog_info,
                context.getString(R.string.notify_locservice_enabled),
                context.getString(R.string.notify_locservice_enabled_detail));
    }

    /**
     * 位置情報の取得を開始する。
     */
    public void startUpdateLocation() {
        locationManager.requestLocationUpdates(bestProvider, 60000, 3, this);
    }

    /**
     * 位置情報の取得を終了する。
     */
    public void stopUpdateLocation() {
        locationManager.removeUpdates(this);
    }

    // ----- 位置情報関連 ----- //
    @Override
    public void onLocationChanged(Location location) {
        Log.e("LocationCHanged", "Location is now changed.");
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public Location getCurrentLocation(){
        return currentLocation;
    }

}

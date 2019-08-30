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

    LocationManager locationManager;
    String bestProvider;

    Context context;

    Location currentLocation;

    public LocationGetter(Context context) {
        this.context = context;

        initializeLocationManager(context);
        startUpdateLocation();
    }

    /**
     * 位置情報サービスを利用する旨をOSに報告する。
     */
    private void initializeLocationManager(Context context) {
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
     * 位置情報ブロバイダが無効になったときに呼び出される。
     * @param provider 無効になったプロバイダ
     */
    @Override
    public void onProviderDisabled(String provider) {
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
    public void onProviderEnabled(String provider) {
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

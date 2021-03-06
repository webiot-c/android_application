package com.webiot_c.cprss_notifi_recv.utility;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
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
import com.webiot_c.cprss_notifi_recv.app.service.CPRSS_BackgroundAccessService;

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
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

        bestProvider = locationManager.getBestProvider(criteria, true);
        Log.v("LocationGetter", "Best provider is " + bestProvider);

    }
    /**
     * 位置情報ブロバイダが無効になったときに呼び出される。
     * @param provider 無効になったプロバイダ
     */
    @Override
    public void onProviderDisabled(String provider) {
        CPRSS_BackgroundAccessService.updateStatus(context, CPRSS_BackgroundAccessService.LOCATION_SERVICE_DISABLED, true);
    }

    /**
     * 位置情報ブロバイダが有効になったときに呼び出される。
     * @param provider 有効になったプロバイダ
     */
    @Override
    public void onProviderEnabled(String provider) {
        CPRSS_BackgroundAccessService.updateStatus(context, CPRSS_BackgroundAccessService.LOCATION_SERVICE_DISABLED, false);
    }

    /**
     * 位置情報の取得を開始する。
     */
    @SuppressLint("MissingPermission") // ← 権限はこのメソッドが呼び出される前に保障されている。。
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
        Log.v("LocationCHanged", "Location is now changed.");
        currentLocation = location;
    }

    /* API29 からは呼び出されない */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public Location getCurrentLocation(){
        if(!isLocationAvailable())
            throw new IllegalStateException("位置情報が最新ではありません。");
        else
            return currentLocation;
    }

    /**
     * 信頼性とかを無視して、とにかく最後に取得した位置情報を取得する。{@link LocationGetter#getCurrentLocation()}を使おう
     * @return 最後に取得した位置情報
     */
    @Deprecated
    public Location getLatestCurrentLocation(){
        return currentLocation;
    }

    public boolean isLocationAvailable(){
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}

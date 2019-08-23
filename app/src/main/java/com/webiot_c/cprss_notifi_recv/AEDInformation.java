package com.webiot_c.cprss_notifi_recv;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Random;

public class AEDInformation {

    private String aed_id;
    private double lat;
    private double lon;

    private long unique_id;

    public AEDInformation(String aed_id, double latitude, double longitude){
        this.aed_id = aed_id;
        this.lat = latitude;
        this.lon = longitude;
        unique_id = new Random().nextLong();
    }

    public String getAed_id() {
        return aed_id;
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public long getUniqueID(){
        return unique_id;
    }

    public String toString(){
        return String.format("{AEDInformation: aedid: %s, lat: %f, lon: %f}", aed_id, lat, lon);
    }

}

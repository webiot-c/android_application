package com.webiot_c.cprss_notifi_recv.data_struct;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.webiot_c.cprss_notifi_recv.utility.DateCompareUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class AEDInformation {

    private String aed_id;
    private double lat;
    private double lon;
    private Date received_date;

    private long unique_id;

    public AEDInformation(String aed_id, double latitude, double longitude, Date received_date){
        this.aed_id = aed_id;
        this.lat = latitude;
        this.lon = longitude;
        this.received_date = (Date)received_date.clone();
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

    public void setLatitude(double lat){
        this.lat = lat;
    }

    public void setLongitude(double lon){
        this.lon = lon;
    }

    public Date getReceivedDate() {
        return received_date;
    }

    public void setReceivedDate(Date received_date) {
        this.received_date = received_date;
    }


    public boolean isReceivedDateInDuration(Date testDate, long durationSeconds){

        long dayDiff = Math.abs(DateCompareUtility.Diff(new Date(), testDate));

        return (dayDiff / 1000) <= durationSeconds;

    }

    public long getUniqueID(){
        return unique_id;
    }

    public String toString(){
        return String.format("{AEDInformation: aedid: %s, lat: %f, lon: %f}", aed_id, lat, lon);
    }

}

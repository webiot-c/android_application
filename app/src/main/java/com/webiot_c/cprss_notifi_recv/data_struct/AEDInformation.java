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

    /**
     * 受信してからの時間の状態。
     */
    enum DurationStatus {

        /**
         * 受信してからの時間が極めて短い。
         */
        VERY_SHORT,

        /**
         * 受信してから少し経っている。
         */
        FEW_LATE,

        /**
         * かなり経っている。
         */
        LATE


    }

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


    public DurationStatus isReceivedDateInDuration(Date testDate, long very_short, long few_late){

        long dayDiff = Math.abs(DateCompareUtility.Diff(testDate, received_date));
        dayDiff = dayDiff / 1000;

        if (dayDiff < very_short){
            return DurationStatus.VERY_SHORT;
        } else if(dayDiff < few_late) {
            return DurationStatus.FEW_LATE;
        } else {
            return DurationStatus.LATE;
        }
    }

    public long getUniqueID(){
        return unique_id;
    }

    public String toString(){
        return String.format("{AEDInformation: aedid: %s, lat: %f, lon: %f}", aed_id, lat, lon);
    }

}

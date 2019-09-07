package com.webiot_c.cprss_notifi_recv.utility;

import android.util.Log;

import java.util.Date;

public class DateCompareUtility {

    public static long Diff(Date a, Date b){
        long long_aedDate = a.getTime();
        long long_testDate = b.getTime();

        long dayDiff = Math.abs(long_aedDate - long_testDate);

        Log.e("AED Duration", "[A]: " + a.toString());
        Log.e("AED Duration", "[B]: " + b.toString());
        Log.e("AED Duration", "Dif: " + dayDiff);

        return dayDiff;

    }

}

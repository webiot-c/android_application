package com.webiot_c.cprss_notifi_recv.utility;

import android.util.Log;

import java.util.Date;

public class DateCompareUtility {

    /**
     * 2つの[ateインスタンスが表す日時の差をms単位で返す。
     * @param a 比較対象のDate
     * @param b 比較対象のDate
     * @return 日時の差(ms単位)。
     */
    public static long Diff(Date a, Date b){
        long long_aedDate = a.getTime();
        long long_testDate = b.getTime();

        long dayDiff = long_aedDate - long_testDate;

        Log.e("AED Duration", "[A]: " + a.toString());
        Log.e("AED Duration", "[B]: " + b.toString());
        Log.e("AED Duration", "Dif: " + dayDiff);

        return dayDiff;

    }

}

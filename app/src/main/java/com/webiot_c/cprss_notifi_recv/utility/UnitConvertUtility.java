package com.webiot_c.cprss_notifi_recv.utility;

import android.content.Context;
import android.util.DisplayMetrics;

public class UnitConvertUtility {

    /**
     * dpからpixelへの変換
     * @param dp
     * @param context
     * @return float pixel
     */
    public static float convertDp2Px(float dp, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * metrics.density;
    }

}

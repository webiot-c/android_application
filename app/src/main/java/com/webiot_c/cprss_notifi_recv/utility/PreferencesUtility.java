package com.webiot_c.cprss_notifi_recv.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.webiot_c.cprss_notifi_recv.R;

public class PreferencesUtility {

    public static final String MAXIMUM_NOTIFICATION_RANGE = "maximum_notification_range";
    public static final String VERY_SHORT_DURATION = "very_short_duration";
    public static final String FEW_LATE_DURATION = "few_late_duration";
    public static final String LATE_DURATION = "late_duration";

    static Context appContext;
    static SharedPreferences preferences;

    public static void initialize(Context context){
        appContext = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public static String getStringValue(String key, String defaultValue){
        return preferences.getString(key, defaultValue);
    }

    public static boolean getBooleanValue(String key, boolean defaultValue){
        return preferences.getBoolean(key, defaultValue);
    }

    public static int getCastedIntValue(String key, int defaultValue){
        String tmp_value = preferences.getString(key, String.valueOf(defaultValue));
        return Integer.parseInt(tmp_value);
    }
    public static float getCastedFloatValue(String key, float defaultValue){
        String tmp_value = preferences.getString(key, String.valueOf(defaultValue));
        return Float.parseFloat(tmp_value);
    }
    public static long getCastedLongValue(String key, long defaultValue){
        String tmp_value = preferences.getString(key, String.valueOf(defaultValue));
        return Long.parseLong(tmp_value);
    }

    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedIntValue(String, int)}を使おう
     */
    @Deprecated
    public static int getIntValue(String key, int defaultValue){
        return preferences.getInt(key, defaultValue);
    }
    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedFloatValue(String, float)}を使おう
     */
    @Deprecated
    public static float getFloatValue(String key, float defaultValue){
        return preferences.getFloat(key, defaultValue);
    }
    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedLongValue(String, long)}を使おう
     */
    @Deprecated
    public static long getLongValue(String key, long defaultValue){
        return preferences.getLong(key, defaultValue);
    }

}

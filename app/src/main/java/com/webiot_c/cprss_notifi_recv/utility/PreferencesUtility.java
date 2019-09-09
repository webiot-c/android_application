package com.webiot_c.cprss_notifi_recv.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class PreferencesUtility{

    public static class PreferenceInfo{

        public static class CustomTextGenerator{

            public String getCustomText(Object value, String unit, String summary){
                String tmp_summary = String.format("( %s )", summary);
                if(summary.equals("")) { tmp_summary = ""; }

                return String.format("%s %s %s", value, unit, tmp_summary);
            }

        }

        private String preferencekey;
        private String unit;
        private Object defaultvalue;
        private int summary_resource;
        private CustomTextGenerator customTextGenerator;

        public PreferenceInfo(String preferencekey, Object defaultValue, String unit, int summary_resource) {
            this(preferencekey, defaultValue, unit, summary_resource, new CustomTextGenerator());
        }

        public PreferenceInfo(String preferencekey, Object defaultValue, String unit, int summary_resource, CustomTextGenerator ctg) {
            this.preferencekey = preferencekey;
            this.unit = unit;
            this.summary_resource = summary_resource;
            this.customTextGenerator = ctg;
            this.defaultvalue = defaultValue;
        }

        public String getPreferenceKey() {
            return preferencekey;
        }

        public String getUnit() {
            return unit;
        }

        public String getFormattedText(Resources resources, Object value){
            String summary = (summary_resource != 0) ? resources.getString(summary_resource) : "";
            return customTextGenerator.getCustomText(value, unit, summary);
        }

        public int getSummaryResourceID() {
            return summary_resource;
        }

        public Object getDefaultValue(){
            return defaultvalue;
        }

        public void setCustomTextGenerator(@NonNull CustomTextGenerator ctg){
            this.customTextGenerator = ctg;
        }

        @NonNull
        @Override
        public String toString() {
            return preferencekey;
        }
    }

    private static final Map<String, PreferenceInfo> preferenceInfos = new HashMap<>();
    public static final String MAXIMUM_NOTIFICATION_RANGE = "maximum_notification_range";
    public static final String VERY_SHORT_DURATION = "very_short_duration";
    public static final String FEW_LATE_DURATION = "few_late_duration";
    public static final String LATE_DURATION = "late_duration";

    static Context appContext;
    static SharedPreferences preferences;

    public static void initialize(Context context){
        appContext = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        preferenceInfos.put(VERY_SHORT_DURATION , new PreferenceInfo(VERY_SHORT_DURATION ,2, "min.", 0));
        preferenceInfos.put(FEW_LATE_DURATION   , new PreferenceInfo(FEW_LATE_DURATION   ,3, "min.", 0));
        preferenceInfos.put(LATE_DURATION       , new PreferenceInfo(LATE_DURATION       ,7, "min.", 0));

        preferenceInfos.put(MAXIMUM_NOTIFICATION_RANGE,
                new PreferenceInfo(MAXIMUM_NOTIFICATION_RANGE, 2.0f, "Km"  , 0,
                    new PreferenceInfo.CustomTextGenerator(){
                        @Override
                        public String getCustomText(Object value, String unit, String summary) {

                            Float typed_value = Float.valueOf((String)value);

                            String tmp_value = String.valueOf(typed_value);
                            if(typed_value == 0 ){
                                tmp_value = "無制限"; unit = "";
                            }
                            return super.getCustomText(tmp_value, unit, summary);
                        }
                    })
        );

    }

    public static PreferenceInfo getPreferenceInfo(String key){
        return preferenceInfos.get(key);
    }

    public static PreferenceInfo[] getPrefreInfos(){
        return preferenceInfos.values().toArray(new PreferenceInfo[preferenceInfos.size()]);
    }

    public static String getStringValue(PreferenceInfo preferenceInfo){
        String key = preferenceInfo.getPreferenceKey();
        Object defaultValue = preferenceInfo.getDefaultValue();

        return preferences.getString(key, (String)defaultValue);
    }

    public static boolean getCastedBooleanValue(PreferenceInfo preferenceInfo){
        String key = preferenceInfo.getPreferenceKey();
        Object defaultValue = preferenceInfo.getDefaultValue();

        String raw_value = preferences.getString(key, String.valueOf(defaultValue));
        return Boolean.valueOf(raw_value);
    }

    public static int getCastedIntValue(PreferenceInfo preferenceInfo){
        String key = preferenceInfo.getPreferenceKey();
        Object defaultValue = preferenceInfo.getDefaultValue();

        String raw_value = preferences.getString(key, String.valueOf(defaultValue));
        return Integer.valueOf(raw_value);
    }
    public static float getCastedFloatValue(PreferenceInfo preferenceInfo){
        String key = preferenceInfo.getPreferenceKey();
        Object defaultValue = preferenceInfo.getDefaultValue();

        String raw_value = preferences.getString(key, String.valueOf(defaultValue));
        return Float.valueOf(raw_value);
    }
    public static long getCastedLongValue(PreferenceInfo preferenceInfo){
        String key = preferenceInfo.getPreferenceKey();
        Object defaultValue = preferenceInfo.getDefaultValue();

        String raw_value = preferences.getString(key, String.valueOf(defaultValue));
        return Long.valueOf(raw_value);
    }
    public static String getStringValue(String key){
        return getStringValue(getPreferenceInfo(key));
    }

    public static boolean getCastedBooleanValue(String key){
        return getCastedBooleanValue(getPreferenceInfo(key));
    }

    public static int getCastedIntValue(String key){
        return getCastedIntValue(getPreferenceInfo(key));
    }
    public static float getCastedFloatValue(String key){
        return getCastedFloatValue(getPreferenceInfo(key));
    }
    public static long getCastedLongValue(String key){
        return getCastedLongValue(getPreferenceInfo(key));
    }

    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedIntValue(String)}を使おう
     */
    @Deprecated
    public static int getIntValue(String key, int defaultValue){
        return preferences.getInt(key, defaultValue);
    }
    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedFloatValue(String)}を使おう
     */
    @Deprecated
    public static float getFloatValue(String key, float defaultValue){
        return preferences.getFloat(key, defaultValue);
    }
    /**
     * このソフトウェアでは、数値型もまとめてStringにキャストされてしまう(PreferenceScreenの都合上)ので、{@link PreferencesUtility#getCastedLongValue(String)}を使おう
     */
    @Deprecated
    public static long getLongValue(String key, long defaultValue){
        return preferences.getLong(key, defaultValue);
    }

}

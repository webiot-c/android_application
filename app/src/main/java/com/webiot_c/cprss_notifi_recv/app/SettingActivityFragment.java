package com.webiot_c.cprss_notifi_recv.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.utility.PreferencesUtility;

import java.util.Set;

public class SettingActivityFragment extends PreferenceFragment {

    private static final String PREF_KEY_TEXT = PreferencesUtility.MAXIMUM_NOTIFICATION_RANGE;
    private static final String PREF_KEY_SCREEN_COLOR = "screen_color";
    private static final String PREF_KEY_DISPLAY_IMAGES = "display_images";

    private static final String PREF_KEY_ENABLE_OTHER_SETTINGS = "enable_other_settings";
    private static final String PREF_KEY_OTHER_SETTINGS = "other_settings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // PreferenceScreenリソースを追加する
        addPreferencesFromResource(R.xml.preferences);

        // テキストが変更されたらサマリも更新する
        EditTextPreference text;
        text = (EditTextPreference) findPreference(PREF_KEY_TEXT);
        text.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                preference.setSummary((CharSequence) value);
                return true;
            }
        });
        text.setSummary(text.getText());

        // 「その他の設定を有効にする」がONの場合に「その他の設定」を有効にする
        SwitchPreference enableOtherSettings = (SwitchPreference) findPreference(PREF_KEY_ENABLE_OTHER_SETTINGS);
        enableOtherSettings.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                Preference otherSettingsPreference = findPreference(PREF_KEY_OTHER_SETTINGS);
                otherSettingsPreference.setEnabled((Boolean) value);
                return true;
            }
        });
        findPreference(PREF_KEY_OTHER_SETTINGS).setEnabled(enableOtherSettings.isChecked());
    }

    /**
     * Preferenceに保存されているテキストを取得する
     * @param context Contextインスタンス
     * @return 保存されているテキスト
     */
    public static String getText(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return preferences.getString(PREF_KEY_TEXT, "2");
    }

    /**
     * Preferenceに保存されている値に応じた画面の色を取得する
     * @param context Contextインスタンス
     * @return 保存されている値に応じた色コード
     */
    public static int getScreenColor(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String value = preferences.getString(PREF_KEY_SCREEN_COLOR, null);
        if (value == null) {
            value = "white";
        }
        switch (value) {
            case "white":
                return 0xffffffff;
            case "red":
                return 0xffff0000;
            case "green":
                return 0xff00ff00;
            case "blue":
                return 0xff0000ff;
            case "yellow":
                return 0xffffff00;
            default:
                break;
        }
        return 0x00000000;
    }

    /**
     * Preferenceに保存されている表示対象画像(複数)を取得する
     * @param context Contextインスタンス
     * @return 保存されている値(文字列の配列オブジェクト)
     */
    private static Set<String> getDisplayImages(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (preferences.getBoolean(PREF_KEY_ENABLE_OTHER_SETTINGS, false)) {
            return preferences.getStringSet(PREF_KEY_DISPLAY_IMAGES, null);
        }
        return null;
    }

    /**
     * りんご画像が表示可能かどうか取得する
     * @param context Contextインスタンス
     * @return 画像が表示可能か否か
     */
    public static boolean isAppleImageDisplayable(Context context) {
        final String value = "apple";
        Set<String> displayImages = getDisplayImages(context);
        if (displayImages != null) {
            return displayImages.contains(value);
        }
        return false;
    }

    /**
     * みかん画像が表示可能かどうか取得する
     * @param context Contextインスタンス
     * @return 画像が表示可能か否か
     */
    public static boolean isMandarinOrangeImageDisplayable(Context context) {
        final String value = "mandarin_orange";
        Set<String> displayImages = getDisplayImages(context);
        if (displayImages != null) {
            return displayImages.contains(value);
        }
        return false;
    }

    /**
     * ぶどう画像が表示可能かどうか取得する
     * @param context Contextインスタンス
     * @return 画像が表示可能か否か
     */
    public static boolean isGrapeImageDisplayable(Context context) {
        final String value = "grape";
        Set<String> displayImages = getDisplayImages(context);
        if (displayImages != null) {
            return displayImages.contains(value);
        }
        return false;
    }

    /**
     * もも画像が表示可能かどうか取得する
     * @param context Contextインスタンス
     * @return 画像が表示可能か否か
     */
    public static boolean isPeachImageDisplayable(Context context) {
        final String value = "peach";
        Set<String> displayImages = getDisplayImages(context);
        if (displayImages != null) {
            return displayImages.contains(value);
        }
        return false;
    }
}

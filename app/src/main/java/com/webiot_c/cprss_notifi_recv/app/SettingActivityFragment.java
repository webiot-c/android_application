package com.webiot_c.cprss_notifi_recv.app;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.webiot_c.cprss_notifi_recv.R;
import com.webiot_c.cprss_notifi_recv.utility.PreferencesUtility;

public class SettingActivityFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_fragment);


        for (PreferencesUtility.PreferenceInfo pinfo : PreferencesUtility.getPrefreInfos()) {
            EditTextPreference etp = (EditTextPreference) findPreference(pinfo.getPreferenceKey());
            etp.setOnPreferenceChangeListener(this);
            etp.setSummary(pinfo.getFormattedText(this.getResources(), etp.getText()));
        }

        findPreference("");

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PreferencesUtility.PreferenceInfo pinfo = PreferencesUtility.getPreferenceInfo(preference.getKey());

        if(checkVaildaty(preference, newValue)){
            preference.setSummary(pinfo.getFormattedText(this.getResources(), newValue));
            return true;
        }

        return false;
    }

    public boolean checkVaildaty(Preference preference, Object newValue){

        EditTextPreference very_short_etp = (EditTextPreference) findPreference(PreferencesUtility.VERY_SHORT_DURATION);
        EditTextPreference few_late_etp = (EditTextPreference) findPreference(PreferencesUtility.FEW_LATE_DURATION);
        EditTextPreference late_etp = (EditTextPreference) findPreference(PreferencesUtility.LATE_DURATION);
        int very_short = Integer.valueOf(very_short_etp.getText());
        int few_late = Integer.valueOf(few_late_etp.getText());
        int late = Integer.valueOf(late_etp.getText());

        if(preference.getKey().equals(PreferencesUtility.VERY_SHORT_DURATION))
            very_short =Integer.valueOf((String)newValue);

        if(preference.getKey().equals(PreferencesUtility.FEW_LATE_DURATION))
            few_late =Integer.valueOf((String)newValue);

        if(preference.getKey().equals(PreferencesUtility.LATE_DURATION))
            late =Integer.valueOf((String)newValue);


        if(very_short < few_late && few_late < late){
            return true;
        } else{
            return false;
        }
    }
}

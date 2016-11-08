package com.mediatek.settings.plugin;

import android.app.Dialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultWifiExt;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IWifiExt")
public class OP03WifiExt extends DefaultWifiExt {

    private static final String TAG = "WifiExt";

    public OP03WifiExt(Context context) {
        super(context);
    }

    public void setSleepPolicyPreference(ListPreference sleepPolicyPref, String[] entriesArray, String[] valuesArray) {
        if (sleepPolicyPref.getDialog() != null) {
          Log.d("@M_" + TAG, "SleepPolicyPref dialog is active");
          Dialog dlg = sleepPolicyPref.getDialog();
          if (dlg.isShowing()) {
              Log.d("@M_" + TAG, "Dismiss SleepPolicyPref dialog");
              dlg.cancel();
            }
        }
        String[] newEntries = {entriesArray[1], entriesArray[2]};
        String[] newValues = {valuesArray[1], valuesArray[2]};

        sleepPolicyPref.setEntries(newEntries);
        sleepPolicyPref.setEntryValues(newValues);
        Log.d("@M_" + TAG, "setSleepPolicyPreference");
    }
}


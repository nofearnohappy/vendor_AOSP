package com.mediatek.browser.plugin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserMiscExt;
import com.mediatek.common.PluginImpl;
/**
* class for modify download path.
*/
@PluginImpl(interfaceName = "com.mediatek.browser.ext.IBrowserMiscExt")
public class Op09BrowserMiscExt extends DefaultBrowserMiscExt {

    private static final String TAG = "Op09BrowserMiscExt";

    public static final String FILEMANAGER_EXTRA_NAME = "download path";

    public static final int RESULT_CODE_START_FILEMANAGER = 1000;
    /**
     * Handle the activity result.
     * @param requestCode the request code
     * @param resultCode the result code
     * @param data the intent data
     * @param obj the different object for each requestCode
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data, Object obj) {
        Log.i(TAG, "Enter: " + "onActivityResult" + " --OP09 implement");
        if (requestCode == RESULT_CODE_START_FILEMANAGER) {
            PreferenceFragment prefFrag = (PreferenceFragment) obj;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    String downloadPath = extras.getString(FILEMANAGER_EXTRA_NAME);
                    if (downloadPath != null) {
                        Preference downloadPref = prefFrag.findPreference(Op09BrowserSettingExt.
                            PREF_DOWNLOAD_DIRECTORY_SETTING);
                        Editor ed = downloadPref.getEditor();
                        ed.putString(Op09BrowserSettingExt.PREF_DOWNLOAD_DIRECTORY_SETTING,
                            downloadPath);
                        ed.commit();
                        downloadPref.setSummary(downloadPath);
                    }
                }
            }
        }
    }
}

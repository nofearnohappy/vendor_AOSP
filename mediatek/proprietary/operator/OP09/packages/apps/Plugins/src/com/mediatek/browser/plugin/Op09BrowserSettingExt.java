package com.mediatek.browser.plugin;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.custom.CustomProperties;
import com.mediatek.op09.plugin.R;
import com.mediatek.storage.StorageManagerEx;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSettingExt")
public class Op09BrowserSettingExt extends DefaultBrowserSettingExt {

    private static final String TAG = "Op09BrowserSettingExt";

    private static final String DEFAULT_DOWNLOAD_DIRECTORY_OP09 = "/storage/sdcard0/Download";
    private static final String DEFAULT_DOWNLOAD_FOLDER_OP09 = "/Download";
    private static final String ACTION_DOWNLOAD_LOCATION =
            "com.mediatek.filemanager.DOWNLOAD_LOCATION";
    public static final String PREF_DOWNLOAD_DIRECTORY_SETTING = "download_directory_setting";
    private Context mContext;

    public Op09BrowserSettingExt(Context context) {
        super();
        mContext = context;
    }

    public String getCustomerHomepage() {
        Log.i("@M_" + TAG, "Enter: " + "getCustomerHomepage" + " --OP09 implement");
        return mContext.getResources().getString(R.string.homepage_base_site_navigation);
    }

    public String getDefaultDownloadFolder() {
        Log.i("@M_" + TAG, "Enter: " + "getDefaultDownloadFolder()" + " --OP09 implement");
        String defaultDownloadPath = DEFAULT_DOWNLOAD_DIRECTORY_OP09;
        String defaultStorage = StorageManagerEx.getDefaultPath();
        if (null != defaultStorage) {
            defaultDownloadPath = defaultStorage + DEFAULT_DOWNLOAD_FOLDER_OP09;
        }
        Log.v("@M_" + TAG, "device default storage is: " + defaultStorage +
                " defaultPath is: " + defaultDownloadPath);
        return defaultDownloadPath;
    }

    /**
     * Customize the preference.
     * @param index the preference index
     * @param prefSc the preference prefSc
     * @param onPreferenceChangeListener the preference change listener
     * @param sharedPref the shared preference
     * @param prefFrag the preferenceFragment prefFrag
     */
    public void customizePreference(int index, PreferenceScreen prefSc,
                    OnPreferenceChangeListener onPreferenceChangeListener,
                    SharedPreferences sharedPref, PreferenceFragment prefFrag) {
        Log.i(TAG, "Enter: " + "customizePreference" + " --OP09 implement");
        switch (index) {
            case IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_ADVANCED:
                PackageManager pm = prefSc.getContext().getPackageManager();
                try {
                    Intent intent = new Intent(ACTION_DOWNLOAD_LOCATION);
                    ResolveInfo info = pm.resolveActivity(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        Preference downloadPref = new Preference(prefSc.getContext());
                        downloadPref.setKey(PREF_DOWNLOAD_DIRECTORY_SETTING);
                        downloadPref.setTitle(mContext.getResources()
                            .getString(R.string.pref_extras_set_download_directory_title));
                        downloadPref.setOnPreferenceChangeListener(onPreferenceChangeListener);
                        Op09BrowserSettingExt settingExt = new Op09BrowserSettingExt(mContext);
                        String downloadDir = sharedPref.getString(PREF_DOWNLOAD_DIRECTORY_SETTING,
                                                settingExt.getDefaultDownloadFolder());
                        downloadPref.setOnPreferenceClickListener(
                            clickDownloadDirectorySetting(sharedPref, prefFrag));
                        downloadPref.setSummary(downloadDir);
                        prefSc.addPreference(downloadPref);
                    }
                } catch (ActivityNotFoundException exception) {
                    Log.e(TAG, "occur ActivityNotFoundException");
                }
                break;

            default:
                break;
        }
    }

    private Preference.OnPreferenceClickListener
        clickDownloadDirectorySetting(SharedPreferences sharedPref, PreferenceFragment prefFrag) {
        final SharedPreferences shared = sharedPref;
        final PreferenceFragment frag = prefFrag;
        return new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Op09BrowserSettingExt settingExt = new Op09BrowserSettingExt(mContext);
                String selectedPath = shared.getString(PREF_DOWNLOAD_DIRECTORY_SETTING,
                                        settingExt.getDefaultDownloadFolder());
                Intent intent = new Intent(ACTION_DOWNLOAD_LOCATION);
                intent.putExtra(Op09BrowserMiscExt.FILEMANAGER_EXTRA_NAME, selectedPath);
                frag.startActivityForResult(intent,
                    Op09BrowserMiscExt.RESULT_CODE_START_FILEMANAGER);
                return true;
            }
        };
    }

    /**
     * Customize the user agent string.
     * @param defaultUA the default user agent string
     * @return the customized user agent string
     */
    public String getOperatorUA(String defaultUA) {
        Log.i("@M_" + TAG, "Enter: " + "getOperatorUA, default UA: " + defaultUA + " --OP09 implement");
        String op09UA = defaultUA;
        String manufacturer = CustomProperties.getString(CustomProperties.MODULE_BROWSER,
                                CustomProperties.MANUFACTURER);
        if (defaultUA != null && defaultUA.length() > 0
            && manufacturer != null && manufacturer.length() > 0) {
            String newModel = manufacturer + "-" + Build.MODEL;
            if (!defaultUA.contains(newModel)) {
                op09UA = defaultUA.replace(Build.MODEL, newModel);
            }
        }
        Log.i("@M_" + TAG, "Exit: " + "getOperatorUA, OP09UA: " + op09UA);
        return op09UA;
    }
}

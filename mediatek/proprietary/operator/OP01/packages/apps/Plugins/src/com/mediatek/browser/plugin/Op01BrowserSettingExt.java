package com.mediatek.browser.plugin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.preference.Preference.OnPreferenceChangeListener;
import android.webkit.WebSettings;

import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSettingExt")
public class Op01BrowserSettingExt extends DefaultBrowserSettingExt {

    private static final String TAG = "Op01BrowserSettingExt";

    private static final String PREF_FONT_FAMILY = "font_family";
    public static final String PREF_DOWNLOAD_DIRECTORY_SETTING = "download_directory_setting";
    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String SERCH_ENGIN_BAIDU = "baidu";
    private static final String PREF_LANDSCAPEONLY = "landscape_only";
    private static final String DEFAULT_FONT_FAMILY = "Sans-serif";
    public static final String PREF_NOT_REMIND = "pref_not_remind";

    private static final String ACTION_DOWNLOAD_LOCATION = "com.mediatek.filemanager.DOWNLOAD_LOCATION";

    private Context mContext;

    public Op01BrowserSettingExt(Context context) {
        super();
        mContext = context;
    }

    public String getCustomerHomepage() {
        Log.i("@M_" + TAG, "Enter: " + "getCustomerHomepage" + " --OP01 implement");
        return mContext.getResources().getString(R.string.homepage_for_op01);
    }

    public boolean updatePreferenceItem(Preference pref, Object objValue) {
        Log.i("@M_" + TAG, "Enter: " + "updatePreferenceItem" + " --OP01 implement");
        if (PREF_FONT_FAMILY.equals(pref.getKey())) {
            pref.setSummary(objValue.toString());
            return true;
        } else if (PREF_LANDSCAPEONLY.equals(pref.getKey())) {
            CheckBoxPreference landscape = (CheckBoxPreference) pref;
            boolean value = ((Boolean) objValue).booleanValue();
            Editor ed = landscape.getEditor();
            ed.putBoolean(PREF_LANDSCAPEONLY, value);
            ed.commit();
            return true;
        }
        return false;
    }

    //@Override
    public void customizePreference(int index, PreferenceScreen prefSc,
                    OnPreferenceChangeListener onPreferenceChangeListener,
                    SharedPreferences sharedPref, PreferenceFragment prefFrag) {
        Log.i("@M_" + TAG, "Enter: " + "customizePreference" + " --OP01 implement");
        switch (index) {
            case IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_ACCESSIBILITY:
                ListPreference fontFamily = new ListPreference(prefSc.getContext());
                fontFamily.setKey(PREF_FONT_FAMILY);
                fontFamily.setTitle(mContext.getResources().getString(R.string.pref_default_font_family));
                fontFamily.setDialogTitle(mContext.getResources().getString(R.string.pref_default_font_family_dialogtitle));
                fontFamily.setDefaultValue(mContext.getResources().getString(R.string.pref_default_font_family_default));
                fontFamily.setEntries(mContext.getResources().getTextArray(R.array.pref_default_font_family_choices));
                fontFamily.setEntryValues(mContext.getResources().getTextArray(R.array.pref_default_font_family_values));
                fontFamily.setOnPreferenceChangeListener(onPreferenceChangeListener);
                fontFamily.setSummary(sharedPref.getString(PREF_FONT_FAMILY, DEFAULT_FONT_FAMILY));
                prefSc.addPreference(fontFamily);
                break;

            case IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_ADVANCED:
                CheckBoxPreference openInBackground = (CheckBoxPreference) prefFrag.findPreference("open_in_background");
                if (openInBackground != null) {
                    CheckBoxPreference landscape = new CheckBoxPreference(prefSc.getContext());
                    landscape.setKey(PREF_LANDSCAPEONLY);
                    landscape.setEnabled(true);
                    landscape.setTitle(mContext.getResources().getString(R.string.pref_content_landscape_only));
                    landscape.setChecked(sharedPref.getBoolean(PREF_LANDSCAPEONLY, false));
                    landscape.setOnPreferenceChangeListener(onPreferenceChangeListener);
                    landscape.setSummary(mContext.getResources().getString(R.string.pref_content_landscape_only_summary));
                    landscape.setOrder(openInBackground.getOrder() + 1);
                    prefSc.addPreference(landscape);
                }

                PackageManager pm = prefSc.getContext().getPackageManager();
                try {
                    Intent intent = new Intent(ACTION_DOWNLOAD_LOCATION);
                    ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        Preference downloadPref = new Preference(prefSc.getContext());
                        downloadPref.setKey(PREF_DOWNLOAD_DIRECTORY_SETTING);
                        downloadPref.setTitle(mContext.getResources().getString(R.string.pref_extras_set_download_directory_title));
                        downloadPref.setOnPreferenceChangeListener(onPreferenceChangeListener);
                        Op01BrowserSettingExt settingExt = new Op01BrowserSettingExt(mContext);
                        String downloadDir = sharedPref.getString(PREF_DOWNLOAD_DIRECTORY_SETTING,
                                                settingExt.getDefaultDownloadFolder());
                        downloadPref.setOnPreferenceClickListener(
                            clickDownloadDirectorySetting(sharedPref, prefFrag));
                        downloadPref.setSummary(downloadDir);
                        prefSc.addPreference(downloadPref);
                    }
                } catch (ActivityNotFoundException exception) {
                    Log.e("@M_" + TAG, "occur ActivityNotFoundException");
                }
                break;

            default:
                break;
        }
    }

    public String getSearchEngine(SharedPreferences mPrefs, Context context) {
        Log.i("@M_" + TAG, "Enter: " + "getSearchEngine" + " --OP01 implement");
        return mPrefs.getString(PREF_SEARCH_ENGINE, SERCH_ENGIN_BAIDU);
    }

    public void setStandardFontFamily(WebSettings settings, SharedPreferences mPrefs) {
        Log.i("@M_" + TAG, "Enter: " + "setStandardFontFamily" + " --OP01 implement");
        settings.setStandardFontFamily(mPrefs.getString(PREF_FONT_FAMILY, DEFAULT_FONT_FAMILY));
    }

    public String getOperatorUA(String defaultUA) {
        Log.i("@M_" + TAG, "Enter: " + "getOperatorUA" + " --OP01 implement");
        return "MT6582_TD/V1 Linux/3.4.5 Android/4.2.2 Release/03.26.2013 " + "Browser/AppleWebKit534.30 "
        + "Mobile Safari/534.30 MBBMS/2.2 System/Android 4.2.2";
    }

    public void setOnlyLandscape(SharedPreferences mPrefs, Activity activity) {
        Log.i("@M_" + TAG, "Enter: " + "setOnlyLandscape" + " --OP01 implement");
        if (activity != null) {
            if (mPrefs.getBoolean(PREF_LANDSCAPEONLY, false)) {
                Log.d("@M_" + TAG, "Activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE)");
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }

    private Preference.OnPreferenceClickListener clickDownloadDirectorySetting(SharedPreferences sharedPref, PreferenceFragment prefFrag) {
        final SharedPreferences shared = sharedPref;
        final PreferenceFragment frag = prefFrag;
        return new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Op01BrowserSettingExt settingExt = new Op01BrowserSettingExt(mContext);
                String selectedPath = shared.getString(PREF_DOWNLOAD_DIRECTORY_SETTING,
                                        settingExt.getDefaultDownloadFolder());
                Intent intent = new Intent(ACTION_DOWNLOAD_LOCATION);
                intent.putExtra(Op01BrowserMiscExt.FILEMANAGER_EXTRA_NAME, selectedPath);
                frag.startActivityForResult(intent, Op01BrowserMiscExt.RESULT_CODE_START_FILEMANAGER);
                return true;
            }
        };
    }
}

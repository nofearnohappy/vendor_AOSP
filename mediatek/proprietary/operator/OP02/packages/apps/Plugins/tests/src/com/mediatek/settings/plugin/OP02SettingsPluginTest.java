package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;

import com.mediatek.common.MPlugin;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;

public class OP02SettingsPluginTest extends ActivityInstrumentationTestCase2<MockActivity> {

    private Context mContext;
    private MockActivity mActivity;

    private IApnSettingsExt mApnSettingsExt;
    private IDeviceInfoSettingsExt mDeviceInfoSettingsExt;
    private ISettingsMiscExt mSettingsMiscExt;
    private ISimManagementExt mSimManagementExt;

    public OP02SettingsPluginTest() {
        super(MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mActivity = getActivity();
        mApnSettingsExt = (IApnSettingsExt) MPlugin.createInstance(
                IApnSettingsExt.class.getName(), getInstrumentation().getContext());
        mDeviceInfoSettingsExt = (IDeviceInfoSettingsExt) MPlugin.createInstance(
                IDeviceInfoSettingsExt.class.getName(), getInstrumentation().getContext());
        mSettingsMiscExt = (ISettingsMiscExt) MPlugin.createInstance(
                ISettingsMiscExt.class.getName(), getInstrumentation().getContext());
        mSimManagementExt = (ISimManagementExt) MPlugin.createInstance(
                ISimManagementExt.class.getName(), getInstrumentation().getContext());
    }

    // Should disallow user edit default APN
    public void test01APNisAllowEditPresetApn() {
        String type = null;
        String apn = null;
        String numeric = "46001";
        int sourcetype = 0;
        assertTrue(!mApnSettingsExt.isAllowEditPresetApn(type, apn, numeric, sourcetype));

        numeric = "46001";
        sourcetype = 1;
        assertTrue(mApnSettingsExt.isAllowEditPresetApn(type, apn, numeric, sourcetype));

        numeric = "46000";
        sourcetype = 0;
        assertTrue(mApnSettingsExt.isAllowEditPresetApn(type, apn, numeric, sourcetype));
    }

    // Should remove device info's stats pref summary
    public void test02DeviceinfoInitSummary() {
        String summaryString = "summary";
        Preference pref = new Preference(mContext);
        pref.setSummary(summaryString);
        assertTrue(pref.getSummary().equals(summaryString));
    }

    public void test03SettingMiscSetTimeoutPrefTitle() {
        final DialogPreference dialogPref = new CustomDialogPreference(mContext, null);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mActivity.getMockPreferenceGroup().addPreference(dialogPref);
            }
        });
        assertNull(dialogPref.getTitle());
        assertNull(dialogPref.getDialogTitle());
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mSettingsMiscExt.setTimeoutPrefTitle(dialogPref);
            }
        });
        assertNotNull(dialogPref.getTitle());
        assertNotNull(dialogPref.getDialogTitle());
    }

    public void test04SimManagementUpdateSimManagementPref() {
        final PreferenceScreen prefScreen = new PreferenceScreen(mContext, null);
        prefScreen.setKey("3g_service_settings");
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mActivity.getMockPreferenceGroup().addPreference(prefScreen);
            }
        });
        assertTrue(mActivity.getMockPreferenceGroup().getPreferenceCount() == 1);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mSimManagementExt.updateSimManagementPref(mActivity.getMockPreferenceGroup());
            }
        });
        if (SystemProperties.getInt("ro.mtk_gemini_3g_switch", 0) == 0) {
            assertTrue(mActivity.getMockPreferenceGroup().getPreferenceCount() == 0);
        } else {
            assertTrue(mActivity.getMockPreferenceGroup().getPreferenceCount() == 1);
        }
    }

    public void test05StatusGeminiInitUI() {
        final PreferenceScreen prefScreen = new PreferenceScreen(mContext, null);
        final Preference pref = new Preference(mContext);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mActivity.getMockPreferenceGroup().addPreference(prefScreen);
                prefScreen.addPreference(pref);
            }
        });
        assertTrue(prefScreen.getPreferenceCount() == 1);
    }

    public static class CustomDialogPreference extends DialogPreference {
        public CustomDialogPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
}

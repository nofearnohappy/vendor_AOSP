package com.mediatek.settings.plugin.tests;

import android.content.ContentValues;
import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.test.ActivityInstrumentationTestCase2;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.settings.ext.IApnSettingsExt;

public class OP09APNSettingsPluginTest extends ActivityInstrumentationTestCase2<MockActivity> {

    private static final String TAG = "OP09APNSettingsPluginTest";
    private static final String KEY_APN_NAME = "apn_name";
    private static final String KEY_APN_APN = "apn_apn";
    private static final String KEY_AUTH_TYPE = "auth_type";
    private static final String KEY_APN_USER = "apn_user";
    private static final String KEY_APN_PASSWORD = "apn_password";
    private static final String KEY_APN_MMS_PROXY = "apn_mms_proxy";
    private static final String KEY_APN_MMS_PORT = "apn_mms_port";
    private static final String KEY_APN_MMSC = "apn_mmsc";
    private static final String KEY_APN_MCC = "apn_mcc";
    private static final String KEY_APN_MNC = "apn_mnc";

    private static final String KEY_PPP_DIALOG = "PPP dialing number";

    // China Macoo
    private static final String MACOO_NW_MCC = "45500";
    // U.S.
    private static final String US_NW_MCC = "31001";

    // Region definition
    private static final int REGION_MAINLAND = 0;
    private static final int REGION_MACOO = 1;
    private static final int REGION_OTHERS = 2;

    private static final String KEY_PPP_DIALING_NUMBER = "PPP dialing number";

    private Context mContext;
    private MockActivity mActivity;
    private IApnSettingsExt mApnSettingsExt;

    public OP09APNSettingsPluginTest() {
        super(MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mContext = getInstrumentation().getContext();
        mApnSettingsExt = (IApnSettingsExt) PluginManager.createPluginObject(
                getInstrumentation().getContext(), IApnSettingsExt.class.getName());
    }

    public void test01_addPreference() {
        int slotId = PhoneConstants.GEMINI_SIM_1;
        PreferenceScreen prefScreen = mActivity.getPreferenceScreen();
        mApnSettingsExt.addPreference(slotId, prefScreen);
        assertNotNull(prefScreen.findPreference(KEY_PPP_DIALOG));

        slotId = PhoneConstants.GEMINI_SIM_2;
        prefScreen.removeAll();
        mApnSettingsExt.addPreference(slotId, prefScreen);
        assertNull(prefScreen.findPreference(KEY_PPP_DIALOG));
    }

    public void test02_customizeApnTitles() {
        int slotId = PhoneConstants.GEMINI_SIM_1;
        initRootPreference();
        PreferenceScreen prefScreen = mActivity.getPreferenceScreen();
        mApnSettingsExt.customizeApnTitles(slotId, prefScreen);
        assertEquals(prefScreen.findPreference(KEY_APN_NAME).getTitle(), "Connection Name");
        assertEquals(prefScreen.findPreference(KEY_APN_APN).getTitle(), "Access point name");
        assertEquals(prefScreen.findPreference(KEY_AUTH_TYPE).getTitle(), "Authentication type");
        assertEquals(prefScreen.findPreference(KEY_APN_USER).getTitle(), "User name");
        assertEquals(prefScreen.findPreference(KEY_APN_PASSWORD).getTitle(), "Password");

        slotId = PhoneConstants.GEMINI_SIM_2;
        initRootPreference();
        mApnSettingsExt.customizeApnTitles(slotId, prefScreen);
        assertNull(prefScreen.findPreference(KEY_PPP_DIALOG));

        assertEquals(prefScreen.findPreference(KEY_APN_NAME).getTitle(), "apn name");
        assertEquals(prefScreen.findPreference(KEY_APN_APN).getTitle(), "apn");
        assertEquals(prefScreen.findPreference(KEY_AUTH_TYPE).getTitle(), "auth type");
        assertEquals(prefScreen.findPreference(KEY_APN_USER).getTitle(), "apn user");
        assertEquals(prefScreen.findPreference(KEY_APN_PASSWORD).getTitle(), "apn password");
    }

    public void test03_customizeApnProjection() {
        String[] projection1 = {"aaa", "bbb", "ccc"};
        String[] projection2 = {"aaa", "bbb", "ccc", Telephony.Carriers.PPP};

        String[] projectionDst1 = mApnSettingsExt.customizeApnProjection(projection1);
        assertTrue(projectionDst1.length == 4);
        assertEquals(projectionDst1[3], Telephony.Carriers.PPP);

        String[] projectionDst2 = mApnSettingsExt.customizeApnProjection(projection2);
        assertTrue(projectionDst2.length == 4);
        assertEquals(projectionDst2[3], Telephony.Carriers.PPP);
    }

    public void test04_saveApnValues() {
        ContentValues contentValues = new ContentValues();

        // Do customize preference to add EditTextPreference first
        PreferenceScreen prefScreen = mActivity.getPreferenceScreen();
        mApnSettingsExt.addPreference(PhoneConstants.GEMINI_SIM_1, prefScreen);
        Preference pref = prefScreen.findPreference(KEY_PPP_DIALING_NUMBER);
        assertTrue(pref instanceof EditTextPreference);
        ((EditTextPreference) pref).setText("ppp");

        mApnSettingsExt.saveApnValues(contentValues);
        assertTrue(contentValues.size() > 0);
        assertTrue(contentValues.containsKey(Telephony.Carriers.PPP));
        assertNotNull(contentValues.getAsString(Telephony.Carriers.PPP));
        assertEquals(contentValues.getAsString(Telephony.Carriers.PPP), "ppp");
    }

    public void test05_setPreferenceTextAndSummary() {
        ContentValues contentValues = new ContentValues();

        // Do customize preference to add EditTextPreference first
        mApnSettingsExt.addPreference(PhoneConstants.GEMINI_SIM_1, mActivity.getPreferenceScreen());
        mApnSettingsExt.saveApnValues(contentValues);
        assertTrue(contentValues.size() > 0);
        assertTrue(contentValues.containsKey(Telephony.Carriers.PPP));
        assertTrue(mActivity.getPreferenceScreen().findPreference(KEY_PPP_DIALING_NUMBER) instanceof EditTextPreference);
        EditTextPreference editPref = (EditTextPreference) mActivity.getPreferenceScreen().findPreference(
                KEY_PPP_DIALING_NUMBER);
        assertEquals(contentValues.getAsString(Telephony.Carriers.PPP), editPref.getText());
    }

    public void test06_updateFieldsStatus() {
        initRootPreference();
        PreferenceScreen prefScreen = mActivity.getPreferenceScreen();
        mApnSettingsExt.updateFieldsStatus(PhoneConstants.GEMINI_SIM_1, prefScreen);
        assertFalse(prefScreen.findPreference(KEY_APN_MMS_PROXY).isEnabled());
        assertFalse(prefScreen.findPreference(KEY_APN_MMS_PORT).isEnabled());
        assertFalse(prefScreen.findPreference(KEY_APN_MMSC).isEnabled());
        assertFalse(prefScreen.findPreference(KEY_APN_MCC).isEnabled());
        assertFalse(prefScreen.findPreference(KEY_APN_MNC).isEnabled());

        prefScreen.removeAll();
        initRootPreference();
        mApnSettingsExt.updateFieldsStatus(PhoneConstants.GEMINI_SIM_2, prefScreen);
        assertTrue(prefScreen.findPreference(KEY_APN_MMS_PROXY).isEnabled());
        assertTrue(prefScreen.findPreference(KEY_APN_MMS_PORT).isEnabled());
        assertTrue(prefScreen.findPreference(KEY_APN_MMSC).isEnabled());
        assertTrue(prefScreen.findPreference(KEY_APN_MCC).isEnabled());
        assertTrue(prefScreen.findPreference(KEY_APN_MNC).isEnabled());
    }

    private void initRootPreference() {
        PreferenceScreen prefScreen = mActivity.getPreferenceScreen();
        prefScreen.removeAll();

        Preference apnNamePref = new Preference(mActivity);
        apnNamePref.setKey(KEY_APN_NAME);
        apnNamePref.setTitle("apn name");
        prefScreen.addPreference(apnNamePref);

        Preference apnPref = new Preference(mActivity);
        apnPref.setKey(KEY_APN_APN);
        apnPref.setTitle("apn");
        prefScreen.addPreference(apnPref);

        Preference apnAuthPref = new Preference(mActivity);
        apnAuthPref.setKey(KEY_AUTH_TYPE);
        apnAuthPref.setTitle("auth type");
        prefScreen.addPreference(apnAuthPref);

        Preference apnUserPref = new Preference(mActivity);
        apnUserPref.setKey(KEY_APN_USER);
        apnUserPref.setTitle("apn user");
        prefScreen.addPreference(apnUserPref);

        Preference apnPwdPref = new Preference(mActivity);
        apnPwdPref.setKey(KEY_APN_PASSWORD);
        apnPwdPref.setTitle("apn password");
        prefScreen.addPreference(apnPwdPref);

        Preference apnProxyPref = new Preference(mActivity);
        apnProxyPref.setKey(KEY_APN_MMS_PROXY);
        prefScreen.addPreference(apnProxyPref);

        Preference apnPortPref = new Preference(mActivity);
        apnPortPref.setKey(KEY_APN_MMS_PORT);
        prefScreen.addPreference(apnPortPref);

        Preference apnMmscPref = new Preference(mActivity);
        apnMmscPref.setKey(KEY_APN_MMSC);
        prefScreen.addPreference(apnMmscPref);

        Preference apnMccPref = new Preference(mActivity);
        apnMccPref.setKey(KEY_APN_MCC);
        prefScreen.addPreference(apnMccPref);

        Preference apnMncPref = new Preference(mActivity);
        apnMncPref.setKey(KEY_APN_MNC);
        prefScreen.addPreference(apnMncPref);
    }
}

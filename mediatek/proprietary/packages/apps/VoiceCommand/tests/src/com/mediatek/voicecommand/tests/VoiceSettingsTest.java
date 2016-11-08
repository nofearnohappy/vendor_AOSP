package com.mediatek.voicecommand.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.mediatek.voicecommand.R;
import com.mediatek.voicecommand.mgr.ConfigurationManager;
import com.mediatek.voicecommand.ui.settings.VoiceUiCommandPlayActivity;
import com.mediatek.voicecommand.ui.settings.VoiceUiResUtil;
import com.mediatek.voicecommand.ui.settings.VoiceUiSettingsActivity;
import com.jayway.android.robotium.solo.Solo;

public class VoiceSettingsTest extends ActivityInstrumentationTestCase2<VoiceUiSettingsActivity> {
    private VoiceUiSettingsActivity mVoiceUiSettings;
    private Instrumentation mIns;
    private Solo mSolo;
    private Switch mSwitch;
    private ConfigurationManager mVoiceConfigMgr;
    private Context mContext;
    private String[] featureList;
    private static final String TAG = "VoiceCommandUiSettingsTest";

    public VoiceSettingsTest() {
        super(VoiceUiSettingsActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        mVoiceUiSettings = getActivity();
        mIns = getInstrumentation();
        mSolo = new Solo(mIns, mVoiceUiSettings);
        mSwitch = (Switch) mVoiceUiSettings.getActionBar().getCustomView();
        mContext = (Context) mVoiceUiSettings;
        mVoiceConfigMgr = ConfigurationManager.getInstance(mContext);
        featureList = mVoiceConfigMgr.getFeatureNameList();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mVoiceUiSettings != null) {
            mVoiceUiSettings.finish();
        }
        super.tearDown();
    }

    public void test01_EnableAllClick() {
        Log.d(TAG, "EnableAllClick start");
        turnOffALL();
        TestSleep.sleep(1000);
        turnOnALL();
        TestSleep.sleep(1000);
    }

    private void turnOnALL() {
        for (int i = 0; i < featureList.length; i++) {
            int processID = mVoiceConfigMgr.getProcessID(featureList[i]);
            int TitleId = VoiceUiResUtil.getProcessTitleResourceId(processID);
            String text = mContext.getString(TitleId);
            Log.d(TAG, "turnOnALL text = " + text);
            Switch debugSwitch = findSwitch(text);
            if (debugSwitch != null && !debugSwitch.isChecked()) {
                mSolo.clickOnView(debugSwitch);
                TestSleep.sleep(1000);
            }
        }
    }

    private void turnOffALL() {
        for (int i = 0; i < featureList.length; i++) {
            int processID = mVoiceConfigMgr.getProcessID(featureList[i]);
            int TitleId = VoiceUiResUtil.getProcessTitleResourceId(processID);
            String text = mContext.getString(TitleId);
            Log.d(TAG, "turnOffALL text = " + text);
            Switch debugSwitch = findSwitch(text);
            if (debugSwitch != null && debugSwitch.isChecked()) {
                mSolo.clickOnView(debugSwitch);
                TestSleep.sleep(1000);
            }
        }
    }

    public void test02_ClickFeatureList() {
        Log.d(TAG, "ClickFeatureList start");
        for (int i = 0; i < featureList.length; i++) {
            // String processName =
            // mVoiceConfigMgr.getProcessName(featureList[i]);
            int processID = mVoiceConfigMgr.getProcessID(featureList[i]);
            int TitleId = VoiceUiResUtil.getProcessTitleResourceId(processID);
            String text = mContext.getString(TitleId);
            mSolo.clickOnText(text);
            TestSleep.sleep(500);
            mSolo.assertCurrentActivity("Except VoiceUiCommandPlayActivity Activity",
                    VoiceUiCommandPlayActivity.class);
            mSolo.goBack();
            boolean isEnabled = mVoiceConfigMgr.isProcessEnable(featureList[i]);
            Log.d(TAG, "ClickFeatureList text = " + text);
            Switch debugSwitch = findSwitch(text);
            if (debugSwitch != null) {
                boolean isChecked = debugSwitch.isChecked();
                assertEquals(isEnabled, isChecked);
            }

        }
    }

    public void test03_ClickLanguageList() {
        Log.d(TAG, "ClickLanguageList start");
        String[] LanguageList = mVoiceConfigMgr.getLanguageList();
        if (LanguageList == null) {
            Log.d(TAG, "test03_ClickLanguageList,LanguageList is null");
            return;
        }
        for (int i = 0; i < LanguageList.length; i++) {
            mSolo.clickOnText(mSolo.getString(R.string.voice_ui_language_title));
            TestSleep.sleep(1000);
            mSolo.clickOnText(LanguageList[i]);
            TestSleep.sleep(1000);
            Log.d(TAG, "ClickLanguageList" + i + " = " + LanguageList[i]);
            assertEquals(i, mVoiceConfigMgr.getCurrentLanguage());
        }
        TestSleep.sleep(500);
    }

    private Switch findSwitch(String text) {
        TextView tv = (TextView) mSolo.getText(text);
        if (tv == null) {
            return null;
        }
        LinearLayout parentLayout = (LinearLayout) tv.getParent().getParent();
        LinearLayout widgetFrameLayout = (LinearLayout) parentLayout.getChildAt(2);
        if (widgetFrameLayout == null) {
            return null;
        }
        Switch hftSwitch = (Switch) widgetFrameLayout.getChildAt(0);
        return hftSwitch;
    }

    public void test04_UiCommandPlay() {
        Log.d(TAG, "UiCommandPlay start");
        String processKey = featureList[0];
        int processID = mVoiceConfigMgr.getProcessID(processKey);
        int TitleId = VoiceUiResUtil.getProcessTitleResourceId(processID);
        String text = mContext.getString(TitleId);
        mSolo.clickOnText(text);
        TestSleep.sleep(500);
        String[] mCommands = mVoiceConfigMgr.getKeyWordForSettings(processKey);
        if (mCommands == null) {
            Log.e(TAG, "test04_UiCommandPlay,mCommands is null. ");
            return;
        }
        for (int i = 0; i < mCommands.length; i++) {
            Log.d(TAG, "mCommands" + i + " = " + mCommands[i]);
            TextView tView = (TextView) mSolo.getText(mCommands[i]);
            mSolo.clickOnView(tView);
            TestSleep.sleep(1000);
        }
        mSolo.goBack();
    }
}
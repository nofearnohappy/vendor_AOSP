package com.mediatek.ppl.test.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.ppl.ControlData;
import com.mediatek.ppl.MessageManager.PendingMessage;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.ControlPanelActivity;
import com.mediatek.ppl.ui.SetupManualActivity;
import com.mediatek.ppl.ui.SetupPasswordActivity;
import com.mediatek.ppl.ui.SetupTrustedContactsActivity;

import java.util.List;

public class BasicFlowTest extends ActivityInstrumentationTestCase2<SetupPasswordActivity> {
    private static final String TAG = "PPL/BasicFlowTest";

    private Solo mSolo;
    private SetupPasswordActivity mActivity;
    private static final int BACK_NONE = 1;
    private static final int BACK_VIA_BAR = 2;
    private static final int BACK_VIA_KEY = 3;
    private static final String PASSWORD = MockPplUtil.PASSWORD_ORIGINAL;

    public BasicFlowTest() {
        super(SetupPasswordActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mSolo.finishOpenedActivities();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        super.tearDown();
    }

    /**
     * Operation: only set up & basic use
     * Check: NA
     * */
    public void test00() {
        MockPplUtil.formatLog(TAG, "test00");
        assertNotNull(mActivity);
        assertNotNull(mSolo);
    }

    /**
     * Operation: only set up & basic use
     * Check: NA
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");
        runAll(BACK_NONE);
    }

    /**
     * Operation: add status bar operation
     * Check: NA
     * */
    public void test02() {
        MockPplUtil.formatLog(TAG, "test02");
        runAll(BACK_VIA_BAR);
    }

    /**
     * Operation: a flow test on UI from set up to basic use
     * Check: NA
     * */
    private void runAll(int backOption) {
        // 1/3: SetupPasswordActivity
        mSolo.enterText(0, PASSWORD);
        mSolo.enterText(1, PASSWORD);
        mSolo.clickOnButton(mSolo.getString(R.string.button_next));

        // 2/3: SetupTrustedContactsActivity
        mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
        mSolo.clickOnButton(mSolo.getString(R.string.button_next));

        if (backOption != BACK_NONE) {
            goPrevious(backOption);
            mSolo.waitForActivity(SetupTrustedContactsActivity.class);

            goPrevious(backOption);
            mSolo.waitForActivity(SetupPasswordActivity.class);
            mSolo.clickOnButton(mSolo.getString(R.string.button_next));

            mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
            mSolo.clickOnButton(mSolo.getString(R.string.button_next));
            mSolo.waitForActivity(SetupManualActivity.class);
        }

        // 3/3: SetupManualActivity -> ControlPanelActivity
        mSolo.clickOnButton(mSolo.getString(R.string.button_finish));

        if (backOption == BACK_NONE) {
            checkControlData();
        }

        // UpdatePasswordActivity
        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_change_password));
        mSolo.enterText(0, PASSWORD);
        mSolo.enterText(1, PASSWORD);
        if (backOption != BACK_NONE) {
            goPrevious(backOption);
        } else {
            mSolo.clickOnButton(mSolo.getString(R.string.button_confirm));
        }
        mSolo.waitForActivity(ControlPanelActivity.class);

        // UpdateTrustedContactsActivity
        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_update_emergency_contacts));
        mSolo.clickOnButton(mSolo.getString(R.string.button_add_contact_line));
        mSolo.enterText(1, MockPplUtil.SERVICE_NUMBER_1st);
        if (backOption != BACK_NONE) {
            goPrevious(backOption);
        } else {
            mSolo.clickOnButton(mSolo.getString(R.string.button_confirm));
        }
        mSolo.waitForActivity(ControlPanelActivity.class);

        // ViewManualActivity
        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_view_instructions));
        if (backOption != BACK_NONE) {
            goPrevious(backOption);
        } else {
            mSolo.clickOnButton(mSolo.getString(R.string.button_finish));
        }
        mSolo.waitForActivity(ControlPanelActivity.class);

        // DialogDisablePplFragment
        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_disable));
        if (backOption != BACK_NONE) {
            mSolo.goBack();
        } else {
            mSolo.clickOnButton(mSolo.getString(android.R.string.cancel));
        }
        mSolo.waitForActivity(ControlPanelActivity.class);

        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_disable));
        mSolo.clickOnButton(mSolo.getString(android.R.string.ok));
    }

    private void goPrevious(int backOption) {
        if (backOption == BACK_VIA_BAR) {
            mSolo.clickOnActionBarHomeButton();
        } else if (backOption == BACK_VIA_KEY) {
            mSolo.goBack();
        }
    }

    private void checkControlData() {
        ControlData controlData = MockDataUtil.loadControlData();
        Log.i(TAG, "Control data is " + controlData);

        assertTrue(controlData.isEnabled());
        assertTrue(controlData.isProvisioned());
        assertFalse(controlData.isLocked());
        assertFalse(controlData.isSimLocked());
        assertFalse(controlData.hasWipeFlag());

        assertTrue(MockDataUtil.checkPassword(MockPplUtil.PASSWORD_ORIGINAL.getBytes(),
                controlData.salt, controlData.secret));

        List<String> trustedList = controlData.TrustedNumberList;
        assertTrue(trustedList.size() == 1);
        assertTrue(trustedList.get(0).equals(MockPplUtil.SERVICE_NUMBER_1st));

        List<PendingMessage> msgList = controlData.PendingMessageList;
        assertTrue(msgList == null || msgList.size() == 0);
    }
}

/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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
import com.mediatek.ppl.ui.LaunchPplActivity;
import com.mediatek.ppl.ui.SetupManualActivity;
import com.mediatek.ppl.ui.SetupTrustedContactsActivity;

import java.util.List;

public class NewSettingTest extends ActivityInstrumentationTestCase2<LaunchPplActivity> {
    private static final String TAG = "PPL/NewSettingTest";

    private Solo mSolo;
    private LaunchPplActivity mActivity;
    private String[] mModeList = null;

    public NewSettingTest() {
        super(LaunchPplActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        prepareEnvironment();

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);
        mModeList = mActivity.getApplication().getResources().getStringArray(R.array.enable_mode_list);
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
     * Operation: Write sample data & disable feature
     * Check: NA
     * */
    public void test00() {
        ControlData controlData = MockDataUtil.buildSampleControlData();
        controlData.setEnable(false);
        MockDataUtil.writeControlData(controlData);
    }

    /**
     * Operation: click second item (use new setting)
     * Check: UI set up flow & state of feature, contact number
     * */
    public void test01() {
        int index = 1;
        Log.i(TAG, "mModeList[" + index + "] is " + mModeList[index]);
        mSolo.clickOnText(mModeList[index]);

        // 1/3: SetupPasswordActivity
        mSolo.enterText(0, MockPplUtil.PASSWORD_CHANGED);
        mSolo.enterText(1, MockPplUtil.PASSWORD_CHANGED);
        mSolo.clickOnButton(mSolo.getString(R.string.button_next));
        mSolo.waitForActivity(SetupTrustedContactsActivity.class);

        // 2/3: SetupTrustedContactsActivity
        mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_2nd);
        mSolo.clickOnButton(mSolo.getString(R.string.button_next));
        mSolo.waitForActivity(SetupManualActivity.class);

        // 3/3: SetupManualActivity -> ControlPanelActivity
        mSolo.clickOnButton(mSolo.getString(R.string.button_finish));

        ControlData controlData = MockDataUtil.loadControlData();
        Log.i(TAG, "Control data is " + controlData);

        assertTrue(controlData.isEnabled());
        assertTrue(controlData.isProvisioned());
        assertFalse(controlData.isLocked());
        assertFalse(controlData.isSimLocked());
        assertFalse(controlData.hasWipeFlag());

        assertTrue(MockDataUtil.checkPassword(MockPplUtil.PASSWORD_CHANGED.getBytes(),
                controlData.salt, controlData.secret));

        List<String> trustedList = controlData.TrustedNumberList;
        assertTrue(trustedList.size() == 1);
        assertTrue(trustedList.get(0).equals(MockPplUtil.SERVICE_NUMBER_2nd));

        List<PendingMessage> msgList = controlData.PendingMessageList;
        assertTrue(msgList == null || msgList.size() == 0);

    }

    private void prepareEnvironment() {
        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());

        // Write the sample data
        ControlData controlData = MockDataUtil.buildSampleControlData();
        controlData.setEnable(false);
        MockDataUtil.writeControlData(controlData);
    }
}

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
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.LaunchPplActivity;

public class PreviousSettingTest extends ActivityInstrumentationTestCase2<LaunchPplActivity> {
    private static final String TAG = "PPL/PreviousSettingTest";

    private Solo mSolo;
    private LaunchPplActivity mActivity;
    private String[] mModeList = null;

    public PreviousSettingTest() {
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
     * Operation: NA
     * Check: length of enable_mode_list array
     * */
    public void test00() {
        assertNotNull(mActivity);
        assertNotNull(mModeList);
        assertTrue(mModeList.length == 2);

        MockPplUtil.testUiReceiver(mActivity, "LaunchPplActivity", PplService.Intents.UI_NO_SIM);
    }

    /**
     * Operation: click cancel button
     * Check: NA
     * */
    public void test01() {
        mSolo.clickOnButton(mSolo.getString(android.R.string.cancel));
        Log.i(TAG, "Click button " + mSolo.getString(android.R.string.cancel));
    }

    /**
     * Operation: click first item (use previous setting)
     * Check: new password
     * */
    public void test02() {
        ControlData oldData = MockDataUtil.loadControlData();
        Log.i(TAG, "Control data is " + oldData);

        assertFalse(oldData.isEnabled());
        assertTrue(MockDataUtil.checkPassword(MockPplUtil.PASSWORD_ORIGINAL.getBytes(),
                oldData.salt, oldData.secret));

        int index = 0;
        Log.i(TAG, "mode[" + index + "] is " + mModeList[index]);
        mSolo.clickOnText(mModeList[index]);

        mSolo.enterText(0, MockPplUtil.PASSWORD_ORIGINAL);
        mSolo.clickOnButton(mSolo.getString(R.string.button_confirm));

        ControlData newData = MockDataUtil.loadControlData();
        assertTrue(newData.isEnabled());

        closeFeature();
    }

    /**
     * Operation: disable feature
     * Check: state of feature
     * */
    private void closeFeature() {
        // Close feature in next step
        mSolo.clickOnButton(mSolo.getString(R.string.button_control_panel_disable));
        mSolo.clickOnButton(mSolo.getString(android.R.string.ok));

        ControlData controlData = MockDataUtil.loadControlData();
        assertFalse(controlData.isEnabled());
        assertTrue(controlData.isProvisioned());
    }

    private void prepareEnvironment() {
        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());

        // Write the sample data
        ControlData controlData = MockDataUtil.buildSampleControlData();
        controlData.setEnable(false);
        MockDataUtil.writeControlData(controlData);
    }
}

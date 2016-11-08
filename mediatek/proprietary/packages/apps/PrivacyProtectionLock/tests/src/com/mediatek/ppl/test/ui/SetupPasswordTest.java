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
import android.widget.Button;
import android.widget.EditText;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.SetupPasswordActivity;

public class SetupPasswordTest extends ActivityInstrumentationTestCase2<SetupPasswordActivity> {
    private static final String TAG = "PPL/SetupPasswordTest";

    private Solo mSolo;
    private SetupPasswordActivity mActivity;
    private Button mNextButton;
    private EditText mFirstPassword;
    private EditText mSecondPassword;

    public SetupPasswordTest() {
        super(SetupPasswordActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);
        mNextButton = (Button) mActivity.findViewById(R.id.btn_bottom_next);
        mFirstPassword = (EditText) mActivity.findViewById(R.id.et_setup_password_input1);
        mSecondPassword = (EditText) mActivity.findViewById(R.id.et_setup_password_input2);
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
     * Check: state of button
     * */
    public void test00() {
        MockPplUtil.formatLog(TAG, "test00");
        assertNotNull(mActivity);
        assertNotNull(mNextButton);
        assertFalse(mNextButton.isEnabled());
        mSolo.clickOnActionBarHomeButton();
    }

    /**
     * Operation: input password
     * Check: input and state of button
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");
        String shortPass = "11111";
        mSolo.enterText(0, shortPass);
        mSolo.enterText(1, shortPass);
        assertFalse(mNextButton.isEnabled());
        assertTrue(mFirstPassword.getText().toString().equals(shortPass));
        assertTrue(mSecondPassword.getText().toString().equals(shortPass));

        mSolo.enterText(0, "0");
        assertFalse(mNextButton.isEnabled());
        assertTrue(mFirstPassword.getText().toString().equals(shortPass + "0"));

        mSolo.enterText(1, "1");
        assertTrue(mNextButton.isEnabled());
        assertTrue(mSecondPassword.getText().toString().equals(shortPass + "1"));

        mSolo.clickOnButton(mSolo.getString(R.string.button_next));
        MockPplUtil.testUiReceiver(mActivity, "SetupPasswordActivity", PplService.Intents.UI_QUIT_SETUP_WIZARD);
    }

    /**
     * Operation: only input first EditText
     * Check: state of button
     * */
    public void test02() {
        MockPplUtil.formatLog(TAG, "test02");
        mSolo.enterText(0, "111111");
        assertFalse(mNextButton.isEnabled());
        MockPplUtil.testUiReceiver(mActivity, "SetupPasswordActivity", PplService.Intents.UI_NO_SIM);
    }
}

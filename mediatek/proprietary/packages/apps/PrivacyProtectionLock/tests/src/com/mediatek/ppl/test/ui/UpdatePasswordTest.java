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

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.ppl.ControlData;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.UpdatePasswordActivity;

public class UpdatePasswordTest extends ActivityInstrumentationTestCase2<UpdatePasswordActivity> {
    private static final String TAG = "PPL/UpdatePasswordTest";
    private static final String PASS_SHORT = "000";
    private static final String PASS_OLD = MockPplUtil.PASSWORD_ORIGINAL;
    private static final String PASS_NEW = MockPplUtil.PASSWORD_CHANGED;

    private Solo mSolo;
    private UpdatePasswordActivity mActivity;

    private Button mConfirmButton;
    private EditText mFirstPassword;
    private EditText mSecondPassword;

    public UpdatePasswordTest() {
        super(UpdatePasswordActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());
        MockDataUtil.writeSampleControlData();

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);

        mConfirmButton = (Button) mActivity.findViewById(R.id.btn_bottom_next);
        mFirstPassword = (EditText) mActivity.findViewById(R.id.et_setup_password_input1);
        mSecondPassword = (EditText) mActivity.findViewById(R.id.et_setup_password_input2);
    }

    @Override
    protected void tearDown() throws Exception {
        mActivity.finish();
        try {
            mSolo.finishOpenedActivities();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.tearDown();
    }

    /**
     * Operation: Input short and long password
     * Check: input, state of button
     * */
    public void test00() {
        MockPplUtil.formatLog(TAG, "test00");
        assertNotNull(mActivity);
        assertNotNull(mFirstPassword);
        assertNotNull(mSecondPassword);

        assertFalse(mConfirmButton.isEnabled());

        inputAndCheck(PASS_SHORT, PASS_SHORT);
        assertFalse(mConfirmButton.isEnabled());

        inputAndCheck(PASS_OLD, PASS_NEW);
        assertTrue(mConfirmButton.isEnabled());
        mSolo.clickOnButton(mConfirmButton.getText().toString());
    }

    /**
     * Operation: rotate
     * Check: input & button state not changed
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");
        inputAndCheck(PASS_SHORT, PASS_SHORT);
        assertFalse(mConfirmButton.isEnabled());

        // rotate
        mSolo.setActivityOrientation(Solo.LANDSCAPE);

        assertFalse(mConfirmButton.isEnabled());
        assertTrue(PASS_SHORT.equals(mFirstPassword.getText().toString()));
        assertTrue(PASS_SHORT.equals(mSecondPassword.getText().toString()));

        MockPplUtil.testUiReceiver(mActivity, "UpdatePasswordActivity", Intent.ACTION_SCREEN_OFF);
    }

    /**
     * Operation: set new password
     * Check: new password
     * */
    public void test02() {
        MockPplUtil.formatLog(TAG, "test02");
        inputAndCheck(PASS_NEW, PASS_NEW);
        assertTrue(mConfirmButton.isEnabled());
        mSolo.clickOnButton(mConfirmButton.getText().toString());

        ControlData controlData = MockDataUtil.loadControlData();

        assertTrue(MockDataUtil.checkPassword(PASS_NEW.getBytes(),
                controlData.salt, controlData.secret));

        MockPplUtil.testUiReceiver(mActivity, "UpdatePasswordActivity", PplService.Intents.UI_NO_SIM);
    }

    private void inputAndCheck(String firstText, String secondText) {
        mSolo.clearEditText(0);
        mSolo.enterText(0, firstText);

        mSolo.clearEditText(1);
        mSolo.enterText(1, secondText);
        assertTrue(firstText.equals(mFirstPassword.getText().toString()));
        assertTrue(secondText.equals(mSecondPassword.getText().toString()));
    }
}

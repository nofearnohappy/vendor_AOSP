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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.LoginPplActivity;

public class LoginTest extends ActivityInstrumentationTestCase2<LoginPplActivity> {
    private static final String TAG = "PPL/LoginTest";

    private Solo mSolo;
    private LoginPplActivity mActivity;

    private CheckBox mShowPassword;
    private EditText mPassword;
    private Button mConfirmButton;

    public LoginTest() {
        super(LoginPplActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());
        MockDataUtil.writeSampleControlData();

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);

        mPassword = (EditText) mActivity.findViewById(R.id.et_login_ppl_input);
        mShowPassword = (CheckBox) mActivity.findViewById(R.id.cb_login_ppl_show_pw);
        mConfirmButton = (Button) mActivity.findViewById(R.id.btn_bottom_confirm);

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
        assertNotNull(mPassword);
        assertNotNull(mShowPassword);
        assertNotNull(mConfirmButton);

        assertFalse(mConfirmButton.isEnabled());
        assertFalse(mShowPassword.isChecked());
        mSolo.clickOnActionBarHomeButton();
    }

    /**
     * Operation: input and click check box
     * Check: state of check box
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");
        inputText("0");
        mSolo.clickOnButton(mShowPassword.getText().toString());
        assertTrue(mShowPassword.isChecked());

        inputText("1");
        mSolo.clickOnButton(mShowPassword.getText().toString());
        assertFalse(mShowPassword.isChecked());

        MockPplUtil.testUiReceiver(mActivity, "LoginPplActivity", PplService.Intents.UI_QUIT_SETUP_WIZARD);
    }

    /**
     * Operation: input a wrong password, and click next
     * Check: NA
     * */
    public void test02() {
        MockPplUtil.formatLog(TAG, "test02");
        inputText("1000010000");
        assertTrue(mConfirmButton.isEnabled());
        mSolo.clickOnButton(mConfirmButton.getText().toString());

        MockPplUtil.testUiReceiver(mActivity, "LoginPplActivity", PplService.Intents.UI_NO_SIM);
    }

    /**
     * Operation: input password
     * Check: content of edit text
     * */
    private void inputText(String text) {
        mSolo.clearEditText(mPassword);
        mSolo.enterText(mPassword, text);
        Log.i(TAG, "Entered password is " + mPassword.getText());
        assertTrue(text.equals(mPassword.getText().toString()));
    }

}

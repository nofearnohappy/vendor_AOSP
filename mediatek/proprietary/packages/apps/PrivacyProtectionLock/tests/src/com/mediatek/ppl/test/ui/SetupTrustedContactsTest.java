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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;
import com.mediatek.ppl.ui.SetupTrustedContactsActivity;

public class SetupTrustedContactsTest extends ActivityInstrumentationTestCase2<SetupTrustedContactsActivity> {
    private static final String TAG = "PPL/SetupTrustedContactsTest";

    private Solo mSolo;
    private SetupTrustedContactsActivity mActivity;
    private Button mNextButton;
    private Button mAddButton;

    public SetupTrustedContactsTest() {
        super(SetupTrustedContactsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockDataUtil.preparePlatformManager((PplApplication) getActivity().getApplication());

        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);
        mNextButton = (Button) mActivity.findViewById(R.id.btn_bottom_next);
        mAddButton = (Button) mActivity.findViewById(R.id.btn_setup_trusted_add_contact);
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
     * Operation: NA
     * Check: state & text of view
     * */
    public void test00() {
        MockPplUtil.formatLog(TAG, "test00");
        assertNotNull(mActivity);
        assertNotNull(mNextButton);
        assertNotNull(mAddButton);

        assertEquals(mAddButton.getVisibility(), View.GONE);
        assertFalse(mNextButton.isEnabled());

        mSolo.clickOnView(mSolo.getView(ImageButton.class, 0));
        mSolo.goBack();
    }

    /**
     * Operation: set all three EditText
     * Check: text in all three EditText
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");
        setAllContact(MockPplUtil.SERVICE_NUMBER_1st);
        MockPplUtil.testUiReceiver(mActivity, "SetupTrustedContactsActivity", PplService.Intents.UI_QUIT_SETUP_WIZARD);

    }

    /**
     * Operation: click delete button of 1st contact
     * Check: NA
     * */
     public void test02() {
         MockPplUtil.formatLog(TAG, "test02");
         clickDel(0, MockPplUtil.SERVICE_NUMBER_1st);
         MockPplUtil.testUiReceiver(mActivity, "SetupTrustedContactsActivity", PplService.Intents.UI_NO_SIM);
     }

     /**
      * Operation: click delete button of 2nd contact
      * Check: NA
      * */
     public void test03() {
         MockPplUtil.formatLog(TAG, "test03");
         mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         clickDel(1, MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Operation: click delete button of 3rd contact
      * Check: NA
      * */
     public void test04() {
         MockPplUtil.formatLog(TAG, "test04");
         mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         mSolo.enterText(1, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         clickDel(2, MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Operation: choose a contact number from a contact
      * Check: text in 1st EditText
      * */
     public void test05() {
         MockPplUtil.formatLog(TAG, "test05");
         String[] number = {MockPplUtil.SERVICE_NUMBER_1st, MockPplUtil.SERVICE_NUMBER_1st};
         MockPplUtil.displayNumDiag(mActivity, number, "TEST", 0);

         ListView listView = (ListView) mSolo.getView(ListView.class, 0);
         if (listView != null) {
             mSolo.clickOnView(listView.getChildAt(0));
         }
     }

     /**
      * Operation: set all three EditText (LANDSCAPE screen)
      * Check: text in all three EditText
      * */
     public void test11() {
         MockPplUtil.formatLog(TAG, "test11");
         mSolo.setActivityOrientation(Solo.LANDSCAPE);

         setAllContact(MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Operation: click delete button of 1st contact
      * Check: NA
      * */
     public void test12() {
         MockPplUtil.formatLog(TAG, "test12");
         mSolo.setActivityOrientation(Solo.LANDSCAPE);
         clickDel(0, MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Operation: click delete button of 2nd contact
      * Check: NA
      * */
     public void test13() {
         MockPplUtil.formatLog(TAG, "test13");
         mSolo.setActivityOrientation(Solo.LANDSCAPE);

         mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         clickDel(1, MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Operation: click delete button of 3rd contact
      * Check: NA
      * */
     public void test14() {
         MockPplUtil.formatLog(TAG, "test14");
         mSolo.setActivityOrientation(Solo.LANDSCAPE);

         mSolo.enterText(0, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         mSolo.enterText(1, MockPplUtil.SERVICE_NUMBER_1st);
         mSolo.clickOnButton(mAddButton.getText().toString());

         clickDel(2, MockPplUtil.SERVICE_NUMBER_1st);
     }

     /**
      * Click delete button
      * */
     private void clickDel(int textId, String number) {
         mSolo.enterText(textId, number);
         checkEditText(textId, number);
         mSolo.clickOnView(mSolo.getView(ImageButton.class, textId * 2 + 1));
     }

     /**
      * Input three number for *ContactsActivities
      * */
     private void setAllContact(String number) {
         for (int i = 0; i < number.length(); ++i) {
             mSolo.enterText(0, number.substring(i, i + 1));
         }
         mSolo.clickOnButton(mAddButton.getText().toString());
         checkEditText(0, number);

         mSolo.enterText(1, number);
         mSolo.clickOnButton(mAddButton.getText().toString());
         checkEditText(1, number);

         mSolo.enterText(2, number);
         checkEditText(2, number);
     }

     private void checkEditText(int textId, String number) {
         String text = "";
         EditText editText = (EditText) mSolo.getView(EditText.class, textId);
         if (editText != null) {
             text = editText.getText().toString();
         }
         assertEquals(text.trim(), number.trim());
     }
}

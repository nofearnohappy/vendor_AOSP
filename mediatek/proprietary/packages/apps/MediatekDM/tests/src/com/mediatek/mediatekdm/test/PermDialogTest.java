/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm.test;

import android.app.NotificationManager;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.mediatekdm.CollectSetPermissionControl;
import com.mediatek.mediatekdm.CollectSetPermissionDialog;
import com.mediatek.mediatekdm.DmConst;

public class PermDialogTest extends ActivityInstrumentationTestCase2<CollectSetPermissionDialog> {
    private static final String TAG = "MDMTest/PermDialogTest";

    private Solo mSolo;
    private CollectSetPermissionDialog mActivity;

    public PermDialogTest() {
        super(CollectSetPermissionDialog.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), mActivity);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mSolo.finishOpenedActivities();
            clearNotification();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.tearDown();
    }

    /**
     * Operation: rotate Check: NA
     */
    public void test00() {
        CollectSetPermissionControl.getInstance().isPermFileReady();
        CollectSetPermissionControl.getInstance().resetKeyValue();

        Log.i(TAG, "test00.");
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
    }

    /**
     * Operation: click ok Check: NA
     */
    public void test01() {
        Log.i(TAG, "test01.");
        mSolo.clickOnButton(mSolo.getString(android.R.string.ok));
    }

    /**
     * Operation: click check box and ok Check: NA
     */
    public void test02() {
        Log.i(TAG, "test02.");
        mSolo.clickOnCheckBox(0);
        mSolo.clickOnButton(mSolo.getString(android.R.string.ok));
    }

    /**
     * Operation: click cancel -> ok Check: NA
     */
    public void test03() {
        Log.i(TAG, "test03.");
        Log.i(TAG, "Click cancle button.");
        mSolo.clickOnButton(mSolo.getString(android.R.string.cancel));

        Log.i(TAG, "Click ok button of 2nd dialogue");
        mSolo.clickOnButton(mSolo.getString(android.R.string.ok));
    }

    /**
     * Operation: click cancel -> cancel Check: NA
     */
    public void test04() {
        Log.i(TAG, "test04.");
        Log.i(TAG, "Click cancle button.");
        mSolo.clickOnButton(mSolo.getString(android.R.string.cancel));

        Log.i(TAG, "Click cancel button of 2nd dialogue");
        mSolo.clickOnButton(mSolo.getString(android.R.string.cancel));

        CollectSetPermissionControl.getInstance().resetKeyValue();
    }

    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) mActivity
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager
                .cancel(DmConst.NotificationInteractionType.TYPE_COLLECT_SET_PERM_NOTIFICATION);
    }
}

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

package com.mediatek.engineermode.tests;

import android.R.string;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.sensor.Sensor;
import com.mediatek.engineermode.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SensorTest extends ActivityInstrumentationTestCase2<Sensor> {

    private static final int LIST_ITEMS_COUNT = 9;
    private static final int FIR_SPINNER_COUNT = 6;

    private Solo mSolo;
    private Context mContext;
    private Instrumentation mIns;
    private Activity mActivity;
    private ListView mSensorFuncList;
    public SensorTest() {
        super("com.mediatek.engineermode", Sensor.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mIns = getInstrumentation();
        mContext = mIns.getTargetContext();
        mActivity = getActivity();
        mSolo = new Solo(mIns, mActivity);
        mSensorFuncList = (ListView) mActivity.findViewById(R.id.list_view_calibration);
    }

    public void testCase01_TestListView() {
        verifyPreconditions();
        int count = mSensorFuncList.getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            mSolo
                    .clickOnText(mSensorFuncList.getAdapter().getItem(i)
                            .toString());
            mSolo.sleep(EmOperate.TIME_LONG);
            mSolo.goBackToActivity(Sensor.class.getSimpleName());
        }
    }

    public void testCase02_TestMSensor() {
        verifyPreconditions();
        mSolo.clickOnText(mSensorFuncList.getAdapter().getItem(0).toString());
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.clickOnText(mActivity.getResources().getString(
                    R.string.msensor_current_data));
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.goBackToActivity(Sensor.class.getSimpleName());
    }

    public void testCase03_TestGSensor() {
        verifyPreconditions();
        mSolo.clickOnText(mSensorFuncList.getAdapter().getItem(1).toString());
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.clickOnText(mActivity.getResources().getString(
                    R.string.sensor_calibration_gsensor));
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.clickOnButton(0);
        mSolo.sleep(EmOperate.TIME_LONG);
        mSolo.goBack();

        mSolo.clickOnText(mActivity.getResources().getString(
                    R.string.sensor_calibration_gyroscope));
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.clickOnButton(0);
        mSolo.sleep(EmOperate.TIME_LONG);
        mSolo.goBack();
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.goBack();

    }

    public void testCase04_TestPSensor() {
        verifyPreconditions();
        mSolo.clickOnText(mSensorFuncList.getAdapter().getItem(2).toString());
        mSolo.sleep(EmOperate.TIME_MID);


        mSolo.clickOnText(mActivity.getResources().getString(
                    R.string.psensor_calibration_select));
        mSolo.sleep(EmOperate.TIME_MID);
        mSolo.clickOnText(mActivity.getResources().getString(
                    R.string.psensor_calibration));
        for (int i = 0; i < 4; i++) {
            mSolo.clickOnButton(i);
            mSolo.sleep(EmOperate.TIME_MID);
        }
        mSolo.goBackToActivity(Sensor.class.getSimpleName());
    }
    private void verifyPreconditions() {
        assertTrue(mIns != null);
        assertTrue(mActivity != null);
        assertTrue(mContext != null);
        assertTrue(mSolo != null);
        assertTrue(mSensorFuncList != null);
    }
}

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

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.test.ActivityUnitTestCase;
import android.widget.CheckBox;

import com.mediatek.engineermode.R;
import com.mediatek.engineermode.bandselect.BandSelect;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

public class BandSelectTest extends ActivityUnitTestCase<BandSelect> {
    private boolean mNetType;
    private Context mContext = null;
    private Instrumentation mInst = null;
    private Intent mStartIntent = null;

    public BandSelectTest() {
        super(BandSelect.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mNetType = getModemType();
        mInst = this.getInstrumentation();
        mContext = mInst.getContext();
        mStartIntent = new Intent(Intent.ACTION_MAIN);
        mStartIntent.setComponent(new ComponentName(mContext, BandSelect.class
                .getName()));
    }

    @Override
        protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test01_Precodition() {
        assertNotNull(mInst);
        assertNotNull(mContext);
        BandSelect activity = startActivity(mStartIntent, null, null);
        assertNotNull(activity);
    }

    public void test02_CurrentModeCheck() {
        BandSelect activity = startActivity(mStartIntent, null, null);
        if (mNetType) {
            CheckBox gsmChk = (CheckBox) activity
                    .findViewById(R.id.TDD_GSM_EGSM900);
            CheckBox dcsChk = (CheckBox) activity
                    .findViewById(R.id.TDD_GSM_DCS1800);
            assertEquals(true, gsmChk.isChecked());
            assertEquals(true, dcsChk.isChecked());
        } else {
            CheckBox gsmChk = (CheckBox) activity
                    .findViewById(R.id.BandSel_GSM_EGSM900);
            CheckBox dcsChk = (CheckBox) activity
                    .findViewById(R.id.BandSel_GSM_DCS1800);
            assertEquals(true, gsmChk.isChecked());
            assertEquals(true, dcsChk.isChecked());
        }
    }

    private static boolean getModemType() {
        boolean tddMode;
        int mask = WorldPhoneUtil.get3GDivisionDuplexMode();
        if (mask == 1) {
                    tddMode = false;
        } else if (mask == 2) {
            tddMode = true;
        } else {
            tddMode = false;
        }
        return tddMode;
    }
}

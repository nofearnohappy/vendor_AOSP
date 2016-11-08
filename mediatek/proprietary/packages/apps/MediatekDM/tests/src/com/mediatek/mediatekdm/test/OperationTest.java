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

import android.test.ApplicationTestCase;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperationManager;

public class OperationTest extends ApplicationTestCase<DmApplication> {
    public static final String TAG = "MDMTest/OperationTest";

    public OperationTest() {
        super(DmApplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "OperationTest.setUp()");
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "OperationTest.tearDown()");
        super.tearDown();
    }

    public void testcase01() {
        DmOperationManager om = DmOperationManager.getInstance();
        assertNotNull(om);
        assertSame(om, DmOperationManager.getInstance());
    }

    public void testcase02() {
        assertTrue(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_CI + "xxx"));
        assertTrue(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_CI_FUMO));
        assertTrue(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_REPORT_SCOMO));
        assertTrue(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_REPORT_FUMO));
        assertTrue(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_REPORT_LAWMO));
        assertFalse(DmOperation.Type.isCIOperation(DmOperation.Type.TYPE_SI));

        assertTrue(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_SI));
        assertTrue(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_SI + "xxx"));
        assertFalse(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_CI_FUMO));
        assertFalse(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_REPORT_SCOMO));
        assertFalse(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_REPORT_FUMO));
        assertFalse(DmOperation.Type.isSIOperation(DmOperation.Type.TYPE_REPORT_LAWMO));

        assertFalse(DmOperation.Type.isReportOperation(DmOperation.Type.TYPE_SI));
        assertFalse(DmOperation.Type.isReportOperation(DmOperation.Type.TYPE_CI_FUMO));
        assertTrue(DmOperation.Type.isReportOperation(DmOperation.Type.TYPE_REPORT_SCOMO));
        assertTrue(DmOperation.Type.isReportOperation(DmOperation.Type.TYPE_REPORT_FUMO));
        assertTrue(DmOperation.Type.isReportOperation(DmOperation.Type.TYPE_REPORT_LAWMO));

        assertTrue(DmOperation.Type.isDLOperation(DmOperation.Type.TYPE_DL));
        assertTrue(DmOperation.Type.isDLOperation(DmOperation.Type.TYPE_DL + "xxx"));
        assertFalse(DmOperation.Type.isDLOperation("xxx" + DmOperation.Type.TYPE_DL));
    }

    public void testcase03() {

    }
}

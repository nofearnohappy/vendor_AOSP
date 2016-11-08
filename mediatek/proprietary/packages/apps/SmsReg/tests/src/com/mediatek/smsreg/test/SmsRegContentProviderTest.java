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

package com.mediatek.smsreg.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.util.Log;

import com.mediatek.smsreg.SmsRegContentProvider;
import com.mediatek.smsreg.test.util.MockSmsRegUtil;

import java.io.IOException;

public class SmsRegContentProviderTest extends ProviderTestCase2<SmsRegContentProvider> {
    private static final String TAG = "SmsReg/ContentProviderTest";
    private static final String AUTHORITY = "com.mediatek.providers.smsreg";
    private static final String PATH_PROVIDER = "content://" + AUTHORITY;

    private String[] mSmsRegColumn = { "autoReboot", "enable", "imei", "op", "smsNumber",
            "smsPort", "manufacturer", "product", "version" };
    private ContentResolver mResolver;

    public SmsRegContentProviderTest() {
        super(SmsRegContentProvider.class, AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        mResolver = null;
        super.tearDown();
    }

    /**
     * Operation: invoke method of query() Check: NA
     */
    public void test00() throws IOException {
        MockSmsRegUtil.formatLog(TAG, "test00");

        Cursor cursor = mResolver.query(Uri.parse(PATH_PROVIDER), mSmsRegColumn, null, null, null);
        assertNotNull(cursor);
        if (cursor == null) {
            Log.i(TAG, "Query cursor == null");
        } else {
            try {
                cursor.moveToFirst();
                String autoReboot = cursor.getString(cursor.getColumnIndex("autoReboot"));
                Log.i(TAG, "Query autoReboot = " + autoReboot);

                String enable = cursor.getString(cursor.getColumnIndex("enable"));
                Log.i(TAG, "Query enable = " + enable);

                String imei = cursor.getString(cursor.getColumnIndex("imei"));
                Log.i(TAG, "Query imei = " + imei);

                String op = cursor.getString(cursor.getColumnIndex("op"));
                Log.i(TAG, "Query op = " + op);

                String smsNumber = cursor.getString(cursor.getColumnIndex("smsNumber"));
                Log.i(TAG, "Query smsNumber = " + smsNumber);

                String smsPort = cursor.getString(cursor.getColumnIndex("smsPort"));
                Log.i(TAG, "Query smsPort = " + smsPort);

                String manufacturer = cursor.getString(cursor.getColumnIndex("manufacturer"));
                Log.i(TAG, "Query manufacturer = " + manufacturer);

                String product = cursor.getString(cursor.getColumnIndex("product"));
                Log.i(TAG, "Query product = " + product);

                String version = cursor.getString(cursor.getColumnIndex("version"));
                Log.i(TAG, "Query version = " + version);
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Operation: invoke method of insert() Check: return value is null
     */
    public void test01() throws IOException {
        MockSmsRegUtil.formatLog(TAG, "test01");
        ContentValues conValues = new ContentValues();
        conValues.put("product", "MTK1");
        Uri uri = mResolver.insert(Uri.parse(PATH_PROVIDER), conValues);
        assertNull(uri);
    }

    /**
     * Operation: invoke method of update() Check: return value is 0
     */
    public void test02() throws IOException {
        MockSmsRegUtil.formatLog(TAG, "test02");
        ContentValues conValues = new ContentValues();
        conValues.put("product", "MTK1");
        int valueUpdate = mResolver.update(Uri.parse(PATH_PROVIDER), conValues, null, null);
        assertEquals(valueUpdate, 0);
    }

    /**
     * Operation: invoke method of delete() Check: return value is 0
     */
    public void test03() throws IOException {
        MockSmsRegUtil.formatLog(TAG, "test03");
        int valDel = mResolver.delete(Uri.parse(PATH_PROVIDER), null, null);
        assertEquals(valDel, 0);
    }
}
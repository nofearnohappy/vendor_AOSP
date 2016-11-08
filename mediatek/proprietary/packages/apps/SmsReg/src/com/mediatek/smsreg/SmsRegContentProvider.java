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

package com.mediatek.smsreg;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SmsRegContentProvider extends ContentProvider {
    private static final String TAG = "SmsReg/ContentProvider";
    private String[] mSmsRegColumn = { "autoReboot", "enable", "imei", "op", "smsNumber", "smsPort", "manufacturer",
            "product", "version" };

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        // Don't SmsApplication.getPlatformManager, as SmsApplication is inited later by system)
        Log.i(TAG, "onCreate..");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i(TAG, "query");

        XmlGenerator xmlGenerator = XmlGenerator.getInstance(SmsRegApplication.getPlatformManager().getConfigPath());

        String autoReboot = "no";
        if (SmsRegApplication.getPlatformManager().isAutoBoot(getContext())) {
            autoReboot = "yes";
        }
        Log.i(TAG, "autoReboot: " + autoReboot);

        String enable = "no";
        if (SmsRegApplication.getPlatformManager().isSmsRegEnabled()) {
            enable = "yes";
        }
        Log.i(TAG, "is SmsReg enable: " + enable);

        String imei = SmsBuilder.getContentInfo(xmlGenerator, "getimei", 0);
        Log.i(TAG, "imei : " + imei);

        String product = SmsBuilder.getContentInfo(xmlGenerator, "getproduct", 0);
        Log.i(TAG, "product : " + product);

        String version = SmsBuilder.getContentInfo(xmlGenerator, "getversion", 0);
        Log.i(TAG, "product : " + version);

        String opName = xmlGenerator.getOperatorName();
        Log.i(TAG, "opName : " + opName);

        String manufacturerName = SmsBuilder.getContentInfo(xmlGenerator, "getvendor", 0);
        Log.i(TAG, "manufacturer : " + manufacturerName);

        String smsNumber = xmlGenerator.getSmsNumber();
        Log.i(TAG, "smsNumber : " + smsNumber);

        String smsPort = Short.toString(xmlGenerator.getSmsPort());
        Log.i(TAG, "smsPort : " + smsPort);

        String[] row = { autoReboot, enable, imei, opName, smsNumber, smsPort, manufacturerName, product, version };

        MatrixCursor cur = new MatrixCursor(mSmsRegColumn);
        cur.addRow(row);

        return cur;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }
}

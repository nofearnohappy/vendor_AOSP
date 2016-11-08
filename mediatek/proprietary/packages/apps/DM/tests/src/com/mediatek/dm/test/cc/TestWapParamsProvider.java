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

package com.mediatek.dm.test.cc;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.mediatek.dm.DmService;
import com.mediatek.dm.cc.DmWAPNodeIoHandler;
import com.redbend.vdm.VdmException;

public class TestWapParamsProvider extends AndroidTestCase {

    /*
     * content://com.android.browser/homepage ./Setting/WAP/HomePage
     */
    // private String[] projection = {"homepage"};

    protected void tearDown() throws Exception {
        DmService.sCCStoredParams.clear();
    }

    public void testRead() {
        Uri treeUri = Uri.parse("./Setting/WAP/HomePage");
        String mccMnc = "46001";
        DmWAPNodeIoHandler handler = new DmWAPNodeIoHandler(mContext, treeUri,
                mccMnc);
        int len = 0;
        byte[] readBytes = new byte[64];
        try {
            len = handler.read(0, readBytes);
        } catch (VdmException e) {
            e.printStackTrace();
            fail("read failed!");
        }
        assertTrue(len > 0);
    }

    public void testReadDB() {
        Uri table = Uri.parse("content://com.android.browser/homepage");
        String recordToRead = null;
        String[] projection = { "homepage" };
        Cursor cur = null;
        try {
            cur = mContext.getContentResolver().query(table, projection, null,
                    null, null);
            if (cur != null && cur.moveToFirst()) {
                int col = cur.getColumnIndex("homepage");
                recordToRead = cur.getString(col);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("read DB failed!");
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        assertNotNull(recordToRead);
        assertTrue(recordToRead.length() > 0);
    }

    public void testWriteDB() {

        Uri table = Uri.parse("content://com.android.browser/homepage");
        ContentValues values = new ContentValues();
        String strToWrite = "http://www.test.com";
        values.put("homepage", strToWrite);
        mContext.getContentResolver().update(table, values, null, null);

        Cursor cur = null;
        String[] projection = { "homepage" };
        String recordToRead = null;
        try {
            cur = mContext.getContentResolver().query(table, projection, null,
                    null, null);
            if (cur != null && cur.moveToFirst()) {
                int col = cur.getColumnIndex("homepage");
                recordToRead = cur.getString(col);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("read DB failed!");
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        assertNotNull(recordToRead);
        assertEquals(strToWrite, recordToRead);

    }

    public void testWrite() {
        Uri treeUri = Uri.parse("./Setting/WAP/HomePage");
        String mccMnc = "46001";
        DmWAPNodeIoHandler handler = new DmWAPNodeIoHandler(mContext, treeUri,
                mccMnc);
        String recordToWrite = "http://www.google.com";
        byte[] writeBytes = recordToWrite.getBytes();
        int lenToWrite = writeBytes.length;
        try {
            handler.write(0, writeBytes, lenToWrite);
        } catch (VdmException e) {
            e.printStackTrace();
            fail("write failed!");
        }
        byte[] byteRead = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, byteRead);
        } catch (VdmException e) {
            e.printStackTrace();
            fail("write failed!");
        }
        assertTrue(len > 0);
        assertEquals(lenToWrite, len);

    }

}

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
import android.util.Log;

import com.mediatek.dm.DmService;
import com.mediatek.dm.cc.DmMMSNodeIoHandler;
import com.redbend.vdm.VdmException;

import junit.framework.Assert;

public class TestMMSParamsProvider extends AndroidTestCase {

    String[] projection = { "name", "mmsc" };
    //
    Uri table = Uri.parse("content://telephony/carriers");

    private String treeUriMMSCString = "./Setting/MMS/MSCCenter";
    private Uri treeUriMMSC = Uri.parse(treeUriMMSCString);
    private String mccMnc = "46001";
    private String mccMncClause = "mcc='460' AND type='mms' AND mnc='01' AND (sourcetype='0')";

    protected void tearDown() throws Exception {
        DmService.sCCStoredParams.clear();
    }

    public void testContext() {
        Assert.assertNotNull("Context shoudn't be NULL.", this.mContext);
    }

    public String testMMSCProviderRead() {
        Cursor cur = null;
        String recordToRead = null;
        try {
            cur = mContext.getContentResolver().query(table, projection,
                    mccMncClause, null, null);
            if (cur != null && cur.moveToFirst()) {
                int col = cur.getColumnIndex("mmsc");
                recordToRead = cur.getString(col);
                Log.i("test", "recordToRead:" + recordToRead);
                assertNotNull(recordToRead);
            }
        } catch (Exception e) {
            Log.e("test", "test mmsc failed!" + e.toString());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return recordToRead;
    }

    public int testRead() {
        DmMMSNodeIoHandler handler = new DmMMSNodeIoHandler(mContext,
                treeUriMMSC, mccMnc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        assertTrue(len > 0);
        return len;
    }

    public void testMMSCProviderWrite() {
        ContentValues values = new ContentValues();
        String mmscTestString = "http://www.soho.com";
        values.put("mmsc", mmscTestString);
        int count = mContext.getContentResolver().update(table, values,
                mccMncClause, null);
        assert (count > 0);
        String result = testMMSCProviderRead();
        assertNotNull(result);
        assertEquals(mmscTestString, result);
    }

    public void testWrite() {
        DmMMSNodeIoHandler handler = new DmMMSNodeIoHandler(mContext,
                treeUriMMSC, mccMnc);
        String mmscTestString = "http://www.test.com";
        byte[] mmscTestbytes = mmscTestString.getBytes();
        int mmscLen = mmscTestbytes.length;
        try {
            handler.write(0, mmscTestbytes, mmscLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        int len = testRead();
        assertEquals(len, mmscLen);
        String readContent = testMMSCProviderRead();
        assertEquals(mmscTestString, readContent);
    }

}

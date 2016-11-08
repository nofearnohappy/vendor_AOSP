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

import android.app.Service;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmService;
import com.mediatek.dm.cc.DmConnNodeIoHandler;
import com.redbend.vdm.VdmException;

public class TestConnParamsProvider extends AndroidTestCase {
    private static final String TAG = "testcase";

    /*
     * content://telephony/carriers ./Setting/Conn/apn ./Setting/Conn/ProxyAddr
     * ./Setting/Conn/ProxyPort ./Setting/Conn/PPPAuthName
     * ./Setting/Conn/PPPAuthPwd
     */

    private String treeUriStr_apn = "./Setting/Conn/apn";
    private String treeUriStr_proxyAddr = "./Setting/Conn/ProxyAddr";
    private String treeUriStr_proxyPort = "./Setting/Conn/ProxyPort";
    // private String treeUriStr_pppAuthName = "./Setting/Conn/PPPAuthName";
    // private String treeUriStr_pppAuthPwd = "./Setting/Conn/PPPAuthPwd";

    private String mMccMnc = null;

    // private String[] item = { "Cmnet/Name", "Cmnet/apn","Cmwap/Name",
    // "Cmwap/apn", "gprs/Addr",
    // "PortNbr", "csd0/Addr","apn","ProxyAddr","ProxyPort"};
    private String[] projection = { "apn", "name", "name", "apn", "proxy",
            "port", "csdnum", "apn", "proxy", "port" };

    private Uri table = Uri.parse("content://telephony/carriers");

    protected void setUp() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
        assertNotNull("Get TELEPHONY_SERVICE failed.", telephonyManager);
        mMccMnc = telephonyManager.getSimOperator(SubscriptionManager.getDefaultSubId());
        Log.i(TAG, "mMccMnc:" + mMccMnc);
        assertNotNull(mMccMnc);
        assertFalse("MccMnc is empty!", mMccMnc.equals(""));
    }

    protected void tearDown() throws Exception {
        DmService.sCCStoredParams.clear();
    }

    public void testReadApnDB() {
        Cursor cur = null;
        String recordToRead = null;
        String mcc = mMccMnc.substring(0, 3);
        String mnc = mMccMnc.substring(3);
        String selection = "mcc='" + mcc + "' AND (type='default,supl,wap') AND mnc='" + mnc
                + "' AND (sourcetype='0')";

        try {
            cur = mContext.getContentResolver().query(table, projection, selection, null, null);
            assertNotNull(cur);
            assertTrue(selection, cur.getCount() > 0);
            if (cur != null && cur.moveToFirst()) {
                int col = cur.getColumnIndex("apn");
                recordToRead = cur.getString(col);
                Log.i(TAG, "recordToRead:" + recordToRead);
                assertNotNull(recordToRead);
            }
        } catch (Exception e) {
            Log.e(TAG, "test mmsc failed!" + e.toString());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public void testWriteApnDB() {
        String mcc = mMccMnc.substring(0, 3);
        String mnc = mMccMnc.substring(3);
        String selection = "mcc='" + mcc + "' AND (type='default,supl,wap') AND mnc='" + mnc
                + "' AND (sourcetype='0')";
        ContentValues values = new ContentValues();
        String testString = "3gwap";
        values.put("proxy", testString);

        int count = mContext.getContentResolver().update(table, values, selection, null);
        assert (count > 0);

        Cursor cur = null;
        String recordToRead = null;
        try {
            cur = mContext.getContentResolver().query(table, projection, selection, null, null);
            assertNotNull(cur);
            assertTrue(selection, cur.getCount() > 0);
            if (cur != null && cur.moveToFirst()) {
                int col = cur.getColumnIndex("proxy");
                recordToRead = cur.getString(col);
                Log.i(TAG, "recordToRead:" + recordToRead);
                assertNotNull(recordToRead);
            }
        } catch (Exception e) {
            Log.e(TAG, "test mmsc failed!" + e.toString());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        assertEquals(testString, recordToRead);
    }

    public void testReadApn() {
        Uri treeUri = Uri.parse(treeUriStr_apn);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        if (len == 0) {
            String recordToWrite = "3gwap";
            byte[] testbytes = recordToWrite.getBytes();
            int testLen = testbytes.length;
            try {
                handler.write(0, testbytes, testLen);
            } catch (VdmException ve) {
                fail("Vdm exception.");
            }
        }

        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        assertTrue(len > 0);
    }

    public void testWriteApn() {
        Uri treeUri = Uri.parse(treeUriStr_apn);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        String recordToWrite = "3gwap";
        byte[] testbytes = recordToWrite.getBytes();
        int testLen = testbytes.length;
        try {
            handler.write(0, testbytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        assertEquals(testLen, len);
    }

    public void testReadProxyAddr() {
        Uri treeUri = Uri.parse(treeUriStr_proxyAddr);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        if (len == 0) {
            String recordToWrite = "10.0.0.172";
            byte[] testbytes = recordToWrite.getBytes();
            int testLen = testbytes.length;
            try {
                handler.write(0, testbytes, testLen);
            } catch (VdmException ve) {
                fail("Vdm exception.");
            }
        }

        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        assertTrue(len > 0);
    }

    public void testWriteProxyAddr() {
        Uri treeUri = Uri.parse(treeUriStr_proxyAddr);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        String recordToWrite = "10.0.0.172";
        byte[] testbytes = recordToWrite.getBytes();
        int testLen = testbytes.length;
        try {
            handler.write(0, testbytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        assertEquals(testLen, len);
    }

    public void testReadProxyPort() {
        Uri treeUri = Uri.parse(treeUriStr_proxyPort);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        if (len == 0) {
            String recordToWrite = "80";
            byte[] testbytes = recordToWrite.getBytes();
            int testLen = testbytes.length;
            try {
                handler.write(0, testbytes, testLen);
            } catch (VdmException ve) {
                fail("Vdm exception.");
            }
        }

        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        assertTrue(len > 0);
    }

    public void testWriteProxyPort() {
        Uri treeUri = Uri.parse(treeUriStr_proxyPort);
        DmConnNodeIoHandler handler = new DmConnNodeIoHandler(this.mContext,
                treeUri, mMccMnc);
        String recordToWrite = "80";
        byte[] testbytes = recordToWrite.getBytes();
        int testLen = testbytes.length;
        try {
            handler.write(0, testbytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        assertEquals(testLen, len);
    }

}

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

import android.content.ContentResolver;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmService;
//import com.mediatek.dm.cc.DmPim1NodeIoHandler;

/**
 * phase out
 */
public class TestPIMParamsProvider extends AndroidTestCase {

    protected void tearDown() throws Exception {
        DmService.sCCStoredParams.clear();
    }

    public void testGetPIM_APN() {
        String apnStr = null;
        ContentResolver resolver = mContext.getContentResolver();
        apnStr = Settings.System.getString(resolver, "PIM_APN");
        Log.d("test", "pim apn:" + apnStr);
        assertNotNull(apnStr);
        // return apnStr;
    }

    public void testGetPIM_SERVER() {
        String pimServer = null;
        ContentResolver resolver = mContext.getContentResolver();
        pimServer = Settings.System.getString(resolver, "PIM_SERVER");
        Log.d("test", "pim server:" + pimServer);
        assertNotNull(pimServer);
        // return pimServer;
    }

    public void testGetPIM_CONTACT() {
        String pimContact = null;
        ContentResolver resolver = mContext.getContentResolver();
        pimContact = Settings.System.getString(resolver, "PIM_CONTACT");
        Log.d("test", "pim contact:" + pimContact);
        assertNotNull(pimContact);
    }

    public void testGetPIM_CALENDAR() {
        String pimCalendar = null;
        ContentResolver resolver = mContext.getContentResolver();
        pimCalendar = Settings.System.getString(resolver, "PIM_CALENDAR");
        Log.d("test", "pim calendar:" + pimCalendar);
        assertNotNull(pimCalendar);
    }

    /*public void atestReadApn() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/ConnProfile");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        Log.d("test", "pim apn len:" + len);
        assertTrue(len > 0);
        // return len;
    }

    public void atestReadAddr() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/Addr");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        Log.d("test", "pim server len:" + len);
        assertTrue(len > 0);
        // return len;
    }

    public void atestReadContact() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/AddressBookURI");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        Log.d("test", "contact len:" + len);
        assertTrue(len > 0);
        // return len;
    }

    public void atestReadCalendar() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/CalendarURI");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        Log.d("test", "calendar len:" + len);
        assertTrue(len > 0);
        // return len;
    }

    public void atestWriteAddr() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/Addr");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        String strToWrite = "http://www.testserver.com";
        byte[] testBytes = strToWrite.getBytes();
        int testLen = testBytes.length;
        try {
            handler.write(0, testBytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String pimServer = null;
        ContentResolver resolver = mContext.getContentResolver();
        pimServer = Settings.System.getString(resolver, "PIM_SERVER");
        Log.d("test", "pim server:" + pimServer);
        assertNotNull(pimServer);

        assertEquals(strToWrite, pimServer);
    }

    public void atestWriteApn() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/ConnProfile");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        String strToWrite = "test wap";
        byte[] testBytes = strToWrite.getBytes();
        int testLen = testBytes.length;
        try {
            handler.write(0, testBytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        ContentResolver resolver = mContext.getContentResolver();
        readResult = Settings.System.getString(resolver, "PIM_APN");
        Log.d("test", "pim apn:" + readResult);
        assertNotNull(readResult);

        assertEquals(strToWrite, readResult);
    }

    public void atestWriteCalendar() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/CalendarURI");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        String strToWrite = "test calendar";
        byte[] testBytes = strToWrite.getBytes();
        int testLen = testBytes.length;
        try {
            handler.write(0, testBytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        ContentResolver resolver = mContext.getContentResolver();
        readResult = Settings.System.getString(resolver, "PIM_CALENDAR");
        Log.d("test", "pim calendar:" + readResult);
        assertNotNull(readResult);

        assertEquals(strToWrite, readResult);
    }

    public void atestWriteContact() {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse("./Setting/PIM/AddressBookURI");
        DmPim1NodeIoHandler handler = new DmPim1NodeIoHandler(mContext,
                treeUri, mncMcc);
        String strToWrite = "test contact";
        byte[] testBytes = strToWrite.getBytes();
        int testLen = testBytes.length;
        try {
            handler.write(0, testBytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        ContentResolver resolver = mContext.getContentResolver();
        readResult = Settings.System.getString(resolver, "PIM_CONTACT");
        Log.d("test", "pim contact:" + readResult);
        assertNotNull(readResult);

        assertEquals(strToWrite, readResult);
    }*/

}

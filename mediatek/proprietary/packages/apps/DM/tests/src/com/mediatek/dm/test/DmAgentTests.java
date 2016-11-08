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

package com.mediatek.dm.test;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Assert;

public class DmAgentTests extends AndroidTestCase {
    private static final String TAG = "[DmAgentTests]";

    private DmAgent mAgent;

    protected void setUp() {
        android.os.IBinder binder = android.os.ServiceManager
                .getService("DmAgent");
        Assert.assertNotNull("get DmAgent service failed.", binder);

        mAgent = DmAgent.Stub.asInterface(binder);
        Assert.assertNotNull("DmAgent asInterface failed.", mAgent);
    }

    /*
     * //not use it to read operator any more public void testReadOperatorName()
     * { try { String opName = mAgent.readOperatorName(); Log.d(TAG,
     * "operator name = " + opName); Assert.assertNotNull(opName); } catch
     * (RemoteException e) { e.printStackTrace();
     * Assert.fail("DmAgent readOperatorName failed"); }
     *
     * }
     */
    public void testReadImsi() {
        String imsi = null;
        try {
            byte[] imsiByte = mAgent.readImsi();
            if (imsiByte != null) {
                imsi = new String(imsiByte);
                Log.d(TAG, "registered Imsi = " + imsi);
            } else {
                return; // the device may be has not imei, like debug phone.
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Assert.fail("DmAgent testReadImsi failed");
        }
        Assert.assertNotNull("imsi read from Nvram is null", imsi);
    }

    public void testLockFlag() {
        try {
            boolean isLocked = mAgent.isLockFlagSet();
            Assert.assertFalse(isLocked);

            boolean setOK = mAgent.setLockFlag("partially".getBytes());
            Assert.assertTrue(setOK);

            boolean isLockedNow = mAgent.isLockFlagSet();
            Assert.assertTrue(isLockedNow);

            boolean clearOK = mAgent.clearLockFlag();
            Assert.assertTrue(clearOK);

            boolean isLockedLast = mAgent.isLockFlagSet();
            Assert.assertFalse(isLockedLast);
        } catch (RemoteException e) {
            e.printStackTrace();
            Assert.fail("DmAgent testLockFlag failed");
        }
    }

    public void testReadWriteImsi() {
        TelephonyManager telMgr = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = telMgr.getSubscriberId();
        Log.d(TAG, "phone's imsi = " + imsi);
        Assert.assertNotNull("imsi read from telephonyManager is null", imsi);

        try {
            boolean ok = mAgent.writeImsi(imsi.getBytes());
            Assert.assertTrue("write IMSI failed!", ok);

            String written = null;
            byte[] writtenByte = mAgent.readImsi();
            if (writtenByte != null) {
                written = new String(writtenByte);
            }
            Log.d(TAG, "registed IMSI = " + written);
            Assert.assertEquals("read IMSI failed!", written, imsi);
        } catch (RemoteException e) {
            e.printStackTrace();
            Assert.fail("DmAgent testReadWriteIMSI failed");
        }

    }

    public void testSetRebootFlag() {
        try {
            boolean ok = mAgent.setRebootFlag();
        } catch (Exception e) {
            Assert.fail("DmAgent test SetRebootFlag failed");
        }

        File file = new File("/cache/recovery/command");
        assertTrue("/cache/recovery/command exists", file.exists());

        byte[] command = "--fota_delta_path=/cache/delta".getBytes();
        int result = 0;
        final int length = command.length;
        byte[] buffer = new byte[length];
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                result = inputStream.read(buffer);
            } catch (Exception e) {
                fail("read /cache/recovery/command failed");
            } finally {
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Assert.fail("DmAgent testSetRebootFlag failed for /cache/recovery/command not found");
        } catch (IOException e) {
            Log.e("DmAgentTests", "IOException " + e.toString());
        }

        assertEquals(length, result);
        for (int i = 0; i < length; ++i) {
            assertEquals(command[i], buffer[i]);
        }

        try {
            mAgent.clearRebootFlag();
        } catch (Exception e) {
            Assert.fail("DmAgent test clearRebootFlag failed");
        }
    }

}

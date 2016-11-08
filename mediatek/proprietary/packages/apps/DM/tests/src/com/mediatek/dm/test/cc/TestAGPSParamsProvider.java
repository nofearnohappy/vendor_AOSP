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

import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmService;
import com.mediatek.dm.cc.DmAGPSNodeIoHandler;
import com.mediatek.lbs.em2.utils.AgpsInterface;
import com.mediatek.lbs.em2.utils.SuplProfile;

import com.redbend.vdm.VdmException;

public class TestAGPSParamsProvider extends AndroidTestCase {
    private static final String TAG = "[DmAGPSTest]";
    private String[] mItem = { "IAPID", "ProviderID", "Name", "PrefConRef", "ToConRef", "ConRef",
            "SLP", "port" };

    private String[] mProjection = { "appId", "providerId", "name", "defaultApn", "optionApn",
            "optionApn2", "addr", "addrType" };

    private String treeUri_ConRef = "./Setting/AGPS/ConRef";
    private String treeUri_toConRef = "./Setting/AGPS/ToConRef";
    private String treeUri_SLP = "./Setting/AGPS/SLP";
    private String treeUri_ProviderID = "./Setting/AGPS/ProviderID";
    private String treeUri_IAPID = "./Setting/AGPS/IAPID";
    private String treeUri_Name = "./Setting/AGPS/Name";
    private String treeUri_port = "./Setting/AGPS/port";
    private String treeUri_PrefConRef = "./Setting/AGPS/PrefConRef";

    private String tableUri = "content://com.settings.agps.profiles_provider/profiles/46007";

    private static final boolean READ_FROM_DB = false;
    private SuplProfile agpsProfile;

    protected void setUp() throws Exception {

        super.setUp();
        Log.e(TAG, "-------------!!!!!!!!!!!!!!---------------------");
        if (READ_FROM_DB) {

        } else {
            AgpsInterface agpsInterface = new AgpsInterface();
            agpsProfile = agpsInterface.getAgpsConfig().curSuplProfile;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        DmService.sCCStoredParams.clear();
    }

    public void testReadConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_ConRef));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null || readResult.equals(agpsProfile.optionalApn2));
    }

    public void testWriteConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_ConRef));
        String recordToWrite = "n/a[1]";
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
        assertEquals(len, testLen);
    }

    public void testReadToConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_toConRef));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null || readResult.equals(agpsProfile.optionalApn));
    }

    public void testWriteToConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_toConRef));
        String recordToWrite = "CMNET";
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
        assertEquals(len, testLen);
    }

    public void testReadSLP() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_SLP));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.addr + ":" + agpsProfile.port));
    }

    public void testWriteSLP() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_SLP));
        String recordToWrite = "221.176.0.55:7275";
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
        assertEquals(len, testLen);
    }

    public void testReadProviderID() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext,
                Uri.parse(treeUri_ProviderID));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.providerId));

    }

    public void testWriteProviderID() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext,
                Uri.parse(treeUri_ProviderID));
        String recordToWrite = "221.176.0.55";
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
        assertEquals(len, testLen);
    }

    public void testReadIAPID() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_IAPID));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.appId));

    }

    public void testWriteIAPID() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_IAPID));
        String recordToWrite = "ap0004";
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
        assertEquals(len, testLen);
    }

    public void testReadName() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_Name));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.name));
    }

    public void testWriteName() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_Name));
        String recordToWrite = "China Mobile AGPS Server";
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
        assertEquals(len, testLen);
    }

    public void testReadport() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_port));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.addressType));

    }

    public void testWriteport() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext, Uri.parse(treeUri_port));
        String recordToWrite = "IPv4address:port";
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
        assertEquals(len, testLen);
    }

    public void testReadPrefConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext,
                Uri.parse(treeUri_PrefConRef));
        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        assertTrue(readResult == null
                || readResult.equals(agpsProfile.defaultApn));

    }

    public void testWritePrefConRef() {
        DmAGPSNodeIoHandler handler = new DmAGPSNodeIoHandler(mContext,
                Uri.parse(treeUri_PrefConRef));
        String recordToWrite = "CMCC WAP";
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
        assertEquals(len, testLen);
    }

}

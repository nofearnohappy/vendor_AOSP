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

import android.test.AndroidTestCase;

import com.mediatek.dm.DmService;
//import com.mediatek.dm.cc.DmPIMNodeIoHandler;
//import com.scan.pimcontentprovider.PimContentProvider;

public class TestScanPIMParamsProvider extends AndroidTestCase {

    private static final String treeUri_DefProfile = "./Setting/PIM/DefProfile";
    private static final String treeUri_ConnProfile = "./Setting/PIM/ConnProfile";
    private static final String treeUri_Addr = "./Setting/PIM/Addr";
    private static final String treeUri_Contact = "./Setting/PIM/AddressBookURI";

    // private static final Uri CONTENT_URI =
    // Uri.parse("content://com.scan.cmpim/item");
    // private static final String[] mProjection =
    // {"account_name","NOT_SURE","server_sync_address","db_name"};

    private static final String databaseUri_DefProfile = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/DefaultProfile";
    private static final String databaseUri_ConnProfile = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/Conn";
    private static final String databaseUri_Addr = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/URL";
    private static final String databaseUri_Contact = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/DataStores/contact/ServerPath";

    private static final String testSstr_DefProfile = "NANE";
    private static final String testStr_ConnProfile = "cmwap";
    private static final String testStr_Addr = "http://218.206.176.241/sync";
    private static final String testStr_Contact = "./contact";

    protected void tearDown() throws Exception {
        DmService.sCCStoredParams.clear();
    }

    /*public void test01ReadPIM_DefProfile() {
        int len = readByHandler(treeUri_DefProfile);
        assertTrue(len > 0);
    }

    public void test02WritePIM_DefProfile() {
        String strToWrite = testSstr_DefProfile;
        writeByHandler(treeUri_DefProfile, strToWrite);

        // String readContent = readFromDatabases(mProjection[0]);
        String readContent = null;
        Uri uri = Uri.parse(databaseUri_DefProfile);
        try {
            readContent = PimContentProvider.ipth_HALGetDefaultPIMProfile(uri,
                    mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(strToWrite, readContent);
    }

    public void test03ReadPIM_Conn() {
        int len = readByHandler(treeUri_ConnProfile);
        assertTrue(len > 0);
    }

    public void test04WritePIM_Conn() {
        String strToWrite = testStr_ConnProfile;
        writeByHandler(treeUri_ConnProfile, strToWrite);
        //
        // String readContent = readFromDatabases(mProjection[1]);
        String readContent = null;
        Uri uri = Uri.parse(databaseUri_ConnProfile);
        try {
            readContent = PimContentProvider
                    .ipth_HALGetCMCCPIMConnectionProfile(uri, mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(strToWrite, readContent);
    }

    public void test05ReadPIM_Addr() {
        int len = readByHandler(treeUri_Addr);
        assertTrue(len > 0);
    }

    public void test06WritePIM_Addr() {
        String strToWrite = testStr_Addr;
        writeByHandler(treeUri_Addr, strToWrite);

        // String readContent = readFromDatabases(mProjection[2]);
        String readContent = null;
        Uri uri = Uri.parse(databaseUri_Addr);
        try {
            readContent = PimContentProvider.ipth_HALGetCMCCPIMServerAddr(uri,
                    mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(strToWrite, readContent);
    }

    public void test07ReadPIM_Contact() {
        int len = readByHandler(treeUri_Contact);
        assertTrue(len > 0);
    }

    public void test08WritePIM_Contact() {
        String strToWrite = testStr_Contact;
        writeByHandler(treeUri_Contact, strToWrite);

        // String readContent = readFromDatabases(mProjection[3]);
        String readContent = null;
        Uri uri = Uri.parse(databaseUri_Contact);
        try {
            readContent = PimContentProvider.ipth_HALGetCMCCPIMContactPath(uri,
                    mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(strToWrite, readContent);
    }

    private void writeByHandler(String treeUriString, String strToWrite) {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse(treeUriString);
        DmPIMNodeIoHandler handler = new DmPIMNodeIoHandler(mContext, treeUri,
                mncMcc);
        byte[] testBytes = strToWrite.getBytes();
        int testLen = testBytes.length;
        try {
            handler.write(0, testBytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
    }

    private int readByHandler(String treeUriString) {
        String mncMcc = "46007";
        Uri treeUri = Uri.parse(treeUriString);
        DmPIMNodeIoHandler handler = new DmPIMNodeIoHandler(mContext, treeUri,
                mncMcc);
        byte[] testBytes = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, testBytes);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
        return len;
    }*/

    /*
     * private String readFromDatabases(String projection) { String param =
     * null; Cursor cursor = mContext.getContentResolver().query( CONTENT_URI,
     * new String[] { projection }, null, null, null); if ((cursor != null) &&
     * (cursor.getCount() > 0)) { cursor.moveToFirst(); param =
     * cursor.getString(0); cursor.close(); } return param; }
     */

}

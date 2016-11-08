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

package com.mediatek.dm.test.scomo;

import android.test.AndroidTestCase;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.mediatek.dm.DmService;
import com.mediatek.dm.ext.MTKMediaContainer;
import com.mediatek.dm.scomo.DmScomoHandler;
import com.mediatek.dm.scomo.DmScomoPackageManager;

import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDc;
import com.redbend.vdm.VdmException;


import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.*;

import junit.framework.Assert;

public class ScomoInstallTest extends AndroidTestCase {
    private static final String TAG = "[ScomoInstallTest]";
    private DmScomoPackageManager mDmScomoPackageManager;
    private DmService mDmService;
    private PackageManager mPackageManager;
    private Context mContext;
    private MTKMediaContainer mMtkContainer;

    private static final String SCOMO_PACKAGE_PATH = "/data/data/com.mediatek.dm/files/scomo.zip";
    private static final String SCOMO_ROOT = "./SCOMO";
    private static final String TEST_PACKAGE = "com.mediatek.packagesender";
    private static final String PACKAGE_DES = "test";
    private static final String SCOMO_STATE_FILE = "scomo_state";
    private static final String TEST_ARCHIVE = "/data/data/com.mediatek.dm/files/packageinfo.zip";

    private static final long DEF_CONTAINER_SHRESHOLD = 1024 * 1024;

    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();

        if (DmService.getInstance() == null) {
            startDmService();
            Thread.sleep(3000); // wait for DM service starting completed
        }
        mDmService = DmService.getInstance();
        mPackageManager = mContext.getPackageManager();
        mDmScomoPackageManager = DmScomoPackageManager.getInstance();
    }

    protected void startDmService() {
        Intent dmIntent = new Intent();
        dmIntent.setAction(Intent.ACTION_DEFAULT);
        dmIntent.setClass(mContext, DmService.class);
        mContext.startService(dmIntent);
    }

    public void testInstallApk() {
        Log.d(TAG, "testInstallApk");
        if (!IsTestFileExists()) {
            Log.d(TAG, "Scomo test package doesn't exist, skip this case.");
            return;
        }
        mDmScomoPackageManager.install(SCOMO_PACKAGE_PATH, null, true);
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    public void testScanPakcage() {
        Log.d(TAG, "testScanPakcage");
        try {
            mDmService.initDmController();
            mDmScomoPackageManager.scanPackage();
            sleep(3000); // sleep a while to ensure vdm init over.
            List<ApplicationInfo> installedList = mPackageManager.getInstalledApplications(0);
            List<VdmScomoDc> dcs = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs();
            Set<String> dcsNames = new HashSet<String>();
            for (VdmScomoDc dc : dcs) {
                dcsNames.add(dc.getId());
            }

            Set<String> appNames = new HashSet<String>();
            for (ApplicationInfo appInfo : installedList) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    continue;
                }
                appNames.add(appInfo.packageName);
            }
            Assert.assertTrue(dcsNames.equals(appNames));
        } catch (VdmException e) {
            Log.d(TAG, "Test scan package failed!");
        }
    }

    public void testGetPacakgeInfo() {
        Log.i(TAG, "Test getPacakgeInfo");
        try {
            mDmScomoPackageManager.getPackageInfo(TEST_PACKAGE);
        } catch (Exception e) {
            Log.e(TAG, "Test getPackageInfo failed!");
        }
        return;
    }

//    public void testGetMinimalPackageInfo() {
//        Log.i(TAG, "Test getMinimalPackageInfo");
//        mDmScomoPackageManager.getMinimalPackageInfo(TEST_ARCHIVE);
//        return;
//    }

    public void testCheckSpace() {
        Log.d(TAG, "Test checkSpace begin");
        boolean bFlagScomo = mDmScomoPackageManager.checkSpace(SCOMO_PACKAGE_PATH);
        Assert.assertTrue(bFlagScomo);
    }

    protected boolean IsTestFileExists() {
        Log.d(TAG, "check if test file exits!");
        File scomoTestFile = new File(SCOMO_PACKAGE_PATH);
        if (!scomoTestFile.exists()) {
            Log.d(TAG, "FOTA package doesn't exist, skip this case.");
            return false;
        }
        return true;
    }

    protected void teardown() throws Exception {
        super.tearDown();
    }
}

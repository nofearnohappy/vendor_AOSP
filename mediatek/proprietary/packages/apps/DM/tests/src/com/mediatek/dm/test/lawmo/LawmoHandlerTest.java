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

package com.mediatek.dm.test.lawmo;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.dm.DmMmiFactory;
import com.mediatek.dm.DmPLFactory;
import com.mediatek.dm.DmService;
import com.mediatek.dm.lawmo.DmLawmoHandler;

import com.redbend.android.VdmLogLevel;

import com.redbend.vdm.VdmComponent;
import com.redbend.vdm.VdmEngine;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;

import java.io.File;


public class LawmoHandlerTest extends AndroidTestCase {
    private static final String TAG = "[LawmoHanderTest]";

    private static final String LAWMO_URI = "./LAWMO/State";
    private DmLawmoHandler mLawmoHandler;
    private DmAgent mDmAgent;
    private VdmEngine mEngine;
    private static final String LAWMO_FILE = "/data/data/com.mediatek.dm/wipe";

    protected void setUp() throws Exception {
        super.setUp();
        boolean treeReady = false;
        try {
            treeReady = DmService.isDmTreeReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue("DmTree is not ready", treeReady);
        if (mEngine == null) {
            Log.v(TAG, "mEngine is null, need init it");
            mEngine = new VdmEngine(mContext, new DmMmiFactory(),
                    new DmPLFactory(mContext));
            mEngine.setDefaultLogLevel(VdmLogLevel.DEBUG);
            mEngine.setComponentLogLevel(VdmComponent.TREE, VdmLogLevel.WARNING);
            mEngine.start();
        }
        assertTrue("Dm Engine is not idle", mEngine.isIdle());

        mLawmoHandler = new DmLawmoHandler(mContext);

        IBinder binder = ServiceManager.getService("DmAgent");
        if (binder != null)
            mDmAgent = DmAgent.Stub.asInterface(binder);
    }

    public void atest00PreCondition() {
    }

    public void aatest01Lock() {
        mLawmoHandler.executePartiallyLock();

        VdmTree mDmTree = new VdmTree();
        try {
            int treeLawmoStatus = mDmTree.getIntValue(LAWMO_URI);
            assertEquals(20, treeLawmoStatus);
        } catch (VdmException e) {
            Log.e(TAG, "mDmTree.getIntValue execption");
        }

        try {
            boolean isLocked = mDmAgent.isLockFlagSet();
            assertTrue(isLocked);

            int lockType = mDmAgent.getLockType();
            assertEquals(0, lockType);
        } catch (RemoteException e) {
            Log.e(TAG, "mDmAgent.isLockFlagSet execption");
        }
    }

    public void atest02Unlock() {
        mLawmoHandler.executeUnLock();

        VdmTree mDmTree = new VdmTree();
        try {
            int treeLawmoStatus = mDmTree.getIntValue(LAWMO_URI);
            assertEquals(30, treeLawmoStatus);
        } catch (VdmException e) {
            Log.e(TAG, "mDmTree.getIntValue execption");
        }

        try {
            boolean isLocked = mDmAgent.isLockFlagSet();
            assertFalse(isLocked);
        } catch (RemoteException e) {
            Log.e(TAG, "mDmAgent.isLockFlagSet execption");
        }
    }

    public void atest03FactoryReset() {
        mLawmoHandler.executeFactoryReset();

        File wipeFile = new File(LAWMO_FILE);
        assertTrue(wipeFile.exists());

        wipeFile.delete();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Log.v(TAG, "---tearDown---");
        if (mEngine != null) {
            Log.v(TAG, "mEngine is not null, need destroy it");
            mEngine.stop();
            mEngine.destroy();
            mEngine = null;
        }
        mLawmoHandler = null;
        mDmAgent = null;
    }
}

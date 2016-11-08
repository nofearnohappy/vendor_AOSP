/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm.lawmo;

import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoAction;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoHandler;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoOperationResult;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoResultCode;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoState;

import java.io.File;
import java.io.IOException;

public class DmLawmoHandler implements LawmoHandler {
    private static final String STATE_URI = "State";

    private MdmTree mDmTree;

    private DmService mService;

    public DmLawmoHandler(DmService service) {
        mService = service;
        mDmTree = new MdmTree();
    }

    public LawmoOperationResult executeFactoryReset() {
        Log.i(TAG.LAWMO, "+executeFactoryReset()");

        if (isUsbMassStorageEnabled()) {
            Log.w(TAG.LAWMO, "phone is in mass storage state, do not execute factory reset");
            return buildResult(LawmoResultCode.WIPE_DATA_NOT_PERFORMED);
        }

        try {
            LawmoComponent component = (LawmoComponent) DmApplication.getInstance()
                    .findComponentByName(LawmoComponent.NAME);
            component.getLawmoManager().setPendingAction(LawmoAction.FACTORY_RESET_EXECUTED);
            PlatformManager.getInstance().setWipeFlag();
            File wipeFile = new File(PlatformManager.getInstance().getPathInData(mService,
                    DmConst.Path.WIPE_FILE));
            wipeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG.LAWMO, "-executeFactoryReset()");
            return buildResult(LawmoResultCode.WIPE_DATA_FAILED);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.i(TAG.LAWMO, "-executeFactoryReset()");
            return buildResult(LawmoResultCode.WIPE_DATA_FAILED);
        }

        Log.i(TAG.LAWMO, "-executeFactoryReset()");
        return buildResult(DmConst.LawmoResult.OPERATION_SUCCESSSFUL);
    }

    public LawmoOperationResult executeFullyLock() {
        // TODO Execute fully lock
        Log.i(TAG.LAWMO, "Execute fully lock");

        return buildResult(DmConst.LawmoResult.OPERATION_SUCCESSSFUL);
    }

    public LawmoOperationResult executePartiallyLock() {
        Log.i(TAG.LAWMO, "+executePartiallyLock");

        try {
            // 1. set flag
            PlatformManager.getInstance().setLockFlag("partially".getBytes());
            Log.i(TAG.LAWMO, "executePartiallyLock partially flag has been set");

            // 2. Set action, broadcast intent & restart if needed
            // we use the same intent as redbend solution
            mService.sendBroadcast(new Intent("com.mediatek.dm.LAWMO_LOCK"));
            Log.i(TAG.LAWMO, "broadcast com.mediatek.dm.LAWMO_LOCK.");

            Log.i(TAG.LAWMO, "isRestartAndroid = " + isRestartAndroid());
            if (isRestartAndroid()) {
                Thread.sleep(1000);
                PlatformManager.getInstance().restartAndroid();
                Log.i(TAG.LAWMO, "executePartiallyLock restart android");
            }

            // 3. Update lawmo state & return result
            mDmTree.replaceIntValue(MdmTree.makeUri(LawmoComponent.ROOT_URI, STATE_URI),
                    LawmoState.PARTIALLY_LOCKED.val);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write, lawmo staus is "
                            + mDmTree.getIntValue(MdmTree.makeUri(LawmoComponent.ROOT_URI,
                                    STATE_URI)));

            Log.i(TAG.LAWMO, "-executePartiallyLock");
            return buildResult(DmConst.LawmoResult.OPERATION_SUCCESSSFUL);

        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.PARTIALLY_LOCK_FAILED);
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.PARTIALLY_LOCK_FAILED);
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.PARTIALLY_LOCK_FAILED);
        }
    }

    public LawmoOperationResult executeUnLock() {
        Log.i(TAG.LAWMO, "+executeUnLock");

        try {
            // 1. Clear flag
            PlatformManager.getInstance().clearLockFlag();

            // 2. broadcast intent & restart if need
            // we use the same intent as redbend solution
            mService.sendBroadcast(new Intent("com.mediatek.dm.LAWMO_UNLOCK"));
            Log.i(TAG.LAWMO, "broadcaste com.mediatek.dm.LAWMO_UNLOCK.");

            if (isRestartAndroid()) {
                Thread.sleep(500);
                PlatformManager.getInstance().restartAndroid();
                Log.i(TAG.LAWMO, "executeUnLock restart android");
            }

            // 3. Update lawmo state & return result
            mDmTree.replaceIntValue(MdmTree.makeUri(LawmoComponent.ROOT_URI, STATE_URI),
                    LawmoState.UNLOCKED.val);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write, lawmo staus is "
                            + mDmTree.getIntValue(MdmTree.makeUri(LawmoComponent.ROOT_URI,
                                    STATE_URI)));

            Log.i(TAG.LAWMO, "-executeUnLock");
            return buildResult(DmConst.LawmoResult.OPERATION_SUCCESSSFUL);

        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.UNLOCK_FAILED);
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.UNLOCK_FAILED);
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, e.getMessage());
            return buildResult(LawmoResultCode.PARTIALLY_LOCK_FAILED);
        }
    }

    public LawmoOperationResult executeWipe(String[] dataToWipe) {
        // TODO Execute wipe command
        Log.i(TAG.LAWMO, "executeWipe Execute wipe command");

        if (dataToWipe.length == 0) {
            return buildResult(LawmoResultCode.OPERATION_SUCCESSSFUL);
        }
        mService.sendBroadcast(new Intent("com.mediatek.mediatekdm.LAWMO_WIPE"));
        return buildResult(LawmoResultCode.OPERATION_SUCCESSSFUL);
    }

    private LawmoOperationResult buildResult(int value) {
        return new LawmoOperationResult(new LawmoResultCode(value));
    }

    private boolean isRestartAndroid() {
        Log.i(TAG.LAWMO, "if restart android when lock and unlock");
        return DmConfig.getInstance().restartOnLock();
    }

    private boolean isUsbMassStorageEnabled() {
        IMountService mountService = IMountService.Stub.asInterface(ServiceManager
                .getService("mount"));
        boolean isUsbMassStorageEnabled;
        try {
            isUsbMassStorageEnabled = mountService.isUsbMassStorageEnabled();
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, "RemoteException when call mountService.isUsbMassStorageConnected()");
            isUsbMassStorageEnabled = false;
            e.printStackTrace();
        }
        Log.i(TAG.LAWMO, "isUsbMassStorageEnabled : " + isUsbMassStorageEnabled);
        return isUsbMassStorageEnabled;
    }
}

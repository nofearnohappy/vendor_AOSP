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

package com.mediatek.dm.lawmo;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.xml.DmXMLParser;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;
import com.redbend.vdm.lawmo.LawmoAction;
import com.redbend.vdm.lawmo.LawmoHandler;
import com.redbend.vdm.lawmo.LawmoOperationResult;
import com.redbend.vdm.lawmo.LawmoResultCode;

import java.io.File;
import java.io.IOException;

public class DmLawmoHandler implements LawmoHandler {

    private static final long SLEEP_TIME = 500;
    private static final String PARTIALLY_LOCK = "partially";
    private Context mContext;
    private DmAgent mDmAgent;
    private VdmTree mDmTree;

    public DmLawmoHandler(Context context) {
        mContext = context;
        mDmAgent = MTKPhone.getDmAgent();
        mDmTree = new VdmTree();
    }

    public LawmoOperationResult executeFactoryReset() {
        Log.d(TAG.LAWMO, "executeFactoryReset Execute factory reset");
        try {
            DmService.sFakeLawmoAction = LawmoAction.FACTORY_RESET_EXECUTED;
            Log.d(TAG.LAWMO, "executeFactoryReset fakeLawmoAction has been set");
            File wipeFile = new File(DmConst.PathName.WIPE_FILE);
            wipeFile.createNewFile();
            Log.d(TAG.LAWMO, "create new file " + DmConst.PathName.WIPE_FILE);
        } catch (IOException e) {
            Log.e(TAG.LAWMO, "executeFactoryReset " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.WIPE_DATA_FAILED));
        }

        return new LawmoOperationResult();
    }

    public LawmoOperationResult executeFullyLock() {
        // TODO Execute fully lock
        Log.i(TAG.LAWMO, "Execute fully lock");
        return new LawmoOperationResult(new LawmoResultCode(
                DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
    }

    public LawmoOperationResult executePartiallyLock() {
        Log.i(TAG.LAWMO, "Execute partially lock");
        try {
            if (mDmAgent == null) {
                Log.w(TAG.LAWMO, "executePartiallyLock mDmAgent is null");
                return new LawmoOperationResult(new LawmoResultCode(
                        LawmoResultCode.PARTIALLY_LOCK_FAILED));
            }
            mDmAgent.setLockFlag(PARTIALLY_LOCK.getBytes());
            Log.i(TAG.LAWMO, "executePartiallyLock partially flag has been set");
            mContext.sendBroadcast(new Intent(DmConst.IntentAction.ACTION_LAWMO_LOCK));
            Log.i(TAG.LAWMO,
                    "executePartiallyLock Intent : com.mediatek.dm.LAWMO_LOCK broadcasted.");

            if (isRestartAndroid()) {
                Thread.sleep(SLEEP_TIME);
                mDmAgent.restartAndroid();
                Log.i(TAG.LAWMO, "executePartiallyLock restart android");
            }

            Log.i(TAG.LAWMO, "Lock OK");

            mDmTree.replaceIntValue(DmConst.LawmoStatus.LAWMO_URI,
                    DmConst.LawmoStatus.PARTIALY_LOCK);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write status, the lawmo staus is "
                            + mDmTree.getIntValue(DmConst.LawmoStatus.LAWMO_URI));

            return new LawmoOperationResult(new LawmoResultCode(
                    DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
        } catch (VdmException e) {
            Log.e(TAG.LAWMO, "executePartiallyLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(
                    LawmoResultCode.PARTIALLY_LOCK_FAILED));
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, "executePartiallyLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(
                    LawmoResultCode.PARTIALLY_LOCK_FAILED));
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, "executePartiallyLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(
                    LawmoResultCode.PARTIALLY_LOCK_FAILED));
        }
    }

    public LawmoOperationResult executeUnLock() {
        Log.i(TAG.LAWMO, "Execute unlock command");

        try {
            if (mDmAgent == null) {
                Log.w(TAG.LAWMO, "executeUnLock mDmAgent is null");
                return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
            }
            mDmAgent.clearLockFlag();
            Log.i(TAG.LAWMO, "executeUnLock flag has been cleared");
            mContext.sendBroadcast(new Intent(DmConst.IntentAction.ACTION_LAWMO_UNLOCK));
            Log.i(TAG.LAWMO, "executeUnLock Intent : com.mediatek.dm.LAWMO_UNLOCK broadcasted.");

            if (isRestartAndroid()) {
                Thread.sleep(SLEEP_TIME);
                mDmAgent.restartAndroid();
                Log.i(TAG.LAWMO, "executeUnLock restart android");
            }
            Log.i(TAG.LAWMO, "UnLock OK");
            mDmTree.replaceIntValue(DmConst.LawmoStatus.LAWMO_URI, DmConst.LawmoStatus.UN_LOCK);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.LAWMO,
                    "After write status, the lawmo staus is "
                            + mDmTree.getIntValue(DmConst.LawmoStatus.LAWMO_URI));

            return new LawmoOperationResult(new LawmoResultCode(
                    DmConst.LawmoResult.OPERATION_SUCCESSSFUL));

        } catch (VdmException e) {
            Log.e(TAG.LAWMO, "executeUnLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        } catch (InterruptedException e) {
            Log.e(TAG.LAWMO, "executeUnLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, "executeUnLock " + e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        }
    }

    public LawmoOperationResult executeWipe(String[] dataToWipe) {
        // TODO Execute wipe command
        Log.i(TAG.LAWMO, "executeWipe Execute wipe command");

        if (dataToWipe.length == 0) {
            return new LawmoOperationResult(new LawmoResultCode(
                    DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
        }
        mContext.sendBroadcast(new Intent(DmConst.IntentAction.ACTION_LAWMO_WIPE));
        return new LawmoOperationResult(new LawmoResultCode(
                DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
    }

    /**
     * whether need restart android to kill all running applications.
     *
     * @return boolean
     */
    public static boolean isRestartAndroid() {
        Log.i(TAG.LAWMO, "if restart android when lock and unlock");
        boolean ret = false;
        try {
            File configFileInSystem = new File(DmConst.PathName.CONFIG_FILE_IN_SYSTEM);
            if (configFileInSystem != null && configFileInSystem.exists()) {
                DmXMLParser xmlParser = new DmXMLParser(DmConst.PathName.CONFIG_FILE_IN_SYSTEM);
                if (xmlParser != null) {
                    String ifRestartAndroid = xmlParser.getValByTagName("LockRestart");
                    Log.i(TAG.LAWMO, "the restart flag is " + ifRestartAndroid);
                    ret = "yes".equalsIgnoreCase(ifRestartAndroid);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG.LAWMO, "isRestartAndroid, " + e.getMessage());
        }
        return ret;
    }

}
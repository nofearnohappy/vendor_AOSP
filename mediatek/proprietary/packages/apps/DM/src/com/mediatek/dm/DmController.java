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

package com.mediatek.dm;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.bootstrap.DmBootstrapHandler;
import com.mediatek.dm.bootstrap.DmCpObserver;
import com.mediatek.dm.conn.DmDatabase;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.ext.MTKOptions;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.fumo.DmFumoHandler;
import com.mediatek.dm.fumo.FOTADeltaFiles;
import com.mediatek.dm.fumo.FumoExtNodeIoHandler;
import com.mediatek.dm.lawmo.DmLawmoHandler;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.scomo.DmPLInventory;
import com.mediatek.dm.scomo.DmScomoDcHandler;
import com.mediatek.dm.scomo.DmScomoDpHandler;
import com.mediatek.dm.scomo.DmScomoHandler;
import com.mediatek.dm.session.DmSessionStateObserver;
import com.mediatek.dm.session.DmSessionStateObserver.DmAction;

import com.redbend.android.VdmLogLevel;

import com.redbend.vdm.ActiveSessType;
import com.redbend.vdm.BootProfile;
import com.redbend.vdm.CpSecurity;
import com.redbend.vdm.SessionInitiator;
import com.redbend.vdm.VdmComponent;
import com.redbend.vdm.VdmEngine;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;
import com.redbend.vdm.fumo.FumoAction;
import com.redbend.vdm.fumo.FumoState;
import com.redbend.vdm.fumo.VdmFumo;
import com.redbend.vdm.fumo.FumoResultCode;
import com.redbend.vdm.lawmo.LawmoAction;
import com.redbend.vdm.lawmo.LawmoOperationType;
import com.redbend.vdm.lawmo.LawmoResultCode;
import com.redbend.vdm.lawmo.VdmLawmo;
import com.redbend.vdm.scomo.ScomoAction;
import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDc;
import com.redbend.vdm.scomo.VdmScomoDp;

import java.io.IOException;
import java.util.ArrayList;

import junit.framework.Assert;

/**
 * This class wraps vdm classes, including vdm engine, vdm fumo and vdm tree.
 */
public class DmController {
    private static final String NO_WAIT = "nowait";
    private static final String INTERVAL = "interval";
    private static final String WINDOW = "window";
    private VdmEngine mEngine;
    private VdmTree mDmTree;
    private static VdmFumo sFumo;
    private DmFumoHandler mFumoHandler;
    private static VdmLawmo sLawmo;
    private static VdmScomo sScomo;
    private DmLawmoHandler mLawmoHandler;
    private DmNiaMsgHandler mDmNiaMsgHandler;
    private DmSessionStateObserver mSessionStateObserver;
    private DmDevSwVNodeIOHandler mSwVIOHandler;
    private DmDevIdNodeIOHandler mDevIdIOHandler;
    private DmPLLogger mLogger;
    private DmConfig mDmConfig;
    private Context mContext;

    /**
     * Constructor of DmController. Initiate vdm controls.
     */
    public DmController(final Context context) {
        Log.i(TAG.CONTROLLER, "Enter DmController constructor");
        mLogger = new DmPLLogger(DmService.getInstance());
        Log.i(TAG.CONTROLLER, "Logger created");
        createEngine(DmService.getInstance());
        mDmNiaMsgHandler = new DmNiaMsgHandler();
        mSessionStateObserver = new DmSessionStateObserver();
        mDmConfig = new DmConfig(DmService.getInstance());
        mDmConfig.configure();
        setTimeout(DmConst.Time.TIMEOUTVAL);
        startEngine();
        createFumo(DmConst.NodeUri.FUMO_ROOT);
        if (Options.LAWMO_SUPPORT) {
            createLawmo(DmConst.NodeUri.LAWMO_ROOT, context);
        }
        if (Options.SCOMO_SUPPORT) {
            createScomo();
        }
        mDmTree = new VdmTree();

        if (!Options.USE_DIRECT_INTERNET) {
            Log.i(TAG.CONTROLLER, "Start to sync dm address");
            if (!syncDmServerAddr()) {
                Log.e(TAG.CONTROLLER, "Sync Dm tree with database failed!!");
            }
        }
        syncLawmoStatus();
        mSwVIOHandler = new DmDevSwVNodeIOHandler();
        mDevIdIOHandler = new DmDevIdNodeIOHandler(context);
        registerSwVNodeIOHandler();
        registerDevIdNodeIOHandler();

        TelephonyManager telMgr = TelephonyManager.getDefault();
        if (telMgr == null) {
            Log.e(TAG.CONTROLLER, "Get TelephonyManager failed.");
            return;
        }

        if (Options.USE_SMS_REGISTER) {
            int subId = DmCommonFun.getRegisterSubID(context);
            if (subId != -1) {
                String mccmnc = telMgr.getSimOperator(subId);
                Log.w(TAG.CONTROLLER, "reg sim mccmnc: " + mccmnc);
                DmRegister dr = new DmRegister(mContext, mccmnc);
                dr.registerCCNodeIoHandler(DmConst.PathName.TREE_FILE_IN_DATA);
                if (MTKOptions.MTK_IMS_SUPPORT) {
                    dr.registerImsNodeIoHandler(DmConst.PathName.TREE_FILE_IN_DATA);
                }
            } else {
                Log.e(TAG.CONTROLLER, "SIM reg not finished or wrong");
            }
        } else {
            String mccmnc = telMgr.getSimOperator();
            Log.w(TAG.CONTROLLER, "Sim1 mccmnc: " + mccmnc);
            DmRegister dr = new DmRegister(mContext, mccmnc);
            dr.registerCCNodeIoHandler(DmConst.PathName.TREE_FILE_IN_DATA);
            if (MTKOptions.MTK_IMS_SUPPORT) {
                dr.registerImsNodeIoHandler(DmConst.PathName.TREE_FILE_IN_DATA);
            }
        }

        registerSessionStateObserver();
        registerCpObserver();
        Log.i(TAG.CONTROLLER, "Exit DmController constructor");
    }

    /**
     * Create vmd engine
     *
     * @param Context
     *            context - context that use vdm engine.
     */
    public void createEngine(Context context) {
        try {
            mContext = context;
            Log.i(TAG.CONTROLLER, "Create vdm engine.");
            mEngine = new VdmEngine(mContext, new DmMmiFactory(), new DmPLFactory(mContext));
            mLogger.logMsg(Log.INFO, "VdmEngine created ");
            mLogger.logMsg(
                    Log.INFO,
                    new StringBuilder("/////// vDM Version: ").append(mEngine.getVersion())
                            .append(" ////////////").toString());
            Log.i(TAG.CONTROLLER, "Vdm engine created.");
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "Create vdm engine error.");
            mLogger.logMsg(Log.ERROR, "VdmEngine ctor exception " + e.getError().name());
            e.printStackTrace();
        }
    }

    /**
     * Start vdm engine
     */
    public void startEngine() {
        try {
            Log.i(TAG.CONTROLLER, "Start vdm engine.");
            mEngine.setDefaultLogLevel(VdmLogLevel.DEBUG);
            mEngine.setComponentLogLevel(VdmComponent.TREE, VdmLogLevel.WARNING);
            mEngine.start();
            Log.i(TAG.CONTROLLER, "Vdm engine started.");
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in startEngine()", e);
            mLogger.logMsg(Log.ERROR, "VdmEngine start exception " + e);
        }
    }

    /**
     * Release vdm resources
     */
    public void stop() {
        if (mEngine != null) {
            mEngine.stop();
        }
        terminateFumo();
        terminateLawmo();
        terminateScomo();

        if (mEngine != null) {
            Log.i(TAG.CONTROLLER, "Unregister session state observer.");
            mEngine.unregisterSessionStateObserver(mSessionStateObserver);
            Log.i(TAG.CONTROLLER, "Destroy vdm engine.");
            mEngine.destroy();
            mEngine = null;
        }
    }

    public void stopEngine() {
        if (mEngine != null) {
            Log.w(TAG.CONTROLLER, "stopEngine stop engine");
            mEngine.stop();
        }
    }

    /**
     * Create vdm fumo and fumo handler
     *
     * @param String
     *            fumoRootUri - root node uri of fumo in dm tree
     */
    public void createFumo(String fumoRootUri) {
        mLogger.logMsg(Log.VERBOSE, "Creating FUMO object");
        Log.i(TAG.CONTROLLER, "Create fumo handler.");
        mFumoHandler = new DmFumoHandler();
        Log.i(TAG.CONTROLLER, "Create vdm fumo.");
        sFumo = new VdmFumo(fumoRootUri, mFumoHandler);
        Log.i(TAG.CONTROLLER, "Vdm fumo created.");
    }

    /**
     * Destroy fumo
     */
    public void terminateFumo() {
        if (sFumo != null) {
            Log.i(TAG.CONTROLLER, "Destroy vdm fumo.");
            sFumo.destroy();
            sFumo = null;
        }

    }

    /**
     * Create vdm lawmo and lawmo handler
     *
     * @param String
     *            fumoRootUri - root node uri of fumo in dm tree
     */
    public void createLawmo(String lawmoRootUri, Context context) {
        mLogger.logMsg(Log.VERBOSE, "Creating LAWMO object");
        Log.i(TAG.CONTROLLER, "Create lawmo handler.");
        mLawmoHandler = new DmLawmoHandler(context);
        Log.i(TAG.CONTROLLER, "Create vdm lawmo.");
        sLawmo = new VdmLawmo(lawmoRootUri, mLawmoHandler);
        Log.i(TAG.CONTROLLER, "Vdm lawmo created.");
    }

    /**
     * Destroy lawmo
     */
    public void terminateLawmo() {
        if (sLawmo != null) {
            Log.i(TAG.CONTROLLER, "Destroy vdm lawmo.");
            sLawmo.destroy();
            sLawmo = null;
        }
    }

    public void createScomo() {
        try {
            sScomo = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT, DmScomoHandler.getInstance());
            Log.d(TAG.CONTROLLER, "setAutoAddDPChildNotes to true");
            sScomo.setAutoAddDPChildNodes(true);
            // DmScomoHandler.getInstance().attachToScomo(mScomo);
            ArrayList<VdmScomoDp> dps = sScomo.getDps();
            if (dps != null) {
                for (VdmScomoDp dp : dps) {
                    dp.setHandler(DmScomoDpHandler.getInstance());
                }
            }
            ArrayList<VdmScomoDc> dcs = sScomo.getDcs();
            if (dcs != null) {
                Log.d(TAG.CONTROLLER, "createScomo: dcs size is " + dcs.size());
                for (VdmScomoDc dc : dcs) {
                    dc.setHandler(DmScomoDcHandler.getInstance());
                    dc.setPLInventory(DmPLInventory.getInstance());
                }
            }
        } catch (VdmException e) {
            e.printStackTrace();
        }
    }

    public void terminateScomo() {
        if (sScomo != null) {
            Log.i(TAG.CONTROLLER, "Destroy scomolawmo.");
            sScomo.destroy();
            sScomo = null;
        }
    }

    /**
     * Register node IO handler of getting software version
     */
    public void registerSwVNodeIOHandler() {
        try {
            mDmTree.registerNodeIoHandler(DmConst.NodeUri.DEV_DETAIL_SWV, mSwVIOHandler);
            Log.i(TAG.CONTROLLER, "Node : " + DmConst.NodeUri.DEV_DETAIL_SWV
                    + "IO handler registered");
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in registerSwVNodeIOHandler()", e);
        }
    }

    /**
     * Register node IO handler of getting software version
     */
    public void registerDevIdNodeIOHandler() {
        try {
            mDmTree.registerNodeIoHandler(DmConst.NodeUri.DEV_INFO_DEVID, mDevIdIOHandler);
            Log.i(TAG.CONTROLLER,
                    new StringBuilder("Node : ").append(DmConst.NodeUri.DEV_INFO_DEVID)
                            .append("IO handler registered").toString());
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in registerDevIdNodeIOHandler()", e);
        }
    }

    public void registerFumoExtNodeIoHandler() {
        Log.d(TAG.CONTROLLER, "registerFumoExtNodeIoHandler");

        ArrayList<String> uriList = new ArrayList<String>();
        uriList.add(DmConst.NodeUri.FUMO_EXT_SEVERITY);
        uriList.add(DmConst.NodeUri.FUMO_EXT_URL);
        uriList.add(DmConst.NodeUri.FUMO_EXT_POSTPONE);
        uriList.add(DmConst.NodeUri.FUMO_EXT_POLLFREQUENCY);

        for (String uriStr : uriList) {
            FumoExtNodeIoHandler ioHandler = new FumoExtNodeIoHandler(mContext, uriStr);
            if (ioHandler != null) {
                try {
                    mDmTree.registerNodeIoHandler(uriStr, ioHandler);
                } catch (VdmException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Register session state observer
     */
    public void registerSessionStateObserver() {
        Log.i(TAG.CONTROLLER, "Register session state observer.");
        if (mEngine != null) {
            mEngine.registerSessionStateObserver(mSessionStateObserver);
            Log.i(TAG.CONTROLLER, "Session state observer registered.");
        }
    }

    /**
     * Register cp observer
     */
    private void registerCpObserver() {
        Log.i(TAG.CONTROLLER, "registerCpObserver");
        if (mEngine != null) {
            mEngine.registerCpObserver(new DmCpObserver());
            Log.i(TAG.CONTROLLER, "CpObserver registered.");
        }
    }

    /**
     * Set time out value
     *
     * @param int seconds - time out value
     */
    public void setTimeout(int seconds) {
        Log.i(TAG.CONTROLLER, "Set connection time out : " + seconds + "seconds");
        if (mEngine != null) {
            mEngine.setConnectionTimeout(seconds);
        }
        Log.i(TAG.CONTROLLER, "Connection time out set.");
    }

    /**
     * Trigger fumo session
     *
     * @param byte[] message - message transfered to vdm fumo. Could be null.
     */
    public void triggerFumoSession(byte[] message, VdmFumo.ClientType clientType) {
        // VdmFumo.ClientType clientType = VdmFumo.ClientType.USER;
        try {
            // byte[] message1 = new byte[10];
            // mFumo.triggerSession(message1, clientType);
            Log.d(TAG.CONTROLLER, "<---[fumo]trigger session start--->");
            sFumo.triggerSession(null, clientType);
            Log.d(TAG.CONTROLLER, "<---[fumo]session triggerred--->");
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in triggerFumoSession()", e);
        }
    }

    public FumoState getFumoState() {
        FumoState state = FumoState.IDLE;
        try {
            if (sFumo != null) {
                state = sFumo.getState();
            }
        } catch (VdmException e) {
            Log.w(TAG.CONTROLLER, e.getMessage());
        }
        return state;
    }

    /**
     * Confirm to start download
     */
    public void proceedDLSession() {
        try {
            boolean isNeedResume = PersistentContext.getInstance(mContext)
                    .getIsNeedResumeDLSession();
            if (isNeedResume) {
                Log.v(TAG.CONTROLLER, "abnormal flow, need resume dl session");
                PersistentContext.getInstance(mContext).setIsNeedResumeDLSession(false);
                sFumo.resumeDLSession();
            } else {
                Log.v(TAG.CONTROLLER, "normal flow, proceed dl session");
                mEngine.notifyDLSessionProceed();
            }
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in proceedDLSession()", e);
        }
    }

    /**
     * Cancel download session
     */
    public void cancelDLSession() {
        try {
            mEngine.cancelActiveSession(ActiveSessType.DL);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in cancelDLSession()", e);
        }
    }

    /**
     * Resume download session
     */
    public void resumeDLSession() {
        Log.i(TAG.CONTROLLER, "resuming DL session.");
        try {
            sFumo.resumeDLSession();
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in resumeDLSession()", e);
        }
    }

    /**
     * Execute firmware update
     */
    public void executeFwUpdate() {
        // Set flag to update
        Log.d(TAG.CONTROLLER, "setting reboot to recovery flag.");

        // Update DL status
        PersistentContext pc = PersistentContext.getInstance(mContext);
        pc.setDLSessionStatus(IDmPersistentValues.STATE_UPDATE_RUNNING);

        if (pc.getIsUpdateRecovery()) {
            updateRecovery();
        } else {
            setUpdateFlag();
        }
        // Reboot to update firmware
        Intent intentReboot = new Intent(Intent.ACTION_REBOOT);
        intentReboot.putExtra(NO_WAIT, 1);
        intentReboot.putExtra(INTERVAL, 1);
        intentReboot.putExtra(WINDOW, 0);
        mContext.sendBroadcast(intentReboot);
    }

    /**
     * Set update flag
     *
     * @return If set flag successfully, return true, else return false
     */
    private Boolean setUpdateFlag() {
        Log.i(TAG.CONTROLLER, "Set recovery fota command.");
        Boolean ret = true;
        DmAgent agent = MTKPhone.getDmAgent();

        if (agent != null) {
            try {
                if (MTKOptions.MTK_EMMC_SUPPORT) {
                    if (agent.clearOtaResult() == 0) {
                        Log.e(TAG.CONTROLLER, "clear Ota Result false");
                        return false;
                    }
                }
                ret = agent.setRebootFlag();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * update recovery partition, a blocking method.
     */
    private void updateRecovery() {
        Log.i(TAG.CONTROLLER, "[updateRecovery]");
        SystemProperties.set("ctl.start", "rbfota");
        int counter = 0;
        while(true) {
            try {
                Thread.sleep(2000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            String result = SystemProperties.get("init.svc.rbfota");
            if (result != null && result.equals("stopped")) {
                // fota1 execute over.
                Log.i(TAG.CONTROLLER, "[updateRecovery] process exitValue = " + result);
                return;
            }
            counter++;
            if (counter > 100) {
                Log.i(TAG.CONTROLLER, "[updateRecovery] update recovery time out!");
                return;
            }
        }
    }

    /**
     * Trigger report session
     *
     * @param FumoResultCode inResultCode - result of update
     */
    public void triggerReportSession(FumoResultCode inResultCode) {
        try {
            Log.i(TAG.CONTROLLER,
                    "triggerReportSession fumo report result to server, result code is "
                            + inResultCode);
            sFumo.triggerReportSession(inResultCode);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in triggerReportSession(" + inResultCode + ")", e);
        }
    }

    /**
     * Trigger NIA dm session
     *
     * @param byte[] message - message body of wap push
     */
    public void triggerNiaDmSession(byte[] message) {
        if (mEngine == null) {
            Log.w(TAG.CONTROLLER, "triggerNiaDmSession mEngine is null");
            return;
        }
        try {
            mEngine.triggerNIADmSession(message, mDmNiaMsgHandler, mDmNiaMsgHandler);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in triggerNiaDmSession()", e);
        }
    }

    /**
     * Proceed NIA dm session
     */
    public void proceedNiaSession() {
        try {
            mEngine.notifyNIASessionProceed();
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in proceedNiaSession()", e);
        }
    }

    /**
     * Trigger boot strap session
     *
     * @param BootProfile
     *            profile - boot profile
     * @param CpSecurity
     *            security - security level
     * @param String
     *            mac - mac region of wap push
     * @param byte[] message - message body of wap push
     */
    public void triggerBootstrapSession(BootProfile profile, CpSecurity security, String mac,
            byte[] message) {
        DmBootstrapHandler dmBootstrapHandler = null;
        if (profile.equals(BootProfile.WAP)) {

            Log.d(TAG.CONTROLLER, "trigger CP_BOOTSTRAP");
            dmBootstrapHandler = new DmBootstrapHandler(
                    DmConst.SessionInitiatorId.INITIATOR_CP_BOOTSTRAP);

        } else {

            Log.d(TAG.CONTROLLER, "trigger DM_BOOTSTRAP");
            dmBootstrapHandler = new DmBootstrapHandler(
                    DmConst.SessionInitiatorId.INITIATOR_DM_BOOTSTRAP);

        }
        try {
            mEngine.triggerBootstrapSession(null, profile, security, mac, message,
                    dmBootstrapHandler, dmBootstrapHandler);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in triggerBootstrapSession()", e);
        }
    }

    public void triggerDmSession(String account, String genericAlertType, SessionInitiator initiator) {
        if (account == null) {
            account = mEngine.getCurrentAccount();
        }
        try {
            mEngine.triggerDMSession(account, genericAlertType, null, initiator);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in triggerBootstrapSession()", e);
        }
    }

    /**
     * The DM tree file is ok or not
     */

    public boolean isIdle() {
        return (mEngine != null) && mEngine.isIdle();
    }

    public void cancelSession() {
        try {
            mEngine.cancelActiveSession(ActiveSessType.ALL);
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "VdmException in cancelSession()", e);
        }
    }

    private void syncLawmoStatus() {
        // boolean isPartillyLock = true;
        boolean isFullyLock = false;
        int lockStatus = -1;
        try {
            DmAgent agent = MTKPhone.getDmAgent();
            if (agent != null) {
                Log.i(TAG.CONTROLLER, "The device lock status is " + agent.isLockFlagSet());
                if (agent.isLockFlagSet()) {
                    // the staus is locked, if it is full lock
                    // isPartillyLock = agent.getLockType();
                    isFullyLock = agent.getLockType() == 1;
                    Log.i(TAG.CONTROLLER, "is fully lock is " + isFullyLock);
                    if (!isFullyLock) {
                        lockStatus = DmConst.LawmoStatus.PARTIALY_LOCK;
                    } else {
                        lockStatus = DmConst.LawmoStatus.FULLY_LOCK;
                    }
                    Log.i(TAG.CONTROLLER, "Lock status is " + lockStatus);
                    if (lockStatus == DmConst.LawmoStatus.FULLY_LOCK
                            || lockStatus == DmConst.LawmoStatus.PARTIALY_LOCK) {
                        int treeLawmoStatus = mDmTree.getIntValue(DmConst.LawmoStatus.LAWMO_URI);
                        Log.i(TAG.CONTROLLER, "Lawmo status in tree is " + treeLawmoStatus);
                        if (lockStatus != treeLawmoStatus) {
                            // need to write dm tree to sync lawmo status
                            mDmTree.replaceIntValue(DmConst.LawmoStatus.LAWMO_URI, lockStatus);
                            mDmTree.writeToPersistentStorage();
                            Log.i(TAG.CONTROLLER, "After write status, the lawmo staus is "
                                    + mDmTree.getIntValue(DmConst.LawmoStatus.LAWMO_URI));
                        }
                    }
                }
            } else {
                Log.e(TAG.CONTROLLER, "DmAgent is null");
                return;
            }
        } catch (VdmException e) {
            Log.e(TAG.CONTROLLER, "get lock status error. VdmException happened.");
            e.printStackTrace();
        } catch (RemoteException e) {
            Log.e(TAG.CONTROLLER, "get lock status error. RemoteException happened");
            e.printStackTrace();
        }

    }

    public boolean triggerLawmoReportSession(LawmoOperationType type, LawmoResultCode code) {
        Log.w(TAG.CONTROLLER, "triggerLawmoReportSession begin");
        boolean ret = true;
        if (sLawmo != null) {
            try {
                Log.w(TAG.CONTROLLER, "triggerLawmoReportSession trigger report session type is "
                        + type);
                sLawmo.triggerReportSession(type, new LawmoResultCode(
                        LawmoResultCode.OPERATION_SUCCESSSFUL));
            } catch (VdmException e) {
                ret = false;
                Log.e(TAG.CONTROLLER, "triggerLawmoReportSession" + e.getMessage());
            }
        } else {
            ret = false;
            Log.e(TAG.CONTROLLER, "triggerLawmoReportSession mLawmo is null");
        }
        return ret;
    }

    /**
     * Query fumo session actions
     */
    public int queryFumoSessionActions() {
        return (sFumo == null) ? FumoAction.NONE : sFumo.querySessionActions();
    }

    /**
     * Query lawmo session actions
     */
    public int queryLawmoSessionActions() {
        return (sLawmo == null) ? LawmoAction.NONE : sLawmo.querySessionActions();
    }

    public int queryScomoSessionActions() {
        return (sScomo == null) ? ScomoAction.NONE : sScomo.querySessionActions();
    }

    public static DmAction getDmAction() {
        DmAction action = new DmAction();
        action.mFumoAction = (sFumo == null) ? FumoAction.NONE : sFumo.querySessionActions();
        action.mLawmoAction = (sLawmo == null) ? LawmoAction.NONE : sLawmo.querySessionActions();
        action.mScomoAction = (sScomo == null) ? ScomoAction.NONE : sScomo.querySessionActions();

        Log.w(TAG.CONTROLLER,
                new StringBuilder("getDmAction left, fumo action is ").append(action.mFumoAction)
                        .append(",lawmo action is ").append(action.mLawmoAction)
                        .append(",scomo action is ").append(action.mScomoAction).toString());
        return action;
    }

    private boolean syncDmServerAddr() {
        Assert.assertFalse("syncDmServerAddr MUST NOT called in direct internet",
                Options.USE_DIRECT_INTERNET);

        Log.i(TAG.CONTROLLER, "Start to sync dm server address with dm tree");
        String nodeUri = null;
        DmDatabase dmDatabase = new DmDatabase(mContext);
        String serverAddrInDb = dmDatabase.getDmAddressFromSettings();
        Log.i(TAG.CONTROLLER, "Get dm server address in database is " + serverAddrInDb);
        if (TextUtils.isEmpty(serverAddrInDb)) {
            Log.e(TAG.CONTROLLER, "Get dm server address from database error!");
            return false;
        }
        try {
            String opName = DmCommonFun.getOperatorName();
            if (opName == null) {
                Log.e(TAG.CONTROLLER, "Get operator name from config file is null");
                return false;
            }
            Log.i(TAG.CONTROLLER, "operator name is " + opName);
            if (opName.equals("cmcc")) {
                nodeUri = "./DMAcc/OMSAcc/AppAddr/SrvAddr/Addr";
            } else if (opName.equals("cu")) {
                nodeUri = "./DMAcc/CUDMAcc/AppAddr/CUDMAcc/Addr";
            } else {
                Log.e(TAG.CONTROLLER, "There is not the right operator");
                return false;
            }

            Log.i(TAG.CONTROLLER, "The urinode is " + nodeUri);

            String serverAddrInTree = mDmTree.getStringValue(nodeUri);
            if (!(serverAddrInDb.equals(serverAddrInTree))) {
                Log.i(TAG.CONTROLLER, "Start to write serverAddrInTree = " + serverAddrInTree);
                mDmTree.replaceStringValue(nodeUri, serverAddrInDb);
            }
            Log.i(TAG.CONTROLLER,
                    "After write serverAddr in Dm Tree  = " + mDmTree.getStringValue(nodeUri));
        } catch (VdmException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void notifyUserPinSet(String pinCode, boolean accepted) {
        if (mEngine != null) {
            try {
                mEngine.notifyUserPinSet(pinCode, accepted);
            } catch (VdmException e) {
                e.printStackTrace();
                Log.e(TAG.CONTROLLER, e.getMessage());
            }
        }
    }
}

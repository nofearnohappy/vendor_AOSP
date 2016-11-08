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

package com.mediatek.mediatekdm;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.conn.DmDatabase;
import com.mediatek.mediatekdm.iohandler.DmDevIdNodeIoHandler;
import com.mediatek.mediatekdm.iohandler.DmDevSwVNodeIoHandler;
import com.mediatek.mediatekdm.iohandler.DmManNodeIoHandler;
import com.mediatek.mediatekdm.iohandler.DmModNodeIoHandler;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.MmiFactory;
import com.mediatek.mediatekdm.mdm.NIAMsgHandler;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;
import com.mediatek.mediatekdm.pl.DmPLFactory;

/**
 * This class is a thin wrapper of MdmEngine and MOs creation. It configures engine, register
 * observer and IO handlers.
 */
public class DmController {

    /**
     * @param service
     *        Android context
     */
    public DmController(DmService service, SessionStateObserver sessionObserver,
            NIAMsgHandler niaHandler, SessionInitiator niaInitiator, MmiFactory mmiFactory) {
        Log.i(TAG.CONTROLLER, "+DmController()");
        mService = service;
        createEngine(mmiFactory);
        mNiaHandler = niaHandler;
        mNiaSessionInitiator = niaInitiator;
        mSessionStateObserver = sessionObserver;
        mDmConfig = DmConfig.getInstance();
        mDmConfig.configure();
        mEngine.setConnectionTimeout(60);
        startEngine();
        mDmTree = new MdmTree();

        if (mDmConfig.useMobileDataOnly()) {
            syncDmServerAddr();
        }

        registerBasicNodeIOHandlers();

        mEngine.registerSessionStateObserver(mSessionStateObserver);
        Log.i(TAG.CONTROLLER, "-DmController()");
    }

    public void stop() {
        if (mEngine != null) {
            mEngine.stop();
        }
    }

    public void destroy() {
        if (mEngine != null) {
            Log.i(TAG.CONTROLLER, "Unregister session state observer.");
            mEngine.unregisterSessionStateObserver(mSessionStateObserver);
            Log.i(TAG.CONTROLLER, "Destroy mdm engine.");
            try {
                mEngine.destroy();
            } catch (MdmException e) {
                throw new Error(e);
            }
            mEngine = null;
        }
    }

    /**
     * The DM engine is idle or not.
     */
    public boolean isIdle() {
        if (mEngine != null) {
            return mEngine.isIdle();
        } else {
            return false;
        }
    }

    /**
     * Cancel current session.
     */
    public void cancelSession() {
        try {
            mEngine.cancelSession();
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in cancelSession()", e);
        }
    }

    /**
     * Confirm to start download.
     */
    public void proceedDLSession() {
        try {
            mEngine.notifyDLSessionProceed();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    /**
     * Trigger NIA DM session
     *
     * @param byte[] message - message body of wap push
     */
    public void triggerNiaDmSession(byte[] message) {
        if (mEngine == null) {
            Log.w(TAG.CONTROLLER, "triggerNiaDmSession mEngine is null");
            return;
        }
        try {
            mEngine.triggerNIADmSession(message, mNiaSessionInitiator, mNiaHandler);
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in triggerNiaDmSession()", e);
        }
    }

    public void triggerReportSession(String correlator, String format, String type, String mark,
            String source, String target, String data, String account, SessionInitiator initiator) {
        if (mEngine == null) {
            Log.w(TAG.CONTROLLER, "triggerReportSession mEngine is null");
            return;
        }
        try {
            mEngine.triggerReportSession(correlator, format, type, mark, source, target, data,
                    account, initiator);
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in triggerReportSession()", e);
        }
    }

    /**
     * Proceed NIA dm session
     */
    public void proceedNiaSession() {
        try {
            mEngine.notifyNIASessionProceed();
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in proceedNiaSession()", e);
        }
    }

    /**
     * Create mdm engine
     *
     * @param Context
     *        context - context that use mdm engine.
     */
    private void createEngine(MmiFactory mmiFactory) {
        Log.i(TAG.CONTROLLER, "Create mdm engine.");
        try {
            mEngine = new MdmEngine(mService, mmiFactory, new DmPLFactory(mService));
            Log.i(TAG.CONTROLLER, "Mdm engine created.");
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    /**
     * Start MDM engine
     */
    private void startEngine() {
        try {
            Log.i(TAG.CONTROLLER, "Start mdm engine.");
            mEngine.start();
            Log.i(TAG.CONTROLLER, "Mdm engine started.");
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in startEngine()", e);
            throw new Error("Fatal error! Failed to start engine.");
        }
    }

    private void registerBasicNodeIOHandlers() {
        try {
            mDmTree.registerNodeIoHandler(DmDevSwVNodeIoHandler.URI, new DmDevSwVNodeIoHandler());
            Log.i(TAG.CONTROLLER, "Node : " + DmDevSwVNodeIoHandler.URI + "IO handler registered");
            mDmTree.registerNodeIoHandler(DmDevIdNodeIoHandler.URI, new DmDevIdNodeIoHandler());
            Log.i(TAG.CONTROLLER, "Node : " + DmDevIdNodeIoHandler.URI + "IO handler registered");
            mDmTree.registerNodeIoHandler(DmManNodeIoHandler.URI, new DmManNodeIoHandler());
            Log.i(TAG.CONTROLLER, "Node : " + DmManNodeIoHandler.URI + "IO handler registered");
            mDmTree.registerNodeIoHandler(DmModNodeIoHandler.URI, new DmModNodeIoHandler());
            Log.i(TAG.CONTROLLER, "Node : " + DmModNodeIoHandler.URI + "IO handler registered");
        } catch (MdmException e) {
            Log.e(TAG.CONTROLLER, "MdmException in registerSwVNodeIOHandler()", e);
        }
    }

    /**
     * Update DM server address in DM tree with values from DmDatabase.
     */
    private boolean syncDmServerAddr() {
        Log.d(TAG.CONTROLLER, "+syncDmServerAddr()");
        String nodeUri = null;
        DmDatabase dmDatabase = new DmDatabase(mService);
        String serverAddrInDb = dmDatabase.getDmAddressFromSettings();
        Log.i(TAG.CONTROLLER, "Get dm server address in database is " + serverAddrInDb);
        if (serverAddrInDb == null || serverAddrInDb.equals("")) {
            Log.e(TAG.CONTROLLER, "Get dm server address from database error!");
            return false;
        }
        try {
            String opName = mDmConfig.getCustomizedOperator();
            if (opName == null) {
                Log.e(TAG.CONTROLLER, "Get operator name from config file returns null");
                Log.d(TAG.CONTROLLER, "-syncDmServerAddr()");
                return false;
            }
            Log.i(TAG.CONTROLLER, "operator name is " + opName);
            if (opName.equals("cmcc")) {
                nodeUri = "./DMAcc/OMSAcc/AppAddr/SrvAddr/Addr";
            } else if (opName.equals("cu")) {
                nodeUri = "./DMAcc/CUDMAcc/AppAddr/CUDMAcc/Addr";
            } else {
                Log.e(TAG.CONTROLLER, "This is not the right operator");
                Log.d(TAG.CONTROLLER, "-syncDmServerAddr()");
                return false;
            }

            Log.i(TAG.CONTROLLER, "The urinode is " + nodeUri);

            String serverAddrInTree = mDmTree.getStringValue(nodeUri);
            if (serverAddrInDb != null && !(serverAddrInDb.equals(serverAddrInTree))) {
                Log.i(TAG.CONTROLLER, "Start to write serverAddrInTree = " + serverAddrInTree);
                mDmTree.replaceStringValue(nodeUri, serverAddrInDb);
            }
            String serverAddrInTree1 = mDmTree.getStringValue(nodeUri);
            Log.i(TAG.CONTROLLER, "After write serverAddr in Dm Tree  = " + serverAddrInTree1);

        } catch (MdmException e) {
            e.printStackTrace();
        }
        Log.d(TAG.CONTROLLER, "-syncDmServerAddr()");
        return true;
    }

    private MdmEngine mEngine;
    private MdmTree mDmTree;
    private SessionInitiator mNiaSessionInitiator;
    private NIAMsgHandler mNiaHandler;
    private SessionStateObserver mSessionStateObserver;
    private DmConfig mDmConfig;
    private DmService mService;
}

/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.IntentAction;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse.InteractionType;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager.IOperationStateObserver;
import com.mediatek.mediatekdm.DmOperationManager.State;
import com.mediatek.mediatekdm.DmOperationManager.TriggerResult;
import com.mediatek.mediatekdm.IDmComponent.DispatchResult;
import com.mediatek.mediatekdm.IDmComponent.OperationAction;
import com.mediatek.mediatekdm.conn.DmDataConnection;
import com.mediatek.mediatekdm.conn.DmDataConnection.DataConnectionListener;
import com.mediatek.mediatekdm.iohandler.IoCacheManager;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.MmiChoiceList;
import com.mediatek.mediatekdm.mdm.MmiConfirmation;
import com.mediatek.mediatekdm.mdm.MmiFactory;
import com.mediatek.mediatekdm.mdm.MmiInfoMsg;
import com.mediatek.mediatekdm.mdm.MmiInputQuery;
import com.mediatek.mediatekdm.mdm.MmiObserver;
import com.mediatek.mediatekdm.mdm.MmiProgress;
import com.mediatek.mediatekdm.mdm.MmiViewContext;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;
import com.mediatek.mediatekdm.util.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class DmService extends Service {
    public static class DmBinder extends Binder {
        private final DmService mService;

        public DmBinder(DmService service) {
            mService = service;
        }

        public void cancelNiaAlertTimeout() {
            mService.cancelNiaAlertTimeout();
        }

        public void clearDmNotification() {
            mService.clearDmNotification();
        }

        public MmiViewContext getAlertConfirmContext() {
            return mService.mAlertConfirmContext;
        }

        public DmService getService() {
            return mService;
        }

        public void showAlertConfirm(MmiViewContext context) {
            mService.showAlertConfirm(context);
        }
    }

    public interface IServiceMessage {
        // / When recover, check whether network ready
        int MSG_CHECK_NETWORK = 100;
        // / Process next operation.
        int MSG_OPERATION_PROCESS_NEXT = 500;
        // / Recover current operation. This message should be parameterized with the operation to
        // recover for sanity
        // check.
        int MSG_OPERATION_RECOVER_CURRENT = 501;
        // / Retry current operation. This message should be parameterized with the operation to
        // recover for sanity
        // check.
        int MSG_OPERATION_RETRY_CURRENT = 502;
        // / Operation has timed out. This message should be parameterized with the operation timed
        // out for sanity
        // check.
        int MSG_OPERATION_TIME_OUT = 503;
        // / Quit service if we are idle.
        int MSG_QUIT_SERVICE = 600;
        int MSG_WAP_CONNECTION_SUCCESS = 103;
        int MSG_WAP_CONNECTION_TIMEOUT = 104;
    }

    private class DmServiceHandler extends Handler implements IOperationStateObserver {
        private static final int EXIT_DELAY = 120000; // 2min
        // This heart beat is an experience safe value for reserving a invocation time window for
        // triggerNow().
        private static final int HEART_BEAT = 5000; // 5s
        private static final int NETWORK_CHECK_INTERVAL = 29000; // 29s

        public DmServiceHandler() {
            // super(getMainLooper());
            super();
            Log.e(TAG.SERVICE, "DmServiceHandler in thread: " + getLooper().getThread().getId());
        }

        @Override
        public void handleMessage(Message msg) {
            // Dump queue.
            getLooper().getQueue().dumpMessageQueue();
            Log.d(TAG.SERVICE, "DmServiceHandler.handleMessage(" + msg.what + ")");
            DmOperation operation = null;
            TriggerResult result = null;

            if (msg.what != IServiceMessage.MSG_QUIT_SERVICE) {
                removeMessages(IServiceMessage.MSG_QUIT_SERVICE);
            }

            switch (msg.what) {
                case IServiceMessage.MSG_OPERATION_PROCESS_NEXT:
                    Log.d(TAG.SERVICE, "Process next operation in queue.");
                    result = mOperationManager.triggerNext();
                    if (result == TriggerResult.BUSY) {
                        Log.w(TAG.SERVICE,
                                "The operation is busy. Retry later after the current is finished.");
                        // Clear redundant MSG_OPERATION_PROCESS_NEXT in queue. When the current
                        // operation is finished,
                        // we will try to process the next later.
                        removeMessages(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                        break;
                    } else if (result == TriggerResult.SKIPPED) {
                        // Skip this one to process the next.
                        sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                        break;
                    } else if (result == TriggerResult.NO_PENDING_OPERATION) {
                        Log.d(TAG.SERVICE, "Nothing to process.");
                        // Schedule exit action.
                        // TODO check HP here?
                        sendMessageDelayed(obtainMessage(IServiceMessage.MSG_QUIT_SERVICE),
                                EXIT_DELAY);
                        break;
                    }
                    // Clear caches
                    Log.d(TAG.SERVICE, "Clear IO cache");
                    IoCacheManager.getInstance().purge();

                    operation = mOperationManager.current();
                    if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_SI)) {
                        processSIOperation(operation);
                    } else {
                        boolean accepted = false;
                        for (IDmComponent component : mComponents) {
                            if (component.dispatchOperationAction(OperationAction.NEW, operation) == DispatchResult.ACCEPT) {
                                accepted = true;
                                break;
                            }
                        }
                        if (!accepted) {
                            Log.e(TAG.SERVICE, "No suitable handler to process operation "
                                    + operation);
                        }
                    }
                    break;

                case IServiceMessage.MSG_OPERATION_RECOVER_CURRENT:
                    operation = mOperationManager.current();
                    Log.d(TAG.SERVICE, "Recover current operation " + operation);
                    if (mOperationManager.isInRecovery()) {
                        if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                            Log.w(TAG.SERVICE, "Sanity check passed. Recover.");
                            // Clear the pending timeout messages for the current operation.
                            removeMessages(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
                            removeMessages(IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, operation);
                            removeMessages(IServiceMessage.MSG_OPERATION_RETRY_CURRENT, operation);
                            removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
                            mOperationManager.recoverCurrent();
                        } else {
                            throw new Error("Invalid operation state");
                        }
                    } else {
                        if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                            Log.w(TAG.SERVICE, "Ignore duplicate recover message.");
                            break;
                        } else {
                            throw new Error("Invalid operation state");
                        }
                    }

                    if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_SI)) {
                        processSIOperation(operation);
                    } else {
                        for (IDmComponent component : mComponents) {
                            if (component.dispatchOperationAction(OperationAction.RECOVER,
                                    operation) == DispatchResult.ACCEPT) {
                                break;
                            }
                        }
                        Log.e(TAG.SERVICE, "No suitable handler to recover operation " + operation);
                    }
                    break;

                case IServiceMessage.MSG_OPERATION_RETRY_CURRENT:
                    operation = mOperationManager.current();
                    Log.d(TAG.SERVICE, "Retry current operation " + operation);
                    if (mOperationManager.isInRecovery()) {
                        if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                            Log.w(TAG.SERVICE, "Sanity check passed. Retry.");
                            // Clear the pending timeout messages for the current operation.
                            removeMessages(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
                            removeMessages(IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, operation);
                            removeMessages(IServiceMessage.MSG_OPERATION_RETRY_CURRENT, operation);
                            removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
                            mOperationManager.retryCurrent();
                        } else {
                            throw new Error("Invalid operation state");
                        }
                    } else {
                        if (operation.getId() == ((DmOperation) msg.obj).getId()) {
                            Log.w(TAG.SERVICE, "Ignore duplicate retry message.");
                            break;
                        } else {
                            throw new Error("Invalid operation state");
                        }
                    }

                    if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_SI)) {
                        processSIOperation(operation);
                    } else {
                        for (IDmComponent component : mComponents) {
                            if (component.dispatchOperationAction(OperationAction.RETRY, operation) == DispatchResult.ACCEPT) {
                                break;
                            }
                        }
                        Log.e(TAG.SERVICE, "No suitable handler to retry operation " + operation);
                    }
                    break;

                case IServiceMessage.MSG_OPERATION_TIME_OUT:
                    operation = mOperationManager.current();
                    if (mOperationManager.isInRecovery() && operation == (DmOperation) msg.obj) {
                        if (operation.getRetry() > 0) {
                            sendMessage(obtainMessage(IServiceMessage.MSG_OPERATION_RETRY_CURRENT,
                                    msg.obj));
                        }
                    } else {
                        Log.e(TAG.SERVICE, "Invalid time out message with operation " + msg.obj);
                        Log.e(TAG.SERVICE, "Ignore it.");
                    }
                    break;
                case IServiceMessage.MSG_WAP_CONNECTION_SUCCESS:
                    Log.w(TAG.SERVICE, "Receive message is MSG_WAP_CONNECTION_SUCCESS");
                    removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
                    if (mOperationManager.isBusy()) {
                        if (mOperationManager.isInRecovery()) {
                            sendMessage(obtainMessage(
                                    IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, 0, 0,
                                    mOperationManager.current()));
                        } else {
                            Log.i(TAG.SERVICE,
                                    "There is already an operation running, ignore this event.");
                        }
                    } else {
                        sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                    }
                    break;
                case IServiceMessage.MSG_QUIT_SERVICE:
                    // TODO Quit service properly
                    if (!mOperationManager.isBusy() && !mOperationManager.hasNext()) {
                        stopSelf();
                    }
                    Log.w(TAG.SERVICE, "NOT IMPLEMENTED: Exit message if there is nothing to do.");
                    break;
                case IServiceMessage.MSG_CHECK_NETWORK:
                    Log.i(TAG.SERVICE, "Receive MSG_CHECK_NETWORK after "
                            + (NETWORK_CHECK_INTERVAL / 1000) + "s.");
                    Intent serviceIntent = new Intent();
                    serviceIntent.setClass(DmService.this, DmService.class);
                    serviceIntent.setAction(DmConst.IntentAction.CHECK_NETWORK);
                    startService(serviceIntent);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        @Override
        public void notify(State state, State previousState, Object extra) {
            if (state == State.IDLE) {
                IoCacheManager.getInstance().flush();
                if (mOperationManager.hasNext()) {
                    // WARNING: MSG_OPERATION_PROCESS_NEXT message MUST be sent after a certain
                    // delay, otherwise it will
                    // close the window for triggerNow() invocations!
                    sendEmptyMessageDelayed(IServiceMessage.MSG_OPERATION_PROCESS_NEXT, HEART_BEAT);
                }
            } else if (state == State.RECOVERING) {
                Log.i(TAG.SERVICE, "In recover, Send MSG_CHECK_NETWORK after "
                        + (NETWORK_CHECK_INTERVAL / 1000) + "s.");
                // WARNING: if in recover state, check network after a certain delay
                sendEmptyMessageDelayed(IServiceMessage.MSG_CHECK_NETWORK, NETWORK_CHECK_INTERVAL);
            }
        }

        private void processSIOperation(DmOperation operation) throws Error {
            byte[] message = operation.getByteArrayProperty(KEY.NIA);
            if (DmApplication.getInstance().forceSilentMode()) {
                Log.w(TAG.MMI, "NIA UI is skipped in silent mode");
                mDmController.triggerNiaDmSession(message);
            } else {
                int uiMode = NiaSessionManager.extractUIModeFromNIA(message);
                Log.i(TAG.SERVICE, "UI mode is " + uiMode);
                if (uiMode >= 0) {
                    InteractionResponse ir = null;
                    int lastResponse = InteractionResponse.INVALID;
                    switch (uiMode) {
                        case 0:
                        case 1:
                            Log.d(TAG.SERVICE, "Trigger NIA in Handler");
                            mDmController.triggerNiaDmSession(message);
                            break;
                        case 2:
                            ir = operation.getNextUIResponse();
                            if (ir != null) {
                                if (ir.type != InteractionResponse.InteractionType.NOTIFICATION) {
                                    throw new Error("Invalid interaction response type!");
                                } else {
                                    // Set a fake flag to skip notification of NIA.
                                    lastResponse = InteractionResponse.POSITIVE;
                                }
                            }
                            if (lastResponse == InteractionResponse.INVALID) {
                                mOperationManager.current().addUIResponse(
                                        new InteractionResponse(InteractionType.NOTIFICATION,
                                                InteractionResponse.POSITIVE, null));
                                mDmNotification
                                        .showNotification(NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE);
                                setNiaAlertTimeout(
                                        NiaSessionManager.DEFAULT_NOTIFICATION_VISIBLE_TIMEOUT,
                                        NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE);
                            }
                            mDmController.triggerNiaDmSession(message);
                            break;
                        case 3:
                            ir = operation.getNextUIResponse();
                            if (ir != null) {
                                if (ir.type != InteractionResponse.InteractionType.NOTIFICATION) {
                                    throw new Error("Invalid interaction response type!");
                                } else {
                                    lastResponse = ir.response;
                                }
                            }
                            if (lastResponse == InteractionResponse.INVALID) {
                                mDmNotification
                                        .showNotification(NotificationInteractionType.TYPE_NOTIFICATION_INTERACT);
                                setNiaAlertTimeout(
                                        NiaSessionManager.DEFAULT_NOTIFICATION_INTERACT_TIMEOUT,
                                        NotificationInteractionType.TYPE_NOTIFICATION_INTERACT);
                            } else if (lastResponse == InteractionResponse.POSITIVE) {
                                mDmController.triggerNiaDmSession(message);
                            } else {
                                // lastResponse CANNOT be InteractionResponse.NEGATIVE,
                                // otherwise there should be nothing to recover.
                                throw new Error("Invalid last response for notification "
                                        + lastResponse);
                            }
                            break;
                        default:
                            // Ignore it
                            Log.e(TAG.SERVICE, "Invalid notification UI mode " + uiMode);
                            break;
                    }
                }
            }
        }
    }

    private static class DmSessionStateObserver implements SessionStateObserver {
        private Semaphore mSemaphore;
        private DmService mService;

        DmSessionStateObserver(DmService service) {
            mService = service;
            mSemaphore = new Semaphore(1);
            mSemaphore.drainPermits();
        }

        // Interface method of SessionStateObserver.
        // Called by engine when state of session changes.
        public void notify(SessionType type, SessionState state, int lastError,
                SessionInitiator initiator) {
            if (mService == null) {
                Log.w(TAG.SESSION, "receive notify with mService is null");
            }
            Log.i(TAG.SESSION, "---- session state notify ----");
            Log.i(TAG.SESSION, "[type] = " + type);
            Log.i(TAG.SESSION, "[state] = " + state);
            Log.i(TAG.SESSION, "[last error] = " + MdmError.fromInt(lastError) + "(" + lastError
                    + ")");
            Log.i(TAG.SESSION, "[initiator]= " + initiator.getId());
            Log.i(TAG.SESSION, "---- session state dumped ----");

            DmOperationManager operationManager = DmOperationManager.getInstance();
            DmOperation operation = operationManager.current();
            Log.d(TAG.SERVICE, "Current operation is " + operation);
            SessionHandler sessionHandler = null;

            for (IDmComponent component : DmApplication.getInstance().getComponents()) {
                sessionHandler = component.dispatchSessionStateChange(type, state, lastError,
                        initiator, operation);
                if (sessionHandler != null) {
                    break;
                }
            }

            if (sessionHandler == null) {
                if (initiator.getId().startsWith(NiaSessionManager.INITIATOR)) {
                    Log.d(TAG.SESSION, "Normal session");
                    sessionHandler = mService.mNiaManager;
                } else {
                    Log.e(TAG.SESSION, "unknown initiator: " + initiator.getId());
                    return;
                }
            }

            // Throw it to main thread to run.
            class SSORunnable implements Runnable {
                private int mLastError;
                private SessionHandler mSession;
                private SessionState mState;
                private SessionType mType;

                public SSORunnable(SessionHandler session, SessionType type, SessionState state,
                        int lastError) {
                    mSession = session;
                    mType = type;
                    mState = state;
                    mLastError = lastError;
                }

                @Override
                public void run() {
                    Log.d(TAG.SESSION, "SSORunnable run!");
                    mSession.onSessionStateChange(mType, mState, mLastError);
                    Log.i(TAG.SESSION, "SSO mSemaphore.release");
                    mSemaphore.release();
                }
            }

            Runnable r = new SSORunnable(sessionHandler, type, state, lastError);
            mService.mServiceHandler.post(r);
            try {
                Log.i(TAG.SESSION, "SSO mSemaphore.acquire");
                mSemaphore.acquire();
                Log.i(TAG.SESSION, "SSO mSemaphore.acquire OK");
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

    }

    public static final String BIND_SERVICE = "com.mediatek.mediatekdm.BIND_SERVICE";
    // All time out are defined by CMCC specification. in DM session, MAXDT=30
    private static final int DEFAULT_ALERT_1101_TIMEOUT = 30;
    private AlarmManager mAlarmManager = null;
    private DmAlertConfirm mAlertConfirm;
    private MmiViewContext mAlertConfirmContext;
    private DmBinder mBinder = null;
    private DmDataConnection mDataConnection = null;
    private DmController mDmController = null;
    private boolean mFatalErrorOccurred = false;
    private int mHP = 0;
    private PendingIntent mNiaAlertTimeoutIntent = null;
    private NiaSessionManager mNiaManager = new NiaSessionManager(this);
    private DmOperationManager mOperationManager = DmOperationManager.getInstance();
    private Handler mServiceHandler = null;

    ArrayList<IDmComponent> mComponents = null;
    DmNotification mDmNotification = null;

    public void cancelNiaAlertTimeout() {
        Log.i(TAG.SERVICE, "+cancelNiaAlertTimeout()");
        if (mAlarmManager != null && mNiaAlertTimeoutIntent != null) {
            mAlarmManager.cancel(mNiaAlertTimeoutIntent);
            mNiaAlertTimeoutIntent = null;
        }
        Log.i(TAG.SERVICE, "-cancelNiaAlertTimeout()");
    }

    public void clearDmNotification() {
        if (mDmNotification != null) {
            mDmNotification.clear();
        }
    }

    public DmController getController() {
        return mDmController;
    }

    public Handler getHandler() {
        return mServiceHandler;
    }

    /**
     * Override function of android.app.Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG.SERVICE, "+onBind(): Fatal error flag is " + mFatalErrorOccurred);
        if (mFatalErrorOccurred) {
            return null;
        } else {
            final String action = intent.getAction();
            if (action == null) {
                return null;
            } else if (action.equals(BIND_SERVICE)) {
                return mBinder;
            } else {
                for (IDmComponent component : mComponents) {
                    IBinder binder = component.getBinder(intent);
                    if (binder != null) {
                        return binder;
                    }
                }
                return null;
            }
        }
    }

    /**
     * Override function of android.app.Service, initiate MDM controls.
     */
    public void onCreate() {
        Log.i(TAG.SERVICE, "On create service");
        super.onCreate();

        PlatformManager.getInstance().stayForeground(this);

        mFatalErrorOccurred = false;
        long availableStorage = Utilities.getAvailableInternalMemorySize();
        if (availableStorage < 0) {
            Log.e(TAG.SERVICE, "Storage is not enough.");
            mFatalErrorOccurred = true;
            Utilities.removeDirectoryRecursively(getFilesDir());
            Process.killProcessQuiet(Process.myPid());
            return;
        }
        Log.e(TAG.SERVICE, "Create Service Handler in thread " + Thread.currentThread().getId());
        mServiceHandler = new DmServiceHandler();
        Log.e(TAG.SERVICE, "Service Handler's looper belongs to thread "
                + mServiceHandler.getLooper().getThread().getId());

        try {
            prepareDmTreeFile();
            CollectSetPermissionControl.getInstance().isPermFileReady();
            initDmController();
            mComponents = DmApplication.getInstance().getComponents();
            attachComponents();
            MdmTree tree = new MdmTree();
            for (IDmComponent component : mComponents) {
                component.configureDmTree(tree);
            }

            mDmNotification = new DmNotification(this);

            if (DmConfig.getInstance().useMobileDataOnly()) {
                mDataConnection = DmDataConnection.getInstance(this);
            }
            mBinder = new DmBinder(this);
        } catch (Error e) {
            e.printStackTrace();
            mFatalErrorOccurred = true;
            // Utilities.removeDirectoryRecursively(getFilesDir());
        }
        DmOperationManager.getInstance()
                .registerObserver((IOperationStateObserver) mServiceHandler);
    }

    /**
     * Override function of android.app.Service
     */
    public void onDestroy() {
        Log.d(TAG.SERVICE, "+onDestroy()");

        PlatformManager.getInstance().leaveForeground(this);

        DmOperationManager.getInstance().unregisterObserver(
                (IOperationStateObserver) mServiceHandler);
        try {
            if (mDmController != null) {
                mDmController.stop();
            }

            if (DmConfig.getInstance().useMobileDataOnly()) {
                if (mDataConnection != null) {
                    mDataConnection.stopDmDataConnectivity();
                    mDataConnection.destroyInstance();
                }
            }

            detachComponents();
            if (mDmController != null) {
                mDmController.destroy();
                mDmController = null;
            }
            mAlarmManager = null;
            if (mDmNotification != null) {
                mDmNotification.clear();
                mDmNotification = null;
            }
            mBinder = null;
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(TAG.SERVICE, "Ignore all exceptions in onDestroy()");
        }

        mServiceHandler = null;

        super.onDestroy();
        Log.d(TAG.SERVICE, "-onDestroy()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG.SERVICE, "+onStartCommand()");

        if (mFatalErrorOccurred) {
            Log.e(TAG.SERVICE, "Fatal error happened. Exit.");
            stopSelf();
            return START_NOT_STICKY;
        }

        addHP();

        if (intent == null || intent.getAction() == null) {
            Log.w(TAG.SERVICE, "Intent or action is null");
            cutHP();
            Log.d(TAG.SERVICE, "-onStartCommand()");
            return START_NOT_STICKY;
        }

        if (!DmApplication.getInstance().checkPrerequisites()) {
            cutHP();
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();
        Log.d(TAG.SERVICE, "Received start id " + startId + " : " + intent + " action is " + action);

        /*
         * Incoming intents can be organized in the following category: 1. Initial Trigger: SI or CI
         * actions issued either by DM server, user (via Settings) or device (polling & reboot
         * check). This kind of intents will activate the network. 2. Interactions with UI
         * components: For example, alert or notification responses. This kind of intents will not
         * activate the network. 3. Miscellaneous:
         */
        if (action.equals(IntentAction.DM_WAP_PUSH)) {
            for (IDmComponent component : mComponents) {
                DispatchResult result = component.validateWapPushMessage(intent);
                if (result == DispatchResult.ABORT) {
                    cutHP();
                    return START_NOT_STICKY;
                }
            }
            String type = intent.getType();
            Log.v(TAG.SERVICE, "WAP push message with type '" + type + "' received");
            if (type != null && type.equals(DmConst.IntentType.DM_NIA)) {
                // For cmcc wap push turn on DataConnectivity ,extra key value set by CMCCComponent
                if (intent.getBooleanExtra(DmConst.DmNiaServer.IS_CMCC_DMACC_SERVER, false)) {
                    int result = DmDataConnection.getInstance(this).enableMobileDataForWapPush();
                    Log.i(TAG.SERVICE, "Wap push message enable DM WAP conn...ret=" + result);
                }
                DmOperation operation = new DmOperation();
                operation.initSI(intent.getByteArrayExtra("data"));
                operation.setProperty(DmOperation.KEY.INITIATOR, "Server");
                mOperationManager.enqueue(operation);
            }
        } else if (action.equals(IntentAction.DM_KICK_OFF)) {
            kickoff();
            return START_NOT_STICKY;
        } else if (action.equals(IntentAction.DM_NOTIFICATION_RESPONSE)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            clearDmNotification();
            if (intent.getBooleanExtra("response", false)) {
                Log.d(TAG.SERVICE, "User confirmed notification, proceed");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.NOTIFICATION,
                                InteractionResponse.POSITIVE, null));
                mDmController.triggerNiaDmSession(mOperationManager.current().getByteArrayProperty(
                        KEY.NIA));
            } else {
                Log.d(TAG.SERVICE, "User canceled notification, proceed to next operation");
                mOperationManager.finishCurrent();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_ALERT_RESPONSE)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (intent.getBooleanExtra("response", false)) {
                Log.d(TAG.SERVICE, "User confirmed alert");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION,
                                InteractionResponse.POSITIVE, null));
                mAlertConfirm.confirm();
            } else {
                Log.d(TAG.SERVICE, "User canceled alert");
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION,
                                InteractionResponse.NEGATIVE, null));
                mAlertConfirm.cancel();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable anymore, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_NOTIFICATION_TIMEOUT)) {
            int type = intent.getIntExtra("type", NotificationInteractionType.TYPE_INVALID);
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (type == NotificationInteractionType.TYPE_NOTIFICATION_INTERACT) {
                mOperationManager.finishCurrent();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(IntentAction.DM_ALERT_TIMEOUT)) {
            cancelNiaAlertTimeout();
            sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
            if (mDmNotification != null) {
                mDmNotification.clear();
            }
            if (DmFeatureSwitch.CMCC_SPECIFIC) {
                // CMCC DM 3.0 specification demands that DM client should reply negative if timeout
                // occurred.
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION,
                                InteractionResponse.NEGATIVE, null));
                mAlertConfirm.cancel();
            } else {
                // OMA DM specification demands that DM client should use DR in server's request as
                // the default
                // response.
                mOperationManager.current().addUIResponse(
                        new InteractionResponse(InteractionType.CONFIRMATION,
                                InteractionResponse.TIMEOUT, null));
                mAlertConfirm.timeout();
            }
            // Do NOT require network directly as it should be activated by DM_WAP_PUSH. If the
            // network is unavailable any more, we will facilitate the error recovery mechanism.
            return START_STICKY;
        } else if (action.equals(DmConst.IntentAction.DM_PENDING_OPERATION_SCAN_RESULT)) {
            Log.d(TAG.SERVICE, "Pending operation found...");
            if (intent.getBooleanExtra("found", false)) {
                Log.d(TAG.SERVICE, "Pending operation found. Connect to network.");
            } else {
                if (!mOperationManager.isBusy() && !mOperationManager.hasNext()) {
                    Log.d(TAG.SERVICE,
                            "No pending operation found and service is idle, schedule exit message.");
                    cutHP();
                }
                return START_NOT_STICKY;
            }
        } else if (action.equals(DmConst.IntentAction.CHECK_NETWORK)) {
            Log.d(TAG.SERVICE, "Re check network...");
            mServiceHandler.removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
        } else if (action.equals(DmConst.IntentAction.DM_SERVICE_KEEP_ALIVE)) {
            Log.d(TAG.SERVICE, "Just keep alive...");
            return START_NOT_STICKY;
        } else {
            // Dispatch start command to components.
            boolean requiresTrigger = false;
            boolean accepted = false;
            for (IDmComponent component : mComponents) {
                DispatchResult result = component.dispatchCommand(intent);
                if (result == DispatchResult.ACCEPT) {
                    accepted = true;
                    break;
                } else if (result == DispatchResult.ACCEPT_AND_TRIGGER) {
                    requiresTrigger = true;
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                Log.w(TAG.SERVICE, "Unsupported intent " + intent + ", ignore.");
                cutHP();
                return START_NOT_STICKY;
            } else if (!requiresTrigger) {
                return START_STICKY;
            }
        }

        // Activate network
        if (DmConfig.getInstance().useMobileDataOnly()) {
            try {
                mDataConnection.registerListener(new DataConnectionListener() {
                    public void notifyStatus(int status) {
                        mServiceHandler.sendEmptyMessage(status);
                    }
                });
                int result = mDataConnection.startDmDataConnectivity();
                Log.i(TAG.SERVICE, "starting DM WAP conn...ret=" + result);
                if (result == PlatformManager.APN_ALREADY_ACTIVE) {
                    Log.i(TAG.SERVICE, "WAP is ready.");
                    mServiceHandler.sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                } else {
                    Log.i(TAG.SERVICE, "Net not ready, send MSG_CHECK_NETWORK again.");
                    mServiceHandler.sendEmptyMessageDelayed(IServiceMessage.MSG_CHECK_NETWORK,
                            DmServiceHandler.NETWORK_CHECK_INTERVAL);
                }
                // Network is not available now, we will recovery/trigger the operation when network
                // is ready.
            } catch (IOException e) {
                Log.e(TAG.SERVICE, "startDmDataConnectivity failed.", e);
            }
        } else {
            Log.i(TAG.SERVICE, "Process next operation directly when using internet.");
            mServiceHandler.removeMessages(IServiceMessage.MSG_CHECK_NETWORK);
            if (mOperationManager.isBusy()) {
                if (mOperationManager.isInRecovery()) {
                    mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
                            IServiceMessage.MSG_OPERATION_RECOVER_CURRENT, 0, 0,
                            mOperationManager.current()));
                } else {
                    Log.i(TAG.SERVICE, "There is already an operation running, ignore this event.");
                }
            } else {
                mServiceHandler.sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            }
            Log.i(TAG.SERVICE, "Dump queue A which belongs to thread "
                    + mServiceHandler.getLooper().getThread().getId());
            mServiceHandler.getLooper().getQueue().dumpMessageQueue();
            Log.i(TAG.SERVICE, "Dump queue B which belongs to thread "
                    + getMainLooper().getThread().getId());
            getMainLooper().getQueue().dumpMessageQueue();
        }

        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mOperationManager.isBusy() || mOperationManager.hasNext()) {
            Intent serviceIntent = new Intent(DmConst.IntentAction.DM_SERVICE_KEEP_ALIVE);
            serviceIntent.setClass(this, DmService.class);
            startService(serviceIntent);
        }
        return false;
    }

    /*
     * This method is used to control the life cycle of DmService, but it is only applied to the
     * onStart situation. FIXME Not complete. Do NOT rely on it.
     */
    private synchronized void addHP() {
        mHP += 1;
        Log.d(TAG.SERVICE, "Add HP to " + mHP);
        if (mHP > 0) {
            if (mHP == 1) {
                Log.d(TAG.SERVICE, "Come to life.");
            }
            mServiceHandler.removeMessages(IServiceMessage.MSG_QUIT_SERVICE);
        }
    };

    private void attachComponents() {
        if (mComponents != null) {
            for (IDmComponent com : mComponents) {
                Log.i(TAG.SERVICE, "Init component: " + com.getName());
                com.attach(this);
            }
        }
    }

    /*
     * This method is used to control the life cycle of DmService, but it is only applied to the
     * onStart situation. FIXME Not complete. Do NOT rely on it.
     */
    private synchronized void cutHP() {
        mHP -= 1;
        Log.d(TAG.SERVICE, "Cut HP to " + mHP);
        if (mHP <= 0) {
            Log.d(TAG.SERVICE, "Schedule the death.");
            mServiceHandler.sendMessageDelayed(
                    mServiceHandler.obtainMessage(IServiceMessage.MSG_QUIT_SERVICE),
                    DmServiceHandler.EXIT_DELAY);
        }
    };

    private void detachComponents() {
        if (mComponents != null) {
            for (IDmComponent com : mComponents) {
                Log.i(TAG.SERVICE, "Deinit component: " + com.getName());
                com.detach(this);
            }
        }
    }

    private void initDmController() {
        mDmController = new DmController(this, new DmSessionStateObserver(this), mNiaManager,
                mNiaManager, new MmiFactory() {

                    public MmiChoiceList createChoiceListDlg(MmiObserver observer) {
                        return new DmMmiChoiceList(DmService.this, observer);
                    }

                    public MmiConfirmation createConfirmationDlg(MmiObserver observer) {
                        mAlertConfirm = new DmAlertConfirm(DmService.this, observer);
                        return mAlertConfirm;
                    }

                    public MmiInfoMsg createInfoMsgDlg(MmiObserver observer) {
                        return new DmMmiInfoMsg(DmService.this, observer);
                    }

                    public MmiInputQuery createInputQueryDlg(MmiObserver observer) {
                        return new DmMmiInputQuery();
                    }

                    public MmiProgress createProgress(int total) {
                        return new MmiProgress() {
                            public void update(int current, int total) {
                                final DmOperation operation = mOperationManager.current();
                                if (operation != null) {
                                    for (IDmComponent component : mComponents) {
                                        component.dispatchMmiProgressUpdate(operation, current,
                                                total);
                                    }
                                }
                            }
                        };
                    }
                });
    }

    private void kickoff() {
        // Notify the components it's ready to go.
        for (IDmComponent component : DmApplication.getInstance().getComponents()) {
            component.kickoff(this);
        }
        // Launch operation scanner for pending operations.
        KickoffActor.kickoff(new OperationScaner(this));
    }

    private void prepareDmTreeFile() {
        File systemTree = new File(PlatformManager.getInstance().getPathInSystem(
                DmConst.Path.DM_TREE_FILE));
        File dataTree = new File(PlatformManager.getInstance().getPathInData(this,
                DmConst.Path.DM_TREE_FILE));
        File dataFilesDir = getFilesDir();
        if (!dataTree.exists()) {
            if (!systemTree.exists()) {
                throw new Error("No DM tree file found in system folder.");
            }
            if (!dataFilesDir.exists()) {
                Log.d(TAG.SERVICE, "there is no /files dir in dm folder");
                if (dataFilesDir.mkdir()) {
                    // chmod for recovery access?
                    Utilities.openPermission(dataFilesDir.getAbsolutePath());
                } else {
                    throw new Error("Failed to create folder in data folder.");
                }
            }
            Utilities.copyFile(systemTree, dataTree);
        }
    }

    private void setNiaAlertTimeout(long seconds, int type) {
        Log.i(TAG.SERVICE, "+setNiaAlertTimeout(" + seconds + ", " + type + ")");
        Intent intent = new Intent(this, DmService.class);
        intent.putExtra("type", type);
        switch (type) {
            case NotificationInteractionType.TYPE_ALERT_1101:
            case NotificationInteractionType.TYPE_ALERT_1102:
            case NotificationInteractionType.TYPE_ALERT_1103:
            case NotificationInteractionType.TYPE_ALERT_1104:
                intent.setAction(DmConst.IntentAction.DM_ALERT_TIMEOUT);
                break;
            case NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE:
            case NotificationInteractionType.TYPE_NOTIFICATION_INTERACT:
                intent.setAction(DmConst.IntentAction.DM_NOTIFICATION_TIMEOUT);
                break;
            default:
                throw new Error("Invalid alert type " + type);
        }

        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        mNiaAlertTimeoutIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.cancel(mNiaAlertTimeoutIntent);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + seconds * 1000),
                mNiaAlertTimeoutIntent);
        Log.i(TAG.SERVICE, "-setNiaAlertTimeout()");
    }

    /**
     * Notify UI to show alert 1101.
     */
    void showAlertConfirm(MmiViewContext context) {
        Log.i(TAG.SERVICE, "showNiaNotification");
        mAlertConfirmContext = context;
        DmOperation operation = mOperationManager.current();
        InteractionResponse ir = operation.getNextUIResponse();
        if (ir != null) {
            if (ir.type == InteractionType.CONFIRMATION) {
                switch (ir.response) {
                    case InteractionResponse.POSITIVE:
                        mAlertConfirm.confirm();
                        break;
                    case InteractionResponse.NEGATIVE:
                        mAlertConfirm.cancel();
                        break;
                    case InteractionResponse.TIMEOUT:
                        mAlertConfirm.timeout();
                        break;
                    default:
                        throw new Error("Invalid interaction response " + ir.response);
                }
            } else {
                throw new Error("Invalid interaction response type " + ir.type);
            }
        } else {
            /* Alert message */
            mDmNotification.showNotification(NotificationInteractionType.TYPE_ALERT_1101);
            setNiaAlertTimeout((context.maxDT > 0 ? context.maxDT : DEFAULT_ALERT_1101_TIMEOUT),
                    NotificationInteractionType.TYPE_ALERT_1101);
        }
    }
}

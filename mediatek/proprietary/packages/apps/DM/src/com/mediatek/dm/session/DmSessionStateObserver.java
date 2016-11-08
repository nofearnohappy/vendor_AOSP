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

package com.mediatek.dm.session;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.dm.DmApplication;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmController;
import com.mediatek.dm.DmService;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.session.SessionEventQueue.DLAbortState;
import com.mediatek.dm.util.DmThreadPool;
import com.redbend.android.RbException.VdmError;
import com.redbend.vdm.SessionInitiator;
import com.redbend.vdm.SessionStateObserver;
import com.redbend.vdm.fumo.FumoAction;
import com.redbend.vdm.lawmo.LawmoAction;
import com.redbend.vdm.scomo.ScomoAction;

public class DmSessionStateObserver implements SessionStateObserver {
    private static final String INITIATOR_SCOMO = DmConst.SessionInitiatorId.INITIATOR_SCOMO;
    private static final String INITIATOR_FUMO = DmConst.SessionInitiatorId.INITIATOR_FUMO;
    private static final String INITIATOR_LAWMO = DmConst.SessionInitiatorId.INITIATOR_LAWMO;
    private static final String INITIATOR_NETWORK = DmConst.SessionInitiatorId.INITIATOR_NETWORK;
    private static final String INITIATOR_CP_BOOTSTRAP = DmConst.SessionInitiatorId.INITIATOR_CP_BOOTSTRAP;
    private static final String INITIATOR_DM_BOOTSTRAP = DmConst.SessionInitiatorId.INITIATOR_DM_BOOTSTRAP;
    private static final String INITIATOR_CI = DmConst.SessionInitiatorId.INITIATOR_CI;

    public DmSessionStateObserver() {

    }

    // Interface method of sessionstateobserver.
    // Called by engine when state of session changes.
    public void notify(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator) {
        Log.i(TAG.SESSION,
                new StringBuilder("---- session state notify ----").append("[session-type] = ")
                        .append(type).append("\n").append("[session-stat] = ").append(state)
                        .append("\n").append("[last-err-msg] = ")
                        .append(VdmError.fromInt(lastError)).append("(").append(lastError)
                        .append(")").append("\n").append("[ses-initiator]= ")
                        .append(initiator.getId()).append("\n")
                        .append("---- session state dumped ----").toString());

        String initiatorName = initiator.getId();

        if (initiatorName.startsWith(INITIATOR_SCOMO)) {
            DmService.sSessionType = DmService.SESSION_TYPE_SCOMO;
            final Handler h = DmService.getInstance().getScomoHandler();
            if (type.equals(SessionType.DM)) {
                if (state.equals(SessionState.COMPLETE)) {
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DM_SESSION_COMPLETED));
                } else if (state.equals(SessionState.ABORTED)) {
                    Message msg = h.obtainMessage(IDmPersistentValues.MSG_DM_SESSION_ABORTED);
                    msg.arg1 = lastError;
                    msg.sendToTarget();
                }
            } else if (type.equals(SessionType.DL)) {
                if (state.equals(SessionState.COMPLETE)) {
                    Log.d(TAG.SESSION, "--- DL session COMPLETED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_COMPLETED);
                    h.sendMessage(h
                            .obtainMessage(IDmPersistentValues.MSG_SCOMO_DL_SESSION_COMPLETED));
                } else if (state.equals(SessionState.STARTED)) {
                    Log.d(TAG.SESSION, "--- DL session STARTED ---");
                    h.sendEmptyMessage(IDmPersistentValues.MSG_SCOMO_DL_SESSION_START);
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_STARTED);
                } else if (state.equals(SessionState.ABORTED)) {
                    Log.d(TAG.SESSION, "+++ DL session ABORTED +++");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_ABORTED, Integer.valueOf(lastError));
                    executeSessionAbort(initiatorName, lastError, h);
                }
            }
        } else if (initiatorName.startsWith(INITIATOR_FUMO)
                || initiatorName.startsWith(INITIATOR_LAWMO)) {
            if (initiatorName.startsWith(INITIATOR_FUMO)) {
                DmService.sSessionType = DmService.SESSION_TYPE_FUMO;
            } else if (initiatorName.startsWith(INITIATOR_LAWMO)) {
                DmService.sSessionType = DmService.SESSION_TYPE_LAWMO;
            }
            final Handler h = DmService.getInstance().mHandler;
            if (type.equals(SessionType.DM)) {
                if (state.equals(SessionState.COMPLETE)) {
                    Log.i(TAG.SESSION, "DM session complete, send message to service.");
                    DmAction action = DmController.getDmAction();
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONCOMPLETED,
                            action));
                } else if (state.equals(SessionState.ABORTED)) {
                    Log.i(TAG.SESSION, "DM session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONABORTED,
                            lastError, 0, initiatorName));
                }
            } else if (type.equals(SessionType.DL)) {
                if (state.equals(SessionState.ABORTED)) {
                    Log.d(TAG.SESSION, "+++ DL session ABORTED +++");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_ABORTED, Integer.valueOf(lastError));
                    executeSessionAbort(initiatorName, lastError, h);
                } else if (state.equals(SessionState.STARTED)) {
                    h.sendEmptyMessage(IDmPersistentValues.MSG_DLPKGSTARTED);
                    Log.d(TAG.SESSION, "--- DL session STARTED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_STARTED);
                } else if (state.equals(SessionState.COMPLETE)) {
                    Log.d(TAG.SESSION, "--- DL session COMPLETED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_COMPLETED);
                }

            }
        } else if (initiatorName.startsWith(INITIATOR_NETWORK)
                || initiatorName.startsWith(INITIATOR_CI)) {
            DmService.sSessionType = DmService.SESSION_TYPE_NONE;
            Handler h = DmService.getInstance().mHandler;
            if (type.equals(SessionType.DM)) {

                if (state.equals(SessionState.COMPLETE)) {
                    Log.i(TAG.SESSION, "DM session complete, send message to service.");
                    DmAction action = DmController.getDmAction();
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONCOMPLETED,
                            action));
                } else if (state.equals(SessionState.ABORTED)) {
                    Log.i(TAG.SESSION, "DM session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONABORTED,
                            lastError, 0, initiatorName));
                }
            } else if (type.equals(SessionType.DL)) {
                if (state.equals(SessionState.ABORTED)) {
                    Log.i(TAG.SESSION, "DL session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DLSESSIONABORTED,
                            lastError, 0, initiatorName));

                }
            }
        } else if (initiatorName.startsWith(INITIATOR_CP_BOOTSTRAP)
                || initiatorName.startsWith(INITIATOR_DM_BOOTSTRAP)) {
            DmService.sSessionType = DmService.SESSION_TYPE_BOOTSTRAP;
            Handler h = DmService.getInstance().mHandler;
            if (type.equals(SessionType.BOOTSTRAP)) {
                if (state.equals(SessionState.COMPLETE)) {
                    Log.i(TAG.SESSION, "DM session complete, send message to service.");
                    h.sendMessage(h.obtainMessage(
                            IDmPersistentValues.MSG_BOOTSTRAPSESSIONCOMPLETED, initiatorName));
                } else if (state.equals(SessionState.ABORTED)) {
                    Log.i(TAG.SESSION, "DM session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_BOOTSTRAPSESSIONABORTED,
                            lastError, 0, initiatorName));
                }
            }
        } else {
            Log.e(TAG.SESSION, "unknown initiator: " + initiator.getId());
            return;
        }
    }

    private void executeSessionAbort(final String initiator, final int lastError,
            final Handler handler) {

        final boolean isFumo = initiator.startsWith(INITIATOR_FUMO);
        final boolean isScomo = initiator.startsWith(INITIATOR_SCOMO);
        SessionEventQueue.DLAbortState abState = DmApplication.getInstance().analyzeDLAbortState();
        switch (abState.mValue) {
        case DLAbortState.STATE_NEED_RESUME_NOW:
            Log.d(TAG.CLIENT, "+++ connection re-setup +++, continue DL session...");
            Runnable resumeJob = new Runnable() {
                @Override
                public void run() {
                    if (isFumo) {
                        DmService.getInstance().resumeDlPkg();
                    } else if (isScomo) {
                        DmService.getInstance().resumeDlScomoPkgNoUI();
                    }
                }
            };
            DmThreadPool.getInstance().execute(resumeJob);
            break;

        case DLAbortState.STATE_HAVENOT_TIMEOUT:
            Log.d(TAG.CLIENT, "+++ connection not timeout(5min) +++, waiting...");
            Runnable batchJob = new Runnable() {
                @Override
                public void run() {
                    if (DmApplication.getInstance().isDMWapConnected()) {
                        Log.d(TAG.DEBUG, "[batch-task]->cancel all other pending jobs.");
                        DmApplication.getInstance().cancelAllPendingJobs();

                        Log.d(TAG.DEBUG, "[batch-task]->netowrk re-setup, resume DL.");
                        if (isFumo) {
                            DmService.getInstance().resumeDlPkg();
                        } else if (isScomo) {
                            DmService.getInstance().resumeDlScomoPkgNoUI();
                        }
                    } else {
                        Log.d(TAG.DEBUG, "[batch-task]->network un-ready, bypass.");
                    }
                }
            };
            Runnable pendingJob = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG.DEBUG, "[pending-task]->cancel all other pending jobs.");
                    DmApplication.getInstance().cancelAllPendingJobs();

                    if (DmApplication.getInstance().isDMWapConnected()) {
                        Log.d(TAG.DEBUG, "[pending-task]->netowrk re-setup, resume DL.");
                        if (isFumo) {
                            DmService.getInstance().resumeDlPkg();
                        } else if (isScomo) {
                            DmService.getInstance().resumeDlScomoPkgNoUI();
                        }
                    } else {
                        Log.d(TAG.DEBUG, "[pending-task]->send DL ABORT msg after timeout.");
                        if (isFumo) {
                            handler.sendMessage(handler.obtainMessage(
                                    IDmPersistentValues.MSG_DLSESSIONABORTED, lastError, 0,
                                    initiator));
                        } else if (isScomo) {
                            handler.sendMessage(handler.obtainMessage(
                                    IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED, lastError, 0,
                                    initiator));
                        }
                    }
                }
            };
            DmApplication.getInstance().cancelAllPendingJobs();
            DmApplication.getInstance().scheduleBatchJobs(batchJob, 10 * 1000, abState.mLeftTime);
            DmApplication.getInstance().scheduleJob(pendingJob, abState.mLeftTime);
            break;

        case DLAbortState.STATE_ALREADY_TIMEOUT:
        default:
            Log.i(TAG.SESSION, "DL session aborted/timeout, send message to service.");
            if (isFumo) {
                handler.sendMessage(handler.obtainMessage(IDmPersistentValues.MSG_DLSESSIONABORTED,
                        lastError, 0, initiator));
            } else if (isScomo) {
                handler.sendMessage(handler.obtainMessage(
                        IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED, lastError, 0, initiator));
            }
        }
    }

    public static class DmAction {
        public int mFumoAction = FumoAction.NONE;
        public int mLawmoAction = LawmoAction.NONE;
        public int mScomoAction = ScomoAction.NONE;
    }
}

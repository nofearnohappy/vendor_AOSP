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

package com.mediatek.rcs.contacts.networkcontacts;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Manager of Contacts AutoSync schedule.
 */
public class SyncScheduler {
    private static final String TAG = "NetworkContacts::SyncScheduler";
    private SyncRequestQueue mSyncRequestQueue;
    private ContactsSyncEngine mEngine;
    private ContactsSyncListener mContactsSyncListener;
    private SyncRequest mCurrentSyncRequest = null;
    private boolean mIsSyncing = false;
    private SyncService mService = null;

    private static final int MSG_STATE_CHANGE = 1;


    /**
     * Constructor.
     * @param service Sync Service.
     */
    public SyncScheduler(SyncService service) {
        mService = service;
        mContactsSyncListener = new ContactsSyncListener();
        mSyncRequestQueue = new SyncRequestQueue();
        mEngine = ContactsSyncEngine.getInstance(service);
        mEngine.setListener(mContactsSyncListener);
    }

    /**
     * start execute the sync request task,firstly add the request to the SyncRequestQueue.
     * @param request new request.
     */
    public void syncStart(SyncRequest request) {
        boolean isAdd = mSyncRequestQueue.addRequest(request, mCurrentSyncRequest);
        if (isAdd) {
            if (!mIsSyncing) {
                syncNext();
            }
        }
    }

    /**
     * @return if scheduler is idle, or no request in queue.
     */
    public boolean isIdle() {
        return mSyncRequestQueue.isEmpty();
    }

    /**
     * @return if scheduler is in sync process.
     */
    public boolean isSyncing() {
        return mIsSyncing;
    }

    /**
     * @return if scheduler is in sync process.
     */
    public SyncRequest getCurrentRequest() {
        return mCurrentSyncRequest;
    }

    /**
     * start engine to execute the sync request task.
     * @return void
     */
    private void syncNext() {
        Log.d(TAG, "syncNext +++++");
        mCurrentSyncRequest = mSyncRequestQueue.getFirstRequest();
        if (mCurrentSyncRequest != null) {
            mIsSyncing = true;
            mCurrentSyncRequest.notifyState(SyncRequest.SyncNotify.SYNC_STATE_START);
            switch (mCurrentSyncRequest.mSyncType) {
            case SyncRequest.SYNC_BACKUP:
                mEngine.sync(ContactsSyncEngine.SYNC_MODE_BACKUP);
                break;
            case SyncRequest.SYNC_RESTORE:
                mEngine.sync(ContactsSyncEngine.SYNC_MODE_RESTORE);
                break;
            case SyncRequest.CHECK_RESTORE_RESULT:
                mEngine.sync(ContactsSyncEngine.SYNC_MODE_CHECK_RESTORE);
                break;
            default:
                break;
            }
        } else {
            Log.d(TAG, "SyncRequestQueue is empty !");
        }
        Log.d(TAG, "syncNext -----");
    }
    /**
     * handle the sync result.
     * @param state 0:SyncListener.SYNC_STATE_SUCCESS;-1:SyncListener.SYNC_STATE_ERROR
     * @return void
     */
    private void stateChange(int state) {
        Log.d(TAG, String.format("stateChange: %d ++++", state));

        if (mCurrentSyncRequest != null) {
            if (state <= SyncListener.SYNC_STATE_SUCCESS) {
                switch (state) {
                case SyncListener.SYNC_STATE_SUCCESS:
                    mCurrentSyncRequest
                        .notifyState(SyncRequest.SyncNotify.SYNC_STATE_SUCCESS);
                    /*
                     * we'll not update the version on
                     * SyncRequest.CHECK_RESTORE_RESULT to avoid saving an
                     * unbackuped version.
                     */
                    if (mCurrentSyncRequest.mSyncType != SyncRequest.CHECK_RESTORE_RESULT) {
                        SyncService.ContactsChecker.getInstance(mService)
                                .updateVersion();
                    }
                    break;
                case SyncListener.SYNC_STATE_ERROR:
                    mCurrentSyncRequest
                        .notifyState(SyncRequest.SyncNotify.SYNC_STATE_ERROR);
                    break;
                case SyncListener.SYNC_STATE_SUCCESS_DONOTHING:
                    mCurrentSyncRequest
                        .notifyState(SyncRequest.SyncNotify.SYNC_STATE_SUCCESS_DONOTHING);
                    break;
                default:
                    break;
                }
                mIsSyncing = false;
                mSyncRequestQueue.completeRequest(mCurrentSyncRequest);
                mCurrentSyncRequest = null;
                /*sync next*/
                syncNext();
            }
        } else {
            Log.e(TAG, "receive state change while the current request is null");
        }
        Log.d(TAG, "stateChange -----");
    }

    /**
     * msg handler to serialize the notification.
     * @author MTK80963
     *
     */
    private static class MsgHandler extends Handler {
        private WeakReference<SyncScheduler> mSched;

        public MsgHandler(SyncScheduler sched) {
            mSched = new WeakReference<SyncScheduler>(sched);
        }

        @Override
        public void handleMessage(Message msg) {
            SyncScheduler sched = mSched.get();

            Log.d(TAG, String.format("handleMessage %d", msg.what));
            if (sched == null) {
                Log.d(TAG, "sched has been deleted!!!");
                return;
            }

            switch(msg.what) {
            case MSG_STATE_CHANGE:
                sched.stateChange(msg.arg1);
                break;

            default:
                break;

            }
        }

    };

    private Handler mHandler = new MsgHandler(this);

    /**
     * state listener.
     *
     */
    private class ContactsSyncListener implements SyncListener {

        @Override
        public void syncNotify(int state) {
            Message.obtain(mHandler, MSG_STATE_CHANGE, state, 0).sendToTarget();
        }
    }
}

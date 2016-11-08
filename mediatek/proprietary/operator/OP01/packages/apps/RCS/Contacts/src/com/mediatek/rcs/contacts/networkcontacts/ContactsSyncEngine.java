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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Contacts Backup/Restore engine.
 *
 */
public final class ContactsSyncEngine {
    private static final String TAG = "NetworkContacts::ContactsSyncEngine";
    /**
     * If use refresh from server to restore contacts.
     */
    private static final boolean REFRESH_FROM_SERVER = true;
    //available values for syncMode
    public static final int SYNC_MODE_BACKUP = 1;
    public static final int SYNC_MODE_RESTORE = 2;
    public static final int SYNC_MODE_CHECK_RESTORE = 3;

    public static final int SYNC_STATE_INIT = 0;
    /**
     * start syncing.
     */
    public static final int SYNC_STATE_SYNCING = 1;
    /**
     * canceled by user, engine not responsed.
     */
    public static final int SYNC_STATE_CANCELED = 1;

    private Context mContext;
    private int mSyncMode;
    private Integer mSyncState = SYNC_STATE_INIT;
    private boolean mCanCancel = false;
    private Boolean mCancel = false;
    private SyncThread mWorkerThread = null;

    private SyncListener mListener = null;

    private SyncSource mSource = null;
    private TransportConnection mConnection = new TransportConnection();
    private static volatile ContactsSyncEngine sEngine = null;

    /**
     * constructor.
     */
    private ContactsSyncEngine(Context ctx) {
        //mSource = new VCardContactSource(ctx);
        mSource = new ContactsSource(ctx);
        mContext = ctx;
    }

    /**
     * Get engine instance.
     * @param ctx
     *        Cannot be null when first call this function
     * @return SyncEngine
     */
    public static ContactsSyncEngine getInstance(Context ctx) {
        if (sEngine == null) {
            synchronized (ContactsSyncEngine.class) {
                if (sEngine == null) {
                    if (ctx == null) {
                        Log.e(TAG, "getInstance: cannot create engine while ctx is null");
                        return null;
                    }
                    sEngine = new ContactsSyncEngine(ctx);
                }
            }
        }

        return sEngine;
    }

    /**
     * @return context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Start synchronization.
     * @param syncMode valid values: {@link #SYNC_MODE_BACKUP}, {@link #SYNC_MODE_RESTORE}
     * @throws SyncException engine is busy.
     */
    public synchronized void sync(int syncMode) throws SyncException {
        synchronized (mSyncState) {
            if (mSyncState != SYNC_STATE_INIT) {
                throw new SyncException(SyncException.ERR_BUSY,
                        "Engine is busy!");
            }
            mSyncMode = syncMode;

            /* Create sync thread */
            mWorkerThread = new SyncThread();
            mWorkerThread.start();
            mSyncState = SYNC_STATE_SYNCING;
            mCanCancel = true;
        }
    }

    /**
     * Cancel synchronization.
     * @return true or false
     */
    public boolean cancel() {
        if (!mCanCancel) {
            Log.e(TAG, String.format("Cannot be canceled, state: %d!", mSyncState));
            return false;
        }

        synchronized (mCancel) {
            mCancel = true;
        }

        return true;
    }

    /**
     * @param l listener.
     */
    public void setListener(SyncListener l) {
        mListener = l;
    }

    /**
     * init all the data modified by sync.
     */
    private void reInit() {
        synchronized (mSyncState) {
            mWorkerThread = null;
            mCanCancel = false;
            mCancel = false;
            mSyncState = SYNC_STATE_INIT;
        }
    }

    private static final int MSG_STATE_ERROR = -1;
    private static final int MSG_STATE_SUCESS = 0;
    private static final int MSG_STATE_SUCESS_DONOTHING = -2;
    /**
     * data has been sent to server. cannot be canceled for backup.
     */
    private static final int MSG_STATE_TO_SERVER = 1;
    /**
     * Data has been commit to datastore. Cannot be canceled for restore.
     */
    private static final int MSG_STATE_TO_LOCAL = 2;

    /**
     *
     * @author MTK80963
     *
     */
    private static class StateChangeHandler extends Handler {
        private WeakReference<ContactsSyncEngine> mEngine;

        public StateChangeHandler(ContactsSyncEngine engine) {
            mEngine = new WeakReference<ContactsSyncEngine>(engine);
        }

        @Override
        public void handleMessage(Message msg) {
            ContactsSyncEngine engine = mEngine.get();

            Log.d(TAG, String.format("handleMessage %d", msg.what));
            if (engine == null) {
                Log.d(TAG, "engine has been deleted!!!");
                return;
            }

            SyncListener listener = engine.mListener;

            switch(msg.what) {
            case MSG_STATE_ERROR:
                if (listener != null) {
                    listener.syncNotify(SyncListener.SYNC_STATE_ERROR);
                }
                engine.reInit();
                break;

            case MSG_STATE_SUCESS:
                if (listener != null) {
                    listener.syncNotify(SyncListener.SYNC_STATE_SUCCESS);
                }
                engine.reInit();
                break;

            case MSG_STATE_SUCESS_DONOTHING:
                if (listener != null) {
                    listener.syncNotify(SyncListener.SYNC_STATE_SUCCESS_DONOTHING);
                }
                engine.reInit();
                break;

            case MSG_STATE_TO_SERVER:
                if (engine.mSyncMode == SYNC_MODE_BACKUP) {
                    engine.mCanCancel = false;
                }
                break;

            case MSG_STATE_TO_LOCAL:
                if (engine.mSyncMode == SYNC_MODE_RESTORE) {
                    engine.mCanCancel = false;
                }
                break;

            default:
                break;
            }
        }

    };

    private Handler mStateChangeHandler = new StateChangeHandler(this);

    /**
     * native API
     * All the native API must be called by the worker thread.
     */
    static {
        try {
            Log.d(TAG, "loadLibrary ");
            System.loadLibrary("jni_mds");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "jni_mds.so is not found!");
        }
    }

    /**
     *
     * @param mode
     * @return: 0 -- success, others -- fail
     */
    private native int nativeSync(int mode);

    /**
     * called from native by worker thread.
     * @param state
     */
    private void syncStateChange(int state) {
        Message msg = new Message();
        msg.what = state;
        mStateChangeHandler.sendMessage(msg);
    }

    private boolean isCanceled() {
        synchronized (mSyncState) {
            return mSyncState == SYNC_STATE_CANCELED;
        }
    }

    private SyncSource getSyncSource() {
        return mSource;
    }

    private TransportConnection getTransportConnection() {
        return mConnection;
    }

    /**
     * @author MTK80963
     *
     */
    private class SyncThread extends Thread {
        public SyncThread() {
            super();
        }

        @Override
        public void run() {
            int ret = 0;
            if (SYNC_MODE_CHECK_RESTORE == mSyncMode) {
                if (REFRESH_FROM_SERVER) {
                    syncStateChange(mSource.checkBackup() ? MSG_STATE_SUCESS
                            : MSG_STATE_ERROR);
                }
                return;
            }
            mSource.startSync();
            /* Normal sync */
            if ((SYNC_MODE_RESTORE == mSyncMode) && REFRESH_FROM_SERVER) {
                //backup current database
                mSource.backup();
            }

            ret = nativeSync(mSyncMode);
            mSource.endSync();
            Log.d(TAG, "ret : " + ret);
            if (ret == 0) {
                //delete backup database
                if ((SYNC_MODE_RESTORE == mSyncMode) && REFRESH_FROM_SERVER) {
                    if (mSource.isEmptyRestoreFromServer()) {
                        mSource.rollback();
                        syncStateChange(MSG_STATE_SUCESS_DONOTHING);
                    } else {
                        mSource.clearBackup();
                        syncStateChange(MSG_STATE_SUCESS);
                    }
                } else {
                    syncStateChange(MSG_STATE_SUCESS);
                }
            } else {
                //restore backup database
                if ((SYNC_MODE_RESTORE == mSyncMode) && REFRESH_FROM_SERVER) {
                    mSource.rollback();
                }
                syncStateChange(MSG_STATE_ERROR);
            }
        }
    }
}

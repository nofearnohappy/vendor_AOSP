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

package com.mediatek.rcs.contacts.networkcontacts.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.networkcontacts.SyncRequest;
import com.mediatek.rcs.contacts.networkcontacts.SyncService;
import com.mediatek.rcs.contacts.networkcontacts.SyncService.SyncBinder;

import java.lang.ref.WeakReference;

/**
 * The activity be launched to ask user if need to restore the.
 * contacts from cloud while first using contacts
 *
 * @author MTK81350
 *
 */
public class AutoRestoreActivity extends Activity implements SyncRequest.SyncNotify {
    private static final String TAG = "NetworkContacts::AutoRestoreActivity";
    private ProgressDialog mProgressDialog = null;
    private SyncService mService = null;
    private int mSyncResult;
    private static final int MSG_SERVICE_CONNECTTED = 0;
    SyncBinder mBinder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(BIND_AUTO_CREATE);
        bindService();
    }

    /**
     * dialog fragment.
     *
     */
    public static class ConfirmDialogFragment extends DialogFragment {

        /**
         * @param msg msg to display
         * @return dialog fragment instance.
         */
        public static ConfirmDialogFragment getInstance(String msg) {
            ConfirmDialogFragment dia = new ConfirmDialogFragment();
            Bundle b = new Bundle();
            b.putString("msg", msg);
            dia.setArguments(b);
            return dia;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            String msg = getArguments().getString("msg");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("title")
                    .setCancelable(false)
                    .setMessage(msg)
                    .setPositiveButton(getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dialog.dismiss();
                                    getActivity().finish();
                                }
                            });

            return builder.create();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        if (mBinder == null) {
            bindService();
        }
        setIntent(intent);
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mService != null) {
            unbindService(mConnection);
        }
        mProgressDialog = null;
        mService = null;
        super.onDestroy();

    }

    private void bindService() {
        Intent intent = new Intent(this, SyncService.class);
        Log.i(TAG, "Bindservice...");
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);

    }

    private ServiceConnection mConnection = new ServiceConnection() {
       @Override
       public void onServiceConnected(ComponentName name, IBinder service) {
           mBinder = (SyncBinder) service;
           mService = mBinder.getService();
           Log.i(TAG, "onServiceConnected.....");
           mHandler.sendEmptyMessage(MSG_SERVICE_CONNECTTED);
       }

       @Override
       public void onServiceDisconnected(ComponentName name) {
           Log.i(TAG, "onServiceDisConnected.....");
           mService = null;
       }
    };

    private Runnable mShowProgressDialog = new Runnable() {

        @Override
        public void run() {
            if (mProgressDialog == null) {
                Log.d(TAG, "mProgressDialog == null");
                if (isFinishing()) {
                    Log.d(TAG, "isFinishing");
                }
                mProgressDialog = new ProgressDialog(AutoRestoreActivity.this);
            }
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setMessage(getString(R.string.restoring));
            mProgressDialog.show();
        }

    };
    private Runnable mCancelProgressDialog = new Runnable() {

        @Override
        public void run() {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;

                String msg;
                if (mSyncResult == SYNC_STATE_SUCCESS) {
                    msg = getString(R.string.restore_success);
                } else if (mSyncResult == SYNC_STATE_ERROR) {
                    msg = getString(R.string.restore_fail);
                } else {
                    Log.e(TAG, String.format("Invalid sync result: %d", mSyncResult));
                    return;
                }

                ConfirmDialogFragment diaFrag = ConfirmDialogFragment.getInstance(msg);
                diaFrag.show(getFragmentManager(), "confirm");
            }
        }
    };

    /**
     * internal handler.
     *
     */
    private static class MsgHandler extends Handler {
        private WeakReference<AutoRestoreActivity> mAct;

        public MsgHandler(AutoRestoreActivity act) {
            mAct = new WeakReference<AutoRestoreActivity>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            AutoRestoreActivity act = mAct.get();

            Log.d("AutoRestoreActivity", String.format("handleMessage %d", msg.what));
            if (act == null) {
                Log.d("AutoRestoreActivity", "act has been deleted!!!");
                return;
            }

            switch(msg.what) {
            case MSG_SERVICE_CONNECTTED:
                act.mService.sync(new SyncRequest(SyncRequest.SYNC_RESTORE, act));
                break;

            default:
                break;

            }
        }

    };

    private Handler mHandler = new MsgHandler(this);

    @Override
    public void onStateChange(int state, SyncRequest request) {
        mSyncResult = state;

        switch (state) {
        case SYNC_STATE_START:
            mHandler.post(mShowProgressDialog);
            break;
        case SYNC_STATE_SUCCESS:
            mHandler.post(mCancelProgressDialog);
            break;
        case SYNC_STATE_ERROR:
            mHandler.post(mCancelProgressDialog);
            break;
        default:
            break;
        }

    }
}

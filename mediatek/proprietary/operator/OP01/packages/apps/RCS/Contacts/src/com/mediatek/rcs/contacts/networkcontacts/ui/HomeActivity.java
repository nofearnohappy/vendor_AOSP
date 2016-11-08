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
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.networkcontacts.ContactsSource;
import com.mediatek.rcs.contacts.networkcontacts.NetworkStatusManager;
import com.mediatek.rcs.contacts.networkcontacts.SettingsSharedPreference;
import com.mediatek.rcs.contacts.networkcontacts.SyncRequest;
import com.mediatek.rcs.contacts.networkcontacts.SyncService;
import com.mediatek.rcs.contacts.networkcontacts.SyncService.SyncBinder;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.chat.ChatService;

/**
 *
 * @author MTK81350
 *
 */
public class HomeActivity extends Activity implements SyncRequest.SyncNotify {
    /** Called when the activity is first created. */
    private static final String TAG = "NetworkContacts::HomeActivity";
    private Button mBackup;
    private Button mRestore;
    private ProgressDialog mProgressDialog = null;

    private SyncService mService = null;
    private SyncBinder mBinder;
    private int mSyncResult;
    private int mSyncType;
    private NetworkStatusManager mNetworkStatusManager = null;
    private SettingsSharedPreference mPreferences;
    private ChatService mChatService = null;
    private boolean mRegistrationStatus = false;
    private RegistrationListener mRegistrationListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "getIntent().getAction(): " + getIntent().getAction());
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_contacts_backuprestore);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(false);

        initializeRcsRegisterService();

        mNetworkStatusManager = new NetworkStatusManager(this);
        mPreferences = new SettingsSharedPreference(this);

        mBackup = (Button) findViewById(R.id.button_backup_id);
        mRestore = (Button) findViewById(R.id.button_restore_id);

        mBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog(true);
            }
        });

        mRestore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog(false);
            }
        });
        bindService();
    }

    private void fireBackup() {
        mSyncType = SyncRequest.SYNC_BACKUP;
        mService.sync(new SyncRequest(SyncRequest.SYNC_BACKUP,
                HomeActivity.this));
    }

    private void fireRestore() {
        mSyncType = SyncRequest.SYNC_RESTORE;
        /* disable immediate backup*/
        mService.blockImmBackup();
        mService.sync(new SyncRequest(SyncRequest.SYNC_RESTORE, HomeActivity.this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.setting:
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), SettingsActicity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater class is used to instantiate menu XML files into Menu objects
        Log.d(TAG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contacts_backup_restore_setting, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mService != null) {
            mService.removeAutoBackupListener(this);
            unbindService(mConnection);
        }

        unInitializeRcsRegisterService();

        mPreferences = null;
        mBackup = null;
        mRestore = null;
        mService = null;
        mBinder = null;
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
           Log.i(TAG, "onServiceConnected...");
           mService.addAutoBackupListener(HomeActivity.this);
       }

       @Override
       public void onServiceDisconnected(ComponentName name) {
           Log.i(TAG, "onServiceDisConnected...");
           mService = null;
       }
    };

    private void showConfirmDialog(boolean isBackup) {
        final boolean backup = isBackup;
        int strid;

        boolean isBackupEmpty = false;
        //If rcs is registered to server
        if (mRegistrationStatus) {
            if (isBackup) {
                ContactsSource contacts = new ContactsSource(this);
                isBackupEmpty = (contacts.getAllItemCountWithNoCache() == 0);
                /* check if contacts is empty */
                if (isBackupEmpty) {
                    strid = R.string.no_contacts;
                } else {
                    strid = R.string.confirm_backup;
                }
            } else {
                strid = R.string.confirm_restore;
            }
        } else {
            strid = R.string.rcs_unavailable;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        AlertDialog ad = builder.create();
        ad.setMessage(getString(strid));
        //If rcs is registered to server
        if (mRegistrationStatus) {
            if (isBackupEmpty) {
                ad.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            } else {
                ad.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (backup) {
                                    if (!showNetworkDialog(true)) {
                                        fireBackup();
                                    }
                                } else {
                                    if (!showNetworkDialog(false)) {
                                        fireRestore();
                                    }
                                }
                            }
                        });
                ad.setButton(AlertDialog.BUTTON_NEGATIVE,
                        getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }
        } else {
            ad.setButton(AlertDialog.BUTTON_POSITIVE,
                    getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }
        ad.show();
    }

    /**
     * @return true on show a dialog, false on no need to show.
     */
    private boolean showNetworkDialog(boolean isBackup) {
        final boolean backup = isBackup;
        /**
         * 0 -- no need to show dialog
         * 1 -- show dialog to ask setting wlan.
         * 2 -- show dialog to ask continue
         */
        int dialogType = 0;
        if (!mNetworkStatusManager.isWiFiConnected()) {
            if (mNetworkStatusManager.isConnected()
                    && !mPreferences.isWifiBackupOnly()) {
                dialogType = 2;
            } else {
                dialogType = 1;
            }
        }

        if (0 == dialogType) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        AlertDialog ad = builder.create();
        if (1 == dialogType) {
            ad.setMessage(getString(R.string.open_wifi_tips));
            ad.setButton(AlertDialog.BUTTON_POSITIVE,
                    getString(R.string.set_wlan),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIFI_SETTINGS));
                        }
                    });
        } else {
            ad.setMessage(getString(R.string.open_wifi_propose));
            ad.setButton(AlertDialog.BUTTON_POSITIVE,
                    getString(R.string.btcontinue),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (backup) {
                                fireBackup();
                            } else {
                                fireRestore();
                            }
                        }
                    });

        }

        ad.setButton(AlertDialog.BUTTON_NEGATIVE,
                getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
            });
        ad.show();

        return true;
    }

    private void showResultDialog() {
        String msg;
        if (mSyncResult == SYNC_STATE_SUCCESS) {
            if (mSyncType == SyncRequest.SYNC_BACKUP) {
                msg = getString(R.string.backup_success);
            } else {
                msg = getString(R.string.restore_success);
            }
        } else if (mSyncResult == SYNC_STATE_ERROR) {
            if (mSyncType == SyncRequest.SYNC_RESTORE) {
                msg = getString(R.string.restore_fail);
            } else {
                msg = getString(R.string.backup_fail);
            }
        } else if (mSyncResult == SYNC_STATE_SUCCESS_DONOTHING) {
            msg = getString(R.string.restore_success_donoting);
        } else {
            Log.e(TAG, String.format("Invalid sync result: %d", mSyncResult));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        AlertDialog ad = builder.create();
        ad.setMessage(msg);
        DialogInterface.OnClickListener l = null;
        ad.setButton(AlertDialog.BUTTON_NEGATIVE,
                HomeActivity.this.getString(android.R.string.ok), l);
        ad.show();

    }

    private Runnable mShowProgressDialog = new Runnable() {

        @Override
        public void run() {
            if (mProgressDialog == null) {
                Log.d(TAG, "mProgressDialog == null");
                if (isFinishing()) {
                    Log.d(TAG, "isFinishing");
                }
                mProgressDialog = new ProgressDialog(HomeActivity.this);
            }
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            if (mSyncType == SyncRequest.SYNC_BACKUP) {
                mProgressDialog.setMessage(getString(R.string.backuping));
            } else if (mSyncType == SyncRequest.SYNC_RESTORE) {
                mProgressDialog.setMessage(getString(R.string.restoring));
            } else {
                Log.d(TAG, "unKnown sync type:" + mSyncType);
            }
            mProgressDialog.show();
        }
    };

    private Runnable mCancelProgressDialog = new Runnable() {

        @Override
        public void run() {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;
            }
            showResultDialog();
        }

    };

    private Runnable mUnblockImmBackup = new Runnable() {

        @Override
        public void run() {
            if (mService != null) {
                mService.unblockImmBackup();
            } else {
                Log.e(TAG, "mUnblockImmBackup: mService is null");
            }
        }
    };

    private Handler mHandler = new Handler();

    @Override
    public void onStateChange(int state, SyncRequest request) {
        mSyncResult = state;
        mSyncType = request.mSyncType;

        switch (state) {
        case SYNC_STATE_START:
            mHandler.post(mShowProgressDialog);
            break;
        case SYNC_STATE_SUCCESS:
            mHandler.post(mCancelProgressDialog);
            mHandler.post(mUnblockImmBackup);
            break;
        case SYNC_STATE_SUCCESS_DONOTHING:
            mHandler.post(mCancelProgressDialog);
            mHandler.post(mUnblockImmBackup);
            break;
        case SYNC_STATE_ERROR:
            mHandler.post(mCancelProgressDialog);
            mHandler.post(mUnblockImmBackup);
            break;
        default:
            break;
        }
    }

    private void initializeRcsRegisterService() {
        mRegistrationListener = new RegistrationListener();
        mChatService = new ChatService(this, new MyJoynServiceListener());
        mChatService.connect();
    }

    private void unInitializeRcsRegisterService() {
        try {
            mChatService.removeServiceRegistrationListener(mRegistrationListener);
            mChatService.disconnect();
            mChatService = null;
            mRegistrationListener = null;
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    private class MyJoynServiceListener implements JoynServiceListener {
        @Override
        public void onServiceConnected() {
            Log.d(TAG, "ChatService onServiceConnected entry");
            try {
                mRegistrationStatus = mChatService.isServiceRegistered();
                Log.d(TAG, "mRegistrationStatus :" + mRegistrationStatus);
                mChatService.addServiceRegistrationListener(mRegistrationListener);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int error) {
            Log.d(TAG, "ChatService onServiceDisconnected entry");
            mRegistrationStatus = false;
        }
    }

    private class RegistrationListener extends JoynServiceRegistrationListener {
        @Override
        public void onServiceRegistered() {
            Log.d(TAG, "RCS Registered");
            mRegistrationStatus = true;
        }

        @Override
        public void onServiceUnregistered() {
            Log.d(TAG, "RCS Unregistered");
            mRegistrationStatus = false;
        }
    }
}

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

package com.mediatek.dm.fumo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.dm.DmApplication;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmDownloadNotification;
import com.mediatek.dm.DmPLStorage;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.mediatek.dm.conn.DmDataConnection;
import com.mediatek.dm.data.DownloadInfo;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.util.DialogFactory;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.DownloadDescriptor.Field;
import com.redbend.vdm.PLStorage.ItemType;
import com.redbend.vdm.VdmException;


import junit.framework.Assert;

public class DmClient extends Activity implements OnDismissListener {

    private static final int ONE_SEVOND = 1000;
    private static final int TIME_OUT_VALUE = 30;
    private ProgressDialog mNetworkDetectProgressDialog;

    private Button mDownloadButton;
    private Button mPausedButton;
    private Button mCancelButton;
    private Button mUpdateButton;
    private Button mRetryButton;
    private TextView mDlRatialTextView;
    private TextView mDlDescriptionsTextView;
    private TextView mDlNewFeatureNotesTextView;
    private TextView mAuthenticationTextView;
    private ListView mSingleChoiceListView;
    private ProgressBar mDlPackageProgressBar;
    private ProgressDialog mDlDdProgressBar;
    private ProgressDialog mResumingDialog;
    private ProgressDialog mVerifyingDialog;

    private DmService mService;
    private int mUpdateType = 0;
    private int mStatus;

    private static DmClient sClientInstance;

    static final int DIALOG_NETWORK_ERROR = 0;
    static final int DIALOG_NON_NEWVERSION = 1;
    static final int DIALOG_CANCEL_DOWNLOAD = 2;
    static final int DIALOG_NO_ENOUGH_SPACE = 3;
    static final int DIALOG_GPRS_DOWNLOAD = 4;
    static final int DIALOG_ROAMING_DOWNLOAD = 5;
    static final String ACTION_MANAGE_APPLICATION = "android.settings.MANAGE_APPLICATIONS_SETTINGS";
    static final String EXTRA_TAG_CLIENT = "DmClient";
    private int mShowingDialogId = -1;

    private boolean mActive = false; // when client is foreground it is true.

    /**
     * Get the reference of dm client instance.
     *
     * @return The reference of current dm client instance
     */
    public static synchronized DmClient getVdmClientInstance() {
        return sClientInstance;
    }

    /**
     * Override function of android.app.Activity, bind dm service when create.
     */
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG.CLIENT, "DmClient->onCreate()");
        super.onCreate(savedInstanceState);
        sClientInstance = this;

        if (mFumoObserver != null) {
            Log.w(TAG.CLIENT, "onCreate DmClient register listener");
            PersistentContext.getInstance(this).registerObserver(mFumoObserver);
        }

        // start and bind the dm controller service.
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction(DmConst.IntentAction.ACTION_DM_SERVE);

        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void onResume() {
        Log.i(TAG.CLIENT, "DmClient->onResume()");
        mActive = true;
        super.onResume();
    }

    protected void onPause() {
        Log.i(TAG.CLIENT, "DmClient->onPause()");
        mActive = false;
        super.onPause();
    }

    protected void onNewIntent(Intent intent) {
        Log.d(TAG.CLIENT, "DmClient->onNewIntent()");
        super.onNewIntent(intent);
        if (mShowingDialogId != -1) {
            mStatus = PersistentContext.getInstance(this).getDLSessionStatus();

            Log.i(TAG.CLIENT, "onNewIntent, mShowingDialogId = " + mShowingDialogId);
            Log.i(TAG.CLIENT, "onNewIntent, getDLSessionStatus = " + mStatus);

            if ((IDmPersistentValues.STATE_PAUSE_DOWNLOAD == mStatus && mShowingDialogId == DIALOG_CANCEL_DOWNLOAD)
                    || (IDmPersistentValues.STATE_NEW_VERSION_DETECTED == mStatus && mShowingDialogId != DIALOG_CANCEL_DOWNLOAD)
                    || (IDmPersistentValues.STATE_DOWNLOADING == mStatus)) {
                return;
            } else {
                dismissDialog(mShowingDialogId);
                mShowingDialogId = -1;
            }
        }
        if (mService == null) {
            Log.w(TAG.CLIENT, "DmClient->onNewIntent() mService is null, wait for bind service");
            return;
        }
        if (Options.USE_DIRECT_INTERNET) {
            Log.i(TAG.CLIENT, "onNewIntent, [option]=internet, execute update...");
            executeUpdate();
        } else {
            Log.i(TAG.CLIENT, "onNewIntent, [option]=DM-WAP, check network...");
            checkNetwork();
        }
    }

    /**
     * Override function of android.app.Activity, unbind service when destroy.
     */
    public void onDestroy() {
        Log.i(TAG.CLIENT, "DmClient->onDestroy()");

        sClientInstance = null;
        cancleDialog();
        cancelResumingDialog();

        if (mFumoObserver != null) {
            Log.w(TAG.CLIENT, "onDestroy DmClient unregister listener");
            PersistentContext.getInstance(this).unregisterObserver(mFumoObserver);
        }
        unbindService(mConnection);
        super.onDestroy();
    }

    /**
     * Override function of android.app.Activity, called when change the orientation of screen.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG.CLIENT, "DmClient->onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Show query new version UI and invoke query function of dm service.
     */
    private void onQueryNewVersion() {
        Log.i(TAG.CLIENT, "DmClient==>onQueryNewVersion()");
        DmPLStorage storage = new DmPLStorage(this);
        if (storage != null) {
            storage.delete(ItemType.DLRESUME);
        }
        PersistentContext.getInstance(this).deleteDeltaPackage();
        setDLStatus(IDmPersistentValues.STATE_QUERY_NEW_VERSION);
        setContentView(R.layout.main);
        if (mDlDdProgressBar == null) {
            mDlDdProgressBar = new ProgressDialog(this);
            mDlDdProgressBar.setCancelable(false);
            mDlDdProgressBar.setIndeterminate(true);
            mDlDdProgressBar.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mDlDdProgressBar.setMessage(getString(R.string.wait));
        }
        mDlDdProgressBar.show();

        mService.setSessionInitor(IDmPersistentValues.CLIENT_PULL);
        if (mService.mSessionStateHandler != null) {
            mService.mSessionStateHandler
                    .sendEmptyMessage(IDmPersistentValues.STATE_QUERY_NEW_VERSION);
        } else {
            Log.w(TAG.CLIENT, "[onQueryNewVersion] mService.mSessionStateHandler is null");
        }
    }

    /**
     * Show network error UI and will query again if click retry button.
     */
    public void onNetworkError() {
        Log.w(TAG.CLIENT, "DmClient==>onNetworkError()");
        cancleDialog();
        cancelResumingDialog();
        setContentView(R.layout.networkerror);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if (mRetryButton == null) {
            return;
        }

        mRetryButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.d(TAG.CLIENT, "[onNetworkError] mStatus = " + mStatus);
                if (mStatus == IDmPersistentValues.STATE_DOWNLOADING
                        || mStatus == IDmPersistentValues.STATE_START_TO_DOWNLOAD
                        || mStatus == IDmPersistentValues.STATE_PAUSE_DOWNLOAD
                        || mStatus == IDmPersistentValues.STATE_RESUME_DOWNLOAD) {

                    mStatus = IDmPersistentValues.STATE_PAUSE_DOWNLOAD;
                    onDownloadingPkg(IDmPersistentValues.STATE_RESUME_DOWNLOAD);

                } else if (mStatus == IDmPersistentValues.STATE_DM_DETECT_WAP
                        || mStatus == IDmPersistentValues.STATE_DM_WAP_CONNECT_TIMEOUT) {

                    checkNetwork();

                } else {

                    onQueryNewVersion();

                }
            }
        });
    }

    /**
     * Show other error (error message except network error) UI and will query again if click retry
     * button.
     */
    public void onOtherError(String errorMsg) {
        Log.w(TAG.CLIENT, "DmClient==>onOtherError(" + errorMsg + ")");
        cancleDialog();
        cancelResumingDialog();
        setContentView(R.layout.networkerror);

        mAuthenticationTextView = (TextView) findViewById(R.id.errorView);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if ((mAuthenticationTextView == null) || (mRetryButton == null)) {
            return;
        }

        mAuthenticationTextView.setText(errorMsg);
        mRetryButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                onQueryNewVersion();
            }
        });
    }

    /**
     * Show no new version UI.
     */
    public void onNoNewVersionDetected() {
        Log.i(TAG.CLIENT, "DmClient==>onNoNewVersionDetected()");
        cancleDialog();
        cancelResumingDialog();
        // mDmPersistent.deleteDlInfo();
        setContentView(R.layout.nonewversion);
    }

    /**
     * Show new version detected UI and will start download if click download button.
     *
     * @param DownloadDescriptor
     *            dd - Download descriptor of delta package to download.
     */
    public void onNewVersionDetected(final DownloadDescriptor dd) {
        Log.i(TAG.CLIENT, "DmClient==>onNewVersionDetected(),dd=" + dd);
        if (dd == null) {
            Log.w(TAG.CLIENT, "onNewVersionDetected dd is null");
            return;
        }

        cancleDialog();
        cancelResumingDialog();

        setContentView(R.layout.releasenotes);
        mDlDescriptionsTextView = (TextView) findViewById(R.id.dscrpContent);
        mDlNewFeatureNotesTextView = (TextView) findViewById(R.id.featureNotes);
        mDownloadButton = (Button) findViewById(R.id.buttonDl);
        if ((mDlDescriptionsTextView == null) || (mDlNewFeatureNotesTextView == null)
                || (mDownloadButton == null)) {
            return;
        }

        String description = new StringBuilder(mDlDescriptionsTextView.getText()).append(" ")
                .append(dd.getField(Field.VERSION)).append(" (").append(dd.size).append(" Bytes)")
                .toString();
        String releasenotes = dd.getField(Field.DESCRIPTION);
        mDlDescriptionsTextView.setText(description);
        mDlNewFeatureNotesTextView.setText(releasenotes);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // On download button clicked
                boolean hasEnoughSpace = PersistentContext.getInstance(DmClient.this).getMaxSize() > dd.size * 2.5;
                if (!hasEnoughSpace) {
                    showDialog(DIALOG_NO_ENOUGH_SPACE);
                    mShowingDialogId = DIALOG_NO_ENOUGH_SPACE;
                    return;
                }

                if (!Options.USE_DIRECT_INTERNET) {
                    // for CMCC case (CMWAP connection)
                    onDownloadingPkg(IDmPersistentValues.STATE_START_TO_DOWNLOAD);
                } else {
                    // check network type before downloading.
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connMgr != null) {
                        NetworkInfo net = connMgr.getActiveNetworkInfo();
                        if (net != null) {
                            int netType = net.getType();
                            if (netType == ConnectivityManager.TYPE_MOBILE) {
                                if (net.isRoaming()) {
                                    Log.d(TAG.CLIENT, "[WARNING] downloading in roaming network.");
                                    showDialog(DIALOG_ROAMING_DOWNLOAD);
                                    mShowingDialogId = DIALOG_ROAMING_DOWNLOAD;
                                } else {
                                    Log.d(TAG.CLIENT, "[WARNING] downloading in mobile network.");
                                    showDialog(DIALOG_GPRS_DOWNLOAD);
                                    mShowingDialogId = DIALOG_GPRS_DOWNLOAD;
                                }
                            } else if (netType == ConnectivityManager.TYPE_WIFI) {
                                Log.i(TAG.CLIENT, "[INFO] downloading via wifi.");
                                onDownloadingPkg(IDmPersistentValues.STATE_START_TO_DOWNLOAD);
                            } else {
                                Log.w(TAG.CLIENT,
                                        (new StringBuilder()
                                                .append("[ERROR] invalid network type:")
                                                .append(netType).append(".").append(net
                                                .getTypeName())).toString());
                            }
                        } else {
                            Log.e(TAG.CLIENT,
                                    "[ERROR] no active network for downloading FOTA pack.");
                            showDialog(DIALOG_NETWORK_ERROR);
                            mShowingDialogId = DIALOG_NETWORK_ERROR;
                        }
                    } else {
                        Log.w(TAG.CLIENT, "[ERROR] no active network for downloading FOTA pack.");
                        showDialog(DIALOG_NETWORK_ERROR);
                        mShowingDialogId = DIALOG_NETWORK_ERROR;
                    }
                }

            }
        });
    }

    /**
     * Show downloading UI and will: 1. Pause download if click pause button; 2. Show confirm cancel
     * download dialog if click cancel button; 3. Downloading back ground if click back key.
     *
     * @param int status - downloading status
     */
    private void onDownloadingPkg(int status) {
        int oldStatus = mStatus;

        Log.i(TAG.CLIENT,
                (new StringBuilder("DmClient==>onDownloadingPkg(").append(status).append(
                        "), and old status = ").append(oldStatus)).toString());

        long downloadedSize = PersistentContext.getInstance(this).getDownloadedSize();
        long totalSize = PersistentContext.getInstance(this).getSize();

        if (totalSize == 0L && IDmPersistentValues.STATE_START_TO_DOWNLOAD == status) {
            Log.d(TAG.CLIENT, "status is STATE_START_TO_DOWNLOAD, and totalSize is 0."
                    + "waiting for NEW_VERSION_DETECTED to get DownloadDescriptor.");
            return;
        }

        if (status != IDmPersistentValues.STATE_DOWNLOADING) {
            Log.d(TAG.CLIENT, "FOTA DL is starting, waiting...");
            showResumingDialog();
        }

        Log.i(TAG.CLIENT,
                (new StringBuilder("downloaded size is ").append(downloadedSize).append(
                        ", and total size is ").append(totalSize)).toString());

        setContentView(R.layout.downloading);
        mDlPackageProgressBar = (ProgressBar) findViewById(R.id.progressbarDownload);
        mDlRatialTextView = (TextView) findViewById(R.id.rate);
        mDlDescriptionsTextView = (TextView) findViewById(R.id.dscrpContentDl);
        mDlNewFeatureNotesTextView = (TextView) findViewById(R.id.featureNotesDl);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mPausedButton = (Button) findViewById(R.id.buttonSuspend);
        if ((mDlPackageProgressBar == null) || (mDlRatialTextView == null)
                || (mDlDescriptionsTextView == null) || (mDlNewFeatureNotesTextView == null)
                || (mCancelButton == null) || (mPausedButton == null)) {
            return;
        }

        String description;
        String releasenotes;

        DownloadInfo info = PersistentContext.getInstance(this).getDownloadInfo();
        description = (new StringBuilder(mDlDescriptionsTextView.getText()).append(" ")
                .append(info.mVersion).append(" (").append(totalSize).append(" Bytes)")).toString();

        releasenotes = info.mDescription;
        mDlDescriptionsTextView.setText(description);
        mDlNewFeatureNotesTextView.setText(releasenotes);
        mCancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (IDmPersistentValues.STATE_PAUSE_DOWNLOAD != mStatus) {
                    onDlPkgCancelled();
                }
            }
        });

        mPausedButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (IDmPersistentValues.STATE_PAUSE_DOWNLOAD != mStatus) {
                    onDlPkgPaused();
                }
            }
        });

        onDlPkgUpgrade(downloadedSize, totalSize);

        if (mService.mSessionStateHandler != null) {
            if (oldStatus != status) {
                setDLStatus(status);
                mService.mSessionStateHandler.sendEmptyMessage(status);
            } else {
                Log.d(TAG.CLIENT,
                        "[onDownloadingPkg] old status is the same as the new one, both are "
                                + status);
            }
        } else {
            Log.w(TAG.CLIENT, "[onDownloadingPkg] mService.mSessionStateHandler is null");
        }
    }

    private void showResumingDialog() {
        Log.d(TAG.CLIENT, "DmClient==>showResumingDialog");
        if (mResumingDialog == null) {
            mResumingDialog = new ProgressDialog(this);
            mResumingDialog.setCancelable(false);
            mResumingDialog.setIndeterminate(true);
            mResumingDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mResumingDialog.setMessage(getString(R.string.prepare_download));
        }
        mResumingDialog.show();
    }

    private void cancelResumingDialog() {
        if (mResumingDialog != null) {
            Log.d(TAG.CLIENT, "DmClient==>cancelResumingDialog");
            mResumingDialog.dismiss();
            mResumingDialog = null;
        }
    }

    /**
     * Update the download progress of downloading UI.
     *
     * @param float dlSize - the downloaded size of the delta package
     * @param float totalSize - the total size of the delta package
     */
    public void onDlPkgUpgrade(long dlSize, long totalSize) {
        Log.i(TAG.CLIENT,
                (new StringBuilder("DmClient==>onDlPkgUpgrade(").append(dlSize).append(", ")
                        .append(totalSize).append(")")).toString());

        int ratial = (int) (((float) dlSize / (float) totalSize) * 100);
        if (mDlPackageProgressBar != null) {
            mDlPackageProgressBar.setProgress(ratial);
        }

        if (mDlRatialTextView != null) {
            CharSequence text = (new StringBuilder().append(ratial).append("%    ")
                    .append((int) dlSize).append(" Bytes / ").append((int) totalSize)
                    .append(" Bytes")).toString();
            mDlRatialTextView.setText(text);
        }
    }

    /**
     * The response function of click pause button of downloading UI.
     */
    private void onDlPkgPaused() {
        Log.i(TAG.CLIENT, "DmClient==>onDlPkgPaused()");
        DmApplication.getInstance().cancelAllPendingJobs();

        setDLStatus(IDmPersistentValues.STATE_PAUSE_DOWNLOAD);
        if (mService.mSessionStateHandler != null) {
            mService.mSessionStateHandler
                    .sendEmptyMessage(IDmPersistentValues.STATE_PAUSE_DOWNLOAD);
        } else {
            Log.w(TAG.CLIENT, "[onDlPkgPaused] mService.mSessionStateHandler is null");
        }
        finish();
    }

    /**
     * The response function of click cancel button of downloading UI.
     */
    private void onDlPkgCancelled() {
        Log.i(TAG.CLIENT, "DmClient==>onDlPkgCancelled()");
        DmApplication.getInstance().cancelAllPendingJobs();

        // pause the download first before show the cancel dialog
        Log.d(TAG.CLIENT, "onDlPkgCancelled set the session state is cancled");
        setDLStatus(IDmPersistentValues.STATE_PAUSE_DOWNLOAD);
        if (mService.mSessionStateHandler != null) {
            mService.mSessionStateHandler
                    .sendEmptyMessage(IDmPersistentValues.STATE_PAUSE_DOWNLOAD);
        } else {
            Log.w(TAG.CLIENT, "[onDlPkgCancelled] mService.mSessionStateHandler is null");
        }
        showDialog(DIALOG_CANCEL_DOWNLOAD);
        mShowingDialogId = DIALOG_CANCEL_DOWNLOAD;
    }

    /**
     * Show download complete UI after download delta package finished.
     */
    public void onDlPkgComplete(String[] updateTypes) {
        Log.i(TAG.CLIENT, "DmClient==>onDlPkgComplete():" + updateTypes);
        setContentView(R.layout.updateenquire);
        onShowUpdateList(updateTypes);
        mUpdateButton = (Button) findViewById(R.id.update);
        if (mUpdateButton == null) {
            return;
        }

        mUpdateButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                onUpdateTypeSelected();
            }
        });
    }

    /**
     * The response function of click OK button of download complete UI.
     *
     * @param long typeId - the id of the list view item user selected.
     */
    private void onUpdateTypeSelected() {
        Log.i(TAG.CLIENT, "DmClient==>onUpdateTypeSelected()");
        if (mService.mSessionStateHandler != null) {
            Message msg = mService.mSessionStateHandler
                    .obtainMessage(IDmPersistentValues.STATE_DL_PKG_COMPLETE);
            msg.arg1 = mUpdateType;
            mService.mSessionStateHandler.sendMessage(msg);
        } else {
            Log.w(TAG.CLIENT, "[onUpdateTypeSelected] mService.mSessionStateHandler is null");
        }
        finish();
    }

    /**
     * Show update type list view.
     *
     * @param String
     *            [] updateTypes - string array which contains the text content of update types.
     */
    private void onShowUpdateList(final String[] updateTypes) {
        Log.i(TAG.CLIENT, "DmClient==>onShowUpdateList():" + updateTypes);
        mSingleChoiceListView = (ListView) findViewById(R.id.updatetypelist);
        if (null == mSingleChoiceListView) {
            return;
        }

        mSingleChoiceListView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, updateTypes));
        mSingleChoiceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mSingleChoiceListView.setClickable(true);
        mSingleChoiceListView.setItemsCanFocus(false);
        mSingleChoiceListView.setItemChecked(0, true);
        mSingleChoiceListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2,
                    final long arg3) {
                mSingleChoiceListView.setItemChecked(arg2, true);
                mUpdateType = arg2;
                Log.d(TAG.CLIENT, "onItemClick select is " + arg2);
            }

        });
    }

    @Override
    /**
     * Override function of com.android.Activity
     * @param ind id - The dialog type to create.
     */
    protected Dialog onCreateDialog(final int id) {
        Log.i(TAG.CLIENT, "DmClient->onCreateDialog(" + id + ")");
        AlertDialog dialog = null;
        switch (id) {
        case DIALOG_NETWORK_ERROR:
            dialog = DialogFactory.newAlert(this).setTitle(R.string.networkerror)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            // On informed the network error
                            finish();
                        }
                    }).create();
            break;
        case DIALOG_NON_NEWVERSION:
            dialog = DialogFactory.newAlert(this).setTitle(R.string.nonewversion)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            // On informed the current version is the
                            // latest
                            finish();
                        }
                    }).create();
            break;
        case DIALOG_NO_ENOUGH_SPACE:
            dialog = DialogFactory.newAlert(this).setTitle(R.string.noenoughspace)
                    .setPositiveButton(R.string.appmanager, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            // On user go to application manager to
                            // release
                            // space
                            Intent intent = new Intent();
                            intent.setAction(ACTION_MANAGE_APPLICATION);
                            // intent.putExtra(EXTRA_TAG_CLIENT, true);
                            // sendBroadcast(intent);
                            DmClient.this.startActivity(intent);
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            // On user abort to down load
                            finish();
                        }
                    }).create();
            break;
        case DIALOG_CANCEL_DOWNLOAD:
            dialog = DialogFactory.newAlert(this).setTitle(R.string.cancel)
                    .setMessage(R.string.canceldownload)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            // On user confirm cancel download
                            setDLStatus(IDmPersistentValues.STATE_NOT_DOWNLOAD);
                            if (mService.mSessionStateHandler != null) {
                                mService.mSessionStateHandler
                                        .sendEmptyMessage(IDmPersistentValues.STATE_CANCEL_DOWNLOAD);
                            } else {
                                Log.w(TAG.CLIENT,
                                        "[DIALOG_CANCELDOWNLOAD]positive button, mService.mSessionStateHandler is null");
                            }
                            finish();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int whichButton) {
                            // On user abort cancel download
                            onDownloadingPkg(IDmPersistentValues.STATE_RESUME_DOWNLOAD);
                        }
                    }).create();
            break;
        case DIALOG_GPRS_DOWNLOAD:
            dialog = DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.gprs_download_title)
                    .setMessage(R.string.gprs_download)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    onDownloadingPkg(IDmPersistentValues.STATE_START_TO_DOWNLOAD);
                                }
                            })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            if (mService.mSessionStateHandler != null) {
                                mService.mSessionStateHandler
                                        .sendEmptyMessage(IDmPersistentValues.STATE_NOT_DOWNLOAD);
                            } else {
                                Log.w(TAG.CLIENT,
                                        "[DIALOG_GPRSDOWNLOAD] negative button, mService.mSessionStateHandler is null");
                            }
                            finish();
                        }
                    }).create();
            break;
        case DIALOG_ROAMING_DOWNLOAD:
            dialog = DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.gprs_download_title)
                    .setMessage(R.string.roaming_download)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    onDownloadingPkg(IDmPersistentValues.STATE_START_TO_DOWNLOAD);
                                }
                            })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            if (mService.mSessionStateHandler != null) {
                                mService.mSessionStateHandler
                                        .sendEmptyMessage(IDmPersistentValues.STATE_NOT_DOWNLOAD);
                            } else {
                                Log.w(TAG.CLIENT,
                                        "[DIALOG_ROAMING_DOWNLOAD] negative button, mService.mSessionStateHandler is null");
                            }
                            finish();
                        }
                    }).create();
            break;
        default:
            break;

        }
        if (dialog != null) {
            dialog.setOnDismissListener(DmClient.this);
        }
        return dialog;
    }

    @Override
    public final void onDismiss(final DialogInterface dialog) {
        Log.d(TAG.CLIENT, "dialog onDismiss " + mShowingDialogId);
        mShowingDialogId = -1;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            Log.i(TAG.CLIENT, "DmClient->onServiceConnected(), got service reference");
            mService = ((DmService.DmBinder) service).getService();
            if (mService == null) {
                Log.w(TAG.CLIENT, "onServiceConnected mService is null");
                return;
            }

            if (Options.USE_DIRECT_INTERNET) {
                Log.i(TAG.CLIENT, "[option]=internet, execute update...");
                executeUpdate();
            } else {
                Log.i(TAG.CLIENT, "[option]=DM-WAP, check network...");
                checkNetwork();
            }
        }

        public void onServiceDisconnected(final ComponentName className) {
            mService = null;
        }
    };

    private void executeUpdate() {
        Log.i(TAG.CLIENT, "DmClient==>executeUpdate()");
        if (mService == null) {
            Log.w(TAG.CLIENT, "executeUpdate service is not available");
            return;
        }

        if (!mService.isInitDmController()) {
            mService.initDmController();
        }

        mService.initFotaThread();

        mStatus = PersistentContext.getInstance(this).getDLSessionStatus();
        Log.i(TAG.CLIENT, "executeUpdate state is " + mStatus);

        switch (mStatus) {
        case IDmPersistentValues.STATE_PAUSE_DOWNLOAD:
            onDownloadingPkg(IDmPersistentValues.STATE_RESUME_DOWNLOAD);
            break;
        case IDmPersistentValues.STATE_RESUME_DOWNLOAD:
        case IDmPersistentValues.STATE_START_TO_DOWNLOAD:
        case IDmPersistentValues.STATE_DOWNLOADING:
            onDownloadingPkg(mStatus);
            break;
        case IDmPersistentValues.STATE_DL_PKG_COMPLETE:
            String[] updateTypes = mService.getUpdateTypes();
            if (updateTypes == null) {
                Log.i(TAG.CLIENT, "updateTypes is null.");
                break;
            }
            onDlPkgComplete(updateTypes);
            break;
        case IDmPersistentValues.STATE_NEW_VERSION_DETECTED:
            DownloadDescriptor dd = PersistentContext.getInstance(this).getDownloadDescriptor();
            onNewVersionDetected(dd);
            break;
        case IDmPersistentValues.STATE_VERIFY_NO_STORAGE:
            onVerifyingPky();
            break;
        case IDmPersistentValues.STATE_VERIFY_FAIL:
            onVerifyFail();
            break;
        default:
            onQueryNewVersion();
        }
    }

    private void onVerifyFail() {
        Log.d(TAG.CLIENT, "[onVerifyingPky]");
        onOtherError(this.getString(R.string.error_message, "Upgrade Package Verify Fail"));
    }

    private void onVerifyingPky() {
        Log.d(TAG.CLIENT, "[onVerifyingPky]");
        if (mService.mHandler != null) {
            mService.mHandler.sendEmptyMessage(IDmPersistentValues.MSG_DLPKGCOMPLETE);
        } else {
            Log.w(TAG.CLIENT, "[onVerifyingPky] mService.mHandler is null");
        }
    }

    private synchronized void setDLStatus(int status) {
        Log.d(TAG.CLIENT, "DmClient->setDmStatus(" + status + ")");
        if (status < 0) {
            return;
        }

        mStatus = status;

        PersistentContext.getInstance(this).setDLSessionStatus(status);
    }

    private synchronized void cancleDialog() {
        Log.d(TAG.CLIENT, "DmClient->cancleDialog()");
        if (mDlDdProgressBar != null) {
            Log.i(TAG.CLIENT, "cancleDialog cancle mProgressBarDlDD");
            mDlDdProgressBar.cancel();
            mDlDdProgressBar = null;
        }
    }

    private PersistentContext.FumoUpdateObserver mFumoObserver = new PersistentContext.FumoUpdateObserver() {

        public void syncDLstatus(int status) {
            Log.d(TAG.CLIENT, "FumoObserver->syncDLstatus(" + status + ")");
            switch (status) {
            case IDmPersistentValues.STATE_NEW_VERSION_DETECTED:
                Log.d(TAG.CLIENT, "[syncDLstatus]new version detected.");
                if (mShowingDialogId == DIALOG_CANCEL_DOWNLOAD) {
                    dismissDialog(mShowingDialogId);
                    mShowingDialogId = -1;
                }
                DownloadDescriptor dd = PersistentContext.getInstance(DmClient.this)
                        .getDownloadDescriptor();
                onNewVersionDetected(dd);
                break;
            case IDmPersistentValues.STATE_DOWNLOADING:
                Log.d(TAG.CLIENT, "[syncDLstatus] pkg ongoing.");
                cancelResumingDialog();
                long currentSize = PersistentContext.getInstance(DmClient.this).getDownloadedSize();
                long totalSize = PersistentContext.getInstance(DmClient.this).getSize();
                onDlPkgUpgrade(currentSize, totalSize);
                break;
            case IDmPersistentValues.STATE_START_TO_DOWNLOAD:
                Log.d(TAG.CLIENT, "[syncDLstatus] pkg started.");
                break;
            case IDmPersistentValues.STATE_DL_PKG_COMPLETE:
                Log.d(TAG.CLIENT, "[syncDLstatus] pkg complete.");
                cancelVerifyingDialog();
                if (DmService.getInstance() != null) {
                    String[] updateTypes = DmService.getInstance().getUpdateTypes();
                    if (updateTypes != null) {
                        onDlPkgComplete(updateTypes);
                    }
                }
                break;
            case IDmPersistentValues.STATE_VERIFY_NO_STORAGE:
                Log.d(TAG.CLIENT, "[syncDLstatus] no enough storage when unzip delta files.");
                cancelVerifyingDialog();
                showDialog(DIALOG_NO_ENOUGH_SPACE);
                break;
            case IDmPersistentValues.STATE_VERIFY_FAIL:
                Log.d(TAG.CLIENT, "[syncDLstatus] verfify delta files fail.");
                cancelVerifyingDialog();
                onVerifyFail();
                break;
            case IDmPersistentValues.STATE_VERIFYING_PKG:
                Log.d(TAG.CLIENT, "[syncDLstatus] disable the UI button when verifying.");
                showVerifyingDialog();
                break;
            default:
                break;
            }
            mStatus = status;
        }

        public void syncDmSession(int status) {
            switch (status) {
            case IDmPersistentValues.STATE_DM_NIA_COMPLETE:
                Log.d(TAG.CLIENT,
                        (new StringBuilder("[syncDmSession] session complete. DL status = ")
                                .append(mStatus).append(", initor = ").append(DmService
                                .getInstance().getSessionInitor())).toString());
                if (mStatus == IDmPersistentValues.STATE_NOT_DOWNLOAD) {
                    onNoNewVersionDetected();
                }
                break;
            case IDmPersistentValues.STATE_DM_NIA_CANCLE:
                Log.w(TAG.CLIENT, "[syncDmSession] session aborted.");
                int lasterror = PersistentContext.getInstance(DmClient.this).getFumoErrorCode();
                if (lasterror == VdmException.VdmError.OK.val) {
                    Log.d(TAG.CLIENT, "[syncDmSession] NOT 'VDM_FUMO' error");
                    return;
                }
                if (lasterror == VdmException.VdmError.COMMS_FATAL.val
                        || lasterror == VdmException.VdmError.COMMS_NON_FATAL.val
                        || lasterror == VdmException.VdmError.COMMS_SOCKET_ERROR.val
                        || lasterror == VdmException.VdmError.COMMS_HTTP_ERROR.val
                        || lasterror == VdmException.VdmError.COMMS_SOCKET_TIMEOUT.val) {
                    onNetworkError();
                    Log.w(TAG.CLIENT, "[syncDmSession]Get network error message.");
                } else if ((mStatus != IDmPersistentValues.STATE_PAUSE_DOWNLOAD && mStatus != IDmPersistentValues.STATE_NOT_DOWNLOAD)
                        || lasterror != VdmException.VdmError.CANCEL.val) {
                    Log.i(TAG.CLIENT, "[syncDmSession]Get other error");
                    String errorMsg = DmClient.this.getString(R.string.error_message,
                            VdmException.VdmError.fromInt(lasterror).name());
                    onOtherError(errorMsg);
                }
                break;
            default:
                break;
            }
        }

    };

    public Handler mApnConnHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG.CLIENT, "DmClient->apnConnHandler->handleMessage()");
            if (mStatus != IDmPersistentValues.STATE_DM_DETECT_WAP) {
                Log.w(TAG.CLIENT, "apnConnHandler state is not STATE_DETECT_WAP, the status = "
                        + mStatus);
                return;
            }

            Log.i(TAG.CLIENT, "apnConnHandler message is " + msg.what);
            switch (msg.what) {
            case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
                Log.i(TAG.CLIENT, "apnConnHandler handleMessage message is connect sucesss");
                mStatus = IDmPersistentValues.STATE_DM_WAP_CONNECT_SUCCESS;
                if (mNetworkDetectProgressDialog != null) {
                    mNetworkDetectProgressDialog.cancel();
                    mNetworkDetectProgressDialog = null;
                }

                executeUpdate();
                break;
            case IDmPersistentValues.MSG_WAP_CONNECTION_TIMEOUT:
                Log.i(TAG.CLIENT, "apnConnHandler handleMessage message is connect timeout");
                mStatus = IDmPersistentValues.STATE_DM_WAP_CONNECT_TIMEOUT;
                if (mNetworkDetectProgressDialog != null) {
                    mNetworkDetectProgressDialog.cancel();
                    mNetworkDetectProgressDialog = null;
                }
                onNetworkError();
                break;
            default:
                break;
            }
        }
    };

    private void checkNetwork() {
        Assert.assertTrue("check network should only be used in DM WAP connection.",
                !Options.USE_DIRECT_INTERNET);
        Log.i(TAG.CLIENT, "checkNetwork begin");

        if (mNetworkDetectProgressDialog == null) {
            mNetworkDetectProgressDialog = new ProgressDialog(this);
            mNetworkDetectProgressDialog.setCancelable(false);
            mNetworkDetectProgressDialog.setIndeterminate(true);
            mNetworkDetectProgressDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mNetworkDetectProgressDialog.setMessage(getString(R.string.network_detect));
        }
        mNetworkDetectProgressDialog.show();

        Log.d(TAG.CLIENT, "checkNetwork begin check ");
        int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
        Log.d(TAG.CLIENT, "checkNetwork result is " + result);
        if (result == MTKPhone.NETWORK_AVAILABLE) {
            Log.i(TAG.CLIENT, "checkNetwork network is ok, continue");
            mNetworkDetectProgressDialog.cancel();
            executeUpdate();
        } else {
            Log.i(TAG.CLIENT, "checkNetwork network is not ok, request network establish");
            mStatus = IDmPersistentValues.STATE_DM_DETECT_WAP;
        }
    }

    private void showVerifyingDialog() {
        if (mVerifyingDialog == null) {
            mVerifyingDialog = new ProgressDialog(DmClient.this);
            mVerifyingDialog.setCancelable(false);
            mVerifyingDialog.setIndeterminate(true);
            mVerifyingDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mVerifyingDialog.setMessage(getString(R.string.verify_wait_dialog));
        }
        mVerifyingDialog.show();
    }

    private void cancelVerifyingDialog() {
        if (mVerifyingDialog != null) {
            mVerifyingDialog.dismiss();
            mVerifyingDialog = null;
        }
    }

    /**
     * check whether client is in foreground.
     * @return active status
     */
    public boolean isActive() {
        return mActive;
    }
}

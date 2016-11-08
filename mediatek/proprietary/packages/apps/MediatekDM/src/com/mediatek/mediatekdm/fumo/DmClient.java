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

package com.mediatek.mediatekdm.fumo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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

import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.conn.DmDataConnection;
import com.mediatek.mediatekdm.conn.DmDataConnection.DataConnectionListener;
import com.mediatek.mediatekdm.fumo.FumoComponent.FumoBinder;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor.Field;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.mediatekdm.util.DialogFactory;
import com.mediatek.mediatekdm.util.Utilities;

import java.io.IOException;

/**
 * UI for firmware update progress include server initiated and client initiated. This activity only
 * can be triggered by other components, e.g. DmService and DmEntry. The control logic of firmware
 * update is implemented in the activity, too. See See 5.1.5.4.3 in China Mobile Device Management
 * Service Terminal Specification 3.0.0
 */
public class DmClient extends Activity {

    /**
     * Override function of android.app.Activity, bind DM service when create.
     */
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG.CLIENT, "DmClient->onCreate()");
        super.onCreate(savedInstanceState);
        if (mProgressBarDlDD == null) {
            Log.d(TAG.CLIENT, "onCreate creat mProgressBarDlDD");
            mProgressBarDlDD = new ProgressDialog(this);
        }
        mHandler = new Handler();

        bindService();
        mApnConnHandler = new ApnConHandler(this);
        DmDataConnection.getInstance(this).registerListener(mDataConnectionListener);
    }

    private void bindService() {
        Log.d(TAG.COMMON, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(FumoComponent.BIND_FUMO);
        if (!bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new Error("Failed to bind to fumo service.");
        }
        Log.d(TAG.COMMON, "-bindService()");
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.w(TAG.CLIENT, "onServiceConnected DmClient register listener");
            mBinder = (FumoBinder) binder;
            mFumo = mBinder.getManager();
            mFumo.registerObserver(mFumoObserver);
            mFumo.registerDownloadObserver(mDownloadObserver);
            startToWork();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBinder = null;
            mFumo = null;
        }
    };

    protected void onNewIntent(Intent intent) {
        Log.d(TAG.CLIENT, "+onNewIntent(" + intent + ")");
        super.onNewIntent(intent);
        // Check whether we need to bind to service
        if (mBinder == null) {
            bindService();
        } else {
            startToWork();
        }
        Log.d(TAG.CLIENT, "-onNewIntent()");
    }

    // Tiny little method for common codes
    private void startToWork() {
        Log.d(TAG.CLIENT, "+startToWork()");
        Log.d(TAG.CLIENT, "Is there an operation in recovery? "
                + DmOperationManager.getInstance().isInRecovery());
        if (!DmConfig.getInstance().useMobileDataOnly()
                || DmOperationManager.getInstance().isInRecovery()) {
            Log.i(TAG.CLIENT, "Execute update");
            executeUpdate();
        } else {
            Log.i(TAG.CLIENT, "Check WAP first");
            checkNetwork();
        }
        Log.d(TAG.CLIENT, "-startToWork()");
    }

    /**
     * Override function of android.app.Activity, unbind service when destroy.
     */
    public void onDestroy() {
        Log.i(TAG.CLIENT, "DmClient->onDestroy()");

        DmDataConnection.getInstance(this).unregisterListener(mDataConnectionListener);
        cancelDialog();
        cancelResumingDialog();
        if (mFumo != null) {
            Log.i(TAG.CLIENT, "DmClient unregister listener");
            mFumo.unregisterObserver(mFumoObserver);
            mFumo.unregisterDownloadObserver(mDownloadObserver);
        }
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    /**
     * Show query new version UI and invoke query function of FUMO interface.
     */
    private void onQueryNewVersion() {
        Log.i(TAG.CLIENT, "DmClient==>onQueryNewVersion()");
        deleteFile(FumoComponent.FUMO_RESUME_FILE_NAME);
        PersistentContext.getInstance().deleteDeltaPackage();
        mFumo.setFumoState(DmFumoState.QUERY_NEW_VERSION, null, null);
        setContentView(R.layout.main);
        if (mProgressBarDlDD == null) {
            mProgressBarDlDD = new ProgressDialog(this);
        }
        mProgressBarDlDD.setCancelable(false);
        mProgressBarDlDD.setIndeterminate(true);
        mProgressBarDlDD.setProgressStyle(android.R.attr.progressBarStyleSmall);
        mProgressBarDlDD.setMessage(getString(R.string.wait));
        mProgressBarDlDD.show();
        mFumo.queryNewVersion();
    }

    /**
     * Show network error UI and will query again if click retry button.
     */
    private void onNetworkError() {
        Log.w(TAG.CLIENT, "DmClient==>onNetworkError()");
        cancelDialog();
        setContentView(R.layout.networkerror);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if (mRetryButton == null) {
            return;
        }

        mRetryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int state = mFumo.getFumoState();
                Log.d(TAG.CLIENT, "Network Error: Retry clicked with fumo state " + state);
                if (state == DmFumoState.WAP_CONNECTING || state == DmFumoState.WAP_CONNECT_TIMEOUT) {
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
    private void onOtherError(String errorMsg) {
        Log.w(TAG.CLIENT, "DmClient==>onOtherError(" + errorMsg + ")");
        cancelDialog();
        setContentView(R.layout.networkerror);

        mAuthentication = (TextView) findViewById(R.id.errorView);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if ((mAuthentication == null) || (mRetryButton == null)) {
            return;
        }

        mAuthentication.setText(errorMsg);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onQueryNewVersion();
            }
        });
    }

    /**
     * Show no new version UI.
     */
    private void onNoNewVersionDetected() {
        cancelDialog();
        setContentView(R.layout.nonewversion);
    }

    /**
     * Show new version detected UI and will start download if click download button.
     *
     * @param DownloadDescriptor
     *        dd - Download descriptor of delta package to download.
     */
    @SuppressWarnings("deprecation")
    private void onNewVersionDetected(DownloadDescriptor dd) {
        Log.i(TAG.CLIENT, "DmClient==>onNewVersionDetected(),dd=" + dd);
        if (dd == null) {
            Log.w(TAG.CLIENT, "onNewVersionDetected dd is null");
            return;
        }

        if (dd != null) {
            mDd = dd;
        }
        cancelDialog();

        setContentView(R.layout.releasenotes);
        mDlDescriptions = (TextView) findViewById(R.id.dscrpContent);
        mDlNewFeatureNotes = (TextView) findViewById(R.id.featureNotes);
        mDownloadButton = (Button) findViewById(R.id.buttonDl);
        if ((mDlDescriptions == null) || (mDlNewFeatureNotes == null) || (mDownloadButton == null)) {
            return;
        }

        String description = mDlDescriptions.getText() + " " + mDd.getField(Field.DD_VERSION)
                + " (" + mDd.size + " Bytes)";
        String releasenotes = mDd.getField(Field.DESCRIPTION);
        mDlDescriptions.setText(description);
        mDlNewFeatureNotes.setText(releasenotes);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // On download button clicked
                boolean hasEnoughSpace = Utilities.getAvailableInternalMemorySize() > mDd.size;
                if (!hasEnoughSpace) {
                    showDialog(DIALOG_NOENOUGHSPACE);
                    return;
                }

                if (DmConfig.getInstance().useMobileDataOnly()) {
                    // for CMCC case (CMWAP connection)
                    showDownloadingUI(OP_START);
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
                                } else {
                                    Log.d(TAG.CLIENT, "[WARNING] downloading in mobile network.");
                                    showDialog(DIALOG_GPRS_DOWNLOAD);
                                }
                            } else if (netType == ConnectivityManager.TYPE_WIFI) {
                                Log.i(TAG.CLIENT, "[INFO] downloading via wifi.");
                                showDownloadingUI(OP_START);
                            } else {
                                Log.e(TAG.CLIENT, "[ERROR] invalid network type: " + netType + "."
                                        + net.getTypeName());
                            }
                        } else {
                            Log.e(TAG.CLIENT,
                                    "[ERROR] no active network for downloading FOTA pack.");
                            showDialog(DIALOG_NETWORKERROR);
                        }
                    } else {
                        Log.e(TAG.CLIENT, "[ERROR] no active network for downloading FOTA pack.");
                        showDialog(DIALOG_NETWORKERROR);
                    }
                }

            }
        });
    }

    /**
     * Show downloading UI and will: 1. Pause download if click pause button; 2. Show confirm cancel
     * download dialog if click cancel button; 3. Downloading back ground if click back key.
     *
     * @param int opcode - operations to be made.
     */
    private void showDownloadingUI(int opcode) {
        Log.i(TAG.CLIENT, "+showDownloadingUI(" + opcode + ")");
        DmOperation operation = DmOperationManager.getInstance().current();
        mFumo.setFumoState(DmFumoState.DOWNLOADING, operation, null);

        mDd = PersistentContext.getInstance().getDownloadDescriptor();
        long downloadedSize = PersistentContext.getInstance().getDownloadedSize();

        Log.i(TAG.CLIENT, "download size is " + downloadedSize);
        Log.i(TAG.CLIENT, "total size is " + mDd.size);

        setContentView(R.layout.downloading);
        mProgressBarDlPkg = (ProgressBar) findViewById(R.id.progressbarDownload);
        mDlRatial = (TextView) findViewById(R.id.rate);
        mDlDescriptions = (TextView) findViewById(R.id.dscrpContentDl);
        mDlNewFeatureNotes = (TextView) findViewById(R.id.featureNotesDl);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mPausedButton = (Button) findViewById(R.id.buttonSuspend);

        mDlDescriptions.setText(mDlDescriptions.getText() + " " + " (" + mDd.size + " Bytes)");
        mDlNewFeatureNotes.setText(mDd.getField(Field.DESCRIPTION));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onDlPkgCancelled();
            }
        });

        mPausedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onDlPkgPaused();
            }
        });

        Log.i(TAG.CLIENT, "mDownloadedSize is " + downloadedSize + ", mDd size is " + mDd.size);
        updateDlProgress(downloadedSize, mDd.size);
        if (opcode == OP_RESUME) {
            showResumingDialog();
            mFumo.resumeDlPkg();
        } else if (opcode == OP_START) {
            mFumo.startDlPkg();
        }
    }

    private void showResumingDialog() {
        Log.v(TAG.CLIENT, "DmClient==>showResumingDialog");
        if (mResumingDialog == null) {
            mResumingDialog = new ProgressDialog(this);
        }
        mResumingDialog.setCancelable(false);
        mResumingDialog.setIndeterminate(true);
        mResumingDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
        mResumingDialog.setMessage(getString(R.string.resuming_download));
        mResumingDialog.show();
    }

    private void cancelResumingDialog() {
        Log.v(TAG.CLIENT, "+cancelResumingDialog()");
        if (mResumingDialog != null) {
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
    private void updateDlProgress(long dlSize, long totalSize) {
        int ratial = (int) (((float) dlSize / (float) totalSize) * 100);
        if (mProgressBarDlPkg != null) {
            mProgressBarDlPkg.setProgress(ratial);
        }
        CharSequence text = Integer.toString(ratial) + "%    " + Integer.toString((int) dlSize)
                + " Bytes / " + Integer.toString((int) totalSize) + " Bytes";
        if (mDlRatial != null) {
            mDlRatial.setText(text);
        }
    }

    /**
     * The response function of click pause button of downloading UI.
     */
    private void onDlPkgPaused() {
        mFumo.pauseDlPkg();
        mPendingAction = "Pause";
        mPausedButton.setEnabled(false);
        finish();
    }

    /**
     * The response function of click cancel button of downloading UI.
     */
    private void onDlPkgCancelled() {
        // pause the download first before show the cancel dialog
        Log.i(TAG.CLIENT, "onDlPkgCancelled set the session state is cancled");
        mCancelButton.setEnabled(false);
        mFumo.cancelDlPkg();
    }

    /**
     * Show download complete UI after download delta package finished.
     */
    private void onDownloadComplete() {
        Log.i(TAG.CLIENT, "DmClient==>onDownloadComplete()");
        mDd = null;
        setContentView(R.layout.updateenquire);
        onShowUpdateList(mFumo.getUpdateTypeStrings());
        mUpdateButton = (Button) findViewById(R.id.update);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFumo.firmwareUpdate();
                finish();
            }
        });
    }

    /**
     * Show update type list view.
     *
     * @param String
     *        [] updateTypes - string array which contains the text content of update types.
     */
    private void onShowUpdateList(String[] updateTypes) {
        Log.i(TAG.CLIENT, "DmClient==>onShowUpdateList():" + updateTypes);
        mFumo.setUpdateType(0);
        mSingleChoiceList = (ListView) findViewById(R.id.updatetypelist);
        mSingleChoiceList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, updateTypes));
        mSingleChoiceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mSingleChoiceList.setClickable(true);
        mSingleChoiceList.setItemsCanFocus(false);
        mSingleChoiceList.setItemChecked(0, true);
        mSingleChoiceList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSingleChoiceList.setItemChecked(position, true);
                mFumo.setUpdateType(position);
                Log.d(TAG.CLIENT, "onItemClick select is " + position);
            }
        });
    }

    @Override
    /**
     * Override function of com.android.Activity
     * @param id - The dialog type to create.
     */
    protected Dialog onCreateDialog(int id) {
        Log.i(TAG.CLIENT, "DmClient->onCreateDialog(" + id + ")");
        switch (id) {
            case DIALOG_NETWORKERROR:
                return DialogFactory.newAlert(this).setTitle(R.string.networkerror)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On informed the network error
                                finish();
                            }
                        }).create();

            case DIALOG_NONEWVERSION:
                return DialogFactory.newAlert(this).setTitle(R.string.nonewversion)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On informed the current version is
                                // the
                                // latest
                                finish();
                            }
                        }).create();

            case DIALOG_NOENOUGHSPACE:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.noenoughspace)
                        .setPositiveButton(R.string.appmanager,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // On user go to application manager to release space
                                        Intent intent = new Intent();
                                        intent.setAction("com.android.settings.ManageApplications");
                                        intent.putExtra("DmClient", true);
                                        sendBroadcast(intent);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On user abort to down load
                                finish();
                            }
                        }).create();

            case DIALOG_CANCELDOWNLOAD:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.cancel)
                        .setMessage(R.string.canceldownload)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On user confirm cancel download
                                mFumo.clearDlStateAndReport(0,
                                        MdmFumoUpdateResult.ResultCode.USER_CANCELED.val);
                                mFumo.clearFumoNotification();
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (!DmOperationManager.getInstance().isInRecovery()) {
                                    // On user abort cancel download
                                    showDownloadingUI(OP_RESUME);
                                }
                            }
                        }).create();
            case DIALOG_GPRS_DOWNLOAD:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.gprs_download_title)
                        .setMessage(R.string.gprs_download)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        showDownloadingUI(OP_START);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mFumo.cancelDlPkg();
                                finish();
                            }
                        }).create();

            case DIALOG_ROAMING_DOWNLOAD:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.gprs_download_title)
                        .setMessage(R.string.roaming_download)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        showDownloadingUI(OP_START);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mFumo.cancelDlPkg();
                                finish();
                            }
                        }).create();
            default:
                break;

        }
        return null;
    }

    private void executeUpdate() {
        Log.i(TAG.CLIENT, "DmClient==>executeUpdate()");
        if (mBinder == null) {
            Log.w(TAG.CLIENT, "executeUpdate service is not available");
            return;
        }

        int state = mFumo.getFumoState();
        Log.d(TAG.FUMO, "Current FUMO state: " + state);
        if (DmFumoState.isWAPState(state)) {
            state = mFumo.reloadFumoState();
        }
        Log.d(TAG.FUMO, "Reload FUMO state: " + state);

        switch (state) {
            case DmFumoState.DOWNLOAD_PAUSED:
                if (!DmOperationManager.getInstance().isInRecovery()) {
                    showDownloadingUI(OP_RESUME);
                }
                break;
            case DmFumoState.DOWNLOADING:
                // show UI, not call service's operation
                showDownloadingUI(OP_NONE);
                break;
            case DmFumoState.DOWNLOAD_COMPLETE:
                onDownloadComplete();
                break;
            case DmFumoState.NEW_VERSION_FOUND:
                mDd = PersistentContext.getInstance().getDownloadDescriptor();
                onNewVersionDetected(mDd);
                break;
            default:
                onQueryNewVersion();
        }
    }

    private synchronized void cancelDialog() {
        Log.d(TAG.CLIENT, "DmClient->cancleDialog()");
        if (mProgressBarDlDD != null) {
            mProgressBarDlDD.cancel();
            mProgressBarDlDD = null;
        }
    }

    private void checkNetwork() {
        Log.i(TAG.CLIENT, "+checkNetwork()");
        try {
            if (mNetworkDetectProgressDialog == null) {
                mNetworkDetectProgressDialog = new ProgressDialog(this);
            }
            mNetworkDetectProgressDialog.setCancelable(false);
            mNetworkDetectProgressDialog.setIndeterminate(true);
            mNetworkDetectProgressDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mNetworkDetectProgressDialog.setMessage(getString(R.string.network_detect));
            mNetworkDetectProgressDialog.show();

            Log.d(TAG.CLIENT, "checkNetwork begin check ");

            int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
            Log.d(TAG.CLIENT, "checkNetwork result is " + result);
            if (result == PlatformManager.APN_ALREADY_ACTIVE) {
                Log.i(TAG.CLIENT, "checkNetwork network is ok, continue");
                mNetworkDetectProgressDialog.cancel();
                executeUpdate();
            } else {
                mFumo.setFumoState(DmFumoState.WAP_CONNECTING, null, null);
                Log.i(TAG.CLIENT, "checkNetwork network is not ok, request network establish");
                setNetworkTimeoutAlarm();
            }
        } catch (IOException e) {
            Log.e(TAG.CLIENT, e.getMessage(), e);
        }
    }

    private void setNetworkTimeoutAlarm() {
        Log.d(TAG.CLIENT, "setAlarm alarm");
        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.NET_DETECT_TIMEOUT);
        sAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (sAlarmManager == null) {
            Log.w(TAG.CLIENT, "setAlarm sAlarmManager is null");
            return;
        }
        if (sAlarmManager != null) {
            sNetworkTimeoutIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            sAlarmManager.cancel(sNetworkTimeoutIntent);
            sAlarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + TIME_OUT_VALUE
                    * ONE_SECOND), sNetworkTimeoutIntent);
        }
    }

    private void cancelNetworkTimeoutAlarm() {
        if (sAlarmManager != null && sNetworkTimeoutIntent != null) {
            sAlarmManager.cancel(sNetworkTimeoutIntent);
            sAlarmManager = null;
            sNetworkTimeoutIntent = null;
        }
    }

    private Handler mApnConnHandler;

    static class ApnConHandler extends Handler {
        private DmClient mDmClient;

        ApnConHandler(DmClient dmClient) {
            mDmClient = dmClient;
        }

        public void handleMessage(Message msg) {
            Log.d(TAG.CLIENT, "DmClient->ApnConHandler->handleMessage(), client=" + mDmClient);
            if (mDmClient == null) {
                return;
            }
            ProgressDialog networkDetect = mDmClient.mNetworkDetectProgressDialog;
            int state = mDmClient.mFumo.getFumoState();
            if (state != DmFumoState.WAP_CONNECTING) {
                Log.w(TAG.CLIENT, "apnConnHandler state is not STATE_DETECT_WAP, the status = "
                        + state);
                return;
            }

            Log.i(TAG.CLIENT, "apnConnHandler message is " + msg.what);
            switch (msg.what) {
                case IServiceMessage.MSG_WAP_CONNECTION_SUCCESS:
                    Log.i(TAG.CLIENT, "apnConnHandler handleMessage message is connect sucesss");
                    // Ignore success message in time out state.
                    if (state != DmFumoState.WAP_CONNECT_TIMEOUT) {
                        mDmClient.mFumo.setFumoState(DmFumoState.WAP_CONNECT_SUCCESS, null, null);
                        if (networkDetect != null) {
                            networkDetect.cancel();
                            networkDetect = null;
                        }
                        mDmClient.cancelNetworkTimeoutAlarm();
                        mDmClient.executeUpdate();
                    }
                    break;
                case IServiceMessage.MSG_WAP_CONNECTION_TIMEOUT:
                    Log.i(TAG.CLIENT, "apnConnHandler handleMessage message is connect timeout");
                    mDmClient.mFumo.setFumoState(DmFumoState.WAP_CONNECT_TIMEOUT, null, null);
                    if (networkDetect != null) {
                        networkDetect.cancel();
                        networkDetect = null;
                    }
                    mDmClient.onNetworkError();
                    break;
                default:
                    break;
            }
        }
    };

    private static final int ONE_SECOND = 1000;
    private static final int TIME_OUT_VALUE = 30;
    private static AlarmManager sAlarmManager = null;
    private static PendingIntent sNetworkTimeoutIntent = null;
    private ProgressDialog mNetworkDetectProgressDialog = null;
    /******************************** DM APN end **********************************/

    private Button mDownloadButton;
    private Button mPausedButton;
    private Button mCancelButton;
    private Button mUpdateButton;
    private Button mRetryButton;
    private TextView mDlRatial;
    private TextView mDlDescriptions;
    private TextView mDlNewFeatureNotes;
    private TextView mAuthentication;
    private ListView mSingleChoiceList;
    private ProgressBar mProgressBarDlPkg;
    private ProgressDialog mProgressBarDlDD = null;
    private ProgressDialog mResumingDialog = null;
    private String mPendingAction = null;

    private FumoManager mFumo = null;
    private FumoBinder mBinder = null;
    private DownloadDescriptor mDd = null;
    private Handler mHandler = null;

    static final int DIALOG_NETWORKERROR = 0;
    static final int DIALOG_NONEWVERSION = 1;
    static final int DIALOG_CANCELDOWNLOAD = 2;
    static final int DIALOG_NOENOUGHSPACE = 3;
    static final int DIALOG_GPRS_DOWNLOAD = 4;
    static final int DIALOG_ROAMING_DOWNLOAD = 5;

    /**
     * operations to be made after entering downloading status.
     */
    static final int OP_NONE = 0;
    static final int OP_START = 1;
    static final int OP_RESUME = 2;

    @SuppressWarnings("deprecation")
    private IDmFumoStateObserver mFumoObserver = new IDmFumoStateObserver() {
        @Override
        public void notify(final int newState, final int previousState,
                final DmOperation operation, final Object extra) {
            Log.d(TAG.FUMO, "DmClient notified with state " + newState + " and operation "
                    + operation);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    String reason = null;
                    switch (newState) {
                        case DmFumoState.NEW_VERSION_FOUND:
                            if (mBinder != null) {
                                if (operation.getProperty(DmOperation.KEY.TYPE).equals(
                                        DmOperation.Type.TYPE_DL)
                                        && operation.getBooleanProperty(DmOperation.KEY.FUMO_TAG,
                                                false)) {
                                    if (!operation.getProperty(DmOperation.KEY.INITIATOR).equals(
                                            "Server")) {
                                        onNewVersionDetected((DownloadDescriptor) extra);
                                    }
                                }
                            }
                            break;

                        case DmFumoState.NO_NEW_VERSION_FOUND:
                            reason = (String) extra;
                            if (reason.equals("NO_FUMO_ACTION")) {
                                if (operation.getProperty(KEY.INITIATOR).equals("User")) {
                                    onNoNewVersionDetected();
                                }
                            } else if (reason.equals("USER_CANCELED")) {
                                // do nothing
                                Log.d(TAG.FUMO, "User canceled FUMO DM session.");
                            } else if (reason.equals("NETWORK_ERROR")) {
                                onNetworkError();
                            } else {
                                onOtherError("Fatal error");
                            }
                            break;

                        case DmFumoState.DOWNLOAD_PAUSED:
                            reason = (String) extra;
                            if (reason.equals("USER_CANCELED")) {
                                showDialog(DIALOG_CANCELDOWNLOAD);
                                mCancelButton.setEnabled(false);
                            } else if (reason.equals("USER_PAUSED")) {
                                if (mPendingAction != null && mPendingAction.equals("Paused")) {
                                    mPendingAction = null;
                                    finish();
                                }
                            }
                            break;

                        case DmFumoState.DOWNLOAD_CANCELED:
                            // TODO do we need to update UI?
                            break;

                        case DmFumoState.DOWNLOAD_FAILED:
                            reason = (String) extra;
                            if (reason.equals("NETWORK_ERROR")) {
                                onNetworkError();
                            } else {
                                onOtherError("Fatal error");
                            }
                            break;

                        case DmFumoState.DOWNLOADING:
                            cancelResumingDialog();
                            break;

                        case DmFumoState.DOWNLOAD_COMPLETE:
                            onDownloadComplete();
                            break;

                        case DmFumoState.UPDATE_COMPLETE:
                            finish();
                            break;

                        case DmFumoState.DOWNLOAD_STARTED:
                            break;

                        default:
                            break;
                    }
                }
            });
        }
    };

    private IDmFumoDownloadProgressObserver mDownloadObserver = new IDmFumoDownloadProgressObserver() {
        @Override
        public void updateProgress(final long current, final long total) {
            Log.d(TAG.CLIENT, "Current FUMO state is " + mFumo.getFumoState());
            if (mFumo.getFumoState() == DmFumoState.DOWNLOADING) {
                // We can only update UI in main thread
                mBinder.getService().getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        updateDlProgress(current, total);
                    }
                });
            }
        }
    };

    private DataConnectionListener mDataConnectionListener = new DataConnectionListener() {
        public void notifyStatus(int status) {
            mApnConnHandler.sendMessage(mApnConnHandler.obtainMessage(status));
        }
    };
}

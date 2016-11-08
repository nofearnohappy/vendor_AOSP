/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.scomo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.mediatek.dm.conn.DmDataConnection;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.util.DialogFactory;


public class DmScomoDownloadDetailActivity extends Activity implements OnDmScomoUpdateListener {
    private static final String CLASS_TAG = TAG.SCOMO + "DownloadDetail";

    private Button mPauseButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mDescription;
    private TextView mNewFeature;
    private boolean mIsPaused;
    // check network
    private ProgressDialog mNetworkDetectDialog;
    private static DmScomoDownloadDetailActivity sInstance;
    private static int sNetworkStatus = IDmPersistentValues.STATE_DM_DETECT_WAP;
    private static final int ONESEVOND = 1000;
    private static final int TIME_OUT_VALUE = 30;

    private static final int KB = 1024;
    private boolean mCancelButtonClicked = false;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        sInstance = this;
        DmService.getInstance().registerScomoListener(this);
        checkNetwork();
    }

    private void setUI() {
        this.setContentView(R.layout.downloading);
        mPauseButton = (Button) findViewById(R.id.buttonSuspend);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbarDownload);
        mProgressText = (TextView) findViewById(R.id.rate);
        mDescription = (TextView) findViewById(R.id.dscrpContentDl);
        mNewFeature = (TextView) findViewById(R.id.featureNotesDl);

        mPauseButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                mPauseButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                Log.i(CLASS_TAG, "DownloadDetailActivity -> onClick mPauseButton begin");
                DmScomoState scomoState = DmScomoState.getInstance(DmScomoDownloadDetailActivity.this);
                if (DmScomoState.PAUSED == scomoState.mState) {
                    DmService.getInstance().resumeDlScomoPkg();
                } else {
                    DmService.getInstance().pauseDlScomoPkg();
                }
                Log.i(CLASS_TAG, "DownloadDetailActivity -> onClick mPauseButton end");
            }

        });

        mCancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mCancelButtonClicked) {
                    Log.i(CLASS_TAG, "Cancel button has been clicked once, click too fast!");
                    return;
                }
                mCancelButtonClicked = true;
                Log.i(CLASS_TAG, "DownloadDetailActivity -> onClick cancel button begin");
                mCancelButton.setEnabled(false);
                mPauseButton.setEnabled(false);
                DmService.getInstance().pauseDlScomoPkg();
                DialogFactory.newAlert(DmScomoDownloadDetailActivity.this).setCancelable(false)
                        .setTitle(R.string.scomo_activity_title).setIcon(R.drawable.ic_dialog_info)
                        .setMessage(R.string.scomo_cancel_download_message)
                        .setNegativeButton(R.string.scomo_discard, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DmService.getInstance().cancelDlScomoPkg();
                                DmScomoDownloadDetailActivity.this.finish();
                                mCancelButtonClicked = false;
                            }
                        }).setPositiveButton(R.string.scomo_continue, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mPauseButton.setEnabled(false);
                                mCancelButton.setEnabled(false);
                                DmService.getInstance().resumeDlScomoPkg();
                                mCancelButtonClicked = false;
                            }
                        }).show();
                Log.i(CLASS_TAG, "DownloadDetailActivity -> onClick cancel button end");
            }
        });

        onScomoUpdated();
    }

    public void onResume() {
        Log.i(CLASS_TAG, "DmScomoDownloadDetailActivity onResume");
        mIsPaused = false;
        super.onResume();
        onScomoUpdated();
    }

    public void onScomoUpdated() {
        if (mIsPaused || IDmPersistentValues.STATE_DM_WAP_CONNECT_SUCCESS != sNetworkStatus) {
            return;
        }

        Log.i(CLASS_TAG, "DownloadDetailActivity -> onScomoUpdated begin..");

        DmScomoState scomoState = DmScomoState.getInstance(DmScomoDownloadDetailActivity.this);
        if (scomoState == null || !scomoState.mVerbose) {
            return;
        }
        // set description
        mDescription.setText(scomoState.getDescription());

        String version = scomoState.getVersion();
        if (TextUtils.isEmpty(version)) {
            version = getString(R.string.unknown);
        }

        mNewFeature.setText(getString(R.string.featureNotes, version, String.valueOf(scomoState.mTotalSize / KB) + "KB"));

        // set progress
        String progress = new StringBuilder().append(scomoState.mCurrentSize / KB).append("KB / ")
                .append(scomoState.mTotalSize / KB).append("KB").toString();
        mProgressText.setText(progress);
        mProgressBar.setMax(scomoState.mTotalSize);
        mProgressBar.setProgress(scomoState.mCurrentSize);
        //

        int state = scomoState.mState;
        Log.i(CLASS_TAG, "Scomo state is " + state);
        switch (state) {
        case DmScomoState.DOWNLOADING_STARTED:
        case DmScomoState.DOWNLOADING:
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
            break;
        case DmScomoState.RESUMED:
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(false);
            mCancelButton.setEnabled(false);
            break;
        case DmScomoState.PAUSED:
            mPauseButton.setText(R.string.resume);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
            break;
        default:
            finish();
            break;
        }
        Log.i(CLASS_TAG, "DownloadDetailActivity -> onScomoUpdated done");
    }

    protected void onPause() {
        Log.i(CLASS_TAG, "DmScomoDownloadDetailActivity -> onPause");
        mIsPaused = true;
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        DmService.getInstance().removeScomoListener(this);
    }

    private void checkNetwork() {
        Log.i(CLASS_TAG, "DownloadDetailActivity -> checkNetwork begin");
        if (mNetworkDetectDialog == null) {
            mNetworkDetectDialog = new ProgressDialog(this);
            mNetworkDetectDialog.setCancelable(false);
            mNetworkDetectDialog.setIndeterminate(true);
            mNetworkDetectDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mNetworkDetectDialog.setMessage(getString(R.string.network_detect));
            mNetworkDetectDialog.show();

        }
        Log.d(CLASS_TAG, "checkNetwork begin check ");
        int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
        Log.d(CLASS_TAG, "checkNetwork result is " + result);
        if (MTKPhone.NETWORK_AVAILABLE == result) {
            Log.i(CLASS_TAG, "checkNetwork network is ok, continue");
            sNetworkStatus = IDmPersistentValues.STATE_DM_WAP_CONNECT_SUCCESS;
            mNetworkDetectDialog.cancel();
            setUI();
        } else {
            Log.i(CLASS_TAG, "checkNetwork network is not ok, request network establish");
            sNetworkStatus = IDmPersistentValues.STATE_DM_DETECT_WAP;
        }
    }

    public static DmScomoDownloadDetailActivity getInstance() {
        return sInstance;
    }

    public Handler mApnConnHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(CLASS_TAG, "DownloadDetailActivity->apnConnHandler->handleMessage()");
            if (sNetworkStatus != IDmPersistentValues.STATE_DM_DETECT_WAP) {
                Log.w(CLASS_TAG, "apnConnHandler state is not STATE_DETECT_WAP, the status = " + sNetworkStatus);
                return;
            }

            Log.i(CLASS_TAG, "apnConnHandler message is " + msg.what);
            switch (msg.what) {
            case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
                Log.i(CLASS_TAG, "apnConnHandler handleMessage message is connect sucesss");
                sNetworkStatus = IDmPersistentValues.STATE_DM_WAP_CONNECT_SUCCESS;
                if (mNetworkDetectDialog != null) {
                    mNetworkDetectDialog.cancel();
                    mNetworkDetectDialog = null;
                }

                setUI();
                break;
            case IDmPersistentValues.MSG_WAP_CONNECTION_TIMEOUT:
                Log.i(CLASS_TAG, "apnConnHandler handleMessage message is connect timeout");
                sNetworkStatus = IDmPersistentValues.STATE_DM_WAP_CONNECT_TIMEOUT;
                if (mNetworkDetectDialog != null) {
                    mNetworkDetectDialog.cancel();
                    mNetworkDetectDialog = null;
                }
                onNetworkError();
                break;
            default:
                break;
            }
        }
    };

    public void onNetworkError() {
        Log.w(CLASS_TAG, "DownloadDetailActivity==>onNetworkError()");
        setContentView(R.layout.networkerror);
        Button mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if (mRetryButton == null) {
            return;
        }
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkNetwork();
            }
        });
    }
}

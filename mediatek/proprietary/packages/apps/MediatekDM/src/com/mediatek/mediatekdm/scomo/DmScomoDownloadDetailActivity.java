package com.mediatek.mediatekdm.scomo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.scomo.ScomoComponent.ScomoBinder;
import com.mediatek.mediatekdm.util.DialogFactory;

public class DmScomoDownloadDetailActivity extends Activity implements IDmScomoStateObserver,
        IDmScomoDownloadProgressObserver {
    private Button mPauseButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mDescription;
    private TextView mNewFeature;
    private boolean mIsPaused = false;
    private ScomoBinder mBinder = null;
    private ScomoManager mScomo = null;
    private static final String TAG = DmConst.TAG.SCOMO + "/DmScomoDownloadDetailActivity";

    private void bindService() {
        Log.d(TAG, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(ScomoComponent.BIND_SCOMO);
        if (!bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new Error("Failed to bind to fumo service.");
        }
        Log.d(TAG, "-bindService()");
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.w(TAG, "onServiceConnected DmClient register listener");
            mBinder = (ScomoBinder) binder;
            mScomo = mBinder.getManager();
            mScomo.registerObserver(DmScomoDownloadDetailActivity.this);
            mScomo.registerDownloadObserver(DmScomoDownloadDetailActivity.this);
            updateUI(mScomo.getScomoState().state, null);
        }

        public void onServiceDisconnected(ComponentName className) {
            mScomo.unregisterDownloadObserver(DmScomoDownloadDetailActivity.this);
            mScomo.unregisterObserver(DmScomoDownloadDetailActivity.this);
            mScomo = null;
            mBinder = null;
        }
    };

    public void onCreate(Bundle icicle) {
        Log.d(TAG, "+onCreate()");
        super.onCreate(icicle);
        bindService();
        setContentView(R.layout.downloading);
        setTitle(R.string.scomo_activity_title);
        mPauseButton = (Button) findViewById(R.id.buttonSuspend);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbarDownload);
        mProgressText = (TextView) findViewById(R.id.rate);
        mDescription = (TextView) findViewById(R.id.dscrpContentDl);
        mNewFeature = (TextView) findViewById(R.id.featureNotesDl);

        mPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPauseButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                int state = mScomo.getScomoState().state;
                Log.d(TAG, "Pause button clicked with state " + state);
                if (state == DmScomoState.DOWNLOAD_PAUSED) {
                    mScomo.resumeDlPkg();
                } else {
                    mScomo.pauseDlPkg();
                }
            }
        });

        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPauseButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                Log.d(TAG, "Cancel button clicked with state " + mScomo.getScomoState().state);
                mScomo.pauseDlPkg();
                DialogFactory
                        .newAlert(DmScomoDownloadDetailActivity.this)
                        .setTitle(R.string.scomo_activity_title)
                        .setIcon(R.drawable.ic_dialog_info)
                        .setMessage(R.string.scomo_cancel_download_message)
                        .setNegativeButton(R.string.scomo_discard,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.d(TAG,
                                                "Discard button clicked with state "
                                                        + mScomo.getScomoState().state);
                                        if (mScomo.getScomoState().state == DmScomoState.DOWNLOAD_PAUSED) {
                                            mScomo.clearDlStateAndReport(0);
                                            DmScomoDownloadDetailActivity.this.finish();
                                        }
                                    }
                                })
                        .setPositiveButton(R.string.scomo_continue,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.d(TAG, "Abort cancel button clicked with state "
                                                + mScomo.getScomoState().state);
                                        if (mScomo.getScomoState().state == DmScomoState.DOWNLOAD_PAUSED) {
                                            mPauseButton.setEnabled(false);
                                            mCancelButton.setEnabled(false);
                                            if (mScomo != null) {
                                                mScomo.resumeDlPkg();
                                            } else {
                                                Log.d(TAG, " mService is null");
                                            }
                                        }
                                    }
                                }).show();
            }
        });

    }

    public void onResume() {
        Log.i(TAG, " onResume");
        mIsPaused = false;
        super.onResume();
        if (mScomo != null) {
            updateUI(mScomo.getScomoState().state, null);
        }
    }

    protected void onPause() {
        Log.i(TAG, " onPause");
        mIsPaused = true;
        super.onPause();
    }

    public void onDestroy() {
        Log.d(TAG, " onDestroy");
        super.onDestroy();
        if (mScomo != null) {
            mScomo.unregisterObserver(this);
        }
        unbindService(mServiceConnection);
        mScomo = null;
    }

    @Override
    public void updateProgress(long current, long total) {
        Log.d(TAG, "+updateProgress(" + current + ", " + total + ")");
        if (mIsPaused || mScomo == null) {
            Log.w(TAG, "Not connected to scomo. Ignore.");
            return;
        }
        if (mScomo.getScomoState().state == DmScomoState.DOWNLOADING) {
            updateUI(DmScomoState.DOWNLOADING, null);
        }
        Log.d(TAG, "-updateProgress()");
    }

    private void updateUI(int state, Object extra) {
        DmScomoState scomoState = mScomo.getScomoState();
        // set description
        mDescription.setText(scomoState.getDescription());
        String version = scomoState.getVersion();
        mNewFeature.setText(getString(R.string.featureNotes, version,
                String.valueOf(scomoState.totalSize / 1024) + "KB"));
        // set progress
        String progress = scomoState.currentSize / 1024 + "KB / " + scomoState.totalSize / 1024
                + "KB";
        mProgressText.setText(progress);
        mProgressBar.setMax((int) scomoState.totalSize);
        mProgressBar.setProgress((int) scomoState.currentSize);
        //
        Log.i(TAG, "state is " + state);
        if (state == DmScomoState.DOWNLOADING) {
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else if (state == DmScomoState.DOWNLOADING_STARTED) {
            mPauseButton.setText(R.string.pause);
            // We cannot pause in this state because we cannot resume later if we cancel the DL
            // session now.
            mPauseButton.setEnabled(false);
            mCancelButton.setEnabled(true);
        } else if (state == DmScomoState.NEW_DP_FOUND) {
            mPauseButton.setText(R.string.pause);
            // DD is ready, we can pause now
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else if (state == DmScomoState.DOWNLOAD_PAUSED) {
            mPauseButton.setText(R.string.resume);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else {
            Log.d(TAG, "Finish DmScomoDownloadDetailActivity with state " + state);
            DmScomoDownloadDetailActivity.this.finish();
        }
    }

    @Override
    public void notify(int state, int previousState, DmOperation operation, Object extra) {
        if (mIsPaused || mScomo == null) {
            Log.d(TAG, "onScomoUPdated: mIsPaused=" + mIsPaused + ", mScomo=" + mScomo);
            return;
        }
        updateUI(state, extra);
    }
}

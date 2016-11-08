package com.mediatek.hotknotbeam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.mediatek.hotknotbeam.HotKnotBeamConstants.HotKnotFileType;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;
import com.mediatek.hotknotbeam.HotKnotFileServer.HotKnotFileServerCb;

/**
 * Common UI activity for HotKnotBeam receiving.
 *
 * @hide
 */
public class HotKnotBeamRxActivity extends Activity implements View.OnClickListener,
            Handler.Callback {
    private static final String TAG = "HotKnotBeamRxActivity";

    private final Object mItemLock = new Object();
    private Context mContext;
    private Handler mHandler;
    private DownloadInfo mInfo;
    private Resources mResource;
    private AlertDialog mUiFailedDialog;

    private HotKnotFileServer mFileServer;
    private ProgressBar mProgressBar;
    private TextView    mFileStatus;
    private int         mInfoId;
    private HotKnotBeamRxActivity mUiActivity;

    private int MAX_PROGRESS_VALUE = 100;

    private static int HOTKNOT_FAILED_DIALOG = 0;

    static private final int MSG_POLLING = 0;
    static private final int MSG_UPDATE_UI = 1;
    static private final int MSG_FINISH = 2;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.i(TAG, "onCreate");

        setContentView(R.layout.file_rx_activity);
        mContext = this.getBaseContext();
        mUiActivity = this;

        Intent intent = getIntent();
        mInfoId = intent.getIntExtra(DownloadInfo.EXTRA_ITEM_ID, 0);

        mHandler = new Handler(this);

        mResource = getResources();

        ((Button) findViewById(R.id.cancel_button)).setOnClickListener(this);

        mFileStatus = (TextView) findViewById(R.id.share_info);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(MAX_PROGRESS_VALUE);

        mFileServer = HotKnotFileServer.getInstance();

        if (mFileServer != null) {
            mInfo = mFileServer.getDownloadItem(mInfoId);
            mFileServer.setHotKnotFileServerUiCb(new HotKnotFileServerCb() {
                public void onSetExternalPath(boolean isExternal) {

                }
                public void onHotKnotFileServerFinish(int status) {

                }
                public void onUpdateNotification() {
                    synchronized (mItemLock) {
                        Log.d(TAG, "onUpdateNotification");
                        DownloadInfo info = mFileServer.getDownloadItem(mInfoId);

                        if (info != null) {
                            mInfo = info;
                            Log.e(TAG, "Get new info:" + mInfo);
                        } else {
                            mHandler.sendEmptyMessage(MSG_UPDATE_UI);
                            mHandler.sendEmptyMessage(MSG_FINISH);
                        }
                    }
                }
                public void onStartUiActivity(int id) {

                }
            });
        }

        updateProgressBar();
    }

    private String getDisplayTitle() {
        String title;

        synchronized (mItemLock) {
            Log.i(TAG, "File name:" + mInfo.getFileName());
            HotKnotFileType fileType = MimeUtilsEx.getFileType(mInfo.getFileName());
            String device = mInfo.getDeviceName();

            if (mInfo.isGroup()) {
                if (fileType == HotKnotFileType.IMAGE) {
                    title = mResource.getString(R.string.rx_group_ui_image_status,
                            mInfo.mOrder, mInfo.mCount, device);
                } else if (fileType == HotKnotFileType.VIDEO) {
                    title = mResource.getString(R.string.rx_group_ui_video_status,
                            mInfo.mOrder, mInfo.mCount, device);
                } else if (fileType == HotKnotFileType.MUSIC) {
                    title = mResource.getString(R.string.rx_group_ui_music_status,
                            mInfo.mOrder, mInfo.mCount, device);
                } else {
                    title = mResource.getString(R.string.rx_group_ui_status,
                            mInfo.mOrder, mInfo.mCount, device);
                }
            } else {
                if (fileType == HotKnotFileType.IMAGE) {
                    title = mResource.getString(R.string.rx_ui_image_status, device);
                } else if (fileType == HotKnotFileType.VIDEO) {
                    title = mResource.getString(R.string.rx_ui_video_status, device);
                } else if (fileType == HotKnotFileType.MUSIC) {
                    title = mResource.getString(R.string.rx_ui_music_status, device);
                } else {
                    title = mResource.getString(R.string.rx_ui_status, device);
                }
            }
        }

        return title;
    }

    private boolean updateProgressBar() {
        boolean retValue = false;

        if (this.isFinishing()) {
            Log.e(TAG, "Activity is finished");
            return false;
        }

        synchronized (mItemLock) {
            if (mInfo == null) {
                Log.e(TAG, "mInfo is null:" + mInfoId);
                finish();
                return false;
            } else {
                mFileStatus.setText(getDisplayTitle());
            }

            Log.d(TAG, "Info:" + mInfo);

            if (mInfo.mState == State.RUNNING || mInfo.mState == State.CONNECTING) {
                long currentBytes = mInfo.mCurrentBytes;
                long totalBytes = mInfo.mTotalBytes;
                int progress = 0;
                if (totalBytes != 0) {
                    progress = (int) ((currentBytes * MAX_PROGRESS_VALUE / totalBytes));
                } else{
                    progress = 100;
                }
                Log.d(TAG, "File progress:" + progress);
                mProgressBar.setProgress(progress);
                mHandler.sendEmptyMessageDelayed(MSG_POLLING,
                        HotKnotBeamConstants.FILE_UI_PROGRESS_POLL);
                return true;
            } else if (mInfo.mState == State.COMPLETE) {
                if (mInfo.getResult()) {
                    mProgressBar.setProgress(MAX_PROGRESS_VALUE);
                } else {
                    showUiDialog(HOTKNOT_FAILED_DIALOG);
                    return true;
                }

                if (!mInfo.isLastOne()) {
                    Log.i(TAG, "Wait for next activity");
                    return true;
                }

                HotKnotFileServer.openUiActivity(mInfo, mContext);
                Log.i(TAG, "Finish UI activity");
            } else {
                Log.e(TAG, "wrong state:" + mInfo.mState);
            }
        }

        return retValue;
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiFailedDialog = null;
        updateProgressBar();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        mFileServer.setHotKnotFileServerUiCb(null);

        if (mHandler.hasMessages(MSG_POLLING)) {
            mHandler.removeMessages(MSG_POLLING);
        }

        dismissDialog();
        Log.i(TAG, "onPause");
        finish();
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case MSG_UPDATE_UI:
            case MSG_POLLING:
                if (!updateProgressBar()) {
                    finish();
                }
                break;
            case MSG_FINISH:
                Log.d(TAG, "Finish UI activity");

                if (mUiFailedDialog == null) {
                    finish();
                }
                break;
            default:
                break;
        }
        return false;
    }

    public void onClick(View v) {
        int buttonId = v.getId();

        switch (buttonId) {
            case R.id.cancel_button:
                synchronized (mItemLock) {
                if (mInfo != null && mInfo.mState != State.COMPLETE) {
                    Log.i(TAG, "Cancel download procedure:" + mInfo.mId);
                    mFileServer.cancel(mInfo.mId);
                } else {
                    if (mInfo != null) {
                        Log.e(TAG, "Can't Cancel:" + mInfo.mState);
                    }
                }
                }
                break;
            default:
                break;
        }
    }

    private void showUiDialog(int id) {
        Log.i(TAG, "showUiDialog");

        if (id == HOTKNOT_FAILED_DIALOG && mUiFailedDialog == null) {
            mUiFailedDialog = new AlertDialog.Builder(HotKnotBeamRxActivity.this,
                                AlertDialog.THEME_HOLO_DARK).create();
            mUiFailedDialog.setTitle(mResource.getString(R.string.rx_ui_failed_status));

            synchronized (mItemLock) {
                mUiFailedDialog.setMessage(mInfo.getFailureText());
            }

            mUiFailedDialog.setIcon(R.drawable.ic_settings_hotknot);
            mUiFailedDialog.setButton(mResource.getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mUiActivity.finish();
                }
            });

            if (!mUiFailedDialog.isShowing()) {
                Log.i(TAG, "showUiDialog show");

                mUiFailedDialog.show();
            }
        }
    }

    private void dismissDialog() {
        dismissDialogDetail(mUiFailedDialog);
    }

    private void dismissDialogDetail(AlertDialog dialog) {
        if (null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    private void showUiApp() {
        synchronized (mItemLock) {
            String mimeType = mInfo.getMimeType();
            String appIntent = mInfo.getAppIntent();
            boolean isCheck = mInfo.isMimeTypeCheck();
            Uri uri = mInfo.getUri();

            final Intent intent = new Intent(HotKnotBeamService.HOTKNOT_DL_COMPLETE,
                        null, mContext, HotKnotBeamReceiver.class);
            intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_INTENT, appIntent);
            intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_URI, uri);
            intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_MIMETYPE, mimeType);
            intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_ISCHECK, isCheck);
            mContext.sendBroadcast(intent);
        }

    }
}
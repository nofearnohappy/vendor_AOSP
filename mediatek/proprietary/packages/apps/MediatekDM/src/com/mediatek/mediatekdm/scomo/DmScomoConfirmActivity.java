package com.mediatek.mediatekdm.scomo;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.scomo.ScomoComponent.ScomoBinder;
import com.mediatek.mediatekdm.util.DialogFactory;

public class DmScomoConfirmActivity extends Activity implements IDmScomoStateObserver {
    private static final String TAG = DmConst.TAG.SCOMO + "/DmScomoConfirmActivity";

    private static final int DIALOG_DOWNLOAD_FAILED = 0;
    private static final int DIALOG_NEW_DP_FOUND = 1;
    private static final int DIALOG_INSTALL_FAILED = 2;
    private static final int DIALOG_INSTALL_OK = 3;
    private static final int DIALOG_GENERIC_ERROR = 4;
    private static final int DIALOG_NETWORK_ERROR = 5;
    private static final int DIALOG_CONFIRM_INSTALL = 6;

    private int mDialogId = -1;
    private ScomoBinder mBinder = null;
    private ScomoManager mScomo = null;

    private void bindService() {
        Log.d(TAG, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(ScomoComponent.BIND_SCOMO);
        if (!bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new Error("Failed to bind to fumo service.");
        }
        Log.d(TAG, "-bindService()");
    };

    @SuppressWarnings("deprecation")
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.w(TAG, "onServiceConnected DmClient register listener");
            mBinder = (ScomoBinder) binder;
            mScomo = mBinder.getManager();
            mScomo.registerObserver(DmScomoConfirmActivity.this);
            if (mDialogId != -1) {
                showDialog(mDialogId);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mScomo.unregisterObserver(DmScomoConfirmActivity.this);
            mScomo = null;
            mBinder = null;
        }
    };

    @SuppressWarnings("deprecation")
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent intent = getIntent();
        int action = intent.getIntExtra("action", -1);
        Log.e(TAG, "action is " + action);
        switch (action) {
            case DmScomoState.NEW_DP_FOUND:
                mDialogId = DIALOG_NEW_DP_FOUND;
                break;
            case DmScomoState.DOWNLOAD_FAILED:
                mDialogId = DIALOG_DOWNLOAD_FAILED;
                break;
            case DmScomoState.CONFIRM_INSTALL:
                mDialogId = DIALOG_CONFIRM_INSTALL;
                break;
            case DmScomoState.IDLE:
                if (intent.hasExtra("reason")) {
                    String reason = intent.getStringExtra("reason");
                    Log.d(TAG, "IDLE state with reason: " + reason);
                    if (reason.equals("DM_NETWORK_ERROR")) {
                        mDialogId = DIALOG_NETWORK_ERROR;
                    } else if (reason.equals("DM_FAILED")) {
                        mDialogId = DIALOG_GENERIC_ERROR;
                    } else if (reason.equals("INSTALL_FAILED")) {
                        mDialogId = DIALOG_INSTALL_FAILED;
                    } else if (reason.equals("INSTALL_OK")) {
                        mDialogId = DIALOG_INSTALL_OK;
                    }
                } else {
                    Log.d(TAG, "IDLE state with no reason");
                }
                break;
            default:
                mDialogId = -1;
                break;
        }

        if (mDialogId != -1) {
            showDialog(mDialogId);
            bindService();
        } else {
            finish();
        }
    }

    protected void onDestroy() {
        if (mScomo != null) {
            mScomo.unregisterObserver(this);
        }
        unbindService(mServiceConnection);
        mScomo = null;
        mBinder = null;
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Log.d(TAG, "onCreateDialog with id " + id);
        Dialog dialog = null;
        switch (id) {
            case DIALOG_NEW_DP_FOUND:
                dialog = onConfirmDownload();
                break;
            case DIALOG_INSTALL_FAILED:
                dialog = onInstallFailed();
                break;
            case DIALOG_DOWNLOAD_FAILED:
                dialog = onDownloadFailed();
                break;
            case DIALOG_NETWORK_ERROR:
                dialog = onNetworkError();
                break;
            case DIALOG_INSTALL_OK:
                dialog = onInstallOk();
                break;
            case DIALOG_GENERIC_ERROR:
                dialog = onGenericError();
                break;
            case DIALOG_CONFIRM_INSTALL:
                dialog = onConfirmInstall();
                break;
            default:
                Log.e(TAG, "Invalid dialog id :" + id);
                break;
        }
        return dialog;
    }

    private Dialog onNetworkError() {
        Log.d(TAG, "onNetworkError()");
        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.networkerror)
                .setNeutralButton(R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG, "Neutral button clicked, DM failed due to network error!");
                        mScomo.setScomoState(DmScomoState.IDLE, null, null);
                        finish();
                    }
                }).create();
    }

    private Dialog onGenericError() {
        Log.v(TAG, "onGenericError()");
        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.unknown_error)
                .setNeutralButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.v(TAG, "Neutral button clicked, DM failed due to generic error!");
                        mScomo.setScomoState(DmScomoState.IDLE, null, null);
                        finish();
                    }
                }).create();
    }

    private Dialog onDownloadFailed() {
        Log.v(TAG, "onDownloadFailed()");
        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.download_failed)
                .setNeutralButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG, "Neutral button clicked, download_failed!");
                        mScomo.setScomoState(DmScomoState.IDLE, null, null);
                        finish();
                    }
                }).create();
    }

    private Dialog onInstallOk() {
        Log.v(TAG, "onInstallOk");
        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.install_complete)
                .setNeutralButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.v(TAG, "Neutral button clicked, install OK!");
                        mScomo.setScomoState(DmScomoState.IDLE, null, null);
                        finish();
                    }
                }).create();
    }

    private Dialog onInstallFailed() {
        Log.v(TAG, "onInstallFailed");

        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.install_failed)
                .setNeutralButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.v(TAG, "Neutral button clicked, install failed!");
                        mScomo.setScomoState(DmScomoState.IDLE, null, null);
                        finish();
                    }
                }).create();
    }

    private Dialog onConfirmDownload() {
        Log.v(TAG, "onConfirmDownload");

        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(R.string.confirm_download_msg)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG, "positive button clicked, start to download");
                        mScomo.startDlPkg();
                        finish();
                    }
                }).setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_NEGATIVE:
                                Log.v(TAG, "negative button clicked, cancel download");
                                mScomo.cancelDlPkg();
                                break;
                            default:
                                break;
                        }
                        finish();
                    }
                }).create();
    }

    private Dialog onConfirmInstall() {
        Log.v(TAG, "onConfirmInstall");

        CharSequence message = "";
        String preVersion = mScomo.getPreVersion();
        if (preVersion == null) {
            message = getString(R.string.confirm_update_scomo);
        } else {
            message = getString(R.string.scomo_replace_exist);
        }

        return DialogFactory.newAlert(this).setCancelable(false).setTitle(R.string.software_update)
                .setMessage(message).setPositiveButton(R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG, "positive button clicked, start to install");
                        mScomo.startInstall();
                        finish();
                    }
                }).setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_NEGATIVE:
                                Log.v(TAG, "negative button clicked, cancel install");
                                mScomo.cancelInstall();
                                break;
                            default:
                                break;
                        }
                        finish();
                    }
                }).create();
    }

    @Override
    public void notify(int state, int previousState, DmOperation operation, Object extra) {
        Log.d(TAG, "+notify");
        if (mScomo == null) {
            Log.d(TAG, "DmScomoConfirmActivity.notify(): mScomo = " + mScomo);
            return;
        }
        Log.d(TAG, "notify with state " + state);
        if (state == DmScomoState.IDLE) {
            if (extra != null) {
                String reason = (String) extra;
                Log.d(TAG, "State idle, reason is " + reason);
            } else {
                Log.d(TAG, "State idle with no reason, clear dialog.");
                finish();
            }
        } else {
            Log.d(TAG, "Not idle, do nothing here.");
        }
        Log.d(TAG, "-notify");
    }
}

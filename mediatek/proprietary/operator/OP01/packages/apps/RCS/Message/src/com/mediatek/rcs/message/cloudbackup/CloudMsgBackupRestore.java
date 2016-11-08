package com.mediatek.rcs.message.cloudbackup;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.message.cloudbackup.BackupService.BackupBinder;
import com.mediatek.rcs.message.cloudbackup.MsgBackupAPI.ErrorCode;
import com.mediatek.rcs.message.cloudbackup.MsgBackupAPI.Result;
import com.mediatek.rcs.message.cloudbackup.MsgBackupAPI.ResultListener;
import com.mediatek.rcs.message.cloudbackup.RestoreService.RestoreBinder;
import com.mediatek.rcs.message.cloudbackup.store.MessageStore;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.ui.RcsSettingsActivity;
import com.mediatek.rcs.message.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class CloudMsgBackupRestore {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "CloudMsgBackupRestore";
    private Context mContext;
    private Context mUiContext;
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mCancelDlg;
    private MsgBackupAPI mMsgBackupAPI;
    private boolean mIsCancel = false;// this constant will be assgin value,
                                      // when first press backup.
    private Action mAction;

    public CloudMsgBackupRestore(Context context) {
        mContext = context;
        mUiContext = context;
        initHandler();
    }

    public CloudMsgBackupRestore(Context context, Context pluginContext) {
        mUiContext = context;
        mContext = pluginContext;
        initHandler();
    }

    private ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mUiContext);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelMessage(mHandler
                    .obtainMessage(CloudBrUtils.MessageID.PRESS_BACK));
        }
        return mProgressDialog;
    }

    private ProgressDialog createProgressCancelDlg(String content) {
        if (mCancelDlg == null) {
            mCancelDlg = new ProgressDialog(mUiContext);
            mCancelDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mCancelDlg.setCancelable(false);
        }
        mCancelDlg.setMessage(content);
        return mCancelDlg;
    }

    protected void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.show();
    }

    /**
     * is cloud backup restore feature is avaliable.
     * @return
     */
    public boolean isCloudBrFeatureAvalible() {
        boolean result = true;
        String resultStr = null;
        RCSServiceManager rcsServiceManager = RCSServiceManager.getInstance();
        boolean isServiceConfigured = rcsServiceManager.isServiceConfigured();
        if (!isServiceConfigured) {
                Log.d(CLASS_TAG, "RCS service is not configuration");
                resultStr = mContext.getString(R.string.rcs_not_configu);
                result = false;
        }
        if (!result) {
            Toast.makeText(mUiContext, resultStr, Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public void startBackup() {
        Log.d(CLASS_TAG, "startBackup() mAction = Action.BACKUP");
        mAction = Action.BACKUP;
        createProgressDlg();
        mProgressDialog.setMessage(mContext.getString(R.string.in_backup_wait));
        mProgressDialog.show();
        Intent intent = new Intent(mUiContext, BackupService.class);
        mUiContext.bindService(intent, mBackupServiceCon, Service.BIND_AUTO_CREATE);
    }

    public void firstEntryMmsRestore() {
        Log.d(CLASS_TAG, "firstEntryMmsRestore");
        Builder builder = new AlertDialog.Builder(mUiContext);
        builder.setTitle(mContext.getString(R.string.restore_data));
        builder.setMessage(mContext.getString(R.string.restore_from_network));
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                    Log.d(CLASS_TAG, "firstEntryMmsRestore begine restore");
                    startRestore();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    public Builder createNoticeRestoreDialog(final boolean isShowWilanConDialog) {
        Builder builder = new AlertDialog.Builder(mUiContext);
        builder.setTitle(mContext.getString(R.string.restore_data));
        builder.setMessage(mContext.getString(R.string.is_continue_restore));
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (isShowWilanConDialog) {
                    Log.d(CLASS_TAG, "isShowWilanConDialog true show dialog");
                    createContinueNoticeDialog().show();
                } else {
                    startRestore();
               }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder;
    }

    private Builder createContinueNoticeDialog() {
        Log.d(CLASS_TAG, "createContinueNoticeDialog");
        Builder builder = new AlertDialog.Builder(mUiContext);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(mContext.getString(R.string.restore_nowilan));
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                startRestore();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder;
    }

    public Builder createQueryBackupDialog() {
        Log.d(CLASS_TAG, "createQueryBackupDialog");
        Builder builder = new AlertDialog.Builder(mUiContext);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(mContext.getString(R.string.is_continue_backup));
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                startBackup();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder;
    }

    public void startRestore() {
        Log.d(CLASS_TAG, "startRestore() mAction = Action.RESTORE");
        mAction = Action.RESTORE;
        createProgressDlg();
        mProgressDialog.setMessage(mContext.getString(R.string.in_restore_wait));
        mProgressDialog.show();
        Log.d(CLASS_TAG, "mProgressDialog show");

        String messageFolderPath = FileUtils.ModulePath.BACKUP_DATA_FOLDER
                + MessageStore.RemoteFolderName.MSG_BACKUP;
        String favoriteFolderPath = FileUtils.ModulePath.BACKUP_DATA_FOLDER
                + MessageStore.RemoteFolderName.MSG_FAVORITE;
        boolean createFolderResult = FileUtils.createFolder(messageFolderPath);
        if (!createFolderResult) {
            Log.e(CLASS_TAG, "startRestore messageFolderPath fail, return");
            onRestoreDownloadError(mContext.getString(R.string.restore_msg_fail));
            return;
        }
        Log.d(CLASS_TAG, "startRestore messageFolderPath = " + messageFolderPath);
        createFolderResult = FileUtils.createFolder(favoriteFolderPath);
        if (!createFolderResult) {
            Log.e(CLASS_TAG, "startRestore create favorite folder fail, return");
            onRestoreDownloadError(mContext.getString(R.string.restore_msg_fail));
            return;
        }
        Log.d(CLASS_TAG, "startRestore favoriteFolderPath = " + favoriteFolderPath);

        if (mMsgBackupAPI == null) {
            mMsgBackupAPI = MsgBackupAPI.getInstance(mUiContext);
        }
        mMsgBackupAPI.restore(messageFolderPath, favoriteFolderPath,
                new ResultListener() {
                    @Override
                    public void onResult(Result result, ErrorCode code, List<String> list) {
                        Log.d(CLASS_TAG, "onResult(" + result + ", " + code + ")");
                        mMsgBackupAPI = null;
                        if (result == Result.OK) {
                            if (FileUtils.isEmptyFolder(new File(FileUtils.ModulePath
                                    .BACKUP_DATA_FOLDER))) {
                                Log.d(CLASS_TAG, "no message stored in network,end restore");
                                onRestoreDownloadError(mContext.getString(R.string.restore_msg_empty));
                            } else {
                                Intent intent = new Intent(mUiContext, RestoreService.class);
                                mUiContext.bindService(intent, mRestoreServiceCon,
                                        Service.BIND_AUTO_CREATE);
                            }

                        } else {
                            Log.e(CLASS_TAG, "download msg from network fail! stop restore now");
                            onRestoreDownloadError(mContext.getString(R.string.restore_msg_fail));
                        }
                    }
                });
    }

    BackupBinder mBackupBinder;
    RestoreBinder mRestoreBinder;
    private ServiceConnection mBackupServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mBackupBinder = (BackupBinder) service;
            if (mBackupBinder != null) {
                if (mHandler != null) {
                    Log.i(CLASS_TAG, "onServiceConnected mBackupService.setHandler");
                    mBackupBinder.setHandler(mHandler);
                    mBackupBinder.setContext(mUiContext);
                    startBackupCloudMsg(FileUtils.ModulePath.BACKUP_DATA_FOLDER);
                }
            } else {
                Log.e(CLASS_TAG, "onServiceConnected iBinder = null");
                Message msg = new Message();
                msg.what = CloudBrUtils.MessageID.BACKUP_END;
                Bundle bundle = new Bundle();
                bundle.putInt(CloudBrUtils.BACKUP_RESULT, CloudBrUtils.ResultCode.OTHER_EXCEPTION);
                msg.setData(bundle);
                if (mHandler != null) {
                    mHandler.sendMessage(msg);
                }
            }
            Log.i(CLASS_TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBackupBinder = null;
            Log.i(CLASS_TAG, "onServiceDisconnected");
        }
    };

    private ServiceConnection mRestoreServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mRestoreBinder = (RestoreBinder) service;
            if (mRestoreBinder != null) {
                if (mHandler != null) {
                    Log.i(CLASS_TAG, "onServiceConnected RestoreService.setHandler");
                    mRestoreBinder.setHandler(mHandler);
                    mRestoreBinder.setContext(mUiContext);
                    startRestoreCloudMsg(FileUtils.ModulePath.BACKUP_DATA_FOLDER);
                }
            } else {
                Log.e(CLASS_TAG, "onServiceConnected iBinder = null");
                Message msg = new Message();
                msg.what = CloudBrUtils.MessageID.RESTORE_END;
                Bundle bundle = new Bundle();
                bundle.putInt(CloudBrUtils.RESTORE_RESULT, CloudBrUtils.ResultCode.OTHER_EXCEPTION);
                msg.setData(bundle);
                if (mHandler != null) {
                    mHandler.sendMessage(msg);
                }
            }
            Log.i(CLASS_TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBackupBinder = null;
            Log.i(CLASS_TAG, "onServiceDisconnected");
        }
    };

    private void startBackupCloudMsg(String backupDataFolder) {
        if (mBackupBinder == null) {
            Log.e(CLASS_TAG, "startBackupCloudMsg: mBackupBinder = null, return");
            String text = mContext.getString(R.string.backup_msg_fail);
            Toast.makeText(mUiContext, text, Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(CLASS_TAG, "startBackupCloudMsg backupDataFolder = " + backupDataFolder);
        mBackupBinder.setBackupFolder(backupDataFolder);
        int backupResult = mBackupBinder.startBackup();
    }

    private void startRestoreCloudMsg(String downloadFileFolder) {
        if (mRestoreBinder == null) {
            Log.e(CLASS_TAG, "startRestoreCloudMsg: mRestoreBinder = null, return");
            String text = mContext.getString(R.string.restore_msg_fail);
            Toast.makeText(mUiContext, text, Toast.LENGTH_LONG).show();
            return;
        }
        mRestoreBinder.setRestoreDataFolder(downloadFileFolder);
        int backupResult = mRestoreBinder.startRestore();
    }

    private void createCancelDialog() {
        Log.d(CLASS_TAG, "createCancelDialog");
        Builder builder = new AlertDialog.Builder(mUiContext)
                .setTitle(android.R.string.dialog_alert_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface arg0, final int arg1) {
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        if (mBackupStringResult != null) {
                            Log.d(CLASS_TAG, "backup have end");
                            Toast.makeText(mUiContext, "canceled", Toast.LENGTH_SHORT);
                            mIsCancel = false;
                            return;
                        } else if (mRestoreStringResult != null) {
                            Log.d(CLASS_TAG, "restore have end");
                            Toast.makeText(mUiContext, "canceled", Toast.LENGTH_SHORT);
                            mIsCancel = false;
                            return;
                        }

                        if (mAction == Action.BACKUP) {
                            createProgressCancelDlg(mContext.getString(
                                    R.string.cancel_backup_wait)).show();
                            if (mBackupBinder != null) {
                                mBackupBinder.cancelBackup();
                            }
                        }

                        if (mAction == Action.RESTORE) {
                            createProgressCancelDlg(mContext.getString(
                                    R.string.cancel_restore_wait)).show();
                            if (mRestoreBinder != null) {
                                mRestoreBinder.cancelRestore();
                            }
                        }

                        if (mMsgBackupAPI != null) {
                            mMsgBackupAPI.cancel();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface arg0, final int arg1) {
                        if (mIsCancel && mBackupStringResult != null) {
                            Log.d(CLASS_TAG,
                                    "backup end and the result have saved in mBackupStringResult, "
                                            + "so only need show result");
                            mIsCancel = false;
                            Toast.makeText(mUiContext, mBackupStringResult, Toast.LENGTH_LONG);
                            mBackupStringResult = null;
                        } else if (mIsCancel && mRestoreStringResult != null) {
                            Log.d(CLASS_TAG,
                                    "restore end and the result have saved in mRestoreStringResult,"
                                            + "so only need show result");
                            mIsCancel = false;
                            Toast.makeText(mUiContext, mRestoreStringResult, Toast.LENGTH_LONG);
                            mRestoreStringResult = null;
                        } else {
                            mIsCancel = false;
                            if (mProgressDialog != null) {
                                mProgressDialog.show();
                            }
                        }
                    }
                }).setCancelable(false);

                if (mAction == Action.BACKUP) {
                    builder.setMessage(mContext.getString(
                            R.string.is_cancel_backup));
                }
                if (mAction == Action.RESTORE) {
                    builder.setMessage(mContext.getString(
                            R.string.is_cancel_restore));
                }
        builder.show();
    }

    String mRestoreStringResult = null;
    String mBackupStringResult = null;

    protected final void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                Bundle data = msg.getData();
                switch (msg.what) {
                case CloudBrUtils.MessageID.BACKUP_END:
                    Log.d(CLASS_TAG, "CloudBrUtils.MessageID.BACKUP_END local data backup end");
                    if (mBackupBinder != null) {
                        mBackupBinder.setHandler(null);
                        mUiContext.unbindService(mBackupServiceCon);
                        mBackupBinder = null;
                    }

                    int backupResult = data.getInt(CloudBrUtils.BACKUP_RESULT);
                    Log.d(CLASS_TAG, "backup local data Result = " + backupResult);

                    String msgPath = data.getString(CloudBrUtils.MESSAGE_PATH);
                    String favPath = data.getString(CloudBrUtils.FAVORITE_PATH);
                    Log.d(CLASS_TAG, "msgPath = " + msgPath);
                    Log.d(CLASS_TAG, "favPath = " + favPath);
                    File msgFolder = null;
                    if (msgPath != null) {
                        msgFolder = new File(msgPath);
                    }
                    File favFolder = null;
                    if (favPath != null) {
                        favFolder = new File(favPath);
                    }

                    if (backupResult == CloudBrUtils.ResultCode.OK) {
                        Log.d(CLASS_TAG, "data backup success");
                        boolean isMsgFolderEmpty = FileUtils.isEmptyFolder(msgFolder);
                        boolean isFavFolderEmpty = FileUtils.isEmptyFolder(favFolder);
                        if (isMsgFolderEmpty && isFavFolderEmpty) {
                            Log.d(CLASS_TAG, "no valid message to backup, backup end");
                            if (msgFolder != null && msgFolder.exists()) {
                                FileUtils.deleteFileOrFolder(msgFolder);
                            }
                            if (favFolder != null && favFolder.exists()) {
                                FileUtils.deleteFileOrFolder(favFolder);
                            }
                            onBackupLocalMsgError(CloudBrUtils.ResultCode.BACKUP_FOLDER_EMPTY,
                                    mContext.getString(R.string.backup_msg_empty));
                        } else {
                            Log.d(CLASS_TAG, "data backup success, begin upload to service");
                            if (mMsgBackupAPI == null) {
                                mMsgBackupAPI = MsgBackupAPI.getInstance(mUiContext);
                            }
                            mMsgBackupAPI.backup(msgPath, favPath,
                                    new ResultListener() {

                                    @Override
                                        public void onResult(Result result, ErrorCode code,
                                                List<String> list) {
                                            Log.d(CLASS_TAG, "onResult ErrorCode = " + code);
                                            mMsgBackupAPI = null;
                                            FileUtils.deleteFileOrFolder(
                                                    new File(FileUtils.ModulePath.BACKUP_DATA_FOLDER));
                                            onGetBackupUploadResult(result);
                                        }
                                    });
                        }
                    } else {
                        Log.d(CLASS_TAG, "data backup fail, stop backup now");
                        onBackupLocalMsgError(backupResult,
                                mContext.getString(R.string.backup_msg_fail));
                    }
                    break;

                case CloudBrUtils.MessageID.RESTORE_END:
                    Log.d(CLASS_TAG, "CloudBrUtils.MessageID.RESTORE_END");
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }

                    int restoreResult = data.getInt(CloudBrUtils.RESTORE_RESULT);
                    Log.d(CLASS_TAG, "restoreResult = " + restoreResult);
                    if (restoreResult == CloudBrUtils.ResultCode.OK) {
                        mRestoreStringResult = mContext.getString(R.string.restore_msg_success);
                    } else if (restoreResult == CloudBrUtils.ResultCode.SERVICE_CANCELED) {
                        mRestoreStringResult = "restore is canceled";
                    } else {
                        mRestoreStringResult = mContext.getString(R.string.restore_msg_fail);
                    }

                    if (!mIsCancel) {
                        Toast.makeText(mUiContext, mRestoreStringResult, Toast.LENGTH_LONG).show();
                    }
                    mRestoreStringResult = null;

                    if (mRestoreBinder != null) {
                        mRestoreBinder.setHandler(null);
                        mUiContext.unbindService(mRestoreServiceCon);
                        mRestoreBinder = null;
                    }
                    if (mCancelDlg != null && mCancelDlg.isShowing()) {
                        mCancelDlg.dismiss();
                    }
                    break;

                case CloudBrUtils.MessageID.PRESS_BACK:
                    Log.d(CLASS_TAG, "CloudBrUtils.MessageID.PRESS_BACK");
                    mIsCancel = true;
                    createCancelDialog();
                    break;

                default:
                    break;
                }
            }
        };
    }

    private void onBackupLocalMsgError(int backupResult, String content) {
        Log.d(CLASS_TAG, "local backup error onBackupLocalError");

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mCancelDlg != null && mCancelDlg.isShowing()) {
            mCancelDlg.dismiss();
        }

        if (backupResult != CloudBrUtils.ResultCode.OK
                && backupResult != CloudBrUtils.ResultCode.SERVICE_CANCELED) {
            mBackupStringResult = content;
            Toast.makeText(mUiContext, mBackupStringResult, Toast.LENGTH_SHORT).show();
        }
        mBackupStringResult = null;
    }

    private void onGetBackupUploadResult(Result result) {
        Log.d(CLASS_TAG, "onGetBackupUploadResult");
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mCancelDlg != null && mCancelDlg.isShowing()) {
            mCancelDlg.dismiss();
        }

        if (result != Result.OK) {
            mBackupStringResult = mContext.getString(R.string.backup_msg_fail);
        } else {
            mBackupStringResult = mContext.getString(R.string.backup_msg_success);
        }
        if (!mIsCancel) {
            Toast.makeText(mUiContext, mBackupStringResult, Toast.LENGTH_SHORT).show();
        }
        mBackupStringResult = null;
    }

    private void onRestoreDownloadError(String content) {
        Log.d(CLASS_TAG, "onRestoreDownloadError");
        FileUtils.deleteFileOrFolder(new File(FileUtils.ModulePath.BACKUP_DATA_FOLDER));
        mRestoreStringResult = content;
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mCancelDlg != null && mCancelDlg.isShowing()) {
            mCancelDlg.dismiss();
        }
        if (!mIsCancel) {
            Toast.makeText(mUiContext, mRestoreStringResult, Toast.LENGTH_SHORT).show();
        }
        mRestoreStringResult = null;
    }

    public void destroy() {
        Log.i(CLASS_TAG, "onDestroy");
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (mBackupBinder != null) {
            mBackupBinder.setHandler(null);
            mUiContext.unbindService(mBackupServiceCon);
        }

        if (mRestoreBinder != null) {
            mRestoreBinder.setHandler(null);
            mUiContext.unbindService(mRestoreServiceCon);
        }
    }

    public void init() {
        mIsCancel = false;
    }

    public void createRestoreDialog(boolean isShowWilanConDialog) {
        createNoticeRestoreDialog(isShowWilanConDialog).show();
    }

    private enum Action {BACKUP, RESTORE}
}

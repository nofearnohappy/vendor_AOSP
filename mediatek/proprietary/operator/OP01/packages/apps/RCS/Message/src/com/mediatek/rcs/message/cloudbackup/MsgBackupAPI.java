package com.mediatek.rcs.message.cloudbackup;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

import com.cmcc.sso.sdk.auth.AuthnConstants;
import com.cmcc.sso.sdk.auth.AuthnHelper;
import com.cmcc.sso.sdk.auth.TokenListener;
import com.cmcc.sso.sdk.util.SsoSdkConstants;

import com.mediatek.gba.GbaManager;
import com.mediatek.gba.NafSessionKey;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.message.cloudbackup.store.MessageStore;
import com.mediatek.rcs.message.cloudbackup.store.MessageStore.CmdResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.MessagingException;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener;
import com.mediatek.rcs.message.cloudbackup.utils.TempDirectory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;


/**
 * API of message backup/restore from cloud.
 */
public class MsgBackupAPI {
    private static final String TAG = CloudBrUtils.MODULE_TAG + "MsgBackupAPI";
    /**
     * message backup folder on cloud.
     */
    private static final String MSG_FOLDER = "msgBackup";
    /**
     * favorite backup folder on cloud.
     */
    private static final String FAVOR_FOLDER = "msgFavorite";

    private static MsgBackupAPI sInstance;
    private Context mContext;
    private String mPassword;
    private String mUser;
    private Account mAccount;
    private EngineHandler mHandler;
    private Thread mWorker = null;

    private MessageStore mStore = null;
    private ResultListener mListener;
    private boolean mBusy = false;
    private boolean mCancel = false;
    private Operation mOperation;
    private String mLocalMsgPath;
    private String mLocalFavorPath;
    private String mRemotePath;

    private static final int MSG_OK = 0;
    /**
     * arg1 is ordinal of ErrorCode.
     */
    private static final int MSG_ERROR = -1;
    /**
     * get account successfully.
     */
    private static final int MSG_GET_ACCOUNT = 2;

    /**
     * enumeration for operations.
     *
     */
    private enum Operation {
        BACKUP, RESTORE, LIST
    }

    private MsgBackupAPI(Context context) {
        mContext = context;
        mHandler = new EngineHandler(this);
        TempDirectory.setTempDirectory(context);
    }

    /**
     * get API instance.
     * @param context context.
     * @return instance.
     */
    public static MsgBackupAPI getInstance(Context context) {
        synchronized (MsgBackupAPI.class) {
            if (sInstance == null) {
                if (context == null) {
                    Log.e(TAG, "context cannot be null for first getInstance!");
                    return null;
                }

                sInstance = new MsgBackupAPI(context);
            } else {
                Log.d(TAG, "getInstance() just update context");
                if (context != null) {
                    sInstance.mContext = context;
                    TempDirectory.setTempDirectory(context);
                } else {
                    Log.e(TAG, "getInstance() context is null!");
                }
            }
        }

        return sInstance;
    }

    /**
     * enumeration for result.
     *
     */
    public enum Result {
        OK, ERROR
    }

    /**
     * Enumeration for error code with Result.ERROR.
     * BUSY: previous operation is not complete.
     * GETACCOUNT: get account failed.
     * INVALID_OPERATION: invalid operation.
     * OPERATION_FAIL: execute operation fail.
     * NO_OPERATION_TO_CANCEL: there's no operation being done while calling cancel.
     */
    public enum ErrorCode {
        NONE,
        BUSY,
        GETACCOUNT,
        INVALID_OPERATION,
        OPERATION_FAIL,
        NO_OPERATION_TO_CANCEL,
        CANCELED,
        OTHER
    }

    /**
     * result listener.
     *
     */
    public interface ResultListener {
        /**
         * @param result operation result.
         * @param code error code.
         * @param list list result which is valid when operation is Operation.LIST.
         */
        void onResult(Result result, ErrorCode code, List<String> list);
    }

    private void notifyResult(Result result, ErrorCode code, List<String> list) {
        if (mListener != null) {
            mListener.onResult(result, code, list);
        }
        mBusy = false;
        mWorker = null;
    }

    private boolean doOperation() {
        mBusy = true;
        mCancel = false;
        mWorker = null;
        return getAccount() != null;
    }

    /**
     * Backup message from local to cloud. User must prepare the backup files in
     * folder @localPath and @localFavorPath to operator's requirements.
     *
     * @param localMsgPath message path to backup.
     * @param localFavorPath favorite path to backup.
     * @param listener result listener.
     */
    public synchronized void backup(String localMsgPath, String localFavorPath, ResultListener listener) {
        Log.d(TAG, "backup() begin");
        if (mBusy) {
            Log.e(TAG, "engine is busy!");
            if (listener != null) {
                listener.onResult(Result.ERROR, ErrorCode.BUSY, null);
            }
            return;
        }

        mOperation = Operation.BACKUP;
        mListener = listener;
        mLocalMsgPath = localMsgPath;
        mLocalFavorPath = localFavorPath;
        boolean result = doOperation();
        if (!result) {
            Log.e(TAG, "backup()doOperation fail!");
            notifyResult(Result.ERROR, ErrorCode.OPERATION_FAIL, null);
        }
    }

    /**
     * Restore message from cloud to local.
     *
     * @param localMsgPath message path to backup.
     * @param localFavorPath favorite path to backup.
     * @param listener result listener.
     */
    public synchronized void restore(String localMsgPath,
            String localFavorPath, ResultListener listener) {
        Log.d(TAG, "restore begin");
        if (mBusy) {
            Log.e(TAG, "engine is busy!");
            if (listener != null) {
                listener.onResult(Result.ERROR, ErrorCode.BUSY, null);
            }
            return;
        }

        mOperation = Operation.RESTORE;
        mListener = listener;
        mLocalMsgPath = localMsgPath;
        mLocalFavorPath = localFavorPath;
        boolean result = doOperation();
        if (!result) {
            Log.e(TAG, "restore()doOperation fail!");
            notifyResult(Result.ERROR, ErrorCode.OPERATION_FAIL, null);
        }
    }

    /**
     * List contents in remote path on cloud. User must pick one from the list
     * to restore.
     *
     * @param remotePath remote path to be listed.
     * @param listener result listener.
     */
    public synchronized void list(String remotePath, ResultListener listener) {
        if (mBusy) {
           Log.e(TAG, "engine is busy!");
            if (listener != null) {
                listener.onResult(Result.ERROR, ErrorCode.BUSY, null);
            }
            return;
        }
        mOperation = Operation.LIST;
        mListener = listener;
        mRemotePath = remotePath;

        boolean result = doOperation();
        if (!result) {
            Log.e(TAG, "list()doOperation fail!");
            notifyResult(Result.ERROR, ErrorCode.OPERATION_FAIL, null);
        }

        return;
    }

    /**
     * Cancel Operation currently doing.
     *
     */
    public synchronized void cancel() {
        Log.d(TAG, "cancel");
        if (!mBusy) {
            Log.d(TAG, "No operation to cancel.");
            return;
        }

        mCancel = true;
    }

    private boolean open() {
        try {
            mStore = new MessageStore(mContext, mUser, mPassword);
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean close(boolean forceStop) {
        if (mStore != null) {
            try {
                if (!forceStop) {
                    mStore.close();
                    mStore.logout();
                }
                mStore.closeConnection();
            } catch (MessagingException e) {
                Log.e(TAG, "close with error!");
                e.printStackTrace();
            } finally {
                mStore = null;
            }
        }

        return true;
    }


    /**
     * Handler to process message from worker thread.
     *
     */
    private static class EngineHandler extends Handler {
        private WeakReference<MsgBackupAPI> mEngine;

        public EngineHandler(MsgBackupAPI engine) {
            mEngine = new WeakReference<MsgBackupAPI>(engine);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage " + msg);
            MsgBackupAPI engine = mEngine.get();
            if (engine == null) {
                Log.e(TAG, "mEngine is null");
                return;
            }

            switch (msg.what) {
            case MSG_OK:
                engine.notifyResult(Result.OK, ErrorCode.NONE, (List<String>) msg.obj);
                break;

            case MSG_ERROR:
                engine.notifyResult(Result.ERROR, ErrorCode.values()[msg.arg1], (List<String>) msg.obj);
                break;

            case MSG_GET_ACCOUNT:
                engine.mWorker = new Thread() {
                    @Override
                    public void run() {
                        MsgBackupAPI engine = mEngine.get();
                        if (engine == null) {
                            Log.e(TAG, "mEngine is null");
                            return;
                        }
                        engine.mUser = engine.mAccount.getUser();
                        engine.mPassword = engine.mAccount.getPassword();
                        if (engine.mUser == null || engine.mPassword == null) {
                            Log.e(TAG, "get account error");
                            engine.mHandler.obtainMessage(MSG_ERROR,
                                    ErrorCode.GETACCOUNT.ordinal(), 0)
                                    .sendToTarget();
                            return;
                        }

                        if (!engine.open()) {
                            Log.e(TAG, "open failed!");
                            if (engine.mHandler != null) {
                                engine.mHandler.obtainMessage(MSG_ERROR, ErrorCode.OTHER.ordinal(),
                                        0).sendToTarget();
                            }
                            return;
                        }

                        ErrorCode result = ErrorCode.OTHER;
                        List<String> list = null;
                        if (engine.mOperation == Operation.BACKUP) {
                            result = engine.upload(engine.mLocalMsgPath,
                                    MsgBackupAPI.MSG_FOLDER);
                            if (result == ErrorCode.NONE) {
                                result = engine.upload(engine.mLocalFavorPath,
                                        MsgBackupAPI.FAVOR_FOLDER);
                            }
                        } else if (engine.mOperation == Operation.RESTORE) {
                            result = engine.download(engine.mLocalMsgPath,
                                    MsgBackupAPI.MSG_FOLDER);
                            if (result == ErrorCode.NONE) {
                                result = engine.download(engine.mLocalFavorPath,
                                        MsgBackupAPI.FAVOR_FOLDER);
                            }
                        } else if (engine.mOperation == Operation.LIST) {
                            list = engine.doList();
                            if (list != null) {
                                result = ErrorCode.NONE;
                            }
                        } else {
                            Log.e(TAG, "Unknown operation " + engine.mOperation);
                            if (engine.mHandler != null) {
                                engine.mHandler.obtainMessage(MSG_ERROR,
                                        ErrorCode.INVALID_OPERATION.ordinal(), 0).sendToTarget();
                            }
                            return;
                        }
                        engine.close(result != ErrorCode.NONE);
                        if (engine.mHandler != null) {
                            if (result == ErrorCode.NONE) {
                                engine.mHandler.obtainMessage(MSG_OK, list).sendToTarget();
                            } else {
                                engine.mHandler.obtainMessage(MSG_ERROR,
                                        result.ordinal(), 0).sendToTarget();
                            }
                        }
                    }
                };
                engine.mWorker.start();
                break;

            default:
                super.handleMessage(msg);
                break;
            }
        }
    }

    /**
     * Account.
     *
     */
    interface Account {
        boolean init();
        String getUser();
        String getPassword();
    }

    /**
     * Account using GBA.
     *
     */
    private class GbaAccount implements Account {
        private NafSessionKey mNafSessionKey;

        public boolean init() {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(MSG_GET_ACCOUNT);
            }

            return true;
        }

        private void produceKey() {
            if (mNafSessionKey == null) {
                GbaManager gbaManager = GbaManager
                        .getDefaultGbaManager(mContext);
                String nafFqdn = Config.getHost() + ":"
                        + String.valueOf(Config.getPort());
                mNafSessionKey = gbaManager.runGbaAuthentication(nafFqdn,
                        gbaManager.getNafSecureProtocolId(true), false);
            }
        }

        public String getUser() {
            produceKey();
            if (mNafSessionKey != null) {
                return mNafSessionKey.getBtid();
            }

            return null;
        }

        public String getPassword() {
            produceKey();
            if (mNafSessionKey != null) {
                byte[] key = mNafSessionKey.getKey();
                if (key != null) {
                    return new String(key);
                }
            }

            return null;
        }

    }

    /**
     * Account with temperary token.
     *
     */
    private class TokenAccount implements Account {
        private String mPwd;

        public boolean init() {
            // TODO: get these information from config file or SIM
            // This will be replaced for GBA is the final solution.
            final String appId = "01000138";
            final String appKey = "7B47F12C01B607A1";
            AuthnHelper authHelper = new AuthnHelper(mContext);
            // authHelper.setTest(true);
            authHelper.setDefaultUI(true);

            mPwd = null;
            try {
                authHelper.getAccessToken(appId, appKey, null, SsoSdkConstants.LOGIN_TYPE_DEFAULT, new TokenListener() {
                        @Override
                        public void onGetTokenComplete(JSONObject json) {
                            if (json == null) {
                                Log.d(TAG, "json is null!");
                                if (mHandler != null) {
                                    mHandler.obtainMessage(MSG_ERROR, ErrorCode.GETACCOUNT.ordinal(), 0).sendToTarget();
                                }

                                return;
                            }

                            Log.d(TAG, json.toString());
                            try {
                                Integer result = (Integer)json.get("resultCode");
                                if (result == AuthnConstants.CLIENT_CODE_SUCCESS) {
                                    mPwd = (String)json.get("token");
                                    Log.d(TAG, "resultCode" + result
                                          + ",\ntoken:" + mPwd
                                          + ",\npassid:" + (String) json.get("passid"));
                                    if (mHandler != null) {
                                        mHandler.sendEmptyMessage(MSG_GET_ACCOUNT);
                                    }

                                    return;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (mHandler != null) {
                                mHandler.obtainMessage(MSG_ERROR, ErrorCode.GETACCOUNT.ordinal(), 0).sendToTarget();
                            }
                        }
                    });
            } catch (BadTokenException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        public String getUser() {
            String myNumber = RCSMessageManager.getInstance().getMyNumber();
            Log.d(TAG, "getPhoneNum()" + myNumber);
            return myNumber;
        }

        public String getPassword() {
            return mPwd;
        }
    }

    private Account getAccount() {
        if (Config.useGBA()) {
            mAccount = new GbaAccount();
        } else {
            mAccount = new TokenAccount();
        }

        if (mAccount.init()) {
            return mAccount;
        }

        Log.d(TAG, "getAccount() null!");
        return null;
    }

    private ErrorCode upload(String localPath, String remotePath) {
        Log.d(TAG, "upload(" + localPath + ", " + remotePath + ")");
        if (mStore == null) {
            Log.e(TAG, "connection is not opened!");
            return ErrorCode.OTHER;
        }

        if (localPath == null) {
            Log.d(TAG, "localPath is null.");
            return ErrorCode.NONE;
        }

        // int count = mStore.select(mRemotePath);
        // if (count < 0) {
        //     Log.e(TAG, "select failed!");
        //     return false;
        // }

        ErrorCode ret = ErrorCode.NONE;
        File path = new File(localPath);
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null && files.length > 0) {
                // check cancel
                if (mCancel) {
                    mBusy = false;
                    return ErrorCode.CANCELED;
                }

                CmdResultCode res = CmdResultCode.ERROR;
                res = mStore.create(remotePath);
                if (res != CmdResultCode.OK) {
                    Log.e(TAG, "create failed!");
                    return ErrorCode.OPERATION_FAIL;
                }

                // check cancel
                if (mCancel) {
                    mBusy = false;
                    return ErrorCode.CANCELED;
                }

                for (File file : files) {
                    res = mStore.append(file.getAbsolutePath(), remotePath, new ProgressListener() {
                        @Override
                        public void updateProgress(int phaseId, long total,
                                long complete) throws UserCancelException {
                            Log.d(TAG, "complete " + complete + " Bytes");
                            if (mCancel) {
                                mBusy = false;
                                throw new UserCancelException();
                            }
                        }
                    });

                    if (res != CmdResultCode.OK) {
                        Log.d(TAG, "upload() fail, file:" + file.getAbsoluteFile());
                        break;
                    }
                }

                if (res == CmdResultCode.OK) {
                    ret = ErrorCode.NONE;
                } else if (res == CmdResultCode.CANCELED) {
                    ret = ErrorCode.CANCELED;
                } else {
                    ret = ErrorCode.OPERATION_FAIL;
                }
            } else {
                Log.d(TAG, localPath + " is empty!");
            }
        } else {
            Log.d(TAG, localPath + " not exist!");
        }

        return ret;
    }

    private List<String> doList() {
        if (mStore == null) {
            Log.e(TAG, "dolist: not opened!");
            return null;
        }

        List<String> ret = mStore.list(mRemotePath);
        Log.d(TAG, "doList return: " + ret);
        return ret;
    }

    private ErrorCode download(String localPath, String remotePath) {
        Log.d(TAG, "download(" + localPath + ", " + remotePath + ")");
        if (mStore == null) {
            Log.e(TAG, "download: not opened!");
            return ErrorCode.OTHER;
        }

        if (localPath == null) {
            Log.d(TAG, "localPath is null.");
            return ErrorCode.NONE;
        }

        ErrorCode ret = ErrorCode.OTHER;
        File path = new File(localPath);
        if (path.exists()) {
            File file = new File(localPath);
            if (!file.isDirectory()) {
                Log.e(TAG, localPath + "is not a directory!");
                return ErrorCode.OTHER;
            }

            // check cancel
            if (mCancel) {
                mBusy = false;
                return ErrorCode.CANCELED;
            }

            List<String> list = mStore.list(remotePath);
            if (list == null || list.isEmpty()) {
                Log.e(TAG, "list error!");
                return ErrorCode.OPERATION_FAIL;
            }

            // check cancel
            if (mCancel) {
                mBusy = false;
                return ErrorCode.CANCELED;
            }

            int msgCount = mStore.select(list.get(0));
            // check cancel
            if (mCancel) {
                mBusy = false;
                return ErrorCode.CANCELED;
            }

            if (msgCount < 0) {
                Log.e(TAG, "select error");
                ret = ErrorCode.OPERATION_FAIL;
            } else if (msgCount == 0) {
                Log.d(TAG, "no message to restore.");
                ret = ErrorCode.NONE;
            } else {
                CmdResultCode res = mStore.fetch(localPath, 1, msgCount, new ProgressListener() {
                        @Override
                        public void updateProgress(int phaseId, long total, long complete) throws UserCancelException {
                            Log.d(TAG, "complete " + complete + " Bytes");
                            if (mCancel) {
                                mBusy = false;
                                throw new UserCancelException();
                            }
                        }
                    });

                if (res == CmdResultCode.OK) {
                    ret = ErrorCode.NONE;
                } else if (res == CmdResultCode.CANCELED) {
                    Log.d(TAG, "download() fail, file");
                    ret = ErrorCode.CANCELED;
                } else {
                    ret = ErrorCode.OPERATION_FAIL;
                }
            }
        } else {
            Log.e(TAG, localPath + " not exists!");
        }

        return ret;
    }

}

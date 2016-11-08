package com.mediatek.security.service;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.mediatek.common.mom.IMobileConnectionCallback;
import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.IPermissionListener;
import com.mediatek.common.mom.Permission;
import com.mediatek.common.mom.PermissionRecord;
import com.mediatek.security.R;
import com.mediatek.security.datamanager.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PermControlService extends AsyncService implements OnClickListener, OnDismissListener {

    private static final String TAG = "PermControlService";
    private static final int DELAY_TIME = 1000;
    private static final int MSG_RESET = 101;
    private static final int MSG_SHOW_TOAST = MSG_RESET + 1;
    private static final int MSG_SHOW_CONF_DLG = MSG_RESET + 2;
    private static final int MSG_COUNT_DOWN = MSG_RESET + 3;
    private static final int COUNT_DOWN_TIMER = PermControlUtils.MAX_WATI_TIME / 1000;
    private static final int EXTRA_TIMER = 5000;

    //for block the thread to wait user confirm
    private Object mUserConfirmLock = new Object();

    private List<PermissionRecord> mPermRecordList = new ArrayList<PermissionRecord>();
    private IMobileManager mMoMService;
    private boolean mIsGranted;
    private boolean mIsAttached;
    private boolean mIsRegisted = false;

    private PermissionRecord mCurrentPermRecord;
    private CheckBox mCheckBox;
    private TextView mTimeCountDown;
    private AlertDialog mAlertDlg;

    private static final int NOTIFY_FOREGROUND_ID = 1201;

    // Receiver to handle package update broadcast
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                Log.d("@M_" + TAG, "onReceive with action = " + action);
                if (IMobileManager.ACTION_PACKAGE_CHANGE.equals(action)) {
                    handleReceiverIntent(intent);
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_TOAST) {
                handleDenyToastMsg(msg.getData());
            } else if (msg.what == MSG_SHOW_CONF_DLG) {
                handleConfirmDlgMsg(mCurrentPermRecord, msg.arg1);
            } else if (msg.what == MSG_COUNT_DOWN) {
                handleCountDownMsg(msg);
            }
        }
    };

    public PermControlService() {
        super("PermControlService");
    }


    protected void handleCountDownMsg(Message msg) {
        int timer = msg.arg1 - 1;
        Log.d("@M_" + TAG, "timer is = " + timer);
        if (timer > 0) {
            updateCount(timer);
        } else {
            Log.d("@M_" + TAG, "time out and deny the permission");
            // time out dismiss dialog and return false onPermissionCheck
            PermControlUtils.showDenyToast(getApplicationContext(), mCurrentPermRecord.mPackageName,
                                        mCurrentPermRecord.mPermissionName);
            mIsGranted = false;
            if (mAlertDlg != null) {
                mAlertDlg.dismiss();
            }
        }
    }

    protected void handleDenyToastMsg(Bundle data) {
        if (data != null) {
            Log.d("@M_" + TAG, "handleDenyToastMsg");
            String pkgName = data.getString(PermControlUtils.PACKAGE_NAME);
            String permName = data.getString(PermControlUtils.PERMISSION_NAME);
            PermControlUtils.showDenyToast(getApplicationContext(), pkgName, permName);
        }
    }

    protected void stopPermControlService(boolean isEnabled) {
        Log.d("@M_" + TAG, "stopPermControlService isEnabled = " + isEnabled);
        mMoMService.enablePermissionController(isEnabled);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("@M_" + TAG, "onCreate()");
        initService();
    }

    /**
     * Must call before access any further moms apis
     */
    private void initService() {
        Log.d("@M_" + TAG, "initService()");
        PermControlUtils.initUtil(getApplicationContext());
        if (mMoMService == null) {
            mMoMService = (IMobileManager) getSystemService(Context.MOBILE_SERVICE);
        }
        setServiceInforeground();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IMobileManager.ACTION_PACKAGE_CHANGE);
        registerReceiver(mReceiver, intentFilter);
        mIsRegisted = true;
    }

    // for increase the adj of process so set service in foreground
    private void setServiceInforeground() {
        Notification notification = new Notification();
        String titleStr = getString(R.string.foreground_notification_title);
        String summaryStr = getString(R.string.foreground_notification_summary);
        notification.icon = R.drawable.ic_settings_security;
        notification.tickerText = titleStr;
        notification.when = 0;
        notification.flags = Notification.FLAG_NO_CLEAR;
        Intent intent = new Intent();
        intent.setAction(PermControlUtils.PERM_UI_ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, 0);
        notification.setLatestEventInfo(this, titleStr, summaryStr,
                pendingIntent);
        startForeground(NOTIFY_FOREGROUND_ID, notification);
    }

    private void attachMoMS() {
        mIsAttached = mMoMService.attach(new IMobileConnectionCallback.Stub() {
            @Override
            public void onConnectionEnded() throws RemoteException {
                Log.d("@M_" + TAG, "onConnectionEnded in service callback");
                mIsAttached = false;
            }
            @Override
            public void onConnectionResume() {

            }
        });
        PermControlUtils.setInHouseEnabled(this, mIsAttached);
        Log.d("@M_" + TAG, "mIsAttached = " + mIsAttached);
    }

    private void registerMoMS() {
        DatabaseManager.initDataBase(this);
        attachMoMS();
        registerReceiver();
        if (mIsAttached) {
            List<PermissionRecord> permRecordList = DatabaseManager.getAllPermRecordList();
            printRecordList(permRecordList);
            try {
                mMoMService.setPermissionRecords(permRecordList);
                mMoMService.registerPermissionListener(new PermissionListener());
                mMoMService.enablePermissionController(true);
            } catch (SecurityException e) {
                Log.d("@M_" + TAG, "is detached so no permission to use api with " + e);
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onHandleIntent() action = " + action);
            if (action == null) {
                handleMomsLaunch();
            } else if (PermControlUtils.START_SERVICE_ACTION.equals(action)) {
                if (!mIsAttached) {
                    handleAppLaunch(intent);
                }
            } else if (IMobileManager.ACTION_PACKAGE_CHANGE.equals(action)) {
                handlePackgeUpdate(intent);
            }
        } else {
            //Handle if service is killed
            Log.d("@M_" + TAG, "intent = null servie is killed and relaunched by system");
            registerMoMS();
        }
    }

    private void handleAppLaunch(Intent intent) {
        Log.d("@M_" + TAG, "handleAppLaunch()");
        // In case the notification may still exist, need to removed first
        PermControlUtils.cancelNotification(this);
        registerMoMS();
        sendLoadFinishBroadcast();
    }



    private void handleMomsLaunch() {
        Log.d("@M_" + TAG, "handleMomsLaunch()");
        if (PermControlUtils.isInHouseEnabled(this)) {
            boolean isEnabled = PermControlUtils.isPermControlOn(this);
            PermControlUtils.showHintNotify(this);
            if (isEnabled) {
                registerMoMS();
            } else {
                stopSelf();
            }
        } else {
            stopSelf();
        }
    }

    private void handlePackgeUpdate(Intent intent) {
        int state = intent.getIntExtra(IMobileManager.ACTION_EXTRA_STATUS, IMobileManager.PACKAGE_ADDED);
        Log.d("@M_" + TAG, "state = " + state);
        switch (state) {
            case IMobileManager.PACKAGE_ADDED:
            case IMobileManager.PACKAGE_UPDATED:
                String pkgNameAdd = intent.getStringExtra(IMobileManager.ACTION_EXTRA_PACKAGE);
                handleInstall(pkgNameAdd);
                break;
            case IMobileManager.PACKAGE_REMOVED:
                String pkgNameRemoved = intent.getStringExtra(IMobileManager.ACTION_EXTRA_PACKAGE);
                handleRemove(pkgNameRemoved);
                break;
            case IMobileManager.PACKAGE_EXT_AVAILABLE:
                // sd card mount the installed pkg will be available
                String[] pkgArray = intent.getStringArrayExtra(IMobileManager.ACTION_EXTRA_PACKAGE_LIST);
                handleExtApp(pkgArray);
                break;
            case IMobileManager.PACKAGE_EXT_UNAVAILABLE:
                //sd card unmount the installed pkg will be unavailable
                String[] pkgArrayRm = intent.getStringArrayExtra(IMobileManager.ACTION_EXTRA_PACKAGE_LIST);
                handleExtAppRm(pkgArrayRm);
                break;
            default:
                Log.e("@M_" + TAG, "Need to check");
                break;
        }
    }

    private void handleExtAppRm(String[] pkgArrayRm) {
        if (pkgArrayRm != null) {
            for (String pkgName : pkgArrayRm) {
                DatabaseManager.setRemovedCacheMap(pkgName);
                DatabaseManager.deletePkgFromCache(pkgName);
                sendCacheUpdateBroadcast(pkgName);
            }
        }
    }


    private void handleExtApp(String[] pkgArray) {
        Log.d("@M_" + TAG, "handleExtApp()");
        List<PermissionRecord> permRecordList;
        if (pkgArray != null) {
            for (String pkgName : pkgArray) {
                permRecordList = getRemoveMap(pkgName);
                if (permRecordList != null) {
                    // only update cache
                    DatabaseManager.add(permRecordList, pkgName);
                    mMoMService.setPermissionRecords(permRecordList);
                    sendCacheUpdateBroadcast(pkgName);
                } else {
                    handleInstall(pkgName);
                }
            }
        }
    }

    private List<PermissionRecord> getRemoveMap(String pkgName) {
        Map<String, List<PermissionRecord>> removeMap = DatabaseManager.getRemovedCacheMap();
        if (removeMap != null && removeMap.containsKey(pkgName)) {
            return removeMap.get(pkgName);
        }
        return null;
    }


    private void handleRemove(String pkgName) {
        Log.d("@M_" + TAG, "handleRemove() with pkgName = " + pkgName);
        if (pkgName != null) {
            DatabaseManager.delete(pkgName);
            sendCacheUpdateBroadcast(pkgName);
        }
    }

    private void handleInstall(String pkgName) {
        Log.d("@M_" + TAG, "handleInstall() with pkgName = " + pkgName);
        if (pkgName != null) {
            if (PermControlUtils.isPkgInstalled(this, pkgName)) {
                List<Permission> permList = mMoMService.getPackageGrantedPermissions(pkgName);
                List<PermissionRecord> permRecordList = DatabaseManager.add(pkgName, permList);
                if (permRecordList != null && mIsAttached) {
                    mMoMService.setPermissionRecords(permRecordList);
                    sendCacheUpdateBroadcast(pkgName);
                }
            } else {
                Log.e("@M_" + TAG, "Receive add broadcast but can not query appinfo, internal app removed case");
                handleRemove(pkgName);
            }
        }
    }

    /**
     * Show a system confirm dialog from service
     * @param record the PermissionRecord data type
     * @param flag the flag of the PermissionRecord
     */
    private void handleConfirmDlgMsg(PermissionRecord record, int flag) {
        Log.d("@M_" + TAG, "Show confirm dialog");
        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notify_dialog_title);
        builder.setPositiveButton(R.string.accept_perm, this);
        builder.setNegativeButton(R.string.deny_perm, this);
        builder.setCancelable(false);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.notify_dialog_customview, null);
        builder.setView(view);

        TextView messageText = (TextView) view.findViewById(R.id.message);
        mTimeCountDown = (TextView) view.findViewById(R.id.count_timer);
        mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        if ((flag & IMobileManager.PERMISSION_FLAG_USERCONFIRM) > 0) {
            mCheckBox.setVisibility(View.GONE);
        }
        String label = PermControlUtils.getApplicationName(this, record.mPackageName);
        String msg = getString(R.string.notify_dialog_msg_body, label,
                                PermControlUtils.getMessageBody(this, record.mPermissionName));
        messageText.setText(msg);

        mAlertDlg = builder.create();
        mAlertDlg.setOnDismissListener(this);
        mAlertDlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        setStatusBarEnableStatus(false);

        mAlertDlg.show();
        updateCount(COUNT_DOWN_TIMER);
    }

    private void updateCount(int timer) {
        setCountText(timer);
        Message msg = Message.obtain();
        msg.what = MSG_COUNT_DOWN;
        msg.arg1 = timer;
        mHandler.sendMessageDelayed(msg, DELAY_TIME);
    }

    private void setCountText(int timer) {
        String msg = getString(R.string.time_count_down_hint, String.valueOf(timer));
        mTimeCountDown.setText(msg);
    }
    private void printRecordList(List<PermissionRecord> permRecordList) {
        if (permRecordList != null) {
            for (PermissionRecord permrecord : permRecordList) {
                Log.d("@M_" + TAG, "pkg = " + permrecord.mPackageName +
                           "permName = " + permrecord.mPermissionName +
                           "status = " + permrecord.getStatus());
            }
        }
    }

    class PermissionListener extends IPermissionListener.Stub {
        @Override
        public boolean onPermissionCheck(PermissionRecord record, int flag, int uid, Bundle data) {
            Log.d("@M_" + TAG, "onPermissionCheck pkg = " + record.mPackageName + " " +
                        record.mPermissionName + " " +
                        record.getStatus() + " " + flag);
            if (record.getStatus() == IMobileManager.PERMISSION_STATUS_CHECK) {
                if (checkCurrentUid()) {
                    return handleCheckCase(record, flag);
                } else {
                    Log.d("@M_" + TAG, "Not in same user deny the permission");
                    return false;
                }
            } else if (record.getStatus() == IMobileManager.PERMISSION_STATUS_DENIED) {
                showDenyToast(record);
                return false;
            } else if (record.getStatus() == IMobileManager.PERMISSION_STATUS_GRANTED) {
                return true;
            } else {
                Log.e("@M_" + TAG, "Not correct status");
                return false;
            }
        }
        @Override
        public void onPermissionChange(PermissionRecord record) {
            Log.d("@M_" + TAG, "onPermissionChange");
        }
    }

    /*
     * Synchronized the function of handleCheckCase, whenever one permission confirm thread hold the lock
     * other permission thread need to wait previous release otherwise wait
     *
     */
    private synchronized boolean handleCheckCase(PermissionRecord record, int flag) {
        Log.d("@M_" + TAG, "handleCheckCase()");
        synchronized (mUserConfirmLock) {
            try {
                mCurrentPermRecord = record;
                showConfirmDlg(flag);
                // add extra timer as the time counter is not accurate, in some case the lock
                // may wake up before time counter up to 20s
                mUserConfirmLock.wait(PermControlUtils.MAX_WATI_TIME + EXTRA_TIMER);
                Log.d("@M_" + TAG, "release the lock");
            } catch (InterruptedException e) {
                Log.d("@M_" + TAG, "error");
            }
        }
        Log.d("@M_" + TAG, "mIsGranted " + mIsGranted);
        return mIsGranted;
    }

    public boolean checkCurrentUid() {
        int myId = UserHandle.myUserId();
        UserManager userMgr = (UserManager) getSystemService(Context.USER_SERVICE);
        int switchUserId = userMgr.getSwitchedUserId();
        Log.d("@M_" + TAG, "myId = " + myId + " switchUserId = " + switchUserId);
        return myId == switchUserId ? true : false;
    }

    /*
     * Because the system dialog need to show in main thread of service so show the dialog via a handler
     */
    private void showConfirmDlg(int flag) {
        Message msg = Message.obtain();
        msg.arg1 = flag;
        msg.what = MSG_SHOW_CONF_DLG;
        mHandler.sendMessage(msg);
    }

    /*
     * The toast have to show in a main thread so show the toast via a handler
     */
    private void showDenyToast(PermissionRecord record) {
        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putCharSequence(PermControlUtils.PACKAGE_NAME, record.mPackageName);
        data.putCharSequence(PermControlUtils.PERMISSION_NAME, record.mPermissionName);
        msg.setData(data);
        msg.what = MSG_SHOW_TOAST;
        mHandler.sendMessage(msg);
    }

    private void setStatusBarEnableStatus(boolean enabled) {
        Log.i("@M_" + TAG, "setStatusBarEnableStatus(" + enabled + ")");
        StatusBarManager statusBarManager;
        statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBarManager != null) {
            if (enabled) {
                statusBarManager.disable(StatusBarManager.DISABLE_NONE);
            } else {
                statusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                                         StatusBarManager.DISABLE_RECENT |
                                         StatusBarManager.DISABLE_HOME);
            }
        } else {
            Log.e("@M_" + TAG, "Fail to get status bar instance");
        }
    }


    /*
     * Whenever the data cache modified need to call this function to notify
     */
    private void sendCacheUpdateBroadcast(String pkgName) {
        Intent intent = new Intent();
        intent.putExtra(PermControlUtils.PACKAGE_NAME, pkgName);
        intent.setAction(PermControlUtils.PERM_CONTROL_DATA_UPDATE);
        sendBroadcast(intent);
    }

    private void sendLoadFinishBroadcast() {
        Intent intent = new Intent();
        intent.setAction(PermControlUtils.PERM_CONTROL_DATA_UPDATE);
        sendBroadcast(intent);
    }


    /**
     * true for grant and false for deny
     * @param status enable or not the permission
     */
    public void releaseLock() {
        synchronized (mUserConfirmLock) {
            mUserConfirmLock.notifyAll();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("@M_" + TAG, "onDestroy");
        stopForeground(true);
        if (mIsRegisted) {
            unregisterReceiver(mReceiver);
            mIsRegisted = false;
        }
        setStatusBarEnableStatus(true);
        if (mIsAttached && mMoMService != null) {
            try {
                mMoMService.enablePermissionController(false);
                Log.d("@M_" + TAG, "Service destroy and disable permission control");
            } catch (SecurityException e) {
                Log.e("@M_" + TAG, "catch log as in house has been detached");
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean enable = false;
        int status = IMobileManager.PERMISSION_STATUS_CHECK;
        if (which == DialogInterface.BUTTON_POSITIVE) {
            status = IMobileManager.PERMISSION_STATUS_GRANTED;
            enable = true;
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            status = IMobileManager.PERMISSION_STATUS_DENIED;
            enable = false;
        }
        if (mCheckBox.isChecked()) {
            mCurrentPermRecord.setStatus(status);
            PermControlUtils.changePermission(mCurrentPermRecord, this);
        }
        Log.d("@M_" + TAG, "Click dialog button with check box " + mCheckBox.isChecked() + " enable = " + enable);
        mIsGranted = enable;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d("@M_" + TAG, "Dialog dimissed");
        setStatusBarEnableStatus(true);
        mHandler.removeMessages(MSG_COUNT_DOWN);
        releaseLock();
    }
}

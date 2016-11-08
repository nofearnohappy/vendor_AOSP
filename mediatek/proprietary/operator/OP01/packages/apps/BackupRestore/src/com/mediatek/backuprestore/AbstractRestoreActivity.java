package com.mediatek.backuprestore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;

import com.mediatek.backuprestore.BackupRestoreService.OnErrListener;
import com.mediatek.backuprestore.CheckedListActivity.OnCheckedCountChangedListener;
import com.mediatek.backuprestore.RestoreService.RestoreServiceBinder;
import com.mediatek.backuprestore.ResultDialog.ResultEntity;
import com.mediatek.backuprestore.SDCardReceiver.OnSDCardStatusChangedListener;
import com.mediatek.backuprestore.utils.Constants.DialogID;
import com.mediatek.backuprestore.utils.Constants.ErrorType;
import com.mediatek.backuprestore.utils.Constants.MessageID;
import com.mediatek.backuprestore.utils.Constants.State;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractRestoreActivity extends CheckedListActivity implements
        OnCheckedCountChangedListener {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/AbstractRestoreActivity";

    private static final String SELECT_INDEX = "index";
    private static final String TITLE = "title";
    private static final String INFO = "info";
    protected ArrayList<String> mUnCheckedList = new ArrayList<String>();
    protected Handler mHandler;
    BaseAdapter mAdapter;
    private Button mBtRestore = null;
    private Button mBtSelect = null;
    private ProgressDialog mProgressDialog;
    protected RestoreServiceBinder mRestoreService;
    OnErrListener mRestoreErrListener = new RestoreErrListener();
    OnSDCardStatusChangedListener mSDCardListener;
    // / M: add for notice dialog (import/replace)
    private List<HashMap<String, String>> mListHashMap;
    private HashMap<String, String> mMap;
    private SimpleAdapter mSimpleAdapter;
    private RadioButton mRadioButton = null;
    private int mSelectedIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            restoreIndex(savedInstanceState);
        }
        setProgressBarIndeterminateVisibility(false);
        setContentView(R.layout.restore);
        init();

        /*
         * bind Restore Service when activity onCreate, and unBind Service when
         * activity onDestroy
         */
        this.bindService();
        MyLogger.logI(CLASS_TAG, " onCreate");
    }

    private void restoreIndex(Bundle savedInstanceState) {
        mSelectedIndex = savedInstanceState.getInt(SELECT_INDEX);
    }

    // / M: add for notice dialog (import/replace)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECT_INDEX, mSelectedIndex);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLogger.logI(CLASS_TAG, " onDestroy");
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        unRegisteSDCardListener();

        if (mRestoreService != null) {
            mRestoreService.setHandler(null);
        }

        if (mRestoreService != null) {
            mRestoreService.setOnErrListener(null);
        }

        if (mRestoreService != null && mRestoreService.getState() == State.INIT) {
            this.stopService();
        }

        this.unBindService();
        mHandler = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        MyLogger.logI(CLASS_TAG, " onPasue");
    }

    @Override
    public void onStop() {
        super.onStop();
        MyLogger.logI(CLASS_TAG, " onStop");
    }

    @Override
    protected void onStart() {
        MyLogger.logI(CLASS_TAG, " onStart");
        super.onStart();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MyLogger.logI(CLASS_TAG, " onConfigurationChanged");
    }

    @Override
    public void onCheckedCountChanged() {
        mAdapter.notifyDataSetChanged();
        updateButtonState();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void init() {
        registerOnCheckedCountChangedListener(this);
        registerSDCardListener();
        initButton();
        initHandler();
        initDetailList();
        createProgressDlg();
    }

    protected Dialog onCreateDialog(int id) {
        return onCreateDialog(id, null);
    }

    protected Dialog onCreateDialog(int id, Bundle args) {
        Dialog dialog = null;
        MyLogger.logI(CLASS_TAG, " oncreateDialog, id = " + id);
        switch (id) {

        case DialogID.DLG_RESTORE_CONFIRM:
            dialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.notice)
                    .setSingleChoiceItems(PersonalRestoreTypeAdapter(), mSelectedIndex,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    mSelectedIndex = which;
                                    mSimpleAdapter.notifyDataSetChanged();
                                }
                            }).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MyLogger.logI(CLASS_TAG, " to Restore" + mSelectedIndex);
                            startRestore(mSelectedIndex);
                        }
                    }).setCancelable(false).create();
            break;

        case DialogID.DLG_SDCARD_REMOVED:
            dialog = new AlertDialog.Builder(this).setTitle(R.string.file_no_exist_and_update)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.sdcard_removed)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            if (mRestoreService != null) {
                                mRestoreService.reset();
                            }
                            stopService();
                            AbstractRestoreActivity.this.finish();
                        }
                    }).setCancelable(false).create();
            break;

        case DialogID.DLG_SDCARD_FULL:
            dialog = new AlertDialog.Builder(this).setTitle(R.string.error)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.sdcard_is_full)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            if (mRestoreService != null) {
                                mRestoreService.cancelRestore();
                                mRestoreService.reset();
                            }
                        }
                    }).setCancelable(false).create();
            break;

        default:
            break;
        }
        return dialog;
    }

    private void unRegisteSDCardListener() {
        if (mSDCardListener != null) {
            SDCardReceiver receiver = SDCardReceiver.getInstance();
            if (receiver != null) {
                receiver.unRegisterOnSDCardChangedListener(mSDCardListener);
            }
        }
    }

    private void registerSDCardListener() {
        mSDCardListener = new OnSDCardStatusChangedListener() {
            @Override
            public void onSDCardStatusChanged(boolean mount, String path) {
                if (!mount) {
                    if (externalDataChecked(path)) {
                        if (mRestoreService != null) {
                            mRestoreService.cancelRestore();
                            mRestoreService.reset();
                            AbstractRestoreActivity.this.finish();
                        }
                    }
                }
            }
        };
        SDCardReceiver receiver = SDCardReceiver.getInstance();
        receiver.registerOnSDCardChangedListener(mSDCardListener);
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                Bundle data = msg.getData();
                switch (msg.what) {
                case MessageID.COMPOSER_CHANGED:
                    String content = formatProgressDialogMsg(0,
                            data.getString(Constants.MESSAGE_CONTENT));
                    if (mProgressDialog != null) {
                        mProgressDialog.setMessage(content);
                        mProgressDialog.setMax(data.getInt(Constants.MESSAGE_MAX_PROGRESS));
                        mProgressDialog.setProgress(0);
                    }
                    break;

                case MessageID.PROGRESS_CHANGED:
                    int curProcess = data.getInt(Constants.MESSAGE_CURRENT_PROGRESS);
                    int max = data.getInt(Constants.MESSAGE_MAX_PROGRESS);
                    boolean isUpdataMsg = data.getBoolean(Constants.MESSAGE_IS_UPDATA_MSG, false);
                    if (mProgressDialog != null) {
                        if (isUpdataMsg && curProcess < max) {
                            String msgContent = formatProgressDialogMsg(curProcess, null);
                            mProgressDialog.setMessage(msgContent);
                        }
                        mProgressDialog.setProgress(curProcess);
                    }
                    break;

                case MessageID.RESTORE_END:
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }

                    ArrayList<ResultEntity> ResultRecord = (ArrayList<ResultEntity>) data
                            .get(Constants.MESSAGE_RESULT_RECORD);
                    showRestoreResult(ResultRecord);

                    if (mRestoreService != null) {
                        mRestoreService.reset();
                    }
                    stopService();
                    break;

                default:
                    break;
                }
            }
        };
    }

    private void resetSelectButton() {
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                isClick = false;
            }
        }, 500);
    }

    volatile boolean isClick = false;

    public void onClick(View v) {
        if (isClick) {
            Log.i(CLASS_TAG, "one Button have clicked, return!");
            return;
        }
        isClick = true;

        int id = v.getId();
        switch (id) {
        case R.id.restore_bt_select:
            Log.e(CLASS_TAG, "mButton Select clicked ");
            if (isAllChecked(true)) {
                setAllChecked(false);
            } else {
                setAllChecked(true);
            }
            isClick = false;
            Log.i(CLASS_TAG, "set isClick = false");
            break;

        case R.id.restore_bt_restore:
            Log.i(CLASS_TAG, "mButton restore clicked ");
            if (getCheckedCount() > 0) {
                setButtonStatus(false);
                showDialogModule(DialogID.DLG_RESTORE_CONFIRM);
            }
            setButtonStatus(true);
            resetSelectButton();
            break;

        default:
            break;
        }

        return;
    }

    private void initButton() {
        mBtSelect = (Button) findViewById(R.id.restore_bt_select);
        mBtRestore = (Button) findViewById(R.id.restore_bt_restore);
    }

    protected void updateButtonState() {
        if (getCount() == 0) {
            setButtonsEnable(false);
            return;
        }

        if (isAllChecked(false)) {
            mBtRestore.setEnabled(false);
            mBtSelect.setText(R.string.selectall);
        } else {
            mBtRestore.setEnabled(true);
            if (isAllChecked(true)) {
                mBtSelect.setText(R.string.unselectall);
            } else {
                mBtSelect.setText(R.string.selectall);
            }
        }
    }

    protected void setButtonsEnable(boolean enabled) {
        if (mBtSelect != null) {
            mBtSelect.setEnabled(enabled);
        }
        if (mBtRestore != null) {
            mBtRestore.setEnabled(enabled);
        }
    }

    private void initDetailList() {
        mAdapter = initAdapter();
        setListAdapter(mAdapter);
        this.updateButtonState();
    }

    protected abstract BaseAdapter initAdapter();

    protected void notifyListItemCheckedChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        updateButtonState();
    }

    protected ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(getString(R.string.restoring));
            mProgressDialog.setCancelable(false);
        }
        return mProgressDialog;
    }

    protected void showProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.show();
    }

    protected void setProgressDialogMax(int max) {
        if (mProgressDialog != null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.setMax(max);
    }

    protected void setProgressDialogProgress(int value) {
        if (mProgressDialog != null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.setProgress(value);
    }

    protected void setProgressDialogMessage(CharSequence message) {
        if (mProgressDialog != null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.setMessage(message);
    }

    protected boolean isProgressDialogShowing() {
        if (mProgressDialog != null) {
            return mProgressDialog.isShowing();
        }
        return false;
    }

    protected void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    protected boolean isCanStartRestore() {
        if (mRestoreService == null) {
            MyLogger.logE(CLASS_TAG, " isCanStartRestore : mRestoreService is null");
            return false;
        }

        if (mRestoreService.getState() != State.INIT) {
            MyLogger.logE(CLASS_TAG,
                    " isCanStartRestore :Can not to start Restore. Restore Service is ruuning");
            return false;
        }
        return true;
    }

    protected int errChecked(String path) {
        int errorType = -1;
        if (!SDCardUtils.checkedPath(path)) {
            // no sdcard
            // ret = true
            errorType = ErrorType.SDCARD_REMOVED;
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    public void run() {
                        showDialog(DialogID.DLG_SDCARD_REMOVED, null);
                    }
                });
            }
        } else if (path != null && SDCardUtils.getAvailableSize(path) <= 512) {
            // no space
            errorType = ErrorType.SDCARD_FULL;
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    public void run() {
                        showDialog(DialogID.DLG_SDCARD_FULL, null);
                    }
                });
            }
        } else {
            errorType = ErrorType.WARNING;

        }
        MyLogger.logE(CLASS_TAG, " errChecked path = " + path + " errorType = "
                + errorType);
        return errorType;
    }

    protected void setHandler(Handler handler) {
        if (mHandler != null && mRestoreService != null) {
            mRestoreService.setHandler(handler);
        }
    }

    protected abstract void afterServiceConnected();

    protected abstract void startRestore(int command);

    protected abstract void startRestore();

    protected abstract void showRestoreResult(ArrayList<ResultEntity> list);

    protected abstract String formatProgressDialogMsg(int currentProgress, String content);

    protected abstract int errChecked();

    protected abstract boolean externalDataChecked(String path);

    protected void showDialogModule(int dialogId) {
        showDialog(dialogId);
    }

    private void bindService() {
        getApplicationContext().bindService(new Intent(this, RestoreService.class), mServiceCon,
                Service.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        if (mRestoreService != null) {
            mRestoreService.setHandler(null);
            mRestoreService.setOnErrListener(null);
        }
        getApplicationContext().unbindService(mServiceCon);
    }

    protected void startService() {
        startService(new Intent(this, RestoreService.class));
    }

    protected void stopService() {
        if (mRestoreService != null) {
            mRestoreService.reset();
        }
        stopService(new Intent(this, RestoreService.class));
    }

    ServiceConnection mServiceCon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mRestoreService = (RestoreServiceBinder) service;
            if (mRestoreService != null) {
                mRestoreService.setHandler(mHandler);
                mRestoreService.setOnErrListener(mRestoreErrListener);
                afterServiceConnected();
            }
            MyLogger.logI(CLASS_TAG, " onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName name) {
            mRestoreService = null;
            MyLogger.logI(CLASS_TAG, " onServiceDisconnected");
        }
    };

    private class RestoreErrListener implements OnErrListener {
        public void onErr(final IOException e) {
            MyLogger.logE(CLASS_TAG, "onRestoreErr");
            switch (errChecked()) {
            case ErrorType.SDCARD_REMOVED:
                if (mRestoreService != null && mRestoreService.getState() != State.INIT
                        && mRestoreService.getState() != State.FINISH) {
                    MyLogger.logE(CLASS_TAG, "onRestoreErr: sdcard is removed !");
                    mRestoreService.pauseRestore();
                }
                break;
            case ErrorType.WARNING:
                // / M: add for the path is internal path,
                // / but picture path is sdcard and unmount sdcard
                MyLogger.logE(CLASS_TAG, "onRestoreErr: restore is warnning !");
                break;
            case ErrorType.SDCARD_FULL:
            default:
                MyLogger.logE(CLASS_TAG, "onRestoreErr: sdcard is full and default!");
                if (mRestoreService != null) {
                    mRestoreService.cancelRestore();
                    mRestoreService.reset();
                }
                break;
            }
        }
    }

    // / M: add for notice dialog (import/replace)
    private List<HashMap<String, String>> initRestoreTypeData() {
        mListHashMap = new ArrayList<HashMap<String, String>>();
        mMap = new HashMap<String, String>();
        mMap.put(TITLE, this.getResources().getString(R.string.import_data));
        mMap.put(INFO, this.getResources().getString(R.string.import_info));

        mListHashMap.add(mMap);

        mMap = new HashMap<String, String>();
        mMap.put(TITLE, this.getResources().getString(R.string.replace_data));
        mMap.put(INFO, this.getResources().getString(R.string.replace_info));
        mListHashMap.add(mMap);
        return mListHashMap;
    }

    // / M: add for notice dialog (import/replace)
    private ListAdapter PersonalRestoreTypeAdapter() {
        mSimpleAdapter = new SimpleAdapter(this, initRestoreTypeData(), R.layout.dialog_restore,
                new String[] { TITLE, INFO }, new int[] { R.id.title, R.id.summary }) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // TODO Auto-generated method stub
                View view = super.getView(position, convertView, parent);
                mRadioButton = (RadioButton) view.findViewById(R.id.radiobutton);
                mRadioButton.setOnClickListener(null);
                mRadioButton.setClickable(false);
                if (mSelectedIndex == position) {
                    mRadioButton.setChecked(true);
                } else {
                    mRadioButton.setChecked(false);
                }
                return view;
            }
        };
        return mSimpleAdapter;
    }

    public void initCheckStatus(boolean isCheck) {
        setAllChecked(isCheck);
    }
}

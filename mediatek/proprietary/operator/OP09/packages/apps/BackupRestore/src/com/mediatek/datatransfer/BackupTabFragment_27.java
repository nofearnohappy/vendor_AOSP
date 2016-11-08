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

package com.mediatek.datatransfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.datatransfer.BackupEngine.BackupResultType;
import com.mediatek.datatransfer.BackupService.BackupBinder;
import com.mediatek.datatransfer.BackupService.OnBackupStatusListener;
import com.mediatek.datatransfer.modules.CalendarBackupComposer;
import com.mediatek.datatransfer.modules.CalllogBackupComposer;
import com.mediatek.datatransfer.modules.Composer;
import com.mediatek.datatransfer.modules.ContactBackupComposer;
import com.mediatek.datatransfer.modules.MmsBackupComposer;
import com.mediatek.datatransfer.modules.SmsBackupComposer;
import com.mediatek.datatransfer.utils.Constants.MessageID;
import com.mediatek.datatransfer.utils.ModuleType;
import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.datatransfer.utils.NotifyManager;
import com.mediatek.datatransfer.utils.SDCardUtils;
import com.mediatek.datatransfer.utils.Constants.DialogID;
import com.mediatek.datatransfer.utils.Constants;
import com.mediatek.datatransfer.utils.Constants.State;
import com.mediatek.datatransfer.utils.Constants.ContactType;
import com.mediatek.datatransfer.utils.Utils;
import com.mediatek.datatransfer.ResultDialog.ResultEntity;
//import com.mediatek.datatransfer.CheckedListActivity.OnCheckedCountChangedListener;

public class BackupTabFragment_27 extends ListFragment {
    private final String CLASS_TAG = MyLogger.LOG_TAG + "/BackupTabFragment";

    private final String SAVE_STATE_UNCHECKED_IDS = "CheckedListActivity/unchecked_ids";

    private ArrayList<PersonalItemData> mBackupItemDataList = new ArrayList<PersonalItemData>();
    private ArrayList<AlertDialog> dialogs = new ArrayList<AlertDialog>();
    private PersonalDataBackupAdapter mBackupListAdapter ;
    InitPersonalDataTask initDataTask = null;
    private OnBackupStatusListener mBackupListener;
    private boolean[] mContactCheckTypes = new boolean[10];
    private boolean[] mMessageCheckTypes = new boolean[2];
    private String CONTACT_TYPE = "contact";
    private String MESSAGE_TYPE = "message";
    private String mBackupFolderPath;
    protected BaseAdapter mAdapter;

    private List<AppSnippet> mData = new ArrayList<AppSnippet>();

    private CheckBox mCheckBoxSelect;
    private Button mButtonBackup;
    private View mDivider;
    private View mView;
    private LinearLayout mCaView;
    private LinearLayout mCtView;

    protected ProgressDialog mProgressDialog;
    protected ProgressDialog mCancelDlg;
    protected Handler mHandler;
    protected BackupBinder mBackupService;


    private static final float DISABLE_ALPHA = 0.4f;
    private static final float ENABLE_ALPHA = 1.0f;

    List<SubInfoRecord> mSimInfoList;
    int mSimCount = 0;
    private BackupFinishCallBack mCallBack;
    protected ArrayList<Long> mUnCheckedIds = new ArrayList<Long>();
    protected ArrayList<Long> mDisabledIds = new ArrayList<Long>();
    List<String> messageEnable = new ArrayList<String>();

    public interface BackupFinishCallBack {
        public void onBackupFinish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(CLASS_TAG, "onCreate done");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallBack = (BackupFinishCallBack) activity;
        } catch (ClassCastException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw e;
        }
        Log.i(CLASS_TAG, "onAttach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //inflate layout for this fragment
        View view = inflater.inflate(R.layout.backup, null, false);
        Log.i(CLASS_TAG, "onCreateview done");

        mCaView = (LinearLayout) view.findViewById(R.id.loading_container);
        mCtView = (LinearLayout) view.findViewById(R.id.backup_content);

        mCheckBoxSelect = (CheckBox) view.findViewById(R.id.backup_checkbox_select);

        mDivider  = view.findViewById(R.id.backup_divider);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        mDivider.setBackground(listView.getDivider());
        mButtonBackup = (Button) view.findViewById(R.id.backup_bt_backcup);
        mButtonBackup.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mBackupService == null || mBackupService.getState() != State.INIT) {
                    Log.e(CLASS_TAG,
                            "Can not to start. BackupService not ready or BackupService is ruuning");
                    return;
                }

                if (isAllChecked(false)) {
                    Log.e(CLASS_TAG, "to Backup List is null or empty");
                    return;
                }
                String path = SDCardUtils.getStoragePath();
                if (path != null) {
                    startBackup();
                } else {
                    showDialog(DialogID.DLG_NO_SDCARD, null);
                }
            }
        });



        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(CLASS_TAG, "-----OnActivityCreated------");

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        ActionBar actionBar = this.getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            mContactCheckTypes = savedInstanceState.getBooleanArray(CONTACT_TYPE);
            mMessageCheckTypes = savedInstanceState.getBooleanArray(MESSAGE_TYPE);
        } else {
            getSimInfoList();
            for (int index = 0; index < mContactCheckTypes.length; index++) {
                mContactCheckTypes[index] = true;
            }
            mMessageCheckTypes[0] = true;
            mMessageCheckTypes[1] = true;
        }

        init();
        initDataTask = new InitPersonalDataTask();
        initDataTask.execute();
    }

    public void onPause() {
        super.onPause();
        Log.i(CLASS_TAG, "onPasue");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(CLASS_TAG, "onStop");
        if (initDataTask != null && initDataTask.getStatus() == AsyncTask.Status.RUNNING) {
            initDataTask.cancel(true);
            mBackupListAdapter.reset();
            initDataTask = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialogs != null && !dialogs.isEmpty()) {
            for (AlertDialog dialog : dialogs) {
                dialog.cancel();
            }
        }
        Log.i(CLASS_TAG, "onDestroy");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLASS_TAG, "onResume");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(CLASS_TAG, "onDetach");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int size = mUnCheckedIds.size();
        long[] array = new long[size];
        for (int position = 0; position < size; position++) {
            array[position] = mUnCheckedIds.get(position);
        }
        outState.putLongArray(SAVE_STATE_UNCHECKED_IDS, array);

        outState.putBooleanArray(CONTACT_TYPE, mContactCheckTypes);
        outState.putBooleanArray(MESSAGE_TYPE, mMessageCheckTypes);
    }

    private void restoreInstanceState(final Bundle savedInstanceState) {
        long array[] = savedInstanceState.getLongArray(SAVE_STATE_UNCHECKED_IDS);
        if (array != null) {
            for (long item : array) {
                Log.d(CLASS_TAG, "in restoreInstanceState mUnCheckedIds add " + item);
                mUnCheckedIds.add(item);
            }
        }
    }

    private void init() {

        this.bindService();
        initButton();
        initHandler();
        initLoadingView();
        createProgressDlg();

        initAdapter();
    }

    private ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this.getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(getString(R.string.backuping));
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelMessage(mHandler.obtainMessage(MessageID.PRESS_BACK));
        }
        return mProgressDialog;
    }

    private void bindService() {
        this.getActivity().bindService(new Intent(this.getActivity(), BackupService.class),
                mServiceCon, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mBackupService = (BackupBinder) service;
            if (mBackupService != null) {
                if (mBackupListener != null) {
                    mBackupService.setOnBackupChangedListner(mBackupListener);
                }
            }
            // checkBackupState();
            afterServiceConnected();
            Log.i(CLASS_TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBackupService = null;
            Log.i(CLASS_TAG, "onServiceDisconnected");
        }
    };

    protected void afterServiceConnected() {
        mBackupListener = new PersonalDataBackupStatusListener();
        setOnBackupStatusListener(mBackupListener);
        checkBackupState();
    }

    public class NomalBackupStatusListener implements OnBackupStatusListener {

        @Override
        public void onBackupEnd(final BackupResultType resultCode,
                final ArrayList<ResultEntity> resultRecord,
                final ArrayList<ResultEntity> appResultRecord) {
            // do nothing
        }

        @Override
        public void onBackupErr(final IOException e) {
            if (errChecked()) {
                if (mBackupService != null && mBackupService.getState() != State.INIT
                        && mBackupService.getState() != State.FINISH) {
                    mBackupService.pauseBackup();
                }
            }
        }

        @Override
        public void onComposerChanged(final Composer composer) {

        }

        @Override
        public void onProgressChanged(final Composer composer, final int progress) {
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.setProgress(progress);
                        }
                    }
                });
            }
        }
    }

    public class PersonalDataBackupStatusListener extends NomalBackupStatusListener {
        @Override
        public void onBackupEnd(final BackupResultType resultCode,
                final ArrayList<ResultEntity> resultRecord,
                final ArrayList<ResultEntity> appResultRecord) {
            RecordXmlInfo backupInfo = new RecordXmlInfo();
            backupInfo.setRestore(false);
            backupInfo.setDevice(Utils.getPhoneSearialNumber());
            backupInfo.setTime(String.valueOf(System.currentTimeMillis()));
            RecordXmlComposer xmlCompopser = new RecordXmlComposer();
            xmlCompopser.startCompose();
            xmlCompopser.addOneRecord(backupInfo);
            xmlCompopser.endCompose();
            if (mBackupFolderPath != null && !mBackupFolderPath.isEmpty()) {
                Utils.writeToFile(xmlCompopser.getXmlInfo(), mBackupFolderPath + File.separator
                        + Constants.RECORD_XML);
            }
            final BackupResultType iResultCode = resultCode;
            final ArrayList<ResultEntity> iResultRecord = resultRecord;
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showBackupResult(iResultCode, iResultRecord);
                    }
                });
            }
        }


        @Override
        public void onComposerChanged(final Composer composer) {
            if (composer == null) {
                MyLogger.logE(CLASS_TAG, "onComposerChanged: error[composer is null]");
                return;
            } else {
                MyLogger.logI(CLASS_TAG, "onComposerChanged: type = " + composer.getModuleType()
                        + "Max = " + composer.getCount());
            }
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String msg = getProgressDlgMessage(composer.getModuleType());
                        if (mProgressDialog != null) {
                            mProgressDialog.setMessage(msg);
                            mProgressDialog.setMax(composer.getCount());
                            mProgressDialog.setProgress(0);
                        }
                    }
                });
            }
        }
    }

    protected String getProgressDlgMessage(final int type) {
        StringBuilder builder = new StringBuilder(getString(R.string.backuping));
        builder.append("(");
        builder.append(ModuleType.getModuleStringFromType(this.getActivity(), type));
        builder.append(")");
        return builder.toString();
    }

    protected void showBackupResult(final BackupResultType result,
            final ArrayList<ResultEntity> resultRecord) {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (mCancelDlg != null && mCancelDlg.isShowing()) {
            mCancelDlg.dismiss();
        }

        if (result != BackupResultType.Cancel) {
            Bundle args = new Bundle();
            args.putParcelableArrayList(Constants.RESULT_KEY, resultRecord);
            ListAdapter adapter = ResultDialog.createResultAdapter(this.getActivity(), args);
            AlertDialog dialog = new AlertDialog.Builder(this.getActivity()).setCancelable(false)
                    .setTitle(R.string.backup_result)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int which) {
                            if (mBackupService != null) {
                                mBackupService.reset();
                            }
                            stopService();
                            mCallBack.onBackupFinish();
                            NotifyManager.getInstance(getActivity()).clearNotification();
                        }
                    }).setAdapter(adapter, null).create();
            dialog.show();
        } else {
            stopService();
        }
    }

    protected void stopService() {
        if (mBackupService != null) {
            mBackupService.reset();
        }
        this.getActivity().stopService(new Intent(this.getActivity(), BackupService.class));
    }

    LinearLayout loadingContent = null;
    private void initLoadingView() {
        // TODO Auto-generated method stub
        LayoutInflater inflater = LayoutInflater.from(this.getActivity());
        View view = inflater.inflate(R.layout.backup, null);
        loadingContent =  (LinearLayout) view.findViewById(R.id.loading_container);

    }

    private void initButton() {

        mDivider  = getView().findViewById(R.id.backup_divider);
        mDivider.setBackground(getListView().getDivider());
        mButtonBackup = (Button) getView().findViewById(R.id.backup_bt_backcup);
        mButtonBackup.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mBackupService == null || mBackupService.getState() != State.INIT) {
                    Log.e(CLASS_TAG,
                            "Can not to start. BackupService not ready or BackupService is ruuning");
                    return;
                }

                if (isAllChecked(false)) {
                    Log.e(CLASS_TAG, "to Backup List is null or empty");
                    return;
                }
                String path = SDCardUtils.getStoragePath();
                if (path != null) {
                    startBackup();
                } else {
                    // scard not available
                    showDialog(DialogID.DLG_NO_SDCARD, null);
                }
            }
        });

        mCheckBoxSelect = (CheckBox) getView().findViewById(R.id.backup_checkbox_select);
        mCheckBoxSelect.setChecked(true);
        mCheckBoxSelect.setVisibility(View.INVISIBLE);
        mCheckBoxSelect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Log.d(CLASS_TAG, "enter the onClick functions to set all checked or unchecked");
                if (isAllChecked(true)) {
                    setAllChecked(false);
                } else {
                    setAllChecked(true);
                }
                updateButtonState();
                mBackupListAdapter.notifyDataSetChanged();
            }
        });
    }

    public void setAllChecked(boolean checked) {

        mUnCheckedIds.clear();

        if (!checked) {
            ListAdapter adapter = getListAdapter();
            if (adapter != null) {
                int count = adapter.getCount();
                for (int position = 0; position < count; position++) {
                    long itemId = adapter.getItemId(position);
                    Log.d(CLASS_TAG, "in setAllChecked mUnCheckedIds add " + itemId);
                    mUnCheckedIds.add(itemId);
                }
            }
        } else {
            for (int i = 0; i < mDisabledIds.size(); i++) {
                Log.d(CLASS_TAG, "in setAllChecked seconded mUnCheckedIds add " + mDisabledIds.get(i));
                mUnCheckedIds.add(mDisabledIds.get(i));
            }
        }
        //notifyItemCheckChanged();
    }

    public void startBackup() {
        Log.v(CLASS_TAG, "startBackup");

        if (isSimCardSelected()) {
            if (isAirModeOn(this.getActivity())) {
                showAirModeOnDialog();
                return;
            }
            if (!checkSimLocked(null)) {
                showDialog(DialogID.DLG_EDIT_FOLDER_NAME, null);
                return;
            }
        } else {
            showDialog(DialogID.DLG_EDIT_FOLDER_NAME, null);
        }
    }

    private boolean checkSimLocked(SubInfoRecord simInfo) {
        // TODO Auto-generated method stub
        if (simInfo != null) {
            showSIMLockedDialog(simInfo);
            return true;
        } else {
            if (mSimInfoList != null) {
                MyLogger.logI(CLASS_TAG, "[mSimInfoList]===>mSimInfoList size = " + mSimInfoList.size());
                for (SubInfoRecord tsimInfo : mSimInfoList) {
                    int simId = (int) tsimInfo.subId;
                    if (isSIMLocked(simId) && isSimCardSelected(simId)) {
                        MyLogger.logD(CLASS_TAG, "[checkSimLocked]===> isSIMLocked " + simId);
                        checkSimLocked(tsimInfo);
                        simInfo = null;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showSIMLockedDialog(final SubInfoRecord simInfo) {
        // TODO Auto-generated method stub
        AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity())
        .setTitle(R.string.sim_locked_dialog_title)
        .setCancelable(true)
        .setMessage(R.string.sim_locked_dialog_message)
        .setPositiveButton(R.string.sim_locked_dialog_unlock, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startUnlockSim(simInfo.slotId);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SubInfoRecord nextSim = getNextSimCard(simInfo);
                MyLogger.logD(CLASS_TAG, "simInfo = " + simInfo.subId);
                if (nextSim != null) {
                    checkSimLocked(nextSim);
                } else {
                    showDialog(DialogID.DLG_EDIT_FOLDER_NAME, null);
                }
            }
        }).create();
        alertDialog.show();
        dialogs.add(alertDialog);
    }

    private SubInfoRecord getNextSimCard(SubInfoRecord info) {
        if (info == null) {
            return null;
        }
        getSimInfoList();
        int simID = (int) info.subId;
        if (simID > 0 && (mSimCount) > simID) {
            SubInfoRecord nextSIM = mSimInfoList.get(simID);
            return isSimCardSelected((int) nextSIM.subId) && isSIMLocked((int) nextSIM.subId)
                    ? nextSIM : null;
        }
        return null;

    }

    private void startUnlockSim(int slot) {
        Intent it = new Intent();
        it.setAction("com.android.phone.SetupUnlockPINLock");
        it.putExtra("PhoneConstants.GEMINI_SIM_ID_KEY", slot);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(it);
    }

    private boolean isSIMLocked(int simId) {
        TelephonyManagerEx mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        int statue = mTelephonyManagerEx.getSimState(simId - 1);
        MyLogger.logD(CLASS_TAG, "SIM card " + simId + " It's statue = " + statue);
        return false; // temp statue ==
                     // com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_LOCKED;
    }

    private boolean isSimCardSelected(int simID) {
         getSimInfoList();
         if (mSimCount >= 1) {
            for (int i = 1; i <= mSimCount; i++) {
                if (mContactCheckTypes[i] && (simID == i)) {
                    return true;
                }
            }
           }
         return false;
    }

    private void showAirModeOnDialog() {
        // TODO Auto-generated method stub
        AlertDialog airModeDialog = new AlertDialog.Builder(this.getActivity())
        .setTitle(R.string.air_mode_dialog_title)
        .setCancelable(true)
        .setMessage(R.string.air_mode_dialog_message)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //AIR MODE OFF
                setAirplaneMode(BackupTabFragment_27.this.getActivity(), true);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //continue backup
                showDialog(DialogID.DLG_EDIT_FOLDER_NAME, null);
            }
        }).create();
        airModeDialog.show();
        dialogs.add(airModeDialog);
    }

    public void setAirplaneMode(Context context, boolean enabling) {
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", false);
        context.sendBroadcast(intent);
    }

    private boolean isSimCardSelected() {
     getSimInfoList();
     if (mSimCount >= 1) {
            for (int i = 1; i <= mSimCount; i++) {
                if (mContactCheckTypes[i]) {
                    return true;
                }
            }
        }
     return false;
   }

    private boolean isAirModeOn(Context context) {
        // TODO Auto-generated method stub
        int result = 0;
        try {
            result = Settings.Global.getInt(this.getActivity().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            // TODO: handle exception
        }
        return result == 1;
    }

    protected final void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                case MessageID.PRESS_BACK:
                    if (mBackupService != null && mBackupService.getState() != State.INIT
                            && mBackupService.getState() != State.FINISH) {
                        mBackupService.pauseBackup();
                        showDialog(DialogID.DLG_CANCEL_CONFIRM, null);
                    }
                    break;
                default:
                    break;
                }
            }
        };
    }

    private ProgressDialog createCancelDlg() {
        if (mCancelDlg == null) {
            mCancelDlg = new ProgressDialog(this.getActivity());
            mCancelDlg.setMessage(getString(R.string.cancelling));
            mCancelDlg.setCancelable(false);
        }
        return mCancelDlg;
    }

    public void showDialog(final int dlgId, String str) {
        Log.i(CLASS_TAG, "begin to create alert dialog with dialog id");
        Dialog dialog = null;
        Log.d(CLASS_TAG, "the id of the dlg is " + dlgId);
        switch(dlgId) {
        case DialogID.DLG_CANCEL_CONFIRM:
            dialog = new AlertDialog.Builder(this.getActivity())
                    .setTitle(R.string.warning).setMessage(R.string.cancel_backup_confirm)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface arg0, final int arg1) {
                            if (mBackupService != null && mBackupService.getState() != State.INIT
                                    && mBackupService.getState() != State.FINISH) {
                                if (mCancelDlg == null) {
                                    mCancelDlg = createCancelDlg();
                                }
                                mCancelDlg.show();
                                mBackupService.cancelBackup();
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {

                            if (mBackupService != null && mBackupService.getState() == State.PAUSE) {
                                mBackupService.continueBackup();
                            }
                            if (mProgressDialog != null) {
                                mProgressDialog.show();
                            }
                        }
                    }).setCancelable(false).create();
            break;
        case DialogID.DLG_SDCARD_REMOVED:
            dialog = new AlertDialog.Builder(this.getActivity())
                    .setTitle(R.string.warning).setMessage(R.string.sdcard_removed)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            if (mBackupService != null && mBackupService.getState() == State.PAUSE) {
                                mBackupService.cancelBackup();
                            }
                        }

                    }).setCancelable(false).create();
            break;
        case DialogID.DLG_SDCARD_FULL:
            dialog = new AlertDialog.Builder(this.getActivity())
                    .setTitle(R.string.warning).setMessage(R.string.sdcard_is_full)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            if (mBackupService != null && mBackupService.getState() == State.PAUSE) {
                                mBackupService.cancelBackup();
                            }
                        }
                    }).setCancelable(false).create();
            break;
        case DialogID.DLG_NO_SDCARD:
            dialog = new AlertDialog.Builder(this.getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.notice)
                    .setMessage(SDCardUtils.getSDStatueMessage(this.getActivity()))
                    .setPositiveButton(android.R.string.ok, null).create();
            break;

        case DialogID.DLG_CREATE_FOLDER_FAILED:
            //String name = args.getString("name");
            String name = str;
            String msg = String.format(getString(R.string.create_folder_fail), name);
            dialog = new AlertDialog.Builder(this.getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.notice)
                    .setMessage(msg).setPositiveButton(android.R.string.ok, null).create();
            break;
        case DialogID.DLG_EDIT_FOLDER_NAME:
            dialog = createFolderEditorDialog();
        default:
            break;
        }
        dialog.show();
    }



private AlertDialog createFolderEditorDialog() {
        MyLogger.logD(CLASS_TAG, "createFolderEditorDialog [getStoragePath] is " + SDCardUtils.getStoragePath());
        LayoutInflater factory = LayoutInflater.from(this.getActivity());
        final View view = factory.inflate(R.layout.dialog_edit_folder_name, null);
        EditText editor = (EditText) view.findViewById(R.id.edit_folder_name);
        final AlertDialog dialog = new AlertDialog.Builder(this.getActivity())
                .setTitle(R.string.edit_folder_name).setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AlertDialog d = (AlertDialog) dialog;
                        EditText editor = (EditText) d.findViewById(R.id.edit_folder_name);
                        if (editor != null) {
                            CharSequence folderName = editor.getText();
                            String path = SDCardUtils.getStoragePath();
                            StringBuilder builder = new StringBuilder(path);
                            builder.append(File.separator);
                            builder.append(folderName);
                            mBackupFolderPath = builder.toString();
                            hideKeyboard(editor);
                            editor.setText("");
                            startPersonalDataBackup(mBackupFolderPath);
                        } else {
                            MyLogger.logE(CLASS_TAG, " can not get folder name");
                        }
                    }

                    private void hideKeyboard(EditText editor) {
                        // TODO Auto-generated method stub
                        InputMethodManager imm = ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE));
                        imm.hideSoftInputFromWindow(editor.getWindowToken(), 0);
                    }
                }).setNegativeButton(android.R.string.cancel, null).create();
            editor.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() <= 0 || s.toString().matches(".*[/\\\\:#*?\"<>|].*")
                        || s.toString().matches(" *\\.+ *") || s.toString().trim().length() == 0) { // characters
                    // not allowed
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
        return dialog;
    }


    private void startPersonalDataBackup(String folderName) {
        if (folderName == null || folderName.trim().equals("")) {
            return;
        }
        startService();
        if (mBackupService != null) {
            ArrayList<Integer> list = getSelectedItemList();
            mBackupService.setBackupModelList(list);
            if (list.contains(ModuleType.TYPE_CONTACT)) {
                ArrayList<String> params = new ArrayList<String>();
                String[] contactTypes = new String[] { ContactType.ALL, ContactType.PHONE,
                        ContactType.SIM1, ContactType.SIM2 , ContactType.SIM3 , ContactType.SIM4};
                if (mContactCheckTypes[0]) {
                    params.add(ContactType.PHONE);
                }
                if (mSimCount >= 1) {
                    for (int i = 1; i <= mSimCount; i++) {
                        if (mContactCheckTypes[i]) {
                            params.add(mSimInfoList.get(i - 1).displayName);
                            MyLogger.logD(CLASS_TAG, " displayName is "
                                    + mSimInfoList.get(i - 1).displayName);
                        }
                    }
                }
                mBackupService.setBackupItemParam(ModuleType.TYPE_CONTACT, params);
            }
            if (list.contains(ModuleType.TYPE_MESSAGE)) {
                ArrayList<String> params = new ArrayList<String>();
                if (mMessageCheckTypes[0]) {
                    params.add(Constants.ModulePath.NAME_SMS);
                }
                if (mMessageCheckTypes[1]) {
                    params.add(Constants.ModulePath.NAME_MMS);
                }
                mBackupService.setBackupItemParam(ModuleType.TYPE_MESSAGE, params);
            }
            boolean ret = mBackupService.startBackup(folderName);
            if (ret) {
                showProgress();
            } else {
                String path = SDCardUtils.getStoragePath();
                if (path == null) {
                    // no sdcard
                    Log.d(CLASS_TAG, "SDCard is removed");
                    ret = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(DialogID.DLG_SDCARD_REMOVED, null);
                        }
                    });
                } else if (SDCardUtils.getAvailableSize(path) <= SDCardUtils.MINIMUM_SIZE) {
                    // no space
                    Log.d(CLASS_TAG, "SDCard is full");
                    ret = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(DialogID.DLG_SDCARD_FULL, null);
                        }
                    });
                } else {
                    Log.e(CLASS_TAG, "unkown error");
                    /*Bundle b = new Bundle();
                    b.putString("name", folderName.substring(folderName.lastIndexOf('/') + 1));*/
                    String str = folderName.substring(folderName.lastIndexOf('/') + 1);
                    showDialog(DialogID.DLG_CREATE_FOLDER_FAILED, str);
                }
                stopService();
            }
        } else {
            stopService();
            MyLogger.logE(CLASS_TAG, "startPersonalDataBackup: error! service is null");
        }
    }

    protected void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.show();
    }


    protected void startService() {
        this.getActivity().startService(new Intent(this.getActivity(), BackupService.class));
    }


    private ArrayList<Integer> getSelectedItemList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        int count = getCount();
        for (int position = 0; position < count; position++) {
            PersonalItemData item = (PersonalItemData) getItemByPosition(position);
            if (isItemCheckedByPosition(position)) {
                list.add(item.getType());
            }
        }
        return list;
    }

    public Object getItemByPosition(int position) {
        ListAdapter adapter = getListAdapter();
        if (adapter == null) {
            Log.d(CLASS_TAG, "adapter is null, please check it");
            return null;
        }
        return adapter.getItem(position);
    }

    private void initAdapter() {
        Log.d(CLASS_TAG, "Begin to init the row layout");
        mBackupListAdapter = new PersonalDataBackupAdapter(this.getActivity(), mBackupItemDataList, R.layout.backup_personal_data_item);

        setListAdapter(mBackupListAdapter);
    }


    private void updateData(ArrayList<PersonalItemData> list) {
        mBackupItemDataList = list;
        mBackupListAdapter.changeData(mBackupItemDataList);
        syncUnCheckedItems();
        mBackupListAdapter.notifyDataSetChanged();
        //updateTitle();
        updateButtonState();
        checkBackupState();
    }

    protected void syncUnCheckedItems() {
        ListAdapter adapter = getListAdapter();
        if (adapter == null) {
            mUnCheckedIds.clear();
        } else {
            ArrayList<Long> list = new ArrayList<Long>();
            int count = adapter.getCount();
            Log.d(CLASS_TAG, "item count in the list is " + mUnCheckedIds.size());
            for (int position = 0; position < count; position++) {
                long itemId = adapter.getItemId(position);
                if (mUnCheckedIds.contains(itemId)) {
                    list.add(itemId);
                }
            }
            mUnCheckedIds.clear();
            mUnCheckedIds = list;
        }
    }

    protected void updateButtonState() {
        mCheckBoxSelect.setVisibility(View.VISIBLE);
        mCheckBoxSelect.setText(this.getActivity().getApplication().getResources().getString(R.string.selectall));
        mDivider.setVisibility(View.VISIBLE);
        if (isAllChecked(false)) {
            mButtonBackup.setEnabled(false);
            mCheckBoxSelect.setChecked(false);
        } else {
            mButtonBackup.setEnabled(true);
            mCheckBoxSelect.setChecked(isAllChecked(true));
        }
    }

    public boolean isAllChecked(boolean checked) {

        boolean ret = true;
        if (checked) {
            // is it all checked
            if (getUnCheckedCount() - getDisabledCount() > 0) {
                ret = false;
            }
        } else {
            // is it all unchecked
            if (getCheckedCount() > 0) {
                ret = false;
            }
        }
        return ret;
    }

    public int getUnCheckedCount() {
        return mUnCheckedIds.size();
    }


    public int getDisabledCount() {
        return mDisabledIds.size();
    }

    public int getCount() {
        int count = 0;
        ListAdapter adapter = getListAdapter();
        if (adapter != null) {
            count = adapter.getCount();
        }
        return count;
    }

    public int getCheckedCount() {
        return getCount() - getUnCheckedCount();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.v(CLASS_TAG, "onListItemClick enter");
        super.onListItemClick(l, v, position, id);

        String item = (String) l.getAdapter().getItem(position);
        Log.v(CLASS_TAG, "onLIstItemClick: item is " + item);

        revertItemCheckedByPosition(position);
    }


    private int getModulesCount(Composer... composers) {
        int count = 0;
        for (Composer composer: composers) {
            if (composer.init()) {
                count += composer.getCount();
                composer.onEnd();
            }
        }
        return count;
    }

    public boolean isItemCheckedByPosition(int position) {
        boolean ret = true;
        ListAdapter adapter = getListAdapter();
        if (adapter != null) {
            long itemId = adapter.getItemId(position);
            ret = isItemCheckedById(itemId);
        }
        return ret;
    }

    public boolean isItemCheckedById(long id) {
        boolean ret = true;

        if (mUnCheckedIds != null && mUnCheckedIds.contains(id)) {
            ret = false;
        }
        return ret;
    }

    private int getContactTypeNumber() {
        int count = (mSimCount + 1) < mContactCheckTypes.length ? (mSimCount + 1)
                : mContactCheckTypes.length;
        return count;
    }

    private boolean isAllValued(boolean[] array, int count, boolean value) {
        boolean ret = true;
        for (int position = 0; position < count; position++) {
            if (array[position] != value) {
                ret = false;
                break;
            }
        }
        return ret;
    }


    public void setItemCheckedByPosition(int position, boolean checked) {
        ListAdapter adapter = getListAdapter();
        if (adapter != null && mUnCheckedIds != null) {
            long itemId = adapter.getItemId(position);
            if (checked) {
                mUnCheckedIds.remove(itemId);
            } else {
                if (!mUnCheckedIds.contains(itemId)) {
                    Log.d(CLASS_TAG, "in setItemCheckedByPosition mUnCheckedIds add " + itemId);
                    mUnCheckedIds.add(itemId);
                }
            }
        }
    }


    private void getSimInfoList() {
        mSimInfoList = SubscriptionManager.getActiveSubInfoList();
        if (mSimInfoList != null) {
            for (SubInfoRecord simInfo : mSimInfoList) {
                MyLogger.logD(CLASS_TAG, "sim id  = " + simInfo.subId + ", name = "
                        + simInfo.displayName + ", slot = " + simInfo.slotId);
            }
        } else {
            MyLogger.logD(CLASS_TAG, "No SIM inserted!");
        }
        if (mSimInfoList != null) {
            mSimCount = mSimInfoList.isEmpty() ? 0 : mSimInfoList.size();
        }
    }

    private void showContactConfigDialog() {
        final String[] select;
        final boolean[] temp = new boolean[mContactCheckTypes.length];
        for (int index = 0; index < mContactCheckTypes.length; index++) {
            temp[index] = mContactCheckTypes[index];
        }

        switch (mSimCount) {
            case 1:
                select = new String[] {
                        getString(R.string.contact_phone),
                        mSimInfoList.get(0).displayName
                };
                break;

            case 2:
                select = new String[] {
                        getString(R.string.contact_phone),
                        mSimInfoList.get(0).displayName, mSimInfoList.get(1).displayName
                };
                break;
            case 3:
                select = new String[] {
                        getString(R.string.contact_phone),
                        mSimInfoList.get(0).displayName, mSimInfoList.get(1).displayName,
                        mSimInfoList.get(2).displayName
                };
                break;
            case 4:
                select = new String[] {
                        getString(R.string.contact_phone),
                        mSimInfoList.get(0).displayName, mSimInfoList.get(1).displayName,
                        mSimInfoList.get(2).displayName,
                        mSimInfoList.get(3).displayName
                };
                break;
            default:
                select = new String[] {
                    getString(R.string.contact_phone)
                };
                break;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity())
                .setTitle(R.string.contact_module)
                .setCancelable(true)
                .setMultiChoiceItems(select, temp,
                        new DialogInterface.OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                MyLogger.logD(CLASS_TAG, "DialogID.DLG_CONTACT_CONFIG: the number "
                                        + which + " is checked(" + isChecked + ")");
                                AlertDialog d = (AlertDialog) dialog;
                                int count = mSimCount + 1;

                                if (isAllValued(temp, count, false)) {
                                    d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                } else {
                                    d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                }
                            }
                        })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean empty = true;
                        for (int index = 0; index < mContactCheckTypes.length; index++) {
                            mContactCheckTypes[index] = temp[index];
                        }
                        int count = getContactTypeNumber();
                        for (int index = 0; index < count; index++) {
                            if (mContactCheckTypes[index]) {
                                empty = false;
                                break;
                            }
                        }
                        if (empty) {
                            setItemCheckedByPosition(0, false);
                        } else {
                            setItemCheckedByPosition(0, true);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).create();
        alertDialog.show();
    }


    private void showMessageConfigDialog() {
        for (String string : messageEnable) {
            MyLogger.logE(CLASS_TAG, "messageEnable = " + string);
        }
        final String[] select =  (String[]) messageEnable.toArray(new String[messageEnable.size()]);
        final boolean[] temp = new boolean[2];
        for (int index = 0; index < 2; index++) {
            temp[index] = mMessageCheckTypes[index];
        }
        AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity())
        .setTitle(R.string.message_module)
        .setCancelable(true)
        .setMultiChoiceItems(select, temp,
            new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    MyLogger.logD(CLASS_TAG, "DialogID.DLG_CONTACT_CONFIG: the number "
                            + which + " is checked(" + isChecked + ")");
                    AlertDialog d = (AlertDialog) dialog;
                    int count = select.length;

                    if (isAllValued(temp, count, false)) {
                        d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    } else {
                        d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
        })
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int index = 0; index < 2; index++) {
                    mMessageCheckTypes[index] = temp[index];
                }
            }
        }).setNegativeButton(android.R.string.cancel, null).create();
        alertDialog.show();
    }

    public void revertItemCheckedByPosition(int position) {
        boolean checked = isItemCheckedByPosition(position);
        Log.d(CLASS_TAG, "the checked status of the checkbox is " + checked + " for " + getListAdapter().getItemId(position));
        setItemCheckedByPosition(position, !checked);
    }

    private class PersonalDataBackupAdapter extends BaseAdapter {
        private ArrayList<PersonalItemData> mDataList;
        private int mLayoutId;
        private LayoutInflater mInflater;

        public PersonalDataBackupAdapter(Context context, ArrayList<PersonalItemData> list,
                int resource) {
            Log.d(CLASS_TAG, "enter the constructor of PersonalDataBackupAdapter");


            mDataList = list;
            mLayoutId = resource;
            mInflater = LayoutInflater.from(context);
        }

        public void changeData(ArrayList<PersonalItemData> list) {
             mDataList = list;
        }

        public void reset() {
            mDataList = null;
        }

        @Override
        public int getCount() {
            return mDataList.size();
        }

        @Override
        public Object getItem(final int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return mDataList.get(position).getType();
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            Log.d(CLASS_TAG, "enter getView function");
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(mLayoutId, parent, false);
            }

            final PersonalItemData item = mDataList.get(position);
            final View content = view.findViewById(R.id.item_content);
            final View config = view.findViewById(R.id.item_config);
            final ImageView imgView = (ImageView) view.findViewById(R.id.item_image);
            final TextView textView = (TextView) view.findViewById(R.id.item_text);
            final CheckBox chxbox = (CheckBox) view.findViewById(R.id.item_checkbox);

            boolean bEnabled = item.isEnable();
            imgView.setEnabled(bEnabled);
            textView.setEnabled(bEnabled);
            content.setAlpha(bEnabled ? ENABLE_ALPHA : DISABLE_ALPHA);
            chxbox.setEnabled(bEnabled);
            if (item.getType() == ModuleType.TYPE_CONTACT) {
                boolean isChecked = isItemCheckedByPosition(position);

                float alpha = isChecked ? ENABLE_ALPHA : DISABLE_ALPHA;
                config.setEnabled(isChecked);
                config.setAlpha(alpha);
                config.setVisibility(View.VISIBLE);
                config.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        getSimInfoList();
                        showContactConfigDialog();
                    }
                });
            } else if (item.getType() == ModuleType.TYPE_MESSAGE) {
                boolean isChecked = isItemCheckedByPosition(position);

                float alpha = isChecked ? ENABLE_ALPHA : DISABLE_ALPHA;
                config.setEnabled(isChecked);
                config.setAlpha(alpha);
                config.setVisibility(View.VISIBLE);
                config.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        showMessageConfigDialog();
                    }
                });
            } else {
                config.setVisibility(View.GONE);
                config.setOnClickListener(null);
            }

            content.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Log.v(CLASS_TAG, "Click the item on position : " + position);
                    if (chxbox.isEnabled()) {
                        Log.v(CLASS_TAG, "the check box on position : " + position + "is enable");
                        revertItemCheckedByPosition(position);
                        chxbox.setChecked(isItemCheckedByPosition(position));
                    }
                    else {
                        Log.v(CLASS_TAG, "the check box on position : " + position + "is disable");
                    }
                }
            });


            if (!bEnabled) {
                Log.v(CLASS_TAG, "the check box satus is set to false because the it is diable");
                chxbox.setChecked(false);
            }

            long id = getItemId(position);
            setItemDisabledById(id, !bEnabled);
            imgView.setImageResource(item.getIconId());
            textView.setText(item.getTextId());


            if (isItemCheckedByPosition(position)) {
                if (chxbox.isEnabled()) {
                    Log.d(CLASS_TAG, "the checkbox status of the position :" + position + "is checked");
                    chxbox.setChecked(true);
                }
                else {
                    Log.d(CLASS_TAG, "this is not enable");
                }
            } else {
                Log.d(CLASS_TAG, "the checkbox status of the position :" + position + "is unchecked");
                if (chxbox.isEnabled()) {
                    chxbox.setChecked(false);
                }
            }

            return view;
        }
    }


    public void setItemDisabledById(long id, boolean bDisabled) {
        if (mDisabledIds == null || mUnCheckedIds == null) {
            return;
        }
        if (!bDisabled) {
            mDisabledIds.remove(id);
        } else {
            if (!mDisabledIds.contains(id)) {
                mDisabledIds.add(id);
            }
            if (!mUnCheckedIds.contains(id)) {
                Log.d(CLASS_TAG, "in setItemDisabledById mUnCheckedIds add " + id);
                mUnCheckedIds.add(id);
            }
        }
        //notifyItemCheckChanged();
    }

    protected void checkBackupState() {
        if (mBackupService != null) {
            int state = mBackupService.getState();
            switch (state) {
            case State.ERR_HAPPEN:
                errChecked();
                break;
            default:
                break;
            }
        }
    }

    protected boolean errChecked() {
        boolean ret = false;
        String path = SDCardUtils.getStoragePath();
        if (path == null) {
            // no sdcard
            Log.d(CLASS_TAG, "SDCard is removed");
            ret = true;
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BackupTabFragment_27.this.getActivity().showDialog(DialogID.DLG_SDCARD_REMOVED);
                    }
                });
            }
        } else if (SDCardUtils.getAvailableSize(path) <= SDCardUtils.MINIMUM_SIZE) {
            // no space
            Log.d(CLASS_TAG, "SDCard is full");
            ret = true;
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BackupTabFragment_27.this.getActivity().showDialog(DialogID.DLG_SDCARD_FULL);
                    }
                });
            }
        } else {
            Log.e(CLASS_TAG, "unkown error");
        }
        return ret;
    }

    protected void setButtonsEnable(boolean enable) {
        MyLogger.logD(CLASS_TAG, "setButtonsEnable - " + enable);
        if (mButtonBackup != null) {
            mButtonBackup.setEnabled(enable);
        }
        if (mCheckBoxSelect != null) {
            mCheckBoxSelect.setEnabled(enable);
        }
    }

    public void setOnBackupStatusListener(OnBackupStatusListener listener) {
        mBackupListener = listener;
        if (mBackupListener != null && mBackupService != null) {
            mBackupService.setOnBackupChangedListner(mBackupListener);
        }
    }

    private class InitPersonalDataTask extends AsyncTask<Void, Void, Long> {
        private static final String TASK_TAG = "InitPersonalDataTask";
        ArrayList<PersonalItemData> mBackupDataList;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MyLogger.logD(CLASS_TAG, TASK_TAG + "---onPreExecute");
            showLoadingContent(true);
            setButtonsEnable(false);
        }


        @Override
        protected void onPostExecute(Long arg0) {
            showLoadingContent(false);
            setButtonsEnable(true);
            updateData(mBackupDataList);
            setOnBackupStatusListener(mBackupListener);
            super.onPostExecute(arg0);
        }

        @Override
        protected Long doInBackground(Void... arg0) {
            messageEnable.clear();
            mBackupDataList = new ArrayList<PersonalItemData>();
            int types[] = new int[] {
                    ModuleType.TYPE_CONTACT,
                    ModuleType.TYPE_MESSAGE,
                    ModuleType.TYPE_CALLLOG,
                    ModuleType.TYPE_CALENDAR,
                    };

            int num = types.length;
            for (int i = 0; i < num; i++) {
                boolean bEnabled = true;
                int count = 0;
                Composer composer;
                switch (types[i]) {
                case ModuleType.TYPE_CONTACT:
                    count = getModulesCount(new ContactBackupComposer(BackupTabFragment_27.this.getActivity()));
                    break;
                case ModuleType.TYPE_MESSAGE:
                    int countSMS = 0;
                    int countMMS = 0;
                    composer = new SmsBackupComposer(BackupTabFragment_27.this.getActivity());
                    if (composer.init()) {
                        countSMS = composer.getCount();
                        composer.onEnd();
                    }
                    if (countSMS != 0) {
                        messageEnable.add(getString(R.string.message_sms));
                    }
                    composer = new MmsBackupComposer(BackupTabFragment_27.this.getActivity());
                    if (composer.init()) {
                        countMMS = composer.getCount();
                        composer.onEnd();
                    }
                    count = countSMS + countMMS;
                    MyLogger.logE(CLASS_TAG, "countSMS = " + countSMS + " countMMS " + countMMS);
                    if (countMMS != 0) {
                        messageEnable.add(getString(R.string.message_mms));
                    }
                    break;
                case ModuleType.TYPE_CALLLOG:
                    count = getModulesCount(new CalllogBackupComposer(BackupTabFragment_27.this.getActivity()));
                    break;
                case ModuleType.TYPE_CALENDAR:
                    count = getModulesCount(new CalendarBackupComposer(BackupTabFragment_27.this.getActivity()));
                    break;
                default:
                    break;
                }
                composer = null;
                bEnabled = !(count == 0);
                PersonalItemData item = new PersonalItemData(types[i], bEnabled);
                mBackupDataList.add(item);
            }
            return null;
        }
    }

    protected void showLoadingContent(boolean show) {

        Log.i(CLASS_TAG, "show loading content");

        if (mCaView == null && mCtView == null) {
            Log.i(CLASS_TAG, "[showLoadingContent] mCaView and mCtView == null");
            return;
        }

        mCaView.setVisibility(show ? View.VISIBLE : View.GONE);
        mCtView.setVisibility(!show ? View.VISIBLE : View.GONE);
    }


}

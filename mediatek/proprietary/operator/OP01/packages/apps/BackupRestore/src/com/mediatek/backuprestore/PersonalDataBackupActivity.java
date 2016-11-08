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

package com.mediatek.backuprestore;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreProgress;
import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreResultType;
import com.mediatek.backuprestore.CheckedListActivity.OnCheckedCountChangedListener;
import com.mediatek.backuprestore.CheckedListActivity.OnUnCheckedChangedListener;
import com.mediatek.backuprestore.ResultDialog.ResultEntity;
import com.mediatek.backuprestore.modules.Composer;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.Constants.ContactType;
import com.mediatek.backuprestore.utils.Constants.DialogID;
import com.mediatek.backuprestore.utils.Constants.State;
import com.mediatek.backuprestore.utils.FileUtils;
import com.mediatek.backuprestore.utils.ModuleType;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PersonalDataBackupActivity extends AbstractBackupActivity implements
        OnCheckedCountChangedListener, OnUnCheckedChangedListener {
    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/PersonalDataBackupActivity";
    private ArrayList<PersonalItemData> mBackupItemDataList = new ArrayList<PersonalItemData>();
    private PersonalDataBackupAdapter mBackupListAdapter;
    private boolean[] mContactCheckTypes = new boolean[3];
    private static final String CONTACT_TYPE = "contact";
    private List<Composer> mComposerList;
    // /M add for simcard info display
    private ArrayList<ContactItemData> mContactItemDataList;
    private ContactItemData mContactItemData;
    private AlertDialog alertDialog;
    private SimInfoAdapter mSimInfoAdapter;
    private boolean[] mContactCheckedTemp;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mContactCheckTypes = savedInstanceState.getBooleanArray(CONTACT_TYPE);
        } else {
            restoreInstanceStateFromBackupTab();
        }
        Log.i(CLASS_TAG, "onCreate");
        init();
    }

    private void restoreInstanceStateFromBackupTab() {
        Intent intent = getIntent();
        Bundle mBundle = intent.getExtras();
        if (mBundle != null) {
            if (mBundle.getBooleanArray("contactType") == null) {
                initContactCheckTypes();
            } else {
                mContactCheckTypes = mBundle.getBooleanArray("contactType");
            }
        } else {
            initContactCheckTypes();
        }
        Log.i(CLASS_TAG, "restoreInstanceStateFromBackupTab  add mContactCheckTypes");
    }

    private void initContactCheckTypes() {
        for (int index = 0; index < 3; index++) {
            mContactCheckTypes[index] = true;
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(CONTACT_TYPE, mContactCheckTypes);
    }

    @Override
    protected void onResume() {
        initCheckStatus(false);
        super.onResume();
        Log.i(CLASS_TAG, "onResume");
    }

    private void init() {
        initActionBar();
        // do in background ... updateTitle
        // for new feature UI modify
        updateTitle();
        updateButtonState();
        getSimInfoList();
        setRequestCode(Constants.RESULT_PERSON_DATA);
    }

    public void updateTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.backup_personal_data));
        int totalNum = getCount();
        int checkedNum = getCheckedCount();
        Log.i(CLASS_TAG, "updateTitle totalNum = " + totalNum + ",  checkedNum = " + checkedNum);
        sb.append("(" + checkedNum + "/" + totalNum + ")");
        this.setTitle(sb.toString());
        if (checkedNum == 0) {
            updateButtonState();
        }
    }

    private void initActionBar() {
        ActionBar bar = this.getActionBar();
        bar.setDisplayShowHomeEnabled(false);
    }

    @Override
    public void startPersonalDataBackup(String folderName) {
        Log.v(CLASS_TAG, "startPersonalDataBackup, contactType = " + mContactCheckTypes);
        if (mBackupService != null) {
            ArrayList<Integer> list = getSelectedItemList();
            if (list == null || list.size() == 0) {
                MyLogger.logE(CLASS_TAG, "Error: no item to backup");
                return;
            }
            startService();
            mBackupService.setBackupModelList(list);
            if (list.contains(ModuleType.TYPE_CONTACT)) {
                ArrayList<String> params = new ArrayList<String>();
                if (mContactCheckTypes[0]) {
                    params.add(ContactType.PHONE);
                }
                if (mContactCheckTypes.length > 1 && mContactCheckTypes[1] && mSimCount >= 1) {
                    params.add(Integer.toString(mSimInfoList.get(0).getSimSlotIndex()));
                }
                if (mContactCheckTypes.length > 2 && mContactCheckTypes[2] && mSimCount == 2) {
                    params.add(Integer.toString(mSimInfoList.get(1).getSimSlotIndex()));
                }
                mBackupService.setBackupItemParam(ModuleType.TYPE_CONTACT, params);
            }
            boolean ret = mBackupService.startBackup(folderName);
            if (ret) {
                showProgress();
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setMessage(getString(R.string.backuping));
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog.setProgressPercentFormat(null);
            } else {
                String path = StorageSettingsActivity.getCurrentPath(this);
                if (!SDCardUtils.checkedPath(path)) {
                    // no sdcard
                    Log.d(CLASS_TAG, "SDCard is removed");
                    ret = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(DialogID.DLG_SDCARD_REMOVED);
                        }
                    });
                } else if (SDCardUtils.getAvailableSize(path) <= SDCardUtils.MINIMUM_SIZE) {
                    // no space
                    Log.d(CLASS_TAG, "SDCard is full");
                    ret = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(DialogID.DLG_SDCARD_FULL);
                        }
                    });
                } else {
                    Log.e(CLASS_TAG, "unkown error");
                    Bundle b = new Bundle();
                    b.putString("name", folderName.substring(folderName.lastIndexOf('/') + 1));
                    showDialog(DialogID.DLG_CREATE_FOLDER_FAILED, b);
                }

                stopService();
            }
        } else {
            stopService();
            MyLogger.logE(CLASS_TAG, "startPersonalDataBackup: error! service is null");
        }
    }

    @Override
    public void startBackup() {
        Log.v(CLASS_TAG, "startBackup");
        showDialog(DialogID.DLG_EDIT_FOLDER_NAME);
    }

    protected void afterServiceConnected() {
        setHandler(mHandler);
        checkBackupState();
    }

    private ArrayList<Integer> getSelectedItemList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        int count = getCount();
        for (int position = 0; position < count; position++) {
            PersonalItemData item = (PersonalItemData) getItemByPosition(position);
            if (isItemCheckedByPosition(position) && item != null) {
                list.add(item.getType());
            }
        }
        return list;
    }

    public BaseAdapter initBackupAdapter() {
        mBackupItemDataList = new ArrayList<PersonalItemData>();
        int types[] = getResources().getIntArray(R.array.module_type);
        int num = types.length;

        for (int i = 0; i < num; i++) {
            PersonalItemData item = new PersonalItemData(types[i], true);
            mBackupItemDataList.add(item);
        }

        mBackupListAdapter = new PersonalDataBackupAdapter(this, mBackupItemDataList,
                R.layout.backup_personal_data_item);
        return mBackupListAdapter;
    }

    @Override
    public void onCheckedCountChanged() {
        super.onCheckedCountChanged();
        updateTitle();
    }

    protected void showBackupResult(final BackupRestoreResultType result,
            final ArrayList<ResultEntity> list) {
        Bundle args = new Bundle();
        MyLogger.logD(CLASS_TAG,
                "showBackupResult list size = " + ((list == null) ? null : list.size()));
        args.putParcelableArrayList(Constants.RESULT_KEY, list);
        ListAdapter adapter = ResultDialog.createResultAdapter(this, args);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.backup_result)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                                if (!SDCardUtils.isSupprotSDcard(getApplicationContext())
                                        && !StorageSettingsActivity.getNoticeStatus(
                                                getApplicationContext(),
                                                Constants.NOSDCARD_CHANGE_NOTICE)) {
                                    showNotice();
                                }
                    }
                }).setAdapter(adapter, null).create();
        dialog.show();
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

    private void initContactConfig() {
        MyLogger.logD(CLASS_TAG, "initContactConfig ");
        mContactCheckedTemp = new boolean[mContactCheckTypes.length];
        SubscriptionInfo mSIMInfo = null;
        for (int index = 0; index < mContactCheckTypes.length; index++) {
            mContactCheckedTemp[index] = mContactCheckTypes[index];
        }
        mContactItemDataList = new ArrayList<ContactItemData>();
        mContactItemData = new ContactItemData(-1, mContactCheckedTemp[0],
                getString(R.string.contact_phone), this.getResources().getDrawable(
                        R.drawable.contact_phone_storage));
        mContactItemDataList.add(mContactItemData);
        if (mSimInfoList != null) {
            for (int i = 0; i < mSimInfoList.size(); i++) {
                SubscriptionInfo simInfo = mSimInfoList.get(i);
                if (simInfo != null) {
                    Drawable simIcon = new BitmapDrawable(this.getResources(),
                            simInfo.createIconBitmap(PersonalDataBackupActivity.this));
                    mContactItemData = new ContactItemData(simInfo.getSimSlotIndex(),
                            mContactCheckedTemp[i + 1], simInfo.getDisplayName().toString(),
                            simIcon);
                    MyLogger.logD(CLASS_TAG, "initContactConfig : mContactCheckedTemp[i + 1] = "
                            + mContactCheckedTemp[i + 1] + "sim mNumber  = " + simInfo.getNumber()
                            + ", name = " + simInfo.getDisplayName().toString() + ", slot = "
                            + simInfo.getSimSlotIndex());
                    mContactItemDataList.add(mContactItemData);
                }
            }
        }
        mSimInfoAdapter = new SimInfoAdapter(this, mContactItemDataList, mContactCheckedTemp);
        MyLogger.logD(CLASS_TAG, "initContactConfig mSimInfoAdapter = " + mSimInfoAdapter);
    }

    private void showContactConfigDialog() {
        initContactConfig();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.contact_module)
                .setCancelable(false)
                .setAdapter(mSimInfoAdapter, null)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean empty = true;
                        for (int index = 0; index < mContactCheckedTemp.length; index++) {
                            mContactCheckTypes[index] = mContactCheckedTemp[index];
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

                        MyLogger.logD(CLASS_TAG, "positive2  mContactCheckTypes.length =  "
                                + mContactCheckTypes.length);
                        // / M: save mContactCheckTypes for result activity
                        setContactCheckTypes(mContactCheckTypes);
                        if (mContactCheckedTemp != null) {
                            mContactCheckedTemp = null;
                        }
                        isShowContactDialog = false;
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        if (mContactCheckedTemp != null) {
                            mContactCheckedTemp = null;
                        }
                        isShowContactDialog = false;
                    }
                }).create();
        alertDialog.show();
        checkContactItem();
    }

    @Override
    protected Dialog onCreateDialog(final int id, final Bundle args) {
        Dialog dialog = null;
        switch (id) {
        // input backup file name
        case DialogID.DLG_EDIT_FOLDER_NAME:
            dialog = createFolderEditorDialog();
            break;

        case DialogID.DLG_BACKUP_CONFIRM_OVERWRITE:
            dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.notice).setMessage(R.string.backup_confirm_overwrite_notice)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MyLogger.logI(CLASS_TAG, " to backup");
                            File folder = new File(mBackupFolder);
                            File[] files = folder.listFiles();
                            if (files != null && files.length > 0) {
                                DeleteFolderTask task = new DeleteFolderTask();
                                task.execute(files);
                            } else {
                                if (checkedStartBackup()) {
                                    startPersonalDataBackup(mBackupFolder);
                                }
                            }
                        }
                    })
                    // .setCancelable(false)
                    .create();
            break;
        default:
            dialog = super.onCreateDialog(id, args);
            break;
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(final int id, final Dialog dialog, final Bundle args) {
        switch (id) {
        case DialogID.DLG_EDIT_FOLDER_NAME:
            EditText editor = (EditText) dialog.findViewById(R.id.edit_folder_name);
            if (editor != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String dateString = dateFormat.format(new Date(System.currentTimeMillis()));
                editor.setText(dateString);
                editor.setSelection(dateString.length());
            }
            break;
        default:
            super.onPrepareDialog(id, dialog, args);
            break;
        }
    }

    private AlertDialog createFolderEditorDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.dialog_edit_folder_name, null);
        EditText editor = (EditText) view.findViewById(R.id.edit_folder_name);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_folder_name).setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AlertDialog d = (AlertDialog) dialog;
                        EditText editor = (EditText) d.findViewById(R.id.edit_folder_name);
                        if (editor != null) {
                            String folderName = editor.getText().toString().trim();
                            String path = SDCardUtils
                                    .getPersonalDataBackupPath(StorageSettingsActivity
                                            .getCurrentPath(getApplicationContext()));
                            if (path == null) {
                                Toast.makeText(PersonalDataBackupActivity.this,
                                        R.string.sdcard_removed, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            StringBuilder builder = new StringBuilder(path);
                            builder.append(File.separator);
                            builder.append(folderName);
                            MyLogger.logE(CLASS_TAG, "backup folder is " + builder.toString());
                            mBackupFolder = builder.toString();
                            File folder = new File(mBackupFolder);
                            File[] files = null;
                            if (folder != null && folder.exists()) {
                                files = folder.listFiles();
                            } else {

                            }

                            if (files != null && files.length > 0) {
                                showDialog(DialogID.DLG_BACKUP_CONFIRM_OVERWRITE);
                            } else {
                                if (checkedStartBackup()) {
                                    startPersonalDataBackup(mBackupFolder);
                                }
                            }
                        } else {
                            MyLogger.logE(CLASS_TAG, " can not get folder name");
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).create();
        editor.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputName = s.toString().trim();
                if (inputName.length() <= 0 || inputName.matches(".*[/\\\\:*?\"<>|].*")) {
                    // characters not allowed
                    if (inputName.matches(".*[/\\\\:*?\"<>|].*")) {
                        Toast invalid = Toast.makeText(getApplicationContext(),
                                R.string.invalid_char_prompt, Toast.LENGTH_SHORT);
                        invalid.show();
                    }
                    Button botton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (botton != null) {
                        botton.setEnabled(false);
                    }
                } else {
                    Button botton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (botton != null) {
                        botton.setEnabled(true);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return dialog;
    }

    protected String getProgressDlgMessage(final int type) {
        StringBuilder builder = new StringBuilder(getString(R.string.backuping));
        builder.append("(");
        builder.append(ModuleType.getModuleStringFromType(this, type));
        builder.append(")");
        return builder.toString();
    }

    @Override
    protected void checkBackupState() {
        if (mBackupService != null) {
            int state = mBackupService.getState();
            switch (state) {
            case State.RUNNING:
                /* fall through */
            case State.PAUSE:
                BackupRestoreProgress p = mBackupService.getCurrentProgress();
                Log.e(CLASS_TAG, "checkBackupState: Max = " + p.mMax + " curprogress = "
                        + p.mCurNum);
                if (state == State.RUNNING) {
                    mProgressDialog.show();
                }
                if (p.mCurNum < p.mMax) {
                    String msg = getProgressDlgMessage(p.mType);
                    if (mProgressDialog != null) {
                        mProgressDialog.setMessage(msg);
                    }
                }
                if (mProgressDialog != null) {
                    mProgressDialog.setMax(p.mMax);
                    mProgressDialog.setProgress(p.mCurNum);
                }
                break;
            case State.FINISH:
                showBackupResult(mBackupService.getResultType(), mBackupService.getResult());
                break;
            default:
                super.checkBackupState();
                break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(CLASS_TAG, "onConfigurationChanged");
    }

    List<SubscriptionInfo> mSimInfoList;
    int mSimCount = 0;

    private void getSimInfoList() {
        mSimInfoList = new SubscriptionManager(PersonalDataBackupActivity.this)
                .getActiveSubscriptionInfoList();
        if (mSimInfoList == null) {
            MyLogger.logD(CLASS_TAG, "mSimInfoList == null!");
            return;
        }
        mSimCount = mSimInfoList.isEmpty() ? 0 : mSimInfoList.size();
        if (mSimInfoList.size() > 1) {
            Collections.sort(mSimInfoList, new Comparator<SubscriptionInfo>() {
                public int compare(SubscriptionInfo object1, SubscriptionInfo object2) {
                    int left = object1.getSimSlotIndex();
                    int right = object2.getSimSlotIndex();
                    if (left < right) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        }
        MyLogger.logD(CLASS_TAG, "SIM count = " + mSimCount);
    }

    private boolean isShowContactDialog = false;

    private class PersonalDataBackupAdapter extends BaseAdapter {
        private ArrayList<PersonalItemData> mDataList;
        private int mLayoutId;
        private LayoutInflater mInflater;

        public PersonalDataBackupAdapter(Context context, ArrayList<PersonalItemData> list,
                int resource) {
            mDataList = list;
            mLayoutId = resource;
            mInflater = LayoutInflater.from(context);
        }

        public void changeData(ArrayList<PersonalItemData> list) {
            mDataList = list;
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

            if (config != null) {
                if (item.getType() == ModuleType.TYPE_CONTACT) {
                    boolean isChecked = isItemCheckedByPosition(position);
                    MyLogger.logD(CLASS_TAG, "contact config: positon + " + position
                            + " is checked: " + isChecked);
                    float alpha = isChecked ? Constants.ENABLE_ALPHA : Constants.DISABLE_ALPHA;
                    config.setEnabled(isChecked);
                    config.setAlpha(alpha);
                    config.setVisibility(View.VISIBLE);
                    config.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            // contact config click
                            if (!isShowContactDialog) {
                                isShowContactDialog = true;
                                showContactConfigDialog();
                            }
                        }
                    });
                } else {
                    config.setVisibility(View.GONE);
                    config.setOnClickListener(null);
                }
            }
            content.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (chxbox.isEnabled()) {
                        revertItemCheckedByPosition(position);
                    }
                }
            });

            if (item != null) {
                imgView.setImageResource(item.getIconId());
                textView.setText(item.getTextId());
            }
            if (isItemCheckedByPosition(position)) {
                if (chxbox.isEnabled()) {
                    chxbox.setChecked(true);
                }
            } else {
                if (chxbox.isEnabled()) {
                    chxbox.setChecked(false);
                }
            }
            return view;
        }
    }

    private class DeleteFolderTask extends AsyncTask<File[], String, Long> {
        private ProgressDialog mDeletingDialog;

        public DeleteFolderTask() {
            mDeletingDialog = new ProgressDialog(PersonalDataBackupActivity.this);
            mDeletingDialog.setCancelable(false);
            if (getString(R.string.delete_please_wait) != null) {
                mDeletingDialog.setMessage(getString(R.string.delete_please_wait));
            }
            mDeletingDialog.setIndeterminate(true);
        }

        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            if (mBackupFolder != null) {
                if (checkedStartBackup()) {
                    startPersonalDataBackup(mBackupFolder);
                }
            }

            if (mDeletingDialog != null) {
                mDeletingDialog.dismiss();
            }
        }

        protected void onPreExecute() {
            if (mDeletingDialog != null) {
                mDeletingDialog.show();
            }
        }

        protected Long doInBackground(File[]... params) {
            File[] files = params[0];
            for (File file : files) {
                FileUtils.deleteFileOrFolder(file);
            }

            return null;
        }
    }

    /**
     * add for Contact card dialog display
     */
    private class SimInfoAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater = null;
        private ArrayList<ContactItemData> mData = null;
        private String[] mSelectInfo = null;
        private boolean[] mCheckStatus = null;
        private HashMap<Integer, Boolean> mChecked = null;

        SimInfoAdapter(Context context) {
        }

        public SimInfoAdapter(Context context, ArrayList<ContactItemData> list,
                boolean[] mContactCheckedTemp) {
            mLayoutInflater = LayoutInflater.from(context);
            this.mData = list;
            this.mCheckStatus = mContactCheckedTemp;
        }

        public void updataData(ArrayList<ContactItemData> list) {
            this.mData = list;
        }

        public int getCount() {
            return mData.size();
        }

        public Object getItem(int position) {
            return mData.get(position);
        }

        public long getItemId(int position) {
            return mData.get(position).getSimId();
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewSimInfo mViewSimInfo = null;
            if (mData == null) {
                MyLogger.logD(CLASS_TAG, "warning no data !!");
                return null;
            }
            if (convertView == null) {
                mViewSimInfo = new ViewSimInfo();
                convertView = mLayoutInflater.inflate(R.layout.dialog_contacts_import, null);
                mViewSimInfo.mImg = (ImageView) convertView.findViewById(R.id.icon);
                mViewSimInfo.mSimName = (TextView) convertView.findViewById(R.id.simName);
                mViewSimInfo.mCheckbox = (CheckBox) convertView.findViewById(R.id.checkbox);
                convertView.setTag(mViewSimInfo);
            } else {
                mViewSimInfo = (ViewSimInfo) convertView.getTag();
            }
            mViewSimInfo.mImg.setBackground(mData.get(position).getIcon());
            if (mData.get(position).getmContactName() != null) {
                mViewSimInfo.mSimName.setText(mData.get(position).getmContactName());
            }
            mViewSimInfo.mCheckbox.setChecked(mData.get(position).isChecked());

            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MyLogger.logD(CLASS_TAG, "DialogID.DLG_CONTACT_CONFIG: click position === "
                            + position);
                    updateCheckStatus(position);
                }
            });

            return convertView;
        }
    }

    private void updateUnCheckedStatus(int position) {
        MyLogger.logD(CLASS_TAG, "updateUnCheckedStatus : false position = " + position);
        mContactCheckedTemp[position] = false;
    }

    public void updateCheckStatus(final int position) {
        int count = mSimCount + 1;

        mContactItemData = mContactItemDataList.get(position);
        if (mContactItemData.isChecked()) {
            mContactItemData.setChecked(false);
            mContactCheckedTemp[position] = false;
        } else {
            mContactItemData.setChecked(true);
            mContactCheckedTemp[position] = true;
        }
        boolean isAllUnChecked = true;
        mSimInfoAdapter.notifyDataSetChanged();
        for (ContactItemData mItemData : mContactItemDataList) {
            if (mItemData.isChecked()) {
                MyLogger.logD(CLASS_TAG, "mItemData.isChecked == true");
                isAllUnChecked = false;
                break;
            }
        }
        if (isAllUnChecked) {
            if (alertDialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        } else {
            if (alertDialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
        }
        MyLogger.logD(CLASS_TAG, "DialogID.DLG_CONTACT_CONFIG: updateCheckStatus ");

    }

    public void checkContactItem() {
        int index = 0;
        boolean isAllUnChecked = true;
        if (mContactItemDataList != null && mContactItemDataList.size() > 0) {
            for (ContactItemData mItemData : mContactItemDataList) {
                if (mItemData.isChecked()) {
                    index++;
                }
            }
            if (index == 0 && alertDialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                MyLogger.logD(CLASS_TAG, "checkContactItem setEnabled false ");
            }
        }
    }

    private class ViewSimInfo {
        private ImageView mImg;
        private TextView mSimName;
        private CheckBox mCheckbox;
    }

    public void OnUnCheckedChanged() {
        super.OnUnCheckedChanged();
        MyLogger.logD(CLASS_TAG, "OnUnCheckedChanged and updateTitle");
        updateTitle();
    }

    protected String formatProgressDialogMsg(int currentProgress, String content) {
        StringBuilder builder = new StringBuilder(getString(R.string.backuping));
        if (content != null) {
            builder.append("(").append(content).append(")");
        }
        return builder.toString();
    }

    @Override
    public void startBackup(boolean flag) {

    }
}

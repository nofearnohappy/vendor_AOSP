package com.mediatek.backuprestore;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.backuprestore.SDCardReceiver.OnSDCardStatusChangedListener;
import com.mediatek.backuprestore.utils.BackupFilePreview;
import com.mediatek.backuprestore.utils.BackupFileScanner;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.Constants.MessageID;
import com.mediatek.backuprestore.utils.BackupAppFilePreview;
import com.mediatek.backuprestore.utils.FileUtils;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.OldBackupFilePreview;
import com.mediatek.backuprestore.utils.SDCardUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RestoreTabFragment extends PreferenceFragment {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/RestoreTabFragment";
    private static final int START_ACTION_MODE_DELAY_TIME = 500;
    private static final String STATE_DELETE_MODE = "deleteMode";
    private static final String STATE_CHECKED_ITEMS = "checkedItems";

    private ListView mListView;
    private BackupFileScanner mFileScanner;
    private Handler mHandler;
    private ProgressDialog mLoadingDialog;
    public ActionMode mDeleteActionMode;
    private DeleteActionMode mActionModeListener;
    OnSDCardStatusChangedListener mSDCardListener;
    private boolean mIsActive = false;
    TextView mEmptyView;
    DialogFragment mDeviceInfoDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(CLASS_TAG, "RestoreTabFragment: onCreate");
        addPreferencesFromResource(R.xml.restore_tab_preference);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(CLASS_TAG, "RestoreTabFragment: onAttach");
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(CLASS_TAG, "RestoreTabFragment: onActivityCreated");
        init();
        if (savedInstanceState != null) {
            boolean isActionMode = savedInstanceState.getBoolean(STATE_DELETE_MODE, false);
            if (isActionMode) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDeleteActionMode = getActivity().startActionMode(mActionModeListener);
                        mActionModeListener.restoreState(savedInstanceState);
                    }
                }, START_ACTION_MODE_DELAY_TIME);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(CLASS_TAG, "RestoreTabFragment: onDestroy");
        if (mFileScanner != null) {
            mFileScanner.setHandler(null);
        }
        unRegisteSDCardListener();
    }

    public void onPause() {
        super.onPause();
        Log.i(CLASS_TAG, "RestoreTabFragment: onPasue");
        if (mFileScanner != null) {
            mFileScanner.quitScan();
        }
        if (mLoadingDialog != null) {
            mLoadingDialog.cancel();
            MyLogger.logV(CLASS_TAG, "mFileScanner is canle and mLoadingDialog need dismiss");
        }
        mIsActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLASS_TAG, "RestoreTabFragment: onResume");
        mIsActive = true;
        mEmptyView.setVisibility(View.GONE);
        if (mActionModeListener != null && mDeleteActionMode != null) {
            Log.i(CLASS_TAG, "RestoreTabFragment: onResume and mDeleteActionMode need finish");
            mDeleteActionMode.finish();
        }
        // refresh
        // if (SDCardUtils.isSdCardAvailable()) {
        startScanFiles(false);
        /*
         * } else { ps.removeAll(); if (mActionModeListener != null &&
         * mDeleteActionMode != null) { Log.i(CLASS_TAG,
         * "RestoreTabFragment: onResume and mDeleteActionMode need finish");
         * mDeleteActionMode.finish(); } }
         */
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(CLASS_TAG, "RestoreTabFragment: onDetach");
    }

    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDeleteActionMode != null) {
            outState.putBoolean(STATE_DELETE_MODE, true);
            mActionModeListener.saveState(outState);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference instanceof RestoreCheckBoxPreference) {
            RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
            Log.i(CLASS_TAG, "onPreferenceTreeClick: preference === " + p.getKey());
            if (mDeleteActionMode == null) {
                Intent intent = p.getIntent();
                if (intent.getStringExtra("key").equals("app")) {
                    List<String> file = intent.getStringArrayListExtra(Constants.FILENAME);
                    if (file != null) {
                        String[] fileName = file.toArray(new String[file.size()]);
                        if (fileName != null && fileName.length > 0) {
                            startActivity(intent);
                        } else {
                            Toast.makeText(getActivity(), R.string.file_no_exist_and_update,
                                    Toast.LENGTH_SHORT);
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.file_no_exist_and_update,
                                Toast.LENGTH_SHORT);
                    }
                } else {
                    String filePath = intent.getStringExtra(Constants.FILENAME);
                    File file = new File(filePath);
                    if (file != null && file.exists()) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), R.string.file_no_exist_and_update,
                                Toast.LENGTH_SHORT);
                    }
                }

            } else if (mActionModeListener != null) {
                mActionModeListener.setItemChecked(p, !p.isChecked());
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(CLASS_TAG, "RestoreTabFragment: onStart");
    }

    private void init() {
        initHandler();
        initListView(getView());
        initLoadingDialog();
        registerSDCardListener();
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
            public void onSDCardStatusChanged(final boolean mount, String path) {
                if (mIsActive) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsActive) {
                                if (mount) {
                                    startScanFiles(false);
                                }
                                int resId = mount ? R.string.sdcard_swap_insert
                                        : R.string.sdcard_swap_remove;
                                Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
                            }
                            if (!mount) {
                                startScanFiles(false);
                                if (mActionModeListener != null && mDeleteActionMode != null) {
                                    Log.i(CLASS_TAG,
                                            "RestoreTabFragment" +
                                            ": sdCard Umount and mDeleteActionMode need finish");
                                    mDeleteActionMode.finish();
                                }
                            }
                        }
                    });
                }
            }
        };

        SDCardReceiver receiver = SDCardReceiver.getInstance();
        receiver.registerOnSDCardChangedListener(mSDCardListener);
    }

    private void initLoadingDialog() {
        mLoadingDialog = new ProgressDialog(getActivity());
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setMessage(getString(R.string.loading_please_wait));
        mLoadingDialog.setIndeterminate(true);
    }

    private void initListView(View root) {
        View view = root.findViewById(android.R.id.list);
        if (view != null && view instanceof ListView) {
            mListView = (ListView) view;
            mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
            mEmptyView.setVisibility(View.GONE);
            mEmptyView.setText(R.string.no_data);
            mEmptyView.setTextAppearance(getActivity(), android.R.attr.textAppearanceMedium);

            mActionModeListener = new DeleteActionMode();
            mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> listView, View view, int position,
                        long id) {
                    mDeleteActionMode = getActivity().startActionMode(mActionModeListener);
                    showCheckBox(true);
                    mActionModeListener.onPreferenceItemClick(getPreferenceScreen(), position);
                    return true;
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void startDeleteItems(final HashSet<String> deleteItemIds) {
        PreferenceScreen ps = getPreferenceScreen();
        int count = ps.getPreferenceCount();
        HashSet<File> files = new HashSet<File>();
        for (int position = 0; position < count; position++) {
            Preference preference = ps.getPreference(position);
            if (preference != null && preference instanceof RestoreCheckBoxPreference) {
                RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
                String key = p.getKey();
                if (deleteItemIds.contains(key)) {
                    if (p.getAccociateFile() != null) {
                        files.add(p.getAccociateFile());
                    } else {
                        CopyOnWriteArraySet<String> mData = p.getAppData();
                        for (String apkPath : mData) {
                            File apkFile = new File(apkPath);
                            if (apkFile != null && apkFile.exists()) {
                                files.add(apkFile);
                            }
                        }
                    }
                }
            }
        }
        DeleteCheckItemTask deleteTask = new DeleteCheckItemTask();
        deleteTask.execute(files);
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {

                case MessageID.SCANNER_ONE_ITEM:
                    if (getActivity() != null) {
                        if (msg.obj != null) {
                            addScanResultsAsPreferences(msg.obj);
                        } else {
                            PreferenceScreen ps = getPreferenceScreen();
                            // clear the old items last scan
                            ps.removeAll();
                        }
                    }
                    /*
                     * if (mLoadingDialog != null) { mLoadingDialog.cancel();
                     * MyLogger.logV(CLASS_TAG,
                     * "hanlde msg FINISH --- mLoadingDialog cancel"); }
                     */
                    break;
                case MessageID.SCANNER_FINISH:
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), R.string.scan_finish, Toast.LENGTH_SHORT)
                                .show();
                        boolean flag = false;
                        if (msg.obj != null) {
                            List<Object> dataList = (List<Object>) msg.obj;
                            if (dataList != null && dataList.size() > 0) {
                                flag = true;
                            }
                        }
                        if (!flag) {
                            mEmptyView.setText(R.string.no_data);
                            mEmptyView.setVisibility(View.VISIBLE);
                            mListView.setEmptyView(mEmptyView);
                            showDialogInfo();
                        }
                    }
                    break;
                default:
                    break;
                }
            }

        };
    }

    boolean mShowNotice = false;

    private void showDialogInfo() {
        if (SDCardUtils.isSupprotSDcard(getActivity())) {
            if (!StorageSettingsActivity.getNoticeStatus(getActivity(),
                    Constants.NO_SDCARD_RESTORE_INFO)
                    && !mShowNotice
                    && !SDCardUtils.isSdCardAvailable(getActivity())) {
                DeviceInfoDialogFragment fg = (DeviceInfoDialogFragment) getFragmentManager()
                        .findFragmentByTag("dialog");
                if (fg != null) {
                    MyLogger.logV(CLASS_TAG,
                            "the dialog is show and no longer display the digloagInfo ~! ");
                    return;
                }

                mDeviceInfoDialogFragment = DeviceInfoDialogFragment.newInstance(
                        R.string.sdcard_unmount_backup, Constants.NO_SDCARD_RESTORE_INFO,
                        R.array.sdcard_unmount_restore_info);
                mDeviceInfoDialogFragment.show(getFragmentManager(), "dialog");
                mShowNotice = true;
            }
        } else {
            if (!StorageSettingsActivity.getNoticeStatus(getActivity(),
                    Constants.SDCARD_UNMOUNT_RESTORE_INFO) && !mShowNotice) {
                DeviceInfoDialogFragment fg = (DeviceInfoDialogFragment) getFragmentManager()
                        .findFragmentByTag("dialog");
                if (fg != null) {
                    MyLogger.logV(CLASS_TAG,
                            "the dialog is show and no longer display the digloagInfo ~! ");
                    return;
                }
                mDeviceInfoDialogFragment = DeviceInfoDialogFragment.newInstance(
                        R.string.change_phone_info, Constants.SDCARD_UNMOUNT_RESTORE_INFO,
                        R.array.no_sdcard_restore_info);
                mDeviceInfoDialogFragment.show(getFragmentManager(), "dialog");
                mShowNotice = true;
            }
        }
    }

    public void startScanFiles(boolean isFullScan) {
        if (!mIsActive) {
            MyLogger.logV(CLASS_TAG, "no need to scan files as mIsActive is false");
            return;
        }
        /*
         * if (!mLoadingDialog.isShowing()) { mLoadingDialog.show(); }
         */
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }

        // Toast.makeText(getActivity(),"Loading...",
        // Toast.LENGTH_SHORT).show();
        if (mFileScanner == null) {
            mFileScanner = new BackupFileScanner(getActivity(), mHandler, isFullScan);
        } else {
            mFileScanner.setHandler(mHandler);
        }

        if (mFileScanner != null) {
            mFileScanner.setScanAll(isFullScan);
            mFileScanner.quitScan();
        }
        if (isFullScan) {
            mShowNotice = false;
        }
        if (!mFileScanner.isScanning()) {
            MyLogger.logD(CLASS_TAG, "RestoreTabFragment: startScanFiles");
            mFileScanner.startScan();
        } else {
            MyLogger.logD(CLASS_TAG, "don't need to startScanFiles mFileScanner is running");
        }
    }

    @SuppressWarnings("unchecked")
    private void addScanResultsAsPreferences(Object obj) {

        PreferenceScreen ps = getPreferenceScreen();
        ps.removeAll();
        /*
         * PreferenceCategory personDataPc = (PreferenceCategory)
         * this.findPreference
         * ("prefer_key_backup_personal_data_history_category");
         * PreferenceCategory appDataPc = (PreferenceCategory)
         * this.findPreference("prefer_key_backup_app_data_history_category");
         * ps.removePreference(personDataPc);
         */
        // clear the old items last scan
        //

        /*
         * HashMap<String, List<?>> map = (HashMap<String, List<?>>) obj;
         *
         * // personal data List<BackupFilePreview> items =
         * (List<BackupFilePreview>)
         * map.get(Constants.SCAN_RESULT_KEY_PERSONAL_DATA); if (items != null
         * && !items.isEmpty()) { addPreferenceCategory(ps,
         * R.string.backup_personal_data_history); for (BackupFilePreview item :
         * items) { addRestoreCheckBoxPreference(ps, item, "personal data"); } }
         * // old backup data List<OldBackupFilePreview> itemsOlds =
         * (List<OldBackupFilePreview>)
         * map.get(Constants.SCAN_RESULT_KEY_OLD_DATA); if (itemsOlds != null &&
         * !itemsOlds.isEmpty()) { // addPreferenceCategory(ps,
         * R.string.backup_personal_data_history); for (OldBackupFilePreview
         * itemsOld : itemsOlds) { addRestoreCheckBoxPreference(ps, itemsOld,
         * "old data"); MyLogger.logD(CLASS_TAG,
         * "addScanResultsAsPreferences: old data having add"); } }
         *
         * // app data items = (List<BackupFilePreview>)
         * map.get(Constants.SCAN_RESULT_KEY_APP_DATA); if (items != null &&
         * !items.isEmpty()) { addPreferenceCategory(ps,
         * R.string.backup_app_data_history); for (BackupFilePreview item :
         * items) { addRestoreCheckBoxPreference(ps, item, "app"); } }
         */

        // / modify CMCC feature 2015-1-25

        /*
         * BackupFilePreview dataItem = (BackupFilePreview)
         * map.get(Constants.SCAN_RESULT_KEY_PERSONAL_DATA); if (dataItem !=
         * null) { //addPreferenceCategory(ps,
         * R.string.backup_personal_data_history); MyLogger.logD(CLASS_TAG,
         * "addScanResultsAsPreferences : dataItem name = "+
         * dataItem.getFileName());
         * addRestoreCheckBoxPreference(personDataPc,dataItem,"personal data");
         * }
         *
         * OldBackupFilePreview itemsOlds = (OldBackupFilePreview)
         * map.get(Constants.SCAN_RESULT_KEY_OLD_DATA); if (itemsOlds != null) {
         * //addPreferenceCategory(ps, R.string.backup_personal_data_history);
         * MyLogger.logD(CLASS_TAG,
         * "addScanResultsAsPreferences : dataItem name = "+
         * itemsOlds.getFileName());
         * addRestoreCheckBoxPreference(personDataPc,itemsOlds,"old data"); }
         * BackupFilePreview appItem = (BackupFilePreview)
         * map.get(Constants.SCAN_RESULT_KEY_APP_DATA); if (appItem != null) {
         * //addPreferenceCategory(ps, R.string.backup_app_data_history);
         * MyLogger.logD(CLASS_TAG,
         * "addScanResultsAsPreferences : dataItem name = "+
         * appItem.getFileName());
         * addRestoreCheckBoxPreference(appDataPc,appItem,"app"); }
         */
        long size = 0;
        BackupAppFilePreview appData = null;
        List<Object> dataList = (List<Object>) obj;
        for (int i = 0; i < dataList.size(); i++) {
            Object data = dataList.get(i);
            if (data instanceof BackupFilePreview) {
                BackupFilePreview backupFilePreview = (BackupFilePreview) data;
                MyLogger.logD(CLASS_TAG, "addScanResultsAsPreferences : dataItem name = "
                        + backupFilePreview.getFileName());
                if (backupFilePreview != null) {
                    if (ps.findPreference("PERSON") == null) {
                        addPreferenceCategory(ps, R.string.backup_personal_data_history, "PERSON");
                    }
                    addRestoreCheckBoxPreference(ps, backupFilePreview, "personal data");
                }
            }

            else if (data instanceof OldBackupFilePreview) {
                OldBackupFilePreview oldData = (OldBackupFilePreview) data;
                if (oldData != null) {
                    MyLogger.logD(CLASS_TAG, "addScanResultsAsPreferences : oldData name = "
                            + oldData.getFileName());
                    if (ps.findPreference("PERSON") == null) {
                        addPreferenceCategory(ps, R.string.backup_personal_data_history, "PERSON");
                    }
                    addRestoreCheckBoxPreference(ps, oldData, "old data");
                }
            } else if (data instanceof BackupAppFilePreview) {
                appData = (BackupAppFilePreview) data;
                size = appData.getAppDataSize();
                MyLogger.logD(CLASS_TAG, "addScanResultsAsPreferences : add size  = " + size);
            }

        }

        if (size > 0) {
            if (ps.findPreference("APP") == null) {
                addPreferenceCategory(ps, R.string.backup_app_data_history, "APP");
            }
            addRestoreCheckBoxPreference(ps, appData, "app");
        }

        if (mDeleteActionMode != null && mActionModeListener != null) {
            mActionModeListener.confirmSyncCheckedPositons();
        }
    }

    private PreferenceCategory addPreferenceCategory(PreferenceScreen ps, int titleID, String key) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(titleID);
        category.setKey(key);
        ps.addPreference(category);
        return category;
    }

    private <T> void addRestoreCheckBoxPreference(PreferenceScreen ps, T items, String type) {
        long size = 0;
        String fileName = null;
        File file = null;
        String path = null;
        Set appData = null;
        if (items == null || type == null) {
            MyLogger.logE(CLASS_TAG, "addRestoreCheckBoxPreference: Error!");
            return;
        }
        RestoreCheckBoxPreference preference = new RestoreCheckBoxPreference(getActivity());
        if (type.equals("app")) {
            preference.setTitle(R.string.backup_app_data_preference_title);
            BackupAppFilePreview item = (BackupAppFilePreview) items;
            size = item.computeSize();
            file = item.getFile();
            appData = item.getAppData();
            fileName = item.getFileName();
            preference.setAppData(appData);
        } else if (type.equals("personal data")) {
            BackupFilePreview item = (BackupFilePreview) items;
            fileName = item.getFileName();
            preference.setTitle(fileName);
            size = item.getFileSize();
            file = item.getFile();
            path = item.getFile().getAbsolutePath();
            preference.setAccociateFile(file);
        } else if (type.equals("old data")) {
            OldBackupFilePreview item = (OldBackupFilePreview) items;
            fileName = item.getFileName();
            int index = fileName.lastIndexOf(".");
            fileName = fileName.substring(0, index);
            preference.setTitle(fileName);
            size = item.getFileSize();
            file = item.getFile();
            path = item.getFile().getAbsolutePath();
            preference.setAccociateFile(file);
        }
        // MyLogger.logI(CLASS_TAG, "addRestoreCheckBoxPreference: type is " +
        // type + " fileName = " + fileName);
        StringBuilder builder = new StringBuilder(getString(R.string.backup_data));
        builder.append(" ");
        builder.append(FileUtils.getDisplaySize(size, getActivity()));
        preference.setSummary(builder.toString());
        if (mDeleteActionMode != null) {
            preference.showCheckbox(true);
        }

        Intent intent = new Intent();
        if (type.equals("app")) {
            intent.setClass(getActivity(), AppRestoreActivity.class);
            intent.putStringArrayListExtra(Constants.FILENAME, new ArrayList<String>(appData));
            intent.putExtra("key", "app");
        } else {
            intent.setClass(getActivity(), PersonalDataRestoreActivity.class);
            intent.putExtra(Constants.FILENAME, path);
            intent.putExtra("key", "person_data");
        }

        preference.setIntent(intent);
        ps.addPreference(preference);
        mEmptyView.setVisibility(View.GONE);
    }

    private void showCheckBox(boolean bShow) {
        PreferenceScreen ps = getPreferenceScreen();
        int count = ps.getPreferenceCount();
        for (int position = 0; position < count; position++) {
            Preference p = ps.getPreference(position);
            if (p instanceof RestoreCheckBoxPreference) {
                ((RestoreCheckBoxPreference) p).showCheckbox(bShow);
            }
        }
    }

    class DeleteActionMode implements ActionMode.Callback {

        private int mCheckedCount;
        private HashSet<String> mCheckedItemIds;
        private ActionMode mMode;

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.select_all:
                setAllItemChecked(true);
                break;

            case R.id.cancel_select:
                setAllItemChecked(false);
                break;

            case R.id.delete:
                if (mCheckedCount == 0) {
                    Toast.makeText(getActivity(), R.string.no_item_selected, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    startDeleteItems(mCheckedItemIds);
                    mode.finish();
                }
                break;

            default:
                break;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mMode = mode;
            mListView.setLongClickable(false);
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.multi_select_menu, menu);
            mCheckedItemIds = new HashSet<String>();
            setAllItemChecked(false);
            showCheckBox(true);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mCheckedItemIds = null;
            mCheckedCount = 0;
            mDeleteActionMode = null;
            mListView.setLongClickable(true);
            showCheckBox(false);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        private void updateTitle() {
            StringBuilder builder = new StringBuilder();
            builder.append(mCheckedCount);
            builder.append(" ");
            builder.append(getString(R.string.selected));
            mMode.setTitle(builder.toString());
        }

        public void onPreferenceItemClick(PreferenceScreen ps, final int position) {
            Preference preference = ps.getPreference(position);
            if (preference instanceof RestoreCheckBoxPreference) {
                RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
                boolean toChecked = !p.isChecked();
                p.setChecked(toChecked);
                String key = null;
                if (p.getAccociateFile() != null) {
                    key = p.getAccociateFile().getAbsolutePath();
                } else {
                    key = p.getKey();
                }
                if (toChecked) {
                    mCheckedItemIds.add(key);
                    mCheckedCount++;
                } else {
                    mCheckedItemIds.remove(key);
                    mCheckedCount--;
                }
                updateTitle();
            }
        }

        public void setItemChecked(final RestoreCheckBoxPreference p, final boolean checked) {
            if (p.isChecked() != checked) {
                p.setChecked(checked);
                String key = p.getKey();
                if (checked) {
                    mCheckedItemIds.add(key);
                    mCheckedCount++;
                } else {
                    mCheckedItemIds.remove(key);
                    mCheckedCount--;
                }
            }
            updateTitle();
        }

        private void setAllItemChecked(boolean checked) {
            PreferenceScreen ps = getPreferenceScreen();

            mCheckedCount = 0;
            mCheckedItemIds.clear();
            int count = ps.getPreferenceCount();
            for (int position = 0; position < count; position++) {
                Preference preference = ps.getPreference(position);
                if (preference instanceof RestoreCheckBoxPreference) {
                    RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
                    p.setChecked(checked);
                    if (checked) {
                        if (p.getAccociateFile() != null) {
                            mCheckedItemIds.add(p.getAccociateFile().getAbsolutePath());
                            mCheckedCount++;
                        } else {
                            mCheckedItemIds.add(p.getKey());
                            mCheckedCount++;
                        }

                    }
                }
            }
            updateTitle();
        }

        /**
         * after refreshed, must sync witch mCheckedItemIds;
         */
        public void confirmSyncCheckedPositons() {
            mCheckedCount = 0;

            HashSet<String> tempCheckedIds = new HashSet<String>();
            PreferenceScreen ps = getPreferenceScreen();
            int count = ps.getPreferenceCount();
            for (int position = 0; position < count; position++) {
                Preference preference = ps.getPreference(position);
                if (preference instanceof RestoreCheckBoxPreference) {
                    RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
                    String key = p.getKey();
                    if (mCheckedItemIds.contains(key)) {
                        tempCheckedIds.add(key);
                        p.setChecked(true);
                        mCheckedCount++;
                    }
                }
            }
            mCheckedItemIds.clear();
            mCheckedItemIds = tempCheckedIds;
            updateTitle();
        }

        public void saveState(final Bundle outState) {
            ArrayList<String> list = new ArrayList<String>();
            for (String item : mCheckedItemIds) {
                list.add(item);
            }
            outState.putStringArrayList(STATE_CHECKED_ITEMS, list);
        }

        public void restoreState(Bundle state) {
            ArrayList<String> list = state.getStringArrayList(STATE_CHECKED_ITEMS);
            if (list != null && !list.isEmpty()) {
                for (String item : list) {
                    mCheckedItemIds.add(item);
                }
            }
            PreferenceScreen ps = getPreferenceScreen();
            if (ps.getPreferenceCount() > 0) {
                confirmSyncCheckedPositons();
            }
        }
    }

    private class DeleteCheckItemTask extends AsyncTask<HashSet<File>, String, Long> {

        private ProgressDialog mDeletingDialog;

        public DeleteCheckItemTask() {
            mDeletingDialog = new ProgressDialog(getActivity());
            mDeletingDialog.setCancelable(false);
            mDeletingDialog.setMessage(getString(R.string.delete_please_wait));
            mDeletingDialog.setIndeterminate(true);
        }

        @Override
        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            startScanFiles(false);
            Activity activity = getActivity();
            if (activity != null && mDeletingDialog != null) {
                mDeletingDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = getActivity();
            if (activity != null && mDeletingDialog != null) {
                mDeletingDialog.show();
            }
        }

        @Override
        protected Long doInBackground(HashSet<File>... params) {
            HashSet<File> deleteFiles = params[0];
            for (File file : deleteFiles) {
                FileUtils.deleteFileOrFolder(file);
            }
            return null;
        }
    }
}

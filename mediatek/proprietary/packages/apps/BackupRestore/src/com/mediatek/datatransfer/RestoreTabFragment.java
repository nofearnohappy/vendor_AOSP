package com.mediatek.datatransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.datatransfer.SDCardReceiver.OnSDCardStatusChangedListener;
import com.mediatek.datatransfer.utils.BackupFilePreview;
import com.mediatek.datatransfer.utils.BackupFileScanner;
import com.mediatek.datatransfer.utils.Constants;
import com.mediatek.datatransfer.utils.Constants.MessageID;
import com.mediatek.datatransfer.utils.FileUtils;
import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.datatransfer.utils.SDCardUtils;
import com.mediatek.datatransfer.utils.Utils;

/**
 * @author mtk81330
 *
 */
public class RestoreTabFragment extends PreferenceFragment {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/RestoreTabFragment";
    private static final int START_ACTION_MODE_DELAY_TIME = 100;
    private static final String STATE_DELETE_MODE = "deleteMode";
    private static final String STATE_CHECKED_ITEMS = "checkedItems";

    private ListView mListView;
    private BackupFileScanner mFileScanner;
    private Handler mHandler;

    /**
     * @return Handler
     */
    public Handler getmHandler() {
        return mHandler;
    }
    private ProgressDialog mLoadingDialog;
    private ActionMode mDeleteActionMode;
    private DeleteActionMode mActionModeListener;
    OnSDCardStatusChangedListener mSDCardListener;
    private boolean mIsActive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onCreate");
        addPreferencesFromResource(R.xml.restore_tab_preference);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onAttach");
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onActivityCreated");
        init();
        if (savedInstanceState != null) {
            boolean isActionMode = savedInstanceState.getBoolean(STATE_DELETE_MODE, false);
            if (isActionMode) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null) {
                            return;
                        }
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
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onDestroy");
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        if (mFileScanner != null) {
            mFileScanner.setHandler(null);
        }
        unRegisteSDCardListener();
    }

    /**
     * onPause.
     */
    public void onPause() {
        super.onPause();
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onPasue");
        if (mFileScanner != null) {
            mFileScanner.quitScan();
        }
        mIsActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onResume");
        mIsActive = true;
        startScanFiles(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onDetach");
    }

    /**
     * @param outState state
     */
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDeleteActionMode != null) {
            outState.putBoolean(STATE_DELETE_MODE, true);
            mActionModeListener.saveState(outState);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (SDCardUtils.getExternalStoragePath(getActivity()) == null) {
            Utils.exitLockTaskModeIfNeeded(getActivity());
            getActivity().finish();
            return true;
        }
        if (preference instanceof RestoreCheckBoxPreference) {
            RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
            if (mDeleteActionMode == null) {
                Intent intent = p.getIntent();
                String fileName = intent.getStringExtra(Constants.FILENAME);
                File file = new File(fileName);
                if (file.exists()) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), R.string.file_no_exist_and_update,
                            Toast.LENGTH_SHORT).show();
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
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: onStart");
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
            receiver.unRegisterOnSDCardChangedListener(mSDCardListener);
        }
    }

    private void registerSDCardListener() {
        mSDCardListener = new OnSDCardStatusChangedListener() {
            @Override
            public void onSDCardStatusChanged(final boolean mount) {
                if (mIsActive) {
                    if (!mount) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                PreferenceScreen ps = getPreferenceScreen();
                                ps.removeAll();
                                // fix bug for ALPS01895396 start
                                try {
                                    Toast.makeText(getActivity(),
                                            R.string.nosdcard_notice,
                                            Toast.LENGTH_SHORT).show();
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                                // end
                            }
                        });
                        Utils.exitLockTaskModeIfNeeded(getActivity());
                        getActivity().finish();
                    }
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
    private TextView mEmptyView;
    private void initListView(View root) {
        View view = root.findViewById(android.R.id.list);
        if (view != null && view instanceof ListView) {
            mListView = (ListView) view;
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
        mEmptyView = new TextView(getActivity());
        mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setVisibility(View.GONE);
        ((ViewGroup) getListView().getParent()).addView(mEmptyView);
        getListView().setEmptyView(mEmptyView);
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
                    files.add(p.getAccociateFile());
                }
            }
        }
        DeleteCheckItemTask deleteTask = new DeleteCheckItemTask();
        deleteTask.execute(files);
        ps.removeAll();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {

                case MessageID.SCANNER_FINISH:
                    MyLogger.logD(CLASS_TAG, "recieve MessageID.SCANNER_FINISH");
                    if (getActivity() != null) {
                        addScanResultsAsPreferences(msg.obj);
                    }
                    MyLogger.logD(CLASS_TAG, "mLoadingDialog != null: "
                            + (mLoadingDialog != null));
                    if (mLoadingDialog != null) {
                        mLoadingDialog.cancel();
                    }
                    break;

                default:
                    break;
                }
            }
        };
    }

    private void startScanFiles(Context context) {
        if (mLoadingDialog == null) {
            initLoadingDialog();
        }
        mLoadingDialog.show();

        if (mFileScanner == null) {
            mFileScanner = new BackupFileScanner(context, mHandler);
        } else {
            mFileScanner.setHandler(mHandler);
        }
        MyLogger.logI(CLASS_TAG, "RestoreTabFragment: startScanFiles");
        mFileScanner.startScan();
    }

    @SuppressWarnings("unchecked")
    private void addScanResultsAsPreferences(Object obj) {

        PreferenceScreen ps = getPreferenceScreen();

        // clear the old items last scan
        ps.removeAll();

        if (obj == null) {
            MyLogger.logD(CLASS_TAG, "addScanResultsAsPreferences obj = " + obj);
            return;
        }

        HashMap<String, List<BackupFilePreview>> map =
                (HashMap<String, List<BackupFilePreview>>) obj;
        Preference noDataPreference = new Preference(getActivity());
        noDataPreference.setTitle("No Record");
        boolean noRecord = true;
        // personal data
        List<BackupFilePreview> items = map.get(Constants.SCAN_RESULT_KEY_PERSONAL_DATA);
        if (items != null && !items.isEmpty()) {
            noRecord = false;
            addPreferenceCategory(ps, R.string.backup_personal_data_history);
            for (BackupFilePreview item : items) {
                addRestoreCheckBoxPreference(ps, item, "personal data");
            }
        }

        // app data
        items = map.get(Constants.SCAN_RESULT_KEY_APP_DATA);
        if (items != null && !items.isEmpty()) {
            addPreferenceCategory(ps, R.string.backup_app_data_history);
            noRecord = false;
            for (BackupFilePreview item : items) {
                addRestoreCheckBoxPreference(ps, item, "app");
            }
        }
        if (noRecord) {
            MyLogger.logD(CLASS_TAG, "getActivity = " + getActivity());
            if (mEmptyView != null) {
                mEmptyView.setText(R.string.no_data);
            }
            // ps.addPreference(noDataPreference);
        }

        if (mDeleteActionMode != null && mActionModeListener != null) {
            MyLogger.logD(CLASS_TAG, " confirmSyncCheckedPositons now!!!");
            mActionModeListener.confirmSyncCheckedPositons();
        }
    }

    private void addPreferenceCategory(PreferenceScreen ps, int titleId) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(titleId);
        ps.addPreference(category);
    }

    private void addRestoreCheckBoxPreference(PreferenceScreen ps, BackupFilePreview item,
            String type) {
        if (item == null || type == null) {
            MyLogger.logE(CLASS_TAG, "addRestoreCheckBoxPreference: Error!");
            return;
        }
        RestoreCheckBoxPreference preference = new RestoreCheckBoxPreference(getActivity());
        if (type.equals("app")) {
            preference.setTitle(R.string.backup_app_data_preference_title);
        } else {
            String fileName = item.getFileName();
            preference.setTitle(fileName);
        }
        MyLogger.logI(CLASS_TAG, "addRestoreCheckBoxPreference: type is " + type + " fileName = "
                + item.getFileName());
        StringBuilder builder = new StringBuilder(getString(R.string.backup_data));
        builder.append(" ");
        builder.append(FileUtils.getDisplaySize(item.getFileSize(), getActivity()));
        preference.setRestored(item.isRestored());
        preference.setSummary(builder.toString());
        if (mDeleteActionMode != null) {
            preference.showCheckbox(true);
        }
        preference.setAccociateFile(item.getFile());

        Intent intent = new Intent();
        if (type.equals("app")) {
            intent.setClass(getActivity(), AppRestoreActivity.class);
        } else {
            intent.setClass(getActivity(), PersonalDataRestoreActivity.class);
        }
        intent.putExtra(Constants.FILENAME, item.getFile().getAbsolutePath());
        preference.setIntent(intent);
        ps.addPreference(preference);
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

    /**
     * @author mtk81330
     *
     */
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
            MyLogger.logI(CLASS_TAG,
                    "updateTitle() titleString : " + builder.toString());
        }

        public void onPreferenceItemClick(PreferenceScreen ps, final int position) {
            Preference preference = ps.getPreference(position);
            if (preference instanceof RestoreCheckBoxPreference) {
                RestoreCheckBoxPreference p = (RestoreCheckBoxPreference) preference;
                boolean toChecked = !p.isChecked();
                p.setChecked(toChecked);
                String key = p.getAccociateFile().getAbsolutePath();
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
                        mCheckedItemIds.add(p.getAccociateFile().getAbsolutePath());
                        mCheckedCount++;
                    }
                }
            }
            updateTitle();
        }

        /**
         * @return
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
                    String key = p.getAccociateFile().getAbsolutePath();
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

    /**
     * @author mtk81330
     *
     */
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
            Activity activity = getActivity();
            if (activity != null) {
                startScanFiles(getActivity());
            }
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
                FileUtils.deleteFileOrFolder(file, getActivity());
            }
            return null;
        }
    }

    /**
     * @return ActionMode.
     */
    public ActionMode getDeleteMode() {
        return mDeleteActionMode;
    }
}

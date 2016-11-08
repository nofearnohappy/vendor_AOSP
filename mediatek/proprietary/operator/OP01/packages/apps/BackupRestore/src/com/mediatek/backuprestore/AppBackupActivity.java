package com.mediatek.backuprestore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreProgress;
import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreResultType;
import com.mediatek.backuprestore.ResultDialog.ResultEntity;
import com.mediatek.backuprestore.modules.AppBackupComposer;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.Constants.DialogID;
import com.mediatek.backuprestore.utils.Constants.State;
import com.mediatek.backuprestore.utils.ModuleType;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppBackupActivity extends AbstractBackupActivity {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/AppBackupActivity";
    private List<AppSnippet> mData = new ArrayList<AppSnippet>();
    private List<AppSnippet> mOriginData = new ArrayList<AppSnippet>();
    private AppBackupAdapter mAdapter;
    private InitDataTask mInitDataTask;
    private boolean mIsDataInitialed = false;
    private boolean mIsCheckedBackupStatus = false;
    private Bundle mSettingData;
    private static SharedPreferences sSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setProgressBarIndeterminateVisibility(false);
        Log.v(CLASS_TAG, "onCreate");
        setRequestCode(Constants.RESULT_APP_DATA);
        if (savedInstanceState != null) {
            mSettingData = savedInstanceState.getBundle(Constants.DATA_TITLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // update
        if (mInitDataTask == null || !mInitDataTask.isRunning()) {
            mInitDataTask = new InitDataTask(this);
            mInitDataTask.execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInitDataTask != null) {
            mInitDataTask.setCancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem m = menu.add(Menu.NONE, Menu.FIRST + 1, 0,
                getResources().getString(R.string.settings));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Menu.FIRST + 1:
            Intent intent = new Intent(this, SettingsAppActivity.class);
            if (mSettingData != null) {
                intent.putExtras(mSettingData);
            } else {
                sSharedPreferences = getInstance(this);
                mSettingData = new Bundle();
                mSettingData.putString(Constants.DATE,
                        sSharedPreferences.getString((Constants.DATA_TITLE), null));
                intent.putExtras(mSettingData);
            }

            startActivityForResult(intent, 0);
            Log.v(CLASS_TAG, "onOptionsItemSelected startSettings from menu mSettingData = "
                    + mSettingData.getString(Constants.DATE));
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public BaseAdapter initBackupAdapter() {
        mAdapter = new AppBackupAdapter(this, mData, R.layout.app_item);
        return mAdapter;
    }

    @Override
    public void startBackup() {
        if (checkedStartBackup()) {
            startBackup(true);
        }
    }

    @Override
    public void startBackup(boolean checkedPath) {
        if (checkedPath) {
            Log.v(CLASS_TAG, "startBackup");
            mOriginData = mData;
            startService();
            if (mBackupService != null) {
                ArrayList<Integer> backupList = new ArrayList<Integer>();
                backupList.add(ModuleType.TYPE_APP);
                mBackupService.setBackupModelList(backupList);

                boolean needBackupData = getSettingInfo();
                Log.v(CLASS_TAG, "~~~~ startBackup needBackupData = " + needBackupData);
                ArrayList<String> list = getSelectedPackageNameList();
                if (list == null || list.size() <= 0) {
                    MyLogger.logE(CLASS_TAG, "Error: no item to backup");
                    return;
                }
                mBackupService.setBackupItemParam(ModuleType.TYPE_APP, list);
                mBackupService.setBackupAppData(needBackupData);
                String appPath = SDCardUtils.getAppsBackupPath(StorageSettingsActivity
                        .getCurrentPath(this));
                MyLogger.logD(CLASS_TAG, "backup path is: " + appPath);
                boolean ret = mBackupService.startBackup(appPath);
                if (ret) {
                    showProgress();
                    mProgressDialog.setProgress(0);
                    mProgressDialog.setMax(list.size());
                    String msg = formatProgressDialogMsg(0, null);
                    mProgressDialog.setMessage(msg);
                } else {
                    showDialog(DialogID.DLG_SDCARD_FULL);
                    stopService();
                }
            }
        }
    }

    private boolean getSettingInfo() {
        if (mSettingData != null) {
            String title = mSettingData.getString(Constants.DATE);
            Log.v(CLASS_TAG, "~~ getSettingInfo title = " + title);
            if (title != null && title.equals(Constants.APP_AND_DATA)) {
                return true;
            }
        } else {
            sSharedPreferences = getInstance(this);
            if (sSharedPreferences.getString(Constants.DATA_TITLE, null).equals(
                    Constants.APP_AND_DATA)) {
                return true;
            }
        }
        return false;
    }

    protected void afterServiceConnected() {
        MyLogger.logD(CLASS_TAG, "afterServiceConnected, to checkBackupState");
        setHandler(mHandler);
        checkBackupState();
    }

    private ArrayList<String> getSelectedPackageNameList() {
        ArrayList<String> list = new ArrayList<String>();
        int count = mAdapter.getCount();
        for (int position = 0; position < count; position++) {
            AppSnippet item = (AppSnippet) getItemByPosition(position);
            if (isItemCheckedByPosition(position)) {
                list.add(item.getPackageName());
            }
        }
        return list;
    }

    @Override
    public void onCheckedCountChanged() {
        super.onCheckedCountChanged();
        updateTitle();
    }

    private void updateData(ArrayList<AppSnippet> list) {
        if (list == null) {
            MyLogger.logE(CLASS_TAG, "updateData, list is null");
            return;
        }
        mData = list;
        mOriginData = mData; // ALPS01854159
        mAdapter.changeData(list);
        initCheckStatus(false);
        syncUnCheckedItems();
        mAdapter.notifyDataSetChanged();
        updateTitle();
        updateButtonState();
        mIsDataInitialed = true;
        MyLogger.logD(CLASS_TAG, "data is initialed, to checkBackupState");
        checkBackupState();
    }

    private AppSnippet getAppSnippetByPackageName(String packageName) {

        AppSnippet result = null;
        for (AppSnippet item : mData) {
            if (item.getPackageName().equalsIgnoreCase(packageName)) {
                result = item;
                break;
            }
        }
        return result;
    }

    protected String formatProgressDialogMsg(int currentProgress, String content) {
        AppSnippet item = null;
        if (mBackupService != null) {
            ArrayList<String> params = mBackupService.getItemParam(ModuleType.TYPE_APP);
            String apkName = null;
            if (params != null) {
                apkName = params.get(currentProgress);
            }
            item = getAppSnippetByPackageName(apkName);
        }

        StringBuilder builder = new StringBuilder(getString(R.string.backuping));
        if (item != null) {
            builder.append("(").append(item.getName()).append(")");
        }
        return builder.toString();
    }

    public void updateTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.backup_app));
        int totalNum = mAdapter.getCount();
        int selectNum = this.getSelectedPackageNameList().size();
        sb.append("(" + selectNum + "/" + totalNum + ")");
        this.setTitle(sb.toString());
    }

    @Override
    protected void checkBackupState() {
        if (mIsCheckedBackupStatus) {
            MyLogger.logD(CLASS_TAG, "can not checkBackupState, as it has been checked");
            return;
        }
        if (!mIsDataInitialed) {
            MyLogger.logD(CLASS_TAG, "can not checkBackupState, wait data to initialed");
            return;
        }
        MyLogger.logD(CLASS_TAG, "to checkBackupState");
        mIsCheckedBackupStatus = true;
        if (mBackupService != null) {
            int state = mBackupService.getState();
            MyLogger.logD(CLASS_TAG, "checkBackupState: state = " + state);
            switch (state) {
            case State.RUNNING:
            case State.PAUSE:
                ArrayList<String> params = mBackupService.getItemParam(ModuleType.TYPE_APP);
                BackupRestoreProgress p = mBackupService.getCurrentProgress();
                Log.e(CLASS_TAG, CLASS_TAG + "checkBackupState: Max = " + p.mMax
                        + " curprogress = " + p.mCurNum);

                if (state == State.RUNNING) {
                    mProgressDialog.show();
                }
                if (p.mCurNum < p.mMax) {
                    String msg = null;
                    if (params != null) {
                        String packageName = params.get(p.mCurNum);
                        msg = formatProgressDialogMsg(p.mCurNum, null);
                    } else {
                        msg = getString(R.string.error);
                    }
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
                showBackupResult(mBackupService.getResultType(), mBackupService.getAppResult());
                break;
            default:
                super.checkBackupState();
                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(final int id, final Bundle args) {
        Dialog dialog = null;
        switch (id) {
        case DialogID.DLG_LOADING:
            ProgressDialog progressDlg = new ProgressDialog(this);
            progressDlg.setCancelable(false);
            progressDlg.setMessage(getString(R.string.loading_please_wait));
            progressDlg.setIndeterminate(true);
            dialog = progressDlg;
            break;

        default:
            dialog = super.onCreateDialog(id, args);
            break;
        }
        return dialog;
    }

    protected void showBackupResult(final BackupRestoreResultType result,
            final ArrayList<ResultEntity> appResultRecord) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("result", appResultRecord);
        ListAdapter adapter = ResultDialog.createAppResultAdapter(mOriginData, this, args,
                ResultDialog.RESULT_TYPE_BACKUP);
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

    private class InitDataTask extends AsyncTask<Void, Void, Long> {

        List<ApplicationInfo> mAppInfoList;
        ArrayList<AppSnippet> mAppDataList;
        private boolean mIsCanceled = false;
        private boolean mIsRunning = false;
        private boolean mIsSetting = false;
        private Context mContext = null;

        public InitDataTask(Context context) {
            mContext = context;
        }

        public void setCancel() {
            mIsCanceled = true;
            MyLogger.logD(CLASS_TAG, "FilePreviewTask: set cancel");
        }

        public boolean isRunning() {
            return mIsRunning;
        }

        @Override
        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            if (!mIsCanceled) {
                setButtonsEnable(true);
                updateData(mAppDataList);
                setHandler(mHandler);
                setProgressBarIndeterminateVisibility(false);
            }

            if (mIsSetting) {
                Intent intent = new Intent(mContext, SettingsAppActivity.class);
                if (mSettingData != null) {
                    intent.putExtras(mSettingData);
                }
                startActivityForResult(intent, 0);
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // show progress and set title as "updating"
            if (!mIsCanceled) {
                setProgressBarIndeterminateVisibility(true);
                setTitle(R.string.updating);
                setButtonsEnable(false);
            }
        }

        @Override
        protected Long doInBackground(Void... arg0) {
            mAppInfoList = AppBackupComposer.getUserAppInfoList(AppBackupActivity.this);
            PackageManager pm = getPackageManager();
            mAppDataList = new ArrayList<AppSnippet>();
            for (ApplicationInfo info : mAppInfoList) {
                if (!mIsCanceled) {
                    Drawable icon = info.loadIcon(pm);
                    CharSequence name = info.loadLabel(pm);
                    AppSnippet snippet = new AppSnippet(icon, name, info.packageName);
                    mAppDataList.add(snippet);
                }
            }
            if (!mIsCanceled) {
                Collections.sort(mAppDataList, new Comparator<AppSnippet>() {
                    public int compare(AppSnippet object1, AppSnippet object2) {
                        String left = new StringBuilder(object1.getName()).toString();
                        String right = new StringBuilder(object2.getName()).toString();
                        if (left != null && right != null) {
                            return left.compareTo(right);
                        }
                        return 0;
                    }
                });
            }
            sSharedPreferences = getInstance(mContext);
            Log.d(CLASS_TAG, "~~ doInBackground  ~mSettingData == " + mSettingData
                    + ", sSharedPreferences.getString(Constants.DATA_TITLE, null) = "
                    + sSharedPreferences.getString(Constants.DATA_TITLE, null));
            if (sSharedPreferences.getString(Constants.DATA_TITLE, null) == null
                    && mSettingData == null) {
                mIsSetting = true;
                Log.d(CLASS_TAG, "~~ doInBackground  ~mIsSetting == " + mIsSetting);
            }
            return null;
        }
    }

    private class AppBackupAdapter extends BaseAdapter {

        private List<AppSnippet> mList;
        private int mLayoutId;
        private LayoutInflater mInflater;

        public AppBackupAdapter(Context context, List<AppSnippet> list, int resource) {
            mList = list;
            mLayoutId = resource;
            mInflater = LayoutInflater.from(context);
        }

        public void changeData(List<AppSnippet> list) {
            mList = list;
        }

        public int getCount() {
            if (mList == null) {
                return 0;
            }
            return mList.size();
        }

        public Object getItem(int position) {
            if (mList == null) {
                return null;
            }
            return mList.get(position);
        }

        public long getItemId(int position) {
            return mList.get(position).getPackageName().hashCode();
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (mList == null) {
                return null;
            }
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(mLayoutId, parent, false);
            }
            final AppSnippet item = mList.get(position);
            ImageView imgView = (ImageView) view.findViewById(R.id.item_image);
            TextView textView = (TextView) view.findViewById(R.id.item_text);
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.item_checkbox);
            imgView.setBackgroundDrawable(item.getIcon());
            textView.setText(item.getName());
            checkbox.setChecked(isItemCheckedByPosition(position));
            return view;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(CLASS_TAG, "onActivityResult requestCode = " + requestCode + ", resultCode = "
                + resultCode + ", data = " + data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            mSettingData = data.getExtras();
            Log.i(CLASS_TAG, "onActivityResult mSettingData = " + mSettingData);
            if (mSettingData != null) {
                Log.i(CLASS_TAG,
                        "onActivityResult mSettingData = "
                                + mSettingData.getString(Constants.DATE));
            }
        } else {
            Log.w(CLASS_TAG, "Intent data is null !!!");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSettingData != null) {
            outState.putBundle(Constants.DATA_TITLE, mSettingData);
        }
    }

    public static SharedPreferences getInstance(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = context.getSharedPreferences(Constants.SETTINGINFO,
                    Activity.MODE_PRIVATE);
        }
        return sSharedPreferences;
    }

    @Override
    public void startPersonalDataBackup(String folderName) {

    }
}

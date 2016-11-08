package com.mediatek.backuprestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;
import com.mediatek.backuprestore.utils.Constants.ModulePath;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

public class StorageSettingsActivity extends PreferenceActivity {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/StorageSettingsActivity";
    private StorageManager mStorageManager = null;
    private String mPhoneTittl;
    private String mSdcardTittl;
    private boolean mIsMount;
    private boolean mIsOnlyPhoneStorage;
    private static SharedPreferences sSharedPreferences;
    private String mCustomizeSummary;
    CheckBoxPreference mPhonePreference;
    CheckBoxPreference mSdcardPreference;
    CheckBoxPreference mCustomize;
    List<SettingData> mSettingDataList;
    String mStoragePath = null;
    SDCardStatusChangedListener mSDCardStatusChangedListener = new SDCardStatusChangedListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CLASS_TAG, "~~ onCreate ~");
        addPreferencesFromResource(R.xml.pref_storage_settings);
        mSettingDataList = initData(savedInstanceState);

        addResultsAsPreferences(mSettingDataList);
        registerSDCardListener();
    }

    private PreferenceCategory addPreferenceCategory(PreferenceScreen ps, int titleID) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(titleID);
        ps.addPreference(category);
        return category;
    }

    private void addResultsAsPreferences(List<SettingData> mSettingDataList) {
        PreferenceScreen ps = getPreferenceScreen();
        addPreferenceCategory(ps, R.string.settings_storage);
        if (mSettingDataList != null && mSettingDataList.size() > 0 && ps != null) {
            for (SettingData mSettingData : mSettingDataList) {
                addRadioButtonPreferences(ps, mSettingData);
            }

        }
    }

    private void addRadioButtonPreferences(PreferenceScreen ps, SettingData mSettingData) {
        AppBackupRadioButtonPreference radioButtonPreference = new AppBackupRadioButtonPreference(
                this);
        if (mSettingData != null) {
            radioButtonPreference.setTitle(mSettingData.getTitle());
            if (mSettingData.getType().equals(Constants.CUSTOMIZE_STOTAGE)) {
                radioButtonPreference.setSummary(mSettingData.getSummary());
            }
            radioButtonPreference.setKey(mSettingData.getType());
            radioButtonPreference.setChecked(mSettingData.isChecked());
            if (mSettingData.getType().equals(Constants.SDCARD_STOTAGE)
                    && SDCardUtils.getSDCardDataPath(this) == null) {
                radioButtonPreference.setEnabled(false);
            } else {
                radioButtonPreference.setEnabled(true);
            }
        }
        ps.addPreference(radioButtonPreference);
    }

    private List<SettingData> initData(Bundle savedInstanceState) {
        String[] storagetitle = this.getResources().getStringArray(R.array.storage_title);
        String[] storageData = this.getResources().getStringArray(R.array.storage_list);
        String title = null;
        String summary = null;
        String key = null;
        boolean checked = true;

        List<SettingData> mSettingDataList = new ArrayList<SettingData>();

        for (int i = 0; i < storageData.length; i++) {
            if (!SDCardUtils.isSupprotSDcard(this)
                    && storageData[i].equals(Constants.SDCARD_STOTAGE)) {
                continue;
            }
            SettingData mData = new SettingData(storagetitle[i], storageData[i]);
            mSettingDataList.add(mData);
        }

        key = getPathIndexKey(this);
        mStoragePath = getCustomizePath(this);
        Log.d(CLASS_TAG, "~~ initData  ~key = " + key);
        for (SettingData mData : mSettingDataList) {
            if (!SDCardUtils.isSupprotSDcard(this)
                    && mData.getType().equals(Constants.SDCARD_STOTAGE)) {
                Log.d(CLASS_TAG, "~initData remove  = " + mData.getTitle());
                mSettingDataList.remove(mData);
            }
            if (mData.getType().equals(key)) {
                mData.setChecked(true);
            } else {
                mData.setChecked(false);
            }

            if (key.equals(Constants.CUSTOMIZE_STOTAGE)) {
                mData.setSummary(getCustomizePath(this));
            }
        }
        return mSettingDataList;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference != null && preference instanceof AppBackupRadioButtonPreference) {
            AppBackupRadioButtonPreference radioButtonPreference = (AppBackupRadioButtonPreference) preference;
            radioButtonPreference.setChecked(true);

            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                if (preferenceScreen.getPreference(i).getKey() != radioButtonPreference
                        .getKey()
                        && preferenceScreen.getPreference(i) instanceof AppBackupRadioButtonPreference) {
                    AppBackupRadioButtonPreference appBackupRadioButtonPreference = (AppBackupRadioButtonPreference) preferenceScreen
                            .getPreference(i);
                    appBackupRadioButtonPreference.setChecked(!radioButtonPreference
                            .isChecked());
                }
            }
            String key = radioButtonPreference.getKey();
            switch (key) {
            case Constants.PHONE_STOTAGE:
                mStoragePath = SDCardUtils.getPhoneDataPath(this);
                if (mStoragePath != null) {
                    setCurrentPath(this, mStoragePath);
                    setPathIndexKey(this, key);
                }
                this.finish();
                break;

            case Constants.SDCARD_STOTAGE:
                mStoragePath = SDCardUtils.getSDCardDataPath(this);
                if (mStoragePath != null) {
                    setCurrentPath(this, mStoragePath);
                    setPathIndexKey(this, key);
                }
                this.finish();
                break;

            case Constants.CUSTOMIZE_STOTAGE:
                Intent intent = new Intent(this, StorageListActivity.class);
                startActivity(intent);
                this.finish();
                break;
            default:
                break;
            }
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.DATE, mStoragePath);
        Log.d(CLASS_TAG, "~~ onSaveInstanceState ~mStoragePath = " + mStoragePath);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterSDCardListener();
    }

    @Override
    public void onBackPressed() {
        setCurrentPath(this, mStoragePath);
        Log.d(CLASS_TAG, "~~ onBackPressed :  mStoragePath = " + mStoragePath);
        super.onBackPressed();
    }

    public static SharedPreferences getInstance(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = context.getSharedPreferences(Constants.SETTINGINFO,
                    Activity.MODE_PRIVATE);
        }
        return sSharedPreferences;
    }

    public static String getCustomizePath(Context context) {
        return getInstance(context).getString("StoragePath", "");
    }

    public static String getCurrentPath(Context context) {
        return getInstance(context).getString("StoragePath", null);
    }

    public static void setCurrentPath(Context context, String path) {
        SharedPreferences.Editor editor = getInstance(context).edit();
        editor.putString("StoragePath", path);
        editor.commit();
    }

    public static void setNoticeStatus(Context context, boolean status, String key) {
        SharedPreferences.Editor editor = getInstance(context).edit();
        editor.putBoolean(key, status);
        editor.commit();
    }

    public static boolean getNoticeStatus(Context context, String key) {
        return getInstance(context).getBoolean(key, false);
    }

    public static void setPathIndexKey(Context context, String index) {
        SharedPreferences.Editor editor = getInstance(context).edit();
        editor.putString("IndexKey", index);
        editor.commit();
    }

    public static String getPathIndexKey(Context context) {
        return getInstance(context).getString("IndexKey", null);
    }

    private void registerSDCardListener() {
        SDCardReceiver.getInstance().registerOnSDCardChangedListener(
                mSDCardStatusChangedListener);
    }

    private void unRegisterSDCardListener() {
        if (SDCardReceiver.getInstance() != null) {
            SDCardReceiver.getInstance().unRegisterOnSDCardChangedListener(
                    mSDCardStatusChangedListener);
        }
    }

    class SDCardStatusChangedListener implements
            SDCardReceiver.OnSDCardStatusChangedListener {

        @Override
        public void onSDCardStatusChanged(boolean mount, String path) {
            getPreferenceScreen().removeAll();
            Bundle mSettingData = null;
            if (!mount) {
                String sdcardPath = SDCardUtils.getSdCardMountPath(getApplicationContext());
                if (sdcardPath != null && mStoragePath.contains(sdcardPath)) {
                    mStoragePath = SDCardUtils.getPhoneDataPath(getApplicationContext())
                            + File.separator + ModulePath.FOLDER_BACKUP;
                    mSettingData = new Bundle();
                    mSettingData.putString(Constants.DATE, mStoragePath);
                }
            }
            MyLogger.logD(CLASS_TAG, "onSDCardStatusChanged - mount = " + mount
                    + " mStoragePath = " + mStoragePath);
            mSettingDataList = initData(mSettingData);
            addResultsAsPreferences(mSettingDataList);
        }
    }
}

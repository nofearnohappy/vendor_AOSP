/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediatek.setting;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.android.mms.ui.AsyncDialog;
import com.android.mms.ui.FontSizeDialogAdapter;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.NumberPickerDialog;
import com.android.mms.ui.UriImage;
import com.android.mms.ui.NumberPickerDialog.OnNumberSetListener;
import com.android.mms.util.Recycler;

import android.telephony.SubscriptionInfo;
import android.provider.Telephony;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MmsLog;
import com.mediatek.cb.cbsettings.CellBroadcastActivity;
import com.mediatek.mms.ext.IOpGeneralPreferenceActivityExt;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.setting.SimStateMonitor.SimStateListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class GeneralPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SimStateListener {
    private static final String TAG = "GeneralPreferenceActivity";

    // Symbolic names for the keys used for preference lookup
    public static final String GENERAL_CHAT_WALLPAPER = "pref_key_chat_wallpaper";

    public static final String SHOW_EMAIL_ADDRESS = "pref_key_show_email_address";

    public static final String BACKUP_MESSAGE = "pref_key_backup_message";

    public static final String RESTORE_MESSAGE = "pref_key_restore_message";

    public static final String AUTO_DELETE = "pref_key_auto_delete";

    public static final String CELL_BROADCAST = "pref_key_cell_broadcast";

    public static final String STORAGE_SETTING = "pref_key_storage_settings";

    public static final String DISPLAY_PREFERENCE = "pref_key_display_preference_settings";

    public static final String MMS_DELETE_LIMIT = "pref_key_mms_delete_limit";

    public static final String SMS_DELETE_LIMIT = "pref_key_sms_delete_limit";

    public static final String WAPPUSH_AUTO_DOWNLOAD = "pref_key_wappush_sl_autoloading";

    public static final String WAPPUSH_SETTING = "pref_key_wappush_settings";

    public static final String WAPPUSH_ENABLED = "pref_key_wappush_enable";

    private static final String MAX_SMS_PER_THREAD = "MaxSmsMessagesPerThread";

    private static final String MAX_MMS_PER_THREAD = "MaxMmsMessagesPerThread";

    private static final String MMS_PREFERENCE = "com.android.mms_preferences";

    public static final String CHAT_SETTINGS_URI = "content://mms-sms/thread_settings";

    public static final String GENERAL_WALLPAPER_FOR_PROVIDER =
                    "/data/data/com.android.providers.telephony/" +
                    "app_wallpaper/general_wallpaper.jpeg";

    private IOpGeneralPreferenceActivityExt mOpGeneralPreferenceActivityExt;

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;

    private Preference mChatWallpaperPref;

    private Preference mSmsLimitPref;

    private Preference mMmsLimitPref;

    private Recycler mSmsRecycler;

    private Recycler mMmsRecycler;

    private Preference mCBsettingPref;

    private Preference mFontSize;

    private AlertDialog mFontSizeDialog;

    private String[] mFontSizeChoices;

    private String[] mFontSizeValues;

    private CheckBoxPreference mShowEmailPref;

    private static final int FONT_SIZE_DIALOG = 10;

    public static final String FONT_SIZE_SETTING = "pref_key_message_font_size";

    public static final String TEXT_SIZE = "message_font_size";

    public static final int TEXT_SIZE_DEFAULT = 18;

    private Preference mCellBroadcastMultiSub;

    private NumberPickerDialog mSmsDisplayLimitDialog;

    private NumberPickerDialog mMmsDisplayLimitDialog;

    private static final String LOCATION_PHONE = "Phone";

    private static final String LOCATION_SIM = "Sim";

    private Handler mSMSHandler = new Handler();

    private Handler mMMSHandler = new Handler();

    private ProgressDialog mProgressDialog = null;

    public String SUB_TITLE_NAME = "sub_title_name";

    private String mChatWallpaperUri = "";

    private String mWallpaperPathForCamera = "";

    private static final int PICK_WALLPAPER = 2;

    private static final int PICK_GALLERY = 3;

    private static final int PICK_PHOTO = 4;

    private static final int MMS_SIZE_LIMIT_DEFAULT = 1000;

    private static final int SMS_SIZE_LIMIT_DEFAULT = 10000;
  /// M: fix bug ALPS01523754.set google+ pic as wallpaper.@{
    private AsyncDialog mAsyncDialog;
/// @}
    private int[] mWallpaperImage = new int[] {R.drawable.wallpaper_launcher_wallpaper,
        R.drawable.wallpaper_launcher_gallery, R.drawable.wallpaper_launcher_camera,
        R.drawable.wallpaper_launcher_default, };

    private int[] mWallpaperText = new int[] {R.string.dialog_wallpaper_chooser_wallpapers,
        R.string.dialog_wallpaper_chooser_gallery, R.string.dialog_wallpaper_chooser_take,
        R.string.dialog_wallpaper_chooser_default};

    /// M: add for plugin
    @Override
    protected void onPause() {
        super.onPause();
        if (mSmsDisplayLimitDialog != null) {
            mSmsDisplayLimitDialog.dismiss();
        }
        if (mMmsDisplayLimitDialog != null) {
            mMmsDisplayLimitDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        if (null != mProgressDialog && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG, "onCreate");
        if (icicle != null && icicle.containsKey("wallpaperCameraPath")) {
           mWallpaperPathForCamera = icicle.getString("wallpaperCameraPath", "");
        }
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.actionbar_general_setting));
        actionBar.setDisplayHomeAsUpEnabled(true);
        /// M: add for plugin
        setMessagePreferences();
    }

    /// KK migration, for default MMS function. @{
    @Override
    protected void onResume() {
        super.onResume();
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        MmsLog.d(TAG, "onResume sms enable? " + isSmsEnabled);
        if (!isSmsEnabled) {
            finish();
        }
    }
    /// @}

    @Override
    protected void onStart() {
        super.onStart();
        SimStateMonitor.getInstance().addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SimStateMonitor.getInstance().removeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        MmsLog.d(TAG, "mWallpaperPathForCamera: " + mWallpaperPathForCamera);
        outState.putString("wallpaperCameraPath", mWallpaperPathForCamera);
    }

    private void setMessagePreferences() {
        addPreferencesFromResource(R.xml.generalpreferences);
        mShowEmailPref = (CheckBoxPreference) findPreference(SHOW_EMAIL_ADDRESS);
        PreferenceCategory displayOptions = (PreferenceCategory) findPreference(DISPLAY_PREFERENCE);
        displayOptions.removePreference(mShowEmailPref);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        MmsLog.d(TAG, "email address check = " + sp.getBoolean(mShowEmailPref.getKey(), true));
        if (mShowEmailPref != null) {
            mShowEmailPref.setChecked(sp.getBoolean(mShowEmailPref.getKey(), true));
        }

        PreferenceCategory storageCategory = (PreferenceCategory) findPreference(STORAGE_SETTING);
        mOpGeneralPreferenceActivityExt = OpMessageUtils.getOpMessagePlugin()
                .getOpGeneralPreferenceActivityExt();
        mOpGeneralPreferenceActivityExt.setMessagePreferences(
                GeneralPreferenceActivity.this, storageCategory);

        mFontSize = (Preference) findPreference(FONT_SIZE_SETTING);
        mFontSizeChoices = getResourceArray(R.array.pref_message_font_size_choices);
        mFontSizeValues = getResourceArray(R.array.pref_message_font_size_values);
        mFontSize = (Preference) findPreference(FONT_SIZE_SETTING);
        mFontSize.setSummary(mFontSizeChoices[getPreferenceValueInt(FONT_SIZE_SETTING, 0)]);
        mChatWallpaperPref = findPreference(GENERAL_CHAT_WALLPAPER);
        // if (FeatureOption.MTK_GMO_ROM_OPTIMIZE) {
        removeWallPaperSetting();
        // }
        mCBsettingPref = findPreference(CELL_BROADCAST);
        if (SimStateMonitor.getInstance().getSubCount() < 1) {
            mCBsettingPref.setEnabled(false);
        }
        mSmsLimitPref = findPreference(SMS_DELETE_LIMIT);
        mMmsLimitPref = findPreference(MMS_DELETE_LIMIT);
        if (!MmsConfig.getMmsEnabled()) {
            PreferenceCategory storageOptions
                    = (PreferenceCategory) findPreference(STORAGE_SETTING);
            storageOptions.removePreference(findPreference(MMS_DELETE_LIMIT));
        }
        enablePushSetting();
        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();
        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();
        // Change the key to the SIM-related key, if has one SIM card, else set default value.
        if (SimStateMonitor.getInstance().getSubCount() > 1) {
            setMultiCardPreference();
        }
        mChatWallpaperUri = sp.getString(GENERAL_CHAT_WALLPAPER, "");
    }

    public void removeWallPaperSetting() {
        PreferenceCategory displayOptions = (PreferenceCategory) findPreference(DISPLAY_PREFERENCE);
        displayOptions.removePreference(findPreference(GENERAL_CHAT_WALLPAPER));
    }

    private void setMultiCardPreference() {
        // MTK_OP02_PROTECT_END
        mCellBroadcastMultiSub = findPreference(CELL_BROADCAST);
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(getString(R.string.pref_summary_delete_limit,
                mMmsRecycler.getMessageLimit(this)));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(getString(R.string.pref_summary_delete_limit,
                mSmsRecycler.getMessageLimit(this)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTORE_DEFAULTS:
            restoreDefaultPreferences();
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSmsLimitPref) {
            mSmsDisplayLimitDialog = new NumberPickerDialog(this, mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this), mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(), R.string.pref_title_sms_delete);
            mSmsDisplayLimitDialog.show();
        } else if (preference == mCellBroadcastMultiSub) {
            Intent it = new Intent();
            it.setClass(this, SubSelectActivity.class);
            it.putExtra(SmsPreferenceActivity.PREFERENCE_KEY, preference.getKey());
            it.putExtra(SmsPreferenceActivity.PREFERENCE_TITLE_ID, R.string.cell_broadcast);
            startActivity(it);
        } else if (preference == mMmsLimitPref) {
            mMmsDisplayLimitDialog = new NumberPickerDialog(this, mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this), mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(), R.string.pref_title_mms_delete);
            mMmsDisplayLimitDialog.show();
        } else if (preference == mCBsettingPref) {
            if (SimStateMonitor.getInstance().getSubCount() == 0) {
                MmsLog.d(TAG, "there is no sim card");
                return true;
            }
            int subId = SimStateMonitor.getInstance().getSubInfoList().get(0).getSubscriptionId();
            MmsLog.d(TAG, "mCBsettingPref subId is : " + subId);
            if (FeatureOption.MTK_C2K_SUPPORT && MessageUtils.isUSimType(subId)) {
                showToast(R.string.cdma_not_support);
            } else {
                Intent it = new Intent();
                it.setClass(this, CellBroadcastActivity.class);
                it.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
                it.putExtra(SUB_TITLE_NAME, SimStateMonitor.getInstance()
                        .getSubInfoList().get(0).getDisplayName().toString());
                startActivity(it);
            }
        } else if (preference == mFontSize) {
            showDialog(FONT_SIZE_DIALOG);
        } else if (preference == mChatWallpaperPref) {
            pickChatWallpaper();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        SharedPreferences.Editor editor
                = PreferenceManager.getDefaultSharedPreferences(
                        GeneralPreferenceActivity.this).edit();
        editor.putInt(FONT_SIZE_SETTING, 0);
        editor.putFloat(TEXT_SIZE, Float.parseFloat(mFontSizeValues[0]));
        editor.putBoolean(AUTO_DELETE, false);
        editor.putInt(MAX_SMS_PER_THREAD, SMS_SIZE_LIMIT_DEFAULT);
        editor.putInt(MAX_MMS_PER_THREAD, MMS_SIZE_LIMIT_DEFAULT);
        editor.putBoolean(CELL_BROADCAST, false);
        /// M: fix bug ALPS00759844, WAPPUSH_ENABLED should be true.
        editor.putBoolean(WAPPUSH_ENABLED, true);
        editor.putBoolean(WAPPUSH_AUTO_DOWNLOAD, false);
        /// M: fix bug ALPS00432361, restore default preferences
        /// about GroupMms and ShowEmailAddress @{
        editor.putBoolean(SHOW_EMAIL_ADDRESS, true);
        /// @}
        editor.apply();
        setPreferenceScreen(null);
        clearWallpaperAll();
        setMessagePreferences();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case FONT_SIZE_DIALOG:
            FontSizeDialogAdapter adapter = new FontSizeDialogAdapter(
                    GeneralPreferenceActivity.this, mFontSizeChoices, mFontSizeValues);
            mFontSizeDialog = new AlertDialog.Builder(GeneralPreferenceActivity.this).setTitle(
                R.string.message_font_size_dialog_title).setNegativeButton(
                        R.string.message_font_size_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mFontSizeDialog.dismiss();
                    }
                }).setSingleChoiceItems(adapter, getFontSizeCurrentPosition(),
                        new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        GeneralPreferenceActivity.this).edit();
                    editor.putInt(FONT_SIZE_SETTING, which);
                    editor.putFloat(TEXT_SIZE, Float.parseFloat(mFontSizeValues[which]));
                    editor.apply();
                    mFontSizeDialog.dismiss();
                    mFontSize.setSummary(mFontSizeChoices[which]);
                }
            }).create();
            mFontSizeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    GeneralPreferenceActivity.this.removeDialog(FONT_SIZE_DIALOG);
                }
            });
            return mFontSizeDialog;
        }
        return super.onCreateDialog(id);
    }

    /*
     * Notes: if wap push is not support, wap push setting should be removed
     */
    private void enablePushSetting() {
        PreferenceCategory wapPushOptions = (PreferenceCategory) findPreference(WAPPUSH_SETTING);
        if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
            if (!MmsConfig.getSlAutoLanuchEnabled()) {
                wapPushOptions.removePreference(findPreference(WAPPUSH_AUTO_DOWNLOAD));
            }
        } else {
            if (getPreferenceScreen() != null) {
                getPreferenceScreen().removePreference(wapPushOptions);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        // / for Sms&Mms
        return true;
    }

    private CharSequence getVisualTextName(
            String enumName, int choiceNameResId, int choiceValueResId) {
        CharSequence[] visualNames = getResources().getTextArray(choiceNameResId);
        CharSequence[] enumNames = getResources().getTextArray(choiceValueResId);
        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }
        for (int i = 0; i < enumNames.length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener
            = new NumberPickerDialog.OnNumberSetListener() {
        public void onNumberSet(int limit) {
            if (limit <= mSmsRecycler.getMessageMinLimit()) {
                limit = mSmsRecycler.getMessageMinLimit();
            } else if (limit >= mSmsRecycler.getMessageMaxLimit()) {
                limit = mSmsRecycler.getMessageMaxLimit();
            }
            mSmsRecycler.setMessageLimit(GeneralPreferenceActivity.this, limit);
            setSmsDisplayLimit();
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = ProgressDialog.show(
                        GeneralPreferenceActivity.this, "", getString(R.string.deleting), true);
            }
            mSMSHandler.post(new Runnable() {
                public void run() {
                    new Thread(new Runnable() {
                        public void run() {
                            Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());
                            if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
                                Recycler.getWapPushRecycler().deleteOldMessages(
                                        getApplicationContext());
                            }
                            if (null != mProgressDialog && mProgressDialog.isShowing()) {
                                mProgressDialog.dismiss();
                            }
                        }
                    }, "DeleteSMSOldMsgAfterSetNum").start();
                }
            });
        }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener
            = new NumberPickerDialog.OnNumberSetListener() {
        public void onNumberSet(int limit) {
            if (limit <= mMmsRecycler.getMessageMinLimit()) {
                limit = mMmsRecycler.getMessageMinLimit();
            } else if (limit >= mMmsRecycler.getMessageMaxLimit()) {
                limit = mMmsRecycler.getMessageMaxLimit();
            }
            mMmsRecycler.setMessageLimit(GeneralPreferenceActivity.this, limit);
            setMmsDisplayLimit();
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = ProgressDialog.show(
                        GeneralPreferenceActivity.this, "", getString(R.string.deleting),
                    true);
            }
            mMMSHandler.post(new Runnable() {
                public void run() {
                    new Thread(new Runnable() {
                        public void run() {
                            MmsLog.d("Recycler", "mMmsLimitListener");
                            Recycler.getMmsRecycler().deleteOldMessages(getApplicationContext());
                            if (null != mProgressDialog && mProgressDialog.isShowing()) {
                                mProgressDialog.dismiss();
                            }
                        }
                    }, "DeleteMMSOldMsgAfterSetNum").start();
                }
            });
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged: newConfig = " + newConfig + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        setTheme(R.style.MmsTheme);
        this.getListView().clearScrapViewsIfNeeded();
    }
    // MTK_OP01_PROTECT_START
    private String[] getFontSizeArray(int resId) {
        return getResources().getStringArray(resId);
    }

    private int getFontSizeCurrentPosition() {
        SharedPreferences sp = getSharedPreferences(MMS_PREFERENCE, MODE_WORLD_READABLE);
        return sp.getInt(FONT_SIZE_SETTING, 0);
    }

    // MTK_OP01_PROTECT_END
    private void showToast(int id) {
        Toast t = Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_SHORT);
        t.show();
    }

    private int getPreferenceValueInt(String key, int defaultValue) {
        SharedPreferences sp = getSharedPreferences(
                "com.android.mms_preferences", MODE_WORLD_READABLE);
        return sp.getInt(key, defaultValue);
    }

    private String[] getResourceArray(int resId) {
        return getResources().getStringArray(resId);
    }

    public void pickChatWallpaper() {
        AlertDialog.Builder wallpaperDialog = new AlertDialog.Builder(this);
        ArrayList<HashMap<String, Object>> wallpaper = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < 4; i++) {
            HashMap<String, Object> hm = new HashMap<String, Object>();
            hm.put("ItemImage", mWallpaperImage[i]);
            hm.put("ItemText", getResources().getString(mWallpaperText[i]));
            wallpaper.add(hm);
        }
        SimpleAdapter wallpaperDialogAdapter
                = new SimpleAdapter(GeneralPreferenceActivity.this, wallpaper,
                R.layout.wallpaper_item_each, new String[] {"ItemImage", "ItemText"}, new int[] {
                    R.id.wallpaperitemeachimageview, R.id.wallpaperitemeachtextview});
        LayoutInflater mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = mInflater.inflate(R.layout.wallpaper_chooser_gridview_dialog,
            (ViewGroup) findViewById(R.id.forwallpaperchooser));
        GridView gv = (GridView) layout.findViewById(R.id.wallpaperchooserdialog);
        gv.setAdapter(wallpaperDialogAdapter);
        final AlertDialog wallpaperChooser = wallpaperDialog.setTitle(
            getResources().getString(R.string.dialog_wallpaper_title)).setView(layout).create();
        wallpaperChooser.show();
        gv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (arg2) {
                case 0:
                    MmsLog.d(TAG, "system begin");
                    pickSysWallpaper();
                    wallpaperChooser.dismiss();
                    break;
                case 1:
                    pickWallpaperFromGallery();
                    wallpaperChooser.dismiss();
                    break;
                case 2:
                    pickWallpaperFromCam();
                    wallpaperChooser.dismiss();
                    break;
                case 3:
                    new Thread() {
                        public void run() {
                            boolean isClearAll = clearWallpaperAll();
                            showSaveWallpaperResult(isClearAll);
                        }
                    } .start();
                    wallpaperChooser.dismiss();
                    break;
                default:
                    break;
                }
            }
        });
    }

    public boolean clearWallpaperAll() {
        ContentValues cv = new ContentValues();
        cv.put(Telephony.ThreadSettings.WALLPAPER, "");
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), 0);
        int i = getContentResolver().update(uri, cv, null, null);
        if (i > 0) {
            return true;
        }
        return false;
    }

    private void pickWallpaperFromCam() {
        if (getSDCardPath(this) != null) {
            mWallpaperPathForCamera = getSDCardPath(this) + File.separator
                + "Message_WallPaper" + File.separator
                + "general_wallpaper_" + System.currentTimeMillis() + ".jpeg";
            File out = new File(mWallpaperPathForCamera);
            if (!out.getParentFile().exists()) {
                out.getParentFile().mkdirs();
            }
            Uri mWallpaperTakeuri = Uri.fromFile(out);
            Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mWallpaperTakeuri);
            MmsLog.d(TAG, "MediaStoreUri: " + mWallpaperTakeuri);
            try {
                startActivityForResult(imageCaptureIntent, PICK_PHOTO);
            } catch (ActivityNotFoundException e) {
                MmsLog.d(TAG, "pickWallpaperFromCam, ActivityNotFoundException.");
            }
        } else {
            MmsLog.d(TAG, "SDcard not esisted ");
            Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                startActivityForResult(imageCaptureIntent, PICK_PHOTO);
            } catch (ActivityNotFoundException e) {
                MmsLog.d(TAG, "pickWallpaperFromCam, ActivityNotFoundException2.");
            }
        }
    }

    private void pickWallpaperFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "Gallery"), PICK_GALLERY);
        } catch (ActivityNotFoundException e) {
            MmsLog.d(TAG, "pickWallpaperFromGallery, ActivityNotFoundException.");
        }
    }

    public static String getSDCardPath(Context c) {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();
        if (sdStatus != null) {
            boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();
                return sdDir.toString();
            } else {
                return null;
            }
        }
        return null;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case PICK_PHOTO:
                pickWallpaperFromCamResult();
                break;
            case PICK_WALLPAPER:
                pickWallpaperFromSys(data);
                MmsLog.d(TAG, "sytem result");
                break;
            case PICK_GALLERY:
                pickWallpaperFromGalleryResult(data);
                break;
            default:
                break;
            }
            return;
        } else if (resultCode == RESULT_CANCELED) {
            MmsLog.d(TAG, "nothing selected");
            return;
        }
    }

    private void pickWallpaperFromGalleryResult(final Intent data) {
        if (null == data) {
            return;
        }
        final Uri mChatWallpaperGalleryUri = data.getData();
        MmsLog.d(TAG, "Save wallpaper Gallery Uri: " + mChatWallpaperGalleryUri);
        /// M: fix bug ALPS01523754.set google+ pic as wallpaper.@{
        if (MessageUtils.isGooglePhotosUri(mChatWallpaperGalleryUri)) {
            MmsLog.d(TAG, "isGooglePhotosUri == true");
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    String wallpapertempfilePath = MessageUtils.getTempWallpaper(
                            getApplicationContext(), mChatWallpaperGalleryUri);
                    final String chatWallpaperCompressForGallery
                            = compressAndRotateForMemory(wallpapertempfilePath);
                    new Thread() {
                        public void run() {
                            boolean isSaveForGallery
                                    = saveWallpaperToMemory(chatWallpaperCompressForGallery);
                            showSaveWallpaperResult(isSaveForGallery);
                        }
                    } .start();
                }
            }, null, R.string.adding_wallpaper_title);
            return;
        }
        /// @}

        Cursor c = getContentResolver().query(mChatWallpaperGalleryUri, new String[] {
                MediaStore.Images.Media.DATA
        },
                null, null, null);
        String wallpaperPathForGallery = "";
        if (c != null) {
            try {
                if (c.getCount() == 0) {
                    MmsLog.d(TAG, "[pickWallpaperFromGalleryResult] c.getCount() == 0");
                    return;
                } else {
                    c.moveToFirst();
                    wallpaperPathForGallery = c.getString(0);
                }
            } finally {
                c.close();
            }
        } else {
            String scheme = mChatWallpaperGalleryUri.getScheme();
            if (scheme != null && scheme.equals("file")) {
                String path = mChatWallpaperGalleryUri.getPath();
                File file = new File(path);
                if (file != null && file.isFile() && file.exists()) {
                    wallpaperPathForGallery = path;
                } else {
                    MmsLog.d(TAG, "[pickWallpaperFromGalleryResult] The path haven't file");
                    return;
                }
            } else {
                MmsLog.d(TAG, "[pickWallpaperFromGalleryResult] isn't file uri");
                return;
            }
        }

        MmsLog.d(TAG, "Save wallpaper Gallery Path: " + wallpaperPathForGallery);
        final String chatWallpaperCompressForGallery
                = compressAndRotateForMemory(wallpaperPathForGallery);
        new Thread() {
            public void run() {
                boolean isSaveForGallery = saveWallpaperToMemory(chatWallpaperCompressForGallery);
                showSaveWallpaperResult(isSaveForGallery);
            }
        }.start();
        return;
    }

    void showSaveWallpaperResult(boolean isShow) {
        if (isShow) {
                    GeneralPreferenceActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.save_wallpaper_success),
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    GeneralPreferenceActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.save_wallpaper_fail),
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

    private void pickWallpaperFromCamResult() {
        final String chatWallpaperCompressForCamera
                = compressAndRotateForMemory(mWallpaperPathForCamera);
        new Thread() {
            public void run() {
                boolean isSaveForCamera = saveWallpaperToMemory(chatWallpaperCompressForCamera);
                showSaveWallpaperResult(isSaveForCamera);
            }
        } .start();
        return;
    }

    private boolean saveWallpaperToMemory(String oldWallpaper) {
        if (oldWallpaper == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(Telephony.ThreadSettings.WALLPAPER, GENERAL_WALLPAPER_FOR_PROVIDER);
        /*getContentResolver().update(THREAD_SETTINGS_ID_URI, cv, null, null);
        Cursor c = getApplicationContext().getContentResolver().query(THREAD_SETTINGS_ID_URI,
            new String[] {ThreadSettings.THREAD_ID}, null, null, null);
        if (c == null) {
            MmsLog.d(TAG, "cursor is null.");
            return false;
        }
        boolean isSaveSuccess = false ;
        try {
            if (c.getCount() > 0) {
                c.moveToFirst();
                int firstThreadId = c.getInt(0);
                Uri uri = ContentUris.withAppendedId(THREAD_SETTINGS_ID_URI, firstThreadId);
                try {
                    OutputStream o = getContentResolver().openOutputStream(uri);
                    Bitmap bm = BitmapFactory.decodeFile(oldWallpaper);
                    isSaveSuccess = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
                    MmsLog.d(TAG, "decodeFile over");
                    if (o != null) {
                        o.close();
                    }
                    if (bm != null) {
                        bm.recycle();
                    }
                    File tempFile = new File(oldWallpaper);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (FileNotFoundException e) {
                    MmsLog.d(TAG, "FileNotFoundException", e);
                } catch (IOException e) {
                    MmsLog.d(TAG, "IOException", e);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            return isSaveSuccess;
        }*/
        boolean isSaveSuccess = false ;
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), 0);
        getContentResolver().update(uri, cv, null, null);
        try {
            OutputStream o = getContentResolver().openOutputStream(uri);
            Bitmap bm = BitmapFactory.decodeFile(oldWallpaper);
            isSaveSuccess = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
            MmsLog.d(TAG, "decodeFile over");
            if (o != null) {
                o.close();
            }
            if (bm != null) {
                bm.recycle();
            }
            File tempFile = new File(oldWallpaper);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (FileNotFoundException e) {
            MmsLog.d(TAG, "FileNotFoundException", e);
        } catch (IOException e) {
            MmsLog.d(TAG, "IOException", e);
        } finally {
            return isSaveSuccess;
        }
    }

    public File getAlbumStorageDir(String wallpaperDirName) {
       // Get the directory for the app's private pictures directory.
       File file = new File(getApplicationContext().getExternalFilesDir(
               Environment.DIRECTORY_PICTURES), wallpaperDirName);
       if (!file.mkdirs()) {
           MmsLog.d(TAG,  "Directory not created");
       }
       return file;
    }

    private String compressAndRotateForMemory(String wallpaperCache) {
        String fosFileName = null;
        // if wallpaperCache is null, do nothing.
        if (wallpaperCache == null) {
            return null;
        }
        File mChatWallpaperPStore = new File(wallpaperCache);
        String chatWallpaperUri = mChatWallpaperUri;

        if (mChatWallpaperPStore.exists()) {
            File mChatWallpaperMemory = getAlbumStorageDir("wallpaper_tmp");

            MmsLog.d(TAG, "mChatWallpapterMemory " + mChatWallpaperMemory.getName());
            fosFileName = mChatWallpaperMemory + "/general_wallpaper_"
                    + System.currentTimeMillis() + ".jpeg";
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(fosFileName, true);
            } catch (FileNotFoundException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, FileNotFoundException");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(wallpaperCache, options);
            options.inJustDecodeBounds = false;
            int wallpaperHeight = options.outHeight;
            int wallpaperWidth = options.outWidth;
            MmsLog.d(TAG, "wallpaperHeight = " + wallpaperHeight
                    + " wallpaperWidth = " + wallpaperWidth);
            int ratio = MessageUtils.calculateWallpaperSize(
                    getApplicationContext(), wallpaperHeight, wallpaperWidth);
            MmsLog.d(TAG, "ratio: " + ratio);
            options.inSampleSize = ratio;
            int orientation = 0;
            int degree = 0;
            boolean isCopyed = false;
            try {
                ExifInterface exif = new ExifInterface(wallpaperCache);
                if (exif != null) {
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                    degree = UriImage.getExifRotation(orientation);
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, FileNotFoundException1");
            }
            Bitmap bm = BitmapFactory.decodeFile(wallpaperCache, options);
            if (bm != null) {
                bm = UriImage.rotate(bm, degree);
                isCopyed = bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
            try {
                if (fos != null) {
                    fos.close();
                }
                if (bm != null) {
                    bm.recycle();
                }
                if (!isCopyed) {
                    return null;
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, FileNotFoundException2");
            }
            try {
                fos.close();
                if (bm != null) {
                    bm.recycle();
                }
            } catch (IOException e) {
                MmsLog.d(TAG, "compressAndRotateForMemory, FileNotFoundException3");
            }
            MmsLog.d(TAG, "isCopyed: " + isCopyed);
            if (!isCopyed) {
                chatWallpaperUri = mChatWallpaperPStore.getPath();
                return chatWallpaperUri;
            }
            chatWallpaperUri = fosFileName;
        }
        return chatWallpaperUri;
    }

    private void pickSysWallpaper() {
        Intent intent = new Intent(this, WallpaperChooser.class);
        startActivityForResult(intent, PICK_WALLPAPER);
    }

    private void pickWallpaperFromSys(Intent data) {
        final int sourceId = data.getIntExtra("wallpaper_index", -1);
        MmsLog.d(TAG, "sourceId: " + sourceId);
        new Thread() {
            public void run() {
                boolean isSaveForSystem = saveResourceWallpaperToMemory(sourceId);
                showSaveWallpaperResult(isSaveForSystem);
            }
        } .start();
    }

    private boolean saveResourceWallpaperToMemory(int resourceId) {
        Resources r = getResources();
        InputStream is = null;
        try {
            is = r.openRawResource(resourceId);
        } catch (NotFoundException e) {
            MmsLog.d(TAG, "NotFoundException", e);
        }
        Bitmap bm = BitmapFactory.decodeStream(is);
        ContentValues cv = new ContentValues();
        cv.put(Telephony.ThreadSettings.WALLPAPER, GENERAL_WALLPAPER_FOR_PROVIDER);
        /*getContentResolver().update(THREAD_SETTINGS_ID_URI, cv, null, null);
        Cursor c = getApplicationContext().getContentResolver().query(THREAD_SETTINGS_ID_URI,
            new String[] {ThreadSettings.THREAD_ID}, null, null, null);
        if (c == null) {
            MmsLog.d(TAG, "cursor is null.");
            return false;
        }
        boolean isSaveSuccessed = false;
        try {
            if (c.getCount() > 0) {
                c.moveToFirst();
                int firstThreadId = c.getInt(0);
                Uri uri = ContentUris.withAppendedId(THREAD_SETTINGS_ID_URI, firstThreadId);
                try {
                    OutputStream o = getContentResolver().openOutputStream(uri);
                    isSaveSuccessed = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
                    MmsLog.d(TAG, "decodeFile over");
                    if (o != null) {
                        o.close();
                    }
                    if (bm != null) {
                        bm.recycle();
                    }
                } catch (FileNotFoundException e) {
                    MmsLog.d(TAG, "FileNotFoundException", e);
                } catch (IOException e) {
                    MmsLog.d(TAG, "IOException", e);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            return isSaveSuccessed;
        }*/
        boolean isSaveSuccessed = false;
        Uri uri = ContentUris.withAppendedId(Uri.parse(CHAT_SETTINGS_URI), 0);
        getContentResolver().update(uri, cv, null, null);
        try {
            OutputStream o = getContentResolver().openOutputStream(uri);
            isSaveSuccessed = bm.compress(Bitmap.CompressFormat.JPEG, 100, o);
            MmsLog.d(TAG, "decodeFile over");
            if (o != null) {
                o.close();
            }
            if (bm != null) {
                bm.recycle();
            }
        } catch (FileNotFoundException e) {
            MmsLog.d(TAG, "FileNotFoundException", e);
        } catch (IOException e) {
            MmsLog.d(TAG, "IOException", e);
        } finally {
            return isSaveSuccessed;
        }
    }
    /// M: fix bug ALPS01523754.set google+ pic as wallpaper.@{
    private AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }
    /// @}

    @Override
    public void onSimStateChanged() {
        MmsLog.d(TAG, "onSimStateChanged");
        if (SimStateMonitor.getInstance().getSubCount() < 1) {
            mCBsettingPref.setEnabled(false);
        } else {
            mCBsettingPref.setEnabled(true);
        }
    }
}

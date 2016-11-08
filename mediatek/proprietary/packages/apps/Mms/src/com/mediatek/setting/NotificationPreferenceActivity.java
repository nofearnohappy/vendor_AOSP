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
import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import android.telephony.SubscriptionManager;

import com.android.mms.ui.MessageUtils;
import com.android.mms.util.MmsLog;


/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class NotificationPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationPreferenceActivity";

    private static final boolean DEBUG = false;

    // Symbolic names for the keys used for preference lookup
    public static final String NOTIFICATION_MUTE = "pref_key_mute";

    public static final String NOTIFICATION_VIBRATE = "pref_key_vibrate";

    public static final String POPUP_NOTIFICATION = "pref_key_popup_notification";

    public static final String NOTIFICATION_ENABLED = "pref_key_enable_notifications";

    public static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";

    public static final String AUTO_RETRIEVAL = "pref_key_mms_auto_retrieval";

    public static final String MUTE_START = "mute_start";

    public static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";

    // System ring tone path start with "content://media/internal/audio/media/".
    // If the ring tone file is added by user, like put music under storage/Notifications folder,
    // then the ring tone URI start with this.
    private static final String EXTERNAL_RINGTONE_PATH = "content://media/external/audio/media/";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;

    private CheckBoxPreference mEnableNotificationsPref;

    private CheckBoxPreference mVibratePref;

    private CheckBoxPreference mPopupNotificationPref;

    private ListPreference mNotificaitonMute;

    private int mCurrentSimCount = 0;

    private RingtonePreference mRingtonePref;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /// KK migration, for default MMS function. @{
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        MmsLog.d(TAG, "onResume sms enable? " + isSmsEnabled);
        if (!isSmsEnabled) {
            finish();
            return;
        }
        /// @}
        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        setListPrefSummary();
        // for ALPS01836799, refresh ring tone summary.
        setRingtoneSummary(getMmsRingtone(this));
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG, "onCreate");
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.actionbar_notification_setting));
        actionBar.setDisplayHomeAsUpEnabled(true);
        setMessagePreferences();
    }

    private void setListPrefSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        long muteStart = sp.getLong(MUTE_START, 0);
        int muteOrigin = Integer.parseInt(sp.getString(NOTIFICATION_MUTE, "0"));
        if (muteStart > 0 && muteOrigin > 0) {
            MmsLog.d(TAG, "thread mute timeout, reset to default.");
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            if ((muteOrigin * 3600 + muteStart / 1000) <= currentTime) {
                SharedPreferences.Editor editor =
                                PreferenceManager.getDefaultSharedPreferences(
                                        getApplicationContext()).edit();
                editor.putLong(NotificationPreferenceActivity.MUTE_START, 0);
                editor.putString(NOTIFICATION_MUTE, "0");
                editor.commit();
                // Fix ALPS01088380, should call setValueIndex when preference changed.
                mNotificaitonMute.setValueIndex(0);
            }
        }
        // For notificationMute;
        String notificationMute = sp.getString(NOTIFICATION_MUTE, "0");
        mNotificaitonMute.setSummary(MessageUtils.getVisualTextName(this,
                notificationMute, R.array.pref_mute_choices,
                R.array.pref_mute_values));
    }

    private void setMessagePreferences() {
        mCurrentSimCount = SubscriptionManager.from(this).getActiveSubscriptionInfoCount();
        addPreferencesFromResource(R.xml.notificationpreferences);
        mNotificaitonMute = (ListPreference) findPreference(NOTIFICATION_MUTE);
        mNotificaitonMute.setOnPreferenceChangeListener(this);
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        mPopupNotificationPref = (CheckBoxPreference) findPreference(POPUP_NOTIFICATION);
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);
        mRingtonePref.setOnPreferenceChangeListener(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        setRingtoneSummary(soundValue);
    }

    private void setRingtoneSummary(String soundValue) {
        MmsLog.d(TAG, "setRingtoneSummary soundValue " + soundValue);
        /// for ALPS01836799, set the ring tone as DEFAULT_RINGTONE if the ring tone not exist. @{
        if (!TextUtils.isEmpty(soundValue) && soundValue.startsWith(EXTERNAL_RINGTONE_PATH)) {
            boolean isRingtoneExist = RingtoneManager.isRingtoneExist(this, Uri.parse(soundValue));
            MmsLog.d(TAG, "Ring tone is exist: " + isRingtoneExist);
            if (!isRingtoneExist) {
                restoreDefaultRingtone();
                soundValue = DEFAULT_RINGTONE;
            }
        }
        /// @}
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTORE_DEFAULTS:
            restoreDefaultPreferences();
            return true;
        case android.R.id.home:
            // The user clicked on the Messaging icon in the action bar. Take them back from
            // wherever they came from
            finish();
            return true;
        default:
            break;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(
                                NotificationPreferenceActivity.this).edit();
        editor.putBoolean(NOTIFICATION_ENABLED, true);
        editor.putString(NOTIFICATION_MUTE, "0");
        editor.putString(NOTIFICATION_RINGTONE, DEFAULT_RINGTONE);
        editor.putBoolean(NOTIFICATION_VIBRATE, true);
        editor.putBoolean(POPUP_NOTIFICATION, true);
        editor.apply();
        setPreferenceScreen(null);
        setMessagePreferences();
        setListPrefSummary();
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled
                = prefs.getBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor
                = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED, enabled);
        editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        String notificationMute = (String) newValue;
        if (NOTIFICATION_MUTE.equals(key)) {
            CharSequence mMute = MessageUtils.getVisualTextName(
                    this, notificationMute, R.array.pref_mute_choices,
                    R.array.pref_mute_values);
            mNotificaitonMute.setSummary(mMute);
            MmsLog.d(TAG, "preference change: " + mMute.toString());
            if (notificationMute.equals("0")) {
                SharedPreferences sp = PreferenceManager
                        .getDefaultSharedPreferences(NotificationPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(MUTE_START, 0);
                editor.commit();
            } else {
                Long muteTime = System.currentTimeMillis();
                SharedPreferences sp = PreferenceManager
                        .getDefaultSharedPreferences(NotificationPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(MUTE_START, muteTime);
                editor.commit();
            }
        } else if (NOTIFICATION_RINGTONE.equals(key)) {
            setRingtoneSummary((String) newValue);
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged: newConfig = " + newConfig + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        this.getListView().clearScrapViewsIfNeeded();
    }

    public static boolean isNotificationEnable() {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean enable = prefs.getBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED,
                false);
        return enable;
    }

    public static boolean isPopupNotificationEnable() {
        if (!isNotificationEnable()) {
            return false;
        }
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean enable = prefs.getBoolean(NotificationPreferenceActivity.POPUP_NOTIFICATION, true);
        return enable;
    }

    public static String getMmsRingtone(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
    }

    /**
     * Use to set DEFAULT_RINGTONE as ring tone.
     */
    private void restoreDefaultRingtone() {
        // Restore the value of ring tone in SharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();
        editor.putString(NotificationPreferenceActivity.NOTIFICATION_RINGTONE, DEFAULT_RINGTONE);
        editor.apply();
    }
}

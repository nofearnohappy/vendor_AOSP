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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.mediatek.blemanager.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.blemanager.ui.IconSeekBarPreference.OnSeekBarProgressChangedListener;

public class AlertSettingPreference extends PreferenceActivity {
    private static final String TAG = BleConstants.COMMON_TAG + "[AlertSettingPreference]";

    private static final String RANGE_ALERT_CHECK_PREFERENCE = "range_alert_check_preference";
    private static final String RINGTONE_PREFERENCE = "ringtone_preference";
    private static final String DISCONNECT_WARNING_PREFERENCE = "disconnect_warning_preference";
    private static final String VIBRATION_PREFERENCE = "vibration_preference";
    private static final String VOLUME_SEEK_BAR_PREFERENCE = "volume_seek_bar_preference";
    private static final String RANGE_ALERT_COMPOSE_PREFERENCE = "range_alert_compose_preference";
    private static final String INTENT_EXTRA_CURRENT_DEVICE = "current_device";

    private static final int REQUEST_CODE_SELLECT_RINGTONE = 1;
    private int mCurrent = -1;

    private NonChangeCheckBoxPreference mRingtonePreference;
    private CheckBoxPreference mDisWarningPreference;
    private CheckBoxPreference mRangeAlertPreference;
    private CheckBoxPreference mVibrationPreference;
    private RangeComposePreference mComposePreference;
    private IconSeekBarPreference mSeekBarPreference;

    private Switch mAlertSwitch;

    private CachedBleDeviceManager mCachedBleDeviceManager;
    private CachedBleDevice mCachedBleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        mCurrent = intent.getIntExtra(INTENT_EXTRA_CURRENT_DEVICE, -1);
        Log.i(TAG, "[onCreate]mCurrent = " + mCurrent);
        if (mCurrent == -1) {
            // TODO:"device is wrong" need to be coding as R.string.xxxx
            Toast.makeText(this, "device is wrong", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        mCachedBleDevice = mCachedBleDeviceManager.getCachedDeviceFromDisOrder(mCurrent);
        if (mCachedBleDevice == null) {
            Log.w(TAG, "[onCreate]mCachedDevice is null,finish activity.");
            finish();
            return;
        }
        mCachedBleDevice.registerAttributeChangeListener(mListener);

        addPreferencesFromResource(R.xml.alert_setting_preference);

        mRangeAlertPreference = (CheckBoxPreference) this
                .findPreference(RANGE_ALERT_CHECK_PREFERENCE);

        mRingtonePreference = (NonChangeCheckBoxPreference) this
                .findPreference(RINGTONE_PREFERENCE);

        mDisWarningPreference = (CheckBoxPreference) this
                .findPreference(DISCONNECT_WARNING_PREFERENCE);

        mVibrationPreference = (CheckBoxPreference) this.findPreference(VIBRATION_PREFERENCE);

        mComposePreference = (RangeComposePreference) this
                .findPreference(RANGE_ALERT_COMPOSE_PREFERENCE);

        mSeekBarPreference = (IconSeekBarPreference) this
                .findPreference(VOLUME_SEEK_BAR_PREFERENCE);

        // initPreferences
        mRangeAlertPreference.setOnPreferenceClickListener(mPreferenceClickListener);
        mRingtonePreference.setOnPreferenceClickListener(mPreferenceClickListener);
        mRingtonePreference.setCheckStateChangeListener(mCachedBleDevice
                .getBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG),
                mRingtoneCheckStateChangeListener);

        mDisWarningPreference.setOnPreferenceClickListener(mPreferenceClickListener);
        mVibrationPreference.setOnPreferenceClickListener(mPreferenceClickListener);
        mComposePreference.setChangeListener(new ComposeListener());
        mSeekBarPreference.setOnProgressChanged(new OnSeekBarProgressChangedListener() {
            @Override
            public void onProgressChanged(final int progress) {
                AlertSettingPreference.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCachedBleDevice.setIntAttribute(CachedBleDevice.DEVICE_VOLUME_FLAG,
                                progress);
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "[onStart]...");
        updatePreferences();
        initActionBar();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[onDestroy]...");
        if (mComposePreference != null) {
            mComposePreference.clear();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "[onActivityResult]requestCode = " + requestCode + ",resultCode = " + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELLECT_RINGTONE) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri == null) {
                    Log.w(TAG, "[onActivityResult] uri is null");
                    return;
                }
                Ringtone r = RingtoneManager.getRingtone(this, uri);
                String title = r.getTitle(this);

                mCachedBleDevice.setRingtoneUri(uri);
                String summ = RingtoneManager.getRingtone(this, mCachedBleDevice.getRingtoneUri())
                        .getTitle(this);
                Log.d(TAG, "[onActivityResult] title : " + title + ",uri = " + uri + ",summ = "
                        + summ);
                mRingtonePreference.setSummary(summ);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.i(TAG, "[onOptionsItemSelected]click home key,finish activity.");
            this.finish();
            break;
        default:
            break;
        }
        return true;
    }

    private void updatePreferences() {
        boolean checked = mCachedBleDevice
                .getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG);
        Log.d(TAG, "[updatePreferences] checked : " + checked);
        if (checked) {
            mDisWarningPreference.setEnabled(true);
            mVibrationPreference.setEnabled(true);
            if (mCachedBleDevice.isSupportPxpOptional()) {
                mRangeAlertPreference.setEnabled(true);
            } else {
                mRangeAlertPreference.setEnabled(false);
            }
            mRingtonePreference.setEnabled(true);
            mSeekBarPreference.setEnabled(true);
        } else {
            mDisWarningPreference.setEnabled(false);
            mVibrationPreference.setEnabled(false);
            mRangeAlertPreference.setEnabled(false);
            mRingtonePreference.setEnabled(false);
            mSeekBarPreference.setEnabled(false);
        }
        mRangeAlertPreference.setChecked(mCachedBleDevice
                .getBooleanAttribute(CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG));
        mDisWarningPreference
                .setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG));
        Ringtone r = RingtoneManager.getRingtone(this, mCachedBleDevice.getRingtoneUri());
        mRingtonePreference.setSummary(r.getTitle(this));
        mSeekBarPreference.setProgress(mCachedBleDevice
                .getIntAttribute(CachedBleDevice.DEVICE_VOLUME_FLAG));
        mVibrationPreference.setChecked(mCachedBleDevice
                .getBooleanAttribute(CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG));
        if (checked && mRangeAlertPreference.isChecked()) {
            mComposePreference.setEnabled(true);
        } else {
            mComposePreference.setEnabled(false);
        }
        mComposePreference.setState(mComposePreference.isEnabled(), mCachedBleDevice
                .getIntAttribute(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG), mCachedBleDevice
                .getIntAttribute(CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG));
    }

    private void initActionBar() {
        ActionBar bar = this.getActionBar();
        bar.setTitle(mCachedBleDevice.getDeviceName());
        View v = LayoutInflater.from(this).inflate(R.layout.alert_setting_action_bar_switch, null);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setCustomView(v, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        mAlertSwitch = (Switch) v.findViewById(R.id.alert_setting_menu_switch);
        mAlertSwitch.setChecked(mCachedBleDevice
                .getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG));
        mAlertSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG, arg1);
                updatePreferences();
            }
        });
    }

    private class ComposeListener implements
            RangeComposePreference.ComposePreferenceChangedListener {

        @Override
        public void onSeekBarProgressChanged(final int startPorgress, final int stopProgress) {
            AlertSettingPreference.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[onSeekBarProgressChanged] startPorgress : " + startPorgress
                            + ", stopProgress : " + stopProgress);
                    if (startPorgress == stopProgress) {
                        Log.i(TAG, "[onSeekBarProgressChanged] progress not changed!!");
                        return;
                    }
                    mCachedBleDevice.setIntAttribute(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG,
                            stopProgress);
                }
            });
        }

        @Override
        public void onRangeChanged(final boolean outRangeChecked) {
            AlertSettingPreference.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[onRangeChanged] outRangeChecked : " + outRangeChecked);
                    if (!mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG)) {
                        showInformationDialog();
                    }
                    int outChecked;
                    if (outRangeChecked) {
                        outChecked = CachedBleDevice.OUT_OF_RANGE_ALERT_VALUE;
                    } else {
                        outChecked = CachedBleDevice.IN_RANGE_ALERT_VALUE;
                    }
                    mCachedBleDevice.setIntAttribute(
                            CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG, outChecked);
                }
            });
        }
    }

    private Preference.OnPreferenceClickListener mPreferenceClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            Log.d(TAG, "[onPreferenceClick] key : " + key);
            if (RANGE_ALERT_CHECK_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG,
                        mRangeAlertPreference.isChecked());
                if (mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG)) {
                    if (mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG)) {
                        mComposePreference.setEnabled(true);
                    } else {
                        mComposePreference.setEnabled(false);
                    }
                } else {
                    mComposePreference.setEnabled(false);
                }
                mComposePreference.setState(mComposePreference.isEnabled(),
                                mCachedBleDevice
                                        .getIntAttribute(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG),
                                mCachedBleDevice
                                        .getIntAttribute(CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG));
            } else if (DISCONNECT_WARNING_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG,
                        mDisWarningPreference.isChecked());
            } else if (VIBRATION_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG,
                        mVibrationPreference.isChecked());
            } else if (RINGTONE_PREFERENCE.equals(key)) {
                showRingtoneSelector();
            }

            return true;
        }
    };

    private NonChangeCheckBoxPreference.OnCheckStateChangeListener mRingtoneCheckStateChangeListener = new NonChangeCheckBoxPreference.OnCheckStateChangeListener() {
        @Override
        public void onCheckdChangeListener(boolean checked) {
            // TODO Auto-generated method stub
            mCachedBleDevice.setBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG,
                    checked);
            mRingtonePreference.setCheckState(mCachedBleDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG));
        }
    };
    private CachedBleDevice.DeviceAttributeChangeListener mListener = new CachedBleDevice.DeviceAttributeChangeListener() {
        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int which) {
            // TODO Auto-generated method stub
        }
    };

    private void showInformationDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.range_information_dialog_layout, null);
        final CheckBox cb = (CheckBox) v.findViewById(R.id.range_information_dialog_check_box);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (cb.isChecked()) {
                    mCachedBleDevice.setBooleanAttribute(
                            CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG, true);
                }
            }
        });
        builder.create().show();
    }

    private void showRingtoneSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mCachedBleDevice.getRingtoneUri());
        this.startActivityForResult(intent, REQUEST_CODE_SELLECT_RINGTONE);
    }

}

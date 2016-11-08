/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.settings.plugin;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.settings.ext.DefaultAudioProfileExt;
import com.mediatek.telephony.SmsManagerEx;

/**
 * audio profile all items can edit.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IAudioProfileExt")
public class OP09AudioProfileExt extends DefaultAudioProfileExt {

    private static final String TAG = "OP09AudioProfileExt";

    private static final String CRBT_SETTINGS_KEY = "crtb_settings_key";

    // Broadcast for sending sms
    private static final String ACTION_SENDTO_SEND = "com.mediatek.ct.csm.action.ACTION_SEND";
    private static final String ACTION_SENDTO_DELIVERED =
        "com.mediatek.ct.csm.action.ACTION_DELIVERED";

    private static final String CT_COLOR_RING_BACK_TONE_ADDRESS = "118100";
    private static final String CT_COLOR_RING_BACK_TONE_CONTENT = "sla";

    private Fragment mFragment;
    private Context mContext;
    private TextView mTextView = null;
    private RadioButton mCheckboxButton = null;
    private ImageView mImageView = null;

    private ProgressDialog mProgress;
    private IntentFilter mIntentFilter;
    private Preference mCrbtPreferernce;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent != null) {
                final String action = intent.getAction();
                final int resultCode = getResultCode();
                Log.d(TAG, "broadcast action=" + action + ", resultCode=" + resultCode);
                if (action.equals(ACTION_SENDTO_SEND)) {
                    hideProgress();
                    if (resultCode == Activity.RESULT_OK) {
                        showInfoAsToast(R.string.crbt_send_ok);
                    } else {
                        showInfoAsToast(R.string.crbt_send_fail);
                        Log.w(TAG, "Send sms failed! resultCode=" + resultCode);
                    }
                } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                        || action.equals(action.equals(Intent.ACTION_MSIM_MODE_CHANGED))) {
                    updateCrbtPreference();
                }
            }
        }
    };

    /**
     * Constructor method.
     * @param context is Settings's context.
     */
    public OP09AudioProfileExt(Context context) {
        super(context);
        mContext = context;
    }


    @Override
    public void addCustomizedPreference(PreferenceScreen preferenceScreen) {
        // Add coloring ring back tone setting
        PreferenceCategory crbtSettingsCateGory =
            new PreferenceCategory(preferenceScreen.getContext(), null);
        crbtSettingsCateGory.setTitle(
                mContext.getResources().getText(R.string.crbt_settings_title));
        crbtSettingsCateGory.setPersistent(false);
        preferenceScreen.addPreference(crbtSettingsCateGory);
        Preference crbtSettings = new Preference(preferenceScreen.getContext(), null);
        crbtSettings.setKey(CRBT_SETTINGS_KEY);
        crbtSettings.setTitle(mContext.getResources().getText(R.string.crbt_settings_title));
        crbtSettingsCateGory.addPreference(crbtSettings);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(CRBT_SETTINGS_KEY)) {
            sendSms(preference.getContext(),
                    PhoneConstants.SIM_ID_1,
                    CT_COLOR_RING_BACK_TONE_ADDRESS,
                    CT_COLOR_RING_BACK_TONE_CONTENT);
        }
        return false;
    }

    @Override
    public void onAudioProfileSettingResumed(PreferenceFragment fragment) {
        if (mIntentFilter == null) {
            initIntentFilter();
        }
        fragment.getActivity().registerReceiver(mReceiver, mIntentFilter);
        Preference crbtSettings = fragment.getPreferenceScreen().findPreference(CRBT_SETTINGS_KEY);
        if (mCrbtPreferernce == null || !mCrbtPreferernce.equals(crbtSettings)) {
            mCrbtPreferernce = crbtSettings;
        }
        updateCrbtPreference();
    }

    @Override
    public void onAudioProfileSettingPaused(PreferenceFragment fragment) {
        fragment.getActivity().unregisterReceiver(mReceiver);
    }

    private void updateCrbtPreference() {
        if (mCrbtPreferernce != null) {
            mCrbtPreferernce.setEnabled(isCdmaSimEnabled());
        }
    }

    private void initIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_SENDTO_SEND);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_MSIM_MODE_CHANGED);
    }

    private void sendSms(Context context, int simId, String address, String message) {
        if (mContext == null) {
            Log.e(TAG, "sendSms: No context!");
            return;
        }
        Log.d(TAG, "sendSms by sim " + simId);

        PendingIntent sentIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(
                ACTION_SENDTO_SEND), 0);
        PendingIntent deliveryIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(
                ACTION_SENDTO_DELIVERED), 0);

        sendTextMessage(address, message, simId, sentIntent, deliveryIntent);

        // Show the progress dialog just only a message has been sent
        String dialogMessage = mContext.getResources().getString(R.string.crbt_wait);
        showProgress(context, makeSmsDialogTitle(address, message), dialogMessage);
    }

    private String makeSmsDialogTitle(String address, String message) {
        final String sending = getResources().getString(R.string.crbt_sending);
        final String to = getResources().getString(R.string.crbt_to);
        StringBuilder builder = new StringBuilder();
        builder.append(sending);
        builder.append(message);
        builder.append(to);
        builder.append(address);
        return builder.toString();
    }

    private void showProgress(Context context, String title, String message) {
        hideProgress();
        if (mProgress == null) {
            mProgress = new ProgressDialog(context);
        }
        Log.d(TAG, "showProgress: create progress dialog");
        mProgress.setTitle(title);
        mProgress.setMessage(message);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
        mProgress.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mProgress.show();
    }

    private void hideProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            Log.d(TAG, "hideProgress: dismiss progress dialog");
            mProgress.dismiss();
        }
    }

    private void showInfoAsToast(int resId) {
        if (mContext != null) {
            Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCdmaSimEnabled() {

        boolean isSimInsert = Utils.isSIMInserted(PhoneConstants.SIM_ID_1);
        boolean isSimStateReady = Utils.isSimStateReady(mContext, PhoneConstants.SIM_ID_1);
        boolean isRadioOn = Utils.isTargetSimRadioOn(PhoneConstants.SIM_ID_1);
        boolean isAirPlaneMode = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1);
        Log.i(TAG, "isSimInsert = " + isSimInsert + " isSimStateReady = " + isSimStateReady +
                " isRadioOn = " + isRadioOn + " isAirPlaneMode = " + isAirPlaneMode);
        return isSimInsert && isSimStateReady && isRadioOn && !isAirPlaneMode;
    }

    /**
     * send message.
     * @param addr receive phone number like 13988888888
     * @param msg  message
     * @param simId though simx send
     * @param sentIntent send error or OK message
     * @param deliveryIntent receive error or OK message
     */
    public static void sendTextMessage(String addr, String msg, int simId,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (Utils.isGeminiSupport()) {
            SmsManagerEx.getDefault().sendTextMessage(addr, null, msg,
                    sentIntent, deliveryIntent, simId);
        } else {
            SmsManager.getDefault().sendTextMessage(addr, null, msg,
                    sentIntent, deliveryIntent);
        }
    }

}

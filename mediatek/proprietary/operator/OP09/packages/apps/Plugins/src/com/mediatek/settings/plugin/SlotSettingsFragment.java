/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2013. All rights reserved.
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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;


/**
 * Slot settings preference.
 */
public class SlotSettingsFragment extends PreferenceFragment {
    private static final String TAG = "SlotSettingsFragment";

    public static final String NUMERIC_CHINA_TELE = "46003";
    public static final String NUMERIC_CHINA_TELE_LTE = "46011";
    public static final String NUMERIC_CHINA_MACAO_TELE = "45502";
    public static final String NUMERIC_NO_NETWORK = "00000";
    public static final String NUMERIC_UNKNOWN = "-1";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    private static final int MODE_PHONE1_ONLY = 1;
    private static final int MODE_PHONE2_ONLY = 2;

    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;

    private static final int DLG_NETWORK_AUTO_SELECT = 1;

    private static final String PREFERENCES_NAME = "SlotSettingsFragment";
    private static final String GSM_AUTO_NETWORK_SELECTION = "gsm_auto_network_selection";

    // China numeric
    private static final String NATIVE_NUMERIC2 = "45502";

    // constant for current sim mode
    private static final int ALL_RADIO_OFF = 0;
    private static final int SIM_SLOT_1_RADIO_ON = 1;
    private static final int SIM_SLOT_2_RADIO_ON = 2;
    private static final int ALL_RADIO_ON = 3;

    // when finish gsm network auto select, receive msg with this id
    private static final int EVENT_AUTO_SELECT_DONE = 2;

    private static final String KEY_SIM_RADIO_STATE = "sim_radio_state";
    private static final String KEY_CURRENT_NETWORK_INFO = "current_network_info";
    private static final String KEY_MANUAL_NETWORK_CDMA_SELECTION = "manual_network_selection";
    private static final String KEY_MANUAL_NETWORK_GSM_SELECTION = "manual_network_selection_gsm";
    private static final String KEY_ROAMING_HOTLINE = "roaming_hotline";
    private static final String KEY_NOTES = "notes";

    private int mTargetSlot = 0;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private TelephonyManager mTelephonyManager;
    private ITelephony mTelephony;
    private TelephonyManagerEx mTelephonyManagerEx;

    private Dialog mDialog;

    private IntentFilter mIntentFilter;
    private boolean mIsSim1Inserted;
    private boolean mIsSim2Inserted;
    private boolean mIsCTSupportIRCard;
    private CheckBoxPreference mEnableSimRadioPref;
    private Preference mNetworkInfoPref;
    private Preference mManualNetworkPref;
    private CheckBoxPreference mManualNetworkGsmPref;
    private Preference mRoamingHotlinePref;
    private Preference mNotesPref;
    private boolean mIsSIMRadioSwitching = false;
    private Phone mPhone;
    private boolean mIsForeground;
    private PhoneStateListener mPhoneStateListener;
    private ServiceState mServiceState;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("@M_" + TAG, "handleMessage--mIsSIMRadioSwitching = " + mIsSIMRadioSwitching);
            switch (msg.what) {
                case EVENT_AUTO_SELECT_DONE:
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.d("@M_" + TAG, "EVENT_AUTO_SELECT_DONE network selection: failed!");
                        if (mIsForeground) {
                            displayNetworkSelectionFailed(ar.exception);
                        }
                    } else {
                        Log.d("@M_" + TAG, "EVENT_AUTO_SELECT_DONE network selection: succeed!");
                        if (mIsForeground) {
                            displayNetworkSelectionSucceeded();
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    };


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("@M_" + TAG, "onReceive action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                    || action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)
                    || action.equals(Intent.ACTION_MSIM_MODE_CHANGED)) {
                updateScreen();
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                // Update SIM inserted status and update screen when SIM changes.
                updateSimInsertedState();
                updateScreen();
            } else if (action.equals(TelephonyIntents.ACTION_CDMA_CARD_TYPE)) {
                IccCardConstants.CardType cardType =
                        (IccCardConstants.CardType) intent.getSerializableExtra(
                        TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
                if ((cardType != null)
                    && ((cardType.compareTo(IccCardConstants.CardType.CT_UIM_SIM_CARD) == 0)
                    || (cardType.compareTo(IccCardConstants.CardType.CT_4G_UICC_CARD) == 0))) {
                    mIsCTSupportIRCard = true;
                } else {
                    mIsCTSupportIRCard = false;
                }
                updateScreen();
                Log.d("@M_" + TAG, "ACTION_CDMA_CARD_TYPE: cardType = " + cardType
                    + " and mIsCTSupportIRCard = " + mIsCTSupportIRCard);
            }
        }
    };

    /**
     * Set the slot id of the fragment, this should be done before
     * Fragment.onCreate().
     *
     * @param slot Input the id of slot.
     */
    public void setSlotId(int slot) {
        mTargetSlot = slot;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("@M_" + TAG, "onCreate + mTargetSlot = " + mTargetSlot);
        if (mTargetSlot != PhoneConstants.SIM_ID_1
                && mTargetSlot != PhoneConstants.SIM_ID_2) {
            throw new IllegalStateException(
                    "Require sim slot is either slot1 or slo2");
        }

        addPreferencesFromResource(R.xml.slot_network_settings);
        mTelephonyManager = (TelephonyManager) getActivity().getSystemService(
                Context.TELEPHONY_SERVICE);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager
                .getService("phone"));
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        mEnableSimRadioPref = (CheckBoxPreference) findPreference(KEY_SIM_RADIO_STATE);
        mNetworkInfoPref = findPreference(KEY_CURRENT_NETWORK_INFO);
        mManualNetworkPref = findPreference(KEY_MANUAL_NETWORK_CDMA_SELECTION);
        mManualNetworkGsmPref = (CheckBoxPreference) findPreference(
                KEY_MANUAL_NETWORK_GSM_SELECTION);
        mRoamingHotlinePref = findPreference(KEY_ROAMING_HOTLINE);
        mNotesPref = findPreference(KEY_NOTES);

        if (mTargetSlot == PhoneConstants.SIM_ID_2) {
            getPreferenceScreen().removePreference(mRoamingHotlinePref);
            getPreferenceScreen().removePreference(mNotesPref);
            getPreferenceScreen().removePreference(mManualNetworkPref);
        } else if (mTargetSlot == PhoneConstants.SIM_ID_1) {
            getPreferenceScreen().removePreference(mManualNetworkGsmPref);
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_MSIM_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_CDMA_CARD_TYPE);
        updateSimInsertedState();
    }

    private void updateSimInsertedState() {
        if (mTelephonyManagerEx != null) {
            mIsSim1Inserted = mTelephonyManagerEx.hasIccCard(PhoneConstants.SIM_ID_1);
            mIsSim2Inserted = mTelephonyManagerEx.hasIccCard(PhoneConstants.SIM_ID_2);
            Log.d("@M_" + TAG, "updateSimInsertedState mIsSim1Inserted=" + mIsSim1Inserted
                    + ", mIsSim2Inserted=" + mIsSim2Inserted);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsForeground = true;
        int[] subId  = SubscriptionManager.getSubId(mTargetSlot);
        if (subId != null) {
            mSubId = subId[0];
            int phoneID = SubscriptionManager.getPhoneId(mSubId);
            mPhone = PhoneFactory.getPhone(phoneID);
            if (mPhone != null) {
                mServiceState = mPhone.getServiceState();
            }
            Log.d("@M_" + TAG, "onResume: mTargetSlot = " + mTargetSlot + ", mSubId = " +
                       mSubId + ", phoneID = " + phoneID);
        } else {
            Log.e("@M_" + TAG, "onResume: getSubId = null");
        }
        phoneStateChangeListener();
        updateSimInsertedState();
        initPreferenceState();
        updateScreen();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("@M_" + TAG, "onPause");
        mIsForeground = false;
        getActivity().unregisterReceiver(mReceiver);
        mTelephonyManager.listen(
            mPhoneStateListener,
            PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("@M_" + TAG, "onDestroy");
        mIsSIMRadioSwitching = false;
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void initPreferenceState() {
        mEnableSimRadioPref.setChecked(getRadioState());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnableSimRadioPref) {
            if (!mIsSIMRadioSwitching) {
                mIsSIMRadioSwitching = true;
                switchSimRadioState(mTargetSlot,
                        mEnableSimRadioPref.isChecked());
            } else {
                Log.d("@M_" + TAG, "Click too fast it is switching and set the switch to previous state");
                mEnableSimRadioPref.setChecked(!mEnableSimRadioPref.isChecked());
            }
        } else if (preference == mRoamingHotlinePref) {
            Log.d("@M_" + TAG, "start FreeService");
            Intent intent = new Intent(getActivity(), FreeService.class);
            if (mIsSim1Inserted && mIsSim2Inserted) {
                intent.putExtra(FreeService.SIM_INFO, FreeService.TWO_SIM);
            } else if (mIsSim1Inserted) {
                intent.putExtra(FreeService.SIM_INFO, FreeService.ONE_CDMA);
            } else if (mIsSim2Inserted) {
                intent.putExtra(FreeService.SIM_INFO, FreeService.ONE_GSM);
            } else {
                intent.putExtra(FreeService.SIM_INFO, FreeService.NO_SIM_ERROR);
            }
            getActivity().startService(intent);
        } else if (preference == mNetworkInfoPref) {
            Intent intent = new Intent();
            intent.putExtra(PhoneConstants.SLOT_KEY, mTargetSlot);
            intent.setClassName("com.mediatek.op09.plugin",
                    "com.mediatek.settings.plugin.CurrentNetworkInfoStatus");
            startActivity(intent);
        } else if (preference == mManualNetworkPref) {
            Intent manualNetworkSettingIntent = new Intent(
                    "com.mediatek.OP09.MANUAL_NETWORK_SELECTION");
            manualNetworkSettingIntent.putExtra(PhoneConstants.SLOT_KEY, mTargetSlot);
            startActivity(manualNetworkSettingIntent);
        } else if (preference == mManualNetworkGsmPref) {
            if (mManualNetworkGsmPref.isChecked()) {
                Intent manualNetworkSettingIntent = new Intent(
                        "com.mediatek.OP09.MANUAL_NETWORK_SELECTION");
                manualNetworkSettingIntent.putExtra(PhoneConstants.SLOT_KEY, mTargetSlot);
                startActivity(manualNetworkSettingIntent);
            } else {
                selectNetworkAutomatic();
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void selectNetworkAutomatic() {
        Log.d("@M_" + TAG, "select network automatically...");
        SlotSettingsFragment.setGsmAutoNetowrkSelection(getActivity(), true);
        if (mIsForeground) {
            showDialog(DLG_NETWORK_AUTO_SELECT);
        }
        Message msg = mHandler.obtainMessage(EVENT_AUTO_SELECT_DONE);
        if (mPhone != null) {
            mPhone.setNetworkSelectionModeAutomatic(msg);
        }

    }

    /**
     * listen whether radio switch completely.
     *
     * @param subInfoRecord
     */
    private void phoneStateChangeListener() {
        Log.i("@M_" + TAG, "listenPhoneStateListener, mTargetSlot: "
                  + mTargetSlot + ", mSubId: " + mSubId);
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneStateListener(mSubId) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.i("@M_" + TAG, "listenPhoneStateListener, status: " + serviceState);
                    if (null == SlotSettingsFragment.this.getActivity()) {
                        Log.i("@M_" + TAG, "fragment is removed from activity, return");
                        return;
                    }
                    mServiceState = serviceState;
                    if (mIsSIMRadioSwitching) {
                        int currentSimMode = Settings.System.getInt(
                            getActivity().getContentResolver(),
                            Settings.System.MSIM_MODE_SETTING, -1);
                        boolean isOff = ((currentSimMode & (MODE_PHONE1_ONLY << mTargetSlot)) == 0)
                            ? RADIO_POWER_OFF : RADIO_POWER_ON;
                        Log.i("@M_" + TAG, "soltId: " + mTargetSlot + ", radio is off : " + isOff);
                        if (isOff) {
                            if (serviceState.getState() != ServiceState.STATE_POWER_OFF) {
                                mIsSIMRadioSwitching = false;
                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                            }
                        } else {
                            if (serviceState.getState() == ServiceState.STATE_POWER_OFF) {
                                mIsSIMRadioSwitching = false;
                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                                handleDataSubSwitch();
                            }
                        }
                    }
                    updateScreen();
                }
            };
        }
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private void switchSimRadioState(int slot, boolean isChecked) {
        if (!SubscriptionManager.isValidSlotId(slot)) {
            Log.i("@M_" + TAG, "switchSimRadioState, slot id is invalid");
            return;
        }
        int dualSimMode = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.MSIM_MODE_SETTING,
                -1);
        Log.i("@M_" + TAG, "The current dual sim mode is " + dualSimMode);


        int dualState = 0;
        boolean isRadioOn = false;
        int modeSlot = MODE_PHONE1_ONLY << slot;
        if ((dualSimMode & modeSlot) > 0) {
            dualState = dualSimMode & (~modeSlot);
            isRadioOn = false;
        } else {
            dualState = dualSimMode | modeSlot;
            isRadioOn = true;
        }
        int msgId = 0;
        if (isRadioOn) {
            msgId = R.string.gemini_sim_mode_progress_activating_message;
        } else {
            msgId = R.string.gemini_sim_mode_progress_deactivating_message;
        }
        showProgressDialg(msgId);
        Log.d("@M_" + TAG, "dualState=" + dualState + " isRadioOn=" + isRadioOn);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, dualState);
        Intent intent = new Intent(Intent.ACTION_MSIM_MODE_CHANGED);
        intent.putExtra(Intent.EXTRA_MSIM_MODE, dualState);
        getActivity().sendBroadcast(intent);
    }

    private void showProgressDialg(int msgId) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(msgId));
        dialog.setIndeterminate(true);
        mDialog = dialog;
        mDialog.setCancelable(false);
        mDialog.show();
    }

    private void showDialog(int dialogId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        switch (dialogId) {
            case DLG_NETWORK_AUTO_SELECT:
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                mDialog = new ProgressDialog(getActivity());
                ((ProgressDialog) mDialog).setMessage(getActivity().getResources().getString(
                        R.string.register_automatically));
                ((ProgressDialog) mDialog).setCancelable(false);
                ((ProgressDialog) mDialog).setIndeterminate(true);
                mDialog.show();
                break;

            default:
                break;
        }
    }

    private boolean getRadioState() {
        Log.d("@M_" + TAG, "getRadioState()");
        boolean isRadioStateOn = true;
        if (mTelephony == null) {
            mTelephony = ITelephony.Stub.asInterface(ServiceManager
                    .getService("phone"));
        }
        try {
            if (mTelephony != null) {
                isRadioStateOn = mTelephony.isRadioOnForSubscriber(mSubId, PACKAGE_NAME);
            }
        } catch (RemoteException e) {
            Log.d("@M_" + TAG, "exception happend unable to get Itelephony state");
        }
        Log.d("@M_" + TAG, "isRadioStateOn = " + isRadioStateOn);
        return isRadioStateOn;
    }

    private boolean targetSlotRadioOn() {
        boolean isRadioInOn = false;
        try {
            if (mTelephony != null) {
                isRadioInOn = mTelephony.isRadioOnForSubscriber(mSubId, PACKAGE_NAME);
                Log.d("@M_" + TAG, "Slot " + mTargetSlot + " is in radion state " + isRadioInOn);
            }
        } catch (RemoteException e) {
            Log.e("@M_" + TAG, "mTelephony exception");
        }
         return isRadioInOn;
    }

    private void updateScreen() {
        final boolean isAirplaneOn = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) > 0;

        final boolean targetSlotRadioOn = targetSlotRadioOn();
        Log.d("@M_" + TAG, "updateScreen: isAirplaneOn = " + isAirplaneOn + ", mTargetSlot = "
                + mTargetSlot + ", mIsSim1Inserted = " + mIsSim1Inserted + ", mIsSim2Inserted = "
                + mIsSim2Inserted + ", targetSlotRadioOn = " + targetSlotRadioOn);
        getPreferenceScreen().setEnabled(!isAirplaneOn);

        if (!isAirplaneOn) {
            if (mTargetSlot == PhoneConstants.SIM_ID_1) {
                mEnableSimRadioPref.setEnabled(mIsSim1Inserted);
                mEnableSimRadioPref.setChecked(mIsSim1Inserted && targetSlotRadioOn);
                mNetworkInfoPref.setEnabled(mIsSim1Inserted && targetSlotRadioOn);
                mRoamingHotlinePref.setEnabled(mIsSim1Inserted && targetSlotRadioOn);
                mNotesPref.setEnabled(mIsSim1Inserted && targetSlotRadioOn);
            } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                mEnableSimRadioPref.setEnabled(mIsSim2Inserted);
                mEnableSimRadioPref.setChecked(mIsSim2Inserted && targetSlotRadioOn);
                mNetworkInfoPref.setEnabled(mIsSim2Inserted && targetSlotRadioOn);
            }

            if (targetSlotRadioOn) {
                // The target slot is slot1.
                if (getPreferenceScreen().findPreference(KEY_MANUAL_NETWORK_CDMA_SELECTION) !=
                        null) {
                    boolean externalSlotInRoaming = (mIsCTSupportIRCard
                        && mPhone != null && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM
                        && externalSlotInRoamingService(mServiceState));
                    mManualNetworkPref.setEnabled(externalSlotInRoaming);
                    Log.d("@M_" + TAG, "updateScreen: externalSlotInRoaming = " + externalSlotInRoaming);
                }
                // The target slot is slot2.
                if (getPreferenceScreen().findPreference(KEY_MANUAL_NETWORK_GSM_SELECTION) !=
                        null) {
                    mManualNetworkGsmPref.setEnabled(mIsSim2Inserted);
                    mManualNetworkGsmPref.setChecked(!isGsmAutoNetowrkSelection(getActivity()));
                }
            } else {
                if (getPreferenceScreen().findPreference(KEY_MANUAL_NETWORK_CDMA_SELECTION) !=
                        null) {
                    mManualNetworkPref.setEnabled(false);
                }
                if (getPreferenceScreen().findPreference(KEY_MANUAL_NETWORK_GSM_SELECTION) !=
                        null) {
                    mManualNetworkGsmPref.setEnabled(false);
                    mManualNetworkGsmPref.setChecked(!isGsmAutoNetowrkSelection(getActivity()));
                }
            }
        } else {
            mEnableSimRadioPref.setChecked(false);
            if (getPreferenceScreen().findPreference(KEY_MANUAL_NETWORK_GSM_SELECTION) != null) {
                mManualNetworkGsmPref.setEnabled(false);
                mManualNetworkGsmPref.setChecked(!isGsmAutoNetowrkSelection(getActivity()));
            }
        }
    }

   /**
     * @param serviceState judge current serviceState is inService.
     * @return true if the service state of external slot is roaming.
     */
    public static boolean externalSlotInRoamingService(ServiceState serviceState) {
        boolean isRoamingMode = false;
        SvlteRatController lteRatController = SvlteRatController.getInstance();

        if (lteRatController != null) {
            isRoamingMode =
                (lteRatController.getRoamingMode() == RoamingMode.ROAMING_MODE_NORMAL_ROAMING);
        }
        Log.d("@M_" + TAG, "externalSlotInRoamingService, isRoamingMode : " + isRoamingMode);
        return isRoamingMode;
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status = null;
        if ((ex != null && ex instanceof CommandException) &&
                ((CommandException) ex).getCommandError() ==
                    CommandException.Error.ILLEGAL_SIM_OR_ME) {
            status = OP09SettingsMiscExtImp.replaceSimBySlotInner(getString(R.string.not_allowed));
        } else {
            status = getResources().getString(R.string.connect_later);
        }
        Toast.makeText(getActivity(), status, Toast.LENGTH_LONG).show();
    }

    private void displayNetworkSelectionSucceeded() {
        String status = getResources().getString(R.string.registration_done);
        Toast.makeText(getActivity(), status, Toast.LENGTH_LONG).show();
    }

    /**
     * If the state of GSM auto network selection is automatical.
     * @param context Input context.
     * @return the state.
     */
    public static boolean isGsmAutoNetowrkSelection(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(GSM_AUTO_NETWORK_SELECTION, true);
    }

    /**
     * Set the state of GSM auto network selection.
     * @param context Input context.
     * @param autoSelect Input if select automatically.
     */
    public static void setGsmAutoNetowrkSelection(Context context, boolean autoSelect) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putBoolean(GSM_AUTO_NETWORK_SELECTION, autoSelect);
        editor.commit();
    }

    private void handleDataSubSwitch() {
        SubscriptionManager subManager = SubscriptionManager.from(getActivity());
        List<SubscriptionInfo> si = subManager.getActiveSubscriptionInfoList();
        if (mTargetSlot >= 0 && si.size() > 1) {
            if (isMobileDataOn()) {
                int preferredDataSub = getPreferedDataSub();
                if (preferredDataSub > -1 &&
                        SubscriptionManager.getDefaultDataSubId() != preferredDataSub) {
                    mTelephonyManager.setDataEnabled(SubscriptionManager.getDefaultDataSubId(),
                        false);
                    subManager.setDefaultDataSubId(preferredDataSub);
                    mTelephonyManager.setDataEnabled(preferredDataSub, true);

                    Log.d(TAG, "IR Setting radio change, switch data sub to " + preferredDataSub
                            + ", slotId=" + SubscriptionManager.getSlotId(preferredDataSub));
                }
            }
        }
    }

    private int getPreferedDataSub() {
        int preferredDataSub = -1;
        List<SubscriptionInfo> subInfoRecordList = SubscriptionManager.from(getActivity())
            .getActiveSubscriptionInfoList();
        for (SubscriptionInfo subInfoRecord : subInfoRecordList) {
            if (subInfoRecord.getSimSlotIndex() != mTargetSlot
                && isTargetSubRadioOn(subInfoRecord.getSubscriptionId())) {
                preferredDataSub = subInfoRecord.getSubscriptionId();
                break;
            }
        }
        return preferredDataSub;
    }

    private boolean isTargetSubRadioOn(int subId) {
        boolean isRadioOn = false;
        try {
            isRadioOn = mTelephony.isRadioOnForSubscriber(subId, PACKAGE_NAME);
        } catch (RemoteException e) {
            isRadioOn = false;
            Log.e(TAG, "ITelephony exception");
        }
        Log.d(TAG, "isTargetSubRadioOn subId=" + subId + ", isRadioOn=" + isRadioOn);
        return isRadioOn;
    }

    private boolean isMobileDataOn() {
        boolean dataEnabled = mTelephonyManager.getDataEnabled();
        Log.d(TAG, "isMobileDataOn dataEnabled=" + dataEnabled);
        return dataEnabled;
    }
}

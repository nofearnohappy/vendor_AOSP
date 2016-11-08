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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;

import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;


/**
 * CT customization, to show current SIM card's network related information.
 */
public class CurrentNetworkInfoStatus extends PreferenceActivity {

    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_CURRENT_OPERATORS_MCCMNC = "current_operators_mccmnc";
    // For slot 1 only
    private static final String KEY_CURRENT_SIDNID = "current_sidnid";
    // For slot 1 only
    private static final String KEY_CURRENT_CELLID = "current_cellid";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    // For slot 1 and slot 2 GMS mode only
    private static final String KEY_SMS_CENTER = "sms_center";
    // For slot 1 only
    private static final String KEY_CT_PRL_VERSION = "ct_prl_version";
    /// For getting the saved Cdma Prl version
    private static final String PRL_VERSION_KEY_NAME = "cdma.prl.version";

    private static final String TAG = "CurrentNetworkInfoStatus";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int PLMN_NORMAL_LEN = 5;
    private static final int PLMN_SPECIAL_LEN = 6;
    private static final int MCC_LEN = 3;
    private static final int MNC_MAX_LEN = 3;

    private CDMAPhone mCdmaPhone;
    private GSMPhone mGsmPhone;
    private Phone mSvlteDcPhone;
    private Phone mPhone;
    private Preference mSignalStrengthPreference;

    private TelephonyManager mTelephonyManager;
    private TelephonyManagerEx mTelephonyManagerEx;
    // SimId, get from the intent extra
    private int mSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mIsAborting = false;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private ScAsyncTask mScAyncTask;

    private String mUnknown;

    // related to mobile network type and mobile network state
    private PhoneStateListener mPhoneStateListener;

    private void createPhoneStateListener() {
        mPhoneStateListener = new PhoneStateListener(mSubId) {
            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                updateNetworkType();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Log.d("@M_" + TAG, "onSignalStrengthsChanged, mSlotId : " + mSlotId +
                    " SignalStrength : " + signalStrength);
                mSignalStrength = signalStrength;
                updateSignalStrength();
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                Log.d("@M_" + TAG, "onServiceStateChanged, mSlotId : " + mSlotId + ", VoiceRegState : "
                    + serviceState.getVoiceRegState() + ", DataRegState : "
                    + serviceState.getDataRegState() +  ", DataNetworkType : "
                    + serviceState.getDataNetworkType());
                mServiceState = serviceState;
                updateServiceState();
                updateNetworkType();
                updateSignalStrength();
                setMccMnc();
            }
        };
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
       @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean airplaneMode = intent.getBooleanExtra("state", false);
                if (airplaneMode) {
                   CurrentNetworkInfoStatus.this.finish();
                }
            } else if (TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)
                       || Intent.ACTION_MSIM_MODE_CHANGED.equals(action)
                       || TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                if (!checkTargetSlotEnable()) {
                    CurrentNetworkInfoStatus.this.finish();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.current_networkinfo_status);

        // get the correct simId according to the intent extra
        Intent it = getIntent();
        mSlotId = it.getIntExtra(PhoneConstants.SLOT_KEY, -1);
        int[] subId  = SubscriptionManager.getSubId(mSlotId);
        if (subId != null) {
            mSubId = subId[0];
        } else {
            Log.e("@M_" + TAG, "invoke finish in onCreate,because subId array i;s null");
            mIsAborting = true;
            finish();
        }
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            Log.e("@M_" + TAG, "invoke finish in onCreate,because mSubId is invalid");
            mIsAborting = true;
            finish();
        }
        Log.d("@M_" + TAG, "onCreate, mSlotId is : " + mSlotId + ", mSubId is : " + mSubId);

        mUnknown = getResources().getString(R.string.device_info_default);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mTelephonyManagerEx = new TelephonyManagerEx(this);
        mSignalStrengthPreference = findPreference(KEY_SIGNAL_STRENGTH);
        initPreferences();
    }

    private void initPreferences() {
        int phoneID = SubscriptionManager.getPhoneId(mSubId);
        int phoneType = PhoneConstants.PHONE_TYPE_NONE;
        mPhone = PhoneFactory.getPhone(phoneID);
        if (mPhone != null) {
            phoneType = mPhone.getPhoneType();
        } else {
            Log.d("@M_" + TAG, "mPhone == null, finish() ");
            mIsAborting = true;
            finish();
        }
        Log.d("@M_" + TAG, "initPreferences, slotId = " + mSlotId + " phoneType = " + phoneType);
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            mCdmaPhone = (CDMAPhone) (((PhoneProxy) mPhone).getActivePhone());
            if (mCdmaPhone != null && mPhone instanceof SvltePhoneProxy) {
                mSvlteDcPhone = ((SvltePhoneProxy) mPhone).getLtePhone();
            }
            removePreference(KEY_SMS_CENTER);
        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            mGsmPhone = (GSMPhone) (((PhoneProxy) mPhone).getActivePhone());
            if (mSlotId == getExternalModemSlot()) {
               removePreference(KEY_CURRENT_SIDNID);
            } else {
                removePreference(KEY_CURRENT_SIDNID);
                removePreference(KEY_CURRENT_CELLID);
                removePreference(KEY_CT_PRL_VERSION);
            }
        }
    }

    private void removePreference(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private void updatePreferences() {
        setMccMnc();
        getSidNidPrlValue();
        getCellIdValue();
        updateSmsServiceCenter();
    }

    private void setMccMnc() {
        String numeric = null;
        if (isLteDataNwReg()) {
            numeric = getMccMncProperty(mSvlteDcPhone);
        } else {
            numeric = getMccMncProperty(mPhone);
        }

        Log.d("@M_" + TAG, "setMccMnc, numeric=" + numeric);
        if ((numeric.length() == PLMN_NORMAL_LEN) || (numeric.length() == PLMN_SPECIAL_LEN)) {
                String mcc = numeric.substring(0, MCC_LEN);
                String mnc = numeric.substring(MNC_MAX_LEN);
                setSummaryText(KEY_CURRENT_OPERATORS_MCCMNC, mcc + "," + mnc);
        }
    }

    private String getMccMncProperty(Phone phone) {
        String prop = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC);
        String propVal = null;
        int phoneId = 0;
        if (phone != null) {
            phoneId = phone.getPhoneId();
        }
        if ((prop != null) && (prop.length() > 0)) {
            String values[] = prop.split(",");
            if ((phoneId >= 0) && (phoneId < values.length) && (values[phoneId] != null)) {
                propVal = values[phoneId];
            }
        }
        return propVal == null ? "" : propVal;
    }

    private void getSidNidPrlValue() {
        if (mCdmaPhone != null) {
            setSummaryText(KEY_CURRENT_SIDNID, mCdmaPhone.getSid() + "," + mCdmaPhone.getNid());
        }
        // Get the saved Cdma Prl version and set text.
        final ContentResolver cr = mPhone.getContext().getContentResolver();
        String prlVersion = android.provider.Settings.System.getString(cr, PRL_VERSION_KEY_NAME);
        setSummaryText(KEY_CT_PRL_VERSION, prlVersion);
        Log.d("@M_" + TAG, "getSidNidPrlValue: key = " + PRL_VERSION_KEY_NAME +
                ", prlVersion = " + prlVersion);
    }

    // Only for Slot 1
    private void getCellIdValue() {
        if (mSlotId == getExternalModemSlot()) {
            if (mCdmaPhone != null) {
                CdmaCellLocation cellLocation = (CdmaCellLocation) mCdmaPhone.getCellLocation();
                setSummaryText(KEY_CURRENT_CELLID,
                        Integer.toString(cellLocation.getBaseStationId()));
            } else if (mGsmPhone != null) {
                GsmCellLocation cellLocation = (GsmCellLocation) mGsmPhone.getCellLocation();
                setSummaryText(KEY_CURRENT_CELLID, Integer.toString(cellLocation.getCid()));
            }
        }
    }

    // Only for GSM mode
    private void updateSmsServiceCenter() {
        if (mGsmPhone != null) {
            if (mSlotId == getExternalModemSlot() && !mServiceState.getRoaming()) {
                removePreference(KEY_SMS_CENTER);
                return;
            }
            mScAyncTask = new ScAsyncTask();
            mScAyncTask.execute();
        } else {
            removePreference(KEY_SMS_CENTER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkTargetSlotEnable()) {
            mIsAborting = true;
            finish();
            return;
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intentFilter.addAction(Intent.ACTION_MSIM_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mReceiver, intentFilter);
        // related to my phone number, may be null or empty
        /// M: LEGO refactory start @{
        String rawNumber = mTelephonyManagerEx.getLine1Number(mSlotId);
        /// @}

        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }

        // after registerIntent, it will receive the message, so do not need to update
        // signalStrength and service state.

        if (mSvlteDcPhone != null) {
            mServiceState = mSvlteDcPhone.getServiceState();
        } else {
            mServiceState = mPhone.getServiceState();
        }
        mSignalStrength = new SignalStrength(mPhone.getSignalStrength());
        if (isLteDataNwReg()) {
            mSignalStrength.setLteRsrp(mSvlteDcPhone.getSignalStrength().getLteDbm());
        }
        updateServiceState();
        updateSignalStrength();
        updateNetworkType();
        createPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                | PhoneStateListener.LISTEN_SERVICE_STATE);

        updatePreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mIsAborting == true) {
            mIsAborting = false;
            return;
        }
        if (mScAyncTask != null) {
            mScAyncTask.cancel(true);
        }
        unregisterReceiver(mReceiver);
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private void setSummaryText(String preference, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.getResources().getString(R.string.device_info_default);
        }
        // some preferences may be missing
        Preference p = findPreference(preference);
        if (p != null) {
            p.setSummary(text);
        }
    }

    private boolean isLteDataNwReg() {
        boolean isLteDataNW = false;
         if (mSlotId == getExternalModemSlot() && mCdmaPhone != null && mServiceState != null
                && mServiceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE
                && mServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE) {
            isLteDataNW = true ;
         }
         Log.d("@M_" + TAG, "isLteDataNwReg: " + isLteDataNW + ", CdmaPhone active : "
                + (mCdmaPhone != null) + ", ServiceState : " + mServiceState);
         return isLteDataNW;
    }

    private void updateNetworkType() {
        String netWorkTypeName = "UNKNOWN";
        // Whether EDGE, UMTS, etc...
        netWorkTypeName = mTelephonyManager.getNetworkTypeName(
                    mServiceState.getVoiceNetworkType());

        netWorkTypeName = renameNetworkTypeNameForCTSpec(netWorkTypeName);
        Preference p = findPreference(KEY_NETWORK_TYPE);
        if (p != null) {
            if (isLteDataNwReg()) {
                String lteNetWorkTypeName = mTelephonyManager.getNetworkTypeName(
                    mServiceState.getDataNetworkType());
                p.setSummary((netWorkTypeName.equals("UNKNOWN")) ? mUnknown
                    : netWorkTypeName + "," + lteNetWorkTypeName);
            } else {
                p.setSummary((netWorkTypeName.equals("UNKNOWN")) ? mUnknown : netWorkTypeName);
            }
        }
    }

    /**
     * CT spec requires that network type should apply to spec
     * "CDMA - EvDo rev. 0" -> "CDMA EVDO"
     * "CDMA - EvDo rev. A" -> "CDMA EVDO"
     * "CDMA - EvDo rev. B" -> "CDMA EVDO"
     * "CDMA - 1xRTT" -> "1x"
     * "GPRS" -> "GSM"
     * "HSDPA" -> "WCDMA"
     * "HSUPA" -> "WCDMA"
     * "HSPA" -> "WCDMA"
     * "HSPA+" -> "WCDMA"
     * "UMTS" -> "WCDMA"
     */
    static String renameNetworkTypeNameForCTSpec(String netWorkTypeName) {
        Log.d("@M_" + TAG, "renameNetworkTypeNameForCTSpec, netWorkTypeName=" + netWorkTypeName);
        if (netWorkTypeName.equals("CDMA - EvDo rev. 0")
                || netWorkTypeName.equals("CDMA - EvDo rev. A")
                || netWorkTypeName.equals("CDMA - EvDo rev. B")) {
            return "CDMA EVDO";
        } else if (netWorkTypeName.equals("CDMA - 1xRTT")) {
            return "CDMA 1x";
        } else if (netWorkTypeName.equals("GPRS")
                || netWorkTypeName.equals("EDGE")
                || netWorkTypeName.equals("GSM")) {
            return "GSM";
        } else if (netWorkTypeName.equals("HSDPA")
                || netWorkTypeName.equals("HSUPA")
                || netWorkTypeName.equals("HSPA")
                || netWorkTypeName.equals("HSPA+")
                || netWorkTypeName.equals("UMTS")) {
            return "WCDMA";
        } else if (netWorkTypeName.equals("CDMA - eHRPD")) {
            return "eHRPD";
        } else {
            return netWorkTypeName;
        }
    }

    private void updateServiceState() {
        setSummaryText(KEY_OPERATOR_NAME, mServiceState.getOperatorAlphaLong());
    }

    void updateSignalStrength() {
        Log.d("@M_" + TAG, "updateSignalStrength()");
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.

        // not loaded in some versions of the code (e.g., zaku)
        if (mSignalStrengthPreference != null) {
            Resources r = getResources();
            boolean isGsmSignal = true;
            boolean isNoNwState = false;
            int signalDbm = 0;
            int signalAsu = 0;

            if (mSignalStrength == null) {
                mSignalStrengthPreference.setSummary("0");
                return;
            }
            isGsmSignal = mSignalStrength.isGsm();
            if (isGsmSignal && (mSlotId == getExternalModemSlot()) && !mServiceState.getRoaming()) {
                if ((ServiceState.STATE_OUT_OF_SERVICE == mServiceState.getDataRegState())
                    || (ServiceState.STATE_POWER_OFF == mServiceState.getDataRegState())) {
                    isNoNwState = true;
                }
            } else {
                if ((ServiceState.STATE_OUT_OF_SERVICE == mServiceState.getVoiceRegState())
                    || (ServiceState.STATE_POWER_OFF == mServiceState.getVoiceRegState())) {
                    isNoNwState = true;
                }
            }
            if (isNoNwState) {
                mSignalStrengthPreference.setSummary("0");
                return;
            }

            signalDbm = mSignalStrength.getDbm();
            signalAsu = mSignalStrength.getAsuLevel();
            Log.d("@M_" + TAG, "updateSignalStrength, SignalStrength is " + signalDbm + " dbm , "
                    + signalAsu + " asu");
            signalDbm = (-1 == signalDbm) ? 0 : signalDbm;
            signalAsu = (-1 == signalAsu) ? 0 : signalAsu;

            if (!isGsmSignal && isLteDataNwReg()) {
                int lteSignalDbm = 0;
                int lteSignalAsu = 0;

                //lteSignalDbm = mSignalStrength.getLteDbm();
                lteSignalDbm = mSignalStrength.getRealLteRsrp();
                lteSignalAsu = mSignalStrength.getLteAsuLevel();
                Log.d("@M_" + TAG, "updateSignalStrength, LTE SignalStrength is "
                        + lteSignalDbm + " dbm , " + lteSignalAsu + " asu");
                lteSignalDbm = (-1 == lteSignalDbm) ? 0 : lteSignalDbm;
                lteSignalAsu = (-1 == lteSignalAsu) ? 0 : lteSignalAsu;
                mSignalStrengthPreference.setSummary("CDMA "
                    + String.valueOf(signalDbm) + " "
                    + r.getString(R.string.radioInfo_display_dbm) + " "
                    + String.valueOf(signalAsu) + " "
                    + r.getString(R.string.radioInfo_display_asu)
                    + " \nLTE "
                    + String.valueOf(lteSignalDbm) + " "
                    + r.getString(R.string.radioInfo_display_dbm) + " "
                    + String.valueOf(lteSignalAsu) + " "
                    + r.getString(R.string.radioInfo_display_asu));
            } else {
                mSignalStrengthPreference.setSummary(String.valueOf(signalDbm) + " "
                    + r.getString(R.string.radioInfo_display_dbm) + " "
                    + String.valueOf(signalAsu) + " "
                    + r.getString(R.string.radioInfo_display_asu));
            }
        }
    }


    /**
     * The async task for get SMS service center address.
     */
    class ScAsyncTask extends AsyncTask {
        @Override
        protected String doInBackground(Object... params) {
            return mTelephonyManagerEx.getScAddress(mSubId);
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);

            String gotScNumber = (String) result;
            Log.d("@M_" + TAG, this + " Sms Service Center: " + gotScNumber);
            setSummaryText(KEY_SMS_CENTER, gotScNumber);
        }
    }


     private int getExternalModemSlot() {
      int sExternalMD = -1;
        if (sExternalMD < 0) {
            sExternalMD = SystemProperties.getInt("ril.external.md", 0);
        }
        return sExternalMD - 1;
    }
    private boolean checkTargetSlotEnable() {
        boolean isAirplanMode = Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) > 0;
        if (isAirplanMode) {
           return false;
        }
        boolean isRadioInOn = false;
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        try {
            if (telephony != null) {
                isRadioInOn = telephony.isRadioOnForSubscriber(mSubId, PACKAGE_NAME);
                Log.d("@M_" + TAG, "Slot " + mSlotId + " is in radion state " + isRadioInOn);
            }
        } catch (RemoteException e) {
            Log.e("@M_" + TAG, "Telephony exception");
        }
        return isRadioInOn;

    }
}

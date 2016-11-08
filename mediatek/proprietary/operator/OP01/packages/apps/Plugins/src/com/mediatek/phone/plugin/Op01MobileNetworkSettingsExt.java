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

package com.mediatek.phone.plugin;

import android.preference.PreferenceActivity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.op01.plugin.R;
import com.mediatek.phone.ext.DefaultMobileNetworkSettingsExt;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;



@PluginImpl(interfaceName="com.mediatek.phone.ext.IMobileNetworkSettingsExt")
public class Op01MobileNetworkSettingsExt extends DefaultMobileNetworkSettingsExt {
    private static final String LOG_TAG = "Op01MobileNetworkSettingsExt";

    //private PreferenceScreen mPreferenceScreen;
    private ListPreference mListPreference;
    public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    public static final String BUTTON_ENABLED_NETWORK_MODE = "enabled_networks_key";
    public static final String BUTTON_NETWORK_MODE_LTE_KEY = "button_network_mode_LTE_key";
    public static final String SIM = "SIM";
    private final static String LTE_SUPPORT = "1";
    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;
    private static final int MODE_PHONE1_ONLY = 1;
    private Context mContext = null;
    private int mCurrentTab = -1;
    private boolean mFlag = false;
    private static final String[] MCCMNC_TABLE_TYPE_CU = {
        "46001", "46006", "46009", "45407", "46005"};
    private static final String[] MCCMNC_TABLE_TYPE_CT = {
        "45502", "46003", "46011", "46012", "46013"};

    public Op01MobileNetworkSettingsExt(Context context) {
        super();
        mContext = context;
        log("mContext =" + mContext);
    }

    /**
     * init CMCC network preference screen
     * @param activity
     * @param currentTab current Tab
     */
    public void initMobileNetworkSettings(PreferenceActivity activity, int currentTab) {
        log("initMobileNetworkSettings currentTab: " + currentTab);
        mCurrentTab = currentTab;
        if (activity != null) {
            if (mFlag == false) {
                log("initMobileNetworkSettings,mFlag==false");
                IntentFilter filter= new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                mContext.registerReceiver(mReceiver, filter);
                mFlag = true;
            }
        } else {
            log("initMobileNetworkSettings, preferenceScreen null");
            return;
        }
        PreferenceScreen mPreferenceScreen = activity.getPreferenceScreen();
        ListPreference buttonEnabledNetworkPreference = 
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_ENABLED_NETWORK_MODE);
        ListPreference buttonPreferredNetworkPreference =
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_PREFERED_NETWORK_MODE);
        ListPreference buttonNetworkModeLtePreference =
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_NETWORK_MODE_LTE_KEY);

        if (isLTESupport()) {
            if (buttonEnabledNetworkPreference != null) {
                dealInitNetworkMode(buttonEnabledNetworkPreference);
                mListPreference = buttonEnabledNetworkPreference;
            } else if (buttonPreferredNetworkPreference != null) {
                dealInitNetworkMode(buttonPreferredNetworkPreference);
                mListPreference = buttonPreferredNetworkPreference;
            } else if(buttonNetworkModeLtePreference != null) {
                dealInitNetworkMode(buttonNetworkModeLtePreference);
                mListPreference = buttonNetworkModeLtePreference;
            }
        }
    }

    /**
     *up is internal use, under is host to call
     * @param preference
     */
    public void updateNetworkTypeSummary(ListPreference preference) {
        if(preference == null) {
            return;
        }
        int subId = get34GCapabilitySubId();
        int slotId = get34GCapabilitySIMSlotId();
        log("updateNetworkTypeSummary, slotId:" + slotId);
         // for SIM
        String type = getSIMType(subId);
        log("updateNetworkTypeSummary, type: " + type);
        if (isLTESupport()) {
            if (slotId == -1 || isNotPrimarySIM(slotId) ||
                    !isLTEModeEnable(slotId, preference.getContext())) {
                preference.setSummary("");
                preference.setEnabled(false);
                log("updateNetworkTypeSummary, summary set empty and disable");
            } else if (SIM.equals(type)) {
                preference.setValue(Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                preference.setSummary(preference.getEntry());
                preference.setEnabled(false);
                log("updateNetworkTypeSummary, SIM type");
            } else {
                Phone phone = getPhoneUsingSubId(subId);
                //if(mPreferenceScreen != null) {
                    //Phone.NT_MODE_LTE_GSM_WCDMA is 9
                    int settingsNetworkMode = android.provider.Settings.Global.getInt(
                            phone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                            Phone.NT_MODE_LTE_GSM_WCDMA);
                    log("updateNetworkTypeSummary mode:" +  settingsNetworkMode);
                    if (settingsNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                            settingsNetworkMode == Phone.NT_MODE_LTE_GSM_WCDMA) {
                        preference.setValue(Integer.toString(settingsNetworkMode));
                        log("updateNetworkTypeSummary, summary:" + preference.getEntry());
                        if(preference.getEntry() != null){
                           preference.setSummary(preference.getEntry());
                           preference.setEnabled(true);
                        } else {
                           preference.setSummary("");
                           //preference.setEnabled(false);
                           log("updateNetworkTypeSummary, set summary  empty");
                        }
                    } else {
                        preference.setSummary("");
                        //preference.setEnabled(false);
                        log("updateNetworkTypeSummary, set summary  empty");
                    }
                //}
           }
        }
    }

    /**
     * update LTE mode status
     * @param preference
     */
    public void updateLTEModeStatus(ListPreference preference) {
        log("updateLTEModeStatus");
        updateNetworkTypeSummary(preference);
        if (!preference.isEnabled()) {
            Dialog dialog = preference.getDialog();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                log("updateLTEModeStatus: dismiss dialog ");
            }
        }
    }

    /**
     * Preference Change, update network preference value and summary
     * @param preference
     * @param objValue
     */
    public void onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals(BUTTON_ENABLED_NETWORK_MODE)) {
            int buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int subId = get34GCapabilitySubId();
            Phone phone = getPhoneUsingSubId(subId);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    phone.getContext().getContentResolver(),
                    android.provider.Settings.Global.USER_PREFERRED_NETWORK_MODE + subId,
                    Phone.NT_MODE_LTE_GSM_WCDMA);
            log("onPreferenceChange, buttonNetworkMode: " + buttonNetworkMode +
                    " settingsNetworkMode: " + settingsNetworkMode);
            ListPreference networkPreference = (ListPreference) preference;
            networkPreference.setValue(Integer.toString(buttonNetworkMode));
            networkPreference.setSummary(networkPreference.getEntry());
        }
    }

    /**
     * app use to update LTE mode value and summary
     * just for LTE, only show 0 and 9,Phone.NT_MODE_LTE_GSM_WCDMA-9,Phone.NT_MODE_WCDMA_PREF--0
     * but Phone.NT_MODE_GSM_UMTS is 3 trans to 0,because sim the networkmode for LTE is disable,
     * this only fit to usim
     * @param listLteNetworkMode
     */
    private void dealInitNetworkMode(ListPreference preference) {
        if (mContext != null) {
            log("dealInitNetworkMode, mContext is not null");
            preference.setEntries(
                    mContext.getResources().getStringArray(R.array.lte_network_mode_choices));
            preference.setEntryValues(
                    mContext.getResources().getStringArray(R.array.lte_network_mode_values));
        }
        updateNetworkTypeSummary(preference);
    }

    /**
     * is LTE Mode Enable
     * @param slotId
     * @param context
     * @return true or false
     */
    private boolean isLTEModeEnable(int slotId, Context context) {
        log("isLTEModeEnable, slotId = " + slotId);
        if(getRadioStateForSlotId(slotId, context) == RADIO_POWER_OFF ||
                getSimOperator(slotId) == null || getSimOperator(slotId).equals("")) {
            log("RadioState == RADIO_POWER_OFF, or SimOperator is null");
            return false;
        }

        if (!isSupportWorldPhonePolicy() && isCUOrCTCard(slotId)) {
            log("isLTEModeEnable, WorldPhonePolicy CU CT card, shoule be disabled");
            return false;
        }

        // for forigen LTE
        if(!isWorldPhoneSupport() && !isInChina(get34GCapabilitySubId())) {
            log("isLTEModeEnable, is not worldphone and china, forigen should disable");
            return false;
        }

        log("isLTEModeEnable, should enable");
        return true;
    }

    /**
     * get 3G/4G capability slotId
     * @return the SIM id which support 3G/4G.
     */
    private int get34GCapabilitySIMSlotId() {
        int slotId = -1;
        int subId = get34GCapabilitySubId();
        if(subId >= 0) {
            slotId = SubscriptionManager.getSlotId(subId);
        }
        log("get4GCapabilitySIMSlotId, slotId: " + slotId);
        return slotId;
    }

    /**
     * get the 3G/4G Capability subId.
     * @return the 3G/4G Capability subId
     */
    private int get34GCapabilitySubId() {
        int subId = -1;
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.
                getService(Context.TELEPHONY_SERVICE_EX));
        if (iTelEx != null) {
                try {
                int phoneId = iTelEx.getMainCapabilityPhoneId();
                log("subId : " + subId + ", PhoneId : " + phoneId);
                if (phoneId >= 0) {
                    subId = SubscriptionManager.getSubIdUsingPhoneId(iTelEx.
                            getMainCapabilityPhoneId());
                    }
                } catch (RemoteException e) {
                log("get34GCapabilitySubId FAIL to getSubId" + e.getMessage());
            }
        }
        return subId;
    }

    /**
     * get radio state for slot id
     * @param slotId
     * @param context
     * @return radio state, on or off
     */
    private boolean getRadioStateForSlotId(final int slotId, Context context) {
        int currentSimMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                RADIO_POWER_OFF : RADIO_POWER_ON;
        log("soltId: " + slotId + ", radiosState : " + radiosState);
        return radiosState;
    }

    /**
     * app use to judge the Card is CU or CT
     * @param slotId
     * @return true is CU
     */
    private boolean isCUOrCTCard(int slotId) {
        log("isCUOrCTCard, slotId = " + slotId);
        String simOperator = null;
        simOperator = getSimOperator(slotId);
        if (simOperator != null) {
            log("isCUOrCTCard, simOperator =" + simOperator);
            for (String mccmnc : MCCMNC_TABLE_TYPE_CU) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }

            for (String mccmnc : MCCMNC_TABLE_TYPE_CT) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    * app use to judge wheather is in china
    * @param void
    * @return true is in china
    */
    private boolean isInChina(int subId) {
        log("isInChina");
        ITelephonyEx iTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.
                getService("phoneEx"));
        String strLocatedPlmn = null;
        try {
            strLocatedPlmn = iTelephonyEx.getLocatedPlmn(subId);
            if (strLocatedPlmn != null) {
                String strLocatedMcc = strLocatedPlmn.substring(0,3);
                log("isInChina::strLocatedPlmn:" + strLocatedPlmn +
                        " strLocatedMcc:" + strLocatedMcc);
                if (strLocatedMcc.equals("460"))
                    return true;
            } else {
                log("isInChina::LocatedMcc is null but now as true");
                return true;
            }
        } catch (RemoteException e) {
            log("isCsfbMode exception: ");
        }
       return false;
   }

    /**
    * app use to which is 5 mode or 3 mode
    * @param void
    * @return true is 5 mode, false is 3 mode
    */
    private boolean isWorldPhoneSupport() {
        boolean IS_WORLD_PHONE_SUPPORT = (SystemProperties.getInt(
                "ro.mtk_world_phone", 0) == 1);
        log("isWorldPhone:" + IS_WORLD_PHONE_SUPPORT);
        return IS_WORLD_PHONE_SUPPORT;
    }

    /**
     * check the slotId value.
     * @param slotId
     * @return true or false
    */
    private boolean isValidSlot(int slotId) {
        final int[] geminiSlots = {0, 1};
        for (int i = 0; i < geminiSlots.length; i++) {
            if (geminiSlots[i] == slotId) {
                return true;
            }
        }
        return false;
     }

    /**
      * @get SimConfig by TelephonyManager.getDefault().getMultiSimConfiguration().
      * @return true if the device has 2 or more slots
      */
    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants mSimConfig = TelephonyManager.
                getDefault().getMultiSimConfiguration();
        if (mSimConfig == TelephonyManager.MultiSimVariants.DSDS ||
                mSimConfig == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    /**
     * get the state of the device SIM card
     * @param slotId
     * @return return SIM state.
     */
    private int getSimState(int slotId) {
        int status;
        if (isGeminiSupport() && isValidSlot(slotId)) {
            status = TelephonyManagerEx.getDefault().getSimState(slotId);
        } else {
            status = TelephonyManager.getDefault().getSimState();
        }
        log("getSimState, slotId = " + slotId + "; status = " + status);
        return status;
    }

    /**
     * Gets the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM.
     * 5 or 6 decimal digits.
     * Availability: The result of calling getSimState() must be 
     * android.telephony.TelephonyManager.SIM_STATE_READY.
     * @param slotId  Indicates which SIM to query.
     * @return MCC+MNC (mobile country code + mobile network code) of the provider of the SIM.
     *         5 or 6 decimal digits.
     */
    private String getSimOperator(int slotId) {
       String simOperator = null;
       boolean isSimStateReady = false;
       isSimStateReady = TelephonyManager.SIM_STATE_READY == getSimState(slotId);
       if (isSimStateReady) {
           if (isGeminiSupport()) {
               simOperator = TelephonyManagerEx.getDefault().getSimOperator(slotId);
           } else {
               simOperator = TelephonyManager.getDefault().getSimOperator();
           }
       }
       log("getSimOperator, simOperator = " + simOperator + " slotId = " + slotId);
       return simOperator;
    }

    /**
     * app use to judge LTE open
     * @return true is LTE open
     */
    private boolean isLTESupport() {
        boolean isSupport = LTE_SUPPORT.equals(
                SystemProperties.get("ro.mtk_lte_support")) ? true : false;
        log("isLTESupport(): " + isSupport);
        return isSupport;
    }

    private String getSIMType(int subId) {
        String type = null;
        if (subId > 0) {
            try {
                   type = ITelephonyEx.Stub.asInterface(ServiceManager.
                           getService("phoneEx")).getIccCardType(subId);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "getSIMType, exception: ", e);
            }
        }
        return type;
     }

    /**
     * Get the sub's display name.
     * @param subId the sub id
     * @return the sub's display name, may return null
     */
    private String getSubDisplayName(int subId) {
        String displayName = "";
        SubscriptionInfo subInfo = null;
        if(subId > 0) {
            subInfo = SubscriptionManager.from(mContext).getSubscriptionInfo(subId);
            if (subInfo != null) {
                displayName = subInfo.getDisplayName().toString();
            }
        }
        return displayName;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("action: " + action);
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                    && mListPreference != null) {
                updateNetworkTypeSummary(mListPreference);
            }
        }
    };

    /**
     * Get phone by sub id.
     * @param subId the sub id
     * @return phone according to the sub id
     */
    public Phone getPhoneUsingSubId(int subId) {
        log("getPhoneUsingSubId subId:" + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if ((phoneId < 0) || (phoneId >= TelephonyManager.getDefault().getPhoneCount()) || (phoneId == Integer.MAX_VALUE)) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    /**
     * 
     * @return true when support world phone policy
     */
    public boolean isSupportWorldPhonePolicy() {
        boolean isSupport = false;
        if (SystemProperties.get("ro.mtk_world_phone_policy").equals("1")) {
            isSupport = true;
        }
        log("isSupportWorldPhonePolicy(): " + isSupport);
        return isSupport;
    }

    /**
     * adjust not primary SIM.
     * @param slotId the sub id.
     * @return true if not primary SIM.
     */
    public boolean isNotPrimarySIM(int slotId) {
        List<SubscriptionInfo> result = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        if (result == null) {
            log("isNotPrimarySIM false, result == null");
        } else {
            if (isGeminiSupport() && result.size() > 1 && slotId != mCurrentTab) {
                return true;
            }
        }
        log("isNotPrimarySIM false, slotId:" + slotId);
        return false;
    }

    /**
     * unRegister mReceiver.
     */
    public void unRegister() {
        log("unRegister");
        if (mFlag == true && mReceiver != null) {
             log("unRegister,mFlag==true");
             mContext.unregisterReceiver(mReceiver);
             mFlag = false;
        }
        mListPreference = null;
    }

    public void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}

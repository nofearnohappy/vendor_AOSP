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

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;
import com.mediatek.op02.plugin.R;
import com.mediatek.phone.ext.DefaultMobileNetworkSettingsExt;

import java.util.List;

/**
 * CU feature,when sim2 is cu card,user can't choose.
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.IMobileNetworkSettingsExt")
public class OP02MobileNetworkSettingExt extends DefaultMobileNetworkSettingsExt {

    private static final String TAG = "OP02MobileNetworkSettingExt";
    private static final String PACKAGE_NAME = "com.mediatek.op02.plugin";
    private Context mOp02Context;
    public PreferenceScreen mPreferenceScreen;
    public SubscriptionManager mSubscriptionManager;
    /**
     * Construct method.
     * @param context context
     */
    public OP02MobileNetworkSettingExt(Context context) {
        mOp02Context = context;
        mSubscriptionManager = SubscriptionManager.from(mOp02Context);
        Log.d(TAG, "OP02NetworkSettingExt");
    }

    @Override
    public void customizePreferredNetworkMode(ListPreference listPreference, int subId) {
        int slotId = SubscriptionManager.getSlotId(subId);
        Log.d(TAG, "customizePreferredNetworkMode subId = " + subId + " slotId = " + slotId);
        String value = SystemProperties.get("ro.mtk_disable_cap_switch");
        boolean needDisable = false;
        if (value != null && value.equals("1")) {
            if (slotId == PhoneConstants.SIM_ID_2) {
                needDisable = true;
            }
        } else {
            int defaultSubId = SubscriptionManager.getDefaultDataSubId();
            int mainCapabilitySubId = get34GCapabilitySubId();
            Log.d(TAG, "customizePreferredNetworkMode defaultSubId = " + defaultSubId
                + ", mainCapabilitySubId = " + mainCapabilitySubId);
            if (mainCapabilitySubId != subId) {
                needDisable = true;
            }
        }
        Log.i(TAG, "sim switch value=" + value + ", needDisable = " + needDisable);
        if (needDisable) {
            listPreference.setEnabled(false);
            listPreference.setSummary(mOp02Context.getResources().getString(R.string.only_gsm));
        }
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
                if (phoneId >= 0) {
                    subId = SubscriptionManager.getSubIdUsingPhoneId(iTelEx.
                            getMainCapabilityPhoneId());
                }
            } catch (RemoteException e) {
                Log.d(TAG, "get34GCapabilitySubId FAIL to getSubId" + e.getMessage());
            }
        }
        return subId;
    }

    /**
     * Set the listPreference enable or not.
     * @param listPreference listPreference
     */
    private void customizeEnabled(ListPreference listPreference, int slotId) {
        if (slotId == PhoneConstants.SIM_ID_2) {
            listPreference.setEnabled(false);
        }
    }

    /**
     * Set the listPreference'Summary.
     * @param listPreference listPreference
     */
    private void customizeSummary(ListPreference listPreference, int slotId) {
        if (slotId == PhoneConstants.SIM_ID_2) {
            listPreference.setSummary(mOp02Context.getResources().getString(R.string.only_gsm));
        }
    }

    private boolean isIdle() {
        Log.i(TAG, "isIdle = " + (TelephonyManager.getDefault().getCallState()
                == TelephonyManager.CALL_STATE_IDLE));
        return (TelephonyManager.getDefault().getCallState()
                == TelephonyManager.CALL_STATE_IDLE);
    }

    /**
     * Check the sim is insert or not.
     * @param simId sim id
     * @return true if sim is insert
     */
    public static boolean isSIMInserted(int simId) {
        try {
            ITelephony tmex = ITelephony.Stub.asInterface(android.os.ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            Log.i(TAG, "isSIMInserted return "
                    + (tmex != null && tmex.hasIccCardUsingSlotId(simId)));
            return (tmex != null && tmex.hasIccCardUsingSlotId(simId));
        } catch (RemoteException e) {
            Log.i(TAG, "isSIMInserted return false because catch RemoteException");
            return false;
        }
    }

    /**
     * Check the sim radio on or not.
     * @param simId which sim will be check
     * @return true if simId is radio on
     */
    public boolean isSimRadioOn(int simId) {
        Log.i(TAG, "isSimRadioOn  simId = " + simId);
        boolean isRadioOn = false;
        int[] subId = SubscriptionManager.getSubId(simId);
        if (subId != null && subId.length > 0) {
            for (int i = 0; i < subId.length; i++) {
                if (isRadioOn(subId[i])) {
                    isRadioOn = true;
                    break;
                }
             }
        }
        Log.i(TAG, "isSimRadioOn isRadioOn =" + isRadioOn);
        return isRadioOn;
    }

    /**
     * check the radio is on or off by sub id.
     *
     * @param subId the sub id
     * @return true if radio on
     */
    public boolean isRadioOn(int subId) {
        Log.i(TAG, "isRadioOn  subId = " + subId);
        boolean isRadioOn = false;
        final ITelephony iTel = ITelephony.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel != null && isValidSubId(subId)) {
            try {
                isRadioOn = iTel.isRadioOnForSubscriber(subId, PACKAGE_NAME);
            } catch (RemoteException e) {
                Log.i(TAG, "isRadioOn  failed to get radio state for sub = " + subId);
                isRadioOn = false;
            }
        } else {
            Log.i(TAG, "isRadioOn  failed because  iTel= null, subId =" + subId);
        }
        Log.i(TAG, "isRadioOn isRadioOn =" + isRadioOn);
        return isRadioOn;
    }

    /**
     * Check whether there has active SubInfo indicated by given subId on the device.
     * @param subId the sub id
     * @return true if the sub id is valid, else return false
     */
    public boolean isValidSubId(int subId) {
        boolean isValid = false;
        SubscriptionInfo subscriptionInfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subscriptionInfo != null) {
            isValid = true;
        }
        Log.i(TAG, "isValidSubId isValid = " + isValid + " subId = " + subId);
        return isValid;
    }
}

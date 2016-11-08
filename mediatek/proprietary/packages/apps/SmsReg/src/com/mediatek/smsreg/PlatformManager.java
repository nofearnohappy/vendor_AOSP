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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.smsreg;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.custom.CustomProperties;
import com.mediatek.telephony.SmsManagerEx;
import com.mediatek.telephony.TelephonyManagerEx;

import java.io.File;
import java.util.Arrays;

public class PlatformManager {
    private static final String TAG = "SmsReg/PlatformManager";

    // Do not modify value of this key, which need to be same with framework
    private static final String KEY_BOOT_ENABLE = "dm_boot_start_enable_key";
    private static final int VALUE_DEFAULT_BOOT = 1;
    private static final long INVALID_SUB_ID = SubscriptionManager.INVALID_SUB_ID;

    public static final int SIM_NUMBER;

    public static final int SUB_NUMBER = 8;

    static {
        if ("1".equals(SystemProperties.get("ro.mtk_gemini_support"))) {
            SIM_NUMBER = TelephonyManager.getDefault().getPhoneCount();
        } else {
            SIM_NUMBER = 1;
        }
    }

    // Configuration path related
    private static final String CONFIG_FILE = "smsSelfRegConfig.xml";
    private static final String TEST_PATH_IN_SYSTEM = "/system/etc/dm/test/";
    private static final String PRODUCTIVE_PATH_IN_SYSTEM = "/system/etc/dm/productive/";

    private static final String TEST_PATH_IN_CUSTOM = "/custom/etc/dm/test/";
    private static final String PRODUCTIVE_PATH_IN_CUSTOM = "/custom/etc/dm/productive/";

    // Read & write IMSI, switch or SmsRegSwitch value (not affected when factory reset)
    private DmAgent mAgent;
    private TelephonyManager mTelephonyManager;

    public PlatformManager() {
        IBinder binder = ServiceManager.getService("DmAgent");
        if (binder == null) {
            throw new Error("Failed to get binder");
        }
        mAgent = DmAgent.Stub.asInterface(binder);
        if (mAgent == null) {
            throw new Error("Failed to get DmAgent");
        }

        mTelephonyManager = TelephonyManager.getDefault();
        if (mTelephonyManager == null) {
            throw new Error("Failed to get TelephonyManager");
        }
    }

    /**
     * Whether is enabled when reboot (set in settings)
     */
    public boolean isAutoBoot(Context context) {
        int value = android.provider.Settings.System.getInt(context.getContentResolver(),
                KEY_BOOT_ENABLE, VALUE_DEFAULT_BOOT);
        Log.i(TAG, "AutoBoot enable state is  " + value);
        if (value == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * A wrapper to send a data message through SmsManagerEx
     */
    public void sendDataMessage(String dstAddr, short dstPort, Short srcPort, byte[] data,
            PendingIntent sentIntent, long subId) {
        String scAddress = null;
        PendingIntent deliveryIntent = null;

        SmsManagerEx.getDefault().sendDataMessage(dstAddr, scAddress, dstPort, srcPort, data,
                sentIntent, deliveryIntent, SubscriptionManager.getSlotId(subId));
    }

    // ********************************************
    // DmAgent related
    // ********************************************
    /**
     * Get path of the configuration xml when running
     */
    public String getConfigPath() {
        if ("1".equals(getSwitchValue())) {
            File configFile = new File(PRODUCTIVE_PATH_IN_CUSTOM + CONFIG_FILE);
            if (configFile.exists()) {
                Log.i(TAG, "CIP PRODUCTIVE Smsreg Config Path");
                return PRODUCTIVE_PATH_IN_CUSTOM + CONFIG_FILE;
            } else {
                return PRODUCTIVE_PATH_IN_SYSTEM + CONFIG_FILE;
            }
        } else {
            File configFile = new File(TEST_PATH_IN_CUSTOM + CONFIG_FILE);
            if (configFile.exists()) {
                Log.i(TAG, "CIP TEST Smsreg Config Path");
                return TEST_PATH_IN_CUSTOM + CONFIG_FILE;
            } else {
                return TEST_PATH_IN_SYSTEM + CONFIG_FILE;
            }
        }
    }

    /**
     * Get state of environment: 0-test, 1-productive
     */
    private String getSwitchValue() {
        try {
            byte[] switchValue = mAgent.getSwitchValue();
            if (switchValue != null) {
                return new String(switchValue);
            } else {
                return "0";
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Reset IMSI & SmsRegSwitch value when switch (test -> productive or productive -> test)
     */
    public void clearFileWhenSwitch() {
        try {
            byte[] switchValue = mAgent.getSmsRegSwitchValue();
            if (switchValue != null && (new String(switchValue)).equals("1")) {
                Log.i(TAG, "There is a pending SmsReg flag.");
                mAgent.writeImsi("0".getBytes());
                mAgent.setSmsRegSwitchValue("0".getBytes());
                Log.i(TAG, "IMSI cleared.");
            } else {
                Log.i(TAG, "There is no pending SmsReg flag.");
            }

        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Read saved IMSI via DmAgent
     */
    public String getSavedImsi() {
        String savedImsi = null;

        try {
            byte[] imsiByte = mAgent.readImsi();
            savedImsi = (imsiByte == null) ? null : new String(imsiByte);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Get saved imsi " + savedImsi + ".");
        return savedImsi;
    }

    /**
     * Read saved IMSI via DmAgent
     */
    public void setSavedImsi(String imsi) {
        try {
            mAgent.writeImsi(imsi.getBytes());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "save imsi [" + imsi + "]");
    }

    /**
     * whether SmsReg is enabled via DmAgent
     */
    public boolean isSmsRegEnabled() {
        // SmsReg state: 0-disable, 1-enable
        int savedCTA = 0;
        try {
            byte[] ctaBytes = mAgent.getRegisterSwitch();
            savedCTA = Integer.parseInt(ctaBytes == null ? null : new String(ctaBytes));
        } catch (RemoteException e) {
            Log.e(TAG, "Get savedCTA failed.");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "SmsReg is enabled? " + (savedCTA == 1));
        return savedCTA == 1;
    }

    // ********************************************
    // Telephony related
    // ********************************************

    /**
     * Init the activated sub id list, sort and return length
     */
    public int initSubIdList(long[] simList) {
        long[] subId = SubscriptionManager.getActiveSubIdList();

        // Sort in ascend order
        Arrays.sort(subId);

        int i = 0;
        for (; i < subId.length; ++i) {
            simList[i] = subId[i];
        }

        for (; i < SUB_NUMBER; ++i) {
            simList[i] = INVALID_SUB_ID;
        }

        return subId.length;
    }

    public String getDeviceImei() {
        String imei = mTelephonyManager.getDeviceId(SmsRegConst.GEMINI_SIM_1);
        return imei;
    }

    public String getSubImsi(long subId) {
        return mTelephonyManager.getSubscriberId(subId);
    }

    public String getNetworkCountryIso(long subId) {
        return mTelephonyManager.getNetworkCountryIso(subId);
    }

    public String getSubCountryIso(long subId) {
        return mTelephonyManager.getSimCountryIso(subId);
    }

    public String getSubOperator(long subId) {
        return mTelephonyManager.getSimOperator(subId);
    }

    public int getSubState(long subId) {
        return mTelephonyManager.getSimState(SubscriptionManager.getSlotId(subId));
    }

    public boolean hasSimCard(long slotId) {
        return mTelephonyManager.hasIccCard(slotId);
    }

    public void listenPhoneState(PhoneStateListener listener, int simId) {
        Log.i(TAG, "Listen phone state for " + simId + ".");
        TelephonyManagerEx.getDefault().listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE,
                simId);
    }

    public void unListenPhoneState(PhoneStateListener listener, int simId) {
        Log.i(TAG, "Unlisten phone state for " + simId + ".");
        TelephonyManagerEx.getDefault().listen(listener, PhoneStateListener.LISTEN_NONE, simId);
    }

    // ********************************************
    // CustomProperties related
    // ********************************************
    public String getDeviceVersion() {
        String sWVerno = CustomProperties.getString("Setting", "SWVerno", Build.DISPLAY);
        Log.i(TAG, "Device version is " + sWVerno + " while Build.DISPLAY is " + Build.DISPLAY);
        return sWVerno;
    }

    public String getManufacturerName() {
        String manufacturer = CustomProperties.getString(CustomProperties.MODULE_DM,
                CustomProperties.MANUFACTURER, "MTK1");
        Log.i(TAG, "Manufacturer('MTK1' is for test): " + manufacturer);
        return manufacturer;
    }

    public String getProductName() {
        String product = CustomProperties.getString(CustomProperties.MODULE_DM,
                CustomProperties.MODEL, "MTK");
        Log.i(TAG, "Product('MTK' is for test): " + product);
        return product;
    }

    /**
     * Get slotId associated with the subscription.
     */
    public int getSlotId(long subId) {
        return SubscriptionManager.getSlotId(subId);
    }

    /**
     * Start an alarm with certain intent
     */
    public void startAlarm(Context context, PendingIntent pendingIntent, long delay) {
        Log.i(TAG,
                "Start alarm, delay " + (delay / 1000) + "s, intent " + pendingIntent.getIntent());
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, java.lang.System.currentTimeMillis() + delay,
                pendingIntent);
    }

    /**
     * Start an alarm with certain intent
     */
    public void cancelAlarm(Context context, PendingIntent pendingIntent) {
        Log.i(TAG, "Cancel alarm , intent " + pendingIntent.getIntent());
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pendingIntent);
    }
}

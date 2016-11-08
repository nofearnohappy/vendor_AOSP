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

package com.mediatek.configurecheck;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncResult;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import android.os.ServiceManager;
import com.mediatek.common.dm.DmAgent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;

import com.mediatek.custom.CustomProperties;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;


class CheckIMEI extends CheckItemBase {

    private String mImei = null;

    CheckIMEI(Context c, String key) {
        super(c, key);
        /* No check + No auto configration */

        setTitle(R.string.imei_title);
        setNote(R.string.imei_note);
    }

    public boolean onCheck() {
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        TelephonyManager tmEx = TelephonyManager.getDefault();
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            if (!getKey().equals(CheckItemKeySet.CI_IMEI_IN_DM)) {
                //if is Gemini phone, need to query two IMEI, except
                //CI_IMEI_IN_DM which only need to query the default IMEI
                String nextLine = System.getProperty("line.separator");
                String sim1IMEI = tmEx.getDeviceId(PhoneConstants.SIM_ID_1);
                CTSCLog.e("IMEI", "sim1IMEI: " + sim1IMEI);
                String sim2IMEI = tmEx.getDeviceId(PhoneConstants.SIM_ID_2);
                CTSCLog.e("IMEI", "sim2IMEI: " + sim2IMEI);

                StringBuilder sb = new StringBuilder(getContext().getString(R.string.imei_slot1));
                if (null == sim1IMEI) {
                    sb.append("null");
                } else {
                    sb.append(sim1IMEI);
                }
                sb.append(nextLine).append(getContext().getString(R.string.imei_slot2));
                if (null == sim2IMEI) {
                    sb.append("null");
                } else {
                    sb.append(sim2IMEI);
                }

                mImei = sb.toString();
                setValue(mImei);
                return true;
            }
        }

        mImei = tm.getDeviceId();
        CTSCLog.e("IMEI", "IMEI: " + mImei);

        if (mImei == null) {
            setValue("null");
        } else {
            setValue(mImei);
        }
        return true;
    }

    public check_result getCheckResult() {

        mResult = super.getCheckResult();
        if (mImei == null && (check_result.UNKNOWN == mResult)) {
            mResult = check_result.WRONG;
        }
        return mResult;
    }
}

class CheckDMState extends CheckItemBase {

    CheckDMState(Context c, String key) {
        super(c, key);

        setTitle(R.string.smsreg_state_title);
        if (key.equals(CheckItemKeySet.CI_DMSTATE_CHECK_ON_ONLY)) {
            setNote(R.string.smsreg_note_on);
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_DMSTATE_ON_CONFG)){
            setNote(R.string.smsreg_note_on);
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DMSTATE_OFF_CONFG)){
            setNote(R.string.smsreg_note_off);
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        }
    }

    private DmAgent getDmAgent() {
        IBinder binder = ServiceManager.getService("DmAgent");
        return DmAgent.Stub.asInterface(binder);
    }

    public check_result getCheckResult() {
        check_result result = check_result.UNKNOWN;
        DmAgent dma = getDmAgent();
        if (dma == null) {
            CTSCLog.e("DMCheck", "DM agent is null ");
            setValue(R.string.smsreg_err_no_agent);
            return result;
        }

        try {
            byte[] data = dma.getRegisterSwitch();
            int cta = 0;
            if (data != null) {
                cta = Integer.parseInt(new String(data));
                CTSCLog.e("DMCheck", "dm sting " + new String(data));
            }

                if (cta == 1) {
                    setValue(R.string.ctsc_enabled);
                } else {
                    setValue(R.string.ctsc_disabled);
                }

                if (getKey().equals(CheckItemKeySet.CI_DMSTATE_ON_CONFG) ||
                    getKey().equals(CheckItemKeySet.CI_DMSTATE_CHECK_ON_ONLY)) {
                    result = (cta == 1) ? check_result.RIGHT : check_result.WRONG;
                } else if (getKey().equals(CheckItemKeySet.CI_DMSTATE_OFF_CONFG)) {
                    result = (cta == 1) ? check_result.WRONG : check_result.RIGHT;
                }else {
                    result = super.getCheckResult();
                }
                CTSCLog.e("DMCheck", "dm result" + result);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        CTSCLog.e("DMCheck", "result = " + result);
        return result;
    }

    public boolean onReset() {
        if (!isConfigurable()) {
            return false;
        }

        DmAgent dma = getDmAgent();
        if (null == dma) {
            return false;
        }
        String data = new String("1");
        if (getKey().equals(CheckItemKeySet.CI_DMSTATE_OFF_CONFG)) {
            data = "0";
        }
        try {
            dma.setRegisterSwitch(data.getBytes());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return true;
    }
}

class CheckMisConfig extends CheckItemBase {
    private static final String TAG = "CheckMisConfig";
    private static final String KEY_MISC_CONFIG = Settings.Global.TELEPHONY_MISC_FEATURE_CONFIG;
    private int mConfig;
    private int index = 2; //self config index is 2

    CheckMisConfig(Context c, String key) {
        super(c, key);

        setTitle(R.string.MisConfig_title);
        if (key.equals(CheckItemKeySet.CI_MISCONFIG_ON_CHECK)) {
            setNote(R.string.MisConfig_note_on);
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_MISCONFIG_ON_CONFIG)) {
            setNote(R.string.MisConfig_note_on);
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG)) {
            setNote(R.string.MisConfig_note_off);
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        }
    }

    public boolean onCheck() {
        mConfig = Settings.Global.getInt(getContext().getContentResolver(), KEY_MISC_CONFIG, 0);
        CTSCLog.e(TAG, "Current config is:" + mConfig);
        if ((mConfig & (1 << index)) != 0) {
            if (getKey().equals(CheckItemKeySet.CI_MISCONFIG_ON_CONFIG) ||
                getKey().equals(CheckItemKeySet.CI_MISCONFIG_ON_CHECK)) {
                mResult = check_result.RIGHT;
                setValue(R.string.ctsc_enabled);
            } else {
                mResult = check_result.WRONG;
                setValue(R.string.ctsc_enabled);
            }
        } else {
            if (getKey().equals(CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG)) {
                mResult = check_result.RIGHT;
                setValue(R.string.ctsc_disabled);
            } else {
                mResult = check_result.WRONG;
                setValue(R.string.ctsc_disabled);
            }
        }
        return true;
    }

    public check_result getCheckResult() {
        return mResult;
    }

    public boolean onReset() {
        if (getKey().equals(CheckItemKeySet.CI_MISCONFIG_ON_CONFIG)) {
            mConfig |= (1 << index);
            setValue(R.string.ctsc_enabled);
        } else {
            mConfig &= ~(1 << index);
            setValue(R.string.ctsc_disabled);
        }
        mResult = check_result.RIGHT;
        CTSCLog.d(TAG, "Set " + KEY_MISC_CONFIG + " = " + mConfig);
        Settings.Global.putInt(getContext().getContentResolver(), KEY_MISC_CONFIG, mConfig);

        return true;
    }

}

class CheckSN extends CheckItemBase {

    CheckSN(Context c, String key) {
        super(c, key);
        setTitle(R.string.sn_title);
        setNote(R.string.sn_note);
    }

    public boolean onCheck() {

        StringBuilder sResult = new StringBuilder("");
        InputStream inputstream = null;
        BufferedReader bufferedreader = null;

        try {
            Process proc = Runtime.getRuntime().exec("cat /sys/class/android_usb/android0/iSerial");
            inputstream = proc.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            bufferedreader = new BufferedReader(inputstreamreader);

            if (proc.waitFor() == 0) {
                String line;
                while ((line = bufferedreader.readLine()) != null) {
                    CTSCLog.i("SN", "read line " + line);
                    sResult.append(line);
                }
            } else {
                CTSCLog.i("SN", "exit value = " + proc.exitValue() + "|| get:sb-- " + sResult);
            }

        } catch (InterruptedException e) {
            CTSCLog.i("SN", "exe fail " + e.toString() + "|| get:sb-- " + sResult);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
             if (null != bufferedreader) {
                 try {
                     bufferedreader.close();
                 } catch (IOException e) {
                    CTSCLog.w("SN", "close IOException: " + e.getMessage());
                }
            }
        }

        setValue(sResult.toString());
        return true;
    }

    public check_result getCheckResult() {
        return super.getCheckResult();
    }
}

class CheckBattery extends CheckItemBase {

    IntentFilter mIF;
    int mLevel = -1;

    private BroadcastReceiver mIR = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int newlevel = intent.getIntExtra("level", 200);
                if (mLevel != newlevel) {
                    mLevel = newlevel;
                    sendBroadcast();
                } else {
                    CTSCLog.i("checkBattery", "no change, return");
                    return;
                }
                CTSCLog.i("checkBattery", "level = " + mLevel);
                setValue(String.valueOf(mLevel) + "%");
            }
        }

    };

    CheckBattery(Context c, String key) {
        super(c, key);

        mIF = new IntentFilter();
        mIF.addAction(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mIR, mIF);

        setProperty(PROPERTY_AUTO_CHECK);
        setTitle(R.string.battery_title);
        setNote(R.string.battery_note);
    }

    public check_result getCheckResult() {

         if (mLevel < 0) {
            setValue(R.string.ctsc_querying);
            return check_result.UNKNOWN;
         } else if (mLevel < 90) {
             return check_result.WRONG;
         } else {
             return check_result.RIGHT;
         }
    }

}

class CheckBuildType extends CheckItemBase {

    private final String PROPERTY_RO_BUILD_TYPE = "ro.build.type";

    CheckBuildType(Context c, String key) {
        super(c, key);
        setTitle(R.string.buildtype_title);
        setNote(R.string.buildtype_note);
        setProperty(PROPERTY_AUTO_CHECK);
    }

    public boolean onCheck() {
        String type = SystemProperties.get(PROPERTY_RO_BUILD_TYPE);
        setValue(type + " mode");
        mResult = type.equals("user") ? check_result.RIGHT : check_result.WRONG;
        return true;
    }

    public check_result getCheckResult() {
        return mResult;
    }
}

class CheckTargetMode extends CheckItemBase {
    private final String PROPERTY_RO_TARGET_MODE = "ro.mediatek.platform";
    CheckTargetMode(Context c, String key) {
        super(c, key);
        setTitle(R.string.targetmode_title);
        setNote(R.string.targetmode_note);
    }

    public boolean onCheck() {
        setValue(SystemProperties.get(PROPERTY_RO_TARGET_MODE));
        return true;
    }
}

class CheckTargetVersion extends CheckItemBase {

    private final String PROPERTY_RO_BUILD_VERSION = "ro.build.version.release";

    CheckTargetVersion(Context c, String key) {
        super(c, key);
        setTitle(R.string.targetversion_title);
        setNote(R.string.targetversion_note);
    }

    public boolean onCheck() {
        setValue(SystemProperties.get(PROPERTY_RO_BUILD_VERSION));
        return true;
    }
}

class CheckRestore extends CheckItemBase {
    CheckRestore(Context c, String key) {
        super(c, key);
        setTitle(R.string.restore_title);
        setNote(R.string.restore_note);
    }
}

class CheckCalData extends CheckItemBase {

    CheckCalData(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_calibration_data);
        setNote(R.string.note_calibration_data);
        setProperty(PROPERTY_CLEAR);
    }
}

class CheckWlanSSID extends CheckItemBase {

    CheckWlanSSID(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_wlan_ssid);
        setNote(R.string.note_wlan_ssid);
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();

        WifiManager wifi = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration config = wifi.getWifiApConfiguration();
        String ssid = config == null ? null : config.SSID;

        if (ssid == null && !wifi.isWifiEnabled()) {
            setValue(R.string.value_please_open_wlan);
        } else {
            setValue(ssid);
        }

        return result;
    }
}

class CheckWlanMacAddr extends CheckItemBase {

    CheckWlanMacAddr(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_wlan_mac_addr);
        setNote(R.string.note_wlan_mac_addr);
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();

        WifiManager wifi = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();

        if (macAddress == null && !wifi.isWifiEnabled()) {
            setValue(R.string.value_please_open_wlan);
        } else {
            setValue(macAddress);
        }

        return result;
    }
}

class CheckNFC extends CheckItemBase {
    private static final String TAG = "CheckNFC";
    private NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                sendBroadcast();
            }
        }
    };

    CheckNFC(Context c, String key) {
        super(c, key);

        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setTitle(R.string.NFC_title);
        if (getKey().equals(CheckItemKeySet.CI_NFC_ON)) {
            setNote(getContext().getString(R.string.NFC_on_note));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    public check_result getCheckResult() {
        if (mNfcAdapter == null) {
            CTSCLog.d(TAG, "Nfc Adapter is null");
            setValue(R.string.NFC_not_support);
            setProperty(PROPERTY_AUTO_CHECK);
            return check_result.UNKNOWN;
        }

        switch (mNfcAdapter.getAdapterState()) {
        case NfcAdapter.STATE_OFF:
            setValue(R.string.ctsc_disabled);
            if (getKey().equals(CheckItemKeySet.CI_NFC_ON)) {
                mResult = check_result.WRONG;
            }
            break;
        case NfcAdapter.STATE_ON:
            setValue(R.string.ctsc_enabled);
            if (getKey().equals(CheckItemKeySet.CI_NFC_ON)) {
                mResult = check_result.RIGHT;
            }
            break;
        case NfcAdapter.STATE_TURNING_ON:
        case NfcAdapter.STATE_TURNING_OFF:
            setValue(R.string.ctsc_querying);
            mResult = check_result.UNKNOWN;
            break;
        default:
            setValue(R.string.ctsc_error);
            mResult = check_result.UNKNOWN;
            break;
        }

        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (mNfcAdapter != null) {
            mNfcAdapter.enable();
        }
        return true;
    }
}

class CheckNFCDTA extends CheckItemBase {
    private static final String TAG = "CheckNFCDTA";
    private NfcAdapter mNfcAdapter;

    CheckNFCDTA(Context c, String key) {
        super(c, key);
        setTitle(R.string.NFC_DTA_title);
        if (getKey().equals(CheckItemKeySet.CI_NFC_DTA_CONFIG)) {
            setNote(getContext().getString(R.string.NFC_DTA_note));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        setProperty(PROPERTY_CLEAR);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
    }


    public check_result getCheckResult(){
        check_result result = super.getCheckResult();
        if (mNfcAdapter == null) {
            CTSCLog.d(TAG, "Nfc Adapter is null");
            setValue(R.string.NFC_not_support);
        }
        return mResult;
    }

}

class CheckIMEID extends CheckItemBase {
    private static final String TAG = "CheckIMEID";
    CDMAPhone phone = getCDMAPhone();

    CheckIMEID(Context c, String key) {
        super(c, key);
        setTitle(R.string.IMEID_title);
        setNote(R.string.IMEID_note);
        setProperty(PROPERTY_CLEAR);
    }

    public boolean onCheck() {
        setValue(phone.getMeid());
        return true;
    }

     private CDMAPhone getCDMAPhone() {
        Phone phone = null;
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone p : phones) {
            if (p.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                phone = p;
                break;
            }
        }
        CTSCLog.d(TAG, "getCDMAPhone find cdma phone by phone list = " + phone);

        if (phone == null) {
            // Maybe this is the svlte roaming case, try it.
            int slotId = SvlteModeController.getInstance().getCdmaSocketSlotId();
            if (SubscriptionManager.isValidSlotId(slotId)) {
                SvltePhoneProxy phoneProxy = SvlteUtils.getSvltePhoneProxy(slotId);
                if (null != phoneProxy) {
                    phone = phoneProxy.getNLtePhone();
                }
            }
        }
        CTSCLog.d(TAG, "getCDMAPhone find cdma phone by phone = " + phone);

        if (phone != null && (phone.getPhoneType()
                != PhoneConstants.PHONE_TYPE_CDMA)) {
            phone = null;
        }

        CDMAPhone cdmaPhone = null;
        if (phone != null) {
            // we have got the cdma type phone.
            if (phone instanceof PhoneProxy) {
                PhoneProxy cdmaPhoneProxy = (PhoneProxy) phone;
                cdmaPhone = (CDMAPhone) cdmaPhoneProxy.getActivePhone();
            } else if (phone instanceof CDMAPhone) {
                cdmaPhone = (CDMAPhone) phone;
            }
        }
        CTSCLog.d(TAG, "getCDMAPhone = " + cdmaPhone);
        return cdmaPhone;
    }
}

class CheckBTMacAddr extends CheckItemBase {

    CheckBTMacAddr(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_bt_mac_addr);
        setNote(R.string.note_bt_mac_addr);
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && bluetooth.isEnabled()) {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            if (!TextUtils.isEmpty(address)) {
               // Convert the address to lowercase for consistency with the wifi MAC address.
                setValue(address.toLowerCase());
            } else {
                setValue(R.string.bt_open_device);
            }
        } else {
            setValue(R.string.bt_open_device);
        }

        return result;
    }
}

class CheckPhoneMode extends CheckItemBase {
    private static final String TAG = "CheckPhoneMode";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";
    CheckPhoneMode(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_phone_mode);
        setNote(R.string.note_phone_mode);
        setProperty(PROPERTY_CLEAR);
    }
    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from.
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read.
     */
    private String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     *
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        StringBuilder phonemode = new StringBuilder(Build.MODEL + getMsvSuffix());
        setValue(phonemode.toString());
        return result;
    }
}

class CheckHWMode extends CheckItemBase {
    public static final String TAG = "CheckHWMode";
    public static final String HARDWARE_DEFAULT = "V1";
    CheckHWMode(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_HW_mode);
        setNote(R.string.note_HW_mode);
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        StringBuilder hwversion =
            new StringBuilder(CustomProperties.getString(CustomProperties.MODULE_DM,
            "HardwareVersion", HARDWARE_DEFAULT));
        setValue(hwversion.toString());
        return result;
    }
}

class CheckSWMode extends CheckItemBase {
    public static final String SOFTWARE_VERSION_DEFAULT = "MT6735.P0";
    CheckSWMode(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_SW_mode);
        setNote(R.string.note_SW_mode);
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        StringBuilder swversion =
               new StringBuilder(CustomProperties.getString(CustomProperties.MODULE_DM,
               "SoftwareVersion", SOFTWARE_VERSION_DEFAULT));
        setValue(swversion.toString());
        return result;
    }
}

class CheckPSSensor extends CheckItemBase {
    public static final String TAG = "CheckPSSensor";

    CheckPSSensor(Context c, String Key) {
        super(c, Key);
        setTitle(R.string.title_PSSensor);
        setNote(getContext().getString(R.string.note_PSSensor) +
                getContext().getString(R.string.note_PSSensor_note2));
        setProperty(PROPERTY_CLEAR);
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        return result;
    }
}

class CheckATCI extends CheckItemBase {

    CheckATCI(Context c, String key) {
        super(c, key);

        setProperty(PROPERTY_AUTO_CHECK);
        setTitle(R.string.atci_title);
        setNote(R.string.atci_note);
    }

    public boolean onCheck() {
        CTSCLog.i("CheckATCI", "onCheck()");
        boolean hasAtcidProc = false;
        //boolean hasAtciServiceProc = false;
        Process proc = null;
        InputStream inputstream = null;
        InputStreamReader inputstreamreader = null;
        BufferedReader bufferedreader = null;

        try {
            proc = Runtime.getRuntime().exec("ls /system/bin/");
            inputstream = proc.getInputStream();
            inputstreamreader = new InputStreamReader(inputstream);
            bufferedreader = new BufferedReader(inputstreamreader);
            if (proc.waitFor() == 0) {
                String line;
                while ((line = bufferedreader.readLine()) != null) {
                    CTSCLog.i("CheckATCI", "line value: " + line);
                    if (line.contains("atcid")) {
                        CTSCLog.i("CheckATCI", "line contains atcid, line: " + line);
                        hasAtcidProc = true;
                        break;
                    }
                }
            } else {
                CTSCLog.i("CheckATCI", "exit value = " + proc.exitValue());
            }

        } catch (InterruptedException e) {
            CTSCLog.i("CheckATCI", "exe fail " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
             if (null != bufferedreader) {
                 try {
                     bufferedreader.close();
                 } catch (IOException e) {
                    CTSCLog.w("CheckATCI", "close IOException: " + e.getMessage());
                }
            }
        }

        if (hasAtcidProc) {
            setValue(R.string.atci_value_on);
            mResult = check_result.RIGHT;
        } else {
            setValue(R.string.atci_value_off);
            mResult = check_result.WRONG;
        }

        return true;
    }

    public check_result getCheckResult() {
        return mResult;
    }
}

class CheckAtciInEM extends CheckItemBase {
    private static final String TAG = "CheckAtciInEM";
    private CheckATCI mAtcidServiceCheck = null;
    private static final String ATCI_USERMODE = "persist.service.atci.usermode";
    private static final String RADIO_PORT_INDEX = "persist.radio.port_index";
    private static final String RO_BUILD_TYPE = "ro.build.type";
    private static final String ATCI_AUTO_START = "persist.service.atci.autostart";

    CheckAtciInEM(Context c, String key) {
        super(c, key);
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setTitle(R.string.atci_in_EM_title);
        setNote(R.string.atci_in_EM_note);
        mAtcidServiceCheck = new CheckATCI(c, CheckItemKeySet.CI_ATCI);
    }

    public boolean onCheck() {
        CTSCLog.i(TAG, "onCheck()");
        boolean isEnabled = false;
        int value =
          Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.ACM_ENABLED, 0);
        mAtcidServiceCheck.onCheck();
        if (SystemProperties.get(ATCI_USERMODE).equals("1")
                && SystemProperties.get(RADIO_PORT_INDEX).equals("1")
                && 1 == value
                && check_result.RIGHT == mAtcidServiceCheck.getCheckResult()) {
            isEnabled = true;
        }
        if (isEnabled) {
            setValue(R.string.ctsc_enabled);
            mResult = check_result.RIGHT;
        } else {
            setValue(R.string.ctsc_disabled);
            mResult = check_result.WRONG;
        }
        return true;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        SystemProperties.set(ATCI_USERMODE, "1");
        SystemProperties.set(RADIO_PORT_INDEX, "1");
        Settings.Global.putInt(getContext().getContentResolver(), Settings.Global.ACM_ENABLED, 0);
        Settings.Global.putInt(getContext().getContentResolver(), Settings.Global.ACM_ENABLED, 1);

        String type = SystemProperties.get(RO_BUILD_TYPE, "unknown");
        String isAutoStart = SystemProperties.get(ATCI_AUTO_START);
        String optr = SystemProperties.get("ro.operator.optr");
        String isCT6m = SystemProperties.get("ro.ct6m_support");

        CTSCLog.v(TAG, "build type: " + type + ", optr: " + optr + ", isCT6m: " + isCT6m);
        if (!type.equals("eng")) {
            try {
                if ((optr != null && "OP09".equals(optr)) ||
                        (isCT6m != null && "1".equals(isCT6m))) {
                    if (isAutoStart.equals("1") == true) {
                        CTSCLog.v(TAG, "atuo start enabled.");
                        return true;
                    } else {
                        SystemProperties.set(ATCI_AUTO_START, "1");
                    }
                }
                CTSCLog.v(TAG, "start atcid-daemon-u");
                Process proc = Runtime.getRuntime().exec("start atcid-daemon-u");
                 proc = Runtime.getRuntime().exec("start atci_service");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent("com.mediatek.atci.service.startup");
        getContext().sendBroadcast(intent);
        return true;
    }
}

 class CheckScreenOn extends CheckItemBase {
     private static final String TAG = " CheckScreenOn";
     private Context cc;

     CheckScreenOn(Context c, String key) {
        super(c, key);
        cc = c;
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        if (key.equals(CheckItemKeySet.CI_SCREEN_ON_UNLOCK)) {
            setTitle(R.string.title_lockscreen);
            setNote(R.string.note_lockscreen);
        }
     }

     public boolean onCheck() {
         CTSCLog.d(TAG, " oncheck");
         if (getKey().equals(CheckItemKeySet.CI_SCREEN_ON_UNLOCK)) {
             int isOn =
               Settings.Global.getInt(cc.getContentResolver(),
                                      Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0);
             CTSCLog.d(TAG, "the lock screen is " + isOn);

             if (isOn <= 0) {
                 setValue(R.string.value_lockscreen);
                 mResult = check_result.WRONG;
             } else {
                 setValue(R.string.value_unlockscreen);
                 mResult = check_result.RIGHT;
             }
         }
         return true;
     }

     public check_result getCheckResult() {
         return mResult;
     }

     public boolean onReset() {
         CTSCLog.i(TAG, "onReset");
         if (!isConfigurable()) {
             return false;
         }
         if (getKey().equals(CheckItemKeySet.CI_SCREEN_ON_UNLOCK)) {
             setValue(R.string.value_unlockscreen);
             Settings.Global.putInt(cc.getContentResolver(),
                                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 3);
         }
         mResult = check_result.RIGHT;
         return true;
     }
 }


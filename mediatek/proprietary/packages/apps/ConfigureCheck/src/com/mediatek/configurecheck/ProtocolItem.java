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

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioSystem;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.RadioAccessFamily;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;

import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CheckNetworkMode extends CheckItemBase {
     private static final String TAG = " ProtocolItem CheckNetWorkMode";
     private boolean mAsyncDone = true;
     private boolean mNeedNofity = false;

     private static final int WCDMA_PREFERRED = Phone.NT_MODE_WCDMA_PREF; //(0)
     private static final int GSM_ONLY = Phone.NT_MODE_GSM_ONLY; //(1)
     private static final int WCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY; //(2)
     private static final int GSM_WCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;//(3)  NETWORK_MODE_CDMA(4)
     private static final int CDMA_NO_EVDO = Phone.NT_MODE_CDMA_NO_EVDO;
     private static final int LTE_ONLY = Phone.NT_MODE_LTE_ONLY;
     private static final int LTE_GSM_WCDMA = Phone.NT_MODE_LTE_GSM_WCDMA; //(9)
     private static final int LTE_GSM_WCDMA_PREFERRED = 31;
     private static final int LTE_WCDMA = Phone.NT_MODE_LTE_WCDMA;
     private static final int TDSCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY; //(2)
     private static final int GSM_TDSCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;//(3)
     private static final int CDMA_ONLY = 0;
     private static final int LTE_CDMA_EVDO_GSM_WCDMA = Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;

     private int mUserSelectMode = -1;
     private int mSubId = 1;

     private final Handler mNetworkQueryHandler = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.d(TAG, "Receive msg from network mode query");
            mAsyncDone = true;
            AsyncResult ar = (AsyncResult) msg.obj;
              if (ar.exception == null) {
                  int type = ((int[]) ar.result)[0];
                  CTSCLog.d(TAG, "Get Preferred Type " + type);
                  switch (type) {
                  case GSM_ONLY: //GSM only
                      setValue("GSM only");
                      mResult = check_result.WRONG;
                      break;
                  case WCDMA_ONLY: //TD-SCDMA only
                      setValue(R.string.value_NM_TD_SCDMA_Only);
                      mResult = check_result.WRONG;
                      break;
                  case GSM_WCDMA_AUTO: //GSM/TD-SCDMA(auto)
                  case WCDMA_PREFERRED:
                      setValue(R.string.value_NM_TD_DUAL_MODE);
                      mResult = check_result.WRONG;
                      break;
                  case LTE_ONLY: //LTE only
                      setValue("LTE only");
                      if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG) ||
                      getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CHECK)) {
                          mResult = check_result.RIGHT;
                      } else {
                          mResult = check_result.WRONG;
                      }
                      break;
                  case LTE_GSM_WCDMA:
                  case LTE_GSM_WCDMA_PREFERRED: //4G/3G/2G(auto)
                  case LTE_CDMA_EVDO_GSM_WCDMA:
                      setValue("4G/3G/2G(auto)");
                      if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto_Check)
                          || getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)) {
                          mResult = check_result.RIGHT;
                      } else {
                          mResult = check_result.WRONG;
                      }
                      break;
                  case LTE_WCDMA: //4G/3G
                      setValue("4G/3G");
                      mResult = check_result.WRONG;
                      break;
                  default:
                      setValue("Invalid Mode");
                      mResult = check_result.WRONG;
                      break;
                  }

              } else {
                 setValue("Query failed");
              }
              if (mNeedNofity) {
                sendBroadcast();
              }
          }
      };

      private final Handler mNetworkSetHandler = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg from network mode set");
            if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                setValue(R.string.value_NM_LTE_Only);
            } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)) {
                setValue(R.string.value_NM_4G3G2GAuto);
            }
            CTSCLog.d(TAG, "update network mode done refresh");
            mResult = check_result.RIGHT;
            sendBroadcast();
          }
      };
    /*
     * set title and note in constructor function
     */
    CheckNetworkMode(Context c, String key) {
        super(c, key);

        setTitle(R.string.title_Network_Mode);
        StringBuilder note = new StringBuilder();

        if (key.equals(CheckItemKeySet.CI_LTE_ONLY_CHECK)) {
            note.append(getContext().getString(R.string.note_NM_Lte_Only));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)) {
            note.append(getContext().getString(R.string.note_NM_Lte_Only));
            setProperty(PROPERTY_AUTO_CONFG | PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto_Check)) {
            note.append(getContext().getString(R.string.note_NM_4g3g2gAuto));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto)) {
            note.append(getContext().getString(R.string.note_NM_4g3g2gAuto));
            setProperty(PROPERTY_AUTO_CONFG | PROPERTY_AUTO_CHECK);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        setNote(note.toString());
    }

    public boolean onCheck() {
        getNetworkMode();
        return true;
    }

    public check_result getCheckResult() {
        if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        mNeedNofity = false;
        CTSCLog.d(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            return false;
        }
        setNetWorkMode();
        return true;
    }

    private int getCapabilitySim() {
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        ITelephony iTelephony =
                ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        CTSCLog.d(TAG, "getSimCount: " + telephonyManager.getSimCount());
        if (iTelephony == null || telephonyManager == null
                || telephonyManager.getSimCount() <= 1) {
            return PhoneConstants.SIM_ID_1;
        }
        ITelephonyEx iTeIEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (iTeIEx != null) {
            try {
                CTSCLog.d(TAG, "get 3G capability: " + iTeIEx.getMainCapabilityPhoneId());
                return iTeIEx.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                CTSCLog.e(TAG, e.getMessage());
            }
        } else {
            return PhoneConstants.SIM_ID_1;
        }
        return PhoneConstants.SIM_ID_1;
    }

    private void getNetworkMode() {
        Phone mPhone = null;
        CTSCLog.i(TAG, "getNetworkMode");
        mAsyncDone = false;
        int mSimType = getCapabilitySim();
        int[] subId = SubscriptionManager.getSubId(mSimType);
        if (subId != null) {
            for (int i = 0; i < subId.length; i++) {
                CTSCLog.i(TAG, "subId[" + i + "]: " + subId[i]);
            }
        }
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CTSCLog.e(TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }

        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(mSimType);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        if (SystemProperties.get("ro.mtk_c2k_support").equals("1") == true) {
            if ((SystemProperties.get("ro.mtk_svlte_support").equals("1") == true ||
                SystemProperties.get("ro.mtk_srlte_support").equals("1") == true)
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if ((SystemProperties.get("ro.evdo_dt_support").equals("1") == true)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }

        mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
    }

    private void setNetWorkMode() {
        Phone mPhone = null;
        Message msg = null;
        int mSimType = getCapabilitySim();
        int[] subId = SubscriptionManager.getSubId(mSimType);
        if (subId != null) {
            for (int i = 0; i < subId.length; i++) {
                CTSCLog.i(TAG, "subId[" + i + "]: " + subId[i]);
            }
        }
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CTSCLog.e(TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }
        CTSCLog.i(TAG, "setNetworkMode");
        setValue("Modifing...");
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(mSimType);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }

        if (SystemProperties.get("ro.mtk_c2k_support").equals("1") == true) {
            if ((SystemProperties.get("ro.mtk_svlte_support").equals("1") == true ||
            SystemProperties.get("ro.mtk_srlte_support").equals("1") == true)
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if ((SystemProperties.get("ro.evdo_dt_support").equals("1") == true)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                mUserSelectMode = LTE_ONLY;
                String[] cmd = new String[] {"AT+ERAT=3", ""};
                mPhone.invokeOemRilRequestStrings(cmd, mNetworkSetHandler.obtainMessage());
                cmd = new String[] {"AT+EMDSTATUS=0,0", ""};
                mPhone.invokeOemRilRequestStrings(cmd, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
                mUserSelectMode = LTE_GSM_WCDMA;
                mPhone.setPreferredNetworkType(LTE_GSM_WCDMA, mNetworkSetHandler.obtainMessage());
            }
        } else {
            if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                mUserSelectMode = LTE_ONLY;
                String[] cmd = new String[] {"AT+ERAT=3", ""};
                mPhone.invokeOemRilRequestStrings(cmd, mNetworkSetHandler.obtainMessage());
                cmd = new String[] {"AT+EMDSTATUS=0,0", ""};
                mPhone.invokeOemRilRequestStrings(cmd, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
                mUserSelectMode = GSM_TDSCDMA_AUTO;
                mPhone.setPreferredNetworkType(GSM_TDSCDMA_AUTO,
                                               mNetworkSetHandler.obtainMessage());
            }
        }
        Settings.Global.putInt(getContext().getContentResolver(),
              Settings.Global.USER_PREFERRED_NETWORK_MODE, mUserSelectMode);
        Settings.Global.putInt(getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId, mUserSelectMode);
    }
}

class CheckCTAFTA extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckCTAFTA";
    private boolean mAsyncSIMDone = false;
    private boolean mAsyncModemDone = false;
    private boolean mNeedNofity = false;

    private static final int MODEM_FTA = 2;
    private static final int MODEM_QUERY = 4;
    private static final int MODEM_QUERY_CDMA = 7;
    private static final int MODEM_CDMA = 8;
    private static final int MODEM_QUERY_CLSC = 9;
    private static final int MODEM_CLSC = 10;

    private static final int EVENT_QUERY_PREFERRED_TYPE_DONE = 1000;
    private static final int EVENT_SET_PREFERRED_TYPE_DONE = 1001;
    private static final int PCH_DATA_PREFER = 0;
    private static final int NETWORK_TYPE = 3;
    private static final int IPO_ENABLE = 1;
    private static final int IPO_DISABLE = 0;
    private static final int FLAG_UNLOCK = 0x200000;
    private static final String PROP_TEST_CARD = "persist.sys.forcttestcard";
    private boolean mModemFlag;
    private boolean mUnlockTestSimEnable = false;
    private boolean mModemModeOK = false;
    private Phone mPhone = null;
    private Phone mCdmaPhone = null;
    private int mCurrentFlag = 0;
    private int mCurrentMode;
    private String mCurrentCdmaMode = null;
    private int mSubId = 1;
    private int mFtaoption = 0;


    private final Handler mModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MODEM_FTA:
                ar = (AsyncResult) msg.obj;
                mAsyncModemDone = true;
                if (ar.exception == null) {
                    CTSCLog.d(TAG, " AT cmd success.");
                    setValue(R.string.value_mode_modify_sucess);
                    mResult = check_result.RIGHT;
                } else {
                    CTSCLog.d(TAG, " AT cmd failed.");
                    setValue(R.string.value_mode_modify_fail);
                    mResult = check_result.WRONG;
                }
                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            case MODEM_QUERY:
                CTSCLog.i(TAG, "recieve msg from query CTAFTA ");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String data[] = (String[]) ar.result;
                    handleQuery(data);
                    if (Utils.isCdma()) {
                        queryCdmaOption();
                        queryUnlockOption();
                    } else {
                        mAsyncModemDone = true;
                    }
                } else {
                    setValue("Query failed");
                    mResult = check_result.UNKNOWN;
                }

                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            case EVENT_QUERY_PREFERRED_TYPE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int type = ((int[]) ar.result)[0];
                    CTSCLog.i(TAG, "Get Preferred Type " + type);
                    if (type == 0) {
                        mModemFlag = true;
                    } else {
                        mModemFlag = false;
                    }
                }
                break;

            case EVENT_SET_PREFERRED_TYPE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    CTSCLog.e(TAG, "Set preferrd type Fail");
                }
                break;
           case MODEM_QUERY_CDMA:
                mAsyncModemDone = true;
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    CTSCLog.i(TAG, "Query success.");
                    String[] data = (String[]) ar.result;
                    handleQueryCdma(data);
                }
                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            case MODEM_CDMA:
                mAsyncModemDone = true;
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    CTSCLog.d(TAG, "AT cmd success.");
                    setValue(R.string.value_mode_modify_sucess);
                    mResult = check_result.RIGHT;
                } else {
                    CTSCLog.d(TAG, " AT cmd failed.");
                    setValue(R.string.value_mode_modify_fail);
                    mResult = check_result.WRONG;
                }
                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            case MODEM_QUERY_CLSC:
                mAsyncSIMDone = true;
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    CTSCLog.i(TAG, "Query success.");
                    String[] data = (String[]) ar.result;
                    handleQueryUnlock(data);
                }
                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            case MODEM_CLSC:
                mAsyncSIMDone = true;
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    CTSCLog.d(TAG, "AT cmd success.");
                    setValue(R.string.value_mode_modify_sucess);
                    mResult = check_result.RIGHT;
                } else {
                    CTSCLog.d(TAG, " AT cmd failed.");
                    setValue(R.string.value_mode_modify_fail);
                    mResult = check_result.WRONG;
                }
                if (mNeedNofity) {
                    sendBroadcast();
                }
                break;
            }
        }
    };

    private void handleQuery(String[] data) {
        if (null != data) {
            CTSCLog.i(TAG, "data length is " + data.length);
        } else {
            CTSCLog.i(TAG, "The returned data is wrong.");
        }
        int i = 0;
        for (String str : data) {
            i++;
        }
        mModemModeOK = false;
        mResult = check_result.WRONG;
        if (data.length > 0 && (data[0] != null) && data[0].length() > 6) {
            String mode = data[0].substring(7, data[0].length());
            if (mode.length() >= 3) {
                String subMode = mode.substring(0, 1);
                String subCtaMode = mode.substring(2, mode.length());
                CTSCLog.d(TAG, "subMode is " + subMode);
                CTSCLog.d(TAG, "subCtaMode is " + subCtaMode);
                mCurrentMode = Integer.parseInt(subMode);
                mCurrentFlag = Integer.parseInt(subCtaMode);
                if ("0".equals(subMode)) {
                    setValue(R.string.value_modem_none);
                } else if ("1".equals(subMode)) {
                    setValue(R.string.value_modem_Integrity_off);
                } else if ("2".equals(subMode)) {
                    setValue(R.string.value_modem_FTA);
                  //array: 0  anite(1) crtug(2) crtuw(3) anritsu(4) ame500(5)
                    int val = Integer.valueOf(subCtaMode).intValue();
                    CTSCLog.i(TAG, "val is " + val);
                    int j = 0;
                    for (; j < 5; j++) {
                        if ((val & (1 << j)) != 0) {
                            if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS) ||
                                 getKey().equals(CheckItemKeySet.CI_CTAFTA_RS))
                             && j == 4) {
                                mModemModeOK = true;
                                break;
                            }
                            if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE) ||
                                 getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE))
                              && j == 0) {
                                mModemModeOK = true;
                                break;
                            }
                        }
                    }
                    if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA) ||
                    getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA)) && j == 5 ) {
                        mModemModeOK = true;
                    }
                } else if ("3".equals(subMode)) {
                    setValue(R.string.value_modem_IOT);
                } else if ("4".equals(subMode)) {
                    setValue(R.string.value_modem_Operator);
                } else if ("5".equals(subMode)) {
                    setValue(R.string.value_modem_Factory);
                }
            } else {
                setValue("Value failed");
                mResult = check_result.UNKNOWN;
            }
        } else {
            setValue("Value failed");
            mResult = check_result.WRONG;
        }
    }

    private void handleQueryUnlock(String[] data) {
        if (null == data || data.length <= 0) {
            CTSCLog.d(TAG, "The returned data is wrong.");
            return;
        }
        if (data[0].length() > "+ECLSC:".length()) {
            String str = data[0].substring("+ECLSC:".length()).trim();
            CTSCLog.i(TAG, "unlock setting is " + str);
            int value = Integer.parseInt(str);
            if (value == 1) {
                mUnlockTestSimEnable = true;
            } else {
                mUnlockTestSimEnable = false;
            }
        } else {
            CTSCLog.i(TAG, "The data returned is not right.");
            mCurrentCdmaMode = "NONE";
        }
    }

    private void setUnlockOption(boolean unlock) {
        mCurrentFlag = unlock ? (mCurrentFlag | FLAG_UNLOCK) : (mCurrentFlag & ~FLAG_UNLOCK);
        mPhone.invokeOemRilRequestStrings(
                new String[] {"AT+EPCT=" + mCurrentMode + "," + mCurrentFlag, ""},
                mModemATHander.obtainMessage(MODEM_CLSC));
        mCdmaPhone.invokeOemRilRequestStrings(
                new String[] {"AT+ECLSC=" + (unlock ? "1" : "0"), ""},
                mModemATHander.obtainMessage(MODEM_CLSC));
        mCdmaPhone.invokeOemRilRequestStrings(new String[] {"AT+RFSSYNC", ""}, null);

        String optr = SystemProperties.get("ro.operator.optr");
        if (unlock && "OP09".equals(optr)) {
            SystemProperties.set(PROP_TEST_CARD, "1");
        }
    }

    private void handleQueryCdma(String[] data) {
        if (null == data) {
            CTSCLog.d(TAG,"The returned data is wrong.");
            return;
        } else {
            CTSCLog.i(TAG, "data length is " + data.length);
            int i = 0;
            for (String str : data) {
                if (str != null) {
                    CTSCLog.i(TAG, "data[" + i + "] is : " + str);
                }
                i++;
            }
        }
        if (data[0].length() > 6) {
            String mode = data[0].substring(6, data[0].length()).trim();
            mode = mode.substring(1, mode.length() - 1);
            CTSCLog.i(TAG, "mode is " + mode);
            mCurrentCdmaMode = mode;
        } else {
            CTSCLog.i(TAG, "The data returned is not right.");
            mCurrentCdmaMode = "NONE";
        }
    }

    private void queryUnlockOption() {
        String[] cmd = new String[2];
        cmd[0] = "AT+ECLSC?";
        cmd[1] = "+ECLSC:";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(MODEM_QUERY_CLSC));
    }

    private void queryCdmaOption() {
        String[] cmd = new String[2];
        cmd[0] = "AT+ECTM?";
        cmd[1] = "+ECTM:";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(MODEM_QUERY_CDMA));
    }

    private void sendATCommandCdma(String str, int message) {
        String[] cmd = new String[2];
        cmd[0] = "AT+ECTM=" + str;
        cmd[1] = "";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(message));

        cmd[0] = "AT+RFSSYNC";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, null);
    }

    private void sendATCommad(String str, int flag, int message) {
        String[] cmd = new String[2];
        cmd[0] = "AT+EPCT=" + str + "," + ((mCurrentFlag & 0xFF0000) | flag);
        cmd[1] = "";
        mPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(message));
    }

    private void enableIPO(boolean value) {
        CTSCLog.i(TAG, value ? "enableIOP(true)" : "enableIPO(false)");
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.IPO_SETTING, value ? IPO_ENABLE : IPO_DISABLE);
    }

    private void setGprsTransferType(int type) {
        String property = (type == PCH_DATA_PREFER ? "1" : "0");
        CTSCLog.i(TAG, "Change persist.radio.gprs.prefer to " + property);
        SystemProperties.set("persist.radio.gprs.prefer", property);
        for (int i = 0 ; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            phone.invokeOemRilRequestStrings(new String[] {"AT+EGTP=" + type, ""}, null);
            phone.invokeOemRilRequestStrings(new String[] {"AT+EMPPCH=" + type, ""}, null);
        }
    }

    private void setCdmaOption() {
        String[] cmd = new String[2];
        cmd[0] = "AT+ECTM=" + "\"SPIRENT\"";
        cmd[1] = "";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(MODEM_CDMA));

        cmd[0] = "AT+RFSSYNC";
        mCdmaPhone.invokeOemRilRequestStrings(cmd, null);
    }

    private void getCTAFTA() {
        String cmd[] = new String[2];
        cmd[0] = "AT+EPCT?";
        cmd[1] = "+EPCT:";
        CTSCLog.i(TAG, "getCTAFTA");
        mAsyncSIMDone = false;
        mAsyncModemDone = false;
        mPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(MODEM_QUERY));
    }

    private void checkNetworkType() {
        CTSCLog.i(TAG, "checkNetworkType");
        mPhone.getPreferredNetworkType(mModemATHander
                .obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
    }

    private void writePreferred(int type) {
        SharedPreferences sh = getContext().getSharedPreferences("RATMode",
                Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sh.edit();
        editor.putInt("ModeType", type);
        editor.commit();
    }
    CheckCTAFTA(Context c, String key) {
        super(c, key);

        setTitle(R.string.title_CTA_FTA);
        if (key.equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SIM)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_unlock));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_unlock) + "\n"
                   + getContext().getString(R.string.note_CTA_FTA_SPIRENT));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_SPIRENT)){
            setNote(getContext().getString(R.string.note_CTA_FTA_SPIRENT));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_unlock) + "\n"
                   + getContext().getString(R.string.note_CTA_FTA_FTA));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_unlock) + "\n"
                   + getContext().getString(R.string.note_CTA_FTA_FTA_RS));
        }else if (key.equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_unlock) + "\n"
                   + getContext().getString(R.string.note_CTA_FTA_FTA_ANITE));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_FTA)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_FTA));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_RS)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_FTA_RS));
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_ANITE)) {
            setNote(getContext().getString(R.string.note_CTA_FTA_FTA_ANITE));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        int[] subId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_1);
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CTSCLog.e(TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        mPhone = PhoneFactory.getDefaultPhone();
         if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }
        if (SystemProperties.get("ro.mtk_c2k_support").equals("1") == true) {
            if ((SystemProperties.get("ro.mtk_svlte_support").equals("1") == true ||
            SystemProperties.get("ro.mtk_srlte_support").equals("1") == true)
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if ((SystemProperties.get("ro.evdo_dt_support").equals("1") == true)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }
        if (Utils.isCdma()) {
            int cdmaSlotId = SvlteModeController.getInstance().getCdmaSocketSlotId();
            SvltePhoneProxy proxy = SvlteUtils.getSvltePhoneProxy(cdmaSlotId);
            if (null != proxy) {
                mCdmaPhone = proxy.getNLtePhone();
            }
        }
    }

    public boolean onCheck() {
        mModemModeOK = false;
        checkNetworkType();
        getCTAFTA();
        return true;
    }

    public check_result getCheckResult() {
        CTSCLog.d(TAG, "sycmodem value = " + mAsyncModemDone + " simsync value" + mAsyncSIMDone);
        if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_SPIRENT) ||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA) ||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_RS) ||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE))&& mAsyncModemDone == false) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        if ((!(getKey().equals(CheckItemKeySet.CI_CTAFTA_SPIRENT)||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA)||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_RS)||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE)) &&
             (mAsyncModemDone == false || mAsyncSIMDone == false))) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        mNeedNofity = false;

        if (getKey().equals(CheckItemKeySet.CI_CTAFTA_SPIRENT)) {
            if ("SPIRENT".equals(mCurrentCdmaMode)) {
                setValue("the C2K Mode is SPIRENT");
                mResult = check_result.RIGHT;
            } else {
                setValue("the C2K Mode is " + mCurrentCdmaMode);
                mResult = check_result.WRONG;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SIM)) {
            if (mUnlockTestSimEnable) {
                setValue(R.string.value_SIM_UNLOCK);
                mResult = check_result.RIGHT;
            } else {
                setValue(R.string.value_SIM_UNLOCK_no);
                mResult = check_result.WRONG;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT)) {
            if (mUnlockTestSimEnable && "SPIRENT".equals(mCurrentCdmaMode)) {
                setValue(getContext().getString(R.string.value_SIM_UNLOCK) + "\n" +
                    "the C2K mode is " + mCurrentCdmaMode);
                mResult = check_result.RIGHT;
            } else if (!mUnlockTestSimEnable) {
                setValue(R.string.value_SIM_UNLOCK_no);
                mResult = check_result.WRONG;
            } else {
                setValue("the C2K mode is " + mCurrentCdmaMode);
                mResult = check_result.WRONG;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA) ||
                getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS) ||
                getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE) ||
                getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA) ||
                getKey().equals(CheckItemKeySet.CI_CTAFTA_RS) ||
                getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE)) {
            if (mUnlockTestSimEnable && mModemModeOK) {
                if (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA)) {
                    setValue(getContext().getString(R.string.value_SIM_UNLOCK) + "\n" +
                      "the mode is FTA");
                } else if(getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS)) {
                    setValue(getContext().getString(R.string.value_SIM_UNLOCK) + "\n" +
                      "the mode is FTA: CMW500");
                } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE)) {
                   setValue(getContext().getString(R.string.value_SIM_UNLOCK) + "\n" +
                      "the mode is FTA: ANITI");
                } else if(getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA)) {
                    setValue("the mode is FTA");
                } else if(getKey().equals(CheckItemKeySet.CI_CTAFTA_RS)) {
                    setValue("the mode is FTA: CMW500");
                } else {
                   setValue("the mode is FTA: ANITI");
                }
                mResult = check_result.RIGHT;
            } else if (!mUnlockTestSimEnable && mModemModeOK) {
                if(getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA)) {
                    setValue("the mode is FTA");
                    mResult = check_result.RIGHT;
                } else if(getKey().equals(CheckItemKeySet.CI_CTAFTA_RS)) {
                    setValue("the mode is FTA: CMW500");
                    mResult = check_result.RIGHT;
                } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE)) {
                   setValue("the mode is FTA: ANITI");
                   mResult = check_result.RIGHT;
                } else {
                    setValue(R.string.value_SIM_UNLOCK_no);
                    mResult = check_result.WRONG;
                }
            } else {
                switch(mCurrentMode){
                    case 0:
                        setValue("the mode is none");
                        break;
                    case 1:
                        setValue("the mode is Integrity Off");
                        break;
                    case 2:
                        CTSCLog.i(TAG, "mCurrentFlag is " + mCurrentFlag);
                        int j = 0;
                        for (; j < 5; j++) {
                            if ((mCurrentFlag & (1 << j)) != 0) {
                                switch(j) {
                                    case 0:
                                         setValue("the mode is FTA: ANITE");
                                         break;
                                    case 1:
                                         setValue("the mode is FTA: CRTUG");
                                         break;
                                    case 2:
                                         setValue("the mode is FTA: CRTUW");
                                         break;
                                    case 3:
                                         setValue("the mode is FTA: ANRITSU");
                                         break;
                                    case 4:
                                         setValue("the mode is FTA: CMW500");
                                         break;
                                    default:
                                         setValue("the mode is FTA");
                                         break;
                                }
                                break;
                            }
                        }
                        if (j == 5) {
                            setValue("the mode is FTA");
                        }
                        break;
                    case 3:
                        setValue("the mode is IOT");
                        break;
                    case 4:
                        setValue("the mode is Operator");
                        break;
                    case 5:
                        setValue("the mode is Factory");
                        break;
                    default:
                        setValue("the mode is invalid");
                        break;
                }
                mResult = check_result.WRONG;
            }
        }
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onResult");
        if (!isConfigurable()) {
            return false;
        }
        mAsyncSIMDone = false;
        mAsyncModemDone = false;
        if(!mUnlockTestSimEnable &&
            (getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SIM))) {
            setUnlockOption(true);
        }

        if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT) ||
             getKey().equals(CheckItemKeySet.CI_CTAFTA_SPIRENT)) && mModemModeOK == false) {
            setCdmaOption();
        } else if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_FTA)) && mModemModeOK == false) {
            if (mModemFlag) {
                writePreferred(NETWORK_TYPE);
                Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId, NETWORK_TYPE);
                mPhone.setPreferredNetworkType(NETWORK_TYPE,
                                   mModemATHander.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE));
            }
            sendATCommad("2", 1<<6, MODEM_FTA);
            enableIPO(false);
            setGprsTransferType(PCH_DATA_PREFER);
        } else if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_RS) ||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_RS)) && mModemModeOK == false) {
            if (mModemFlag) {
                writePreferred(NETWORK_TYPE);
                Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId, NETWORK_TYPE);
                mPhone.setPreferredNetworkType(NETWORK_TYPE,
                                   mModemATHander.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE));
            }
            sendATCommad("2", 1<<4, MODEM_FTA);
            enableIPO(false);
            setGprsTransferType(PCH_DATA_PREFER);
        } else if ((getKey().equals(CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE)||
            getKey().equals(CheckItemKeySet.CI_CTAFTA_ANITE)) && mModemModeOK == false) {
            if (mModemFlag) {
                writePreferred(NETWORK_TYPE);
                Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId, NETWORK_TYPE);
                mPhone.setPreferredNetworkType(NETWORK_TYPE,
                                   mModemATHander.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE));
            }
            sendATCommad("2", 1, MODEM_FTA);
            enableIPO(false);
            setGprsTransferType(PCH_DATA_PREFER);
        }
        setRebootFlag(true);
        return true;
    }
}

class CheckBgDataSelect extends CheckItemBase {
    private static final String TAG = "CheckBgDataSelect";
    private static final String BUTTON_FLAG = "flag";
    private static final String SHREDPRE_NAME = "ehrpdBgData";
    private static boolean mEhrpdBgDataEnable = false;
    private INetworkManagementService nwService;

    CheckBgDataSelect(Context c, String Key) {
        super(c, Key);
        setTitle(R.string.BG_DATA_SELECT_title);
        setNote(R.string.BG_DATA_SELECT_note);
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }

    private Context getEMContext() {
        Context eMContext = null;
        try {
            eMContext = getContext().createPackageContext(
                    "com.mediatek.engineermode", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if (null == eMContext) {
            throw new NullPointerException("eMContext=" + eMContext);
        }
        return eMContext;
    }

    private void setDataDisable(boolean isEnable) {
        if (null != nwService) {
            try {
                if (!isEnable) {
                    CTSCLog.d(TAG, "clearIotFirewall");
                    nwService.clearIotFirewall();
                } else {
                    CTSCLog.d(TAG, "setIotFirewall");
                    nwService.setIotFirewall();
                }
            } catch (RemoteException e) {
                CTSCLog.d(TAG, "RomoteException");
            }
        } else {
            CTSCLog.d(TAG, "nwService == null");
        }
    }

    public check_result getCheckResult() {
        nwService = INetworkManagementService.Stub.asInterface(
            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));

        final SharedPreferences autoAnswerSh = getEMContext().getSharedPreferences(SHREDPRE_NAME,
                Context.MODE_WORLD_READABLE);
        mEhrpdBgDataEnable = autoAnswerSh.getBoolean(BUTTON_FLAG, false);
        setDataDisable(mEhrpdBgDataEnable);
        if (!mEhrpdBgDataEnable) {
            setValue("Enable");
            mResult = check_result.WRONG;
        } else {
            setValue("Disable");
            mResult = check_result.RIGHT;
        }
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.d(TAG, "onReset");
        setDataDisable(true);
        final SharedPreferences autoAnswerSh = getEMContext().getSharedPreferences(SHREDPRE_NAME,
                Context.MODE_WORLD_READABLE);
        final SharedPreferences.Editor editor = autoAnswerSh.edit();
        editor.putBoolean(BUTTON_FLAG, true);
        editor.commit();
        return true;
    }
}

class CheckCFU extends CheckItemBase {
    private static final String TAG = " CheckCFU";
    private boolean mAsyncDone = true;
    private Phone mPhone;

    private final Handler mSetModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form CFU set");
            mAsyncDone = true;
             String cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP,
                PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE);
             CTSCLog.i(TAG, "mSetModemATHander cfuSetting = " + cfuSetting);

            if (cfuSetting.equals("0")) {
                setValue(R.string.value_CFU_default);
                mResult = check_result.WRONG;
            } else if (cfuSetting.equals("1")) {
                setValue(R.string.value_CFU_always_not_query);
                mResult = check_result.RIGHT;
            } else if (cfuSetting.equals("2")) {
                setValue(R.string.value_CFU_always_query);
                mResult = check_result.WRONG;
            } else {
                setValue("CFU query failed");
                mResult = check_result.WRONG;
            }
            sendBroadcast();
        }
    };

    CheckCFU(Context c, String key) {
        super(c, key);

        if (key.equals(CheckItemKeySet.CI_CFU_OFF_CONFIG)) {
            setTitle(R.string.title_CFU);
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
            setNote(getContext().getString(R.string.note_CFU_off));
        }
    }

    public boolean onCheck() {
        CTSCLog.i(TAG, "onCheck");
        String cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP,
                PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE);
        CTSCLog.i(TAG, "cfuSetting = " + cfuSetting);

        if (cfuSetting.equals("0")) {
            setValue(R.string.value_CFU_default);
            mResult = check_result.WRONG;
        } else if (cfuSetting.equals("1")) {
            setValue(R.string.value_CFU_always_not_query);
            mResult = check_result.RIGHT;
        } else if (cfuSetting.equals("2")) {
            setValue(R.string.value_CFU_always_query);
            mResult = check_result.WRONG;
        } else {
            setValue("CFU query failed");
            mResult = check_result.WRONG;
        }

        return true;
    }

    public check_result getCheckResult() {
        if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        setCFU();
        return true;
    }

    private void setCFU() {
        mAsyncDone = false;
        String cmdString[] = new String[2];
        cmdString[0] = "AT+ESSP=1";
        cmdString[1] = "";
        CTSCLog.i(TAG, "setCFU");

        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        if (SystemProperties.get("ro.mtk_c2k_support").equals("1")) {
            if ((SystemProperties.get("ro.mtk_svlte_support").equals("1") ||
               SystemProperties.get("ro.mtk_srlte_support").equals("1"))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
                CTSCLog.i(TAG, "setCFU getltephone");
            }
            if (SystemProperties.get("ro.evdo_dt_support").equals("1")
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
                CTSCLog.i(TAG, "setCFU getcdmaphone");
            }
        }
        mPhone.invokeOemRilRequestStrings(cmdString, mSetModemATHander.obtainMessage());
    }
}

class CheckDataConnect extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckDataConnect";
    private boolean mhasSim = true;
    private boolean mReseting = false;

    CheckDataConnect(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_Data_Connection);

        if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
            setNote(getContext().getString(R.string.note_DC_off));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_ON)) {
            setNote(getContext().getString(R.string.note_DC_on));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CHECK)) {
            setNote(getContext().getString(R.string.note_DC_on));
            setProperty(PROPERTY_AUTO_CHECK);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }

        List<SubscriptionInfo> mSimList =
          SubscriptionManager.from(c).getActiveSubscriptionInfoList();
        if (mSimList==null || mSimList.size() == 0) {
            setProperty(PROPERTY_AUTO_CHECK);
            setValue(R.string.value_SIM);
            mResult = check_result.UNKNOWN;
            mhasSim = false;
        }else {
            CTSCLog.i(TAG, "mSimList size : "+mSimList.size());
        }
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, "OnCheck mHasSim = " + mhasSim);
        if (mReseting) {
            setValue(R.string.ctsc_querying);
            return true;
        }
        if (mhasSim) {
            boolean dataEnable = TelephonyManager.getDefault().getDataEnabled();
            if (!dataEnable) {
                setValue(R.string.value_DC_off);
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON) ||
                    getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CHECK)) {
                    mResult = check_result.WRONG;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
                    mResult = check_result.RIGHT;
                }
            } else {
                setValue(R.string.value_DC_on);
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CHECK)) {
                    mResult = check_result.RIGHT;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
                    mResult = check_result.WRONG;
                }
            }
            CTSCLog.i(TAG, "onCheck data enable = " +dataEnable + " mResult = " + mResult);
        }
        return true;
    }


    public check_result getCheckResult() {
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }

    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubId();
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            return false;
        }
        if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
            TelephonyManager.getDefault().setDataEnabled(false);
            setValue(R.string.value_DC_off);
        } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)) {
            TelephonyManager.getDefault().setDataEnabled(true);
            setValue(R.string.value_DC_on);
        }

        mReseting = true;
        new Handler().postDelayed(new Runnable() {
             public void run() {
                CTSCLog.d(TAG, "data connect send set refresh");
                sendBroadcast();
                mReseting = false;
                mResult = check_result.RIGHT;
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
                    setValue(R.string.value_DC_off);
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)) {
                    setValue(R.string.value_DC_on);
                }
           }
        }, 4000);

        return true;
    }
}

class CheckUsbShareNet extends CheckItemBase {
    private static final String TAG = "CheckUsbShareNet";
    private static final String PROP_USB_TETHERING = "persist.service.usbtethering";
    private static final String MODE_NONE = "0";
    private ConnectivityManager mCm = null;
    private boolean mIsWaiting = false;

    CheckUsbShareNet(Context c, String key) {
        super(c, key);
        if (key.equals(CheckItemKeySet.CI_USB_SHARE_NET_AT_Config)) {
            setTitle(R.string.UsbShareNet_title);
            setNote(c.getString(R.string.UsbShareNet_AT_note));
        } else if (key.equals(CheckItemKeySet.CI_USB_SHARE_NET_Config)) {
            setTitle(R.string.UsbShareNet_title);
            setNote(c.getString(R.string.UsbShareNet_note));
            setValue(R.string.value_Usb_set);
            mResult = check_result.WRONG;
        } else if (key.equals(CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config)) {
             setTitle(R.string.ShareNetHOT_title);
             setNote(c.getString(R.string.ShareNetHOT_note));
        }
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        mCm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

    }

    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        if (mIsWaiting) {
            setValue(R.string.ctsc_querying);
            mResult = check_result.UNKNOWN;
        } else {
            if (getKey().equals(CheckItemKeySet.CI_USB_SHARE_NET_AT_Config)){
                String usbenable = SystemProperties.get(PROP_USB_TETHERING, MODE_NONE);
                CTSCLog.i(TAG, "usbenable : " + usbenable);
                if (usbenable.equals("1") == true) {
                    mResult = check_result.RIGHT;
                } else {
                    mResult = check_result.WRONG;
                }
            } else if (getKey().equals(CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config)) {
                if (SystemProperties.get("ro.mtk_tetheringipv6_support").equals("1") == true) {
                    int ipv6Value = Settings.System.getInt(getContext().getContentResolver(),
                                              Settings.System.TETHER_IPV6_FEATURE, 0);
                    CTSCLog.d(TAG, "getTetheringIpv6Enable value = " + ipv6Value);
                    if (ipv6Value == 1) {
                        setValue(R.string.IPV4V6);
                        mResult = check_result.RIGHT;
                    } else {
                        setValue(R.string.IPV4);
                        mResult = check_result.WRONG;
                    }
                } else {
                    setValue(R.string.IPV4V6_no_support);
                    mResult = check_result.WRONG;
                }
            } else {
                String[] tethered = mCm.getTetheredIfaces();
                String[] usbRegexs = mCm.getTetherableUsbRegexs();

                boolean usbTethered = false;
                for (String s : tethered) {
                    for (String regex : usbRegexs) {
                        if (s.matches(regex)) {
                            usbTethered = true;
                        }
                    }
                }
                if (usbTethered) {
                    setValue(R.string.UsbShareNet_yes);
                    mResult = check_result.RIGHT;
                } else {
                    setValue(R.string.UsbShareNet_no);
                    mResult = check_result.WRONG;
                }
            }
        }

        return mResult;
    }

    private void setUsbTethering(boolean enabled) {
        mCm.setUsbTethering(enabled);
        mIsWaiting = true;

        new Handler().postDelayed(new Runnable() {
            public void run() {
                CTSCLog.d(TAG, "wait 1s for setting successfully");
                sendBroadcast();
                mIsWaiting = false;
            }
        }, 6000);
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (getKey().equals(CheckItemKeySet.CI_USB_SHARE_NET_AT_Config)){
            SystemProperties.set(PROP_USB_TETHERING, "1");
            setRebootFlag(true);
            mResult = check_result.RIGHT;
        } else if (getKey().equals(CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config)) {
            if (SystemProperties.get("ro.mtk_tetheringipv6_support").equals("1") == true) {
                Settings.System.putInt(getContext().getContentResolver(),
                            Settings.System.TETHER_IPV6_FEATURE, 1);
                setValue(R.string.IPV4V6);
                mResult = check_result.RIGHT;
            } else {
                setValue(R.string.IPV4V6_no_support);
                mResult = check_result.WRONG;
            }
        } else {
            setUsbTethering(true);
        }
        return true;
    }
}


class CheckBluetoothState extends CheckItemBase {
    private static final String TAG = "CheckBluetoothState";
    private boolean mIsWaiting = false;

    CheckBluetoothState(Context c, String key) {
        super(c, key);
        setTitle(R.string.Bluetooth_title);
        setNote(c.getString(R.string.Bluetooth_off_note));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }

    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        if (mIsWaiting) {
            setValue(R.string.ctsc_querying);
            mResult = check_result.UNKNOWN;
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            switch (adapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    setValue(R.string.ctsc_enabled);
                    mResult = check_result.WRONG;
                    break;
                case BluetoothAdapter.STATE_OFF:
                    setValue(R.string.ctsc_disabled);
                    mResult = check_result.RIGHT;
                    break;
                default:
                    setValue(R.string.ctsc_unknown);
                    mResult = check_result.WRONG;
                    break;
            }
        }
        return mResult;
    }

    private void setBluetoothState(boolean enabled) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (enabled) {
            adapter.enable();
        } else {
            adapter.disable();
        }
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        setBluetoothState(false);
        mIsWaiting = true;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mIsWaiting = false;
                sendBroadcast();
            }
        }, 3000);
        return true;
    }
}


class CheckIRsetting extends CheckItemBase {
    private static final String TAG = "CheckIRsetting";
    private static final String IR_MODE_PROPERTY = "persist.radio.ct.ir.engmode";
    private static final String MODE_NONE = "0";
    private static final String MODE_CDMA_ONLY = "1";
    private static final String MODE_GSM_ONLY = "2";
    private static final String MODE_FTA_ONLY = "3";
    private boolean mIsWaiting = false;

    CheckIRsetting(Context c, String key) {
        super(c, key);
        if (key.equals(CheckItemKeySet.CI_IR_LWG)) {
            setTitle(R.string.ir_setting_title);
            setNote(c.getString(R.string.ir_setting_LWG_note));
        } else if (key.equals(CheckItemKeySet.CI_IR_FTA_LTE)) {
            setTitle(R.string.ir_setting_title);
            setNote(c.getString(R.string.ir_setting_FTA_note));
        } else if (key.equals(CheckItemKeySet.CI_IR_CDMA)) {
            setTitle(R.string.ir_setting_title);
            setNote(c.getString(R.string.ir_setting_CMDA_note));
        } else if (key.equals(CheckItemKeySet.CI_IR_WCDMA_INTEGRITY_CHECK)) {
            setTitle(R.string.WCDMA_Integrity_title);
            setNote(c.getString(R.string.WCDMA_Integrity_note));
        }
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }

    private Context getEMContext() {
        Context eMContext = null;
        try {
            eMContext = getContext().createPackageContext(
                    "com.mediatek.engineermode", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (null == eMContext) {
            throw new NullPointerException("eMContext=" + eMContext);
        }
        return eMContext;
    }

    public check_result getCheckResult() {
        if (getKey().equals(CheckItemKeySet.CI_IR_WCDMA_INTEGRITY_CHECK)) {
            if (mIsWaiting) {
                setValue(R.string.ctsc_querying);
                mResult = check_result.UNKNOWN;
                return mResult;
            }
            SharedPreferences preference =
                getEMContext().getSharedPreferences("pref", Context.MODE_PRIVATE);
            int currentValue = preference.getInt("current_value", 0);
            CTSCLog.d(TAG, "currentValue = " + currentValue);
            if (currentValue == 1) {
                setValue(R.string.WCDMA_Integrity_on_value);
                mResult = check_result.RIGHT;
            } else {
                setValue(R.string.WCDMA_Integrity_off_value);
                mResult = check_result.WRONG;
            }
            return mResult;
        }
        String mode = SystemProperties.get(IR_MODE_PROPERTY, MODE_NONE);
        CTSCLog.i(TAG, "ir mode : " + mode);
        if (mode.equals(MODE_CDMA_ONLY)) {
            setValue("CDMA only");
            if (getKey().equals(CheckItemKeySet.CI_IR_CDMA)) {
                mResult = check_result.RIGHT;
            } else {
                mResult = check_result.WRONG;
            }
        } else if (mode.equals(MODE_GSM_ONLY)) {
            if (SystemProperties.get("ro.mtk_svlte_lcg_support", "0").equals("1")) {  // 4M
                setValue("LTE/GSM Only");
            } else { // 5M
                setValue("LTE/WCDMA/GSM Only");
            }
            if (getKey().equals(CheckItemKeySet.CI_IR_LWG)) {
                mResult = check_result.RIGHT;
            } else {
                mResult = check_result.WRONG;
            }
        } else if (mode.equals(MODE_FTA_ONLY)) {
            setValue("FTA LTE only");
            if (getKey().equals(CheckItemKeySet.CI_IR_LWG)) {
                mResult = check_result.WRONG;
            } else {
                mResult = check_result.RIGHT;
            }
        } else {
            setValue("None");
            mResult = check_result.WRONG;
        }
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (getKey().equals(CheckItemKeySet.CI_IR_WCDMA_INTEGRITY_CHECK)) {
            Phone mPhone = null;
            int cdmaSlotId = SvlteModeController.getInstance().getCdmaSocketSlotId();
            SvltePhoneProxy proxy = (SvltePhoneProxy) PhoneFactory.getPhone(cdmaSlotId);
            if (proxy == null) {
                CTSCLog.i("@M_" + TAG, "proxy is null " );
                setValue(R.string.WCDMA_Integrity_off_value);
                return true;
            } else {
                mPhone = proxy.getLtePhone();
                mIsWaiting = true;
                mPhone.invokeOemRilRequestStrings(new String[] {"AT+EFAKECFG=1", ""}, null);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        mIsWaiting = false;
                        SharedPreferences preference =
                            getEMContext().getSharedPreferences("pref", Context.MODE_PRIVATE);
                        preference.edit().putInt("current_value", 1).commit();
                        setValue(R.string.WCDMA_Integrity_on_value);
                        mResult = check_result.RIGHT;
                        sendBroadcast();
                    }
                }, 3000);
            }
            return true;
        }

        if (getKey().equals(CheckItemKeySet.CI_IR_LWG)) {
            SystemProperties.set(IR_MODE_PROPERTY, MODE_GSM_ONLY);
        } else if (getKey().equals(CheckItemKeySet.CI_IR_FTA_LTE)) {
            SystemProperties.set(IR_MODE_PROPERTY, MODE_FTA_ONLY);
        } else if (getKey().equals(CheckItemKeySet.CI_IR_CDMA)) {
            SystemProperties.set(IR_MODE_PROPERTY, MODE_CDMA_ONLY);
        }
        return true;
    }
}

class CheckDormainTimer extends CheckItemBase {
    private static final String TAG = "CheckDormainTimer";

    CheckDormainTimer(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_Dormant_Timer);
        setNote(c.getString(R.string.note_Dormant_Timer));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setValue(R.string.value_DoTimer_set);
        mResult = check_result.UNKNOWN;
    }

    public check_result getCheckResult() {
        check_result result = super.getCheckResult();
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        String[] cmd = new String[2];
        cmd[0] = "AT+CTA=10";
        cmd[1] = "";
        Phone mPhone = PhoneFactory.getDefaultPhone();
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }

        if (SystemProperties.get("ro.mtk_c2k_support").equals("1")) {
            if ((SystemProperties.get("ro.mtk_svlte_support").equals("1") ||
               SystemProperties.get("ro.mtk_srlte_support").equals("1"))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
                CTSCLog.i(TAG, "setCFU getltephone");
            }
            if (SystemProperties.get("ro.evdo_dt_support").equals("1")
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
                CTSCLog.i(TAG, "setCFU getcdmaphone");
            }
        }
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }
        mPhone.invokeOemRilRequestStrings(cmd, null);
        setValue(R.string.value_Dormant_timer);
        mResult = check_result.RIGHT;
        return true;
    }
}

class CheckUser2Root extends CheckItemBase {
    private static final String TAG = "CheckUser2Root";
    private static final String ANDROID_BUILD_VERSION = "ro.build.version.sdk";
    private static final int ANDROID_BUILD_ICS = 14;
    private static final String PREF_FILE = "development";
    private static final String PREF_SHOW = "show";
    private static final String RO_BUILD_TYPE = "ro.build.type";

    private static final String ENABLE_ADB = "enable_adb";

    CheckUser2Root(Context c, String key) {
        super(c, key);

        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setTitle(R.string.user2root_title);
        if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
            setNote(getContext().getString(R.string.user2root_note));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, "oncheck");

        int debugvalue =
            Settings.Global.getInt(getContext().getContentResolver(),
                 Settings.Global.ADB_ENABLED, 0);

        if (debugvalue == 1) {
            setValue(R.string.root);
            if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
                mResult = check_result.RIGHT;
            }
        } else {
            setValue(R.string.user);
            if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
                mResult = check_result.WRONG;
            }
        }
        return true;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        getContext().getSharedPreferences(PREF_FILE,
                     Context.MODE_PRIVATE).edit().putBoolean(
                     PREF_SHOW, true).apply();

        Settings.Global.putInt(getContext().getContentResolver(),
                        Settings.Global.ADB_ENABLED, 1);
        String type = SystemProperties.get(RO_BUILD_TYPE, "unknown");
        CTSCLog.v(TAG, "build type: " + type);
        if (!type.equals("eng")) {
            try {
                CTSCLog.v(TAG, "user2root start atcid-daemon-u");
                Process proc = Runtime.getRuntime().exec("start atcid-daemon-u");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}

class CheckTimeandTimeZone extends CheckItemBase {
    private static final String TAG = "CheckTimeandTimeZone";
    private Context cc;

    CheckTimeandTimeZone(Context c, String key) {
        super(c, key);
        cc = c;

        if (key.equals(CheckItemKeySet.CI_TIME_OFF_CONFIG)) {
            setTitle(R.string.Time_AutoSync_title);
            setNote(R.string.Time_AutoSync_note);
        } else if (key.equals(CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG)) {
            setTitle(R.string.TimeZone_AutoSync_title);
            setNote(R.string.TimeZone_AutoSync_note);
        }
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }

    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        mResult = check_result.WRONG;
        if (getKey().equals(CheckItemKeySet.CI_TIME_OFF_CONFIG)) {
            try {
                boolean autoTimeEnabled =
                    Settings.Global.getInt(getContext().getContentResolver(),
                                       Settings.Global.AUTO_TIME) > 0;
                boolean  autoTimeGpsEnabled =
                    Settings.Global.getInt(getContext().getContentResolver(),
                                       Settings.System.AUTO_TIME_GPS) > 0;

                if (autoTimeEnabled) {
                    setValue(R.string.Time_AutoSync_net_value);
                } else if (autoTimeGpsEnabled) {
                    setValue(R.string.Time_AutoSync_gps_value);
                } else {
                    setValue(R.string.Time_AutoSync_off_value);
                    mResult = check_result.RIGHT;
                }
            } catch (SettingNotFoundException snfe) {
               CTSCLog.d(TAG, "get value fail");
            }
        } else if (getKey().equals(CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG)) {
            try {
                boolean autoTimeZoneEnabled =
                    Settings.Global.getInt(getContext().getContentResolver(),
                                       Settings.Global.AUTO_TIME_ZONE) > 0;
                if (autoTimeZoneEnabled) {
                    setValue(R.string.TimeZone_AutoSync_on_value);
                } else {
                    setValue(R.string.TimeZone_AutoSync_off_value);
                    mResult = check_result.RIGHT;
                }
            } catch (SettingNotFoundException snfe) {
               CTSCLog.d(TAG, "get value fail2");
            }
        }
        return mResult;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (getKey().equals(CheckItemKeySet.CI_TIME_OFF_CONFIG)) {
            Settings.Global.putInt(
                     getContext().getContentResolver(), Settings.Global.AUTO_TIME, 0);
            Settings.Global.putInt(
                     getContext().getContentResolver(), Settings.System.AUTO_TIME_GPS, 0);
            setValue(R.string.Time_AutoSync_off_value);
        } else if (getKey().equals(CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG)) {
            Settings.Global.putInt(
                    getContext().getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
            setValue(R.string.TimeZone_AutoSync_off_value);
        }
         mResult = check_result.RIGHT;
         return true;
     }
 }

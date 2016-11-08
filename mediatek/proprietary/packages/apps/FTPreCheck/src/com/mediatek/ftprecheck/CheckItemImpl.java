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

package com.mediatek.ftprecheck;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;


/**
 * Set of all check type which instruct the behavior of check item
 * Rule: the string must be the same with Condition.mCheckType
 */
final class CheckTypeSet {

    public static final String CI_VALUE_BETWEEN = "value_between";
    public static final String CI_VALUE_GREATER = "value_greater";
    public static final String CI_VALUE_EQUAL = "value_equal";
    public static final String CI_ENABLE = "enable";
    public static final String CI_DISABLE = "disable";
    public static final String CI_MORE_THAN_20 = "more than 20%";
    public static final String CI_CAN_NOT_MUCH = "can not much";
    public static final String CI_4G_ONLY = "4G only";

    public static final String CI_BAND_F = "F";
    public static final String CI_BAND_D = "D";
    public static final String CI_BAND_E = "E";

    public static final String CI_4G_HAS_3G_NEIGHBOR = "4g_has_3g_neighbor";
    public static final String CI_4G_HAS_2G_NEIGHBOR = "4g_has_2g_neighbor";
    public static final String CI_4G_HAS_NOT_3G_NEIGHBOR = "4g_has_not_3g_neighbor";

    public static final String CI_3G_HAS_4G_NEIGHBOR = "3g_has_4g_neighbor";
    public static final String CI_3G_HAS_2G_NEIGHBOR = "3g_has_2g_neighbor";
    public static final String CI_3G_HAS_NOT_4G_NEIGHBOR = "3g_has_not_4g_neighbor";

    public static final String CI_2G_HAS_4G_NEIGHBOR = "2g_has_4g_neighbor";
    public static final String CI_2G_HAS_3G_NEIGHBOR = "2g_has_3g_neighbor";
    public static final String CI_2G_HAS_NOT_4G_NEIGHBOR = "2g_has_not_4g_neighbor";

    public static final String CI_3G_HAS_4G_MEASURE = "3g_has_4g_measure";

    public static final String CI_4G_HAS_NOT_2G_MEASURE = "4g_has_not_2g_measure";
    public static final String CI_4G_HAS_2G_MEASURE = "4g_has_2g_measure";
    public static final String CI_4G_HAS_NOT_3G_MEASURE = "4g_has_not_3g_measure";
    public static final String CI_4G_HAS_3G_MEASURE = "4g_has_3g_measure";

    public static final String CI_4G2_R8_REDIRECTION = "4g2_r8_redirection";
    public static final String CI_4G2_R9_REDIRECTION = "4g2_r9_redirection";
    public static final String CI_4G3_R8_REDIRECTION = "4g3_r8_redirection";
    public static final String CI_4G_NO_REDIRECTION_3G = "4g_not_redirection_3g";
    public static final String CI_4G_NO_REDIRECTION_2G = "4g_not_redirection_2g";
    public static final String CI_CSFB_REDIRECTION_2G = "csfb_redirection_2g";

    public static final String CI_3G4_R8_REDIRECTION = "3g4_r8_redirection";
    public static final String CI_3G_NO_REDIRECTION_4G = "3g_not_redirection_4g";

    public static final String CI_4G2_IRCR = "4g2_ircr";
    public static final String CI_2G4_IRCR = "2g4_ircr";
    public static final String CI_4G3_IRCR = "4g3_ircr";
    public static final String CI_3G4_IRCR = "3g4_ircr";
    public static final String CI_4G3 = "4g3";
    public static final String CI_3G4 = "3g4";
    public static final String CI_4G2_IR_SEARCH = "4g2_ir_search";
    public static final String CI_4G3_IR_SEARCH = "4g3_ir_search";
    public static final String CI_3G4_IR_SEARCH = "3g4_ir_search";

    public static final String CI_3G2 = "3g2";
    public static final String CI_2G3 = "2g3";

    public static final String CI_LA_CHANGED = "changed";

    public static final String CI_PS_4G = "4G";
    public static final String CI_PS_3G = "3G";
    public static final String CI_PS_2G = "2G";
    public static final String CI_PS_NO_REGISTER = "none";
}

class CheckSINR extends EnwinfoItemBase {

    private static final String TAG = "CheckSINR";

    private static final int EL1TX_EM_TX_INFO = 249;
    private String mCondValue;
    private String mCondVauleLeft;
    private String mCondVauleRight;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private static final UrcField[] FRONT_FIELDS = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrp", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rssi", 2),
            new UrcField(UrcParser.TYPE_INT16, "snr", 2),
            new UrcField(UrcParser.TYPE_INT16, "rsrp", 1),
            new UrcField(UrcParser.TYPE_INT16, "rsrq", 1)
            };
    //add for 91
    private static final UrcField[] FRONT_FIELDS2 = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1), //data 0
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_cc_idx", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),//data 1
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_cc_idx", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),

            new UrcField(UrcParser.TYPE_INT16, "dl_rssi", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrp", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrq", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_sinr", 2),
            new UrcField(UrcParser.TYPE_INT16, "rsrp", 1),
            new UrcField(UrcParser.TYPE_INT16, "rsrq", 1)
            };

    public CheckSINR(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
        if (getCheckType().equals(CheckTypeSet.CI_VALUE_BETWEEN)) {
            //this check type, we do not care mCondValue
            mCondVauleLeft = (String) conditionValues[1];
            mCondVauleRight = (String) conditionValues[2];
        } else if (getCheckType().equals(CheckTypeSet.CI_VALUE_GREATER)) {
            //this check type, we do not care mCondVauleLeft & mCondVauleRight
            mCondValue = (String) conditionValues[0];
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }

        FTPCLog.d(TAG, "hash code: " + this.hashCode() + " ");
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EL1TX_EM_TX_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos;
        //use world mode to disturb 90 and 91
        if (SystemProperties.getInt("ro.mtk_md_world_mode_support", 0) == 1) {
            startPos = UrcParser.calculateOffset(FRONT_FIELDS2);
        } else {
            startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        }
        FTPCLog.d(TAG, "os_snr offset = " + startPos);
        int os_snr = UrcParser.getValueFrom2Byte(data, startPos, true);
        mValueList.add(os_snr);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        FTPCLog.d(TAG, "mValueList of " + this.hashCode() + " : " + mValueList);

        Integer sum = 0;
        int satisfiedCount = 0;
        for (Integer value : mValueList) {
            sum += value;
            if (getCheckType().equals(CheckTypeSet.CI_VALUE_BETWEEN)) {
                if (value >= Integer.valueOf(mCondVauleLeft)
                        && value <= Integer.valueOf(mCondVauleRight)) {
                    satisfiedCount++;
                }
            } else if (getCheckType().equals(CheckTypeSet.CI_VALUE_GREATER)) {
                if (value >= Integer.valueOf(mCondValue)) {
                    satisfiedCount++;
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }

//        Double value = ((double)sum) / mValueList.size();
//        setValue(String.format("%.2f", value));

        Double ratio = ((double) satisfiedCount) / mValueList.size();
        setValue(String.format(getContext().getString(R.string.value_stable), ratio * 100));
        if (ratio > 0.8) {
            setCheckResult(CheckResult.SATISFIED);
        } else {
            setCheckResult(CheckResult.UNSATISFIED);
        }
    }
}

class ChecRSRP extends EnwinfoItemBase {

    private static final String TAG = "ChecRSRP";

    private static final int EL1TX_EM_TX_INFO = 249;
    private String mCondValue;
    private String mCondVauleLeft;
    private String mCondVauleRight;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private static final UrcField[] FRONT_FIELDS = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrp", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rssi", 2),
            new UrcField(UrcParser.TYPE_INT16, "snr", 2)
            };
 //add for 91
    private static final UrcField[] FRONT_FIELDS2 = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1), //data 0
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_cc_idx", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),//data 1
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "sp_cfg", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tm", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_cc_idx", 1),
            new UrcField(UrcParser.TYPE_INT16, "pci", 1),
            new UrcField(UrcParser.TYPE_UINT16, "earfcn", 1),
            new UrcField(UrcParser.TYPE_UINT16, "dlFreq", 1),
            new UrcField(UrcParser.TYPE_UINT16, "ulFreq", 1),

            new UrcField(UrcParser.TYPE_INT16, "dl_rssi", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrp", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_rsrq", 2),
            new UrcField(UrcParser.TYPE_INT16, "dl_sinr", 2)
            };

    public ChecRSRP(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
        if (getCheckType().equals(CheckTypeSet.CI_VALUE_BETWEEN)) {
            mCondVauleLeft = (String) conditionValues[1];
            mCondVauleRight = (String) conditionValues[2];
        } else if (getCheckType().equals(CheckTypeSet.CI_VALUE_GREATER)) {
            mCondValue = (String) conditionValues[0];
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }

        FTPCLog.d(TAG, "hash code: " + this.hashCode() + " ");
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EL1TX_EM_TX_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos;
        if (SystemProperties.getInt("ro.mtk_md_world_mode_support", 0) == 1) {
            startPos = UrcParser.calculateOffset(FRONT_FIELDS2);
        } else {
            startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        }
        FTPCLog.d(TAG, "rsrp offset = " + startPos);
        int rsrp = UrcParser.getValueFrom2Byte(data, startPos, true);
        mValueList.add(rsrp);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        FTPCLog.d(TAG, "mValueList of " + this.hashCode() + " : " + mValueList);

        Integer sum = 0;
        int satisfiedCount = 0;
        for (Integer value : mValueList) {
            sum += value;
            if (getCheckType().equals(CheckTypeSet.CI_VALUE_BETWEEN)) {
                if (value >= Integer.valueOf(mCondVauleLeft)
                        && value <= Integer.valueOf(mCondVauleRight)) {
                    satisfiedCount++;
                }
            } else if (getCheckType().equals(CheckTypeSet.CI_VALUE_GREATER)) {
                if (value >= Integer.valueOf(mCondValue)) {
                    satisfiedCount++;
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }

//        Double value = ((double)sum) / mValueList.size();
//        setValue(String.format("%.2f", value));

        Double ratio = ((double) satisfiedCount) / mValueList.size();
        setValue(String.format(getContext().getString(R.string.value_stable), ratio * 100));
        if (ratio > 0.9) {
            setCheckResult(CheckResult.SATISFIED);
        } else {
            setCheckResult(CheckResult.UNSATISFIED);
        }
    }
}

class CheckSubFrame extends EnwinfoItemBase {

    private static final String TAG = "CheckSubFrame";

    private static final int EL1TX_EM_TX_INFO = 249;
    private int mCondVauleLeft;
    private int mCondVauleRight;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private static final UrcField[] FRONT_FIELDS = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1)
            };

    public CheckSubFrame(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
        if (getCheckType().equals(CheckTypeSet.CI_VALUE_EQUAL)) {
            mCondVauleLeft = Integer.valueOf((String) conditionValues[1]);
            mCondVauleRight = Integer.valueOf((String) conditionValues[2]);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EL1TX_EM_TX_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        FTPCLog.d(TAG, "tdd_cfg offset = " + startPos);
        int tdd_cfg = UrcParser.getValueFromByte(data, startPos, false);
        mValueList.add(tdd_cfg);
    }

    public void onValueAndResultSet() {
        boolean isset = false;
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        setCheckResult(CheckResult.UNSATISFIED);
        for (Integer lastValue : mValueList) {
            switch (lastValue) {
            case 0:// DL:UL=1:3
                setValue("1:3");
                if (1 == mCondVauleLeft && 3 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 1:// DL:UL=1:1
                setValue("1:1");
                if (1 == mCondVauleLeft && 1 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 2:// DL:UL=3:1
                setValue("3:1");
                if (3 == mCondVauleLeft && 1 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 3:// DL:UL=2:1
                setValue("2:1");
                if (2 == mCondVauleLeft && 1 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 4:// DL:UL=7:2
                setValue("7:2");
                if (7 == mCondVauleLeft && 2 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 5:// DL:UL=8:1
                setValue("8:1");
                if (8 == mCondVauleLeft && 1 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 6:// DL:UL=3:5
                setValue("3:5");
                if (3 == mCondVauleLeft && 5 == mCondVauleRight) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            default:
                FTPCLog.d(TAG, "no this tdd_cfg value: " + lastValue);
                break;
            }
            if (isset) {
                return ;
            }
        }
    }
}

class CheckSSP extends EnwinfoItemBase {

    private static final String TAG = "CheckSSP";

    private static final int EL1TX_EM_TX_INFO = 249;
    private int mCondValue;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private static final UrcField[] FRONT_FIELDS = {
            new UrcField(UrcParser.TYPE_UINT8, "band", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ant_port", 1),
            new UrcField(UrcParser.TYPE_UINT8, "dl_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "ul_bw", 1),
            new UrcField(UrcParser.TYPE_UINT8, "tdd_cfg", 1)
            };

    public CheckSSP(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
        if (getCheckType().equals(CheckTypeSet.CI_VALUE_EQUAL)) {
            mCondValue = Integer.valueOf((String) conditionValues[0]);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EL1TX_EM_TX_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        FTPCLog.d(TAG, "sp_cfg offset = " + startPos);
        int sp_cfg = UrcParser.getValueFromByte(data, startPos, false);
        mValueList.add(sp_cfg);
    }

    public void onValueAndResultSet() {
        boolean isset = false;
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        setCheckResult(CheckResult.UNSATISFIED);
        for (Integer lastValue : mValueList) {
            switch (lastValue) {
            case 0:
                setValue("SSP0");
                if (0 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 1:
                setValue("SSP1");
                if (1 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 2:
                setValue("SSP2");
                if (2 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 3:
                setValue("SSP3");
                if (3 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 4:
                setValue("SSP4");
                if (4 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 5:
                setValue("SSP5");
                if (5 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 6:
                setValue("SSP6");
                if (6 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 7:
                setValue("SSP7");
                if (7 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 8:
                setValue("SSP8");
                if (8 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 9:
                setValue("SSP9");
                if (9 == mCondValue) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            default:
                FTPCLog.d(TAG, "no this sp_cfg value: " + lastValue);
                break;
            }
            if (isset) {
                return ;
            }
        }
    }
}

class CheckFreqBand extends EnwinfoItemBase {

    private static final String TAG = "CheckFreqBand";

    private static final int EL1TX_EM_TX_INFO = 249;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private static final UrcField[] FRONT_FIELDS = {};

    public CheckFreqBand(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EL1TX_EM_TX_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        int band = UrcParser.getValueFromByte(data, startPos, false);
        mValueList.add(band);
    }

    public void onValueAndResultSet() {
        boolean isset = false;
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        setCheckResult(CheckResult.UNSATISFIED);
        for (Integer lastValue : mValueList) {
            switch (lastValue) {
            case 38:
            case 41:
                setValue("D");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_BAND_D)) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 39:
                setValue("F");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_BAND_F)) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            case 40:
                setValue("E");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_BAND_E)) {
                    setCheckResult(CheckResult.SATISFIED);
                    isset = true;
                }
                break;
            default:
                setValue("");
                FTPCLog.d(TAG, "no this band value: " + lastValue);
                break;
            }
            if (isset) {
                return ;
            }
        }
    }
}

class Check4gNeighborCell extends EnwinfoItemBase {

    private static final String TAG = "Check4gNeighborCell";

    private static final int ERRC_EM_SERV_IR_NEIGHBOR_INFO = 327;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();

    private UrcField[] FRONT_FIELDS = {}; //can not be static

    public Check4gNeighborCell(Context c, String checkType) {
        super(c, checkType, TAG);
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_3G_NEIGHBOR)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_3G_NEIGHBOR)) {
            //no front fields
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_NEIGHBOR)) {
            FRONT_FIELDS = new UrcField[1];
            FRONT_FIELDS[0] = new UrcField(UrcParser.TYPE_INT8, "with_3g_neighbor", 1);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(ERRC_EM_SERV_IR_NEIGHBOR_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_3G_NEIGHBOR)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_3G_NEIGHBOR)) {
            FTPCLog.d(TAG, "with_3g_neighbor offset = " + startPos);
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_NEIGHBOR)) {
            FTPCLog.d(TAG, "with_2g_neighbor offset = " + startPos);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
        int value = UrcParser.getValueFromByte(data, startPos, true);
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        for (Integer lastValue : mValueList) {
            FTPCLog.d(TAG, "lastValue = " + lastValue);
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_3G_NEIGHBOR)
                    || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_NEIGHBOR)) {
                if (0 == lastValue) {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                } else {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_3G_NEIGHBOR)) {
                if (0 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }
    }
}

class Check3gNeighborCell extends EnwinfoItemBase {

    private static final String TAG = "Check3gNeighborCell";

    private static final int EM_MEME_INFO_GSM_CELL_INFO = 98;
    private static final int EM_MEME_INFO_LTE_CELL_INFO = 99;
// for 91
    //private static final int EM_MEME_INFO_GSM_CELL_INFO = 97;
    //private static final int EM_MEME_INFO_LTE_CELL_INFO = 98;

    private ArrayList<Integer> mValueList = new ArrayList<Integer>();
    private static UrcField[] FRONT_FIELDS = {};

    public Check3gNeighborCell(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_4G_NEIGHBOR)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_NOT_4G_NEIGHBOR)) {
            if (SystemProperties.getInt("ro.mtk_md_world_mode_support", 0) == 1) {
                setUrcType(EM_MEME_INFO_LTE_CELL_INFO - 1);
            } else {
                setUrcType(EM_MEME_INFO_LTE_CELL_INFO);
            }
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_2G_NEIGHBOR)) {
            if (SystemProperties.getInt("ro.mtk_md_world_mode_support", 0) == 1) {
                setUrcType(EM_MEME_INFO_GSM_CELL_INFO - 1);
            } else {
                setUrcType(EM_MEME_INFO_GSM_CELL_INFO);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        int value = UrcParser.getValueFromByte(data, startPos, true);
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        for (Integer lastValue : mValueList) {
            FTPCLog.d(TAG, "lastValue = " + lastValue);
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_4G_NEIGHBOR)) {
                if (1 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_2G_NEIGHBOR)) {
                if (1 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_NOT_4G_NEIGHBOR)) {
                if (0 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }
    }
}

class Check2gNeighborCell extends EnwinfoItemBase {

    private static final String TAG = "Check2gNeighborCell";

    private static final int RRM_EM_IR_3G_NEIGHBOR_CELL_STATUS_IND_STRUCT_INFO = 21;
    private static final int RRM_EM_IR_4G_NEIGHBOR_CELL_STATUS_IND_STRUCT_INFO = 22;

    private ArrayList<Integer> mValueList = new ArrayList<Integer>();
    private static UrcField[] FRONT_FIELDS = {};

    public Check2gNeighborCell(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_4G_NEIGHBOR)
                 || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_NOT_4G_NEIGHBOR)) {
            setUrcType(RRM_EM_IR_4G_NEIGHBOR_CELL_STATUS_IND_STRUCT_INFO);
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_3G_NEIGHBOR)) {
            setUrcType(RRM_EM_IR_3G_NEIGHBOR_CELL_STATUS_IND_STRUCT_INFO);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        int value = UrcParser.getValueFromByte(data, startPos, true);
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        for (Integer lastValue : mValueList) {
            FTPCLog.d(TAG, "lastValue = " + lastValue);
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_4G_NEIGHBOR)
                    || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_3G_NEIGHBOR)) {
                if (0 == lastValue) {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                } else {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G_HAS_NOT_4G_NEIGHBOR)) {
                if (0 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }
    }
}

class Check4gMeasureCell extends EnwinfoItemBase {

    private static final String TAG = "Check4gMeasureCell";
    private static final int ERRC_EM_IRAT_MEAS_CFG = 329;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();
    private UrcField[] FRONT_FIELDS = {}; //can not be static

    public Check4gMeasureCell(Context c, String checkType) {
        super(c, checkType, TAG);
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_3G_MEASURE)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_3G_MEASURE)) {
            //no front fields
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_2G_MEASURE)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_MEASURE)) {
            FRONT_FIELDS = new UrcField[1];
            FRONT_FIELDS[0] = new UrcField(UrcParser.TYPE_INT8, "utran_meas_cfg", 1);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(ERRC_EM_IRAT_MEAS_CFG);
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_2G_MEASURE)
                || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_MEASURE)) {
            FTPCLog.d(TAG, "geran_meas_cfg offset = " + startPos);
        }
        int value = UrcParser.getValueFromByte(data, startPos, false);
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        for (Integer lastValue : mValueList) {
            FTPCLog.d(TAG, "lastValue = " + lastValue);
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_3G_MEASURE)
                    || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_2G_MEASURE)) {
                if (0 == lastValue) {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                } else {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_3G_MEASURE)
                    || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_HAS_NOT_2G_MEASURE)) {
                if (0 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }
    }
}

class Check3gMeasureCell extends EnwinfoItemBase {

    private static final String TAG = "Check3gMeasureCell";
    private static final int EM_MEME_INFO_LTE_CELL_INFO = 99;
    private ArrayList<Integer> mValueList = new ArrayList<Integer>();
    private static UrcField[] FRONT_FIELDS = {
        new UrcField(UrcParser.TYPE_INT8, "with_4g_neighbor", 1)
        };

    public Check3gMeasureCell(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_4G_MEASURE)) {
            setUrcType(EM_MEME_INFO_LTE_CELL_INFO);
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    @Override
    public void onUrcDataParse(String data) {
        int startPos = UrcParser.calculateOffset(FRONT_FIELDS);
        int value = UrcParser.getValueFromByte(data, startPos, true);
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        //Integer lastValue = mValueList.get(mValueList.size() - 1);
        for (Integer lastValue : mValueList) {
            FTPCLog.d(TAG, "lastValue = " + lastValue);
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_HAS_4G_MEASURE)) {
                if (1 == lastValue) {
                    setValue(R.string.value_yes);
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else {
                    setValue(R.string.value_no);
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }
        }
    }
}

class Check4gRedirection extends EnwinfoItemBase {

    private static final String TAG = "Check4gRedirection";
    private static final int ERRC_EM_IR_REDIR_EVENT = 328;


    private class RedirectData_4G {
        int redir_event;
        int is_csfb_ongoing;
    }

    private ArrayList<RedirectData_4G> mValueList = new ArrayList<RedirectData_4G>();

    public Check4gRedirection(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(ERRC_EM_IR_REDIR_EVENT);
    }

    @Override
    public void onUrcDataParse(String data) {
        final RedirectData_4G listData = new RedirectData_4G();
        listData.redir_event = UrcParser.getValueFromByte(data, 0, false);
        listData.is_csfb_ongoing = UrcParser.getValueFromByte(data, 2, true);
        mValueList.add(listData);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_NO_REDIRECTION_3G)
                    || getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_NO_REDIRECTION_2G)) {
                setValue(R.string.value_yes);
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setValue("no redirection");
                setCheckResult(CheckResult.UNSATISFIED);
            }
            return;
        }

        setCheckResult(CheckResult.UNSATISFIED);
        for (RedirectData_4G mLastData : mValueList) {
            FTPCLog.d(TAG, "redir_event = " + mLastData.redir_event);
            FTPCLog.d(TAG, "is_csfb_ongoing = " + mLastData.is_csfb_ongoing);

            switch (mLastData.redir_event) {
            case 0://EM_ERRC_EVENT_REDIR_FROM_LTE_TO_GSM_R8
                setValue("4G2 R8");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G2_R8_REDIRECTION)) {
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_CSFB_REDIRECTION_2G)) {
                    if (0 != mLastData.is_csfb_ongoing) {
                        setValue(R.string.value_yes);
                        setCheckResult(CheckResult.SATISFIED);
                        return ;
                    }
                }
                break;
            case 1://EM_ERRC_EVENT_REDIR_FROM_LTE_TO_UMTS_R8
            case 3://EM_ERRC_EVENT_REDIT_FROM_LTE_TO_UMTS_R9_LATER
                setValue("4G3 R8");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G3_R8_REDIRECTION)) {
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
                break;
            case 2://EM_ERRC_EVENT_REDIR_FROM_LTE_TO_GSM_R9_LATER
                setValue("4G2 R9");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G2_R9_REDIRECTION)) {
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_CSFB_REDIRECTION_2G)) {
                    if (0 != mLastData.is_csfb_ongoing) {
                        setValue(R.string.value_yes);
                        setCheckResult(CheckResult.SATISFIED);
                        return ;
                    }
                }
                break;
            default:
                setValue("");
                FTPCLog.d(TAG, "no this redir_event value: " + mLastData.redir_event);
                break;
            }
        }
    }
}

class Check3gRedirection extends EnwinfoItemBase {

    private static final String TAG = "Check3gRedirection";
    private static final int EM_RRCE_3G4_REDIR_EVENT = 131;

    public Check3gRedirection(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EM_RRCE_3G4_REDIR_EVENT);
    }

    @Override
    public void onUrcDataParse(String data) {
        //do nothing
    }

    public void onValueAndResultSet() {
        FTPCLog.d(TAG, "isUrcArrived() = " + isUrcArrived());
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G4_R8_REDIRECTION)) {
            if (isUrcArrived()) {
                setValue("3G4 R8");
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setValue(R.string.value_no);
                setCheckResult(CheckResult.UNSATISFIED);
            }
        } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G_NO_REDIRECTION_4G)) {
            if (isUrcArrived()) {
                setValue("3G4 R8");
                setCheckResult(CheckResult.UNSATISFIED);
            } else {
                setValue(R.string.value_yes);
                setCheckResult(CheckResult.SATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}

class CheckLA extends EnwinfoItemBase {

    private static final String TAG = "CheckLA";
    private static final int EMM_L4C_LAI_CHANGE_INFO = 253;

    public CheckLA(Context c, String checkType) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EMM_L4C_LAI_CHANGE_INFO);
    }

    @Override
    public void onUrcDataParse(String data) {
        //do nothing
    }

    public void onValueAndResultSet() {
        FTPCLog.d(TAG, "isUrcArrived() = " + isUrcArrived());
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_LA_CHANGED)) {
            if (isUrcArrived()) {
                setValue(R.string.value_yes);
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setValue(R.string.value_no);
                setCheckResult(CheckResult.UNSATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}

class CheckLteRatChange extends EnwinfoItemBase {

    private static final String TAG = "CheckLteRatChange";
    private static final int EM_L4C_RAT_CHANGE_IND = 275;
    //private String mLastData = null;
    private ArrayList<String> mValueList = new ArrayList<String>();

    public CheckLteRatChange(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(EM_L4C_RAT_CHANGE_IND);
    }

    @Override
    public void onUrcDataParse(String data) {
        //mLastData = data;
        String value = new String();
        value = data;
        mValueList.add(data);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }
        for (String mLastData : mValueList) {
            int irat_type = UrcParser.getValueFromByte(mLastData, 0, false);
            int source_rat = UrcParser.getValueFromByte(mLastData, 2, false);
            int target_rat = UrcParser.getValueFromByte(mLastData, 4, false);
            FTPCLog.d(TAG, "irat_type = " + irat_type);
            FTPCLog.d(TAG, "source_rat = " + source_rat);
            FTPCLog.d(TAG, "target_rat = " + target_rat);

            boolean isSatisfied = false;
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G3)) {
                if ((3 == source_rat && 2 == target_rat && irat_type != 6)
                        || (3 == source_rat && 6 == irat_type)) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G4)) {
                if ((2 == source_rat && 3 == target_rat && irat_type != 6)
                        || (3 == target_rat && 6 == irat_type)) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G2_IR_SEARCH)) {
                if (3 == source_rat && 6 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G3_IR_SEARCH)) {
                if (3 == source_rat && 6 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G4_IR_SEARCH)) {
                if (3 == target_rat && 6 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G2_IRCR)) {
                if (3 == source_rat && 0 == target_rat && 5 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G4_IRCR)) {
                if (0 == source_rat && 3 == target_rat && 5 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G3_IRCR)) {
                if (3 == source_rat && 2 == target_rat && 5 == irat_type) {
                    isSatisfied = true;
                }
            } else if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G4_IRCR)) {
                if (2 == source_rat && 3 == target_rat && 5 == irat_type) {
                    isSatisfied = true;
                }
            } else {
                throw new IllegalArgumentException("No this check type: " + getCheckType());
            }

            if (isSatisfied) {
                setCheckResult(CheckResult.SATISFIED);
                setValue(R.string.value_yes);
                return ;
            } else {
                setCheckResult(CheckResult.UNSATISFIED);
                setValue(R.string.value_no);
            }
        }
    }
}

class CheckRatChange extends EnwinfoItemBase {

    private static final String TAG = "CheckRatChange";
    private static final int RATCM_EM_23G_RAT_CHANGE_IND = 254;
    //private String mLastData = null;
    private ArrayList<String> mValueList = new ArrayList<String>();
    public CheckRatChange(Context c, String checkType, Object... conditionValues) {
        super(c, checkType, TAG);
    }

    @Override
    public void onUrcTypeConfig() {
        setUrcType(RATCM_EM_23G_RAT_CHANGE_IND);
    }

    @Override
    public void onUrcDataParse(String data) {
        String value = new String();
        value = data;
        mValueList.add(value);
    }

    public void onValueAndResultSet() {
        if (mValueList.isEmpty()) {
            setValue("");
            setCheckResult(CheckResult.UNSATISFIED);
            return;
        }

        for (String mLastData : mValueList) {
            int source_rat = UrcParser.getValueFromByte(mLastData, 0, false);
            int target_rat = UrcParser.getValueFromByte(mLastData, 2, false);
            FTPCLog.d(TAG, "source_rat = " + source_rat);
            FTPCLog.d(TAG, "target_rat = " + target_rat);

            setCheckResult(CheckResult.UNSATISFIED);
            if (1 == source_rat && 0 == target_rat) {
                setValue("3G2");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_3G2)) {
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
            } else if (0 == source_rat && 1 == target_rat) {
                setValue("2G3");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_2G3)) {
                    setCheckResult(CheckResult.SATISFIED);
                    return ;
                }
            } else {
                FTPCLog.e(TAG, "unknown rat change");
                setValue("");
            }
        }
    }
}

class CheckRegiNetwork extends CheckItemBase {

    private int mRegState = -1;
    private int mNwType = TelephonyManager.NETWORK_TYPE_UNKNOWN;

    public CheckRegiNetwork(Context c, String checkType, Object... conditionValues) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        //only need to check at the end of Timer
        //so, do nothing here
    }

    private void getRegiNetwork() {
        Phone phone = null;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            phone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            phone = PhoneFactory.getDefaultPhone();
        }
        mRegState = phone.getServiceState().getDataRegState();
        mNwType = phone.getServiceState().getDataNetworkType();
    }

    @Override
    public void onStopCheck() {
        getRegiNetwork();

        setCheckResult(CheckResult.UNSATISFIED);
        if (ServiceState.STATE_OUT_OF_SERVICE == mRegState
                || ServiceState.STATE_POWER_OFF == mRegState) {
            setValue("PS no register");
            if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_PS_NO_REGISTER)) {
                setCheckResult(CheckResult.SATISFIED);
            }
        } else {
            switch(mNwType) {
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                setValue("unknown");
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                setValue("PS 2G");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_PS_2G)) {
                    setCheckResult(CheckResult.SATISFIED);
                }
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                setValue("PS 3G");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_PS_3G)) {
                    setCheckResult(CheckResult.SATISFIED);
                }
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                setValue("PS 4G");
                if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_PS_4G)) {
                    setCheckResult(CheckResult.SATISFIED);
                }
                break;
            default:
                setValue("");
                FTPCLog.d("CheckRegiNetwork", "no this network type: " + mNwType);
                break;
            }
        }
    }
}

class CheckIPO extends CheckItemBase {

    private boolean mIpoEnabled = false;

    public CheckIPO(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        if (!SystemProperties.get("ro.mtk_ipo_support").equals("1")
                || UserHandle.myUserId() != UserHandle.USER_OWNER) {
            FTPCLog.d("CheckIPO", "Not support IPO!");
        } else {
            mIpoEnabled = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.IPO_SETTING, 1) == 1;
            FTPCLog.d("CheckIPO", "Feature option is exists and query result is " + mIpoEnabled);
        }
    }

    @Override
    public void onStopCheck() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_ENABLE)) {
            if (mIpoEnabled) {
                setValue(R.string.value_enable);
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setValue(R.string.value_disable);
                setCheckResult(CheckResult.UNSATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}


class CheckBattery extends CheckItemBase {

    IntentFilter mIF;
    int mLevel = -1;

    public CheckBattery(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        mIF = new IntentFilter();
        mIF.addAction(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mIR, mIF);
    }

    @Override
    public void onStopCheck() {
        getContext().unregisterReceiver(mIR);
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_MORE_THAN_20)) {
            if (mLevel > 0 && mLevel <= 100) {
                setValue(String.valueOf(mLevel) + "%");
                if (mLevel >= 20) {
                    setCheckResult(CheckResult.SATISFIED);
                } else {
                    setCheckResult(CheckResult.UNSATISFIED);
                }
            } else {
                setCheckResult(CheckResult.UNKNOWN);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }

    private BroadcastReceiver mIR = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int newlevel = intent.getIntExtra("level", 200);
                FTPCLog.i("checkBattery", "level = " + mLevel);
                if (mLevel != newlevel) {
                    mLevel = newlevel;
                }
            }
        }
    };
}

class CheckScreenUnlock extends CheckItemBase {

    private boolean mIslock;

    public CheckScreenUnlock(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        int duration = Settings.Global.getInt(getContext().getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0);
        FTPCLog.d("CheckScrUnlock", "the lock screen is " + duration);
        mIslock = (duration <= 0);
    }

    @Override
    public void onStopCheck() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_ENABLE)) {
            if (mIslock) {
                setValue(R.string.value_disable);
                setCheckResult(CheckResult.UNSATISFIED);
            } else {
                setValue(R.string.value_enable);
                setCheckResult(CheckResult.SATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}


class CheckHangupByPower extends CheckItemBase {

    private boolean mHangupByPower = false;

    public CheckHangupByPower(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !isVoiceCapable(getContext())) {
            FTPCLog.d("CheckIPO", "Not support hang up by power key!");
        } else {
            final int incallPowerBehavior = Settings.Secure.getInt(getContext().getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            mHangupByPower = (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            FTPCLog.d("CheckHangupByPower", "mHangupByPower =  " + incallPowerBehavior);
        }
    }

    private boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    @Override
    public void onStopCheck() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_ENABLE)) {
            if (mHangupByPower) {
                setValue(R.string.value_enable);
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setValue(R.string.value_disable);
                setCheckResult(CheckResult.UNSATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}

class CheckWifiAp extends CheckItemBase {

    private static final String TAG = "CheckWifiAp";

    public CheckWifiAp(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        // only need to check at the end of Timer.
        // so, do nothing here.
    }

    @Override
    public void onStopCheck() {
        if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_CAN_NOT_MUCH)) {
            WifiManager wifiManager = (WifiManager) getContext()
                    .getSystemService(Context.WIFI_SERVICE);
            HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
            // -1 represents get all channels number.
            boolean isSuitable = wifiManager.isSuitableForTest(-1, result);
            FTPCLog.i(TAG, "isSuitable: " + isSuitable
                    + ", wifi channel info: " + result.toString());
            ConditionManager.getConditionManager(getContext()).setWifiChanneInfo(result);
            setValue(R.string.value_ref_report);
            if (isSuitable) {
                setCheckResult(CheckResult.SATISFIED);
            } else {
                setCheckResult(CheckResult.UNSATISFIED);
            }
        } else {
            throw new IllegalArgumentException("No this check type: " + getCheckType());
        }
    }
}

/**
 * Only support CSFB load.
 * Reference: EM->Telephony->Network Selecting.
 */
class CheckRatMode extends CheckItemBase {
    private static final String TAG = "CheckRatMode";
    private boolean mAsyncDone = true;

    private static final int WCDMA_PREFERRED = Phone.NT_MODE_WCDMA_PREF;
    private static final int GSM_ONLY = Phone.NT_MODE_GSM_ONLY;
    private static final int WCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY;
    private static final int GSM_WCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;
    private static final int LTE_ONLY = Phone.NT_MODE_LTE_ONLY;
    private static final int LTE_GSM_WCDMA = Phone.NT_MODE_LTE_GSM_WCDMA;
//    private static final int LTE_GSM_WCDMA_PREFERRED = RILConstants.NETWORK_MODE_LTE_GSM_WCDMA_PREF;
    private static final int LTE_WCDMA = Phone.NT_MODE_LTE_WCDMA;
//    private static final int LTE_GSM = Phone.NT_MODE_LTE_GSM;

    public CheckRatMode(Context c, String checkType) {
        super(c, checkType);
    }

    @Override
    public void onCheck() {
        getNetworkMode();
    }

    private void getNetworkMode() {
        FTPCLog.i(TAG, "getNetworkMode");
        Phone mPhone = null;
        mAsyncDone = false;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
            mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
            mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
        }
    }

    private final Handler mNetworkQueryHandler = new Handler() {
        public final void handleMessage(Message msg) {
            FTPCLog.d(TAG, "Receive msg from network mode query");
            setCheckResult(CheckResult.UNSATISFIED);
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int type = ((int[]) ar.result)[0];
                FTPCLog.d(TAG, "Get Preferred Type " + type);
                switch (type) {
                case GSM_ONLY: // GSM only
                    setValue("GSM only");
                    break;
                case WCDMA_ONLY: // TD-SCDMA only
                    setValue("TD-SCDMA only");
                    break;
                case GSM_WCDMA_AUTO: // GSM/TD-SCDMA(auto)
                case WCDMA_PREFERRED:
                    setValue("GSM/TD-SCDMA(auto)");
                    break;
                case LTE_ONLY: // LTE only
                    setValue("LTE only");
                    if (getCheckType().equalsIgnoreCase(CheckTypeSet.CI_4G_ONLY)) {
                        setCheckResult(CheckResult.SATISFIED);
                    }
                    break;
                case LTE_GSM_WCDMA:
                //case LTE_GSM_WCDMA_PREFERRED: // 4G/3G/2G(auto)
                    setValue("4G/3G/2G(auto)");
                    break;
                case LTE_WCDMA: // 4G/3G
                    setValue("4G/3G");
                    break;
//                case LTE_GSM: // 4G/2G
//                    setValue("4G/2G");
//                    break;
                default:
                    break;
                }
            } else {
                setValue("Query failed");
            }
            mAsyncDone = true;
        }
    };

    @Override
    public void onStopCheck() {
        if (!mAsyncDone) {
            setValue("querying");
            setCheckResult(CheckResult.UNKNOWN);
        }
    }
}

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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import android.telephony.TelephonyManager;

//APN
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import java.io.IOException;
import java.util.*;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
// SUPL
import com.mediatek.lbs.em2.utils.AgpsInterface;
import com.mediatek.lbs.em2.utils.AgpsConfig;
import com.mediatek.lbs.em2.utils.SuplProfile;
//import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

// BT
import android.bluetooth.BluetoothAdapter;

// MMS Roaming
import android.content.ComponentName;
import android.content.Intent;

class CheckLabAPN extends CheckItemBase {
    private List<SubscriptionInfo> mSimInfoList = null;
    private Set<Integer> mSimNeedSetAPN = new HashSet<Integer>();
    private final static String TAG = "CheckLabAPN";

    CheckLabAPN(Context c, String key) {
        super(c, key);

        setTitle(R.string.apn_check_title);

        StringBuilder noteStr = new StringBuilder();
        if (key.equals(CheckItemKeySet.CI_APN_CTLTE)) {
            noteStr.append(getContext().getString(R.string.set_apn) + "\n")
                   .append(getContext().getString(R.string.apn_apn_lab) + "\n")
                   .append("ctlte" + "\n")
                   .append(getContext().getString(R.string.apn_protocol) + "\n")
                   .append("IPV4V6");
        } else if (key.equals(CheckItemKeySet.CI_APN_RSAPN)) {
            noteStr.append(getContext().getString(R.string.set_apn) + "\n")
                   .append(getContext().getString(R.string.apn_name) + "\n")
                   .append("rsapn" + "\n")
                   .append(getContext().getString(R.string.apn_apn_lab) + "\n")
                   .append("rsapn");
        } else if (key.equals(CheckItemKeySet.CI_APN_CTNET)) {
            noteStr.append(getContext().getString(R.string.set_apn) + "\n")
                   .append(getContext().getString(R.string.apn_name) + "\n")
                   .append("ctnet" + "\n")
                   .append(getContext().getString(R.string.apn_apn_lab) + "\n")
                   .append("ctnet");
        }

        setNote(noteStr.toString());
        mSimInfoList = SubscriptionManager.from(c).getActiveSubscriptionInfoList();

        if (mSimInfoList == null || mSimInfoList.isEmpty()) {
            setProperty(PROPERTY_AUTO_CHECK);
        } else {
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        }
    }

    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubId();
    }

    public check_result getCheckResult() {
        CTSCLog.v(TAG, "getCheckResult()");
        if (!isCheckable()) {
            return super.getCheckResult();
        }

        if (mSimInfoList == null || mSimInfoList.isEmpty()) {
            setValue(R.string.string_sim);
            return check_result.UNKNOWN;
        }

        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            StringBuilder valueStr = new StringBuilder();
            check_result result = check_result.RIGHT;

            for (SubscriptionInfo simInfo : mSimInfoList) {
                int simNo = simInfo.getSimSlotIndex() + 1;
                int subid = getSubIdBySlot(simInfo.getSimSlotIndex());
                Cursor cursor_APN = getContext().getContentResolver().query(
                       Uri.parse("content://telephony/carriers/preferapn" + "/subId/" + subid),
                                            null, null, null, null);

                if (cursor_APN == null || !cursor_APN.moveToNext()) {
                    if (cursor_APN != null) {
                        cursor_APN.close();
                    }
                    valueStr.append("Sim" + simNo);
                    valueStr.append(getContext().getString(R.string.apn_not_setted));
                    result = check_result.WRONG;
                    mSimNeedSetAPN.add(new Integer(simInfo.getSimSlotIndex()));
                } else {
                    String curAPN =
                      cursor_APN.getString(cursor_APN.getColumnIndex(Telephony.Carriers.APN));
                    String curAPNType =
                      cursor_APN.getString(cursor_APN.getColumnIndex(Telephony.Carriers.TYPE));
                    String curAPNProtocol =
                      cursor_APN.getString(cursor_APN.getColumnIndex(Telephony.Carriers.PROTOCOL));
                    String curAPNRoamPro =
                      cursor_APN.getString(
                      cursor_APN.getColumnIndex(Telephony.Carriers.ROAMING_PROTOCOL));

                    cursor_APN.close();
                    CTSCLog.v(TAG, "curAPN = " + curAPN);

                    if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                        if (null==curAPN || !curAPN.equalsIgnoreCase("rsapn") ||
                        curAPNRoamPro == null || !curAPNRoamPro.equalsIgnoreCase("IPV4V6") ||
                        curAPNProtocol == null || !curAPNProtocol.equalsIgnoreCase("IPV4V6")) {
                            result = check_result.WRONG;
                            mSimNeedSetAPN.add(new Integer(simInfo.getSimSlotIndex()));
                            if(valueStr.toString().length() != 0) {
                                valueStr.append("\n");
                            }
                            valueStr.append("Sim" + simNo);
                            valueStr.append(getContext().getString(R.string.apn_not_correct));
                        }
                        continue;
                    }
                    if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE)) {
                        if (null==curAPNProtocol || !curAPNProtocol.equalsIgnoreCase("IPV4V6") ||
                        null==curAPN || !curAPN.equalsIgnoreCase("ctlte")) {
                            result = check_result.WRONG;
                            mSimNeedSetAPN.add(new Integer(simInfo.getSimSlotIndex()));
                            if(valueStr.toString().length() != 0) {
                                valueStr.append("\n");
                            }
                            valueStr.append("Sim" + simNo);
                            valueStr.append(getContext().getString(R.string.apn_not_correct));
                        }
                        continue;
                    }

                    if (getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                        if (null==curAPNProtocol || !curAPNProtocol.equalsIgnoreCase("IPV4V6") ||
                        null==curAPN || !curAPN.equalsIgnoreCase("ctnet")) {
                            result = check_result.WRONG;
                            mSimNeedSetAPN.add(new Integer(simInfo.getSimSlotIndex()));
                            if(valueStr.toString().length() != 0) {
                                valueStr.append("\n");
                            }
                            valueStr.append("Sim" + simNo);
                            valueStr.append(getContext().getString(R.string.apn_not_correct));
                        }
                        continue;
                    }
                }
            }

            setValue(valueStr.toString());
            return result;
        } else {
            Cursor cursor_APN = getContext().getContentResolver().query(
                                            Uri.parse("content://telephony/carriers/preferapn"),
                                            null, null, null, null);
            if (cursor_APN == null || !cursor_APN.moveToNext()) {
                if (cursor_APN != null) {
                    cursor_APN.close();
                }
                setValue(R.string.apn_not_setted);
                return check_result.WRONG;
            } else {
                String curAPN =
                  cursor_APN.getString(cursor_APN.getColumnIndex(Telephony.Carriers.APN));
                String curAPNProtocol =
                  cursor_APN.getString(cursor_APN.getColumnIndex(Telephony.Carriers.PROTOCOL));
                String curAPNRoamPro =
                  cursor_APN.getString(
                  cursor_APN.getColumnIndex(Telephony.Carriers.ROAMING_PROTOCOL));

                cursor_APN.close();
                CTSCLog.v(TAG, "curAPN = " + curAPN);
                if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                    if (null==curAPN || !curAPN.equalsIgnoreCase("rsapn") ||
                        curAPNRoamPro == null || !curAPNRoamPro.equalsIgnoreCase("IPV4V6") ||
                        curAPNProtocol == null || !curAPNProtocol.equalsIgnoreCase("IPV4V6")) {
                        setValue(R.string.apn_not_correct);
                        return check_result.WRONG;
                    } else {
                        setValue("");
                        return check_result.RIGHT;
                    }
                }

                if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE)) {
                    if (null==curAPNProtocol || !curAPNProtocol.equalsIgnoreCase("IPV4V6") ||
                    null==curAPN || !curAPN.equalsIgnoreCase("ctlte")) {
                        setValue(R.string.apn_not_correct);
                        return check_result.WRONG;
                    } else {
                        setValue("");
                        return check_result.RIGHT;
                    }
                }
                if (getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                    if (null==curAPNProtocol || !curAPNProtocol.equalsIgnoreCase("IPV4V6") ||
                    null==curAPN || !curAPN.equalsIgnoreCase("ctnet")) {
                        setValue(R.string.apn_not_correct);
                        return check_result.WRONG;
                    } else {
                        setValue("");
                        return check_result.RIGHT;
                    }
                }
                return check_result.WRONG;
            }
        }

    }

    public boolean onReset() {
        CTSCLog.v(TAG, "onReset()");
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            for (Integer simId : mSimNeedSetAPN) {
                String where = new String();
                APNBuilder apnBuilder = null;
                if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                    where = "apn=\'rsapn\'";
                    apnBuilder = new APNBuilder(getContext(), simId.intValue(), "rsapn", "rsapn");
                } else if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE)) {
                    where = "protocol=\'IPV4V6\'";
                    apnBuilder = new APNBuilder(getContext(), simId.intValue(), "ctlte", "ctlte");
                } else if (getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                    where = "protocol=\'IPV4V6\'";
                    apnBuilder = new APNBuilder(getContext(), simId.intValue(), "ctnet", "ctnet");
                }

                where += " and numeric=\'" + apnBuilder.getSimOperator() + "\'";
                CTSCLog.v(TAG, "where = " + where);
                int simNo = simId.intValue() + 1;
                Uri uri= Uri.parse("content://telephony/carriers");
                Cursor cursor = getContext().getContentResolver().query(uri, new String[] {
                                        "_id", "numeric"}, where, null, null);

                if ((cursor != null) && (true == cursor.moveToFirst())) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex("_id");
                    String apnId = cursor.getString(index);
                    if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                        ContentValues values = new ContentValues();
                        values.put(Telephony.Carriers.NAME, "rsapn");
                        values.put(Telephony.Carriers.APN, "rsapn");
                        values.put(Telephony.Carriers.PROTOCOL, "IPV4V6");
                        values.put(Telephony.Carriers.ROAMING_PROTOCOL, "IPV4V6");
                        String whereUpdate = "_id=\"" + apnId + "\"";

                        getContext().getContentResolver().update(
                                uri,
                                values,
                                whereUpdate,
                                null);
                    }
                    ContentValues values_prefer = new ContentValues();
                    values_prefer.put("apn_id", apnId);
                    int subid = getSubIdBySlot(simId.intValue());
                    getContext().getContentResolver().update(
                           Uri.parse("content://telephony/carriers/preferapn" + "/subId/" + subid),
                           values_prefer,
                           null,
                           null);

                } else {
                    if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE)||
                    getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                        apnBuilder.setApnProtocol("IPV4V6");
                    }

                    if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                        apnBuilder.setApnProtocol("IPV4V6");
                        apnBuilder.setApnRoamingProtocol("IPV4V6");
                    }
                    apnBuilder.build().setAsCurrent();
                }

                if (null != cursor) {
                    cursor.close();
                }
            }
        } else {
            String where = new String();
            APNBuilder apnBuilder = null;
            if (getKey().equals(CheckItemKeySet.CI_APN_RSAPN)) {
                where = "apn=\'rsapn\'";
                apnBuilder = new APNBuilder(getContext(), "rsapn", "rsapn");
            } else if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE)) {
                where = "protocol=\'IPV4V6\'";
                apnBuilder = new APNBuilder(getContext(), "ctlte", "ctlte");
            } else if (getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                where = "protocol=\'IPV4V6\'";
                apnBuilder = new APNBuilder(getContext(), "ctnet", "ctnet");
            }
            /*where += " and mcc=\'" + apnBuilder.getMCC() + "\'"
                    + " and mnc=\'" + apnBuilder.getMNC() + "\'"
                    + " and numeric=\'" + apnBuilder.getSimOperator() + "\'";
            */
            where += " and numeric=\'" + apnBuilder.getSimOperator() + "\'";
            CTSCLog.v(TAG, "where = " + where);

            Uri uri = Uri.parse("content://telephony/carriers");
            Cursor cursor = getContext().getContentResolver().query(uri, new String[] {
                                        "_id", "numeric"}, where, null, null);

            if ((cursor != null) && (true == cursor.moveToFirst())) {
                cursor.moveToFirst();

                int index = cursor.getColumnIndex("_id");
                String apnId = cursor.getString(index);

                ContentValues values_prefer = new ContentValues();
                values_prefer.put("apn_id", apnId);

                getContext().getContentResolver().update(
                                    Uri.parse("content://telephony/carriers/preferapn"),
                                    values_prefer,
                                    null,
                                    null);


            } else {
                if (getKey().equals(CheckItemKeySet.CI_APN_CTLTE) ||
                getKey().equals(CheckItemKeySet.CI_APN_CTNET)) {
                    apnBuilder.setApnProtocol("IPV4V6");
                }
                apnBuilder.build().setAsCurrent();
            }

            if (null != cursor) {
                cursor.close();
            }
        }
        return true;
    }
}


class APNBuilder {
    private final static String TAG = "CheckLabAPN";
    private Context mContext = null;
    private APN mAPN = null;
    private Uri mUri = null;
    private int mSlot = -1;
    private static final String CT_NUMERIC_CDMA = "46003";
    private static final String CT_NUMERIC_LTE = "46011";
    private static final String CT_CHINA_NW_MCC = "460";
    private static final String CT_MACOO_NW_MCC = "455";


    public APNBuilder(Context context, String apnName, String apn) {
        CTSCLog.v(TAG, "APNBuilder: apnName = " + apnName + " apn = " + apn);
        mContext = context;

        mAPN = new APN();
        mAPN.name = apnName;
        mAPN.apn = apn;
    }

    public APNBuilder(Context context, int slotId, String apnName, String apn) {
        CTSCLog.v(TAG, "APNBuilder: apnName = " + apnName + " apn = " + apn);
        mContext = context;

        mAPN = new APN();
        mAPN.name = apnName;
        mAPN.apn = apn;

        mSlot = slotId;
    }

    // for gemini
    public APNBuilder build() {
        CTSCLog.v(TAG, "build()");
        ContentValues values = new ContentValues();
        setParams(values);

        if(mSlot == -1) {
            mUri = mContext.getContentResolver().insert(
                                 Uri.parse("content://telephony/carriers"),
                                 values
                                 );
        } else {
            int simNo = mSlot + 1;
            mUri = mContext.getContentResolver().insert(
                                     Uri.parse("content://telephony/carriers"),
                                     values
                                     );
        }

        return this;
    }

    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubId();
    }

    private void setParams(ContentValues values) {
        CTSCLog.v(TAG, "setParams: mAPN.name = " + mAPN.name + " mAPN.apn = " + mAPN.apn);
        values.put(Telephony.Carriers.NAME, mAPN.name);
        values.put(Telephony.Carriers.APN, mAPN.apn);

        values.put(Telephony.Carriers.MCC, getMCC());
        values.put(Telephony.Carriers.MNC, getMNC());
        values.put(Telephony.Carriers.NUMERIC, getSimOperator());

        if(mAPN.proxy != null) {
            values.put(Telephony.Carriers.PROXY, mAPN.proxy);
        }

        if(mAPN.port != null) {
            values.put(Telephony.Carriers.PORT, mAPN.port);
        }

        if(mAPN.uName != null) {
            values.put(Telephony.Carriers.USER, mAPN.uName);
        }

        if(mAPN.pwd != null) {
            values.put(Telephony.Carriers.PASSWORD, mAPN.pwd);
        }

        if(mAPN.server != null) {
            values.put(Telephony.Carriers.SERVER, mAPN.server);
        }

        if(mAPN.mmsc != null) {
            values.put(Telephony.Carriers.MMSC, mAPN.mmsc);
        }

        if(mAPN.mmsproxy != null) {
            values.put(Telephony.Carriers.MMSPROXY, mAPN.mmsproxy);
        }

        if(mAPN.mmsport != null) {
            values.put(Telephony.Carriers.MMSPORT, mAPN.mmsport);
        }

        if(mAPN.authtype != null) {
            values.put(Telephony.Carriers.AUTH_TYPE, mAPN.authtype);
        }

        if(mAPN.apntype != null) {
            values.put(Telephony.Carriers.TYPE, mAPN.apntype);
        }

        if(mAPN.protocol != null) {
            values.put(Telephony.Carriers.PROTOCOL, mAPN.protocol);
        }

        if(mAPN.roaming_protocol != null) {
            values.put(Telephony.Carriers.ROAMING_PROTOCOL, mAPN.roaming_protocol);
        }
    }

    public void setAsCurrent() {
        CTSCLog.v(TAG, "setAsCurrent()");
        if (mUri == null) {
            return;
        }
        Cursor cur = mContext.getContentResolver().query(mUri, null, null, null, null);
        cur.moveToFirst();
        int index = cur.getColumnIndex("_id");
        String apnId = cur.getString(index);
        cur.close();

        ContentValues values = new ContentValues();
        values.put("apn_id", apnId);
        CTSCLog.v(TAG, "setAsCurrent: apn_id = " + apnId);
        if (mSlot == -1) {
            mContext.getContentResolver().update(
                                Uri.parse("content://telephony/carriers/preferapn"),
                                values,
                                null,
                                null);
        } else {
            //int simNo = mSlot + 1;
            int subid = getSubIdBySlot(mSlot);
            mContext.getContentResolver().update(
                           Uri.parse("content://telephony/carriers/preferapn" + "/subId/" + subid),
                           values,
                           null,
                           null);
        }
    }

    public APNBuilder setProxy(String proxy) {
        mAPN.proxy = proxy;

        return this;
    }

    public APNBuilder setPort(String port) {
        mAPN.port = port;

        return this;
    }

    public APNBuilder setUserName(String name) {
        mAPN.uName = name;

        return this;
    }

    public APNBuilder setPassword(String pwd) {
        mAPN.pwd = pwd;

        return this;
    }

    public APNBuilder setServer(String server) {
        mAPN.server = server;

        return this;
    }

    public APNBuilder setMMSC(String mmsc) {
        mAPN.mmsc = mmsc;

        return this;
    }

    public APNBuilder setMMSProxy(String mmsproxy) {
        mAPN.mmsproxy = mmsproxy;

        return this;
    }

    public APNBuilder setMMSPort(String mmsport) {
        mAPN.mmsport = mmsport;

        return this;
    }

    public APNBuilder setAuthType(String authtype) {
        mAPN.authtype = authtype;

        return this;
    }

    public APNBuilder setApnType(String apntype) {
        mAPN.apntype = apntype;

        return this;
    }

    public APNBuilder setApnProtocol(String protocol) {
        mAPN.protocol = protocol;

        return this;
    }

    public APNBuilder setApnRoamingProtocol(String protocol) {
        mAPN.roaming_protocol = protocol;

        return this;
    }

    String getMCC() {
        String oprator = getSimOperator();
        if (null == oprator) {
            return "";
        }
        return oprator.substring(0,3);
    }

    String getMNC() {
        String oprator = getSimOperator();
        if (null == oprator) {
            return "";
    }
        return oprator.substring(3);
    }


    String getSimOperator() {
        String oprator = null;

        switch(mSlot) {
            case -1:
            case 0:
            case 1:
                int subId = getSubIdBySlot(mSlot);
                oprator = TelephonyManager.getDefault().getSimOperator(subId);
                oprator = updateMccMncForSvlte(subId, oprator);
                break;
            default:
                break;
        }

        return oprator;
    }

    private boolean isCtNumeric(String numeric) {
        return (numeric != null && (numeric.contains(CT_NUMERIC_LTE) || numeric
                .contains(CT_NUMERIC_CDMA)));
    }

    private boolean isCdmaCardType(int slotId) {
        SvlteUiccUtils util = SvlteUiccUtils.getInstance();
        boolean isCdmaCard = util.isRuimCsim(slotId);
        CTSCLog.d(TAG, "slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

   private boolean isCtInRoaming(String numeric, int subId) {
        if (isCtNumeric(numeric)) {
            String networkNumeric = TelephonyManager.getDefault()
                    .getNetworkOperatorForSubscription(subId);
            if (networkNumeric != null && networkNumeric.length() >= 3
                    && !networkNumeric.startsWith(CT_CHINA_NW_MCC)
                    && !networkNumeric.startsWith(CT_MACOO_NW_MCC)) {
                return true;
            }
        }
        return false;
    }

    private String updateMccMncForSvlte(int subId, String iccNumeric) {
        String networkNumeric = TelephonyManager.getDefault().getNetworkOperatorForSubscription(
                subId);
        int slotId = SubscriptionManager.getSlotId(subId);
        CTSCLog.d(TAG, "updateMccMncForCdma, iccNumeric = " + iccNumeric + ", subid = " + subId
                + ", networkNumeric = " + networkNumeric);
        if (isCdmaCardType(slotId) && isCtInRoaming(networkNumeric, subId)) {
            CTSCLog.d(TAG, "ROAMING, return " + networkNumeric);
            return networkNumeric;
        }
        return iccNumeric;
    }

    class APN {
        String name;
        String apn;
        String proxy;
        String port;
        String uName;
        String pwd;
        String server;
        String mmsc;
        String mmsproxy;
        String mmsport;
        String mcc;
        String mnc;
        String authtype;
        String apntype;
        String protocol;
        String roaming_protocol;
    }
}


class CheckBT extends CheckItemBase {
    private BluetoothAdapter mAdapter = null;

    CheckBT(Context c, String key) {
        super(c, key);

        setTitle(R.string.bt_name_check_title);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        StringBuilder sb = new StringBuilder();
        sb.append(getContext().getString(R.string.bt_cmcc_req));
        setNote(sb.toString());
    }

    public boolean onCheck() {
        if (mAdapter.isEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(getContext().getString(R.string.bt_name_current));
            sb.append(mAdapter.getName());

            setValue(sb.toString());
        } else {
            setValue(R.string.bt_open_device);
        }

        return true;
    }

    public boolean setCheckResult(check_result result) {
        return super.setCheckResult(result);
    }

    public check_result getCheckResult() {
        return super.getCheckResult();
    }

}


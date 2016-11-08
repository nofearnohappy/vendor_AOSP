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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.OperatorInfo;
import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.phone.ext.DefaultNetworkSettingExt;

import java.util.ArrayList;
import java.util.List;

/**
 * CMCC feature,when inserting CMCC card,if searched network is not CMCC network, add "forbidden".
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.INetworkSettingExt")
public class OP01NetworkSettingExt extends DefaultNetworkSettingExt {

    private static final String TAG = "OP01NetworkSettingExt";

    private static final String CT_4G_NUMERIC = "46011";
    private static final String CMCC_NUMERIC_1 = "46000";
    private static final String CMCC_NUMERIC_2 = "46002";
    private static final String CMCC_NUMERIC_3 = "46007";

    private Context mOp01Context;

    /**
     * Construct method.
     * @param context context
     */
    public OP01NetworkSettingExt(Context context) {
        mOp01Context = context;
        Log.d(TAG, "OP01NetworkSettingExt");
    }

    /**
     * If user insert CMCC card, set CT operator networks 46011 as forbidden.
     * @param operatorInfoList old operatorInfoList
     * @param subId The sub id user selected
     * @return new list OperatorInfo
     */
    @Override
    public List<OperatorInfo> customizeNetworkList(
            List<OperatorInfo> operatorInfoList, int subId) {
        Log.d(TAG, "OP01NetworkSettingExt,customizeNetworkList");
        if (operatorInfoList == null || operatorInfoList.size() == 0) {
            Log.d(TAG, "customizeNetworkList return null list");
            return operatorInfoList;
        }
        String iccNumeric;
        TelephonyManager telephonyManager = (TelephonyManager) mOp01Context
                .getSystemService(Context.TELEPHONY_SERVICE);
        iccNumeric = telephonyManager.getSimOperator(subId);
        Log.d(TAG, "customizeNetworkList subId" + subId + ",iccNumeric=" + iccNumeric);
        //if inserting non CMCC card in CMCC load, do not care the situation.
        if (iccNumeric == null ||(!iccNumeric.equals(CMCC_NUMERIC_1)
                && !iccNumeric.equals(CMCC_NUMERIC_2) && !iccNumeric.equals(CMCC_NUMERIC_3))) {
            return operatorInfoList;
        }
        String cuOpName = mOp01Context.getResources()
                .getString(com.mediatek.R.string.oper_long_46001);
        List<OperatorInfo> newOPInfoList = new ArrayList<OperatorInfo>(operatorInfoList.size());
        for (OperatorInfo operatorInfo : operatorInfoList) {
            Log.d(TAG, "old customizeNetworkList operatorInfo=" + operatorInfo.toString());
        // CT new 4G network is under testing, its network alphalong name is "46011 4G"
        // when CMCC card performs network search.
        // We need to check this network and set it as forbidden here
            if (operatorInfo.getOperatorAlphaLong().contains(cuOpName)
                    || operatorInfo.getOperatorNumeric().contains(CT_4G_NUMERIC)) {
                newOPInfoList.add(new OperatorInfo(operatorInfo.getOperatorAlphaLong(),
                        operatorInfo.getOperatorAlphaShort(),
                        operatorInfo.getOperatorNumeric(), "forbidden"));
            } else {
                newOPInfoList.add(operatorInfo);
            }
            Log.d(TAG, "new customizeNetworkList operatorInfo,OperatorNumeric:"
                    + operatorInfo.getOperatorNumeric()
                    + "OperatorAlphaLong:" + operatorInfo.getOperatorAlphaLong());
        }
        return newOPInfoList;
    }
}

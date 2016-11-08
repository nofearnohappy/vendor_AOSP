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

package com.mediatek.dm;

import android.app.Service;
import android.content.Context;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.xml.DmXMLParser;

import java.io.File;

/**
 * Provide common functions for the whole application.
 */
public final class DmCommonFun {

    /**
     * a tag name in config file
     */
    private static final String XML_TAG_OP = "op";

    /**
     * Parse operator name from a config file.
     *
     * @return operator name
     */
    public static String getOperatorName() {
        String opName = null;
        File configFileInSystem = new File(DmConst.PathName.CONFIG_FILE_IN_SYSTEM);
        if (configFileInSystem.exists()) {
            DmXMLParser xmlParser = new DmXMLParser(DmConst.PathName.CONFIG_FILE_IN_SYSTEM);
            opName = xmlParser.getValByTagName(XML_TAG_OP);
            Log.i(TAG.COMMON, "operator = " + opName);
        }

        return opName;

    }

    /**
     * Get the registered Subscription ID by compare the current IMSI with the saved
     * IMSI.
     *
     * @param context The Context.
     * @return Subscription ID
     */
    public static int getRegisterSubID(Context context) {
        String registerImsi = null;
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        if (telMgr == null) {
            Log.e(TAG.COMMON, "Get TelephonyManager failed.");
            return -1;
        }

        try {
            DmAgent agent = MTKPhone.getDmAgent();
            if (agent == null) {
                Log.e(TAG.COMMON, "get dm_agent_binder failed.");
                return -1;
            }
            byte[] imsiByte = agent.readImsi();
            if (imsiByte != null) {
                registerImsi = new String(imsiByte);
            }
        } catch (RemoteException e) {
            Log.e(TAG.COMMON, "get registered IMSI failed", e);
        }

        if (registerImsi == null) {
            Log.e(TAG.COMMON, "get registered IMSI failed");
            return -1;
        }

        Log.i(TAG.COMMON, "registered imsi=" + registerImsi);

        for (int i = 0; i < 2; i++) {
            int[] subId = SubscriptionManager.getSubId(DmConst.GEMINI_SIMS[i]);
            /// M: A Slot may have two subscription IDs.
            if (subId == null) {
                continue;
            }

            String[] imsi = new String[2];
            for (int j = 0; j < 2; j++) {
                if (subId[j] == -1) {
                    continue;
                }
                imsi[j] = telMgr.getSubscriberId(subId[j]);
                Log.i(TAG.COMMON, "SubscriberId = " + subId[j]);

                if (imsi[j] != null && imsi[j].equals(registerImsi)) {
                    Log.i(TAG.COMMON, "register SIM card is SIM " + i);
                    Log.i(TAG.COMMON, "IMSI: " + imsi[j]);
                    return subId[j];
                }
            }
        }

        return -1;
    }

    /**
     * Get preferred subscription ID for data connection.
     * @param context The Context.
     * @return preferred subscription ID
     */
    public static int getPreferredSubID(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        if (telMgr == null) {
            Log.e(TAG.COMMON, "Get TelephonyManager failed.");
            return -1;
        }

        ///M: check if any sim card has connected to the network.
        int subId = SubscriptionManager.getDefaultDataSubId();
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG.COMMON, "getPreferredSubID(), Default Data SubId: " + subId);
            return subId;
        }

        Log.d(TAG.COMMON, "getPreferredSubID(), mobile data not available.");

        subId = SubscriptionManager.getDefaultSubId();
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG.COMMON, "getPreferredSubID(), Default SubId: " + subId);
            return subId;
        }

        return -1;
    }

}

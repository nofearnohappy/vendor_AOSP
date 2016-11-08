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

import android.util.Log;

import java.util.List;

public class SmsBuilder {
    private static final String TAG = "SmsReg/SmsBuilder";
    private static final String CMD_GET_IMSI = "getimsi";
    private static final String CMD_GET_IMEI = "getimei";
    private static final String CMD_GET_VERSION = "getversion";
    private static final String CMD_GET_PRODUCT = "getproduct";
    private static final String CMD_GET_VERNDOR = "getvendor";
    private static final String CMD_GET_OEM = "getOem";

    /**
     * Build the registered message content
     */
    public static String getSmsContent(XmlGenerator xmlGenerator, long subId) {
        Log.i(TAG, "SimId = " + subId);
        if (subId < 0) {
            throw new Error("SimId is not valid!");
        }

        List<SmsInfoUnit> smsInfoList = xmlGenerator.getSmsInfoList();
        if (smsInfoList == null) {
            throw new Error("No segment found in config file.");
        }

        String smsContent = "";
        for (int i = 0; i < smsInfoList.size(); i++) {
            SmsInfoUnit smsUnit = smsInfoList.get(i);
            String contentCommand = smsUnit.getContent();
            String contentInfo = getContentInfo(xmlGenerator, contentCommand, subId);
            Log.i(TAG, "Command is " + contentCommand + ", info is " + contentInfo);

            if (contentInfo != null) {
                String prefix = smsUnit.getPrefix();
                String postfix = smsUnit.getPostfix();
                if (prefix != null) {
                    smsContent += prefix;
                }
                smsContent += contentInfo;
                if (postfix != null) {
                    smsContent += postfix;
                }
            } else {
                Log.w(TAG, "The command " + contentCommand + "'s content is null");
                smsContent = null;
                break;
            }
        }
        Log.i(TAG, "Sms content: " + smsContent);
        return smsContent;
    }

    /**
     * Get content info for a command
     */
    public static String getContentInfo(XmlGenerator xmlGenerator, String command, long subId) {

        PlatformManager platformManager = SmsRegApplication.getPlatformManager();

        if (command.equals(CMD_GET_IMSI)) {
            return platformManager.getSubImsi(subId);

        } else if (command.equals(CMD_GET_IMEI)) {
            return platformManager.getDeviceImei();

        } else if (command.equals(CMD_GET_VERSION)) {
            return platformManager.getDeviceVersion();

        } else if (command.equals(CMD_GET_PRODUCT)) {
            return platformManager.getProductName();

        } else if (command.equals(CMD_GET_VERNDOR)) {
            return platformManager.getManufacturerName();

        } else if (command.equals(CMD_GET_OEM)) {
            String oemName = xmlGenerator.getOemName();
            if (oemName == null) {
                // Here should use the system api to get oem name;
                return null;
            }
            return oemName;
        }

        Log.e(TAG, "Wrong command, return null.");
        return null;
    }
}

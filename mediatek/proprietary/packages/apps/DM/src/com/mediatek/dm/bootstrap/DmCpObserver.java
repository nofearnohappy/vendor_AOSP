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

package com.mediatek.dm.bootstrap;

import android.content.ContentValues;
import android.provider.Telephony;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.redbend.vdm.CpObserver;

public class DmCpObserver implements CpObserver {

    private static final String TYPE_CP_NAPDEF = "NAPDEF";
    private static final String TYPE_CP_PXLOGICAL = "PXLOGICAL";
    private static final String TYPE_CP_PXPHYSICAL = "PXPHYSICAL";
    private static final String TYPE_CP_PORT = "PORT";

    private static final String FIELD_APN_NAME = "NAME";
    private static final String FIELD_APN_APN = "NAP-ADDRESS";
    private static final String FIELD_APN_PROXY = "PXADDR";
    private static final String FIELD_APN_PROXY_TYPE = "PXADDRTYPE";
    private static final String FIELD_APN_PORT = "PORTNBR";
    private static final String FIELD_APN_USERNAME = "AUTHNAME";
    private static final String FIELD_APN_PASSWORD = "AUTHSECRET";
    private static final String FIELD_APN_SERVER = "SERVER";
    private static final String FIELD_APN_AUTH_TYPE = "AUTHTYPE";
    private static final String FIELD_APN_TYPE = "APPID";
    private static final String FIELD_APN_NAP_ID = "NAPID";
    private static final String FIELD_APN_PROXY_ID = "PROXY-ID";
    private static final String FIELD_APN_BEARER = "BEARER";

    private static final String BROWSER_APPID = "w2";
    private static final String MMS_APPID = "w4";
    private static final String DM_APPID = "w7";
    private static final String SMTP_APPID = "25";
    private static final String POP3_APPID = "110";
    private static final String IMAP4_APPID = "143";
    private static final String RTSP_APPID = "554";
    private static final String SUPL_APPID = "ap0004";
    private static final String MMS_2_APPID = "ap0005";
    private static final String APN_APPID = "apn";
    private static final String DS_APID = "w5";
    private static final String IMPS_APPID = "wA";

    private static ContentValues sValues;
    private static boolean isBootstrap;

    public void notify(String field, String value, boolean alreadyHandled) {

        String str = new StringBuilder("field =  ").append(field).append(" value = ").append(value)
                .append(" alreadyHandled = ").append(alreadyHandled).toString();

        Log.i(TAG.CP, str);

        if (alreadyHandled) {
            isBootstrap = true;
        }

        if (!alreadyHandled) {
            if (field.contains(TYPE_CP_NAPDEF)) {
                configCpApn(field, value);
            } else if (field.contains(TYPE_CP_PXLOGICAL)) {
                configProxy(field, value);
            }
        }
    }

    private void put(String key, String value) {
        if (sValues == null) {
            sValues = new ContentValues();
        }
        sValues.put(key, value);
    }

    private void configCpApn(String field, String value) {

        String[] fieldArray = field.split("/");
        int end = fieldArray.length - 1;

        if (end < 1) {
            return;
        }

        if (!fieldArray[0].contains(TYPE_CP_NAPDEF)) {
            Log.w(TAG.CP, "not NAPDEF type");
            return;
        }

        if (FIELD_APN_NAME.equals(fieldArray[end])) {
            put(Telephony.Carriers.NAME, value);

            int s = field.indexOf("[");
            int e = field.indexOf("]");
            String napId = fieldArray[0].substring(s + 1, e);
            put(Telephony.Carriers.NAPID, napId);

        } else if (FIELD_APN_APN.equals(fieldArray[end])) {
            put(Telephony.Carriers.APN, value);
        } else if (FIELD_APN_USERNAME.equals(fieldArray[end])) {
            put(Telephony.Carriers.USER, value);
        } else if (FIELD_APN_PASSWORD.equals(fieldArray[end])) {
            put(Telephony.Carriers.PASSWORD, value);
        } else if (FIELD_APN_SERVER.equals(fieldArray[end])) {
            put(Telephony.Carriers.SERVER, value);
        } else if (FIELD_APN_AUTH_TYPE.equals(fieldArray[end])) {
            put(Telephony.Carriers.AUTH_TYPE, value);
        } else if (FIELD_APN_TYPE.equals(fieldArray[end])) {
            String type = getAPNType(value);
            if (type != null) {
                put(Telephony.Carriers.TYPE, type);
            }

        } else if (FIELD_APN_BEARER.equals(fieldArray[end])) {
            // put(Telephony.Carriers.BEARER, value);
        }
    }

    private void configProxy(String field, String value) {

        String[] fieldArray = field.split("/");
        int end = fieldArray.length - 1;

        if (end < 2) {
            return;
        }

        if (!TYPE_CP_PXLOGICAL.equals(fieldArray[0])) {
            Log.w(TAG.CP, "not PXLOGICAL type");
            return;
        }

        if (!TYPE_CP_PXPHYSICAL.equals(fieldArray[1])) {
            Log.w(TAG.CP, "not PXPHYSICAL type");
            return;
        }

        if (FIELD_APN_PROXY.equals(fieldArray[end])) {
            put(Telephony.Carriers.PROXY, value);
        } else if (FIELD_APN_PROXY_TYPE.equals(fieldArray[end])) {
            put(Telephony.Carriers.TYPE, value);
        } else if (FIELD_APN_PORT.equals(fieldArray[end])) {
            if (end < 3 || !TYPE_CP_PORT.equals(fieldArray[2])) {
                Log.w(TAG.CP, "not PORT type, don't config portnbr");
                return;
            }
            put(Telephony.Carriers.PORT, value);
        } else if (FIELD_APN_PROXY_ID.equals(fieldArray[end])) {
            put(Telephony.Carriers.PROXYID, value);
        } else if (FIELD_APN_NAP_ID.equals(fieldArray[end])) {
            put(Telephony.Carriers.NAPID, value);
        }
    }

    private String getAPNType(String appId) {
        String apnType = null;
        if (appId.equalsIgnoreCase(BROWSER_APPID)) {
            apnType = "default";
        } else if ((appId.equalsIgnoreCase(MMS_APPID) || appId.equalsIgnoreCase(MMS_2_APPID))
                && (apnType == null || !apnType.contains("mms"))) {
            apnType = "mms";
        } else if (appId.equalsIgnoreCase(SUPL_APPID)) {
            apnType = "supl";
        }
        return apnType;
    }

    public static synchronized ContentValues getContentValue() {
        if (sValues != null) {
            return new ContentValues(sValues);
        } else {
            return null;
        }
    }

    public static synchronized boolean isBootstrap() {
        return isBootstrap;
    }

    public static synchronized void resetStatus() {
        sValues = null;
        isBootstrap = false;
    }
}
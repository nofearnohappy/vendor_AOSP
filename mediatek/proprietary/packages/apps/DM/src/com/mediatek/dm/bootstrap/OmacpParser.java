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

package com.mediatek.dm.bootstrap;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.kxml2.wap.WbxmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class OmacpParser {

        public static String sName;

        public static String sUri;

        public static String sType;

        public static String sListType;

        public static String sParmName;

        public static String sParmValue;

    private static final String TAG = "DM/OmacpParser";

    // constants for labels

    private static final String CHARACTERISTIC = "characteristic";
    private static final String PARM = "parm";

    private static WbxmlParser sParser;

    public static final String[] TAG_TABLE_PAGE0 = { "wap-provisioningdoc", "characteristic",
            "parm" };

    public static final String[] TAG_TABLE_PAGE1 = { "", "characteristic", "parm" };

    public static final String[] ATTR_START_TABLE_PAGE0 = {
            "name",
            "value",
            "name=NAME",
            "name=NAP-ADDRESS",
            "name=NAP-ADDRTYPE",
            "name=CALLTYPE", // 5~A
            "name=VALIDUNTIL",
            "name=AUTHTYPE",
            "name=AUTHNAME",
            "name=AUTHSECRET",
            "name=LINGER", // B~F
            "name=BEARER",
            "name=NAPID",
            "name=COUNTRY",
            "name=NETWORK",
            "name=INTERNET",
            "name=PROXY-ID", // 10~15
            "name=PROXY-PROVIDER-ID",
            "name=DOMAIN",
            "name=PROVURL",
            "name=PXAUTH-TYPE",
            "name=PXAUTH-ID", // 16~1A
            "name=PXAUTH-PW",
            "name=STARTPAGE",
            "name=BASAUTH-ID",
            "name=BASAUTH-PW",
            "name=PUSHENABLED", // 1B~1F
            "name=PXADDR",
            "name=PXADDRTYPE",
            "name=TO-NAPID",
            "name=PORTNBR",
            "name=SERVICE",
            "name=LINKSPEED", // 20~25
            "name=DNLINKSPEED",
            "name=LOCAL-ADDR",
            "name=LOCAL-ADDRTYPE",
            "name=CONTEXT-ALLOW",
            "name=TRUST", // 26~2A
            "name=MASTER",
            "name=SID",
            "name=SOC",
            "name=WSP-VERSION",
            "name=PHYSICAL-PROXY-ID", // 2B~2F
            "name=CLIENT-ID",
            "name=DELIVERY-ERR-SDU",
            "name=DELIVERY-ORDER",
            "name=TRAFFIC-CLASS",
            "name=MAX-SDU-SIZE",
            "name=MAX-BITRATE-UPLINK", // 30~35
            "name=MAX-BITRATE-DNLINK",
            "name=RESIDUAL-BER",
            "name=SDU-ERROR-RATIO",
            "name=TRAFFIC-HANDL-PRIO",
            "name=TRANSFER-DELAY", // 36~3A
            "name=GUARANTEED-BITRATE-UPLINK", "name=GUARANTEED-BITRATE-DNLINK",
            "name=PXADDR-FQDN",
            "name=PROXY-PW",
            "name=PPGAUTH-TYPE", // 3B~3F
            "", "", "", "", "",
            "version",
            "version=1.0", // 40~46
            "name=PULLENABLED", "name=DNS-ADDR",
            "name=MAX-NUM-RETRY",
            "name=FIRST-RETRY-TIMEOUT", // 47~4A
            "name=REREG-THRESHOLD", "name=T-BIT", "",
            "name=AUTH-ENTITY",
            "name=SPI", // 4B~4F
            "type", "type=PXLOGICAL", "type=PXPHYSICAL", "type=PORT",
            "type=VALIDITY",
            "type=NAPDEF", // 50~55
            "type=BOOTSTRAP", "type=VENDORCONFIG", "type=CLIENTIDENTITY", "type=PXAUTHINFO",
            "type=NAPAUTHINFO", "type=ACCESS" // 56~5B
    };

    public static final String[] ATTR_START_TABLE_PAGE1 = { "name", "value", "name=NAME", "",
            "",
            "", // 5~A
            "", "", "", "",
            "", // B~F
            "", "", "", "", "name=INTERNET",
            "", // 10~15
            "", "", "", "",
            "", // 16~1A
            "", "name=STARTPAGE", "", "",
            "", // 1B~1F
            "", "", "name=TO-NAPID", "name=PORTNBR", "name=SERVICE",
            "", // 20~25
            "", "", "", "",
            "", // 26~2A
            "", "", "", "name=ACCEPT",
            "name=AAUTHDATA", // 2B~2F
            "name=AAUTHLEVEL", "name=AAUTHNAME", "name=AAUTHSECRET", "name=AAUTHTYPE", "name=ADDR",
            "name=ADDRTYPE", // 30~35
            "name=APPID", "name=ARPOTOCOL", "name=PROVIDER-ID", "name=TO-PROXY", "name=URI", // 36~3A
            "name=RULE", "", "", "", "", // 3B~3F
            "", "", "", "", "", "", "", // 40~46
            "", "", "", "", // 47~4A
            "", "", "", "", "", // 4B~4F
            "type", "", "", "type=PORT", "", "type=APPLICATION", // 50~55
            "type=APPADDR", "type=APPAUTH", "type=CLIENTIDENTITY", "type=RESOURCE", "", "" // 56~5B
    };

    public static final String[] ATTR_VALUE_TABLE_PAGE0 = { "IPV4", "IPV6", "E164",
            "ALPHA",
            "APN",
            "SCODE", // 85~8A
            "TETRA-ITSI", "MAN", "",
            "",
            "", // 8B~8F
            "ANALOG-MODEM", "V.120", "V.110", "X.31",
            "BIT-TRANSPARENT",
            "DIRECT-ASYNCHRONOUS-DATA-SERVICE", // 90~95
            "", "", "", "",
            "PAP", // 96~9A
            "CHAP", "HTTP-BASIC", "HTTP-DIGEST", "WTLS-SS",
            "MD5", // 9B~9F
            "", "", "GSM-USSD", "GSM-SMS", "ANSI-136-GUTS",
            "IS-95-CDMA-SMS", // A0~A5
            "IS-95-CDMA-CSD", "IS-95-CDMA-PACKET", "ANSI-136-CSD", "ANSI-136-GPRS",
            "GSM-CSD", // A6~AA
            "GSM-GPRS", "AMPS-CDPD", "PDC-CSD", "PDC-PACKET",
            "IDEN-SMS", // AB~AF
            "IDEN-CSD", "IDEN-PACKET", "FLEX/REFLEX", "PHS-SMS", "PHS-CSD",
            "TETRA-SDS", // B0~B5
            "TETRA-PACKET", "ANSI-136-GHOST", "MOBITEX-MPAK", "CDMA2000-1X-SIMPLE-IP",
            "CDMA2000-1X-MOBILE-IP", // B6~BA
            "", "", "", "", "", // BB~BF
            "", "", "", "", "", "AUTOBAUDING", // C0~C5
            "", "", "", "", "CL-WSP", // C6~CA
            "CO-WSP", "CL-SEC-WSP", "CO-SEC-WSP", "CL-SEC-WTA", "CO-SEC-WTA", // CB~CF
            "OTA-HTTP-TO", "OTA-HTTP-TLS-TO", "OTA-HTTP-PO", "OTA-HTTP-TLS-PO", // D0~D3
            "", "", "", "", "", "", "", // D4~DA
            "", "", "", "", "", // DB~DF
            "AAA", "HA" };

    public static final String[] ATTR_VALUE_TABLE_PAGE1 = { "", "", "NAME", "", "", "", // 5~A
            "", "", "", "", "", // B~F
            "", "", "", "", "INTERNET", "", // 10~15
            "", "", "", "", "", // 16~1A
            "", "STARTPAGE", "", "", "", // 1B~1F
            "", "", "TO-NAPID", "PORTNBR", "SERVICE", "", // 20~25
            "", "", "", "", "", // 26~2A
            "", "", "", "ACCEPT", "AAUTHDATA", // 2B~2F
            "AAUTHLEVEL", "AAUTHNAME", "AAUTHSECRET", "AAUTHTYPE", "ADDR", "ADDRTYPE", // 30~35
            "APPID", "ARPOTOCOL", "PROVIDER-ID", "TO-PROXY", "URI", // 36~3A
            "RULE", "", "", "", "", // 3B~3F
            "", "", "", "", "", "", "", // 40~46
            "", "", "", "", // 47~4A
            "", "", "", "", "", // 4B~4F
            "", "", "", "PORT", "", "APPLICATION", // 50~55
            "APPADDR", "APPAUTH", "CLIENTIDENTITY", "RESOURCE", "", "", // 56~5B
            "", "", "", "", // 5C~5F
            "", "", "", "", "", "", // 60~65
            "", "", "", "", "", // 66~6A
            "", "", "", "", "", // 6B~6F
            "", "", "", "", "", "", // 70~75
            "", "", "", "", "", // 76~7A
            "", "", "", "", "", // 7B~7F
            "", "", "", "", "", // 80~84

            "", "IPV6", "E164", "ALPHA", "", "", // 85~8A
            "", "", "APPSRV", "OBEX", "", // 8B~8F
            ",(comma character", "HTTP-", "BASIC", "DIGEST", "", "", // 90~95
    };

    private static WbxmlParser getWbxmlParser() {
        WbxmlParser parser = new WbxmlParser();
        parser.setTagTable(0, TAG_TABLE_PAGE0);
        parser.setAttrStartTable(0, ATTR_START_TABLE_PAGE0);
        parser.setAttrValueTable(0, ATTR_VALUE_TABLE_PAGE0);

        parser.setTagTable(1, TAG_TABLE_PAGE1);
        parser.setAttrStartTable(1, ATTR_START_TABLE_PAGE1);
        parser.setAttrValueTable(1, ATTR_VALUE_TABLE_PAGE1);

        return parser;
    }

    public static boolean isDmCpBootstrap(byte[] data) {

        boolean result = false;
        sParser = getWbxmlParser();
        InputStream in = new ByteArrayInputStream(data);
        try {
            sParser.setInput(in, null);

            int eventType = -1;
            eventType = getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (result) {
                    break;
                }
                getParserName();
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;

                case XmlPullParser.START_TAG:
                    result = getStartTagData();
                    break;

                case XmlPullParser.END_TAG:
                    getEndTagData();
                    break;

                default:
                    Log.e(TAG, "OmacpParserBase parse eventType error, eventType is : " + eventType);
                    break;
                }
                eventType = getEventType();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "OmacpParserBase parse Exception, e is : " + e);
        }
        return result;
    }

    private static boolean getStartTagData() {
        if (sName.equalsIgnoreCase(CHARACTERISTIC)) {
            // Get the value of the type attribute
            getCharacteristicParams();
        } else if (sName.equalsIgnoreCase(PARM)) {
            // Get the value of the type attribute
            // Get the value of name and value attributes
            return isDmCp();
        }
        return false;
    }

    private static void getEndTagData() {
        if (sName.equalsIgnoreCase("CHARACTERISTIC")) {
            if (sType == null) {
                Log.e(TAG, "OmacpParserBase END_TAG type is null.");
                return;
            }

            if (sType.equalsIgnoreCase("PORT")) {
                if (sListType.equalsIgnoreCase("APPLICATION")) {
                    sType = "APPADDR";
                } else if (sListType.equalsIgnoreCase("PXPHYSICAL")) {
                    sType = "PXPHYSICAL";
                } else if (sListType.equalsIgnoreCase("PXLOGICAL")) {
                    sType = "PXLOGICAL";
                }
            } else if (sType.equalsIgnoreCase("APPADDR")
                    || sType.equalsIgnoreCase("APPAUTH")
                    || sType.equalsIgnoreCase("RESOURCE")) {
                sType = "APPLICATION";
            } else if (sType.equalsIgnoreCase("NAPAUTHINFO")
                    || sType.equalsIgnoreCase("VALIDITY")) {
                sType = "NAPDEF";
            } else if (sType.equalsIgnoreCase("PXAUTHINFO")
                    || sType.equalsIgnoreCase("PXPHYSICAL")) {
                sType = "PXLOGICAL";
            }
        }
    }

    private static int getEventType() {
        int eventType = -1;
        try {
            eventType = sParser.next();
        } catch (XmlPullParserException e) {
            Log.e(TAG, "OmacpParser get eventType Exception, e is : " + e);
        } catch (IOException e) {
            Log.e(TAG, "OmacpParser get eventType Exception, e is : " + e);
        }
        return eventType;
    }

    private static void getParserName() {
        sName = ((WbxmlParser) sParser).getName();

        if (sName == null) {
            Log.e(TAG, "OmacpParserBase START_TAG name is null.");
            return;
        }
    }

    private static void getCharacteristicParams() {
        getParserUriAndType();
        if (sType.equalsIgnoreCase("PXLOGICAL")
                || sType.equalsIgnoreCase("PXPHYSICAL")
                || sType.equalsIgnoreCase("NAPDEF")
                || sType.equalsIgnoreCase("APPLICATION")) {
            sListType = sType;
        }
        Log.d(TAG, "OmacpParserBase, type = " + sType + ",listType = "
                + sListType);
    }

    private static void getParserUriAndType() {
        sUri = sParser.getNamespace();
        sType = sParser.getAttributeValue(sUri, "type");
        if (sType == null) {
            Log.e(TAG, "OmacpParserBase START_TAG type is null.");
            throw new IllegalArgumentException();
        }
    }

    private static boolean isDmCp() {
        String appId = null;
        sUri = sParser.getNamespace();
        sParmName = sParser.getAttributeValue(sUri, "name");
        sParmValue = sParser.getAttributeValue(sUri, "value");

        Log.d(TAG, "getAttributeParams, mUri = " + sUri);
        Log.d(TAG, "getAttributeParams, mParmName = " + sParmName);
        Log.d(TAG, "getAttributeParams, mParmValue = " + sParmValue);
        if (sListType != null) {
            if (sListType.equalsIgnoreCase("APPLICATION")) {
                if (sType.equalsIgnoreCase("APPLICATION")) {
                    if (sParmName.equalsIgnoreCase("APPID")) {
                        appId = sParmValue;
                    }
                }
            }
        }
        return "w7".equals(appId) || "x7".equals(appId);
    }

}

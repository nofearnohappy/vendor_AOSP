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

/* //device/content/providers/telephony/TelephonyProvider.java
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.mediatek.mediatekdm.conn;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SimpleXMLAccessor;

import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class DmDatabase {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    private static final String DEFAULTPROXYADDR = "10.0.0.172";
    private static final int DEFAULTPROXYPORT = 80;

    private ContentResolver mContentResolver;
    private long mSubId = -1;

    /**
     * Information of apn for DM.
     */
    public static class DmApnInfo {
        public Integer id;
        public String name;
        public String numeric;
        public String mcc;
        public String mnc;
        public String apn;
        public String user;
        public String password;
        public String server;
        public String proxy;
        public String port;
        public String type;
    }

    private Context mContext;
    private Builder mBuilder;

    public DmDatabase(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    public void prepareApn() {
        for (long subId : PlatformManager.getInstance().getSubIdList()) {
            prepareDmApn(subId);
        }
    }

    public boolean prepareDmApn(long subId) {

        Log.i(TAG.DATABASE, "Sub Id = " + subId);
        Cursor cursor = null;
        boolean ret = false;
        int count = 0;

        cursor = mContentResolver.query(PlatformManager.getInstance().getDmContentUri(), null,
                null, null, null);

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }

        if (count > 0) {
            Log.w(TAG.DATABASE, "There are apn data in dm apn mTable, the record is " + count);
            return true;
        }

        Log.w(TAG.DATABASE, "There is no data in dm apn mTable");

        ret = initDmApnTable(getApnValuesFromConfFile(), subId);
        if (!ret) {
            Log.e(TAG.DATABASE, "Init Apn mTable error!");
        }
        return ret;
    }

    public static void clearDB(Context context) {
        ContentResolver cr = context.getContentResolver();

        cr.delete(PlatformManager.getInstance().getDmContentUri(), null, null);
    }

    private static class DmApnConfParser extends SimpleXMLAccessor {
        public ArrayList<DmApnInfo> buildApnInfos(InputStream is, String operatorName) {
            parse(is);
            ArrayList<DmApnInfo> result = new ArrayList<DmApnInfo>();
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression operatorExpression = xpath.compile("//dmapn/" + operatorName);
                NodeList apnList = (NodeList) operatorExpression.evaluate(mDocument,
                        XPathConstants.NODESET);
                for (int i = 0; i < apnList.getLength(); i++) {
                    DmApnInfo info = new DmApnInfo();
                    XPathExpression fieldExpression = xpath.compile("child::*");
                    NodeList fields = (NodeList) fieldExpression.evaluate(apnList.item(i),
                            XPathConstants.NODESET);
                    for (int j = 0; j < fields.getLength(); j++) {
                        String name = fields.item(j).getNodeName();
                        String value = fields.item(j).getTextContent().trim();
                        if (name.equals("id")) {
                            info.id = Integer.parseInt(value);
                        } else if (name.equals("name")) {
                            info.name = value;
                        } else if (name.equals("numeric")) {
                            info.numeric = value;
                        } else if (name.equals("mcc")) {
                            info.mcc = value;
                        } else if (name.equals("mnc")) {
                            info.mnc = value;
                        } else if (name.equals("apn")) {
                            info.apn = value;
                        } else if (name.equals("user")) {
                            info.user = value;
                        } else if (name.equals("password")) {
                            info.password = value;
                        } else if (name.equals("server")) {
                            info.server = value;
                        } else if (name.equals("proxy")) {
                            info.proxy = value;
                        } else if (name.equals("port")) {
                            info.port = value;
                        } else if (name.equals("type")) {
                            info.type = value;
                        } else {
                            Log.w(TAG.DATABASE, "Unsupported field: " + name);
                        }
                    }
                    result.add(info);
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
            return result;
        }
    }

    private ArrayList<DmApnInfo> getApnValuesFromConfFile() {
        Log.i(TAG.DATABASE, "getApnValuesFromConfFile");
        InputStream is = null;
        try {
            is = new FileInputStream(PlatformManager.getInstance().getPathInSystem(
                    DmConst.Path.DM_APN_INFO_FILE));
            DmApnConfParser xmlParser = new DmApnConfParser();
            final String operatorName = DmConfig.getInstance().getCustomizedOperator();
            if (operatorName == null) {
                throw new Error("Get operator name from config file is null");
            }
            return xmlParser.buildApnInfos(is, operatorName);
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }

    // init the mTable if need
    private boolean initDmApnTable(ArrayList<DmApnInfo> apnInfo, long subId) {
        Log.i(TAG.DATABASE, "Enter init Dm Apn Table");
        if (apnInfo == null || apnInfo.size() <= 0) {
            Log.e(TAG.DATABASE, "Apn that read from apn configure file is null");
            return false;
        }

        int size = apnInfo.size();
        Log.i(TAG.DATABASE, "apnInfo size = " + size);
        ArrayList<ContentValues> apnInfoEntry = new ArrayList<ContentValues>(size);

        for (int i = 0; i < size; i++) {
            Log.i(TAG.DATABASE, "insert i = " + i);
            Log.i(TAG.DATABASE, "apnInfo.get(" + i + ").id = " + apnInfo.get(i).id);
            ContentValues v = new ContentValues();
            if (apnInfo.get(i) == null || apnInfo.get(i).id == null) {
                Log.w(TAG.DATABASE, "before continue apnInfo.get.id " + apnInfo.get(i).id);
                continue;
            }

            v.put("_id", apnInfo.get(i).id);
            if (apnInfo.get(i).name != null) {
                v.put("name", apnInfo.get(i).name);
            }
            if (apnInfo.get(i).numeric != null) {
                v.put("numeric", apnInfo.get(i).numeric);
            }
            if (apnInfo.get(i).mcc != null) {
                v.put("mcc", apnInfo.get(i).mcc);
            }
            if (apnInfo.get(i).mnc != null) {
                v.put("mnc", apnInfo.get(i).mnc);
            }

            if (apnInfo.get(i).apn != null) {
                v.put("apn", apnInfo.get(i).apn);
            }
            if (apnInfo.get(i).type != null) {
                v.put("type", apnInfo.get(i).type);
            }

            if (apnInfo.get(i).user != null) {
                v.put("user", apnInfo.get(i).user);
            }
            if (apnInfo.get(i).server != null) {
                v.put("server", apnInfo.get(i).server);
            }
            if (apnInfo.get(i).password != null) {
                v.put("password", apnInfo.get(i).password);
            }
            if (apnInfo.get(i).proxy != null) {
                v.put("proxy", apnInfo.get(i).proxy);
            }
            if (apnInfo.get(i).port != null) {
                v.put("port", apnInfo.get(i).port);
            }

            v.put("sub_id", subId);
            apnInfoEntry.add(v);

        }
        int insertSize = apnInfoEntry.size();
        Log.i(TAG.DATABASE, "insert size = " + insertSize);
        if (insertSize > 0) {
            mBuilder = PlatformManager.getInstance()
                    .getDmContentUri()
                    .buildUpon();
            ContentValues[] values = new ContentValues[apnInfoEntry.size()];
            for (int i = 0; i < insertSize; i++) {
                Log.i(TAG.DATABASE, "insert to values i = [" + i + "]");
                values[i] = apnInfoEntry.get(i);
            }
            // bulk insert
            mContentResolver.bulkInsert(mBuilder.build(), values);
        }

        Log.i(TAG.DATABASE, "Init Dm database finish");
        return true;
    }

    public String getApnProxyFromSettings() {
        String operator = getOperator();
        if (operator == null) {
            return DEFAULTPROXYADDR;
        }

        Cursor cursor = getCursor(operator);
        if (cursor == null) {
            return null;
        }

        cursor.moveToFirst();
        int proxyAddrID = cursor.getColumnIndex("proxy");
        String proxyAddr = cursor.getString(proxyAddrID);
        if (cursor != null) {
            cursor.close();
        }
        Log.i(TAG.DATABASE, "proxy address = " + proxyAddr);
        return proxyAddr;
    }

    public int getApnProxyPortFromSettings() {
        String operator = getOperator();
        if (operator == null) {
            return DEFAULTPROXYPORT;
        }

        Cursor cursor = getCursor(operator);
        if (cursor == null) {
            return -1;
        }

        cursor.moveToFirst();
        String port = cursor.getString(cursor.getColumnIndex("port"));
        if (cursor != null) {
            cursor.close();
        }

        Log.i(TAG.DATABASE, "proxy port = " + port);
        return (Integer.parseInt(port));
    }

    public String getDmAddressFromSettings() {
        // waiting for Yuhui's interface
        String operator = getOperator();
        if (operator == null) {
            return null;
        }

        Cursor cursor = getCursor(operator);
        if (cursor == null) {
            return null;
        }

        cursor.moveToFirst();
        String serverAddr = cursor.getString(cursor.getColumnIndex("server"));
        if (cursor != null) {
            cursor.close();
        }

        Log.i(TAG.DATABASE, "server address = " + serverAddr);
        return serverAddr;
    }

    private String getOperator() {
        String operator = null;
        mSubId = PlatformManager.getInstance().getRegisteredSubId();
        if (mSubId == -1) {
            Log.e(TAG.DATABASE, "Get Register sub Id error");
            return null;
        }

        operator = PlatformManager.getInstance().getSubOperator(mSubId);
        Log.i(TAG.DATABASE, "sub operator numberic = " + operator);
        if (operator == null || operator.equals("")) {
            Log.e(TAG.DATABASE, "Get sub operator wrong");
            return null;
        }
        return operator;
    }

    private Cursor getCursor(String simOperator) {
        String where = "numeric =" + simOperator;
        Cursor cursor = mContentResolver.query(PlatformManager.getInstance()
                .getDmContentUri(), null, where, null, null);

        if (cursor == null || cursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        return cursor;
    }

}

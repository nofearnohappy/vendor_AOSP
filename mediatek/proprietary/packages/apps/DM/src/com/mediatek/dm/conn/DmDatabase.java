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

package com.mediatek.dm.conn;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Telephony.Carriers;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmCommonFun;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.xml.DmXMLParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

/**
 * Provide APN, proxy and server address for the application.
 */
public class DmDatabase {

    private TelephonyManager mTelephonyManager;
    private ContentResolver mContentResolver;
    private Cursor mCursor;
    private static final Uri DM_URI = MTKPhone.CONTENT_URI_DM;
    private static final String DEFAULT_PROXY_ADDR = "10.0.0.172";
    private static final int DEFAULT_PROXY_PORT = 80;
    private static final String DB_ID = "_id";
    private static final String DB_NAME = Carriers.NAME;
    private static final String DB_NUMERIC = Carriers.NUMERIC;
    private static final String DB_MCC = Carriers.MCC;
    private static final String DB_MNC = Carriers.MNC;
    private static final String DB_APN = Carriers.APN;
    private static final String DB_TYPE = Carriers.TYPE;
    private static final String DB_USER = Carriers.USER;
    private static final String DB_PASSWORD = Carriers.PASSWORD;
    private static final String DB_SERVER = Carriers.SERVER;
    private static final String DB_PROXY = Carriers.PROXY;
    private static final String DB_PORT = Carriers.PORT;
    private static final String DB_SUB_ID = Carriers.SUBSCRIPTION_ID;
    private static final String NODE_ID = "id";
    private static final String NODE_NAME = "name";
    private static final String NODE_NUMERIC = "numeric";
    private static final String NODE_MCC = "mcc";
    private static final String NODE_MNC = "mnc";
    private static final String NODE_APN = "apn";
    private static final String NODE_TYPE = "type";
    private static final String NODE_USER = "user";
    private static final String NODE_PASSWORD = "password";
    private static final String NODE_SERVER = "server";
    private static final String NODE_PROXY = "proxy";
    private static final String NODE_PORT = "port";
    private static final String NODE_DMAPN = "dmapn";

    /**
     * Information of APN for DM.
     *
     */
    public static class DmApnInfo {
        public Integer mId;
        public String mName;
        public String mNumeric;
        public String mMcc;
        public String mMnc;
        public String mApn;
        public String mUser;
        public String mPassword;
        public String mServer;
        public String mProxy;
        public String mPort;
        public String mType;
    }

    private ArrayList<DmApnInfo> mApnInfoList;
    private static int sRegisterSubId = -1;
    private Context mContext;
    private Builder mBuilder;
    private String mMccMncRegistered;

    public DmDatabase(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
    }

    /**
     * Update server address of database.
     *
     * @param subId
     *            current subscription ID
     * @param serverAddr
     *            The server address that to be updated
     * @return boolean Return the update result
     */
    public boolean updateDmServer(int subId, String serverAddr) {
        String mccMnc = mTelephonyManager.getSimOperator(subId);

        Log.i(TAG.DATABASE, new StringBuilder("The mccmnc is = ").append(mccMnc).append(", and the register mccmnc is ")
                .append(mMccMncRegistered).toString());
        if (mccMnc.equals(mMccMncRegistered) || serverAddr == null) {
            Log.e(TAG.DATABASE, "It is not the right mccmnc or server address is null!");
            return false;
        }
        Log.i(TAG.DATABASE,
                new StringBuilder("The subId id from intent is = ").append(subId).append(", and the register sub id is ")
                        .append(sRegisterSubId).toString());
        if (subId != sRegisterSubId || serverAddr == null) {
            Log.e(TAG.DATABASE, "It is not the right sim card or server address is null!");
            return false;
        }
        if (mContentResolver == null) {
            Log.e(TAG.DATABASE, "mContentResolver is null!!");
            return false;
        }

        Log.i(TAG.DATABASE, "before update");
        ContentValues v = new ContentValues();
        v.put(DB_SERVER, serverAddr);

        String where = DB_SUB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf(subId)};
        mContentResolver.update(DM_URI, v, where, selectionArgs);

        Log.i(TAG.DATABASE, "Update server addr finished");
        return true;
    }

    /**
     * Check whether the DM APN is already initiated in the database.
     *
     * @param subId current Subscription ID
     * @return boolean Return the check result
     */
    public boolean isDmApnReady(int subId) {
        int slotId = SubscriptionManager.getSlotId(subId);
        Log.d(TAG.DATABASE, "isDmApnReady(), subId: " + subId + " Slot ID: " + slotId);

        if (slotId != DmConst.GEMINI_SIM_1 && slotId != DmConst.GEMINI_SIM_2) {
            Log.e(TAG.DATABASE, "slotId = [" + slotId + "]is error! ");
            return false;
        }

        sRegisterSubId = subId;
        Cursor cursor = null;
        boolean ret = false;
        int count = 0;

        String where = DB_SUB_ID + " = ?";
        String[] selectionArgs = new String[] {String.valueOf(subId)};
        cursor = mContentResolver.query(DM_URI, null, where, selectionArgs, null);

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
            Log.i(TAG.DATABASE, "cursor count = " + ret);
            if (count > 0) {
                Log.w(TAG.DATABASE, "There are apn data in dm apn table, the record is " + count);
                return true;
            }
        }

        getApnValuesFromConfFile();
        ret = initDmApnTable(mApnInfoList, subId);
        if (!ret) {
            Log.e(TAG.DATABASE, "Init Apn table error!");
        }
        return ret;
    }

    /**
     * Parse config file to get the apn values.
     */
    private void getApnValuesFromConfFile() {
        String operatorName = mTelephonyManager.getSimOperatorNameForSubscription(sRegisterSubId);
        Log.i(TAG.DATABASE, "getApnValuesFromConfFile():operatorName = " + operatorName);

        File dmApnFile = new File(DmConst.PathName.DM_APN_INFO_FILE);
        if (dmApnFile == null || (!dmApnFile.exists())) {
            Log.e(TAG.DATABASE, "Apn file is not exists or dmApnFile is null");
            return;
        }
        DmXMLParser xmlParser = new DmXMLParser(DmConst.PathName.DM_APN_INFO_FILE);
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getChildNode(nodeList, NODE_DMAPN);
        if (nodeList != null && nodeList.size() > 0) {
            Log.i(TAG.DATABASE, "dmapn node list size = " + nodeList.size());
            operatorName = DmCommonFun.getOperatorName();
            if (operatorName == null) {
                Log.e(TAG.DATABASE, "Get operator name from config file is null");
                return;
            }
            Log.i(TAG.DATABASE, "Operator  = " + operatorName);
            Node node = nodeList.get(0);
            List<Node> operatorList = new ArrayList<Node>();
            xmlParser.getChildNode(node, operatorList, operatorName);
            int operatorSize = operatorList.size();
            Log.i(TAG.DATABASE, "OperatorList size  =  " + operatorSize);
            mApnInfoList = new ArrayList<DmApnInfo>(operatorSize);
            for (int i = 0; i < operatorSize; i++) {
                DmApnInfo mDmApnInfo = new DmApnInfo();
                Log.i(TAG.DATABASE, "this is the [" + i + "] operator apn");
                Node operatorNode = operatorList.get(i);
                List<Node> operatorLeafNodeList = new ArrayList<Node>();
                String nodeIdValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_ID);
                if (nodeIdValue != null) {
                    mDmApnInfo.mId = Integer.parseInt(nodeIdValue);
                }
                String nodeNameValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_NAME);
                if (nodeNameValue != null) {
                    mDmApnInfo.mName = nodeNameValue;
                }
                // numberic
                String nodeNumericValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_NUMERIC);
                if (nodeNumericValue != null) {
                    mDmApnInfo.mNumeric = nodeNumericValue;
                }
                // mcc
                String nodeMccValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_MCC);
                if (nodeMccValue != null) {
                    mDmApnInfo.mMcc = nodeMccValue;
                }
                // mnc
                String nodeMncValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_MNC);
                if (nodeMncValue != null) {
                    mDmApnInfo.mMnc = nodeMncValue;
                }
                // apn
                String nodeApnValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_APN);
                if (nodeApnValue != null) {
                    mDmApnInfo.mApn = nodeApnValue;
                }
                // type
                String nodeTypeValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_TYPE);
                if (nodeTypeValue != null) {
                    mDmApnInfo.mType = nodeTypeValue;
                }
                // user
                String nodeUserValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_USER);
                if (nodeUserValue != null) {
                    mDmApnInfo.mUser = nodeUserValue;
                }
                // password
                String nodePasswordValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_PASSWORD);
                if (nodePasswordValue != null) {
                    mDmApnInfo.mPassword = nodePasswordValue;
                }
                // server
                String nodeServerValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_SERVER);
                if (nodeServerValue != null) {
                    mDmApnInfo.mServer = nodeServerValue;
                }
                // proxy
                String nodeProxyValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_PROXY);
                if (nodeProxyValue != null) {
                    mDmApnInfo.mProxy = nodeProxyValue;
                }
                // port
                String nodePortValue = getLeafNodeValue(xmlParser, operatorNode, operatorLeafNodeList, NODE_PORT);
                if (nodePortValue != null) {
                    mDmApnInfo.mPort = nodePortValue;
                }
                Log.i(TAG.DATABASE,
                        new StringBuilder("Before add to array mDmApnInfo[").append(i).append("] = ").append(mDmApnInfo.mId)
                                .toString());
                mApnInfoList.add(mDmApnInfo);

            } // for(int i=0; i < operatorSize; i++)
        }
    }

    private String getLeafNodeValue(DmXMLParser xmlParser, Node operatorNode, List<Node> operatorLeafNodeList,
            String nodeKey) {
        xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, nodeKey);
        if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
            int operatorLeafSize = operatorLeafNodeList.size();
            Log.i(TAG.DATABASE,
                    new StringBuilder("OperatorLeafList size of").append(nodeKey).append(" =  ").append(operatorLeafSize)
                            .toString());
            String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild().getNodeValue();
            Log.i(TAG.DATABASE, new StringBuilder("node").append(nodeKey).append(" = ").append(nodeStr).toString());
            return nodeStr;
        } else {
            return null;
        }

    }

    // init the table if need
    private boolean initDmApnTable(ArrayList<DmApnInfo> apnInfoList, int subId) {
        Log.i(TAG.DATABASE, "Enter init Dm Apn Table");
        boolean ret = false;

        if (apnInfoList == null || apnInfoList.size() <= 0) {
            Log.e(TAG.DATABASE, "Apn that read from apn configure file is null");
            return false;
        }

        int size = apnInfoList.size();
        Log.i(TAG.DATABASE, "apnInfo size = " + size);
        ArrayList<ContentValues> apnInfoInsert = new ArrayList<ContentValues>(size);

        for (int i = 0; i < size; i++) {
            DmApnInfo apnInfo = apnInfoList.get(i);
            Log.i(TAG.DATABASE, "insert i = " + i);
            Log.i(TAG.DATABASE, "apnInfo.get(" + i + ").id = " + apnInfo.mId);
            ContentValues v = new ContentValues();
            if (apnInfo == null || apnInfo.mId == null) {
                Log.w(TAG.DATABASE, "before continue apnInfo.get.id " + apnInfo.mId);
                continue;
            }

            v.put(DB_ID, apnInfo.mId);
            if (apnInfo.mName != null) {
                v.put(DB_NAME, apnInfo.mName);
            }
            if (apnInfo.mNumeric != null) {
                v.put(DB_NUMERIC, apnInfo.mNumeric);
            }
            if (apnInfo.mMcc != null) {
                v.put(DB_MCC, apnInfo.mMcc);
            }
            if (apnInfo.mMnc != null) {
                v.put(DB_MNC, apnInfo.mMnc);
            }

            if (apnInfo.mApn != null) {
                v.put(DB_APN, apnInfo.mApn);
            }
            if (apnInfo.mType != null) {
                v.put(DB_TYPE, apnInfo.mType);
            }

            if (apnInfo.mUser != null) {
                v.put(DB_USER, apnInfo.mUser);
            }
            if (apnInfo.mServer != null) {
                v.put(DB_SERVER, apnInfo.mServer);
            }
            if (apnInfo.mPassword != null) {
                v.put(DB_PASSWORD, apnInfo.mPassword);
            }
            if (apnInfo.mProxy != null) {
                v.put(DB_PROXY, apnInfo.mProxy);
            }
            if (apnInfo.mPort != null) {
                v.put(DB_PORT, apnInfo.mPort);
            }
            v.put(DB_SUB_ID, subId);

            apnInfoInsert.add(v);

        }
        int insertSize = apnInfoInsert.size();
        Log.i(TAG.DATABASE, "insert size = " + insertSize);
        if (insertSize > 0) {
            mBuilder = DM_URI.buildUpon();

            ContentValues[] values = new ContentValues[insertSize];
            for (int i = 0; i < insertSize; i++) {
                Log.i(TAG.DATABASE, "insert to values i = [" + i + "]");
                values[i] = apnInfoInsert.get(i);
            }
            // bulk insert
            ret = (mContentResolver.bulkInsert(mBuilder.build(), values) == insertSize);
        }

        Log.i(TAG.DATABASE, "Init Dm database finish");
        return ret;
    }

    public String getApnProxyFromSettings() {
        String proxyAddr = null;
        String simOperator = null;

        if (sRegisterSubId == -1) {
            Log.e(TAG.DATABASE, "Get Register SIM ID error");
            return null;
        }

        Log.i(TAG.DATABASE, "SubId register = " + sRegisterSubId);

        simOperator = mTelephonyManager.getSimOperator(sRegisterSubId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(TAG.DATABASE, "Get sim operator wrong ");
            return DEFAULT_PROXY_ADDR;
        }

        String where = DB_NUMERIC + " = ?" + " AND " + DB_SUB_ID + " = ?";
        String[] selectionArgs = new String[] {simOperator, String.valueOf(sRegisterSubId)};
        mCursor = mContentResolver.query(DM_URI, null, where, selectionArgs, null);

        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return null;
        }
        mCursor.moveToFirst();
        int proxyAddrID = mCursor.getColumnIndex(DB_PROXY);
        proxyAddr = mCursor.getString(proxyAddrID);
        if (mCursor != null) {
            mCursor.close();
        }
        Log.i(TAG.DATABASE, "proxy address = " + proxyAddr);
        return proxyAddr;
    }

    public int getApnProxyPortFromSettings() {
        String port = null;
        String simOperator = null;

        if (sRegisterSubId == -1) {
            Log.e(TAG.DATABASE, "Get Register SIM ID error");
            return -1;
        }

        Log.i(TAG.DATABASE, "SubId register = " + sRegisterSubId);

        simOperator = mTelephonyManager.getSimOperator(sRegisterSubId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(TAG.DATABASE, "Get sim operator wrong");
            return DEFAULT_PROXY_PORT;
        }

        String where = DB_NUMERIC + " = ?" + " AND " + DB_SUB_ID + " = ?";
        String[] selectionArgs = new String[] {simOperator, String.valueOf(sRegisterSubId)};
        mCursor = mContentResolver.query(DM_URI, null, where, selectionArgs, null);

        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return -1;
        }
        mCursor.moveToFirst();
        // int serverAddrID = mCursor.getColumnIndex("server");
        int portId = mCursor.getColumnIndex(DB_PORT);
        port = mCursor.getString(portId);
        if (mCursor != null) {
            mCursor.close();
        }
        Log.i(TAG.DATABASE, "proxy port = " + port);
        // return lookupHost(serverAddr);
        return (Integer.parseInt(port));
    }

    public String getDmAddressFromSettings() {
        String serverAddr = null;
        String simOperator = null;

        if (sRegisterSubId == -1) {
            Log.e(TAG.DATABASE, "Get Register subscription ID error");
            return null;
        }

        Log.i(TAG.DATABASE, "SubId register = " + sRegisterSubId);

        simOperator = mTelephonyManager.getSimOperator(sRegisterSubId);
        Log.i(TAG.DATABASE, "simOperator numberic = " + simOperator);
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(TAG.DATABASE, "Get sim operator wrong");
            return null;
        }

        String where = DB_NUMERIC + " = ?" + " AND " + DB_SUB_ID + " = ?";
        String[] selectionArgs = new String[] {simOperator, String.valueOf(sRegisterSubId)};
        mCursor = mContentResolver.query(DM_URI, null, where, selectionArgs, null);

        if (mCursor == null || mCursor.getCount() <= 0) {
            Log.e(TAG.DATABASE, "Get cursor error or cursor is no record");
            if (mCursor != null) {
                mCursor.close();
            }
            return null;
        }
        mCursor.moveToFirst();
        int serverAddrID = mCursor.getColumnIndex(DB_SERVER);
        serverAddr = mCursor.getString(serverAddrID);
        if (mCursor != null) {
            mCursor.close();
        }
        Log.i(TAG.DATABASE, "server address = " + serverAddr);
        return serverAddr;
    }

}

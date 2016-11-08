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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmCommonFun;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.conn.DmDatabase;
import com.mediatek.dm.ext.MTKOptions;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.option.Options;

import java.util.ArrayList;

public class OmaCpReceiver extends BroadcastReceiver {

    // private static final String TAG = "OmaCpReceiver";
    public static final String APP_ID_KEY = "appId";
    public static final String DM_ID = "w7";
    private static final String CP_DM_SETTING_ACTION = "com.mediatek.omacp.settings";
    private static final String CP_DM_CAPABILITY_ACTION = "com.mediatek.omacp.capability";
    private static final String CP_DM_APP_SETTING_RESULT_ACTION = "com.mediatek.omacp.settings.result";
    private static final String CP_DM_CAPABILITY_RESULT_ACTION = "com.mediatek.omacp.capability.result";
    private static final String EXTRA_ADDR = "ADDR";
    private static final String EXTRA_SIM_ID = "simId";
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_DM = "dm";
    private static final String EXTRA_PROVIDER_ID = "dm_provider_id";
    private static final String EXTRA_SERVER_NAME = "dm_server_name";
    private static final String EXTRA_PROXY = "dm_to_proxy";
    private static final String EXTRA_NAPID = "dm_to_napid";
    private static final String EXTRA_SERVER_ADDR = "dm_server_address";
    private static final String EXTRA_ADDR_TYPE = "dm_addr_type";
    private static final String EXTRA_PORT_NO = "dm_port_number";
    private static final String EXTRA_AUTH_LEVEL = "dm_auth_level";
    private static final String EXTRA_AUTH_TYPE = "dm_auth_type";
    private static final String EXTRA_AUTH_NAME = "dm_auth_name";
    private static final String EXTRA_AUTH_SECRET = "dm_auth_secret";
    private static final String EXTRA_AUTH_DATA = "dm_auth_data";
    private static final String EXTRA_INIT = "dm_init";
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG.CP, "Receiver intent: " + intent);
        mContext = context;
        String intentAction = intent.getAction();
        String dmServerAddr = null;
        if (CP_DM_CAPABILITY_ACTION.equals(intentAction)) {
            Log.i(TAG.CP, "Receive cp config dm capability intent");
            handleCpCapabilityMessage(intent);
        } else if (CP_DM_SETTING_ACTION.equals(intentAction)) {
            Log.i(TAG.CP, "Receive cp config dm setting intent");
            if (MTKOptions.MTK_OMACP_SUPPORT) {
                Log.i(TAG.CP, "OMA CP in not supported by feature option");
            }

            int subId = intent.getIntExtra(MTKPhone.SUBSCRIPTION_KEY, 0);
            Log.i(TAG.CP, "Get server address array list from intent");
            ArrayList<String> dmAddr = intent.getStringArrayListExtra(EXTRA_ADDR);
            if (dmAddr != null) {
                Log.i(TAG.CP, "Get server address array list from intent");
                dmServerAddr = dmAddr.get(0);
            }

            if (TextUtils.isEmpty(dmServerAddr)) {
                Log.w(TAG.CP, "Get invalid form cp intent");
                return;
            }
            Log.i(TAG.CP, new StringBuilder("In receiver: server address = ").append(dmServerAddr)
                    .append(" subId = ").append(subId).toString());
            boolean result = handleCpConfigMessage(subId, dmServerAddr);
            Intent resultIntent = new Intent();
            resultIntent.setAction(CP_DM_APP_SETTING_RESULT_ACTION);
            resultIntent.putExtra(APP_ID_KEY, DM_ID);
            resultIntent.putExtra(EXTRA_RESULT, result);
            mContext.sendBroadcast(resultIntent);
            Log.i(TAG.CP, "send OMA CP config DM result intent");

        } else {
            Log.i(TAG.CP, "Normal intent.");
        }
    }

    private void handleCpCapabilityMessage(Intent intent) {
        Intent cpResultIntent = new Intent();
        cpResultIntent.setAction(CP_DM_CAPABILITY_RESULT_ACTION);
        cpResultIntent.putExtra(APP_ID_KEY, DM_ID);
        cpResultIntent.putExtra(EXTRA_DM, true);
        cpResultIntent.putExtra(EXTRA_PROVIDER_ID, false);
        cpResultIntent.putExtra(EXTRA_SERVER_NAME, false);
        cpResultIntent.putExtra(EXTRA_PROXY, false);
        cpResultIntent.putExtra(EXTRA_NAPID, false);
        cpResultIntent.putExtra(EXTRA_SERVER_ADDR, true);
        cpResultIntent.putExtra(EXTRA_ADDR_TYPE, false);
        cpResultIntent.putExtra(EXTRA_PORT_NO, false);
        cpResultIntent.putExtra(EXTRA_AUTH_LEVEL, false);
        cpResultIntent.putExtra(EXTRA_AUTH_TYPE, false);
        cpResultIntent.putExtra(EXTRA_AUTH_NAME, false);
        cpResultIntent.putExtra(EXTRA_AUTH_SECRET, false);
        cpResultIntent.putExtra(EXTRA_AUTH_DATA, false);
        cpResultIntent.putExtra(EXTRA_INIT, false);
        mContext.sendBroadcast(cpResultIntent);
        return;
    }

    private boolean handleCpConfigMessage(int subId, String serverAddr) {
        // TODO: need to config the dm server address in dm tree
        Log.i(TAG.CP,
                new StringBuilder("Enter config DmServer addr : server addr = ").append(serverAddr)
                        .append("subId id is ").append(subId).toString());
        if (serverAddr == null) {
            Log.e(TAG.CP, "server address is null");
            return false;
        }

        if (Options.USE_SMS_REGISTER) {
            int registerSubId = DmCommonFun.getRegisterSubID(mContext);
            if (registerSubId == -1 || subId != registerSubId) {
                Log.e(TAG.CP,
                        "sim card is not register OR cp sim card is not the register sim card.");
                return false;
            }
        }

        boolean ret = true;
        if (!Options.USE_DIRECT_INTERNET) {
            Log.w(TAG.CP, "---- handling CP config msg ----");
            DmDatabase mDb = new DmDatabase(mContext);
            if (!mDb.isDmApnReady(subId)) {
                Log.e(TAG.CP, "Initialize dm database error and can not insert data to dm table");
                return false;
            }
            ret = mDb.updateDmServer(subId, serverAddr);
        } else {
            Log.w(TAG.CP, "----skipped handling CP config msg----");
        }

        Log.i(TAG.CP, "Update dm tree in database [" + ret + "]");

        return ret;

    }
}

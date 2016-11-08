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

package com.mediatek.mediatekdm;

import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.conn.DmDatabase;
import com.mediatek.mediatekdm.mdm.MdmConfig;
import com.mediatek.mediatekdm.mdm.MdmConfig.DmAccConfiguration;
import com.mediatek.mediatekdm.mdm.MdmException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DmConfig {
    private static DmConfig sInstance;

    private final Context mContext;
    private final Properties mParamTable;
    private final String mConfigFile;

    public static final String KEY_DM_WBXML = "WBXML";
    public static final String KEY_DM_HEX_SID = "HexSID";
    public static final String KEY_MOBILE_DATA_ONLY = "MobileDataOnly";
    public static final String KEY_USE_SMS_REG = "UseSmsReg";
    public static final String KEY_DM_PROXY = "DmProxy";
    public static final String KEY_DL_PROXY = "DlProxy";
    public static final String KEY_RESTART_ON_LOCK = "RestartOnLock";
    public static final String KEY_OPERATOR = "Operator";
    public static final String KEY_SEQUENTIAL_NONCE = "SequentialNonce";
    public static final String KEY_COLLECT_SET_MSG_PERMISSION = "CollectSetPermission";

    public static synchronized DmConfig getInstance() {
        if (sInstance == null) {
            sInstance = new DmConfig(DmApplication.getInstance());
        }
        return sInstance;
    }

    private DmConfig(Context context) {
        mContext = context;
        String f = PlatformManager.getInstance().getPathInData(mContext,
                DmConst.Path.DM_CONFIG_FILE);
        mConfigFile = (new File(f).isFile()) ? f : PlatformManager.getInstance().getPathInSystem(
                DmConst.Path.DM_CONFIG_FILE);
        InputStream is = null;
        try {
            is = new FileInputStream(mConfigFile);
            mParamTable = new Properties();
            mParamTable.loadFromXML(is);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
    }

    public void configure() {
        try {
            MdmConfig config = new MdmConfig();
            if (!useMobileDataOnly()) {
                Log.i(TAG.COMMON, "[DMConfig] skip setting proxy for direct internet.");
            } else {
                Log.i(TAG.COMMON, "[DMConfig] setting proxy for WAP.");

                DmDatabase dmDB = new DmDatabase(mContext);
                dmDB.prepareApn();

                String proxyAddr = dmDB.getApnProxyFromSettings();
                int proxyPort = dmDB.getApnProxyPortFromSettings();
                Log.i(TAG.COMMON, "Proxy addr = " + proxyAddr + ", port = " + proxyPort);

                if (proxyAddr != null && proxyPort > 0) {
                    config.setDmProxy("http://" + proxyAddr + ":" + proxyPort);
                    config.setDlProxy("http://" + proxyAddr + ":" + proxyPort);
                } else {
                    Log.w(TAG.COMMON, "KEY_DM_PROXY not configed");
                }
            }

            config.setResendCommandInAuth(true);
            config.setMaxNetRetries(3);
            config.setDDVersionCheck(false);
            config.setNotificationVerificationMode(MdmConfig.NotifVerificationMode.DISABLED);

            DmAccConfiguration dmacc = config.new DmAccConfiguration();
            dmacc.activeAccountDmVersion = MdmConfig.DmVersion.DM_1_2;
            dmacc.dm12root = "./DMAcc/OMSAcc";
            dmacc.isExclusive = false;
            dmacc.updateInactiveDmAccount = false;
            config.setDmAccConfiguration(dmacc);

            if (mParamTable.containsKey(KEY_DM_HEX_SID)
                    && mParamTable.getProperty(KEY_DM_HEX_SID).equals("true")) {
                config.setSessionIDAsDec(false);
            } else {
                config.setSessionIDAsDec(true);
            }

            if (mParamTable.containsKey(KEY_DM_PROXY)) {
                String dmProxy = mParamTable.getProperty(KEY_DM_PROXY);
                config.setDmProxy(dmProxy);
            }

            if (mParamTable.containsKey(KEY_DL_PROXY)) {
                String dlProxy = mParamTable.getProperty(KEY_DL_PROXY);
                config.setDlProxy(dlProxy);
            }

            if (mParamTable.containsKey(KEY_DM_WBXML)
                    && mParamTable.getProperty(KEY_DM_WBXML).equals("true")) {
                Log.d(TAG.COMMON, "KEY_DM_WBXML to true");
                config.setEncodeWBXMLMsg(true);
            } else {
                Log.d(TAG.COMMON, "KEY_DM_WBXML to false");
                config.setEncodeWBXMLMsg(false);
            }

            if (mParamTable.containsKey(KEY_SEQUENTIAL_NONCE)
                    && mParamTable.getProperty(KEY_SEQUENTIAL_NONCE).equals("true")) {
                config.setUseSequentialNonce(true);
            } else {
                config.setUseSequentialNonce(false);
            }
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    public boolean useSmsReg() {
        if (mParamTable.containsKey(KEY_USE_SMS_REG)
                && mParamTable.getProperty(KEY_USE_SMS_REG).equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean useMobileDataOnly() {
        if (mParamTable.containsKey(KEY_MOBILE_DATA_ONLY)
                && mParamTable.getProperty(KEY_MOBILE_DATA_ONLY).equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean restartOnLock() {
        if (mParamTable.containsKey(KEY_RESTART_ON_LOCK)
                && mParamTable.getProperty(KEY_RESTART_ON_LOCK).equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public String getCustomizedOperator() {
        return mParamTable.getProperty(KEY_OPERATOR);
    }

    public boolean getCollectSetMsgPermission() {
        if (mParamTable.containsKey(KEY_COLLECT_SET_MSG_PERMISSION)
                && mParamTable.getProperty(KEY_COLLECT_SET_MSG_PERMISSION).equals("true")) {
            return true;
        } else {
            return false;
        }
    }
}

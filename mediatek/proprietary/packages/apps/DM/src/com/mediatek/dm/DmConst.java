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

import com.redbend.vdm.lawmo.LawmoState;


public final class DmConst {

    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    public static final int[] GEMINI_SIMS = { GEMINI_SIM_1, GEMINI_SIM_2 };

    public static final class TAG {
        public static final String LOG_TAG_PREFIX = "DM/";
        public static final String APPLICATION = LOG_TAG_PREFIX + "Application";
        public static final String CLIENT = LOG_TAG_PREFIX + "Client";
        public static final String SERVICE = LOG_TAG_PREFIX + "Service";
        public static final String RECEIVER = LOG_TAG_PREFIX + "Receiver";
        public static final String COMMON = LOG_TAG_PREFIX + "Common";
        public static final String CONTROLLER = LOG_TAG_PREFIX + "Controller";
        public static final String DATABASE = LOG_TAG_PREFIX + "Database";
        public static final String CONNECTION = LOG_TAG_PREFIX + "Connection";
        public static final String XML = LOG_TAG_PREFIX + "XML";
        public static final String NOTIFICATION = LOG_TAG_PREFIX + "Notification";
        public static final String NODE_IO_HANDLER = LOG_TAG_PREFIX + "NodeIOHandler";
        public static final String SESSION = LOG_TAG_PREFIX + "Session";
        public static final String PL = LOG_TAG_PREFIX + "PL";
        public static final String BOOTSTRAP = LOG_TAG_PREFIX + "Bootstrap";
        public static final String CP = LOG_TAG_PREFIX + "CP";
        public static final String FUMO = LOG_TAG_PREFIX + "Fumo";
        public static final String LAWMO = LOG_TAG_PREFIX + "Lawmo";
        public static final String MMI = LOG_TAG_PREFIX + "MMI";
        public static final String SCOMO = LOG_TAG_PREFIX + "Scomo";
        public static final String SMSREG = LOG_TAG_PREFIX + "SmsReg";
        public static final String DEBUG = LOG_TAG_PREFIX + "Debug";
        public static final String POLLING = LOG_TAG_PREFIX + "Polling";
    }

    public static final class NodeName {
        public static final String SETTING = "Setting";
        public static final String IMSMO = "IMSMO";
        public static final String XDMMO = "XDMMO";
        public static final String NAME = "name";
        public static final String VALUE = "value";
        public static final String NODE = "node";
        public static final String LEAF = "leaf";
        public static final String TEXT = "text";
        public static final String TIMING = "timing";
    }

    public static final class PathName {
        public static final String PATH_IN_SYSTEM = "/system/etc/dm";
        public static final String PATH_IN_DATA = "/data/data/com.mediatek.dm/files";
        public static final String REMINDER_FILE = PATH_IN_SYSTEM + "/reminder.xml";
        public static final String TREE_FILE_IN_DATA = PATH_IN_DATA + "/tree.xml";
        public static final String TREE_FILE_IN_SYSTEM = PATH_IN_SYSTEM + "/tree.xml";
        public static final String CONFIG_FILE_IN_SYSTEM = PATH_IN_SYSTEM + "/vdmconfig.xml";
        public static final String CONFIG_FILE_IN_DATA = PATH_IN_DATA + "/vdmconfig.xml";
        public static final String UPDATE_RESULT_FILE = "/cache/recovery/updateResult";
        public static final String NIA_FILE = "/data/data/com.mediatek.dm/files/NIA";
        public static final String WIPE_FILE = "/data/data/com.mediatek.dm/wipe";
        public static final String DM_VALUES_FILE = PATH_IN_DATA + "/dm_values.xml";
        public static final String DELTA_ZIP_FILE = PATH_IN_DATA + "/delta.zip";
        public static final String DL_RESUME_DAT_FILE = PATH_IN_DATA + "/dlresume.dat";
        public static final String DM_APN_INFO_FILE = "/system/etc/dm/DmApnInfo.xml";
        public static final String FOTA_EXECUTING_FILE = PATH_IN_DATA + "/fota_executing";
    }

    public static final class OperatorName {
        public static final String CU = "cu";
        public static final String CMCC = "cmcc";
    }

    public static final class NodeUri {
        public static final String FUMO_ROOT = "./FwUpdate";
        public static final String LAWMO_ROOT = "./LAWMO";
        public static final String SCOMO_ROOT = "./SCOMO";
        public static final String DMACC_SERVER_AUTH_DATA = "./DMAcc/OMSAcc/AppAuth/serverAuth/AAuthData";
        public static final String DMACC_CLIENT_AUTH_DATA = "./DMAcc/OMSAcc/AppAuth/clientAuth/AAuthData";
        public static final String DEV_DETAIL_SWV = "./DevDetail/SwV";
        public static final String DEV_INFO_DEVID = "./DevInfo/DevId";
        public static final String FUMO_EXT_SEVERITY = "./FwUpdate/Ext/Severity";
        public static final String FUMO_EXT_URL = "./FwUpdate/Ext/URL";
        public static final String FUMO_EXT_POSTPONE = "./FwUpdate/Ext/Postpone";
        public static final String FUMO_EXT_POLLFREQUENCY = "./FwUpdate/Ext/POLLFrequency";
        public static final String CON_BEARER = "./Con/NAP/Bearer";
        public static final String CON_ADDR = "./Con/NAP/Addr";
        public static final String CON_NAME = "./Con/NAP/Name";
        public static final String CON_USERNAME = "./Con/NAP/UserName";
        public static final String CON_PASSWORD = "./Con/NAP/Password";
        public static final String CON_AUTHTYPE = "../Con/NAP/AuthType";
        public static final String CON_PROXYADDR = "./Con/NAP/ProxyAddr";
        public static final String CON_PROXYPORT = "./Con/NAP/ProxyPort";
    }

    public static final class Time {
        public static final int ONESECOND = 1;
        public static final int ONEMINUTE = 60 * ONESECOND;
        public static final int TIMEOUTVAL = 1 * ONEMINUTE;
    }

    public static final class IntentAction {
        public static final String DM_WAP_PUSH = "android.provider.Telephony.WAP_PUSH_RECEIVED";
        public static final String DM_BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
        public static final String DM_REMINDER = "com.mediatek.dm.REMINDER";
        public static final String DM_DL_FOREGROUND = "com.mediatek.dm.DMDOWNLOADINGFOREGROUND";
        public static final String DM_SWUPDATE = "com.mediatek.DMSWUPDATE";
        public static final String DM_CLIENT = "com.mediatek.dm.DMCLIENT";
        public static final String PROXY_CHANGED = "android.intent.action.PROXY_CHANGE";
        public static final String NIA_RECEIVED = "com.mediatek.dm.NIA_RECEIVED";
        public static final String DM_START = "com.mediatek.dm.DM_STARTED";
        public static final String DM_NIA_START = "com.mediatek.dm.DM_NIA_START";
        public static final String NET_DETECT_TIMEOUT = "com.mediatek.dm.NETDETECTTIMEOUT";
        public static final String DM_FACTORYSET = "com.mediatek.dm.FACTORYSET";
        public static final String ACTION_DM_SERVE = "com.mediatek.dm.DMSERVE";
        public static final String DM_CLOSE_DIALOG = "com.mediatek.dm.CLOSE_DIALOG";

        public static final String ACTION_REBOOT_CHECK = "com.mediatek.dm.REBOOT_CHECK";
        public static final String ACTION_LAWMO_LOCK = "com.mediatek.dm.LAWMO_LOCK";
        public static final String ACTION_LAWMO_UNLOCK = "com.mediatek.dm.LAWMO_UNLOCK";
        public static final String ACTION_LAWMO_WIPE = "com.mediatek.dm.LAWMO_WIPE";
        public static final String ACTION_FUMO_CI = "com.mediatek.dm.fumo_ci";
        public static final String ACTION_POLLING = "com.mediatek.dm.polling_action";
    }

    public static final class IntentType {
        public static final String DM_NIA = "application/vnd.syncml.notification";
        public static final String BOOTSTRAP_NIA = "application/vnd.syncml.dm+wbxml";
        public static final String BOOTSTRAP_CP = "application/vnd.wap.connectivity-wbxml";
    }

    public static final class SessionInitiatorId {
        public static final String INITIATOR_CP_BOOTSTRAP = "CP_Bootstrap";
        public static final String INITIATOR_DM_BOOTSTRAP = "DM_Bootstrap";
        public static final String INITIATOR_SCOMO = "VDM_SCOMO";
        public static final String INITIATOR_FUMO = "VDM_FUMO";
        public static final String INITIATOR_LAWMO = "VDM_LAWMO";
        public static final String INITIATOR_NETWORK = "Network Inited";
        public static final String INITIATOR_CI = "ci_initiator";

    }

    public static final class LawmoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class LawmoStatus {
        public static final String LAWMO_URI = "./LAWMO/State";
        public static final int FULLY_LOCK = LawmoState.FULLY_LOCKED.val();
        public static final int PARTIALY_LOCK = LawmoState.PARTIALLY_LOCKED.val();
        public static final int UN_LOCK = LawmoState.UNLOCKED.val();
    }

    public static final class FumoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class ServerMessage {
        public static final int TYPE_ALERT_1100 = 1;
        public static final int TYPE_ALERT_1101 = 2;
        public static final int TYPE_ALERT_1103_1104 = 3;
        public static final int TYPE_UIMODE_VISIBLE = 4;
        public static final int TYPE_UIMODE_INTERACT = 5;
        public static final int TYPE_ALERT_1102 = 6;
    }

    public static final String MCC_460 = "460"; // net number
    public static final String MNC_00 = "00"; // for cmcc
    public static final String MNC_02 = "02"; // for cmcc
    public static final String MNC_07 = "07"; // for cmcc
    public static final String MNC_01 = "01"; // for cu

}

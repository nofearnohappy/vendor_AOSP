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

package com.mediatek.configurecheck;

import com.mediatek.configurecheck.R;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneConstants;

import android.content.Context;
import android.os.SystemProperties;

public class ProviderFactory {
    /**
     * @Prompt Add your provider in this provider factory
     */
    public static CheckItemProviderBase getProvider(Context c, String category) {

        if (category.equals(c.getString(R.string.one_button_check))) {
            return new OneBtnCheckProvider(c);
        } else if (category.equals(c.getString(R.string.NFC_SWP_HCI))) {
            return new NFCSWPHCICheckProvider(c);
        } else if (category.equals(c.getString(R.string.NFC_SECURITY))) {
            return new NFCSecurityCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_MEID))) {
            return new LTEMeidCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_Hybrid))) {
            return new LTEHybirdCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_EVDO_SC))) {
            return new LTEEVDOSCCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_EVDO_Rev))) {
            return new LTEEVDORevCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_EVDO_RF))) {
            return new LTEEVDORFCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_eHRPD))) {
            return new CTeHRPDCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_CrossAction))) {
            return new CTLTECrossActionCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_Sprirent))) {
            return new CTLTESprirentCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_SVLTE))) {
            return new CTSVLTECheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_SRLTE))) {
            return new CTSRLTECheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_RAndS))) {
            return new CTLTERAndSCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_RF_RRM))) {
            return new CTLTERFRRMCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_PCT_RAndS))) {
            return new CTLTEPCTRAndSCheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_PCT_ANITE))) {
            return new CTLTEPCTANITECheckProvider(c);
        } else if (category.equals(c.getString(R.string.CT_LTE_InterRoming))) {
            return new CTLTEInterRomingCheckProvider(c);
        } else if (category.equals(c.getString(R.string.NFC))) {
            return new NFCCheckProvider(c);
        } else {
            throw new RuntimeException("No such category !!! ");
        }
    }
}

/*
 * Add check item key here !
 * CI_[item name]_[characteristic]
 */
final class CheckItemKeySet {

    public static final String CI_CTAFTA_UNLOCK_SIM = "unlocktestsim";
    public static final String CI_CTAFTA_UNLOCK_SPIRENT = "unlockandspirent";
    public static final String CI_CTAFTA_UNLOCK_FTA = "unlockandFTA";
    public static final String CI_CTAFTA_UNLOCK_RS = "unlockandCMW500";
    public static final String CI_CTAFTA_UNLOCK_ANITE = "unlockandANITE";
    public static final String CI_CTAFTA_SPIRENT = "spirentonly";
    public static final String CI_CTAFTA_FTA = "FTA";
    public static final String CI_CTAFTA_RS = "CMW500";
    public static final String CI_CTAFTA_ANITE = "ANITE";

    public static final String CI_DATA_CONNECT_OFF = "DataConnectOff";
    public static final String CI_DATA_CONNECT_ON = "DataConnectOn";
    public static final String CI_DATA_CONNECT_ON_CHECK = "DataConnectOnCheck";

    public static final String CI_RESTORE = "Restore";
    public static final String CI_IMEI_IN_DM = "IMEIInDm";
    public static final String CI_IMEI = "IMEI";
    public static final String CI_DMSTATE_CHECK_ON_ONLY = "DMCheckOnly";
    public static final String CI_DMSTATE_ON_CONFG = "DMAutoConfgOn";
    public static final String CI_DMSTATE_OFF_CONFG = "DMAutoConfgOff";
    public static final String CI_BATTERY = "Battery";
    public static final String CI_BUILD_TYPE = "BuildType";
    public static final String CI_SCREEN_ON_UNLOCK = "checkscreenonunlock";

    public static final String CI_MISCONFIG_ON_CHECK = "MisConfigonCheck";
    public static final String CI_MISCONFIG_ON_CONFIG = "MisConfigonConfig";
    public static final String CI_MISCONFIG_OFF_CONFIG = "MisConfigoffConfig";

    public static final String CI_TARGET_VERSION = "TargetVersion";
    public static final String CI_TARGET_MODE = "TargetMode";
    public static final String CI_SERIAL_NUMBER = "SerialNumber";

    public static final String CI_CALIBRATION_DATA = "CalibrationData";
    public static final String CI_WLAN_SSID = "WlanSSID";
    public static final String CI_WLAN_MAC_ADDR = "WlanMacAddress";

    public static final String CI_LTE_ONLY_CHECK = "LteOnlyCheck";
    public static final String CI_LTE_ONLY_CONFIG = "LteOnly";
    public static final String CI_4G_3G_2G_Auto_Check = "4G3G2GAutoCheck";
    public static final String CI_4G_3G_2G_Auto = "4G3G2GAuto";
    public static final String CI_APN_CTLTE = "Apnctlte";
    public static final String CI_APN_RSAPN = "rsapn";
    public static final String CI_APN_CTNET = "ctnet";

    public static final String CI_USB_SHARE_NET_Config = "UsbShareNetConfig";
    public static final String CI_USB_SHARE_NET_AT_Config = "UsbShareNetATConfig";
    public static final String CI_USB_SHARE_NET_HOT_Config = "UsbShareNetHotConfig";

    public static final String CI_CFU_OFF_CONFIG = "CFUConfigOff";
    public static final String CI_NFC_ON = "NfcOn";
    public static final String CI_NFC_DTA_CONFIG = "NfcDTA";

    //add for CT
    public static final String CI_PS_SENSOR = "PSSensor";
    public static final String CI_GSM_TD_AUTO = "GSM/TD-SCDMA(auto)";
    public static final String CI_IMEID = "IMEID";
    public static final String CI_BT_CHECK_ONLY = "BTCheckOnly";
    public static final String CI_BT_ADDR = "BTAddr";
    public static final String CI_PHONE_MODE = "Phonemode";
    public static final String CI_HW_MODE = "HWMode";
    public static final String CI_SW_MODE = "SWMode";

    public static final String CI_IR_LWG = "LTEWCDMAGSMOnly";
    public static final String CI_IR_FTA_LTE = "FTALTEOnly";
    public static final String CI_IR_CDMA = "CDMA";
    public static final String CI_IR_WCDMA_INTEGRITY_CHECK = "WCDMAIntegrityCheck";
    public static final String CI_DORMANT_TIMER = "DormantTimer";
    public static final String CI_ATCI_IN_EM_ENABLE = "atciemenable";
    public static final String CI_ATCI = "atci";
    public static final String CI_USER2ROOT_ROOT = "User2RootConfigRoot";
    public static final String CI_TIME_OFF_CONFIG = "TimeOffConfig";
    public static final String CI_TIMEZONE_OFF_CONFIG = "TimeZoneOffConfig";

    public static final String CI_BG_DATA_SELECT = "bgdataselect";
}

/**
 * @Prompt Add your owner provider below, then put it into ProviderFactory
 */
class OneBtnCheckProvider extends CheckItemProviderBase {
    OneBtnCheckProvider(Context c) {
        mArrayItems.add(new CheckBuildType(c, CheckItemKeySet.CI_BUILD_TYPE));
        mArrayItems.add(new CheckBattery(c, CheckItemKeySet.CI_BATTERY));
        mArrayItems.add(new CheckPSSensor(c, CheckItemKeySet.CI_PS_SENSOR));
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto_Check));
        mArrayItems.add(new CheckCalData(c, CheckItemKeySet.CI_CALIBRATION_DATA));
        mArrayItems.add(new CheckRestore(c, CheckItemKeySet.CI_RESTORE));
        mArrayItems.add(new CheckTargetMode(c, CheckItemKeySet.CI_TARGET_MODE));
        mArrayItems.add(new CheckTargetVersion(c, CheckItemKeySet.CI_TARGET_VERSION));
        mArrayItems.add(new CheckIMEI(c, CheckItemKeySet.CI_IMEI));
        mArrayItems.add(new CheckIMEID(c, CheckItemKeySet.CI_IMEID));
        mArrayItems.add(new CheckSN(c, CheckItemKeySet.CI_SERIAL_NUMBER));
        mArrayItems.add(new CheckBT(c, CheckItemKeySet.CI_BT_CHECK_ONLY));
        mArrayItems.add(new CheckBTMacAddr(c, CheckItemKeySet.CI_BT_ADDR));
        mArrayItems.add(new CheckWlanSSID(c, CheckItemKeySet.CI_WLAN_SSID));
        mArrayItems.add(new CheckWlanMacAddr(c, CheckItemKeySet.CI_WLAN_MAC_ADDR));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CHECK));
        mArrayItems.add(new CheckPhoneMode(c, CheckItemKeySet.CI_PHONE_MODE));
        mArrayItems.add(new CheckHWMode(c, CheckItemKeySet.CI_HW_MODE));
        mArrayItems.add(new CheckSWMode(c, CheckItemKeySet.CI_SW_MODE));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_CHECK_ON_ONLY));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_ON_CHECK));
    }
}


class NFCSWPHCICheckProvider extends CheckItemProviderBase {
    NFCSWPHCICheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SIM));
            mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_LWG));
        }
        mArrayItems.add(new CheckNFC(c, CheckItemKeySet.CI_NFC_ON));
        mArrayItems.add(new CheckNFCDTA(c, CheckItemKeySet.CI_NFC_DTA_CONFIG));
    }
}


class NFCSecurityCheckProvider extends CheckItemProviderBase {
    NFCSecurityCheckProvider(Context c) {
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
    }
}

class LTEMeidCheckProvider extends CheckItemProviderBase {
    LTEMeidCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT));
        }
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDormainTimer(c, CheckItemKeySet.CI_DORMANT_TIMER));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class LTEHybirdCheckProvider extends CheckItemProviderBase {
    LTEHybirdCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT));
        }
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDormainTimer(c, CheckItemKeySet.CI_DORMANT_TIMER));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class LTEEVDOSCCheckProvider extends CheckItemProviderBase {
    LTEEVDOSCCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT));
        }
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDormainTimer(c, CheckItemKeySet.CI_DORMANT_TIMER));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class LTEEVDORevCheckProvider extends CheckItemProviderBase {
    LTEEVDORevCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT));
        }
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDormainTimer(c, CheckItemKeySet.CI_DORMANT_TIMER));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));

    }
}

class LTEEVDORFCheckProvider extends CheckItemProviderBase {
    LTEEVDORFCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SPIRENT));
        }
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class CTeHRPDCheckProvider extends CheckItemProviderBase {
    CTeHRPDCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_AT_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckBgDataSelect(c, CheckItemKeySet.CI_BG_DATA_SELECT));
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_CDMA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIME_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG));
    }
}

class CTLTECrossActionCheckProvider extends CheckItemProviderBase {
    CTLTECrossActionCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_AT_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckBgDataSelect(c, CheckItemKeySet.CI_BG_DATA_SELECT));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIME_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG));
    }
}

class CTSRLTECheckProvider extends CheckItemProviderBase {
    CTSRLTECheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_AT_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckBgDataSelect(c, CheckItemKeySet.CI_BG_DATA_SELECT));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIME_OFF_CONFIG));
        mArrayItems.add(new CheckTimeandTimeZone(c, CheckItemKeySet.CI_TIMEZONE_OFF_CONFIG));
    }
}

class CTLTESprirentCheckProvider extends CheckItemProviderBase {
    CTLTESprirentCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_AT_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CTNET));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class CTSVLTECheckProvider extends CheckItemProviderBase {
    CTSVLTECheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_AT_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CTNET));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_SPIRENT));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
    }
}

class CTLTERAndSCheckProvider extends CheckItemProviderBase {
    CTLTERAndSCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_HOT_Config));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_RSAPN));
        if (SystemProperties.get("ro.ct6m_support").equals("1") == true) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_RS));
        } else {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_RS));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_LWG));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
    }
}

class CTLTERFRRMCheckProvider extends CheckItemProviderBase {
    CTLTERFRRMCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_FTA));
        } else {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_FTA));
        }
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_ONLY_CONFIG));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
    }
}

class CTLTEPCTRAndSCheckProvider extends CheckItemProviderBase {
    CTLTEPCTRAndSCheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_RS));
        } else {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_RS));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_FTA_LTE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
    }
}

class CTLTEPCTANITECheckProvider extends CheckItemProviderBase {
    CTLTEPCTANITECheckProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_ANITE));
        } else {
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_ANITE));
        }
        mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_FTA_LTE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
    }
}

class CTLTEInterRomingCheckProvider extends CheckItemProviderBase {
    CTLTEInterRomingCheckProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_OFF_CONFG));
        mArrayItems.add(new CheckMisConfig(c, CheckItemKeySet.CI_MISCONFIG_OFF_CONFIG));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_UNLOCK));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_OFF_CONFIG));
        if("MT6753".equals(SystemProperties.get("ro.mediatek.platform")) ||
          "MT6735".equals(SystemProperties.get("ro.mediatek.platform")) ||
          "MT6735m".equals(SystemProperties.get("ro.mediatek.platform"))) {
            mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_WCDMA_INTEGRITY_CHECK));
        }
    }
}

class NFCCheckProvider extends CheckItemProviderBase {
    NFCCheckProvider(Context c) {
        if("OP09".equals(SystemProperties.get("ro.operator.optr"))) {
            mArrayItems.add(new CheckIRsetting(c, CheckItemKeySet.CI_IR_LWG));
            mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_UNLOCK_SIM));
        }
        mArrayItems.add(new CheckNFCDTA(c, CheckItemKeySet.CI_NFC_DTA_CONFIG));
        mArrayItems.add(new CheckNFC(c, CheckItemKeySet.CI_NFC_ON));
    }
}



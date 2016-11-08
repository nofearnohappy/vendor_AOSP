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

package com.mediatek.configurecheck2;

import com.mediatek.configurecheck2.R;
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

        } else if (category.equals(c.getString(R.string.wc_ttcn_3g_6210))) {
            return new TTCN3GProvider(c);

        } else if (category.equals(c.getString(R.string.wc_anritsu_23g_8470))) {
            return new Anritsu23GProvider(c);

        } else if (category.equals(c.getString(R.string.wc_ttcn_23g_6210))) {
            return new TTCN23GProvider(c);

        } else if (category.equals(c.getString(R.string.wc_rrm_6200))) {
            return new RRMProvider(c);

        } else if (category.equals(c.getString(R.string.wc_rf_6010_8960))) {
            return new RFProvider(c);

        } else if (category.equals(c.getString(R.string.wc_network_iot))) {
            return new NetworkIOTProvider(c);

        } else if (category.equals(c.getString(R.string.wc_basic_communication))) {
            return new BasicCommunProvider(c);

        } else if (category.equals(c.getString(R.string.wc_phone_card_compatible))) {
            return new PhoneCardCompatProvider(c);

        } else if (category.equals(c.getString(R.string.sa_local_func))) {
            return new LocalFuncProvider(c);

        } else if (category.equals(c.getString(R.string.sa_broadband_internet))) {
            return new BroadbandNetProvider(c);

        } else if (category.equals(c.getString(R.string.sa_video_tel))) {
            return new VideoTelProvider(c);

        } else if (category.equals(c.getString(R.string.sa_streaming))) {
            return new StreamingProvider(c);

        //} else if (category.equals(c.getString(R.string.sa_dm_lab_net))) {
        //    return new DMLabNetProvider(c);

        //} else if (category.equals(c.getString(R.string.sa_dm_real_net))) {
         //   return new DMRealNetProvider(c);

        //} else if (category.equals(c.getString(R.string.sa_mobile_tv))) {
            //return new MobileTVProvider(c);

        } else if (category.equals(c.getString(R.string.sa_agps))) {
            return new AgpsProvider(c);

        } else if (category.equals(c.getString(R.string.sa_wlan))) {
            return new WlanProvider(c);

        } else if (category.equals(c.getString(R.string.sr_mtbf))) {
            return new MTBFProvider(c);

        } else if (category.equals(c.getString(R.string.sr_local_perf))) {
            return new LocalPerfProvider(c);

        } else if (category.equals(c.getString(R.string.hr_power_consumption))) {
            return new PowerConsumpProvider(c);

        } else if (category.equals(c.getString(R.string.fd_field_test))) {
            return new FieldTestProvider(c);

        } else if (category.equals(c.getString(R.string.MTBF))) {
            return new MTKMTBFProvider(c);

        } else if (category.equals(c.getString(R.string.tds_pro_con_ttcn_23g))) {
            return new TdsTTCN23gProvider(c);

        } else if (category.equals(c.getString(R.string.tds_pro_con_anritsu_23g))) {
            return new TdsAnri23gProvider(c);

        } else if (category.equals(c.getString(R.string.tds_rrm_conformance))) {
            return new TdsRrmConProvider(c);

        } else if (category.equals(c.getString(R.string.tds_pro_con_ttcn))) {
            return new TdsProConTtcnProvider(c);

        } else if (category.equals(c.getString(R.string.tds_rf_conformance))) {
            return new TdsRfConProvider(c);

        } else if (category.equals(c.getString(R.string.lte_ns_iot_agilent_case_833))) {
            return new TdlNsIotAgiDataOnProvider(c);

        } else if (category.equals(c.getString(R.string.lte_ns_iot_agilent_case_831_837))) {
            return new TdlNsIotAgiDataOffProvider(c);

        } else if (category.equals(c.getString(R.string.lte_ns_iot_anite))
                || category.equals(c.getString(R.string.lte_ns_iot_ani_not_csfb))) {
            return new TdlNsIotAniteProvider(c);

        } else if (category.equals(c.getString(R.string.lte_ns_iot_RAndS))) {
            return new TdlNsIotRAndSProvider(c);

        } else if (category.equals(c.getString(R.string.lte_ns_iot_ani_csfb))) {
            return new TdlNsIotAniCsfbProvider(c);

        } else if (category.equals(c.getString(R.string.lte_nv_iot))) {
            return new TdlNvIotProvider(c);

        } else if (category.equals(c.getString(R.string.lte_usim_tdd_bip))) {
            return new TdlUsimTddBipProvider(c);

        } else if (category.equals(c.getString(R.string.lte_usim_tdd_not_bip))) {
            return new TdlUsimTddNotBipProvider(c);

        } else if (category.equals(c.getString(R.string.lte_usim_fdd_bip))) {
            return new TdlUsimFddBipProvider(c);

        } else if (category.equals(c.getString(R.string.lte_usim_fdd_not_bip))) {
            return new TdlUsimFddNotBipProvider(c);

        }/* else if (category.equals(c.getString(R.string.lte_rrm_tdd))) {
            return new TdlRrmTddProvider(c);

        } else if (category.equals(c.getString(R.string.lte_rrm_fdd_fddtdd))) {
            return new TdlRrmFddFddTddProvider(c);

        }*/ else if (category.equals(c.getString(R.string.lte_network_iot))) {
            return new TdlNwIotProvider(c);

        } else if (category.equals(c.getString(R.string.lte_pct_rrm_rf_tdd))) {
            return new TdlPctTddProvider(c);

        } else if (category.equals(c.getString(R.string.lte_pct_rrm_rf_fdd_fddtdd))) {
            return new TdlPctFddFddTddProvider(c);

        }/* else if (category.equals(c.getString(R.string.lte_rf_fdd_fddtdd))) {
            return new TdlRfFddFddTddProvider(c);

        } else if (category.equals(c.getString(R.string.lte_rf_tdd))) {
            return new TdlRfTddProvider(c);

        }*/ else if (category.equals(c.getString(R.string.lte_IPv6_2g_case))) {
            return new TdlIPv62gCaseProvider(c);

        } else if (category.equals(c.getString(R.string.lte_IPv6_3g_case))) {
            return new TdlIPv63gCaseProvider(c);

        } else if (category.equals(c.getString(R.string.lte_IPv6_4g_case))) {
            return new TdlIPv64gCaseProvider(c);

        } else if (category.equals(c.getString(R.string.internal_roaming))) {
            return new TdlInternalRoamingCaseProvider(c);

        } else if (category.equals(c.getString(R.string.HK_FT_labtest))) {
            return new HKFTLabCaseProvider(c);

        } else if (category.equals(c.getString(R.string.LTE_internal_roaming_Anite))) {
            return new LTEInternalRoamingAniteCaseProvider(c);

        } else if (category.equals(c.getString(R.string.LTE_internal_roaming_Anritsu))) {
            return new LTEInternalRoamingAnritsuCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nsiot_csfb_volte_interrat))) {
            return new NSIOTCsfbVolteInterRatCaseProvider(c);

        }  else if (category.equals(c.getString(R.string.nsiot_tdl_tds_roaming))) {
            return new NSIOTTDLTDSRoamingCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nsiot_other))) {
            return new NSIOTOtherCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nsiot_agilent_pwrc))) {
            return new NSIOTAgilentPwrcCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nsiot_inter_roaming))) {
            return new NSIOTInterRoamingRoamingCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nviot_csfb))) {
            return new NVIOTCSFBCaseProvider(c);

        } else if (category.equals(c.getString(R.string.nviot_volte))) {
            return new NVIOTVOLTECaseProvider(c);

        } else if (category.equals(c.getString(R.string.nviot_ca))) {
            return new NVIOTCACaseProvider(c);

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
    public static final String CI_GPRS_CHECK_ONLY = "GPRSCheckOnly";
    public static final String CI_GPRS_AUTO_CONFG = "GPRSConfgOn";

    public static final String CI_APN = "APNConfg";

    // yaling
    public static final String CI_TDWCDMA_ONLY = "TDWCDMAOnly";
    public static final String CI_GSM_ONLY_CONFIG = "GSMOnlyConfig";
    public static final String CI_TDWCDMA_ONLY_CONFIG = "TDWCDMAOnlyConfig";
    public static final String CI_TDWCDMA_PREFERED_CONFIG = "TDWCDMAPreferredConfig";
    public static final String CI_DUAL_MODE_CONFIG = "DualModeConfig";
    public static final String CI_DUAL_MODE_CHECK = "DualModeCheck";
    public static final String CI_GPRS_ON = "GPRSOn";
    public static final String CI_GPRS_CONFIG = "GPRSalwaysattachConfig";
    public static final String CI_GPRS_ATTACH_CONTINUE = "GPRS always attach continue";
    public static final String CI_GPRS_ATTACH_CHECK = "GPRS always attach continue check";
    public static final String CI_CFU = "CFUcheck";
    public static final String CI_CFU_CONFIG = "CFUConfigOff";
    public static final String CI_CTAFTA = "CTAFTAcheck";
    public static final String CI_CTAFTA_CONFIG_OFF = "CTAFTAConfigOff";
    public static final String CI_CTAFTA_CONFIG_ON = "CTAFTAConfigIntegrityCheckOn";
    public static final String CI_DATA_CONNECT_CHECK = "DataConnectCheck";
    public static final String CI_DATA_CONNECT_OFF = "DataConnectOff";
    public static final String CI_DATA_CONNECT_OFF_CONFIG = "DataConnectOffConfig";
    public static final String CI_DATA_CONNECT_ON = "DataConnectOn";
    public static final String CI_DATA_CONNECT_ON_DM = "DataConnectOnDm";
    public static final String CI_PLMN_DEFAULT = "PLMNDefault";
    public static final String CI_PLMN_DEFAULT_CONFIG = "PLMNDefaultConfig";
    public static final String CI_DT = "CheckDualtalk";
    public static final String CI_DUAL_SIM_CHECK = "CheckDualSIM";
    public static final String CI_SIM_3G_CHECK = "CheckSingleSIM3G";
    public static final String CI_DATA_ROAM = "CheckDataRoam";
    public static final String CI_DATA_ROAM_CONFIG = "CheckDataRoamConfig";
    public static final String CI_DATA_ROAM_OFF_CONFIG = "CheckDataRoamoffConfig";
    public static final String CI_LTE_TDS_ONLY_CONFIG = "LteTdscdmaOnlyConfig";
    public static final String CI_LTE_GSM_TDS_CONFIG = "LteGsmTdscdmaAutoConfig";

    // ruoyao
    public static final String CI_RESTORE = "Restore";
    public static final String CI_IMEI_IN_DM = "IMEIInDm";
    public static final String CI_IMEI = "IMEI";
    public static final String CI_DMSTATE_CHECK_ONLY = "DMCheckOnly";
    public static final String CI_DMSTATE_AUTO_CONFG = "DMAutoConfgOn";
    public static final String CI_BATTERY = "Battery";
    public static final String CI_VENDOR_APK = "VendorApk";
    public static final String CI_SMSNUMBER_LAB = "SMSNumberLab";
    public static final String CI_SMSNUMBER_CURR = "SMSNumberCurr";
    public static final String CI_SMSPORT = "SMSPort";
    public static final String CI_DMSERVER_LAB = "DMServerLab";
    public static final String CI_DMSERVER_CURR = "DMServerCurr";
    public static final String CI_DMMANU = "DMManufacturer";
    public static final String CI_DM_AUTO_CHECK = "DMAutoCheck";
    public static final String CI_DMPRODUCT = "DMProduct";
    public static final String CI_DMVERSION = "DMVersion";
    public static final String CI_BUILD_TYPE = "BuildType";
    public static final String CI_FR = "FR";
    public static final String CI_HSUPA = "HSUPA";
    public static final String CI_BASE_FUNC = "BaseFunc";

    public static final String CI_TARGET_VERSION = "TargetVersion";
    public static final String CI_TARGET_MODE = "TargetMode";
    public static final String CI_ROOT = "Root";
    public static final String CI_IVSR = "IVSR";
    public static final String CI_IVSR_CHECK_ONLY = "IVSRCheckOnly";
    public static final String CI_ATCI = "ATCI";
    public static final String CI_ATCI_IN_EM_ENABLE = "ATCIInEmEnable";
    public static final String CI_SERIAL_NUMBER = "SerialNumber";
    public static final String CI_LOGGER_ON = "LoggerOn";
    public static final String CI_LOGGER_OFF = "LoggerOff";
    public static final String CI_TAGLOG_OFF = "Taglogoff";
    public static final String CI_WIFI_ALWAYKEEP = "WifiAlwaysKeep";
    public static final String CI_WIFI_NEVERKEEP = "WifiNeverKeep";
    public static final String CI_WIFI_OFF = "WifiOff";
    public static final String CI_WIFI_NEVERKEEP_ONLYCHECK = "WifiNeverKeepOnlyCheck";
    public static final String CI_CTIA_ENABLE = "CTIAEnable";
    public static final String CI_GPRS_DATA_PREF = "GprsDataPreffer";
    public static final String CI_GPRS_CALL_PREF = "GprsCallPreffer";

    public static final String CI_UA_BROWSER = "UABrowser";
    public static final String CI_UA_BROWSERURL = "UABrowserURL";
    public static final String CI_UA_MMS = "UAMms";
    public static final String CI_UA_MMSURL = "UAMmsURL";
    public static final String CI_UA_HTTP = "UAHttp";
    public static final String CI_UA_HTTPURL = "UAHttpURL";
    public static final String CI_UA_RTSP = "UARtsp";
    public static final String CI_UA_RTSPURL = "UARtspURL";
    public static final String CI_UA_CMMB = "UACmmb";

    //boli
    public static final String CI_SUPL = "SUPL";
    public static final String CI_SUPL_CHECK_ONLY = "SUPLCheckOnly";
    public static final String CI_SUPL_AUTO_CONFG = "SUPLAutoConfig";

    public static final String CI_LABAPN = "LabAPN";
    public static final String CI_LABAPN_CHECK_MMS = "LabAPNCheckMMS";
    public static final String CI_LABAPN_CHECK_TYPE = "LabAPNCheckType";
    public static final String CI_LABAPN_CHECK_MMS_TYPE = "LabAPNCheckMMSAndType";

    public static final String CI_SMSC = "SMSC";
    public static final String CI_SMSC_CHECK_ONLY = "SMSCCheckOnly";
    public static final String CI_SMSC_AUTO_CONFG = "SMSCAutoConfig";

    public static final String CI_CMWAP = "CMWAP";
    public static final String CI_CMWAP_CHECK_ONLY = "CMWAPCheckOnly";
    public static final String CI_CMWAP_AUTO_CONFG = "CMWAPAutoConfig";

    public static final String CI_CMNET = "CMNET";
    public static final String CI_CMNET_CHECK_ONLY = "CMNETCheckOnly";
    public static final String CI_CMNET_AUTO_CONFG = "CMNETAutoConfig";

    public static final String CI_BT = "BT";
    public static final String CI_BT_CHECK_ONLY = "BTCheckOnly";
    public static final String CI_BT_AUTO_CONFG = "BTAutoConfig";

    public static final String CI_MMS_ROAMING = "MMSRoaming";

    //Yongmao
    public static final String CI_CALIBRATION_DATA = "CalibrationData";
    public static final String CI_APK_CALCULATOR = "Calculator";
    public static final String CI_APK_STOPWATCH = "Stopwatch";
    public static final String CI_APK_OFFICE = "Office";
    public static final String CI_APK_NOTEBOOK = "Notebook";
    public static final String CI_APK_BACKUP = "Backup";
    public static final String CI_APK_CALENDAR = "Calendar";
    public static final String CI_24H_CHARGE_PROTECT = "24hChargeProtect";
    public static final String CI_WLAN_SSID = "WlanSSID";
    public static final String CI_WLAN_MAC_ADDR = "WlanMacAddress";
    public static final String CI_MODEM_SWITCH_TDD = "ModemSwitchTdd";
    public static final String CI_MODEM_SWITCH_TG = "ModemSwitchTg";
    public static final String CI_MODEM_SWITCH_AUTO = "ModemSwitchAuto";

    //2.2
    public static final String CI_BJ_DATA_TIME = "CheckBJTime";
    public static final String CI_SCREEN_ON_SLEEP = "checkscreenonsleep";//sleep 30min
    public static final String CI_SCREEN_ON_SLEEP_15SEC = "checkscreenonsleep15s";
    public static final String CI_SCREEN_ON_UNLOCK = "checkscreenonunlock";
    public static final String CI_SCREEN_ROTATE = "checkscreenrotate";
    public static final String CI_BROWSER_URL = "checkUrl";
    public static final String CI_BROWSER_URL_COMCAT = "checkUrlcomcat";
    public static final String CI_SHORTCUT = "checkshortcut";
    public static final String CI_DEFAULTSTORAGE = "checkDefaultStorage";
    public static final String CI_DEFAULTIME = "checkdefaultime";
    public static final String CI_WIFI_CONTROL = "checkwificontrol";
    public static final String CI_MANUL_CHECK = "checkManualCheck";
    public static final String CI_DISABLED_RED_SCREEN = "disabledRedScreen";
    public static final String CI_ADD_Mo_CALL_LOG = "addMoCallLog";

    public static final String CI_3G_ONLY_CONFIG = "3GOnlyConfig";
    public static final String CI_3G_AND_2G_CONFIG = "3GAnd2GConfig";
    public static final String CI_4G_3G_AND_2G_Check = "4G3GAnd2GCheck";
    public static final String CI_TG_And_3G_ONLY = "ModemTgAndNetwork3gOnly";
    public static final String CI_TDD_And_TDWCDMA_ONLY = "MdTddAndNwTdscdmaOnly";
    public static final String CI_TG_And_3G_2G = "ModemTgAndNetwork3g2g";
    public static final String CI_TDD_And_DUAL_MODE = "MdTddAndNwGsmTdscdmaAuto";
    public static final String CI_4G_ONLY_CONFIG = "4GOnlyConfig";
    public static final String CI_2G_ONLY_CONFIG = "2GOnlyConfig";
    public static final String CI_SGLTE_And_4G_ONLY = "MdSglteAndNw4gOnly";
    public static final String CI_SGLTE_And_2G_ONLY = "MdSglteAndNw2gOnly";
    public static final String CI_SGLTE_And_3G_ONLY = "MdSglteAndNw3gOnly";
    public static final String CI_TDDCSFB_And_GSM_TDS = "MdTddcsfbAndGsmTdscdma";
    public static final String CI_TDDCSFB_And_TDS_ONLY = "MdTddcsfbAndTdscdmaOnly";
    public static final String CI_TDDCSFB_And_LTE_ONLY = "MdTddcsfbAndLteOnly";
    public static final String CI_TDDCSFB_And_GSM_ONLY = "MdTddcsfbAndGsmOnly";
    public static final String CI_LTE_ONLY_CONFIG = "LteOnlyConfig";
    public static final String CI_4G2G_CONFIG = "4g2gConfig";
    public static final String CI_TDDCSFB_And_4g2g = "MdTddcsfbAnd4g2g";
    public static final String CI_4G_3G_2G_Auto_Check = "4G3G2GAutoCheck";
    public static final String CI_4G_3G_2G_Auto = "4G3G2GAuto";
    public static final String CI_4G_3G_AND_2G = "4G3GAnd2G";
    public static final String CI_SGLTE_And_4G3GAnd2G = "MdSglteAnd4G3GAnd2G";
    public static final String CI_TDDCSFB_And_4G3G2GAuto = "MdTddcsfbAnd4G3G2GAuto";
    public static final String CI_FDDCSFB_And_4G3GAnd2G = "MdFddcsfbAnd4G3GAnd2G";
    public static final String CI_FDDCSFB_And_4G3G2GAuto = "MdFddcsfbAnd4G3G2GAuto";

    public static final String CI_MS_TDD_CASE_SGLTE = "MSTddCaseSglte";
   // public static final String CI_MODEM_SWITCH_SGLTE = "ModemSwitchSglte";
    public static final String CI_MS_FDD_CASE_FDD_CSFB = "MSFddCaseFddCsfb";
    public static final String CI_MODEM_SWITCH_FDD_CSFB = "ModemSwitchFddCsfb";
    public static final String CI_MODEM_SWITCH_TDD_CSFB = "ModemSwitchTddCsfb";
    //public static final String CI_MODEM_TEST_FTA_ON = "ModemTestFtaOn";
    public static final String CI_DATA_CONNECT_OFF_CONFIG_LTE = "DataConnOffConfigLte";
    public static final String CI_DATA_CONNECT_ON_CONFIG_LTE = "DataConnOnConfigLte";
    public static final String CI_APN_IMS2 = "ApnIms2";
    public static final String CI_DATA_ROAM_CONFIG_LTE = "DataRoamOnLte";
    public static final String CI_LAB_4G_USIM = "Lab4gUsim";
    public static final String CI_USER2ROOT_ROOT = "User2RootConfigRoot";
    public static final String CI_USB_SHARE_NET_Config = "UsbShareNetConfig";
    public static final String CI_APN_APN = "ApnApn";
    public static final String CI_PREF_WCDMA_PREF = "3g2gAutoConfig";
    public static final String CI_CFU_CONFIG_LTE = "CFUConfigOffLte";
    public static final String CI_CTAFTA_CONFIG_ON_LTE = "CTAFTAConfigIntegrityCheckOnLte";
    public static final String CI_LOGGER_OFF_LTE = "LoggerOffLte";
    public static final String CI_APN_PROTOCOL = "ApnProtocol";
    public static final String CI_APN_TYPE_SUPL = "ApnTypeSupl";
    public static final String CI_APN_TYPE_NOT_SUPL_EXIST = "ApnTypeNotSuplExist";
    public static final String CI_2G_ROAMING_DISABLE = "2gOnlyRoamingDisable";
    public static final String CI_GPRS_ATTACH_CONTINUE_LTE = "GPRSAlwaysAttachContinueLte";
    public static final String CI_SCREEN_OFF = "ScreenOff";
    public static final String CI_NFC_OFF = "NfcOff";
    public static final String CI_GPS_OFF = "GPSOff";
    public static final String CI_BT_OFF = "BluetoothOff";
    public static final String CI_IPO_SUPPORT = "IPOSupport";
    public static final String CI_USB_CBA_SUPPORT = "USBCBASupport";
    public static final String CI_APN_4C4745 = "Apn4C4745";
    public static final String CI_APN_CHECK = "Apncheck";
    public static final String CI_APN_IMS = "ApnIms";
    public static final String CI_RSRP_SIGNAL = "DLRSRP";
    public static final String CI_4G_SIGNAL = "4gSignal";
    public static final String CI_VOLTE_VOICE_HIGH_LEVEL = "VolteVoiceHigh";
    public static final String CI_VOLTE_VOICE_NORMAL_LEVEL = "VolteVoiceNormal";
    public static final String CI_VOLTE_IPSEC = "VolteIPsec";
    public static final String CI_VOLTE_4G_ENHANCE = "Volte4gEnhance";
}

/**
 * @Prompt Add your owner provider below, then put it into ProviderFactory
 */
class OneBtnCheckProvider extends CheckItemProviderBase {
    OneBtnCheckProvider(Context c) {
        mArrayItems.add(new CheckCalData(c, CheckItemKeySet.CI_CALIBRATION_DATA));
        mArrayItems.add(new CheckRestore(c, CheckItemKeySet.CI_RESTORE));
        mArrayItems.add(new CheckBuildType(c, CheckItemKeySet.CI_BUILD_TYPE));
        mArrayItems.add(new CheckTargetMode(c, CheckItemKeySet.CI_TARGET_MODE));
        mArrayItems.add(new CheckTargetVersion(c, CheckItemKeySet.CI_TARGET_VERSION));
        mArrayItems.add(new CheckIMEI(c, CheckItemKeySet.CI_IMEI));
        mArrayItems.add(new CheckGprsMode(c, CheckItemKeySet.CI_GPRS_CALL_PREF));
        //mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_CHECK_ONLY));
        mArrayItems.add(new CheckATCI(c, CheckItemKeySet.CI_ATCI));
        mArrayItems.add(new CheckBattery(c, CheckItemKeySet.CI_BATTERY));
        //mArrayItems.add(new CheckWIFISleepPolicy(c, CheckItemKeySet.CI_WIFI_NEVERKEEP_ONLYCHECK));
        mArrayItems.add(new CheckSN(c, CheckItemKeySet.CI_SERIAL_NUMBER));
        mArrayItems.add(new CheckIVSR(c, CheckItemKeySet.CI_IVSR_CHECK_ONLY));
        mArrayItems.add(new CheckVendorApk(c, CheckItemKeySet.CI_VENDOR_APK));
        //mArrayItems.add(new CheckDMAuto(c, CheckItemKeySet.CI_DM_AUTO_CHECK));
        //mArrayItems.add(new CheckDMManufacturer(c, CheckItemKeySet.CI_DMMANU));
        //mArrayItems.add(new CheckDMProduct(c, CheckItemKeySet.CI_DMPRODUCT));
        //mArrayItems.add(new CheckDMVersion(c, CheckItemKeySet.CI_DMVERSION));
        //mArrayItems.add(new CheckFR(c, CheckItemKeySet.CI_FR));
        mArrayItems.add(new CheckHSUPA(c, CheckItemKeySet.CI_HSUPA));
        mArrayItems.add(new CheckBaseFunc(c, CheckItemKeySet.CI_BASE_FUNC));
        mArrayItems.add(new CheckRoot(c, CheckItemKeySet.CI_ROOT));

        // yaling
        if (Utils.IS_SGLTE_PHONE){
            mArrayItems.add(new CheckLteNetworkMode(c, CheckItemKeySet.CI_4G_3G_AND_2G_Check));
        } else if (Utils.IS_CSFB_PHONE) {
            mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto_Check));
        } else {
            mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_DUAL_MODE_CHECK));
        }
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_ON));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_CHECK));
        mArrayItems.add(new CheckPLMN(c, CheckItemKeySet.CI_PLMN_DEFAULT));

        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_BROWSER));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_BROWSERURL));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_MMS));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_MMSURL));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_HTTP));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_HTTPURL));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_RTSP));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_RTSPURL));
        //mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_CMMB));

        //Yongmao

        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_CALCULATOR));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_STOPWATCH));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_NOTEBOOK));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_BACKUP));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_CALENDAR));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_OFFICE));
        mArrayItems.add(new CheckWlanMacAddr(c, CheckItemKeySet.CI_WLAN_MAC_ADDR));
        mArrayItems.add(new CheckWlanSSID(c, CheckItemKeySet.CI_WLAN_SSID));

        // Bo
        mArrayItems.add(new CheckBT(c, CheckItemKeySet.CI_CMNET_CHECK_ONLY));
        mArrayItems.add(new CheckUSBCBAState(c, CheckItemKeySet.CI_USB_CBA_SUPPORT));
    }
}

class TTCN3GProvider extends CheckItemProviderBase {
    TTCN3GProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG));
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
        if (Utils.MTK_GEMINI_SUPPORT == true &&
            SystemProperties.get("ro.mtk_dt_support").equals("1") == false) {
           mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE));
        }
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_CONFIG_OFF));
        mArrayItems.add(new CheckPLMN(c, CheckItemKeySet.CI_PLMN_DEFAULT_CONFIG));
        if (WorldPhoneUtil.isWorldPhoneSupport()) {
            //before SGLTE & CSFB, this feature option is required in EM to judge if show World Phone
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDD_And_TDWCDMA_ONLY));
        }
    }
}

class Anritsu23GProvider extends CheckItemProviderBase {
    Anritsu23GProvider(Context c) {
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
        if (WorldPhoneUtil.isWorldPhoneSupport()) {
            //before SGLTE & CSFB, this feature option is required in EM to judge if show World Phone
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDD_And_DUAL_MODE));
        }
    }
}

class TTCN23GProvider extends CheckItemProviderBase {
    TTCN23GProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG));
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
        if (Utils.MTK_GEMINI_SUPPORT == true &&
            SystemProperties.get("ro.mtk_dt_support").equals("1") == false) {
            mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE));
        }
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_CONFIG_OFF));
        mArrayItems.add(new CheckPLMN(c, CheckItemKeySet.CI_PLMN_DEFAULT_CONFIG));
        if (WorldPhoneUtil.isWorldPhoneSupport()) {
            //before SGLTE & CSFB, this feature option is required in EM to judge if show World Phone
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDD_And_DUAL_MODE));
        }
    }
}

class RRMProvider extends CheckItemProviderBase {
    RRMProvider(Context c) {
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
        if (WorldPhoneUtil.isWorldPhoneSupport()) {
           //before SGLTE & CSFB, this feature option is required in EM to judge if show World Phone
           mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD));
        }
    }
}

class RFProvider extends CheckItemProviderBase {
    RFProvider(Context c) {
        mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
        if (Utils.MTK_GEMINI_SUPPORT == true &&
            SystemProperties.get("ro.mtk_dt_support").equals("1") == false) {
            mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE));
        }
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG));

        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG));
        mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_CONFIG_ON));
    }
}

class NetworkIOTProvider extends CheckItemProviderBase {
    NetworkIOTProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_LABAPN_CHECK_MMS_TYPE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckMMSRoaming(c, CheckItemKeySet.CI_MMS_ROAMING));
        if (WorldPhoneUtil.isWorldPhoneSupport()) {
           //before SGLTE & CSFB, this feature option is required in EM to judge if show World Phone
           mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD));
        }
    }
}

class BasicCommunProvider extends CheckItemProviderBase {
    BasicCommunProvider(Context c) {
        mArrayItems.add(new CheckGprsMode(c, CheckItemKeySet.CI_GPRS_DATA_PREF));
    }
}

class PhoneCardCompatProvider extends CheckItemProviderBase {
    PhoneCardCompatProvider(Context c) {
         mArrayItems.add(new CheckGPRSProtocol(c, CheckItemKeySet.CI_GPRS_CONFIG));
         mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_LABAPN_CHECK_MMS));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TG));
        } else if (Utils.IS_CSFB_PHONE) {
            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB));
        } else {
            //before SGLTE & CSFB, below feature option is required in EM to judge if show World Phone
            if (WorldPhoneUtil.isWorldPhoneSupport()) {
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD));
            }
        }
    }
}

class LocalFuncProvider extends CheckItemProviderBase {
    LocalFuncProvider(Context c) {
        //Yongmao
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_CALCULATOR));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_STOPWATCH));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_NOTEBOOK));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_BACKUP));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_CALENDAR));
        mArrayItems.add(new CheckApkExist(c, CheckItemKeySet.CI_APK_OFFICE));


    }
}

class BroadbandNetProvider extends CheckItemProviderBase {
    BroadbandNetProvider(Context c) {
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_BROWSER));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_BROWSERURL));
        mArrayItems.add(new CheckCurAPN(c, CheckItemKeySet.CI_CMNET));
    }
}

class VideoTelProvider extends CheckItemProviderBase {
    VideoTelProvider(Context c) {

    }
}

class StreamingProvider extends CheckItemProviderBase {
    StreamingProvider(Context c) {
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_HTTP));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_HTTPURL));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_RTSP));
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_RTSPURL));
        mArrayItems.add(new CheckCurAPN(c, CheckItemKeySet.CI_CMWAP));
    }
}
/*
class DMLabNetProvider extends CheckItemProviderBase {
    DMLabNetProvider(Context c) {
        mArrayItems.add(new CheckIMEI(c, CheckItemKeySet.CI_IMEI_IN_DM));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_AUTO_CONFG));
        mArrayItems.add(new CheckSMSNumber(c, CheckItemKeySet.CI_SMSNUMBER_LAB));
        mArrayItems.add(new CheckSMSPort(c, CheckItemKeySet.CI_SMSPORT));
        mArrayItems.add(new CheckDMServer(c, CheckItemKeySet.CI_DMSERVER_LAB));
        mArrayItems.add(new CheckDMManufacturer(c, CheckItemKeySet.CI_DMMANU));
        mArrayItems.add(new CheckDMProduct(c, CheckItemKeySet.CI_DMPRODUCT));
        mArrayItems.add(new CheckDMVersion(c, CheckItemKeySet.CI_DMVERSION));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_DM));
    }
}

class DMRealNetProvider extends CheckItemProviderBase {
    DMRealNetProvider(Context c) {
        mArrayItems.add(new CheckIMEI(c, CheckItemKeySet.CI_IMEI_IN_DM));
        mArrayItems.add(new CheckDMState(c, CheckItemKeySet.CI_DMSTATE_AUTO_CONFG));
        mArrayItems.add(new CheckSMSNumber(c, CheckItemKeySet.CI_SMSNUMBER_CURR));
        mArrayItems.add(new CheckSMSPort(c, CheckItemKeySet.CI_SMSPORT));
        mArrayItems.add(new CheckDMServer(c, CheckItemKeySet.CI_DMSERVER_CURR));
        mArrayItems.add(new CheckDMManufacturer(c, CheckItemKeySet.CI_DMMANU));
        mArrayItems.add(new CheckDMProduct(c, CheckItemKeySet.CI_DMPRODUCT));
        mArrayItems.add(new CheckDMVersion(c, CheckItemKeySet.CI_DMVERSION));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_DM));
    }
}
*/
class MobileTVProvider extends CheckItemProviderBase {
    MobileTVProvider(Context c) {
        mArrayItems.add(new CheckUA(c, CheckItemKeySet.CI_UA_CMMB));
    }
}

class AgpsProvider extends CheckItemProviderBase {
    AgpsProvider(Context c) {
            mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_LABAPN_CHECK_MMS));
            mArrayItems.add(new CheckSUPL(c, CheckItemKeySet.CI_SUPL));
            mArrayItems.add(new CheckSMSC(c, CheckItemKeySet.CI_SMSC));
    }
}

class WlanProvider extends CheckItemProviderBase {
    WlanProvider(Context c) {
        mArrayItems.add(new CheckWIFISleepPolicy(c, CheckItemKeySet.CI_WIFI_ALWAYKEEP));
        mArrayItems.add(new CheckCTIA(c, CheckItemKeySet.CI_CTIA_ENABLE));
    }
}

class MTBFProvider extends CheckItemProviderBase {
    MTBFProvider(Context c) {
        //mArrayItems.add(new CheckRoot(c, CheckItemKeySet.CI_ROOT));
        mArrayItems.add(new CheckIVSR(c, CheckItemKeySet.CI_IVSR));
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_TAGLOG_OFF));
        mArrayItems.add(new CheckSN(c, CheckItemKeySet.CI_SERIAL_NUMBER));

        //Yongmao
        mArrayItems.add(new Check24hChargeProtect(c,
                CheckItemKeySet.CI_24H_CHARGE_PROTECT));
    }
}

class LocalPerfProvider extends CheckItemProviderBase {
    LocalPerfProvider(Context c) {
        mArrayItems.add(new CheckBuildType(c, CheckItemKeySet.CI_BUILD_TYPE));
        //yaling
        mArrayItems.add(new CheckSIMSlot(c, CheckItemKeySet.CI_SIM_3G_CHECK));
    }
}

class PowerConsumpProvider extends CheckItemProviderBase {
    PowerConsumpProvider(Context c) {
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_OFF));
        mArrayItems.add(new CheckWIFISleepPolicy(c, CheckItemKeySet.CI_WIFI_NEVERKEEP));
        //mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG));//no need

    }
}

class FieldTestProvider extends CheckItemProviderBase {
    FieldTestProvider(Context c) {
        if (SystemProperties.get("ro.mtk_lte_dc_support").equals("1") == true){
            mArrayItems.add(new CheckLteNetworkMode(c, CheckItemKeySet.CI_3G_AND_2G_CONFIG));
        } else {
            mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_DUAL_MODE_CONFIG));
        }
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_ON));
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_TAGLOG_OFF));
        mArrayItems.add(new CheckDTSUPPORT(c, CheckItemKeySet.CI_DT));
        mArrayItems.add(new CheckSIMSlot(c, CheckItemKeySet.CI_DUAL_SIM_CHECK));
        if (Utils.IS_CSFB_PHONE) {
            mArrayItems.add(new CheckIPO(c, CheckItemKeySet.CI_IPO_SUPPORT));
        }
        mArrayItems.add(new CheckRSRP(c, CheckItemKeySet.CI_RSRP_SIGNAL));
    }
}

class TdsProConTtcnProvider extends CheckItemProviderBase {
    TdsProConTtcnProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TG_And_3G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_TDS_ONLY));
            }
        }
    }
}

class TdsTTCN23gProvider extends CheckItemProviderBase {
    TdsTTCN23gProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TG_And_3G_2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_GSM_TDS));
            }
        }
    }
}

class TdsAnri23gProvider extends CheckItemProviderBase {
    TdsAnri23gProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        //mArrayItems.add(new CheckCTAFTA(c, CheckItemKeySet.CI_CTAFTA_CONFIG_ON_LTE));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TG_And_3G_2G));
        } else if (Utils.IS_CSFB_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_GSM_TDS));
        }
    }
}

class TdsRrmConProvider extends CheckItemProviderBase {
    TdsRrmConProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TG_And_3G_2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS) {
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_GSM_TDS));
            }
        }
    }
}

class TdsRfConProvider extends CheckItemProviderBase {
    TdsRfConProvider(Context c) {
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TG_And_3G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_TDS_ONLY));
            }
        }
    }
}

class TdlNvIotProvider extends CheckItemProviderBase {
    TdlNvIotProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_IMS2));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));
        if (Utils.IS_SGLTE_PHONE) {
//            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
//            mArrayItems.add(new CheckSIMSlot(c, CheckItemKeySet.CI_LAB_4G_USIM));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB));
            }
        }
    }
}

class TdlUsimTddBipProvider extends CheckItemProviderBase {
    TdlUsimTddBipProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_TYPE_SUPL));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
//            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB));
            }
        }
    }
}

class TdlUsimTddNotBipProvider extends CheckItemProviderBase {
    TdlUsimTddNotBipProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_TYPE_NOT_SUPL_EXIST));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
//            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB));
            }
        }
    }
}

class TdlUsimFddBipProvider extends CheckItemProviderBase {
    TdlUsimFddBipProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_TYPE_SUPL));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        if (Utils.IS_SGLTE_PHONE) {
            if (Utils.IS_3_MODEMS) {
//                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        }
    }
}

class TdlUsimFddNotBipProvider extends CheckItemProviderBase {
    TdlUsimFddNotBipProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_TYPE_NOT_SUPL_EXIST));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            if (Utils.IS_3_MODEMS) {
//                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        }
    }
}

class TdlRrmTddProvider extends CheckItemProviderBase {
    TdlRrmTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlRrmFddFddTddProvider extends CheckItemProviderBase {
    TdlRrmFddFddTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3GAnd2G));
            }
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlNwIotProvider extends CheckItemProviderBase {
    TdlNwIotProvider(Context c) {
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckLteNetworkMode(c, CheckItemKeySet.CI_PREF_WCDMA_PREF));
        } else if (Utils.IS_CSFB_PHONE) {
            mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_PREF_WCDMA_PREF));
        }
    }
}

class TdlPctTddProvider extends CheckItemProviderBase {
    TdlPctTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlPctFddFddTddProvider extends CheckItemProviderBase {
    TdlPctFddFddTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3GAnd2G));
            }
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlRfTddProvider extends CheckItemProviderBase {
    TdlRfTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
//            mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB));
            }
        }
    }
}

class TdlRfFddFddTddProvider extends CheckItemProviderBase {
    TdlRfFddFddTddProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));

        if (Utils.IS_SGLTE_PHONE) {
            if (Utils.IS_3_MODEMS) {
//                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_SGLTE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
            }
        }
    }
}

class TdlNsIotRAndSProvider extends CheckItemProviderBase {
    TdlNsIotRAndSProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlNsIotAgiDataOnProvider extends CheckItemProviderBase {
    TdlNsIotAgiDataOnProvider(Context c) {
        //mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        //mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_APN));
        //mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_OFF_LTE));

        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));
        mArrayItems.add(new CheckWIFIControl(c, CheckItemKeySet.CI_WIFI_OFF));
        mArrayItems.add(new CheckBluetoothState(c, CheckItemKeySet.CI_BT_OFF));
        mArrayItems.add(new CheckGPSState(c, CheckItemKeySet.CI_GPS_OFF));
        mArrayItems.add(new CheckNFC(c, CheckItemKeySet.CI_NFC_OFF));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_SLEEP_15SEC));
        mArrayItems.add(new CheckScreen(c, CheckItemKeySet.CI_SCREEN_OFF));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlNsIotAgiDataOffProvider extends CheckItemProviderBase {
    TdlNsIotAgiDataOffProvider(Context c) {
        //mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        //mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_APN));
        //mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_OFF_LTE));

        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));
        mArrayItems.add(new CheckWIFIControl(c, CheckItemKeySet.CI_WIFI_OFF));
        mArrayItems.add(new CheckBluetoothState(c, CheckItemKeySet.CI_BT_OFF));
        mArrayItems.add(new CheckGPSState(c, CheckItemKeySet.CI_GPS_OFF));
        mArrayItems.add(new CheckNFC(c, CheckItemKeySet.CI_NFC_OFF));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_SLEEP_15SEC));
        mArrayItems.add(new CheckScreen(c, CheckItemKeySet.CI_SCREEN_OFF));

        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlNsIotAniteProvider extends CheckItemProviderBase {
    TdlNsIotAniteProvider(Context c) {
        //mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        //mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_APN));
        //mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_OFF_LTE));

        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY));
            }
        }
    }
}

class TdlNsIotAniCsfbProvider extends CheckItemProviderBase {
    TdlNsIotAniCsfbProvider(Context c) {
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
         //       mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G2G_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS) {
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class TdlIPv62gCaseProvider extends CheckItemProviderBase {
    TdlIPv62gCaseProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_PROTOCOL));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_2G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_GSM_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_GSM_ONLY));
            }
        }
    }
}

class TdlIPv63gCaseProvider extends CheckItemProviderBase {
    TdlIPv63gCaseProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_PROTOCOL));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_3G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_TDS_ONLY));
            }
        }
    }
}

class TdlIPv64gCaseProvider extends CheckItemProviderBase {
    TdlIPv64gCaseProvider(Context c) {
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_PROTOCOL));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE));
        if (Utils.IS_SGLTE_PHONE) {
            mArrayItems.add(new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_SGLTE_And_4G_ONLY));
        } else if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY));
            }
        }
    }
}

class TdlInternalRoamingCaseProvider extends CheckItemProviderBase {
    TdlInternalRoamingCaseProvider(Context c) {
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
    }
}

class HKFTLabCaseProvider extends CheckItemProviderBase {
    HKFTLabCaseProvider(Context c) {
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_AUTO));
    }
}

class LTEInternalRoamingAniteCaseProvider extends CheckItemProviderBase {
    LTEInternalRoamingAniteCaseProvider(Context c) {
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_APN));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_AUTO));
    }
}

class LTEInternalRoamingAnritsuCaseProvider extends CheckItemProviderBase {
    LTEInternalRoamingAnritsuCaseProvider(Context c) {
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_AUTO));
    }
}

class NSIOTCsfbVolteInterRatCaseProvider extends CheckItemProviderBase {
    NSIOTCsfbVolteInterRatCaseProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CHECK));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto));
            }
        }
    }
}

class NSIOTTDLTDSRoamingCaseProvider extends CheckItemProviderBase {
    NSIOTTDLTDSRoamingCaseProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CHECK));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto));
            }
        }
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
    }
}

class NSIOTOtherCaseProvider extends CheckItemProviderBase {
    NSIOTOtherCaseProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CHECK));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY));
            }
        }
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
    }
}

class NSIOTAgilentPwrcCaseProvider extends CheckItemProviderBase {
    NSIOTAgilentPwrcCaseProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_4C4745));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_LTE_ONLY_CONFIG));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY));
            }
        }
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckWIFIControl(c, CheckItemKeySet.CI_WIFI_OFF));
        mArrayItems.add(new CheckBluetoothState(c, CheckItemKeySet.CI_BT_OFF));
        mArrayItems.add(new CheckGPSState(c, CheckItemKeySet.CI_GPS_OFF));
        mArrayItems.add(new CheckNFC(c, CheckItemKeySet.CI_NFC_OFF));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_SLEEP_15SEC));
    }
}

class NSIOTInterRoamingRoamingCaseProvider extends CheckItemProviderBase {
    NSIOTInterRoamingRoamingCaseProvider(Context c) {
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_CHECK));
        mArrayItems.add(new CheckDataRoam(c, CheckItemKeySet.CI_DATA_ROAM_CONFIG));
        mArrayItems.add(new CheckCFU(c, CheckItemKeySet.CI_CFU_CONFIG_LTE));
        //mArrayItems.add(new CheckModemSwitch(c, CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB));
        if (Utils.IS_CSFB_PHONE) {
            if (Utils.IS_3_MODEMS) {
                mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
                mArrayItems.add(new Check2gRoaming(c, CheckItemKeySet.CI_2G_ROAMING_DISABLE));
            } else if (Utils.IS_5_MODEMS){
                mArrayItems.add(
                  new CheckMdSwitchAndNwMode(c, CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto));
            }
        }
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
    }
}

class NVIOTCSFBCaseProvider extends CheckItemProviderBase {
    NVIOTCSFBCaseProvider(Context c) {
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_ON));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckBJTime(c, CheckItemKeySet.CI_BJ_DATA_TIME));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_UNLOCK));
        mArrayItems.add(new CheckRSRP(c, CheckItemKeySet.CI_RSRP_SIGNAL));
        mArrayItems.add(new Check4GSIGAL(c, CheckItemKeySet.CI_4G_SIGNAL));
    }
}

class NVIOTVOLTECaseProvider extends CheckItemProviderBase {
    NVIOTVOLTECaseProvider(Context c) {
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_ON));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckBJTime(c, CheckItemKeySet.CI_BJ_DATA_TIME));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_UNLOCK));
        mArrayItems.add(new CheckRSRP(c, CheckItemKeySet.CI_RSRP_SIGNAL));
        mArrayItems.add(new CheckVOLTEVoice(c, CheckItemKeySet.CI_VOLTE_VOICE_HIGH_LEVEL));
        mArrayItems.add(new CheckLabAPN(c, CheckItemKeySet.CI_APN_IMS));
        mArrayItems.add(new Check4GEnhanceOpen(c, CheckItemKeySet.CI_VOLTE_4G_ENHANCE));
        mArrayItems.add(new CheckIPO(c, CheckItemKeySet.CI_IPO_SUPPORT));
        mArrayItems.add(new CheckVolteIPSec(c, CheckItemKeySet.CI_VOLTE_IPSEC));
    }
}

class NVIOTCACaseProvider extends CheckItemProviderBase {
    NVIOTCACaseProvider(Context c) {
        mArrayItems.add(new CheckUser2Root(c, CheckItemKeySet.CI_USER2ROOT_ROOT));
        mArrayItems.add(new CheckUsbShareNet(c, CheckItemKeySet.CI_USB_SHARE_NET_Config));
        mArrayItems.add(new CheckNetworkMode(c, CheckItemKeySet.CI_4G_3G_2G_Auto));
        mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
        mArrayItems.add(new CheckMTKLogger(c, CheckItemKeySet.CI_LOGGER_ON));
        mArrayItems.add(new CheckAtciInEM(c, CheckItemKeySet.CI_ATCI_IN_EM_ENABLE));
        mArrayItems.add(new CheckBJTime(c, CheckItemKeySet.CI_BJ_DATA_TIME));
        mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_UNLOCK));
        mArrayItems.add(new CheckRSRP(c, CheckItemKeySet.CI_RSRP_SIGNAL));
        mArrayItems.add(new Check4GSIGAL(c, CheckItemKeySet.CI_4G_SIGNAL));
    }
}

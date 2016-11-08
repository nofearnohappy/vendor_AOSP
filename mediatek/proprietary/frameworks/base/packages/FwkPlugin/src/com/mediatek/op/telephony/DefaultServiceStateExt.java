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

package com.mediatek.op.telephony;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.telephony.IServiceStateExt;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;

import java.util.Map;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IServiceStateExt")
public class DefaultServiceStateExt implements IServiceStateExt {
    static final String TAG = "GSM";
    private Context mContext;

    public DefaultServiceStateExt() {
    }

    public DefaultServiceStateExt(Context context) {
        mContext = context;
    }

    public String onUpdateSpnDisplay(String plmn, ServiceState ss, int phoneId) {
        /* ALPS00362903 */
        if (SystemProperties.get("ro.mtk_network_type_always_on").equals("1")) {
            // for LTE
            if (ss.getRilVoiceRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE
                && plmn != Resources.getSystem().getText(com.android.internal.R.string.
                        lockscreen_carrier_default).toString()) {
                plmn = plmn + " 4G";
            } else if (ss.getRilVoiceRadioTechnology() > ServiceState.RIL_RADIO_TECHNOLOGY_EDGE
                /* ALPS00492303 */
                //if (radioTechnology > 2 && plmn != null){
                    && plmn != Resources.getSystem().getText(com.android.internal.R.string.
                            lockscreen_carrier_default).toString()) {
                plmn = plmn + " 3G";
            }
        }

        return plmn;
    }

    public boolean isImeiLocked() {
        return false;
    }

    public boolean isBroadcastEmmrrsPsResume(int value) {
        return false;
    }

    public boolean needEMMRRS() {
        return false;
    }

    public boolean needSpnRuleShowPlmnOnly() {
        //[ALPS01679495]-start: don't show SPN for CTA case
        if (SystemProperties.get("ro.mtk_cta_support").equals("1")) {
            return true;
        }
        //[ALPS01679495]-end
        return false;
    }

    public boolean needBrodcastAcmt(int errorType, int errorCause) {
        return false;
    }

    public boolean needRejectCauseNotification(int cause) {
        return false;
    }
    /**
     * Ignore for Femtocell for given cause and state.
     * @param state current state
     * @param cause current cause
     * @return true/false
     */
    public boolean needIgnoreFemtocellUpdate(int state, int cause) {
        return false;
    }
    /**
     * Show CSG ID or not.
     * @param hnbName HNB name of network
     * @return true/false
     */
    public boolean needToShowCsgId() {
        return true;
    }
    public boolean needBlankDisplay(int cause) {
        return false;
    }
    public boolean needIgnoredState(int state, int newState, int cause) {
        if ((state == ServiceState.STATE_IN_SERVICE) && (newState == 2)) {
            /* Don't update for searching state, there shall be final registered state
               update later */
            Log.i(TAG, "set dontUpdateNetworkStateFlag for searching state");
            return true;
        }

        /* -1 means modem didn't provide <cause> information. */
        if (cause != -1) {
            // [ALPS01384143] need to check if previous state is IN_SERVICE for invalid sim
            if ((state == ServiceState.STATE_IN_SERVICE) && (newState == 3) && (cause != 0)) {
            //if((newState == 3) && (cause != 0)){
                /* This is likely temporarily network failure, don't update for better UX */
                Log.i(TAG, "set dontUpdateNetworkStateFlag for REG_DENIED with cause");
                return true;
            //[ALPS01976914] - start
            } else if ((state == ServiceState.STATE_IN_SERVICE) && (newState == 0) && (cause != 0)) {
                Log.i(TAG, "set dontUpdateNetworkStateFlag for NOT_REG_AND_NOT_SEARCH with cause");
                return true;
            }
            //[ALPS01976914] - end
        }

        Log.i(TAG, "clear dontUpdateNetworkStateFlag");

        return false;
    }

    public boolean ignoreDomesticRoaming() {
        return false;
    }

    public int mapGsmSignalLevel(int asu, int gsmRscpQdbm) {
        int level;
        // [ALPS01055164] -- START , for 3G network
        if (gsmRscpQdbm < 0) {
            // 3G network
            if (asu <= 5 || asu == 99) {
                level = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            } else if (asu >= 15) {
                level = SignalStrength.SIGNAL_STRENGTH_GREAT;
            } else if (asu >= 12) {
                level = SignalStrength.SIGNAL_STRENGTH_GOOD;
            } else if (asu >= 9) {
                level = SignalStrength.SIGNAL_STRENGTH_MODERATE;
            } else {
                level = SignalStrength.SIGNAL_STRENGTH_POOR;
            }
        // [ALPS01055164] -- END
        } else {
            // 2G network
            if (asu <= 2 || asu == 99) {
                level = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            } else if (asu >= 12) {
                level = SignalStrength.SIGNAL_STRENGTH_GREAT;
            } else if (asu >= 8) {
                level = SignalStrength.SIGNAL_STRENGTH_GOOD;
            } else if (asu >= 5) {
                level = SignalStrength.SIGNAL_STRENGTH_MODERATE;
            } else {
                level = SignalStrength.SIGNAL_STRENGTH_POOR;
            }
        }
        return level;
    }

    //[ALPS01440836][ALPS01594704]-START: change level mapping rule of signal for CMCC
    public int mapLteSignalLevel(int mLteRsrp, int mLteRssnr, int mLteSignalStrength) {
        /*
         * TS 36.214 Physical Layer Section 5.1.3 TS 36.331 RRC RSSI = received
         * signal + noise RSRP = reference signal dBm RSRQ = quality of signal
         * dB= Number of Resource blocksxRSRP/RSSI SNR = gain=signal/noise ratio
         * = -10log P1/P2 dB
         */
        int rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        int rsrpIconLevel = -1;
        int snrIconLevel = -1;

        if (mLteRsrp > -44) {
            rsrpIconLevel = -1;
        } else if (mLteRsrp >= -85) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (mLteRsrp >= -95) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (mLteRsrp >= -105) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (mLteRsrp >= -115) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else if (mLteRsrp >= -140) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }

        /*
         * Values are -200 dB to +300 (SNR*10dB) RS_SNR >= 13.0 dB =>4 bars 4.5
         * dB <= RS_SNR < 13.0 dB => 3 bars 1.0 dB <= RS_SNR < 4.5 dB => 2 bars
         * -3.0 dB <= RS_SNR < 1.0 dB 1 bar RS_SNR < -3.0 dB/No Service Antenna
         * Icon Only
         */
        if (mLteRssnr > 300) {
            snrIconLevel = -1;
        } else if (mLteRssnr >= 130) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (mLteRssnr >= 45) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (mLteRssnr >= 10) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (mLteRssnr >= -30) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else if (mLteRssnr >= -200) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }
        Log.i(TAG, "getLTELevel - rsrp:" + mLteRsrp + " snr:" + mLteRssnr + " rsrpIconLevel:"
                + rsrpIconLevel + " snrIconLevel:" + snrIconLevel);

        /* Choose a measurement type to use for notification */
        if (snrIconLevel != -1 && rsrpIconLevel != -1) {
            /*
             * The number of bars displayed shall be the smaller of the bars
             * associated with LTE RSRP and the bars associated with the LTE
             * RS_SNR
             */
            return (rsrpIconLevel < snrIconLevel ? rsrpIconLevel : snrIconLevel);
        }

        if (snrIconLevel != -1) {
            return snrIconLevel;
        }

        if (rsrpIconLevel != -1) {
            return rsrpIconLevel;
        }

        /* Valid values are (0-63, 99) as defined in TS 36.331 */
        if (mLteSignalStrength > 63) {
            rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        } else if (mLteSignalStrength >= 12) {
            rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (mLteSignalStrength >= 8) {
            rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (mLteSignalStrength >= 5) {
            rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (mLteSignalStrength >= 0) {
            rssiIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        }
        Log.i(TAG, "getLTELevel - rssi:" + mLteSignalStrength + " rssiIconLevel:"
                + rssiIconLevel);
        return rssiIconLevel;
    }
    //[ALPS01440836][ALPS01594704]-END: change level mapping rule of signal for CMCC

    public int mapGsmSignalDbm(int gsmRscpQdbm, int asu) {
        int dBm;
        Log.d(TAG, "mapGsmSignalDbm() gsmRscpQdbm=" + gsmRscpQdbm + " asu=" + asu);
        if (gsmRscpQdbm < 0) {
            dBm = gsmRscpQdbm / 4; //Return raw value for 3G Network
        } else {
            dBm = -113 + (2 * asu);
        }
        return dBm;
    }

    public void log(String text) {
        Log.d(TAG, text);
    }

    public Map<String, String> loadSpnOverrides() {
        return null;
    }

    public boolean allowSpnDisplayed() {
        return true;
    }

    public boolean supportEccForEachSIM() {
        return false;
    }

    public void updateOplmn(Context context, Object ci) {
    }

    //[ALPS01558804] MTK-START: send notification for using some spcial icc card
    public boolean needIccCardTypeNotification(String iccCardType) {
        return false;
    }

    //[ALPS01862093]-Start: not supprot auto switch rat for SIM card type
    //du to UI spec. define chagend
    public int needAutoSwitchRatMode(int phoneId, String nwPlmn) {
        return -1;
    }

    /**
     * Return network mode for India 3M Project.
     *
     * @param phone the gsmphone.
     * @param roaming romaing or not for GSM.
     * @return network type for india3M if insert CMCC card.
     */
    public int getNetworkModeFor3MProj(Object phone, boolean roaming) {
        //M: For India 3M project, insert CMCC card, only show GSM only
        int projectType = SvlteRatController.getSvlteProjectType();
        IccRecords records = ((PhoneBase) phone).mIccRecords.get();

        if ((SvlteRatController.SVLTE_PROJ_SC_3M == projectType
             || SvlteRatController.SVLTE_PROJ_DC_3M == projectType)
             && (records != null)) {
            log("setDeviceRatMode, roaming = " + roaming);
            if (records.getOperatorNumeric() != null
                && (records.getOperatorNumeric().equals("46000")
                || records.getOperatorNumeric().equals("46002")
                || records.getOperatorNumeric().equals("46007")
                || records.getOperatorNumeric().equals("46008"))
                && !roaming) {
                log("setDeviceRatMode, operator = " + records.getOperatorNumeric());
                return RILConstants.NETWORK_MODE_GSM_ONLY;
            }
        }
        return -1;
    }

    //re-mark
    /*
    //[ALPS01577029] To support auto switch rat mode
    public int needAutoSwitchRatMode(int phoneId,String nwPlmn) {
        int simType = -1; // 0: SIM , 1: USIM
        int currentNetworkMode = -1;
        int userNetworkMode = -1;
        Phone[] phones = null; //all phone proxy instance
        Phone phoneProxy = null; //current phone proxy instance


        // For World phone OM version START
        if ((SystemProperties.getInt("ro.mtk_lte_support", 0) == 1) &&
            (SystemProperties.getInt("ro.mtk_world_phone", 0) == 1)) {
            //get sim switch status
            int switchStatus = Integer.valueOf(
                    SystemProperties.get(PhoneConstants.CAPABILITY_SWITCH_PROP, "1"));

            log("needAutoSwitchRatMode,phoneId=" + phoneId+", switchStatus="+switchStatus+
                ",SubscriptionManager.isValidPhoneId(phoneId)="+SubscriptionManager.isValidPhoneId(phoneId));

            if ((phoneId == (switchStatus - 1)) &&
	               SubscriptionManager.isValidPhoneId(phoneId)) {
                phones = PhoneFactory.getPhones();
                if (phones.length > phoneId) {
                    phoneProxy = phones[phoneId];
                }

                if (phoneProxy == null) {
                    log("needSwitchRatMode()= -1 cause phone proxy is null");
                    return currentNetworkMode;
                }

                String simtype = null;
                simtype = ((PhoneProxy)phoneProxy).getIccCard().getIccCardType();
                if (simtype != null && simtype.equals("SIM")) {
                    simType = 0;
                } else if (simtype != null && simtype.equals("USIM")) {
                    simType = 1;
                }

                if (simType == 0) {//SIM
                    //get rat mode if user has change it
                    userNetworkMode = Settings.Global.getInt(mContext.getContentResolver(),
                                            Settings.Global.USER_PREFERRED_NETWORK_MODE, -1);

                    if (userNetworkMode >= Phone.NT_MODE_WCDMA_PREF) {
                        currentNetworkMode = userNetworkMode;
                        if(userNetworkMode >= Phone.NT_MODE_LTE_GSM_WCDMA) {
                            currentNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        }
                        log("DefaultServiceStateExt needSwitchRatMode for SIM: userNetworkMode=" + userNetworkMode);
                    } else {
                        //log("needSwitchRatMode: set Rat to 2/3G auto");
                        currentNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    }
                } else if (simType == 1) {//USIM
                    //get rat mode if user has change it
                    userNetworkMode = Settings.Global.getInt(mContext.getContentResolver(),
                                            Settings.Global.USER_PREFERRED_NETWORK_MODE, -1);

                    if (userNetworkMode >= Phone.NT_MODE_WCDMA_PREF) {
                        log("DefaultServiceStateExt needSwitchRatMode for USIM: userNetworkMode=" + userNetworkMode);
                        currentNetworkMode = userNetworkMode;
                    } else {
                        //log("needSwitchRatMode: set Rat to 4/3/2G");
                        currentNetworkMode = Phone.NT_MODE_LTE_GSM_WCDMA;
                    }
                } else {
                    log("DefaultServiceStateExt unknown sim type, do nothing");
                }
            }
        }
        // For World phone OM version END

        log("DefaultServiceStateExt currentNetworkMode = "+currentNetworkMode+" ,simType= "+simType);
        return currentNetworkMode;
    }
    */
    //[ALPS01862093]-End

    public boolean isSupportRatBalancing() {
        return false;
    }

    /**
     * Return if roaming for special SIM.
     *
     * @param strServingPlmn The operator numberic get from service state.
     * @param strHomePlmn The mcc+mnc get from SIM IMSI.
     * @return if roaming for the special SIM
     */
    public boolean isRoamingForSpecialSIM(String strServingPlmn, String strHomePlmn) {
        return false;
    }
}

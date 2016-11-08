package com.mediatek.op.telephony;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyProperties;

//[ALPS01577029]-START
//To support auto switch rat mode to 2G only
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.mediatek.telephony.TelephonyManagerEx;
// [ALPS01577029]-END

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IServiceStateExt")
public class ServiceStateExtOP01 extends DefaultServiceStateExt {
    private static final String ACTION_EMMRRS_PS_RESUME_INDICATOR
            = "android.intent.action.EMMRRS_PS_RESUME";
    private Context mContext;

    //[ALPS01646248] To support auto switch rat mode to 2G only for 3M TDD csfb project when inserting non-CMCC China operator Card
    private String[] CU_CT_GSM_ONLY_PLMN_SIM = {"46001", "46006", "46009", "45407", "46005", "45502","46003","46011"};
    private String[] CMCC_PLMN_SIM = {"46000", "46002", "46007", "46008"};

    public ServiceStateExtOP01() {
    }

    public ServiceStateExtOP01(Context context) {
        mContext = context;
    }

    public String onUpdateSpnDisplay(String plmn, ServiceState ss, int phoneId) {
        //[ALPS01663902]-Start
        if (plmn == null) {
            return plmn;
        }
        //[ALPS01663902]-End
        int radioTechnology;
        boolean isRoming;

        radioTechnology = ss.getRilVoiceRadioTechnology();
        isRoming = ss.getRoaming();
        log("onUpdateSpnDisplay: radioTechnology = " + radioTechnology
                + ", phoneId = " + phoneId + ",isRoming = " + isRoming);

        // for LTE
        if (radioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_LTE
                && plmn != Resources.getSystem().getText(
                        com.android.internal.R.string.lockscreen_carrier_default).toString()) {
            plmn = plmn + " 4G";
        } else if (radioTechnology > ServiceState.RIL_RADIO_TECHNOLOGY_EDGE
                && radioTechnology != ServiceState.RIL_RADIO_TECHNOLOGY_GSM
                && plmn != Resources.getSystem().getText(
                        com.android.internal.R.string.lockscreen_carrier_default).toString()) {
            plmn = plmn + " 3G";
        }
        if (isRoming) {
            String prop1 = TelephonyManager.getTelephonyProperty(
                    phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, "");
            log("getSimOperatorName simId = " + phoneId + " prop1 = " + prop1);
            if (prop1.equals("")) {
                String prop2 = TelephonyManager.getTelephonyProperty(
                        phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME, "");
                log("getMTKdefinedSimOperatorName simId = " + phoneId + " prop2 = " + prop2);
                if (!prop2.equals("")) {
                    plmn = plmn + "(" + prop2 + ")";
                }
            } else {
                plmn = plmn + "(" + prop1 + ")";
            }
        }
        log("Current PLMN: " + plmn);
        return plmn;
    }


    public int mapGsmSignalLevel(int asu, int gsmRscpQdbm) {
        int level;

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
        return level;
    }


    public int mapLteSignalLevel(int mLteRsrp, int mLteRssnr, int mLteSignalStrength) {
        int rsrpIconLevel;

        if (mLteRsrp < -140 || mLteRsrp > -44) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        } else if (mLteRsrp >= -97) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (mLteRsrp >= -105) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (mLteRsrp >= -113) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (mLteRsrp >= -120) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }
        Log.i(TAG, "op01_mapLteSignalLevel=" + rsrpIconLevel);
        return rsrpIconLevel;
    }


    public int mapGsmSignalDbm(int gsmRscpQdbm, int asu) {
        int dBm;

        if (gsmRscpQdbm < 0) {
            dBm = gsmRscpQdbm / 4; // Return raw value for TDD 3G network.
        } else {
            dBm = -113 + (2 * asu);
        }

        return dBm;
    }


    public boolean isBroadcastEmmrrsPsResume(int value) {
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
            if (value == 1) {
                Intent intent = new Intent(ACTION_EMMRRS_PS_RESUME_INDICATOR);
                mContext.sendBroadcast(intent);
                return true;
            }
        }
        return false;
    }

    public boolean needEMMRRS() {
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean needSpnRuleShowPlmnOnly() {
        return true;
    }

    /*[ALPS01577029]-START:To support auto switch rat mode:
       (1) 2G only feature:
         case 1. CT/CU sim card
         case 2. 3M TDD csfb project when we are not in china

       (2) CMCC SIM 2/3g auto & USIM 2/3/4g auto rule
         
       Returns -1 when not need to be switched, or return real rat mode
     */
    public int needAutoSwitchRatMode(int phoneId,String nwPlmn){
        boolean isTdd = false;
        String basebandCapability;
        String property_name = "gsm.baseband.capability";
        String testSimProperty = "gsm.sim.ril.testsim";
        int modemType;
        int testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
        int currentNetworkMode = -1;
        int userNetworkMode = -1;
        boolean isTestIccCard = false;

        //[ALPS01646248]
        String simOperator = null;
        int userType = UNKNOWN_USER;
        boolean isSimType = false;//sim or usim
        Phone[] phones = null; //all phone proxy instance
        Phone phoneProxy = null; //current phone proxy instance
        Phone activePhone = null; //current gsm phone instance

        //op01 need support LTE
        if (!isLTESupport()) {
            log("needSwitchRatMode()= -1 cause not LTE support");
            return currentNetworkMode;
        }

        //Get Telephony Misc Feature Config
        int miscFeatureConfig = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.TELEPHONY_MISC_FEATURE_CONFIG, 0);

        //get sim switch status
        int switchStatus = Integer.valueOf(
                SystemProperties.get(PhoneConstants.PROPERTY_CAPABILITY_SWITCH, "1"));

        log("needSwitchRatMode: miscFeatureConfig=" + miscFeatureConfig +
            ",phoneId=" + phoneId + ", switchStatus=" + switchStatus +
            ",SubscriptionManager.isValidPhoneId(phoneId)=" +
            SubscriptionManager.isValidPhoneId(phoneId));

        if ((phoneId == (switchStatus - 1)) && SubscriptionManager.isValidPhoneId(phoneId)){

            //[ALPS01646248]-Start: auto switch rat mode to 2G only when inserting non-CMCC China operator Card
            phones = PhoneFactory.getPhones();
            if (phones.length > phoneId) {
                phoneProxy = phones[phoneId];
            }

            if (phoneProxy == null) {
                log("needSwitchRatMode()= -1 cause phone proxy is null");
                return currentNetworkMode;
            }

            activePhone = ((PhoneProxy)phoneProxy).getActivePhone();;

            if (activePhone == null) {
                log("needSwitchRatMode()= -1 cause phone is null");
                return currentNetworkMode;
            }

            SIMRecords simRecords = null;
            IccRecords r = ((PhoneBase)activePhone).mIccRecords.get();
            if (r != null) {
                simRecords = (SIMRecords)r;
                String imsi = (simRecords != null) ? simRecords.getIMSI() : null;
                if (imsi != null && !imsi.equals("")) {
                    simOperator = imsi.substring(0, 5);
                }
            }
            log("needSwitchRatMode: simOperator=" + simOperator + ",nwPlmn="+ nwPlmn);
            String propStr;
            if (phoneId == 0) {
                propStr = "gsm.ril.sst.mccmnc";
            } else {
                propStr = "gsm.ril.sst.mccmnc." + (phoneId + 1);
            }
            if (simOperator == null || simOperator.equals("")) {
                // maybe Phone hasnot start read IMSI, use SystemProperties from RILD frist
                simOperator = SystemProperties.get(propStr, "");
                log("needSwitchRatMode: simOperator=" + simOperator + ",from gsm.ril.sst.mccmnc");
            } else {
                // already get IMSI from phone process, so clear it
                SystemProperties.set(propStr, "");
            }

            if (simOperator != null && !simOperator.equals("")) {
                userType = TYPE2_USER;
                for (String plmn : CU_CT_GSM_ONLY_PLMN_SIM) {
                    if (simOperator.equals(plmn)) {
                        userType = TYPE3_USER;
                        break;
                    }
                }
                for (String plmn : CMCC_PLMN_SIM) {
                    if (simOperator.equals(plmn)) {
                        userType = TYPE1_USER;
                        break;
                    }
                }
            } else {
                log("needSwitchRatMode: get simOpertor return null or empty!!");
            }

            if (userType == UNKNOWN_USER) {
                log("needSwitchRatMode()= -1 cause userType is unknown");
                return currentNetworkMode;
            }

            String simtype = null;
            simtype = ((PhoneProxy)phoneProxy).getIccCard().getIccCardType();
            if (simtype != null && simtype.equals("SIM")) {
                isSimType = true;
            }
            //[ALPS01646248]-End

            if ((phoneId >= 0 && phoneId <= 3) &&
                SystemProperties.getInt(PROPERTY_RIL_TEST_SIM[phoneId], 0) == 1) {
                isTestIccCard = true;
            }

            //if(phoneId > 0){
            //    property_name = property_name + (phoneId+1) ;
            //}

            basebandCapability = SystemProperties.get(property_name);
            if ((basebandCapability != null) && (!(basebandCapability.equals("")))){
                modemType = Integer.valueOf(basebandCapability);
                log("needSwitchRatMode: modemType="+modemType);

                if ((modemType & TDSCDMA) == TDSCDMA){
                   isTdd = true;
                }
            }

            log("needSwitchRatMode: isTdd:" + isTdd + ",isWorldPhone:" + isWorldPhoneSupport() +
                ",isLte:" + isLTESupport() + ",nwPlmn:" + nwPlmn + ",testMode:" + testMode +
                ",isTestIccCard:" + isTestIccCard + ",userType:" + userType +
                ",isSimType:" + isSimType);

            if ((testMode != 0) || (isTestIccCard == true)) {
                log("needSwitchRatMode()= -1 cause testMode or testIccCard");
                return currentNetworkMode;
            }

            if (isSimType) {
                log("needSwitchRatMode: set Rat to 2/3G auto for sim type");
                currentNetworkMode = Phone.NT_MODE_WCDMA_PREF;
            }

            if((miscFeatureConfig & PhoneConstants.MISC_FEATURE_CONFIG_MASK_AUTO_SWITCH_RAT) == 1) {
                if (userType == TYPE3_USER){//type3
                    if (!SystemProperties.get("ro.mtk_world_phone_policy").equals("1")) {
                        /* switch Rat Mode to 2G only when inserting non-CMCC China operator Card */
                        log("needSwitchRatMode: set Rat to 2G only when inserting non-CMCC Card");
                        return Phone.NT_MODE_GSM_ONLY;
                    }
                }
                //type1/2
                if (!isWorldPhoneSupport() && (isTdd == true)) {//3M
                    if (nwPlmn != null && !(isInDesignateRegion("460", nwPlmn)) &&
                        !(isLabTestPlmn(nwPlmn))) {
                        /* For 3M TDD CSFB project , we switch Rat Mode to 2G only when NOT in China */
                        return Phone.NT_MODE_GSM_ONLY;
                    }
                }
            } else {
                log("needSwitchRatMode: EM setting off");
            }
            //[ALPS01646248]-End
        }
        log("needSwitchRatMode: currentNetworkMode = " + currentNetworkMode);
        return currentNetworkMode;
    }

    private static final String[] MCC_TABLE_LAB_TEST = {
        "001", "002", "003", "004", "005", "006",
        "007", "008", "009", "010", "011", "012"
    };
    private static final String[] PLMN_TABLE_LAB_TEST = {
        "46004", "46602", "50270", "46003"
    };

    private static final String PROPERTY_RIL_TEST_SIM[] = {
        "gsm.sim.ril.testsim",
        "gsm.sim.ril.testsim.2",
        "gsm.sim.ril.testsim.3",
        "gsm.sim.ril.testsim.4",
    };

    private static final int TDSCDMA = 0x08;
    private static final int UNKNOWN_USER = 0;
    private static final int TYPE1_USER = 1; //CMCC
    private static final int TYPE2_USER = 2; //
    private static final int TYPE3_USER = 3; //CU/CT

    private boolean isInDesignateRegion(String baseMcc, String nwPlmn) {
        String mcc = nwPlmn.substring(0, 3);
        if (mcc.equals(baseMcc)) {
            Log.i(TAG,"nwPlmn: "+nwPlmn+ "is in MCC: "+baseMcc);
            return true;
        }

        Log.i(TAG,"nwPlmn: "+nwPlmn+ "NOT in MCC: "+baseMcc);
        return false;
    }

    private static boolean isLabTestPlmn(String nwPlmn) {
        String nwMcc = nwPlmn.substring(0, 3);
        for (String mcc : MCC_TABLE_LAB_TEST) {
            if (mcc.equals(nwMcc)) {
                Log.i(TAG,"Test MCC");
                return true;
            }
        }
        for (String plmn : PLMN_TABLE_LAB_TEST) {
            if (plmn.equals(nwPlmn)) {
                Log.i(TAG,"Test PLMN");
                return true;
            }
        }

        Log.i(TAG,"Not in Lab test PLMN list");

        return false;
    }
   
    private boolean isLTESupport() {
        if (SystemProperties.getInt("ro.mtk_lte_support", 0) == 1) {
            return true;
        }
        return false;
    }

    private boolean isWorldPhoneSupport() {
        if(SystemProperties.getInt("ro.mtk_world_phone", 0) == 1) {
            return true;
        }
        return false;
    }
    //[ALPS01577029]-END
}

package com.mediatek.dialer.plugin.dialpad;

import android.os.Build;
import android.os.Bundle;

import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;

import com.mediatek.custom.CustomProperties;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;
import com.mediatek.op09.plugin.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

/**
 * implement display for version info plugin for op09.
 */
public class CdmaInfoSpecification extends PreferenceActivity {

    private static final String TAG = "CdmaInfoSpecification";

    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_PRODUCT_MODEL = "product_model";
    private static final String KEY_HARDWARE_VERSION = "hardware_version";
    private static final String KEY_SOFTWARE_VERSION = "software_version";

    public static final String KEY_CDMA_INFO = "cdma_info";
    public static final String KEY_PRL_VERSION = "prl_version";
    public static final String KEY_SID = "sid";
    public static final String KEY_NID = "nid";

    public static final String KEY_MEID = "meid";
    public static final String KEY_ESN = "esn";
    public static final String KEY_IMEI_1 = "imei1";
    public static final String KEY_IMEI_2 = "imei2";

    public static final String KEY_ICCID_1 = "iccid1";
    public static final String KEY_ICCID_2 = "iccid2";

    public static final String KEY_OPERATOR_1 = "operator1";
    public static final String KEY_OPERATOR_2 = "operator2";

    public static final String KEY_UIM_ID = "uim_id";
    public static final String KEY_SUB_ID = "subid";

    public static final String SOFTWARE_VERSION_DEFAULT = "MT6735.P0";
    public static final String HARDWARE_DEFAULT = "V1";

    private String mEsn;
    private String mMeid;
    private String mUimId;
    private String mSid;
    private String mNid;
    private String mPrl;

    private HashMap<String, String> mInfoMap = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Init all information
        initInfo();
        addPreferencesFromResource(R.xml.cdma_info_specifications);
        setDeviceValuesToPreferences();
        setInfoToPreference();
    }
    /**
     * set the info from phone to preference.
     *
     * @return void.
     */
    private void setDeviceValuesToPreferences() {
        log("setPhoneValuesToPreferences()");
        PreferenceScreen parent = (PreferenceScreen) getPreferenceScreen();
        Preference preference = parent.findPreference(KEY_PRODUCT_MODEL);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_PRODUCT_MODEL));
        }

        preference = parent.findPreference(KEY_HARDWARE_VERSION);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_HARDWARE_VERSION));
        }
        preference = parent.findPreference(KEY_SOFTWARE_VERSION);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_SOFTWARE_VERSION));
        }
    }

    /**
     * set the info from cdma phone to preference.
     *
     * @param slot indicator which slot is cdma phone or invalid.
     * @return void.
     */
    private void setInfoToPreference() {
        PreferenceScreen parent = (PreferenceScreen) getPreferenceScreen();

        Preference preference = findPreference(KEY_PRL_VERSION);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_PRL_VERSION));
        }

        preference = findPreference(KEY_SID);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_SID));
        }

        preference = findPreference(KEY_NID);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_NID));
        }

        preference = findPreference(KEY_MEID);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_MEID));
        }

        preference = findPreference(KEY_IMEI_1);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_IMEI_1));
        } else {
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_IMEI_2);
        if (null != preference && null != mInfoMap.get(KEY_IMEI_2)) {
            preference.setSummary(mInfoMap.get(KEY_IMEI_2));
        } else {
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_ICCID_1);
        if (null != preference && null != mInfoMap.get(KEY_ICCID_1)) {
            preference.setSummary(mInfoMap.get(KEY_ICCID_1));
        } else {
            //preference.setVisibility(View.INVISIBLE);
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_ICCID_2);
        if (null != preference && null != mInfoMap.get(KEY_ICCID_2)) {
            preference.setSummary(mInfoMap.get(KEY_ICCID_2));
        } else {
            //preference.setVisibility(View.INVISIBLE);
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_UIM_ID);
        if (null != preference && null != mInfoMap.get(KEY_UIM_ID)) {
            preference.setSummary(mInfoMap.get(KEY_UIM_ID));
        } else {
            //preference.setVisibility(View.INVISIBLE);
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_OPERATOR_1);
        if (null != preference && null != mInfoMap.get(KEY_OPERATOR_1)) {
            preference.setSummary(mInfoMap.get(KEY_OPERATOR_1));
        } else {
            //preference.setVisibility(View.INVISIBLE);
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_OPERATOR_2);
        if (null != preference && null != mInfoMap.get(KEY_OPERATOR_2)) {
            preference.setSummary(mInfoMap.get(KEY_OPERATOR_2));
        } else {
            //preference.setVisibility(View.INVISIBLE);
            parent.removePreference(preference);
        }
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     *
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from.
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read.
     */
    private String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private CDMAPhone getCDMAPhone() {
        Phone phone = null;
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone p : phones) {
            if (p.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                phone = p;
                break;
            }
        }
        log("getCDMAPhone find cdma phone by phone list = " + phone);

        if (phone == null) {
            // Maybe this is the svlte roaming case, try it.
            int slotId = SvlteModeController.getInstance().getCdmaSocketSlotId();
            if (SubscriptionManager.isValidSlotId(slotId)) {
                SvltePhoneProxy phoneProxy = SvlteUtils.getSvltePhoneProxy(slotId);
                if (null != phoneProxy) {
                    phone = phoneProxy.getNLtePhone();
                }
            }
        }
        log("getCDMAPhone find cdma phone by phone = " + phone);

        if (phone != null && (phone.getPhoneType()
                != PhoneConstants.PHONE_TYPE_CDMA)) {
            phone = null;
        }

        CDMAPhone cdmaPhone = null;
        if (phone != null) {
            // we have got the cdma type phone.
            if (phone instanceof PhoneProxy) {
                PhoneProxy cdmaPhoneProxy = (PhoneProxy) phone;
                cdmaPhone = (CDMAPhone) cdmaPhoneProxy.getActivePhone();
            } else if (phone instanceof CDMAPhone) {
                cdmaPhone = (CDMAPhone) phone;
            }
        }
        log("getCDMAPhone = " + cdmaPhone);
        return cdmaPhone;
    }

    private void initInfo() {
        mInfoMap.clear();

        fillDeviceInfo();
        fillIMEIInfo();
        fillIccid();
        fillCdmaInfo();

        adjustInfoByRule();

        dumpInfoMap();
    }

    private void adjustInfoByRule() {
        //Decide which need to be display/undisplay.
    }

    private void fillDeviceInfo() {
        mInfoMap.put(KEY_PRODUCT_MODEL, Build.MODEL + getMsvSuffix());

        String hardWare = CustomProperties.getString(CustomProperties.MODULE_DM,
                "HardwareVersion", HARDWARE_DEFAULT);
        mInfoMap.put(KEY_HARDWARE_VERSION, hardWare);

        String softWare = CustomProperties.getString(CustomProperties.MODULE_DM,
                "SoftwareVersion", SOFTWARE_VERSION_DEFAULT);
        mInfoMap.put(KEY_SOFTWARE_VERSION, softWare);
    }

    private void fillIMEIInfo() {
        Phone[] phones = PhoneFactory.getPhones();
        if (phones != null && phones.length >= 2) {
            SvltePhoneProxy phoneProxy = SvlteUtils.getSvltePhoneProxy(0);
            if (null != phoneProxy) {
                Phone gsmPhone = phoneProxy.getLtePhone();
                log("use 0 gsm phone to get imei." + gsmPhone);
                if (null != gsmPhone) {
                    mInfoMap.put(KEY_IMEI_1, gsmPhone.getImei());
                }
            }
            SvltePhoneProxy phoneProxy1 = SvlteUtils.getSvltePhoneProxy(1);
            if (null != phoneProxy1) {
                Phone gsmPhone = phoneProxy1.getLtePhone();
                log("use 1 gsm phone to get imei." + gsmPhone);
                if (null != gsmPhone) {
                    mInfoMap.put(KEY_IMEI_2, gsmPhone.getImei());
                }
            }

            mInfoMap.put(KEY_OPERATOR_1, getNetworkId(phones[0]));
            mInfoMap.put(KEY_OPERATOR_2, getNetworkId(phones[1]));
        } else if (phones != null && phones.length == 1) {
            SvltePhoneProxy phoneProxy = SvlteUtils.getSvltePhoneProxy(0);
            if (null != phoneProxy) {
                Phone gsmPhone = phoneProxy.getLtePhone();
                log("use 0 gsm phone to get imei." + gsmPhone);
                if (null != gsmPhone) {
                    mInfoMap.put(KEY_IMEI_1, gsmPhone.getImei());
                }
            }
            mInfoMap.put(KEY_IMEI_2, null);

            mInfoMap.put(KEY_OPERATOR_1, getNetworkId(phones[0]));
            mInfoMap.put(KEY_OPERATOR_2, null);
        } else {
            mInfoMap.put(KEY_IMEI_1, null);
            mInfoMap.put(KEY_IMEI_2, null);

            mInfoMap.put(KEY_OPERATOR_1, null);
            mInfoMap.put(KEY_OPERATOR_2, null);
        }
    }

    private void fillIccid() {
        String iccid1 = SystemProperties.get("ril.iccid.sim1");
        String iccid2 = SystemProperties.get("ril.iccid.sim2");

        if (iccid1 == null || iccid1.isEmpty() || "N/A".equals(iccid1)) {
            mInfoMap.put(KEY_ICCID_1, null);
        } else {
            mInfoMap.put(KEY_ICCID_1, iccid1);
        }

        if (iccid2 == null || iccid2.isEmpty() || "N/A".equals(iccid2)) {
            mInfoMap.put(KEY_ICCID_2, null);
        } else {
            mInfoMap.put(KEY_ICCID_2, iccid2);
        }
    }

    private void fillCdmaInfo() {
        CDMAPhone phone = getCDMAPhone();
        if (phone == null) {
            mInfoMap.put(KEY_PRL_VERSION, null);
            mInfoMap.put(KEY_SID, null);
            mInfoMap.put(KEY_NID, null);
            mInfoMap.put(KEY_MEID, null);
            mInfoMap.put(KEY_UIM_ID, null);
        } else {
            mInfoMap.put(KEY_PRL_VERSION, phone.getPrl());

            boolean isInService = false;
            if (phone.getServiceState().getVoiceRegState()
                    == ServiceState.STATE_IN_SERVICE
                    || phone.getServiceState().getDataRegState()
                    == ServiceState.STATE_IN_SERVICE) {
                isInService = true;
            }

            log("fillCdmaInfo isInService = " + isInService);
            //Display the title, but no values.
            String sid = phone.getSid();
            if (sid == null || sid.isEmpty() || !isInService
                    || phone.getSubId() < 0) {
                sid = "";
            }
            mInfoMap.put(KEY_SID, sid);

            String nid = phone.getNid();
            if (nid == null || nid.isEmpty() || !isInService
                    || phone.getSubId() < 0) {
                nid = "";
            }
            mInfoMap.put(KEY_NID, nid);

            mInfoMap.put(KEY_MEID, phone.getMeid());
            if (phone.getImei() != null) {
                mInfoMap.put(KEY_UIM_ID, phone.getImei().toUpperCase());
            } else {
                mInfoMap.put(KEY_UIM_ID, null);
            }
        }
    }

    private String getNetworkId(Phone phone) {
        //Because of C2K will return in service state even no uim inserted,
        //so check the card state first.
        if (SubscriptionManager.isValidSubscriptionId(phone.getSubId())
                && ((phone.getServiceState().getVoiceRegState()
                == ServiceState.STATE_IN_SERVICE)
                || (phone.getServiceState().getDataRegState()
                == ServiceState.STATE_IN_SERVICE))) {
           String nid = phone.getServiceState().getOperatorNumeric();
           return nid.substring(0, 3) + "、" + nid.substring(3);
        }
        return null;
    }

    private void dumpInfoMap() {
        Set<String> set = mInfoMap.keySet();
        log("CdmaInfoSpecification dump start");

        log(KEY_PRODUCT_MODEL + " = " + mInfoMap.get(KEY_PRODUCT_MODEL));
        log(KEY_HARDWARE_VERSION + " = " + mInfoMap.get(KEY_HARDWARE_VERSION));
        log(KEY_SOFTWARE_VERSION + " = " + mInfoMap.get(KEY_SOFTWARE_VERSION));

        log(KEY_PRL_VERSION + " = " + mInfoMap.get(KEY_PRL_VERSION));
        log(KEY_SID + " = " + mInfoMap.get(KEY_SID));
        log(KEY_NID + " = " + mInfoMap.get(KEY_NID));
        log(KEY_MEID + " = " + mInfoMap.get(KEY_MEID));
        log(KEY_UIM_ID + " = " + mInfoMap.get(KEY_UIM_ID));

        log(KEY_IMEI_1 + " = " + mInfoMap.get(KEY_IMEI_1));
        log(KEY_IMEI_2 + " = " + mInfoMap.get(KEY_IMEI_2));

        log(KEY_ICCID_1 + " = " + mInfoMap.get(KEY_ICCID_1));
        log(KEY_ICCID_2 + " = " + mInfoMap.get(KEY_ICCID_2));

        log(KEY_OPERATOR_1 + " = " + mInfoMap.get(KEY_OPERATOR_1));
        log(KEY_OPERATOR_2 + " = " + mInfoMap.get(KEY_OPERATOR_2));

        log("CdmaInfoSpecification dump end");
    }
}

package com.mediatek.sysinfo;

import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.CellLocation;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;

import com.mediatek.custom.CustomProperties;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;
import com.mediatek.sysinfo.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * implement display for version info plugin for op09.
 */
public class CdmaInfoSpecification extends PreferenceActivity {

    private static final String TAG = "CdmaInfoSpecification";
    private static final String PACKAGE_NAME = "com.mediatek.sysinfo";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_PRODUCT_MODEL = "product_model";
    private static final String KEY_HARDWARE_VERSION = "hardware_version";
    private static final String KEY_SOFTWARE_VERSION = "software_version";

    public static final String KEY_CDMA_INFO = "cdma_info";
    public static final String KEY_PRL_VERSION = "prl_version";
    public static final String KEY_SID = "sid";
    public static final String KEY_NID = "nid";
    public static final String KEY_BASE_ID = "base_id";

    public static final String KEY_MEID = "meid";
    public static final String KEY_ESN = "esn";
    public static final String KEY_IMEI_1 = "imei1";
    public static final String KEY_IMEI_2 = "imei2";

    public static final String KEY_ICCID_1 = "iccid1";
    public static final String KEY_ICCID_2 = "iccid2";

    public static final String KEY_IMSI = "imsi";
    public static final String KEY_IMSI_1 = "imsi1";
    public static final String KEY_IMSI_2 = "imsi2";
    public static final String KEY_IMSI_LTE = "imsi_lte";
    public static final String KEY_IMSI_CDMA = "imsi_cdma";

    public static final String KEY_NETWORK_1 = "network1";
    public static final String KEY_NETWORK_2 = "network2";

    public static final String KEY_ANDROID_VERSION = "android";
    public static final String KEY_STORAGE = "storage";

    public static final String KEY_OPERATOR_1 = "operator1";
    public static final String KEY_OPERATOR_2 = "operator2";

    public static final String KEY_UIM_ID = "uim_id";
    public static final String KEY_SUB_ID = "subid";

    public static final String SOFTWARE_VERSION_DEFAULT = "MT6735.P0";
    public static final String HARDWARE_DEFAULT = "V1";

    private static final int SLOT1 = 0;
    private static final int SLOT2 = 1;

    private TelephonyManager mTelephonyManager;
    private boolean mFlightMode;
    private boolean mMeidValid = false;
    private boolean mSlotSwitch = false;
    private boolean mPhoneSwitch = false;
    private int     mMainSubId = -1;
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
        mTelephonyManager = TelephonyManager.from(this);
        mFlightMode = Settings.Global.getInt(this.getContentResolver(),
                                    Settings.Global.AIRPLANE_MODE_ON, 0) > 0;
        log("onCreate(), mFlightMode = " + mFlightMode);
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

        preference = parent.findPreference(KEY_ANDROID_VERSION);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_ANDROID_VERSION));
        }

        preference = parent.findPreference(KEY_STORAGE);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_STORAGE));
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

        preference = findPreference(KEY_BASE_ID);
        if (null != preference) {
            preference.setSummary(mInfoMap.get(KEY_BASE_ID));
        }

        preference = findPreference(KEY_IMEI_1);
        if (mPhoneSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_IMEI_2)) {
                preference.setSummary(mInfoMap.get(KEY_IMEI_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_IMEI_1)) {
                preference.setSummary(mInfoMap.get(KEY_IMEI_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_IMEI_2);
        if (mPhoneSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_IMEI_1)) {
                preference.setSummary(mInfoMap.get(KEY_IMEI_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_IMEI_2)) {
                preference.setSummary(mInfoMap.get(KEY_IMEI_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_ICCID_1);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_ICCID_2)) {
                preference.setSummary(mInfoMap.get(KEY_ICCID_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_ICCID_1)) {
                preference.setSummary(mInfoMap.get(KEY_ICCID_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_ICCID_2);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_ICCID_1)) {
                preference.setSummary(mInfoMap.get(KEY_ICCID_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_ICCID_2)) {
                preference.setSummary(mInfoMap.get(KEY_ICCID_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_IMSI_LTE);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_IMSI_2)) {
                preference.setSummary(mInfoMap.get(KEY_IMSI_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_IMSI_1)) {
                preference.setSummary(mInfoMap.get(KEY_IMSI_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_IMSI_2);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_IMSI_1)) {
                preference.setSummary(mInfoMap.get(KEY_IMSI_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_IMSI_2)) {
                preference.setSummary(mInfoMap.get(KEY_IMSI_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_IMSI_CDMA);
        if (null != preference && null != mInfoMap.get(KEY_IMSI_CDMA)) {
            preference.setSummary(mInfoMap.get(KEY_IMSI_CDMA));
        } else if (null != preference) {
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_NETWORK_1);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_NETWORK_2)) {
                preference.setSummary(mInfoMap.get(KEY_NETWORK_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_NETWORK_1)) {
                preference.setSummary(mInfoMap.get(KEY_NETWORK_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_NETWORK_2);
        if (mSlotSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_NETWORK_1)) {
                preference.setSummary(mInfoMap.get(KEY_NETWORK_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_NETWORK_2)) {
                preference.setSummary(mInfoMap.get(KEY_NETWORK_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }


        preference = findPreference(KEY_UIM_ID);
        if (null != preference && null != mInfoMap.get(KEY_UIM_ID)) {
            preference.setSummary(mInfoMap.get(KEY_UIM_ID));
        } else if (null != preference) {
            parent.removePreference(preference);
        }

        preference = findPreference(KEY_OPERATOR_1);
        if (mPhoneSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_OPERATOR_2)) {
                preference.setSummary(mInfoMap.get(KEY_OPERATOR_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_OPERATOR_1)) {
                preference.setSummary(mInfoMap.get(KEY_OPERATOR_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        }

        preference = findPreference(KEY_OPERATOR_2);
        if (mPhoneSwitch) {
            if (null != preference && null != mInfoMap.get(KEY_OPERATOR_1)) {
                preference.setSummary(mInfoMap.get(KEY_OPERATOR_1));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
        } else {
            if (null != preference && null != mInfoMap.get(KEY_OPERATOR_2)) {
                preference.setSummary(mInfoMap.get(KEY_OPERATOR_2));
            } else if (null != preference) {
                parent.removePreference(preference);
            }
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

    private void fillSimInfo() {
        log("fillSimInfo");
        fillIccid();
        fillImsi();
        //fillBaseId();
        fillNetworkInfo();
        fillIMEIInfo();
        fillCdmaInfo();
    }

    private void initInfo() {
        initSimInfo();
        mInfoMap.clear();

        fillDeviceInfo();
        fillSimInfo();

        adjustInfoByRule();

        dumpInfoMap();
    }

    private void adjustInfoByRule() {
        //Decide which need to be display/undisplay.
    }

    private boolean isSimSlotSwitch(int subId) {
        log("isSimSlotSwitch, subId = " + subId);
        if (subId < 0) {
            return false;
        }

        int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId == SLOT2) {
            log("isSimSlotSwitch true");
            return true;
        }
        return false;
    }

    private void initSimInfo() {
        mMainSubId = SubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.INVALID_SUBSCRIPTION_ID == mMainSubId) {
            mMainSubId = get4GCapabilitySubId();
        }
        mSlotSwitch = isSimSlotSwitch(mMainSubId);
    }

    private int get4GCapabilitySubId() {
        ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (iTelephony != null) {
            for (int i = 0; i < telephonyManager.getPhoneCount(); i++) {
                try {
                    log("get4GCapabilitySubId, getRadioAccessFamily(" + i + "): "
                        + iTelephony.getRadioAccessFamily(i, PACKAGE_NAME));
                    if (((iTelephony.getRadioAccessFamily(i, PACKAGE_NAME) &
                        (RadioAccessFamily.RAF_UMTS | RadioAccessFamily.RAF_LTE)) > 0)) {
                        subId = SubscriptionManager.getSubIdUsingPhoneId(i);
                        log("get34GCapabilitySubId success, subId: " + subId);
                        return subId;
                    }
                } catch (RemoteException e) {
                    log("get4GCapabilitySubId FAIL to getPhoneRat i" + i +
                        " error:" + e.getMessage());
                }
            }
        }
        return subId;
    }

    private void fillBaseId() {
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone p : phones) {
            int phoneId = p.getPhoneId();
            int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
            if (SubscriptionManager.INVALID_SUBSCRIPTION_ID < subId) {
                CellLocation location = p.getCellLocation();
                if (location instanceof CdmaCellLocation) {
                    int baseId = ((CdmaCellLocation)location).getBaseStationId();
                    if (baseId > 0) {
                        mInfoMap.put(KEY_BASE_ID, "" + baseId);
                    } else {
                        mInfoMap.put(KEY_BASE_ID, null);
                    }
                }
                int slot = SubscriptionManager.getSlotId(subId);
                log("fillBaseId, slot = " + slot);
            }
        }
    }

    private void fillDeviceInfo() {
        mInfoMap.put(KEY_PRODUCT_MODEL, Build.MODEL + getMsvSuffix());

        String hardWare = CustomProperties.getString(CustomProperties.MODULE_DM,
                "HardwareVersion", HARDWARE_DEFAULT);
        mInfoMap.put(KEY_HARDWARE_VERSION, hardWare);

        String softWare = CustomProperties.getString(CustomProperties.MODULE_DM,
                "SoftwareVersion", SOFTWARE_VERSION_DEFAULT);
        mInfoMap.put(KEY_SOFTWARE_VERSION, softWare);

        String version = android.os.Build.VERSION.RELEASE;
        String swVersion = "Android" + version;
        mInfoMap.put(KEY_ANDROID_VERSION, swVersion);

        long storageSize = getStorageSize();
        String size = "" + storageSize + "M";
        mInfoMap.put(KEY_STORAGE, size);
    }

    private void fillIMEIInfo() {
        Phone[] phones = PhoneFactory.getPhones();
        if (phones != null && phones.length >= 1) {
            if (mMainSubId >= 0) {
                log("fillIMEIInfo, mMainSubId = " + mMainSubId +
                    ", phoneSubId = " + phones[0].getSubId());
                if (phones[0].getSubId() != mMainSubId) {
                    mPhoneSwitch = true;
                }
            }
        }

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

    private void fillImsi() {
        String imsiArray = SystemProperties.get("gsm.sim.operator.imsi");
        String[] imsi = imsiArray.split(",");
        for (int i = 0; i < imsi.length; i++) {
            log("ims id = " + i + ", ims = " + imsi[i]);
            int id = i + 1;
            if (imsi[i] == null || imsi[i].isEmpty()) {
                mInfoMap.put(KEY_IMSI + id, null);
            } else {
                mInfoMap.put(KEY_IMSI + id, imsi[i]);
            }
        }
    }

    private int getNetworkType(int subId) {
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (mFlightMode) {
            return networkType;
        }

        final int dataNetworkType = mTelephonyManager.getDataNetworkType(subId);
        final int voiceNetworkType = mTelephonyManager.getVoiceNetworkType(subId);
        Log.d(TAG, "updateNetworkType(), dataNetworkType = " + dataNetworkType
                + ", voiceNetworkType = " + voiceNetworkType);
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != dataNetworkType) {
            networkType = dataNetworkType;
        } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != voiceNetworkType) {
            networkType = voiceNetworkType;
        }
        return networkType;
    }

    private void fillNetworkInfo() {
        int sub1 = getSubIdBySlotId(SLOT1);
        log("getSubId, sub1 = " + sub1);
        if (sub1 > SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            int network = getNetworkType(sub1);
            String networkType = parseNetwokType(network);
            mInfoMap.put(KEY_NETWORK_1, networkType);
        } else {
            mInfoMap.put(KEY_NETWORK_1, null);
        }

        int sub2 = getSubIdBySlotId(SLOT2);
        log("getSubId, sub2 = " + sub2);
        if (sub2 > SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            int network = getNetworkType(sub2);
            String networkType = parseNetwokType(network);
            mInfoMap.put(KEY_NETWORK_2, networkType);
        } else {
            mInfoMap.put(KEY_NETWORK_2, null);
        }
    }

    private long getStorageSize() {
        File path = null;
        boolean sharedSd = SystemProperties.get("ro.mtk_shared_sdcard").equals("1");
        if (sharedSd) {
            path = Environment.getLegacyExternalStorageDirectory();
        } else {
            path = Environment.getDataDirectory();
        }
        log("Storage path is " + path);
        StatFs statFs = new StatFs(path.getPath());
        long blockSize = statFs.getBlockSize();
        long blockCount = statFs.getBlockCount();
        log("Storage blockSize is " + blockSize + ", blockCount is " + blockCount);
        long size = (blockSize * blockCount) / (1024 * 1024);
        return size;
    }

    private int getSubIdBySlotId(int slot) {
        int[] subIds = SubscriptionManager.getSubId(slot);
        if (subIds == null) {
            return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        }

        return subIds[0];
    }

    private String parseNetwokType(int network) {
        log("parseNetwokType network = " + network);
        String networkType = null;
        switch (network) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                networkType = "GPRS";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                networkType = "EDGE";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                networkType = "UMTS";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                networkType = "IS95";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                networkType = "EVDO_0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                networkType = "EVDO_A";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                networkType = "1xRTT";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                networkType = "HSDPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                networkType = "HSUPA";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                networkType = "HSPA";
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                networkType = "IDEN";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                networkType = "EVDO_B";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                networkType = "LTE";
                break;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                networkType = "eHRPD";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                networkType = "HSPA+";
                break;
            case TelephonyManager.NETWORK_TYPE_GSM:
                networkType = "GSM";
                break;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                networkType = "Unknown";
                break;
            default:
                break;
        }
        return networkType;
    }

    private void fillCdmaInfo() {
        CDMAPhone phone = getCDMAPhone();
        if (phone == null) {
            mInfoMap.put(KEY_PRL_VERSION, null);
            mInfoMap.put(KEY_SID, null);
            mInfoMap.put(KEY_NID, null);
            mInfoMap.put(KEY_MEID, null);
            mInfoMap.put(KEY_UIM_ID, null);
            mInfoMap.put(KEY_IMSI_CDMA, null);
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
            if (sid == null || sid.isEmpty() || phone.getSubId() < 0) {
                sid = "";
            }
            mInfoMap.put(KEY_SID, sid);

            String nid = phone.getNid();
            if (nid == null || nid.isEmpty() || phone.getSubId() < 0) {
                nid = "";
            }
            mInfoMap.put(KEY_NID, nid);

            mInfoMap.put(KEY_MEID, phone.getMeid());
            if (phone.getImei() != null) {
                mInfoMap.put(KEY_UIM_ID, phone.getImei().toUpperCase());
            } else {
                mInfoMap.put(KEY_UIM_ID, null);
            }

            int cdmaSubId = phone.getSubId();
            if (cdmaSubId > 0 /*&& (cdmaSubId == mMainSubId)*/) {
                mInfoMap.put(KEY_IMSI_CDMA, phone.getSubscriberId());
            } else {
                mInfoMap.put(KEY_IMSI_CDMA, null);
            }

            int baseId = ((CdmaCellLocation)phone.getCellLocation()).getBaseStationId();
            log("CdmaCellLocation baseId = " + baseId);
            if (baseId > 0 && cdmaSubId > 0 && !mFlightMode) {
                mInfoMap.put(KEY_BASE_ID, "" + baseId);
            } else {
                mInfoMap.put(KEY_BASE_ID, null);
            }

        }
    }

    private String parseNetworkId(Phone phone) {
        if (phone == null) {
            return null;
        }

        ServiceState serviceState = phone.getServiceState();
        if (serviceState == null) {
            return null;
        }
        if ((serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE) ||
            (serviceState.getVoiceRegState() == ServiceState.STATE_IN_SERVICE)) {
            String nid = serviceState.getOperatorNumeric();
            return nid.substring(0, 3) + "、" + nid.substring(3);
        }
        return null;
    }

    private String getNetworkId(Phone phone) {
        //Because of C2K will return in service state even no uim inserted,
        //so check the card state first.
        if (mFlightMode) {
            return null;
        }
        Phone ltePhone = null;
        Phone cdmaPhone = null;
        Phone gsmPhone = null;
        if (phone.getPhoneType()== PhoneConstants.PHONE_TYPE_CDMA) {
            cdmaPhone = (CDMAPhone)((PhoneProxy) phone).getActivePhone();
            if (cdmaPhone != null && phone instanceof SvltePhoneProxy) {
                ltePhone = ((SvltePhoneProxy) phone).getLtePhone();
                Log.d(TAG, "ltePhone = " + ltePhone);
                Log.d(TAG, "cdmaPhone = " + cdmaPhone);
            }
        } else {
            gsmPhone = phone;
        }

        if (ltePhone != null) {
            String nid = null;
            ServiceState serviceState = ltePhone.getServiceState();
            if (serviceState != null) {
                if ((serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE) &&
                    (serviceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE)) {
                    nid = serviceState.getDataOperatorNumeric();
                    String mnc = nid.substring(0, 3) + "、" + nid.substring(3);
                    Log.d(TAG, "mnc = " + mnc);
                    return mnc;
                }
            }
        }

        String network = parseNetworkId(cdmaPhone);
        Log.d(TAG, "cdmaPhone, network = " + network);
        if (network == null) {
            network = parseNetworkId(gsmPhone);
            Log.d(TAG, "gsmPhone, network = " + network);
        }
        return network;
    }

    private String getCdmaNetworkId(Phone phone) {
        int phoneId = 0;
        if (phone != null) {
            phoneId = phone.getPhoneId();
        }
        String value = mTelephonyManager.getTelephonyProperty(phoneId,
                TelephonyProperties.PROPERTY_OPERATOR_NUMERIC,
                "");
        Log.d(TAG,"value = " + value);
        //Because of C2K will return in service state even no uim inserted,
        //so check the card state first.
        if (SubscriptionManager.isValidSubscriptionId(phone.getSubId())
                && ((phone.getServiceState().getVoiceRegState()
                == ServiceState.STATE_IN_SERVICE)
                || (phone.getServiceState().getDataRegState()
                == ServiceState.STATE_IN_SERVICE))) {
           return value.substring(0, 3) + "、" + value.substring(3);
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
        log(KEY_BASE_ID + " = " + mInfoMap.get(KEY_BASE_ID));

        log(KEY_IMEI_1 + " = " + mInfoMap.get(KEY_IMEI_1));
        log(KEY_IMEI_2 + " = " + mInfoMap.get(KEY_IMEI_2));

        log(KEY_ICCID_1 + " = " + mInfoMap.get(KEY_ICCID_1));
        log(KEY_ICCID_2 + " = " + mInfoMap.get(KEY_ICCID_2));

        log(KEY_OPERATOR_1 + " = " + mInfoMap.get(KEY_OPERATOR_1));
        log(KEY_OPERATOR_2 + " = " + mInfoMap.get(KEY_OPERATOR_2));

        log(KEY_IMSI_1 + " = " + mInfoMap.get(KEY_IMSI_1));
        log(KEY_IMSI_2 + " = " + mInfoMap.get(KEY_IMSI_2));
        log(KEY_IMSI_CDMA + " = " + mInfoMap.get(KEY_IMSI_CDMA));

        log(KEY_NETWORK_1 + " = " + mInfoMap.get(KEY_NETWORK_1));
        log(KEY_NETWORK_2 + " = " + mInfoMap.get(KEY_NETWORK_2));

        log(KEY_STORAGE+ " = " + mInfoMap.get(KEY_STORAGE));
        log(KEY_ANDROID_VERSION+ " = " + mInfoMap.get(KEY_ANDROID_VERSION));

        log("CdmaInfoSpecification dump end");
    }
}

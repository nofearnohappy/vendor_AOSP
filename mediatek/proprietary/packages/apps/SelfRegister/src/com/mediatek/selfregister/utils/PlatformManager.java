package com.mediatek.selfregister.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.custom.CustomProperties;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.selfregister.Const;
import com.mediatek.selfregister.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PlatformManager {

    private static final String TAG = Const.TAG_PREFIX + "PlatformManager";

    private static final String PREFERENCE_FILE_0 = "preference_file_0";
    private static final String PREFERENCE_FILE_1 = "preference_file_1";

    private static final String OPERATOR_CT_4G = "46011";
    private static final String OPERATOR_CT_MAC = "45502";
    private static final String OPERATOR_CT = "46003";

    private static final int MEID_LENGTH = 14;
    private static final int IMEI_LENGTH = 15;

    // M: Index of misc feature config switch in engineer mode.
    private static final int BIT_MISC_CONFIG = 2;
    private static final String KEY_MISC_CONFIG = Settings.Global.TELEPHONY_MISC_FEATURE_CONFIG;

    private static final String VALUE_DEFALUT_SOFTWARE_VERSION = "L1.P1";
    private static final String VALUE_DEFAULT_MANUFACTURER = "MTK";
    private static final String VALUE_DEFAULT_MEID = "A0000100001000";

    private static final String INDICATOR_FILE_NAME = "CTSelfRegisterIndicator";

    private TelephonyManager mTelephonyManager;

    public PlatformManager(Context context) {
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager == null) {
            throw new Error("telephony manager is null");
        }
    }

    /*
     * Whether feature is enabled
     */
    public static boolean isFeatureEnabled(ContentResolver contentResolver) {
        int config = Settings.Global.getInt(contentResolver, KEY_MISC_CONFIG, 0);
        return ((config & (1 << BIT_MISC_CONFIG)) != 0);
    }

    /*
     * if CT6M_support, CT card could in two slots;
     * else, it could only in slot0.
     */
    public static boolean supportCTForAllSlots() {
        int value = SystemProperties.getInt("ro.ct6m_support", 0);
        return (value == 1);
    }

    public boolean isSingleLoad() {
        return (mTelephonyManager.getSimCount() == 1);
    }

    public boolean hasCardInDevice(int[] slotIds) {
        for (int i = 0; i < slotIds.length; ++i) {
            if (mTelephonyManager.hasIccCard(slotIds[i])) {
                return true;
            }
        }
        return false;
    }

    public Boolean isSlotForCT(int slotId) {

        if (!mTelephonyManager.hasIccCard(slotId)) {
            Log.i(TAG, "[isSlotForCT] No sim card, return false");
            return false;
        }

        int[] subId = SubscriptionManager.getSubId(slotId);
        if (subId == null || subId[0] < 0) {
            Log.i(TAG, "[isSlotForCT] SudId not valid, return false");
            return false;
        }

        int phoneType = mTelephonyManager.getCurrentPhoneType(subId[0]);
        Log.i(TAG, "[isSlotForCT] Phone type is " + phoneType);

        if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
            String networkOperator = mTelephonyManager.getNetworkOperatorForSubscription(subId[0]);
            Log.i(TAG, "[isSlotForCT] Network operator is " + networkOperator);

            if (OPERATOR_CT.equals(networkOperator)
                    || OPERATOR_CT_MAC.equals(networkOperator)
                    || OPERATOR_CT_4G.equals(networkOperator)) {

                String simOperator = mTelephonyManager.getSimOperator(subId[0]);
                Log.i(TAG, "[isSlotForCT] Sim operator is " + simOperator);

                Boolean condition = networkOperator.equals(simOperator);
                condition |= networkOperator.equals(OPERATOR_CT)
                        && simOperator.equals(OPERATOR_CT_4G);
                condition |= networkOperator.equals(OPERATOR_CT_4G)
                        && simOperator.equals(OPERATOR_CT);
                if (condition) {
                    Log.i(TAG, "[isSlotForCT] " + slotId + " true");
                    return true;
                }
            }
        }

        Log.i(TAG, "[isSlotForCT] " + slotId + " false");
        return false;
    }

    /*
     * Get complex IMSI (For slot 0, CDMA IMS & LTE IMSI; slot 1, only need one IMSI)
     */
    public String[] getComplexImsi(Context context, int slotId) {

        Log.i(TAG, "getComplexImsi " + slotId);

        String imsiArray[] = new String[2];
        int[] subId = getSubId(slotId);

        // For slot 1, need both CDMA/LTE IMSI. If non-CT, CDMA IMSI could be empty.
        if (slotId == 0) {
            Intent imsiIntent = context.registerReceiver(null,
                    new IntentFilter(TelephonyIntents.ACTION_CDMA_CARD_IMSI));

            Log.i(TAG, "Previous intent " + imsiIntent);
            if (imsiIntent != null) {
                if (!supportCTForAllSlots()) {
                    imsiArray[0] = imsiIntent
                            .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_CSIM_IMSI);
                    imsiArray[1] = imsiIntent
                            .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_USIM_IMSI);

                } else {
                    int slotFromIntent = imsiIntent.getIntExtra(
                            TelephonyIntents.INTENT_KEY_SVLTE_MODE_SLOT_ID, -1);
                    Log.i(TAG, "Slot id from intent is " + slotFromIntent);

                    if (slotFromIntent == slotId) {
                        imsiArray[0] = imsiIntent
                                .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_CSIM_IMSI);
                        imsiArray[1] = imsiIntent
                                .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_USIM_IMSI);
                    } else {
                        // non-CT, set CDMA IMSI empty
                        imsiArray[0] = Const.VALUE_EMPTY;
                        imsiArray[1] = mTelephonyManager.getSubscriberId(subId[0]);
                    }
                }

            } else {
                String subscriberId = mTelephonyManager.getSubscriberId(subId[0]);
                if (subscriberId != null &&
                    (subscriberId.startsWith(OPERATOR_CT) ||
                     subscriberId.startsWith(OPERATOR_CT_MAC))) {
                    Log.i(TAG, "CT 3G: " + subscriberId);
                    // CT 3G
                    imsiArray[0] = subscriberId;
                    imsiArray[1] = null;
                } else {
                    // non-CT, set CDMA IMSI empty
                    Log.i(TAG, "Not CT: " + subscriberId);
                    imsiArray[0] = Const.VALUE_EMPTY;
                    imsiArray[1] = mTelephonyManager.getSubscriberId(subId[0]);
                }
            }
        } else {
            // For slot 1, only need one IMSI
            imsiArray[0] = mTelephonyManager.getSubscriberId(subId[0]);
            imsiArray[1] = Const.VALUE_EMPTY;
        }

        Log.i(TAG, "Slot " + slotId + " CDMA/LTE IMSI " + imsiArray[0] + "/" + imsiArray[1]);
        return imsiArray;
    }

    /*
     * If it's for IccCard, return 1; for UiccCard, return 2
     */
    public int getSimType(int slotId) {
        if (SvlteUiccUtils.getInstance().isUsim(slotId)) {
            Log.i(TAG, "It's Uicc, return 2");
            return 2;
        } else {
            Log.i(TAG, "It's icc, return 1");
            return 1;
        }
    }

    public boolean hasIccCard(int slotId) {
        return mTelephonyManager.hasIccCard(slotId);
    }

    public CellLocation getCellLocation() {
        return mTelephonyManager.getCellLocation();
    }

    public String getDeviceMeid(int slotNumber) {

        ITelephonyEx iTelephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (iTelephonyEx == null) {
            Log.e(TAG, "iTelephonyEx == null, use default " + VALUE_DEFAULT_MEID);
            return VALUE_DEFAULT_MEID;
       }

        String[] result = new String[slotNumber];
        for (int i = 0; i < slotNumber; ++i) {

            try {
                result[i] = iTelephonyEx.getSvlteMeid(i);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException " + e);
                e.printStackTrace();
                result[i] = mTelephonyManager.getDeviceId(i);
            }
            Log.i(TAG, "Result for " + i + " is " + result[i]);
        }

        for (int i = 0; i < slotNumber; ++i) {
            if (result[i] != null && result[i].length() == MEID_LENGTH) {
                Log.i(TAG, "Find a valid value " + result[i]);
                return result[i];
            }
        }

        Log.e(TAG, "No valid value, use default " + VALUE_DEFAULT_MEID);
        return VALUE_DEFAULT_MEID;
    }

    public String getImei(int slotId) {

        ITelephonyEx iTelephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (iTelephonyEx == null) {
            Log.e(TAG, "iTelephonyEx == null!");
            return null;
       }

        String imei = null;

        Log.d(TAG, "getIMEI(), get from getSvlteImei()");
        try {
            imei = iTelephonyEx.getSvlteImei(slotId);
        } catch (RemoteException e) {
            Log.e(TAG, "getIMEI(), RemoteException!");
            imei = mTelephonyManager.getDeviceId(slotId);
            e.printStackTrace();
        }

        Log.d(TAG, "getIMEI(), IMEI[" + slotId + "]:" + imei);
        if (imei == null || imei.length() != IMEI_LENGTH) {
            Log.e(TAG, "Get IMEI error! Length: " + (imei == null ? null : imei.length()));
            return "";
        }

        return imei;
    }

    public boolean isIccIDIdentical(String[] iccIdOnSim, int slotNumber) {
        String[] iccIDFromDevice = AgentProxy.getInstance().getSavedIccId(slotNumber);

        if (iccIdOnSim.length != iccIDFromDevice.length) {
            Log.d(TAG, "ICCID is not the same: array length is not equal.");
            return false;
        }

        for (int i = 0; i < slotNumber; i++) {
            Log.i(TAG, "iccId [" + i + "] on sim is " + iccIdOnSim[i]);
            Log.i(TAG, "iccId [" + i + "] saved is " + iccIDFromDevice[i]);
        }

        if (iccIdOnSim.length == 1) {
            return iccIdOnSim[0].equals(iccIDFromDevice[0]);
        }

        Boolean condition = iccIdOnSim[0].equals(iccIDFromDevice[0])
                && iccIdOnSim[1].equals(iccIDFromDevice[1]);
        condition |= iccIdOnSim[0].equals(iccIDFromDevice[1])
                && iccIdOnSim[1].equals(iccIDFromDevice[0]);

        return condition;
    }

    /**
     * Get ICCID from SIM card.
     * @return Array of ICCIDs of two SIM cards.
     */
    public String[] getIccIDFromCard(int[] slotId) {

        String[] iccIdArray = new String[slotId.length];
        for (int i = 0; i < slotId.length; i++) {
            iccIdArray[i] = Const.ICCID_DEFAULT_VALUE;
        }

        for (int i = 0; i < slotId.length; i++) {
            int[] subId = SubscriptionManager.getSubId(i);

            if (subId != null && subId[0] >= 0) {
                Log.i(TAG, "subId[0] is " + subId[0]);

                String value = mTelephonyManager.getSimSerialNumber(subId[0]);
                Log.i(TAG, "value is " + value);

                if (value != null && value.length() == Const.ICCID_DEFAULT_VALUE.length()) {
                    iccIdArray[i] = value;
                }

            }
            Log.i(TAG, "iccIdArray[" + i + "] is " + iccIdArray[i]);
        }

        return iccIdArray;
    }

    public void registerPhoneListener(PhoneStateListener listener) {
        mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        mTelephonyManager.listen(listener,
                PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    public void unRegisterPhoneListener(PhoneStateListener listener) {
        Log.i(TAG, "unRegisterPhoneListener");
        mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
    }

    //------------------------------------------------------
    // Wrapper of SubscriptionManager
    //------------------------------------------------------

    public static int[] getSubId(int slotId) {
        return SubscriptionManager.getSubId(slotId);
    }

    public static int getDefaultDataSubId() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        return subId;
    }

    public static int getSlotId(int subId) {
        int slotId = SubscriptionManager.getSlotId(subId);
        return slotId;
    }

    //------------------------------------------------------
    // Function of Alarm and device related
    //------------------------------------------------------

    public static void setExactAlarm(Context context, Class<?> cls, String action, int delay) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action, null, context, cls);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        long triggerAtMillis = System.currentTimeMillis() + delay;
        alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    public static String getManufactor() {
        String manufacturer = CustomProperties.getString(CustomProperties.MODULE_DM,
                CustomProperties.MANUFACTURER, VALUE_DEFAULT_MANUFACTURER);
        Log.i(TAG, "manufacturer is " + manufacturer);
        return manufacturer;
    }

    public static String getSoftwareVersion() {
        String version = CustomProperties.getString(CustomProperties.MODULE_DM,
                "SoftwareVersion", VALUE_DEFALUT_SOFTWARE_VERSION);
        Log.i(TAG, "software version is " + version);
        return version;
    }

    public static String getHardwareVersion() {
        long systemSize = getDirectorySize(Environment.getRootDirectory());
        long dataSize = getDirectorySize(Environment.getDataDirectory());

        Log.d(TAG, "systemSize:" + systemSize + "\n" + "dataSize:" + dataSize);

        long totalSize = analyseTotalStorage(systemSize + dataSize);

        return totalSize + "G";
    }

    /**
     * If the size locate in the section of 8-16, returns 16;
     * If the size locate in the section of 16-32, returns 32;
     * And so on.
     * @param size
     * @return Integer like "16", "32", "64", Unit: G.
     */
    private static int analyseTotalStorage(long size) {
        double total = ((double) size) / (1024 * 1024 * 1024);
        Log.d(TAG, "analyseTotalStorage(), total: " + total);

        int storageSize = 1;
        while (total > storageSize) {
            storageSize = storageSize << 1;
        }

        return storageSize;
    }

    @SuppressWarnings("deprecation")
    private static long getDirectorySize(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return blockSize * totalBlocks;
    }

    //------------------------------------------------------
    // Function Related to shared preferences
    //------------------------------------------------------
    public static SharedPreferences getUniquePreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SharedPreferences getSimPreference(Context context, int index) {
        if (index == 0) {
            return context.getSharedPreferences(PREFERENCE_FILE_0, Context.MODE_PRIVATE);
        } else {
            return context.getSharedPreferences(PREFERENCE_FILE_1, Context.MODE_PRIVATE);
        }
    }

    public static void clearPreferences(Context context) {
        getUniquePreferences(context).edit().clear().commit();
        getSimPreference(context, 0).edit().clear().commit();
        getSimPreference(context, 1).edit().clear().commit();
    }

    //------------------------------------------------------
    // Function to indicator file
    //------------------------------------------------------
    public static boolean hasIndicator(Context context) {
        File dataDir = context.getFilesDir();
        File file = new File(dataDir.getAbsolutePath() + File.separator + INDICATOR_FILE_NAME);
        return file.isFile();
    }

    public static boolean removeIndicator(Context context) {
        File dataDir = context.getFilesDir();
        File file = new File(dataDir.getAbsolutePath() + File.separator + INDICATOR_FILE_NAME);
        return file.delete();
    }

    public static void createIndicator(Context context) {
        FileOutputStream fos = null;

        try {
            fos = context.openFileOutput(INDICATOR_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(Long.toString(System.currentTimeMillis()).getBytes());

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot create indicator: " + e.getLocalizedMessage());
            e.printStackTrace();

        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getLocalizedMessage());
            e.printStackTrace();

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Faled to close output stream: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Show sim's info
     */
    public void showSimInfo(int[] slotIds) {

        Log.i(TAG, "----- showSimInfo start -----");

        String meid = getDeviceMeid(slotIds.length);
        Log.i(TAG, "Meid is " + meid);

        for (int i = 0; i < slotIds.length; ++i) {

            int slotId = slotIds[i];

            if (mTelephonyManager.hasIccCard(slotId)) {
                Log.i(TAG, "****** slot " + slotId);

                int[] subId = SubscriptionManager.getSubId(i);

                if (subId != null && subId[0] >= 0) {
                    Log.i(TAG, "subId[0] is " + subId[0]);

                    String iccid = mTelephonyManager.getSimSerialNumber(subId[0]);
                    String imsi = mTelephonyManager.getSubscriberId(subId[0]);
                    Log.i(TAG, "imsi/iccid is " + imsi + "/" + iccid);
                } else {
                    Log.i(TAG, "Ivalid sub Id.");
                }
            } else {
                Log.i(TAG, "slot " + slotId + " is empty.");
            }
        }

        Log.i(TAG, "----- showSimInfo end -----");
    }

    //------------------------------------------------------
    // Service priority
    //------------------------------------------------------
    public static void stayForeground(Service service) {
        Log.i(TAG, "Start service to foreground");
        Notification notify = new Notification.Builder(service).setSmallIcon(R.drawable.icon)
              .build();
        notify.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        service.startForeground(1, notify);
    }

    public static void leaveForeground(Service service) {
        Log.i(TAG, "Stop service to foreground");
        service.stopForeground(true);
    }
}

package com.mediatek.deviceregister.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.mediatek.custom.CustomProperties;
import com.mediatek.deviceregister.Const;
import com.mediatek.deviceregister.R;
import com.mediatek.deviceregister.RegisterMessage;
import com.mediatek.telephony.SmsManagerEx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PlatformManager {

    private static final String TAG = Const.TAG_PREFIX + "PlatformManager";

    private static final String INDICATOR_FILE_NAME = "CTDeviceRegisterIndicator";

    private static final String VALUE_DEFAULT_MANUFACTURER = "MTK";
    private static final String VALUE_DEFALUT_SOFTWARE_VERSION = "L1.P1";

    private static final String OPERATOR_CT_4G = "46011";
    private static final String OPERATOR_CT_MAC = "45502";
    private static final String OPERATOR_CT = "46003";

    private static final String SERVER_ADDRESS = "10659401";
    private static final short PORT = 0;

    private static final byte COMMAND_TYPE_RECEIVED = RegisterMessage.COMMAND_TYPE_RECEIVED;

    private TelephonyManager mTelephonyManager;

    public PlatformManager(Context context) {
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager == null) {
            throw new Error("telephony manager is null");
        }
    }

    /**
     * Check if the device support multi-SIM or not.
     * @return true if support only one SIM, false if support multi-SIM.
     */
    public boolean isSingleLoad() {
        return (mTelephonyManager.getSimCount() == 1);
    }

    /*
     * if CT6M_support, CT card could in two slots;
     * else, it could only in slot0.
     */
    public static boolean supportCTForAllSlots() {
        int value = SystemProperties.getInt("ro.ct6m_support", 0);
        return (value == 1);
    }

    //------------------------------------------------------
    // Function compare
    //------------------------------------------------------

    public boolean isImsiSame(String[] imsiOnSim, int[] slotId) {
        String[] imsiFromDevice = AgentProxy.getInstance().getSavedImsi(slotId.length);

        for (int i = 0; i < imsiFromDevice.length; ++i) {
            if (imsiFromDevice[i] == null) {
                imsiFromDevice[i] = Const.VALUE_DEFAULT_IMSI;
            }
        }

        for (int i = 0; i < slotId.length; ++i) {
            Log.i(TAG, "Imsi[" + i + "] saved " + imsiFromDevice[i]);
            Log.i(TAG, "Imsi[" + i + "] on sim " + imsiOnSim[i]);
        }

        boolean condition = false;
        if (imsiOnSim.length == 1) {
            condition = imsiOnSim[0].equals(imsiFromDevice[0]);
        } else {
            condition = imsiOnSim[0].equals(imsiFromDevice[0])
                    && imsiOnSim[1].equals(imsiFromDevice[1]);
            condition |= imsiOnSim[0].equals(imsiFromDevice[1])
                    && imsiOnSim[1].equals(imsiFromDevice[0]);
        }

        if (condition) {
            Log.i(TAG, "Imsi info is the same.");
            return true;
        } else {
            Log.i(TAG, "Imsi info is different.");
            return false;
        }
    }

    public boolean hasSamePairEsn(int[] slotId) {
        String[] deviceMeid = getDeviceMeid(slotId);
        String[] deviceEsn = new String[slotId.length];
        String[] savedEsnOrMeid = getUimEsn(slotId);

        for (int i = 0; i < slotId.length; ++i) {
            deviceEsn[i] = Utils.getEsnFromMeid(deviceMeid[i]);
            Log.i(TAG, "Index " + i + " device meid/esn " + deviceMeid[i] + "/" + deviceEsn[i]);
            Log.i(TAG, "Index " + i + " saved esn " + savedEsnOrMeid[i]);
        }

        boolean condition = false;
        if (deviceMeid.length == 1) {
            condition = isInfoSame(deviceEsn[0], savedEsnOrMeid[0]);
        } else {
            condition =  isInfoSame(deviceEsn[0], savedEsnOrMeid[0]) ||
                         isInfoSame(deviceEsn[1], savedEsnOrMeid[1]) ||
                         isInfoSame(deviceEsn[0], savedEsnOrMeid[1]) ||
                         isInfoSame(deviceEsn[1], savedEsnOrMeid[0]);
        }

        if (condition) {
            Log.i(TAG, "Esn find a pair of same.");
            return true;
        } else {
            Log.i(TAG, "Esn info is different.");
            return false;
        }
    }

    private Boolean isInfoSame(String esn, String esnOrMeid) {
        Log.i(TAG, "Compare " + esn + ", " + esnOrMeid);

        // This is Esn or pEsn
        if (esnOrMeid.length() == 8) {
            return esn.equalsIgnoreCase(esnOrMeid);

        } else {
            // This is MEID
            String pEsn = Utils.getEsnFromMeid(esnOrMeid);
            return esn.equalsIgnoreCase(pEsn);
        }
    }

    //------------------------------------------------------
    // Device Meid & Uim pEsn
    //------------------------------------------------------

    /**
     * Get MEID from device.
     * @param slotId
     * @return if framework returns null, then return "".
     *         if framework returns valid MEID, then return it directly.
     */
    public String[] getDeviceMeid(int[] slotId) {
        String[] result = new String[slotId.length];
        for (int i = 0; i < slotId.length; ++i) {

            result[i] = getDeviceMeid(slotId[i]);

            if (result[i] != null) {
                result[i] = result[i].toUpperCase();
            } else {
                result[i] = "";
            }
        }
        return result;
    }

    public String getDeviceMeid(int slotId) {
        String result = mTelephonyManager.getDeviceId(slotId);
        Log.i(TAG, "Slot " + slotId + "'s meid " + result);
        return result;
    }

    public void setUimEsn(int slotId, String pEsnHex) {
        Log.i(TAG, "write pESN " + pEsnHex + " to uim.");

        int writeCommand = 222;

        byte[] pEsnByte = Utils.hexStringToBytes(pEsnHex);
        byte[] pEsnByteReverse = Utils.getReverseBytes(pEsnByte);
        int reverseLength = pEsnByteReverse.length;

        String pEsnReverseHex = Utils.bytesToHexString(pEsnByteReverse);

        // print return value, framework may need it when debug
        byte[] writeResult = doCmdOnSlot(slotId, writeCommand, reverseLength, pEsnReverseHex);
        Log.i(TAG, "Write pEsn result " + Utils.bytesToHexString(writeResult));
    }

    /**
     * @param slotId
     * @return ESN stored in UIM or "" if no valid ESN found or no ICC Card inserted.
     */
    public String[] getUimEsn(int[] slotId) {
        String[] result = new String[slotId.length];
        for (int i = 0; i < slotId.length; ++i) {
            result[i] = "";

            if (mTelephonyManager.hasIccCard(slotId[i])) {
                String value = getUimEsn(slotId[i]);
                if (value != null) {
                    result[i] = value;
                }
            }
            Log.i(TAG, "[getUimEsn] result[" + i + "] " + result[i]);
        }
        return result;
    }

    /**
     * Get the pEsn from UIM (maybe MEID):
     * @param slotId
     * @return if info is pESN, will return value like 04abcdefgh,
     *         if info is MEID, will return value like 07abcdefghijklmn,
     *         if error happend, return null.
     */
    public String getUimEsn(int slotId) {
        int readCommand = 176;
        String result = null;
        int length = 8;

        byte[] response = doCmdOnSlot(slotId, readCommand, length, null);
        if (response != null && response.length > 2) {
            Log.i(TAG, "response is " + response.toString());

            int realLength = 0;

            if (response[0] == 0x04) {
                realLength = response.length - 5;
                Log.i(TAG, "Find pESN, real length " + realLength);

            } else {
                realLength = response.length - 2;
                Log.i(TAG, "Find MEID, real length " + realLength);
            }

            byte[] realResult = new byte[realLength];
            System.arraycopy(response, 0, realResult, 0, realLength);
            result = Utils.bytesToHexString(Utils.getRealBytes(realResult));
            if (result != null) {
                result = result.toUpperCase();
            }
        } else {
            Log.i(TAG, "Error. Response is " + response);
        }

        Log.i(TAG, "Esn/Meid for slot(" + slotId + ") is " + result);
        return result;
    }

    private byte[] doCmdOnSlot(int slotId, int command, int length, String value) {
        byte[] response = null;

        int fileId = 0x6F38;
        String path = "3F007F25";

        ITelephony iTel = ITelephony.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE));
        try {
            response = iTel.iccExchangeSimIOExUsingSlot(slotId, fileId,
                    command, 0, 0, length, path, value, null);

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    //------------------------------------------------------
    // Function Related to IMSI
    //------------------------------------------------------

    public String[] getImsiFromSim(int[] slotId) {
        String[] imsiArray = new String[slotId.length];
        for (int i = 0; i < slotId.length; ++i) {
            imsiArray[i] = Const.VALUE_DEFAULT_IMSI;
        }

        for (int i = 0; i < slotId.length; ++i) {
            if (mTelephonyManager.hasIccCard(slotId[i])) {
                int[] subId = SubscriptionManager.getSubId(slotId[i]);

                if (subId == null || subId[0] == -1) {
                    Log.i(TAG, "Slot " + i + " subId not valid.");
                    continue;
                }

                String value = mTelephonyManager.getSubscriberId(subId[0]);
                Log.i(TAG, "Slot " + i + " read imsi " +  value);
                if (value != null && value.length() == Const.VALUE_DEFAULT_IMSI.length()) {
                    imsiArray[i] = value;
                }
            } else {
                Log.i(TAG, "Slot " + i + " is empty");
            }
            Log.i(TAG, "Slot[" + i + "] imsi is " +  imsiArray[i]);
        }

        return imsiArray;
    }

    /**
     * Whether uim's network operator, UIM operator and phone type is correct
     *
     * @param uimId
     * @return true or false
     */
    public boolean isSimStateValid(int slotId) {
        Log.i(TAG, "isSimStateValid " + slotId);

        if (!mTelephonyManager.hasIccCard(slotId)) {
            Log.i(TAG, "No sim card, return false");
            return false;
        }

        int[] subId = SubscriptionManager.getSubId(slotId);
        if (subId == null || subId[0] < 0) {
            Log.i(TAG, "SudId not valid, return false");
            return false;
        }

        int phoneType = mTelephonyManager.getCurrentPhoneType(subId[0]);
        Log.i(TAG, "Phone type is " + phoneType);

        if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
            String networkOperator = mTelephonyManager.getNetworkOperatorForSubscription(subId[0]);
            Log.i(TAG, "Network operator is " + networkOperator);

            if (OPERATOR_CT.equals(networkOperator)
                    || OPERATOR_CT_MAC.equals(networkOperator)
                    || OPERATOR_CT_4G.equals(networkOperator)) {

                String simOperator = mTelephonyManager.getSimOperator(subId[0]);
                Log.i(TAG, "Sim operator is " + simOperator);

                Boolean condition = networkOperator.equals(simOperator);
                condition |= networkOperator.equals(OPERATOR_CT)
                        && simOperator.equals(OPERATOR_CT_4G);
                condition |= networkOperator.equals(OPERATOR_CT_4G)
                        && simOperator.equals(OPERATOR_CT);
                if (condition) {
                    return true;
                }
            }
        }
        return false;
    }

    //------------------------------------------------------
    // Function Related to SMS message
    //------------------------------------------------------
    public void sendRegisterMessage(byte[] message, PendingIntent intent, int slotId) {
        Log.i(TAG, "Send message. length " + message.length + " from slot " + slotId);

        SmsManagerEx.getDefault().sendDataMessage(SERVER_ADDRESS, null, PORT,
                message, intent, null, slotId);
    }

    /*
     * get data from intent, and analyze it to check if register is successful.
     */
    public boolean checkRegisterResult(Intent intent) {
        Log.i(TAG, "Check register result " + intent);

        byte[] pduByte = intent.getByteArrayExtra("pdu");
        if (pduByte != null && pduByte.length > 0) {
            SmsMessage message = SmsMessage.createFromPdu(pduByte, SmsMessage.FORMAT_3GPP2);
            String originatingAddress = message.getOriginatingAddress();
            Log.i(TAG, "message originating address:" + originatingAddress);

            if (originatingAddress.equals(SERVER_ADDRESS)) {
                byte[] data = message.getUserData();
                Log.i(TAG, "message user data:" + Utils.bytesToHexString(data));

                if (data != null && data.length > 1) {
                    byte confirmByte = data[1];
                    if (confirmByte == COMMAND_TYPE_RECEIVED) {
                        Log.i(TAG, "Register success!");
                        return true;
                    }

                } else {
                    Log.i(TAG, "Message data not valid!");
                }

            } else {
                Log.i(TAG, "Originating address not valid!");
            }

        } else {
            Log.i(TAG, "Pdu is valid!");
        }
        return false;
    }

    //------------------------------------------------------
    // Function Related to CustomProperties
    //------------------------------------------------------

    public static String getManufacturer() {
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

    //------------------------------------------------------
    // Function Related to shared preferences
    //------------------------------------------------------
    public static SharedPreferences getUniquePreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void clearPreferences(Context context) {
        getUniquePreferences(context).edit().clear().commit();
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

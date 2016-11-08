package com.mediatek.esntrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Timer;
import java.util.TimerTask;

public class EsnTrackController {
    private static final String TAG = "EsnTrackController";
    private static EsnTrackController mEsnTrackcontroller;
    private LinkedHashSet<String> esnMsgQ = new LinkedHashSet<String>();
    private static boolean mFeasibleReceived = false;
    private int sEsnLength = 8;
    private int mOptrCode;

    private int mEsnTrackPU = 0xFF; // power up
    private int mEsnTrackIC = 0xFF; // for Incoming events SMS, Call
    private int mEsnTrackOG = 0xFF; // for Outgoing event SMS, Call, Data
    private int mEsnTrackUT = 0xFF; // for UTK menu selection

    private int mEsnTrackMtsPU = 0xFF;
    private int mEsnTrackMtsOC = 0xFF;
    private int mEsnTrackMtsIC = 0xFF;
    private int mEsnTrackMtsOS = 0xFF;
    private int mEsnTrackMtsIS = 0xFF;

    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private int[] mSlotList;
    private Intent mIntent;
    public static final String MY_PREFS_NAME = "MyEsnPrefsFile";
    public static final String FEASIBLE_PREF = "EsnFeasiblePref";
    public static final String RUIM_ID_PREF = "EsnRuimIdPref";
    private PhoneStateListener mPhoneStateListener = null;
    private int mSlotId = -1;

    private TimerTask mTask = new TimerTask() {
        public void run() {
            processNextMessage();
        }
    };

    private EsnTrackController(Context context, Intent intent) {
        mIntent = intent;
        mContext = context;
        Log.d(TAG, "[ESN TRACK] EsnTrackController mContext = " + mContext);
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
        if (isSingleLoad()) {
            mSlotList = Const.SINGLE_UIM_ID;
            Log.d(TAG, "Single SIM load.");
        } else {
            mSlotList = Const.UIM_ID_LIST;
            Log.d(TAG, "Dual SIM load.");
        }
        mOptrCode = getOpCodeBySpn();
        if (Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                .equalsIgnoreCase(intent.getAction())) {
            int whichSub = intent.getExtras().getInt(
                    PhoneConstants.SUBSCRIPTION_KEY);
            mSlotId = SubscriptionManager.getSlotId(whichSub);
            Log.d(TAG, "mSlotId = " + mSlotId);
        }
    }

    public static EsnTrackController getInstance() {
        Log.d(TAG, "getInstance");
        return mEsnTrackcontroller;
    }

    public static EsnTrackController getInstance(Context context, Intent intent) {
        if (mEsnTrackcontroller == null) {
            String action = null;
            if (intent != null) {
                action = intent.getAction();
                Log.d(TAG, "[ESN TRACK] getInstance action = " + action);
            }
            if (!((Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                    .equalsIgnoreCase(action)) && (EsnTrackController
                    .loadFeasibleFlagFromPref(context) == 0))) {
                if (EsnTrackController.loadFeasibleFlagFromPref(context) == 1) {
                    mFeasibleReceived = true;
                } else {
                    return null;
                }
                Log.d(TAG, "[ESN TRACK] getInstance para new here");
            }
            mEsnTrackcontroller = new EsnTrackController(context, intent);
        } else {
            Log.d(TAG,
                    "[ESN TRACK] getInstance para mEsnTrackcontroller is not null context="
                            + context);
            if (intent != null && context != null) {
                mEsnTrackcontroller.mIntent = intent;
                mEsnTrackcontroller.mContext = context;
            }
        }
        return mEsnTrackcontroller;
    }

    public void startEsnTrackService() {
        if (mOptrCode != Const.TATA && mOptrCode != Const.MTS) {
            Log.d(TAG, "[ESN TRACK] startEsnTrackService Operator not matched");
            return;
        }
        if ((Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                .equalsIgnoreCase(mIntent.getAction()))
                && (mFeasibleReceived == false)) {
            mFeasibleReceived = true;
            setFeasibleFlagReceived(mContext);
            Intent dummyIntent = new Intent(Const.ACTION_DUMMY);
            Log.d(TAG, "start dummy service");
            dummyIntent.setClass(mContext, EsnTrackService.class);
            mContext.startService(dummyIntent);

            new Thread() {
                public void run() {
                    writeEsnToUim();
                    if (isRuimIdSame() && isEsnSame()) {
                        Log.d(TAG,
                                "IMSI and ESN no change so read from Preference");
                        loadValuesfromPref();
                    } else {
                        // it mean SIM/DEVICE change it mean last combo has been
                        // changed
                        Log.d(TAG, "IMSI and ESN change so reset Preference");
                        resetPrefValues();
                        // writeRuimIdToDevice();
                    }
                    // value 1 means success ,0 means failed or Processing and
                    // 0xFF means not tried yet
                    if (resetPrefFlag(Const.PU_TYPE)) {
                        startService();
                    }
                }
            }.start();
        } else if ((Const.ACTION_CDMA_NEW_OUTGOING_CALL
                .equalsIgnoreCase(mIntent.getAction()))
                && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.OC_TYPE)) {
                startService();
            }
        } else if ((Const.ACTION_CDMA_NEW_SMS_RECVD.equalsIgnoreCase(mIntent
                .getAction())) && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.IS_TYPE)) {
                startService();
            }
        } else if ((Const.ACTION_CDMA_MT_CALL.equalsIgnoreCase(mIntent
                .getAction())) && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.IC_TYPE)) {
                startService();
            }
        } else if ((Const.ACTION_CDMA_UTK_MENU_SELECTION
                .equalsIgnoreCase(mIntent.getAction()))
                && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.UT_TYPE)) {
                startService();
            }
        } else if ((Const.ACTION_CDMA_SMS_MSG_SENT.equalsIgnoreCase(mIntent
                .getAction())) && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.OS_TYPE)) {
                startService();
            }
        } else if ((Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE
                .equalsIgnoreCase(mIntent.getAction()))
                && (mFeasibleReceived == true)) {
            if (resetPrefFlag(Const.DA_TYPE)) {
                startService();
            }
        }
    }

    private void startService() {
        if (esnMsgQ.isEmpty()) {
            mIntent.setClass(mContext, EsnTrackService.class);
            mContext.startService(mIntent);
            Log.d(TAG, "startService as the Queue is empty");
        }
        Log.d(TAG, "startService ADD ACTION TO queue");
        esnMsgQ.add(mIntent.getAction());
    }

    private boolean resetPrefFlag(int type) {
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                MY_PREFS_NAME, mContext.MODE_PRIVATE);

        if (mOptrCode == Const.TATA) {
            Log.d(TAG, "resetPrefFlag type:" + type + " " + "OG:" + mEsnTrackOG
                    + "IC:" + mEsnTrackIC + "UT:" + mEsnTrackUT);
            switch (type) {
            case Const.PU_TYPE:
                mEsnTrackPU = sharedpreferences.getInt(Const.KEY_PU_ESN, 0xFF);
                if (mEsnTrackPU == 0xFF) {
                    mEsnTrackPU = 0;
                    return true;
                }
                break;
            case Const.OC_TYPE:
            case Const.OS_TYPE:
            case Const.DA_TYPE:
                mEsnTrackOG = sharedpreferences.getInt(Const.KEY_OG_ESN, 0xFF);
                if (mEsnTrackOG == 0xFF) {
                    mEsnTrackOG = 0;
                    return true;
                }
                break;
            case Const.IC_TYPE:
            case Const.IS_TYPE:
                mEsnTrackIC = sharedpreferences.getInt(Const.KEY_IC_ESN, 0xFF);
                if (mEsnTrackIC == 0xFF) {
                    mEsnTrackIC = 0;
                    return true;
                }
                break;
            case Const.UT_TYPE:
                mEsnTrackUT = sharedpreferences.getInt(Const.KEY_UT_ESN, 0xFF);
                if (mEsnTrackUT == 0xFF) {
                    mEsnTrackUT = 0;
                    return true;
                }
                break;
            }
        } else if (mOptrCode == Const.MTS) {
            Log.d(TAG, "resetPrefFlag type:" + type + "OC:" + mEsnTrackMtsOC
                    + "IC:" + mEsnTrackMtsIC + "OS:" + mEsnTrackMtsOS + "IS:"
                    + mEsnTrackMtsIS);
            switch (type) {
            case Const.PU_TYPE:
                mEsnTrackMtsPU = sharedpreferences.getInt(Const.KEY_MTS_PU_ESN,
                        0xFF);
                if (mEsnTrackMtsPU == 0xFF) {
                    mEsnTrackMtsPU = 0;
                    return true;
                }
                break;
            case Const.OC_TYPE:
                mEsnTrackMtsOC = sharedpreferences.getInt(Const.KEY_MTS_OC_ESN,
                        0xFF);
                if (mEsnTrackMtsOC == 0xFF) {
                    mEsnTrackMtsOC = 0;
                    return true;
                }
                break;
            case Const.IC_TYPE:
                mEsnTrackMtsIC = sharedpreferences.getInt(Const.KEY_MTS_IC_ESN,
                        0xFF);
                if (mEsnTrackMtsIC == 0xFF) {
                    mEsnTrackMtsIC = 0;
                    return true;
                }
                break;
            case Const.OS_TYPE:
                mEsnTrackMtsOS = sharedpreferences.getInt(Const.KEY_MTS_OS_ESN,
                        0xFF);
                if (mEsnTrackMtsOS == 0xFF) {
                    mEsnTrackMtsOS = 0;
                    return true;
                }
                break;
            case Const.IS_TYPE:
                mEsnTrackMtsIS = sharedpreferences.getInt(Const.KEY_MTS_IS_ESN,
                        0xFF);
                if (mEsnTrackMtsIS == 0xFF) {
                    mEsnTrackMtsIS = 0;
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void setFeasibleFlagReceived(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(
                FEASIBLE_PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt(Const.KEY_RECEIVED_FEASIBLE_BROADCAST, 1);
        editor.commit();
    }

    public static void onPhoneBoot(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(
                FEASIBLE_PREF, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();
    }

    public static void SendAtToModem() {
        String s[] = new String[2];
        s[0] = "AT+ECESNT=" + "1";
        s[1] = "";
        Log.d(TAG, "SendAtToModem at command : " + s[0]);
        Phone[] phones = PhoneFactory.getPhones();

        Log.d(TAG, "SendAtToModem phone" + phones);
        if (phones[0].getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            Log.d(TAG, "SendAtToModem phone 0 is CDMA");
            phones[0].invokeOemRilRequestStrings(s, null);
        } else if (phones[1].getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            Log.d(TAG, "SendAtToModem phone 1 is CDMA");
            phones[1].invokeOemRilRequestStrings(s, null);
        }
    }

    private static int loadFeasibleFlagFromPref(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(
                FEASIBLE_PREF, context.MODE_PRIVATE);
        int status = 0;
        status = sharedpreferences.getInt(
                Const.KEY_RECEIVED_FEASIBLE_BROADCAST, 0);
        Log.d(TAG, "loadValuesfromPref FeasibleReceived " + status);
        if (status == 1) {
            return status;
        }
        return 0;
    }

    private void resetPrefValues() {
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                MY_PREFS_NAME, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        mEsnTrackPU = 0xFF;
        mEsnTrackOG = 0xFF;
        mEsnTrackIC = 0xFF;
        mEsnTrackUT = 0xFF;
        mEsnTrackMtsPU = 0xFF;
        mEsnTrackMtsOC = 0xFF;
        mEsnTrackMtsIC = 0xFF;
        mEsnTrackMtsOS = 0xFF;
        mEsnTrackMtsIS = 0xFF;
        editor.clear();
        editor.commit();
    }

    private void loadValuesfromPref() {
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                MY_PREFS_NAME, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        int status = 0;

        status = sharedpreferences.getInt(Const.KEY_PU_ESN, 0);
        Log.d(TAG, "loadValuesfromPref pu " + status);
        if (status == 1) {
            mEsnTrackPU = 1;
        } else {
            editor.putInt(Const.KEY_PU_ESN, mEsnTrackPU);
        }

        status = sharedpreferences.getInt(Const.KEY_OG_ESN, 0);
        Log.d(TAG, "loadValuesfromPref og " + status);
        if (status == 1) {
            mEsnTrackOG = 1;
        } else {
            editor.putInt(Const.KEY_OG_ESN, mEsnTrackOG);
        }

        status = sharedpreferences.getInt(Const.KEY_IC_ESN, 0);
        Log.d(TAG, "loadValuesfromPref ic " + status);
        if (status == 1) {
            mEsnTrackIC = 1;
        } else {
            editor.putInt(Const.KEY_IC_ESN, mEsnTrackIC);
        }

        status = sharedpreferences.getInt(Const.KEY_UT_ESN, 0);
        Log.d(TAG, "loadValuesfromPref ut " + status);
        if (status == 1) {
            mEsnTrackUT = 1;
        } else {
            editor.putInt(Const.KEY_UT_ESN, mEsnTrackUT);
        }

        status = sharedpreferences.getInt(Const.KEY_MTS_PU_ESN, 0);
        Log.d(TAG, "loadValuesfromPref mts pu " + status);
        if (status == 1) {
            mEsnTrackMtsPU = 1;
        } else {
            editor.putInt(Const.KEY_MTS_PU_ESN, mEsnTrackMtsPU);
        }

        status = sharedpreferences.getInt(Const.KEY_MTS_OC_ESN, 0);
        Log.d(TAG, "loadValuesfromPref mts oc " + status);
        if (status == 1) {
            mEsnTrackMtsOC = 1;
        } else {
            editor.putInt(Const.KEY_MTS_OC_ESN, mEsnTrackMtsOC);
        }

        status = sharedpreferences.getInt(Const.KEY_MTS_IC_ESN, 0);
        Log.d(TAG, "loadValuesfromPref mts ic " + status);
        if (status == 1) {
            mEsnTrackMtsIC = 1;
        } else {
            editor.putInt(Const.KEY_MTS_IC_ESN, mEsnTrackMtsIC);
        }

        status = sharedpreferences.getInt(Const.KEY_MTS_OS_ESN, 0);
        Log.d(TAG, "loadValuesfromPref mts os " + status);
        if (status == 1) {
            mEsnTrackMtsOS = 1;
        } else {
            editor.putInt(Const.KEY_MTS_OS_ESN, mEsnTrackMtsOS);
        }

        status = sharedpreferences.getInt(Const.KEY_MTS_IS_ESN, 0);
        Log.d(TAG, "loadValuesfromPref mts is " + status);
        if (status == 1) {
            mEsnTrackMtsIS = 1;
        } else {
            editor.putInt(Const.KEY_MTS_IS_ESN, mEsnTrackMtsIS);
        }
        editor.commit();

    }

    private void writeRuimIdToDevice(String ruimId) {
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                RUIM_ID_PREF, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Const.KEY_RUIM_ID, ruimId);
        editor.commit();
    }

    private void writeEsnToUim() {
        Log.d(TAG, "write pESN to UIM.");
        String meid = getDeviceId();
        Log.d(TAG, "Device id:" + meid);
        if (meid == null || meid.length() == 0) {
            Log.w(TAG, "Device id is null or empty.");
            return;
        }
        int writeCommand = 222;
        int fileId = 0x6F38;
        String path = "3F007F25";

        String pEsn = meidToEsn(meid);
        Log.d(TAG, "pESN:" + pEsn);
        byte[] pEsnByte = hexStringToBytes(pEsn);
        byte[] pEsnByteReverse = getReverseBytes(pEsnByte);
        String pEsnReverse = bytesToHexString(pEsnByteReverse);

        sEsnLength = pEsnByteReverse.length;
        Log.d(TAG, "content to write:" + pEsn + ", length:" + sEsnLength);
        ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        try {
            byte[] writeResult = iTel.iccExchangeSimIOExUsingSlot(mSlotId,
                    fileId, writeCommand, 0, 0, sEsnLength, path, pEsnReverse,
                    null);
            Log.d(TAG, "Write pEsn result:" + bytesToHexString(writeResult));

        } catch (RemoteException e) {
            Log.e(TAG, "write failed!" + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Check if the device support multi-SIM or not.
     * 
     * @return true if support only one SIM, false if support multi-SIM.
     */
    private boolean isSingleLoad() {
        if (mTelephonyManager == null) {
            Log.e(TAG,
                    "isSingleLoad(), mTelephonyManager is null! return false as default.");
            return false;
        }

        return (mTelephonyManager.getSimCount() == 1);
    }

    public String getDeviceId() {
        String meid = "";
        if (mTelephonyManager != null) {
            int uimId = getCurrentLogonUim();
            if (uimId != Const.UIM_NONE) {
                meid = mTelephonyManager.getDeviceId(uimId);
                Log.v(TAG, "[getDeviceId]DeviceId of uim (" + uimId + ") is "
                        + meid);
            }

        }
        if (meid != null) {
            meid = meid.toUpperCase();
        }

        Log.i(TAG, "[getDeviceId]meid = " + meid);
        return meid;

    }

    private int getOpCodeBySpn() {
        int currentLogonUim = Const.UIM_NONE;

        for (int uimId : mSlotList) {
            String spnName = getOpSpn(uimId);
            Log.v(TAG, "[getOpCodeBySpn] spnName API:" + spnName + "SPN Const:"
                    + Const.SPN_TATA);
            if (spnName.equalsIgnoreCase(Const.SPN_TATA)) {
                return Const.TATA;
            } else if (spnName.equalsIgnoreCase(Const.SPN_MTS)) {
                return Const.MTS;
            }
        }
        Log.d(TAG, "Current logon UIM is " + currentLogonUim);

        return 0;
    }

    private String getOpSpn(int uimId) {
        String spnName = "";
        Log.v(TAG, "[getOpSpn] begin uimId: " + uimId);
        if (mTelephonyManager.hasIccCard(uimId)) {
            int[] subId = SubscriptionManager.getSubId(uimId);
            if (subId == null || subId[0] < 0) {
                Log.e(TAG, "[getOpSpn] getSubId invalid!");
                return spnName;
            }
            int phoneType = mTelephonyManager.getCurrentPhoneType(subId[0]);
            Log.v(TAG, "[getOpSpn] phone type of uim (" + uimId + ") = "
                    + phoneType);
            if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
                spnName = mTelephonyManager
                        .getSimOperatorNameForSubscription(subId[0]);
                Log.v(TAG, "[getOpSpn] SPN of uim (" + uimId + ") is "
                        + spnName);
                return spnName;
            }
        }
        return spnName;
    }

    private int getCurrentLogonUim() {
        int currentLogonUim = Const.UIM_NONE;

        for (int uimId : mSlotList) {
            if (isUimAvailable(uimId)) {
                currentLogonUim = uimId;
                break;
            }
        }
        Log.d(TAG, "Current logon UIM is " + currentLogonUim);

        return currentLogonUim;
    }

    private boolean isUimAvailable(int uimId) {
        Log.v(TAG, "[isUimAvailable] begin uimId: " + uimId);
        if (mTelephonyManager.hasIccCard(uimId)) {
            int[] subId = SubscriptionManager.getSubId(uimId);
            if (subId == null || subId[0] < 0) {
                Log.e(TAG, "[isUimAvailable] getSubId invalid!");
                return false;
            }
            int phoneType = mTelephonyManager.getCurrentPhoneType(subId[0]);
            Log.v(TAG, "[isUimAvailable] phone type of uim (" + uimId + ") = "
                    + phoneType);
            if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
                String netwOperator = mTelephonyManager
                        .getNetworkOperatorForSubscription(subId[0]);
                Log.v(TAG, "[isUimAvailable] networkOperator of uim (" + uimId
                        + ") is " + netwOperator);

                String simOperator = mTelephonyManager.getSimOperator(subId[0]);
                Log.v(TAG, "[isUimAvailable] simOperator of uim (" + uimId
                        + ") is " + simOperator);
                String spnName = mTelephonyManager.getSimOperatorName();
                Log.v(TAG, "[isUimAvailable] SPN of uim (" + uimId + ") is "
                        + spnName);
                String spnName2 = mTelephonyManager
                        .getSimOperatorNameForSubscription(subId[0]);
                Log.v(TAG, "[isUimAvailable] SPN of uim (" + uimId + ") is "
                        + spnName2);

                return true;

            }
        }
        return false;
    }

    private String meidToEsn(String meid) {
        if (meid == null || meid.length() == 0) {
            return null;
        }
        return meid; // Currenly using MEid
        /*
         * byte[] meidByte = hexStringToBytes(meid); MessageDigest md; String
         * pEsn = null; try { md =
         * MessageDigest.getInstance(Const.MEID_TO_PESN_HASH_NAME);
         * md.update(meidByte); String result = bytesToHexString(md.digest());
         * int length = result.length(); if (length > 6) { pEsn =
         * Const.PESN_PREFIX + result.substring(length - 6, length); } else {
         * Log.e(TAG, "digest result length < 6, it is not valid:" + result); }
         * } catch (NoSuchAlgorithmException e) { Log.e(TAG,
         * "No such algorithm:" + Const.MEID_TO_PESN_HASH_NAME);
         * e.printStackTrace(); } if (pEsn != null) { pEsn = pEsn.toUpperCase();
         * } return pEsn;
         */

    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    private byte[] getReverseBytes(byte[] byteSrc) {
        byte[] resultByte = new byte[byteSrc.length + 1];
        int i = 0;
        resultByte[i] = (byte) byteSrc.length;
        for (int j = byteSrc.length - 1; j >= 0; j--) {
            i++;
            resultByte[i] = byteSrc[j];
        }
        return resultByte;
    }

    private boolean isEsnSame() {
        String uimSavedEsn = getEsnFromUim();
        String deviceEsn = meidToEsn(getDeviceId());
        if (uimSavedEsn != null && uimSavedEsn.equalsIgnoreCase(deviceEsn)) {
            Log.d(TAG, "ESN is same.");
            return true;
        } else {
            Log.d(TAG, "ESN is not same:" + uimSavedEsn + "--" + deviceEsn);
            return false;
        }
    }

    private byte[] getRuimIdReversedBytes(byte[] byteSrc) {
        byte[] resultByte = new byte[Const.RUIM_ID_EF_BYTES];
        for (int i = 0, j = Const.RUIM_ID_EF_BYTES - 1; j >= 0; j--, i++) {
            resultByte[i] = byteSrc[j];
        }
        return resultByte;
    }

    private String getRuimIdFromUim() {
        int readCommand = 176;
        int fileId = 0x6F74;
        String path = "3F007F25";
        String ruimId = "";

        ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        try {
            byte[] readResult = iTel.iccExchangeSimIOExUsingSlot(mSlotId,
                    fileId, readCommand, 0, 0, Const.RUIM_ID_EF_BYTES, path,
                    null, null);
            Log.d(TAG, "getRuimIdFromUim readResult! "
                    + bytesToHexString(readResult) + " length:"
                    + readResult.length);
            ruimId = bytesToHexString(getRuimIdReversedBytes(readResult));
            Log.d(TAG, "getRuimIdFromUim ruimId:" + ruimId);
        } catch (RemoteException e) {
            Log.e(TAG, "write failed!" + e.getMessage());
            e.printStackTrace();
        }
        return ruimId.toUpperCase();
    }

    private String getEsnFromUim() {
        int readCommand = 176;
        int fileId = 0x6F38;
        String path = "3F007F25";
        String pEsn = null;

        ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        try {
            byte[] readResult = iTel.iccExchangeSimIOExUsingSlot(mSlotId,
                    fileId, readCommand, 0, 0, sEsnLength, path, null, null);
            String s = bytesToHexString(readResult);
            Log.d(TAG, "getEsnFromUim readResult Hex string! s:" + s);
            if (readResult != null && readResult.length > 2) {
                int realLength = readResult.length - 2;
                byte[] realResult = new byte[realLength];
                System.arraycopy(readResult, 0, realResult, 0, realLength);
                pEsn = bytesToHexString(getRealBytes(realResult));
            }

        } catch (RemoteException e) {
            Log.e(TAG, "write failed!" + e.getMessage());
            e.printStackTrace();
        }

        if (pEsn != null) {
            pEsn = pEsn.toUpperCase();
        }

        return pEsn;
    }

    private byte[] getRealBytes(byte[] byteSrc) {
        byte[] resultByte = new byte[byteSrc.length - 1];
        for (int i = 0, j = byteSrc.length - 1; j > 0; j--, i++) {
            resultByte[i] = byteSrc[j];
        }
        return resultByte;
    }

    public String getRuimIdFromDevice() {
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                RUIM_ID_PREF, mContext.MODE_PRIVATE);
        return sharedpreferences.getString(Const.KEY_RUIM_ID, "");
    }

    private boolean isRuimIdSame() {
        String ruimIdFromUim = getRuimIdFromUim();
        String ruimIdFromDevice = getRuimIdFromDevice();
        Log.d(TAG, "isRuimIdSame  ruimIdFromUim:" + ruimIdFromUim);
        Log.d(TAG, "isRuimIdSame  ruimIdFromDevice:" + ruimIdFromDevice);
        boolean isSame = ruimIdFromUim.equalsIgnoreCase(ruimIdFromDevice);
        if (!isSame) {
            Log.d(TAG, "isRuimIdSame  ruimIdFromUim:" + ruimIdFromUim
                    + " ruimIdFromDevice:" + ruimIdFromDevice);
            writeRuimIdToDevice(ruimIdFromUim);
        }
        return isSame;
    }

    public void serviceFinishedProcessNext(String action, boolean result) {
        Log.d(TAG, "serviceFinishedProcessNext Queue :" + esnMsgQ);
        Log.d(TAG, "serviceFinishedProcessNext done action  :" + action);
        SharedPreferences sharedpreferences = mContext.getSharedPreferences(
                MY_PREFS_NAME, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        if (mOptrCode == Const.TATA) {
            if (action
                    .equalsIgnoreCase(Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE)) {
                mEsnTrackPU = (result ? 1 : 0);
                editor.putInt(Const.KEY_PU_ESN, mEsnTrackPU);

            } else if (action.equalsIgnoreCase(Const.ACTION_CDMA_NEW_SMS_RECVD)) {
                mEsnTrackIC = (result ? 1 : 0);
                editor.putInt(Const.KEY_IC_ESN, mEsnTrackIC);

            } else if (action.equalsIgnoreCase(Const.ACTION_CDMA_MT_CALL)) {
                mEsnTrackIC = (result ? 1 : 0);
                editor.putInt(Const.KEY_IC_ESN, mEsnTrackIC);

            } else if (action
                    .equalsIgnoreCase(Const.ACTION_CDMA_NEW_OUTGOING_CALL)) {
                mEsnTrackOG = (result ? 1 : 0);
                editor.putInt(Const.KEY_OG_ESN, mEsnTrackOG);

            } else if (action.equalsIgnoreCase(Const.ACTION_CDMA_SMS_MSG_SENT)) {
                mEsnTrackOG = (result ? 1 : 0);
                editor.putInt(Const.KEY_OG_ESN, mEsnTrackOG);

            } else if (action
                    .equalsIgnoreCase(Const.ACTION_CDMA_UTK_MENU_SELECTION)) {
                mEsnTrackUT = (result ? 1 : 0);
                editor.putInt(Const.KEY_UT_ESN, mEsnTrackUT);

            } else if (action
                    .equalsIgnoreCase(Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE)) {
                mEsnTrackOG = (result ? 1 : 0);
                editor.putInt(Const.KEY_OG_ESN, mEsnTrackOG);

            }
        } else if (mOptrCode == Const.MTS) {
            if (result) {
                mEsnTrackMtsPU = mEsnTrackMtsOC = mEsnTrackMtsIC = mEsnTrackMtsOS = mEsnTrackMtsIS = 1;
                editor.putInt(Const.KEY_MTS_PU_ESN, mEsnTrackMtsPU);
                editor.putInt(Const.KEY_MTS_IC_ESN, mEsnTrackMtsIC);
                editor.putInt(Const.KEY_MTS_IS_ESN, mEsnTrackMtsIS);
                editor.putInt(Const.KEY_MTS_OC_ESN, mEsnTrackMtsOC);
                editor.putInt(Const.KEY_MTS_OS_ESN, mEsnTrackMtsOS);

            } else {
                if (action
                        .equalsIgnoreCase(Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE)) {
                    mEsnTrackMtsPU = 0;
                    editor.putInt(Const.KEY_MTS_PU_ESN, mEsnTrackMtsPU);

                } else if (action
                        .equalsIgnoreCase(Const.ACTION_CDMA_NEW_SMS_RECVD)) {
                    mEsnTrackMtsIS = 0;
                    editor.putInt(Const.KEY_MTS_IS_ESN, mEsnTrackMtsIS);

                } else if (action.equalsIgnoreCase(Const.ACTION_CDMA_MT_CALL)) {
                    mEsnTrackMtsIC = 0;
                    editor.putInt(Const.KEY_MTS_IC_ESN, mEsnTrackMtsIC);

                } else if (action
                        .equalsIgnoreCase(Const.ACTION_CDMA_NEW_OUTGOING_CALL)) {
                    mEsnTrackMtsOC = 0;
                    editor.putInt(Const.KEY_MTS_OC_ESN, mEsnTrackMtsOC);

                } else if (action
                        .equalsIgnoreCase(Const.ACTION_CDMA_SMS_MSG_SENT)) {
                    mEsnTrackMtsOS = 0;
                    editor.putInt(Const.KEY_MTS_OS_ESN, mEsnTrackMtsOS);
                }
            }
        }

        editor.commit();

        esnMsgQ.remove(action);

        if (!esnMsgQ.isEmpty()) {
            Log.d(TAG, "serviceFinishedProcessNext Queue is not empty");
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                public void run() {
                    processNextMessage();
                }
            };
            timer.schedule(task, Const.HALF_MINUTE);
            return;
        }
        Log.d(TAG, "serviceFinishedProcessNext Queue is empty");

    }

    private void processNextMessage() {
        String newAction = null;

        Iterator<String> itr = esnMsgQ.iterator();
        if ((itr != null) && itr.hasNext()) {
            newAction = (String) itr.next();
        }
        Log.d(TAG, "processNextMessage NewAction= " + newAction);
        if (newAction != null) {
            if (mOptrCode == Const.MTS && (mEsnTrackMtsPU == 1)) {
                esnMsgQ.clear();
                return;
            }
            Intent intent = new Intent();
            intent.setAction(newAction);
            intent.setClass(mContext, EsnTrackService.class);
            mContext.startService(intent);
        }
    }

    public int getOptrCode() {
        return mOptrCode;
    }
}

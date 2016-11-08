package com.mediatek.contacts.sne.plugin;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
//import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telephony.SubscriptionManager;

public class SimUtils {
    private static final String TAG = "SimUtils";

    private static final int SLOT_ID1 = com.android.internal.telephony.PhoneConstants.SIM_ID_1;
    private static final int SLOT_ID2 = com.android.internal.telephony.PhoneConstants.SIM_ID_2;
    private static final String SIMPHONEBOOK_SERVICE = "simphonebook";

    private static final int SIM_TYPE_SIM = 0;
    private static final int SIM_TYPE_USIM = 1;
    private static final int SIM_TYPE_UIM = 2;
    private static final String SIM_TYPE_USIM_TAG = "USIM";
    private static final String SIM_TYPE_UIM_TAG = "UIM";

    public static final String ACCOUNT_TYPE_SIM = "SIM Account";
    public static final String ACCOUNT_TYPE_USIM = "USIM Account";
    public static final String ACCOUNT_TYPE_UIM = "UIM Account";

    private static final int ERROR = -1;

    private static String sCurrentAccount = null;
    private static int sCurSubId = -1;


     public static void setCurrentSubId(int subId) {
        sCurSubId = subId;
        sCurrentAccount = getAccountTypeBySub(subId);
        LogUtils.d(TAG, "setCurrentSubId() sCurSubId=" + sCurSubId + " sCurrentAccount=" + sCurrentAccount);
    }

    public static String getCurAccount() {
        LogUtils.d(TAG, "getCurAccount() sCurrentAccount=" + sCurrentAccount);
        return sCurrentAccount;
    }

    public static boolean isUsim(String accountType) {
        return ACCOUNT_TYPE_USIM.equals(accountType);
    }

    public static boolean isSim(String accountType) {
        return ACCOUNT_TYPE_SIM.equals(accountType);
    }

    public static String getAccountTypeBySub(int subId) {
        LogUtils.d(TAG, "[getAccountTypeBySub] subId:" + subId);
        int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2) {
            LogUtils.e(TAG, "[getAccountTypeBySub]Error slotid:" + slotId);
            return null;
        }
        int simtype = SIM_TYPE_SIM;
        String simAccountType = ACCOUNT_TYPE_SIM;

        if (isSimInserted(slotId)) {
            simtype = getSimTypeBySub(subId);
            if (SIM_TYPE_USIM == simtype) {
                simAccountType = ACCOUNT_TYPE_USIM;
            } else if (SIM_TYPE_UIM == simtype) { // UIM
                simAccountType = ACCOUNT_TYPE_UIM;
            }

        } else {
            LogUtils.e(TAG, "[getAccountTypeBySub]Error slotId:" + slotId + " no sim inserted!");
            simAccountType = null;
        }
        LogUtils.d(TAG, "[getAccountTypeBySub] accountType:" + simAccountType);
        return simAccountType;
    }

    private static int getSimTypeBySub(int subId) {
        final ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        int simType = SIM_TYPE_SIM;
        try {
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
                if (SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_USIM;
                } else if (SIM_TYPE_UIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_UIM;
                }
            } else {
                if (SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_USIM;
                } else if (SIM_TYPE_UIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_UIM;
                }
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "[getSimTypeBySub] catched exception.");
            e.printStackTrace();
        }
        LogUtils.d(TAG, "[getSimTypeBySub] simType:" + simType);
        return simType;
    }

    private static boolean isSimInserted(int slotId) {
        //final ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        boolean isSimInsert = false;
        try {
            if (iTel != null) {
                if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
                     isSimInsert = iTel.hasIccCardUsingSlotId(slotId);
                } else {
                    isSimInsert = iTel.hasIccCardUsingSlotId(slotId);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            isSimInsert = false;
        }
        LogUtils.d(TAG, "[isSimInserted] isSimInsert:" + isSimInsert);
        return isSimInsert;
    }

    // --------------------------------------------
    private static int[] MAX_USIM_SNE_MAX_LENGTH = { -1, -1 };

    /**
     * check whether the Usim support SNE field.
     * @return
     */
    public static boolean hasSne(int subId) {
        LogUtils.d(TAG, "[hasSne]subId:" + subId);
        boolean hasSne = false;
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            return hasSne;
        }
        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                hasSne = iIccPhb.hasSne(subId);
                LogUtils.d(TAG, "hasSne, hasSne=" + hasSne);
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "[hasSne] exception.");
        }
        LogUtils.d(TAG, "[hasSne] hasSne:" + hasSne);
        return hasSne;
    }

    /**
     * get the max length of SNE field.
     * @return
     */
    public static int getSneRecordMaxLen(int subId) {
        LogUtils.d(TAG, "[getSneRecordMaxLen] subId:" + subId);
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            return ERROR;
        }
        if (MAX_USIM_SNE_MAX_LENGTH[slot] < 0) {
            try {
                final IIccPhoneBook iIccPhb = getIIccPhoneBook();
                if (iIccPhb != null) {
                    MAX_USIM_SNE_MAX_LENGTH[slot] = iIccPhb.getSneRecordLen(subId);
                    LogUtils.d(TAG, "getSneRecordMaxLen, len=" + MAX_USIM_SNE_MAX_LENGTH[slot]);
                }
            } catch (RemoteException e) {
                LogUtils.e(TAG, "catched exception.");
                MAX_USIM_SNE_MAX_LENGTH[slot] = -1;
            }
        }
        LogUtils.d(TAG, "[getSneRecordMaxLen]maxNameLen:" + MAX_USIM_SNE_MAX_LENGTH[slot]);
        return MAX_USIM_SNE_MAX_LENGTH[slot];
    }

    private static IIccPhoneBook getIIccPhoneBook() {
        LogUtils.d(TAG, "[getIIccPhoneBook]");
        String serviceName = SIMPHONEBOOK_SERVICE;
        final IIccPhoneBook iIccPhb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService(serviceName));
        return iIccPhb;
    }
}

package com.mediatek.contacts.aas.plugin;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
//import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
//import com.mediatek.common.telephony.AlphaTag;
import com.mediatek.internal.telephony.uicc.AlphaTag;


import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import android.os.SystemProperties;

import android.telephony.SubscriptionManager;


public class SimUtils {
    private static final String TAG = "SimUtils";
    public static final String KEY_SLOT = "slot";
    private static final int SLOT_ID1 = com.android.internal.telephony.PhoneConstants.SIM_ID_1;
    private static final int SLOT_ID2 = com.android.internal.telephony.PhoneConstants.SIM_ID_2;
    private static final String SIMPHONEBOOK_SERVICE = "simphonebook";
    private static final String SIMPHONEBOOK2_SERVICE = "simphonebook2";

    private static final int SIM_TYPE_SIM = 0;
    private static final int SIM_TYPE_USIM = 1;
    private static final int SIM_TYPE_UIM = 2;
    private static final String SIM_TYPE_USIM_TAG = "USIM";
    private static final String SIM_TYPE_UIM_TAG = "UIM";

    public static final String ACCOUNT_TYPE_SIM = "SIM Account";
    public static final String ACCOUNT_TYPE_USIM = "USIM Account";
    public static final String ACCOUNT_TYPE_UIM = "UIM Account";

    public static final String IS_ADDITIONAL_NUMBER = "1";

    private static int[] MAX_USIM_AAS_NAME_LENGTH = { -1, -1 };
    private static int[] MAX_USIM_AAS_COUNT = { -1, -1 };
    private static int[] MAX_USIM_ANR_COUNT = { -1, -1 };
    private static HashMap<Integer, List<AlphaTag>> sAasMap = new HashMap<Integer, List<AlphaTag>>(2);

    private static final int ERROR = -1;

    private static int sCurSlotId = -1;
    private static String sCurrentAccount = null;
    private static int sCurSubId = -1;

    public static void setCurrentSlot(int slotId) {
        sCurSlotId = slotId;
        LogUtils.d(TAG, "setCurrentSlot() sCurSlotId=" + sCurSlotId + " sCurrentAccount=" + sCurrentAccount);
    }

    public static void setCurrentSubId(int subId) {
        sCurSubId = subId;
        //L migration sCurSlotId = SubscriptionManager.getSlotId(subId);
        sCurrentAccount = getAccountTypeBySub(subId);
        LogUtils.d(TAG, "setCurrentSubId() sCurSubId=" + sCurSubId + " sCurrentAccount=" + sCurrentAccount);
    }

    public static String getCurAccount() {
        LogUtils.d(TAG, "getCurAccount() sCurrentAccount=" + sCurrentAccount);
        return sCurrentAccount;
    }

    public static int getCurSlotId() {
        LogUtils.d(TAG, "getCurSlotId() sCurSlotId=" + sCurSlotId);
        return sCurSlotId;
    }


    public static int getCurSubId() {
        LogUtils.d(TAG, "getCurSlotId() sCurSubId=" + sCurSubId);
        return sCurSubId;
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
        return isSimInsert;
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
        return simType;
    }

    /**
     * refresh local aas list. after you change the USim card aas info, please refresh local info.
     * @param slot
     * @return
     */
    public static boolean refreshAASList(int subId) {
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            LogUtils.d(TAG, "refreshAASList() slot=" + slot);
            return false;
        }

        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                LogUtils.d(TAG, "refreshAASList subId =" + subId);
                List<AlphaTag> atList = iIccPhb.getUsimAasList(subId);
                LogUtils.d(TAG, "refreshAASList atList =" + atList);
                if (atList != null) {
                    Iterator<AlphaTag> iter = atList.iterator();
                    LogUtils.d(TAG, "refreshAASList success");
                    while (iter.hasNext()) {
                        AlphaTag entry = iter.next();
                        String tag = entry.getAlphaTag();
                        if (TextUtils.isEmpty(tag)) {
                            iter.remove();
                        }
                        LogUtils.d(TAG, "refreshAASList. tag=" + tag);
                    }
                }
                sAasMap.put(slot, atList);
            }
        } catch (RemoteException e) {
            LogUtils.d(TAG, "catched exception.");
            sAasMap.put(slot, null);
        }

        return true;
    }

    /**
     * get USim card aas info without null tag. It will return all aas info that can be used in
     * application.
     * @param slot
     * @return
     */
    public static List<AlphaTag> getAAS(int subId) {
        List<AlphaTag> atList = new ArrayList<AlphaTag>();
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            LogUtils.e(TAG, "getAAS() slot=" + slot);
            return atList;
        }
        // Here, force to refresh the list.
        LogUtils.d(TAG, "getAAS refreshAASList");
        refreshAASList(subId);

        List<AlphaTag> list = sAasMap.get(slot);

        return list != null ? list : atList;
    }


    /**
     * Get the max length of AAS.
     * @param slot The USIM Card slotId
     * @return
     */
    public static int getAASTextMaxLength(int subId) {
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            LogUtils.e(TAG, "getAASMaxLength() slot=" + slot);
            return ERROR;
        }
        LogUtils.d(TAG, "getAASMaxLength() slot:" + slot + "|maxNameLen:" + MAX_USIM_AAS_NAME_LENGTH[slot]);
        if (MAX_USIM_AAS_NAME_LENGTH[slot] < 0) {
            try {
                final IIccPhoneBook iIccPhb = getIIccPhoneBook();
                if (iIccPhb != null) {
                    MAX_USIM_AAS_NAME_LENGTH[slot] = iIccPhb.getUsimAasMaxNameLen(subId);
                }
            } catch (RemoteException e) {
                LogUtils.d(TAG, "catched exception.");
                MAX_USIM_AAS_NAME_LENGTH[slot] = -1;
            }
        }
        LogUtils.d(TAG, "getAASMaxLength() end slot:" + slot + "|maxNameLen:" + MAX_USIM_AAS_NAME_LENGTH[slot]);
        return MAX_USIM_AAS_NAME_LENGTH[slot];
    }

    public static String getAASById(int subId, int index) {
        int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2 || index < 1) {
            return "";
        }
        String aas = "";
        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                aas = iIccPhb.getUsimAasById(subId, index);
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "getUSIMAASById() catched exception.");
        }
        if (aas == null) {
            aas = "";
        }
        LogUtils.d(TAG, "getUSIMAASById() aas=" + aas);
        return aas;
    }

    public static int getAasIndexByName(String aas, int subId) {
          int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2 || TextUtils.isEmpty(aas)) {
            LogUtils.e(TAG, "getAasIndexByName() error slotId=" + slotId + "aas=" + aas);
            return ERROR;
        }
        // here, it only can compare type name
        LogUtils.d(TAG, "getAasIndexByName, tag=" + aas);
        List<AlphaTag> atList = getAAS(subId);
        Iterator<AlphaTag> iter = atList.iterator();
        while (iter.hasNext()) {
            AlphaTag entry = iter.next();
            String tag = entry.getAlphaTag();
            if (aas.equalsIgnoreCase(tag)) {
                LogUtils.d(TAG, "getAasIndexByName, tag=" + tag);
                return entry.getRecordIndex();
            }
        }
        return ERROR;
    }

    public static int insertUSIMAAS(int subId, String aasName) {
         int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2 || TextUtils.isEmpty(aasName)) {
            return ERROR;
        }
        int result = ERROR;
        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                result = iIccPhb.insertUsimAas(subId, aasName);
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "insertUSIMAAS() catched exception.");
        }

        return result;
    }

    public static boolean updateUSIMAAS(int subId, int index, int pbrIndex, String aasName) {
          int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2) {
            return false;
        }
        boolean result = false;
        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                result = iIccPhb.updateUsimAas(subId, index, pbrIndex, aasName);
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "updateUSIMAAS() catched exception.");
        }
        LogUtils.d(TAG, "updateUSIMAAS refreshAASList");
        refreshAASList(slotId);

        return result;
    }

    public static boolean removeUSIMAASById(int subId, int index, int pbrIndex) {
     int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2) {
            return false;
        }
        boolean result = false;
        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook();
            if (iIccPhb != null) {
                result = iIccPhb.removeUsimAasById(subId, index, pbrIndex);
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "removeUSIMAASById() catched exception.");
        }
        LogUtils.d(TAG, "removeUSIMAASById refreshAASList");
        refreshAASList(slotId);

        return result;
    }

    public static boolean isAasTextValid(String text, int subId) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        final int MAX = getAASTextMaxLength(subId);
        try {
            GsmAlphabet.stringToGsm7BitPacked(text);
            if (text.length() > MAX) {
                return false;
            }
        } catch (EncodeException e) {
            if (text.length() > ((MAX - 1) >> 1)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Get the max size of AAS.
     * @param slot The USIM Card slotId
     * @return
     */
    public static int getAASMaxCount(int subId) {
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            return ERROR;
        }
        LogUtils.d(TAG, "[getUSIMAASMaxCount]slot:" + slot + "|maxGroupCount:" + MAX_USIM_AAS_COUNT[slot]);
        if (MAX_USIM_AAS_COUNT[slot] < 0) {
            try {
                final IIccPhoneBook iIccPhb = getIIccPhoneBook();
                if (iIccPhb != null) {
                    MAX_USIM_AAS_COUNT[slot] = iIccPhb.getUsimAasMaxCount(subId);
                }
            } catch (RemoteException e) {
                LogUtils.d(TAG, "catched exception.");
                MAX_USIM_AAS_COUNT[slot] = -1;
            }
        }
        LogUtils.d(TAG, "[getUSIMAASMaxCount]end slot:" + slot + "|maxGroupCount:" + MAX_USIM_AAS_COUNT[slot]);
        return MAX_USIM_AAS_COUNT[slot];
    }

    public static int getAnrCount(int subId) {
        int slot = SubscriptionManager.getSlotId(subId);
        if (slot < SLOT_ID1 || slot > SLOT_ID2) {
            return ERROR;
        }
        LogUtils.d(TAG, "[getAnrCount]slot:" + slot + "|maxGroupCount:" + MAX_USIM_ANR_COUNT[slot]);

         /* When USIM is not ready, iIccPhb.getAnrCount() returns 0, so we can't be sure whether USIM actually
         doesn't support additional number or is just not ready, so the following check should be <= 0 and not < 0 */

        if (MAX_USIM_ANR_COUNT[slot] <= 0) {
            try {
                final IIccPhoneBook iIccPhb = getIIccPhoneBook();
                if (iIccPhb != null) {
                    MAX_USIM_ANR_COUNT[slot] = iIccPhb.getAnrCount(subId);
                }
            } catch (RemoteException e) {
                LogUtils.d(TAG, "catched exception.");
                MAX_USIM_ANR_COUNT[slot] = -1;
            }
        }
        LogUtils.d(TAG, "[getAnrCount]end slot:" + slot + "|maxGroupCount:" + MAX_USIM_ANR_COUNT[slot]);
        return MAX_USIM_ANR_COUNT[slot];
    }

    private static IIccPhoneBook getIIccPhoneBook() {
        LogUtils.d(TAG, "[getIIccPhoneBook]");
        String serviceName = SIMPHONEBOOK_SERVICE;;
        final IIccPhoneBook iIccPhb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService(serviceName));
        return iIccPhb;
    }

    public static boolean isUsim(String accountType) {
        return ACCOUNT_TYPE_USIM.equals(accountType);
    }

    public static boolean isSim(String accountType) {
        return ACCOUNT_TYPE_SIM.equals(accountType);
    }

    public static boolean isPhone(String mimeType) {
        return Phone.CONTENT_ITEM_TYPE.equals(mimeType);
    }

    public static boolean isAasPhoneType(int type) {
        return (Anr.TYPE_AAS == type);
    }

    public static String getSuffix(int count) {
        if (count <= 0) {
            return "";
        } else {
            return String.valueOf(count);
        }
    }
}

package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;
/**
 * replace MTK_FEATION_OPTION.
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    // Check if mtk gemini feature is enabled
    public static boolean isGeminiSupport() {
        return SystemProperties.getInt("ro.mtk_gemini_support", 0) == 1;
    }

    public static boolean isMtkSharedSdcardSupport() {
        return SystemProperties.get("ro.mtk_shared_sdcard").equals("1");
    }

    /**
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    public static boolean isSimStateReady(Context context, int simId) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        Log.i(TAG, "isSimStateReady = " + mTelephonyManager.getSimState(simId));
        return mTelephonyManager.getSimState(simId) == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * judge if sim radio on or not.
     * @param simId simid
     * @return true if sim radio on
     */
    public static boolean isTargetSimRadioOn(int simId) {
        int[] targetSubId = SubscriptionManager.getSubId(simId);
        if (targetSubId != null && targetSubId.length > 0) {
            for (int i = 0; i < targetSubId.length; i++) {
               if (isTargetSlotRadioOn(i)) {
                   Log.i(TAG, "isTargetSimRadioOn true simId = " + simId);
                   return true;
               }
            }
            Log.i(TAG, "isTargetSimRadioOn false simId = " + simId);
            return false;
        } else {
            Log.i(TAG, "isTargetSimRadioOn false because " +
                    "targetSubId[] = null or targetSubId[].length is 0  simId =" + simId);

            return false;
        }
    }

    /**
     * judge subid is radio on or not.
     * @param subId subId
     * @return true if this subId is radio on
     */
    public static boolean isTargetSlotRadioOn(int subId) {
        boolean radioOn = true;
        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (null == iTel) {
                Log.i(TAG, "isTargetSlotRadioOn = false because iTel = null");
                return false;
            }
            Log.i(TAG, "isTargetSlotRadioOn = " + iTel.isRadioOnForSubscriber(subId, PACKAGE_NAME));
            radioOn = iTel.isRadioOnForSubscriber(subId, PACKAGE_NAME);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Log.i(TAG, "isTargetSlotRadioOn radioOn = " + radioOn);
        return radioOn;
    }

    /**
     * Get the slotId insert or not.
     * @param slotId Slot id
     * @return true if slot card is insert.
     */
    public static boolean isSIMInserted(int slotId) {
        try {
            ITelephony tmex = ITelephony.Stub.asInterface(android.os.ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            Log.i(TAG, "isSIMInserted slotId = " + slotId + "return" +
                    (tmex != null && tmex.hasIccCardUsingSlotId(slotId)));
            return (tmex != null && tmex.hasIccCardUsingSlotId(slotId));
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.i(TAG, "isSIMInserted return false because catch RemoteException");
            return false;
        }
    }

    public static final int CT_SIM = 5;
    public static final int CT4G_SIM = 4;
    public static final int CT3G_SIM = 3;
    public static final int GSM_SIM = 2;
    public static final int ERROR_SIM = -1;
    /**
     * get sim type .
     * @param
     * @return sim type
     */
    public static int getSimType() {
        String fullUiccType = getFullIccCardTypeExt();

        if (fullUiccType != null) {
            if (fullUiccType.contains("CSIM") || fullUiccType.contains("RUIM")) {

                if (fullUiccType.contains("CSIM") || fullUiccType.contains("USIM")) {
                    return CT4G_SIM;
                } else if (fullUiccType.contains("SIM")) {
                    return CT3G_SIM;
                }
                return CT_SIM;
            } else if (fullUiccType.contains("SIM") || fullUiccType.contains("USIM")) {
                Log.d(TAG, "getSimType is GSM sim");
                return GSM_SIM;
            } else {
                Log.d(TAG, "getSimType not GSM, CT34G");
                return ERROR_SIM;
            }
        }
        Log.d(TAG, "getSimType fullUiccType null");
        return ERROR_SIM;
    }

    /**
     * Judge if it is CT card.
     * @return true if it is CT card.
     */
    public static boolean isCTCardType() {
        Log.i(TAG, "isCTCardType = " + ((getSimType() == CT4G_SIM)
              || (getSimType() == CT3G_SIM) || (getSimType() == CT_SIM)));
        return (getSimType() == CT4G_SIM) || (getSimType() == CT3G_SIM)
                || (getSimType() == CT_SIM);
    }

    /**
     * Judge if using test card.
     * @return true if it is test card.
     */
    public static boolean isCTLTECardType() {
        Log.i(TAG, "isCTLTECardType = " + (getSimType() == CT4G_SIM));
        return getSimType() == CT4G_SIM;
    }

    /**
     * Judge if it is CDMA card.
     * @return true if it is CDMA card.
     */
    public static boolean isCDMACardType() {
        Log.i(TAG, "isCDMACardType = " + (getSimType() == CT3G_SIM));
        return getSimType() == CT3G_SIM;
    }

    /**
     * Judge if it is SIM one.
     * @param subscriptionInfo the subscription information.
     * @return true if it is test card.
     */
    public static boolean phoneConstantsIsSimOne(SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo != null) {
            int slotId = SubscriptionManager.getSlotId(subscriptionInfo.getSubscriptionId());
            Log.i(TAG, "phoneConstantsIsSimOne slotIsSimOne = "
                  + (PhoneConstants.SIM_ID_1 == slotId));
            return PhoneConstants.SIM_ID_1 == slotId;
        }
        return false;
    }

    /**
     * get getFullIccCardTypeExt type.
     * @param
     * @return sim string type.
     */
    public static String getFullIccCardTypeExt() {
        Log.i(TAG, "getFullIccCardTypeExt cardType = "
              + SystemProperties.get("gsm.ril.fulluicctype"));

        return SystemProperties.get("gsm.ril.fulluicctype");
    }
    /**
     * finds a record with subId.
     * Since the number of SIMs are few, an array is fine.
     * @param context plugin context
     * @param subId sud id of the sim
     * @return subinfo of the subId
     */
    public static SubscriptionInfo findRecordBySubId(Context context, final int subId) {
        final List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();

            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir != null && sir.getSubscriptionId() == subId) {
                    return sir;
                }
            }
        }

        return null;
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     * @param context plugin context
     * @param slotId solt id of the sim
     * @return subinfo of the subId
     */
    public static SubscriptionInfo findRecordBySlotId(Context context, final int slotId) {
        final List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();

            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }

        return null;
    }
}

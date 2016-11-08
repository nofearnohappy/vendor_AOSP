package com.mediatek.phone.plugin;

import android.content.Context;
import android.media.AudioSystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

/**
 * Tools for replace MTK_FEATURE_OPTION.
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final String PACKAGE_NAME = "com.mediatek.phone.plugin";

    private static final String MTK_FEATURE_TTY_STATE = "MTK_FEATURE_TTY_SUPPORT";
    private static final String MTK_FEATURE_TTY_ON = "MTK_FEATURE_TTY_SUPPORT=true";
    private static final String MTK_FEATURE_DUAL_MIC_STATE = "MTK_FEATURE_DUAL_MIC_SUPPORT";
    private static final String MTK_FEATURE_DUAL_MIC_ON = "MTK_FEATURE_DUAL_MIC_SUPPORT=true";

    /**
     * GeminiSupport support or not.
     * @return true if support GeminiSupport
     */
    public static boolean isGeminiSupport() {
        return SystemProperties.getInt("ro.mtk_gemini_support", 0) == 1;
    }

    /**
     * EvdoIRSupport support or not.
     * @return true if support EvdoIRSupport
     */
    public static boolean isEvdoIRSupport() {
        return SystemProperties.getInt("ro.evdo_ir_support", 0) == 1;
    }

    /**
     * MTK_FEATURE_TTY_STATE support or not.
     * @return true if support MTK_FEATURE_TTY_STATE
     */
    public static boolean isTtySupport() {
        String state = AudioSystem.getParameters(MTK_FEATURE_TTY_STATE);
        if (null == state) {
            return false;
        }
        return state.equalsIgnoreCase(MTK_FEATURE_TTY_ON);
    }

    /**
     * MTK_FEATURE_DUAL_MIC_STATE support or not.
     * @return true if support MTK_FEATURE_DUAL_MIC_STATE
     */
    public static boolean isDualMic() {
        String state = AudioSystem.getParameters(MTK_FEATURE_DUAL_MIC_STATE);
        if (null == state) {
            return false;
        }
        return state.equalsIgnoreCase(MTK_FEATURE_DUAL_MIC_ON);
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
        ///just for test
//        return true;
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
}
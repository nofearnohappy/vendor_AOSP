package com.mediatek.keyguard.plugin;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl ;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.keyguard.ext.DefaultCarrierTextExt;

/**
 * Customize carrier text.
 */
@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class OP02CarrierTextExt extends DefaultCarrierTextExt {
    public static final String TAG = "OP02CarrierTextExt";

    @Override
    public CharSequence customizeCarrierText(CharSequence carrierText, CharSequence simMessage,
            int simId) {
        Bundle bd = null;
        try {
            ITelephonyEx phoneEx = ITelephonyEx.Stub.asInterface(ServiceManager
                    .checkService("phoneEx"));
            if (phoneEx != null) {
                final int [] subIds = SubscriptionManager.getSubId(simId);
                Log.i(TAG, "customizeCarrierText, slotId = " + simId + ",subIds = " + subIds);
                if (subIds != null && subIds.length != 0) {
                    int subId = subIds[0];
                    bd = phoneEx.getServiceState(subId);
                    if (bd != null) {
                        ServiceState ss = ServiceState.newFromBundle(bd);
                        if (ss != null) {
                            Log.i(TAG, "customizeCarrierText, slotId = " + simId + ",subId = "
                                    + subId + "ss.isEmergencyOnly()=" + ss.isEmergencyOnly());
                            if (ss.isEmergencyOnly()) {
                                return simMessage;
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.i(TAG, "getServiceState error e:" + e.getMessage());
        }
        return super.customizeCarrierText(carrierText, simMessage, simId);
    }

    @Override
    public boolean showCarrierTextWhenSimMissing(boolean isSimMissing, int simId) {
        ///init sim card info
        int simcardNumber = getSimNumber();

        Log.i(TAG, "showCarrierTextWhenSimMissing, simcardNumber = " + simcardNumber);
        if (simcardNumber == 0 || simcardNumber == 1) {
            return isSimMissing;
        } else if (simcardNumber == 2) {
            if (hasPinPukLock()) {
                return isSimMissing;
            }

            boolean oneCardOutOfService = isOneCardOutOfService();
            Log.i(TAG, "oneCardOutOfService = " + oneCardOutOfService);
            if (!oneCardOutOfService) {
                return isSimMissing;
            } else {
                // / M: when one sim state is not in service && !
                // emerycallonly,sim1 must show ,so reture false
                Log.i(TAG, "simId = " + simId + " return = "
                        + (PhoneConstants.SIM_ID_1 != simId));
                return PhoneConstants.SIM_ID_1 != simId;
            }
        }
        return isSimMissing;
    }

    /**
     * The customized divider of carrier text.
     *
     * @param divider the current carrier text divider string.
     *
     * @return the customized carrier text divider string.
     */
    @Override
    public String customizeCarrierTextDivider(String divider) {
        String carrierDivider = " | ";
        return carrierDivider;
    }

    /**
     * Get the valid sim count.
     * @return the valid sim count.
     */
    private int getSimNumber() {
        Log.i(TAG, "getSimNumber() start ");
        int simNumber = 0;
        ITelephonyEx phoneEx = ITelephonyEx.Stub
                .asInterface(ServiceManager.checkService("phoneEx"));
        try {
            if (null != phoneEx) {
                Bundle bd = null;
                final int simNum = getNumOfSim();
                Log.i(TAG, "getSimNumber() simNum = " + simNum);
                for (int i = PhoneConstants.SIM_ID_1; i < simNum; i++) {
                    final int[] subIds = SubscriptionManager.getSubId(i);
                    Log.i(TAG, "getSimNumber() slotId = " + i + ",subIds = " + subIds);
                    if (subIds != null && subIds.length != 0) {
                        int subId = subIds[0];
                        int simState = TelephonyManager.getDefault().getSimState(i);
                        boolean bSimInserted = TelephonyManager.getDefault().hasIccCard(i);

                        Log.i(TAG, "getSimNumber() slotId = " + i + " subId = " + subId
                                + "bSimInserted = " + bSimInserted + " simState=" + simState);

                        boolean processed = false;
                        if (bSimInserted
                                && ((TelephonyManager.SIM_STATE_PIN_REQUIRED == simState
                                        || TelephonyManager.SIM_STATE_PUK_REQUIRED == simState
                                        || TelephonyManager.SIM_STATE_READY == simState
                                        || TelephonyManager.SIM_STATE_NETWORK_LOCKED
                                                == simState))) {
                            simNumber++;
                            processed = true;
                            continue;
                        }

                        bd = phoneEx.getServiceState(subId);
                        if (bd != null) {
                            ServiceState ss = ServiceState.newFromBundle(bd);
                            if (ss != null) {
                                Log.i(TAG, "getSimNumber() slotId = " + i + " subId = " + subId
                                        + "bSimInserted = " + bSimInserted
                                        + " ss.isEmergencyOnly() = " + ss.isEmergencyOnly()
                                        + " ss.getState() = " + ss.getState());
                                if (!processed && bSimInserted
                                        && ss.getState() == ServiceState.STATE_POWER_OFF) {
                                    simNumber++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "getSimNumber() end: simNumber = " + simNumber);
        return simNumber;
    }

    private boolean hasPinPukLock() {
        final int simNum = getNumOfSim();
        for (int i = PhoneConstants.SIM_ID_1; i < simNum; i++) {
            int simState = TelephonyManager.getDefault().getSimState(i);
            Log.i(TAG, "slot[" + i + "] simState = " + simState);
            if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                    || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
                Log.i(TAG, "return hasPinPukLock slotId = " + i + "simstate = PIN or PUK");
                return true;
            }
        }
        return false;
    }

    private static int getNumOfSim() {
        return TelephonyManager.getDefault().getSimCount();
    }

    private boolean isOneCardOutOfService() {
        Bundle bd = null;
        ITelephonyEx phoneEx = ITelephonyEx.Stub
                .asInterface(ServiceManager.checkService("phoneEx"));
        try {
            if (null != phoneEx) {
                final int simNum = getNumOfSim();
                for (int i = PhoneConstants.SIM_ID_1; i < simNum; i++) {
                    final int[] subIds = SubscriptionManager.getSubId(i);
                    Log.i(TAG, "isOneCardOutOfService, slotId = " + i + ",subIds = " + subIds);
                    if (subIds != null && subIds.length != 0) {
                        int subId = subIds[0];
                        bd = phoneEx.getServiceState(subId);
                        if (bd != null) {
                            ServiceState ss = ServiceState.newFromBundle(bd);
                            if (ss != null) {
                                Log.i(TAG, "isOneCardOutOfService, slotId = " + i + ",subId = "
                                        + subId + " ss.getState() = " + ss.getState());
                                if (ServiceState.STATE_IN_SERVICE != ss.getState()
                                        && !ss.isEmergencyOnly()) {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }
}

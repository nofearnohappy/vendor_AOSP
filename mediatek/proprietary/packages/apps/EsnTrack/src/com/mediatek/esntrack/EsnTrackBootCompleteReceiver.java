package com.mediatek.esntrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;

public class EsnTrackBootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "EsnTrackBootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!isEsnTrackSwitchOpen()) {
            Log.d(TAG, "[ESN TRACK] Switch is closed , return");
            return;
        }
        if (intent != null) {
            String action = intent.getAction();
            if (Const.ACTION_CDMA_SIM_STATE_CHANGED.equalsIgnoreCase(action)) {
                String simStatus = intent
                        .getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                if (slotId == -1) {
                    return;
                }

                int[] subIds = SubscriptionManager.getSubId(slotId);
                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (TelephonyManager.PHONE_TYPE_CDMA == telephonyManager
                        .getCurrentPhoneType(subIds[0])) {
                    if (IccCardConstants.INTENT_VALUE_ICC_LOADED
                            .equals(simStatus)) {
                        Log.d(TAG, "[ESN TRACK] SPN Loaded for CDMA");
                        int optrCode = getCdmaOptrCode(telephonyManager);
                        if (optrCode != Const.TATA && optrCode != Const.MTS) {
                            Log.d(TAG,
                                    "[ESN TRACK] startEsnTrackService Operator not matched");
                            return;
                        }
                        EsnTrackController.SendAtToModem();
                    }
                    return;
                }
            } else {
                Log.d(TAG, "No need to handle");
            }
        }
    }

    private boolean isEsnTrackSwitchOpen() {
        String value = SystemProperties.get("persist.sys.esn_track_switch");
        Log.d(TAG, "isEsnTrackSwitchOpen value:" + value);
        final boolean isEnabled = value.equals("1");
        return isEnabled;

    }

    private int getCdmaOptrCode(TelephonyManager telephonyManager) {
        int[] slotList;
        if (telephonyManager == null) {
            Log.e(TAG,
                    "isSingleLoad(), telephonyManager is null! return false as default.");
            return 0;
        }
        if (telephonyManager.getSimCount() == 1) {
            slotList = Const.SINGLE_UIM_ID;
            Log.d(TAG, "Single SIM load.");
        } else {
            slotList = Const.UIM_ID_LIST;
            Log.d(TAG, "Dual SIM load.");
        }

        int currentLogonUim = Const.UIM_NONE;

        for (int uimId : slotList) {
            String spnName = getOpSpn(telephonyManager, uimId);
            Log.v(TAG, "[getCdmaOptrCode] spnName :" + spnName);
            if (spnName.equalsIgnoreCase(Const.SPN_TATA)) {
                return Const.TATA;
            } else if (spnName.equalsIgnoreCase(Const.SPN_MTS)) {
                return Const.MTS;
            }
        }
        Log.d(TAG, "Current logon UIM is " + currentLogonUim);
        return 0;
    }

    private String getOpSpn(TelephonyManager telephonyManager, int uimId) {
        String spnName = "";
        Log.v(TAG, "[getOpSpn] begin uimId: " + uimId);
        if (telephonyManager.hasIccCard(uimId)) {
            int[] subId = SubscriptionManager.getSubId(uimId);
            if (subId == null || subId[0] < 0) {
                Log.e(TAG, "[getOpSpn] getSubId invalid!");
                return spnName;
            }
            int phoneType = telephonyManager.getCurrentPhoneType(subId[0]);
            Log.v(TAG, "[getOpSpn] phone type of uim (" + uimId + ") = "
                    + phoneType);
            if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
                spnName = telephonyManager
                        .getSimOperatorNameForSubscription(subId[0]);
                Log.v(TAG, "[getOpSpn] SPN of uim (" + uimId + ") is "
                        + spnName);
                return spnName;
            }
        }
        return spnName;
    }

}

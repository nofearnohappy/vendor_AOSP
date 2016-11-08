package com.mediatek.esntrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.NetworkState;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.dm.DmAgent;
//L1.MP3 Patchback only
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

//L1.MP3 Patchback only
import java.util.Set;
import java.util.Iterator;
public class EsnTrackTriggerReceiver extends BroadcastReceiver {

    private static final String TAG = "EsnTrackTriggerReceiver";
    // L1.MP3 Patchback only
    public static final String MY_ADDRESS_PREF = "AddressInEngMode";

    // L1.MP3 Patchback only

    @Override
    public void onReceive(Context context, Intent intent) {
        // L1.MP3 Patchback only
        if (intent != null
                && Const.ACTION_CDMA_ADDRESS_CHANGE_ENGMODE
                        .equalsIgnoreCase(intent.getAction())) {
            String address = intent.getExtras().getString(Const.KEY_ADDRESS);
            Log.d(TAG, "[ESN TRACK]onReceive new address:" + address);
            if (address == null || address.length() == 0) {
                Log.d(TAG, "[ESN TRACK]onReceive new address empty");
                return;
            }
            SharedPreferences sharedpreferences = context.getSharedPreferences(
                    MY_ADDRESS_PREF, context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(Const.KEY_ADDRESS, address);
            editor.commit();
            return;
        }
        // L1.MP3 Patchback only

        if (!isEsnTrackSwitchOpen()) {
            Log.d(TAG, "[ESN TRACK] Switch is closed , return");
            return;
        }
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "[ESN TRACK] onReceive" + action);

            if (!isCdmaAction(context, intent)) {
                Log.d(TAG, "[ESN TRACK] onReceive: Not a CDMA action");
                return;
            }

            if (Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                    .equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] feasible onReceive");
                EsnTrackController etc = EsnTrackController.getInstance(
                        context.getApplicationContext(), intent);
                if (etc != null) {
                    etc.startEsnTrackService();
                }

            } else if (Const.ACTION_CDMA_NEW_OUTGOING_CALL
                    .equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] MO call onReceive");
                EsnTrackController etc = EsnTrackController.getInstance(
                        context.getApplicationContext(), intent);
                if (etc != null) {
                    etc.startEsnTrackService();
                }

            } else if (Const.ACTION_CDMA_NEW_SMS_RECVD.equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] MT SMS onReceive");
                EsnTrackController etc = EsnTrackController.getInstance(
                        context.getApplicationContext(), intent);
                if (etc != null) {
                    etc.startEsnTrackService();
                }

            } else if (Const.ACTION_CDMA_MT_CALL.equalsIgnoreCase(action)) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                        TelephonyManager.EXTRA_STATE_RINGING)) {
                    Log.d(TAG, "[ESN TRACK] MT call onReceive");
                    EsnTrackController etc = EsnTrackController.getInstance(
                            context.getApplicationContext(), intent);
                    if (etc != null) {
                        etc.startEsnTrackService();
                    }
                }

            } else if (Const.ACTION_CDMA_UTK_MENU_SELECTION
                    .equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] UTK Menu Selection onReceive");
                EsnTrackController etc = EsnTrackController.getInstance(
                        context.getApplicationContext(), intent);
                if (etc != null) {
                    etc.startEsnTrackService();
                }

            } else if (Const.ACTION_CDMA_SMS_MSG_SENT.equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] MO SMS onReceive");
                EsnTrackController etc = EsnTrackController.getInstance(
                        context.getApplicationContext(), intent);
                if (etc != null) {
                    etc.startEsnTrackService();
                }

            } else if (Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE
                    .equalsIgnoreCase(action)) {
                Log.d(TAG, "[ESN TRACK] Data Connection");
                if (IsDataConnectionByCdma(context)) {
                    EsnTrackController etc = EsnTrackController.getInstance(
                            context.getApplicationContext(), intent);
                    if (etc != null) {
                        etc.startEsnTrackService();
                    }
                }

            } else if (Const.ACTION_BOOTCOMPLETED.equalsIgnoreCase(action)) {
                EsnTrackController.onPhoneBoot(context.getApplicationContext());
            } else {
                Log.d(TAG, "No need to handle");
            }

        }
    }

    private boolean IsDataConnectionByCdma(Context context) {
        Log.d(TAG, "IsDataConnectionByCdma subId entry");
        try {
            IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
            IConnectivityManager service = null;
            if (b != null) {
                service = IConnectivityManager.Stub.asInterface(b);
            }
            NetworkState[] states = service.getAllNetworkState();
            Log.d(TAG, "IsDataConnectionByCdma states");

            for (NetworkState state : states) {

                if (state.networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                        && state.networkInfo.isConnected()) {

                    TelephonyManager mTelephonyManager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);

                    int[] slotList;
                    if (mTelephonyManager.getSimCount() == 1) {
                        slotList = Const.SINGLE_UIM_ID;
                        Log.d(TAG, "Single SIM load.");
                    } else {
                        slotList = Const.UIM_ID_LIST;
                        Log.d(TAG, "Dual SIM load.");
                    }
                    int subIdConnected = -1;
                    for (int uimId : slotList) {
                        if (mTelephonyManager.hasIccCard(uimId)) {
                            int[] subIdEx = SubscriptionManager.getSubId(uimId);
                            Log.d(TAG, " SubId:" + subIdEx[0]);
                            if (mTelephonyManager.getDataEnabled(subIdEx[0])) {
                                Log.d(TAG, "DataEnabled true for SubId:"
                                        + subIdEx[0]);
                                subIdConnected = subIdEx[0];
                                break;
                            }
                        }
                    }

                    if (TelephonyManager.PHONE_TYPE_CDMA == mTelephonyManager
                            .getCurrentPhoneType(subIdConnected)) {
                        return true;
                    }
                }

            }

        } catch (RemoteException e) {
            Log.d(TAG, "IsDataConnectionByCdma Exception:");
            e.printStackTrace();
        }
        return false;

    }

    private boolean isCdmaAction(Context context, Intent intent) {
        boolean isSingleSim = true;
        TelephonyManager mTelephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE.equalsIgnoreCase(intent
                .getAction())) {
            boolean isCdmaData = IsDataConnectionByCdma(context);
            Log.d(TAG, "[ESN TRACK] DATA_CONNECTION" + isCdmaData);
            return isCdmaData;

        }
        if ((mTelephonyManager.getSimCount() == 1)) {
            Log.d(TAG,
                    "isCdmaAction  single SIM project so must be CDMA, return true");
            return true;
        }

        if (Const.ACTION_CDMA_UTK_MENU_SELECTION.equalsIgnoreCase(intent
                .getAction())
                || Const.ACTION_BOOTCOMPLETED.equalsIgnoreCase(intent
                        .getAction())) {
            Log.d(TAG, "[ESN TRACK] UTK or BOOT COMPLETE");
            return true;
        }
				if(intent.getExtras() != null) {
				Set<String> keys = intent.getExtras().keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Log.e(TAG,"[" + key + "=" + intent.getExtras().get(key)+"]");
        }
        
      }
        if (intent.getExtras() != null
                && intent.getExtras().containsKey(
                        PhoneConstants.SUBSCRIPTION_KEY)) {
            int whichSIM = intent.getExtras().getInt(
                    PhoneConstants.SUBSCRIPTION_KEY);
            Log.d(TAG, "[ESN TRACK] onReceive subscription:" + whichSIM);
            if (whichSIM != SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
                    && TelephonyManager.PHONE_TYPE_CDMA == mTelephonyManager
                            .getCurrentPhoneType(whichSIM)) {
                Log.d(TAG, "[ESN TRACK] CDMA action:");
                return true;
            }

        }
        Log.d(TAG, "[ESN TRACK] not a CDMA action:");
        return false;

    }

    private boolean isEsnTrackSwitchOpen() {
        String value = SystemProperties.get("persist.sys.esn_track_switch");
        Log.d(TAG, "isEsnTrackSwitchOpen value:" + value);
        final boolean isEnabled = value.equals("1");
        return isEnabled;

    }

}

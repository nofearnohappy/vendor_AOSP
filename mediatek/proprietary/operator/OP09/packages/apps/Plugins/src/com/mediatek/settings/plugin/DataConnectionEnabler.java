package com.mediatek.settings.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import java.util.List;

/**
 * DataConnectionEnabler is a helper to manage the Data connection on/off checkbox
 * preference. It turns on/off Data connection and ensures the summary of the
 * preference reflects the current state.
 */
public final class DataConnectionEnabler implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "DataConnectionEnabler";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    private static final int ALL_RADIO_OFF = 0;

    private final Context mContext;
    private Switch mSwitch;
    private IntentFilter mIntentFilter;

    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private int mUpdateAction = 0;
    private String mObserveSim1;
    private String mObserveSim2;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                updateSwitcherState();
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                mUpdateAction = 1;
                updateSwitcherState();
            }
        }
    };

    private ContentObserver mDataConnectionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange selfChange=" + selfChange);
            if (!selfChange) {
                updateSwitcherState();
            }
        }
    };

    private void registerPhoneStateListener() {
        mPhoneStateListener = getPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
        Log.d(TAG, "Register registerPhoneStateListener");
    }

    private void unregisterPhoneStateListener() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
        Log.d(TAG, "Register unregisterPhoneStateListener");
    }

    /**
     * Context method.
     * @param context context
     * @param switchs switch
     */
    public DataConnectionEnabler(Context context, Switch switchs) {
        mContext = context;
        mTelephonyManager = TelephonyManager.from(context);
        mSwitch = switchs;
        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mUpdateAction = 1;
    }

    /**
     * use to resume switch status.
     */
    public void resume() {
        int subId = 0;
        registerPhoneStateListener();
        mContext.registerReceiver(mReceiver, mIntentFilter);

        List<SubscriptionInfo> si =  SubscriptionManager.from(mContext)
             .getActiveSubscriptionInfoList();

        if (si != null && si.size() > 0) {
            for (int i = 0; i < si.size(); i++) {
                SubscriptionInfo subInfo = si.get(i);
                subId = subInfo.getSubscriptionId();
        mContext.getContentResolver().registerContentObserver(
                     Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId),
                true, mDataConnectionObserver);
            }
        }
        mSwitch.setOnCheckedChangeListener(this);
        updateSwitcherState();
    }

    /**
     * when pause,unregisterReceiver and changedListener.
     */
    public void pause() {
        unregisterPhoneStateListener();
        mContext.unregisterReceiver(mReceiver);
        mContext.getContentResolver().unregisterContentObserver(mDataConnectionObserver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    /**
     * when switchbutton checked.
     * @param buttonView switchbutton
     * @param isChecked true if on
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != mTelephonyManager.getDataEnabled()) {
            mTelephonyManager.setDataEnabled(isChecked);
            if (isChecked) {
                mUpdateAction = 2;
            } else {
                mUpdateAction = 3;
            }
            updateSwitcherState();
            Log.d(TAG, "onCheckChanged to " + isChecked + ", and do switch");
        } else {
            Log.d(TAG, "onCheckChanged to " + isChecked + ", state is not changed, do nothing");
        }
    }

    private void updateSwitcherState() {
        boolean enabled = isGPRSEnable(mContext);
        boolean enableBefore;
        int subId = 0;
        Log.d(TAG, "updateSwitcherState enalbed=" + enabled);
        mSwitch.setEnabled(enabled);
        ///M: when receive the broadcast ,update the switch on/off

        if (mUpdateAction == 1) {
            enableBefore = mTelephonyManager.getDataEnabled();
            if (enableBefore) {
                // default on, so set true
                mSwitch.setChecked(true);
                } else {
                    // check all sub,. if one is true, set true. otherwise set false
                    boolean tmp = false;
                    List<SubscriptionInfo> si =  SubscriptionManager.from(mContext)
                           .getActiveSubscriptionInfoList();

                     if (si != null && si.size() > 0) {
                         for (int i = 0; i < si.size(); i++) {
                              SubscriptionInfo subInfo = si.get(i);
                              subId = subInfo.getSubscriptionId();
                              tmp = mTelephonyManager.getDataEnabled(subId);
                              if (tmp) {
                                  break;
                              }
                           }
                      }
                      Log.d(TAG, "mUpdateAction 1  tmp = " + tmp);
                      if (tmp) {
                          mSwitch.setChecked(true);
                      } else {
                          mSwitch.setChecked(false);
                      }
                  }
              } else if (mUpdateAction == 2) {
                  // user update to true, set default true
                  mTelephonyManager.setDataEnabled(true);
                  mSwitch.setChecked(true);
              } else if (mUpdateAction == 3) {
                  // user update to false, set all  false
                  List<SubscriptionInfo> si =  SubscriptionManager.from(mContext)
                       .getActiveSubscriptionInfoList();

                  if (si != null && si.size() > 0) {
                      for (int i = 0; i < si.size(); i++) {
                      SubscriptionInfo subInfo = si.get(i);
                      subId = subInfo.getSubscriptionId();
                      Log.d(TAG, "set subId = " + subId + "false");
                      mTelephonyManager.setDataEnabled(subId, false);
                  }
             }
             Log.d(TAG, "set mSwitch false");
             mSwitch.setChecked(false);
        } else {
            mSwitch.setChecked(mTelephonyManager.getDataEnabled());
            Log.d(TAG, "updateSwitcherState setChecked ="
            + mTelephonyManager.getDataEnabled());
        }
        mUpdateAction = 0;
    }

    private PhoneStateListener getPhoneStateListener() {
        return new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState state) {
                Log.d(TAG, "PhoneStateListener: onServiceStateChanged");
                updateSwitcherState();
            }
        };
    }

    /**
     * Returns whether is in airplance or mms is under transaction.
     * @return is airplane or mms is in transaction
     * MMS NOT SEND && isRadioOff IS NOT  && (RADIO ON && SIM IS NOT SIM_INDICATOR_LOCKED)
     */
    static boolean isGPRSEnable(Context context) {
        boolean isMMSProcess = false;
        boolean isRadioOff = isRadioOff(context);
        boolean hasSimUnLocked = !isRadioOff && hasSlotRadioOnNotLocked(context);
        Log.d(TAG, "isMMSProcess=" + isMMSProcess + " isRadioOff=" +
                isRadioOff + ", hasSimUnLocked=" + hasSimUnLocked);
        return !isMMSProcess && !isRadioOff && hasSimUnLocked;
    }

    private static boolean hasSlotRadioOnNotLocked(Context context) {
        for (int slot = PhoneConstants.SIM_ID_1; slot < getSimNumber(context); slot ++) {
            if (isTargetSimRadioOn(slot) && (isSimStateReady(context, slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return is airplane mode or all sim card is set on radio off
     * AIRPLANE MODE ON || SIMNUMBER==0 || RADIO OFF
     */
    private static boolean isRadioOff(Context context) {
        boolean isAllRadioOff = (Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                || (Settings.System.getInt(context.getContentResolver(),
                        Settings.System.MSIM_MODE_SETTING, -1) == ALL_RADIO_OFF)
                || getSimNumber(context) == 0;
        Log.d(TAG, "isRadioOff=" + isAllRadioOff);
        return isAllRadioOff;
    }

    ///get sim number
    private static int getSimNumber(Context context) {
        int simCount = TelephonyManager.from(context).getSimCount();
        Log.d(TAG, "getSimNumber = " + simCount);
        return simCount;
    }

    ///one sim can has one or more slot id
    static boolean isTargetSimRadioOn(int simId) {
        int[] targetSubId = SubscriptionManager.getSubId(simId);
        if (targetSubId != null && targetSubId.length > 0) {
            for (int i = 0; i < targetSubId.length; i++) {
               if (isTargetSlotRadioOn(targetSubId[i])) {
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

    ///slotid is radio on?
    static boolean isTargetSlotRadioOn(int subId) {
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
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    static boolean isSimStateReady(Context context, int simId) {
        int simState = TelephonyManager.from(context).getSimState(simId);
        Log.i(TAG, "isSimStateReady simState=" + simState);
        return simState == TelephonyManager.SIM_STATE_READY;
    }
}

package com.mediatek.esntrack;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//L1.MP3 Patchback only
import android.content.SharedPreferences;
//L1.MP3 Patchback only
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsUsageMonitor;

import com.mediatek.telephony.SmsManagerEx;

import java.util.Timer;
import java.util.TimerTask;

public class EsnTrackService extends Service {
    private static final String TAG = "EsnTrackService";
    private int sSendMessageRetryTimes = 1;
    private TelephonyManager mTelephonyManager;
    private SmsSendReceiver mSmsSendReceiver;
    private String mAction;
    private int mOptrCode;

    // / M: Modify logic for single SIM load.
    private int[] mSlotList;

    private TimerTask mTask = new TimerTask() {
        public void run() {
            sendRegisterMessage();
        }

    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (isSingleLoad()) {
            mSlotList = Const.SINGLE_UIM_ID;
            Log.d(TAG, "Single SIM load.");
        } else {
            mSlotList = Const.UIM_ID_LIST;
            Log.d(TAG, "Dual SIM load.");
        }
        mOptrCode = EsnTrackController.getInstance().getOptrCode();
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mSmsSendReceiver != null) {
            unregisterReceiver(mSmsSendReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand. action = " + action);
            if (action
                    .equalsIgnoreCase(Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE)
                    || action
                            .equalsIgnoreCase(Const.ACTION_CDMA_NEW_OUTGOING_CALL)
                    || action.equalsIgnoreCase(Const.ACTION_CDMA_NEW_SMS_RECVD)
                    || action
                            .equalsIgnoreCase(Const.ACTION_CDMA_UTK_MENU_SELECTION)
                    || action.equalsIgnoreCase(Const.ACTION_CDMA_SMS_MSG_SENT)
                    || action.equalsIgnoreCase(Const.ACTION_CDMA_MT_CALL)
                    || action
                            .equalsIgnoreCase(Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE)) {
                Log.d(TAG, "Action." + action);
                mAction = action;
                sendRegisterMessage();

            } else if (action.equalsIgnoreCase(Const.ACTION_DUMMY)) {
                Log.d(TAG, "DUMMY Action." + action);
            }
        } else {
            Log.w(TAG, "onStartCommand. Intent is null");
        }
        return START_NOT_STICKY;
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

    /**
     * Whether uim's network operator, UIM operator and phone type is correct
     * 
     * @param uimId
     * @return true or false
     */
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
                // if (netwOperator.equals(simOperator)) {
                return true;
                // }
                // return false;

            }
        }
        return false;
    }

    private void sendRegisterMessage() {
        Log.v(TAG, "sendRegisterMessage,current thread: "
                + Thread.currentThread().getId());
        if (mSmsSendReceiver == null) {
            mSmsSendReceiver = new SmsSendReceiver();
            registerReceiver(mSmsSendReceiver, new IntentFilter(
                    Const.ACTION_REGISTER_MESSAGE_SEND));
        }
        String registerMessage = new EsnTrackSmsComposer(this, mAction,
                mOptrCode).getRegisterMessage();
        Log.d(TAG, "message length:" + registerMessage.length());
        int currentLogonUim = getCurrentLogonUim();
        if (currentLogonUim != Const.UIM_NONE) {
            String serverAddress = Const.TATA_SERVER_ADDRESS;
            if (mOptrCode == Const.MTS) {
                serverAddress = Const.MTS_SERVER_ADDRESS;

                try {
                    ISms mSmsManager = ISms.Stub.asInterface(ServiceManager
                            .getService("isms"));
                    mSmsManager
                            .setPremiumSmsPermission(
                                    "com.mediatek.esntrack",
                                    SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW);
                    Log.d(TAG, "set premium sms permission");
                } catch (RemoteException ex) {
                    Log.d(TAG, "set premium sms permission exception");
                }

            }
            // L1.MP3 Patchback only
            SharedPreferences sharedpreferences = this.getSharedPreferences(
                    EsnTrackTriggerReceiver.MY_ADDRESS_PREF, this.MODE_PRIVATE);
            String Address = sharedpreferences.getString(Const.KEY_ADDRESS, "");
            if (Address != null && Address.length() > 0) {
                serverAddress = Address;
            }
            Log.d(TAG, "sendRegisterMessage Address:" + Address);
            Log.d(TAG, "sendRegisterMessage serverAddress:" + serverAddress);
            // L1.MP3 Patchback only

            SmsManagerEx.getDefault().sendTextMessage(serverAddress, null,
                    registerMessage, getSendPendingIntent(), null,
                    currentLogonUim);
            Log.d(TAG, "send message...");

        } else {
            Log.e(TAG,
                    "there is no UIM is logon CDMA net now. Can't send message.");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private PendingIntent getSendPendingIntent() {
        Log.d(TAG, "get send pending intent");
        Intent mIntent = new Intent();
        mIntent.setAction(Const.ACTION_REGISTER_MESSAGE_SEND);

        PendingIntent mSendPendingIntent = PendingIntent.getBroadcast(this, 0,
                mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(TAG, "return a pending intent");
        return mSendPendingIntent;
    }

    class SmsSendReceiver extends BroadcastReceiver {
        private static final String TAG = Const.TAG + "SmsSendReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");
            if (intent != null) {
                String action = intent.getAction();
                if (action.equalsIgnoreCase(Const.ACTION_REGISTER_MESSAGE_SEND)) {
                    int resultCode = getResultCode();
                    Log.d(TAG, "get result code:" + resultCode);
                    if (resultCode == Activity.RESULT_OK) {
                        Log.d(TAG, "result ok! send register message success.");
                        EsnTrackController.getInstance()
                                .serviceFinishedProcessNext(mAction, true);
                        unregisterReceiver(mSmsSendReceiver);
                        mSmsSendReceiver = null;
                        sSendMessageRetryTimes = 1;
                    } else {
                        Log.d(TAG, "send message failed, retry: "
                                + sSendMessageRetryTimes);
                        if (sSendMessageRetryTimes < Const.SEND_MESSAGE_RETRY_TIMES_MAX) {
                            // sendRegisterMessage();

                            Timer timer = new Timer();
                            TimerTask task = new TimerTask() {
                                public void run() {
                                    sendRegisterMessage();
                                }
                            };
                            if (sSendMessageRetryTimes == 1) {
                                timer.schedule(task, Const.FIRST_RETRY);
                            } else if (sSendMessageRetryTimes == 2) {
                                timer.schedule(task, Const.SECOND_RETRY);
                            }

                            sSendMessageRetryTimes++;
                        } else {
                            EsnTrackController.getInstance()
                                    .serviceFinishedProcessNext(mAction, false);
                            unregisterReceiver(mSmsSendReceiver);
                            mSmsSendReceiver = null;
                            sSendMessageRetryTimes = 1;
                        }

                    }
                } else {
                    Log.d(TAG, "action is not valid." + action);
                }

            } else {
                Log.d(TAG, "intent is null.");
            }

        }
    }
}

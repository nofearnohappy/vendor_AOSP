package com.mediatek.deviceregister;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.deviceregister.utils.AgentProxy;
import com.mediatek.deviceregister.utils.PlatformManager;
import com.mediatek.deviceregister.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class RegisterService extends Service {

    private static final String TAG = Const.TAG_PREFIX + "RegisterService";

    private static final int TIMES_RETRY_MAX = 3;
    private static final int UIM_NONE = -1;

    private static final int SLOT_ID_0 = 0;
    private static final int SLOT_ID_1 = 1;
    private static final int[] SINGLE_UIM_ID = {SLOT_ID_0};
    private static final int[] UIM_ID_LIST = {SLOT_ID_0, SLOT_ID_1};

    private String mMeid;
    private int mCurrentSlotId;
    private int mRetryTimes = 0;

    private int[] mSlotList;
    private String[] mImsiOnSim;

    private PlatformManager mPlatformManager;
    private SmsSendReceiver mSmsSendReceiver;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mPlatformManager = new PlatformManager(this);
        PlatformManager.stayForeground(this);

        if (mPlatformManager.isSingleLoad()) {
            mSlotList = RegisterService.SINGLE_UIM_ID;
        } else {
            mSlotList = RegisterService.UIM_ID_LIST;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        PlatformManager.leaveForeground(this);
        unRegisterSendReceiver();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equalsIgnoreCase(Const.ACTION_PRE_BOOT_COMPLETED)) {
                    doAfterPreBoot();

                } else if (action.equalsIgnoreCase(Const.ACTION_REGISTER_FEASIBLE)) {
                    doAfterFeasible();

                } else if (action.equalsIgnoreCase(Const.ACTION_MESSAGE_SEND)) {
                    doAfterMessageSend();

                } else if (action.equalsIgnoreCase(Const.ACTION_CT_CONFIRMED_MESSAGE)) {
                    doAfterConfirmed(intent);
                }
            }
        } else {
            Log.i(TAG, "Intent is null");
            stopSelf();
        }
        return START_STICKY;
    }

    private void doAfterPreBoot() {
        // This is due to system update, reset register info
        if (PlatformManager.hasIndicator(this)) {
            AgentProxy.getInstance().resetRegisterFlag();
            PlatformManager.removeIndicator(this);

        } else {
            // This is due to factory reset, create the indicator file again
            PlatformManager.createIndicator(this);
        }
    }

    private void doAfterFeasible() {
        new Thread() {
            public void run() {
                if (needRegister()) {

                    registerSendReceiver();
                    new Timer().schedule(new TimerTask() {

                        public void run() {
                            sendRegisterMessage();
                        }
                    }, 60 * 1000);

                } else {
                    Log.d(TAG, "Phone not need to register.");
                    RegisterService.this.stopSelf();
                }
            }
        } .start();
    }

    private void doAfterMessageSend() {
        AgentProxy.getInstance().resetRegisterFlag();
        AgentProxy.getInstance().setSavedImsi(mImsiOnSim);

        new Thread() {
            public void run() {
                writeEsnToUim();
            }
        } .start();
    }

    private void doAfterConfirmed(Intent intent) {
        Boolean result = mPlatformManager.checkRegisterResult(intent);
        AgentProxy.getInstance().setRegisterFlag(result);
        if (result) {
            PlatformManager.createIndicator(this);
            Log.i(TAG, "Register success!");
        } else {
            Log.i(TAG, "Register failed!");
        }
        stopSelf();
    }

    private boolean needRegister() {
        Log.v(TAG, "needRegister");

        updateSimState();

        if (mCurrentSlotId == RegisterService.UIM_NONE) {
            Log.w(TAG, "There is no UIM card is connected CDMA net or it is roaming.");
            return false;
        }

        mMeid = mPlatformManager.getDeviceMeid(mCurrentSlotId);
        Log.d(TAG, "mMeid = " + mMeid);
        mImsiOnSim = mPlatformManager.getImsiFromSim(mSlotList);
        if (mImsiOnSim != null) {
            for (int i = 0; i < mImsiOnSim.length; ++i) {
                Log.d(TAG, "mImsiOnSim[" + i + "] = " + mImsiOnSim[i]);
            }
        } else {
            Log.d(TAG, "mImsiOnSim is null");
        }

        if (AgentProxy.getInstance().isRegistered()) {
            Log.d(TAG, "Have registerd, check imsi and meid.");
            Boolean condition = mPlatformManager.isImsiSame(mImsiOnSim, mSlotList) &&
                    mPlatformManager.hasSamePairEsn(mSlotList);

            if (condition) {
                Log.d(TAG, "imsi and meid same");
                return false;
            }
        }

        return true;
    }

    private void updateSimState() {
        mCurrentSlotId = RegisterService.UIM_NONE;

        // CT could in both slot 0 and 1
        if (PlatformManager.supportCTForAllSlots()) {
            for (int slotId : mSlotList) {
                if (mPlatformManager.isSimStateValid(slotId)) {
                    mCurrentSlotId = slotId;
                    break;
                }
            }
        } else {
            // CT could only in slot 0
            if (mPlatformManager.isSimStateValid(SLOT_ID_0)) {
                mCurrentSlotId = SLOT_ID_0;
            }
        }

        Log.d(TAG, "Current logon uim is " + mCurrentSlotId);
    }

    private void sendRegisterMessage() {
        byte[] registerMessage = new RegisterMessage(this).getRegisterMessage();

        Intent intent = new Intent(Const.ACTION_MESSAGE_SEND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mPlatformManager.sendRegisterMessage(registerMessage, pendingIntent, mCurrentSlotId);
    }

    private void writeEsnToUim() {
        String pEsn = Utils.getEsnFromMeid(mMeid);
        mPlatformManager.setUimEsn(mCurrentSlotId, pEsn);
    }

    private void registerSendReceiver() {
        if (mSmsSendReceiver == null) {
            mSmsSendReceiver = new SmsSendReceiver();
            registerReceiver(mSmsSendReceiver, new IntentFilter(Const.ACTION_MESSAGE_SEND));
        }
    }

    private void unRegisterSendReceiver() {
        if (mSmsSendReceiver != null) {
            unregisterReceiver(mSmsSendReceiver);
        }
    }

    public String getCurrentMeid() {
        return mMeid;
    }

    public String getCurrentImsi() {
        return mImsiOnSim[mCurrentSlotId];
    }

    class SmsSendReceiver extends BroadcastReceiver {
        private static final String TAG = Const.TAG_PREFIX + "SmsSendReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive " + intent);

            String action = intent.getAction();
            if (action.equalsIgnoreCase(Const.ACTION_MESSAGE_SEND)) {
                int resultCode = getResultCode();
                Log.i(TAG, "get result code:" + resultCode);

                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Send register message success.");

                    intent.setClass(context, RegisterService.class);
                    context.startService(intent);

                } else {
                    Log.i(TAG, "Send register message failed.");

                    if (mRetryTimes < TIMES_RETRY_MAX) {
                        Log.i(TAG, "Have tried " + mRetryTimes + " times.");
                        sendRegisterMessage();
                        mRetryTimes++;
                    } else {
                        Log.i(TAG, "Send failed, reach limit " + mRetryTimes);
                        stopSelf();
                    }

                }

            } else {
                Log.i(TAG, "action is not valid." + action);
            }
        }
    }
}

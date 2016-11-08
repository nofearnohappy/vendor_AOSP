package com.mediatek.bluetoothle.pasp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

public class PhoneRingerController {
    private static final String TAG = "[Pasp][PhoneRingerController]";
    private TelephonyManager mTelephonyManager;
    private PhoneComingListener mPhoneListener;
    private RingerChangeListener mRingerListener;
    private int mRingerMode = 2;
    private Context mContext;
    private AudioManager mAudioManager = null;
    private TelecomManager telecomManager = null;
    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;
    public static final int RINGER_MODE_NORMAL = 2;

    // get ringer state from system ringerSetting
    // index 0:ringerSetting state; index 1:alertState
    public PhoneRingerController(PhoneComingListener pl, RingerChangeListener rl, Context context) {
        mPhoneListener = pl;
        mRingerListener = rl;
        mContext = context;
        mTelephonyManager = (TelephonyManager) (context.getSystemService(Context.TELEPHONY_SERVICE));
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        telecomManager = (TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        context.registerReceiver(mReceiver, filter);

    }

    public PhoneRingerController(Context context) {
        // r = RingtoneManager.getRingtone(context, mRingtoneUri);
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        telecomManager = (TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);

        Log.i(TAG, "RingerControllerMachine construct instance");
    }

    static ITelephony getTelephonyService() {
        return ITelephony.Stub.asInterface(ServiceManager.checkService(Context.TELEPHONY_SERVICE));
    }

    // index[0]:ringerState::0->silent;1->normal
    // index[1]:alertState::
    // expose Alert Status:
    // Ringer state: index:0; key:0(Ringer State not active); key:1(Ringer State
    // active)
    // Vibrator state: index:1;key:0(Vibrate State not active); key:1(Vibrate
    // State active)
    // Display Alert Status: index:2;key:0(Display Alert Status not
    // active);key:1(Display Alert Status active)
    // active,here default active
    public byte[][] getRingerSetting() {
        int ringerMode = mAudioManager.getRingerMode();
        byte[][] ringerSettingStatus = { { 0 }, { 0 } };
        // alert:silent set byte alert value:100-->4
        byte[] as = { 4 };
        // alert:vibrate:set byte alert value:110-->6
        byte[] av = { 6 };
        // alert ringer nomal:set byte alert value:101-->5
        byte[] an = { 5 };
        // ringer state:0-->silent;1-->normal
        byte[] s = { 0 };
        byte[] n = { 1 };
        Log.i(TAG, "ringer mode=" + ringerMode);
        switch (ringerMode) {
        case RINGER_MODE_SILENT:
            ringerSettingStatus[1] = as;
            ringerSettingStatus[0] = s;
            break;
        case RINGER_MODE_VIBRATE:
            ringerSettingStatus[1] = av;
            ringerSettingStatus[0] = n;
            break;
        case RINGER_MODE_NORMAL:
            ringerSettingStatus[0] = n;
            ringerSettingStatus[1] = an;
            break;
         default:
             Log.v(TAG, "invalid ringer mode");

        }

        return ringerSettingStatus;
    }

    public int getOriginalRingerState() {
        int ringerMode = mAudioManager.getRingerMode();
        return ringerMode;
    }

    public void muteOnce() {        
        try {
            if (telecomManager.isRinging()) {                
                telecomManager.silenceRinger();   
                Log.i(TAG, "excute muteOnce() from telecomManager");
            }
        } catch (Exception ex) {
            Log.w(TAG, "telecomManager throw Exception ", ex);
        }

    }

    public void setSilentMode() {
        if (mAudioManager == null) {
            Log.i(TAG, "setSilentMode::audioManager is null");
            return;
        }
        mRingerMode = mAudioManager.getRingerMode();
        mAudioManager.setRingerModeInternal(RINGER_MODE_SILENT);
        Log.i(TAG, "after call setSilent():setRingerModeInternal mode=" + mAudioManager.getRingerMode());

    }

    public void cancelSilentMode() {
        if (mAudioManager == null) {
            Log.i(TAG, "cancelSilentMode::audioManager is null");
            return;
        }
        if (RINGER_MODE_SILENT == mRingerMode) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_NORMAL);
        } else {
            mAudioManager.setRingerModeInternal(mRingerMode);
        }
        Log.i(TAG, "after call cancelSilentMode():ringer mode=" + mRingerMode);

    }

    public void resumeRingerMode(int ringerMode) {
        // should keep audio config change, not need to resume pre ringer mode
        //mAudioManager.setRingerMode(ringerMode);
    }

    public void unRegisterReceiver(Context context) {
        context.unregisterReceiver(mReceiver);
    }
    private boolean mIsRinging = false;
    // listen the change of system RingerSetting
    // listen incoming phone,in case of coming,call getRingerSetting() to
    // get current ringer state,
    // then write them in characteristic alertState,and according the
    // chageFilte,notify
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.i(TAG, "PhoneStateListener,new state=" + state);
            if (mTelephonyManager != null) {
                int currPhoneCallState = mTelephonyManager.getCallState();
                if (currPhoneCallState == TelephonyManager.CALL_STATE_RINGING) {
                    mPhoneListener.onPhoneStateChanged(true);
                    mIsRinging = true;
                } else if (currPhoneCallState == TelephonyManager.CALL_STATE_IDLE) {
                    mPhoneListener.onPhoneStateChanged(false);
                    mIsRinging = false;
                }
            }
        }
    };

    public interface PhoneComingListener {
        void onPhoneStateChanged(boolean isRinging);
    }
    public interface RingerChangeListener {
        void onRingerModeChanged(byte[] ringerState, byte[] alertState);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
                int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1);
                Log.v(TAG, "receive AudioManager ringer change intent -ringerMode=:" + ringerMode);
                byte[] ringerState = { 2 };
                byte[] alertState = { 3 };
                byte[] s = { 0 };
                byte[] n = { 1 };
                byte[] v = { 2 };
                byte[] as = { 4 };
                byte[] av = { 6 };
                byte[] an = { 5 };
                switch (ringerMode) {
                case RINGER_MODE_SILENT:
                    ringerState = s;
                    if (mIsRinging) {
                        alertState = as;
                    } else {
                        alertState = s;
                    }
                    break;
                case RINGER_MODE_VIBRATE:
                    ringerState = n;
                    if (mIsRinging) {
                        alertState = av;
                    } else {
                        alertState = v;
                    }
                    break;
                case RINGER_MODE_NORMAL:
                    ringerState = n;
                    if (mIsRinging) {
                        alertState = an;
                    } else {
                        alertState = n;
                    }
                    break;
                 default:
                     Log.v(TAG, "invalid ringer change");
                }
                mRingerListener.onRingerModeChanged(ringerState, alertState);
            }
        }

    };

}

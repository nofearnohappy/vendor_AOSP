package com.mediatek.bluetoothle.pasp;

import android.content.Context;
import android.util.Log;

public class RingerControllerMachine {
    public  static final int RINGER_SILENT_STATE = 0;
    public  static final int RINGER_NORMAL_STATE = 1;
    public  static final int SILENT_MODE_VALUE = 1;
    public  static final int MUTE_ONCE_VALUE = 2;
    public  static final int CANCEL_SILENT_VALUE = 3;
    private int mState = 4;
    private PhoneRingerController mPhoneRingerController;
    private static final String TAG = "[Pasp][RingerControllerMachine]";

    public RingerControllerMachine(Context mContext) {
        mPhoneRingerController = new PhoneRingerController(mContext);
    }

    public void setRingerState(int value) {
        Log.i(TAG, "enter setRingerState and value= " + value);
        switch (value) {
        case SILENT_MODE_VALUE:
            mState = RINGER_SILENT_STATE;
            mPhoneRingerController.setSilentMode();
            break;
        case MUTE_ONCE_VALUE:
            mPhoneRingerController.muteOnce();
            break;
        case CANCEL_SILENT_VALUE:
            mState = RINGER_NORMAL_STATE;
            mPhoneRingerController.cancelSilentMode();
            break;
        default:
            break;
        }
    }

    public int getRingerState() {
        return mState;
    }

}

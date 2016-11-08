package com.mediatek.engineermode.lte;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * AT command tool should be able to run commands at background.
 */
public class CommandToolService extends Service {
    private static final String TAG = "EM/CommandToolService";
    private static final int MSG_SEND_NEXT_COMMAND = 1;
    private static final int MSG_AT_COMMAND = 2;

    private List<String> mCommands = new ArrayList<String>();
    private int mInterval = 1;
    private String mOutput = new String();
    private boolean mSending = false;

    private OnUpdateResultListener mOnUpdateResultListener;
    private final CommandToolServiceBinder mBinder = new CommandToolServiceBinder();

    private final Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("@M_" + TAG, "handleMessage() " + msg.what);
            switch (msg.what) {
            case MSG_SEND_NEXT_COMMAND:
                if (!mSending) {
                    return;
                }
                if (mCommands.size() > 0) {
                    sendAtCommand(mCommands.remove(0), MSG_AT_COMMAND);
                    mHander.sendEmptyMessageDelayed(MSG_SEND_NEXT_COMMAND, mInterval * 1000);
                } else {
                    mSending = false;
                    updateResult("Finished\n");
                }
                break;
            default:
                break;
            }
        }
    };

    private final Handler mAtCmdHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("@M_" + TAG, "handleMessage() " + msg.what);
            switch (msg.what) {
            case MSG_AT_COMMAND:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    Object result = ar.result;
                    if (result != null && (result instanceof String[])) {
                        String[] data = (String[]) result;
                        if (data.length > 0) {
                            updateResult("Return: ");
                            for (int i = 0; i < data.length; i++) {
                                updateResult(data[i] + "\n");
                            }
                        }
                    }
                } else {
                    Log.e("@M_" + TAG, "Exception: " + ar.exception);
                    updateResult("Exception: " + ar.exception + "\n");
                }
                break;
            default:
                break;
            }
        }
    };

    public interface OnUpdateResultListener {
        void onUpdateResult();
    }

    public class CommandToolServiceBinder extends Binder {
        CommandToolService getService() {
            return CommandToolService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("@M_" + TAG, "Enter onStartCommand");
        return START_NOT_STICKY;
    }

    public void startTest(List<String> commands, int interval) {
        Log.v("@M_" + TAG, "startTest");
        mCommands = commands;
        mInterval = interval;
        mOutput = "";
        mSending = true;
        mHander.sendEmptyMessage(MSG_SEND_NEXT_COMMAND);
    }

    public void stopTest() {
        Log.v("@M_" + TAG, "stopTest");
        if (mSending) {
            mSending = false;
            updateResult("Stopped\n");
        }
    }

    private void sendAtCommand(String str, int message) {
        Log.d("@M_" + TAG, "sendAtCommand() " + str);
        updateResult("Send " + str + "\n");
        String[] cmd = new String[2];
        cmd[0] = str;
        cmd[1] = "";
        if (str.length() > 3 && str.endsWith("?")) {
            cmd[1] = str.substring(2, str.length() - 1);
        }
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            Phone mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
            mPhone.invokeOemRilRequestStrings(cmd, mAtCmdHander.obtainMessage(message));
        } else {
            Phone mPhone = (Phone) PhoneFactory.getDefaultPhone();
            mPhone.invokeOemRilRequestStrings(cmd, mAtCmdHander.obtainMessage(message));
        }
    }

    private void updateResult(String result) {
        mOutput += result;
        if (mOnUpdateResultListener != null) {
            mOnUpdateResultListener.onUpdateResult();
        }
    }

    public String getOutput() {
        return mOutput;
    }

    public boolean isRunning() {
        return mSending;
    }

    public void setOnUpdateResultListener(OnUpdateResultListener listener) {
        mOnUpdateResultListener = listener;
    }
}


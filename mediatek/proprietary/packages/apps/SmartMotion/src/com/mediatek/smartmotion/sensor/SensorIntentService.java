package com.mediatek.smartmotion.sensor;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.telecom.TelecomManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.util.Log;

import java.util.List;

import com.mediatek.sensorhub.ActionDataResult;
import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.DataCell;
import com.mediatek.smartmotion.MainActivity;
import com.mediatek.smartmotion.R;

public class SensorIntentService extends IntentService {
    private static final String TAG = "SensorIntentService";
    private SoundPool mSounds;
    private int mSoundStreamId;
    private int mSoundId;

    private final class LoadCompleteCallback implements OnLoadCompleteListener {
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized(mSounds) {
                Log.d(TAG, "Sample " + sampleId + " load status = " + status);
                mSounds.notify();
            }
        }
    }

    public SensorIntentService() {
        super("SensorIntentService");
    }

    private void initSoundPool() {
        mSounds = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        //mSounds.setOnLoadCompleteListener(new LoadCompleteCallback());
        mSoundId = mSounds.load(this, R.raw.in_pocket, 0/*priority*/);
        try {
            //synchronized(mSounds) {
            //    mSounds.wait();
            //}
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Log.e(TAG, "initSoundPool: InterruptedException", ex);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!ActionDataResult.hasResult(intent)) {
            Log.w(TAG, "onHandleIntent: intent has no action data result.");
            return;
        }

        if (MainActivity.sDemoMode) {
            TelecomManager telecomManager = TelecomManager.from(this);
            if (telecomManager == null || !telecomManager.isRinging()) {
                Log.w(TAG, "can not get telephonyService or is not ringing");
                return;
            }

            Log.i(TAG, "try to extract result");
            ActionDataResult result = ActionDataResult.extractResult(intent);
            List<DataCell> datalist = result.getData();
            for (DataCell data : datalist) {
                if (data.isPrevious()) {
                    continue;
                }
                int idx = data.getIndex();
                Log.i(TAG, "data index = " + idx);
                switch (idx) {
                case ContextInfo.Pickup.VALUE:
                    Log.i(TAG, "onHandleIntent TYPE_QUICKANSWER");
                    telecomManager.acceptRingingCall();
                    break;
                case ContextInfo.Shake.VALUE:
                    Log.i(TAG, "onHandleIntent TYPE_EASYREJECT");
                    telecomManager.endCall();
                    break;
                case ContextInfo.Facing.FACE_DOWN:
                    Log.i(TAG, "onHandleIntent TYPE_SMARTSILENT");
                    telecomManager.silenceRinger();
                    break;
                }
            }
        } else {
            Log.i(TAG, "onHandleIntent: extracting result...");
            ActionDataResult result = ActionDataResult.extractResult(intent);
            List<DataCell> datalist = result.getData();
            for (DataCell data : datalist) {
                if (data.isPrevious()) {
                    continue;
                }
                int idx = data.getIndex();
                Log.i(TAG, "data index = " + idx);
                switch (idx) {
                case ContextInfo.Pickup.VALUE:
                    Log.i(TAG, "onHandleIntent: TYPE_PICKUP");
                    playSound();
                    break;
                case ContextInfo.Shake.VALUE:
                    Log.i(TAG, "onHandleIntent: TYPE_SHAKE");
                    playSound();
                    break;
                case ContextInfo.Facing.FACE_DOWN:
                    Log.i(TAG, "onHandleIntent: TYPE_FACEDOWN");
                    playSound();
                    break;
                case ContextInfo.Carry.IN_POCKET:
                    Log.i(TAG, "onHandleIntent: TYPE_INPOCKET");
                    playSound();
                    break;
                case ContextInfo.Pedometer.TOTAL_COUNT:
                    Log.i(TAG, "onHandleIntent: TYPE_PEDOMETER");
                    playSound();
                    break;
                case ContextInfo.UserActivity.CURRENT_STATE:
                    Log.i(TAG, "onHandleIntent: TYPE_ACTIVITY");
                    playSound();
                    break;
                }
            }
        }
    }

    private void playSound() {
        Log.i(TAG, "playSound>>>streamId=" + mSoundStreamId);
        if (mSounds == null) {
            initSoundPool();
        }
        mSounds.stop(mSoundStreamId);
        mSoundStreamId = mSounds.play(mSoundId, 1/*leftVolume*/, 1/*rightVolume*/, 1/* priortiy */, 0/* loop */, 1.0f/* rate */);
        Log.i(TAG, "playSound<<<streamId=" + mSoundStreamId);
    }
}

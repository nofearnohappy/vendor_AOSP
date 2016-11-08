package com.mediatek.nfc.dta;

import android.util.Log;
import android.app.Application;
import android.content.Context;
import android.content.ContentResolver;
import android.media.AudioManager;
import android.media.SoundPool;
import android.provider.Settings;

//import com.mediatek.dta.R;
import com.mediatek.nfc.dta.DeviceTestApp;
import com.mediatek.nfc.dta.dynamicload.NativeDynamicLoad;

public class NativeDtaManager {
    private static final String TAG = "DTA";
    private static final boolean DBG = true;

    private static final int MTK_NFC_CHIP_TYPE_MT6605 = 0x02;
    private int mNative;
    private String mMessage;
    private Callback mCallback;
    private static int mChipVersion;

    static {
        int versionCode;
        if (DeviceTestApp.sContext != null) {
            versionCode = Settings.Global.getInt(DeviceTestApp.sContext.getContentResolver(),"nfc_controller_code",2);
        } else {
            Log.d(TAG, "BUG : versionCode default set 0x02(6605)");
            versionCode = 2;
        }

        try {

            switch(versionCode) {
                case MTK_NFC_CHIP_TYPE_MT6605 :
                    Log.d(TAG, "MTK_NFC nfc chip");
                    System.loadLibrary("dta_mt6605_jni");
                    mChipVersion = 0x02;
                    break;
                default:
                    Log.d(TAG, "no nfc chip support");
                    System.loadLibrary("dta_mt6605_jni");
                    mChipVersion = 0x02;
                    break;
            }
        }catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Dta JNI library not found!");
            mChipVersion = 0x00;
        }
    }

    // for use with playSound()
    public static final int SOUND_START = 0;
    public static final int SOUND_END = 1;
    public static final int SOUND_ERROR = 2;

    //Platform listen parameters
    public int mListenSwio;
    public int mListenUidLevel;
    public int mListenDidSupport;
    public int mListenFsci;

    int mStartSound;
    int mEndSound;
    int mErrorSound;
    SoundPool mSoundPool; // playback synchronized on this

    private native int doInitialize();
    private native int doDeinitialize();
    private native int doEnableDiscovery(int patternNumber, int testType);
    private native int doDisableDiscovery();
    private native int doReset();
    private native int doSetDtaMode(int mode);
    private native int doSetDtaQuickMode(int mode);
    private native int doSetPatternNumber(int patternNumber); ///for NDEF
    private native String doGetDtaVersion();  // Jni version
    private native int doSetDtaConfigPath(String path);

    public int initialize() {
        if (DBG) Log.d(TAG, "initialize");
        return doInitialize();
    }

    public int initialize(Callback callback) {
        if (DBG) Log.d(TAG, "initialize + callback");
        mCallback = callback;
        return doInitialize();
    }
/**
 * 2015/06/08 bug fix , when keydown= back , kill nfcstackp
 */
    public int deinitialize() {
        if (DBG) Log.d(TAG, "[QE]deinitialize");
        // clear JNI sync config
        setDtaQuickMode(0);
        // set JNI sync config
        setDtaQuickMode(1);
        return doDeinitialize();
    }

    public int enableDiscovery(int patternNumber, int testType) {
        if (DBG) Log.d(TAG, "enableDiscovery , patternNumber = " + patternNumber);
        return doEnableDiscovery(patternNumber, testType);
    }

    public int disableDiscovery() {
        if (DBG) Log.d(TAG, "disableDiscovery");
        return doDisableDiscovery();
    }

    public int reset() {
        if (DBG) Log.d(TAG, "reset");
        return doReset();
    }
    public int setDtaQuickMode(int mode){
        if (DBG) Log.d(TAG, "setDtaQuickMode :"+mode);
        return doSetDtaQuickMode(mode);
    }
    public int setDtaMode(int mode){
        if (DBG) Log.d(TAG, "setDtaMode");
        return doSetDtaMode(mode);
    }

    public int setPatternNumber(int patternNumber) {
        if (DBG) Log.d(TAG, "setPatternNumber");
        return doSetPatternNumber(patternNumber);
    }

    public int setDtaConfigPath(String path){
        if (DBG) Log.d(TAG, "setDtaConfigPath");
        return doSetDtaConfigPath(path);
    }

    //get version
    public String getDtaVersion(){
        if (DBG) Log.d(TAG, "getDtaVersion");
        return doGetDtaVersion();
    }

    public int getChipVersion(){
        return mChipVersion;
    }

    public String dumpMessage() {
        if (DBG) Log.d(TAG, "dumpMessage : " + mMessage);
        return mMessage;
    }

    public void notifyMessageListener() {
        mCallback.notifyMessageListener();
    }

    public void switchTestState() {
        mCallback.switchTestState();
    }

    public void initSoundPool(Context context) {
        synchronized(this) {
            if (mSoundPool == null) {
                mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
                mStartSound = mSoundPool.load(context, R.raw.start, 1);
                mEndSound = mSoundPool.load(context, R.raw.end, 1);
                mErrorSound = mSoundPool.load(context, R.raw.error, 1);
            }
        }
    }

    public void releaseSoundPool() {
        synchronized(this) {
            if (mSoundPool != null) {
                mSoundPool.release();
                mSoundPool = null;
            }
        }
    }

    public void playSound(int sound) {
        synchronized (this) {
            if (mSoundPool == null) {
                Log.w(TAG, "Not playing sound when NFC is disabled");
                return;
            }
            switch (sound) {
                case SOUND_START:
                    mSoundPool.play(mStartSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_END:
                    mSoundPool.play(mEndSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_ERROR:
                    mSoundPool.play(mErrorSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
            }
        }
    }

    public interface Callback {
        public void notifyMessageListener();
        public void switchTestState();
    }

}

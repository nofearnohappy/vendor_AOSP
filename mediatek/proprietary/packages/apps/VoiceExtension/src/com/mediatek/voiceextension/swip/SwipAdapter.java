package com.mediatek.voiceextension.swip;

import android.util.ArrayMap;
import android.util.Log;

import com.mediatek.voiceextension.VoiceCommonState;
import com.mediatek.voiceextension.cfg.ConfigurationManager;
import com.mediatek.voiceextension.common.CommonManager;

import java.lang.ref.WeakReference;

/**
 * The SwipAdapter provides APIs to communication with JNI layer.
 *
 */
public class SwipAdapter implements ISwipInteractor {

    // Native will set this memory address to check whether the call back
    // exist
    private long mNativeContext = 0;
    private static int sInitResult = 0;
    /**
     * @Integer indicate feature type
     * @ISwipCallback indicate managers which need to be notified
     */
    ArrayMap<Integer, ISwipCallback> mCallbacks = new ArrayMap<Integer, ISwipCallback>();

    /**
     * SwipAdapter constructor.
     */
    public SwipAdapter() {
        native_setup(new WeakReference<SwipAdapter>(this));
        sInitResult = setViePath(ConfigurationManager.getInstance().getModelPath(),
                                           ConfigurationManager.getInstance().getDatabasePath());
    }

    static {
        System.loadLibrary("vie_jni");
        try {
             native_init();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void registerCallback(int featureType, ISwipCallback callback) {

        mCallbacks.put(featureType, callback);

    }

    /**
     * Native init when load library.
     *
     * @throws NoSuchMethodException
     */
    private static final native void native_init() throws NoSuchMethodException;

    /**
     * Need to setup the pointer of swipadapter for native call back.
     *
     * @param jniSwipAdapter_this
     * @throws RuntimeException
     */
    private final native void native_setup(Object jniSwipAdapter_this)
            throws RuntimeException;

    /**
     * When error happen , native need to release some occupied object if
     * possible.
     */
    private final native void release();

    private native int setViePath(String modelPath, String databasePath);

    private native int createVieSetName(String keyName, int featureType);

    private native int deleteVieSetName(String keyName);

    private native int isVieSetCreated(String keyName, int featureType);

    private native String[] getVieSets(String processName, int featureType);

    private native void startVieRecognition(String keyName, int featureType);

    private native void stopVieRecognition(String keyName, int featureType);

    private native void pauseVieRecognition(String keyName, int featureType);

    private native void resumeVieRecognition(String keyName, int featureType);

    private native void setupVieCommandsByString(String keyName,
            String[] commands);

    private native void setupVieCommandsByFile(String keyName, byte[] fileData,
            boolean end);

    private native String[] getVieCommands(String keyName);

    /**
     * Called from native code when an interesting event happens. This method
     * just uses the EventHandler system to post the event back to the main app
     * thread. We use a weak reference to the original SwipAdapter object so
     * that the native code is safe from the object disappearing from underneath
     * it. (This is the cookie passed to native_setup().)
     *
     * @param jniSwipAdapter_ref
     *            JNI refrence class
     * @param setName
     *            swip command set name
     * @param featureType
     *            feature type
     * @param apiType
     *            api type
     * @param msg1
     *            swip result
     * @param msg2
     *            recognition command id
     * @param extraMsg
     *            recognition command string list
     */
    private static void postEventFromNative(Object jniSwipAdapter_ref,
            String setName, int featureType, int apiType, int msg1, int msg2,
            Object extraMsg) {

        SwipAdapter adapter = (SwipAdapter) ((WeakReference<?>) jniSwipAdapter_ref)
                .get();

        String[] recogStrings = null;
        if (extraMsg != null) {
            String[] tempArray = (String[]) extraMsg;
            if (tempArray != null) {
                int length = tempArray.length;
                recogStrings = new String[length];
                System.arraycopy(tempArray, 0, recogStrings, 0, length);
            }
        }

        (adapter.mCallbacks.get(featureType)).onSwipMessageNotify(setName,
                apiType, msg1, msg2, recogStrings);

    }

    @Override
    public boolean isSwipReady() {
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG, "isSwipReady mInitResult:" + sInitResult
                    + ", mNativeContext:" + mNativeContext);
        }
        return (sInitResult == VoiceCommonState.RET_COMMON_SUCCESS)
                && (mNativeContext != 0);
    }

    @Override
    public int createSetName(String name, int featureType) {
        return createVieSetName(name, featureType);
    }

    @Override
    public int deleteSetName(String name) {
        return deleteVieSetName(name);
    }

    @Override
    public String[] getAllSets(String processName, int featureType) {
        return getVieSets(processName, featureType);
    }

    @Override
    public String[] getCommands(String setName) {
        return getVieCommands(setName);
    }

    @Override
    public int isSetCreated(String name, int featureType) {
        return isVieSetCreated(name, featureType);
    }

    @Override
    public void startRecognition(String setName, int featureType) {
        startVieRecognition(setName, featureType);
    }

    @Override
    public void stopRecognition(String setName, int featureType) {
        stopVieRecognition(setName, featureType);
    }

    @Override
    public void pauseRecognition(String setName, int featureType) {
        pauseVieRecognition(setName, featureType);
    }

    @Override
    public void resumeRecognition(String setName, int featureType) {
        resumeVieRecognition(setName, featureType);
    }

    @Override
    public void setCommands(String setName, String[] commands) {
        setupVieCommandsByString(setName, commands);
    }

    @Override
    public void setCommands(String setName, byte[] data, boolean end) {
        setupVieCommandsByFile(setName, data, end);
    }

}

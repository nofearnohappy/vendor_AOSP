package com.mediatek.bluetooth.dtt;

import android.util.Log;

/**
 * Created by MTK01635 on 2015/4/16.
 *
 * Bluetooth Debugging and Testing Tool
 */
public class BluetoothDtt {

    private static final String TAG = "BT.DTT";

    // 對應 btif_dtt.h 定義
    public static final int LOG_TYPE_CATCHER = 0;
    public static final int LOG_TYPE_HCI = 1;
    public static final int LOG_TYPE_FIRMWARE = 2;

    public static final int LOG_STATE_DISABLE = 0;
    public static final int LOG_STATE_W_SOCKET = 1;
    public static final int LOG_STATE_W_FILE = 2;


    public void start(){
        this.initializeNative();
    }

    public void stop(){
        this.cleanupNative();
    }

    public static void configLog(boolean isCatcherEnabled, boolean isHciEnabled, boolean isFirmwareEnabled, String folder){

        BluetoothDtt dtt = new BluetoothDtt();
        dtt.start();

        // 設定 catcher
        dtt.configLogNative(
                BluetoothDtt.LOG_TYPE_CATCHER,
                isCatcherEnabled ? BluetoothDtt.LOG_STATE_W_SOCKET : BluetoothDtt.LOG_STATE_DISABLE,
                folder
        );

        // 設定 hci
        dtt.configLogNative(
                BluetoothDtt.LOG_TYPE_HCI,
                isHciEnabled ? BluetoothDtt.LOG_STATE_W_SOCKET : BluetoothDtt.LOG_STATE_DISABLE,
                folder
        );

        // 設定 firmware
        dtt.configLogNative(
                BluetoothDtt.LOG_TYPE_FIRMWARE,
                isFirmwareEnabled ? BluetoothDtt.LOG_STATE_W_SOCKET : BluetoothDtt.LOG_STATE_DISABLE,
                folder
        );
        dtt.stop();
    }


    /**
     * JNI integration
     */
    static {
        // System.load("/system/lib/libbtdtt_jni.so");
        System.loadLibrary("btdtt_jni");
        classInitNative();
    }

    /**
     * JNI functions
     */
    private native static void classInitNative();
    private native void initializeNative();
    private native void cleanupNative();

    /**
     * native method for log configuration
     *
     * @param type
     * @param state
     * @param folder
     * @return
     */
    public native boolean configLogNative(int type, int state, String folder);

    /**
     * JNI callback - log config callback
     *
     * @param type
     * @param state
     */
    private void onLogConfigChanged(int type, int state){

        // TODO 錯誤時要通知使用者
        Log.i(TAG, "onLogConfigChanged:" + type + ", " + state);
    }
}

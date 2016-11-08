package com.mediatek.bluetooth.dtt;

import android.media.AudioSystem;
import android.util.Log;

/**
 * Created by MTK01635 on 2015/4/17.
 */
public class SpeechLogUtil {

    private static final String TAG = "BtVmLog";

    /** 常數 */
    private static final int DATA_SIZE = 1444;
    private static final int VM_LOG_POS = 1440;
    private static final int SET_SPEECH_VM_ENABLE = 0x60;

    /**
     * enable vm log
     */
    public static boolean enableVmLog() {

        //   error handling: toast + log (android main log)
        //   res1: -1 代表失敗
        //   res2: 0 代表成功, 其他代表 Error Code
        //   res3: 0 代表成功, 其他代表 Error Code
        byte[] data = new byte[DATA_SIZE];
        int res = AudioSystem.setAudioCommand(SET_SPEECH_VM_ENABLE, 1); // 設定參數: VM + EPL
        if (res == -1){
            Log.w(TAG, "enableVmLog().setAudioCommand(SET_SPEECH_VM_ENABLE) failed!");
            return false;
        }
        res = AudioSystem.getEmParameter(data, DATA_SIZE); // 取得最新的參數設定
        if (res != 0){
            Log.w(TAG, "enableVmLog().getEmParameter() error: " + res);
            return false;
        }
        data[VM_LOG_POS] |= 0x01;  // 啟動 Log 設定
        res = AudioSystem.setEmParameter(data, DATA_SIZE); // 回寫設定
        if (res != 0){
            Log.w(TAG, "enableVmLog().setEmParameter() error: " + res);
            return false;
        }
        return true;
    }

    /**
     * disable vm log
     *
     * @return result
     */
    public static boolean disableVmLog() {

        byte[] data = new byte[DATA_SIZE];
        int res = AudioSystem.getEmParameter(data, DATA_SIZE); // 取得最新的參數設定
        if (res != 0){
            Log.w(TAG, "disableVmLog().getEmParameter() error: " + res);
            return false;
        }
        data[VM_LOG_POS] &= ~0x01; // 關閉 Log 設定
        res = AudioSystem.setEmParameter(data, DATA_SIZE); // 回寫設定
        if (res != 0){
            Log.w(TAG, "disableVmLog().setEmParameter() error: " + res);
            return false;
        }
        return true;
    }
}

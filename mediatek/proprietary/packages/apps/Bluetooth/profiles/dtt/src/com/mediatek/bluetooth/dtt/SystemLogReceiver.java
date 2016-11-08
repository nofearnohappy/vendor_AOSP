package com.mediatek.bluetooth.dtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SystemLogReceiver extends BroadcastReceiver {

    private static final String TAG = "BT.DTT";

    // Broadcast Action
    public static final String MTK_LOGGER_ACTION_START = "com.mediatek.mtklogger.BLUETOOTH_LOG";
    public static final String MTK_LOGGER_ACTION_STOP = "com.mediatek.mtklogger.BLUETOOTH_LOG";

    // Broadcast Extra
    public static final String MTK_LOGGER_EXTRA_SWITCH = "logging_switch";  // 0:stop, 1:start
    public static final String MTK_LOGGER_EXTRA_STATUS = "btlog_status";
    public static final String MTK_LOGGER_EXTRA_PATH = "mtklog_path";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.i(TAG, "SystemLogReceiver:" + action);

        // not proper action
        if (!MTK_LOGGER_ACTION_START.equals(action) && !MTK_LOGGER_ACTION_STOP.equals(action)){
            Log.w(TAG, "invalid action: " + action);
            return;
        }

        // retrive info
        int logSwitch = intent.getIntExtra(MTK_LOGGER_EXTRA_SWITCH, -1);
        switch( logSwitch ){
            case 0:
                this.handleLogStop(context, intent);
                break;
            case 1:
                this.handleLogStart(context, intent);
                break;
            default:
                Log.e(TAG, "undefined logging_switch:" + logSwitch);
        }
    }

    /**
     * 啟動相關 Log
     *
     * @param context
     * @param intent
     */
    private void handleLogStart(Context context, Intent intent){

        Log.i(TAG, "starting bluetooth log...");

        // bit0: hci / bit1: fw => 0：hci和fw都是关闭 / 1：hci是开启，fw是关闭 / 2：hci是关闭，fw是开启 / 3：hci和fw都是开启
        int status = intent.getIntExtra(MTK_LOGGER_EXTRA_STATUS, -1);
        if (status == -1){
            Log.e(TAG, "invalid btlog_status" + status);
            return;
        }

        // folder TODO 決定到 mobile log 的 path
        String folder = intent.getStringExtra(MTK_LOGGER_EXTRA_PATH);

        // 啟動 speech vm log
        if (!SpeechLogUtil.enableVmLog()){
            Log.w(TAG, "enable speech vm log failed!");
        }

        // check log config
        boolean isCatcherEnabled = ( status & 0x04 ) > 0;
        boolean isHciEnabled = ( status & 0x01 ) > 0;
        boolean isFirmwareEnabled = ( status & 0x02 ) > 0;

        // 設定 bluetooth log
        Log.i(TAG, "log config: [" + isCatcherEnabled + "," + isHciEnabled + "," + isFirmwareEnabled + "][" + folder + "]");
        BluetoothDtt.configLog(isCatcherEnabled, isHciEnabled, isFirmwareEnabled, folder);
    }

    /**
     * 停用相關 Log
     *
     * @param context
     * @param intent
     */
    private void handleLogStop(Context context, Intent intent) {

        Log.i(TAG, "stopping bluetooth log...");

        // 設定 bluetooth log (全部關閉)
        BluetoothDtt.configLog(false, false, false, null);

        // 關閉 speech vm log
        if (!SpeechLogUtil.disableVmLog()){
            Log.w(TAG, "disable speech vm log failed!");
        }
    }
}

package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.mms.callback.IDialogModeActivityCallback;
import com.mediatek.mms.ext.DefaultOpDialogModeActivityExt;

/**
 * Op01DialogModeActivityExt.
 *
 */
public class Op01DialogModeActivityExt extends DefaultOpDialogModeActivityExt {

    private final static String TAG = "Mms/Op01DialogModeActivityExt";

    /**
     * Construction.
     * @param context Context
     */
    public Op01DialogModeActivityExt(Context context) {
        super(context);
    }

    @Override
    public boolean openThread() {
        return true;
    }

    @Override
    public boolean simSelection(int selectedSubId, int subCount,
            String number, int messageSubId, Intent intent,
            long currentSubId, int[] subIdList,
            IDialogModeActivityCallback callback) {

        if (subCount == 1) {
            callback.setIpSelectedSubId(selectedSubId);
            Log.d(TAG, "mSelectedSubId = " + selectedSubId);
            return false;
        }

        if (messageSubId == Settings.System.SMS_SIM_SETTING_AUTO) {
            for (int subid : subIdList) {
                if (subid == currentSubId) {
                    selectedSubId = subid;
                    break;
                }
            }

            if (selectedSubId == currentSubId) {
                callback.setIpSelectedSubId(selectedSubId);
                callback.onIpConfirmSendMessageIfNeeded();
            } else {
                callback.onIpShowSubSelectedDialog(false, intent);
                callback.onIpUpdateSendButtonState();
            }
            return true;
        }
        Log.d(TAG, "mSelectedSubId = " + selectedSubId);
        return false;
    }
}

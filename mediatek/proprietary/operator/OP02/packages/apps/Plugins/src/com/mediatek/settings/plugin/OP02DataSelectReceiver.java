package com.mediatek.settings.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;
/**
 * For CU Spec, show a dialog to set default data.
 */
public class OP02DataSelectReceiver extends BroadcastReceiver {

    private static final String TAG = "OP02DataSelectReceiver";
    private TelephonyManager mTelephonyManager = null;
    private SubscriptionManager mSubscriptionManager = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "receive broadcast: " + action);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(context);
        if (isSimDialogNeeded(context)) {
            Intent startDataPicker = new Intent(context, OP02DataPickService.class);
            context.startService(startDataPicker);
        }
    }

    private boolean isSimDialogNeeded(Context context) {
        final int numSlots = mTelephonyManager.getSimCount();
        final boolean isInProvisioning = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 0;

        Log.d(TAG, "isSimDialogNeeded numSlots = " + numSlots +
                " isInProvisioning = " + isInProvisioning);
        // Do not create notifications on single SIM devices or when provisiong.
        if (numSlots < 2 || isInProvisioning) {
            return false;
        }

        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null || subInfoList.size() < 1) {
            Log.d(TAG, "do nothing since no cards inserted");
            return false;
        }
        Log.d(TAG, "dialog show flag: " + OP02DataPickService.sIsShow);
        Log.d(TAG, "dialog click flag: " + OP02DataPickService.sIsClick);
        if (OP02DataPickService.sIsShow || OP02DataPickService.sIsClick) {
            return false;
        }
        return true;
    }
}

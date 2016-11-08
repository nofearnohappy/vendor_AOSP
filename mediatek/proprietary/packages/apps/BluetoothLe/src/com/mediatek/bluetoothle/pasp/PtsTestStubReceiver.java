package com.mediatek.bluetoothle.pasp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class PtsTestStubReceiver extends BroadcastReceiver {
    private static final String TAG = "[Pasp][PtsTestStubReceiver]";
    private static final String PTSACTION = "bluetoothle.pasp.pts";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "PtsTestStubreceiver receives intent,intent=" + intent);
            String action = intent.getAction();

            if (action.equals(PTSACTION)) {

                 updateDisplayState();

            }
    }

    private void updateDisplayState() {
        PaspServerService.getPaspInstance().updateDisplayState();
    }

 }

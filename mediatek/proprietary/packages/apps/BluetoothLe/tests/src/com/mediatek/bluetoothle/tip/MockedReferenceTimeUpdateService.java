
package com.mediatek.bluetoothle.tip;

import android.util.Log;

public class MockedReferenceTimeUpdateService extends ReferenceTimeUpdateService {
    private static final String TAG = "MockedReferenceTimeUpdateService";

    MockedReferenceTimeUpdateService(final TipServerService tipService) {
        super(tipService);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onStateUpdate(final int state, final int result) {
        Log.d(TAG, "onStateUpdate");
    }

    @Override
    public void onStateUpdate(final int state) {
        Log.d(TAG, "onStateUpdate");
    }

    @Override
    public void onTimeUpdate(final long time) {
        Log.d(TAG, "onTimeUpdate");
    }
}

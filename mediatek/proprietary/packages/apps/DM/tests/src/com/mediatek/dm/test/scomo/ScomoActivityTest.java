/**
 *
 */
package com.mediatek.dm.test.scomo;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.util.Log;

import com.mediatek.dm.scomo.DmScomoActivity;

/**
 * @author MTK80987
 *
 */
public class ScomoActivityTest extends ActivityUnitTestCase<DmScomoActivity> {
    private static final String TAG = "[ScomoActivityTest]";
    private Intent intent;
    private Instrumentation instrument;

    public ScomoActivityTest() {
        super(DmScomoActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        instrument = this.getInstrumentation();
        intent = new Intent(Intent.ACTION_MAIN);
        startActivity(intent, null, null);
    }

    public void testOnResume() {
        Log.d(TAG, "start test onResume");
        instrument.callActivityOnResume(this.getActivity());
    }

    public void testOnPause() {
        Log.d(TAG, "start test onPause");
        instrument.callActivityOnPause(this.getActivity());
    }

    public void testOnStop() {
        Log.d(TAG, "start test onStop");
        instrument.callActivityOnStop(this.getActivity());
    }

    public void testOnDestroy() {
        Log.d(TAG, "start test onDestroy");
        instrument.callActivityOnDestroy(this.getActivity());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}

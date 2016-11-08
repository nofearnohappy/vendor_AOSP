/**
 *
 */
package com.mediatek.dm.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.mediatek.dm.DmNiInfoActivity;

/**
 * @author MTK80987
 *
 */
public class DmNiInfoActivityTest extends ActivityInstrumentationTestCase2<DmNiInfoActivity> {
    private static final String TAG = "[DmNiInfoActivityTest]";
    private Instrumentation i;
    public static final String EXTRA_TYPE = "Type";

    public static final int TYPE_ALERT_1100 = 1;
    public static final int TYPE_ALERT_1101 = 2;
    public static final int TYPE_ALERT_1103_1104 = 3;
    public static final int TYPE_UIMODE_VISIBLE = 4;
    public static final int TYPE_UIMODE_INTERACT = 5;

    public DmNiInfoActivityTest() {
        super(DmNiInfoActivity.class);
        // TODO Auto-generated constructor stub
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testOnCreate() {
        Log.d(TAG, "test onCreate begin");
        try {

            Intent intent = new Intent();
            intent.putExtra(EXTRA_TYPE, TYPE_ALERT_1100);
            i.callActivityOnCreate(this.getActivity(), null);

            Thread.sleep(500);

            intent.putExtra(EXTRA_TYPE, TYPE_ALERT_1101);
            i.callActivityOnCreate(this.getActivity(), null);
            Thread.sleep(500);

            intent.putExtra(EXTRA_TYPE, TYPE_ALERT_1103_1104);
            i.callActivityOnCreate(this.getActivity(), null);
            Thread.sleep(500);

            intent.putExtra(EXTRA_TYPE, TYPE_UIMODE_VISIBLE);
            i.callActivityOnCreate(this.getActivity(), null);
            Thread.sleep(500);

            intent.putExtra(EXTRA_TYPE, TYPE_UIMODE_INTERACT);
            i.callActivityOnCreate(this.getActivity(), null);
            Thread.sleep(500);

        }
        catch (Exception e) {
            Log.d(TAG, "test onCreate fail");
            e.printStackTrace();
        }

    }



    protected void tearDown() throws Exception {
        super.tearDown();
    }

}

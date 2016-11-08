
package com.mediatek.op01.tests;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.widget.VideoView;
import android.widget.TextView;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.plugin.Op01MmsSlideShowExt;

import android.util.TypedValue;


public class Op01MmsSlideShowExtTest extends InstrumentationTestCase {
    private static final int PLAY_AS_PAUSE = 1;
    private final static float DEFAULT_TEXT_SIZE = 18;
    private Context context;
    private static Op01MmsSlideShowExt mMmsSlideShowPlugin = null;
    private static final String TAG = "Op01MmsSlideShowExtTest";

    protected void setUp() throws Exception {
        super.setUp();
        context = this.getInstrumentation().getContext();
        mMmsSlideShowPlugin = (Op01MmsSlideShowExt)MPlugin.createInstance("com.mediatek.mms.ext.IMmsSlideShowExt",context);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMmsSlideShowPlugin = null;
    }

    public void testConfigTextView() {
        TextView view;
        assertNotNull(mMmsSlideShowPlugin);
        view = new TextView(context);
        if (view != null) {
            mMmsSlideShowPlugin.configTextView(context, view);

            int valDefault = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_TEXT_SIZE, context.getResources().getDisplayMetrics());

            assertEquals(valDefault, (int) view.getTextSize());
            Log.d("@M_" + TAG, "view size = " + view.getTextSize());
            Log.d("@M_" + TAG, "view valDefault = " + valDefault);
        } else {
            Log.e("@M_" + TAG, "can not create Textview");
            assertTrue(false);
        }
    }

    public void testConfigVideoView() {
        myVideoView v;
        assertNotNull(mMmsSlideShowPlugin);
        v = new myVideoView(context);
        mMmsSlideShowPlugin.configVideoView(v);
        assertEquals(1, v.getSeekTo());
    }

    private class myVideoView extends VideoView
    {
        int index = 0;
        public void seekTo(int i) {
            index = i;
        }

        public int getSeekTo() {
            return index;
        }

        public myVideoView(Context context) {
            super(context);
        }
    }
}

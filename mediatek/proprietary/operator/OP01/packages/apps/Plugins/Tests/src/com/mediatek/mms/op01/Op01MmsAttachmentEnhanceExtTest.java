package com.mediatek.op01.tests;

import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.widget.TextView;

import com.mediatek.common.MPlugin;

import com.mediatek.mms.plugin.Op01MmsAttachmentEnhanceExt;
import android.os.Bundle;




public class Op01MmsAttachmentEnhanceExtTest extends InstrumentationTestCase {
    private Context context;
    private static Op01MmsAttachmentEnhanceExt mMmsAttachmentPlugin = null;
    private static final String TAG = "Op01MmsAttachmentEnhanceExtTest";
    private static final String MMS_SAVE_MODE = "savecontent" ;

    private static final int MMS_SAVE_OTHER_ATTACHMENT = 0;
    private static final int MMS_SAVE_ALL_ATTACHMENT = 1;

    protected void setUp() throws Exception {
        super.setUp();
        context = this.getInstrumentation().getContext();
        mMmsAttachmentPlugin = (Op01MmsAttachmentEnhanceExt)MPlugin.createInstance("com.mediatek.mms.ext.IMmsAttachmentEnhanceExt",context);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMmsAttachmentPlugin = null;
    }

   // public void testIsSupportAttachmentEnhance() {
   //     boolean res;
   //     assertNotNull(mMmsAttachmentPlugin);
   //     res = mMmsAttachmentPlugin.isSupportAttachmentEnhance();
        // assertEquals(true, res);
   //     assertTrue(res);
   // }

    public void testSetAttachmentName() {
        TextView txt;
        assertNotNull(mMmsAttachmentPlugin);
        txt = new TextView(context);
        mMmsAttachmentPlugin.setAttachmentName(txt, 2);

        if (txt != null) {
            //assertEquals(getString(R.string.multi_files), txt.getText());
            assertEquals("Multi-files", txt.getText());
        } else {
            Log.e("@M_" + TAG, "Can not create Textview");
            assertTrue(false);
        }
    }

    public void testSetSaveAttachIntent() {
        assertNotNull(mMmsAttachmentPlugin);
        Intent i = new Intent();;
        mMmsAttachmentPlugin.setSaveAttachIntent(i, MMS_SAVE_OTHER_ATTACHMENT);

        int smode = -1;

        Bundle data = i.getExtras();
        if (data != null) {
            smode = data.getInt(MMS_SAVE_MODE);
        }
        assertEquals(smode, MMS_SAVE_OTHER_ATTACHMENT);
    }

    public void testGetSaveAttachMode() {
        assertNotNull(mMmsAttachmentPlugin);
        Intent i = new Intent();
        Bundle data = new Bundle();
        data.putInt(MMS_SAVE_MODE, MMS_SAVE_OTHER_ATTACHMENT);
        i.putExtras(data);
        assertEquals(MMS_SAVE_OTHER_ATTACHMENT, mMmsAttachmentPlugin.getSaveAttachMode(i));
    }
}

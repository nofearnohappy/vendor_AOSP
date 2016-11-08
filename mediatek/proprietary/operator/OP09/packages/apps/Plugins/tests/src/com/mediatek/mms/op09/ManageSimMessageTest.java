package com.mediatek.mms.op09;

import android.database.MatrixCursor;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsManageSimMessageExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;

import java.util.HashMap;

public class ManageSimMessageTest extends BasicCase {
    private IMmsManageSimMessageExt mManageSimMsg;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mManageSimMsg =	MPlugin.createInstance("com.mediatek.mms.ext.IMmsManageSimMessage",mContext);
    }

    /// M: Change the assert statement if necessary. Default: not international card.
    public void test001IsInternationalCard() {
        assertTrue("Slot 0 is not DUAL_MODE card. The test case maybe need to be modified!",
                mManageSimMsg.isInternationalCard(0));
        assertFalse("Slot 1 is DUAL_MODE card. The test case maybe need to be modified!",
                mManageSimMsg.isInternationalCard(1));
    }

    public void test002CanBeOperated() {
        /// M: Construct a cursor.
        String[] column = new String[] {"index_on_icc"};
        MatrixCursor cursor = new MatrixCursor(column);
        cursor.addRow(new Object[] {new Integer(1024)});
        cursor.moveToFirst();

        assertFalse(mManageSimMsg.canBeOperated(cursor));
    }

    public void test003HasIncludeUnoperatedMessage() {
        HashMap<String, Boolean> map = new HashMap<String, Boolean>(1);
        map.put("1024", true);

        assertTrue(mManageSimMsg.hasIncludeUnoperatedMessage(map.entrySet().iterator()));
    }

    public void test004FilterUnoperatedMsgs() {
        String[] test = new String[] {"1024", "1", "10", "4095"};
        String[] expected = new String[] {"1", "10"};

        String[] actual = mManageSimMsg.filterUnoperatedMsgs(test);
        assertEquals(expected[0], actual[0]);
        assertEquals(expected[1], actual[1]);
    }

    public void test005GetAllContentUriForInternationalCard() {
        String actual = mManageSimMsg.getAllContentUriForInternationalCard(0).toString();
        assertEquals("content://sms/icc_international", actual);

        actual = mManageSimMsg.getAllContentUriForInternationalCard(1).toString();
        assertEquals("content://sms/icc2_international", actual);
    }
}

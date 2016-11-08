package com.mediatek.mms.op09;

import android.content.Intent;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsAdvanceSearchExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;
import com.mediatek.pluginmanager.PluginManager;

public class MmsAdvanceSearchTest extends BasicCase {
    private IMmsAdvanceSearchExt mAdvanceSearch;
    private static final String CONTENT = "Content";
    private static final String NAME = "Name";
    private static final String NUMBER = "10086";
    private static final long BEGIN_DATE = 1371398400000L;
    private static final long END_DATE = 1371484800000L;
    private static final String EXPECTED_URI = "content://mms-sms/searchAdvanced?"
            + IMmsAdvanceSearchExt.ADVANCED_SEARCH_CONTENT + "=" + CONTENT
            + "&" + IMmsAdvanceSearchExt.ADVANCED_SEARCH_NAME + "=" + NAME
            + "&" + IMmsAdvanceSearchExt.ADVANCED_SEARCH_NUMBER + "=" + NUMBER
            + "&" + IMmsAdvanceSearchExt.ADVANCED_SEARCH_BEGIN_DATE + "=" + BEGIN_DATE
            + "&" + IMmsAdvanceSearchExt.ADVANCED_SEARCH_END_DATE + "=" + END_DATE;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAdvanceSearch = MPlugin.createInstance("com.mediatek.mms.ext.IMmsAdvanceSearch", mContext);
    }

    public void test001BuildUriAPI() {
        Intent intent = new Intent();
        intent.putExtra(IMmsAdvanceSearchExt.ADVANCED_SEARCH_CONTENT, CONTENT);
        intent.putExtra(IMmsAdvanceSearchExt.ADVANCED_SEARCH_NAME, NAME);
        intent.putExtra(IMmsAdvanceSearchExt.ADVANCED_SEARCH_NUMBER, NUMBER);
        intent.putExtra(IMmsAdvanceSearchExt.ADVANCED_SEARCH_BEGIN_DATE, BEGIN_DATE);
        intent.putExtra(IMmsAdvanceSearchExt.ADVANCED_SEARCH_END_DATE, END_DATE);

        mAdvanceSearch.initAdvancedData(intent);
        assertEquals(EXPECTED_URI, mAdvanceSearch.buildAdvanceSearchUri().toString());
    }

}

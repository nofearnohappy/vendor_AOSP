package com.mediatek.engineermode.cpustress;

import java.util.Collection;
import java.util.HashMap;


public class TestDataSet {
    private static final String TAG = "cpustress/TestDataSet";
    private HashMap<Integer, TestData> mTestDataSet = null;

    private String mTestName;
    public long leftTestCount;
    public boolean running;
    public boolean wantStop;
    public TestDataSet(String name) {
        mTestName = name;
        mTestDataSet = new HashMap<Integer, TestData>();
    }

    public String getTestName() {
        return mTestName;
    }

    public TestData getTestData(int id) {
        if (mTestDataSet != null) {
            return mTestDataSet.get(id);
        }
        return null;
    }

    public void addTestData(TestData data) {
        if (mTestDataSet != null) {
            mTestDataSet.put(data.getId(), data);
        }
    }

    public void addPatchTestData(int[] ids, int testItemNum) {
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                TestData data = new TestData(ids[i], testItemNum);
                addTestData(data);
            }
        }
    }

    public Collection<TestData> getTestDataCollect() {
        return mTestDataSet.values();
    }
}

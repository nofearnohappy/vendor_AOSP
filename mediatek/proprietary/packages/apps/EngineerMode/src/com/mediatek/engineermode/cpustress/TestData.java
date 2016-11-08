package com.mediatek.engineermode.cpustress;

public class TestData {

    public static final int TEST_PASS = 1;
    public static final int TEST_FAIL = 255;
    public static final int TEST_SKIP = 2;
    public static final int TEST_NO_TEST = 3;
    private int mId;
    private int[] mTestResults;
    private int[] mIntParams;
    public int testedCount;
    public int passedCount;
    public boolean enabled;
    public TestData(int id, int testItemNum) {
        mId = id;
        mTestResults = new int[testItemNum];
    }

    public int getId() {
        return mId;
    }

    public int getTestResult(int index) {
        if (mTestResults != null && index >= 0 && index < mTestResults.length) {
            return mTestResults[index];
        }
        return -1;
    }

    public void setTestResult(int index, int val) {
        if (mTestResults != null && index >= 0 && index < mTestResults.length) {
            mTestResults[index] = val;
        }
    }

    public void clearTestResult() {
        if (mTestResults != null) {
            for (int i = 0; i < mTestResults.length; i++) {
                mTestResults[i] = 0;
            }
        }
    }

    public void setIntParams(int[] params) {
        mIntParams = params;
    }

    public int[] getIntParams() {
        return mIntParams;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dump TestData:");
        sb.append(" id:").append(mId);
        sb.append(" enabled:").append(enabled);
        sb.append(" testedCount:").append(testedCount);
        sb.append(" passedCount:").append(passedCount);
        sb.append(" testResult:");
        if (mTestResults != null) {
            for (int n : mTestResults) {
                sb.append(" ").append(n);
            }
        } else {
            sb.append("null");
        }
        return sb.toString();
    }
}

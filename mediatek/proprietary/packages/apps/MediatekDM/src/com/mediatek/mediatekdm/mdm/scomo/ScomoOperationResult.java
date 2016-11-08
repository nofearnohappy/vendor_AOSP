package com.mediatek.mediatekdm.mdm.scomo;

public class ScomoOperationResult {
    boolean mAsync;
    MdmScomoResult mResult;

    public ScomoOperationResult(MdmScomoResult resultCode) {
        mAsync = false;
        mResult = resultCode;
    }

    public ScomoOperationResult() {
        mAsync = true;
        mResult = null;
    }

    public String toString() {
        return "Result(Async = " + mAsync + ", Code = " + mResult + ")";
    }
}

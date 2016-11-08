package com.mediatek.mediatekdm.mdm.lawmo;

/**
 * Structure representing LAWMO operation result. The appropriate constructor should be used,
 * depending if the operation is synchronous or asynchronous. For SYNCHRONOUS operation results, use
 * this constructor: LawmoOperationResult(LawmoResultCode resultCode) For ASYNCHRONOUS operation
 * results, use this constructor: LawmoOperationResult()
     *
 * @author mtk81226
 */
public class LawmoOperationResult {
    final LawmoResultCode mResultCode;
    final boolean mAsync;

    public LawmoOperationResult(LawmoResultCode resultCode) {
        mResultCode = resultCode;
        mAsync = false;
    }

    public LawmoOperationResult() {
        mResultCode = null;
        mAsync = true;
    }
}

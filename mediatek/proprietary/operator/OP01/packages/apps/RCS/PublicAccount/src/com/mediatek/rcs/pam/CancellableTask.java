package com.mediatek.rcs.pam;

import com.mediatek.rcs.pam.model.ResultCode;

public abstract class CancellableTask implements Runnable {
    protected final long mId;
    protected final PASCallbackWrapper mCallback;
    private int mCancelReason = Constants.INVALID;
    protected boolean mIsBackground = false;

    public CancellableTask(long requestId, PASCallbackWrapper callback) {
        this(requestId, callback, false);
    }

    public CancellableTask(long requestId, PASCallbackWrapper callback, boolean isBackground) {
        mId = requestId;
        mCallback = callback;
        mIsBackground = isBackground;
    }

    @Override
    public void run() {
        if (!isCancelled()) {
            if (isBackground()) {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
            } else {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            }
            doRun();
        }
    }

    public void cancel() {
        cancel(ResultCode.USER_CANCELLED);
    }

    public void cancel(int reason) {
        if (reason == Constants.INVALID) {
            throw new Error("Invalid cancel reason");
        }
        mCancelReason = reason;
    }

    public boolean isCancelled() {
        return mCancelReason != Constants.INVALID;
    }

    public boolean isBackground() {
        return mIsBackground;
    }

    public int getCancelReason() {
        return mCancelReason;
    }

    protected abstract void doRun();
}
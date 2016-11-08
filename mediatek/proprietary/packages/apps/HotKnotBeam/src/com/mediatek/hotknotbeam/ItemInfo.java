package com.mediatek.hotknotbeam;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.FailureReason;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;

/**
 * Stores information about an individual upload.
 */
public class ItemInfo {
    private final static String TAG = HotKnotBeamService.TAG;
    private final static int MAX_ID_VALUE = 100000;

    protected Context mContext;
    public int mId;
    public int mGroupId;
    public int    mOrder;
    public int    mCount;
    public String mFileName;
    public String mTitleName;
    public State mState;
    public boolean mIsSucceed;
    public boolean mIsCompress;
    public FailureReason     mFailReason;
    public int mDoneItem;

    public int mTotalBytes;
    public int mCurrentBytes;
    public int mLastCurrentBytes;
    public long mLastTimeStamp;


    public ItemInfo(String filename, int groupId, int totalSize, Context context) {
        mId = Utils.getId();
        mFileName = filename;
        mTitleName = filename;
        mGroupId = groupId;
        mTotalBytes = totalSize;
        mState = HotKnotBeamConstants.State.CONNECTING;

        mContext = context;

        mOrder = 0;
        mCount = 0;
        mDoneItem = 0;
        mCurrentBytes = 0;
        mIsSucceed = false;
        mIsCompress = false;
        mFailReason = FailureReason.NONE;
        mLastTimeStamp = SystemClock.elapsedRealtime();
    }

    public void setState(State state) {
        mState = state;
    }

    public boolean getResult() {
        return mIsSucceed;
    }

    public void setResult(boolean succeed) {
        mIsSucceed = succeed;
    }

    public void setCurrentBytes(int currentBytes) {
        mCurrentBytes = currentBytes;
    }

    public int getCurrentBytes() {
        return mCurrentBytes;
    }

    public void setTotalBytes(int totalBytes) {
        mLastCurrentBytes = -1;
        mLastTimeStamp = SystemClock.elapsedRealtime();

        mTotalBytes = totalBytes;
    }

    public int getTotalBytes() {
        return mTotalBytes;
    }

    public boolean isCompressed() {
        return false;
    }

    public void setTitle(String title) {
        mTitleName = title;
    }

    public String getTitle() {
        return mTitleName;
    }

    public int getId() {
        return mId;
    }

    public String getTag() {
        return "";
    }

    public FailureReason getFailReason() {
        return mFailReason;
    }

    public void setFailReason(FailureReason reason) {
        setState(State.COMPLETE);
        setResult(false);
        mFailReason = reason;
    }

    public String toString() {
        StringBuilder str = new StringBuilder(mId + ":");

        str.append(getTag() + " " + mState + " " + mCurrentBytes + "/"
                + mTotalBytes + ":" + mOrder + ":" + mCount + ":" + isGroup()
                + ":" + getResult() + ":" + mFileName);

        return str.toString();
    }

    public boolean isGroup() {
        return mGroupId != HotKnotBeamConstants.NON_GROUP_ID;
    }

    public int getDoneItem() {
        return mDoneItem;
    }

    public void setDoneItem(int seq) {
        mDoneItem = seq;
    }

    public boolean isLastOne() {
        boolean isFinal = true;

        if (mGroupId != HotKnotBeamConstants.NON_GROUP_ID) {
            if (mCount != mOrder) {
                isFinal = false;
            }
        }

        Log.i(TAG, "isLastOne:" + isFinal);
        return isFinal;
    }

    public int getPercent() {
        int percent = 0;

        if (mTotalBytes == 0) {
            return 0;
        }

        if (mState == State.RUNNING) {
            long currentBytes = mCurrentBytes;
            long totalBytes = mTotalBytes;
            percent = (int) ((currentBytes * 100) / totalBytes);
        } else if (mState == State.COMPLETE && getResult()) {
            percent = 100;
        }

        return percent;
    }

    /**
     * check the HotKnotBeam service is alive or not.
     *
     * @return indicate the service is keep alive or not.
     */
    public boolean isDead() {
        boolean isDead = false;

        if (mLastCurrentBytes < mCurrentBytes) {
            mLastCurrentBytes = mCurrentBytes;
            mLastTimeStamp = SystemClock.elapsedRealtime();
        } else {
            long currentTimeStamp = SystemClock.elapsedRealtime();

            if ((currentTimeStamp - mLastTimeStamp) >
                    HotKnotBeamConstants.MAX_NOTIFY_TIMEOUT_VALUE) {
                Log.e(TAG, "No update too long:"
                            + currentTimeStamp + ":" +  mLastTimeStamp);
                isDead = true;
            }
        }

        return isDead;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String filename) {
        mFileName = filename;
    }

}
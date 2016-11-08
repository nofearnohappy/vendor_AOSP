package com.mediatek.dataprotection;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class BaseActivity extends Activity {

    private static final String TAG = "BaseActivity";
    protected static final String TAG_CANCEL_DIALG = "cacel_dialog_main";
    public static final String EXPIRED = "ExPired";
    protected boolean mHasPriviledge = false;
    protected DataProtectionService mService = null;
    protected long mCancelTaskId;
    protected int mCancelResId;
    protected boolean mNeedRestoreCancelDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mHasPriviledge) {
            // if cancel dialog exists and no permission, need cancel dialog
            Log.d(TAG, "cancel dialog exists and no permission, need cancel dialog");
            mNeedRestoreCancelDialog = AlertDialogFragment.dismissCancelTaskDialog(
                    BaseActivity.this, mService, mCancelResId,
                    TAG_CANCEL_DIALG, mCancelTaskId);
        }
    }

    protected void showCancelTaskDialog() {
        if (mNeedRestoreCancelDialog) {
            Log.d(TAG, "onPatternVerifySuccess - show cancel dialog");
            AlertDialogFragment.showCancelTaskDialog(
                    BaseActivity.this, mService, mCancelResId,
                    TAG_CANCEL_DIALG, mCancelTaskId);
            mNeedRestoreCancelDialog = false;
        }
    }

    protected void setTaskIdAndResId(long taskId, int resId) {
        // record task id, res id
        Log.d(TAG, "onCancel - set task id: " + taskId + ", res id: " + resId);
        mCancelTaskId = taskId;
        mCancelResId = resId;
    }

    protected boolean isExpired() {
        Log.d(TAG, "T1: " + getIntent().getIntExtra(EXPIRED, 0) + " T2: " +
                android.os.Process.myPid());

        if (getIntent().getIntExtra(EXPIRED, 0) == android.os.Process.myPid()) {
            return false;
        } else {
            return true;
        }
    }
}

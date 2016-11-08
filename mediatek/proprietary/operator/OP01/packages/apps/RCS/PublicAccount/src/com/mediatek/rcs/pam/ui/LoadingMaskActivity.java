package com.mediatek.rcs.pam.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.mediatek.rcs.pam.R;

public abstract class LoadingMaskActivity extends Activity {
    private View mLoadingView;
    private ViewGroup mContentView;
    private boolean mIsLoading = true;

    private static final String TAG = "PAM/LoadingMaskActivity";

    protected abstract int getContentLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_with_loading_mask);
        mLoadingView = (View) findViewById(R.id.loading);
        mLoadingView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.d("PAM/LoadingMaskActivity", "loading clicked");
            }
        });
        ViewGroup parent = (ViewGroup) mLoadingView.getParent();
        mContentView = (ViewGroup) getLayoutInflater().inflate(
                getContentLayoutId(), null);
        parent.addView(mContentView, 0);
        switchToLoadingView();
    }

    protected void switchToLoadingView() {
        Log.d(TAG, "switchToLoadingView, current mIsLoading is " + mIsLoading);
        if (mIsLoading) {
            return;
        }
        setProgressBarIndeterminateVisibility(true);
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.bringToFront();
        mIsLoading = true;
    }

    protected void switchToNormalView() {
        Log.d(TAG, "switchToNormalView, current mIsLoading is " + mIsLoading);
        if (!mIsLoading) {
            return;
        }
        setProgressBarIndeterminateVisibility(false);
        mLoadingView.setVisibility(View.GONE);
        mIsLoading = false;
    }

    protected boolean isLoading() {
        return mIsLoading;
    }
}

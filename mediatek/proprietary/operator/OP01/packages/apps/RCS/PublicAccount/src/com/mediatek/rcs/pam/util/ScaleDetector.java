package com.mediatek.rcs.pam.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;

public class ScaleDetector {
    private static final String TAG = "PA/ScaleDetector";
    
    private static final float PRESSURE_THRESHOLD = 0.67f;
    
    private int mActiveId0;
    private int mActiveId1;
    
    private boolean mGestureInProgress;
    
    private boolean mInvalidGesture;
    
    private MotionEvent mPrevEvent;
    private MotionEvent mCurrEvent;
    private float mPrevFingerDiffX;
    private float mPrevFingerDiffY;
    private float mCurrFingerDiffX;
    private float mCurrFingerDiffY;
    private float mPrevLen;
    private float mCurrLen;    
    private float mScaleFactor;
    private float mCurrPressure;
    private float mPrevPressure;
    
    private OnScaleListener mListener;
    private Activity mActivity;
    
    public interface OnScaleListener {
        
        boolean onScaleStart(ScaleDetector detector);
        
        void onScaleEnd(ScaleDetector detector);
        
        boolean onScale(ScaleDetector detector);
    }
    
    public ScaleDetector(OnScaleListener listener) {
        this(null, listener);
    }
    
    public ScaleDetector(Activity activity, OnScaleListener listener) {
        mActivity = activity;
        mListener = listener;
        reset();
    }
    
    public void setActivity(Activity activity) {
        mActivity = activity;
    }
    
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = false;
        
        final int action = event.getActionMasked();
        
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        
        switch (action) {
        
        case MotionEvent.ACTION_DOWN:
            mActiveId0 = event.getPointerId(0);
            Log.d(TAG, "ACTION_DOWN: count = " + event.getPointerCount());
            break;
            
        case MotionEvent.ACTION_POINTER_DOWN:
            int count = event.getPointerCount();
            int index = event.getActionIndex();
            int id = event.getPointerId(index);
            Log.d(TAG, "ACTION_POINTER_DOWN: count = " + count + ", actionId = " + id);
            
            if (count == 2) {
                mActiveId0 = event.getPointerId(0);
                mActiveId1 = event.getPointerId(1);
                
                mPrevEvent = MotionEvent.obtain(event);
                setContext(event);
                
                if (mListener != null) {
                    mGestureInProgress = mListener.onScaleStart(this);
                    if (mGestureInProgress) {
                        MotionEvent cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                        if (mActivity != null) {
                            mActivity.getWindow().superDispatchTouchEvent(cancel);
                        }
                    }
                }
                mInvalidGesture = false;
            }
            
            if (count > 2 && !mInvalidGesture) {
                mInvalidGesture = true;
                setContext(event);
                if (mGestureInProgress && mListener != null) {
                    mListener.onScaleEnd(this);
                }
            }
            break;
            
        case MotionEvent.ACTION_MOVE:
            if (mGestureInProgress && !mInvalidGesture) {
                setContext(event);
                
                if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD) {
                    final boolean updatePrevious = mListener.onScale(this);
                    
                    if (updatePrevious) {
                        mPrevEvent.recycle();
                        mPrevEvent = MotionEvent.obtain(event);
                    }
                }
            }
            break;
            
        case MotionEvent.ACTION_POINTER_UP:
            int count2 = event.getPointerCount();
            int index2 = event.getActionIndex();
            int id2 = event.getPointerId(index2);
            Log.d(TAG, "ACTION_POINTER_UP, count = " + count2 + ", ActionId= " + id2);
            
            if (mGestureInProgress && count2 == 2 && !mInvalidGesture) {
                setContext(event);
                if (mListener != null) {
                    mListener.onScaleEnd(this);
                }
                mInvalidGesture = true;
            }
            break;            
            
        case MotionEvent.ACTION_UP:
            Log.d(TAG, "ACTION_UP");
            reset();
            break;
        case MotionEvent.ACTION_CANCEL:
            Log.d(TAG, "ACTION_CANCEL");
            reset();
            break;
        default: 
            break;
        }
        
        if (!mGestureInProgress) {
            Log.d(TAG, "return value is false, action = " + event.getActionMasked());
        }
        
        return mGestureInProgress;
    }
    
    private void reset() {
        if (mPrevEvent != null) {
            mPrevEvent.recycle();
            mPrevEvent = null;
        }
        if (mCurrEvent != null) {
            mCurrEvent.recycle();
            mCurrEvent = null;
        }
        
        mActiveId0 = -1;
        mActiveId1 = -1;
        mGestureInProgress = false;
        mInvalidGesture = false;
    }
    
    private void setContext(MotionEvent curr) {
        if (mCurrEvent != null) {
            mCurrEvent.recycle();
        }
        mCurrEvent = MotionEvent.obtain(curr);
        
        mCurrLen = -1;
        mPrevLen = -1;
        mScaleFactor= -1;
        
        final MotionEvent prev = mPrevEvent;
        
        final int prevIndex0 = prev.findPointerIndex(mActiveId0);
        final int prevIndex1 = prev.findPointerIndex(mActiveId1);
        final int currIndex0 = curr.findPointerIndex(mActiveId0);
        final int currIndex1 = curr.findPointerIndex(mActiveId1);
        
        if (prevIndex0 < 0 || prevIndex1 < 0 || currIndex0 < 0 || currIndex1 < 0) {
            mInvalidGesture = true;
            if (mGestureInProgress) {
                mListener.onScaleEnd(this);
            }
            return;
        }
        
        final float px0 = prev.getX(prevIndex0);
        final float py0 = prev.getY(prevIndex0);
        final float px1 = prev.getX(prevIndex1);
        final float py1 = prev.getY(prevIndex1);
        final float cx0 = curr.getX(currIndex0);
        final float cy0 = curr.getY(currIndex0);
        final float cx1 = curr.getX(currIndex1);
        final float cy1 = curr.getY(currIndex1);
        
        final float pvx = px1 - px0;
        final float pvy = py1 - py0;
        final float cvx = cx1 - cx0;
        final float cvy = cy1 - cy0;
        mPrevFingerDiffX = pvx;
        mPrevFingerDiffY = pvy;
        mCurrFingerDiffX = cvx;
        mCurrFingerDiffY = cvy;
        
        mCurrPressure = curr.getPressure(currIndex0) + curr.getPressure(currIndex1);
        mPrevPressure = prev.getPressure(prevIndex0) + prev.getPressure(prevIndex1);
    }
    
    public float getCurrentSpan() {
        if (mCurrLen == -1) {
            final float cvx = mCurrFingerDiffX;
            final float cvy = mCurrFingerDiffY;
            mCurrLen = FloatMath.sqrt(cvx * cvx + cvy * cvy);
        }
        return mCurrLen;
    }
    
    public float getCurrentSpanX() {
        return mCurrFingerDiffX;
    }
    
    public float getCurrentSpanY() {
        return mCurrFingerDiffY;
    }
    
    public float getPreviousSpan() {
        if (mPrevLen == -1) {
            final float pvx = mPrevFingerDiffX;
            final float pvy = mPrevFingerDiffY;
            mPrevLen = FloatMath.sqrt(pvx * pvx + pvy * pvy);
        }
        return mPrevLen;
    }
    
    public float getPreviousSpanX() {
        return mPrevFingerDiffX;
    }
    
    public float getPreviousSpanY() {
        return mPrevFingerDiffY;
    }
    
    public float getScaleFactor() {
        if (mScaleFactor == -1) {
            mScaleFactor = getCurrentSpan() / getPreviousSpan();
        }
        return mScaleFactor;
    }
    
    public abstract static class SimpleOnScaleListener implements OnScaleListener {
        private static final String TAG = "PA/SimpleOnScaleListener";
        private static final int DEFAULT_TEXT_SIZE = 18;
        private static final int MIN_TEXT_SIZE = 10;
        private static final int MAX_TEXT_SIZE = 32;
        private float mTextSize = DEFAULT_TEXT_SIZE;
        private static final float MIN_ADJUST_TEXT_SIZE = 0.2f;
        
        private Context mContext;
        
        public interface OnTextSizeChanged {
            void onChanged(float size);
        }
        
        protected abstract void performChangeText(float size);
        
        public SimpleOnScaleListener() {
            this(null, 0.0f);
        }
        
        public SimpleOnScaleListener(Context context, float initTextSize) {
            mContext = context;
            mTextSize = initTextSize;
        }
        
        public void setContext(Context context) {
            mContext = context;
        }
        
        public void setTextSize(float size) {
            mTextSize = size;
        }
        
        public void onScaleEnd(ScaleDetector detector) {
            Log.d(TAG, "onScaleEnd -> mTextSize = " + mTextSize);
            
            if (mTextSize < 10) {
                mTextSize = MIN_TEXT_SIZE;
            } else if (mTextSize > 32) {
                mTextSize = MAX_TEXT_SIZE;
            }
            
            try {
                Context context = mContext.createPackageContext("com.android.mms", 
                        Context.CONTEXT_IGNORE_SECURITY);
                SharedPreferences sp = context.getSharedPreferences("com.android.mms_preferences", 
                    Context.MODE_WORLD_WRITEABLE|Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = sp.edit();
                editor.putFloat("message_font_size", mTextSize);
                editor.commit();
                Log.d(TAG, "set SharedPreferences:" + mTextSize);
            } catch (NameNotFoundException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        
        public boolean onScale(ScaleDetector detector) {
            float size = mTextSize * detector.getScaleFactor();
            
            if (Math.abs(size - mTextSize) < MIN_ADJUST_TEXT_SIZE) {
                return false;
            }
            if (size < MIN_TEXT_SIZE) {
                size = MIN_TEXT_SIZE;
            }
            if (size > MAX_TEXT_SIZE) {
                size = MAX_TEXT_SIZE;
            }
            if ((size - mTextSize > 0.0000001) || (size - mTextSize < -0.0000001)) {
                mTextSize = size;
                performChangeText(size);
            }
            return true;
        }
        
        public boolean onScaleStart(ScaleDetector detector) {
            Log.d(TAG, "onScaleStart -> mTextSize = " + mTextSize);
            return true;
        }
    }
}

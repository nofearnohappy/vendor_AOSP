package com.mediatek.miravision.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import com.mediatek.miravision.setting.MiraVisionJni;

public class Image extends ImageView {
    private static final String TAG = "Miravision/ImagePreference";

    private Handler mHandler;
    private boolean mHostFragmentResumed;
    private KeyguardManager mKeyguardManager;
    private boolean mIsFirstDraw = true;
    private Context mContext;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "runnable mHostFragmentResumed: " + mHostFragmentResumed);
            if (mHostFragmentResumed && mKeyguardManager != null
                    && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
                setPQColorRegion();
            }
        }
    };

    public Image(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void init(Handler handler, KeyguardManager keyguardManager) {
        mHandler = handler;
        mKeyguardManager = keyguardManager;
    }

    public void setHostFragmentResumed(boolean isResumed) {
        mHostFragmentResumed = isResumed;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate()");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "onMeasure()");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "onLayout() left:" + left+" top:"+top+" right:"+right+" bottom:"+bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw()");
        if (mHandler != null && mHostFragmentResumed && mKeyguardManager != null
                && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            if (mIsFirstDraw) {
                Log.d(TAG, "isFirstDraw true");
                setPQColorRegion();
                mIsFirstDraw = false;
            } else {
                Log.d(TAG, "isFirstDraw false");
                mHandler.postDelayed(mRunnable, 300);
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        Log.d(TAG, "onFocusChanged()");
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        Log.d(TAG, "onScreenStateChanged()");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void setPQColorRegion() {
        Log.d(TAG, "setPQColorRegion()");
        int[] location = { -1, -1 };
        getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        if ("tablet".equals(SystemProperties.get("ro.build.characteristics"))) {
            
            String rotation = SystemProperties.get("ro.sf.hwrotation");
            
            android.view.WindowManager manager = (WindowManager) mContext
                    .getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            float density = metrics.density;
            int statusBarHeight = mContext.getResources().getDimensionPixelSize(
                    com.android.internal.R.dimen.navigation_bar_height);
            
            Log.e(TAG, "2tablet rotation:"+rotation +" getWidth() " + getWidth() + " getHeight:" + getHeight() + " x:" + x
                    + " y:" + y + " screenWidth:" + screenWidth + " screenHeight:" + screenHeight
                    + " getTop:" + getTop() + " getBottom" + getBottom() + " statusBarHeight:"
                    + statusBarHeight+" density:"+density);
            
            int startX=0;
            int startY=0;
            int endX=0;
            int endY=0;
            
            //check navigation_bar state
            boolean navigation_bar_state = (screenWidth>screenHeight ?screenHeight:screenWidth)/density <600;
            if(navigation_bar_state){
                if(rotation.equals("0")){
                    startX = statusBarHeight;
                    startY = x;
                    endX = getHeight()+statusBarHeight;
                    endY = x + getWidth();
                }else if(rotation.equals("180")){
                    
                    startX = y;
                    startY = 0;
                    endX = y+getHeight();
                    endY = getWidth() ;
                }else {
                    statusBarHeight = mContext.getResources().getDimensionPixelSize(
                            com.android.internal.R.dimen.navigation_bar_width);
                    startX = screenWidth - x - getWidth()+statusBarHeight;
                    startY = screenHeight - y  - getHeight();
                    endX = screenWidth - x+statusBarHeight;
                    endY = screenHeight - y ;
                }
            }else{
                if(rotation.equals("90")){
                    startX = screenWidth - x - getWidth();
                    startY = screenHeight - y + statusBarHeight - getHeight();
                    endX = screenWidth - x;
                    endY = screenHeight - y + statusBarHeight;
                }else if(rotation.equals("0")){
                    startX = statusBarHeight;
                    startY = x;
                    endX = getHeight()+statusBarHeight;
                    endY = x + getWidth();
                }
            }
            
            if (startX < 0) {
                startX = 0;
            }
            if (startY < 0) {
                startY = 0;
            }
            Log.e(TAG, "navigation_bar_state "+navigation_bar_state+" startX:" + startX + " startY:" + startY
                    + " endX " + endX + " endY:" + endY);
            MiraVisionJni.nativeSetPQColorRegion(1, startX, startY, endX, endY);
        } else {
            Log.e(TAG, "phone");
            // Because this api needs the portrait coordinates, but the fragment
            // is
            // in landscape,
            // so converted the location to portrait coordinates and passed it.

            MiraVisionJni.nativeSetPQColorRegion(1, 0, x, 0 + getHeight(), x + getWidth());
        }
    }
}
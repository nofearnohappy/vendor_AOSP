package com.mediatek.gallery3d.plugin;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.provider.Settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.util.Log;

import com.mediatek.gallery3d.ext.DefaultRewindAndForwardExtension;
import com.mediatek.gallery3d.video.IMoviePlayer;

import com.android.gallery3d.app.MoviePlayer;

import com.mediatek.op02.plugin.R;

public class RewindAndForward extends DefaultRewindAndForwardExtension implements View.OnClickListener {
    private static final String TAG = "RewindAndForward";
    private Context mPluginContext = null;

    private LinearLayout mContollerButtons;
    private ImageView mStop;
    private ImageView mForward;
    private ImageView mRewind;
    private int mButtonWidth;
    private int mButtonHeight;
    private int mControllerButtonPosition;
    private static final int BUTTON_PADDING = 40;
    private int mTimeBarHeight = 0;
    private IMoviePlayer mPlayer;
    private MoviePlayer mMoviePlayer;

    public RewindAndForward() {
        super();
    }

    public RewindAndForward(Context context) {
        super();

        mPluginContext = context;

        Log.v(TAG, "RewindAndForward init");
        //mTimeBarHeight = getPlayer().getTimeBarHight();
        Bitmap button = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_menu_forward);
        mButtonWidth = button.getWidth();
        mButtonHeight = button.getHeight();
        button.recycle();

        mContollerButtons = new LinearLayout(context);
        mContollerButtons.setHorizontalGravity(LinearLayout.HORIZONTAL);
        mContollerButtons.setVisibility(View.VISIBLE);
        mContollerButtons.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams buttonParam = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mRewind = new ImageView(context);
        mRewind.setImageResource(R.drawable.icn_media_rewind);
        mRewind.setScaleType(ScaleType.CENTER);
        mRewind.setFocusable(true);
        mRewind.setClickable(true);
        mRewind.setOnClickListener(this);
        mContollerButtons.addView(mRewind, buttonParam);

        mStop = new ImageView(context);
        mStop.setImageResource(R.drawable.icn_media_stop);
        mStop.setScaleType(ScaleType.CENTER);
        mStop.setFocusable(true);
        mStop.setClickable(true);
        mStop.setOnClickListener(this);
        LinearLayout.LayoutParams stopLayoutParam = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stopLayoutParam.setMargins(BUTTON_PADDING, 0, BUTTON_PADDING, 0);
        mContollerButtons.addView(mStop, stopLayoutParam);

        mForward = new ImageView(context);
        mForward.setImageResource(R.drawable.icn_media_forward);
        mForward.setScaleType(ScaleType.CENTER);
        mForward.setFocusable(true);
        mForward.setClickable(true);
        mForward.setOnClickListener(this);
        mContollerButtons.addView(mForward, buttonParam);
    }

    @Override
    public void onClick(View v) {
        Log.v(TAG, "onClick");
        if (mPlayer != null) {
            //avoid controller to be hidden
            mPlayer.showMovieController();
        }
        if (v == mStop) {
            Log.v(TAG, "RewindAndForward onClick mStop");
            stopVideo();
        } else if (v == mRewind) {
            Log.v(TAG, "RewindAndForward onClick mRewind");
            rewind();
        } else if (v == mForward) {
            Log.v(TAG, "RewindAndForward onClick mForward");
            forward();
        }
    }

    @Override
    public View getView() {
        Log.v(TAG, "getView() mContollerButtons = " + mContollerButtons);
        return mContollerButtons;
    }

    @Override
    public int getPaddingRight() {
        int ret = mButtonWidth * 3 + BUTTON_PADDING * 2;
        Log.v(TAG, "getPaddingRight() return = " + ret);
        return ret;
    }

    @Override
    public int getHeight() {
        Log.v(TAG, "getHeight() mButtonHeight = " + mButtonHeight);
        return mButtonHeight;
    }

    @Override
    public int getControllerButtonPosition() {
        Log.v(TAG, "getControllerButtonPosition() mControllerButtonPosition = " + mControllerButtonPosition);
        return mControllerButtonPosition;
    }


    @Override
    public void hide() {
       Log.v(TAG, "hide()");
       mContollerButtons.setVisibility(View.INVISIBLE);
    }

    @Override
    public void show() {
       Log.v(TAG, "show()");
       mContollerButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void startHideAnimation() {
       Log.v(TAG, "startHideAnimation()");
       startHideAnimation(mContollerButtons);
    }

    @Override
    public void cancelHideAnimation() {
       Log.v(TAG, "cancelHideAnimation()");
       mContollerButtons.setAnimation(null);
    }

    @Override
    public void setViewEnabled(boolean isEnabled) {
       Log.v(TAG, "setViewEnabled() is " + isEnabled);
       mRewind.setEnabled(isEnabled);
       mForward.setEnabled(isEnabled);
    }

    @Override
    public void onLayout(int l, int r, int b, int pr) {
        int cl = (r - l - getPaddingRight()) / 2;
        int cr = cl + getPaddingRight();
        mControllerButtonPosition = cr + pr;
        mContollerButtons.layout(cl + pr, b - mButtonHeight, cr + pr, b);
        int nl = cl + pr;
        int nr = b - mButtonHeight;
        int npr = b;
        int nb =  cr + pr;
        Log.v(TAG, "onLayout() nl = " + nl + " nr = " + nr + " npr = " + npr + " nb = " + nb);
    }

    @Override
    public void updateView() {
        Log.v(TAG, "updateView() getPlayer() = " + getPlayer());
        showControllerButtonsView(getPlayer().canStop(),
            getPlayer().canSeekBackward() &&
                getPlayer().getCurrentPosition() > 0 &&
                getPlayer().isTimeBarEnabled(),
            getPlayer().canSeekForward() &&
                (getPlayer().getCurrentPosition() < getPlayer().getDuration()) && getPlayer().isTimeBarEnabled());
    }

    private void stopVideo() {
        if (getPlayer().canStop()) {
            getPlayer().stopVideo();
            showControllerButtonsView(false, false, false);
        }
    }

    private void rewind() {
        Log.v(TAG, "rewind()");
        if (getPlayer().canSeekBackward()) {
            showControllerButtonsView(
                            getPlayer().canStop(),
                            false,
                            getPlayer().canSeekForward()
                                    && (getPlayer().getCurrentPosition() < getPlayer().getDuration())
                                    && getPlayer().isTimeBarEnabled());
            int stepValue = getStepOptionValue();
            int targetDuration = getPlayer().getCurrentPosition()
                    - stepValue < 0 ? 0 : getPlayer().getCurrentPosition()
                    - stepValue;
            Log.v(TAG, "rewind targetDuration " + targetDuration);
            getPlayer().seekTo(targetDuration);
        } else {
            showControllerButtonsView(
                            getPlayer().canStop(),
                            false,
                            getPlayer().canSeekForward()
                                    && (getPlayer().getCurrentPosition() < getPlayer()
                                            .getDuration())
                                    && getPlayer().isTimeBarEnabled());
        }
    }

    private void forward() {
        Log.v(TAG, "forward()");
        if (getPlayer().canSeekForward()) {
            showControllerButtonsView(getPlayer().canStop(),
                            getPlayer().canSeekBackward()
                              && getPlayer().getCurrentPosition() > 0
                              && getPlayer().isTimeBarEnabled(),
                            false);
            int stepValue = getStepOptionValue();
            int targetDuration = getPlayer().getCurrentPosition()
                    + stepValue > getPlayer().getDuration() ? getPlayer()
                    .getDuration() : getPlayer().getCurrentPosition()
                    + stepValue;
            Log.v(TAG, "forward targetDuration " + targetDuration);
            getPlayer().seekTo(targetDuration);
        } else {
            showControllerButtonsView(
                    getPlayer().canStop(), getPlayer().canSeekBackward()
                            && getPlayer().getCurrentPosition() > 0
                            && getPlayer().isTimeBarEnabled(), false);
        }
    }

    private void showControllerButtonsView(boolean canStop, boolean canRewind, boolean canForward) {
        Log.v(TAG, "showControllerButtonsView " + canStop + canRewind + canForward);
        // show ui
        mStop.setEnabled(canStop);
        mRewind.setEnabled(canRewind);
        mForward.setEnabled(canForward);
    }

    private int getStepOptionValue() {
        final int stepBase = 3000;
        int step = Settings.System.getInt(getContext().getContentResolver(), "selected_step_option", 0) + 1;

        Log.v(TAG, "getStepOptionValue step = " + step);

        return step * stepBase;
    }

    private boolean getTimeBarEanbled() {
        return getPlayer().isTimeBarEnabled();
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(getPlayer().getHideAnimation());
        }
    }

    @Override
    public void setParameter(String key, Object value) {
        super.setParameter(key, value);
        if (value instanceof IMoviePlayer) {
            mPlayer = (IMoviePlayer) value;
        } else if (value instanceof MoviePlayer) {
            mMoviePlayer = (MoviePlayer) value;
        }
    }

    private IMoviePlayer getPlayer() {
        return mPlayer;
    }
}

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.floatmenu;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;


public class KeyButtonView extends ImageView {
    private static final String TAG = "KeyButtonView";
    private static final boolean DEBUG = false;

    final float mGlowMaxScaleFactor = 1.8f;
    public static final float DEFAULT_QUIESCENT_ALPHA = 0.70f;

    long mDownTime;
    int mCode;
    int mTouchSlop;
    Drawable mGlowBG;
    int mGlowWidth;
    int mGlowHeight;
    float mGlowAlpha = 0f;
    float mGlowScale = 1f;
    @ViewDebug.ExportedProperty(category = "drawing")
    float mDrawingAlpha = 1f;
    @ViewDebug.ExportedProperty(category = "drawing")
    float mQuiescentAlpha = DEFAULT_QUIESCENT_ALPHA;
    boolean mSupportsLongpress = true;
    RectF mRect = new RectF();
    AnimatorSet mPressedAnim;
    Animator mAnimateToQuiescent = new ObjectAnimator();

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                } else {
                    // Just an old-fashioned ImageView
                    performLongClick();
                }
            }
        }
    };

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mGlowBG = context.getResources().getDrawable(
                R.drawable.floating_menu_bg_pressed);
        setDrawingAlpha(mQuiescentAlpha);
        if (mGlowBG != null) {
            mGlowWidth = mGlowBG.getIntrinsicWidth();
            mGlowHeight = mGlowBG.getIntrinsicHeight();
        }
        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setKeyCode(int code) {
        mCode = code;
        mSupportsLongpress = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mGlowBG != null) {
            canvas.save();
            final int w = getWidth();
            final int h = getHeight();
            final float aspect = (float) mGlowWidth / mGlowHeight;
            final int drawW = (int) (h * aspect);
            final int drawH = h;
            final int margin = (drawW - w) / 2;
            canvas.scale(mGlowScale, mGlowScale, w * 0.5f, h * 0.5f);
            mGlowBG.setBounds(-margin, 0, drawW - margin, drawH);
            mGlowBG.setAlpha((int) (mDrawingAlpha * mGlowAlpha * 255));
            mGlowBG.draw(canvas);
            canvas.restore();
            mRect.right = w;
            mRect.bottom = h;
        }
        super.onDraw(canvas);
    }

    public void setQuiescentAlpha(float alpha, boolean animate) {
        mAnimateToQuiescent.cancel();
        alpha = Math.min(Math.max(alpha, 0), 1);
        if (alpha == mQuiescentAlpha && alpha == mDrawingAlpha) {
            return;
        }
        mQuiescentAlpha = alpha;
        if (DEBUG) {
            Log.d("@M_" + TAG, "New quiescent alpha = " + mQuiescentAlpha);
        }
        if (mGlowBG != null && animate) {
            mAnimateToQuiescent = animateToQuiescent();
            mAnimateToQuiescent.start();
        } else {
            setDrawingAlpha(mQuiescentAlpha);
        }
    }

    private ObjectAnimator animateToQuiescent() {
        return ObjectAnimator.ofFloat(this, "drawingAlpha", mQuiescentAlpha);
    }

    public float getQuiescentAlpha() {
        return mQuiescentAlpha;
    }

    public float getDrawingAlpha() {
        return mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        // Calling setAlpha(int), which is an ImageView-specific
        // method that's different from setAlpha(float). This sets
        // the alpha on this ImageView's drawable directly
        setImageAlpha((int) (x * 255));
        mDrawingAlpha = x;
    }

    public float getGlowAlpha() {
        if (mGlowBG == null) {
            return 0;
        }
        return mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        if (mGlowBG == null) {
            return;
        }
        mGlowAlpha = x;
        invalidate();
    }

    public float getGlowScale() {
        if (mGlowBG == null) {
            return 0;
        }
        return mGlowScale;
    }

    public void setGlowScale(float x) {
        if (mGlowBG == null) {
            return;
        }
        mGlowScale = x;
        final float w = getWidth();
        final float h = getHeight();
        if (mGlowMaxScaleFactor <= 1.0f) {
            // this only works if we know the glow will never leave our bounds
            invalidate();
        } else {
            final float rx = (w * (mGlowMaxScaleFactor - 1.0f)) / 2.0f + 1.0f;
            final float ry = (h * (mGlowMaxScaleFactor - 1.0f)) / 2.0f + 1.0f;
            // also invalidate our immediate parent to help avoid situations
            // where nearby glows
            // interfere
            ((View) getParent()).invalidate();
        }
    }

    public void setPressed(boolean pressed) {
        if (mGlowBG != null) {
            if (pressed != isPressed()) {
                if (mPressedAnim != null && mPressedAnim.isRunning()) {
                    mPressedAnim.cancel();
                }
                mPressedAnim = new AnimatorSet();
                final AnimatorSet as = mPressedAnim;
                if (pressed) {
                    if (mGlowScale < mGlowMaxScaleFactor) {
                        mGlowScale = mGlowMaxScaleFactor;
                    }
                    if (mGlowAlpha < mQuiescentAlpha) {
                        mGlowAlpha = mQuiescentAlpha;
                    }
                    setDrawingAlpha(1f);
                    as.playTogether(ObjectAnimator.ofFloat(this, "glowAlpha",
                            1f), ObjectAnimator.ofFloat(this, "glowScale",
                            mGlowMaxScaleFactor));
                    as.setDuration(50);
                } else {
                    mAnimateToQuiescent.cancel();
                    mAnimateToQuiescent = animateToQuiescent();
                    as.playTogether(ObjectAnimator.ofFloat(this, "glowAlpha",
                            0f), ObjectAnimator.ofFloat(this, "glowScale", 1f),
                            mAnimateToQuiescent);
                    as.setDuration(500);
                }
                as.start();
            }
        }
        super.setPressed(pressed);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x;
        int y;
        if (mCallback != null) {
            mCallback.onTouched(action);
        }
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mDownTime = SystemClock.uptimeMillis();
            setPressed(true);
            if (mCode != 0) {
                sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
            } else {
                // Provide the same haptic feedback that the system offers for
                // virtual keys.
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            if (mSupportsLongpress) {
                removeCallbacks(mCheckLongPress);
                postDelayed(mCheckLongPress, ViewConfiguration
                        .getLongPressTimeout());
            }
            break;
        case MotionEvent.ACTION_MOVE:
            x = (int) ev.getX();
            y = (int) ev.getY();
            setPressed(x >= -mTouchSlop && x < getWidth() + mTouchSlop
                    && y >= -mTouchSlop && y < getHeight() + mTouchSlop);
            break;
        case MotionEvent.ACTION_CANCEL:
            setPressed(false);
            if (mCode != 0) {
                sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
            }
            if (mSupportsLongpress) {
                removeCallbacks(mCheckLongPress);
            }
            break;
        case MotionEvent.ACTION_UP:
            final boolean doIt = isPressed();
            setPressed(false);
            if (mCode != 0) {
                if (doIt) {
                    sendEvent(KeyEvent.ACTION_UP, 0);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                    // [ALPS00439010] We should NOT play sound here because
                    // PhoneWindow will play sound when it shows menu
                    if (mCode != KeyEvent.KEYCODE_MENU) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                    }
                } else {
                    sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                }
            } else {
                // no key code, just a regular ImageView
                if (doIt) {
                    performClick();
                }
            }
            if (mSupportsLongpress) {
                removeCallbacks(mCheckLongPress);
            }
            break;
        default:
            break;
        }

        return true;
    }

    void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int action, int flags, long when) {
        final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
        final KeyEvent ev = new KeyEvent(mDownTime, when, action, mCode,
                repeatCount, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags
                        | KeyEvent.FLAG_FROM_SYSTEM
                        | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        InputManager.getInstance().injectInputEvent(ev,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public void setTouchedCallback(TouchedCallback callback) {
        mCallback = callback;
    }

    private TouchedCallback mCallback;

    public interface TouchedCallback {
        void onTouched(int action);
    }
}

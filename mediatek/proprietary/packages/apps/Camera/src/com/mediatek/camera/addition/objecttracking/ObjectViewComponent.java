/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mediatek.camera.addition.objecttracking;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.camera.R;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;

public class ObjectViewComponent extends View {
    private static final String TAG = "ObjectViewComponent";

    private static final int ANIMATION_IDLE = 0;
    private static final int ANIMATION_DOING = 1;
    private static final int ANIMATION_DONE = 2;
    private static final int SCALING_UP_TIME = 300;
    private static final int SCALING_DOWN_TIME = 200;
    private static final int OBJECT_TRACKING_SUCCEED = 100;
    private static final int OBJECT_TRACKING_FAILED = 50;
    private static final int OBJECT_TRACKING_ICON_NUM = 3;
    private static final int OBJECT_FOCUSING = 0;
    private static final int OBJECT_FOCUSED = 1;
    private static final int OBJECT_FOCUSFAILED = 2;

    private int mZoomInAnimaState = ANIMATION_IDLE;
    private int mZoomOutAnimaState = ANIMATION_IDLE;
    private static final double MIN_RATE = 0.05;

    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mDisplayOrientation;
    // The orientation compensation for the object indicator to make it look
    // correctly in all device orientations. Ex: if the value is 90, the
    // indicator should be rotated 90 degrees counter-clockwise.
    private int mOrientation;
    private boolean mIsClear;
    private Face mFace;
    private Drawable mTrackIndicator;

    private Runnable mStartAction = new StartAction();
    private Runnable mEndAction = new EndAction();

    private Activity mActivity;

    private Matrix mMatrix = new Matrix();
    private RectF mRect = new RectF();
    private int mPreviewFrameWidth;
    private int mPreviewFrameHeight;
    private int mUnCropWidth;
    private int mUnCropHeight;
    private int mCompesation;

    private Drawable[] mTrackStatusIndicator = new Drawable[OBJECT_TRACKING_ICON_NUM];
    private static final int[] OBJECT_TRACKING_ICON = new int[] { R.drawable.ic_object_tracking,
            R.drawable.ic_object_tracking_succeed, R.drawable.ic_object_tracking_failed };

    public ObjectViewComponent(Context context, AttributeSet attr) {
        super(context, attr);
        Log.i(TAG, "[ObjectViewComponent]constructor...:");
        mActivity = (Activity) context;
        mTrackIndicator = mTrackStatusIndicator[OBJECT_FOCUSING];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "[onDraw]mZoomInAnimaState:" + mZoomInAnimaState + ", mZoomOutAnimaState:"
                + mZoomOutAnimaState + ", mOrientation:" + mOrientation);
        if (mZoomOutAnimaState != ANIMATION_DONE
                || mZoomInAnimaState != ANIMATION_DONE) {
            return;
        }
        if (mFace != null) {
            if (mFace.score == OBJECT_TRACKING_SUCCEED) {
                mTrackIndicator = mTrackStatusIndicator[OBJECT_FOCUSED];
            } else {
                Log.i(TAG, "[onDraw]mFace is null.");
                return;
            }
            // Prepare the matrix.
            Util.prepareMatrix(mMatrix, false, mDisplayOrientation,
                    mUnCropWidth, mUnCropHeight);
            // OT indicator is directional. Rotate the matrix and the canvas
            // so it looks correctly in all orientations.
            float dx = (getWidth() - mUnCropWidth) / 2;
            float dy = (getHeight() - mUnCropHeight) / 2;
            Matrix pointMatrix = new Matrix();
            float[] pointes = new float[] { dx, dy };
            canvas.save();
            mMatrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            pointMatrix.postRotate(mCompesation);
            pointMatrix.mapPoints(pointes);
            // Transform the coordinates.
            mRect.set(mFace.rect);
            mMatrix.mapRect(mRect);
            mRect.offset(pointes[0], pointes[1]);
            mTrackIndicator.setBounds(Math.round(mRect.left),
                    Math.round(mRect.top), Math.round(mRect.right),
                    Math.round(mRect.bottom));
            if (needDraw()) {
              mTrackIndicator.draw(canvas);
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    public void setObject(Face face) {
        Log.d(TAG, "[setObject]mZoomInAnimaState:" + mZoomInAnimaState + ""
                + ", mZoomOutAnimaState:" + mZoomOutAnimaState);

        if (mZoomInAnimaState == ANIMATION_DOING || mIsClear) {
            return;
        }
        mFace = face;

        if (mZoomOutAnimaState == ANIMATION_IDLE) {
            if (face.score == OBJECT_TRACKING_SUCCEED) {
                showSuccess(true);
            } else if (face.score == OBJECT_TRACKING_FAILED) {
                showFail(true);
            }
        } else if (mZoomOutAnimaState == ANIMATION_DONE) {
            Log.i(TAG, "[setObject]invalidate ");
            resetView();
            invalidate();
        }
    }

    public void setDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
        Log.d(TAG, "mDisplayOrientation=" + orientation);
    }

    public void setPreviewWidthAndHeight(int width, int height) {
        mPreviewFrameWidth = width;
        mPreviewFrameHeight = height;
    }

    public void setUnCropWidthAndHeight(int width, int height) {
        mUnCropWidth = width;
        mUnCropHeight = height;
        resetView();
    }

    public void setOrientationCompesation(int compesation) {
       mCompesation = compesation;
    }

    public void showStart() {
        Log.d(TAG, "[showStart]...");
        mIsClear = false;
        mZoomInAnimaState = ANIMATION_DOING;
        mZoomOutAnimaState = ANIMATION_IDLE;
        mTrackIndicator = mTrackStatusIndicator[OBJECT_FOCUSING];
        setBackground(mTrackIndicator);
        animate().withLayer().setDuration(SCALING_UP_TIME).scaleX(1.5f).scaleY(1.5f)
                .withEndAction(mStartAction);
    }

    public void showSuccess(boolean timeout) {
        Log.d(TAG, "[showSuccess]timeout= " + timeout);
        mIsClear = false;
        mZoomOutAnimaState = ANIMATION_DOING;
        mTrackIndicator = mTrackStatusIndicator[OBJECT_FOCUSED];
        setBackground(mTrackIndicator);
        animate().withLayer().setDuration(SCALING_DOWN_TIME).scaleX(0.8f).scaleY(0.8f)
                .withEndAction(mEndAction);
    }

    public void showFail(boolean timeout) {
        Log.d(TAG, "[showFail]timeout= " + timeout);
        mIsClear = false;
        mZoomOutAnimaState = ANIMATION_DOING;
        mTrackIndicator = mTrackStatusIndicator[OBJECT_FOCUSED];
        setBackground(mTrackIndicator);
        animate().withLayer().setDuration(SCALING_DOWN_TIME).scaleX(0.8f).scaleY(0.8f)
                .withEndAction(mEndAction);
    }

    public void resetVariable() {
        mIsClear = false;
    }
    public void clear() {
        Log.i(TAG, "[clear]...");
        mFace = null;
        mIsClear = true;
        resetView();
        invalidate();
    }

    public Drawable[] setViewDrawable() {
        for (int i = 0; i < OBJECT_TRACKING_ICON_NUM; i++) {
            mTrackStatusIndicator[i] = mActivity.getResources()
                    .getDrawable(OBJECT_TRACKING_ICON[i]);
        }
        return mTrackStatusIndicator;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        invalidate();
    }

    public void initView() {
        setViewDrawable();
    }

    private class StartAction implements Runnable {
        @Override
        public void run() {
            mZoomInAnimaState = ANIMATION_DONE;
        }
    }

    private class EndAction implements Runnable {
        @Override
        public void run() {
            mZoomOutAnimaState = ANIMATION_DONE;
            resetView();
        }
    }

    private void resetView() {
        Log.i(TAG, "resetView mUnCropWidth = " + mUnCropWidth
                + " mUnCropHeight = " + mUnCropHeight);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
        setBackground(null);
        animate().cancel();
        setScaleX(1f);
        setScaleY(1f);
        params.width = mUnCropWidth;
        params.height = mUnCropHeight;
        params.setMargins(0, 0, 0, 0);
    }

    private boolean needDraw() {
        boolean need  = false;
        float hight = Math.abs(mRect.top - mRect.bottom);
        float width = Math.abs(mRect.right - mRect.left);
        need  =  Math.abs(hight - width) / hight <= MIN_RATE ? true : false;
        Log.i(TAG, "needDraw need = " + need);
        return need;
    }
}

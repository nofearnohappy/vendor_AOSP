/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.addition.effect;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.R;

import com.mediatek.camera.addition.effect.EffectLayout.OnScrollListener;
import com.mediatek.camera.platform.ICameraAppUi;
import com.mediatek.camera.platform.IModuleCtrl;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.ui.CameraView;
import com.mediatek.camera.ui.RotateImageView;
import com.mediatek.camera.ui.UIRotateLayout;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;

public class EffectView extends CameraView implements EffectLayout.OnItemClickListener {

    private static final String TAG = "EffectView";

    public static final int SHOW_EFFECT = 0;
    public static final int HIDE_EFFECT = 1;
    public static final int ON_SIZE_CHANGED = 2;
    public static final int ON_EFFECT_DONE = 3;
    public static final int ON_CAMERA_CLOSE = 4;

    private static final int GRIDVIEW_STEP = 12;
    private static final int EFFECT_NUM_OF_PAGE = 12;
    private static final int MSG_DELAY_ROTATE = 1;
    private static final int MSG_DISPLAY = 2;
    private static final int MSG_HIDE_EFFECT = 3;
    private static final int DELAY_ROTATE = 50;
    private static final int MSG_REMOVE_GRID = 0;
    private static final int DELAY_MSG_REMOVE_GRID_MS = 3000;

    private static final String MTK_CONTROL_EFFECT_MODE_OFF = "none";
    private static final String MTK_CONTROL_EFFECT_MODE_MONO = "mono";
    private static final String MTK_CONTROL_EFFECT_MODE_NEGATIVE = "negative";
    private static final String MTK_CONTROL_EFFECT_MODE_SOLARIZE = "solarize";
    private static final String MTK_CONTROL_EFFECT_MODE_SEPIA = "sepia";
    private static final String MTK_CONTROL_EFFECT_MODE_POSTERIZE = "posterize";
    private static final String MTK_CONTROL_EFFECT_MODE_WHITEBOARD = "whiteboard";
    private static final String MTK_CONTROL_EFFECT_MODE_BLACKBOARD = "blackboard";
    private static final String MTK_CONTROL_EFFECT_MODE_AQUA = "aqua";
    private static final String MTK_CONTROL_EFFECT_MODE_SEPIAGREEN = "sepiagreen";
    private static final String MTK_CONTROL_EFFECT_MODE_SEPIABLUE = "sepiablue";

    private static final String MTK_CONTROL_EFFECT_MODE_NASHVILLE = "nashville"; // LOMO
    private static final String MTK_CONTROL_EFFECT_MODE_HEFE = "hefe";
    private static final String MTK_CONTROL_EFFECT_MODE_VALENCIA = "valencia";
    private static final String MTK_CONTROL_EFFECT_MODE_XPROII = "xproll";
    private static final String MTK_CONTROL_EFFECT_MODE_LOFI = "lofi";
    private static final String MTK_CONTROL_EFFECT_MODE_SIERRA = "sierra";
    private static final String MTK_CONTROL_EFFECT_MODE_KELVIN = "kelvin";
    private static final String MTK_CONTROL_EFFECT_MODE_WALDEN = "walden";
    private static final String MTK_CONTROL_EFFECT_MODE_F1977 = "f1977"; // LOMO
    private static final String MTK_CONTROL_EFFECT_MODE_NUM = "num";

    private int mNumsOfEffect = 0;
    private int mDisplayWidth = 0;
    private int mDisplayHeight = 0;
    private int mBufferWidth = 0;
    private int mBufferHeight = 0;
    private int mOrientation = 0;
    private int mPadding = 0;
    private int mSelectedPosition = 0;
    private float mDensity = 0;

    private boolean mShowEffects = false;
    private boolean mSizeChanged = false;
    private boolean mNeedScrollToFirstPosition = false;
    private boolean mNeedStartFaceDetection = false;
    private boolean mEffectsDone = false;
    private boolean mMirror = false;

    private String mCurrrentFocusMode = null;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private EffectLayout mGridView;
    private RotateImageView mIndicator;
    private Listener mListener;
    private ViewGroup mEffectsLayout;
    private MyAdapter mAdapter;

    private ListPreference mEffectPreference;
    private CharSequence[] mEffectEntryValues;
    private CharSequence[] mEffectEntries;
    private Surface[] mSurfaceList = new Surface[EFFECT_NUM_OF_PAGE];

    private ICameraAppUi mICameraAppUi;
    private IModuleCtrl mIModuleCtrl;

     private static final String[] mEffectName = {
        MTK_CONTROL_EFFECT_MODE_OFF,
        MTK_CONTROL_EFFECT_MODE_MONO,
        MTK_CONTROL_EFFECT_MODE_NEGATIVE,
        MTK_CONTROL_EFFECT_MODE_SOLARIZE,
        MTK_CONTROL_EFFECT_MODE_SEPIA,
        MTK_CONTROL_EFFECT_MODE_POSTERIZE,
        MTK_CONTROL_EFFECT_MODE_WHITEBOARD,
        MTK_CONTROL_EFFECT_MODE_BLACKBOARD,
        MTK_CONTROL_EFFECT_MODE_AQUA,
        MTK_CONTROL_EFFECT_MODE_SEPIAGREEN,
        MTK_CONTROL_EFFECT_MODE_SEPIABLUE,
        MTK_CONTROL_EFFECT_MODE_NASHVILLE,
        MTK_CONTROL_EFFECT_MODE_HEFE,
        MTK_CONTROL_EFFECT_MODE_VALENCIA,
        MTK_CONTROL_EFFECT_MODE_XPROII,
        MTK_CONTROL_EFFECT_MODE_LOFI,
        MTK_CONTROL_EFFECT_MODE_SIERRA,
        MTK_CONTROL_EFFECT_MODE_KELVIN,
        MTK_CONTROL_EFFECT_MODE_WALDEN,
        MTK_CONTROL_EFFECT_MODE_F1977,
        MTK_CONTROL_EFFECT_MODE_NUM,
    };

    public interface Listener {
        public void onInitialize();

        public void onSurfaceAvailable(Surface surface, int width, int height, int effectIndex);

        public void onUpdateEffect(int pos, int effectIndex);

        public void onReceivePreviewFrame(boolean received);

        public void onRelease();

        public void onItemClick(String value);

        public void hideEffect(boolean anmiation, int animationTime);
    }

    public EffectView(Activity activity) {
        super(activity);
        Log.i(TAG, "[EffectView]constructor...");
    }

    @Override
    public void init(Activity activity, ICameraAppUi cameraAppUi, IModuleCtrl moduleCtrl) {
        Log.i(TAG, "[init]...");
        mIModuleCtrl = moduleCtrl;
        mICameraAppUi = cameraAppUi;
    }

    @Override
    protected View getView() {
        return null;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        Log.i(TAG, "onOrientationChanged( " + orientation + ")" + ", mOrientation:" + mOrientation);
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        rotateGridViewItem(orientation);
    }

    @Override
    public boolean update(int type, Object... args) {
        switch (type) {
        case SHOW_EFFECT:
            mEffectPreference = (ListPreference) args[0];
            mMirror = (Boolean) args[1];
            showEffect();
            break;

        case HIDE_EFFECT:
            boolean animation = (Boolean) args[0];
            int animationTime = (Integer) args[1];
            hideEffect(animation, animationTime);
            break;

        case ON_SIZE_CHANGED:
            int width = (Integer) args[0];
            int height = (Integer) args[1];
            onSizeChanged(width, height);
            break;

        case ON_EFFECT_DONE:
            onEffectsDone();
            break;

        case ON_CAMERA_CLOSE:
            if (mMainHandler != null) {
                mMainHandler.removeMessages(MSG_REMOVE_GRID);
                mMainHandler.sendEmptyMessage(MSG_REMOVE_GRID);
            }
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public void setListener(Object listener) {
        mListener = (Listener) listener;
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.i(TAG, "[onItemClick], position:" + position);
        if (!mEffectsDone) {
            return;
        }
        mEffectPreference.setValue(mEffectEntryValues[position].toString());
        if (mListener != null) {
            mListener.onItemClick(mEffectEntryValues[position].toString());
        }
    }

    public boolean onBackPressed() {
        Log.i(TAG, "[onBackPressed]");
        if (mShowEffects) {
            hideEffect(true, DELAY_MSG_REMOVE_GRID_MS);
            return true;
        } else {
            return false;
        }
    }

    public void showEffect() {
        Log.i(TAG, "[showEffect]..., start");
        mMainHandler.removeMessages(MSG_REMOVE_GRID);
        mShowEffects = true;

        // need to reload value for the case of switch camera
        mEffectPreference.reloadValue();
        mSelectedPosition = mEffectPreference.findIndexOfValue(mEffectPreference.getValue());
        if (mGridView != null && mNeedScrollToFirstPosition) {
            mNeedScrollToFirstPosition = false;
            mGridView.scrollToSelectedPosition(mSelectedPosition);
            mGridView.showSelectedBorder(mSelectedPosition);
        }
        if (mEffectsLayout == null) {
            initialEffect();
            if (mEffectsDone) {
                startFadeInAnimation(mEffectsLayout);
            }
        }
        mEffectsLayout.setVisibility(View.VISIBLE);

        Log.i(TAG, "[showEffect]..., end");
    }

    public void onSizeChanged(int width, int height) {
        Log.i(TAG, "input onSizeChanged(), inputSize, width:" + width + ", height:" + height
                + "displayOrientation:" + "" + mIModuleCtrl.getDisplayRotation()
                + ", mOrientation:" + mOrientation);
        mDisplayWidth = Math.max(width, height);
        mDisplayHeight = Math.min(width, height);
        int displayRotation = mIModuleCtrl.getDisplayRotation();
        // when launch camera from WallPaper, the width and height of display
        // size should be rotate
        // in portrait
        if (false/* getContext().isVideoWallPaperIntent() */) {
            if (width < height) {
                int temp = mDisplayWidth;
                mDisplayWidth = mDisplayHeight;
                mDisplayHeight = temp;
            }
        } else {
            if (mGridView != null) {
                // this method is running in GLThread, so post runnable to set
                // display size.
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Log.i(TAG, "setDisplaySize(" + mDisplayWidth + "," + mDisplayHeight + ")");
                        if (mGridView != null) {
                            mGridView.setDisplaySize(mDisplayWidth, mDisplayHeight);
                        }
                    }
                });
            }
        }
        Log.i(TAG, "onSizeChanged(), outputSize, mDisplayWidth:" + mDisplayWidth
                + ", mDisplayHeight:" + mDisplayHeight);

        mSizeChanged = true;
    }

    public void hideEffect(boolean animation, long delay) {
        Log.i(TAG, "hideEffect(), animation:" + animation + ", mEffectsLayout:" +
                "" + mEffectsLayout);
        if (mEffectsLayout != null) {
            mMainHandler.removeMessages(MSG_REMOVE_GRID);
            mShowEffects = false;
            if (animation) {
                startFadeOutAnimation(mEffectsLayout);
            }
            mICameraAppUi.restoreViewState();
            mEffectsLayout.setVisibility(View.GONE);
            // getContext().setSwipingEnabled(true);
            mMainHandler.sendEmptyMessageDelayed(MSG_REMOVE_GRID, delay);
            // show();
        }
    }

    public boolean isShowEffects() {
        return mShowEffects;
    }

    public void onEffectsDone() {
        Log.i(TAG, "onEffectsDone()");
        mMainHandler.sendEmptyMessage(MSG_DISPLAY);
        mEffectsDone = true;
    }

    protected void startFadeInAnimation(View view) {
        if (mFadeIn == null) {
            mFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.gird_effects_fade_in);
        }
        if (view != null && mFadeIn != null) {
            view.startAnimation(mFadeIn);
            mFadeIn = null;
        }
    }

    protected void startFadeOutAnimation(View view) {
        if (mFadeOut == null) {
            mFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.grid_effects_fade_out);
            mFadeOut.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // show();
                }
            });
        }
        if (view != null) {
            view.startAnimation(mFadeOut);
            mFadeOut = null;
        }
    }

    protected Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage(), msg:" + msg);
            switch (msg.what) {
            case MSG_REMOVE_GRID:
                // If we removeView and addView frequently, drawing cache may be
                // wrong.
                // Here avoid do this action frequently to workaround that
                // issue.
                if (mGridView != null) {
                    mGridView.removeAllViews();
                }
                if (mEffectsLayout != null && mEffectsLayout.getParent() != null) {
                    mEffectsLayout.removeAllViews();
                    ((ViewGroup) mEffectsLayout.getParent()).removeView(mEffectsLayout);
                }
                mGridView = null;
                mEffectsLayout = null;
                mEffectsDone = false;
                if (mListener != null) {
                    mListener.onRelease();
                }
                break;
            case MSG_DELAY_ROTATE:
                rotateGridViewItem(getOrientation());
                break;
            case MSG_HIDE_EFFECT:
                hideEffect(false, 0);
                break;
            case MSG_DISPLAY:
                if (mEffectsLayout != null) {
                    startFadeInAnimation(mEffectsLayout);
                    mEffectsLayout.setAlpha(1.0f);
                }
                break;
            default:
                break;
            }
        };
    };

    private void rotateGridViewItem(int orientation) {
        Log.i(TAG, "rotateGridViewItem(), orientation:" + orientation);
        // since portrait mode use ModePickerRotateLayout rotate 270, here need
        // to compensation,
        // compensation should be 270.
        int rotation = Util.computeRotation(getContext(), orientation, 270);
        if (mGridView != null) {
            int rows = mGridView.getChildCount();
            for (int i = 0; i < rows; i++) {
                ViewGroup rowView = (ViewGroup) mGridView.getChildAt(i);
                if (rowView != null) {
                    int cellCounts = rowView.getChildCount();
                    for (int j = 0; j < cellCounts; j++) {
                        View cellView = rowView.getChildAt(j);
                        if (cellView != null) {
                            UIRotateLayout layout = (UIRotateLayout) cellView
                                    .findViewById(R.id.rotate);
                            layoutByOrientation(layout, rotation);
                            Util.setOrientation(layout, rotation, true);
                        }
                    }
                }
            }
        }
    }

    private void layoutByOrientation(UIRotateLayout layout, int orientation) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) layout.getLayoutParams();
        switch (orientation) {
        case 0:
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            break;

        case 180:
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            break;

        case 90:
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            break;

        case 270:
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            break;

        default:
            break;
        }
        layout.setLayoutParams(lp);
        layout.requestLayout();
    }

    private void initialEffect() {
        Log.i(TAG, "[initialEffect]mEffectsLayout:" + mEffectsLayout + ", mSizeChanged:"
                + mSizeChanged + ", mMirror:" + mMirror);
        if (mEffectsLayout == null) {
            mEffectEntryValues = mEffectPreference.getEntryValues();
            mEffectEntries = mEffectPreference.getEntries();
            mNumsOfEffect = mEffectPreference.getEntryValues().length;
            Log.i(TAG, "nums of effect:" + mNumsOfEffect);
            if (mListener != null) {
                mListener.onInitialize();
            }

            mEffectsLayout = (ViewGroup) inflate(R.layout.lomo_effects);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.topMargin = 0;
            getContext().addContentView(mEffectsLayout, params);
            mGridView = (EffectLayout) mEffectsLayout.findViewById(R.id.lomo_effect_gridview);
            int columnWidth = mDisplayWidth % 3 == 0 ? mDisplayWidth / 3 : (mDisplayWidth / 3 + 1);
            mGridView.setColumnWidth(columnWidth);
            mGridView.setColumnHeight(mDisplayHeight / 3);
            mGridView.setColumnCount(3);
            mGridView.setDisplaySize(columnWidth * 3, mDisplayHeight);
            mAdapter = new MyAdapter((Context) getContext());
            mGridView.setAdapter(mAdapter);
            mGridView.setOnItemClickListener(this);
            mGridView.setSelector(R.drawable.lomo_effect_selector);
            mGridView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            mGridView.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollOut(EffectLayout view, int direction) {
                    Log.i(TAG, "onScrollOut()");
                    if (direction == DIRECTION_UP) {
                        mNeedScrollToFirstPosition = true;
                        if (mListener != null) {
                            mListener.hideEffect(false, 0);
                        }
                    }
                }

                @Override
                public void onScrollDone(EffectLayout view, int startPosition, int endPosition) {
                    Log.i(TAG, "onScrollDone(), startPosition:" + startPosition + ", endPosition:"
                            + endPosition);
                    for (int j = startPosition; j < endPosition; j++) {
                        int effectId = getEffectId(mEffectEntryValues[j].toString());
                        int position = j % 12;
                        if (mListener != null) {
                            mListener.onUpdateEffect(position, effectId);
                        }
                    }

                    if (startPosition == 0) {
                        for (int i = endPosition; i < endPosition + 3; i++) {
                            if (mListener != null) {
                                mListener.onUpdateEffect(i, -1);
                            }
                        }
                    } else {
                        for (int i = startPosition - 1; i >= startPosition - 3; i--) {
                            if (mListener != null) {
                                mListener.onUpdateEffect(i, -1);
                            }
                        }
                    }
                }
            });

            mGridView.scrollToSelectedPosition(mSelectedPosition);
            mGridView.showSelectedBorder(mSelectedPosition);
            mSizeChanged = false;
            mEffectsLayout.setAlpha(0.0f);
        } else {
            if (mSizeChanged) {
                mGridView.setDisplaySize(mDisplayWidth, mDisplayHeight);
                mSizeChanged = false;
            }

        }
    }

    private int getEffectId(String effectName) {
        for (int i = 0; i < mEffectName.length; i++) {
            if (Util.equals(effectName, mEffectName[i])) {
                Log.i(TAG, "effectName:" + effectName + ", effetId:" + i);
                return i;
            }
        }
        Log.i(TAG, "effectName:" + effectName + ", effetId: -1");
        return -1;
    }

    public class MyAdapter extends BaseAdapter {

        LayoutInflater mLayoutInflater;

        public MyAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mNumsOfEffect;
        }

        @Override
        public Object getItem(int position) {
            return mSurfaceList[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextureView mTextureView;
            TextView mTextView;
            int mPosition;
            UIRotateLayout mRotateLayout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i(TAG, "convertView:" + convertView + ", position:" + position);
            if (mEffectsLayout == null) {
                Log.i(TAG, "mEffectsLayout is null");
                return null;
            }

            ViewHolder holder = null;
            int effectId = getEffectId(mEffectEntryValues[position].toString());

            // when firstly go in lomo ui, the effect of position 9 to 11 won't
            // in sight,
            // so the position 9 to 11 won't need effect processing.
            if (position > 8 && position < 12 && convertView == null) {
                effectId = -1;
            }
            if (position >= GRIDVIEW_STEP) {
                int index = position - GRIDVIEW_STEP;
                mListener.onUpdateEffect(index, effectId);
            } else {
                mListener.onUpdateEffect(position, effectId);
            }

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.lomo_effects_item, null);
                holder = new ViewHolder();
                holder.mTextureView = (TextureView) convertView.findViewById(R.id.textureview);
                holder.mTextView = (TextView) convertView.findViewById(R.id.effects_name);
                holder.mRotateLayout = (UIRotateLayout) convertView.findViewById(R.id.rotate);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        mDisplayWidth / 3, mDisplayHeight / 3);
                holder.mTextureView.setLayoutParams(params);
                int padding = convertView.getPaddingLeft();
                if (mIModuleCtrl.getDisplayOrientation() == 270
                        || mIModuleCtrl.getDisplayOrientation() == 180) {
                    holder.mTextureView.setPivotX(mDisplayWidth / 6 - padding);
                    holder.mTextureView.setPivotY(mDisplayHeight / 6 - padding);
                    holder.mTextureView.setRotation(180);
                }

                if (mMirror) {
                    holder.mTextureView.setPivotX(mDisplayWidth / 6 - padding);
                    holder.mTextureView.setPivotY(mDisplayHeight / 6 - padding);
                    holder.mTextureView.setRotationY(180);
                }
                LomoSurfaceTextureListener listener = new LomoSurfaceTextureListener(position);
                holder.mTextureView.setSurfaceTextureListener(listener);
                holder.mPosition = position;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTextView.setText(mEffectEntries[position]);
            // some times the content of TextView do not update, so force to
            // call requestLayout()
            int rotation = Util.computeRotation(getContext(), mOrientation, 270);
            layoutByOrientation(holder.mRotateLayout, rotation);
            Util.setOrientation(holder.mRotateLayout, rotation, true);
            return convertView;
        }

    }

    public class LomoSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        private int mPosition;

        public LomoSurfaceTextureListener(int position) {
            mPosition = position;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfacetTextureAvailable(), surface:" + surface + ", width:" + width
                    + ", " + "height:" + height + ", mPosition:" + mPosition);
            mSurfaceList[mPosition] = new Surface(surface);
            if (mListener != null) {
                mListener.onSurfaceAvailable(mSurfaceList[mPosition], width, height, mPosition);
            }

        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureUpdated(), surface:" + surface + ", width:" + width
                    + ", height:" + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "onSurfaceTextureDestroyed(), surface:" + surface + "and mPosition:"
                    + mPosition);
            mSurfaceList[mPosition] = null;
            return true;
        }
    }
}

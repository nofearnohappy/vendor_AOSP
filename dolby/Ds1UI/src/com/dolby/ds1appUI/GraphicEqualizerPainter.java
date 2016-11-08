/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.content.Context;
import android.content.res.Resources;
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;

import com.dolby.ds1appCoreUI.Configuration;
import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tag;
import com.dolby.ds1appCoreUI.Tools;

public class GraphicEqualizerPainter {

    private static final boolean DISPLAY_DEBUG_BARS = false;

    private static final boolean DISPLAY_DEBUG_TEXT = false;

    private static final int MAX_ALPHA = 0xFF;

    private static final float GEQ_TRANS_TIME = 0.3f;

    private static final int SHOW_HIDE_ANIMATION_DURATION = 250;

    private static final int IDLE_HIDE_DELAY = 5000;

    private static int GAIN_SMOOTH_LENGTH = 2;
    private static final float[] GAIN_SMOOTHER_TABLET = new float[] { 0.25f, 0.50f, 0.25f };
    private static final float[] GAIN_SMOOTHER_MOBILE = new float[] { 0.10f, 0.25f, 0.30f, 0.25f, 0.10f };

    private static float[] GAIN_SMOOTHER;
    private static float[][] GAIN_SMOOTHER_INV;
    private static final float[][] GAIN_SMOOTHER_INV_TABLET = { 
        { 1.9500f, -1.8500f, 1.7500f, -1.6500f, 1.5500f, -1.4500f, 1.3500f, -1.2500f, 1.1500f, -1.0500f, 0.9500f, -0.8500f, 0.7500f, -0.6500f, 0.5500f, -0.4500f, 0.3500f, -0.2500f, 0.1500f, -0.0500f }, 
        { -1.8500f, 5.5500f, -5.2500f, 4.9500f, -4.6500f, 4.3500f, -4.0500f, 3.7500f, -3.4500f, 3.1500f, -2.8500f, 2.5500f, -2.2500f, 1.9500f, -1.6500f, 1.3500f, -1.0500f, 0.7500f, -0.4500f, 0.1500f }, 
        { 1.7500f, -5.2500f, 8.7500f, -8.2500f, 7.7500f, -7.2500f, 6.7500f, -6.2500f, 5.7500f, -5.2500f, 4.7500f, -4.2500f, 3.7500f, -3.2500f, 2.7500f, -2.2500f, 1.7500f, -1.2500f, 0.7500f, -0.2500f }, 
        { -1.6500f, 4.9500f, -8.2500f, 11.5500f, -10.8500f, 10.1500f, -9.4500f, 8.7500f, -8.0500f, 7.3500f, -6.6500f, 5.9500f, -5.2500f, 4.5500f, -3.8500f, 3.1500f, -2.4500f, 1.7500f, -1.0500f, 0.3500f }, 
        { 1.5500f, -4.6500f, 7.7500f, -10.8500f, 13.9500f, -13.0500f, 12.1500f, -11.2500f, 10.3500f, -9.4500f, 8.5500f, -7.6500f, 6.7500f, -5.8500f, 4.9500f, -4.0500f, 3.1500f, -2.2500f, 1.3500f, -0.4500f }, 
        { -1.4500f, 4.3500f, -7.2500f, 10.1500f, -13.0500f, 15.9500f, -14.8500f, 13.7500f, -12.6500f, 11.5500f, -10.4500f, 9.3500f, -8.2500f, 7.1500f, -6.0500f, 4.9500f, -3.8500f, 2.7500f, -1.6500f, 0.5500f }, 
        { 1.3500f, -4.0500f, 6.7500f, -9.4500f, 12.1500f, -14.8500f, 17.5500f, -16.2500f, 14.9500f, -13.6500f, 12.3500f, -11.0500f, 9.7500f, -8.4500f, 7.1500f, -5.8500f, 4.5500f, -3.2500f, 1.9500f, -0.6500f }, 
        { -1.2500f, 3.7500f, -6.2500f, 8.7500f, -11.2500f, 13.7500f, -16.2500f, 18.7500f, -17.2500f, 15.7500f, -14.2500f, 12.7500f, -11.2500f, 9.7500f, -8.2500f, 6.7500f, -5.2500f, 3.7500f, -2.2500f, 0.7500f }, 
        { 1.1500f, -3.4500f, 5.7500f, -8.0500f, 10.3500f, -12.6500f, 14.9500f, -17.2500f, 19.5500f, -17.8500f, 16.1500f, -14.4500f, 12.7500f, -11.0500f, 9.3500f, -7.6500f, 5.9500f, -4.2500f, 2.5500f, -0.8500f }, 
        { -1.0500f, 3.1500f, -5.2500f, 7.3500f, -9.4500f, 11.5500f, -13.6500f, 15.7500f, -17.8500f, 19.9500f, -18.0500f, 16.1500f, -14.2500f, 12.3500f, -10.4500f, 8.5500f, -6.6500f, 4.7500f, -2.8500f, 0.9500f }, 
        { 0.9500f, -2.8500f, 4.7500f, -6.6500f, 8.5500f, -10.4500f, 12.3500f, -14.2500f, 16.1500f, -18.0500f, 19.9500f, -17.8500f, 15.7500f, -13.6500f, 11.5500f, -9.4500f, 7.3500f, -5.2500f, 3.1500f, -1.0500f }, 
        { -0.8500f, 2.5500f, -4.2500f, 5.9500f, -7.6500f, 9.3500f, -11.0500f, 12.7500f, -14.4500f, 16.1500f, -17.8500f, 19.5500f, -17.2500f, 14.9500f, -12.6500f, 10.3500f, -8.0500f, 5.7500f, -3.4500f, 1.1500f }, 
        { 0.7500f, -2.2500f, 3.7500f, -5.2500f, 6.7500f, -8.2500f, 9.7500f, -11.2500f, 12.7500f, -14.2500f, 15.7500f, -17.2500f, 18.7500f, -16.2500f, 13.7500f, -11.2500f, 8.7500f, -6.2500f, 3.7500f, -1.2500f }, 
        { -0.6500f, 1.9500f, -3.2500f, 4.5500f, -5.8500f, 7.1500f, -8.4500f, 9.7500f, -11.0500f, 12.3500f, -13.6500f, 14.9500f, -16.2500f, 17.5500f, -14.8500f, 12.1500f, -9.4500f, 6.7500f, -4.0500f, 1.3500f }, 
        { 0.5500f, -1.6500f, 2.7500f, -3.8500f, 4.9500f, -6.0500f, 7.1500f, -8.2500f, 9.3500f, -10.4500f, 11.5500f, -12.6500f, 13.7500f, -14.8500f, 15.9500f, -13.0500f, 10.1500f, -7.2500f, 4.3500f, -1.4500f }, 
        { -0.4500f, 1.3500f, -2.2500f, 3.1500f, -4.0500f, 4.9500f, -5.8500f, 6.7500f, -7.6500f, 8.5500f, -9.4500f, 10.3500f, -11.2500f, 12.1500f, -13.0500f, 13.9500f, -10.8500f, 7.7500f, -4.6500f, 1.5500f }, 
        { 0.3500f, -1.0500f, 1.7500f, -2.4500f, 3.1500f, -3.8500f, 4.5500f, -5.2500f, 5.9500f, -6.6500f, 7.3500f, -8.0500f, 8.7500f, -9.4500f, 10.1500f, -10.8500f, 11.5500f, -8.2500f, 4.9500f, -1.6500f }, 
        { -0.2500f, 0.7500f, -1.2500f, 1.7500f, -2.2500f, 2.7500f, -3.2500f, 3.7500f, -4.2500f, 4.7500f, -5.2500f, 5.7500f, -6.2500f, 6.7500f, -7.2500f, 7.7500f, -8.2500f, 8.7500f, -5.2500f, 1.7500f }, 
        { 0.1500f, -0.4500f, 0.7500f, -1.0500f, 1.3500f, -1.6500f, 1.9500f, -2.2500f, 2.5500f, -2.8500f, 3.1500f, -3.4500f, 3.7500f, -4.0500f, 4.3500f, -4.6500f, 4.9500f, -5.2500f, 5.5500f, -1.8500f }, 
        { -0.0500f, 0.1500f, -0.2500f, 0.3500f, -0.4500f, 0.5500f, -0.6500f, 0.7500f, -0.8500f, 0.9500f, -1.0500f, 1.1500f, -1.2500f, 1.3500f, -1.4500f, 1.5500f, -1.6500f, 1.7500f, -1.8500f, 1.9500f } 
    };
    private static final float[][] GAIN_SMOOTHER_INV_MOBILE ={
        {5.974947f, -9.560246f, 4.623703f, 2.184113f, -1.405723f, -5.037043f, 6.72579f, -0.373153f, -5.246158f, 2.457422f, 3.802012f, -3.388747f, -3.831564f, 7.782701f, -3.292202f, -2.149938f, -0.373736f, 7.831958f, -9.791641f,   4.067505f},
        {-13.18045f, 26.889569f, -8.440565f, -16.616169f, 12.818645f, 12.013738f, -18.509292f, -5.198427f, 25.670954f, -14.322612f, -9.700973f, 8.24131f, 18.635219f, -32.736934f, 15.034376f, 5.795506f, 3.615226f, -31.273586f, 37.815148f,  -15.550682f},
        {4.113967f, -5.082324f, -8.952654f, 27.343688f, -22.909416f, 2.706435f, 2.555595f, 15.42156f, -30.077356f, 19.833288f, -0.460646f, 1.423582f, -21.682881f, 31.254782f, -16.186623f, -0.514169f, -6.608782f, 27.276239f, -30.892205f,  12.437920f},
        {8.344117f, -24.502038f, 31.520372f, -26.155108f, 23.737633f, -25.17765f, 25.598623f, -21.652584f, 16.542082f, -15.21636f, 16.947492f, -16.422271f, 11.712018f, -7.165604f, 6.88614f, -8.576313f, 6.984354f, -1.781692f, -1.944189f,   1.320977f},
        {-6.226015f, 18.838387f, -25.465256f, 22.713017f, -21.256726f, 29.827519f, -32.115904f, 21.236002f, -10.054364f, 11.890145f, -20.536371f, 19.570402f, -6.987888f, -1.7907f, -2.949217f, 10.644462f, -6.298866f, -7.02248f, 12.790858f,  -5.807003f},
        {-6.57178f, 12.226384f, -0.075773f, -20.060267f, 26.38381f, -17.815672f, 15.614193f, -21.487723f, 25.032099f, -19.33689f, 11.35104f, -11.459458f, 17.905648f, -19.426457f, 12.146804f, -5.3923f, 7.700833f, -14.015736f, 13.270786f,  -4.989541f},
        {10.133236f, -20.743704f, 6.736928f, 20.055701f, -28.624014f, 15.294314f, -9.239926f, 28.721205f, -43.695006f, 30.879401f, -8.676571f, 9.569534f, -31.39762f, 40.597473f, -22.548088f, 3.502315f, -11.207588f, 33.28477f, -35.796862f,  14.154500f},
        {1.60317f, -7.413822f, 15.52777f, -20.585885f, 21.812787f, -24.179915f, 30.94837f, -38.777261f, 42.735045f, -33.696837f, 22.031743f, -22.049196f, 30.534809f, -31.571955f, 20.416708f, -10.61373f, 13.679281f, -21.826827f, 19.646841f,  -7.221095f},
        {-11.75217f, 31.36132f, -33.375519f, 18.735259f, -13.362724f, 29.278508f, -46.570728f, 43.262846f, -28.278478f, 28.055968f, -36.890874f, 35.492629f, -19.920393f, 7.494313f, -10.815301f, 18.863667f, -13.528656f, -3.225425f, 12.305661f,  -6.129906f},
        {5.809602f, -16.528959f, 20.08894f, -15.15948f, 13.144674f, -21.076637f, 31.06733f, -32.140619f, 26.696477f, -26.911022f, 36.472343f, -35.048364f, 18.784959f, -6.087141f, 10.011543f, -18.681467f, 13.101933f, 4.347852f, -13.483305f,   6.591341f},
        {6.591341f, -13.483305f, 4.347852f, 13.101933f, -18.681467f, 10.011543f, -6.087141f, 18.784959f, -35.048364f, 36.472343f, -26.911022f, 26.696477f, -32.140619f, 31.06733f, -21.076637f, 13.144674f, -15.15948f, 20.08894f, -16.528959f,   5.809602f},
        {-6.129906f, 12.305661f, -3.225425f, -13.528656f, 18.863667f, -10.815301f, 7.494313f, -19.920393f, 35.492629f, -36.890874f, 28.055968f, -28.278478f, 43.262846f, -46.570728f, 29.278508f, -13.362724f, 18.735259f, -33.375519f, 31.36132f,  -11.752170f},
        {-7.221095f, 19.646841f, -21.826827f, 13.679281f, -10.61373f, 20.416708f, -31.571955f, 30.534809f, -22.049196f, 22.031743f, -33.696837f, 42.735045f, -38.777261f, 30.94837f, -24.179915f, 21.812787f, -20.585885f, 15.52777f, -7.413822f,   1.603170f},
        {14.1545f, -35.796862f, 33.28477f, -11.207588f, 3.502315f, -22.548088f, 40.597473f, -31.39762f, 9.569534f, -8.676571f, 30.879401f, -43.695006f, 28.721205f, -9.239926f, 15.294314f, -28.624014f, 20.055701f, 6.736928f, -20.743704f,  10.133236f},
        {-4.989541f, 13.270786f, -14.015736f, 7.700833f, -5.3923f, 12.146804f, -19.426457f, 17.905648f, -11.459458f, 11.35104f, -19.33689f, 25.032099f, -21.487723f, 15.614193f, -17.815672f, 26.38381f, -20.060267f, -0.075773f, 12.226384f,  -6.571780f},
        {-5.807003f, 12.790858f, -7.02248f, -6.298866f, 10.644462f, -2.949217f, -1.7907f, -6.987888f, 19.570402f, -20.536371f, 11.890145f, -10.054364f, 21.236002f, -32.115904f, 29.827519f, -21.256726f, 22.713017f, -25.465256f, 18.838387f,  -6.226015f},
        {1.320977f, -1.944189f, -1.781692f, 6.984354f, -8.576313f, 6.88614f, -7.165604f, 11.712018f, -16.422271f, 16.947492f, -15.21636f, 16.542082f, -21.652584f, 25.598623f, -25.17765f, 23.737633f, -26.155108f, 31.520372f, -24.502038f,   8.344117f},
        {12.43792f, -30.892205f, 27.276239f, -6.608782f, -0.514169f, -16.186623f, 31.254782f, -21.682881f, 1.423582f, -0.460646f, 19.833288f, -30.077356f, 15.42156f, 2.555595f, 2.706435f, -22.909416f, 27.343688f, -8.952654f, -5.082324f,   4.113967f},
        {-15.550682f, 37.815148f, -31.273586f, 3.615226f, 5.795506f, 15.034376f, -32.736934f, 18.635219f, 8.24131f, -9.700973f, -14.322612f, 25.670954f, -5.198427f, -18.509292f, 12.013738f, 12.818645f, -16.616169f, -8.440565f, 26.889569f,  -13.180450f},
        {4.067505f, -9.791641f, 7.831958f, -0.373736f, -2.149938f, -3.292202f, 7.782701f, -3.831564f, -3.388747f, 3.802012f, 2.457422f, -5.246158f, -0.373153f, 6.72579f, -5.037043f, -1.405723f, 2.184113f, 4.623703f, -9.560246f,   5.974947f}
    };

    private Drawable mSliderThumb;

    private Drawable mSliderThumbBright1;

    private Drawable mSliderThumbBright2;

    private Drawable mSliderThumbBright3;

    private Drawable mSliderBg;

    /**
     * User gains for all profiles and iEQ presets. UNSMOOTHED VALUES!
     */
    private final float[] mUserGainsTemp = new float[Constants.BANDS + 2 * GAIN_SMOOTH_LENGTH];

    /**
     * Queue of touch events which needs to be handled on next visualizer update
     * callback.
     */
    private final EQTouchQueue mEventQueue = new EQTouchQueue(20);

    /**
     * Array of user gains being a result of smoothening.
     */
    private float[] mGainsSmooth;

    private final float[] mGainsSmoothOld = new float[Constants.BANDS];

    /**
     * Gains drawn in the UI.
     */
    private final float[] mGainsUi;

    /**
     * Index of currently modified node by user's finger.
     */
    private int mEditBand = -1;

    /**
     * Gain value of currently modified node.
     */
    private float mEditGain;

    private int mPrevEditBand = -1;

    private float mPrevEditGain;

    private boolean mNotifyListener = false;

    private IEqualizerChangeListener mListener;

    private IDsActivityCommonTemp mActivity;

    private DsClient mDSClient;

    private long mSmoothenTimestamp;

    private int mProfile = -1;

    private int mEqPreset = -1;

    private int mWidth;

    private int mHeight;

    private int mViewHeight;

    private boolean mEnabled = true;

    private boolean mVisible;

    private final Context mContext;

    private final GraphicVisualiser mVisualizer;

    private String mDefaultProfileNames[];

    private boolean mMobileLayout = false;
    private final float TABLET_LAYOUT_INITIAL_COLUMN = 0.0f;
    private final float TABLET_LAYOUT_INITIAL_STEP = 1.0f;
    private final float MOBILE_LAYOUT_INITIAL_COLUMN = 0.0f;
    private final float MOBILE_LAYOUT_INITIAL_STEP = 4.75f;

    private Paint mPaintCurve1;
    private Paint mPaintCurve2;
    private PathEffect mEffect1;
    private MaskFilter  mBlur;

    public GraphicEqualizerPainter(Context context,
            GraphicVisualiser visualizer, float[] gainsUi, float[] gainsUser) {
        this.mContext = context;
        this.mVisualizer = visualizer;
        this.mGainsUi = gainsUi;
        this.mGainsSmooth = gainsUser;
        init();
    }

    private void init() {
        final Resources res = mContext.getResources();
        mMobileLayout = res.getBoolean(R.bool.newLayout);
        if (mMobileLayout) {
           GAIN_SMOOTHER = GAIN_SMOOTHER_MOBILE;
           GAIN_SMOOTHER_INV = GAIN_SMOOTHER_INV_MOBILE;
        } else {
           GAIN_SMOOTHER = GAIN_SMOOTHER_TABLET;
           GAIN_SMOOTHER_INV = GAIN_SMOOTHER_INV_TABLET;
           GAIN_SMOOTH_LENGTH = 1;
        }
        mSliderThumb = res.getDrawable(R.drawable.eq_thumb);
        mSliderThumbBright1 = res.getDrawable(R.drawable.eq_thumb);
        mSliderThumbBright2 = res.getDrawable(R.drawable.eq_thumb);
        mSliderThumbBright3 = res.getDrawable(R.drawable.eq_thumb_touch_state);
        mSliderBg = res.getDrawable(R.drawable.eq_bar);
        mDefaultProfileNames = new String[6];
        mDefaultProfileNames[0] = res.getString(R.string.movie);
        mDefaultProfileNames[1] = res.getString(R.string.music);
        mDefaultProfileNames[2] = res.getString(R.string.game);
        mDefaultProfileNames[3] = res.getString(R.string.voice);
        mDefaultProfileNames[4] = res.getString(R.string.preset_1);
        mDefaultProfileNames[5] = res.getString(R.string.preset_2);
        float scale = 1;
        switch(res.getDisplayMetrics().densityDpi){
        case DisplayMetrics.DENSITY_LOW:
            scale = 0.75f;
            break;
        case DisplayMetrics.DENSITY_HIGH:
            scale = 1.5f;
            break;
        case DisplayMetrics.DENSITY_XHIGH:
            scale = 2.0f;
            break;
        }
        mBlur = new BlurMaskFilter(4*scale, BlurMaskFilter.Blur.NORMAL);
        mEffect1 = new CornerPathEffect(10*scale);
        mPaintCurve1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintCurve1.setStyle(Paint.Style.STROKE);
        mPaintCurve1.setStrokeWidth(3*scale);
        mPaintCurve1.setColor(0xD075d2ff);
        mPaintCurve1.setPathEffect(mEffect1);

        mPaintCurve2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintCurve2.setStyle(Paint.Style.STROKE);
        mPaintCurve2.setStrokeCap(Cap.ROUND);
        mPaintCurve2.setStrokeWidth(10*scale);
        mPaintCurve2.setColor(0x8075d2ff);
        mPaintCurve2.setMaskFilter(mBlur);
        mPaintCurve2.setPathEffect(mEffect1);


        if (mSliderBg instanceof BitmapDrawable) {
            ((BitmapDrawable) mSliderBg).setGravity(Gravity.FILL_VERTICAL | Gravity.CENTER_HORIZONTAL);
        }

        //DS1SOC-566, In LPA mode, no visualizer data and always show the equalizer control
        if (!DS1Application.VISUALIZER_ENABLE) {
        	mVisible = true;
        } else {
            mVisible = false;
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh, int viewH) {
        this.mWidth = w;
        this.mHeight = h;
        this.mViewHeight = viewH;

        if (mSliderBg != null) {
            mSliderBg.setBounds(0, 0, mSliderBg.getIntrinsicWidth(), h);
        }
        if (mSliderThumb != null) {
            mSliderThumb.setBounds(0, 0, mSliderThumb.getIntrinsicWidth(), mSliderThumb.getIntrinsicWidth());
        }
        if (mSliderThumbBright1 != null) {
            mSliderThumbBright1.setBounds(0, 0, mSliderThumbBright1.getIntrinsicWidth(), mSliderThumbBright1.getIntrinsicWidth());
        }
        if (mSliderThumbBright2 != null) {
            mSliderThumbBright2.setBounds(0, 0, mSliderThumbBright2.getIntrinsicWidth(), mSliderThumbBright2.getIntrinsicWidth());
        }
        if (mSliderThumbBright3 != null) {
            mSliderThumbBright3.setBounds(0, 0, mSliderThumbBright3.getIntrinsicWidth(), mSliderThumbBright3.getIntrinsicWidth());
        }
    }

    private void handleNewTouchEvents() {
        // Log.d(Tag.MAIN, "GraphicEqualizerPainter.handleNewTouchEvents");

        int b, bb, i;

        float engineGain;
        float newUserGain;
        float currentUiGain;

        final int size = mEventQueue.size();
        final boolean suspended = mVisualizer.mSuspended;
        for (i = 0; i < size; ++i) {
            b = mEventQueue.getBandAt(i);
            if (suspended) {
                newUserGain = mEventQueue.getGainAt(i);
            } else {
                currentUiGain = mGainsUi[b];
                engineGain = currentUiGain - mGainsSmooth[b];
                newUserGain = mEventQueue.getGainAt(i) - engineGain;
            }
            for (bb = b; bb <= b + 2 * GAIN_SMOOTH_LENGTH; ++bb) {
                mUserGainsTemp[bb] = newUserGain;
            }
        }

        mEventQueue.reset();
    }

    public boolean isModified() {
        for (int b = 0; b < mGainsSmooth.length; b++) {
            if (mGainsSmooth[b] != 0) {
                return true;
            }
        }
        return false;
    }

    private void smoothenCurve() {
        // Log.d(Tag.MAIN, "GraphicEqualizerPainter.smoothenCurve");

        final Configuration conf = MainActivity.getConfiguration();
        if (conf == null) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - mSmoothenTimestamp >= 1000) {
            mSmoothenTimestamp = now;
        }

        final long delta = now - mSmoothenTimestamp;
        final float fDelta = delta / 1000f;

        int b, bb, i;

        final float[] tempGains = mUserGainsTemp;

        final float significantDiff = 0.02f;

        // settle down unsmooth array
        boolean redraw = false;
        float fAlpha = (float) Math.pow(0.5f, fDelta / GEQ_TRANS_TIME);
        float diff;
        final float snap = 0.2f;
        for (b = 0; b < Constants.BANDS + 2 * GAIN_SMOOTH_LENGTH; b++) {

            diff = tempGains[b] - conf.getMaxEditGain();
            if (diff > 0) {
                if (diff >= significantDiff) {
                    if (diff < snap) {
                        tempGains[b] = conf.getMaxEditGain();
                    } else {
                        tempGains[b] = fAlpha * tempGains[b] + (1.0f - fAlpha) * conf.getMaxEditGain();
                    }
                } else {
                    tempGains[b] = conf.getMaxEditGain();
                }
            } else {
                diff = conf.getMinEditGain() - tempGains[b];
                if (diff > 0) {
                    if (diff >= significantDiff) {
                        if (diff < snap) {
                            tempGains[b] = conf.getMinEditGain();
                        } else {
                            tempGains[b] = fAlpha * tempGains[b] + (1.0f - fAlpha) * conf.getMinEditGain();
                        }
                    } else {
                        tempGains[b] = conf.getMinEditGain();
                    }
                }
            }
        }

        System.arraycopy(mGainsSmooth, 0, mGainsSmoothOld, 0, Constants.BANDS);

        // smoothing the curve
        float fGain;
        for (b = 0; b < Constants.BANDS; ++b) {
            fGain = 0;
            for (i = 0, bb = b; i <= 2 * GAIN_SMOOTH_LENGTH; i++, bb++) {
                fGain += GAIN_SMOOTHER[i] * tempGains[bb];
            }

            if (mGainsSmooth[b] != fGain) {
                final float absDiff = Math.abs(mGainsSmooth[b] - fGain);
                if (absDiff > significantDiff) {
                    mGainsSmooth[b] = fGain;
                    if (!redraw) {
                        // Log.d(Tag.MAIN, "C redraw = true abs diff: "
                        // + mDecFormat.format(absDiff));
                        redraw = true;
                    }
                }
            }
        }

        mSmoothenTimestamp = now;

        mForceSmoothenCurve = redraw;
    }

    private int mUserBandsUpdated;

    /**
     * updating EQ user gains in DS Service
     */
    private void updateEqUserGainsInEngine() {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.updateEqUserGainsInEngine");

        if (!mActivity.useDsApiOnUiEvent()) {
            return;
        }

        final Configuration conf = MainActivity.getConfiguration();
        if (conf == null) {
            return;
        }

        final int selectedProfile;
        final DsClientSettings stg;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDSClient);
            stg = DsClientCache.INSTANCE.getProfileSettings(mDSClient, selectedProfile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // updating GEq flag in DS service
        boolean isGeqOn = stg.getGeqOn();

        int b;
        float[] userGain = new float[Constants.BANDS];
        float old;
        float diff;
        int counter = 0;

        final float minEditGain = conf.getMinEditGain();

        boolean changed = false;

        final boolean suspended = mVisualizer.mSuspended;
        try {
            for (b = 0; b < Constants.BANDS; b++) {
                userGain[b] = mGainsSmooth[b];
                old = mGainsSmoothOld[b];
                diff = userGain[b] - old;
                if (!changed) {
                    changed = (diff != 0f);
                }
                if (userGain[b] != 0f && stg != null && !isGeqOn) {
                    isGeqOn = true;
                    Log.d(Tag.MAIN, "GraphicEqualizerPainter.setGeqOn true");
                    stg.setGeqOn(true);
                }
                if (userGain[b] < minEditGain) {
                    userGain[b] = minEditGain;
                } else if (userGain[b] > Constants.MAX_VISIBLE_GAIN) {
                    userGain[b] = Constants.MAX_VISIBLE_GAIN;
                }

                counter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (suspended && changed) {
            Log.d(Tag.MAIN, "mGainsUi: " + Tools.floatArrayToString(mGainsUi));
            mVisualizer.repaint(true);
        }

        if (changed) {
            try {
                mDSClient.setGeq(selectedProfile, this.mEqPreset, userGain);
                Log.d(Tag.MAIN, "GraphicEqualizerPainter DsClientCache.INSTANCE.setProfileSettings");
                DsClientCache.INSTANCE.setProfileSettings(mDSClient, selectedProfile, stg);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        mUserBandsUpdated = counter;
        // Log.d(Tag.MAIN,
        // "GraphicEqualizerPainter.updateEqUserGainsInEngine updates: "
        // + counter);

        if (mNotifyListener) {
            // notifying UI
            if (mListener != null) {
                mListener.onEqualizerEditStart();
            }
            mNotifyListener = false;
        }
    }

    private final Paint mPaintRed = new Paint();

    private final Paint mPaintGreen = new Paint();

    {
        mPaintRed.setColor(0xD0FF8080);
        mPaintRed.setTextSize(20);
        mPaintGreen.setColor(0xD080FF80);
    }

    private final FPSCounter mDrawFpsCounter = new FPSCounter();

    private final FPSCounter mSmoothFpsCounter = new FPSCounter();

    protected void onDraw(Canvas canvas) {
        if (!mVisible) {
            return;
        }

        if (!isEnabled() && !isAnimating()) {
            return;
        }

        final long now = System.currentTimeMillis();

        if (DISPLAY_DEBUG_TEXT) {
            mDrawFpsCounter.nextFrame(now);
        }

        final float[] gainsPaint = mVisualizer.mSuspended ? mGainsSmooth : mGainsUi;

        // alpha animation
        //DS1SOC-566, In LPA mode, the equlizer control is always-showing
        int alpha = MAX_ALPHA;
        if (!DS1Application.VISUALIZER_ENABLE) {
        	canvas.saveLayerAlpha(0, 0, mWidth, mHeight, alpha, Canvas.ALL_SAVE_FLAG);
        }else{
	        if (now < mShowAnimEndTimestamp) {
	            final long delay = now - (mShowAnimEndTimestamp - SHOW_HIDE_ANIMATION_DURATION);
	            alpha = (int) ((delay * MAX_ALPHA) / SHOW_HIDE_ANIMATION_DURATION);
	        } else if (now < mHideAnimEndTimestamp) {
	            final long left = (mHideAnimEndTimestamp - now);
	            alpha = (int) ((left * MAX_ALPHA) / SHOW_HIDE_ANIMATION_DURATION);
	        }
	        if (alpha < 0) {
	            alpha = 0;
	        } else if (alpha > MAX_ALPHA) {
	            alpha = MAX_ALPHA;
	        }
	        if (alpha < MAX_ALPHA) {
	            canvas.saveLayerAlpha(0, 0, mWidth, mHeight, alpha, Canvas.ALL_SAVE_FLAG);
	        } else if (mHideAnimEndTimestamp != 0 && now > mHideAnimEndTimestamp) {
	            mHideAnimEndTimestamp = 0;
	            mVisible = false;
	            return;
	        }
        }

        final int width = getWidth();

        final int nodes = Constants.BANDS;

        float initialI = TABLET_LAYOUT_INITIAL_COLUMN;
        float initialStep = TABLET_LAYOUT_INITIAL_STEP;

        if (mMobileLayout == true) {
            initialI = MOBILE_LAYOUT_INITIAL_COLUMN;
            initialStep = MOBILE_LAYOUT_INITIAL_STEP;
        }

        final int x0 = width / (2 * nodes);
        for (float i = initialI; i < nodes; i += initialStep) {
            final int x = (int) (x0 + ((i * width) / nodes));
            if (mSliderBg != null ) {
                canvas.save();
                canvas.translate(x - (mSliderBg.getBounds().width() / 2) , 0);
                mSliderBg.draw(canvas);
                canvas.restore();
            }

            final Drawable d;
            if (mEditBand == -1) {
                d = mSliderThumb;
            } else {
                final int dist = (int) Math.abs(i - mEditBand);
                if (dist == 0) {
                    d = mSliderThumbBright3;
                } else if (dist == 1) {
                    d = mSliderThumbBright2;
                } else if (dist == 2) {
                    d = mSliderThumbBright1;
                } else {
                    d = mSliderThumb;
                }
            }

            if (d != null) {
                final int dw = d.getBounds().width();
                final int dh = d.getBounds().height();

                canvas.save();

                final float yThumb = translateGaindBToY(gainsPaint,i);
                canvas.translate(x - (dw / 2), yThumb - (dh / 2));

                d.draw(canvas);
                canvas.restore();
                if (DISPLAY_DEBUG_BARS&&!mMobileLayout) {
                    float valY = translateGaindBToY(mUserGainsTemp,i+GAIN_SMOOTH_LENGTH);
                    canvas.drawRect(x - dw/2, valY - 2, x + dw/2, valY + 2, mPaintRed);
                    valY = translateGaindBToY(mGainsSmooth,i);
                    canvas.drawRect(x - dw/2, valY - 2, x + dw/2, valY + 2, mPaintGreen);
                }
            }
        }
        if (mMobileLayout) {
            Path mPath = new Path();
            float yThumb = translateGaindBToY(gainsPaint,0);
            mPath.moveTo(0, yThumb);

            for (int i = 0; i < nodes; i++) {
                int x = x0 + ((i * width) / nodes);
                yThumb = translateGaindBToY(gainsPaint,i);
                mPath.lineTo(x, yThumb);
                if (DISPLAY_DEBUG_BARS) {
                    float valY = translateGaindBToY(mUserGainsTemp,i+GAIN_SMOOTH_LENGTH);
                    float dw = width / (4.0f * Constants.BANDS);
                    canvas.drawRect(x - dw, valY - 2, x + dw, valY + 2, mPaintRed);
                    valY = translateGaindBToY(mGainsSmooth,i);
                    canvas.drawRect(x - dw, valY - 2, x + dw, valY + 2, mPaintGreen);
                }
            }
            mPath.lineTo(x0 + width, yThumb);
            canvas.drawPath(mPath, mPaintCurve2 );
            canvas.drawPath(mPath, mPaintCurve1 );
        }

        if (DISPLAY_DEBUG_TEXT) {
            final double fpsDraw = mDrawFpsCounter.getFPS();
            final double fpsRecalc = mSmoothFpsCounter.getFPS();
            canvas.drawText("UI FPS: " + Tools.mDecFormat.format(fpsDraw) + "   SMOOTH" + " FPS: " + Tools.mDecFormat.format(fpsRecalc) + "   UBU: " + mUserBandsUpdated, 10, 30, mPaintRed);
        }

        if (alpha < MAX_ALPHA) {
            canvas.restore();
        } else {

        }
    }

    private boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean b) {
        this.mEnabled = b;
    }

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    private void animateVisibility(boolean visible) {
        if (isAnimating()) {
            return;
        }

        Log.d(Tag.MAIN, "animateVisibility " + visible);

        preventHiding();

        final long now = System.currentTimeMillis();
        final long until = now + SHOW_HIDE_ANIMATION_DURATION;
        if (visible) {
            mVisible = true;
            mShowAnimEndTimestamp = until;
            delayHide(SHOW_HIDE_ANIMATION_DURATION);
        } else {
            mHideAnimEndTimestamp = until;
        }
    }

    private void preventHiding() {
        DS1Application.HANDLER.removeCallbacks(mHideAction);
    }

    private void delayHide() {
        delayHide(0);
    }

    private void delayHide(long add) {
        DS1Application.HANDLER.removeCallbacks(mHideAction);
        DS1Application.HANDLER.postDelayed(mHideAction, IDLE_HIDE_DELAY + add);
    }

    private final Runnable mHideAction = new Runnable() {

        @Override
        public void run() {
            animateVisibility(false);
            mVisualizer.repaint(true);
        }

    };

    public boolean onTouchEvent(MotionEvent event) {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.onTouchEvent");

        if (!mEnabled) {
            return true;
        }

        final int action = event.getAction();
        if (MotionEvent.ACTION_DOWN == action) {
            mNotifyListener = true;
            preventHiding();
            if (!isAnimatedVisible() || !mVisible) {
                animateVisibility(true);
                mRecalcPositions.run();

            }
        }

        mPrevEditBand = mEditBand;
        mPrevEditGain = mEditGain;

        if (MotionEvent.ACTION_MOVE == action || MotionEvent.ACTION_DOWN == action) {
            final float eX = event.getX();
            final float eY = event.getY();

            if (eX < 0f || eX >= mWidth || eY < 0 || eY >= mViewHeight) {
                // ignore events outside equalizer area
                return true;
            }

            // determine edit node index
            int iSetCenter = Math.max(0, Math.min(Constants.BANDS - 1, (int) (((eX * Constants.BANDS) / mWidth))));

            if (mMobileLayout == true) {
                iSetCenter= (int) Math.round(((4.75f)*Math.round((iSetCenter+1)/5.0)));
                if(iSetCenter >= Constants.BANDS) {
                    iSetCenter = 19;
                }
            }
            mEditBand = iSetCenter;

            // determine uiGain value in dB unit
            final float fTrackdB = translateYtoGaindB(eY);
            final float fSetdBdraw = Math.max(Constants.MIN_VISIBLE_GAIN, Math.min(Constants.MAX_VISIBLE_GAIN, fTrackdB));
            mEditGain = fSetdBdraw;

            // add new touch event to handle or update recently added one if it
            // was for the same band
            mEventQueue.add(iSetCenter, fSetdBdraw);
            if (mVisualizer.mSuspended) {
                mRecalcPositions.run();
            }
        } else if (MotionEvent.ACTION_UP == action) {
            mEditBand = -1;
            mPrevEditBand = -1;
            this.delayHide();
            mForceSmoothenCurve = true;
            mRecalcPositions.run();
        }
        return true;
    }

    private int getVerticalThumbPadding() {
        if (mSliderThumb != null) {
            return mSliderThumb.getBounds().height() / 4;
        } else {
            return 0;
        }
    }

    private float translateYtoGaindB(float y) {
        final int verticalThumbPadding = getVerticalThumbPadding();
        final int height = mViewHeight;
        final int paddedHeight = height - (2 * verticalThumbPadding);
        final float valueAbs = ((height - verticalThumbPadding - y) * Constants.VISIBLE_GAIN_SPAN) / paddedHeight;
        final float value = Math.max(Constants.MIN_VISIBLE_GAIN, Math.min(Constants.MAX_VISIBLE_GAIN, valueAbs + Constants.MIN_VISIBLE_GAIN));
        return value;
    }

    private float translateGaindBToY(float gainval[],float freq) {
        float value = 0.0f;
        if(this.mMobileLayout){
            double ba1 = Math.floor(freq);
            double ba2 = Math.ceil(freq);
            if (ba1 != ba2) {
                float val1 = gainval[(int)ba1];
                float val2 = gainval[(int)ba2];
                float inc = (float) (freq-ba1);
                value = inc*(val2-val1)+val1;
            }else{
                value=gainval[(int)freq];
            }
        } else {
            value=gainval[(int)freq];
        }
        final float verticalThumbPadding = getVerticalThumbPadding();
        final float height = getHeight();
        final float paddedHeight = height - (2 * verticalThumbPadding);
        final float valueAbs = value - Constants.MIN_VISIBLE_GAIN;
        final float paddedY = ((valueAbs * paddedHeight) / Constants.VISIBLE_GAIN_SPAN);

        return (height - verticalThumbPadding - paddedY);
    }

    private static int translateEqPresetIndex(int eqPreset) {
        if (eqPreset == -1) {
            eqPreset = 0;
        } else {
            eqPreset++;
        }
        return eqPreset;
    }

    public void switchPreset(int profile, int eqPreset, boolean updateInDs) {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.switchPreset " + profile + " " + eqPreset);
        eqPreset = translateEqPresetIndex(eqPreset);
        if (mProfile != profile || mEqPreset != eqPreset) {
            Log.i(Tag.MAIN, "updateUserGains " + profile + " " + eqPreset);
            this.mProfile = profile;
            this.mEqPreset = eqPreset;
            readUserGainsFromEngine();
            onIEqPresetChanged();
            if (updateInDs) {
                updateGeqOnInDs();
            }
            this.mRecalcPositions.run();
        }
    }

    public void setEqualizerListener(IEqualizerChangeListener listener) {
        this.mListener = listener;
    }

    public void setActivity(IDsActivityCommonTemp activity) {
        this.mActivity = activity;
    }

    public void setDsClient(DsClient dsc) {
        this.mDSClient = dsc;
    }

    public void hide() {
        animateVisibility(false);
        mVisualizer.repaint(false);
    }

    public void setVisible(boolean b) {
        this.mVisible = b;
    }

    private boolean mForceSmoothenCurve = false;

    private long recalcPosTimestamp = 0;

    private static final long MIN_RECALC_POS_INTERVAL = 60;

    private void doRecalcPositions() {
        if (!mVisualizer.isSurfaceCreated()) {
            return;
        }
        final long beginTime = System.currentTimeMillis();
        long delay = beginTime - recalcPosTimestamp;
        long minInterval = MIN_RECALC_POS_INTERVAL;
        if (mVisualizer.mSuspended) {
            minInterval /= 2;
        }
        if (delay < minInterval) {
            final long postDelay = minInterval - delay + 1;
            Log.d(Tag.MAIN, "GraphicEqualizerPainter.doRecalcPositions ignore");
            DS1Application.HANDLER.postDelayed(mRecalcPositions, postDelay);
            return;
        }

        Log.d(Tag.MAIN, "GraphicEqualizerPainter.doRecalcPositions");
        DS1Application.HANDLER.removeCallbacks(mRecalcPositions);
        recalcPosTimestamp = beginTime;

        if (mEventQueue.size() >= 4) {
            Log.d(Tag.MAIN, "mTouchEvents: " + mEventQueue.size());
        }

        // force update if finger is still down
        final int editBand = mEditBand;
        final float editGain = mEditGain;
        if (mEventQueue.size() == 0 && editBand != -1) {
            mEventQueue.add(editBand, editGain);
        }

        if (mEventQueue.size() != 0 || mForceSmoothenCurve) {
            if (DISPLAY_DEBUG_TEXT) {
                mSmoothFpsCounter.nextFrame();
            }
            handleNewTouchEvents();
            smoothenCurve();
            updateEqUserGainsInEngine();
        }
        if (mForceSmoothenCurve) {
            delayHide();
        }

        if (isVisible()) {
            delay = System.currentTimeMillis() - beginTime;
            delay = MIN_RECALC_POS_INTERVAL - delay;
            DS1Application.HANDLER.postDelayed(mRecalcPositions, delay);
        }
    }

    void readUserGainsFromEngine() {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.readUserGainsFromEngine");
        try {
            boolean changed = false;
            DsClientSettings stgs = mDSClient.getProfileSettings(mProfile);
            DsClientCache.INSTANCE.cacheProfileSettings(mDSClient, mProfile, stgs);
            mGainsSmooth = mDSClient.getGeq(mProfile, this.mEqPreset);
            final Configuration conf = MainActivity.getConfiguration();
            for (int i = 0;i < Constants.BANDS; i++) {
                if (mGainsSmooth[i] > conf.getMaxEditGain()) {
                    mGainsSmooth[i] = conf.getMaxEditGain();
                    changed = true;
                }
                if (mGainsSmooth[i] < conf.getMinEditGain()) {
                    mGainsSmooth[i] = conf.getMinEditGain();
                    changed = true;
                }
            }
            if (changed) {
                try {
                    mDSClient.setGeq(mProfile, this.mEqPreset, mGainsSmooth);
                    Log.d(Tag.MAIN, "GraphicEqualizerPainter DsClientCache.INSTANCE.setProfileSettings");
                    DsClientCache.INSTANCE.setProfileSettings(mDSClient, mProfile, stgs);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            calculateTempGainsFromSmoothed();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTempGainsFromSmoothed() {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.calculateTempGainsFromSmoothed");

        // compute unsmoothed gains from smoothed by applying inverse of the
        // smoother multiply against the 20x20 inverse smoothing matrix
        for (int b = 0; b < Constants.BANDS; b++) {
            mUserGainsTemp[b + GAIN_SMOOTH_LENGTH] = 0.0f;
            for (int bb = 0; bb < Constants.BANDS; bb++) {
                mUserGainsTemp[b + GAIN_SMOOTH_LENGTH] += GAIN_SMOOTHER_INV[b][bb] * mGainsSmooth[bb];
            }
        }
        // fill out the endpoints which are constrained to equal to ends of the
        // part computed above
        for (int i = 0; i < GAIN_SMOOTH_LENGTH; i++) {
            mUserGainsTemp[i] = mUserGainsTemp[GAIN_SMOOTH_LENGTH];
            mUserGainsTemp[2 * GAIN_SMOOTH_LENGTH + Constants.BANDS - 1 - i] = mUserGainsTemp[GAIN_SMOOTH_LENGTH + Constants.BANDS - 1];
        }
    }

    private void onIEqPresetChanged() {
        mEventQueue.reset();

        mSmoothenTimestamp = 0;
        smoothenCurve();
        System.arraycopy(mGainsSmooth, 0, mGainsSmoothOld, 0, Constants.BANDS);

        Log.d(Tag.MAIN, "usergains " + mEqPreset + ": " + Tools.floatArrayToString(mGainsSmooth));
    }

    private final Runnable mRecalcPositions = new Runnable() {

        @Override
        public void run() {
            doRecalcPositions();
        }
    };

    public void resetUserGains(boolean updateInDs) {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.resetUserGains " + updateInDs);
        if (mProfile == -1 || mEqPreset == -1) {
            return;
        }

        for (int b = 0; b < mUserGainsTemp.length; b++) {
            mUserGainsTemp[b] = 0f;
        }

        for (int b = 0; b < mGainsSmooth.length; b++) {
            mGainsSmooth[b] = 0f;
        }

        mEventQueue.reset();
        onIEqPresetChanged();

        if (updateInDs) {
            updateGeqOnInDs();
        }
    }

    private void updateGeqOnInDs() {
        Log.d(Tag.MAIN, "GraphicEqualizerPainter.updateGeqOnInDs");
        final DsClientSettings stg;
        final int selectedProfile;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDSClient);
            stg = DsClientCache.INSTANCE.getProfileSettings(mDSClient, selectedProfile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        float userGain;
        boolean geqOn = false;
        try {
            mDSClient.setGeq(selectedProfile, this.mEqPreset, mGainsSmooth);
            for (int b = 0; b < Constants.BANDS; b++) {
                userGain = mGainsSmooth[b];
                if (userGain != 0f) {
                    geqOn = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        stg.setGeqOn(geqOn);

        try {
            DsClientCache.INSTANCE.setProfileSettings(mDSClient, selectedProfile, stg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long mHideAnimEndTimestamp;

    private long mShowAnimEndTimestamp;

    /**
     * The GEq is currently animating to be visible or invisible (fading in or
     * fading out).
     * 
     * @return
     */
    public boolean isAnimating() {
        final long now = System.currentTimeMillis();
        return isAnimating(now);
    }

    /**
     * The GEq is currently animating to be visible or invisible (fading in or
     * fading out).
     * 
     * @param now
     * @return
     */
    private boolean isAnimating(long now) {
        return now < mHideAnimEndTimestamp || now < mShowAnimEndTimestamp;
    }

    /**
     * The GEq is currently being animated to visible state (fading in).
     * 
     * @return
     */
    private boolean isAnimatedVisible() {
        return System.currentTimeMillis() > mShowAnimEndTimestamp;
    }

    public boolean isVisible() {
        return mVisible;
    }

    private class EQTouchQueue {

        private final int[] mBands;

        private final float[] mGains;

        private int mSize = 0;

        public EQTouchQueue(int maxSize) {
            mBands = new int[maxSize];
            mGains = new float[maxSize];
        }

        public synchronized int size() {
            return mSize;
        }

        public synchronized void reset() {
            mSize = 0;
        }

        public synchronized void add(final int band, final float gain) {
            Log.d(Tag.MAIN, "EQTouchQueue.add  " + band + "  " + Tools.mDecFormat.format(gain) + "  size before: " + mSize + "  mPrevEditBand: " + mPrevEditBand);

            int n;
            if (mPrevEditBand != -1 && ((n = band - mPrevEditBand) < -1 || n > 1)) {
                // adding missing touch events
                final float minBand;
                final float maxBand;
                final float fromValue;
                final float valueSpan;
                final float fromBand = mPrevEditBand + (n < 0 ? -1 : 1);
                final float toBand = band + (n < 0 ? 1 : -1);
                final float inc = n < 0 ? -1 : 1;
                if (n < 0) {
                    minBand = band;
                    maxBand = mPrevEditBand;
                    fromValue = gain;
                    valueSpan = mPrevEditGain - gain;
                } else {
                    minBand = mPrevEditBand;
                    maxBand = band;
                    fromValue = mPrevEditGain;
                    valueSpan = gain - mPrevEditGain;
                }
                float val;
                final float bandSpan = maxBand - minBand;
                for (float b = fromBand; (n < 0 ? (b >= toBand) : (b <= toBand)) && mSize < mBands.length; b += inc) {
                    val = fromValue + (((b - minBand) * valueSpan) / bandSpan);
                    Log.d(Tag.MAIN, "EQTouchQueue adding missing  " + ((int) b) + "  " + Tools.mDecFormat.format(val));
                    mBands[mSize] = (int) b;
                    mGains[mSize] = val;
                    mSize++;
                }
            }

            if (mSize > 0 && mBands[mSize - 1] == band) {
                mGains[mSize - 1] = gain;
            } else if (mSize < mBands.length) {
                mBands[mSize] = band;
                mGains[mSize] = gain;
                mSize++;
            } else {
                Log.d(Tag.MAIN, "EQTouchQueue.add buffer overflow");
            }
        }

        public synchronized int getBandAt(int i) {
            return mBands[i];
        }

        public synchronized float getGainAt(int i) {
            return mGains[i];
        }
    }

}

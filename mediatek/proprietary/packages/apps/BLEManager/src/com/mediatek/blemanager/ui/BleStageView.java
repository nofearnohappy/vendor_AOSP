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

package com.mediatek.blemanager.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDevice.DeviceAttributeChangeListener;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.microedition.khronos.opengles.GL10;

public class BleStageView extends StageView
        implements OnTouchListener, DeviceAttributeChangeListener {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleStageView]";
    
    private static final int MAX_DEVICE_SUPPORT = 4;
    private Context mContext;
    private Resources mResource;
    private Stage mStage;
    private Image mSurface;
    private CachedBleDeviceManager mCachedBluetoothLEDeviceManager;
    private float mTextSize;

    // layout - size
    private static final float SHADOW_WIDTH_RATIO = 0.4870f;
    private static final float SHADOW_HEIGHT_RATIO = 0.5343f;
    private static final float IMAGE_WIDTH_RATIO = 0.4722f;
    private static final float IMAGE_HEIGHT_RATIO = 0.5231f;
    private static final float TEXT_HOLDER_WIDTH_RATIO = 0.4722f;
    private static final float TEXT_HOLDER_HEIGHT_RATIO = 0.0759f;
    private static final float BT_HOLDER_WIDTH_RATIO = 0.1352f;
    private static final float BT_HOLDER_HEIGHT_RATIO = 0.0629f;
    private static final float ALERT_ICON_RATIO = 0.0657f;
    private static final float PROGRESS_ICON_RATIO = 0.0463f;

    // layout - z order
    private static final float SHADOW_Z = 1f;
    private static final float IMAGE_Z = 0f;
    private static final float TEXT_BG_Z = -1f;
    private static final float NAME_Z = -2f;
    private static final float BT_ICON_Z = -2f;
    private static final float PROGRESS_Z = -2f;
    private static final float FRAME_Z = -5f;
    private static final float ALERT_Z = -6f;
    
    private static final float VELOCITY_THRESHOLD = 500f;
    private static final float PROGRESS_THRESHOLD = 0.2f;
    
    private static final float BT_HOLDER_OFFSET = 0.0134f;
    private static final float PROGRESS_ICON_OFFSET = 0.03f;
    
    private static final int DEVICE_IMAGE_WIDTH = 510;
    private static final int DEVICE_IMAGE_HEIGHT = 565;
    
    private static final int DURATION = 300;
    private static final int FLING_DURATION = 200;
    
    // bt signal
    private static final int BT_NO_ICON = -1;
    private static final int BT_SIGNAL_ZERO = 0;
    private static final int BT_SIGNAL_LOW = 1;
    private static final int BT_SIGNAL_MIDDLE = 2;
    private static final int BT_SIGNAL_FULL = 3;
    private static final int BT_CONNECTED = 4;
    private static final int BT_DISCONNECTED = 5;
    
    // layout - position
    private static Point[] sPOS = {
        new Point(0f, 0f, 0f, true),
        new Point(0.36f, 0f, 420f, true),
        new Point(-0.5f, 0f, 650f, true),
        new Point(-0.3f, 0f, 500f, true)
    };
    private static Rotation[] sROTATION = {
        new Rotation(0f, 0f, 0f),
        new Rotation(0f, 30.0f, 0f),
        new Rotation(0f, -60.0f, 0f),
        new Rotation(0f, -60.0f, 0f)
    };
    private static int[] sClockWiseMove = {3, 0, 1, 2};
    private static int[] sCounterClockWiseMove = {1, 2, 3, 0};

    // stage contents dimension
    private Dimension mShadowDimen;
    private Dimension mImageDimen;
    private Dimension mTextHolderDimen;
    private Dimension mBtHolderDimen;
    private Dimension mAlertDimen;
    private Dimension mProgressIconDimen;

    // stage contents point
    private Point mShadowPoint;
    private Point mNamePoint;
    private Point mTextHolderPoint;
    private Point mBtHolderPoint;
    private Point mRightAlertPoint;
    private Point mLeftAlertPoint;
    private Point mProgressIconPoint;

    // transition
    private boolean mIsClockwise;
    private float mSurfaceWidth;
    private float mAccumProgress;
    private float mValidAccum;
    private int mFocus; // focus on certain location index
    private int mMarker = -1; // -1 for moving toward next position, otherwise, restoring position

    private Container mRoot = new Container(); // root container for all tags
    private LinkedList<Integer> mAnimIndexQue = new LinkedList<Integer>();
    private LinkedList<AnimationGroup> mCounterClockwiseAnimQue = new LinkedList<AnimationGroup>();
    private LinkedList<AnimationGroup> mClockwiseAnimQue = new LinkedList<AnimationGroup>();
    private SparseArray<BleDeviceTag> mLocationMap = new SparseArray<BleDeviceTag>();

    // touch, gesture, and animation
    private GestureDetector mDetector;
    private AnimationGroup mAnimGroup;
    private ValueAnimator mFlingAnim;

    // call back listener
    private OnBleEventListener mOnBleEventListener;

    public BleStageView(Context context) {
        super(context);
        init(context);
    }

    public BleStageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        Log.i(TAG, "[init]...");
        mContext = context;
        mResource = mContext.getResources();
        mStage = getStage();
        mTextSize = getTextSize();
        mStage.setBackgroundColor(new Color(248, 248, 248));
        mAnimGroup = new AnimationGroup();

        // create a reflected mirror floor surface
        mSurface = Image.createFromAsset("perlin_noise.png");
        mSurface.setMaterial("reflect.mat");
        mRoot.add(mSurface);

        // create MAX_DEVICE_SUPPORT number of devices
        for (int i = 0; i < MAX_DEVICE_SUPPORT; i++) {
            BleDeviceTag tag = new BleDeviceTag(i);
            mLocationMap.put(i, tag);
            mAnimIndexQue.add(i);
        }

        mStage.add(mRoot);

        // listen to touch events on the StageView.
        setOnTouchListener(this);
        mDetector = new GestureDetector(mContext, new GestureListener());

        // initialize counterclockwise animation
        for (int i = 0; i < MAX_DEVICE_SUPPORT; i++) {
            int j = (i + 1) % MAX_DEVICE_SUPPORT;
            AnimationGroup group = new AnimationGroup();
            group.add(new PropertyAnimation("position", sPOS[i], sPOS[j]).setMode(Mode.LINEAR)
                      .setDuration(DURATION))
            .add(new PropertyAnimation("rotation", sROTATION[i], sROTATION[j])
                 .setMode(Mode.LINEAR).setDuration(DURATION));
            group.disableOptions(AnimationGroup.START_TARGET_WITH_INITIAL_VALUE);
            mCounterClockwiseAnimQue.add(group);
        }

        // initialize clockwise animation
        for (int i = 0; i < MAX_DEVICE_SUPPORT; i++) {
            int j = (MAX_DEVICE_SUPPORT + i - 1) % MAX_DEVICE_SUPPORT;
            AnimationGroup group = new AnimationGroup();
            group.add(new PropertyAnimation("position", sPOS[i], sPOS[j]).setMode(Mode.LINEAR)
                      .setDuration(DURATION))
            .add(new PropertyAnimation("rotation", sROTATION[i], sROTATION[j])
                 .setMode(Mode.LINEAR).setDuration(DURATION));
            group.disableOptions(AnimationGroup.START_TARGET_WITH_INITIAL_VALUE);
            mClockwiseAnimQue.add(group);
        }

        // get device manager
        mCachedBluetoothLEDeviceManager = CachedBleDeviceManager.getInstance();
    }

    public boolean addDevice(CachedBleDevice device) {
        if (device == null) {
            Log.w(TAG, "[addDevice]device...");
            return false;
        }

        int locationIndex = device.getDeviceLocationIndex();
        int state = device.getConnectionState();
        int signal = device.getIntAttribute(CachedBleDevice.DEVICE_DISTANCE_FLAG);
        String name = device.getDeviceName();
        Uri uri = device.getDeviceImageUri();

        Log.d(TAG, "[addDevice] state " + state + " signal " + signal);
        if (isValidParams(/*tagId, */name, uri, locationIndex)) {
            BleDeviceTag tag = mLocationMap.get(locationIndex);
            tag.add(/*tagId, */name, uri, state, signal);
            tag.setAlarm(device.getBooleanAttribute(
                    CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG));
            device.registerAttributeChangeListener(this);
            return true;
        }
        return false;
    }

    public boolean removeDevice(CachedBleDevice device) {
        BleDeviceTag tag = mLocationMap.get(device.getDeviceLocationIndex());
        if (tag == null) {
            Log.e(TAG, "[removeDevice]Fail to remove device due to invalid display order");
            return false;
        }
        tag.remove();
        device.unregisterAttributeChangeListener(this);
        return true;
    }

    private boolean isValidParams(/*int tagId, */String name, Uri uri, int locationIndex) {
        if (name == null) {
            Log.e(TAG, "[isValidParams]Name must not be null");
            return false;
        }

        if (locationIndex < 0 || locationIndex >= MAX_DEVICE_SUPPORT) {
            Log.e(TAG, "Location index must be in between 0 and " + MAX_DEVICE_SUPPORT);
            return false;
        }
        return true;
    }

    private float getTextSize() {
        // get device density and set proper text size for device tag
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        float size;

        switch (dm.densityDpi) {
        case DisplayMetrics.DENSITY_XXXHIGH:
            Log.d(TAG, "Screen density: xxxhdpi");
            size = 60f;
            break;
        case DisplayMetrics.DENSITY_XXHIGH:
            Log.d(TAG, "Screen density: xxhdpi");
            size = 48f;
            break;
        case DisplayMetrics.DENSITY_XHIGH:
            Log.d(TAG, "Screen density: xhdpi");
            size = 30f;
            break;
        case DisplayMetrics.DENSITY_HIGH:
            Log.d(TAG, "Screen density: hdpi");
            size = 25f;
            break;
        case DisplayMetrics.DENSITY_MEDIUM:
            Log.d(TAG, "Screen density: mdpi");
            size = 15f;
            break;
        case DisplayMetrics.DENSITY_LOW:
            Log.d(TAG, "Screen density: ldpi");
            size = 10f;
            break;
        default:
            Log.w(TAG, "[getTextSize]Screen density: unknown");
            size = 10f;
            break;
        }

        return size;
    }

    public void refresh() {
        if (mCachedBluetoothLEDeviceManager.getCachedDeviceFromDisOrder(mFocus) == null) {
            // Remove device only when it is not empty
            if (mLocationMap.get(mFocus).getSignalLevel() != BT_NO_ICON) {
                Log.d(TAG, "[refresh]...");
                mLocationMap.get(mFocus).remove();
            }
        }
    }

    public void setFocus(int index) {
        BleDeviceTag tag = mLocationMap.get(index);
        if (tag == null) {
            Log.e(TAG, "[setFocus]Fail to set focus due to invalid location index");
        } else {
            if (mFocus != index) {
                mFocus = index;
                setDeviceLocation();

                // reset Animation index queue
                while (mAnimIndexQue.getFirst() != 0) {
                    mAnimIndexQue.addLast(mAnimIndexQue.pollFirst());
                }
            }
        }
    }

    public void setDeviceLocation() {
        BleDeviceTag tag;
        for (int i = 0; i < mLocationMap.size(); i++) {
            tag = mLocationMap.get(i);
            int pos = (tag.getLocationIndex() - mFocus + sPOS.length) % sPOS.length;
            tag.setLocation(pos);
        }
    }

    public void setOnBleEventListener(OnBleEventListener listener) {
        mOnBleEventListener = listener;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        Log.d(TAG, "[onSurfaceChanged]width = " + width + ", height = " + height + ")");
        mSurfaceWidth = (float) width;

        // set layout
        setLayout(width, height);
    }

    private void setLayout(int width, int height) {
        mRoot.setPosition(new Point(0.5f, 0.535f, 0f, true));

        // set each device's location
        setDeviceLocation();

        float base = (float) width; // a reference width used to calculate the size

        // calculate size
        mImageDimen = new Dimension(IMAGE_WIDTH_RATIO * base, IMAGE_HEIGHT_RATIO * base);
        mShadowDimen = new Dimension(SHADOW_WIDTH_RATIO * base, SHADOW_HEIGHT_RATIO * base);
        mBtHolderDimen = new Dimension(BT_HOLDER_WIDTH_RATIO * base, BT_HOLDER_HEIGHT_RATIO * base);
        mTextHolderDimen = new Dimension(TEXT_HOLDER_WIDTH_RATIO * base,
                                         TEXT_HOLDER_HEIGHT_RATIO * base);
        mAlertDimen = new Dimension(ALERT_ICON_RATIO * base, ALERT_ICON_RATIO * base);
        mProgressIconDimen = new Dimension(PROGRESS_ICON_RATIO * base, PROGRESS_ICON_RATIO * base);

        // calculate image ratio on the surface view
        float imgHalfW = IMAGE_WIDTH_RATIO  / 2.0f;
        float imgHalfH = IMAGE_HEIGHT_RATIO * base / height / 2.0f;

        // shadow
        // 0.002f is calculated manually (need to adjust for different resolutions)
        float shadowY = 6.0f / height - 0.002f;
        mShadowPoint = new Point(0f, shadowY, SHADOW_Z, true);

        // bt holder
        float btHolderX = -imgHalfW + (BT_HOLDER_WIDTH_RATIO / 2.0f);
        float btHolderY = BT_HOLDER_OFFSET - imgHalfH
                          + (BT_HOLDER_HEIGHT_RATIO * base / height / 2.0f);
        mBtHolderPoint = new Point(btHolderX, btHolderY, BT_ICON_Z, true);

        // alert icon
        mRightAlertPoint = new Point(imgHalfW, -imgHalfH, ALERT_Z, true);
        mLeftAlertPoint = new Point(-imgHalfW, -imgHalfH, ALERT_Z, true);

        // text holder
        float textHolderOffsetY = imgHalfH - (TEXT_HOLDER_HEIGHT_RATIO * base / height / 2.0f);
        mTextHolderPoint = new Point(0f, textHolderOffsetY, TEXT_BG_Z, true);
        mNamePoint = new Point(0f, textHolderOffsetY, NAME_Z, true);

        // progress icon
        mProgressIconPoint = new Point(PROGRESS_ICON_OFFSET - imgHalfW, textHolderOffsetY
                                       , PROGRESS_Z, true);

        // re-layout each device tag
        for (int i = 0; i < mLocationMap.size(); i++) {
            mLocationMap.get(i).relayout();
        }

        // set reflected surface
        // 256 is the width of the reflected surface (perlin_noise.png)
        // use shadowOffsetY to calculate the middle point of the reflected surface
        // 0.0035f, 0.9f, 2f are calculated manually (need to adjust for different resolutions)
        float scale = mSurfaceWidth / 256f;
        float shadowOffsetY = imgHalfH + shadowY - 0.0035f;
        mSurface.setPosition(new Point(0f, shadowOffsetY, 0f, true));
        mSurface.setScale(new Scale(scale + 0.9f, scale + 2f, 1f));
        mSurface.setRotation(new Rotation(270, 0, 0));
    }

    private boolean isClockwise(float val) {
        return (val < 0);
    }

    private void handleScroll(float distanceX) {
        float progress = Math.abs(distanceX) / mSurfaceWidth;

        if (progress == 0f) {
            Log.i(TAG, "[handleScroll]progress = 0,return.");
            return;
        }

        float preAccumProgress = mAccumProgress;

        if (mIsClockwise) {
            mAccumProgress = mAccumProgress - progress;
        } else {
            mAccumProgress = mAccumProgress + progress;
        }

        // check if the direction is reversed
        if (Math.abs(mAccumProgress) < Math.abs(preAccumProgress)) {
            mMarker = mFocus;
        } else {
            mMarker = -1;
        }

        if (((int) Math.signum(preAccumProgress) != (int) Math.signum(mAccumProgress))) {
            // pass origin and go further
            mMarker = -1;
        }

        float validAccum = Math.min(mAccumProgress, 1f);

        if (mMarker == mFocus) {
            mValidAccum = 1.0f - Math.abs(validAccum);
        } else {
            mValidAccum = Math.abs(validAccum);
        }

        updateAnimation(false).setProgress(mValidAccum);
    }

    private void scrollToNearest() {
        if (mIsClockwise) {
            if (mValidAccum <= PROGRESS_THRESHOLD) {
                if (mMarker == mFocus) {
                    mMarker = -1; // scroll to next
                } else {
                    mMarker = mFocus; // scroll to prev
                }

                mIsClockwise = false; // reverse
                mValidAccum = 1.0f - Math.abs(mValidAccum);
            }
        } else {
            if (mValidAccum <= PROGRESS_THRESHOLD) {
                if (mMarker == mFocus) {
                    mMarker = -1; // scroll to next
                } else {
                    mMarker = mFocus; // scroll to prev
                }

                mIsClockwise = true; // reverse
                mValidAccum = 1.0f - Math.abs(mValidAccum);
            }
        }

        Animation anim = updateAnimation(true);
        anim.setProgress(mValidAccum);
        anim.start();
    }

    private void handleFling() {
        actionStop();

        mFlingAnim = ValueAnimator.ofFloat(mValidAccum, 1f);
        mFlingAnim.setInterpolator(new LinearInterpolator());
        mFlingAnim.setDuration(FLING_DURATION);

        mFlingAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator ani) {
                Container container;
                int newIndex;
                int index;
                int size = mLocationMap.size();
                Float progress = (Float) ani.getAnimatedValue();

                for (int i = 0; i < size; i++) {
                    if (mIsClockwise) {
                        if (mMarker == mFocus) {
                            index = mAnimIndexQue.get((size + i - 1) % size);
                        } else {
                            index = mAnimIndexQue.get(i);
                        }

                        newIndex = sClockWiseMove[i];
                    } else {
                        if (mMarker == mFocus) {
                            index = mAnimIndexQue.get((i + 1) % size);
                        } else {
                            index = mAnimIndexQue.get(i);
                        }

                        newIndex = sCounterClockWiseMove[i];
                    }

                    container = mLocationMap.get(index).getContainer();
                    setMovement(container, sPOS[i], sPOS[newIndex],
                                sROTATION[i], sROTATION[newIndex], progress);
                }
            }
        });

        mFlingAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator ani) {
                postTask();
            }
        });

        mFlingAnim.start();
    }

    public void actionStop() {
        if (mFlingAnim != null) {
            mFlingAnim.cancel();
        }
    }

    private void setMovement(Container container, Point fromPoint, Point toPoint,
                             Rotation fromRotation, Rotation toRotation, float progress) {
        // set new rotation
        float[] fromXYZ = fromRotation.getEulerAngles();
        float fromDegree = fromXYZ[1];

        float[] toXYZ = toRotation.getEulerAngles();
        float toDegree = toXYZ[1];

        float newDegree = (toDegree - fromDegree) * progress + fromDegree;
        container.setRotation(new Rotation(fromXYZ[0], newDegree, fromXYZ[2]));

        // set new point
        float x = fromPoint.x + (toPoint.x - fromPoint.x) * progress;
        float y = fromPoint.y + (toPoint.y - fromPoint.y) * progress;
        float z = fromPoint.z + (toPoint.z - fromPoint.z) * progress;
        container.setPosition(new Point(x, y, z, true));
    }

    private Animation updateAnimation(boolean done) {
        AnimationGroup subGroup;
        Container container;
        int index;
        int size = mLocationMap.size();
        mAnimGroup.clear();

        for (int i = 0; i < size; i++) {
            if (mIsClockwise) {
                if (mMarker == mFocus) {
                    index = mAnimIndexQue.get((size + i - 1) % size);
                } else {
                    index = mAnimIndexQue.get(i);
                }

                subGroup = mClockwiseAnimQue.get(i);
            } else {
                if (mMarker == mFocus) {
                    index = mAnimIndexQue.get((i + 1) % size);
                } else {
                    index = mAnimIndexQue.get(i);
                }

                subGroup = mCounterClockwiseAnimQue.get(i);
            }

            container = mLocationMap.get(index).getContainer();
            setAnimationTarget(subGroup, container);
            mAnimGroup.add(subGroup);
        }

        if (done) {
            postTask();
        }

        return mAnimGroup;
    }

    private void setAnimationTarget(AnimationGroup group, Actor target) {
        for (int i = 0; i < group.getAnimationCount(); i++) {
            group.getAnimation(i).setTarget(target);
        }
    }

    private void postTask() {
        Log.d(TAG, "[postTask]mMarker = " + mMarker + ",mFocus = " + mFocus);
        // if the animation is settle down, update the focused index
        if (mMarker != mFocus) {
            if (mIsClockwise) {
                // pop front and push back
                mAnimIndexQue.addLast(mAnimIndexQue.pollFirst());
            } else {
                // pop back and push front
                mAnimIndexQue.addFirst(mAnimIndexQue.pollLast());
            }

            mFocus = mAnimIndexQue.getFirst();
            updateTagState();

            if (mOnBleEventListener != null) {
                mOnBleEventListener.onFocus(mFocus);
            }
        }
    }

    private void updateTagState() {
        for (int i = 0; i < mAnimIndexQue.size(); i++) {
            mLocationMap.get(mAnimIndexQue.get(i)).setCurrAnimIndex(i);
        }

        for (int i = 0; i < mLocationMap.size(); i++) {
            // update alert position according to the location
            mLocationMap.get(i).updateAlertPosition();

            // show BT signal only when the tag is in focus
            mLocationMap.get(i).updateSignalVisibility();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // let the GestureDetector consumes this event
        boolean result = mDetector.onTouchEvent(event);

        // if the GestureDetector does not consume this event, consume it here.
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                scrollToNearest();
                result = true;
            }
        }

        return result;
    }

    /**
     * Extends SimpleOnGestureListener to provide custom gesture processing.
     */
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            // negative distanceX indicates counterclockwise
            mIsClockwise = isClockwise(-distanceX);
            handleScroll(distanceX);

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {

            if (Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                // positive velocityX indicates counterclockwise
                boolean flingClockwise = (isClockwise(velocityX));

                if (flingClockwise != mIsClockwise) {
                    // reverse to the same direction as fling
                    mIsClockwise = flingClockwise;
                    mMarker = -1;
                    mValidAccum = 1.0f - Math.abs(mValidAccum);
                }

                handleFling();
                return true;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            mAccumProgress = 0;
            mValidAccum = 0;
            mMarker = -1;
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // device is clickable only when it is focused
            BleDeviceTag tag = mLocationMap.get(mFocus);
            Actor actor = tag.getContainer().hitTest(new Point(e.getX(), e.getY()));

            if (actor != null && mOnBleEventListener != null) {
                mOnBleEventListener.onClick(mFocus);
            }

            return true;
        }
    }

    @Override
    public void onDeviceAttributeChange(CachedBleDevice device, int whichAttribute) {
        BleDeviceTag tag = mLocationMap.get(device.getDeviceLocationIndex());
        if (tag != null) {
            Log.d(TAG, "[onDeviceAttributeChange]whichAttribute: " + whichAttribute);
            switch (whichAttribute) {
            case CachedBleDevice.DEVICE_NAME_ATTRIBUTE_FLAG:
                tag.setName(device.getDeviceName());
                break;
                
            case CachedBleDevice.DEVICE_IMAGE_ATTRIBUTE_FLAG:
                tag.setImage(device.getDeviceImageUri());
                break;
                
            case CachedBleDevice.DEVICE_ALERT_STATE_FLAG:
                tag.setAlert(device.getBooleanAttribute(
                        CachedBleDevice.DEVICE_ALERT_STATE_FLAG));
                break;
                
            case CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG:
                tag.setAlarm(device.getBooleanAttribute(
                        CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG));
                break;
                
            case CachedBleDevice.DEVICE_DISTANCE_FLAG:
                // TODO: sync signal level
                if (device.isSupportPxpOptional()
                        && device.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "onAttributeChanged: DEVICE_DISTANCE_FLAG");
                    tag.setBtIcon(device.getIntAttribute(
                            CachedBleDevice.DEVICE_DISTANCE_FLAG));
                }
                break;
                
            case CachedBleDevice.DEVICE_CONNECTION_STATE_CHANGE_FLAG:
                int state = device.getConnectionState();
                if (state == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "onAttributeChanged: BT_DISCONNECTED");
                    tag.setBtIcon(BT_DISCONNECTED);
                    tag.setLoading(false);
                } else if (state == BluetoothGatt.STATE_CONNECTED) {
                    tag.setLoading(false);
                    if (device.isSupportPxpOptional()) {
                        Log.d(TAG, "onAttributeChanged: BT_CONNECTED with signal");
                        tag.setBtIcon(device.getIntAttribute(
                                CachedBleDevice.DEVICE_DISTANCE_FLAG));
                    } else {
                        Log.d(TAG, "onAttributeChanged: BT_CONNECTED with no signal");
                        tag.setBtIcon(BT_CONNECTED);
                    }
//                    tag.setBtIcon(device.getIntAttribute(
//                    CachedBluetoothLEDevice.DEVICE_DISTANCE_FLAG));
                } else if (state == BluetoothGatt.STATE_CONNECTING) {
                    tag.setLoading(true);
                }
                break;
                
            default:
                break;
            }
        } else {
            Log.e(TAG, "[onDeviceAttributeChange]tag is null");
        }
    }

    private class BleDeviceTag {
        private final Container mContainer = new Container();
        private final Container mTextHolder = new Container();
        private final Container mBtHolder = new Container();
        private static final int DEFAULT_IMG = R.drawable.image_add;
        private Image mShadow;
        private Image mImage;
        private Image mTextBg;
        private Image mFrame;
        private Image mAlert;
        private Image mLoadingIcon;
        private Text mName;
        private int mLocationIndex;
        private int mCurrAnimIndex;
        private int mSignalLevel = -1;
        private ValueAnimator mBlinkAnim;
        private Animation mLoadingAnim;
        private ArrayList<Image> mBtIconList = new ArrayList<Image>();
        private Bitmap mPrevBitmap;

        public BleDeviceTag(int locationIndex) {
            Log.d(TAG, "[BleDeviceTag]new...");
            mLocationIndex = locationIndex;

            // shadow
            mShadow = Image.createFromResource(mResource, R.drawable.image_shadow);

            // image
            mImage = Image.createFromResource(mResource, DEFAULT_IMG);

            // name
            mName = new Text(mResource.getString(R.string.add_device));
            mName.setTextColor(Color.WHITE);
            mName.setSingleLine(true);
            mName.setMaxWidth(350);
            mName.setTextSize(mTextSize);
            mName.setEllipsizeStyle(Text.ELLIPSIZE_BY_3DOT);

            // text holder
            mTextBg = Image.createFromResource(mResource, R.drawable.text_bg);
            mTextHolder.add(mTextBg, mName);

            // bt holder
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.ic_bt_combine_signal_0));
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.ic_bt_combine_signal_1));
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.ic_bt_combine_signal_2));
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.ic_bt_combine_signal_3));
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.bt_bar_connected));
            mBtIconList.add(Image.createFromResource(mResource, R.drawable.bt_bar_disconnected));

            for (Image img : mBtIconList) {
                img.setVisible(false);
                img.setMaterial("solid.mat");
                mBtHolder.add(img);
            }

            // frame
            mFrame = Image.createFromResource(mResource, R.drawable.image_alerting);
            mFrame.setVisible(false); // always invisible unless during animation

            // alert
            mAlert = Image.createFromResource(mResource, R.drawable.ic_alerting);

            // loading
            mLoadingIcon = Image.createFromResource(mResource, R.drawable.ic_bt_connecting);

            // add all contents to the container
            disableVisibility();
            mContainer.add(mImage, mShadow, mFrame, mBtHolder, mAlert, mTextHolder, mLoadingIcon);
            mRoot.add(mContainer);

            // set up loading animation
            mLoadingAnim = new PropertyAnimation(mLoadingIcon, "rotation", new Rotation(0, 0, 0),
                                                 new Rotation(0, 0, 360)).setDuration(1000)
            .setLoop(true);

            // set up blinking animation
            mBlinkAnim = ValueAnimator.ofFloat(0f, 1f);
            mBlinkAnim.setDuration(400).setRepeatCount(ValueAnimator.INFINITE);

            mBlinkAnim.addListener(new AnimatorListener() {

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    if (mFrame.getVisible())
                    Log.d(TAG, "[onAnimationEnd] end");
                    mFrame.setVisible(false);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    if (mFrame.getVisible()) {
                        mFrame.setVisible(false);
                    } else {
                        mFrame.setVisible(true);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            });
        }

        public void add(/*int tagId, */String name, Uri uri, int state, int signal) {
            Log.d(TAG, "[BleDeviceTag] add - location index: " + mLocationIndex);

            if (state == BluetoothProfile.STATE_CONNECTED) {
                // TODO: sync signal level
                if (signal > -1 && signal <= BT_SIGNAL_FULL) {
                    setBtIcon(signal);
                } else {
                    setBtIcon(BT_CONNECTED);
                }

            } else {
                setBtIcon(BT_DISCONNECTED);
            }

            setName(name);
            setImage(uri);
        }

        public void remove() {
            Log.d(TAG, "[remove]location index: " + mLocationIndex);
            mImage.setImageFromResource(mResource, DEFAULT_IMG);
            if (mImageDimen != null) {
                mImage.setSize(mImageDimen); // Workaroud ALPS01897361
            }
            mName.setText(mResource.getString(R.string.add_device));
            setBtIcon(BT_NO_ICON);
            disableVisibility();
            setAlarm(false);
        }

        private Bitmap getBitmap(Uri uri) {
            if (uri == null) {
                Log.w(TAG, "[getBitmap] uri is null");
                return null;
            }
            
            InputStream is = null;
            InputStream iso = null;
            try {
                is = mContext.getContentResolver().openInputStream(uri);
                iso = mContext.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                return null;
            }
            if (is == null) {
                Log.w(TAG, "[getBitmap] cursor is null");
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            final int height = options.outHeight;
            final int width = options.outWidth;
            Log.d(TAG, "[getBitmap] origin width = " + width + ", height = " + height);
            int inSampleSize = 1;
            if (height > DEVICE_IMAGE_HEIGHT || width > DEVICE_IMAGE_WIDTH) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) > DEVICE_IMAGE_HEIGHT
                        && (halfWidth / inSampleSize) > DEVICE_IMAGE_WIDTH) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = inSampleSize;
            Log.d(TAG, "[getBitmap] options inSampleSize : " + options.inSampleSize);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeStream(iso, null, options);
            try {
                is.close();
            } catch (IOException ex) {
                Log.e(TAG, "[getBitmap] exception happened while close : " + ex.toString());
            }
            return bitmap;
        }

        public void setImage(Uri uri) {
            if (uri == null) {
                Log.d(TAG, "[setImage] uri is null, maybe is first time to add");
                return;
            }
            Log.d(TAG, "[setImage] uri: " + uri.toString());

            if (uri.getScheme().equals("android.resource")) {
                // resource uri
                mImage.setImageFromResource(mResource, Integer.parseInt(uri.getLastPathSegment()));
                if (mImageDimen != null) {
                    mImage.setSize(mImageDimen); // Workaroud ALPS01897361
                }
            } else {
                // file uri
                try {
                    Bitmap bm = getBitmap(uri);
                    if (bm == null) {
                        Log.d(TAG, "[setImage] bit map is null");
                        return;
                    }
                    Log.d(TAG, "[setImage] bitmap width : " +
                            bm.getWidth() + ", height : " + bm.getHeight());
                    Message msg = mHandler.obtainMessage();
                    msg.what = UPDATE_DEVICE_IMAGE;
                    msg.obj = bm;
                    mHandler.sendMessage(msg);
                } catch (NullPointerException e) {
                    Log.e(TAG, "[setImage]Fail to decode bitmap uri");
                }
            }
        }

        public void setName(String name) {
            Message msg = mHandler.obtainMessage();
            msg.what = UPDATE_DEVICE_NAME_FLAG;
            msg.obj = name;
            mHandler.sendMessage(msg);
        }

        public void setBtIcon(int level) {
            Log.d(TAG, "[BleDeviceTag] setBtIcon - level: " + level + ", mSignalLevel: "
                    + mSignalLevel);
            if (mSignalLevel == level) {
                return;
            }

            if (mSignalLevel >= 0) {
                mBtIconList.get(mSignalLevel).setVisible(false);
            }

            if (level >= 0) {
                if (level == BT_DISCONNECTED || mFocus == mLocationIndex) {
                    mBtIconList.get(level).setVisible(true);

                    if (level != BT_CONNECTED) {
                        mBtIconList.get(BT_CONNECTED).setVisible(false);
                    }
                } else {
                    mBtIconList.get(BT_CONNECTED).setVisible(true);
                }
            }

            mSignalLevel = level;
        }

        public void relayout() {
            // image
            mImage.setSize(mImageDimen);
            mImage.setPosition(new Point(0f, 0f, IMAGE_Z, true));

            // shadow
            mShadow.setSize(mShadowDimen);
            mShadow.setPosition(mShadowPoint);

            // bt holder
            for (Image icon : mBtIconList) {
                icon.setSize(mBtHolderDimen);
                icon.setPosition(mBtHolderPoint);
            }

            // text holder
            mTextBg.setSize(mTextHolderDimen);
            mTextBg.setPosition(mTextHolderPoint);
            mName.setPosition(mNamePoint);

            // progress icon
            mLoadingIcon.setSize(mProgressIconDimen);
            mLoadingIcon.setPosition(mProgressIconPoint);

            // frame
            mFrame.setSize(mImageDimen); // frame size is same as image
            mFrame.setPosition(new Point(0f, 0f, FRAME_Z, true));

            // alert
            mAlert.setSize(mAlertDimen);
            updateAlertPosition();
        }

        public void updateSignalVisibility() {
            Log.d(TAG, "[BleDeviceTag] updateSignalVisibility - mSignalLevel: " + mSignalLevel
                    + ", mFocus: " + mFocus);

            switch(mSignalLevel) {
                case BT_SIGNAL_ZERO:
                case BT_SIGNAL_LOW:
                case BT_SIGNAL_MIDDLE:
                case BT_SIGNAL_FULL:
                    if (mFocus == mLocationIndex) {
                        mBtIconList.get(mSignalLevel).setVisible(true);
                        mBtIconList.get(BT_CONNECTED).setVisible(false);
                    } else {
                        mBtIconList.get(mSignalLevel).setVisible(false);
                        mBtIconList.get(BT_CONNECTED).setVisible(true);
                    }
                    break;
                    
                case BT_CONNECTED:
                    mBtIconList.get(BT_CONNECTED).setVisible(true);
                    break;
                    
                case BT_DISCONNECTED:
                    mBtIconList.get(mSignalLevel).setVisible(true);
                    mBtIconList.get(BT_CONNECTED).setVisible(false);
                    break;
                    
                default:
                    break;
            }
        }

        public void setLocation(int pos) {
            mContainer.setPosition(sPOS[pos]);
            mContainer.setRotation(sROTATION[pos]);
            mCurrAnimIndex = pos;
        }

        public void setCurrAnimIndex(int pos) {
            mCurrAnimIndex = pos;
        }

        public void disableVisibility() {
            Log.d(TAG, "[disableVisibility]mSignalLevel: " + mSignalLevel);

            if (mSignalLevel >= 0) {
                mBtIconList.get(mSignalLevel).setVisible(false);
            }

            mAlert.setVisible(false);
            mLoadingIcon.setVisible(false);
        }

        public void setAlert(boolean visible) {
            Message msg = mHandler.obtainMessage();
            if (visible) {
                msg.what = SET_ALERT_VISIBLE_FLAG;
            } else {
                msg.what = SET_ALERT_INVISIBLE_FLAG;
            }
            mHandler.sendMessage(msg);
            updateAlertPosition();
        }

        public void updateAlertPosition() {
            Message msg = mHandler.obtainMessage();
            msg.what = UPDATE_ALERT_POSITION_FLAG;
            mHandler.sendMessage(msg);
        }

        public void setAlarm(boolean alarm) {
            if (alarm) {
                Message msg = mHandler.obtainMessage();
                msg.what = START_BG_ANIMATOR_FLAG;
                mHandler.sendMessage(msg);
            } else {
                Message msg = mHandler.obtainMessage();
                msg.what = STOP_BG_ANIMATOR_FLAG;
                mHandler.sendMessage(msg);
            }
        }

        private static final int UPDATE_DEVICE_NAME_FLAG = 100;
        private static final int START_BG_ANIMATOR_FLAG = 101;
        private static final int STOP_BG_ANIMATOR_FLAG = 102;
        private static final int START_CONNECTING_FLAG = 103;
        private static final int STOP_CONNECTING_FLAG = 104;
        private static final int SET_ALERT_VISIBLE_FLAG = 105;
        private static final int SET_ALERT_INVISIBLE_FLAG = 106;
        private static final int UPDATE_ALERT_POSITION_FLAG = 107;
        private static final int UPDATE_DEVICE_IMAGE = 108;

        private Handler mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                Log.d(TAG, "[handleMessage]what = " + what);
                switch (what) {
                case START_BG_ANIMATOR_FLAG:
                    Log.d(TAG, "handleMessage start anim");
                    mBlinkAnim.start();
                    break;

                case STOP_BG_ANIMATOR_FLAG:
                    Log.d(TAG, "handleMessage end anim");
                    mBlinkAnim.end();
                    break;

                case UPDATE_DEVICE_NAME_FLAG:
                    String name = (String) msg.obj;
                    mName.setText(name);
                    break;

                case START_CONNECTING_FLAG:
                    mLoadingAnim.start();
                    break;

                case STOP_CONNECTING_FLAG:
                    mLoadingAnim.stop();
                    mLoadingIcon.setVisible(false);
                    break;

                case SET_ALERT_VISIBLE_FLAG:
                    mAlert.setVisible(true);
                    break;

                case SET_ALERT_INVISIBLE_FLAG:
                    mAlert.setVisible(false);
                    break;

                case UPDATE_ALERT_POSITION_FLAG:
                    if (mAlert.getVisible()) {
                        if (mRightAlertPoint != null && mLeftAlertPoint != null) {
                            if (mCurrAnimIndex == 0 || mCurrAnimIndex == 1) {
                                mAlert.setPosition(mRightAlertPoint);
                            } else {
                                mAlert.setPosition(mLeftAlertPoint);
                            }
                        } else {
                            Log.d(TAG, "[Error]: UPDATE_ALERT_POSITION_FLAG: visible but alert " +
                                        "position is null");
                        }
                    }
                    break;

                case UPDATE_DEVICE_IMAGE:
                    if (msg.obj instanceof Bitmap) {
                        Bitmap map = (Bitmap) msg.obj;
                        mImage.setImageFromBitmap(map);
                        if (mImageDimen != null) {
                            mImage.setSize(mImageDimen); // Workaroud ALPS01897361
                        }
                        Log.d(TAG, "UPDATE_DEVICE_IMAGE: (" +
                                map.getWidth() + ", " + map.getHeight() + ")");

                        if (mPrevBitmap != null && !mPrevBitmap.isRecycled()) {
                            mPrevBitmap.recycle();
                            mPrevBitmap = null;
                        }

                        mPrevBitmap = map;
                    } else {
                        Log.d(TAG, "handleMessage wrong object");
                    }
                    break;

                    default:
                        Log.d(TAG, "handleMessage wrong id");
                        break;
                }
            }

        };

        public void setLoading(boolean show) {
            Message msg = mHandler.obtainMessage();
            if (show) {
                msg.what = START_CONNECTING_FLAG;
            } else {
                msg.what = STOP_CONNECTING_FLAG;
            }
            mHandler.sendMessage(msg);
        }

        public Container getContainer() {
            return mContainer;
        }

        public int getLocationIndex() {
            return mLocationIndex;
        }

        public int getSignalLevel() {
            return mSignalLevel;
        }
    }
}

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
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.LayoutInflater;
import android.view.ViewGroup;



import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDevice.DeviceAttributeChangeListener;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.provider.BleConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ProgressBar;


import android.util.Size;
import android.graphics.Point;


import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;



public class BleStage2DView extends ViewGroup {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleStage2DView]";

    private static final int MAX_DEVICE_SUPPORT = 16;
    //private static final int DEFAULT_IMG = R.drawable.image_add;

    private Context mContext;
    private Resources mResource;
    private CachedBleDeviceManager mCachedBluetoothLEDeviceManager;
    private float mTextSize;

    private ListViewBaseAdapter mAdapter;


    private static final int MSG_INIT_VIEW = 100;


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_INIT_VIEW:
                    initView();
                    break;
                default:
                    break;
            }
        }
    };

    private DataSetObserver mDataSetObserver =  new DataSetObserver(){
                public void onChanged() {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_INIT_VIEW;
                    mHandler.sendMessage(msg);
                }
            };


    //private int mCurrentSelIndex = 0;

    private float mLastMotionX;
    private boolean mReLayout = false;
    private boolean mCanScroll = false;
    private boolean mIsMoveAction = false;

    private int mInitItemX = 0;
    private int mScreenWidth = 0;
    private int mItemY = 0;
    private int mItemMargin = 0;


    //private OnItemSelectListener mItemSelectListener = null;

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

    // transition
    private boolean mIsClockwise;
    private float mSurfaceWidth;
    private float mAccumProgress;
    private float mValidAccum;
    private int mFocus = 0;
    private int mFocusPause = 0;


    ////private SparseArray<BleDeviceTag> mLocationMap = new SparseArray<BleDeviceTag>();

    // call back listener
    private OnBleEventListener mOnBleEventListener;


    public BleStage2DView(Context context) {
        super(context);
        onCreate(context);
    }

    public BleStage2DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onCreate(context);
    }

    public BleStage2DView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        onCreate(context);
    }


    private void onCreate(Context context) {
        mContext = context;
        mResource = mContext.getResources();
        mTextSize = getTextSize();
        mCachedBluetoothLEDeviceManager = CachedBleDeviceManager.getInstance();

        mAdapter = new ListViewBaseAdapter(null, context);
        this.setAdapter(mAdapter);
        mAdapter.registerDataSetObserver(mDataSetObserver);
    }

    public void onResume() {
        if (mFocusPause > mAdapter.getDeviceSize() - 1) {
            mFocusPause = mAdapter.getDeviceSize() - 1;
        }

        mFocus = mFocusPause;

        for (int focus=0; focus<mAdapter.getCount(); focus++) {

            BleDeviceTag tag = mAdapter.getDeviceTag(focus);

            int locationIndex = getLocationByFocus(focus);

            CachedBleDevice device = CachedBleDeviceManager.getInstance().getCachedDeviceFromDisOrder(locationIndex);
            if (device != null) {
                int state = device.getConnectionState();
                int signal = device.getIntAttribute(CachedBleDevice.DEVICE_DISTANCE_FLAG);
                String name = device.getDeviceName();
                Uri uri = device.getDeviceImageUri();

                if (state == BluetoothGatt.STATE_DISCONNECTED) {
                    tag.setBtIcon(BT_DISCONNECTED);
                    tag.setLoading(false);
                } else if (state == BluetoothGatt.STATE_CONNECTED) {
                    tag.setLoading(false);
                    if (device.isSupportPxpOptional()) {
                        tag.setBtIcon(device.getIntAttribute(
                                CachedBleDevice.DEVICE_DISTANCE_FLAG));
                    } else {
                        tag.setBtIcon(BT_CONNECTED);
                    }
                } else if (state == BluetoothGatt.STATE_CONNECTING) {
                    tag.setLoading(true);
                }


                if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (signal > -1 && signal <= BT_SIGNAL_FULL) {
                        tag.setBtIcon(signal);
                    } else {
                        tag.setBtIcon(BT_CONNECTED);
                    }
                } else {
                    tag.setBtIcon(BT_DISCONNECTED);
                }


                tag.setName(name);
                tag.setImage(uri);
            }
            else {
                mAdapter.removeDevice(focus);
            }

        }


    }

    public void onPause() {
        mFocusPause = mFocus;
        //mAdapter.unregisterDataSetObserver(mDataSetObserver);
    }

    public boolean isFull() {
        if (mAdapter.getDeviceSize() >= MAX_DEVICE_SUPPORT) {
            return true;
        }
        return false;
    }

    private void initView() {
        Log.e(TAG, "initView");

        removeAllViewsInLayout();

        // Loop list.
        for (int i = 0; i < mAdapter.getCount(); i++) {
            //add(mAdapter.getItem(i), i);
            View child = mAdapter.getView(i, null, null);
            this.addView(child);
        }

        setReLayout();
        if (mAdapter.getCount() > 1) {
            setCanScroll(true);
        } else {
            setCanScroll(false);
        }
        if (getChildCount() > 0) {
            if (getChildCount() <= mFocus) {
                setFocus(-1);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int wSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);

        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.getChildAt(i);
            child.measure(wSpec, hSpec);

        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //if (mReLayout) {
            mScreenWidth = r-l;
            int screenHeight = b -t;
            for (int i = 0; i < getChildCount(); i++) {
                View child = this.getChildAt(i);
                child.setVisibility(View.VISIBLE);

                mInitItemX = (mScreenWidth - child.getMeasuredWidth())/2;
                mItemMargin = mScreenWidth - child.getMeasuredWidth()/4 - mInitItemX;
                mItemY = (screenHeight - child.getMeasuredHeight())/2;

                child.layout(mInitItemX + mItemMargin * i, mItemY, mInitItemX + mItemMargin * i + child.getMeasuredWidth(),
                        mItemY + child.getMeasuredHeight());
            }
            scrollView(0);
            for (int i=0; i < mFocus; i++) {
                scrollView(-mItemMargin);
            }
        //    mReLayout = !mReLayout;
        //}
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final float x = ev.getX();

        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mIsMoveAction = false;
            mLastMotionX = x;
            break;
        case MotionEvent.ACTION_MOVE:
            mIsMoveAction = true;
            if (ev.getPointerCount() == 1) {
                int deltaX = 0;
                deltaX = (int) (x - mLastMotionX);
                mLastMotionX = x;

                if (mCanScroll) {
                    if (getChildCount() > 2 || (deltaX < 0 && mFocus == 0) || (deltaX > 0 && mFocus == 1)){
                        scrollView(deltaX);
                    }
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mIsMoveAction) {
                int cur = mFocus;
                View currentSelView = getChildAt(cur);

                if (currentSelView != null) {
                    if (currentSelView.getLeft() < mInitItemX - 5) {
                        cur ++;
                        if (cur > getChildCount() - 1) {
                            cur = 0;
                        }
                    }
                    else if (currentSelView.getLeft() > mInitItemX + 5){
                        cur --;
                        if (cur < 0) {
                            cur = getChildCount() - 1;
                        }
                    }

                    setFocus(cur);
                    currentSelView = getChildAt(cur);
                    scrollView(mInitItemX - currentSelView.getLeft());
                }

            }
            break;
        default:
            return false;
        }
        return true;
    }

    private void scrollView(int deltaX) {
        // Move child view by deltaX.
        moveChildView(deltaX);
        invalidate();
    }

    private void moveChildView(int deltaX) {

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(child.getLeft() + deltaX, child.getTop(),
                    child.getRight() + deltaX, child.getBottom());
        }

        if (getChildCount() > 2) {
            int cur = mFocus;
            int left = (cur - 1 + getChildCount()) % getChildCount();
            int right = (cur + 1) % getChildCount();

            View curView = getChildAt(cur);
            View leftView = getChildAt(left);
            View rightView = getChildAt(right);

            if (leftView != null) {
                leftView.layout(curView.getLeft() - mItemMargin, leftView.getTop(),
                    curView.getRight() - mItemMargin, leftView.getBottom());
            }
            rightView.layout(curView.getLeft() + mItemMargin, rightView.getTop(),
                    curView.getRight() + mItemMargin, rightView.getBottom());
        }

    }

    private void setReLayout() {
        this.mReLayout = true;
    }

    private void setCanScroll(boolean canScroll) {
        this.mCanScroll = canScroll;
    }

    public void setAdapter(ListViewBaseAdapter adapter) {
        mAdapter = adapter;
        initView();

    }

    public int getFocus() {
        return mFocus;
    }

    public void setFocus(int pos) {
        int oldSel = mFocus;
        if (pos < 0) {
            pos = getChildCount() + pos;
        }

        if (pos >= 0) {
            mFocus = pos;
            mReLayout = true;
            invalidate();

            updateTagState();

            if (mOnBleEventListener != null) {
                mOnBleEventListener.onFocus(getLocationByFocus(mFocus));
            }

        }
    }

    private int getLocationByFocus(int focus) {
        BleDeviceTag tag = mAdapter.getDeviceTag(focus);
        if (tag != null) {
            return tag.getLocationIndex();
        }
        return -1;
    }

    private int getFocusByLocation(int location) {

        for (int focus=0; focus<mAdapter.getCount(); focus++) {

            BleDeviceTag tag = mAdapter.getDeviceTag(focus);
            if (tag.getLocationIndex() == location) {
                return focus;
            }
        }

        Log.e(TAG, "getFocusByLocation " + location);
        return -1;
    }

    //public int getSelect(int pos) {
    //  return mFocus;
    //}

    //public void setItemSelectListener(OnItemSelectListener listener) {
    //  mItemSelectListener = listener;
    //}

    //interface OnItemSelectListener {
    //    public void onItemSelect(int position);
    //}

    public boolean addDevice(CachedBleDevice device) {
        if (device == null) {
            Log.w(TAG, "[addDevice]device...");
            return false;
        }

        int locationIndex = device.getDeviceLocationIndex();
        int state = device.getConnectionState();
        int signal = device.getIntAttribute(CachedBleDevice.DEVICE_DISTANCE_FLAG);
        String name = device.getDeviceName();// for test
        Uri uri = device.getDeviceImageUri();

        Log.d(TAG, "[addDevice] state " + state + " signal " + signal);
        if (isValidParams(/*tagId, */name, uri, locationIndex)) {
            //BleDeviceTag tag = mAdapter.getDeviceTag(locationIndex);
            //tag.add(/*tagId, */name, uri, state, signal);
            //tag.setAlarm(device.getBooleanAttribute(
            //        CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG));
            BleDeviceTag tag = mAdapter.addDevice(locationIndex, name, uri, state, signal);
            tag.setAlarm(device.getBooleanAttribute(
                    CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG));
            device.registerAttributeChangeListener(this.mAdapter);
            return true;
        }
        return false;
    }

    public boolean removeDevice(CachedBleDevice device) {
        int focus = getFocusByLocation(device.getDeviceLocationIndex());
        BleDeviceTag tag = mAdapter.getDeviceTag(focus);
        if (tag == null) {
            Log.e(TAG, "[removeDevice]Fail to remove device due to invalid display order");
            return false;
        }
        tag.remove();
        mAdapter.removeDevice(focus);
        device.unregisterAttributeChangeListener(this.mAdapter);
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
        if (mCachedBluetoothLEDeviceManager.getCachedDeviceFromDisOrder(getLocationByFocus(mFocus)) == null) {
            // Remove device only when it is not empty
            if (mAdapter.getDeviceTag(mFocus) != null) {
                if (mAdapter.getDeviceTag(mFocus).getSignalLevel() != BT_NO_ICON) {
                    Log.d(TAG, "[refresh]...");
                    mAdapter.getDeviceTag(mFocus).remove();
                }
            }
        }
    }

//    public void setFocus(int index) {
//        BleDeviceTag tag = mAdapter.getDeviceTag(index);
//        if (tag == null) {
//            Log.e(TAG, "[setFocus]Fail to set focus due to invalid location index");
//        } else {
//            if (mFocus != index) {
//                mFocus = index;
//                setDeviceLocation();
//            }
//        }
//    }

//    public void setDeviceLocation() {
//        BleDeviceTag tag;
//        for (int i = 0; i < mAdapter.getCount(); i++) {
//            tag = mAdapter.getDeviceTag(i);
//            //int pos = (tag.getLocationIndex() - mFocus + sPOS.length) % sPOS.length;
//            //tag.setLocation(pos);
//        }
//    }


    public void setOnBleEventListener(OnBleEventListener listener) {
        mOnBleEventListener = listener;
    }

    private void updateTagState() {
        for (int i = 0; i < mAdapter.getDeviceSize(); i++) {
            // show BT signal only when the tag is in focus
            BleDeviceTag tag = mAdapter.getDeviceTag(i);
            tag.updateSignalVisibility();
        }
    }


    public int getNextLocation() {
        if (mAdapter.getDeviceSize() == 0) {
            return 0;
        }
        else {
            return mAdapter.getDeviceTag(mAdapter.getDeviceSize() - 1).getLocationIndex() + 1;
        }
    }

    //public int getDeviceSize() {
    //    return mAdapter.getDeviceSize();
    //}

    public class BleDeviceTag {
        Bitmap mImageBitmap;
        private int mLocationIndex;
        private int mSignalLevel = -1;

        private ImageView mShadow = null;
        private ImageView mImage = null;
        private ImageView mTextBg = null;
        private ImageView mFrame = null;
        private ImageView mAlert = null;
        private ProgressBar mLoadingIcon = null;
        private ImageView mBtIcon = null;
        private TextView mName = null;

        private String mNameText = "";

        private ValueAnimator mBlinkAnim;
        ////private Animation mLoadingAnim;

        public final int[] mSignalLevelResource = new int[] {
            R.drawable.ic_bt_combine_signal_0,
            R.drawable.ic_bt_combine_signal_1,
            R.drawable.ic_bt_combine_signal_2,
            R.drawable.ic_bt_combine_signal_3,
            R.drawable.bt_bar_connected,
            R.drawable.bt_bar_disconnected,
        };

        public BleDeviceTag(int pos) {
            mLocationIndex = pos;

            // set up loading animation
            ////mLoadingAnim = new PropertyAnimation(mLoadingIcon, "rotation", new Rotation(0, 0, 0),
            ////                                     new Rotation(0, 0, 360)).setDuration(1000).setLoop(true);

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
                    mFrame.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    if (mFrame.getVisibility() == View.VISIBLE) {
                        mFrame.setVisibility(View.INVISIBLE);
                    } else {
                        mFrame.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            });



        }

        public void add(/*int tagId, */String name, Uri uri, int state, int signal) {
            Log.d(TAG, "[BleDeviceTag] add - location index: " + mLocationIndex + " signal: " + signal);

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
            //mImage.setImageResource(DEFAULT_IMG);
            //mName.setText(mResource.getString(R.string.add_device));
            //setBtIcon(BT_NO_ICON);
            //disableVisibility();
            //setAlarm(false);
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
                mImage.setImageResource(Integer.parseInt(uri.getLastPathSegment()));

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
                    //Message msg = mHandler.obtainMessage();
                    //msg.what = UPDATE_DEVICE_IMAGE;
                    //msg.obj = bm;
                    //mHandler.sendMessage(msg);
                    if (mImage != null) {
                        mImage.setImageBitmap(bm);
                    }
                    if (mImageBitmap != null) {
                        mImageBitmap.recycle();
                    }
                    mImageBitmap = bm;



                } catch (NullPointerException e) {
                    Log.e(TAG, "[setImage]Fail to decode bitmap uri");
                    e.printStackTrace();
                }
            }
        }



        public void setName(String name) {
            mNameText = name;

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

            mSignalLevel = level;

            //updateSignalVisibility();
            Message msg = mHandler.obtainMessage();
            msg.what = UPDATE_DEVICE_ICON;
            mHandler.sendMessage(msg);

        }


        public void updateSignalVisibility() {
            Log.d(TAG, "[BleDeviceTag] updateSignalVisibility - mSignalLevel: " + mSignalLevel
                    + ", mFocus: " + mFocus);

            if (mBtIcon != null) {
                switch(mSignalLevel) {
                    case BT_SIGNAL_ZERO:
                    case BT_SIGNAL_LOW:
                    case BT_SIGNAL_MIDDLE:
                    case BT_SIGNAL_FULL:
                        if (getLocationByFocus(mFocus) == mLocationIndex) {
                            mBtIcon.setImageResource(mSignalLevelResource[mSignalLevel]);
                        } else {
                            mBtIcon.setImageResource(mSignalLevelResource[BT_CONNECTED]);
                        }
                        break;

                    case BT_CONNECTED:
                    case BT_DISCONNECTED:
                        mBtIcon.setImageResource(mSignalLevelResource[mSignalLevel]);
                        break;

                    default:
                        break;
                }
            }
            else {
                Log.d(TAG, "[BleDeviceTag] updateSignalVisibility skip");
            }

        }


//        public void setLocation(int pos) {
//            //mContainer.setPosition(sPOS[pos]);
//            //mContainer.setRotation(sROTATION[pos]);
//            //mCurrAnimIndex = pos;
//        }

//        public void setCurrAnimIndex(int pos) {
//            //mCurrAnimIndex = pos;
//        }

        public void disableVisibility() {
            Log.d(TAG, "[disableVisibility]mSignalLevel: " + mSignalLevel);

            if (mSignalLevel >= 0) {
                mBtIcon.setImageResource(mSignalLevelResource[mSignalLevel]);
            }

            mAlert.setVisibility(View.INVISIBLE);
            mLoadingIcon.setVisibility(View.INVISIBLE);
        }

        public void setAlert(boolean visible) {
            Message msg = mHandler.obtainMessage();
            if (visible) {
                msg.what = SET_ALERT_VISIBLE_FLAG;
            } else {
                msg.what = SET_ALERT_INVISIBLE_FLAG;
            }
            mHandler.sendMessage(msg);
            //updateAlertPosition();
        }


        //public void updateAlertPosition() {
            //Message msg = mHandler.obtainMessage();
            //msg.what = UPDATE_ALERT_POSITION_FLAG;
            //mHandler.sendMessage(msg);
        //}

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

        public void setLoading(boolean show) {
            Message msg = mHandler.obtainMessage();
            if (show) {
                msg.what = START_CONNECTING_FLAG;
            } else {
                msg.what = STOP_CONNECTING_FLAG;
            }
            mHandler.sendMessage(msg);
        }


        //public Container getContainer() {
        //    return mContainer;
        //}

        public int getLocationIndex() {
            return mLocationIndex;
        }

        public int getSignalLevel() {
            return mSignalLevel;
        }

        private static final int UPDATE_DEVICE_NAME_FLAG = 100;
        private static final int START_BG_ANIMATOR_FLAG = 101;
        private static final int STOP_BG_ANIMATOR_FLAG = 102;
        private static final int START_CONNECTING_FLAG = 103;
        private static final int STOP_CONNECTING_FLAG = 104;
        private static final int SET_ALERT_VISIBLE_FLAG = 105;
        private static final int SET_ALERT_INVISIBLE_FLAG = 106;
        //private static final int UPDATE_ALERT_POSITION_FLAG = 107;
        private static final int UPDATE_DEVICE_IMAGE = 108;
        private static final int UPDATE_DEVICE_ICON = 109;

        private Handler mHandler = new Handler(Looper.getMainLooper()) {

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
                    if (mName != null) {
                        mName.setText(name);
                    }
                    break;

                case START_CONNECTING_FLAG:
                    mLoadingIcon.setVisibility(View.VISIBLE);
                    break;

                case STOP_CONNECTING_FLAG:
                    mLoadingIcon.setVisibility(View.INVISIBLE);
                    break;

                case SET_ALERT_VISIBLE_FLAG:
                    mAlert.setVisibility(View.VISIBLE);
                    break;

                case SET_ALERT_INVISIBLE_FLAG:
                    mAlert.setVisibility(View.INVISIBLE);
                    break;

                //case UPDATE_ALERT_POSITION_FLAG:
                    //if (mAlert.getVisibility() == View.VISIBLE) {
                        //if (mRightAlertPoint != null && mLeftAlertPoint != null) {
                        //    if (mCurrAnimIndex == 0 || mCurrAnimIndex == 1) {
                        //        mAlert.setPosition(mRightAlertPoint);
                        //    } else {
                        //        mAlert.setPosition(mLeftAlertPoint);
                        //    }
                        //} else {
                        //    Log.d(TAG, "[Error]: UPDATE_ALERT_POSITION_FLAG: visible but alert " +
                        //                "position is null");
                        //}
                    //}
                    //break;

                case UPDATE_DEVICE_IMAGE:

                    //Bitmap map = (Bitmap) msg.obj;
                    mImage.setImageBitmap(mImageBitmap);
                    //if (mImageBitmap != null) {
                    //  mImageBitmap.recycle();
                    //}
                    //mImageBitmap = map;
                    Log.d(TAG, "UPDATE_DEVICE_IMAGE: (" +
                            mImageBitmap.getWidth() + ", " + mImageBitmap.getHeight() + ")");

                    break;

                case UPDATE_DEVICE_ICON:
                    updateSignalVisibility();
                    break;

                default:
                    Log.d(TAG, "handleMessage wrong id");
                    break;
                }
            }

        };


    }





    public class ListViewBaseAdapter extends BaseAdapter
        implements DeviceAttributeChangeListener {

        private ArrayList<BleDeviceTag> mList;
        Context mContext;

        public ListViewBaseAdapter(ArrayList<BleDeviceTag> list,
                Context context) {
            if (list == null) {
                list = new ArrayList<BleDeviceTag>();
            }
            this.mList = list;
            mContext = context;
        }

        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int whichAttribute) {
            BleDeviceTag tag = mAdapter.getDeviceTag(getFocusByLocation(device.getDeviceLocationIndex()));
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
                    } else if (state == BluetoothGatt.STATE_CONNECTING) {
                        Log.d(TAG, "onAttributeChanged: STATE_CONNECTING");
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





        public BleDeviceTag addDevice(int pos, String name, Uri uri, int state, int signal) {
            BleDeviceTag tag = new BleDeviceTag(pos);
            tag.add(/*tagId, */name, uri, state, signal);

            mList.add(tag);
            this.notifyDataSetChanged();
            return tag;
        }

        public void removeDevice(int pos) {
            mList.remove(pos);
            this.notifyDataSetChanged();
        }

        public int getDeviceSize() {
            return mList.size();
        }

        public BleDeviceTag getDeviceTag(int pos) {
            if (mList.size() > pos && pos >= 0) {
                return mList.get(pos);
            }
            return null;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Log.d(TAG, "getView: " + position);

            BleDeviceTag holder = null;
            if (convertView == null) {
                holder = getDeviceTag(position);
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listview_device_tag, null);

                if (holder != null && convertView != null) {
                    holder.mShadow = (ImageView)convertView.findViewById(R.id.tag_device_shadow);
                    holder.mImage = (ImageView)convertView.findViewById(R.id.tag_device_image);
                    holder.mTextBg = (ImageView)convertView.findViewById(R.id.tag_device_textbg);
                    holder.mFrame = (ImageView)convertView.findViewById(R.id.tag_device_frame);
                    holder.mAlert = (ImageView)convertView.findViewById(R.id.tag_device_alert);
                    holder.mLoadingIcon = (ProgressBar)convertView.findViewById(R.id.tag_device_loading_icon);
                    holder.mBtIcon = (ImageView)convertView.findViewById(R.id.tag_device_bt_icon);
                    holder.mName = (TextView)convertView.findViewById(R.id.tag_device_name);
                    convertView.setTag(holder);
                }


            }else {
                holder = (BleDeviceTag)convertView.getTag();
            }

            if (holder != null) {
                holder.mName.setText(holder.mNameText);
                holder.updateSignalVisibility();
                if (holder.mImageBitmap != null) {
                    holder.mImage.setImageBitmap(holder.mImageBitmap);
                }

            }

            return convertView;
        }

    }



}


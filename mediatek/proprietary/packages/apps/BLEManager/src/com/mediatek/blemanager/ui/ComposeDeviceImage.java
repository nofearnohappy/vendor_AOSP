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

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.provider.BleConstants;


public class ComposeDeviceImage extends RelativeLayout {
    private static final String TAG = BleConstants.COMMON_TAG + "ComposeDeviceImage";

    private ImageView mDeviceImageView;
    private ImageView mBackGroundImageView;
    private ImageView mConnectionStateImageView;
    
    private TextView mDeviceNameTextView;

    private int mDeviceSignal;
    private int mDeviceConnectionState;
    private String mDeviceName;
    private boolean mIsAlertState;

//    private Handler mHandler;
    private Runnable mRunnable;
    private Uri mDeviceImageUri;
    
    public ComposeDeviceImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.find_all_compose_device_image, this, true);
    }

    public ComposeDeviceImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ComposeDeviceImage(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDeviceImageView = (ImageView) this.findViewById(R.id.compose_device_image);
        mBackGroundImageView = (ImageView) this.findViewById(R.id.compose_background_image);
        mConnectionStateImageView = (ImageView) this.findViewById(R.id.compose_state_image);
        mDeviceNameTextView = (TextView) this.findViewById(R.id.device_name_text_view);

        updateDeviceImage();
        updateDeviceName();
        updateDeviceStateImage();
    }
    
    public void setDeviceImage(Uri uri) {
        if (uri == null) {
            Log.d(TAG, "[setDeviceImage] uri is null!!");
            return;
        }
        if (mDeviceImageUri == uri) {
            Log.d(TAG, "[setDeviceImage] uri is same as mDeviceImageUri!!");
            return;
        }
        mDeviceImageUri = uri;
        updateDeviceImage();
    }

    public void setDeviceName(String name) {
        if (name == null || name.trim().length() == 0) {
            Log.d(TAG, "[setDeviceName]name = " + name);
            return;
        }
        if (name.equals(mDeviceName)) {
            return;
        }
        mDeviceName = name;
        updateDeviceName();
    }

    public void setDeviceConnectionState(int state) {
        if (mDeviceConnectionState == state) {
            return;
        }
        mDeviceConnectionState = state;
        updateDeviceStateImage();
        updateDeviceImage();
    }

    public void setDeviceSignal(int distance) {
        if (mDeviceSignal == distance) {
            return;
        }
        mDeviceSignal = distance;
        updateDeviceStateImage();
    }

    public void setDeviceAlertState(Handler handler, boolean state) {
        Log.i(TAG, "[setDeviceAlertState]state = " + state);
        if (handler == null) {
            Log.w(TAG, "[setDeviceAlertState]handle is null,return!");
            return;
        }
        if (mIsAlertState == state) {
            Log.i(TAG, "[setDeviceAlertState]mIsAlertState = state,return,state = " + state);
            return;
        }
        mIsAlertState = state;
        flashingBg();
    }
    
    private void updateDeviceImage() {
        if (mDeviceImageView != null) {
            Log.d(TAG, "[updateDeviceImage]mDeviceImageUri = " + mDeviceImageUri);
            mDeviceImageView.setImageURI(mDeviceImageUri);
            if (mDeviceConnectionState == BluetoothGatt.STATE_DISCONNECTED) {
                mDeviceImageView.setAlpha((float) 125.0);
            } else if (mDeviceConnectionState == BluetoothGatt.STATE_CONNECTED) {
                mDeviceImageView.setAlpha((float) 255);
            }
        }
    }

    private void updateDeviceName() {
        if (mDeviceNameTextView != null) {
            mDeviceNameTextView.setText(mDeviceName);
        }
    }

    private void updateDeviceStateImage() {
        int resId = Integer.MAX_VALUE;
        if (mDeviceConnectionState == BluetoothGatt.STATE_DISCONNECTED) {
            resId = R.drawable.bt_bar_disconnected;
        } else if (mDeviceConnectionState == BluetoothGatt.STATE_CONNECTED) {
            if (mDeviceSignal == CachedBleDevice.PXP_DISTANCE_FAR) {
                resId = R.drawable.ic_bt_combine_signal_1;
            } else if (mDeviceSignal == CachedBleDevice.PXP_DISTANCE_MIDDLE) {
                resId = R.drawable.ic_bt_combine_signal_2;
            } else if (mDeviceSignal == CachedBleDevice.PXP_DISTANCE_NEAR) {
                resId = R.drawable.ic_bt_combine_signal_3;
            } else if (mDeviceSignal == CachedBleDevice.PXP_DISTANCE_NO_SIGNAL) {
                resId = R.drawable.ic_bt_combine_signal_0;
            } else {
                resId = R.drawable.bt_bar_connected;
            }
        }
        if (mConnectionStateImageView != null) {
            mConnectionStateImageView.setImageResource(resId);
        }
    }

    private Handler mPostHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
            case 1:
                int curr = mBackGroundImageView.getVisibility();
                if (curr == View.VISIBLE) {
                    Log.d(TAG, "[hanleMessage] 1,set INVISIBLE");
                    mBackGroundImageView.setVisibility(View.INVISIBLE);
                } else if (curr == View.INVISIBLE) {
                    Log.d(TAG, "[hanleMessage] 1,set VISIBLE");
                    mBackGroundImageView.setVisibility(View.VISIBLE);
                } else {
                    mBackGroundImageView.setVisibility(View.INVISIBLE);
                }
                break;
                
            case 2:
                Log.d(TAG, "[hanleMessage] 2, set INVISIBLE");
                mBackGroundImageView.setVisibility(View.INVISIBLE);
                break;
                
            default:
                    break;
            }
        }

    };

    private void flashingBg() {
        Log.i(TAG, "[flashingBg]...");
        if (mRunnable == null) {
            Log.i(TAG, "[flashingBg] new thread.");
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    while (mIsAlertState) {
                        try {
                            Thread.sleep(400);
                            Message msg = mPostHandler.obtainMessage();
                            msg.what = 1;
                            mPostHandler.sendMessage(msg);
                        } catch (InterruptedException ex) {
                            Log.e(TAG,"[flashingBg]ex:" + ex.toString());
                            mBackGroundImageView.setVisibility(View.INVISIBLE);
                        }
                    }
                    Message msg = mPostHandler.obtainMessage();
                    msg.what = 2;
                    mPostHandler.sendMessage(msg);
                    mRunnable = null;
                    Log.i(TAG, "[flashingBg]thread exit run.");
                }

            };
            new Thread(mRunnable).start();
        }
    }
}

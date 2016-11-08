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

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleFindMeProfile;

import java.util.ArrayList;

public class FindAllActivity extends Activity {
    private static final String TAG = BleConstants.COMMON_TAG + "-FindAllActivity";

    private boolean mIsClear = false;
    private boolean mIsAlertingState = false;

    private GridView mDeviceListGridView;
    private ImageView mAlertButton;

    private LayoutInflater mInflater;

    private ArrayList<CachedBleDevice> mSupportFmpDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    private LocalBleManager mLocalBleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "[onCreate]...");
        if (!checkFmpDevice()) {
            Log.w(TAG, "[onCreate]No FMP devices founded,finish activity.");
            finish();
            Toast.makeText(this, R.string.no_found, Toast.LENGTH_SHORT).show();
            return;
        }

        this.setContentView(R.layout.find_all_activity_layout);
        mLocalBleManager = LocalBleManager.getInstance(this);

        mDeviceListGridView = (GridView) this.findViewById(R.id.device_grid_list);
        mAlertButton = (ImageView) this.findViewById(R.id.find_all_alert_btn);

        mInflater = LayoutInflater.from(this);
        mDeviceListAdapter = new DeviceListAdapter();
        mDeviceListGridView.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "[onStart]...");
        initDeviceList();

        if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() != 0) {
            for (CachedBleDevice device : mSupportFmpDeviceList) {
                boolean curr = device
                        .getBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG);
                if (device.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
                    if (!curr) {
                        Log.d(TAG, "[onStart] begin to find device : " + device.getDeviceName());
                        mLocalBleManager.findTargetDevice(
                                BleFindMeProfile.ALERT_LEVEL_HIGH, device.getDevice());
                        device.setBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG,
                                !curr);
                    }
                }
            }
        }

        mAlertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // do find action
                if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() != 0) {
                    for (CachedBleDevice device : mSupportFmpDeviceList) {
                        Log.d(TAG, "[onClick]mIsAlertingState : " + mIsAlertingState);
                        if (mIsAlertingState) {
                            mLocalBleManager.findTargetDevice(
                                    BleFindMeProfile.ALERT_LEVEL_NO, device.getDevice());
                        } else {
                            mLocalBleManager.findTargetDevice(
                                    BleFindMeProfile.ALERT_LEVEL_HIGH, device.getDevice());
                        }
                    }
                }
                updateAlertButtonState();
            }
        });

        mDeviceListGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                CachedBleDevice cacheDevice = mSupportFmpDeviceList.get(arg2);
                if (cacheDevice != null) {
                    if (cacheDevice.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
                        boolean currentState = cacheDevice
                                .getBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG);
                        Log.d(TAG, "[onItemClick]currentState : " + currentState);
                        if (currentState) {
                            mLocalBleManager.findTargetDevice(
                                    BleFindMeProfile.ALERT_LEVEL_NO, cacheDevice.getDevice());
                        } else {
                            mLocalBleManager.findTargetDevice(
                                    BleFindMeProfile.ALERT_LEVEL_HIGH, cacheDevice.getDevice());
                        }
                    }
                }
                updateAlertButtonState();
            }

        });
        updateAlertButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "[onStop]...");
        if (mSupportFmpDeviceList != null) {
            if (mSupportFmpDeviceList.size() != 0) {
                for (CachedBleDevice device : mSupportFmpDeviceList) {
                    device.unregisterAttributeChangeListener(mDeviceAttributeChangeListener);
                }
            }
            mSupportFmpDeviceList.clear();
            mDeviceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "[onPause]...");
        // used to clear back ground thread, which used to flashing the
        // background image.
        mIsClear = true;
        mDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "[onResume]...");
        // resume the clear action, which to avoid that if the activity is
        // covered by dialog or notification.can keep flashing.
        mIsClear = false;
        mDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.i(TAG, "[onOptionsItemSelected]click home key,finish activity.");
            finish();
            break;
        default:
            break;
        }
        return true;
    }

    private void updateAlertButtonState() {
        if (mSupportFmpDeviceList == null || mSupportFmpDeviceList.size() == 0) {
            Log.w(TAG, "[updateAlertButtonState] mSupportFmpDeviceList invalid = "
                    + mSupportFmpDeviceList);
            return;
        }
        int connectedNum = 0;
        int alertingNum = 0;
        for (CachedBleDevice cachedDevice : mSupportFmpDeviceList) {
            if (cachedDevice.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
                connectedNum++;
            }
            if (cachedDevice.getBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG)) {
                alertingNum++;
            }
        }
        Log.d(TAG, "[updateAlertButtonState]connectedNum = " + connectedNum + ",alertingNum= "
                + alertingNum);
        if (connectedNum == 0) {
            this.mAlertButton.setEnabled(false);
            this.mAlertButton.setImageResource(R.drawable.bt_find_disable);
            mIsAlertingState = false;
        } else {
            if (alertingNum == 0) {
                this.mAlertButton.setEnabled(true);
                this.mAlertButton.setImageResource(R.drawable.bt_find_normal);
                mIsAlertingState = false;
            } else {
                this.mAlertButton.setEnabled(true);
                this.mAlertButton.setImageResource(R.drawable.bt_find_pressed);
                mIsAlertingState = true;
            }
        }
    }

    /**
     * check if any device support fmp profile, if the stored device contains at
     * least one will return true, otherwise return false,then finish the
     * activity.
     *
     * @return
     */
    private boolean checkFmpDevice() {
        mSupportFmpDeviceList = CachedBleDeviceManager.getInstance().getFmpDevices();
        if (mSupportFmpDeviceList.size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * register the device attribute change listener for each device
     */
    private void initDeviceList() {
        if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() != 0) {
            for (CachedBleDevice device : mSupportFmpDeviceList) {
                device.registerAttributeChangeListener(mDeviceAttributeChangeListener);
            }
        }
    }

    /**
     * handle the device attribute changed action
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.d(TAG, "[handleMessage]what : " + what);
            switch (what) {
            case CachedBleDevice.DEVICE_NAME_ATTRIBUTE_FLAG:
            case CachedBleDevice.DEVICE_CONNECTION_STATE_CHANGE_FLAG:
            case CachedBleDevice.DEVICE_DISTANCE_FLAG:
            case CachedBleDevice.DEVICE_IMAGE_ATTRIBUTE_FLAG:
            case CachedBleDevice.DEVICE_FMP_STATE_FLAG:
                mDeviceListAdapter.notifyDataSetChanged();
                break;

            default:
                break;
            }
            updateAlertButtonState();
        }

    };

    private CachedBleDevice.DeviceAttributeChangeListener mDeviceAttributeChangeListener = new CachedBleDevice.DeviceAttributeChangeListener() {

        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int which) {
            Log.d(TAG, "[onDeviceAttributeChange] disOrder : " + device.getDeviceLocationIndex()
                    + ", whichAttribute : " + which);
            Message msg = mHandler.obtainMessage();
            msg.what = which;
            mHandler.sendMessage(msg);
        }
    };

    private class DeviceListAdapter extends BaseAdapter {
        ArrayList<ViewHolder> mHolders;

        public DeviceListAdapter() {
            Log.d(TAG, "[DeviceListAdapter]new...");
            mHolders = new ArrayList<ViewHolder>();
        }

        @Override
        public int getCount() {
            int size = mSupportFmpDeviceList.size();
            if (size == 0) {
                Log.d(TAG, "[getCount] mHolder.size : " + mHolders.size());
                for (ViewHolder holder : this.mHolders) {
                    holder.mComposeDeviceImage.setDeviceAlertState(mHandler, false);
                }
            }
            Log.d(TAG, "[getCount]size = " + size);
            return size;
        }

        @Override
        public Object getItem(int position) {
            if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() > 0) {
                return mSupportFmpDeviceList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "[getView]mIsClear = " + mIsClear + ",position = " + position);
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.find_all_device_item, null);
                holder = new ViewHolder();
                holder.mComposeDeviceImage = (ComposeDeviceImage) convertView
                        .findViewById(R.id.fa_device_image);
                convertView.setTag(holder);
                Log.d(TAG, "[getView] add holder to mHolders");
                mHolders.add(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                Log.d(TAG, "[getView] get the holder from Tag.");
            }
            holder.mComposeDeviceImage.setDeviceImage(mSupportFmpDeviceList.get(position)
                    .getDeviceImageUri());
            holder.mComposeDeviceImage.setDeviceName(mSupportFmpDeviceList.get(position)
                    .getDeviceName());

            if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() > 0) {
                if (mSupportFmpDeviceList.get(position).isSupportPxpOptional()) {
                    holder.mComposeDeviceImage.setDeviceSignal(mSupportFmpDeviceList.get(position)
                            .getIntAttribute(CachedBleDevice.DEVICE_DISTANCE_FLAG));
                } else {
                    holder.mComposeDeviceImage.setDeviceSignal(Integer.MAX_VALUE);
                }
            }

            if (mIsClear) {
                holder.mComposeDeviceImage.setDeviceAlertState(mHandler, false);
            } else {
                holder.mComposeDeviceImage.setDeviceAlertState(
                        mHandler,
                        mSupportFmpDeviceList.get(position).getBooleanAttribute(
                                CachedBleDevice.DEVICE_FMP_STATE_FLAG));
            }

            holder.mComposeDeviceImage.setDeviceConnectionState(mSupportFmpDeviceList.get(position)
                    .getConnectionState());

            if (mSupportFmpDeviceList != null && mSupportFmpDeviceList.size() > 0) {
                if (mSupportFmpDeviceList.get(position).getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
                    holder.mComposeDeviceImage.setEnabled(true);
                    holder.mComposeDeviceImage.setAlpha((float) 255);
                } else if (mSupportFmpDeviceList.get(position).getConnectionState() == BluetoothGatt.STATE_DISCONNECTED) {
                    holder.mComposeDeviceImage.setEnabled(false);
                    holder.mComposeDeviceImage.setAlpha((float) 125);
                }
            }

            return convertView;
        }

        private class ViewHolder {
            private ComposeDeviceImage mComposeDeviceImage;
        }
    }
}

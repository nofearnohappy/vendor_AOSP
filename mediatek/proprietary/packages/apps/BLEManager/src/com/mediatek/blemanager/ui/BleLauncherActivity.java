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
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.BluetoothCallback;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleProfileServiceManager;

public class BleLauncherActivity extends Activity {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleLauncherActivity]";

    private boolean mIsTurnOnButtonClicked = false;
    private boolean mIsEnter3DView = false;

    private ImageButton mImageButton;
    private TextView mToastTextView;

    private Context mContext;
    private LocalBleManager mLocalBleManager;
    private CachedBleDeviceManager mCachedBleDeviceManager;

    private ScanAction mScanAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "[onCreate]...");

        mContext = this.getApplicationContext();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            // TODO: use R.string.xxxx
            Log.w(TAG, "[onCreate]adapter is null,finish activity.");
            Toast.makeText(this, "NOT SUPPORT BT", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        if (mCachedBleDeviceManager.getCachedDevicesCopy().size() > 0) {
            Log.i(TAG, "[onCreate]startPairedDeviceListActivity,finish activity.");
            startPairedDeviceListActivity();
            finish();
            return;
        }
        mCachedBleDeviceManager.registerDeviceListChangedListener(mListListener);

        setContentView(R.layout.ble_launcher_activity);
        mImageButton = (ImageButton) this.findViewById(R.id.bluetooth_switch_button);
        mToastTextView = (TextView) this.findViewById(R.id.bluetooth_state_text);
        mScanAction = new ScanAction(this);

        mLocalBleManager = LocalBleManager.getInstance(mContext);
        mLocalBleManager.intialize();
        mLocalBleManager.stopLEScan();
        mLocalBleManager.registerBluetoothAdapterStateCallback(mAdapterCallback);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "[onStart]...");
        super.onStart();
        updateToastTextView();

        mImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // / used to turn bluetooth
                int currentState = mLocalBleManager.getCurrentState();
                Log.i(TAG, "[onStart]currentState = " + currentState);
                if (currentState == BluetoothAdapter.STATE_OFF) {
                    TurnOnBTProgressDialog.show(BleLauncherActivity.this);
                    mIsTurnOnButtonClicked = true;
                } else if (currentState == BluetoothAdapter.STATE_ON) {
                    mScanAction.doScanAction(0);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[onDestroy]...");
        if (mLocalBleManager != null) {
            mLocalBleManager.unregisterAdaterStateCallback(mAdapterCallback);
        }
        if (mCachedBleDeviceManager != null) {
            mCachedBleDeviceManager.unregisterDeviceListChangedListener(mListListener);
        }
        ScanDeviceAlertDialog.dismiss();
        ConnectProgressAlertDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!mIsEnter3DView) {
                if (mLocalBleManager != null) {
                    Log.d(TAG, "[onKeyDown] call to close LocalBluetoothLEManager.");
                    mLocalBleManager.close();
                }
            }
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        Log.i(TAG, "[onOptionsItemSelected]itemId = " + itemId);
        switch (itemId) {
        case 0:
            if (mLocalBleManager != null) {
                mLocalBleManager.setBackgroundMode(true);
            }
            break;

        case 1:
            if (mLocalBleManager != null) {
                mLocalBleManager.setBackgroundMode(false);
            }
            break;

        default:
            break;
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (mLocalBleManager != null) {
            if (mLocalBleManager.getBackgroundMode() == BleProfileServiceManager.STATUS_ENABLED) {
                menu.add(0, 1, 0, R.string.disable_background_service);
            } else {
                menu.add(0, 0, 0, R.string.enable_background_service);
            }
        }
        return true;
    }

    private void startPairedDeviceListActivity() {
        mIsEnter3DView = true;
        Intent intent = new Intent(BleLauncherActivity.this, PairedDeviceListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        this.startActivity(intent);
    }

    private void updateToastTextView() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int state = mLocalBleManager.getCurrentState();
                if (state == BluetoothAdapter.STATE_OFF) {
                    mToastTextView.setText(R.string.tap_to_turn_on_bluetooth);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    mToastTextView.setText(R.string.tap_to_add_device);
                }
            }
        });
    }

    private CachedBleDeviceManager.CachedDeviceListChangedListener mListListener = new CachedBleDeviceManager.CachedDeviceListChangedListener() {

        @Override
        public void onDeviceRemoved(CachedBleDevice device) {

        }

        @Override
        public void onDeviceAdded(CachedBleDevice device) {
            Log.i(TAG, "[onDeviceAdded]star tPairedDeviceListActivity.");
            BleLauncherActivity.this.finish();
            startPairedDeviceListActivity();
        }
    };

    private BluetoothCallback.BluetoothAdapterState mAdapterCallback = new BluetoothCallback.BluetoothAdapterState() {

        @Override
        public void onBluetoothStateChanged(int state) {
            if (state == BluetoothAdapter.STATE_ON) {
                TurnOnBTProgressDialog.dismiss();
                if (mIsTurnOnButtonClicked) {
                    Log.d(TAG, "[onBluetoothStateChanged] start scan action");
                    if (mScanAction != null) {
                        mScanAction.doScanAction(0);
                    }
                }
            } else if (state == BluetoothAdapter.STATE_OFF) {
                mIsTurnOnButtonClicked = false;
            }
            updateToastTextView();
        }

        @Override
        public void onBluetoothScanningStateChanged(boolean started) {

        }
    };
}

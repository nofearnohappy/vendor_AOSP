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

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;


import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.BluetoothCallback;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleFindMeProfile;
import com.mediatek.bluetooth.BleProfileServiceManager;

import java.util.ArrayList;

public class PairedDeviceListActivity extends PreferenceActivity {
    private static final String TAG = BleConstants.COMMON_TAG + "[PairedDeviceListActivity]";

    private static final String ALERT_SETTING_PREFERENCE = "alert_set_preference";
    private static final String DEVICE_MANAER_PREFERENCE = "device_manager_preference";

    private static final String SETTING_EXTRA = "current_device";

    private static final int START_SCAN = 1;
    private static final int CONNECT_ACTION = 2;
    private static final int DISCONNECT_ACTION = 3;

    private Context mContext;

    private ActivityStarter mAlertSettingStarter;
    private ActivityStarter mDeviceManagerStarter;

    private ImageButton mDeviceFindButton;
    private ImageButton mDeviceConnectButton;
    private NonChangeSwitchPreference mAlertSettingPreference;
    private Preference mDeviceManagerPreference;
    //For2D
    //private BleStageView mStageView;
    private BleStage2DView mStageView;

    private CachedBleDeviceManager mCachedBleDeviceManager;
    private LocalBleManager mLocalBleManager;
    private CachedBleDevice mCachedBleDevice;

    private boolean mDeviceManagerServiceConnected;
    private boolean mPxpServiceConnected;

    private int mCurrentLocationIndex;
    private int mClickLocationIndex;
    private ProgressDialog mProgressDialog;
    private ArrayList<BluetoothDevice> mConnectedDeviceList;

    private ScanAction mScanAction;
    private ConnectAction mConnectAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.paired_device_activity_preference);
        this.setContentView(R.layout.paired_device_activity);

        mAlertSettingPreference = (NonChangeSwitchPreference) findPreference(ALERT_SETTING_PREFERENCE);
        mDeviceManagerPreference = findPreference(DEVICE_MANAER_PREFERENCE);

        mContext = this;
        mDeviceFindButton = (ImageButton) this.findViewById(R.id.btn_find_me);
        mDeviceConnectButton = (ImageButton) this.findViewById(R.id.btn_connect);
        //mStageView = (BleStageView) this.findViewById(R.id.ble_stage_view);
        //M: for2D
        mStageView = (BleStage2DView) this.findViewById(R.id.ble_stage_view);

        mConnectedDeviceList = new ArrayList<BluetoothDevice>();
        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        int size = mCachedBleDeviceManager.getCachedDevicesCopy().size();
        Log.i(TAG, "[onCreate] devicemanager size : " + size);

        mLocalBleManager = LocalBleManager
                .getInstance(this.getApplicationContext());
        Log.i(TAG, "[onCreate]intialize...");
        mLocalBleManager.intialize();
        if (mLocalBleManager.getCurrentState() != BluetoothAdapter.STATE_ON) {
            TurnOnBTProgressDialog.show(this);
        }

        initActivity();
        mConnectAction = new ConnectAction(this);
        mScanAction = new ScanAction(this);

        update3DView(true); //for2D

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[onResume]...");
        super.onResume();
        this.mStageView.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "[onPause]...");
        super.onPause();
        this.mStageView.onPause();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "[onStart]...");
        super.onStart();
        this.mDeviceManagerServiceConnected = mLocalBleManager
                .getServiceConnectionState(LocalBleManager.PROFILE_DEVICE_MANAGER_SERVICE_ID);
        this.mPxpServiceConnected = mLocalBleManager
                .getServiceConnectionState(LocalBleManager.PROFILE_PXP_ID);

        Log.d(TAG, "[onStart] mDeviceManagerServiceConnected : " + mDeviceManagerServiceConnected);
        Log.d(TAG, "[onStart] mPxpServiceConnected : " + mPxpServiceConnected);

        mLocalBleManager.registerBluetoothAdapterStateCallback(mAdapterStateCallback);
        mCachedBleDeviceManager
                .registerDeviceListChangedListener(mCachedDeviceListChangedListener);
        mLocalBleManager.registerServiceConnectionListener(mServiceConnectionListener);
        for (CachedBleDevice device : mCachedBleDeviceManager
                .getCachedDevicesCopy()) {
            if (device != null) {
                device.registerAttributeChangeListener(mDeviceAttributeListener);
            }
        }
        //update3DView(true);//for2D
        updateActivityState();

    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[onStop]...");
        super.onStop();

        //update3DView(false);//for2D
        mLocalBleManager.unregisterAdaterStateCallback(mAdapterStateCallback);
        mCachedBleDeviceManager
                .unregisterDeviceListChangedListener(mCachedDeviceListChangedListener);
        mLocalBleManager.unregisterServiceConnectionListener(mServiceConnectionListener);
        for (CachedBleDevice device : mCachedBleDeviceManager
                .getCachedDevicesCopy()) {
            if (device != null) {
                device.unregisterAttributeChangeListener(mDeviceAttributeListener);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (mLocalBleManager != null) {
                Log.d(TAG, "[onKeyDown] call to close LocalBluetoothLEManager");
                mLocalBleManager.close();
            }
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();

        MenuItem item = menu.add(0, 0, 0, R.string.find_all_text);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        //for2D
        item = menu.add(0, 3, 0, "+");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        //

        if (mLocalBleManager != null) {
            if (mLocalBleManager.getBackgroundMode() == BleProfileServiceManager.STATUS_ENABLED) {
                menu.add(0, 2, 0, R.string.disable_background_service);
            } else {
                menu.add(0, 1, 0, R.string.enable_background_service);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        Log.i(TAG, "[onOptionsItemSelected]itemId = " + itemId);
        switch (itemId) {
        case 0:
            if (mLocalBleManager.getCurrentState() != BluetoothAdapter.STATE_ON) {
                showToast(1);
                break;
            }
            if (!mLocalBleManager
                    .getServiceConnectionState(LocalBleManager.PROFILE_DEVICE_MANAGER_SERVICE_ID)) {
                showToast(2);
                break;
            }
            Intent intent = new Intent(this, FindAllActivity.class);
            startActivity(intent);
            break;

        case 1:
            if (mLocalBleManager != null) {
                Log.d(TAG, "[onOptionsItemSelected] set background mode to be true");
                mLocalBleManager.setBackgroundMode(true);
            }
            break;

        case 2:
            if (mLocalBleManager != null) {
                Log.d(TAG, "[onOptionsItemSelected] set background mode to be false");
                mLocalBleManager.setBackgroundMode(false);
            }
            break;
        // for2D
        case 3:
            {
                if (!mStageView.isFull()) {
                    doScanAction(mStageView.getNextLocation());
                }
            }
            break;
        //
        default:
            break;
        }
        return true;
    }

    /**
     * while enter onStart call this to add device while enter onStop, call this
     * method to remove device
     *
     * @param load
     */
    private void update3DView(final boolean load) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<CachedBleDevice> devicelist = mCachedBleDeviceManager
                        .getCachedDevicesCopy();

                int focus = mStageView.getFocus(); //for2D
                if (load) {
                    for (CachedBleDevice device : devicelist) {
                        Log.d(TAG, "mStageView.addDevice");
                        mStageView.addDevice(device);
                    }
                } else {
                    for (CachedBleDevice device : devicelist) {
                        mStageView.removeDevice(device);
                    }
                }

                mStageView.setFocus(focus); // for2D
            }
        });

    }

    private BluetoothCallback.BluetoothAdapterState mAdapterStateCallback = new BluetoothCallback.BluetoothAdapterState() {

        @Override
        public void onBluetoothStateChanged(int state) {
            Log.d(TAG, "[onBluetoothStateChanged]state = " + state);
            if (state == BluetoothAdapter.STATE_ON) {
                mLocalBleManager.unregisterAdaterStateCallback(mAdapterStateCallback);
                TurnOnBTProgressDialog.dismiss();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                TurnOnBTProgressDialog.dismiss();
                mLocalBleManager.unregisterAdaterStateCallback(mAdapterStateCallback);
            }
        }

        @Override
        public void onBluetoothScanningStateChanged(boolean started) {

        }
    };

    private void initActivity() {
        mDeviceFindButton.setOnClickListener(mFindButtonClickListener);
        mDeviceConnectButton.setOnClickListener(mConnectButtonClickListener);
        mAlertSettingStarter = new ActivityStarter();
        mDeviceManagerStarter = new ActivityStarter();
        mAlertSettingPreference.setOnPreferenceClickListener(mAlertSettingStarter);
        mAlertSettingPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                boolean checked = mAlertSettingPreference.isChecked();
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG, !checked);
                mAlertSettingPreference.setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG));
                return true;
            }
        });
        mDeviceManagerPreference.setOnPreferenceClickListener(mDeviceManagerStarter);

        mStageView.setOnBleEventListener(new OnBleEventListener() {

            @Override
            public void onClick(int locationIndex) {
                //for2D
                if (mCachedBleDevice == null) {
                    PairedDeviceListActivity.this.doScanAction(locationIndex);
                }
                //if (mCachedBleDevice == null) {
                //    if (mLocalBleManager.getCurrentState() == BluetoothAdapter.STATE_ON) {
                //        Log.d(TAG, "[mStageView.onClick] locationIndex : " + locationIndex);
                //        mClickLocationIndex = locationIndex;
                //        mScanAction.doScanAction(mClickLocationIndex);
                //    } else {
                //        Log.d(TAG, "[mStageView.onClick] BT is off");
                //        showToast(1);
                //    }
                //}
            }

            @Override
            public void onFocus(int locationIndex) {
                Log.d(TAG, "[onFocus] locationIndex : " + locationIndex);
                mCurrentLocationIndex = locationIndex;
                updateActivityState();
            }

        });
    }

   /**
    * device connect action
    *
    * click the button(+) do connect action
    *
    * @param locationIndex
    */
    public void doScanAction(int locationIndex) {
        if (mLocalBleManager.getCurrentState() == BluetoothAdapter.STATE_ON) {
            Log.d(TAG, "[mStageView.onClick] locationIndex : " + locationIndex);
            mClickLocationIndex = locationIndex;
            mScanAction.doScanAction(mClickLocationIndex);
        } else {
            Log.d(TAG, "[mStageView.onClick] BT is off");
            showToast(1);
        }
    }

    /**
     * while swip the 3D view, update the UX state
     */
    private void updateActivityState() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "[updateActivityState] mCurrentLocationIndex : " + mCurrentLocationIndex);
                mStageView.refresh();
                mCachedBleDevice = mCachedBleDeviceManager
                        .getCachedDeviceFromDisOrder(mCurrentLocationIndex);
                Log.d(TAG, "[updateActivityState] mCachedDevice : " + mCachedBleDevice);
                // update device manager preference
                if (mCachedBleDevice == null) {
                    mDeviceManagerPreference.setEnabled(false);
                } else {
                    mDeviceManagerPreference.setEnabled(true);
                }
                // update alert setting preference
                if (mCachedBleDevice != null) {
                    if (!mCachedBleDevice.isSupportLinkLost()) {
                        mAlertSettingPreference.setEnabled(false);
                        mAlertSettingPreference.setChecked(false);
                    } else {
                        if (mPxpServiceConnected) {
                            mAlertSettingPreference.setEnabled(true);
                        } else {
                            mAlertSettingPreference.setEnabled(false);
                        }
                        mAlertSettingPreference.setChecked(mCachedBleDevice
                                .getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG));
                    }
                } else {
                    mAlertSettingPreference.setChecked(false);
                    mAlertSettingPreference.setEnabled(false);
                }
                // update device find button & connect button
                if (mCachedBleDevice == null) {
                    mDeviceFindButton.setVisibility(View.GONE);
                    mDeviceConnectButton.setImageResource(R.drawable.bt_connect_disable);
                    mDeviceConnectButton.setVisibility(View.VISIBLE);
                    mDeviceConnectButton.setEnabled(false);
                } else {
                    int connectionState = mCachedBleDevice.getConnectionState();
                    Log.d(TAG, "[updateActivityState] connectionState : " + connectionState);
                    if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                        mDeviceConnectButton.setImageResource(R.drawable.bt_connect_normal);
                        mDeviceConnectButton.setVisibility(View.GONE);
                        mDeviceFindButton.setVisibility(View.VISIBLE);
                        if (mCachedBleDevice.isSupportFmp()) {
                            if (mDeviceManagerServiceConnected) {
                                mDeviceFindButton.setEnabled(true);
                            } else {
                                mDeviceFindButton.setEnabled(false);
                            }
                            if (mCachedBleDevice
                                    .getBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG)) {
                                mDeviceFindButton.setImageResource(R.drawable.bt_find_pressed);
                            } else {
                                mDeviceFindButton.setImageResource(R.drawable.bt_find_normal);
                            }
                        } else {
                            mDeviceFindButton.setImageResource(R.drawable.bt_find_disable);
                            mDeviceFindButton.setEnabled(false);
                        }
                    } else if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                        // TODO should add for do connect action
                        mDeviceConnectButton.setImageResource(R.drawable.bt_connect_normal);
                        mDeviceConnectButton.setEnabled(true);
                        mDeviceConnectButton.setVisibility(View.VISIBLE);
                        mDeviceFindButton.setVisibility(View.GONE);
                    } else if (connectionState == BluetoothGatt.STATE_CONNECTING) {
                        mDeviceConnectButton.setImageResource(R.drawable.bt_connect_pressed);
                        mDeviceConnectButton.setEnabled(true);
                        mDeviceConnectButton.setVisibility(View.VISIBLE);
                        mDeviceFindButton.setVisibility(View.GONE);
                    }
                }
            }
        });

    }

    /**
     * CachedBluetoothLEDeviceManager cached listener, if the cache has been
     * changed remove all cached device in 3D view, and the add all device to 3D
     * view, meanwhile update the activity state with the top-current device
     * configuration
     */
    private CachedBleDeviceManager.CachedDeviceListChangedListener mCachedDeviceListChangedListener = new CachedBleDeviceManager.CachedDeviceListChangedListener() {

        @Override
        public void onDeviceAdded(CachedBleDevice device) {
            Log.d(TAG, "[onDeviceAdded]...");
            device.registerAttributeChangeListener(mDeviceAttributeListener);
            //update3DView(false);
            //update3DView(true);
            mStageView.addDevice(device); // for2D
            updateActivityState();
        }

        @Override
        public void onDeviceRemoved(CachedBleDevice device) {
            Log.d(TAG, "[onDeviceRemoved]...");
            //update3DView(false);
            //update3DView(true);
            mStageView.removeDevice(device); // for2D
            updateActivityState();
        }

    };

    /**
     * Find me button action listener
     *
     * if device is on fmp state, do stop find action if device is not on fmp
     * state, do start find action
     */
    private View.OnClickListener mFindButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // do find device action.
            if (mCachedBleDevice == null) {
                Log.w(TAG, "[mFindButtonClickListener] mCachedDevice is null");
                return;
            }

            boolean currentPxpState = mCachedBleDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_PXP_ALARM_STATE_FLAG);
            boolean currentFmpState = mCachedBleDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG);
            Log.d(TAG, "[mFindButtonClickListener] currentFmpState : " + currentFmpState);
            Log.d(TAG, "[mFindButtonClickListener] currentPxpState : " + currentPxpState);
            if (currentPxpState) {
                // call PXP stop alert API
                mLocalBleManager
                        .stopRemoteDeviceAlert(mCachedBleDevice.getDevice());
                mCachedBleDevice.onDevicePxpAlertStateChange(false);
            } else {
                if (currentFmpState) {
                    mLocalBleManager.findTargetDevice(BleFindMeProfile.ALERT_LEVEL_NO,
                            mCachedBleDevice.getDevice());
                } else {
                    mLocalBleManager.findTargetDevice(BleFindMeProfile.ALERT_LEVEL_HIGH,
                            mCachedBleDevice.getDevice());
                }
            }
            if (mCachedBleDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ALARM_STATE_FLAG)) {
                Log.d(TAG, "[mFindButtonClickListener] set find button to be pressed");
                mDeviceFindButton.setImageResource(R.drawable.bt_find_pressed);
            } else {
                Log.d(TAG, "[mFindButtonClickListener] set find button to be normal");
                mDeviceFindButton.setImageResource(R.drawable.bt_find_normal);
            }
        }
    };

    private void showToast(final int which) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                String str = null;
                if (which == 1) {
                    Toast.makeText(PairedDeviceListActivity.this,
                        R.string.bt_off, Toast.LENGTH_SHORT)
                        .show();
                } else if (which == 2) {
                    // NO NEED TRANSTLATE
                    str = "Device Manager service is not ready, cann't do find action";
                    Toast.makeText(PairedDeviceListActivity.this, str, Toast.LENGTH_SHORT).show();
                }
                // for2D
                else if (which == 3) {
                    str = "Add Device";
                    Toast.makeText(PairedDeviceListActivity.this, str, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * device connect action
     *
     * if device is disconnected, click the button do connect action without
     * progress dialog
     */
    private View.OnClickListener mConnectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCachedBleDevice == null) {
                Log.d(TAG, "[mConnectButtonClickListener] mCachedDevice is null");
                return;
            }

            if (mLocalBleManager.getCurrentState() != BluetoothAdapter.STATE_ON) {
                Log.i(TAG, "[mConnectButtonClickListener] current state is not state on,return.");
                showToast(1);
                return;
            }
            // while the device is connecting state, click the button to cancel
            // the connect action
            // while the device is disconected state, click to do connect action
            int connectState = mCachedBleDevice.getConnectionState();
            Log.d(TAG, "[mConnectButtonClickListener] connectState : " + connectState);
            if (connectState == BluetoothGatt.STATE_CONNECTING) {
                mLocalBleManager.disconnectGattDevice(mCachedBleDevice.getDevice());
            } else if (connectState == BluetoothGatt.STATE_DISCONNECTED) {
                mConnectAction.connect(mCachedBleDevice.getDevice(),
                        mCachedBleDevice.getDeviceLocationIndex(), false);
            }
        }
    };

    private CachedBleDevice.DeviceAttributeChangeListener mDeviceAttributeListener = new CachedBleDevice.DeviceAttributeChangeListener() {

        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int which) {
            Log.d(TAG, "[onDeviceAttributeChange] which : " + which);
            updateActivityState();
        }

    };

    private LocalBleManager.ServiceConnectionListener mServiceConnectionListener = new LocalBleManager.ServiceConnectionListener() {
        public void onServiceConnectionChange(int profileService, int connection) {
            Log.d(TAG, "[mServiceConnectionListener] profileService : " + profileService);
            Log.d(TAG, "[mServiceConnectionListener] connection : " + connection);
            if (profileService == LocalBleManager.PROFILE_DEVICE_MANAGER_SERVICE_ID) {
                if (connection == LocalBleManager.PROFILE_CONNECTED) {
                    mDeviceManagerServiceConnected = true;
                } else if (connection == LocalBleManager.PROFILE_DISCONNECTED) {
                    mDeviceManagerServiceConnected = false;
                }
                updateActivityState();
            } else if (profileService == LocalBleManager.PROFILE_PXP_ID) {
                if (connection == LocalBleManager.PROFILE_CONNECTED) {
                    mPxpServiceConnected = true;
                } else if (connection == LocalBleManager.PROFILE_DISCONNECTED) {
                    mPxpServiceConnected = false;
                }
                updateActivityState();
            }
        }
    };

    private class ActivityStarter implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent;
            String key = preference.getKey();
            Log.d(TAG, "[onPreferenceClick] key : " + key);
            if (ALERT_SETTING_PREFERENCE.equals(key)) {
                intent = new Intent(PairedDeviceListActivity.this, AlertSettingPreference.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(SETTING_EXTRA, mCurrentLocationIndex);
                mContext.startActivity(intent);
            } else if (DEVICE_MANAER_PREFERENCE.equals(key)) {
                intent = new Intent(PairedDeviceListActivity.this, DeviceManagerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(SETTING_EXTRA, mCurrentLocationIndex);
                mContext.startActivity(intent);
            }
            return false;
        }
    }

}

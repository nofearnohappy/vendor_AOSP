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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleManager;
import com.mediatek.bluetooth.BleProfile;
import com.mediatek.bluetooth.BleProfileService;
import com.mediatek.bluetooth.BleProfileService.ProfileServiceListener;
import com.mediatek.bluetooth.BleProximityProfileService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PxpAlertDialogService extends Service {
    private static final String TAG = BleConstants.COMMON_TAG + "[PxpAlertDialogService]";

    private static final int UPDATE_ALERT = 0;
    private static final int MOVE_ALERT_TO_NOTIFICATION = 1;
    private static final int COLUMN_DEVICE_NAME = 0;
    private static final int COLUMN_DEVICE_IMAGE = 1;

    private static final int COLUMN_VIBRATION_ENABLER = 0;
    private static final int COLUMN_RINGTONE_ENABLER = 1;
    private static final int COLUMN_VOLUME = 2;
    private static final int COLUMN_RINGTONE = 3;

    private static final int DEVICE_IMAGE_WIDTH = 510;
    private static final int DEVICE_IMAGE_HEIGHT = 565;

    private static final long[] VIBRATE_PATTERN = new long[] { 500, 500 };
    private static final long DIALOG_TIMEOUT_DURATION = 30000;

    private static final String INTENT_STATE = BleProximityProfileService.EXTRA_STATE;
    public static final String ACTION_LAUNCH_BLE_MANAGER = "com.mediatek.bluetooth.action.LAUNCH_BLE_MANAGER";

    private static final Uri DEVICE_URI = BleConstants.TABLE_UX_URI;
    private static final String DEVICE_ADDRESS_SELECTION = BleConstants.COLUMN_BT_ADDRESS + "=?";
    private static final String[] DEVICE_PROJECTION = new String[] {
            BleConstants.DEVICE_SETTINGS.DEVICE_NAME,
            BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA };

    private static final Uri PXP_URI = BleConstants.TABLE_UX_URI;
    private static final String PXP_ADDRESS_SELECTION = BleConstants.COLUMN_BT_ADDRESS + "=?";
    private static final String[] PXP_PROJECTION = new String[] {
            BleConstants.PXP_CONFIGURATION.VIBRATION_ENABLER,
            BleConstants.PXP_CONFIGURATION.RINGTONE_ENABLER, BleConstants.PXP_CONFIGURATION.VOLUME,
            BleConstants.PXP_CONFIGURATION.RINGTONE };

    private HashMap<String, Integer> mStatusHashMap = new HashMap<String, Integer>();
    private HashMap<String, Bitmap> mBitmapHashMap = new HashMap<String, Bitmap>();
    private ArrayList<String> mDeviceList = new ArrayList<String>();
    private AlertDialog mAlertDialog = null;
    private View mContentView = null;
    private GridView mGridView = null;
    private AlertDialogManager mAlertDialogManager;
    private TelephonyManager mTelephonyManager = null;
    private MediaPlayer mMediaPlayer = null;
    private Vibrator mVibrator = null;
    private BleManager mBleManager;
    private BleProximityProfileService mBleProximityProfileService = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mRingingDeviceStr = null;

    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange:" + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                stopRingAndVib();
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(TAG, "[onCallStateChanged]state = " + state + ",incomingNumber = "
                    + incomingNumber);
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    if (mProximityHandler.hasMessages(MOVE_ALERT_TO_NOTIFICATION)) {
                        mProximityHandler.removeMessages(MOVE_ALERT_TO_NOTIFICATION);
                        mProximityHandler.sendMessageAtFrontOfQueue(mProximityHandler
                                .obtainMessage(MOVE_ALERT_TO_NOTIFICATION));
                    }
                }
            }
        }
    };

    private Handler mProximityHandler = new Handler() {

        public void handleMessage(Message msg) {

            Log.d(TAG, "[handleMessage]msg.what = " + msg.what);
            switch (msg.what) {
            case UPDATE_ALERT:
                updateInfo((String) msg.obj, msg.arg1);
                break;

            case MOVE_ALERT_TO_NOTIFICATION:
                moveAlertToNotification();
                break;

            default:
                break;
            }
        }
    };

    /**
     * which used to communicate with BleProximityProfie background service
     */
    private ProfileServiceListener mProfileServiceListener = new ProfileServiceListener() {

        public void onServiceConnected(int profile, BleProfileService proxy) {
            if (proxy == null) {
                Log.e(TAG, "[mProfileListener] onServiceConnected, proxy is null");
                return;
            }

            Log.d(TAG, "[mProfileListener] onServiceConnected, profile : " + profile);
            if (profile == BleProfile.PXP) {
                if (proxy instanceof BleProximityProfileService) {
                    Log.d(TAG,
                            "[mProfileListener] onServiceConnected, set mBleProximityProfileService");
                    mBleProximityProfileService = (BleProximityProfileService) proxy;
                }
            } else {
                Log.e(TAG, "[mProfileListener] onServiceConnected, profile not match PXP");
            }

        }

        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "[mProfileListener] onServiceDisconnected, profile : " + profile);
            if (profile == BleProfile.PXP) {
                mBleProximityProfileService = null;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "[onCreate]...");
        mAlertDialogManager = new AlertDialogManager();
        mTelephonyManager = (TelephonyManager) (getSystemService(Context.TELEPHONY_SERVICE));
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        initDialog();
        initPxpServeice();
        getContentResolver().registerContentObserver(DEVICE_URI, true, mDeviceNameObserver);
    }

    private void initPxpServeice() {
        BluetoothManager bluetoothManager = null;
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "[initPxpServeice]Unable to initialize BluetoothManager.");
                return;
            }
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        mBleManager = BleManager.getDefaultBleProfileManager();
        if (mBleManager != null) {
            mBleManager.getProfileServiceProxy(this, BleProfile.PXP, mProfileServiceListener);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String address = device.getAddress();
            int state = intent.getIntExtra(INTENT_STATE, 0);
            updateInfo(address, state);
        } else {
            checkAndFinishService();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mDeviceNameObserver);
        if (mBleManager != null) {
            mBleManager.closeProfileServiceProxy(BleProfile.PXP, mBleProximityProfileService);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkAndFinishService() {
        if ((!mProximityHandler.hasMessages(UPDATE_ALERT)) && mStatusHashMap.isEmpty()) {
            try {
                finalize();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void initDialog() {
        Log.d(TAG, "[initDialog]...");

        mContentView = LayoutInflater.from(this).inflate(R.layout.alert_dialog_grid_view, null);
        mGridView = (GridView) mContentView.findViewById(R.id.alert_dialog_grid_view);
        mGridView.setAdapter(mAlertDialogManager);

        OnClickListener buttonListener = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "[initDialog]onClick,which = " + which);
                switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    stopRemoteAlert();
                    break;

                case DialogInterface.BUTTON_POSITIVE:
                    Intent intent = new Intent(ACTION_LAUNCH_BLE_MANAGER);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    PxpAlertDialogService.this.startActivity(intent);
                    break;

                default:
                    break;
                }
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                }
                //stopRemoteAlert();
                mStatusHashMap.clear();
                mDeviceList.clear();
                checkUnremovedBitmap(false);
                mProximityHandler.sendMessageAtFrontOfQueue(mProximityHandler
                        .obtainMessage(UPDATE_ALERT));
            }
        };

        mAlertDialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.ble_manager_alert).setCancelable(false).setView(mContentView)
                .setPositiveButton(R.string.check, buttonListener)
                .setNegativeButton(R.string.dismiss, buttonListener)
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        Log.d(TAG, "[initDialog]onDismiss");
                        stopRingAndVib();
                        if (mProximityHandler.hasMessages(MOVE_ALERT_TO_NOTIFICATION)) {
                            mProximityHandler.removeMessages(MOVE_ALERT_TO_NOTIFICATION);
                        }
                    }
                }).setOnKeyListener(new OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                            return true;
                        }
                        return false;
                    }
                }).create();

        mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mAlertDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    private void stopRemoteAlert() {
        if (mBluetoothAdapter == null || mBleProximityProfileService == null) {
            Log.w(TAG, "[stopRemoteAlert]mBluetoothAdapter =" + mBluetoothAdapter);
            return;
        }
        for (String address : mDeviceList) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device != null) {
                if (!mBleProximityProfileService.stopRemoteAlert(device)) {
                    Log.e(TAG, "[stopRemoteAlert] failed: " + address);
                }
            } else {
                Log.e(TAG, "[stopRemoteRing] device is null: " + address);
            }
        }
    }

    private void updateInfo(String address, int state) {
        Log.d(TAG, "[updateInfo]address: " + address + ",state = " + state);
        boolean createAlert = false;
        if (address != null) {
            if (state == BleProximityProfileService.STATE_NO_ALERT) {
                if (mStatusHashMap.containsKey(address)) {
                    mDeviceList.remove(address);
                    mStatusHashMap.remove(address);
                } else {
                    // do nothing
                    return;
                }
            } else {
                if (mDeviceList.contains(address)) {

                } else {
                    mDeviceList.add(address);
                    createAlert = true;
                }
                mStatusHashMap.put(address, state);
            }
        } else {
            // address == null, only when clicked any button of this Dialog,
            // need refresh the Dialog
            createAlert = true;
        }
        updateAlert(createAlert, address);
        checkAndFinishService();
    }

    private void moveAlertToNotification() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        updateNotification(false, false);
    }

    private void updateNotification(boolean isDialogShowing, boolean silentUpdate) {
        Log.d(TAG, "[updateNotification]isDialogShowing = " + isDialogShowing + ",silentUpdate = "
                + silentUpdate);
        removeNotification();

        if (!mDeviceList.isEmpty() && !isDialogShowing) {
            Resources r = this.getResources();
            String infoString = updateNotificationString();
            Intent intent = new Intent(ACTION_LAUNCH_BLE_MANAGER);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification alertNotification = new Notification.Builder(this)
                    .setContentTitle(r.getString(R.string.ble_manager_alert))
                    .setContentText(infoString).setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();

            if (silentUpdate) {
                alertNotification.defaults |= Notification.DEFAULT_LIGHTS;
            } else {
                alertNotification.defaults |= Notification.DEFAULT_ALL;
            }
            alertNotification.flags |= Notification.FLAG_ONGOING_EVENT;

            NotificationManager nM = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nM.notify(R.string.ble_manager_alert, alertNotification);
        }
    }

    private void removeNotification() {
        Log.d(TAG, "[removeNotification]...");
        NotificationManager nM = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nM.cancel(R.string.ble_manager_alert);
    }

    private void updateAlert(boolean createAlert, String address) {
        boolean isDialogShowing = updateDialog(createAlert, address);
        updateNotification(isDialogShowing, false);
        checkUnremovedBitmap(false);
    }

    private boolean updateDialog(boolean createAlert, String address) {
        boolean isInCalling = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING);
        if (mStatusHashMap.size() > 0) {
            if (mAlertDialog.isShowing() && !isInCalling) {
                mAlertDialog.setTitle(R.string.ble_manager_alert);
                mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.check);
                mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.dismiss);
                mAlertDialogManager.notifyDataSetChanged();
                sendDelayMessage();
                /*
                 * If address != null, the triggering device is in non-normal
                 * status. If address == null, the triggering device is in
                 * normal status
                 */
                if (address != null) {
                    applyRingAndVib(address);
                }
                return true;
            } else if (createAlert && !isInCalling) {
                mAlertDialog.setTitle(R.string.ble_manager_alert);
                mAlertDialog.show();
                mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.check);
                mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.dismiss);
                sendDelayMessage();
                // Normally, createAlert == true, address must be null
                if (address != null) {
                    applyRingAndVib(address);
                }
                return true;
            } else {
                if (mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                }
                return false;
            }
        } else {
            if (mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
            return false;
        }
    }

    private void sendDelayMessage() {
        if (mProximityHandler.hasMessages(MOVE_ALERT_TO_NOTIFICATION)) {
            mProximityHandler.removeMessages(MOVE_ALERT_TO_NOTIFICATION);
        }
        mProximityHandler.sendMessageDelayed(
                mProximityHandler.obtainMessage(MOVE_ALERT_TO_NOTIFICATION),
                DIALOG_TIMEOUT_DURATION);
    }

    private String updateNotificationString() {
        StringBuilder infoStringBuilder = new StringBuilder();
        Resources r = getResources();
        Integer status;
        if (mDeviceList == null) {
            Log.w(TAG, "[updateNotificationString] mDeviceList is null.");
            return null;
        }
        for (String address : mDeviceList) {
            if (mStatusHashMap != null) {
                status = mStatusHashMap.get(address);
                if (status == null) {
                    Log.e(TAG, "[updateNotificationString]get NULL from mStatusMap for " + address);
                    continue;
                }
            } else {
                return null;
            }

            String[] queryResult = queryDeviceNameAndImage(address);
            if (queryResult == null) {
                continue;
            }
            String deviceName = queryResult[COLUMN_DEVICE_NAME];
            if (deviceName == null) {
                continue;
            }
            Log.d(TAG, "[updateNotificationString] status = " + status);
            switch (status) {
            case BleProximityProfileService.STATE_DISCONNECTED_ALERT:
                if (infoStringBuilder.length() > 0) {
                    infoStringBuilder.append(",\n");
                }
                infoStringBuilder.append("[" + deviceName + "] ");
                infoStringBuilder.append(r.getString(R.string.disconnected));
                break;

            case BleProximityProfileService.STATE_IN_RANGE_ALERT:
                if (infoStringBuilder.length() > 0) {
                    infoStringBuilder.append(",\n");
                }
                infoStringBuilder.append("[" + deviceName + "] ");
                infoStringBuilder.append(r.getString(R.string.in_range));
                break;

            case BleProximityProfileService.STATE_OUT_RANGE_ALERT:
                if (infoStringBuilder.length() > 0) {
                    infoStringBuilder.append(",\n");
                }
                infoStringBuilder.append("[" + deviceName + "] ");
                infoStringBuilder.append(r.getString(R.string.out_of_range));
                break;
            default:
                break;
            }
        }
        return infoStringBuilder.toString();
    }

    private String[] queryDeviceNameAndImage(String address) {
        Log.d(TAG, "[queryDeviceNameAndImage]address: " + address);
        Cursor cursor = this.getContentResolver().query(DEVICE_URI, DEVICE_PROJECTION,
                DEVICE_ADDRESS_SELECTION, new String[] { address }, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(COLUMN_DEVICE_NAME);
                String image = cursor.getString(COLUMN_DEVICE_IMAGE);
                return new String[] { name, image };
            } else {
                Log.w(TAG, "[queryDeviceNameAndImage]get cursor is null: " + address);
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void stopRingAndVib() {
        Log.d(TAG, "[stopRingAndVib]...");
        mRingingDeviceStr = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(mOnAudioFocusChangeListener);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }
    }

    private void applyRingAndVib(String address) {
        Log.d(TAG, "[applyRingAndVib]addree: " + address);
        if (address == null) {
            return;
        }

        if (address != null && !mDeviceList.contains(address)) {
            if (address.equals(mRingingDeviceStr)) {
                // The ringtone is for this device, and this device is normal
                // status
                //stopRingAndVib(); // remove it for ALPS02193026
                return;
            } else {
                // The ringtone is not for this device, just continue ring
                return;
            }
        }
        stopRingAndVib();
        boolean ringToneEnabler = false;
        boolean vibEnabler = false;
        Uri ringtone = null;
        int ringtoneVolume = 0;
        Cursor cursor = getContentResolver().query(PXP_URI, PXP_PROJECTION, PXP_ADDRESS_SELECTION,
                new String[] { address }, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                ringToneEnabler = (cursor.getInt(COLUMN_RINGTONE_ENABLER) == 1);
                vibEnabler = (cursor.getInt(COLUMN_VIBRATION_ENABLER) == 1);
                String ringtoneString = cursor.getString(COLUMN_RINGTONE);
                ringtoneVolume = cursor.getInt(COLUMN_VOLUME);
                ringtone = (ringtoneString == null) ? null : Uri.parse(ringtoneString);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "[applyRingAndVib]ringToneEnabler:" + ringToneEnabler + ", vibEnabler:"
                + vibEnabler);
        if (ringToneEnabler && ringtone != null) {
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(this, ringtone);
                mMediaPlayer.setLooping(true);

                AudioManager aM = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                aM.setStreamVolume(AudioManager.STREAM_ALARM, ringtoneVolume, 0);

                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setOnErrorListener(new OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer player, int what, int extra) {
                        Log.w(TAG, "[applyRingAndVib]Media Player onError:" + what + ",extra = "
                                + extra);
                        stopRingAndVib();
                        return false;
                    }
                });
                mMediaPlayer.prepare();
                aM.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                mMediaPlayer.start();
                mRingingDeviceStr = address;

            } catch (IllegalStateException e) {
                Log.e(TAG, "[applyRingAndVib]Media Player IllegalStateException");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "[applyRingAndVib]Media Player IOException");
                e.printStackTrace();
            }
        }
        if (vibEnabler) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(VIBRATE_PATTERN, 0);
        }
    }

    private Bitmap storeBitmap(String address, Uri uri) {
        if (address != null && uri != null) {
            Bitmap bitmap = mBitmapHashMap.remove(address);
            if (bitmap == null) {
                bitmap = getBitmap(uri);
                if (bitmap != null) {
                    mBitmapHashMap.put(address, bitmap);
                }
            }
            return bitmap;
        } else {
            return null;
        }
    }

    private void removeBitmap(String address) {
        if (address != null) {
            Bitmap bitmap = mBitmapHashMap.remove(address);
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private void checkUnremovedBitmap(boolean removeAll) {
        Set<String> keySet = mBitmapHashMap.keySet();
        for (String key : keySet) {
            if (removeAll) {
                removeBitmap(key);
            } else {
                if (!mDeviceList.contains(key)) {
                    removeBitmap(key);
                }
            }
        }
    }

    private Bitmap getBitmap(Uri uri) {
        if (uri == null) {
            Log.w(TAG, "[getBitmap] uri is null");
            return null;
        }
        // Cursor cursor = mContext.getContentResolver().query(uri,
        // new String[] {BLEConstants.UX.DEVICE_IAMGE_DATA}, null, null, null);
        InputStream is = null;
        InputStream iso = null;
        try {
            is = this.getContentResolver().openInputStream(uri);
            iso = this.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
        if (is == null || iso == null) {
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

    private class AlertDialogManager extends BaseAdapter {
        @Override
        public int getCount() {
            int count = mDeviceList.size();
            if (count == 1) {
                mGridView.setNumColumns(1);
            } else {
                mGridView.setNumColumns(2);
            }

            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DialogViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(PxpAlertDialogService.this).inflate(
                        R.layout.alert_dialog_grid_item, null);

                if (convertView == null) {
                    Log.w(TAG, "[getView]convertView is null,return.");
                    return null;
                }

                holder = new DialogViewHolder();
                holder.mNameText = (TextView) convertView
                        .findViewById(R.id.alert_dialog_device_name_text);
                holder.mStatusText = (TextView) convertView
                        .findViewById(R.id.alert_dialog_device_status_text);
                holder.mImage = (ImageView) convertView
                        .findViewById(R.id.alert_dialog_device_image);
                convertView.setTag(holder);
            } else {
                // view already existed, update holder
                holder = (DialogViewHolder) convertView.getTag();
            }

            // set the resources
            String address = mDeviceList.get(position);
            Integer status = 0;
            if (mStatusHashMap != null) {
                status = mStatusHashMap.get(address);
            }

            String deviceName = null;
            Uri deviceImage = null;

            if (status != null) {
                String[] queryResult = queryDeviceNameAndImage(address);
                if (queryResult == null) {
                    Log.w(TAG, "[getView]queryResult is null,return.");
                    return convertView;//null;
                }
                deviceName = queryResult[COLUMN_DEVICE_NAME];
                String imageString = "file:///" + queryResult[COLUMN_DEVICE_IMAGE];
                deviceImage = (imageString == null) ? null : Uri.parse(imageString);
            } else {
                Log.w(TAG, "[getView]status is null,return.");
                // holder.mStatusText.setText(r.getString(R.string.disconnected));
                return convertView;//null;
            }

            holder.mNameText.setText(deviceName);
            Bitmap bitmap = storeBitmap(address, deviceImage);
            holder.mImage.setImageBitmap(bitmap);

            Resources r = getResources();
            if (status != null) {
                Log.d(TAG, "[getView]status switch = " + status);
                switch (status) {
                case BleProximityProfileService.STATE_IN_RANGE_ALERT:
                    holder.mStatusText.setText(r.getString(R.string.in_range));
                    break;

                case BleProximityProfileService.STATE_OUT_RANGE_ALERT:
                    holder.mStatusText.setText(r.getString(R.string.out_of_range));
                    break;

                case BleProximityProfileService.STATE_DISCONNECTED_ALERT:
                    holder.mStatusText.setText(r.getString(R.string.disconnected));
                    break;

                default:
                    break;
                }
            }

            return convertView;
        }

        class DialogViewHolder {
            TextView mNameText;
            ImageView mImage;
            TextView mStatusText;
        }
    }

    ContentObserver mDeviceNameObserver = new ContentObserver(mProximityHandler) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "[onChange]mDeviceNameObserver");
            if (!mAlertDialog.isShowing()) {
                updateNotification(false, true);
            }
        }
    };
}

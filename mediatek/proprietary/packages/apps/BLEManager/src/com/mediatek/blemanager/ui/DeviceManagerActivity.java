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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;

public class DeviceManagerActivity extends PreferenceActivity {
    private static final String TAG = BleConstants.COMMON_TAG + "[DeviceManagerActivity]";

    private static final String INCOMING_CALL_PREFERENCE = "incoming_call_notification_preference";
    private static final String MISSED_CALL_PREFERENCE = "missed_call_notification_preference";
    private static final String NEW_MESSAGE_PREFERENCE = "new_message_notification_preference";
    private static final String NEW_EMAIL_PREFERENCE = "new_email_notification_preference";
    private static final String CATEGORY_PREFERENCE_KEY = "alert_notification_preference_category";

    private static final int MENU_DISCONNECT_FLAG = 0;
    private static final int MENU_CONNECT_FLAG = 1;
    private static final int MENU_DELETE_FLAG = 2;

    private static final int GET_PIC_FROM_DEFAULT = 0;
    private static final int CAPTURE_PIC_FROM_CAMERA = 1;
    private static final int GET_PIC_FROM_GALLERY = 2;
    private static final int CROP_IMAGE = 10;

    private static final String EXTRA = "current_device";

    // private static final int CONNECT_ACTION = 100;
    private static final int DISCONNECT_ACTION = 101;
    private static final int CONNECT_TIMEOUT = 102;
    private static final int DIALOG_SHOW = 103;
    private static final int DIALOG_DISMISS = 104;
    private static final int SHOW_TOAST = 105;
    private static final int DELETE_DEVICE = 106;

    // private static final int CONNECT_ACTION_TIMEOUT = 30 * 1000; // 30s

    private boolean mAnpServiceConnected;

    // private PreferenceCategory mCategory;
    private CheckBoxPreference mIncomingCallPreference;
    private CheckBoxPreference mMissedCallPreference;
    private CheckBoxPreference mNewMsgPreference;
    private CheckBoxPreference mNewEmailPreference;

    private ImageView mRenameImageView;
    private ImageView mDeviceImageView;
    private TextView mDeviceNameTextView;

    private AlertDialog mRenameDlg;

    private CachedBleDeviceManager mCachedBleDeviceManager;
    private CachedBleDevice mCachedBleDevice;
    private LocalBleManager mLocalBleManager;

    private ConnectAction mConnectAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        int current = intent.getIntExtra(EXTRA, -1);
        Log.i(TAG, "[onCreate]current = " + current);
        if (current == -1) {
            finish();
            return;
        }

        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        mCachedBleDevice = mCachedBleDeviceManager
                .getCachedDeviceFromDisOrder(current);
        if (mCachedBleDevice == null) {
            finish();
            Log.w(TAG, "[onCreate] enter, device is null,finish activity.");
            return;
        }

        // Add preference to activity and set content view for it.
        this.addPreferencesFromResource(R.xml.device_manager_activity_preference);
        this.setContentView(R.layout.device_manager_activity_layout);

        mDeviceImageView = (ImageView) this.findViewById(R.id.dm_top_device_pic);
        mRenameImageView = (ImageView) this.findViewById(R.id.dm_device_rename_image);
        mDeviceNameTextView = (TextView) this.findViewById(R.id.dm_device_name_text_view);

        // mCategory = (PreferenceCategory)
        // this.findPreference(CATEGORY_PREFERENCE_KEY);
        mIncomingCallPreference = (CheckBoxPreference) this
                .findPreference(INCOMING_CALL_PREFERENCE);
        mMissedCallPreference = (CheckBoxPreference) this.findPreference(MISSED_CALL_PREFERENCE);
        mNewMsgPreference = (CheckBoxPreference) this.findPreference(NEW_MESSAGE_PREFERENCE);
        mNewEmailPreference = (CheckBoxPreference) this.findPreference(NEW_EMAIL_PREFERENCE);

        mLocalBleManager = LocalBleManager
                .getInstance(this.getApplicationContext());
        mConnectAction = new ConnectAction(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "[onStop]...");
        mCachedBleDevice.unregisterAttributeChangeListener(mAttributeListener);
        mLocalBleManager.unregisterServiceConnectionListener(mServiceConnectionListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.mAnpServiceConnected = mLocalBleManager
                .getServiceConnectionState(LocalBleManager.PROFILE_ANP_ID);
        Log.i(TAG, "[onStart] mAnpServiceConnected : " + mAnpServiceConnected);

        initActivity();
        initActionBar();
        mCachedBleDevice.registerAttributeChangeListener(mAttributeListener);
        mLocalBleManager.registerServiceConnectionListener(mServiceConnectionListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (mLocalBleManager.getCurrentState() == BluetoothAdapter.STATE_ON) {
            if (mCachedBleDevice != null) {
                int connectionState = mCachedBleDevice.getConnectionState();
                Log.i(TAG, "[onPrepareOptionsMenu] connectionState : " + connectionState);
                if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                    menu.add(0, MENU_DISCONNECT_FLAG, 0, R.string.disconnect_text);
                }
                if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                    menu.add(0, MENU_CONNECT_FLAG, 0, R.string.connect_text);
                }
            }
            menu.add(0, MENU_DELETE_FLAG, 0, R.string.delete_text);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        Log.i(TAG, "[onOptionsItemSelected]itemId = " + itemId);
        switch (itemId) {
        case MENU_DISCONNECT_FLAG:
            // do disconnect action
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setTitle(R.string.disconnect_text);
            builder1.setMessage(R.string.device_disconnect_dialog_message);
            builder1.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO do delete action
                    sendMessageToHandler(DISCONNECT_ACTION, 0);
                }
            });
            builder1.setNegativeButton(R.string.no, null);
            builder1.create().show();
            break;

        case MENU_DELETE_FLAG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_text);
            builder.setMessage(R.string.device_delete_dialog_message);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO do delete action
                    sendMessageToHandler(DELETE_DEVICE, 0);
                }
            });
            builder.setNegativeButton(R.string.no, null);
            builder.create().show();
            break;

        case MENU_CONNECT_FLAG:
            final BluetoothDevice device = mCachedBleDevice.getDevice();
            if (device == null) {
                Log.i(TAG, "[onOptionsItemSelected]device is null,break.");
                break;
            }
            mConnectAction.connect(device, mCachedBleDevice.getDeviceLocationIndex(), true);
            // sendMessageToHandler(CONNECT_ACTION, 0);
            break;

        case android.R.id.home:
            this.finish();
            break;

        default:
            break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = null;
        Log.i(TAG, "[onActivityResult]requestCode = " + requestCode + ",resultCode = " + resultCode);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case GET_PIC_FROM_DEFAULT:
                int id = data.getIntExtra("which", -1);
                // deviceUri = ActivityUtils.getDrawableUri(this, id,
                // mCachedDevice.getDeviceImage().toString());
                bitmap = ActivityUtils.getDrawbleBitmap(this, id);
                break;

            case CAPTURE_PIC_FROM_CAMERA:
                if (data == null) {
                    ActivityUtils.handlePhotoCrop(this, CROP_IMAGE, ActivityUtils.getTempFileUri());
                }
                break;

            case GET_PIC_FROM_GALLERY:
                Uri uri = data.getData();
                ActivityUtils.handlePhotoCrop(this, CROP_IMAGE, uri);
                break;

            case CROP_IMAGE:
                Uri cropUri = data.getData();
                Log.d(TAG, "[onActivityResult] cropUri : " + cropUri);
                bitmap = ActivityUtils.saveImageFromCustom(this, cropUri, mCachedBleDevice
                        .getDeviceImageUri().toString());
                break;

            default:
                break;
            }
            if (bitmap != null) {
                mCachedBleDevice.updateDeviceImage(ActivityUtils
                        .comproseBitmapToByteArray(bitmap));
                updateUi();
            } else {
                Log.d(TAG, "[onActivityResult] bitmap is null");
            }
        }
    }

    private void initActivity() {
        updatePreferenceState();
        mDeviceNameTextView.setText(mCachedBleDevice.getDeviceName());
        mRenameImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameDialog();
            }
        });

        mDeviceImageView.setImageURI(mCachedBleDevice.getDeviceImageUri());
        mDeviceImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceManagerActivity.this);
                builder.setItems(R.array.device_image_chooser_items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityUtils.startImageChooser(DeviceManagerActivity.this, which);
                            }
                        });
                builder.create().show();
            }
        });
    }

    private void initActionBar() {
        ActionBar bar = this.getActionBar();
        bar.setTitle(mCachedBleDevice.getDeviceName());
    }

    private void updatePreferenceState() {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mIncomingCallPreference.setTitle(R.string.incoming_call_notification_text);
                mMissedCallPreference.setTitle(R.string.missed_call_notificaiotn_text);
                mNewMsgPreference.setTitle(R.string.new_message_notification_text);
                mNewEmailPreference.setTitle(R.string.new_email_notification_text);

                mIncomingCallPreference.setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG));
                mMissedCallPreference.setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG));
                mNewMsgPreference.setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG));
                mNewEmailPreference.setChecked(mCachedBleDevice
                        .getBooleanAttribute(CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG));

                if (mAnpServiceConnected) {
                    mIncomingCallPreference.setEnabled(mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_INCOMING_CALL_FLAGE));
                    mMissedCallPreference.setEnabled(mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_MISSED_CALL_FLAGE));
                    mNewMsgPreference.setEnabled(mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_NEW_MESSAGE_FLAGE));
                    mNewEmailPreference.setEnabled(mCachedBleDevice
                            .getBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_NEW_EMAIL_FLAGE));
                } else {
                    mIncomingCallPreference.setEnabled(false);
                    mMissedCallPreference.setEnabled(false);
                    mNewMsgPreference.setEnabled(false);
                    mNewEmailPreference.setEnabled(false);
                }

                mIncomingCallPreference.setOnPreferenceClickListener(mPreferenceClickListener);
                mMissedCallPreference.setOnPreferenceClickListener(mPreferenceClickListener);
                mNewMsgPreference.setOnPreferenceClickListener(mPreferenceClickListener);
                mNewEmailPreference.setOnPreferenceClickListener(mPreferenceClickListener);
            }

        });

    }

    /**
     * Show rename dialog which used to rename the device name.
     */
    private void showRenameDialog() {

        if (mRenameDlg != null && mRenameDlg.isShowing()) {
            mRenameDlg.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editor = new EditText(this);
        final String befText = mDeviceNameTextView.getText().toString();
        editor.setText(befText);
        editor.setSingleLine(true);
        editor.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                if (arg0.toString().trim().length() == 0) {
                    mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
                    mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    return;
                }
                if (arg0.toString().equals(befText)) {
                    mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
                    mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    return;
                }
                mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(true);
                mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

            }

        });
        builder.setView(editor);
        builder.setTitle(R.string.device_name_change_dialog_title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (editor.getText().toString().trim().length() != 0) {
                    if (!editor.getText().toString().equals(befText)) {
                        mCachedBleDevice.setDeviceName(editor.getText().toString());
                        updateUi();
                        arg0.dismiss();
                    }
                }
            }
        });
        mRenameDlg = builder.create();
        mRenameDlg.show();
        mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
        mRenameDlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void updateUi() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceImageView.setImageURI(null);
                mDeviceNameTextView.setText(mCachedBleDevice.getDeviceName());
                mDeviceImageView.setImageURI(mCachedBleDevice.getDeviceImageUri());
                ActionBar bar = DeviceManagerActivity.this.getActionBar();
                bar.setTitle(mCachedBleDevice.getDeviceName());
            }
        });
    }

    private void sendMessageToHandler(int what, long delay) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        mHandler.sendMessageDelayed(msg, delay);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.i(TAG, "[handleMessage]what = " + what);
            switch (what) {
            case DISCONNECT_ACTION:
                mHandler.removeMessages(CONNECT_TIMEOUT);
                Log.d(TAG, "[mHandler] DISCONNECT_ACTION, start to disconnect gatt device");
                mLocalBleManager.disconnectGattDevice(mCachedBleDevice.getDevice());
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_AUTO_CONNECT_FLAG, false);
                break;

            case DIALOG_SHOW:
                ConnectProgressAlertDialog.show(DeviceManagerActivity.this,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                sendMessageToHandler(DISCONNECT_ACTION, 0);
                                sendMessageToHandler(DIALOG_DISMISS, 0);
                            }
                        });
                break;

            case DIALOG_DISMISS:
                ConnectProgressAlertDialog.dismiss();
                break;

            case SHOW_TOAST:
                String str = (String) msg.obj;
                Toast.makeText(DeviceManagerActivity.this, str, Toast.LENGTH_SHORT).show();
                break;

            case DELETE_DEVICE:
                BluetoothDevice device = mCachedBleDevice.getDevice();
                if (device != null) {
                    Log.d(TAG, "[mHandler] DELETE_DEVICE, start to disconnect gatt device");
                    mLocalBleManager.disconnectGattDevice(device);
                    device.removeBond();
                    mCachedBleDeviceManager.removeDevice(mCachedBleDevice);
                    DeviceManagerActivity.this.finish();
                    mHandler.removeMessages(CONNECT_TIMEOUT);
                }
                break;

            default:
                break;
            }
        }

    };

    private LocalBleManager.ServiceConnectionListener mServiceConnectionListener = new LocalBleManager.ServiceConnectionListener() {

        @Override
        public void onServiceConnectionChange(int profileService, int connection) {
            Log.d(TAG, "[onServiceConnectionChange]profileService = " + profileService
                    + ",connection = " + connection);
            if (profileService == LocalBleManager.PROFILE_ANP_ID) {
                if (connection == LocalBleManager.PROFILE_CONNECTED) {
                    mAnpServiceConnected = true;
                } else if (connection == LocalBleManager.PROFILE_DISCONNECTED) {
                    mAnpServiceConnected = false;
                }
                updatePreferenceState();
            }
        }
    };

    private CachedBleDevice.DeviceAttributeChangeListener mAttributeListener = new CachedBleDevice.DeviceAttributeChangeListener() {

        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int which) {
            if (device == null || device.getDevice() == null) {
                Log.w(TAG, "[mAttributeListener] device is null");
                return;
            }
            Log.d(TAG, "[onDeviceAttributeChange]mAttributeListener.");
            updatePreferenceState();
        }

    };

    private Preference.OnPreferenceClickListener mPreferenceClickListener = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            Log.d(TAG, "[onPreferenceClick]key = " + key);
            if (INCOMING_CALL_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG,
                        mIncomingCallPreference.isChecked());
            } else if (MISSED_CALL_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG,
                        mMissedCallPreference.isChecked());
            } else if (NEW_EMAIL_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG,
                        mNewEmailPreference.isChecked());
            } else if (NEW_MESSAGE_PREFERENCE.equals(key)) {
                mCachedBleDevice.setBooleanAttribute(
                        CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG,
                        mNewMsgPreference.isChecked());
            }
            return true;
        }
    };

}

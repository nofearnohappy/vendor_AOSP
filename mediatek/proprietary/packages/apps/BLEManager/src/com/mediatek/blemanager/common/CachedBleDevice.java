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
package com.mediatek.blemanager.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.net.Uri;
import android.util.Log;

import com.mediatek.blemanager.provider.ActivityDbOperator;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetooth.BleProximityProfileService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Used to record remote bluetooth low energy device attribute. Such as device
 * name, device image
 * 
 */
public class CachedBleDevice {
    private static final String TAG = BleConstants.COMMON_TAG + "[CachedBleDevice]";

    // For UX
    public static final int DEVICE_NAME_ATTRIBUTE_FLAG = 1;
    public static final int DEVICE_IMAGE_ATTRIBUTE_FLAG = 2;
    public static final int DEVICE_LOCATION_INDEX_FLAG = 3;
    public static final int DEVICE_FMP_STATE_FLAG = 4;

    public static final int DEVICE_CONNECTION_STATE_CHANGE_FLAG = 5;
    public static final int DEVICE_AUTO_CONNECT_FLAG = 6;

    // For PXP
    public static final int DEVICE_RANGE_VALUE_FLAG = 10;
    public static final int DEVICE_IN_OUT_RANGE_ALERT_FLAG = 11;
    public static final int DEVICE_VOLUME_FLAG = 12;
    public static final int DEVICE_ALERT_SWITCH_ENABLER_FLAG = 13;
    public static final int DEVICE_RANGE_ALERT_ENABLER_FLAG = 14;
    public static final int DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG = 15;
    public static final int DEVICE_RINGTONE_ENABLER_FLAG = 16;
    public static final int DEVICE_VIBRATION_ENABLER_FLAG = 17;
    public static final int DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG = 18;
    public static final int DEVICE_RINGTONE_URI_FLAG = 19;
    public static final int DEVICE_DISTANCE_FLAG = 20;
    public static final int DEVICE_ALERT_STATE_FLAG = 21;
    public static final int DEVICE_SUPPORT_OPTIONAL = 22;
    public static final int DEVICE_RINGTONE_ALARM_STATE_FLAG = 23;
    public static final int DEVICE_PXP_ALARM_STATE_FLAG = 24;

    public static final int DEVICE_SERVICE_LIST_CHANGE_FLAG = 25;

    public static final int DEVICE_CURRENT_TX_POWER_FLAG = 100;

    // For ANS host
    public static final int DEVICE_INCOMING_CALL_ENABLER_FLAG = 30;
    public static final int DEVICE_MISSED_CALL_ENABLER_FLAG = 31;
    public static final int DEVICE_NEW_MESSAGE_ENABLER_FLAG = 32;
    public static final int DEVICE_NEW_EMAIL_ENABLER_FLAG = 33;

    // For ANS remote
    public static final int DEVICE_REMOTE_INCOMING_CALL_FLAGE = 34;
    public static final int DEVICE_REMOTE_MISSED_CALL_FLAGE = 35;
    public static final int DEVICE_REMOTE_NEW_MESSAGE_FLAGE = 36;
    public static final int DEVICE_REMOTE_NEW_EMAIL_FLAGE = 37;

    // in range alert checked
    public static final int IN_RANGE_ALERT_VALUE = 0;
    // out of range alert checked
    public static final int OUT_OF_RANGE_ALERT_VALUE = 1;

    // range near, middle, far value
    public static final int PXP_RANGE_NEAR_VALUE = 0;
    public static final int PXP_RANGE_MIDDLE_VALUE = 1;
    public static final int PXP_RANGE_FAR_VALUE = 2;

    // current distance value
    public static final int PXP_DISTANCE_NO_SIGNAL = 0;
    public static final int PXP_DISTANCE_FAR = 1;
    public static final int PXP_DISTANCE_MIDDLE = 2;
    public static final int PXP_DISTANCE_NEAR = 3;

    private BluetoothDevice mDevice;

    private int mConnectionState = BluetoothGatt.STATE_DISCONNECTED;
    private ActivityDbOperator mOperator;

    private DeviceAttribute mAttribute;
    private PxpConfiguration mPxpConfig;
    private AnsConfiguration mAnsConfig;

    private CopyOnWriteArrayList<DeviceAttributeChangeListener> mAttributeListener;

    private boolean mIsRingtoneAlarm;

    /**
     * device common configuration
     * 
     */
    private class DeviceAttribute {
        private String mDeviceName;
        private Uri mDeviceImageUri;
        private int mLocationIndex;
        private boolean mDeviceAutoConnect;
        private boolean mFmpAlarmState;
        private ArrayList<UUID> mServiceList;
    }

    /**
     * Device Pxp configuration
     * 
     */
    private class PxpConfiguration {
        // for ui
        // read from service which to indicate current distance
        // should show alert icon or not.
        private int mTxPower;

        // read from proximity service to indicate now is alerting or not
        // should show blinking image or not.
        private boolean mIsAlerting;

        // record current signal strength
        private int mCurrentSignal;

        //
        private boolean mIsPxpAlarmState;

        // for db
        private int mInOutRangeAlert; // db in out of range alert state
        private int mRangeConfigValue; // db range value showed in alert setting
        private int mVolume; // db device volume
        private boolean mIsAlertSwitchEnabler; // db alert switch check state
        private boolean mIsRangeAlertEnabler; // db range alert check box state
        private boolean mIsDisconnectionWarningEnabler; // db disconnection
                                                        // warning check box
                                                        // state
        private boolean mIsRingtoneEnabler; // db ringttone checkbox state
        private boolean mIsVibrationEnabler; // db vibration checkbox state
        // db should show range infor alert dialog check box state
        private boolean mIsShowRangeInfoDialogEnabler;
        private boolean mIsSupportOptional; // db is support optional profile

        private Uri mRingtoneUri; // db ringtone uri
    }

    /**
     * Device Ans Configuration
     * 
     */
    private class AnsConfiguration {
        private boolean mIsIncomingCallEnabler;
        private boolean mIsMissedCallEnabler;
        private boolean mIsNewMessageEnabler;
        private boolean mIsNewEmailEnabler;

        private boolean mIsRemoteIncomingConfig;
        private boolean mIsRemoteMissedCallConfig;
        private boolean mIsRemoteNewMessageConfig;
        private boolean mIsRemoteNewEmailConfig;
    }

    public CachedBleDevice(BluetoothDevice device, int displayOrder) {
        Log.i(TAG, "[CachedBluetoothLEDevice]new,displayOrder = " + displayOrder);
        mDevice = device;
        mAttribute = new DeviceAttribute();
        mPxpConfig = new PxpConfiguration();
        mAnsConfig = new AnsConfiguration();
        mAttribute.mLocationIndex = displayOrder;
        mOperator = ActivityDbOperator.getInstance();
        mAttributeListener = new CopyOnWriteArrayList<DeviceAttributeChangeListener>();
        if (!mOperator.isInDb(this, 0)) {
            doInialization();
            mOperator.initDevice(this);
        }
    }

    private void doInialization() {
        mAttribute.mDeviceName = mDevice.getName();
        // mAttribute.mDeviceImageUri = mOperator.getDefaultImage();
        mAttribute.mFmpAlarmState = false;
        mAttribute.mDeviceAutoConnect = true;
        initPxpLinkLostData();
        initPxpOptionalData();
    }

    private void initPxpLinkLostData() {
        mPxpConfig.mIsAlertSwitchEnabler = true;
        mPxpConfig.mIsDisconnectionWarningEnabler = true;
        mPxpConfig.mIsRingtoneEnabler = true;
        mPxpConfig.mIsShowRangeInfoDialogEnabler = false;
        mPxpConfig.mIsSupportOptional = false;
        mPxpConfig.mIsVibrationEnabler = true;
        mPxpConfig.mVolume = 10;
        mPxpConfig.mRingtoneUri = Uri.parse("content://media/internal/audio/media/7");
    }

    private void initPxpOptionalData() {
        mPxpConfig.mInOutRangeAlert = BleProximityProfileService.RANGE_ALERT_TYPE_OUT;
        mPxpConfig.mIsRangeAlertEnabler = true;
        mPxpConfig.mRangeConfigValue = BleProximityProfileService.RANGE_ALERT_RANGE_FAR;
    }

    // private void initAnsData() {
    // mAnsConfig.mIncomingCallEnabler = true;
    // mAnsConfig.mMissedCallEnabler = true;
    // mAnsConfig.mNewEmailEnabler = true;
    // mAnsConfig.mNewMessageEnabler = true;
    // }

    public interface DeviceAttributeChangeListener {
        void onDeviceAttributeChange(CachedBleDevice device, int which);
    }

    private boolean mFromDb = false;

    /**
     * do check init station from db or not
     * 
     * @param isFromDb
     *            true : from db false : from other
     */
    public void setInitFromDb(boolean isFromDb) {
        Log.i(TAG, "[setInitFromDb]isFromDb = " + isFromDb);
        mFromDb = isFromDb;
    }

    private void notfiyChange(int which) {
        if (mAttributeListener != null && mAttributeListener.size() != 0) {
            for (DeviceAttributeChangeListener listener : mAttributeListener) {
                listener.onDeviceAttributeChange(this, which);
            }
        }
        if (!mFromDb) {
            if (mOperator != null) {
                if (which != DEVICE_IMAGE_ATTRIBUTE_FLAG) {
                    mOperator.updateDeviceAttributeToDb(this, which);
                }
            }
        }
    }

    /**
     * register the device attribute changed listener
     * 
     * @param listener
     */
    public void registerAttributeChangeListener(DeviceAttributeChangeListener listener) {
        Log.i(TAG, "[registerAttributeChangeListener]...");
        if (listener == null) {
            Log.w(TAG, "[registerAttributeChangeListener] listener is null!!");
            return;
        }
        mAttributeListener.add(listener);
    }

    /**
     * un-register the device attribute changed listener
     * 
     * @param listener
     */
    public void unregisterAttributeChangeListener(DeviceAttributeChangeListener listener) {
        Log.i(TAG, "[unregisterAttributeChangeListener]...");
        if (listener == null) {
            Log.w(TAG, "[unregisterAttributeChangeListener] listener is null!!");
            return;
        }
        mAttributeListener.remove(listener);
    }

    /**
     * get remote bluetooth device
     * 
     * @return
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * get remote device name, if the remote device name has been changed in the
     * UX, return the name which changed in the UX. else return the device name
     * from BluetoothDevice
     * 
     * @return
     */
    public String getDeviceName() {
        if (mAttribute.mDeviceName != null && mAttribute.mDeviceName.trim().length() != 0) {
            return mAttribute.mDeviceName;
        }
        mAttribute.mDeviceName = mDevice.getName();
        return mAttribute.mDeviceName;
    }

    /**
     * used to set remote device name by ux editing. and this name will saved in
     * db. next time read from db, and fill the device name.
     * 
     * @param name
     */
    public void setDeviceName(String name) {
        Log.i(TAG, "[setDeviceName] name = " + name);
        if (name == null || name.trim().length() == 0) {
            Log.w(TAG, "[setDeviceName] name is empty!!");
            return;
        }
        mAttribute.mDeviceName = name;
        notfiyChange(DEVICE_NAME_ATTRIBUTE_FLAG);
    }

    /**
     * used to get device image uri.
     * 
     * @return
     */
    public Uri getDeviceImageUri() {
        return mAttribute.mDeviceImageUri;
    }

    /**
     * used to set device image by ux editing,and it will be stored in db.
     * 
     * @param uri
     */
    public void setDeviceImageUri(Uri uri) {
        Log.i(TAG, "[setDeviceName]uri = " + uri);
        if (uri == null) {
            Log.w(TAG, "[setDeviceImage] uri is null!!");
            return;
        }
        mAttribute.mDeviceImageUri = uri;
        Log.d(TAG, "[setDeviceImage] mAttribute.mDeviceImageUri : " + mAttribute.mDeviceImageUri);
        notfiyChange(DEVICE_IMAGE_ATTRIBUTE_FLAG);
    }

    public void updateDeviceImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            Log.w(TAG, "[updateDeviceImage] bytes is wrong");
            return;
        }
        mOperator.updateDeviceImage(this, bytes);
    }

    /**
     * get device display order in the 3d view
     * 
     * @return
     */
    public int getDeviceLocationIndex() {
        return mAttribute.mLocationIndex;
    }

    /**
     * get boolean value which stored in the CachedBluetoothLEDevice
     * 
     * @param which
     *            which boolean value want to get
     * @return
     */
    public boolean getBooleanAttribute(int which) {
        switch (which) {
        case DEVICE_FMP_STATE_FLAG:
            return mAttribute.mFmpAlarmState;

        case DEVICE_AUTO_CONNECT_FLAG:
            return mAttribute.mDeviceAutoConnect;

        case DEVICE_ALERT_SWITCH_ENABLER_FLAG:
            return mPxpConfig.mIsAlertSwitchEnabler;

        case DEVICE_RANGE_ALERT_ENABLER_FLAG:
            return mPxpConfig.mIsRangeAlertEnabler;

        case DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG:
            return mPxpConfig.mIsDisconnectionWarningEnabler;

        case DEVICE_RINGTONE_ENABLER_FLAG:
            return mPxpConfig.mIsRingtoneEnabler;

        case DEVICE_VIBRATION_ENABLER_FLAG:
            return mPxpConfig.mIsVibrationEnabler;

        case DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG:
            return mPxpConfig.mIsShowRangeInfoDialogEnabler;

        case DEVICE_ALERT_STATE_FLAG:
            return mPxpConfig.mIsAlerting;

        case DEVICE_RINGTONE_ALARM_STATE_FLAG:
            return mIsRingtoneAlarm;

            // case DEVICE_SUPPORT_OPTIONAL:
            // return mPxpConfig.mSupportOptional;

        case DEVICE_PXP_ALARM_STATE_FLAG:
            return mPxpConfig.mIsPxpAlarmState;

        case DEVICE_INCOMING_CALL_ENABLER_FLAG:
            return mAnsConfig.mIsIncomingCallEnabler;

        case DEVICE_MISSED_CALL_ENABLER_FLAG:
            return mAnsConfig.mIsMissedCallEnabler;

        case DEVICE_NEW_MESSAGE_ENABLER_FLAG:
            return mAnsConfig.mIsNewMessageEnabler;

        case DEVICE_NEW_EMAIL_ENABLER_FLAG:
            return mAnsConfig.mIsNewEmailEnabler;

        case DEVICE_REMOTE_INCOMING_CALL_FLAGE:
            return mAnsConfig.mIsRemoteIncomingConfig;

        case DEVICE_REMOTE_MISSED_CALL_FLAGE:
            return mAnsConfig.mIsRemoteMissedCallConfig;

        case DEVICE_REMOTE_NEW_MESSAGE_FLAGE:
            return mAnsConfig.mIsRemoteNewMessageConfig;

        case DEVICE_REMOTE_NEW_EMAIL_FLAGE:
            return mAnsConfig.mIsRemoteNewEmailConfig;

        default:
            throw new IllegalArgumentException("[getBooleanState] not recognized id");
        }
    }

    /**
     * Set device boolean configuration
     * 
     * @param which
     * @param state
     */
    public void setBooleanAttribute(int which, boolean state) {
        boolean changed = false;
        switch (which) {
        case DEVICE_FMP_STATE_FLAG:
            if (mAttribute.mFmpAlarmState != state) {
                mAttribute.mFmpAlarmState = state;
                changed = true;
                updateDeviceAlarmState();
            }
            break;

        case DEVICE_ALERT_STATE_FLAG:
            if (mPxpConfig.mIsAlerting != state) {
                mPxpConfig.mIsAlerting = state;
                changed = true;
            }
            break;

        case DEVICE_AUTO_CONNECT_FLAG:
            if (mAttribute.mDeviceAutoConnect != state) {
                mAttribute.mDeviceAutoConnect = state;
                changed = true;
            }
            break;

        case DEVICE_ALERT_SWITCH_ENABLER_FLAG:
            if (mPxpConfig.mIsAlertSwitchEnabler != state) {
                mPxpConfig.mIsAlertSwitchEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_RANGE_ALERT_ENABLER_FLAG:
            if (mPxpConfig.mIsRangeAlertEnabler != state) {
                mPxpConfig.mIsRangeAlertEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG:
            if (mPxpConfig.mIsDisconnectionWarningEnabler != state) {
                mPxpConfig.mIsDisconnectionWarningEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_RINGTONE_ENABLER_FLAG:
            if (mPxpConfig.mIsRingtoneEnabler != state) {
                mPxpConfig.mIsRingtoneEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_VIBRATION_ENABLER_FLAG:
            if (mPxpConfig.mIsVibrationEnabler != state) {
                mPxpConfig.mIsVibrationEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG:
            if (mPxpConfig.mIsShowRangeInfoDialogEnabler != state) {
                mPxpConfig.mIsShowRangeInfoDialogEnabler = state;
                changed = true;
            }
            break;

        // case DEVICE_SUPPORT_OPTIONAL:
        // mPxpConfig.mSupportOptional = state;
        // changed = true;
        // break;

        case DEVICE_INCOMING_CALL_ENABLER_FLAG:
            if (mAnsConfig.mIsIncomingCallEnabler != state) {
                mAnsConfig.mIsIncomingCallEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_MISSED_CALL_ENABLER_FLAG:
            if (mAnsConfig.mIsMissedCallEnabler != state) {
                mAnsConfig.mIsMissedCallEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_NEW_MESSAGE_ENABLER_FLAG:
            if (mAnsConfig.mIsNewMessageEnabler != state) {
                mAnsConfig.mIsNewMessageEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_NEW_EMAIL_ENABLER_FLAG:
            if (mAnsConfig.mIsNewEmailEnabler != state) {
                mAnsConfig.mIsNewEmailEnabler = state;
                changed = true;
            }
            break;

        case DEVICE_REMOTE_INCOMING_CALL_FLAGE:
            if (mAnsConfig.mIsRemoteIncomingConfig != state) {
                mAnsConfig.mIsRemoteIncomingConfig = state;
                changed = true;
            }
            break;

        case DEVICE_REMOTE_MISSED_CALL_FLAGE:
            if (mAnsConfig.mIsRemoteMissedCallConfig != state) {
                mAnsConfig.mIsRemoteMissedCallConfig = state;
                changed = true;
            }
            break;

        case DEVICE_REMOTE_NEW_MESSAGE_FLAGE:
            if (mAnsConfig.mIsRemoteNewMessageConfig != state) {
                mAnsConfig.mIsRemoteNewMessageConfig = state;
                changed = true;
            }
            break;

        case DEVICE_REMOTE_NEW_EMAIL_FLAGE:
            if (mAnsConfig.mIsRemoteNewEmailConfig != state) {
                mAnsConfig.mIsRemoteNewEmailConfig = state;
                changed = true;
            }
            break;

        default:
            throw new IllegalArgumentException("[setBooleanState] not recognized id");
        }
        if (changed) {
            notfiyChange(which);
        }
    }

    /**
     * Get device integer configuration
     * 
     * @param which
     * @return
     */
    public int getIntAttribute(int which) {
        switch (which) {
        case DEVICE_RANGE_VALUE_FLAG:
            return mPxpConfig.mRangeConfigValue;

        case DEVICE_IN_OUT_RANGE_ALERT_FLAG:
            return mPxpConfig.mInOutRangeAlert;

        case DEVICE_VOLUME_FLAG:
            return mPxpConfig.mVolume;

        case DEVICE_DISTANCE_FLAG:
            return mPxpConfig.mCurrentSignal;

        default:
            throw new IllegalArgumentException("[getIntState] not recognized id");
        }
    }

    /**
     * Set device integer configuration
     * 
     * @param which
     * @param value
     */
    public void setIntAttribute(int which, int value) {
        boolean changed = false;
        switch (which) {
        case DEVICE_RANGE_VALUE_FLAG:
            if (mPxpConfig.mRangeConfigValue != value) {
                mPxpConfig.mRangeConfigValue = value;
                updateAlertState(mPxpConfig.mTxPower);
                changed = true;
            }
            break;

        case DEVICE_IN_OUT_RANGE_ALERT_FLAG:
            if (mPxpConfig.mInOutRangeAlert != value) {
                mPxpConfig.mInOutRangeAlert = value;
                updateAlertState(mPxpConfig.mTxPower);
                changed = true;
            }
            break;

        case DEVICE_VOLUME_FLAG:
            if (mPxpConfig.mVolume != value) {
                mPxpConfig.mVolume = value;
                changed = true;
            }
            break;

        case DEVICE_CURRENT_TX_POWER_FLAG:
            if (mPxpConfig.mTxPower != value) {
                mPxpConfig.mTxPower = value;
                updateAlertState(mPxpConfig.mTxPower);
                updateCurrentSignalStrength(mPxpConfig.mTxPower);
                changed = true;
                return;
            }
            break;

        default:
            throw new IllegalArgumentException("[setIntState] not recognized id");
        }
        if (changed) {
            notfiyChange(which);
        }
    }

    /**
     * Get remote device service list
     * 
     * @return
     */
    public ArrayList<UUID> getServiceList() {
        if (mAttribute.mServiceList == null || mAttribute.mServiceList.size() == 0) {
            Log.w(TAG, "[getServiceList] service list is null or empty!!");
            return new ArrayList<UUID>();
        }
        return new ArrayList<UUID>(mAttribute.mServiceList);
    }

    public void setServiceListFromDb(ArrayList<String> serList) {
        if (serList == null || serList.size() == 0) {
            Log.w(TAG, "[setServiceListFromDb] service list is null or empty");
            return;
        }
        if (this.mAttribute.mServiceList == null) {
            this.mAttribute.mServiceList = new ArrayList<UUID>();
        }
        mAttribute.mServiceList.clear();
        for (String s : serList) {
            if (s.trim().length() != 0) {
                UUID uid = UUID.fromString(s);
                mAttribute.mServiceList.add(uid);
            }
        }
    }

    /**
     * check device is support fmp or not
     * 
     * @return
     */
    public boolean isSupportFmp() {
        ArrayList<UUID> serList = getServiceList();
        if (mAttribute.mServiceList != null && mAttribute.mServiceList.size() != 0) {
            for (UUID uid : serList) {
                if (BleGattUuid.Service.IMMEDIATE_ALERT.equals(uid)) {
                    Log.d(TAG, "[isSupportFmp] find device which support fmp profile");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check the device support pxp optional services
     * 
     * Only the device support TX_POWER && IMMEDIATE_ALERT, the method will
     * return true, Other return false;
     * 
     * @return
     */
    public boolean isSupportPxpOptional() {
        boolean supportTxPower = false;
        boolean supportImmediateAlert = isSupportFmp();
        ArrayList<UUID> serList = getServiceList();
        if (mAttribute.mServiceList != null && mAttribute.mServiceList.size() != 0) {
            for (UUID service : serList) {
                if (BleGattUuid.Service.TX_POWER.equals(service)) {
                    Log.d(TAG, "[isSupportLinkLost] find device which support TX power.");
                    supportTxPower = true;
                }
            }
        }
        if (supportTxPower && supportImmediateAlert) {
            return true;
        }
        return false;
    }

    /**
     * Check the device support PXP basic service link lost or not. If the
     * device service list contains LINK_LOST service uuid, return true, other
     * return false;
     * 
     * @return
     */
    public boolean isSupportLinkLost() {
        ArrayList<UUID> serList = getServiceList();
        if (mAttribute.mServiceList != null && mAttribute.mServiceList.size() != 0) {
            for (UUID service : serList) {
                if (BleGattUuid.Service.LINK_LOST.equals(service)) {
                    Log.d(TAG, "[isSupportLinkLost] find device which support link lost");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check the device support ans or not
     * 
     * @return
     */
    // public boolean isSupportAns() {
    // return false;
    // }

    /**
     * Get device ringtone uri
     * 
     * @return
     */
    public Uri getRingtoneUri() {
        return mPxpConfig.mRingtoneUri;
    }

    /**
     * Set device ringtone uri
     * 
     * @param uri
     */
    public void setRingtoneUri(Uri uri) {
        if (uri == null) {
            Log.w(TAG, "[setRingtoneUri] uri is null!!");
            return;
        }
        mPxpConfig.mRingtoneUri = uri;
        notfiyChange(DEVICE_RINGTONE_URI_FLAG);
    }

    /**
     * get the device connection state which may be device station
     * 
     * @return
     */
    public int getConnectionState() {
        return mConnectionState;
    }

    /**
     * which used to update the blinking photo.
     * 
     * if any of pxp or fmp is in alarm state, should blinking the photo
     */
    private void updateDeviceAlarmState() {
        Log.d(TAG, "[updateDeviceAlarmState] mPxpConfig.mPxpAlarmState : "
                + mPxpConfig.mIsPxpAlarmState);
        Log.d(TAG, "[updateDeviceAlarmState] mAttribute.mFmpAlarmState : "
                + mAttribute.mFmpAlarmState);
        if (mPxpConfig.mIsPxpAlarmState || mAttribute.mFmpAlarmState) {
            this.mIsRingtoneAlarm = true;
        } else {
            this.mIsRingtoneAlarm = false;
        }
        Log.d(TAG, "[updateDeviceAlarmState] mRingtoneAlarm : " + mIsRingtoneAlarm);
        this.notfiyChange(DEVICE_RINGTONE_ALARM_STATE_FLAG);
    }

    /**
     * update current signal strength according to tx power the signal strength
     * used to update UX
     * 
     * @param distance
     */
    // TODO this part should be update by pxp and jni discussion
    private void updateCurrentSignalStrength(int distance) {
        if (distance < 0) {
            Log.d(TAG, "[updateCurrentSignalStrength] distance < 0, wrong parameter");
            return;
        }
        Log.d(TAG, "[updateCurrentSignalStrength] distance : " + distance);
        final int THRESHOLD_NEAR = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_NEAR);
        final int THRESHOLD_MIDDLE = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_MIDDLE);
        final int THRESHOLD_FAR = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_FAR);
        // TODO should be change
        if (distance < THRESHOLD_NEAR) {
            mPxpConfig.mCurrentSignal = PXP_DISTANCE_NEAR;
        } else if (distance > THRESHOLD_NEAR && distance < THRESHOLD_MIDDLE) {
            mPxpConfig.mCurrentSignal = PXP_DISTANCE_MIDDLE;
        } else if (distance > THRESHOLD_MIDDLE && distance < THRESHOLD_FAR) {
            mPxpConfig.mCurrentSignal = PXP_DISTANCE_FAR;
        } else {
            mPxpConfig.mCurrentSignal = PXP_DISTANCE_NO_SIGNAL;
        }
        Log.d(TAG, "[updateCurrentSignalStrength] current signal : " + mPxpConfig.mCurrentSignal);
        this.notfiyChange(DEVICE_DISTANCE_FLAG);
    }

    /**
     * update device alert state
     * 
     * if pxp distance is out of setting range, should show a alert icon in 3D
     * view.
     * 
     * @param distance
     */
    private void updateAlertState(int distance) {
        Log.d(TAG, "[updateAlertState]distance = " + distance);
        boolean alert = false;
        final int THRESHOLD_NEAR = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_NEAR);
        final int THRESHOLD_MIDDLE = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_MIDDLE);
        final int THRESHOLD_FAR = BleProximityProfileService
                .getRangeAlertThreshold(BleProximityProfileService.RANGE_ALERT_RANGE_FAR);

        if (mPxpConfig.mInOutRangeAlert == OUT_OF_RANGE_ALERT_VALUE) {
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_NEAR_VALUE) {
                if (mPxpConfig.mTxPower > THRESHOLD_NEAR) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_MIDDLE_VALUE) {
                if (mPxpConfig.mTxPower > THRESHOLD_MIDDLE) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_FAR_VALUE) {
                if (mPxpConfig.mTxPower > THRESHOLD_FAR) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
        } else if (mPxpConfig.mInOutRangeAlert == IN_RANGE_ALERT_VALUE) {
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_NEAR_VALUE) {
                if (mPxpConfig.mTxPower < THRESHOLD_NEAR) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_MIDDLE_VALUE) {
                if (mPxpConfig.mTxPower < THRESHOLD_MIDDLE) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
            if (mPxpConfig.mRangeConfigValue == PXP_RANGE_FAR_VALUE) {
                if (mPxpConfig.mTxPower < THRESHOLD_FAR) {
                    alert = true;
                } else {
                    alert = false;
                }
            }
        } else {
            Log.e(TAG, "[updateAlertState] unknown range configure value");
        }
        mPxpConfig.mIsAlerting = alert;
        notfiyChange(DEVICE_ALERT_STATE_FLAG);
    }

    /**
     * 
     * @param isAlert
     */
    public void onDevicePxpAlertStateChange(boolean isAlert) {
        Log.d(TAG, "[onDevicePxpAlertStateChange] isAlert : " + isAlert);
        mPxpConfig.mIsPxpAlarmState = isAlert;
        updateDeviceAlarmState();
    }

    /**
     * Set remote device service list
     * 
     * @param list
     */
    void onServiceDiscovered(final List<BluetoothGattService> list) {
        if (list == null) {
            Log.w(TAG, "[onServiceDiscovered] list is null");
            return;
        }
        if (list.size() == 0) {
            Log.w(TAG, "[onServiceDiscovered] list size is 0");
            return;
        }
        if (mAttribute.mServiceList == null) {
            mAttribute.mServiceList = new ArrayList<UUID>();
        }
        mAttribute.mServiceList.clear();
        // boolean isSupportTxPower = false;
        // boolean isSupportImmAlert = false;
        for (BluetoothGattService service : list) {
            if (service != null) {
                Log.d(TAG, "[onServiceDiscovered] service : " + service.getUuid());
                mAttribute.mServiceList.add(service.getUuid());
            }
        }

        notfiyChange(DEVICE_SERVICE_LIST_CHANGE_FLAG);
    }

    /**
     * 
     * @param state
     */
    void onConnectionStateChanged(int state) {
        Log.d(TAG, "[onConnectionStateChanged] state : " + state);
        if (mConnectionState == state) {
            Log.d(TAG, "[onConnectionStateChanged] connection state is not changed");
            return;
        }
        mConnectionState = state;
        this.setBooleanAttribute(DEVICE_FMP_STATE_FLAG, false);
        this.onDevicePxpAlertStateChange(false);
        this.setBooleanAttribute(DEVICE_ALERT_STATE_FLAG, false);
        notfiyChange(DEVICE_CONNECTION_STATE_CHANGE_FLAG);
    }
}

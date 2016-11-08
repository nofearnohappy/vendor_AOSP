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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.bluetoothle.anp;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.util.SparseArray;

import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector.DetectorChangedEventProcessor;

import java.util.ArrayList;

public class BluetoothAnsCategoryManager {
    public static final int MSG_NEW_ALERT_FOR_ONE = 1001;
    public static final int MSG_NEW_ALERT_FOR_ALL = 1002;
    public static final int MSG_UNREAD_ALERT_FOR_ONE = 1003;
    public static final int MSG_UNREAD_ALERT_FOR_ALL = 1004;

    private static final String TAG = "[BluetoothAns]BluetoothAnsCategoryManager";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private SparseArray<BluetoothAnsCategory> mCategoryLists;
    private NotificationController mNotificationController;
    private AlertNotifier mAlertNotifer;

    private final DetectorChangedEventProcessor mDetectorListener =
            new DetectorChangedEventProcessor() {

                @Override
                public void onChanged(String address, byte categoryId, int type) {
                    if (DBG) {
                        Log.d(TAG, "mDetectorListener.onChanged(), " + categoryId + ", " + type);
                    }
                    refreshAlertCount(categoryId, type);
                    alertImmediately(address, categoryId, type);
                }

                @Override
                public void onNewAlertTextChanged(byte categoryId, String text) {
                    setNewAlertText(categoryId, text);
                }
            };

    public BluetoothAnsCategoryManager(
            NotificationController controller, AlertNotifier alertNotifer) {
        mCategoryLists = new SparseArray<BluetoothAnsCategory>();
        mNotificationController = controller;
        mAlertNotifer = alertNotifer;
    }

    public void alertImmediately(String deviceAddress, byte categoryId, int type) {
        if (DBG) {
            Log.d(TAG, "alertImmediately() , device = " + deviceAddress + ", categoryId = "
                    + categoryId + ", type = " + type);
        }
        ArrayList<BluetoothDevice> devices = mNotificationController.getNofiyableDevices(
                deviceAddress, categoryId, type);
        if (devices == null) {
            if (DBG) {
                Log.d(TAG, "alertImmediately() , devices = null");
            }
            return;
        } else {
            if (mCategoryLists != null) {
                BluetoothAnsCategory categoryDetectors = mCategoryLists.get(categoryId);
                if (categoryDetectors != null) {
                    byte alertCount = 0;
                    String contentText = null;
                    if (categoryDetectors != null) {
                        alertCount = categoryDetectors.getCategoryAlertCount(type);
                        contentText = categoryDetectors.getCategoryAlertText(type);
                    }
                    if (DBG) {
                        Log.d(TAG, "alertImmediately() , alertCount:" + alertCount
                                + ", contentText:" + contentText);
                        Log.d(TAG, "alertImmediately() , device = " + deviceAddress
                                + ", categoryId = " + categoryId + ", type = " + type);
                    }
                    if (mAlertNotifer != null) {
                        switch (type) {
                            case NotificationController.CATEGORY_ENABLED_NEW:
                                if (DBG) {
                                    Log.d(TAG, "alertImmediately() NEW");
                                }
                                mAlertNotifer.alertNewToDevices(categoryId, alertCount,
                                        contentText, devices);
                                break;
                            case NotificationController.CATEGORY_ENABLED_UNREAD:
                                if (DBG) {
                                    Log.d(TAG, "alertImmediately() UNREAD");
                                }
                                mAlertNotifer.alertUnreadToDevices(categoryId, alertCount, devices);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    public void alertImmediatelyByControl(String deviceAddress, byte categoryId, int type) {
        if (categoryId == NotificationController.CATEGORY_ID_ALL_CATEGORY) {
            byte[] supportedCategory = mNotificationController.getSupportedCategory();
            for (byte id : supportedCategory) {
                alertImmediately(deviceAddress, id, type);
            }
        } else {
            alertImmediately(deviceAddress, categoryId, type);
        }
    }

    public void removeAllDetectors() {
        if (mCategoryLists != null) {
            int size = mCategoryLists.size();
            for (int i = 0; i < size; i++) {
                mCategoryLists.valueAt(i).removeAllDetetors();
            }
            mCategoryLists.clear();
        }
    }

    public void addDetectors(ArrayList<BluetoothAnsDetector> detectorList) {
        if (detectorList != null && mCategoryLists != null) {
            for (BluetoothAnsDetector detector : detectorList) {
                byte categoryId = detector.getDetectorCategory();
                BluetoothAnsCategory categoryDetectors = mCategoryLists.get(categoryId);
                if (categoryDetectors == null) {
                    categoryDetectors = new BluetoothAnsCategory();
                    categoryDetectors.addDetector(detector);
                    mCategoryLists.put(categoryId, categoryDetectors);
                } else {
                    categoryDetectors.addDetector(detector);
                }
                detector.initializeAll();
                detector.registListener(mDetectorListener);
            }
        }
    }

    private void refreshAlertCount(byte categoryId, int type) {
        if (mCategoryLists != null) {
            BluetoothAnsCategory categoryDetectors = mCategoryLists.get(categoryId);
            if (categoryDetectors != null) {
                categoryDetectors.refreshStatus(type);
            }
        }
    }

    private void setNewAlertText(byte categoryId, String text) {
        if (mCategoryLists != null) {
            BluetoothAnsCategory categoryDetectors = mCategoryLists.get(categoryId);
            if (categoryDetectors != null) {
                categoryDetectors.setText(NotificationController.CATEGORY_ENABLED_NEW, text);
            }
        }
    }
}

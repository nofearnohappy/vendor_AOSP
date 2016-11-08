/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.providers.drm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import android.content.Context;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.os.SystemProperties;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;



import com.android.internal.telephony.PhoneConstants;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
/**
 * OMA DRM utility class
 */
public class OmaDrmHelper {
    private static final String TAG = "DRM/OmaDrmHelper";

    public static final String INVALID_DEVICE_ID = "000000000000000";
    private static final String OLD_DEVICE_ID_FILE =
            "/data/data/com.android.providers.drm/files/id/id.dat";
    private static final int DEVICE_ID_LEN = 32;
    private static final String EMPTY_STRING = "";

    /**
     *
     * @param client OMA DRM client
     * @param offset The time offset between device local time and time_server
     * @return The status of updating clock. ERROR_NONE for success, ERROR_UNKNOWN for failed.
     */
    public static int updateClock(OmaDrmClient client, int offset) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_CLOCK);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, String.valueOf(offset));

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client The OMA DRM client
     * @return the status of updating time base. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int updateTimeBase(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_TIME_BASE);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateTimeBase : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client The OMA DRM client
     * @return the status of updating offset. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int updateOffset(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_UPDATE_OFFSET);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "updateOffset : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client The OMA DRM client
     * @return the status of loading clock. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int loadClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_LOAD_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "loadClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client The OMA DRM Client
     * @return the status of saving clock. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int saveClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_SAVE_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "saveClock : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client The OMA DRM Client
     * @return the status of clock
     */
    public static boolean checkClock(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_CHECK_CLOCK);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "checkClock : > " + message);

        return message.equals("valid");
    }

    /**
     *
     * @param client The OMA DRM Client
     * @return the device id
     */
    public static String loadDeviceId(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_LOAD_DEVICE_ID);

        DrmInfo info = client.acquireDrmInfo(request);
        String id = getStringFromDrmInfo(info);
        Log.d(TAG, "loadDeviceId : > " + id);

        return id;
    }

    /**
     *
     * @param client The OMA DRM Client
     * @param deviceId The device id to be saved in file
     * @return the status of save device id. ERROR_NONE for success, ERROR_UNKOWN for failed.
     */
    public static int saveDeviceId(OmaDrmClient client, String deviceId) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_SAVE_DEVICE_ID);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, deviceId);

        DrmInfo info = client.acquireDrmInfo(request);
        String message = getStringFromDrmInfo(info);
        Log.d(TAG, "saveDeviceId : > " + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                OmaDrmClient.ERROR_NONE : OmaDrmClient.ERROR_UNKNOWN;
    }

    /**
     *
     * @param client
     *            The OMA DRM Client
     * @return the secure time in seconds
     */
    public static String getSecureTimeInSeconds(OmaDrmClient client) {
        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_LOAD_SECURE_TIME);

        DrmInfo info = client.acquireDrmInfo(request);
        String time = getStringFromDrmInfo(info);
        Log.d(TAG, "getSecureTimeInSeconds : > " + time);

        return time;
    }

    private static String getStringFromDrmInfo(DrmInfo info) {
        String message = EMPTY_STRING;
        if (info == null) {
            Log.e(TAG, "getStringFromDrmInfo info is null");
            return message;
        }
        byte[] data = info.getData();
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type of the returned DrmInfo data");
                message = EMPTY_STRING;
            }
        }
        Log.v(TAG, "getStringFromDrmInfo : >" + message);
        return message;
    }

    /**
     * Check if network is available.
     *
     * @return return true if network is available, otherwise return false
     *
     * @param context A context to get system service
     */
    public static boolean isNetWorkAvailable(Context context) {
        if (context == null) {
            Log.w(TAG, "context is null");
            return false;
        }
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (connManager == null) {
            Log.w(TAG, "connManager is null");
            return false;
        }
        NetworkInfo[] networkInfos = connManager.getAllNetworkInfo();
        if (networkInfos == null) {
            Log.w(TAG, "networkInfos is null");
            return false;
        }
        for (NetworkInfo info : networkInfos) {
            if (info.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the insert Icc Card is test card or not
     *
     * @return  return true if the Icc Card is test card
     */
    public static boolean isTestIccCard() {
        int ret1 = SystemProperties.getInt("gsm.sim.ril.testsim", 0);
        int ret2 = SystemProperties.getInt("gsm.sim.ril.testsim.2", 0);
        int ret3 = SystemProperties.getInt("gsm.sim.ril.testsim.3", 0);
        int ret4 = SystemProperties.getInt("gsm.sim.ril.testsim.4", 0);
        int result = (ret1 | ret2 | ret3 | ret4);
        Log.d(TAG, "isTestIccCard: " + ret1 + "," + ret2 + "," + ret3 + "," + ret4);
        return result == 1;
    }

    /**
     * Add to workaround CT test case to disable send ntp package. need user push a special
     * file to phone storage, when we check this file exist with right value, disable sync
     * secure timer.<p>
     * use below command to enable this function:<p>
     * adb shell "echo 1 > sdcard/.omadrm"
     *
     * @return true if request disable sync secure timer
     */
    public static boolean isRequestDisableSyncSecureTimer() {
        boolean isRequest = false;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream("sdcard/.omadrm");
            int flag = inputStream.read();
            Log.d(TAG, "flag: " + flag);
            // 49 means char '1'
            if (flag == 49) {
                isRequest = true;
            }
        } catch (IOException e) {
            Log.w(TAG, "Read file fail with ", e);
            isRequest = false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException e) {
                    Log.w(TAG, "close FileInputStream with IOException ", e);
                }
            }
        }
        Log.d(TAG, "isRequestDisableSyncSecureTimer: " + isRequest);
        return isRequest;
    }

    // you may modify this implementation to change the way you retrieve device id.
    // Note that the device id length is limited to 32 ASCII characters.
    // by default it returns an "invalid value" for device id if non of the method
    // can retrieve an valid one
    public static String getDeviceIdFromSystem(Context context) {
        // if invalid, 15 '0' digits are returned.
        String id = INVALID_DEVICE_ID;

        // for most of the cases, we use IMEI for device id. by default it's 15 digits
        // for MEID case, it is 14 digits/characters
        Log.v(TAG, "getDeviceIdFromSystem: try to get IMEI as device id");
        TelephonyManager tm = TelephonyManager.getDefault();
        if (null != tm) {
            String imei = tm.getDeviceId(PhoneConstants.SIM_ID_1);
            Log.d(TAG, "imei = " + imei);
            if (imei == null || imei.isEmpty()) {
                Log.w(TAG, "getDeviceIdFromSystem: Invalid imei: " + imei);
            } else {
                id = imei;
            }
        } else {
            Log.w(TAG, "getDeviceIdFromSystem: Invalid TelephonyManager.");
        }
        tm = null;

        // if we failed to get IMEI at boot-up time, for example, the boot-up timing
        //   issue after MOTA upgrade, we try /data/data/com.android.providers.drm/files/id/id.dat
        //   (the storage position for ICS ver.)
        if (id.equals(INVALID_DEVICE_ID)) {
            Log.v(TAG, "getDeviceIdFromSystem: try to check for old device id file "
        + OLD_DEVICE_ID_FILE);
            File f = new File(OLD_DEVICE_ID_FILE);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "getDeviceIdFromSystem: the old device id file is not found.");
                fis = null;
            }

            if (null != fis) {
                byte[] data = new byte[DEVICE_ID_LEN];
                for (byte element : data)
                    element = 0;

                try {
                    int result = fis.read(data);
                    // find the last byte which does not equals 0
                    int length = 0;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] == 0) {
                            length = i;
                            break;
                        }
                    }
                    byte[] array = new byte[length];
                    for (int j = 0; j < array.length; j++) {
                        array[j] = data[j];
                    }

                    id = new String(array, Charset.forName("US-ASCII"));
                    fis.close();
                } catch (IOException e) {
                    Log.w(TAG, "getDeviceIdFromSystem: I/O error when reading old devicd id file");
                }
            }
        }

        // now, in case there's no IMEI avaiable on device (some are wifi-only),
        // we may use wifi MAC address for an alternative method.
        // however we know that if wifi is closed, we can't get valid MAC value
        if (id.equals(INVALID_DEVICE_ID)) {
            Log.v(TAG, "getDeviceIdFromSystem: try to use mac address for device id.");
            WifiManager wm =
                (WifiManager) (context.getSystemService(Context.WIFI_SERVICE));
            if (null != wm) {
                WifiInfo info = wm.getConnectionInfo();
                String macAddr = (info == null) ? null : info.getMacAddress();
                if (macAddr == null || macAddr.isEmpty()) {
                    Log.w(TAG, "getDeviceIdFromSystem: Invalid mac address: " + macAddr);
                } else {
                    id = macAddr;
                }
            } else {
                Log.w(TAG, "getDeviceIdFromSystem: Invalid WifiManager.");
            }
        }

        // finally if non of those method does not work, the id may remains invalid value
        Log.v(TAG, "getDeviceIdFromSystem: deviceId =  " + id);
        return id;
    }

    /**
     * If the device id saved in drmserver is valid, we need get valid device
     * id from system.
     */
    public static void updateDeviceId(Context context, OmaDrmClient client) {
        String deviceId = OmaDrmHelper.loadDeviceId(client);
        Log.d(TAG, "updateDeviceId: load device id from drmserver with " + deviceId);

        // get an empty device id: the device id was not saved yet
        if (deviceId == null || deviceId.isEmpty() || deviceId.equals(INVALID_DEVICE_ID)) {
            deviceId = OmaDrmHelper.getDeviceIdFromSystem(context);
            OmaDrmHelper.saveDeviceId(client, deviceId);
        }
        Log.d(TAG, "updateDeviceId: save device id to drmserver with " + deviceId);
    }
}

/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.apst.target.data.proxy.sysinfo;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.telephony.TelephonyManager;

import com.mediatek.apst.target.data.proxy.ContextBasedProxy;
import com.mediatek.apst.target.util.Config;
import com.mediatek.apst.target.util.Debugger;
import com.mediatek.apst.util.command.sysinfo.SimDetailInfo;
import com.mediatek.apst.util.entity.message.Message;
import com.mediatek.telephony.TelephonyManagerEx;


/**
 * Class Name: SystemInfoProxy
 * <p>
 * Package: com.mediatek.apst.target.proxy.sysinfo
 * <p>
 * Created on: 2010-8-6
 * <p>
 * <p>
 * Description:
 * <p>
 * Proxy class provides system info related database operations.
 * <p>
 * Support platform: Android 2.2(Froyo)
 * <p>
 *
 * @author mtk80734 Siyang.Miao
 * @version V1.0
 */
/**
 * @author MTK81255
 *
 */
public final class SystemInfoProxy extends ContextBasedProxy {
    /** Singleton instance. */
    private static SystemInfoProxy sInstance = null;

    private static StorageManager sStorageManager = null;

    private static TelephonyManagerEx sTelephonyManager;

    private SystemInfoProxy(Context context) {
        super(context);
        setProxyName("SystemInfoProxy");
        sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        /* Gemini API refactor */
        sTelephonyManager = TelephonyManagerEx.getDefault();
    }

    /**
     * @param context
     *            The context to handle the systeminfoproxy.
     * @return An instance of the systemIfo proxy.
     */
    public static synchronized SystemInfoProxy getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new SystemInfoProxy(context);
        } else {
            sInstance.setContext(context);
        }
        return sInstance;
    }

    /**
     * @return String
     */
    public static String getDevice() {
        return Build.DEVICE;
    }

    /**
     * @return String
     */
    public static String getFirmwareVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * @return String
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * @return String
     */
    public static String getModel() {
        return Build.MODEL;
    }

    /**
     * @return String
     */
    public static String getSdPath() {
        return getExternalStoragePath();
    }

    /**
     * @return long
     */
    public static long getSdTotalSpace() {
        if (isSdMounted()) {
            String sdcard = getSdPath();
            StatFs statFs = new StatFs(sdcard);
            long totalSpace = (long) statFs.getBlockSize() * statFs.getBlockCount();

            return totalSpace;
        } else {
            return -1;
        }
    }

    /**
     * @return long
     */
    public static long getSdAvailableSpace() {
        if (isSdMounted()) {
            String sdcard = getSdPath();
            StatFs statFs = new StatFs(sdcard);
            long availableSpace = (long) statFs.getBlockSize() * statFs.getAvailableBlocks();

            return availableSpace;
        } else {
            return -1;
        }
    }

    /**
     * @return String
     */
    public static String getInternalStoragePath() {
        return getInternalStoragePathSD();
    }

    /**
     * @return long
     */
    public static long getInternalTotalSpace() {
        String data = getInternalStoragePath();
        if (data == null || "".equals(data)) {
            return -1;
        }
        StatFs statFs = new StatFs(data);
        long totalSpace = (long) statFs.getBlockSize() * statFs.getBlockCount();

        return totalSpace;
    }

    /**
     * @return long
     */
    public static long getInternalAvailableSpace() {
        String data = getInternalStoragePath();
        if (data == null || "".equals(data)) {
            return -1;
        }
        StatFs statFs = new StatFs(data);
        long availableSpace = (long) statFs.getBlockSize() * statFs.getAvailableBlocks();

        return availableSpace;
    }

    /**
     * @param simId int
     * @return int
     */
    public static int getSimState(int simId) {
        int simState = TelephonyManager.SIM_STATE_ABSENT;
        if (Config.MTK_GEMINI_SUPPORT) {
            if (Message.SIM1_ID == simId) {
                simState = sTelephonyManager.getSimState(
                        com.android.internal.telephony.PhoneConstants.SIM_ID_1);
            } else if (Message.SIM2_ID == simId) {
                simState = sTelephonyManager.getSimState(
                        com.android.internal.telephony.PhoneConstants.SIM_ID_2);
            } else if (Config.MTK_3SIM_SUPPORT && Message.SIM3_ID == simId) {
                simState = sTelephonyManager.getSimState(
                        com.android.internal.telephony.PhoneConstants.SIM_ID_3);
            } else if (Config.MTK_4SIM_SUPPORT && Message.SIM4_ID == simId) {
                simState = sTelephonyManager.getSimState(
                        com.android.internal.telephony.PhoneConstants.SIM_ID_4);
            }
        } else {
            if (Message.SIM_ID == simId) {
                simState = TelephonyManager.getDefault().getSimState();
            }
        }
        Debugger.logD(new Object[] { simId }, "simId=" + simId + ", simState=" + simState);
        return simState;
    }

    /**
     * @param simState int
     * @return boolean
     */
    public static boolean isSimAccessible(int simState) {
        boolean b = false;

        switch (simState) {
        case TelephonyManager.SIM_STATE_READY:
            b = true;
            break;

        case TelephonyManager.SIM_STATE_ABSENT:
        case TelephonyManager.SIM_STATE_PIN_REQUIRED:
        case TelephonyManager.SIM_STATE_PUK_REQUIRED:
        case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
            b = false;
            break;

        case TelephonyManager.SIM_STATE_UNKNOWN:
            b = false;
            break;

        default:
            b = false;
            break;
        }

        return b;
    }

    /**
     * Get whether SIM card is accessible.
     *
     * @return True if accessible, otherwise false.
     */
    public boolean isSimAccessible() {
        TelephonyManager telMgr = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);

        return isSimAccessible(telMgr.getSimState());
    }

    /**
     * @return boolean
     */
    public static boolean isSim1Accessible() {
        boolean b = false;
        if (Config.MTK_GEMINI_SUPPORT) {
            int simState = sTelephonyManager.getSimState(
                    com.android.internal.telephony.PhoneConstants.SIM_ID_1);

            b = isSimAccessible(simState);
        }
        return b;
    }

    /**
     * @return boolean
     */
    public static boolean isSim2Accessible() {
        boolean b = false;
        if (Config.MTK_GEMINI_SUPPORT) {
            int simState = sTelephonyManager.getSimState(
                    com.android.internal.telephony.PhoneConstants.SIM_ID_2);

            b = isSimAccessible(simState);
        }
        return b;
    }

    /**
     * @return boolean
     */
    public static boolean isSim3Accessible() {
        boolean b = false;
        if (Config.MTK_3SIM_SUPPORT) {
            int simState = sTelephonyManager.getSimState(
                    com.android.internal.telephony.PhoneConstants.SIM_ID_3);

            b = isSimAccessible(simState);
        }
        return b;
    }

    /**
     * @return boolean
     */
    public static boolean isSim4Accessible() {
        boolean b = false;
        if (Config.MTK_4SIM_SUPPORT) {
            int simState = sTelephonyManager.getSimState(
                    com.android.internal.telephony.PhoneConstants.SIM_ID_4);

            b = isSimAccessible(simState);
        }
        return b;
    }

    /**
     * @param slotId int
     * @return boolean
     */
    public static boolean getSimAccessibleBySlot(int slotId) {
        boolean b = false;
        switch (slotId) {
        case SimDetailInfo.SLOT_ID_ONE:
            b = isSim1Accessible();
            break;

        case SimDetailInfo.SLOT_ID_TWO:
            b = isSim2Accessible();
            break;

        case SimDetailInfo.SLOT_ID_THREE:
            b = isSim3Accessible();
            break;

        case SimDetailInfo.SLOT_ID_FOUR:
            b = isSim4Accessible();
            break;

        default:
            b = false;
            break;
        }
        return b;
    }
    /**
     * @return boolean
     */
    public static boolean isSdPresent() {
        String status = getSdStatus(getExternalStoragePath());
        return !(Environment.MEDIA_REMOVED.equals(status)
                || Environment.MEDIA_BAD_REMOVAL.equals(status));
    }

    /**
     * @return boolean
     */
    public static boolean isSdMounted() {
        String status = getSdStatus(getExternalStoragePath());
        return (Environment.MEDIA_MOUNTED.equals(status)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status));
    }

    /**
     * @return boolean
     */
    public static boolean isSdReadable() {
        return isSdMounted();
    }

    /**
     * @return boolean
     */
    public static boolean isSdWriteable() {
        String status = getSdStatus(getExternalStoragePath());
        return Environment.MEDIA_MOUNTED.equals(status);
    }

    /**
     * @param path String
     * @return boolean
     */
    public static String getSdStatus(String path) {
        if (path == null || "".equals(path)) {
            return Environment.MEDIA_REMOVED;
        }
        return sStorageManager.getVolumeState(path);
    }

    /**
     * @return boolean[]
     */
    public boolean[] checkSDCardState() {
        boolean emmcSupport = SystemProperties.get("ro.mtk_emmc_support").equals("1");
        Debugger.logI("SystemProperties : ro.mtk_emmc_support = " + emmcSupport);
        SystemProperties.get("ro.mtk_emmc_support");
        boolean[] sDstate = new boolean[2];
        String mSDCardPath = null;
        String mSDCard2Path = null;
        String[] storagePathList = sStorageManager.getVolumePaths();
        if (storagePathList != null) {
            if (storagePathList.length >= 2) {
                Debugger.logI("storagePathList.length >= 2");
                mSDCardPath = storagePathList[0];
                mSDCard2Path = storagePathList[1];
                if (null != mSDCardPath) {
                    String state = null;
                    state = sStorageManager.getVolumeState(mSDCardPath);
                    sDstate[0] = Environment.MEDIA_MOUNTED.equals(state);
                }

                if (null != mSDCard2Path) {
                    String state = null;
                    state = sStorageManager.getVolumeState(mSDCard2Path);
                    sDstate[1] = Environment.MEDIA_MOUNTED.equals(state);
                }

                if (!emmcSupport) {
                    Debugger.logI("SystemProperties : ro.mtk_emmc_support = false");
                    sDstate[1] = sDstate[0];
                    sDstate[0] = false;
                }
            } else if (storagePathList.length == 1) {
                Debugger.logI("storagePathList.length == 1");
                mSDCardPath = storagePathList[0];

                if (null != mSDCardPath) {
                    if (emmcSupport) {
                        String state = null;
                        state = sStorageManager.getVolumeState(mSDCardPath);
                        sDstate[0] = Environment.MEDIA_MOUNTED.equals(state);
                    } else {
                        String state = null;
                        state = sStorageManager.getVolumeState(mSDCardPath);
                        sDstate[1] = Environment.MEDIA_MOUNTED.equals(state);
                    }
                }
            }
        }
        return sDstate;
    }

    /**
     * @return String
     */
    public static String getInternalStoragePathSD() {
        return getInternalSdPath(sInstance.getContext());
    }

    /**
     * @return String
     */
    public static String getExternalStoragePath() {
        return getExternalSdPath(sInstance.getContext());
    }

    /**
     * @param context Context
     * @return String
     */
    public static String getInternalSdPath(Context context) {
        String internalPath = "";
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            String volumePathStr = volume.getPath();
            if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(volume.getState())) {
                VolumeInfo volumeInfo = mStorageManager.findVolumeById(volume.getId());
                if (volume.isEmulated()) {
                    String viId = volumeInfo.getId();
                    // If external sd card, the viId will be like "emulated:179,130"
                    if (viId.equalsIgnoreCase("emulated")) {
                        internalPath = volumePathStr;
                        break;
                    }
                } else {
                    DiskInfo diskInfo = volumeInfo.getDisk();
                    if (diskInfo == null) {
                        continue;
                    }
                    String diId = diskInfo.getId();
                    // If external sd card, the diId will be like "disk:179,128"
                    if (diId.equalsIgnoreCase("disk:179,0")) {
                        internalPath = volumePathStr;
                        break;
                    }
                }
            } else {
            }
        }
        Debugger.logI("getInternalSdPath : " + internalPath);
        return internalPath;
    }

    /**
     * @param context Context
     * @return String
     */
    public static String getExternalSdPath(Context context) {
        String externalPath = "";
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            String volumePathStr = volume.getPath();
            if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(volume.getState())) {
                VolumeInfo volumeInfo = mStorageManager.findVolumeById(volume.getId());
                if (volume.isEmulated()) {
                    String viId = volumeInfo.getId();
                    // If external sd card, the viId will be like "emulated:179,130"
                    if (!viId.equalsIgnoreCase("emulated")) {
                        externalPath = volumePathStr;
                        break;
                    }
                } else {
                    DiskInfo diskInfo = volumeInfo.getDisk();
                    if (diskInfo == null) {
                        continue;
                    }
                    String diId = diskInfo.getId();
                    // If external sd card, the diId will be like "disk:179,128"
                    if (!diId.equalsIgnoreCase("disk:179,0")) {
                        externalPath = volumePathStr;
                        break;
                    }
                }
            } else {
            }
        }
        Debugger.logI("getExternalSdPath : " + externalPath);
        return externalPath;
    }


    /**
     * @return boolean
     */
    public static boolean isSdSwap() {
        return SystemProperties.get("ro.mtk_2sdcard_swap").equals("1") && isExSdcardInserted();
    }

    /**
     * @return boolean
     */
    public static boolean isExSdcardInserted() {
        boolean status = (getExternalStoragePath() != null);
        Debugger.logI("Sdcard inserted status is " + status);
        return status;
    }
}

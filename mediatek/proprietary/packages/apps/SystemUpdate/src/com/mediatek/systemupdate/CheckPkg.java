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

package com.mediatek.systemupdate;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * CheckOta is used to check if the upgrade package matches the load. The
 * checked items include product, finger-print, partition layout, etc
 *
 * @author mtk80357
 */
class CheckPkg {
    private static final String TAG = "SystemUpdate/CheckPkg";
    private static final int DELTA_PKG = 0;
    private static final int BUFF_SIZE = 1024;
    private static final String ENTRY_BUILD_INFO = "META-INF/com/google/android/updater-script";
    private static final String PARTITION_FILE_PATH = "/proc/dumchar_info";
    private static final int PARTITION_SEC_NUM_DEV_MIN = 4;
    private static final int PARTITION_SEC_NUM_PKG = 2;
    private static final String LAST_PARTITION_NAME1 = "bmtpool";
    private static final String LAST_PARTITION_NAME2 = "otp";
    private static final String FAT_PARTITION_NAME = "fat";
    private static final String PRELOADER_PARTITION_NAME = "preloader";
    private static final String PGPT_PARTITION_NAME = "pgpt";
    private static final int PARTITION_NUM_MAX = 33;
    private static final int SECTOR_SIZE = 512;
    private static final int OFFSET_RADIX = 16;
    private static final int COMBO_EMMC_SECTION_NUM = 6;

    private static final String PROPERTY_DEV_NUM = "ro.product.device";
    private static final String PROPERTY_PRO_NUM = "ro.build.product";
    private static final String PROPERTY_FINGERPRINT = "ro.build.fingerprint";

    private static final String NAME_DEV_NUM = "getprop(\"ro.product.device\") == \"";
    private static final String NAME_PRO_NUM = "getprop(\"ro.build.product\") == \"";
private static final String NAME_FINGERPRINT = "\"ro.build.fingerprint\") == \"";

    private static final String PARTITION_FILE_PATH_NEW = "/sys/class/block/mmcblk0p";
    private static final String LAST_PARTITION_NAME_NEW1 = "flashinfo";
    private static final String LAST_PARTITION_NAME_NEW2 = "sgpt";
    private static final String FAT_PARTITION_NAME_NEW = "intsd";

    private static final String[][] PARTITION_NAME =
            { { "", "USRDATA", "__NODL_CACHE", "ANDROID" },
            { "", "USRDATA", "CACHE", "ANDROID" },
            { "", "__NODL_FAT", "USRDATA", "CACHE", "ANDROID" },
            { "", "FAT", "USRDATA", "CACHE", "ANDROID" },
            { "", "userdata", "cache", "system" },
            { "", "intsd", "userdata", "cache", "system" }
            };

    private int mOtaResult = Util.OTAresult.CHECK_OK;

    private boolean mIsEmmcPhone = false;
    private boolean mPartitionChange = false;
    private String mOtaFilePath;

    private String mOtaTypeFilePath;
    private String mScatterFilePath;
    private String mBuildInfoFilePath;

    private int mPkgType = UpgradePkgManager.OTA_PACKAGE;

    private Context mContext;

    private boolean mGptSchemeSupport = false;

    /**
     * PartitionInfo is used to record one partition information of name and
     * offset.
     *
     * @author mtk80357
     */
    private class PartitionInfo {
        String mName;
        long mOffset;

        public PartitionInfo(String name, long offset) {
            mName = name;
            mOffset = offset;

        }

    }

    /**
     * LoadVersionInfo records version related information of one loads or
     * upgrade package.
     *
     * @author mtk80357
     */
    private class LoadVersionInfo {
        String mDevNum;
        String mProNum;
        String mFingerPrint1;
        String mFingerPrint2; // Upgrade package supplies finger-print for both
                              // old and new loads
    }

    /**
     * Constructor function.
     *
     * @param context
     *            the current context
     * @param strOtaFilePath
     *            the path of upgrade package to check
     */
    public CheckPkg(Context context, int type, String strPackageFilePath) {
        mPkgType = type;
        mOtaFilePath = strPackageFilePath;
        mContext = context;
        Log.i("@M_" + TAG, "mOtaFilePath = " + mOtaFilePath);
        mOtaTypeFilePath = Util.getPackagePathName(mContext) + "/" + Util.PathName.ENTRY_TYPE;
        mScatterFilePath = Util.getPackagePathName(mContext) + "/" + Util.PathName.ENTRY_SCATTER;
        mBuildInfoFilePath = Util.getPackagePathName(mContext) + "/" + "script";

    }

    void deleteUnusedFile() {

        Util.deleteFile(mOtaTypeFilePath);
        Util.deleteFile(mScatterFilePath);
        Util.deleteFile(mBuildInfoFilePath);

    }

    /**
     * Check if the upgrade package matches the load.
     *
     * @return the checking result
     * @see Util.OTAresult
     */
    int execForResult() {

        if (mOtaFilePath == null) {
            return Util.OTAresult.ERROR_OTA_FILE;
        }
        mIsEmmcPhone = false;
        mPartitionChange = false;
        mGptSchemeSupport = false;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(mOtaFilePath);

            if (zipFile == null) {
                mOtaResult = Util.OTAresult.ERROR_OTA_FILE;
                Log.i("@M_" + TAG, "zipFile == null");
                return mOtaResult;
            }

            // Get OTA package type

            int otaType = getOtaType(zipFile);
            Log.i("@M_" + TAG, "otaType = " + otaType + " mOtaResult = " + mOtaResult);
            if (otaType == -1) {
                return mOtaResult;
            }

            if (!checkPartiton(zipFile, otaType)) {
                return mOtaResult;
            }

            if (!checkVersion(zipFile, otaType)) {
                return mOtaResult;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;
            return mOtaResult;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return mOtaResult;
    }

    private boolean checkVersion(ZipFile zipFile, int otaType) {

        LoadVersionInfo infoDev = new LoadVersionInfo();
        LoadVersionInfo infoPkg = new LoadVersionInfo();

        if (!getVersionInfoPkg(zipFile, infoPkg, otaType)) {
            return false;
        }

        //support no need to compare project or finger print case.
        if (((infoPkg.mDevNum == null) && (infoPkg.mProNum == null)) || (otaType == DELTA_PKG)
                && (infoPkg.mFingerPrint1 == null) && (infoPkg.mFingerPrint2 == null)) {
            Log.i("@M_" + TAG, "infoPkg.mDevNum " + infoPkg.mDevNum + " infoPkg.mProNum "
                    + infoPkg.mProNum + " otaType " + otaType + " infoPkg.mFingerPrint1 "
                    + infoPkg.mFingerPrint1 + " infoPkg.mFingerPrint2 "
                    + infoPkg.mFingerPrint2);
            Log.e("@M_" + TAG, "project number or finger print of upgrade package is null,"
                    + "no need to compare.");
            return true;
        }

        Log.i("@M_" + TAG, "infoPkg: " + infoPkg.mDevNum + " " + infoPkg.mProNum
                + " " + infoPkg.mFingerPrint1 + " "
                + infoPkg.mFingerPrint2);
        getVersionInfoDev(infoDev, otaType);
        Log.i("@M_" + TAG, "infoDev: " + infoDev.mDevNum + " " + infoDev.mProNum
                + " " + infoDev.mFingerPrint1 + " "
                + infoDev.mFingerPrint2);

        if (!compDevAndProInfo(infoDev, infoPkg)) {
            mOtaResult = Util.OTAresult.ERROR_MATCH_DEVICE;
            return false;
        }
        if (otaType == DELTA_PKG) {
            if (!compFingerPrint(infoDev, infoPkg)) {
                mOtaResult = Util.OTAresult.ERROR_DIFFERENTIAL_VERSION;
                return false;
            }
        }

        return true;
    }

    private boolean compDevAndProInfo(LoadVersionInfo infoDev, LoadVersionInfo infoPkg) {

        if ((infoDev.mDevNum != null) && (infoDev.mDevNum.equals(infoPkg.mDevNum))) {
            return true;
        }

        if ((infoDev.mProNum != null) && (infoDev.mProNum.equals(infoPkg.mProNum))) {
            return true;
        }

        return false;

    }

    private boolean compFingerPrint(LoadVersionInfo infoDev, LoadVersionInfo infoPkg) {
        if ((infoDev.mFingerPrint1 != null)
                && ((infoDev.mFingerPrint1.equals(infoPkg.mFingerPrint1)) || (infoDev.mFingerPrint1
                        .equals(infoPkg.mFingerPrint2)))) {
            return true;
        }

        return false;

    }

    private void getVersionInfoDev(LoadVersionInfo infoDev, int otaType) {

        infoDev.mDevNum = SystemProperties.get(PROPERTY_DEV_NUM);
        infoDev.mProNum = SystemProperties.get(PROPERTY_PRO_NUM);
        if (otaType == DELTA_PKG) {
            infoDev.mFingerPrint1 = SystemProperties.get(PROPERTY_FINGERPRINT);
        }

    }

    private boolean getVersionInfoPkg(ZipFile zipFile, LoadVersionInfo infoPkg, int otaType) {

        if (!unzipFileElement(zipFile, ENTRY_BUILD_INFO, mBuildInfoFilePath)) {
            Log.i("@M_" + TAG, "unzip fail from " + ENTRY_BUILD_INFO + " to " + mBuildInfoFilePath);
            return false;
        }
        FileReader reader = null;
        try {
            reader = new FileReader(mBuildInfoFilePath);
            BufferedReader bfReader = new BufferedReader(reader);
            String line;

            while ((line = bfReader.readLine()) != null) {

                if (infoPkg.mDevNum == null) {
                    infoPkg.mDevNum = extractVerInfo(line, NAME_DEV_NUM);
                }

                if (infoPkg.mProNum == null) {
                    infoPkg.mProNum = extractVerInfo(line, NAME_PRO_NUM);
                }

                if (otaType == DELTA_PKG) {
                    if (infoPkg.mFingerPrint1 == null) {
                        infoPkg.mFingerPrint1 = extractVerInfo(line, NAME_FINGERPRINT);
                    } else if (infoPkg.mFingerPrint2 == null) {
                        infoPkg.mFingerPrint2 = extractVerInfo(line, NAME_FINGERPRINT);
                    }
                }

            }

            return true;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;

            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String extractVerInfo(String strSrc, String strTag) {
        int nIndexStart = strSrc.indexOf(strTag);
        if (nIndexStart != -1) {
            strSrc = strSrc.substring(nIndexStart + strTag.length());

            int nIndexEnd = strSrc.indexOf('"');
            if (nIndexEnd != -1) {
                return strSrc.substring(0, nIndexEnd);

            }
        }

        return null;
    }

    private int getOtaType(ZipFile zipFile) {

        Log.i("@M_" + TAG, "into:getOtaType");

        if (!unzipFileElement(zipFile, Util.PathName.ENTRY_TYPE, mOtaTypeFilePath)) {
            Log.i("@M_" + TAG, "unzip fail from " + Util.PathName.ENTRY_TYPE + " to " + mOtaTypeFilePath);
            return -1;
        }
        FileReader reader = null;
        try {
            reader = new FileReader(mOtaTypeFilePath);
            BufferedReader bfReader = new BufferedReader(reader);

            String line = bfReader.readLine();

            if (line != null) {
                return Integer.parseInt(line);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }  finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;
        return -1;

    }

    private boolean checkPartiton(ZipFile zipFile, int otaType) {

        List<PartitionInfo> partitionDev = new ArrayList<PartitionInfo>();

        List<PartitionInfo> partitionPkg = new ArrayList<PartitionInfo>();

        if ((partitionDev == null) || (partitionPkg == null)) {
            mOtaResult = Util.OTAresult.ERROR_OUT_OF_MEMORY;
            return false;
        }

        if (!getPartitionInfoDev(partitionDev)) {
            Log.i("@M_" + TAG, "getPartitionInfoDev false");
            return false;
        }
        Log.i("@M_" + TAG, "mIsEmmcPhone = " + mIsEmmcPhone);

        if (!getPartitionInfoPkg(zipFile, partitionPkg)) {
            Log.i("@M_" + TAG, "getPartitionInfoPkg false");
            return false;
        }

        if (partitionDev.size() != partitionPkg.size()) {
            Log.i("@M_" + TAG, "partitionDev.size() != partitionPkg.size()");
            mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
            return false;
        }

        if (!checkPartitionLayout(partitionPkg)) {
            Log.i("@M_" + TAG, "checkPartitionLayout(partitionPkg) false");
            mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
            return false;
        }

        if (!comparePartition(partitionDev, partitionPkg, otaType)) {
            Log.i("@M_" + TAG, "comparePartition false");
            return false;
        }

        if ((otaType == DELTA_PKG) && mPartitionChange) {

            mOtaResult = Util.OTAresult.ERROR_ONLY_FULL_CHANGE_SIZE;
            return false;
        }

        return true;
    }

    private boolean comparePartition(List<PartitionInfo> partitionDev,
            List<PartitionInfo> partitionPkg, int otaType) {

        int length = partitionDev.size();

        Log.i("@M_" + TAG, "comparePartition partitionDev.size() = " + length);
        Log.i("@M_" + TAG, "comparePartition partitionPkg.size() = " + partitionPkg.size());

        if (!mIsEmmcPhone) {
            for (int i = 0; i < (length - 2); i++) {

                Log.i("@M_" + TAG, "i = " + i + " partitionDev.get(i).mOffset = "
                        + partitionDev.get(i).mOffset
                        + " partitionPkg.get(i).mOffset = "
                        + partitionPkg.get(i).mOffset);

                if (partitionDev.get(i).mOffset != partitionPkg.get(i).mOffset) {
                    mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
                    return false;
                }
            }
            if (otaType == DELTA_PKG) {
                for (int k = (length - 2); k < length; k++) {
                    Log.i("@M_" + TAG, "k = " + k + " partitionDev.get(k).mOffset = "
                            + partitionDev.get(k).mOffset
                            + " partitionPkg.get(k).mOffset = "
                            + partitionPkg.get(k).mOffset);

                    if (partitionDev.get(k).mOffset != partitionPkg.get(k).mOffset) {
                        mPartitionChange = true;
                        break;
                    }
                }
            }

        } else {
            // check partition rule: if it is delta package,need check every
            // offset
            // if it is full package,if the last name is not "fat" ,do not need
            // to check the last
            // offset
            int index = 0;
            boolean fatExist = FAT_PARTITION_NAME
                    .equalsIgnoreCase(partitionDev.get(length - 1).mName);

            Log.i("@M_" + TAG, "fat exist " + fatExist);
            if (!fatExist) {
                index = 3; // the last three is android,cache,usrdata
            } else {
                index = 4; // the last four is android,cache,usrdata,fat
            }

            for (int i = 0; i < length - index; i++) {
                Log.i("@M_" + TAG, "i = " + i + " partitionDev.get(i).mOffset = "
                        + partitionDev.get(i).mOffset + " partitionPkg.get(i).mOffset = "
                        + partitionPkg.get(i).mOffset);

                if (partitionDev.get(i).mOffset != partitionPkg.get(i).mOffset) {
                    mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
                    return false;
                }
            }

            boolean isDeltaPkg = (otaType == DELTA_PKG);
            long lastOffsetDev = partitionDev.get(length - 1).mOffset;
            long lastOffsetPkg = partitionPkg.get(length - 1).mOffset;
            Log.i("@M_" + TAG, "lastOffsetDev = " + lastOffsetDev + " lastOffsetPkg = " + lastOffsetPkg);

            if (fatExist || isDeltaPkg) {
                if (lastOffsetDev != lastOffsetPkg) {
                    mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
                    return false;
                }
            }

            if (isDeltaPkg) {
                for (int k = (length - index); k < length - 1; k++) {
                    Log.i("@M_" + TAG,
                            "k = " + k + " partitionDev.get(k).mOffset = "
                                    + partitionDev.get(k).mOffset
                                    + " partitionPkg.get(k).mOffset = "
                                    + partitionPkg.get(k).mOffset);

                    if (partitionDev.get(k).mOffset != partitionPkg.get(k).mOffset) {
                        mPartitionChange = true;
                        break;
                    }
                }
            }
        }
        boolean notifyStatus = DownloadInfo.getInstance(mContext).getNotifyStatus();
        // need notify user to do backup once if user data partition is changed.
        if (!notifyStatus && otaType != DELTA_PKG) {
            for (int i = 0; i < length; i++) {
                if (partitionDev.get(i).mName.toLowerCase().equals("userdata")
                        || partitionDev.get(i).mName.toLowerCase().equals("usrdata")) {
                    if (partitionDev.get(i).mOffset != partitionPkg.get(i).mOffset) {
                        mOtaResult = Util.OTAresult.CONFIRM_UPGRADE;
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean getPartitionInfoPkg(ZipFile zipFile, List<PartitionInfo> partitionPkg) {
        Log.i("@M_" + TAG, "into: getPartitionInfoPkg");

        if (!unzipFileElement(zipFile, Util.PathName.ENTRY_SCATTER, mScatterFilePath)) {
            Log.i("@M_" + TAG, "unzip fail from " + Util.PathName.ENTRY_SCATTER
                    + " to " + mScatterFilePath);
            return false;
        }
        FileReader reader = null;
        try {
            reader = new FileReader(mScatterFilePath);
            BufferedReader bfReader = new BufferedReader(reader);

            String line;
            while ((line = bfReader.readLine()) != null) {
                Log.i("@M_" + TAG, "line = " + line);
                if ((line.length() >= 1) && (line.charAt(0) != '{') && (line.charAt(0) != '}')) {
                    String[] partitionInfoArray = line.split("\\s+");
                    if (partitionInfoArray.length != PARTITION_SEC_NUM_PKG) {
                        continue;
                    }

                    // for partition name changed
                    if (mGptSchemeSupport) {

                        if ((LAST_PARTITION_NAME_NEW1.equals(partitionInfoArray[0]))
                                || (LAST_PARTITION_NAME_NEW2.equals(partitionInfoArray[0]))) {
                            break;
                        }
                        if (PRELOADER_PARTITION_NAME.equals(partitionInfoArray[0])
                                || PGPT_PARTITION_NAME.equals(partitionInfoArray[0])) {
                            continue;
                        }
                    }

                    if ((partitionInfoArray[1] != null) && (partitionInfoArray[1].length() >= 3)) {

                        String strTemp = partitionInfoArray[1].substring(2);

                        PartitionInfo info = new PartitionInfo(partitionInfoArray[0],
                                Long.parseLong(strTemp, OFFSET_RADIX));
                        Log.i("@M_" + TAG,
                                "name = " + partitionInfoArray[0] + " offset is "
                                        + Long.parseLong(strTemp, OFFSET_RADIX));
                        partitionPkg.add(info);

                    } else {
                        continue;
                    }

                }

            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mOtaResult = Util.OTAresult.ERROR_INVALID_ARGS;

            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    private boolean getPartitionInfoDev(List<PartitionInfo> partitionDev) {
        Log.i("@M_" + TAG, "into: getPartitionInfoDev");
        FileReader reader = null;
        try {
            try {
                reader = new FileReader(PARTITION_FILE_PATH);
            } catch (java.io.FileNotFoundException e) {
                Log.i("@M_" + TAG, "into: getPartitionInfoDev path is " + PARTITION_FILE_PATH_NEW);

                FileReader readeruevent, readerstart, readersize;

                String tempfile = Util.getTempPath(mContext) + "partinfo";

                File f = new File(tempfile);

                f.createNewFile();
                OutputStream out = new FileOutputStream(f);

                StringBuilder filedata = new StringBuilder();
                filedata.append("Name              Start             Size\n");
                try {

                    for (int i = 1; i <= PARTITION_NUM_MAX - 2; i++) {
                        readeruevent = new FileReader(PARTITION_FILE_PATH_NEW + i + "/uevent");
                        readerstart = new FileReader(PARTITION_FILE_PATH_NEW + i + "/start");
                        readersize = new FileReader(PARTITION_FILE_PATH_NEW + i + "/size");

                        BufferedReader filereader = new BufferedReader(readeruevent);
                        String data = null;

                        while ((data = filereader.readLine()) != null) {
                            if (data.contains("PARTNAME")) {
                                int start = data.indexOf("=") + 1;
                                String value = data.substring(start, data.length());
                                filedata.append(String.format("%-18s", value));
                            }
                        }
                        filereader.close();

                        filereader = new BufferedReader(readerstart);
                        if ((data = filereader.readLine()) != null) {
                            Long d = Long.valueOf(data) * SECTOR_SIZE;
                            filedata.append(String.format("%#018x ", d));
                        }
                        filereader.close();

                        filereader = new BufferedReader(readersize);
                        if ((data = filereader.readLine()) != null) {
                            Long d = Long.valueOf(data) * SECTOR_SIZE;
                            filedata.append(String.format("%#018x\n", d));
                        }
                        filereader.close();
                    }
                }
                catch (IOException e1) {
                    out.write(filedata.toString().getBytes());
                    out.close();
                    out = null;
                }
                if (out != null) {
                    out.write(filedata.toString().getBytes());
                    out.close();
                }

                reader = new FileReader(tempfile);
                mGptSchemeSupport = true;
            }

            BufferedReader bfReader = new BufferedReader(reader);

            String line = bfReader.readLine();

            if (line == null) {
                mOtaResult = Util.OTAresult.ERROR_INVALID_ARGS;
                return false;
            }
            long offset = 0;

            boolean isComboEmmcPreloader = false;
            boolean isNand = !Util.isEmmcSupport();
            boolean firstPartition = true;
            while ((line = bfReader.readLine()) != null) {
                Log.i("@M_" + TAG, "line = " + line);
                String[] partitionInfoArray = line.split("\\s+");

                if (!mGptSchemeSupport) {
                    if (partitionInfoArray.length < PARTITION_SEC_NUM_DEV_MIN) {
                        continue;
                    }
                }

                if ((LAST_PARTITION_NAME1.equals(partitionInfoArray[0]))
                        || (LAST_PARTITION_NAME2.equals(partitionInfoArray[0]))) {

                    break;
                }

                // for partition name changed
                if (mGptSchemeSupport) {
                    if ((LAST_PARTITION_NAME_NEW1.equals(partitionInfoArray[0]))
                            || (LAST_PARTITION_NAME_NEW2.equals(partitionInfoArray[0]))) {
                        break;
                    }
                }

                isComboEmmcPreloader = (partitionInfoArray.length == COMBO_EMMC_SECTION_NUM)
                        && (PRELOADER_PARTITION_NAME.equals(partitionInfoArray[0]));

                if (isNand) {
                    PartitionInfo nandInfo = new PartitionInfo(partitionInfoArray[0], offset);
                    nandInfo = new PartitionInfo(partitionInfoArray[0],
                            extractSize(partitionInfoArray[2]));
                    Log.i("@M_" + TAG, "NAND partition layout: name = " + nandInfo.mName + " offset = "
                            + nandInfo.mOffset);
                    partitionDev.add(nandInfo);
                } else {
                    if (firstPartition == true) {
                         offset = isComboEmmcPreloader ?
                                 0 : (offset + extractSize(partitionInfoArray[1]));
                         firstPartition = false;
                    }
                    PartitionInfo info = new PartitionInfo(partitionInfoArray[0],
                            isComboEmmcPreloader ? extractSize(partitionInfoArray[1]) : offset);
                    Log.i("@M_" + TAG, "name = " + info.mName + " offset = " + info.mOffset);
                    partitionDev.add(info);

                    if (!mGptSchemeSupport) {
                        if (partitionInfoArray[3] != null) {
                            if (Integer.parseInt(partitionInfoArray[3]) == 2) {
                                mIsEmmcPhone = true;
                            }

                        }

                    }

                }

                if (Util.isEmmcSupport()
                        && (FAT_PARTITION_NAME.equalsIgnoreCase(partitionInfoArray[0]))) {
                    break;
                }
                if (mGptSchemeSupport) {
                    offset = isComboEmmcPreloader ?
                            0 : (offset + extractSize(partitionInfoArray[2]));
                } else {
                    offset = isComboEmmcPreloader ?
                            0 : (offset + extractSize(partitionInfoArray[1]));
                }
            }

            if (partitionDev.size() > PARTITION_NUM_MAX) {
                mOtaResult = Util.OTAresult.ERROR_PARTITION_SETTING;
                return false;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            mOtaResult = Util.OTAresult.ERROR_INVALID_ARGS;
            return false;
        }  finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }


    /**
     * Convert string to long to extract the partition size.
     *
     * @param size
     *            , String to be converted.
     * @return converted value. 0 if size is invalid.
     */

    private long extractSize(String size) {
        if ((size != null) && (size.length() >= 3)) {

            String strTemp = size.substring(2);
            return Long.parseLong(strTemp, OFFSET_RADIX);

        } else {
            Log.e("@M_" + TAG, "Invalid size is " + size);
            return 0;
        }
    }

    private boolean checkPartitionLayout(List<PartitionInfo> partitionPkg) {
        Log.i("@M_" + TAG, "into: checkPartitionLayout");
        int partitionNum = partitionPkg.size();

        for (int k = 0; k < PARTITION_NAME.length; k++) {
            int index = 1;
            int num = PARTITION_NAME[k].length;
            for (; index < num; index++) {

                String strName = partitionPkg.get(partitionNum - index).mName;

                int length = PARTITION_NAME[k][index].length();

                if ((strName != null) && (strName.length() >= length)) {
                    strName = strName.substring(0, length);

                    Log.i("@M_" + TAG, "PARTITION_NAME[k][index] = "
                            + PARTITION_NAME[k][index] + " strName = " + strName);
                    if (!PARTITION_NAME[k][index].equals(strName)) {
                        break;
                    }
                } else {
                    break;
                }
            }

            if (index == num) {
                return true;
            }

        }

        return false;

    }

    private boolean unzipFileElement(ZipFile zipFile, String strSrcFile, String strDesFile) {

        Log.i("@M_" + TAG, "unzipFileElement:" + strSrcFile + ";" + strDesFile);
        ZipEntry zipEntry = zipFile.getEntry(strSrcFile);

        if (zipEntry == null) {
            mOtaResult = Util.OTAresult.ERROR_OTA_FILE;
            return false;
        }

        try {
            InputStream in = zipFile.getInputStream(zipEntry);

            if (in == null) {
                mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;
                return false;
            }

            File desFile = new File(strDesFile);
            if (desFile == null) {
                mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;
                return false;
            }
            if (desFile.exists()) {
                desFile.delete();
            }
            desFile.createNewFile();
            OutputStream out = new FileOutputStream(desFile);
            if (out == null) {
                mOtaResult = Util.OTAresult.ERROR_FILE_OPEN;
                return false;

            }
            byte[] buffer = new byte[BUFF_SIZE];
            int realLength = 0;
            while ((realLength = in.read(buffer)) > 0) {
                out.write(buffer, 0, realLength);
            }
            out.close();
            out = null;
            in.close();
            in = null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            mOtaResult = Util.OTAresult.ERROR_FILE_WRITE;

            return false;
        }

        return true;
    }

}

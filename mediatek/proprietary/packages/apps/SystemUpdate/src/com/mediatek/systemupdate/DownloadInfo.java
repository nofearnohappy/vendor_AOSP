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
import android.content.SharedPreferences;
import android.util.Log;


//Package access permission
/**
 *
 * Manager download status.
 *
 */
final class DownloadInfo {

    static final int STATE_QUERYNEWVERSION = 0;
    static final int STATE_NEWVERSION_READY = 1;
    static final int STATE_DOWNLOADING = 2;
    static final int STATE_CANCELDOWNLOAD = 3;
    static final int STATE_PAUSEDOWNLOAD = 4;
    static final int STATE_DLPKGCOMPLETE = 5;
    static final int STATE_PACKAGEERROR = 6;
    static final int STATE_PACKAGEUNZIPPING = 7;

    private static final String OTA_PREFERENCE = "system_update";
    private static final String OTA_PRE_STATUS = "downlaodStatus";
    private static final String OTA_PRE_DOWNLOAND_PERCENT = "downloadpercent";
    private static final String OTA_PRE_IMAGE_SIZE = "imageSize";
    private static final String OTA_PRE_VER = "version";
    private static final String OTA_PRE_VER_NOTE = "versionNote";
    private static final String OTA_PRE_DELTA_ID = "versionDeltaId";
    private static final String OTA_UNZ_STATUS = "isunzip";
    private static final String OTA_REN_STATUS = "isrename";
    private static final String OTA_QUERY_DATE = "query_date";
    private static final String OTA_UPGRADE_STARTED = "upgrade_started";
    private static final String OTA_FULL_PKG = "is_full_pkg";
    private static final String OTA_ANDR_VER = "android_num";
    private static final String WIFI_DOWNLOAD_ONLY = "wifi_download_only";
    private static final String OTA_AUTO_DOWNLOADING = "ota_auto_downloading";
    private static final String PAUSE_WITHIN_TIME = "pause_whthin_time";
    private static final String NEED_REFRESH_PACKAGE = "need_refresh_package";
    private static final String NEED_REFRESH_MENU = "need_refresh_menu";
    private static final String IS_SHUTTING_DOWN = "is_shutting_down";
    private static final String ACTIVITY_ID = "activity_id";
    private static final String TARGET_VERSION = "target_version";
    private static final String HAS_NOTIFY = "has_notify_backup";

    private SharedPreferences mPreference = null;

    private static DownloadInfo sDownloadInfo = null;
    private static final String TAG = "SystemUpdate/DownloadInfo";

    static synchronized DownloadInfo getInstance(Context context) {

        if (sDownloadInfo == null) {
            sDownloadInfo = new DownloadInfo(context);
        }

        return sDownloadInfo;
    }

    private DownloadInfo(Context context) {

        mPreference = context.getSharedPreferences(OTA_PREFERENCE, Context.MODE_PRIVATE);

    }

    /**
     * Return the size of the upgrade package to download in bytes.
     *
     * @return the size of the upgrade package to download in bytes.
     */
    long getUpdateImageSize() {
        return mPreference.getLong(OTA_PRE_IMAGE_SIZE, -1);
    }

    void setUpdateImageSize(long size) {
        Log.i("@M_" + TAG, "setUpdateImageSize " + size);
        mPreference.edit().putLong(OTA_PRE_IMAGE_SIZE, size).commit();
    }

    int getDLSessionDeltaId() {
        return mPreference.getInt(OTA_PRE_DELTA_ID, -1);
    }

    void setDLSessionDeltaId(int deltaId) {
        Log.i("@M_" + TAG, "setDLSessionDeltaId " + deltaId);
        mPreference.edit().putInt(OTA_PRE_DELTA_ID, deltaId).commit();
    }

    boolean getOtaAutoDlStatus() {
        return mPreference.getBoolean(OTA_AUTO_DOWNLOADING, false);
    }

    void setOtaAutoDlStatus(boolean autoDL) {
        Log.i("@M_" + TAG, "setOtaAutoDlStatus " + autoDL);
        mPreference.edit().putBoolean(OTA_AUTO_DOWNLOADING, autoDL).commit();
    }

    boolean getIfPauseWithinTime() {
        return mPreference.getBoolean(PAUSE_WITHIN_TIME, false);
    }

    void setIfPauseWithinTime(boolean within) {
        Log.i("@M_" + TAG, "setIfPauseWithinTime " + within);
        mPreference.edit().putBoolean(PAUSE_WITHIN_TIME, within).commit();
    }

    /**
     * Return the Android version of the upgrade package.
     *
     * @return the Android version of the upgrade package.
     */
    String getAndroidNum() {
        return mPreference.getString(OTA_ANDR_VER, "");
    }

    void setAndroidNum(String version) {
        mPreference.edit().putString(OTA_ANDR_VER, version).commit();
    }

    /**
     * Return the version number of the upgrade package.
     *
     * @return the version number of the upgrade package.
     */
    String getVerNum() {
        return mPreference.getString(OTA_PRE_VER, "");
    }

    void setVerNum(String version) {
        mPreference.edit().putString(OTA_PRE_VER, version).commit();
    }

    String getTargetVer() {
        return mPreference.getString(TARGET_VERSION, "");
    }

    void setTargetVer(String version) {
        mPreference.edit().putString(TARGET_VERSION, version).commit();
    }

    String getVersionNote() {
        return mPreference.getString(OTA_PRE_VER_NOTE, "");
    }

    void setVersionNote(String note) {
        Log.i("@M_" + TAG, "setVersionNote " + note);
        mPreference.edit().putString(OTA_PRE_VER_NOTE, note).commit();
    }

    /**
     * Return the downloading percent of the upgrade package.
     *
     * @return the downloading percent of the upgrade package.
     */
    int getDownLoadPercent() {
        return mPreference.getInt(OTA_PRE_DOWNLOAND_PERCENT, -1);
    }

    /**
     * Set the downloading percent of the upgrade package.
     *
     * @param percent
     *            the downloading percent of the upgrade package.
     */
    void setDownLoadPercent(int percent) {
        Log.i("@M_" + TAG, "setDownLoadPercent, percent = " + percent);
        mPreference.edit().putInt(OTA_PRE_DOWNLOAND_PERCENT, percent).commit();
    }

    boolean getFullPkgFlag() {
        return mPreference.getBoolean(OTA_FULL_PKG, false);
    }

    void setFullPkgFlag(boolean flag) {
        Log.i("@M_" + TAG, "setFullPkgFlag " + flag);
        mPreference.edit().putBoolean(OTA_FULL_PKG, flag).commit();
    }

    void resetDownloadInfo() {
        Log.i("@M_" + TAG, "resetDownloadInfo");
        setDownLoadPercent(-1);
        setDLSessionUnzipState(false);
        setDLSessionRenameState(false);
        setDLSessionStatus(DownloadInfo.STATE_QUERYNEWVERSION);

        setActivityID(-1);
        setIfNeedRefresh(true);

        mPreference.edit().putString(OTA_PRE_VER_NOTE, null).commit();
        mPreference.edit().putLong(OTA_PRE_IMAGE_SIZE, -1).commit();

        mPreference.edit().putInt(OTA_PRE_DELTA_ID, -1).commit();
        setNotifyStatus(false);
    }

    /*
     * public void setDownloadDesctiptor(DownloadDescriptor savedDd) { if
     * (savedDd == null) { return; } Log.i("@M_" + TAG,
     * "setDownloadDesctiptor, savedDd.version = " + savedDd.version);
     * Log.i("@M_" + TAG, "setDownloadDesctiptor, savedDd.size = " + savedDd.size);
     * mVersionInfo = savedDd; mPreference.edit().putString(OTA_PRE_VER_NOTE,
     * savedDd.newNote) .commit();
     * mPreference.edit().putLong(OTA_PRE_IMAGE_SIZE, savedDd.size).commit(); if
     * (savedDd.version != null) { mPreference.edit().putString(OTA_PRE_VER,
     * savedDd.version).commit(); }
     *
     * mPreference.edit().putInt(OTA_PRE_DELTA_ID, savedDd.pkgId).commit(); }
     */

    /**
     * Return the status during the whole upgrade process.
     *
     * @return the status during the whole upgrade process.
     */
    int getDLSessionStatus() {
        int status = mPreference.getInt(OTA_PRE_STATUS, DownloadInfo.STATE_QUERYNEWVERSION);
        // Log.i("@M_" + TAG, "getDLSessionStatus, get status = "+status);
        return status;
    }

    /**
     * Set the status during the whole upgrade process.
     *
     * @param status
     *            the status during the whole upgrade process to set
     * @see IDownloadStatus.
     */
    void setDLSessionStatus(int status) {
        Log.i("@M_" + TAG, "setDLSessionStatus, status = " + status);
        mPreference.edit().putInt(OTA_PRE_STATUS, status).commit();
    }

    /**
     * Get the status that if the upgrade package has been decompressed.
     *
     * @return true if decompressed, else false.
     */

    boolean getDLSessionUnzipState() {
        boolean status = mPreference.getBoolean(OTA_UNZ_STATUS, false);
        Log.i("@M_" + TAG, "getDLSessionUnzipState, get status = " + status);
        return status;
    }

    /**
     * Set the status that if the upgrade package has been decompressed.
     *
     * @param status
     *            the decompression status to set. True if decompressed, else
     *            false.
     */
    void setDLSessionUnzipState(boolean status) {
        Log.i("@M_" + TAG, "setDLSessionUnzipState, status = " + status);
        mPreference.edit().putBoolean(OTA_UNZ_STATUS, status).commit();
    }

    /**
     * Get the status that if the upgrade package is renamed.
     *
     * @return true if renamed, else false.
     */
    boolean getDLSessionRenameState() {
        boolean status = mPreference.getBoolean(OTA_REN_STATUS, false);
        Log.i("@M_" + TAG, "getDLSessionRenameState, get status = " + status);
        return status;
    }

    /**
     * Set the status that if the upgrade package is renamed.
     *
     * @param status
     *            the renamed status to set. True if renamed, else false.
     */
    void setDLSessionRenameState(boolean status) {
        Log.i("@M_" + TAG, "setDLSessionRenameState, status = " + status);
        mPreference.edit().putBoolean(OTA_REN_STATUS, status).commit();
    }

    /**
     * Get the last time the device access server to query for upgrade package.
     *
     * @return A string of the date.
     */
    String getQueryDate() {

        String strTime = mPreference.getString(OTA_QUERY_DATE, null);
        Log.i("@M_" + TAG, "getQueryTime, time = " + strTime);
        return strTime;
    }

    /**
     * Set the last time the device access server to query for upgrade package.
     *
     * @param strTime
     *            the date string to set.
     */
    void setQueryDate(String strTime) {
        Log.i("@M_" + TAG, "setQueryTime, time = " + strTime);
        mPreference.edit().putString(OTA_QUERY_DATE, strTime).commit();
    }

    /**
     * Get a flag to indicate if one upgrade process has been started before.
     *
     * @return true if started, else false.
     */
    boolean getUpgradeStartedState() {
        boolean status = mPreference.getBoolean(OTA_UPGRADE_STARTED, false);
        Log.i("@M_" + TAG, "getUpgradeStartedState, get status = " + status);
        return status;
    }

    /**
     * Set a flag to indicate if one upgrade process has been started before.
     *
     * @param status
     *            the status to set. True if started, else false.
     */
    void setUpgradeStartedState(boolean status) {
        Log.i("@M_" + TAG, "setUpgradeStartedState, status = " + status);
        mPreference.edit().putBoolean(OTA_UPGRADE_STARTED, status).commit();
    }

    /**
     * Get a flag to indicate if user prefers to use wifi network only.
     *
     * @return true if prefers, else false.
     */
    boolean getIfWifiDLOnly() {
        boolean wifiOnly = mPreference.getBoolean(WIFI_DOWNLOAD_ONLY, true);
        Log.i("@M_" + TAG, "getIfWifiDLOnly, get result = " + wifiOnly);
        return wifiOnly;
    }

    /**
     * Set a flag to indicate if user prefers to use wifi network only.
     *
     * @param status
     *            the status to set. True if prefers, else false.
     */
    void setIfWifiDLOnly(boolean wifiOnly) {
        Log.i("@M_" + TAG, "setIfWifiDLOnly, wifiOnly = " + wifiOnly);
        mPreference.edit().putBoolean(WIFI_DOWNLOAD_ONLY, wifiOnly).commit();
    }

    /**
     * Get detail activity's UpdatePackageInfo order in pkg_info.xml.
     *
     * @return the order of the info in pkg_info.xml.
     */
    int getActivityID() {
        int item = mPreference.getInt(ACTIVITY_ID, -1);
        Log.i("@M_" + TAG, "getActivityID, get result = " + item);
        return item;
    }

    /**
     * Set a flag to indicate if user prefers to use wifi network only.
     *
     * @param status
     *            the status to set. True if prefers, else false.
     */
    void setActivityID(int order) {
        Log.i("@M_" + TAG, "setActivityID, id = " + order);
        mPreference.edit().putInt(ACTIVITY_ID, order).commit();
    }

    /**
     * Get a flag to indicate if need refresh the update packages.
     *
     * @return true if need, else false.
     */
    boolean getIfNeedRefresh() {
        boolean needRefresh = mPreference.getBoolean(NEED_REFRESH_PACKAGE, true);
        Log.i("@M_" + TAG, "getIfNeedRefresh, get result = " + needRefresh);
        return needRefresh;
    }

    /**
     * Set a flag to indicate if need refresh the update packages.
     *
     * @param status
     *            the status to set. True if need, else false.
     */
    void setIfNeedRefresh(boolean needRefresh) {
        Log.i("@M_" + TAG, "setIfNeedRefresh, needRescan = " + needRefresh);
        mPreference.edit().putBoolean(NEED_REFRESH_PACKAGE, needRefresh).commit();
    }

    /**
     * Get a flag to indicate if need a refresh menu in detail activity.
     *
     * @return true if need, else false.
     */
    boolean getIfNeedRefreshMenu() {
        boolean needMenu = mPreference.getBoolean(NEED_REFRESH_MENU, false);
        Log.i("@M_" + TAG, "getIfNeedRefreshMenu, get result = " + needMenu);
        return needMenu;
    }

    /**
     * Set a flag to indicate if need a refresh menu in detail activity.
     *
     * @param status
     *            the status to set. True if need, else false.
     */
    void setIfNeedRefreshMenu(boolean needMenu) {
        Log.i("@M_" + TAG, "setIfNeedRefreshMenu, needMenu = " + needMenu);
        mPreference.edit().putBoolean(NEED_REFRESH_MENU, needMenu).commit();
    }

    /**
     * Get a flag to indicate if in the shut down progress.
     *
     * @return true if need, else false.
     */
    boolean getIsShuttingDown() {
        boolean isShuttingDown = mPreference.getBoolean(IS_SHUTTING_DOWN, false);
        Log.i("@M_" + TAG, "getIsShuttingDown, get result = " + isShuttingDown);
        return isShuttingDown;
    }

    /**
     * Set a flag to indicate if in Shutdown progress.
     *
     * @param status
     *            the status to set. True if is shutting down, else false.
     */
    void setIsShuttingDown(boolean shuttingDown) {
        Log.i("@M_" + TAG, "setIsShuttingDown, shuttingDown = " + shuttingDown);
        mPreference.edit().putBoolean(IS_SHUTTING_DOWN, shuttingDown).commit();
    }
    /**
     * Get a flag to indicate if has notify user to back up data.
     *
     * @return true if has notified, else false.
     */
    boolean getNotifyStatus() {
        boolean hasNotify = mPreference.getBoolean(HAS_NOTIFY, false);
        Log.i("@M_" + TAG, "getNotifyStatus, get result = " + hasNotify);
        return hasNotify;
    }

    /**
     * Set a flag to indicate if has notify user to backup data.
     *
     * @param status
     *            the status to set. True if has notified, else false.
     */
    void setNotifyStatus(boolean hasNotify) {
        Log.i("@M_" + TAG, "setNotifyStatus, hasNotify = " + hasNotify);
        mPreference.edit().putBoolean(HAS_NOTIFY, hasNotify).commit();
    }
}

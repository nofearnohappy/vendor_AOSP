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

package com.mediatek.dm.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.VdmException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PersistentContext implements IDmPersistentValues {
    private static final Object LOCK_OBJ = new Object();
    private static final String DM_VALUES = "dm_values";
    private static PersistentContext sInstance;

    private final Context mContext;
    private boolean mNeedResumeDl;

    private DownloadDescriptor mDd;
    private DownloadInfo mInfo;
    private Map<String, String> mCacheValues = new HashMap<String, String>();

    // use 2 files to make recoverable from power lost.
    private final StateValues mValues;
    private static ArrayList<FumoUpdateObserver> sFumoObservers = new ArrayList<FumoUpdateObserver>();

    private PersistentContext(Context context) {
        mContext = context;
        mValues = new StateValues(context, DM_VALUES);
        loadCache();
    }

    private void loadCache() {
        mValues.load();
        mCacheValues.put(StateValues.ST_DOWNLOADED_SIZE,
                mValues.get(StateValues.ST_DOWNLOADED_SIZE));

        String value = mValues.get(StateValues.ST_STATE);
        int state = STATE_NOT_DOWNLOAD;
        if (!TextUtils.isEmpty(value)) {
            state = Integer.valueOf(value);
        }
        if (state == STATE_DOWNLOADING || state == STATE_RESUME_DOWNLOAD
                || state == STATE_START_TO_DOWNLOAD) {
            value = Integer.toString(STATE_PAUSE_DOWNLOAD);
            mValues.put(StateValues.ST_STATE, value);
            mValues.commit();
        } else if (state == STATE_NEW_VERSION_DETECTED) {
            mNeedResumeDl = true;
        }
        mCacheValues.put(StateValues.ST_STATE, value);
        Log.d(TAG.COMMON, "--- [Persistent] loading, DL state is " + value + " ---");
    }

    public static PersistentContext getInstance(Context context) {
        synchronized (LOCK_OBJ) {
            if (sInstance == null) {
                sInstance = new PersistentContext(context);
            }
        }
        return sInstance;
    }

    /**
     * Once queried new version, DL session interrupt unexpected. restart DM, mark it
     */
    public boolean getIsNeedResumeDLSession() {
        Log.d(TAG.COMMON, "--- [PersistentContext] getIsNeedResumeDLSession = " + mNeedResumeDl);
        return mNeedResumeDl;
    }

    public void setIsNeedResumeDLSession(boolean b) {
        mNeedResumeDl = b;
    }

    @Override
    public long getMaxSize() {
        long maxSize = getTotalInternalMemorySize();
        Log.d(TAG.COMMON, "[persistent]:MAX-SIZE=" + maxSize);
        return maxSize;
    }

    @Override
    public long getDownloadedSize() {
        String value;
        if (mCacheValues.containsKey(StateValues.ST_DOWNLOADED_SIZE)) {
            Log.v(TAG.COMMON, "[persistent]:get DOWNLOADED_SIZE from cache");
            value = mCacheValues.get(StateValues.ST_DOWNLOADED_SIZE);
        } else {
            mValues.load();
            value = mValues.get(StateValues.ST_DOWNLOADED_SIZE);
            mCacheValues.put(StateValues.ST_DOWNLOADED_SIZE, value);
        }

        long dlSize = 0L;
        if (!TextUtils.isEmpty(value)) {
            dlSize = Long.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:DOWNLOADED_SIZE->get=" + dlSize);
        return dlSize;
    }

    @Override
    public void setDownloadedSize(long size) {
        String dlSize = Long.toString(size);
        Log.d(TAG.COMMON, "[persistent]:DOWNLOADED_SIZE->set=" + dlSize);

        mValues.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
        mValues.commit();
        mCacheValues.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
    }

    @Override
    public long getSize() {
        String value;
        if (mCacheValues.containsKey(StateValues.DD_SIZE)) {
            Log.v(TAG.COMMON, "[persistent]:get DD_SIZE from cache");
            value = mCacheValues.get(StateValues.DD_SIZE);
        } else {
            mValues.load();
            value = mValues.get(StateValues.DD_SIZE);
            mCacheValues.put(StateValues.DD_SIZE, value);
        }

        long size = 0L;
        if (!TextUtils.isEmpty(value)) {
            size = Long.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:DD_SIZE->get=" + size);
        return size;
    }

    @Override
    public int getDLSessionStatus() {
        // default session state value for DM, will check network in first step.
        String value;
        if (mCacheValues.containsKey(StateValues.ST_STATE)) {
            Log.v(TAG.COMMON, "[persistent]:get ST_STATE from cache");
            value = mCacheValues.get(StateValues.ST_STATE);
        } else {
            mValues.load();
            value = mValues.get(StateValues.ST_STATE);
            mCacheValues.put(StateValues.ST_STATE, value);
        }

        int state = STATE_NOT_DOWNLOAD;
        if (!TextUtils.isEmpty(value)) {
            state = Integer.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:DL_STATE->get=" + state);
        return state;
    }

    @Override
    public void setDLSessionStatus(int status) {
        String state = Integer.toString(status);
        Log.d(TAG.COMMON, "[persistent]:DL_STATE->set=" + state);

        if (!mCacheValues.containsKey(StateValues.ST_STATE)
                || !state.equals(mCacheValues.get(StateValues.ST_STATE))) {
            mValues.put(StateValues.ST_STATE, state);
            mValues.commit();
            mCacheValues.put(StateValues.ST_STATE, state);
        }

        notifyObserver(status, -1);
    }

    public int getDMSessionStatus() {
        String value = "";
        if (mCacheValues.containsKey(StateValues.DM_STATE)) {
            Log.v(TAG.COMMON, "[persistent]:get DM_STATE from cache");
            value = mCacheValues.get(StateValues.DM_STATE);
        }

        int dmStatus = STATE_DM_NO_ACTION;
        if (!TextUtils.isEmpty(value)) {
            dmStatus = Integer.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:DM_STATE->get=" + dmStatus);
        return dmStatus;
    }

    public void setDMSessionStatus(int dmStatus) {
        String state = Integer.toString(dmStatus);
        Log.d(TAG.COMMON, "[persistent]:DM_STATE->set=" + state);

        mCacheValues.put(StateValues.DM_STATE, state);

        notifyObserver(-1, dmStatus);
    }

    public int getFumoErrorCode() {
        String value = "";
        if (mCacheValues.containsKey(StateValues.ERROR_CODE)) {
            Log.v(TAG.COMMON, "[persistent]:get ERROR_CODE from cache");
            value = mCacheValues.get(StateValues.ERROR_CODE);
        }

        int errorCode = VdmException.VdmError.OK.val;
        if (!TextUtils.isEmpty(value)) {
            errorCode = Integer.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:ERROR_CODE->get=" + errorCode);
        return errorCode;
    }

    public void setFumoErrorCode(int errorCode) {
        String code = Integer.toString(errorCode);
        Log.d(TAG.COMMON, "[persistent]:ERROR_CODE->set=" + code);

        mCacheValues.put(StateValues.ERROR_CODE, code);
    }

    public boolean getIsUpdateRecovery() {
        String value = "";
        if (mCacheValues.containsKey(StateValues.UPDATE_RECOVERY)) {
            Log.v(TAG.COMMON, "[persistent]:get IsUpdateRecovery from cache");
            value = mCacheValues.get(StateValues.UPDATE_RECOVERY);
        } else {
            mValues.load();
            value = mValues.get(StateValues.UPDATE_RECOVERY);
            mCacheValues.put(StateValues.UPDATE_RECOVERY, value);
        }

        boolean isUpdate = false;
        if (!TextUtils.isEmpty(value)) {
            isUpdate = Boolean.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:IsUpdateRecovery->get=" + isUpdate);
        return isUpdate;
    }

    public void setIsUpdateRecovery(boolean isUpdate) {
        String state = Boolean.toString(isUpdate);
        Log.d(TAG.COMMON, "[persistent]:IsUpdateRecovery->set=" + isUpdate);

        if (!mCacheValues.containsKey(StateValues.UPDATE_RECOVERY)
                || !state.equals(mCacheValues.get(StateValues.UPDATE_RECOVERY))) {
            mValues.put(StateValues.UPDATE_RECOVERY, state);
            mValues.commit();
            mCacheValues.put(StateValues.UPDATE_RECOVERY, state);
        }
    }

    public int getPostPoneTimes() {
        String value = "";
        if (mCacheValues.containsKey(StateValues.POSTPONE_TIMES)) {
            Log.v(TAG.COMMON, "[persistent]:get POSTPONE_TIMES from cache");
            value = mCacheValues.get(StateValues.POSTPONE_TIMES);
        }

        int times = 0;
        if (!TextUtils.isEmpty(value)) {
            times = Integer.valueOf(value);
        }

        Log.d(TAG.COMMON, "[persistent]:POSTPONE_TIMES->get=" + times);
        return times;
    }

    public void setPostPoneTimes() {
        int times = getPostPoneTimes() + 1;
        String code = Integer.toString(times);
        Log.d(TAG.COMMON, "[persistent]:POSTPONE_TIMES->set=" + times);

        mCacheValues.put(StateValues.POSTPONE_TIMES, code);
    }

    public void clearPostPoneTimes() {
        String code = Integer.toString(0);
        Log.d(TAG.COMMON, "[persistent]:clear POSTPONE_TIMES");

        mCacheValues.put(StateValues.POSTPONE_TIMES, code);
    }

    @Override
    public DownloadDescriptor getDownloadDescriptor() {
        if (mDd == null) {
            DownloadDescriptor dd = new DownloadDescriptor();

            mValues.load();
            dd.field[0] = mValues.get(StateValues.DD_FIELD0);
            dd.field[1] = mValues.get(StateValues.DD_FIELD1);
            dd.field[2] = mValues.get(StateValues.DD_FIELD2);
            dd.field[3] = mValues.get(StateValues.DD_FIELD3);
            dd.field[4] = mValues.get(StateValues.DD_FIELD4);
            dd.field[5] = mValues.get(StateValues.DD_FIELD5);
            dd.field[6] = mValues.get(StateValues.DD_FIELD6);
            dd.field[7] = mValues.get(StateValues.DD_FIELD7);
            dd.field[8] = mValues.get(StateValues.DD_FIELD8);
            dd.field[9] = mValues.get(StateValues.DD_FIELD9);
            dd.field[10] = mValues.get(StateValues.DD_FIELD10);
            dd.field[11] = mValues.get(StateValues.DD_FIELD11);

            String value = mValues.get(StateValues.DD_SIZE);
            if (!TextUtils.isEmpty(value)) {
                dd.size = Long.valueOf(value);
            } else {
                Log.w(TAG.COMMON, "[persistent]:getDownloadDescriptor DD_SIZE error");
            }
            mDd = dd;
        }
        return mDd;
    }

    @Override
    public void setDownloadDescriptor(DownloadDescriptor dd) {
        if (dd == null) {
            throw new RuntimeException("You can't save an empty DD.");
        }
        String totalSize = Long.toString(dd.size);
        mValues.put(StateValues.DD_FIELD0, dd.field[0]);
        mValues.put(StateValues.DD_FIELD1, dd.field[1]);
        mValues.put(StateValues.DD_FIELD2, dd.field[2]);
        mValues.put(StateValues.DD_FIELD3, dd.field[3]);
        mValues.put(StateValues.DD_FIELD4, dd.field[4]);
        mValues.put(StateValues.DD_FIELD5, dd.field[5]);
        mValues.put(StateValues.DD_FIELD6, dd.field[6]);
        mValues.put(StateValues.DD_FIELD7, dd.field[7]);
        mValues.put(StateValues.DD_FIELD8, dd.field[8]);
        mValues.put(StateValues.DD_FIELD9, dd.field[9]);
        mValues.put(StateValues.DD_FIELD10, dd.field[10]);
        mValues.put(StateValues.DD_FIELD11, dd.field[11]);
        mValues.put(StateValues.DD_SIZE, totalSize);

        mValues.commit();
        mDd = dd;
        mInfo = null;
        mCacheValues.put(StateValues.DD_SIZE, totalSize);
        Log.d(TAG.COMMON, "[persistent]: dd saved.");
    }

    @Override
    public void deleteDeltaPackage() {
        Log.d(TAG.COMMON, "[persistent]: delete package.");
        mContext.deleteFile(DELTA_FILE_NAME);
        mContext.deleteFile(RESUME_FILE_NAME);

        String dlstate = Integer.toString(STATE_NOT_DOWNLOAD);
        mValues.put(StateValues.ST_DOWNLOADED_SIZE, "");
        mValues.put(StateValues.DD_SIZE, "");
        mValues.put(StateValues.ST_STATE, dlstate);
        mValues.put(StateValues.UPDATE_RECOVERY, "");
        mValues.commit();
        mCacheValues.clear();
    }

    @Override
    public DownloadInfo getDownloadInfo() {
        if (mInfo == null) {
            DownloadInfo info = new DownloadInfo();
            mValues.load();
            info.mUrl = mValues.get(StateValues.DD_FIELD1);
            info.mVersion = mValues.get(StateValues.DD_FIELD4);
            info.mDescription = mValues.get(StateValues.DD_FIELD6);
            mInfo = info;
        }

        return mInfo;
    }

    private static long getTotalInternalMemorySize() {
        File dmFolder = new File(DmConst.PathName.PATH_IN_DATA);
        long availableSize = dmFolder.getFreeSpace() - 1000000;
        return availableSize;
    }

    public void resetDLStatus() {
        Log.d(TAG.COMMON, "[persistent]: reset DL Status");
        deleteDeltaPackage();
        clearPostPoneTimes();
        setDLSessionStatus(STATE_NOT_DOWNLOAD);
    }

    public boolean registerObserver(FumoUpdateObserver observer) {
        if (sFumoObservers == null) {
            return false;
        }
        sFumoObservers.add(observer);
        return true;
    }

    public void clearObserver() {
        if (sFumoObservers != null) {
            sFumoObservers.clear();
        }

        sFumoObservers = null;
    }

    public void unregisterObserver(FumoUpdateObserver observer) {
        if (sFumoObservers == null || sFumoObservers.isEmpty()) {
            return;
        }
        sFumoObservers.remove(observer);
    }

    private void notifyObserver(int dlStatus, int dmStatus) {
        if (sFumoObservers == null || sFumoObservers.size() <= 0) {
            return;
        }
        for (FumoUpdateObserver observer : sFumoObservers) {
            if (dlStatus >= 0) {
                observer.syncDLstatus(dlStatus);
            }
            if (dmStatus > 0) {
                observer.syncDmSession(dmStatus);
            }
        }
    }

    public interface FumoUpdateObserver {
        void syncDLstatus(int status);

        void syncDmSession(int status);
    }

}

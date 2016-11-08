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

package com.mediatek.dm.scomo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.mediatek.dm.ext.MTKFileUtil;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DmScomoState implements Serializable {
    /**
     * record scomo state into a file.
     */
    private static final String CLASS_TAG = TAG.SCOMO + "/State";
    private static final long serialVersionUID = 1L;

    private static final String SCOMO_STATE_FILE = "scomo_state";

    public static final int IDLE = 0;

    public static final int DOWNLOAD_VALUE_BEGIN = 1;
    public static final int DOWNLOADING_STARTED = 2;
    public static final int DOWNLOADING = 3;
    public static final int PAUSED = 4;
    public static final int CONFIRM_DOWNLOAD = 5;
    public static final int DOWNLOAD_FAILED = 6;
    public static final int ABORTED = 7;
    // these two states are used to fix the problem that `engine will download
    // some more packages after dl session is cancelled`
    // public static final int STARTED = 8;
    public static final int RESUMED = 9;
    public static final int WRONG_PACKAGE_FORMAT = 10;
    public static final int DOWNLOAD_VALUE_END = 20;

    public static final int INSTALLING = 21;
    public static final int UPDATING = 22;
    public static final int CONFIRM_INSTALL = 23;
    public static final int CONFIRM_UPDATE = 24;
    public static final int INSTALL_FAILED = 25;
    public static final int INSTALL_OK = 26;

    public static final int GENERIC_ERROR = 27;
    public String mErrorMessage;

    public int mState = IDLE;
    public int mCurrentSize = 0;
    public int mTotalSize = 0;
    public VdmScomoDp mCurrentDp;
    public DownloadDescriptor mCurrentDd;
    public DmScomoPackageManager.ScomoPackageInfo mPackageInfo;
    public String mArchiveFilePath = "";
    /**
     * used by scomo listener to decide whether to interact with user. mVerbose==false will hide any
     * UI. mVerbose will be set in MmiConfirmation (ALERT 1101), and be reset after DM session
     */
    public boolean mVerbose = false;
    private static DmScomoState mDmScomoState;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(this.mArchiveFilePath);
        out.writeInt(this.mState);
        out.writeInt(this.mCurrentSize);
        out.writeInt(this.mTotalSize);
        out.writeBoolean(mVerbose);
        if (this.mCurrentDp != null && this.mCurrentDp.getName() != null) {
            out.writeUTF(this.mCurrentDp.getName());
        } else {
            out.writeUTF("");
        }

        // write dd
        if (mCurrentDd == null) {
            out.writeLong(0);
        } else {
            out.writeLong(mCurrentDd.size);
            out.writeObject(mCurrentDd.field);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.mArchiveFilePath = in.readUTF();
        this.mState = in.readInt();
        this.mCurrentSize = in.readInt();
        this.mTotalSize = in.readInt();
        this.mVerbose = in.readBoolean();
        // reload dp
        String dpName = in.readUTF();
        if (!dpName.equals("")) {
            try {
                this.mCurrentDp = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT,
                        DmScomoHandler.getInstance()).createDP(dpName,
                        DmScomoDpHandler.getInstance());
            } catch (VdmException e) {
                Log.e(CLASS_TAG, "readObject, getVdmScomo instance error!");
                e.printStackTrace();
                this.mCurrentDp = null;
            }
        }

        // reload dd
        long ddSize = in.readLong();
        if (ddSize == 0) {
            mCurrentDd = null;
        } else {
            this.mCurrentDd = new DownloadDescriptor();
            this.mCurrentDd.size = ddSize;
            this.mCurrentDd.field = (String[]) in.readObject();
        }

        switch (mState) {
        case INSTALLING:
        case UPDATING:
        case CONFIRM_INSTALL:
        case CONFIRM_UPDATE:
            mState = CONFIRM_INSTALL;
            // reload pkgInfo from archivePath;
            setArchivePath(mArchiveFilePath);
            break;
        case DOWNLOADING:
        case DOWNLOADING_STARTED:
        case RESUMED:
        case PAUSED:
            mState = PAUSED;
            break;
        default:
            Log.w(CLASS_TAG, "abnormal exit, reset scomo state and delete delta files");
            resetState();
            break;
        }
    }

    private void resetState() {
        DmService.getInstance().deleteScomoFile();
        this.mState = IDLE;
        this.mCurrentDd = null;
        this.mCurrentDp = null;
        this.mPackageInfo = null;
        this.mCurrentSize = 0;
        this.mTotalSize = 0;
    }

    public void setArchivePath(final String path) {
        this.mArchiveFilePath = path;
        this.mPackageInfo = DmScomoPackageManager.getInstance().getMinimalPackageInfo(path);
    }

    public static DmScomoState getInstance(final Context context) {
        if (mDmScomoState == null) {
            Object obj = MTKFileUtil.atomicRead(context.getFileStreamPath(SCOMO_STATE_FILE));
            if (obj != null) {
                mDmScomoState = (DmScomoState) obj;
                Log.i(CLASS_TAG, "DmScomoState: state loaded: state= " + mDmScomoState.mState);
            } else {
                mDmScomoState = new DmScomoState();
            }
        }
        return mDmScomoState;
    }

    public static void store(Context context) {
        if (mDmScomoState == null) {
            return;
        }
        if (mDmScomoState.mState == IDLE) {
            Log.i(CLASS_TAG, "state is IDLE, reset state");
            mDmScomoState.resetState();
        }
        Log.d(CLASS_TAG, "store state = " + mDmScomoState.mState);
        MTKFileUtil.atomicWrite(context.getFileStreamPath(SCOMO_STATE_FILE), mDmScomoState);
    }

    // /// name,icon,version,description may from different source,e.g. PM > dd > dp ////
    public String getName() {
        String ret = "";
        if (mPackageInfo != null && mPackageInfo.mLabel != null) {
            ret = mPackageInfo.mLabel;
        } else if (mCurrentDd != null && mCurrentDd.getField(DownloadDescriptor.Field.NAME) != null) {
            ret = mCurrentDd.getField(DownloadDescriptor.Field.NAME);
        } else if (mCurrentDp != null && mCurrentDp.getName() != null) {
            ret = "";
        }
        return ret;
    }

    public String getVersion() {
        String ret = "";
        if (mPackageInfo != null && mPackageInfo.mVersion != null) {
            ret = mPackageInfo.mVersion;
        } else if (mCurrentDd != null
                && mCurrentDd.getField(DownloadDescriptor.Field.VERSION) != null) { //
            ret = mCurrentDd.getField(DownloadDescriptor.Field.VERSION);
        }
        return ret;
    }

    public Drawable getIcon() {
        Drawable ret;
        if (mPackageInfo != null) {
            // pkgInfo.icon is assured to be not-null
            ret = mPackageInfo.mIcon;
        } else {
            ret = DmScomoPackageManager.getInstance().getDefaultActivityIcon();
        }
        return ret;
    }

    public int getSize() {
        int ret = -1;
        if (this.mTotalSize != -1 && this.mTotalSize != 0) {
            ret = this.mTotalSize;
        } else if (mCurrentDd != null) {
            ret = (int) mCurrentDd.size;
        }
        return ret;

    }

    public CharSequence getDescription() {
        Log.i(CLASS_TAG, "getdescription begin");
        String ret = DmService.getInstance().getString(R.string.default_scomo_description);
        if (mPackageInfo != null && mPackageInfo.mDescription != null) {
            ret = mPackageInfo.mDescription;
        } else if (mCurrentDd != null
                && mCurrentDd.getField(DownloadDescriptor.Field.DESCRIPTION) != null) {
            ret = mCurrentDd.getField(DownloadDescriptor.Field.DESCRIPTION);
        }
        // else if (currentDp!=null) {
        // try {
        // Log.e(CLASS_TAG,"dp getdescription begin");
        // String tmp=currelntDp.getDescription();
        // Log.e(CLASS_TAG,"dp getdescription end");
        // if (tmp!=null) {
        // ret=tmp;
        // }
        // } catch (Exception e) {}
        // }
        Log.i(CLASS_TAG, "getdescription end");
        return ret;

    }

    public String getPackageName() {
        String ret = "";
        try {
            if (mPackageInfo != null && mPackageInfo.mName != null) {
                ret = mPackageInfo.mName;
            } else if (mCurrentDp != null && mCurrentDp.getPkgName() != null) {
                ret = mCurrentDp.getPkgName();
            }
        } catch (VdmException e) {
            Log.e(CLASS_TAG, "get packageName from CurrentDp error!");
        }
        return ret;
    }

    public boolean isAboutDownload() {
        return mState > DOWNLOAD_VALUE_BEGIN && mState < DOWNLOAD_VALUE_END;
    }
}

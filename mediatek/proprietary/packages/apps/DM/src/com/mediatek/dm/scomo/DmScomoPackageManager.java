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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.dm.DmApplication;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.ext.MTKMediaContainer;
import com.mediatek.dm.ext.MTKPackageManager;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;
import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDc;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DmScomoPackageManager {
    private static final String CLASS_TAG = TAG.SCOMO + "PackageManager";

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAILED_UPDATE = 1;
    public static final int STATUS_FAILED = 2;

    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer",
            "com.android.defcontainer.DefaultContainerService");

    private static final String THREAD_NAME = "scomo_thread";

    private PackageManager mPackageManager;
    private Context mContext;
    private HandlerThread mThread;
    private static DmScomoPackageManager sInstance;

    class ScomoPackageInfo {
        String mName;
        String mLabel;
        String mVersion;
        String mDescription;
        Drawable mIcon;
    }

    private MTKMediaContainer mMtkContainer;
    private ServiceConnection mContainerServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mMtkContainer = new MTKMediaContainer(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            mMtkContainer.finish();
            mMtkContainer = null;
        }

    };

    public static DmScomoPackageManager getInstance() {
        if (sInstance == null) {
            sInstance = new DmScomoPackageManager();
        }
        return sInstance;
    }

    public DmScomoPackageManager() {
        mContext = DmApplication.getInstance();

        mPackageManager = mContext.getPackageManager();

        Intent intent = new Intent();
        intent.setComponent(DEFAULT_CONTAINER_COMPONENT);
        mContext.bindService(intent, mContainerServiceConnection, Context.BIND_AUTO_CREATE);

        mThread = new HandlerThread(THREAD_NAME);
        mThread.start();
    }

    public HandlerThread getThread() {
        return mThread;
    }

    public boolean checkSpace(String archiveFilePath) {
        if (archiveFilePath == null) {
            return false;
        }
        if (mMtkContainer == null) { // not bounded yet
            return true;
        }
        return mMtkContainer.checkSpace(archiveFilePath);
    }

    public ScomoPackageInfo getPackageInfo(String pkgName) {
        if (pkgName == null || pkgName.length() == 0) {
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        ret.mName = pkgName;
        try {
            PackageInfo info = mPackageManager.getPackageInfo(pkgName, 0);
            ret.mVersion = info.versionName;
            // TODO: get package info description
            ret.mDescription = "test";
            return ret;
        } catch (NameNotFoundException e) {
            Log.e(CLASS_TAG, "getPackageInfo, package name not found for " + pkgName);
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void install(final String absPkgPath, final ScomoPackageInstallObserver observer,
            final boolean shouldUpdate) {
        Log.i(CLASS_TAG, new StringBuilder("pkgPath is ").append(absPkgPath).append(" should update ").append(shouldUpdate)
                .toString());
        if (absPkgPath == null) {
            new Handler(mContext.getMainLooper()).post(new Runnable() {
                public void run() {
                    if (observer != null) {
                        observer.packageInstalled(absPkgPath, STATUS_FAILED);
                    }
                }
            });
            return;
        }
        PackageManager pm = mContext.getPackageManager();
        int installFlag = 0;
        if (shouldUpdate) {
            installFlag |= MTKPackageManager.INSTALL_REPLACE_EXISTING;
        }
        Log.v(CLASS_TAG, "PM: about to install, install Flag is " + installFlag);
        File file = new File(absPkgPath);
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
        Log.v(CLASS_TAG, "open permission, file name is " + file.getAbsolutePath());
        MTKPackageManager.installPackage(pm, Uri.parse(absPkgPath), installFlag, new MTKPackageManager.InstallListener() {
            @Override
            public void packageInstalled(final String name, final int status) {
                new Handler(mContext.getMainLooper()).post(new Runnable() {
                    public void run() {
                        Log.i(CLASS_TAG,
                                new StringBuilder("PM: package installed, status: ").append(status).append(" packageName: ")
                                        .append(name).toString());
                        int ret;
                        if (status == MTKPackageManager.INSTALL_SUCCEEDED) {
                            ret = STATUS_OK;
                        } else if (!shouldUpdate && status == MTKPackageManager.INSTALL_FAILED_ALREADY_EXISTS) {
                            ret = STATUS_FAILED_UPDATE;
                        } else {
                            ret = STATUS_FAILED;
                        }
                        if (observer != null) {
                            observer.packageInstalled(name, ret);
                        }
                    }
                });
            }
        });

    }

    public interface ScomoPackageInstallObserver {
        void packageInstalled(String name, int status);
    }

    public void scanPackage() {
        Log.i(CLASS_TAG, "scanPackage: begin");
        if (DmService.getInstance() == null || !DmService.getInstance().isInitDmController()) {
            Log.w(CLASS_TAG, "scanPackage: DmService or DmController is not ready, return");
            return;
        }
        new Handler(mThread.getLooper()).post(new Runnable() {
            public void run() {
                DmScomoPackageManager.this.scanPackageInternal();
            }
        });
        Log.i(CLASS_TAG, "scanPackage: end");
    }

    private void scanPackageInternal() {
        try {
            List<ApplicationInfo> installedList = mPackageManager.getInstalledApplications(0);
            List<VdmScomoDc> dcs = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs();
            Set<String> dcsNames = new HashSet<String>();
            for (VdmScomoDc dc : dcs) {
                dcsNames.add(dc.getId());
            }
            Set<String> appNames = new HashSet<String>();
            for (ApplicationInfo appInfo : installedList) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    continue;
                }
                appNames.add(appInfo.packageName);
            }

            Set<String> dcsNamesTmp = new HashSet<String>(dcsNames);
            Set<String> appNamesTmp = new HashSet<String>(appNames);

            dcsNamesTmp.removeAll(appNames); // dcsNamesTmp now contains pkg
                                             // need to be removed from dcs
            appNamesTmp.removeAll(dcsNames); // appNamesTMp now contains pkg
                                             // need to be added to dcs

            VdmScomo scomo = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT, DmScomoHandler.getInstance());
            for (String pkgName : dcsNamesTmp) {
                Log.i(CLASS_TAG, "scanPackage: remove " + pkgName);
                VdmScomoDc dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(), DmPLInventory.getInstance());
                dc.deleteFromInventory();
                dc.destroy();
            }
            for (String pkgName : appNamesTmp) {
                Log.i(CLASS_TAG, "scanPackage: add " + pkgName);
                VdmScomoDc dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(), DmPLInventory.getInstance());
                dc.addToInventory(pkgName, pkgName, null, null, null, null, true);
            }
            new VdmTree().writeToPersistentStorage();
        } catch (VdmException e) {
            Log.e(CLASS_TAG, "scanPackageInternal error!");
            e.printStackTrace();
        }
    }

    public ScomoPackageInfo getMinimalPackageInfo(String archiveFilePath) {
        if (archiveFilePath == null) {
            Log.w(CLASS_TAG, "getMinimalPackageInfo fail for archiveFilePath is null");
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        MTKPackageManager.PackageInfo packInfo = MTKPackageManager.getPackageInfo(mPackageManager, mContext.getResources(),
                archiveFilePath);

        if (packInfo == null) {
            Log.w(CLASS_TAG, "get Package Info fail, the archiveFile Path is " + archiveFilePath);
            return null;
        }
        ret.mVersion = packInfo.mVersion;
        ret.mName = packInfo.mName;
        ret.mLabel = packInfo.mLabel;
        ret.mDescription = packInfo.mDescription;
        ret.mIcon = packInfo.mIcon;
        // ret.pkg = packInfo.pkg;

        return ret;
    }

    public boolean isPackageInstalled(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(pkgName, 0);
            return (pi != null);
        } catch (NameNotFoundException e) {
            Log.e(CLASS_TAG, pkgName + " is not found!!");
            e.printStackTrace();
            return false;
        }
    }

    public Drawable getDefaultActivityIcon() {
        return mPackageManager.getDefaultActivityIcon();
    }
}

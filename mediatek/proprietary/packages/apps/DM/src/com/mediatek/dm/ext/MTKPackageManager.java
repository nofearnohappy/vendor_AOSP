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

package com.mediatek.dm.ext;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.mediatek.dm.DmConst;

import java.io.File;

public final class MTKPackageManager {

    public static final int INSTALL_SUCCEEDED = PackageManager.INSTALL_SUCCEEDED;
    public static final int INSTALL_REPLACE_EXISTING = PackageManager.INSTALL_REPLACE_EXISTING;
    public static final int INSTALL_FAILED_ALREADY_EXISTS
            = PackageManager.INSTALL_FAILED_ALREADY_EXISTS;
    private static final String CLASS_TAG = DmConst.TAG.SCOMO + "/MTKPackageManager";

    public interface InstallListener {
        void packageInstalled(final String name, final int status);
    }

    public static final class PackageInfo {
        public String mName;
        public String mLabel;
        public String mVersion;
        public String mDescription;
        public Drawable mIcon;
        public Package mPackage;
    }

    public static PackageInfo getPackageInfo(PackageManager pm, Resources resources,
            String archiveFilePath) {
        PackageInfo ret = new PackageInfo();

        PackageParser packageParser = new PackageParser();
        File sourceFile = new File(archiveFilePath);
        PackageParser.Package pkg = null;
        try {
            pkg = packageParser.parsePackage(sourceFile, 0);
        } catch (PackageParser.PackageParserException e) {
            Log.e(CLASS_TAG, "in getPackageInfo() PackageParserException!");
            e.printStackTrace();
        }

        if (pkg == null) {
            Log.w(CLASS_TAG, "package Parser get package is null");
            return null;
        }
        packageParser = null;

        ret.mName = pkg.packageName;
        ret.mVersion = pkg.mVersionName;
        ret.mPackage = pkg;
        // get icon and label from archieve file
        ApplicationInfo appInfo = pkg.applicationInfo;
        AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(archiveFilePath);
        Resources res = new Resources(assmgr, resources.getDisplayMetrics(),
                resources.getConfiguration());
        CharSequence label = null;
        if (appInfo.labelRes != 0) {
            try {
                label = res.getText(appInfo.labelRes);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (label == null) {
            label = (appInfo.nonLocalizedLabel != null) ? appInfo.nonLocalizedLabel
                    : appInfo.packageName;
        }
        Drawable icon = null;
        if (appInfo.icon != 0) {
            try {
                icon = res.getDrawable(appInfo.icon);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }
        ret.mLabel = label.toString();
        ret.mIcon = icon;

        return ret;
    }

    public static void installPackage(PackageManager pm, Uri packUri, int flag,
            final InstallListener listener) {
        pm.installPackage(packUri, new IPackageInstallObserver.Stub() {
            public void packageInstalled(final String name, final int status) {
                listener.packageInstalled(name, status);
            }
        }, flag, null);
    }

}

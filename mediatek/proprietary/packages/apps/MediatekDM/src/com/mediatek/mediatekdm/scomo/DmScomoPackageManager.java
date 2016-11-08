package com.mediatek.mediatekdm.scomo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;

import java.io.File;

public class DmScomoPackageManager {

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAILED_UPDATE = 1;
    public static final int STATUS_FAILED = 2;

    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            "com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");

    private PackageManager mPackageManager;
    private Context mContext;
    private static DmScomoPackageManager sInstance = null;

    public class ScomoPackageInfo {
        public String name;
        public String label;
        public String version;
        public String description;
        public Drawable icon;
    }

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
    }

    public ScomoPackageInfo getPackageInfo(String pkgName) {
        if (pkgName == null || pkgName.length() == 0) {
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        ret.name = pkgName;
        try {
            PackageInfo info = mPackageManager.getPackageInfo(pkgName, 0);
            ret.version = info.versionName;
            ret.description = "test";
            return ret;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void install(final String absPkgPath,
            final ScomoPackageInstallObserver observer, final boolean shouldUpdate) {
        Log.i(TAG.SCOMO, "pkgPath is " + absPkgPath + " should update " + shouldUpdate);
        PackageManager pm = mContext.getPackageManager();
        int installFlag = 0;
        if (shouldUpdate) {
            installFlag |= PlatformManager.INSTALL_REPLACE_EXISTING;
            installFlag |= PlatformManager.INSTALL_ALLOW_DOWNGRADE;
        }
        Log.v(TAG.SCOMO, "PM: about to install, install Flag is " + installFlag);
        File file = new File(absPkgPath);
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
        Log.v(TAG.SCOMO, "open permission, file name is " + file.getAbsolutePath());
        PlatformManager.getInstance().installPackage(pm, Uri.parse(absPkgPath), installFlag,
                new PlatformManager.InstallListener() {
                    public void packageInstalled(final String name, final int status) {
                        Log.i(TAG.SCOMO, "PM: package installed, status: " + status
                                + " packageName: " + name);
                        int ret = STATUS_OK;
                        if (status == PlatformManager.INSTALL_SUCCEEDED) {
                            ret = STATUS_OK;
                        } else if (!shouldUpdate
                                && status == PlatformManager.INSTALL_FAILED_ALREADY_EXISTS) {
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

    public interface ScomoPackageInstallObserver {
        void packageInstalled(String name, int status);
    }

    public ScomoPackageInfo getMinimalPackageInfo(String archiveFilePath) {
        if (archiveFilePath == null) {
            Log.w(TAG.SCOMO, "getMinimalPackageInfo fail for archiveFilePath is null");
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        PlatformManager.PackageInfo packInfo = PlatformManager.getInstance().getPackageInfo(
                mPackageManager, mContext.getResources(), archiveFilePath);

        if (packInfo == null) {
            Log.w(TAG.SCOMO, "get Package Info fail, the archiveFile Path is " + archiveFilePath);
            return null;
        }
        ret.version = packInfo.version;
        ret.name = packInfo.name;
        ret.label = packInfo.label;
        ret.description = packInfo.description;
        ret.icon = packInfo.icon;
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
            e.printStackTrace();
            return false;
        }
    }

    public Drawable getDefaultActivityIcon() {
        return mPackageManager.getDefaultActivityIcon();
    }
}

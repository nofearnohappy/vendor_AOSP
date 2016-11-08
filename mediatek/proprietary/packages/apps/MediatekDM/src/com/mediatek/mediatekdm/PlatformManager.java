package com.mediatek.mediatekdm;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageParserException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.os.AtomicFile;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.conn.DmDatabase;
import com.mediatek.mediatekdm.util.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;

@SuppressWarnings("deprecation")
public class PlatformManager {
    public interface InstallListener {
        void packageInstalled(final String name, final int status);
    }

    public static final class PackageInfo {
        public String name;
        public String label;
        public String version;
        public String description;
        public Drawable icon;
        public Package pkg;
    }

    public static final int APN_ALREADY_ACTIVE = PhoneConstants.APN_ALREADY_ACTIVE;
    public static final int APN_TYPE_NOT_AVAILABLE = PhoneConstants.APN_TYPE_NOT_AVAILABLE;
    public static final int APN_REQUEST_FAILED = PhoneConstants.APN_REQUEST_FAILED;
    public static final int APN_REQUEST_STARTED = PhoneConstants.APN_REQUEST_STARTED;

    public static final int TYPE_MOBILE_DM = ConnectivityManager.TYPE_MOBILE_DM;
    public static final String SUBSCRIPTION_KEY = PhoneConstants.SUBSCRIPTION_KEY;

    public static final int INSTALL_SUCCEEDED = PackageManager.INSTALL_SUCCEEDED;
    public static final int INSTALL_ALLOW_DOWNGRADE = PackageManager.INSTALL_ALLOW_DOWNGRADE;
    public static final int INSTALL_REPLACE_EXISTING = PackageManager.INSTALL_REPLACE_EXISTING;
    public static final int INSTALL_FAILED_ALREADY_EXISTS = PackageManager.INSTALL_FAILED_ALREADY_EXISTS;

    public static final int DELAY_KICK_OFF = 90 * 1000;
    public static final int DELAY_REQUEST_NETWORK = 60 * 1000;

    private static PlatformManager sInstance;
    public WakeLock mFullWakelock = null;
    public WakeLock mPartialWakelock = null;
    public KeyguardLock mKeyguardLock = null;

    protected PlatformManager() {
    }

    public static synchronized PlatformManager getInstance() {
        if (sInstance == null) {
            sInstance = new PlatformManager();
        }
        return sInstance;
    }

    // ********************************************
    // Screen on/lock
    // ********************************************
    private PowerManager getPowerManager(Context context) {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    private KeyguardManager getKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    public void releaseWakeLock(Context context) {
        releasePartialWakeLock(context);
        releaseFullWakeLock(context);
    }

    public void enableKeyguard(Context context) {
        if (mKeyguardLock != null) {
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
            Log.d(TAG.PLATFORM, "enableKeyguard reenableKeyguard");
        } else {
            Log.d(TAG.PLATFORM, "enableKeyguard mKeyguardLock == null");
        }
    }

    public void releasePartialWakeLock(Context context) {
        if (mPartialWakelock != null) {
            if (mPartialWakelock.isHeld()) {
                mPartialWakelock.release();
                mPartialWakelock = null;
                Log.d(TAG.PLATFORM, "releasePartialWakeLock release");
            } else {
                Log.d(TAG.PLATFORM, "releasePartialWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG.PLATFORM, "releasePartialWakeLock mWakelock == null");
        }
    }

    public void releaseFullWakeLock(Context context) {
        if (mFullWakelock != null) {
            if (mFullWakelock.isHeld()) {
                mFullWakelock.release();
                mFullWakelock = null;
                Log.d(TAG.PLATFORM, "releaseFullWakeLock release");
            } else {
                Log.d(TAG.PLATFORM, "releaseFullWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG.PLATFORM, "releaseFullWakeLock mWakelock == null");
        }
    }

    public void disableKeyguard(Context context) {
        KeyguardManager km = getInstance().getKeyguardManager(context);

        if (mKeyguardLock == null) {
            // get KeyguardLock
            mKeyguardLock = km.newKeyguardLock("dm_KL");
            if (km.inKeyguardRestrictedInputMode()) {
                Log.d(TAG.PLATFORM, "need to disableKeyguard");
                // release key guard lock
                mKeyguardLock.disableKeyguard();
            } else {
                mKeyguardLock = null;
                Log.d(TAG.PLATFORM, "not need to disableKeyguard");
            }
        }
    }

    public void acquireFullWakelock(Context context) {
        PowerManager pm = getInstance().getPowerManager(context);

        if (mFullWakelock == null) {
            // get WakeLock
            mFullWakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "dm_FullLock");
            if (!mFullWakelock.isHeld()) {
                Log.d(TAG.PLATFORM, "need to aquire full wake up");
                // wake lock
                mFullWakelock.acquire();
            } else {
                mFullWakelock = null;
                Log.d(TAG.PLATFORM, "not need to aquire full wake up");
            }
        }
    }

    public void acquirePartialWakelock(Context context) {
        PowerManager pm = getInstance().getPowerManager(context);
        if (mPartialWakelock == null) {
            // get WakeLock
            mPartialWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dm_PartialLock");
            if (!mPartialWakelock.isHeld()) {
                Log.d(TAG.PLATFORM, "need to aquire partial wake up");
                // wake lock
                mPartialWakelock.acquire();
            } else {
                mPartialWakelock = null;
                Log.d(TAG.PLATFORM, "not need to aquire partial wake up");
            }
        }
    }

    /**
     * Start an alarm with certain intent.
     */
    public void startAlarm(Context context, PendingIntent pendingIntent, long delay) {
        Log.i(TAG.RECEIVER,
                "Start alarm, delay " + (delay / 1000) + "s, intent " + pendingIntent.getIntent());
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, java.lang.System.currentTimeMillis() + delay,
                pendingIntent);
    }

    // ********************************************
    // Telephony & Subscription
    // ********************************************
    public String getDeviceImei() {
        return TelephonyManager.getDefault().getDeviceId(PhoneConstants.SIM_ID_1);
    }

    public String getSubImsi(long subId) {
        return TelephonyManager.getDefault().getSubscriberId(subId);
    }

    public String getSubOperator(long subId) {
        return TelephonyManager.getDefault().getSimOperator(subId);
    }

    public Boolean isMobileDataEnabled() {
        return TelephonyManager.getDefault().getDataEnabled();
    }

    public void enableMoibleData() {
        TelephonyManager.getDefault().setDataEnabled(true);
    }

    public void disableMoibleData() {
        TelephonyManager.getDefault().setDataEnabled(false);
    }

    public long[] getSubIdList() {
        return SubscriptionManager.getActiveSubIdList();
    }

    // ********************************************
    // URI
    // ********************************************
    public Uri getDmContentUri() {
        return Telephony.Carriers.CONTENT_URI_DM;
    }

    public Uri getContentUri() {
        return Telephony.Carriers.CONTENT_URI;
    }

    // ********************************************
    // Service priority
    // ********************************************
    public void stayForeground(Service service) {
        Log.i(TAG.PLATFORM, "Bring service to foreground");
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        service.startForeground(1, notification);
    }

    public void leaveForeground(Service service) {
        Log.d(TAG.PLATFORM, "Exec stopForeground with para true.");
        service.stopForeground(true);
    }

    // ********************************************
    // DmAgent basic
    // ********************************************
    private DmAgent getDmAgent() {
        IBinder binder = ServiceManager.getService("DmAgent");
        if (binder == null) {
            Log.e(TAG.PLATFORM, "ServiceManager.getService(DmAgent) failed.");
            return null;
        }
        DmAgent agent = DmAgent.Stub.asInterface(binder);
        if (agent == null) {
            throw new Error("Failed to get DmAgent");
        }
        return agent;
    }

    /**
     * Reset DmSwitch, db & folder when switch (test -> productive or productive -> test)
     */
    public void clearFileWhenSwitch(Context context) {
        try {
            DmAgent agent = getDmAgent();
            byte[] switchValue = agent.getDmSwitchValue();
            if (switchValue != null && (new String(switchValue)).equals("1")) {
                Log.d(TAG.PLATFORM, "There is a pending DM flag.");

                Utilities.removeDirectoryRecursively(context.getFilesDir());
                DmDatabase.clearDB(context);
                agent.setDmSwitchValue("0".getBytes());

                Log.d(TAG.PLATFORM, "Data folder cleared.");
            } else {
                Log.d(TAG.PLATFORM, "There is no pending DM configuration switch flag.");
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Read saved IMSI via DmAgent
     */
    private String getSavedImsi() {
        String savedImsi = null;

        try {
            byte[] imsiByte = getDmAgent().readImsi();
            savedImsi = (imsiByte == null) ? null : new String(imsiByte);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.i(TAG.PLATFORM, "Get saved imsi " + savedImsi + ".");
        return savedImsi;
    }

    /**
     * Get state of environment: 0-test, 1-productive
     */
    private String getSwitchValue() {
        try {
            DmAgent agent = getDmAgent();
            byte[] switchValue = agent.getSwitchValue();
            if (switchValue != null) {
                return new String(switchValue);
            } else {
                return "0";
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    // ********************************************
    // DmAgent extend
    // ********************************************
    public String getPathInSystem(String fileName) {
        if (getSwitchValue().equals("1")) {
            return DmConst.Path.PRODUCTIVE_PATH_IN_SYSTEM + fileName;
        } else {
            return DmConst.Path.TEST_PATH_IN_SYSTEM + fileName;
        }
    }

    public String getPathInData(Context context, String fileName) {
        return context.getFileStreamPath(fileName).getAbsolutePath();
    }

    public long getRegisteredSubId() {
        String savedImsi = getSavedImsi();
        if (savedImsi == null) {
            Log.e(TAG.PLATFORM, "get registered imsi failed");
            return -1;
        }

        long[] subId = getSubIdList();
        for (long id : subId) {
            String imsi = getSubImsi(id);
            Log.d(TAG.PLATFORM, "Sub " + id + ":imsi = " + imsi);
            if (imsi != null && imsi.equals(savedImsi)) {
                Log.d(TAG.PLATFORM, "registered sub id is " + id);
                return id;
            }
        }

        Log.d(TAG.PLATFORM, "Get registerted subId error!");
        return -1;
    }

    // ********************************************
    // DmAgent lawmo/fumo
    // ********************************************
    public void clearLockFlag() throws RemoteException {
        getDmAgent().clearLockFlag();
    }

    public Boolean isLockFlagSet() throws RemoteException {
        return getDmAgent().isLockFlagSet();
    }

    public boolean isWipeSet() throws RemoteException {
        return getDmAgent().isWipeSet();
    }

    public int getLockType() throws RemoteException {
        return getDmAgent().getLockType();
    }

    public void setLockFlag(byte[] arg0) throws RemoteException {
        getDmAgent().setLockFlag(arg0);
    }

    public void setWipeFlag() throws RemoteException {
        getDmAgent().setWipeFlag();
    }

    public void restartAndroid() throws RemoteException {
        getDmAgent().restartAndroid();
    }

    public int readOtaResult() throws RemoteException {
        return getDmAgent().readOtaResult();
    }

    // ********************************************
    // File read/write (Scomo)
    // ********************************************
    public Object atomicRead(File file) {
        Object obj = null;
        try {
            AtomicFile atomicFile = new AtomicFile(file);
            ObjectInputStream in = new ObjectInputStream(atomicFile.openRead());
            obj = in.readObject();
            in.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public void atomicWrite(File file, Object data) {
        AtomicFile atomicFile = new AtomicFile(file);
        FileOutputStream fos = null;
        try {
            fos = atomicFile.startWrite();
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(data);
            atomicFile.finishWrite(fos);
            Log.i(TAG.PLATFORM, "atomicWrite: state stored: " + data);
        } catch (IOException e) {
            atomicFile.failWrite(fos);
            e.printStackTrace();
        }
    }

    // ********************************************
    // Package install (Scomo)
    // ********************************************
    public void installPackage(PackageManager pm, Uri packUri, int flag,
            final InstallListener listener) {
        pm.installPackage(packUri, new IPackageInstallObserver.Stub() {
            public void packageInstalled(final String name, final int status) {
                listener.packageInstalled(name, status);
            }
        }, flag, null);
    }

    public PackageInfo
            getPackageInfo(PackageManager pm, Resources resources, String archiveFilePath) {
        PackageInfo ret = new PackageInfo();

        PackageParser packageParser = new PackageParser();
        File sourceFile = new File(archiveFilePath);
        PackageParser.Package pkg = null;
        try {
            pkg = packageParser.parsePackage(sourceFile, 0);
        } catch (PackageParserException e) {
            e.printStackTrace();
        }
        if (pkg == null) {
            Log.w(DmConst.TAG.PLATFORM, "package Parser get package is null");
            return null;
        }

        ret.name = pkg.packageName;
        ret.version = pkg.mVersionName;
        ret.pkg = pkg;
        // get icon and label from archive file
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
        ret.label = label.toString();
        ret.icon = icon;

        return ret;
    }
}

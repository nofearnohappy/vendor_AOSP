package com.mediatek.voiceextension.cfg;

import android.content.Context;
import android.os.FileUtils;
import android.util.Log;

import com.mediatek.voiceextension.common.CommonManager;

import java.io.File;
import java.util.HashMap;

/**
 * Manage voice extension configuration information.
 *
 */
public class ConfigurationManager {

    private static ConfigurationManager sCfgMgr;
    private static byte[] sInstanceLock = new byte[0];
    private ConfigurationXml mConfigurationXml;
    private final HashMap<String, String> mPaths = new HashMap<String, String>();
    private boolean mIsInit = false;
    private Context mContext;

    private String mServiceDataDir;

    private final String mModelName = "ModelFile";
    private final String mDatabaseName = "Database";

    /**
     * Gets the instance of ConfigurationManager.
     *
     * @return ConfigurationManager instance
     */
    public static ConfigurationManager getInstance() {
        if (sCfgMgr == null) {
            synchronized (sInstanceLock) {
                if (sCfgMgr == null) {
                    sCfgMgr = new ConfigurationManager();
                }
            }
        }
        return sCfgMgr;
    }

    /**
     * Initialize configuration manager.
     *
     * @param context
     *            the context in which the service is running
     */
    public void init(Context context) {
        if (!mIsInit) {
            mConfigurationXml = new ConfigurationXml(context);
            mConfigurationXml.readVoiceFilePathFromXml(mPaths);
            mContext = context;
            mServiceDataDir = mContext.getApplicationInfo().dataDir;
            String databasePath = mServiceDataDir + "/database/";
            mPaths.put(mDatabaseName, databasePath);
            makeDirForPath(databasePath);
            mIsInit = true;
            Log.i(CommonManager.TAG, "Cfg init success");
        }
    }

    /**
     * Get model path.
     *
     * @return model path
     */
    public String getModelPath() {
        return mPaths.get(mModelName);
    }

    /**
     * Get database path.
     *
     * @return database path
     */
    public String getDatabasePath() {
        return mPaths.get(mDatabaseName);
    }

    /**
     * Create dir and file.
     *
     * @param path
     *            make dir path
     * @return true if make dir success, otherwise false
     */
    public boolean makeDirForPath(String path) {
        if (path == null) {
            return false;
        }
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
                FileUtils.setPermissions(dir.getPath(), 0775, -1, -1); // dwxrwxr-x
           }
        } catch (NullPointerException ex) {
            return false;
        }
        return true;
    }

    /**
     * create dir and file.
     *
     * @param file
     *            make dir file
     * @return true if make dir success, otherwise false
     */
    public boolean makeDirForFile(String file) {
        if (file == null) {
            return false;
        }
        try {
            File f = new File(file);
            File dir = f.getParentFile();
            if ((dir != null) && (!dir.exists())) {
                dir.mkdirs();
                FileUtils.setPermissions(dir.getPath(), 0775, -1, -1); // dwxrwxr-x
            }
            FileUtils.setPermissions(f.getPath(), 0666, -1, -1); // -rw-rw-rw-
        } catch (NullPointerException ex) {
            return false;
        }
        return true;
    }

}

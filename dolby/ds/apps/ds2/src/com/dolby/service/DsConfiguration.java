/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013 - 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * DsConfiguration.java
 *
 * 
 */
package com.dolby.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.dolby.ds.DsManager;
import com.dolby.api.IDsCallbacks;
import com.dolby.api.DsCommon;
import com.dolby.api.DsLog;

public class DsConfiguration
{
    private static final String TAG = "DsConfiguration";
    
    /**
     * The flag to determine whether to adopt file system settings or the built-in asset settings.
     * NOTE: On switching to the built-in asset settings, do NOT forget to put the file ds-default.xml,
     *       which contains the default configuration, into the assets sub-folder in this package.
     * 
     */
    public static final boolean isDefaultSettingsOnFileSystem = true;

    /**
     * The path where the file system settings are stored.
     * @internal
     */
    private static final String DS_DEFAULT_SETTINGS_USER_PATH = "/data/dolby";
    private static final String DS_DEFAULT_SETTINGS_VENDOR_PATH = "/system/vendor/etc/dolby";

    /**
     * The file name to store the default settings.
     * @internal
     */
    private static final String DS_DEFAULT_SETTINGS_FILENAME = "ds-default.xml";

    /**
     * The path where the file system settings are stored.
     * 
     * @param context The context to opne the input stream.
     * @param dirPath The path where the config files exist.
     */
    public static InputStream prepare(Context context, String dirPath)
    {
        String userSettingsPath = null;
        InputStream defaultInStream = null;

        try
        {
            File file = new File(dirPath, DsManager.DS_CURRENT_FILENAME);
            if (file.exists()) 
            {
                DsLog.log1(TAG, file.getAbsolutePath() + " alread exists");
            }
            else 
            {
                DsLog.log1(TAG, "Creating " + file.getAbsolutePath());
                // Allow all other applications to have read & write access to the created file.
                FileOutputStream fos = context.openFileOutput(DsManager.DS_CURRENT_FILENAME, Context.MODE_PRIVATE);
                fos.close();
            }
            file = new File(dirPath, DsManager.DS_STATE_FILENAME);
            if (file.exists())
            {
                DsLog.log1(TAG, file.getAbsolutePath() + " alread exists");
            }
            else 
            {
                DsLog.log1(TAG, "Creating " + file.getAbsolutePath());
                // Allow all other applications to have read access to the created file.
                FileOutputStream fos = context.openFileOutput(DsManager.DS_STATE_FILENAME, Context.MODE_PRIVATE);
                fos.close();
            }

            if (isDefaultSettingsOnFileSystem)
            {
                file = new File(DS_DEFAULT_SETTINGS_VENDOR_PATH, DS_DEFAULT_SETTINGS_FILENAME);
                if (file.exists())
                {
                    userSettingsPath = file.getAbsolutePath();
                }
                else
                {
                    userSettingsPath = DS_DEFAULT_SETTINGS_USER_PATH + "/" + DS_DEFAULT_SETTINGS_FILENAME;
                }
                DsLog.log1(TAG, "Adopting the file system settings... " + userSettingsPath);
                if (userSettingsPath != null)
                {
                    defaultInStream = new FileInputStream(userSettingsPath);
                }
                else 
                {
                    Log.e(TAG, "The user settings path NOT defined!");
                }
            }
            else
            {
                DsLog.log1(TAG, "Adopting the built-in settings in assets...");
                AssetManager am = context.getAssets();
                defaultInStream = am.open(DS_DEFAULT_SETTINGS_FILENAME);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception was caught");
            e.printStackTrace();
            defaultInStream = null;
        }
        return defaultInStream;
    }
}

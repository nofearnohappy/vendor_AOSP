/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.service;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import android.media.AudioManager;
import android.content.Context;

import com.dolby.api.DsLog;
import com.dolby.api.DsCommon;
import com.dolby.ds.DsManager;
import com.dolby.ds.DsProperty;
import com.dolby.api.DsConstants;
import com.dolby.api.DsAccess;

/**
 * DsAccessRightManager class provides the methods which help DsService
 * to maintain access rights of connected Ds applications.
 */ 
public class DsAccessRightManager
{
    private static final String TAG = "DsAccessRightManager";

    /**
     * The lock for protecting the DsAccessRightManager context to be thread safe.
     *
     * @internal
     */
    private static final Object lock_ = new Object();

    /**
     * Helper class to maintain the information of all connected applications which are bound to DsService.
     *
     * @internal
     */
    private class DsConnectedAppInfo
    {
         /**
            * The package name of the application bound to DsService.
            */
        String appPackageName;

         /**
            * The Ds API  bridge through which an application is connected.
            */
        int connectionBridge;

         /**
            * The access right which the application has granted currently.
            */
        int appCurrentAccessRight;
    }

    /**
     * The map between the handle of Ds and the information of the applications bound to DsService.
     *
     * @internal
     */
    private HashMap<Integer, DsConnectedAppInfo> dsConnectedAppInfoMap_;

    /**
     * The map between a special access right and the handle of the Ds. It means which application has granted the special access right.
     *
     * @internal
     */
    private HashMap<Integer, Integer> dsAccessRightLookupMap_;

    /**
     * The handle of the app which access right is forcibly released by DsService.
     *
     * @internal
     */
    private int forceAbandonHandle_;
    
    /**
     * The instance of the callback manager.
     *
     * @internal
     */
    private DsCallbackManager cbkManager_ = null;

    /**
     * The instance of DsManager.
     *
     * @internal
     */
    private DsManager dsManager_ = null;

    /**
     * The map between the handle of Ds and the version of Ds.
     *
     * @internal
     */
    private static HashMap<Integer, Integer> dsVersionMap_ = new HashMap <Integer, Integer>();

    /**
     * An error handle value.
     *
     * @internal
     */
    private static final int ERROR_HANDLE = -1;
    
    /**
     * set a specific access right.
     *
     * @param handle The handle of the Ds.
     * @param type The accessRight that the application sets.
     *
     * 
     * @internal
     */
    private void setDsAccessRight (int handle, int type)
    {
        if (dsConnectedAppInfoMap_.containsKey(handle))
        {
            DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
            appInfo.appCurrentAccessRight |= type;
            dsAccessRightLookupMap_.put(type, handle);
        }
    }

    /**
     * clear a specific access right.
     *
     * @param handle The handle of the Ds.
     * @param type The accessRight that the application clears.
     *
     * 
     * @internal
     */
    private void clearDsAccessRight (int handle, int type)
    {
        if (dsConnectedAppInfoMap_.containsKey(handle))
        {
            DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
            appInfo.appCurrentAccessRight &= ~(type);
            dsAccessRightLookupMap_.remove(type);
        }
    }

    /**
     * Query whether some apps have granted a specific access when an app request access.
     *
     * @param name The package name of the app wants to get an access right.
     * @param accessRight The specific accessRight that the app requests.
     *
     * @return The error code.
     *
     * @internal
     */
    private int queryDsAccessRight (String name, int type)
    {
        int error = DsCommon.DS_NO_ERROR;

        for (Map.Entry<Integer, Integer> x : dsAccessRightLookupMap_.entrySet())
        {
            int key = x.getKey();
            int appId = x.getValue();
            if ((type == DsAccess.ACCESS_GLOBAL) && (key == DsAccess.ACCESS_FOCUS))
            {
                //send a callback to app granted the DsFocus to make it know other app wants to request DsGlobal
                boolean isPermitted = cbkManager_.invokeCallback(DsCommon.ACCESS_REQUESTED_MSG, appId, type, 0, name, null);
                if (!isPermitted)
                {
                    error = DsAccess.ERROR_ACCESS_NOT_AGREED;
                    break;
                }
                else
                {
                    dsManager_.restoreCurrentProfiles();
                    restoreSystemProperty();
                }
            }
            else
            {
                if ((type == DsAccess.ACCESS_FOCUS) && (key == DsAccess.ACCESS_GLOBAL))
                {
                    dsManager_.saveDsStateAndSettings();
                }
                cbkManager_.invokeCallback(DsCommon.ACCESS_RELEASED_MSG, appId, type, 0, name, null);   
            }
            
            clearDsAccessRight(appId, key);
            forceAbandonHandle_ = appId;
            if (dsVersionMap_.containsKey(appId))
            {
                dsVersionMap_.remove(appId);
            }
        }
        return error;
    }
    
    /**
     * Find an specific application.
     *
     * @param name The name of the specific app.
     *
     * @return The handle of the specific app. 
     *
     * @internal
     */
    private int findDsConnectedApp(String name)
    {
        int handle = ERROR_HANDLE;
        DsConnectedAppInfo info = null;
        
        for (Map.Entry<Integer, DsConnectedAppInfo> x : dsConnectedAppInfoMap_.entrySet())
        {
            int key = x.getKey();
            info = x.getValue();
            if ((info.appPackageName).equals(name))
            {
                handle = key;
                break;
            }
        }
        return handle;
    }

    /**
     * The constructor.
     *
     */ 
    public DsAccessRightManager(DsManager ds, DsCallbackManager cbk)
    {
        DsLog.log1(TAG, "DsAccessRightManager.DsAccessRightManager");

        synchronized (lock_)
        {
            dsManager_ = ds;
            cbkManager_ = cbk;
            dsConnectedAppInfoMap_ = new HashMap <Integer, DsConnectedAppInfo>();
            dsAccessRightLookupMap_ = new HashMap <Integer, Integer>();
            forceAbandonHandle_ = ERROR_HANDLE;
        }
    }

    /**
     * The method release the resource.
     *
     */
    public void release()
    {
        synchronized (lock_)
        {
            dsAccessRightLookupMap_ = null;
            dsConnectedAppInfoMap_ = null;
            cbkManager_ = null;
            dsManager_ = null;
        }
    }

    /**
     * Add an entry to maintain the information of the application which is connected to DsService.
     *
     * @param handle The handle of the Ds.
     * @param appName The name of the application package.
     * @param connection Ds API through which an application is connected.
     *
     */
    public void addDsConnectedApp(int handle, String appName, int connection)
    {
        DsLog.log1(TAG, "DsAccessRightManager.addDsConnectedApp");
        
        synchronized (lock_)
        {
            DsConnectedAppInfo mDsConnectedAppInfo = new DsConnectedAppInfo();
            mDsConnectedAppInfo.appPackageName = appName;
            mDsConnectedAppInfo.connectionBridge |= connection;
            dsConnectedAppInfoMap_.put(handle, mDsConnectedAppInfo);
        }
    }

    /**
     * Remove an entry when the application is disconnected to DsService.
     *
     * @param handle The handle of the Ds.
     * 
     */
    public void removeDsConnectedApp(int handle)
    {
        DsLog.log1(TAG, "DsAccessRightManager.removeDsConnectedApp");
        
        synchronized (lock_)
        {
            if (dsConnectedAppInfoMap_.containsKey(handle))
            {
                DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
                if (forceAbandonHandle_ != ERROR_HANDLE)
                {
                    if (dsConnectedAppInfoMap_.containsKey(forceAbandonHandle_))
                    {
                        cbkManager_.invokeCallback(DsCommon.ACCESS_AVAILABLE_MSG,forceAbandonHandle_,0,0,null,null);
                    }
                    forceAbandonHandle_ = ERROR_HANDLE;
                }
            
                if ((appInfo.connectionBridge & DsAccess.ACCESS_FOCUS) == DsAccess.ACCESS_FOCUS)
                {
                    dsManager_.restoreCurrentProfiles();
                    restoreSystemProperty();
                }
                dsAccessRightLookupMap_.remove(appInfo.appCurrentAccessRight);
                dsConnectedAppInfoMap_.remove(handle);
            }
        }
    }

    /**
     * Request a specific access right, application must have granted AudioFocus before it requests DsFocus access right.
     *
     * @param handle The handle of the Ds.
     * @param accessRight The accessRight that the application requests.
     * @param audioManager The instance of AudioManager.
     *
     * @return The error code.
     * 
     */
    public int requestAccessRight(int handle, int accessRight, AudioManager audioManager)
    {
        DsLog.log1(TAG, "DsAccessRightManager.requestAccessRight");
        int error = DsCommon.DS_UNKNOWN_ERROR;
        
        synchronized (lock_)
        {
            //check whether the app has already granted the special accessRight
            if (dsAccessRightLookupMap_.containsKey(accessRight))
            {
                int currentHandler = dsAccessRightLookupMap_.get(accessRight);
                if (currentHandler == handle)
                {
                    return DsAccess.ERROR_ACCESS_AREADY_GRANTED;
                }
            }
            
            //Whether the calling app is permitted to request the special accessRight
            DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
            if ((appInfo.connectionBridge & accessRight) != accessRight) 
            {
                return DsAccess.ERROR_ACCESS_NOT_PERMITTED;
            }
            
            //whether the app granted the audioFocus
            if (accessRight == DsAccess.ACCESS_FOCUS)
            {
                boolean isFocused = audioManager.isAppInFocus(appInfo.appPackageName);
                if (!isFocused)
                {
                    return DsAccess.ERROR_ACCESS_NO_AUDIOFOCUS;
                }
            }
            
            switch (accessRight)
            {
                case DsAccess.ACCESS_FOCUS:   //DsFocus access right
                case DsAccess.ACCESS_GLOBAL: //DsGlobal access right
                {
                    error = queryDsAccessRight(appInfo.appPackageName, accessRight);
                    if (error == DsCommon.DS_NO_ERROR)
                    {
                        setDsAccessRight(handle,accessRight);
                        error = DsCommon.DS_NO_ERROR;
                    }
                    break;
                }
                case DsAccess.ACCESS_TUNING:
                    //ToDo..
                    break;
                default:
                    break;
            }
        }
        return error;
    }

    /**
     * abandon a specific access right.
     *
     * @param handle The handle of the Ds.
     * @param accessRight The access right that the application requests.
     * @param audioManager The instance of AudioManager.
     *
     * @return The error code.
     * 
     */
    public int abandonAccessRight(int handle, int accessRight, AudioManager audioManager)
    {
        DsLog.log1(TAG, "DsAccessRightManager.abandonAccessRight");
        
        synchronized (lock_)
        {
            switch (accessRight)
            {
                case DsAccess.ACCESS_FOCUS: //DsFocus access right
                case DsAccess.ACCESS_GLOBAL: //DsGlobal access right
                {
                    if (forceAbandonHandle_ != ERROR_HANDLE)
                    {
                        if (dsConnectedAppInfoMap_.containsKey(forceAbandonHandle_))
                        {
                            cbkManager_.invokeCallback(DsCommon.ACCESS_AVAILABLE_MSG,forceAbandonHandle_,0,0,null,null);
                        }
                        forceAbandonHandle_ = ERROR_HANDLE;
                    }
                    clearDsAccessRight(handle,accessRight);
                    
                    if (accessRight == DsAccess.ACCESS_FOCUS)
                    {
                        if (dsConnectedAppInfoMap_.containsKey(handle))
                        {
                            DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
                            String packageName = appInfo.appPackageName;
                            boolean isFocused = audioManager.isAppInFocus(packageName);
                            if (!isFocused)
                            {
                                dsManager_.restoreCurrentProfiles();
                                restoreSystemProperty();
                            }
                        }
                    }
                    break;
                }
                case DsAccess.ACCESS_TUNING:
                    //ToDo..
                    break;
                default:
                    break;
            }
        
            return DsCommon.DS_NO_ERROR;
        }
    }

    /**
     * Query whether a specific access right is taken by any application.
     *
     * @param handle The handle of the Ds.
     * @accessRight The access right is queried by application.
     *
     * @return  0 if the access right is not granted by any application.
     *              1 if the access right is granted by other application already.
     *              2 if the access right is granted by this application.
     * 
     */
    public int checkAccessRight(int handle, int accessRight)
    {
        DsLog.log1(TAG, "DsAccessRightManager.checkAccessRight");
        
        synchronized(lock_)
        {
            if (dsAccessRightLookupMap_.containsKey(accessRight))
            {
                int appId = dsAccessRightLookupMap_.get(accessRight);
                if (appId == handle)
                {
                    return DsAccess.THIS_APP_GRANTED;
                }
                else
                {
                    return DsAccess.OTHER_APP_GRANTED;
                }
            }
            return DsAccess.NONE_APP_GRANTED;
        }

    }

    /**
     * Get the access right that the application has granted.
     *
     * @param handle The handle of the Ds.
     *
     * @return The access right the application has granted.
     * 
     */
    public int getAppAccessRightGranted(int handle)
    {
        DsLog.log1(TAG, "DsAccessRightManager.getAppAccessRightGranted");
        
        synchronized (lock_)
        {
            DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
            return appInfo.appCurrentAccessRight;
        }
    }

    /**
     * Check whether the app is permitted to set any parameter.
     *
     * @param handle the handle of the Ds.
     *
     * @return ture The app is permmitted to change the settings, else false.
     * 
     */
    public boolean isAppAccessPermitted(int handle)
    {
        DsLog.log1(TAG, "DsAccessRightManager.isAppAccessPermitted.");
        boolean value = false;
        
        synchronized (lock_)
        {
            if (dsVersionMap_.containsKey(handle))
            {
                int version = dsVersionMap_.get(handle);
                if (version == DsCommon.DS_CLIENT_VER_ONE)
                {
                    if ((dsAccessRightLookupMap_.containsKey(DsAccess.ACCESS_FOCUS))
                        || (dsAccessRightLookupMap_.containsKey(DsAccess.ACCESS_GLOBAL))
                        || (dsAccessRightLookupMap_.containsKey(DsAccess.ACCESS_TUNING)))
                    {
                        value = false;	
                    }
                    else
                    {
                        value = true;
                    }
                }
                else if (version == DsCommon.DS_CLIENT_VER_TWO)
                {
                    DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
                    if (((appInfo.appCurrentAccessRight & DsAccess.ACCESS_FOCUS) == DsAccess.ACCESS_FOCUS)
                        ||((appInfo.appCurrentAccessRight & DsAccess.ACCESS_GLOBAL) == DsAccess.ACCESS_GLOBAL))
                    {
                        value = true;
                    }
                    else
                    {
                        value = false;
                    }
                }
            }
        }
        return value;
    }

    /**
     * Add an entry to maintain the version information of the Ds client.
     *
     * @param handle The handle of the Ds.
     * @param version The version of client, DS1 or DS2.
     * 
     */
    public void registerDsVersion(int handle, int version)
    {
        dsVersionMap_.put(handle, version);
    }

    /**
     * Remove an entry to maintain the version information of the Ds client.
     *
     * @param handle The handle of the Ds.
     * 
     */
    public void unRegisterDsVersion(int handle)
    {
        if (dsVersionMap_.containsKey(handle))
        {
            dsVersionMap_.remove(handle);
        }
    }

    /**
     * Check whether there is any entry to maintain the version information of the Ds client.
     * 
     * @return ture The HashMap dsVersionMap_ is empty, else false.
     *
     */
    public boolean isEmptyDsVersion()
    {
        boolean isEmpty = false;
        
        if (dsVersionMap_.isEmpty())
        {
            isEmpty = true;
        }
        return isEmpty;
    }

    /**
     * Check whether the settings is global.
     *
     * @param handle The handle of the Ds.
     *
     * @return ture The settings is golbal, else false.
     */
    public boolean isGlobalSettings(int handle)
    {
        boolean isGlobal = false;
        
        if (handle == 0)
        {
            isGlobal = true;
        }
        else
        {
            if (dsVersionMap_.containsKey(handle))
            {
                int version = dsVersionMap_.get(handle);
                if (version == DsCommon.DS_CLIENT_VER_TWO)
                {
                    DsConnectedAppInfo appInfo = dsConnectedAppInfoMap_.get(handle);
                    int accessRight = appInfo.appCurrentAccessRight;
                    if ((accessRight & DsAccess.ACCESS_GLOBAL) == DsAccess.ACCESS_GLOBAL)
                    {
                        isGlobal = true;
                    }
                }
                else
                {
                    isGlobal = true;
                }
            }
        }
        return isGlobal;
    }

    /**
     * Change the access right when an application lost or gained audio focus.
     *
     * @param name The name of the application.
     * @param action Which action happens about the audio focus.
     * 
     */
    public void doAccessForAudioFocusChange(String name, int action)
    {
        DsLog.log1(TAG, "DsAccessRightManager.doAccessForAudioFocusChange.");

        synchronized (lock_)
        {
            int handle = findDsConnectedApp(name);
            if (handle != ERROR_HANDLE)
            {
                if ((action == DsCommon.DS_AUDIOFOCUS_LOSS) || (action == DsCommon.DS_AUDIOFOCUS_ABANDON))
                {
                    DsConnectedAppInfo info = dsConnectedAppInfoMap_.get(handle);
                    int type = info.appCurrentAccessRight;
                    if ((type & DsAccess.ACCESS_FOCUS) == DsAccess.ACCESS_FOCUS)
                    {
                        clearDsAccessRight(handle, type);
                        cbkManager_.invokeCallback(DsCommon.ACCESS_RELEASED_MSG,handle,type,0, name, null);
                        if(action == DsCommon.DS_AUDIOFOCUS_LOSS)
                        {
                            forceAbandonHandle_ = handle;
                        }
                        if (dsVersionMap_.containsKey(handle))
                        {
                            dsVersionMap_.remove(handle);
                        }
                    }
                    dsManager_.restoreCurrentProfiles();
                    restoreSystemProperty();
                }
                else
                {
                    cbkManager_.invokeCallback(DsCommon.ACCESS_AVAILABLE_MSG,handle,0,0,null,null);
                    forceAbandonHandle_ = ERROR_HANDLE;
                } 
            }      
        }
    }
    
    private void restoreSystemProperty()
    {
        try
        {
            boolean on = (dsManager_.getDsOn() == DsConstants.DS_STATE_ON) ? true : false;
            int profile = dsManager_.getSelectedProfile();
            String curState = (on == true) ? "on" : "off";
            DsProperty.setStateProperty(curState);
            dsManager_.setProfileProperties(profile);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Exception found in setting DS state property");
            ex.printStackTrace();
        }
    }
}


/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * DsService.java
 *
 * Background Service providing access to the DS Audio Effect.
 *
 * This always-running service runs in a different process than the controlling
 * application using IPC to interact with it.
 */

package com.dolby.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalStateException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.media.audiofx.AudioEffect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.dolby.ds.DsManager;
import com.dolby.ds.DsVersion;
import com.dolby.ds.DsAkSettings;
import com.dolby.ds.DsProperty;
import com.dolby.api.IDs;
import com.dolby.api.IDsCallbacks;
import com.dolby.api.DsConstants;
import com.dolby.api.DsLog;
import com.dolby.api.DsCommon;
import com.dolby.api.DsClientInfo;
import com.dolby.api.DsParams;
import com.dolby.api.IDsDeathHandler;
import com.dolby.api.DsProfileName;

public class DsService extends Service
{
    private static final String TAG = "DsService";

    /**
     * The interface to control the DS OpenSL-ES AudioEffect.
     *
     * @internal
     */
    private DsManager dsManager_ = null;

    /**
     * The lock for protecting the DS context to be thread safe.
     *
     * @internal
     */
    private final Object lockDolbyContext_ = new Object();

    /**
     * The intent to launch Consumer UI.
     *
     * @internal
     */
    private static final String ACTION_DOLBY_LAUNCH_APP = "com.dolby.LAUNCH_DS_APP";

    /**
     * The global audio session id.
     *
     * @internal
     */
    private static final int GLOBAL_AUDIO_SESSION_ID = 0;
    private static final String STATE_ON = "on";
    private static final String STATE_OFF = "off";

    /* DOLBY_BROADCAST_DEVICE_CHANGE */
    private static final String DOLBY_DEVICE_CHANGE = "DOLBY_DEVICE_CHANGE";
    /* DOLBY_BROADCAST_DEVICE_CHANGE_END */

    private static final String DS_EFFECT_SUSPEND_ACTION = "DS_EFFECT_SUSPEND_ACTION";
    private static final int DS_EFFECT_SUSPENDED = 1;
    private static final int DS_EFFECT_UNSUSPENDED = 0;

    /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE */
    private static final String DS_AUDIO_FOCUS_CHANGE_ACTION = "DS_AUDIO_FOCUS_CHANGE_ACTION";
    /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE_END */

    /**
     * The period of generating visualizer data, in millisecond.
     *
     * @internal
     */
    private int geqBandCount_ = 0;

    /**
     * The virtual handle of client.
     *
     * @internal
     */
    private static final int globalClientHandle_ = 0;

    /**
     * Integer value for on state.
     *
     *  @internal
     */
    private static final int INT_ON = 1;

    /**
     * Integer value for off state.
     *
     *  @internal
     */
    private static final int INT_OFF = 0;

    /**
     * The map between the handle of Ds and the information of Ds client's application.
     *
     * @internal
     */
    private HashMap<Integer, DsClientInfo> dsClientInfoMap_ = new HashMap <Integer, DsClientInfo>();

    /**
     * The manager for visualizer data.
     *
     * @internal
     */
    private DsVisualizerManager visManager_ = null;

    /**
     * The manager for sending callback.
     *
     * @internal
     */
    private DsCallbackManager cbkManager_ = null;

    /**
     * The manager for access right.
     *
     * @internal
     */
    private DsAccessRightManager arManager_ = null;

    /**
     * The map between the IBinder of Ds and the death listener of Ds.
     *
     * @internal
     */
    private final HashMap<IBinder, DsClientDeathHandler> dsClientDeathHandlerList_ = new HashMap<IBinder, DsClientDeathHandler>();

    /**
     * The broadcast receiver for the init Intent.
     * The init Intent is set from the widget when onEnabled is called.
     *
     * @internal
     */
    private BroadcastReceiver intentReceiver_ = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                String action = intent.getAction();
                String cmd = intent.getStringExtra(DsCommon.CMDNAME);
                String name = intent.getStringExtra(DsCommon.WIDGET_CLASS);
                DsLog.log1(TAG, "intentReceiver_.onReceive " + action + " / " + cmd + " / " + name);

                if ((intent.getAction().equals(Intent.ACTION_REBOOT) || intent.getAction().equals(Intent.ACTION_SHUTDOWN)) &&
                     (dsManager_ != null) )
                {
                    DsLog.log1(TAG, "Save DS state and current settings before shutting down...");
                    dsManager_.saveDsStateAndSettings();
                }
                else if (action.equals("media_server_started"))
                {
                    dsManager_.validateDsEffect();
                    DsLog.log1(TAG, "DS effect recreate successfully");
                }
                /* DOLBY_BROADCAST_DEVICE_CHANGE */
                else if (intent.getAction().equals(DOLBY_DEVICE_CHANGE))
                {
                    if (intent.hasExtra("Device"))
                    {
                        int device = intent.getIntExtra("Device", -1);
                        DsLog.log1(TAG, "Device Changed, New Device : " + device);
                    }
                    if (intent.hasExtra("DeviceUniqueId"))
                    {
                        String deviceName = intent.getStringExtra("DeviceUniqueId");
                        DsLog.log1(TAG, "Device unique id = " + deviceName);
                    }
                    // TODO: Find the right device tuning settings and apply them.
                }
                /* DOLBY_BROADCAST_DEVICE_CHANGE_END */
                else if (action.equals(DS_EFFECT_SUSPEND_ACTION))
                {
                    DsLog.log1(TAG, "DS_EFFECT_SUSPEND_ACTION " + this.getResultCode());
                    switch (this.getResultCode())
                    {
                        case DS_EFFECT_SUSPENDED:
                        {
                            // Check the system property to know the current UI state of DS instead
                            // of using dsManager_.getDsOn as the underlying effect might already be suspended.
                            String dsState = (String) DsProperty.getStateProperty();
                            dsManager_.setDsSuspended(true);
                            DsLog.log1(TAG, "DS_EFFECT_SUSPENDED");
                            cbkManager_.invokeCallback(DsCommon.DS_STATUS_SUSPENDED_MSG, globalClientHandle_, DS_EFFECT_SUSPENDED, 0, null, null);
                            if (dsState.equals(STATE_ON))
                            {
                                DsProperty.setStateProperty(STATE_OFF);
                                cbkManager_.invokeCallback(DsCommon.DS_STATUS_CHANGED_MSG, globalClientHandle_, INT_OFF, 0, null, null);
								cbkManager_.invokeDs1Callback(DsCommon.DS_STATUS_CHANGED_MSG, globalClientHandle_, INT_OFF, 0, null, null);
                            }
                            break;
                        }

                        case DS_EFFECT_UNSUSPENDED:
                        {
                            dsManager_.setDsSuspended(false);
                            DsLog.log1(TAG, "DS_EFFECT_UNSUSPENDED");
                            cbkManager_.invokeCallback(DsCommon.DS_STATUS_SUSPENDED_MSG, globalClientHandle_, DS_EFFECT_UNSUSPENDED, 0, null, null);
                            doSetDsOn(globalClientHandle_, false);
                            break;
                        }

                        default:
                            break;

                    }
                }
                /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE */
                else if (intent.getAction().equals(DS_AUDIO_FOCUS_CHANGE_ACTION))
                {
                    if (intent.hasExtra("packageName"))
                    {
                        String packageName = intent.getStringExtra("packageName");
                        if(intent.hasExtra("focusChange"))
                        {
                            String focusChange = intent.getStringExtra("focusChange");
                            if(focusChange.equals("loss") || focusChange.equals("abandon"))
                            {
                                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                                boolean isFocused = audioManager.isAppInFocus(packageName);
                                if(!isFocused)
                                {
                                    if(focusChange.equals("abandon"))
                                    {
										DsLog.log1(TAG, "DsService,The application named " + packageName + " has abandoned its audio focus");
                                        arManager_.doAccessForAudioFocusChange(packageName, DsCommon.DS_AUDIOFOCUS_ABANDON);
                                    }
                                    else
                                    {
										DsLog.log1(TAG, "DsService,The application named " + packageName + " has lost its audio focus");
                                        arManager_.doAccessForAudioFocusChange(packageName, DsCommon.DS_AUDIOFOCUS_LOSS);
                                    }
                                }
                            }
                            else if(focusChange.equals("gain"))
                            {
                                DsLog.log1(TAG, "DsService,The application named " + packageName + " has gained its audio focus");
                                arManager_.doAccessForAudioFocusChange(packageName, DsCommon.DS_AUDIOFOCUS_GAIN);
                            }
                        }
                    }
                }
                /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE_END */
            }
            catch (Exception ex)
            {
                 Log.e(TAG, "Exception found in DsService::onReceive()");
                 ex.printStackTrace();
            }
        }
    };

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate()
    {
        DsLog.log1(TAG, "DsService.onCreate()");

        // create the global audio effect on boot-up
        try
        {
            createDs(null);
            IntentFilter commandFilter = new IntentFilter();
            commandFilter.addAction(DsCommon.INIT_ACTION);
            commandFilter.addAction(Intent.ACTION_REBOOT);
            commandFilter.addAction(Intent.ACTION_SHUTDOWN);
            commandFilter.addAction("media_server_started");
            /* DOLBY_BROADCAST_DEVICE_CHANGE */
            commandFilter.addAction(DOLBY_DEVICE_CHANGE);
            /* DOLBY_BROADCAST_DEVICE_CHANGE_END */
            commandFilter.addAction(DS_EFFECT_SUSPEND_ACTION);
            /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE */
            commandFilter.addAction(DS_AUDIO_FOCUS_CHANGE_ACTION);
            /* DOLBY_BROADCAST_AUDIOFOCUS_CHANGE_END */
            // add other actions here
            registerReceiver(intentReceiver_, commandFilter);
            cbkManager_ = new DsCallbackManager();
            arManager_ = new DsAccessRightManager(dsManager_,cbkManager_);
            visManager_ = new DsVisualizerManager(dsManager_, cbkManager_);
        }
        catch (Exception ex)
        {
             Log.e(TAG, "Exception found in DsService.onCreate()");
             ex.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        DsLog.log1(TAG, "DsService.onDestroy()");

        synchronized (lockDolbyContext_)
        {
            try
            {
                dsManager_.setDsOn(false);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Exception found in DsService.onDestory()");
                ex.printStackTrace();
            }
        }
        // Unregister all callbacks.
        cbkManager_.release();
        arManager_.release();
        // cleanup the visualizer list
        visManager_.release(); 
        dsManager_ = null;
        unregisterReceiver(intentReceiver_);
    }

    /* (non-Javadoc)
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent callerIntent, int flags, int startId)
    {
        DsLog.log1(TAG, "DsService.onStartCommand()");

        try
        {
            if (callerIntent != null)
            {
                String action = callerIntent.getAction();
                DsLog.log1(TAG, "Intent action is " + action);

                if (DsCommon.ONOFF_ACTION.equals(action))
                {
                    synchronized (lockDolbyContext_)
                    {
                        doToggleDsOn(globalClientHandle_);
                    }
                }
                else if (DsCommon.SELECTPROFILE_ACTION.equals(action))
                {
                    synchronized (lockDolbyContext_)
                    {
                        int profile = callerIntent.getIntExtra(DsCommon.CMDNAME, 0);

                        doSetSelectedProfile(globalClientHandle_, profile);
                    }
                }
                else if (DsCommon.LAUNCH_DOLBY_APP_ACTION.equals(action))
                {
                    Intent intent = getDsConsumerAppIntent();
                    if (intent != null)
                    {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            }
            else
            {
                // This command is not from the widget, ignore it.
                DsLog.log1(TAG, "onStartCommand: callerIntent==null, ignoring...");
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "DsService.onStartCommand() exception found");
            ex.printStackTrace();
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        DsLog.log1(TAG, "DsService.onBind()");

        if (IDs.class.getName().equals(intent.getAction()))
        {
            return binder_;
        }

        Log.e(TAG, "/DsService.onBind() - return null");
        return null;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onTrimMemory(int level)
     */
    @Override
    public void onTrimMemory(int level)
    {
        DsLog.log1(TAG, "DsService.onTrimMemory() level " + level);
    }

    /* (non-Javadoc)
     * @see android.app.Service#onLowMemory()
     */
    @Override
    public void onLowMemory()
    {
        DsLog.log1(TAG, "DsService.onLowMemory()");
    }

    /**
     * Create the instance of DS effect.
     *
     * @param callerIntent The intent creates the effect.
     *
     * @internal
     */
    private void createDs(Intent callerIntent)
    {
        // defaulting to a zero audioSessionId, which results in the AudioEffect
        // being created on the Global mixer
        DsLog.log1(TAG, "createDs()");
        
        synchronized (lockDolbyContext_)
        {
            try 
            {
                String dir = getFilesDir().getAbsolutePath();
                InputStream in = DsConfiguration.prepare(this, dir);
                if (dsManager_.loadSettings(in, dir))
                {
                    geqBandCount_ = DsAkSettings.getGeqBandCount();
                    if (geqBandCount_ > 0)
                    {
                        dsManager_ = new DsManager();
                        dsManager_.createDsEffect(GLOBAL_AUDIO_SESSION_ID);

                        // Set system property
                        boolean on = (dsManager_.getDsOn() == DsConstants.DS_STATE_ON) ? true : false;
                        int profile = dsManager_.getSelectedProfile();
                        String curState = (on == true) ? STATE_ON : STATE_OFF;
                        DsProperty.setStateProperty(curState);
                        dsManager_.setProfileProperties(profile);

                        // Write the selected default to the prefs so that the Redirector activity
                        // knows which one to use.
                        String defPackage = "com.dolby.ds1appUI";
                        String defName = "com.dolby.ds1appUI.MainActivity";
                        SharedPreferences pref = getSharedPreferences("musicfx", MODE_PRIVATE);
                        Editor ed = pref.edit();
                        ed.putString("defaultpanelpackage", defPackage);
                        ed.putString("defaultpanelname", defName);
                        ed.commit();
                        DsLog.log1(TAG, "wrote " + defPackage + "/" + defName + " as default");
                    }
                    else
                    {
                        Log.e(TAG, "createDs() FAILED! graphic equalizer band count NOT initialized yet.");
                    }
                }
                else
                {
                    Log.e(TAG, "createDs() FAILED! DS settings are NOT loaded successfully.");
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Ds() FAILED! Exception");
                ex.printStackTrace();
            }
        }
    }

    private Intent getDsConsumerAppIntent()
    {
        Intent intent = new Intent(ACTION_DOLBY_LAUNCH_APP);
        PackageManager p = getPackageManager();
        List<ResolveInfo> ris = p.queryIntentActivities(intent, PackageManager.GET_DISABLED_COMPONENTS);
        return (ris != null && !ris.isEmpty()) ? intent : null;
    } 

    /**
     * The Handler of listening the Ds client's death.
     *
     * @internal
     */
    private class DsClientDeathHandler implements IBinder.DeathRecipient
    {
        private final IDsDeathHandler mIDsDeathHandler; 
        int mHandle;

        DsClientDeathHandler(IDsDeathHandler dh, int handle)
        {
            mIDsDeathHandler = dh;
            mHandle = handle;
        }

        public void linkToDeath() throws RemoteException
        {
            mIDsDeathHandler.asBinder().linkToDeath(this, 0);
        }

        public void unlinkToDeath()
        {
            mIDsDeathHandler.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied()
        {
            synchronized (lockDolbyContext_)
            {
                arManager_.removeDsConnectedApp(mHandle);
                arManager_.unRegisterDsVersion(mHandle);
                dsClientInfoMap_.remove(mHandle);
                dsClientDeathHandlerList_.remove(mIDsDeathHandler.asBinder());
            }
        }
    }

    /**
     * The IDs is defined through IDL, which provides the interfaces to control the service.
     *
     * @internal
     */
    private final IDs.Stub binder_ = new IDs.Stub()
    {
        /**
         * Register a notification of visualizer data updated.
         *
         * @param handle The handle of the client registers the visualizer data.
         *
         */
        public void iRegisterVisualizerData(int handle)
        {
            DsLog.log1(TAG, "Add a visualizer handle "+ handle);
            
            synchronized (lockDolbyContext_)
            {
                visManager_.register(handle);
            }
        }

        /**
         * Unregister a notification of visualizer data updated.
         *
         * @param handle The handle of the client unregisters the visualizer data.
         *
         */
        public void iUnregisterVisualizerData(int handle)
        {
            DsLog.log1(TAG, "remove a visualzier handle "+ handle);
            
            synchronized (lockDolbyContext_)
            {
                visManager_.unregister(handle);
            }
        }
        
        /**
         * Register a notification of Ds death handler.
         *
         * @param handle The handle of the client.
         * @param dh The client register death handler.
         *
         */
        public void iRegisterDeathHandler(int handle, IDsDeathHandler dh)
        {
            DsLog.log1(TAG, "iRegisterDeathHandler");

            if(dh != null)
            {
                synchronized (lockDolbyContext_)
                {
                    DsClientDeathHandler clientDeathHandler = new DsClientDeathHandler(dh, handle);
                    try
                    {
                        clientDeathHandler.linkToDeath();
                        dsClientDeathHandlerList_.put(dh.asBinder(), clientDeathHandler);
                    }
                    catch (RemoteException e)
                    {
                        Log.e(TAG, "DsService  iRegisterDeathHandler() could not link to "+dh+" binder death");
                    }       
                }
            }
        }

        /**
         *UnRegister a notification of Ds death handler.
         *
         * @param handle The handle of the client.
         * @param dh The client unregister death handler.
         *
         */
        public void iUnregisterDeathHandler(int handle, IDsDeathHandler dh)
        {
            DsLog.log1(TAG, "iUnregisterDeathHandler");

            if(dh != null)
            {
                synchronized (lockDolbyContext_)
                {
                    DsClientDeathHandler clientDeathHandler = dsClientDeathHandlerList_.remove(dh.asBinder());
                    if (clientDeathHandler != null)
                    {
                        clientDeathHandler.unlinkToDeath();
                    }
                }
            }
        }

        /**
         * Register a notification of Ds access right.
         *
         * @param handle The handle of the client registers the Ds access right.
         * @param dsClientInfo The information of the Ds client binded the Ds Service.
         *
         */
        public void iRegisterDsAccess(int handle, DsClientInfo info)
        {
            DsLog.log1(TAG, "iRegisterDsAccess");
            
            synchronized (lockDolbyContext_)
            {
                if (info != null)
                {
                    dsClientInfoMap_.put(handle, info);
                    String packageName = info.getPackageName();
                    int connected = info.getConnectionBridge();
                    arManager_.addDsConnectedApp(handle, packageName, connected);
                }
            }
        }

        /**
         * Unregister a notification of Ds access right.
         *
         * @param handle The handle of the client unregisters the Ds access right.
         *
         */
        public void iUnregisterDsAccess(int handle)
        {
            DsLog.log1(TAG, "iUnregisterDsAccess");
            
            synchronized (lockDolbyContext_)
            {
                arManager_.unRegisterDsVersion(handle);
                arManager_.removeDsConnectedApp(handle);
                dsClientInfoMap_.remove(handle);
            }
        }

        /**
         * Register a callback function to keep the applications informed of the effect on/off/configuration change.
         *
         * @param handle The handle of the client.
         * @param cb The client register the callback.
         * @param version The version of client, DS1 or DS2.
         *
         */
        public void iRegisterCallback(int handle, IDsCallbacks cb, int version)
        {
            if (cb != null)
            {
                synchronized (lockDolbyContext_)
                {
                    cbkManager_.register(cb, handle, version);
                    if (version == DsCommon.DS_CLIENT_VER_ONE)
                    {
                        arManager_.registerDsVersion(handle,version);
                    }
                    DsLog.log1(TAG, "iRegisterCallback");
                }
            }
        }

        /**
         * Unregister a callback function that has been registered to keep the applications informed of the effect
         * on/off/configuration change.
         *
         * @param handle The handle of the client.
         * @param cb The client unregister the callback.
         * @param version The version of the client, DS1 or DS2.
         *
         */
        public void iUnregisterCallback(int handle, IDsCallbacks cb, int version)
        {
            if (cb != null)
            {
                synchronized (lockDolbyContext_)
                {
                    if (version == DsCommon.DS_CLIENT_VER_ONE)
                    {
                        arManager_.unRegisterDsVersion(handle);
                    }
                    cbkManager_.unregister(cb, version);
                    boolean isGlobal = arManager_.isGlobalSettings(handle);			
                    if (isGlobal)
                    {
                        dsManager_.saveDsStateAndSettings();
                    }
                    DsLog.log1(TAG, "iUnregisterCallback");
                }
            }
        }

        /**
         * Query if the DS Audio Effect is turned on.
         *
         * @param on The latest status of DS Audio Effect.
         *
         * @return The error code.
         *
         */
        public int iGetState(int Device, int[] on)
        {
            DsLog.log1(TAG, "DsService.iGetState()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (on != null)
                    {
                        on[0] = dsManager_.getDsOn();
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in iGetState");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iGetState");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Turn on or off DS Audio Effect.
         *
         * @param handle The handle of the client.
         * @param on The state indicating whether the DS Audio Effect will be turned on.
         *
         * @return The error code.
         *
         */
        public int iSetState(int handle, int Device, boolean on)
        {
            DsLog.log1(TAG, "DsService.iSetState(" + on + ")" + ", handle = " + handle);
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        error = doSetDsOn(handle, on);
                    }
                    else
                    {
                        error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in iSetState");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iSetState");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Query the off type of the DS Audio Effect.
         *
         * @param offType The off type of the DS Audio Effect.
         *
         * @return The error code.
         *
         */
        public int iGetOffType(int[] offType)
        {
            DsLog.log1(TAG, "iGetOffType");
            int error = DsCommon.DS_NO_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (offType != null)
                {
                    try
                    {
                        offType[0] = dsManager_.getOffType();
                    }
                    catch (DeadObjectException e)
                    {
                        Log.e(TAG, "DeadObjectException in iGetOffType");
                        e.printStackTrace();
                        error = DsCommon.DS_NOT_RUNNING;
                    }
                    catch (IllegalStateException e)
                    {
                        Log.e(TAG, "IllegalStateException in iGetOffType");
                        e.printStackTrace();
                        error = DsCommon.DS_UNKNOWN_ERROR;
                    }
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Get the version of DS Service.
         *
         * @param version DS Service version.
         *
         * @return The error code.
         *
         */
        public int iGetDsServiceVersion(String[] version)
        {
            DsLog.log1(TAG, "DsService.getDsVersion");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (version != null)
                {
                    version[0] = dsManager_.getDsVersion();
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Get the version of Dap library.
         *
         * @param version Dap version.
         *
         * @return The error code.
         *
         */
        public int iGetDapLibraryVersion(String[] version)
        {
            DsLog.log1(TAG, "DsService.iGetDapLibraryVersion");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (version != null)
                    {
                        version[0] = dsManager_.getDsApVersion();
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in iGetDapLibraryVersion");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iGetDapLibraryVersion");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the version of UDC library.
         *
         * @param version UDC version.
         *
         * @return The error code.
         *
         */
        public int iGetUdcLibraryVersion(String[] version)
        {
            DsLog.log1(TAG, "DsService.iGetUdcLibraryVersion()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (version != null)
                {
                    version[0] = DsVersion.UDC_VERSION;
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Set the value of a specific Dap parameter.
         *
         * @param handle The handle of the client.
         * @param device The device which Ds Audio Effect is attached.
         * @param profile The profile to which the parameter will be applied.
         * @param parmaId The id of the parameter.
         * @param values The value of the parameter.
         *
         * @return The error code.
         *
         */
        public int iSetParameter(int handle, int device, int profile, int paramId, int[] values)
        {
            DsLog.log1(TAG, "DsService.iSetParameter()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        if (dsManager_.setParameter(profile, paramId, values))
                        {
                            if (profile == dsManager_.getSelectedProfile())
                            {
                                dsManager_.setProfileProperties(profile);
                            }
                                                        
                            boolean isGlobal = arManager_.isGlobalSettings(handle);
                            if(isGlobal && (profile == dsManager_.getSelectedProfile()))
                            {
                                cbkManager_.invokeCallback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                                cbkManager_.invokeDs1Callback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                            }
                                error = DsCommon.DS_NO_ERROR;
                            }
                        else
                        {
                            error = DsCommon.DS_INVALID_ARGUMENT;
                        }
                    }
                    else
                    {
                            error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in iSetParameter");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iSetParameter");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the value of a specific Dap parameter.
         *
         * @param device The device which Ds Audio Effect is attached.
         * @param profile The profile from which the parameter will be retrieved.
         * @param parmaId The id of the parameter.
         * @param values The value of the parameter.
         *
         * @return The error code.
         *
         */
        public int iGetParameter(int device, int profile, int paramId, int[] values)
        {
            DsLog.log1(TAG, "DsService.iGetParameter()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (values != null)
                    {
                        int[] realParam = dsManager_.getParameter(profile, paramId);
                        System.arraycopy(realParam, 0, values, 0, realParam.length);
                        error = DsCommon.DS_NO_ERROR;
                    }                 
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iGetParameter");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Set a new intelligent equalizer preset for the specified DS profile.
         *
         * @param handle The handle of the client.
         * @param device The index of the device.
         * @param preset The index of the preset.
         *
         * @return The error code.
         *
         */
        public int iSetIeqPreset(int handle, int device, int preset)
        {
            DsLog.log1(TAG, "DsService.iSetIeqPreset");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        int profile = dsManager_.getSelectedProfile();
                        if (dsManager_.setIeqPreset(profile, preset))
                        {
                            boolean isGlobal = arManager_.isGlobalSettings(handle);
                            if (isGlobal)
                            {
                                cbkManager_.invokeCallback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                                cbkManager_.invokeDs1Callback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                                dsManager_.saveDsStateAndSettings();
                            }
                            error = DsCommon.DS_NO_ERROR;
                        }
                    }
                    else
                    {
                            error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in setIeqPreset");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in setIeqPreset");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setIeqPreset");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the active intelligent equalizer preset.
         *
         * @param device The index of the device.
         * @param preset The Index of active intelligent equalizer preset.
         *
         * @return The error code.
         *
         */
        public int iGetIeqPreset(int device, int[] preset)
        {
            DsLog.log1(TAG, "DsService.iGetIeqPreset");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (preset != null)
                    {
                        int profile = dsManager_.getSelectedProfile();
                        preset[0] =  dsManager_.getIeqPreset(profile);
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in getIeqPreset");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in getIeqPreset");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Query the number of intelligent equalizer preset.
         *
         * @param device The index of the device.
         * @param count The number of intelligent equalizer preset.
         *
         * @return The error code.
         *
         */
        public int iGetIeqPresetCount(int device, int[] count)
        {
            DsLog.log1(TAG, "DsService.iGetIeqPresetCount()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (count != null)
                {
                    count[0] = DsConstants.IEQ_PRESETS_NUMBER;
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Set the DS Audio Effect profile.
         *
         * @param device The index of the device.
         * @param index The index of the DS profile to be set.
         *
         * @return The error code.
         *
         */
        public int iSetProfile(int handle, int device, int profile)
        {
            DsLog.log1(TAG, "DsService.iSetProfile(" + profile + ")");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        boolean success = doSetSelectedProfile(handle, profile);
                        if (success)
                        {
                            error = DsCommon.DS_NO_ERROR;
                        }
                    }
                    else
                    {
                            error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in setSelectedProfile");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in setSelectedProfile");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setSelectedProfile");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the selected DS profile.
         *
         * @param device The index of the device.
         * @param profile The Index of the selected DS profile.
         *
         * @return The error code.
         *
         */
        public int iGetProfile(int device, int[] profile)
        {
            DsLog.log1(TAG, "DsService.iGetProfile");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (profile != null)
                {
                    profile[0] = dsManager_.getSelectedProfile();
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Reset the DS Audio Effect profile.
         *
         * @param device The index of the device.
         * @param profile The index of DS Audio Effect profile.
         *
         * @return The error code.
         *
         */
        public int iResetProfile(int handle, int device, int profile)
        {
            DsLog.log1(TAG, "DsService.iResetProfile");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        if (dsManager_.resetProfile(profile))
                        {
                            // Update the profile properties only if the profile is the current selected one.
                            if (profile == dsManager_.getSelectedProfile())
                            {
                                dsManager_.setProfileProperties(profile);
                            }

                            boolean isGlobal = arManager_.isGlobalSettings(handle);
                            if (isGlobal)
                            {
                                cbkManager_.invokeCallback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                                cbkManager_.invokeDs1Callback(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, handle, profile, 0, null, null);
                                if(profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
                                {
                                    DsProfileName name = dsManager_.getProfileName(profile);
                                    cbkManager_.invokeCallback(DsCommon.PROFILE_NAME_CHANGED_MSG, handle, profile, 0, name.getCurrentName(), null);
                                    cbkManager_.invokeDs1Callback(DsCommon.PROFILE_NAME_CHANGED_MSG, handle, profile, 0, name.getCurrentName(), null);                                    
                                }
                                dsManager_.saveDsStateAndSettings();
                            }
                            error = DsCommon.DS_NO_ERROR;
                        }
                    }
                    else
                    {
                        error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in resetProfile");
                    e.printStackTrace();
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in resetProfile");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (UnsupportedOperationException e)
                {
                    Log.e(TAG, "UnsupportedOperationException in resetProfile");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in resetProfile");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Query the number of DS Audio Effect profile.
         *
         * @param count The number of DS Audio Effect profiles.
         *
         * @return The error code.
         *
         */
        public int iGetProfileCount(int device, int[] count)
        {
            DsLog.log1(TAG, "DsService.getProfileCount()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (count != null)
                {
                    count[0] = dsManager_.getProfileCount();
                    error =  DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Request a specific access right.
         *
         * @param handle The handle of the client.
         * @param type The type of access right.
         *
         * @return The error code.
         *
         */
         public int iRequestAccessRight(int handle, int type)
        {
            DsLog.log1(TAG, "DsService.iRequestAccessRight");
            int error = DsCommon.DS_UNKNOWN_ERROR;
            
            synchronized (lockDolbyContext_)
            {
                try
                {
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    error = arManager_.requestAccessRight(handle, type, audioManager);
                    arManager_.registerDsVersion(handle,DsCommon.DS_CLIENT_VER_TWO);
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iSetAccessLock");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Abandon a specific access right.
         *
         * @param handle The handle of the client.
         * @param type The type of access right.
         *
         * @return The error code.
         *
         */
        public int iAbandonAccessRight(int handle, int type)
        {
            DsLog.log1(TAG, "DsService.iAbandonAccessRight");
            int error = DsCommon.DS_UNKNOWN_ERROR;
            
            synchronized (lockDolbyContext_)
            {
                try
                {
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    error = arManager_.abandonAccessRight(handle, type, audioManager);
                    arManager_.unRegisterDsVersion(handle);
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iAbandonAccessRight");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Query the state of a specific access right.
         *
         * @param handle The handle of the client.
         * @param type The type of access right.
         * @param state The state of a specific access right.
         *
         * @return The error code.
         *
         */
        public int iCheckAccessRight(int handle, int type, int[] state)
        {
            DsLog.log1(TAG, "DsService.iCheckAccessRight");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (state != null)
                    {
                        state[0] = arManager_.checkAccessRight(handle, type);
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iGetAccessLock");
                    e.printStackTrace();
                }
            }
            return error;

        }

        /**
         * Get if the profile settings are modified from factory settings.
         *
         * @param device The index of the device.
         * @param profile The index of the profile.
         * @param flag The modify status.
         *
         * @return The error code.
         *
         */
        public int iGetProfileModified(int device, int profile, boolean[] flag)
        {
            DsLog.log1(TAG, "DsService.iGetProfileModified");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (flag != null)
                {
                    flag[0] = ((dsManager_.getProfileModified(profile) == DsCommon.DS_PROFILE_NOT_MODIFIED)) ? false : true;
                    DsLog.log1(TAG, "DsService.iGetProfileModified " + flag[0]);
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Get Status of speaker.
         *
         * @param mono The type of the speaker.
         *
         * @return The error code.
         *
         */
        public int iGetMonoSpeaker(boolean[] mono)
        {
            DsLog.log1(TAG, "DsService.iGetMonoSpeaker");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (mono != null)
                {
                    String  monoSpeaker = DsProperty.getMonoSpeakerProperty();
                    if (monoSpeaker.equals("true"))
                    {
                        mono[0] = true;
                    }
                    else
                    {
                        mono[0] = false;
                    }
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Get the array length of a specific parameter.
         *
         * @param parmaId The id of the parameter.
         * @param len The length of the array.
         *
         * @return The error code.
         *
         */
        public int iGetParamLength(int paramId, int[] len)
        {
            DsLog.log1(TAG, "DsService.iGetParamLength");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (len != null)
                {
                    len[0] = dsManager_.getParamLength(paramId);
                    error = DsCommon.DS_NO_ERROR;
                }
                else
                {
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
            }
            return error;
        }

        /**
         * Active a special tuning for an endpoint on the product.
         *
         * @param handle The handle of the client.
         * @param endpointPort The endpoint port of the specific device.
         * @param productId The identification of the specific product.
         *
         * @return The error code.
         *
         */
        public int iActivateTuning(int handle, int endpointPort, String productId)
        {
            DsLog.log1(TAG, "DsService.iActivateTuning");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            //Todo...
            
            return error;
        }

        /**
         * Deactivate the specific tuning for an endpoint on the product.
         *
         * @param handle The handle of the client.
         * @param endpointPort The endpoint port of the specific device.
         * @param productId The identification of the specific product.
         *
         * @return The error code.
         *
         */
        public int iDeactivateTuning(int handle, int endpointPort, String productId)
        {
            DsLog.log1(TAG, "DsService.iDeactivateTuning");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            //Todo...
            
            return error;
        }

        /**
         * Set a new name for the specified DS profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS profile.
         * @param name The new name from the client.
         *
         * @return The error code.
         *
         */
        public int iSetProfileName(int handle, int profile, DsProfileName name)
        {
            DsLog.log1(TAG, "DsService.iSetProfileName");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean isPermitted = arManager_.isAppAccessPermitted(handle);
                    if (isPermitted)
                    {
                        if(dsManager_.setProfileName(profile,name))
                        {
                            if(name.getCurrentName() != null)
                            {
                                cbkManager_.invokeCallback(DsCommon.PROFILE_NAME_CHANGED_MSG, handle, profile, 0, name.getCurrentName(), null);
                                cbkManager_.invokeDs1Callback(DsCommon.PROFILE_NAME_CHANGED_MSG, handle, profile, 0, name.getCurrentName(), null);
                            } 
                            dsManager_.saveDsStateAndSettings();
                            error = DsCommon.DS_NO_ERROR;
                        }
                    }
                    else
                    {
                        error = DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in iSetProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (UnsupportedOperationException e)
                {
                    Log.e(TAG, "UnsupportedOperationException in iSetProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setProfileName");
                    e.printStackTrace();
                }
            }
            return error;
        }
        
        /**
         * Get a new name of the specified DS profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS profile.
         * @param name The name of selected profile.
         *
         * @return The error code.
         *
         */
        public int iGetProfileName(int handle, int profile, DsProfileName[] name)
        {
            DsLog.log1(TAG, "DsService.iGetProfileName");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (name != null)
                    {
                        DsProfileName realName = dsManager_.getProfileName(profile);
                        name[0] = realName;
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in iGetProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (UnsupportedOperationException e)
                {
                    Log.e(TAG, "UnsupportedOperationException in iGetProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in iGetProfileName");
                    e.printStackTrace();
                }
            }
            return error;
        }
    };

    // --------------------------------------------------------
    // Implementations of common functionality
    // --------------------------------------------------------
    private int doToggleDsOn(int handle) throws DeadObjectException
    {
        // Lock the dolby context while we toggle to make sure it
        // doesn't change before we actually apply our toggle
        synchronized (lockDolbyContext_)
        {
            boolean on = (dsManager_.getDsOn() == DsConstants.DS_STATE_ON)? true : false;
            return doSetDsOn(handle, !on);
        }
    }

    private int doSetDsOn(int handle, boolean on) throws DeadObjectException
    {
        synchronized (lockDolbyContext_)
        {
            if (dsManager_.getDsSuspended())
            {
                DsLog.log1(TAG, "DS_REQUEST_FAILED_EFFECT_SUSPENDED");
                return DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED;
            }

            dsManager_.setDsOn(on);

            // Update the system property to match the current state
            int newStatus = dsManager_.getDsOn();
            String curState = (newStatus == INT_ON) ? STATE_ON : STATE_OFF;
            try
            {
                DsProperty.setStateProperty(curState);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Exception found in setting DS state property");
                ex.printStackTrace();
            }

            boolean isGlobal = arManager_.isGlobalSettings(handle);			
            if (isGlobal)
            {
                cbkManager_.invokeCallback(DsCommon.DS_STATUS_CHANGED_MSG, handle, newStatus, 0, null, null);
                cbkManager_.invokeDs1Callback(DsCommon.DS_STATUS_CHANGED_MSG, handle, newStatus, 0, null, null);
                dsManager_.saveDsStateAndSettings();
            }
  
            visManager_.toggleVisualizer((newStatus == DsConstants.DS_STATE_ON)? true : false);
        }
        return DsCommon.DS_NO_ERROR;
    }

    private boolean doSetSelectedProfile(int handle, int profile) throws DeadObjectException
    {
        synchronized (lockDolbyContext_)
        {
            boolean success = dsManager_.setSelectedProfile(profile);
            int newProfile = dsManager_.getSelectedProfile();

            if (success && profile == newProfile)
            {
                doUpdateSelectedProfile(handle, profile);
            }

            return success && profile == newProfile;
        }
    }

    private void doUpdateSelectedProfile(int handle, int profile)
    {
        // Set system property
        dsManager_.setProfileProperties(profile);

        //send a callback only when the access of this client is DsGlobal.
        boolean isGlobal = arManager_.isGlobalSettings(handle);			
        if (isGlobal)
        {
            cbkManager_.invokeCallback(DsCommon.PROFILE_SELECTED_MSG, handle, profile, 0, null, null);
            cbkManager_.invokeDs1Callback(DsCommon.PROFILE_SELECTED_MSG, handle, profile, 0, null, null);
            dsManager_.saveDsStateAndSettings();
        }
    }
}

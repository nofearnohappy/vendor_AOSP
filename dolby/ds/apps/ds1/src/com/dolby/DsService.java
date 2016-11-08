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

package com.dolby;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalStateException;
import java.util.ArrayList;

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
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.DeadObjectException;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import android.dolby.ds.Ds;
import android.dolby.ds.DsAkSettings;
import android.dolby.IDs;
import android.dolby.IDsServiceCallbacks;
import android.dolby.DsClientSettings;
import android.dolby.DsConstants;
import android.dolby.DsLog;
import android.dolby.DsCommon;

import java.util.List;

public class DsService extends Service
{
    private static final String TAG = "DsService";
    /**
     * The last setting adopted to keep track of the setting change.
     *
     * @internal
     */
    private DsClientSettings lastSettings_ = null;

    /**
     * The interface to control the DS OpenSL-ES AudioEffect.
     *
     * @internal
     */
    private Ds ds_ = null;

    /**
     * Flag to enable/disable non persistent mode
     *
     * @internal
     */
    private boolean nonPersistentMode_ = false;

    /**
     * The lock for protecting the DS context to be thread safe.
     *
     * @internal
     */
    private final Object lockDolbyContext_ = new Object();

    /**
     * The lock for protecting the callback list to be thread safe.
     *
     * @internal
     */
    private final Object lockCallbacks_ = new Object();

    /**
     * This is a list of callbacks that have been registered with the service. Note that this is package scoped
     * (instead of private) so that it can be accessed more efficiently from inner classes.
     *
     * @internal
     */
    private final RemoteCallbackList<IDsServiceCallbacks> callbacks_ = new RemoteCallbackList<IDsServiceCallbacks>();

    /**
     * The list to store the client who is interested in the visualizer data.
     *
     * @internal
     */
    private ArrayList<Integer> visualizerList_ = new ArrayList<Integer>();

    /**
     * The list to store the clients who are interested in the audio processing parameter change event.
     * @internal
     */
    private ArrayList<Integer> dsApParamEventList_ = new ArrayList<Integer>();

    private static final String ACTION_DOLBY_LAUNCH_APP = "com.dolby.LAUNCH_DS_APP";
   
     /**
     * The global audio session id.
     *
     * @internal
     */
    private static final int GLOBAL_AUDIO_SESSION_ID = 0;       
    
    /**
     * The strings defined for setting system properties.
     *
     * @internal
     */
    private static final String STATE_ON = "on";
    private static final String STATE_OFF = "off";
    private static final String PROP_DS_STATE = "dolby.ds.state";
    private static final String PROP_DS_PROFILE_NAME = "dolby.ds.profile.name";
    private static final String PROP_DS_GEQ_STATE = "dolby.ds.graphiceq.state";
    private static final String PROP_DS_IEQ_STATE = "dolby.ds.intelligenteq.state";
    private static final String PROP_DS_IEQ_PRESET = "dolby.ds.intelligenteq.preset";
    private static final String PROP_DS_VOLUMELEVELER_STATE = "dolby.ds.volumeleveler.state";
    private static final String PROP_DS_HEADPHONE_VIRTUALIZER_STATE = "dolby.ds.hpvirtualizer.state";
    private static final String PROP_DS_SPEAKER_VIRTUALIZER_STATE = "dolby.ds.spkvirtualizer.state";
    private static final String PROP_DS_DIALOGENHANCER_STATE = "dolby.ds.dialogenhancer.state";
    private static final String PROP_MONO_SPEAKER = "dolby.monospeaker";

    private static final String DS_EFFECT_SUSPEND_ACTION = "DS_EFFECT_SUSPEND_ACTION";
    private static final int DS_EFFECT_SUSPENDED = 1;
    private static final int DS_EFFECT_UNSUSPENDED = 0;

    /**
     * The flag to determine whether DsEffect is suspended or not.
     * @internal
     */
    private boolean isDsEffectSuspended = false;

    /**
     * The flag to dertermine whether Ds is ON during DsEffect suspend or not.
     * @internal
     */
    private boolean isDsOnWhileSuspend = false;

    /**
     * The flag to determine whether to adopt file system settings or the built-in asset settings.
     * NOTE: On switching to the built-in asset settings, do NOT forget to put the file ds1-default.xml,
     *       which contains the default configuration, into the assets sub-folder in this package.
     * @internal
     */
    private static final boolean isDefaultSettingsOnFileSystem = true;

    /**
     * The path where the file system settings are stored.
     * @internal
     */
    private static final String DS_DEFAULT_SETTINGS_USER_PATH = "/vendor/etc/dolby";

    /**
     * The file name to store the default settings.
     * @internal
     */
    private static final String DS_DEFAULT_SETTINGS_FILENAME = "ds1-default.xml";

    /**
     * The list of widgets wanted to be updated.
     *
     * @internal
     */
    private ArrayList<IDolbyWidgetUpdateStatus> appWidgetList_ = new ArrayList<IDolbyWidgetUpdateStatus>();

    /**
     * The handler for the visualizer data.
     *
     * @internal
     */
    private Handler visualizerHandler_;

    /**
     * The handler thread for the visualizer data.
     *
     * @internal
     */
    private HandlerThread visualizerThread_;

    /**
     * The period of generating visualizer data, in millisecond.
     *
     * @internal
     */
    private static final int VISUALIZER_UPDATE_TIME = 96;

    /**
     * The period of generating visualizer data, in millisecond.
     *
     * @internal
     */
    private int geqBandCount_ = 0;

    /**
     * The gains of visualizer data.
     *
     * @internal
     */
    private float[] gains_ = null;

    /**
     * The excitations of visualizer data.
     *
     * @internal
     */
    private float[] excitations_ = null;

    /**
     * The flag whether the visualizer data is suspended.
     *
     * @internal
     */
    private boolean isVisualizerSuspended_ = false;

    /**
     * The counter which is used to decide whether to change the suspended status.
     *
     * @internal
     */
    private int noVisualizerCounter_ = 0;

    /**
     * The size of the visualizer date retrieved in previous update period.
     *
     * @internal
     */
    private int previousVisualizerSize_ = 0;

    /**
     * The threshold of the counter.
     *
     * @internal
     */
    private static final int COUNTER_THRESHOLD = 500 / VISUALIZER_UPDATE_TIME;

    /**
     * Integer value for on status.
     *
     *  @internal
     */
    private static final int INT_ON = 1;

    /**
     * Integer value for off status.
     *
     *  @internal
     */
    private static final int INT_OFF = 0;

    /**
     * A zero handle indicate the call is from an Intent.
     *
     *  @internal
     */
    private static final int ZERO_HANDLE = 0;

    /**
     * The callback of the visualizer thread.
     *
     * @internal
     */
    private final Runnable cbkOnVisualizerUpdate_ = new Runnable()
    {
        public void run()
        {
            visualizerUpdate();
        }
    };

    /**
     * The method runs in the visualizer thread.
     *
     * @internal
     */
    private void visualizerUpdate()
    {
        synchronized (lockDolbyContext_)
        {
            int len = 0;
            try
            {
                len = ds_.getVisualizerData(gains_, excitations_);
                if (len != previousVisualizerSize_)
                {
                    noVisualizerCounter_ = 0;
                }
                previousVisualizerSize_ = len;
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception in visualizerUpdate");
                e.printStackTrace();
            }
            if (len == 0)
            {
                // no audio is processing
                if (!isVisualizerSuspended_)
                {
                    // increase the counter
                    noVisualizerCounter_++;
                    if (noVisualizerCounter_ >= COUNTER_THRESHOLD)
                    {
                        // call onVisualizerSuspended with true
                        isVisualizerSuspended_ = true;
                        noVisualizerCounter_ = 0;
                        Message msg = new Message();
                        msg.what = DsCommon.VISUALIZER_SUSPENDED_MSG;
                        mHandler.sendMessage(msg);
                        DsLog.log1(TAG, "send VISUALIZER_SUSPENDED_MSG with true");
                    }
                }
                //Still in suspend mode, do not send callback
            }
            else
            {
                // processing audio
                if (isVisualizerSuspended_)
                {
                     // increase the counter
                    noVisualizerCounter_++;
                    if (noVisualizerCounter_ >= COUNTER_THRESHOLD)
                    {
                        // call onVisualizerSuspended with false
                        isVisualizerSuspended_ = false;
                        noVisualizerCounter_ = 0;
                        Message msg = new Message();
                        msg.what = DsCommon.VISUALIZER_SUSPENDED_MSG;
                        mHandler.sendMessage(msg);
                        DsLog.log1(TAG, "send VISUALIZER_SUSPENDED_MSG with false");
                    }
                }
                else
                {
                    // To avoid the last timer changes the gains and excitations form all zero to other values
                    // when the ds is turned off.
                    try
                    {
                        if (!ds_.getDsOn())
                        {
                            for(int i = 0; i < geqBandCount_; i++)
                            {
                                gains_[i] = 0.0f;
                                excitations_[i] = 0.0f;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                         Log.e(TAG, "Exception found in visualizerUpdate");
                         e.printStackTrace();
                    }
                    //Still in working mode, send VISUALIZER_UPDATED_MSG
                    Message msg = new Message();
                    msg.what = DsCommon.VISUALIZER_UPDATED_MSG;
                    mHandler.sendMessage(msg);
                }
            }

            if (visualizerHandler_ != null)
            {
                visualizerHandler_.removeCallbacks(cbkOnVisualizerUpdate_);
                visualizerHandler_.postDelayed(cbkOnVisualizerUpdate_, VISUALIZER_UPDATE_TIME);
            }
        }
    }

    /**
     * The method starts visualizer data updating.
     *
     * @internal
     */
    private void startVisualizer()
    {
        try
        {
            if (ds_.getDsOn())
            {
                ds_.setVisualizerOn(true);
                
                if (visualizerThread_ == null)
                {
                    visualizerThread_ = new HandlerThread("visualiser thread");
                    visualizerThread_.start();
                }

                if (visualizerHandler_ == null)
                {
                    visualizerHandler_ = new Handler(visualizerThread_.getLooper());
                }
                visualizerHandler_.post(cbkOnVisualizerUpdate_);
                DsLog.log1(TAG, "Visualizer thread is started.");
            }
            else
            {
                DsLog.log1(TAG, "DS is off, will start visualizer thread when it switches to on.");
            }
        }
        catch (Exception e)
        {
             Log.e(TAG, "Exception found in startVisualizer");
             e.printStackTrace();
        }
    }

    /**
     * The method stops visualizer data updating.
     *
     * @internal
     */
    private void stopVisualizer()
    {
        // When this method is called, both on/off status is valid.
        // So we don't need to check the status of the effect, just remove the thread.
        try
        {
            ds_.setVisualizerOn(false);
            
            if (visualizerHandler_ != null)
            {
                visualizerHandler_.getLooper().quit();
                visualizerHandler_ = null;
                visualizerThread_ = null;
            }
        }
        catch (Exception e)
        {
             Log.e(TAG, "Exception found in stopVisualizer");
             e.printStackTrace();
        }
        // Set gains and excitations to zero
        for(int i = 0; i < geqBandCount_; i++)
        {
            gains_[i] = 0.0f;
            excitations_[i] = 0.0f;
        }
        noVisualizerCounter_ = 0;
    }

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
                synchronized (lockDolbyContext_)
                {
                    String action = intent.getAction();
                    String cmd = intent.getStringExtra(DsCommon.CMDNAME);
                    String name = intent.getStringExtra(DsCommon.WIDGET_CLASS);
                    DsLog.log1(TAG, "intentReceiver_.onReceive " + action + " / " + cmd + " / " + name);

                    if (DsCommon.INIT_ACTION.equals(action))
                    {
                        // register the widget
                        if(name != null)
                        {
                            if (name.equals("com.dolby.ds.DolbyWidgetSmallProvider"))
                            {
                                appWidgetList_.add((IDolbyWidgetUpdateStatus)DolbyWidgetSmallProvider.getInstance());
                            }
                            else if (name.equals("com.dolby.ds.DolbyWidgetExtraLargeProvider"))
                            {
                                appWidgetList_.add((IDolbyWidgetUpdateStatus)DolbyWidgetExtraLargeProvider.getInstance());
                            }
                            else
                            {
                                // if we have other widget
                            }
                        }
                        notifyWidget();
                    }
                    else if ((intent.getAction().equals(Intent.ACTION_REBOOT) || intent.getAction().equals(Intent.ACTION_SHUTDOWN)) &&
                         (ds_ != null) )
                    {
                        DsLog.log1(TAG, "Save DS state and current settings before shutting down...");
                        if (!nonPersistentMode_)
                        {
                            ds_.saveDsStateAndSettings();
                        }
                    }
                    else if (action.equals("media_server_started"))
                    {
                        boolean success = ds_.validateDsEffect();
                        if (success)
                        {
                            DsLog.log1(TAG, "DS effect recreate successfully");
                        }
                        else
                        {
                            DsLog.log1(TAG, "DS effect not recreated");
                        }
                    }
                    else if (action.equals(DS_EFFECT_SUSPEND_ACTION))
                    {
                        DsLog.log1(TAG, "DS_EFFECT_SUSPEND_ACTION " + this.getResultCode());
                        switch (this.getResultCode())
                        {
                            case DS_EFFECT_SUSPENDED:
                            {
                                isDsEffectSuspended = true;
                                // Check the system property to know the current UI state of DS instead
                                // of using ds_.getDsOn as the underlying effect might already be suspended.
                                String dsState = SystemProperties.get(PROP_DS_STATE);
                                if (dsState.equals(STATE_ON))
                                {
                                    DsLog.log1(TAG, "DS_EFFECT_SUSPEND_ACTION UI OFF");
                                    isDsOnWhileSuspend = true;
                                    doSetDsOn(ZERO_HANDLE, false);
                                }
                                break;
                            }

                            case DS_EFFECT_UNSUSPENDED:
                            {
                                isDsEffectSuspended = false;
                                if (isDsOnWhileSuspend)
                                {
                                    DsLog.log1(TAG, "DS_EFFECT_SUSPEND_ACTION UI ON");
                                    isDsOnWhileSuspend = false;
                                    doSetDsOn(ZERO_HANDLE, true);
                                }
                                break;
                            }

                            default:
                                break;

                        }
                    }
                }
            }
            catch (RuntimeException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                 Log.e(TAG, "Exception found in DsService::onReceive()");
                 ex.printStackTrace();
            }
        }
    };

    /**
     * Notify the widget to update the status of DS effect.
     * NOTE: If the init intent coming from widgets is not received by the service, the instances of each widget will be added as well
     *       so that they can be notified of the status update.
     *
     * @internal
     */
    private void notifyWidget()
    {
        try
        {
            // update the widgets
            DsWidgetStatus newStatus = DsWidgetStatus.getInstance();
            newStatus.setOn(ds_.getDsOn());
            int selectedProfile = ds_.getSelectedProfile();
            newStatus.setProfile(selectedProfile);
            int modifiedValue = ds_.getProfileModified(selectedProfile);

            if ((modifiedValue & DsCommon.DS_PROFILE_NAME_MODIFIED) == DsCommon.DS_PROFILE_NAME_MODIFIED)
            {
                newStatus.setProfileName(ds_.getProfileNames()[selectedProfile]);
                newStatus.setModified(true);
            }
            else
            {
                newStatus.setModified(false);
            }
            // NOTE: For some reason, sometimes the init intent is not received by the service although the widget DOES send the intent.
            // If this case occurs, we will add instances of each widget if they have not been properly registered.
            if (appWidgetList_.size() == 0)
            {
                if ((IDolbyWidgetUpdateStatus)DolbyWidgetExtraLargeProvider.getInstance() != null)
                    appWidgetList_.add((IDolbyWidgetUpdateStatus)DolbyWidgetExtraLargeProvider.getInstance());
                if ((IDolbyWidgetUpdateStatus)DolbyWidgetSmallProvider.getInstance() != null)
                    appWidgetList_.add((IDolbyWidgetUpdateStatus)DolbyWidgetSmallProvider.getInstance());
            }
            int n = appWidgetList_.size();
            for (int i = 0; i < n; i++)
            {
                IDolbyWidgetUpdateStatus widget = appWidgetList_.get(i);
                if (widget != null)
                {
                    DsLog.log2(TAG, "notifyWidget, i = " + i);
                    widget.notifyStatusUpdate(this, newStatus);
                }
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Exception found in DsService::notifyWidget()");
            ex.printStackTrace();
        }
    }

    /**
     * Send status and all profile settings change events to all connected clients.
     *
     * @internal
     */
    private void sendAllEventsToClients()
    {
        try
        {
            boolean newStatus = ds_.getDsOn();
            Message msg = new Message();
            msg.what = DsCommon.DS_STATUS_CHANGED_MSG;
            msg.arg1 = ZERO_HANDLE;
            msg.arg2 = (newStatus == true) ? INT_ON : INT_OFF;
            mHandler.sendMessage(msg);

            Message msg2 = new Message();
            msg2.what = DsCommon.PROFILE_SELECTED_MSG;
            msg2.arg1 = ZERO_HANDLE;
            msg2.arg2 = ds_.getSelectedProfile();
            mHandler.sendMessage(msg2);

            // Send profile settings changed message
            for (int i = DsConstants.PROFILE_INDEX_MIN; i <= DsConstants.PROFILE_INDEX_MAX; ++i)
            {
                Message msg3 = new Message();
                msg3.what = DsCommon.PROFILE_SETTINGS_CHANGED_MSG;
                msg3.arg1 = ZERO_HANDLE;
                msg3.arg2 = i;
                mHandler.sendMessage(msg3);

                if (i >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
                {
                    // We also need to send a name changed message here
                    String[] names = ds_.getProfileNames();
                    Message msg4 = new Message();
                    msg4.what = DsCommon.PROFILE_NAME_CHANGED_MSG;
                    msg4.arg1 = ZERO_HANDLE;
                    msg4.arg2 = i;
                    msg4.obj = new String(names[i]);
                    mHandler.sendMessage(msg4);
                }
            }

            // Refresh the widget
            synchronized (lockDolbyContext_)
            {
                notifyWidget();
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Exception found in DsService::notifyClients()");
            ex.printStackTrace();
        }
    }

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
            state_createDs(null);
            IntentFilter commandFilter = new IntentFilter();
            commandFilter.addAction(DsCommon.INIT_ACTION);
            commandFilter.addAction(Intent.ACTION_REBOOT);
            commandFilter.addAction(Intent.ACTION_SHUTDOWN);
            commandFilter.addAction("media_server_started");
            commandFilter.addAction(DS_EFFECT_SUSPEND_ACTION);
            // add other actions here
            registerReceiver(intentReceiver_, commandFilter);
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
            if (!nonPersistentMode_)
            {
                ds_.saveDsStateAndSettings();
            }
            try
            {
                ds_.setDsOn(false);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Exception found in DsService.onDestory()");
                ex.printStackTrace();
            }
        }

        synchronized (lockCallbacks_)
        {
            // Unregister all callbacks.
            callbacks_.kill();
            // cleanup the visualizer list
            int size = visualizerList_.size();
            for(int i=0; i<size; i++)
            {
                visualizerList_.remove(i);
            }
            visualizerList_ = null;
        }

        // Remove the next pending message to increment the counter, stopping the increment loop.
        mHandler.removeMessages(DsCommon.DS_STATUS_CHANGED_MSG);
        mHandler.removeMessages(DsCommon.PROFILE_SELECTED_MSG);
        mHandler.removeMessages(DsCommon.PROFILE_SETTINGS_CHANGED_MSG);
        mHandler.removeMessages(DsCommon.PROFILE_NAME_CHANGED_MSG);
        mHandler.removeMessages(DsCommon.VISUALIZER_UPDATED_MSG);
        mHandler.removeMessages(DsCommon.VISUALIZER_SUSPENDED_MSG);
        mHandler.removeMessages(DsCommon.EQ_SETTINGS_CHANGED_MSG);
        mHandler.removeMessages(DsCommon.DS_PARAM_CHANGED_MSG);
        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();

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
                        doToggleDsOn(ZERO_HANDLE);
                    }
                }
                else if (DsCommon.SELECTPROFILE_ACTION.equals(action))
                {
                    synchronized (lockDolbyContext_)
                    {
                        int profile = callerIntent.getIntExtra(DsCommon.CMDNAME, 0);

                        doSetSelectedProfile(ZERO_HANDLE, profile);
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
     * Load the settings from the .xml files.
     * @internal
     *
     * @param path The file path containing the file system settings.
     * @return true if the settings is successfully loaded, and false otherwise.
     */
    private boolean loadSettings(String path)
    {
        boolean ret = true;
        String fileDir =  null;
        InputStream defaultInStream = null;

        try 
        {
            fileDir = getFilesDir().getAbsolutePath();
            if (isDefaultSettingsOnFileSystem) 
            {
                DsLog.log1(TAG, "Adopting the file system settings...");
                if (path != null) 
                {
                    defaultInStream = new FileInputStream(path);
                }
                else
                {
                    Log.e(TAG, "The user settings path NOT defined!");
                }
            }
            else 
            {
                DsLog.log1(TAG, "Adopting the built-in settings in assets...");
                AssetManager am = getAssets();
                defaultInStream = am.open(DS_DEFAULT_SETTINGS_FILENAME);
            }
            File file = new File(fileDir, Ds.DS_CURRENT_FILENAME);
            if (file.exists()) 
            {
                DsLog.log1(TAG, file.getAbsolutePath() + " alread exists");
            }
            else 
            {
                DsLog.log1(TAG, "Creating " + file.getAbsolutePath());
                // Allow all other applications to have read & write access to the created file.
                FileOutputStream fos = openFileOutput(Ds.DS_CURRENT_FILENAME, Context.MODE_PRIVATE);
                fos.close();
            }
            file = new File(fileDir, Ds.DS_STATE_FILENAME);
            if (file.exists()) 
            {
                DsLog.log1(TAG, file.getAbsolutePath() + " alread exists");
            }
            else 
            {
                DsLog.log1(TAG, "Creating " + file.getAbsolutePath());
                // Allow all other applications to have read access to the created file.
                FileOutputStream fos = openFileOutput(Ds.DS_STATE_FILENAME, Context.MODE_PRIVATE);
                fos.close();
            }
        }
        catch (FileNotFoundException e) 
        {
            Log.e(TAG, "FileNotFoundException was caught");
            e.printStackTrace();
            ret = false;
        }
        catch (IOException e)
        {
            Log.e(TAG, "IOException was caught");
            e.printStackTrace();
            ret = false;
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception was caught");
            e.printStackTrace();
            ret = false;
        }

        if(defaultInStream != null)
        {
            if(ret)
            {
                ret = ds_.populateSettings(defaultInStream, fileDir);
            }
            else
            {
                try
                {
                    defaultInStream.close();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else
        {
            ret = false;
        }

        return ret;
    }

    /**
     * Create the instance of DS effect.
     *
     * @param callerIntent The intent creates the effect.
     *
     * @internal
     */
    private void state_createDs(Intent callerIntent)
    {
        // defaulting to a zero audioSessionId, which results in the AudioEffect
        // being created on the Global mixer

        DsLog.log1(TAG, "createDs()");

        String userSettingsPath = null;
        if (isDefaultSettingsOnFileSystem)
        {
            userSettingsPath = DS_DEFAULT_SETTINGS_USER_PATH + "/" + DS_DEFAULT_SETTINGS_FILENAME;
        }

        try
        {
            synchronized (lockDolbyContext_)
            {
                if (loadSettings(userSettingsPath))
                {
                    geqBandCount_ = DsAkSettings.getGeqBandCount();
                    if (geqBandCount_ > 0)
                    {
                        gains_ = new float[geqBandCount_];
                        excitations_ = new float[geqBandCount_];

                        ds_ = new Ds(GLOBAL_AUDIO_SESSION_ID);

                        // Set system property
                        boolean on = ds_.getDsOn();
                        int profile = ds_.getSelectedProfile();
                        String curState = (on == true) ? STATE_ON : STATE_OFF;
                        SystemProperties.set(PROP_DS_STATE, curState);
                        setProfileProperties(profile);

                        notifyWidget();

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
        }
        catch (InstantiationException ex)
        {
            Log.e(TAG, "Ds() FAILED! InstantiationException");
        }
        catch (IllegalStateException ex)
        {
            Log.e(TAG, "Ds() FAILED! IllegalStateException");
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Ds() FAILED! Exception");
            ex.printStackTrace();
        }
    }

    /**
     * Set the system properties related to profile.
     *
     * @internal
     */
    private void setProfileProperties (int profile)
    {
        SystemProperties.set(PROP_DS_PROFILE_NAME, DsCommon.PROFILE_NAMES[profile]);

        DsClientSettings settings = ds_.getProfileSettings(profile);
        String state = (settings.getDialogEnhancerOn() == true) ? STATE_ON : STATE_OFF;
        SystemProperties.set(PROP_DS_DIALOGENHANCER_STATE, state);

        state = (settings.getHeadphoneVirtualizerOn() == true) ? STATE_ON : STATE_OFF;
        SystemProperties.set(PROP_DS_HEADPHONE_VIRTUALIZER_STATE, state);

        state = (settings.getSpeakerVirtualizerOn() == true) ? STATE_ON : STATE_OFF;
        SystemProperties.set(PROP_DS_SPEAKER_VIRTUALIZER_STATE, state);

        state = (settings.getVolumeLevellerOn() == true) ? STATE_ON : STATE_OFF;
        SystemProperties.set(PROP_DS_VOLUMELEVELER_STATE, state);

        state = (settings.getGeqOn() == true) ? STATE_ON : STATE_OFF;
        SystemProperties.set(PROP_DS_GEQ_STATE, state);

        int index = ds_.getIeqPreset(profile);
        if (index == 0)
        {
            SystemProperties.set(PROP_DS_IEQ_STATE, STATE_OFF);
        }
        else
        {
            SystemProperties.set(PROP_DS_IEQ_STATE, STATE_ON);
        }
        SystemProperties.set(PROP_DS_IEQ_PRESET, DsCommon.IEQ_PRESET_NAMES[index]);
    }

    private Intent getDsConsumerAppIntent() {
        Intent intent = new Intent(ACTION_DOLBY_LAUNCH_APP);
        PackageManager p = getPackageManager();
        if(p != null)
        {
            List<ResolveInfo> ris = p.queryIntentActivities(intent,
                    PackageManager.GET_DISABLED_COMPONENTS);
            return (ris != null && !ris.isEmpty()) ? intent : null;
        }
        else
        {
            return null;
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
         * Query if the DS Audio Effect is turned on.
         *
         * @param on The latest status of DS Audio Effect.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#getDsOn(boolean[])
         */
        public int getDsOn(boolean[] on)
        {
            DsLog.log1(TAG, "IDs.getDsOn()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (on != null)
                    {
                        on[0] = ds_.getDsOn();
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in getDsOn");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in getDsOn");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Turn on or off DS Audio Effect.
         *
         * @param handle The handle of the client who calls this method.
         * @param on The state indicating whether the DS Audio Effect will be turned on.
         * @return The error code.
         *
         * @see android.dolby.IDs#setDsOn(boolean)
         */
        public int setDsOn(int handle, boolean on)
        {
            DsLog.log1(TAG, "IDs.setDsOn(" + on + ")");
            int error = DsCommon.DS_UNKNOWN_ERROR;
            try
            {
                error = doSetDsOn(handle, on);
            }
            catch (DeadObjectException e)
            {
                Log.e(TAG, "DeadObjectException in setDsOn");
                e.printStackTrace();
                error = DsCommon.DS_NOT_RUNNING;
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception in setDsOn");
                e.printStackTrace();
            }
            return error;
        }

        /**
         * Turns non-persistent mode on or off.
         *
         * When non-persistent is on, any changes made to DS will be applied but will not be stored persistently.
         * When non-persistent mode is turned off, the state will be restored to the state just before non-persistent mode was turned on.
         * If the device is shut-down while non-persistent mode is on, non-persistent mode will not be on when the device starts up again.
         * The state will be the same as it was before non-persistent mode was turned on prior to shut-down.
         *
         * @param on The new state of non-persistent mode.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setNonPersistentMode(boolean)
         */
        public int setNonPersistentMode(boolean on)
        {
            DsLog.log1(TAG, "IDs.setNonPersistentMode(" + on + ")");
            int error = DsCommon.DS_NO_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (on)
                {
                    if (nonPersistentMode_)
                    {
                        DsLog.log1(TAG, "nonPersistentMode_ already set");
                        return error;
                    }
                    // Before turning on non-persistent mode, save the current Ds settings.
                    ds_.saveDsStateAndSettings();
                    nonPersistentMode_ = on;
                }
                else
                {
                    // If non persistent mode was already set, then load back the settings
                    if (nonPersistentMode_)
                    {
                        String userSettingsPath = null;
                        if (isDefaultSettingsOnFileSystem)
                        {
                            userSettingsPath = DS_DEFAULT_SETTINGS_USER_PATH + "/" + DS_DEFAULT_SETTINGS_FILENAME;
                        }

                        try
                        {
                            if (loadSettings(userSettingsPath))
                            {
                                ds_.restoreCurrentProfiles();
                                // Set system property
                                int profile = ds_.getSelectedProfile();
                                String curState = (ds_.getDsOn() == true) ? STATE_ON : STATE_OFF;
                                SystemProperties.set(PROP_DS_STATE, curState);
                                setProfileProperties(profile);

                                // Send settings change message to all connected clients
                                sendAllEventsToClients();
                                nonPersistentMode_ = on;
                            }
                            else
                            {
                                Log.e(TAG, "loadSettings FAILED! DS settings are NOT loaded successfully.");
                                error = DsCommon.DS_INVALID_STATE;
                            }
                        }
                        catch (Exception e)
                        {
                            Log.e(TAG, "Exception in setDsOn");
                            e.printStackTrace();
                            error = DsCommon.DS_UNKNOWN_ERROR;
                        }
                    }
                }
            }
            return error;
        }

        /**
         * Query the number of DS Audio Effect profile.
         *
         * @param count The number of DS Audio Effect profiles.
         * @return The error code.
         *
         * @see android.dolby.IDs#getProfileCount(int[])
         */
        public int getProfileCount(int[] count)
        {
            DsLog.log1(TAG, "IDs.getProfileCount()");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (count != null)
                {
                    count[0] = ds_.getProfileCount();
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
         * Get the name of the DS profile.
         *
         * @param names All profile names in an array.
         * @return The error code.
         *
         * @see android.dolby.IDs#getProfileNames(String[])
         */
        public int getProfileNames(String[] names)
        {
            DsLog.log1(TAG, "IDs.getProfileNames");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                String[] realNames = ds_.getProfileNames();
                System.arraycopy(realNames, 0, names, 0, realNames.length);
                error = DsCommon.DS_NO_ERROR;
            }
            return error;
        }

        /**
         * Get the number of frequency bands.
         *
         * @param count The number of frequency bands.
         * @return The error code.
         *
         * @see android.dolby.IDs#getBandCount(int[])
         */
        public int getBandCount(int[] count)
        {
            DsLog.log1(TAG, "IDs.getBandCount");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (count != null)
                {
                    count[0] = DsAkSettings.getGeqBandCount();
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
         * Get the center frequencies of all frequency bands.
         *
         * @param frequencies The center frequencies of all frequency bands in an array.
         * @return The error code.
         *
         * @see android.dolby.IDs#getBandFrequencies(int[])
         */
        public int getBandFrequencies(int[] frequencies)
        {
            DsLog.log1(TAG, "IDs.getBandFrequencies");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                int[] realFrequencies = DsAkSettings.getGeqBandFrequencies();
                System.arraycopy(realFrequencies, 0, frequencies, 0, realFrequencies.length);
                error = DsCommon.DS_NO_ERROR;
            }
            return error;
        }

        /**
         * Set the DS Audio Effect profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param index The index of the DS profile to be set.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setSelectedProfile(int)
         */
        public int setSelectedProfile(int handle, int profile)
        {
            DsLog.log1(TAG, "IDs.setSelectedProfile(" + profile + ")");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    boolean success = doSetSelectedProfile(handle, profile);
                    if (success)
                        error = DsCommon.DS_NO_ERROR;
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
         * @param profile The Index of the selected DS profile.
         * @return The error code.
         *
         * @see android.dolby.IDs#getSelectedProfile(int[])
         */
        public int getSelectedProfile(int[] profile)
        {
            DsLog.log1(TAG, "IDs.getSelectedProfile");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (profile != null)
                {
                    profile[0] = ds_.getSelectedProfile();
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
         * Set the settings of DS profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS profile.
         * @param settings The new settings from the client.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setProfileSettings(int, DsClientSettings)
         */
        public int setProfileSettings(int handle, int profile, DsClientSettings settings)
        {
            DsLog.log1(TAG, "IDs.setProfileSettings(" + profile + ")");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.setProfileSettings(profile, settings))
                    {
                        // Update the profile properties only if the profile is the current selected one.
                        if (profile == ds_.getSelectedProfile())
                        {
                            setProfileProperties(profile);
                        }
                       
                        // Send profile settings changed message
                        Message msg = new Message();
                        msg.what = DsCommon.PROFILE_SETTINGS_CHANGED_MSG;
                        msg.arg1 = handle;
                        msg.arg2 = profile;
                        mHandler.sendMessage(msg);
                        error = DsCommon.DS_NO_ERROR;
                    }

                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in setProfileSettings");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in setProfileSettings");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setProfileSettings");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the settings of DS profile.
         *
         * @param profile The index of the DS profile.
         * @param settings The settings of selected profile.
         * @return The error code.
         *
         * @see android.dolby.IDs#getProfileSettings(int, DsClientSettings[])
         */
        public int getProfileSettings(int profile, DsClientSettings[] settings)
        {
            DsLog.log1(TAG, "IDs.getProfileSettings(" + profile + ")");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (settings != null)
                {
                    try
                    {
                        DsClientSettings realSettings = ds_.getProfileSettings(profile);
                        settings[0] = realSettings;
                        error = DsCommon.DS_NO_ERROR;
                    }
                    catch (IllegalArgumentException e)
                    {
                        Log.e(TAG, "IllegalArgumentException in getProfileSettings");
                        e.printStackTrace();
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "Exception in getProfileSettings");
                        e.printStackTrace();
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
         * Reset the DS Audio Effect profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of DS Audio Effect profile.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#resetProfile(int)
         */
        public int resetProfile(int handle, int profile)
        {
            DsLog.log1(TAG, "IDs.resetProfile");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.resetProfile(profile))
                    {
                        // Update the profile properties only if the profile is the current selected one.
                        if (profile == ds_.getSelectedProfile())
                        {
                            setProfileProperties(profile);
                        }

                        // Send profile settings changed message
                        Message msg = new Message();
                        msg.what = DsCommon.PROFILE_SETTINGS_CHANGED_MSG;
                        msg.arg1 = handle;
                        msg.arg2 = profile;
                        mHandler.sendMessage(msg);

                        if (profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
                        {
                            // We also need to send a name changed message here
                            String[] names = ds_.getProfileNames();
                            Message msg2 = new Message();
                            msg2.what = DsCommon.PROFILE_NAME_CHANGED_MSG;
                            msg2.arg1 = handle;
                            msg2.arg2 = profile;
                            msg2.obj = new String(names[profile]);
                            mHandler.sendMessage(msg2);
                        }
                        notifyWidget();
                        error = DsCommon.DS_NO_ERROR;
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
                    error = DsCommon.DS_OPERATION_NOT_PERMITTED;
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
         * Set the name of DS Audio Effect profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS Audio Effect profile.
         * @param name The new name of the profile.
         * @return The error code.
         *
         * @see android.dolby.IDs#setProfileName(int, String)
         */
        public int setProfileName(int handle, int profile, String name)
        {
            DsLog.log1(TAG, "IDs.setProfileName");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.setProfileName(profile, name))
                    {
                        // Send profile name changed message
                        Message msg = new Message();
                        msg.what = DsCommon.PROFILE_NAME_CHANGED_MSG;
                        msg.arg1 = handle;
                        msg.arg2 = profile;
                        msg.obj = new String(name);
                        mHandler.sendMessage(msg);
                        notifyWidget();
                        error = DsCommon.DS_NO_ERROR;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in setProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (UnsupportedOperationException e)
                {
                    Log.e(TAG, "UnsupportedOperationException in setProfileName");
                    e.printStackTrace();
                    error = DsCommon.DS_OPERATION_NOT_PERMITTED;
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
         * Get the version of DS1AK library.
         *
         * @param version DsAp version.
         * @return The error code.
         *
         * @see android.dolby.IDs#getDsApVersion(String[])
         */
        public int getDsApVersion(String[] version)
        {
            DsLog.log1(TAG, "IDs.getDsApVersion");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (version != null)
                    {
                        version[0] = ds_.getDsApVersion();
                        error = DsCommon.DS_NO_ERROR;
                    }
                    else
                    {
                        error = DsCommon.DS_INVALID_ARGUMENT;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in getDsApVersion");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in getDsApVersion");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get the version of DS Android integration.
         *
         * @param version DS version.
         * @return The error code.
         *
         * @see android.dolby.IDs#getDsVersion(String)
         */
        public int getDsVersion(String[] version)
        {
            DsLog.log1(TAG, "IDs.getDsVersion");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (version != null)
                {
                    version[0] = ds_.getDsVersion();
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
         * Get the Mono Speaker state.
         *
         * @param Is Mono Speaker or not.
         * @return The error code.
         *
         * @see android.dolby.IDs#getMonoSpeaker(boolean[])
         */
        public int getMonoSpeaker(boolean[] isMonoSpeaker)
        {
            DsLog.log1(TAG, "IDs.getMonoSpeaker");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            if (isMonoSpeaker != null)
            {
                String  monoSpeaker = SystemProperties.get(PROP_MONO_SPEAKER, "false");
                if(monoSpeaker.equals("true"))
                {
                    isMonoSpeaker[0] = true;
                }
                else
                {
                    isMonoSpeaker[0] = false;
                }
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }

            return error;
        }

        /**
         * Set a new intelligent equalizer preset for the specified DS profile.
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS Audio Effect profile.
         * @param preset The index of the preset.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setIeqPreset(int, int)
         */
        public int setIeqPreset(int handle, int profile, int preset)
        {
            DsLog.log1(TAG, "IDs.setIeqPreset");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.setIeqPreset(profile, preset))
                    {
                        // Send Eq settings changed message.
                        Message msg = new Message();
                        msg.what = DsCommon.EQ_SETTINGS_CHANGED_MSG;
                        msg.arg1 = handle;
                        msg.arg2 = (profile & 0x00ff << 16) | preset;
                        mHandler.sendMessage(msg);
                        error = DsCommon.DS_NO_ERROR;
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
         * @param profile The index of the DS Audio Effect profile.
         * @param preset The Index of active intelligent equalizer preset.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#getIeqPreset(int, int[])
         */
        public int getIeqPreset(int profile, int[] preset)
        {
            DsLog.log1(TAG, "IDs.getIeqPreset");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (preset != null)
                    {
                        preset[0] =  ds_.getIeqPreset(profile);
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
        * Get the specified profile settings modified value if it is different from
        * the factory default settings.
        *
        * @param profile The index of the DS Audio Effect profile.
        * @param modifiedValue Value of modified profile settings from factory defaults.
        *
        * @return The error code.
        *
        * @see android.dolby.IDs#getProfileModified(int, int[])
        */
        public int getProfileModified(int profile, int[] modifiedValue)
        {
            DsLog.log1(TAG, "IDs.getProfileModified");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (modifiedValue != null)
                {
                    modifiedValue[0] = ds_.getProfileModified(profile);
                    DsLog.log1(TAG, "IDs.getProfileModified " + modifiedValue[0]);
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
         * Sets the graphic equalizer band gains for the specified IEQ preset within the specified profile.
         * The onEqSettingsChanged() event will be called on other clients.
         * The number of bands that need to be provided can be determined by calling getBandCount().
         *
         * @param handle The handle of the client who calls this method.
         * @param profile The index of the DS Audio Effect profile.
         * @param preset The index of the Ieq preset.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setGeq(int, int, float[])
         */
        public int setGeq(int handle, int profile, int preset, float[] geqBandGains)
        {
            DsLog.log1(TAG, "IDs.setGeq");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.setGeq(profile, preset, geqBandGains))
                    {
                        // Send Eq settings changed message.
                        Message msg = new Message();
                        msg.what = DsCommon.EQ_SETTINGS_CHANGED_MSG;
                        msg.arg1 = handle;
                        msg.arg2 = (profile & 0x00ff << 16) | preset;
                        mHandler.sendMessage(msg);
                        error = DsCommon.DS_NO_ERROR;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in setGeq");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in setGeq");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setGeq");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Gets the graphic equalizer band gains for the specified IEQ preset within the specified profile.
         *
         * @param profile The index of the DS Audio Effect profile.
         * @param preset The index of the Ieq preset.
         * @param The band gains of graphic equalizer.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#getGeq(int, int, float[])
         */
        public int getGeq(int profile, int preset, float[] gains)
        {
            DsLog.log1(TAG, "IDs.getGeq");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    float[] realGains = ds_.getGeq(profile, preset);
                    System.arraycopy(realGains, 0, gains, 0, realGains.length);
                    error = DsCommon.DS_NO_ERROR;
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(TAG, "IllegalArgumentException in getGeq");
                    e.printStackTrace();
                    error = DsCommon.DS_INVALID_ARGUMENT;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in getGeq");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Set a ds audio processing parameter directly, and the new setting therefore applies to the current profile.
         * The callback onDsApParamChanged() will be called on the registered clients that are interested in the change.
         *
         * @param handle The handle of the client who calls this method.
         * @param parameter  The parameter name.
         * @param values The parameter values.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#setDsApParam(String, int[])
         */
        public int setDsApParam(int handle, String parameter, int[] values)
        {
            DsLog.log1(TAG, "IDs.setDsApParam");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                try
                {
                    if (ds_.setDsApParam(parameter, values))
                    {
                        Message msg = new Message();
                        if (ds_.isBasicProfileSettings(parameter))
                        {
                            // Send profile settings changed message
                            msg.what = DsCommon.PROFILE_SETTINGS_CHANGED_MSG;
                        }
                        else
                        {
                            // Send ds audio processing parameter changed message.
                            msg.what = DsCommon.DS_PARAM_CHANGED_MSG;
                            msg.obj = new String(parameter);
                        }
                        msg.arg1 = handle;
                        msg.arg2 = ds_.getSelectedProfile();
                        mHandler.sendMessage(msg);
                        error = DsCommon.DS_NO_ERROR;
                    }
                }
                catch (DeadObjectException e)
                {
                    Log.e(TAG, "DeadObjectException in setDsApParam");
                    e.printStackTrace();
                    error = DsCommon.DS_NOT_RUNNING;
                }
                catch (UnsupportedOperationException e)
                {
                    Log.e(TAG, "UnsupportedOperationException in setDsApParam");
                    e.printStackTrace();
                    error = DsCommon.DS_OPERATION_NOT_PERMITTED;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception in setDsApParam");
                    e.printStackTrace();
                }
            }
            return error;
        }

        /**
         * Get a parameter directly from the audio processing instance.
         * Hence the parameter value retrieved relates to the currently selected profile.
         *
         * @param parameter The parameter name.
         * @param value The values currently adopted by the specified parameter.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#getDsApParam(String, int[])
         */
        public int getDsApParam(String parameter, int[] values)
        {
            DsLog.log1(TAG, "IDs.getDsApParam");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                int[] realParam = ds_.getDsApParam(parameter);
                System.arraycopy(realParam, 0, values, 0, realParam.length);
                if (values != null)
                {
                    error = DsCommon.DS_NO_ERROR;
                }
            }
            return error;
        }

        /**
         * Get the array length of an AK parameter.
         *
         * @param parameter The parameter name.
         * @param len The length of the array of specified parameter.
         *
         * @return The error code.
         *
         * @see android.dolby.IDs#getDsApParamLength(String, int[])
         */
        public int getDsApParamLength(String parameter, int[] len)
        {
            DsLog.log1(TAG, "IDs.getDsApParamLength");
            int error = DsCommon.DS_UNKNOWN_ERROR;

            synchronized (lockDolbyContext_)
            {
                if (len != null)
                {
                    len[0] = ds_.getDsApParamLength(parameter);
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
         * Register a ds client handle to keep it informed of the ds audio processing parameter change event.
         *
         * @param handle The handle of the client.
         *
         * @see android.dolby.IDs#registerDsApParamEvents(int)
         */
        public void registerDsApParamEvents(int handle)
        {
            synchronized (lockCallbacks_)
            {
                dsApParamEventList_.add(new Integer(handle));
                DsLog.log1(TAG, "registerDsApParamEvents: Add a client handle " + handle);
            }
        }

        /**
         * Unregister a ds client handle to leave it uninformed of the ds audio processing parameter change event.
         *
         * @param handle The handle of the client.
         *
         * @see android.dolby.IDs#unregisterDsApParamEvents(int)
         */
        public void unregisterDsApParamEvents(int handle)
        {
            synchronized (lockCallbacks_)
            {
                int size = dsApParamEventList_.size();
                if (size == 0)
                {
                    DsLog.log1(TAG, "unregisterDsApParamEvents: No client handle registered, do nothing.");
                    return;
                }
                for (Integer hdl : dsApParamEventList_)
                {
                    if (handle == hdl.intValue())
                    {
                        dsApParamEventList_.remove(hdl);
                        DsLog.log1(TAG, "unregisterDsApParamEvents: remove a client handle "+ handle);
                        break;
                    }
                }
            }
        }

        /**
         * Register a callback function to keep the applications informed of the effect on/off/configuration change.
         *
         * @param cb The client register the callback.
         * @param handle The handle of the client.
         *
         * @see android.dolby.IDs#registerCallback(IDsServiceCallbacks)
         */
        public void registerCallback(IDsServiceCallbacks cb, int handle)
        {
            if (cb != null)
            {
                synchronized (lockCallbacks_)
                {
                    callbacks_.register(cb, handle);
                    DsLog.log1(TAG, "the register handle is "+ handle);
                }
            }
        }

        /**
         * Unregister a callback function that has been registered to keep the applications informed of the effect
         * on/off/configuration change.
         *
         * @param cb The client unregister the callback.
         *
         * @see android.dolby.IDs#unregisterCallback(IDsServiceCallbacks)
         */
        public void unregisterCallback(IDsServiceCallbacks cb)
        {
            if (cb != null)
            {
                synchronized (lockDolbyContext_)
                {
                    synchronized (lockCallbacks_)
                    {
                        callbacks_.unregister(cb);
                        
                        if (!nonPersistentMode_)
                        {
                            ds_.saveDsStateAndSettings();
                        }
                     
                        DsLog.log1(TAG, "unregisterCallback");
                    }
                }
            }
        }

        /**
         * Register a notification of visualizer data updated.
         *
         * @param handle The handle of the client registers the visualizer data.
         *
         * @see android.dolby.IDs#registerVisualizerData(int)
         */
        public void registerVisualizerData(int handle)
        {
            synchronized (lockDolbyContext_)
            {
                synchronized (lockCallbacks_)
                {
                    int size = visualizerList_.size();
                    if (size == 0)
                    {
                        // The fisrt visualizer client is registering, enable the visualizer.
                        startVisualizer();
                    }
                    visualizerList_.add(new Integer(handle));
                    // Notify the newly-registered client that visualizer is already suspended
                    if (isVisualizerSuspended_)
                    {
                        Message msg = new Message();
                        msg.what = DsCommon.VISUALIZER_SUSPENDED_MSG;
                        mHandler.sendMessage(msg);
                    }
                    DsLog.log1(TAG, "Add a visualzier handle "+ handle);
                }
            }
        }

        /**
         * Unregister a notification of visualizer data updated.
         *
         * @param handle The handle of the client unregisters the visualizer data.
         * @return The error code.
         *
         * @see android.dolby.IDs#unregisterVisualizerData(int)
         */
        public void unregisterVisualizerData(int handle)
        {
            synchronized (lockDolbyContext_)
            {
                synchronized (lockCallbacks_)
                {
                    int size = visualizerList_.size();
                    if (size == 0)
                    {
                        Log.e(TAG, "No client registering, do nothing.");
                        return;
                    }
                    for (Integer hdl : visualizerList_)
                    {
                        if (handle == hdl.intValue())
                        {
                            visualizerList_.remove(hdl);
                            DsLog.log1(TAG, "remove a visualzier handle "+ handle);
                            int newSize = visualizerList_.size();
                            if(newSize == 0)
                            {
                                // The last visualizer client is unregistering, disable the visualizer.
                                stopVisualizer();
                            }
                            break;
                        }
                    }
                }
            }
        }
    };

    /**
     * Our Handler used to execute operations on the main thread.  This is used
     * to schedule increments of our value.
     *
     * @internal
     */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            synchronized (lockDolbyContext_)
            {
                switch (msg.what)
                {
                    // Broadcast the DS audio effect on/off state change if any
                    case DsCommon.DS_STATUS_CHANGED_MSG:
                    {
                        DsLog.log2(TAG, "handling the DS_STATUS_CHANGES_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            boolean isEffectOn = (msg.arg2 == INT_ON)? true : false;
                            final int N = callbacks_.beginBroadcast();
                            for (int i = 0; i < N; i++)
                            {
                                try
                                {
                                    int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                    if (j != setter_handle)
                                    {
                                        callbacks_.getBroadcastItem(i).onDsOn(isEffectOn);
                                    }
                                }
                                catch (RemoteException e)
                                {
                                    // The RemoteCallbackList will take care of removing
                                    // the dead object for us.
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the DS profile change if any
                    case DsCommon.PROFILE_SELECTED_MSG:
                    {
                        DsLog.log2(TAG, "handling the PROFILE_SELECTED_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            int profile = msg.arg2;
                            final int N = callbacks_.beginBroadcast();
                            for (int i = 0; i < N; i++)
                            {
                                try
                                {
                                    int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                    if (j != setter_handle)
                                    {
                                        callbacks_.getBroadcastItem(i).onProfileSelected(profile);
                                    }
                                }
                                catch (RemoteException e)
                                {
                                    // The RemoteCallbackList will take care of removing
                                    // the dead object for us.
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the DS profile settings change if any
                    case DsCommon.PROFILE_SETTINGS_CHANGED_MSG:
                    {
                        DsLog.log2(TAG, "handling the PROFILE_SETTINGS_CHANGED_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            int profile = msg.arg2;
                            final int N = callbacks_.beginBroadcast();
                            for (int i = 0; i < N; i++)
                            {
                                try
                                {
                                    int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                    if (j != setter_handle)
                                    {
                                        callbacks_.getBroadcastItem(i).onProfileSettingsChanged(profile);
                                    }
                                }
                                catch (RemoteException e)
                                {
                                    // The RemoteCallbackList will take care of removing
                                    // the dead object for us.
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the DS profile name change if any
                    case DsCommon.PROFILE_NAME_CHANGED_MSG:
                    {
                        DsLog.log2(TAG, "handling the PROFILE_NAME_CHANGED_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            int profile = msg.arg2;
                            String name = (String)msg.obj;
                            final int N = callbacks_.beginBroadcast();
                            for (int i = 0; i < N; i++)
                            {
                                try
                                {
                                    int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                    if (j != setter_handle)
                                    {
                                        callbacks_.getBroadcastItem(i).onProfileNameChanged(profile, name);
                                    }
                                }
                                catch (RemoteException e)
                                {
                                    // The RemoteCallbackList will take care of removing
                                    // the dead object for us.
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the visualizer update message if any
                    case DsCommon.VISUALIZER_UPDATED_MSG:
                    {
                        DsLog.log2(TAG, "handling the VISUALIZER_UPDATED_MSG message...");
                        synchronized (lockCallbacks_)
                        {
                            if (!isVisualizerSuspended_)
                            {
                                final int N = callbacks_.beginBroadcast();
                                for (Integer hdl : visualizerList_)
                                {
                                    int handle = hdl.intValue();
                                    for (int i = 0; i < N; i++)
                                    {
                                        try
                                        {
                                            int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                            if (j == handle)
                                            {
                                               callbacks_.getBroadcastItem(i).onVisualizerUpdated(gains_, excitations_);
                                            }
                                        }
                                        catch (RemoteException e)
                                        {
                                            // The RemoteCallbackList will take care of removing
                                            // the dead object for us.
                                        }
                                    }
                                }
                                callbacks_.finishBroadcast();
                            }
                        }
                        break;
                    }
                    // Broadcast the visualizer suspended message if any
                    case DsCommon.VISUALIZER_SUSPENDED_MSG:
                    {
                        DsLog.log2(TAG, "handling the VISUALIZER_SUSPENDED_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            final int N = callbacks_.beginBroadcast();
                            for (Integer hdl : visualizerList_)
                            {
                                int handle = hdl.intValue();
                                for (int i = 0; i < N; i++)
                                {
                                    try
                                    {
                                        int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                        if (j == handle)
                                        {
                                            callbacks_.getBroadcastItem(i).onVisualizerSuspended(isVisualizerSuspended_);
                                        }
                                    }
                                    catch (RemoteException e)
                                    {
                                        // The RemoteCallbackList will take care of removing
                                        // the dead object for us.
                                    }
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the Eq settings change if any
                    case DsCommon.EQ_SETTINGS_CHANGED_MSG:
                    {
                        DsLog.log2(TAG, "handling the EQ_SETTINGS_CHANGED_MSG message...");

                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            int profile = msg.arg2 & 0xff00;
                            int preset = msg.arg2 & 0x00ff;
                            final int N = callbacks_.beginBroadcast();
                            for (int i = 0; i < N; i++)
                            {
                                try
                                {
                                    int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                    if (j != setter_handle)
                                    {
                                        callbacks_.getBroadcastItem(i).onEqSettingsChanged(profile, preset);
                                    }
                                }
                                catch (RemoteException e)
                                {
                                    // The RemoteCallbackList will take care of removing
                                    // the dead object for us.
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // Broadcast the ds audio processing change message if any
                    case DsCommon.DS_PARAM_CHANGED_MSG:
                    {
                        DsLog.log2(TAG, "handling the DS_PARAM_CHANGED_MSG message...");
                        synchronized (lockCallbacks_)
                        {
                            int setter_handle = msg.arg1;
                            int profile = msg.arg2;
                            String paramName = (String)msg.obj;
                            final int N = callbacks_.beginBroadcast();
                            for (Integer hdl : dsApParamEventList_)
                            {
                                int handle = hdl.intValue();
                                for (int i = 0; i < N; i++)
                                {
                                    try
                                    {
                                        int j = ((Integer)callbacks_.getBroadcastCookie(i)).intValue();
                                        if (j == handle && j != setter_handle)
                                        {
                                            callbacks_.getBroadcastItem(i).onDsApParamChange(profile, paramName);
                                        }
                                    }
                                    catch (RemoteException e)
                                    {
                                        // The RemoteCallbackList will take care of removing
                                        // the dead object for us.
                                    }
                                }
                            }
                            callbacks_.finishBroadcast();
                        }
                        break;
                    }
                    // add other cases here
                    default:
                        super.handleMessage(msg);
                }
            }
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
            boolean on = ds_.getDsOn();
            return doSetDsOn(handle, !on);
        }
    }
    
    private int doSetDsOn(int handle, boolean on) throws DeadObjectException
    {
        synchronized (lockDolbyContext_)
        {
            if (isDsEffectSuspended && (on == true))
            {
                DsLog.log1(TAG, "DS_REQUEST_FAILED_EFFECT_SUSPENDED");
                return DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED;
            }

            ds_.setDsOn(on);
            
            // Update the system property to match the current state
            boolean newStatus = ds_.getDsOn();
            String curState = (newStatus == true) ? STATE_ON : STATE_OFF;
            SystemProperties.set(PROP_DS_STATE, curState);

            // Notify all clients and widgets of the change
            Message msg = new Message();
            msg.what = DsCommon.DS_STATUS_CHANGED_MSG;
            msg.arg1 = handle;
            msg.arg2 = (newStatus == true) ? INT_ON : INT_OFF;
            mHandler.sendMessage(msg);
            // Refresh the widget
            notifyWidget();

            int size = 0;
            // Update the visualizer state
            synchronized (lockCallbacks_)
            {
                size = visualizerList_.size();
            }
            if (size > 0)
            {
                if (newStatus)
                {
                    // Turn on the visualizer if necessary
                    startVisualizer();
                }
                else
                {
                    // Turn off the visualizer if necessary
                    stopVisualizer();
                    // In stopVisualizer gains and excitations are already set to zeros
                    // Send the last all zeros message
                    Message msg2 = new Message();
                    msg2.what = DsCommon.VISUALIZER_UPDATED_MSG;
                    mHandler.sendMessage(msg2);
                }
            }
        }

        return DsCommon.DS_NO_ERROR;
    }
    
    private boolean doSetSelectedProfile(int handle, int profile) throws DeadObjectException
    {
        synchronized (lockDolbyContext_)
        {
            boolean success = ds_.setSelectedProfile(profile);
            int newProfile = ds_.getSelectedProfile();
            
            if (success && profile == newProfile)
            {
                // Set system property
                setProfileProperties(newProfile);

                // Send profile selected message
                Message msg = new Message();
                msg.what = DsCommon.PROFILE_SELECTED_MSG;
                msg.arg1 = handle;
                msg.arg2 = profile;
                mHandler.sendMessage(msg);
                // Refresh the widget
                notifyWidget();
            }
            
            return success && profile == newProfile;
        }
    }
    
}

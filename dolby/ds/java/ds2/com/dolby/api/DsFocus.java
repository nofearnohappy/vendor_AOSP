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

/**
 * @defgroup    ds DsFocus
 * @details     Main API for controlling DS by DsFocus access right.
 *
 *              This API can be used by media activities to control the
 *              DS audio effect integrated on the device.
 *              The implementation uses inter-process communication to interact
 *              with the DS service running on the device.
 * @{
 */

package com.dolby.api;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.os.Looper;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * API for controlling DS.
 *
 * This API can be used by media activities to control the
 * DS audio effect integrated on the device.
 * The implementation uses inter-process communication to interact
 * with the DS service running on the device.
 *
 */
public class DsFocus extends DsBase
{
    private static final String TAG = "DsFocus";

    /**
     * The listener for the DS events callbacks.
     *
     */
    protected IDsEvents dsListener_ = null;

    /**
     * The listener for the visualizer data.
     *
     */
    protected IDsVisualizerEvents visualizerListener_ = null;

    /**
     * The listener for the profile name changed events.
     *
     */
    protected IDsProfileNameEvents profileNameListener_ = null;

    /**
     * The listener for the legacy client setting events.
     *
     */
    protected IDsLegacyClientEvents legacyClientListener_ = null;

    /**
     * Band count of visualizer data.
     *
     */
    // TODO: If the band count can be changed, update it
    protected int bandCount_ = 20;

    /**
     * Gains of visualizer data.
     *
     */
    protected float[] gains_ = null;

    /**
     * Excitations of visualzier data.
     *
     */
    protected float[] excitations_ = null;

    /**
     * The default constructor.
     *
     */
    public DsFocus()
    {
        super.setConnectionInfo(DsAccess.ACCESS_FOCUS);
    }

    /**
     * Register for the visualizer function.
     * The first client calling this method will turn on the visualizer timer automatically.
     *
     * @param listener The object interests in the event.
     *
     */
    public void registerVisualizer(IDsVisualizerEvents listener)
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
                    
        try
        {
            if (bandCount_ == 0)
            {
                Log.e(TAG, "graphic equalizer band count NOT initialized yet.");
                throw new RuntimeException("Exception in registerVisualizer");
            }
            else if (listener != null)
            {
                if(visualizerListener_ == null)
                {
                    if (gains_ == null)
                        gains_ = new float[bandCount_];
                    if (excitations_ == null)
                        excitations_ = new float[bandCount_];
                
                    iDs_.iRegisterVisualizerData(clientHandle_);
                    visualizerListener_ = listener;
                }
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }
        }
        catch(Exception e)
        {
            handleException(e, "registerVisualizer");
        }
        convertErrorCodeToException(error);
    }

    /**
     * Unregister from the visualizer function.
     * The last client calling this method will turn off the visualizer timer automatically.
     *
     */
    public void unregisterVisualizer()
    {
        try
        {
            iDs_.iUnregisterVisualizerData(clientHandle_);
            visualizerListener_ = null;
            gains_ = null;
            excitations_ = null;
        }
        catch(Exception e)
        {
            handleException(e, "unregisterVisualizer");
        }
    }

    /**
     * Register for the DS Events.
     *
     * @param listener The object interests in the event.
     *
     */
    public void registerDsEvents(IDsEvents listener)
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
                    
        try
        {
            if (listener != null)
            {
                iDs_.iRegisterCallback(clientHandle_, callbacks_, DsCommon.DS_CLIENT_VER_TWO);
                dsListener_ = listener;
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }
        }
        catch(Exception e)
        {
            handleException(e, "registerDsEvents");
        }
        convertErrorCodeToException(error);
    }

    /**
     * Unregister from the DS Events.
     *
     */
    public void unregisterDsEvents()
    {
        try
        {
            iDs_.iUnregisterCallback(clientHandle_, callbacks_, DsCommon.DS_CLIENT_VER_TWO);
            dsListener_ = null;
        }
        catch(Exception e)
        {
            handleException(e, "unregisterDsEvents");
        }
    }

    /**
     * Turn on or off DS audio processing.
     *
     * This method may fail to set the state of DS.
     * As such, the caller should verify the return code of the function before
     * making any updates to the UI.
     *
     * @param on The new on/off state of DS.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED - The DS state cannot be changed
     *         due to the OS suspending the effect.
     */
    public int setState(boolean on) throws DsAccessException
    {
        // Default return value if nothing is acheived.
        int error = DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED;
        try
        {
            error = iDs_.iSetState(clientHandle_, 0, on);
        }
        catch(Exception e)
        {
            handleException(e, "setState");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Query if DS audio processing is turned on.
     *
     * @return The current state of DS.
     */
    public int getState()
    {
        int ret_val = -1;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetState(0, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getState");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * Set the values of a specific audio processing parameter for the current selected profile.
     *
     * @param paramId The id of the parameter.
     * @param values The values of the parameter.
     *
     * @throws DsAccessException if there is an access right required..
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int setParameter(int paramId, int[] values) throws DsAccessException
    {
        int error = 0;
        try
        {
            int[] paramInt = new int[1];
            error = iDs_.iGetProfile(0, paramInt);
            if (error == DsConstants.DS_NO_ERROR)
            {
                int profile = paramInt[0];
                error = iDs_.iSetParameter(clientHandle_, 0, profile, paramId, values);
            }
        }
        catch(Exception e)
        {
            handleException(e, "setParameter");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Get the values of a specific audio processing parameter for the current selected profile.
     *
     * @param paramId The id of the parameter.
     *
     * @return The values of the parameter .
     */
    public int[] getParameter(int paramId)
    {
        int[] ret_vals = null;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetParamLength(paramId, paramInt);
            if(error == DsConstants.DS_NO_ERROR)
            {
                int[] profileInt = new int[1];
                error = iDs_.iGetProfile(0, profileInt);
                if (error == DsConstants.DS_NO_ERROR)
                {
                    int profile = profileInt[0];
                    ret_vals = new int[paramInt[0]];
                    error = iDs_.iGetParameter(0, profile, paramId, ret_vals);
                }
            }
        }
        catch(Exception e)
        {
            handleException(e, "getParameter");
        }
        convertErrorCodeToException(error);
        return ret_vals;
    }

    /**
     * Query the number of DS intelligent equalizer preset.
     *
     * @return The total number of supported intelligent equalizer presets.
     */
    public int getIeqPresetCount()
    {
        int ret_val = 0;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetIeqPresetCount(0, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getIeqPresetCount");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * Set a new intelligent equalizer preset.
     *
     * @param preset The new preset of intelligent equalizer.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int setIeqPreset(int preset) throws DsAccessException
    {
        int error = 0;
        try
        {
            error = iDs_.iSetIeqPreset(clientHandle_, 0, preset);
        }
        catch(Exception e)
        {
            handleException(e, "setIeqPreset");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Get the active intelligent equalizer preset.
     *
     * @return The index of active intelligent equalizer preset.
     */
    public int getIeqPreset()
    {
        int ret_val = 0;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetIeqPreset(0, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getIeqPreset");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * Query the number of DS profile.
     *
     * @return The total number of profile.
     */
    public int getProfileCount()
    {
        int ret_val = 0;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetProfileCount(0, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getProfileCount");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * Set a new profile.
     *
     * @param profile The new index of profile.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int setProfile(int profile) throws DsAccessException
    {
        int error = 0;
        try
        {
            error = iDs_.iSetProfile(clientHandle_, 0, profile);
        }
        catch(Exception e)
        {
            handleException(e, "setProfile");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Get the selected profile index.
     *
     * @return The index of selected profile.
     */
    public int getProfile()
    {
        int ret_val = 0;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetProfile(0, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getProfile");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * Query whether the specified profile settings have been modified from the factory default settings.
     *
     * @param profile The index of the profile to be queried.
     *
     * @return True if the profile setting is modified, false otherwise.
     */
    public boolean isProfileSettingsModified(int profile)
    {
        boolean ret_val = false;
        boolean[] paramBoolean = new boolean[1];
        int error = 0;
        try
        {
            error = iDs_.iGetProfileModified( 0, profile, paramBoolean);
        }
        catch(Exception e)
        {
            handleException(e, "isProfileSettingsModified");
        }
        convertErrorCodeToException(error);
        ret_val = paramBoolean[0];
        return ret_val;
    }

    /**
     * Resets the specified DS profile to default settings.
     *
     * @param profile The index of the profile to be reset.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int resetProfile(int profile) throws DsAccessException
    {
        int error = 0;
        try
        {
            error = iDs_.iResetProfile(clientHandle_, 0, profile);
        }
        catch(Exception e)
        {
            handleException(e, "setProfile");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Queries whether the internal speaker is mono.
     *
     * @return True if the device uses a mono speaker, false otherwise.
     */
    public boolean isMonoSpeaker()
    {
        boolean ret_val = false;
        boolean[] paramBoolean = new boolean[1];
        int error = 0;
        try
        {
            error = iDs_.iGetMonoSpeaker(paramBoolean);
        }
        catch(Exception e)
        {
            handleException(e, "isMonoSpeaker");
        }
        convertErrorCodeToException(error);
        ret_val = paramBoolean[0];
        return ret_val;
    }

    /**
     * Request DsFocus access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int requestAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.requestAccessRight(DsAccess.ACCESS_FOCUS);
        }
        catch(Exception e)
        {
            handleException(e, "requestAccessRight");
        }
        return error;
    }

    /**
     * Abandon DsFocus access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int abandonAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.abandonAccessRight(DsAccess.ACCESS_FOCUS);
        }
        catch(Exception e)
        {
            handleException(e, "abandonAccessRight");
        }
        return error;
    }

    /**
     * Query if DsFocus access right already hold by another clients.
     *
     * @return The state of the DsFocus access right.
     */
    public int checkAccessRight()
    {
        int ret_val = -1;
        try
        {
            ret_val = super.checkAccessRight(DsAccess.ACCESS_FOCUS);
        }
        catch(Exception e)
        {
            handleException(e, "checkAccessRight");
        }
        return ret_val;
    }

    /**
     * Get the access right current client can request.
     *
     * @return The type of the access right current client can request.
     */
    public int getAvailableAccessRight()
    {
        return DsAccess.ACCESS_FOCUS;
    }

    /**
     * Set the information about access right used by the client.
     *
     */
    protected void setConnectionInfo(int access)
    {
        super.connectionBridge_ = access;
    }

    /**
     * Unbinds from the DsService.
     * After this call, no DS methods should be called on this object except for registerClient.
     * This method must be called when an application no-longer needs to communicate with the DsService
     * or make any further DS method calls.
     * Service will continue to run in the background.
     * All settings changes done by App will be lost after unregister.
     */
    public void unregisterClient()
    {
        DsLog.log1(TAG, "unregisterClient");

        if(iDs_ != null)
        {
            try
            {
                iDs_.iUnregisterVisualizerData(clientHandle_);
                visualizerListener_ = null;
                gains_ = null;
                excitations_ = null;
                iDs_.iUnregisterCallback(clientHandle_, callbacks_, DsCommon.DS_CLIENT_VER_TWO);
                dsListener_ = null;
            }
            catch(Exception e)
            {
                handleException(e, "unregisterClient");
            }
        }
        super.unregisterClient();
    }

    /**
     * The events which will trigger the callbacks.
     *
     */
    protected IDsCallbacks callbacks_ = new IDsCallbacks.Stub() {
        /**
         * This is called by the remote service to tell us about new effect on/off state if any change.
         * Note that IPC calls are dispatched through a thread pool running in each process, so the code
         * executing here will NOT be running in our main thread like most other things -- so, to update
         * the UI, we need to use a Handler to hop over there.
         */
        public void onDsOn(boolean on)
        {
            DsLog.log2(TAG, "event onDsOn()");
            int status = on ? 1: 0;
            handler_.sendMessage(handler_.obtainMessage(DsCommon.DS_STATUS_CHANGED_MSG, status, 0));
        }

        public void onDsSuspended(boolean isSuspended)
        {
            DsLog.log2(TAG, "event onDsSuspended()");
            int status = isSuspended ? 1: 0;
            handler_.sendMessage(handler_.obtainMessage(DsCommon.DS_STATUS_SUSPENDED_MSG, status, 0));
        }

        public void onProfileSelected(int profile)
        {
            DsLog.log2(TAG, "event onProfileSelected()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_SELECTED_MSG, profile, 0));
        }

        public void onProfileSettingsChanged(int profile)
        {
            DsLog.log2(TAG, "event onProfileSettingsChanged()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, profile, 0));
        }

        public void onVisualizerUpdated(float[] gains, float[]excitations)
        {
            DsLog.log3(TAG, "event onVisualizerUpdated()");

            System.arraycopy(gains, 0, gains_, 0, bandCount_);
            System.arraycopy(excitations, 0, excitations_, 0, bandCount_);
            handler_.sendMessage(handler_.obtainMessage(DsCommon.VISUALIZER_UPDATED_MSG, 0, 0));
        }

        public void onVisualizerSuspended(boolean isSuspended)
        {
            DsLog.log2(TAG, "event onVisualizerSuspended()");
            int status = isSuspended ? 1 : 0;
            handler_.sendMessage(handler_.obtainMessage(DsCommon.VISUALIZER_SUSPENDED_MSG, status, 0));
        }

        public void onAccessForceReleased(String app, int type)
        {
            DsLog.log2(TAG, "event onAccessForceReleased()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.ACCESS_RELEASED_MSG, type, 0, app));
        }

        public void onAccessAvailable()
        {
            DsLog.log2(TAG, "event onAccessAvailable()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.ACCESS_AVAILABLE_MSG, 0, 0));
        }

        //This callback method is a synchronized call
        public boolean onAccessRequested(String app, int type)
        {
            DsLog.log2(TAG, "event onAccessRequested()");
            boolean value = false;
            if(accessListener_ != null)
            {
                value = accessListener_.onAccessRequested(app, type);
            }
            return value;
        }

        public void onProfileNameChanged(int profile, String name)
        {
            DsLog.log2(TAG, "event onProfileNameChanged()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_NAME_CHANGED_MSG, profile, 0, name));
        }

        //This callback method is a synchronized call
        public boolean onLegacyClientSetting()
        {
            DsLog.log2(TAG, "event onLegacyClientSetting()");
            boolean value = false;
			if(legacyClientListener_ != null)
			{
            	value = legacyClientListener_.onLegacyClientSetting();
			}
            return value;
        }
    };

    /**
     * The handler to update the GUI defined by Activities.
     *
     */
    protected Handler handler_ = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DsCommon.DS_STATUS_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(DS_STATUS_CHANGED_MSG): isOn = " + msg.arg1);

                    // we can directly call the cbk in the main thread
                    if (dsListener_ != null)
                    {
                        boolean isOn = (msg.arg1 == 0) ? false : true;
                        dsListener_.onDsOn(isOn);
                    }
                    break;

                case DsCommon.PROFILE_SELECTED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_SELECTED_MSG): profile = " + msg.arg1);

                    if (dsListener_ != null)
                    {
                        dsListener_.onProfileSelected(msg.arg1);
                    }
                    break;

                case DsCommon.PROFILE_SETTINGS_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_SETTINGS_CHANGED_MSG): profile = " + msg.arg1);

                    if (dsListener_ != null)
                    {
                        dsListener_.onProfileSettingsChanged(msg.arg1);
                    }
                    break;

                case DsCommon.DS_STATUS_SUSPENDED_MSG:
                    DsLog.log1(TAG, "handleMessage(DS_STATUS_SUSPENDED_MSG): profile = " + msg.arg1);

                    if (dsListener_ != null)
                    {
                        boolean isSuspended = (msg.arg1 == 0) ? false : true;
                        dsListener_.onDsSuspended(isSuspended);
                    }
                    break;

                case DsCommon.VISUALIZER_UPDATED_MSG:
                    DsLog.log3(TAG, "handleMessage(VISUALIZER_UPDATED_MSG):");

                    if (visualizerListener_ != null)
                    {
                        visualizerListener_.onVisualizerUpdate(excitations_, gains_);
                    }
                    break;

                case DsCommon.VISUALIZER_SUSPENDED_MSG:
                    DsLog.log2(TAG, "handleMessage(VISUALIZER_SUSPENDED_MSG): isSuspended = " + msg.arg1);

                    if (visualizerListener_ != null)
                    {
                        boolean isSuspended = (msg.arg1 == 0) ? false : true;
                        visualizerListener_.onVisualizerSuspended(isSuspended);
                    }
                    break;

                case DsCommon.ACCESS_RELEASED_MSG:
                    DsLog.log1(TAG, "handleMessage(ACCESS_RELEASED_MSG): app = " + msg.obj + " type = " + msg.arg1);

                    if (accessListener_ != null)
                    {
                        accessListener_.onAccessForceReleased((String)msg.obj, msg.arg1);
                    }
                    break;

                case DsCommon.ACCESS_AVAILABLE_MSG:
                    DsLog.log1(TAG, "handleMessage(ACCESS_AVAILABLE_MSG)");

                    if (accessListener_ != null)
                    {
                        accessListener_.onAccessAvailable();
                    }
                    break;
                    
                case DsCommon.PROFILE_NAME_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_NAME_CHANGED_MSG): profile = " + msg.arg1 + " name =" + msg.obj);

                    if (profileNameListener_!= null)
                    {
                        profileNameListener_.onProfileNameChanged(msg.arg1, (String)msg.obj);
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    };
}

/**
 * @}
 */

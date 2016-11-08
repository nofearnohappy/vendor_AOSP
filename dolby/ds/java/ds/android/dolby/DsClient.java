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

/**
 * @defgroup    dsclient DS Client
 * @details     Main client API for controlling DS.
 *
 *              This client API can be used by media activities to control the
 *              global DS audio effect integrated on the device.
 *              The implementation uses inter-process communication to interact
 *              with the DS service running on the device.
 * @{
 */

package android.dolby;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;
import android.os.Looper;

import java.lang.UnsupportedOperationException;
import java.util.Iterator;

import com.dolby.api.IDs;
import com.dolby.api.DsLog;
import com.dolby.api.DsCommon;
import com.dolby.api.DsConstants;
import com.dolby.api.IDsCallbacks;
import com.dolby.api.DsParams;
import com.dolby.api.IDsDeathHandler;
import android.dolby.DsClientSettings;
import android.dolby.IDsApParamEvents;
import android.dolby.IDsVisualizerEvents;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * API for controlling DS.
 *
 * This client API can be used by media activities to control the
 * DS global audio effect integrated on the device.
 * The implementation uses inter-process communication to interact
 * with the DS service running on the device.
 *
 * DS provides a number of profiles and intelligent equalizer
 * presets which are identified by index in the API.
 *
 * The DsConstants class defines the indices of these profiles and presets.
 */
public class DsClient
{
    private static final String TAG = "DsClient";

    /**
     * The interface for controlling the DsService.
     *
     *  @internal
     */
    private IDs ds_ = null;

    /**
     * The listener for the callbacks.
     *
     *  @internal
     */
    private IDsClientEvents activityListener_ = null;

    /**
     * The object used for synchronizing among different instances.
     *
     *  @internal
     */
    private static Object lock_ = new Object();

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
     * The basic profile parameters of the class DsClientSettings.
     */
    static private final String profileParams_[] = {"geon", "deon", "dvle", "vdhe", "vspe"};

    /**
     * Translate the error code to an exception.
     * The Android IDL interface (AIDL) does not propagate exceptions
     * between the service and the caller, so we need to provide our
     * own mechanism to transport them.
     *
     *  @internal
     */
    private void translateErrorCodeToExceptions (int errorCode) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RuntimeException
    {
        // Negative error codes indicate exceptions
        // All other error codes indicate success or other feedback to the application.
        if (errorCode >= 0)
            return;

        // Determine which exception was thrown
        switch (errorCode)
        {
            case DsCommon.DS_INVALID_ARGUMENT:
                throw new IllegalArgumentException();
            case DsCommon.DS_NOT_RUNNING:
                throw new DeadObjectException();
            case DsCommon.DS_NOT_INITIALIZED:
                throw new IllegalStateException();
            case DsCommon.DS_SETTING_NOT_PERMITTED:
                throw new UnsupportedOperationException();
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Turn on or off DS audio processing.
     * No event will be called on the calling client.
     *
     * The caller may update the UI display immediately with the specified on/off state.
     *
     * @deprecated This method is deprecated. Please use setDsOnChecked().
     *
     * @param on The new on/off state of DS.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set new status failed.
     */
    public void setDsOn(boolean on) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.iSetState(connection_.hashCode(), 0, on);
                    translateErrorCodeToExceptions(error);
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setDsOn");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setDsOn");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setDsOn");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setDsOn");
                }
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in setDsOn");
        }
    }

    /**
     * Turn on or off DS audio processing.
     * No event will be called on the calling client.
     *
     * This method may fail to set the state of DS.
     * As such, the caller should verify the return code of the function before
     * making any updates to the UI.
     *
     * @param on The new on/off state of DS.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED - The DS state cannot be changed
     *         due to the OS suspending the effect.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set new status failed.
     */
    public int setDsOnChecked(boolean on) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        // Default return value if nothing is acheived.
        int error = DsConstants.DS_REQUEST_FAILED_EFFECT_SUSPENDED;

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    error = ds_.iSetState(connection_.hashCode(), 0, on);
                    translateErrorCodeToExceptions(error);
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setDsOnChecked");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setDsOnChecked");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setDsOnChecked");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setDsOnChecked");
                }
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in setDsOnChecked");
        }
        return error;
    }

    /**
     * Query if DS audio processing is turned on.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get current status failed.
     *
     * @return The current state of DS.
     */
    public boolean getDsOn() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        boolean value = false;

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.iGetState(0, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = (paramInt[0] == 1) ? true : false;
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getDsOn");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getDsOn");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getDsOn");
                e.printStackTrace();
                throw new RuntimeException("Exception in getDsOn");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getDsOn");
        }
        return value;
    }

    /**
     * Query the number of DS profiles.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get profile count failed.
     *
     * @return The total number of supported profiles.
     */
    public int getProfileCount() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int value = 0;
        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.iGetProfileCount(0, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = paramInt[0];
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getProfileCount");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getProfileCount");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getProfileCount");
                e.printStackTrace();
                throw new RuntimeException("Exception in getProfileCount");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getProfileCount");
        }
        return value;
    }

    /**
     * Get the names of all DS profiles.
     *
     * @return The array of profile names.
     */
    public String[] getProfileNames()
    {
        String[] names = new String[DsConstants.PROFILES_NUMBER];

        for (int i = 0; i < DsConstants.PROFILES_NUMBER; i++)
        {
            names[i] = DsCommon.PROFILE_NAMES[i];
        }
        return names;
    }

    /**
     * Get the index of the selected DS profile.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get current profile failed.
     *
     * @return The index of the currently selected profile.
     */
     public int getSelectedProfile() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int value = 0;

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.iGetProfile(0, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = paramInt[0];
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getSelectedProfile");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getSelectedProfile");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getSelectedProfile");
                e.printStackTrace();
                throw new RuntimeException("Exception in getSelectedProfile");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getSelectedProfile");
        }
        return value;
    }

    /**
     * Select a DS profile.
     * No event will be called on the calling client.
     * The caller may update the UI display immediately with the specified profile index.
     * The caller can retrieve the profile settings for the specified profile index by calling
     * getProfileSettings() and getIeqPreset().
     *
     * @param profile The index of the profile to be selected.
     *
     * @throws IllegalArgumentException if profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set profile failed.
     */
    public void setSelectedProfile(int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.iSetProfile(connection_.hashCode(), 0, profile);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setSelectedProfile");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setSelectedProfile");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setSelectedProfile");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setSelectedProfile");
                }
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in setSelectedProfile");
        }
    }

    /**
     * Set the settings of the specified DS profile.
     * No event will be called on the calling client.
     * The caller may update the UI display immediately with the specified settings.
     *
     * Note that the DsClientSettings instance contains graphic equalizer gains. These
     * gains will be applied to the currently-selected intelligent equalizer preset for the
     * specified profile. The current preset can be determined by calling getIeqPreset().
     * Each intelligent equalizer preset in a profile contains independent graphic equalizer
     * gains.
     *
     * @param profile The index of the profile whose settings are being changed.
     * @param settings The new settings for the specified profile.
     *
     * @throws IllegalArgumentException if settings is null or profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set profile settings failed.
     */
    public void setProfileSettings(int profile, DsClientSettings settings) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }
        if (settings == null)
        {
            throw new IllegalArgumentException("invalid settings");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int[] value = new int[1];
                    int paramId = 0;
                    DsParams dsParam;

                    for(String param: profileParams_)
                    {
                        dsParam = DsParams.FromString(param);
                        paramId = dsParam.toInt();

                        if(param.equals("geon"))
                        {
                            value[0] = (settings.getGeqOn()) ? INT_ON : INT_OFF;
                        }
                        else if(param.equals("deon"))
                        {
                            value[0] = (settings.getDialogEnhancerOn()) ? INT_ON : INT_OFF;
                        }
                        else if(param.equals("dvle"))
                        {
                            value[0] = (settings.getVolumeLevellerOn()) ? INT_ON : INT_OFF;
                        }
                        else if(param.equals("vdhe"))
                        {
                            value[0] = (settings.getHeadphoneVirtualizerOn()) ? INT_ON : INT_OFF;
                        }
                        else if(param.equals("vspe"))
                        {
                            value[0] = (settings.getSpeakerVirtualizerOn()) ? INT_ON : INT_OFF;
                        }

                        int error = ds_.iSetParameter(connection_.hashCode(), 0, profile, paramId, value);
                        if (error != DsCommon.DS_NO_ERROR)
                        {
                            translateErrorCodeToExceptions(error);
                        }
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setProfileSettings");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setProfileSettings");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setProfileSettings");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setProfileSettings");
                }
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in setProfileSettings");
        }
    }

    /**
     * Get the settings of the specified DS profile.
     *
     * The graphic equalizer gains within the DsClientSettings instance returned represent
     * the gains for the currently-selected intelligent equalizer preset for the specified
     * profile. The current preset can be determined by calling getIeqPreset().
     *
     * @param profile The index of the profile whose settings are being retrieved.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get profile settings failed.
     *
     * @return The settings of the specified profile.
     */
    public DsClientSettings getProfileSettings(int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        DsClientSettings settings = null;
        int error;

        if (ds_ != null)
        {
            try
            {
                DsClientSettings clientSettings = new DsClientSettings();
                int paramId = 0;
                int[] paramInt = new int[1];
                boolean[] value = new boolean[5];
                int index = 0;
                DsParams dsParam;

                for(String param: profileParams_)
                {
                    dsParam = DsParams.FromString(param);
                    paramId = dsParam.toInt();

                    error = ds_.iGetParameter(0, profile, paramId, paramInt);
                    if (error != DsConstants.DS_NO_ERROR)
                      {
                          translateErrorCodeToExceptions(error);
                    }
                    value[index] = (paramInt[0] == 1) ? true : false;
                    index++;
                }

                clientSettings.setGeqOn(value[0]);
                clientSettings.setDialogEnhancerOn(value[1]);
                clientSettings.setVolumeLevellerOn(value[2]);
                clientSettings.setHeadphoneVirtualizerOn(value[3]);
                clientSettings.setSpeakerVirtualizerOn(value[4]);

                settings = clientSettings;
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getProfileSettings");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getProfileSettings");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getProfileSetting");
                e.printStackTrace();
                throw new RuntimeException("Exception in getProfileSettings");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getProfileSettings");
        }
        return settings;
    }

    /**
     * Resets the specified DS profile to default settings.
     * No event will be called on the calling client.
     * The caller should retrieve the new settings by calling getProfileSettings(), getIeqPreset()
     *      and getProfileNames() methods in order to update the UI display.
     *
     * @param profile The index of the profile to be reset.
     *
     * @throws IllegalArgumentException if profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if reset profile failed.
     */
    public void resetProfile(int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.iResetProfile(connection_.hashCode(), 0, profile);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in resetProfile");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in resetProfile");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in resetProfile");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in resetProfile");
                }
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in resetProfile");
        }
    }

    /**
     * Get the version of the underlying DS audio processing library.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get version failed.
     *
     * @return The version string of the DS audio processing library.
     */
    public String getDsApVersion() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        String version = "";

        if (ds_ != null)
        {
            try
            {
                String[] paramString = new String[1];
                int error = ds_.iGetDapLibraryVersion(paramString);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    version = paramString[0];
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getDsApVersion");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getDsApVersion");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getDsApVersion");
                e.printStackTrace();
                throw new RuntimeException("Exception in getDsApVersion");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getDsApVersion");
        }
        return version;
    }

    /**
     * Get the version of the DS Android integration.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get version failed.
     *
     * @return The version string of the DS Android integration.
     */
    public String getDsVersion() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        String version = "";

        if (ds_ != null)
        {
            try
            {
                String[] paramString = new String[1];
                int error = ds_.iGetDsServiceVersion(paramString);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    version = paramString[0];
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getDsVersion");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getDsVersion");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getDsVersion");
                e.printStackTrace();
                throw new RuntimeException("Exception in getDsVersion");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getDsVersion");
        }
        return version;
    }

    /**
     * This method is for internal use.
     *
     * @param param  Internal use.
     * @param values Internal use.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get current status failed.
     */
    public void setDsApParam(String param, int[] values) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (ds_ != null)
        {
            try
            {
                DsParams dsParam = DsParams.FromString(param);
                if (dsParam == null)
                {
                    throw new IllegalArgumentException();
                }
                int paramId = dsParam.toInt();
                int[] paramInt = new int[1];
                int error = ds_.iGetProfile(0, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    int profile = paramInt[0];
                    // The parameter is always applied to the current selected profile.
                    error = ds_.iSetParameter(connection_.hashCode(), 0, profile, paramId, values);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in setDsApParam");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in setDsApParam");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in setDsApParam");
                e.printStackTrace();
                throw new RuntimeException("Exception in setDsApParam");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in setDsApParam");
        }
    }

    /**
     * This method is for internal use.
     *
     * @param param Internal use.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get current status failed.
     *
     * @return Internal use.
     */
    public int[] getDsApParam(String param) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException,RuntimeException
    {
        int[] values = null;

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                DsParams dsParam = DsParams.FromString(param);
                if (dsParam == null)
                {
                    throw new IllegalArgumentException();
                }
                int paramId = dsParam.toInt();

                int error = ds_.iGetParamLength(paramId, paramInt);
                if (error == DsCommon.DS_NO_ERROR)
                {
                    int[] profileInt = new int[1];
                    error = ds_.iGetProfile(0, profileInt);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                    else
                    {
                        int profile = profileInt[0];
                        values = new int[paramInt[0]];
                        // The parameter is always retrieved from the current selected profile.
                        error = ds_.iGetParameter(0, profile, paramId, values);
                        if (error != DsCommon.DS_NO_ERROR)
                        {
                            translateErrorCodeToExceptions(error);
                        }
                    }
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getDsApParam");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getDsApParam");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getDsApParam");
                e.printStackTrace();
                throw new RuntimeException("Exception in getDsApParam");
            }
        }
        else
        {
            throw new NullPointerException("NullPointerException in getDsApParam");
        }
        return values;
    }

    /**
     * Register the event listener coming from the activities.
     *
     * @param listener The object interested in the events.
     */
    public void setEventListener(IDsClientEvents listener)
    {
        if (listener != null)
        {
            activityListener_ = listener;
        }
    }

    /**
     * Binds to the DS Service so that DsClient API calls can be made on this object.
     * This method must be called before calling other DsClient methods.
     *
     * @param context The object binds to the DS service.
     *
     * @return True if you have successfully bound to the service, false otherwise.
     */
    public boolean bindDsService(Context context)
    {
        DsLog.log1(TAG, "bindDsService()");
        Intent bindIntent = new Intent(IDs.class.getName());

        // Establish a couple connections with the service, binding by interface names. This allows other applications to be
        // installed that replace the remote service by implementing the same interface.
        return context.bindService(bindIntent, connection_, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbinds from the DS Service.
     * After this call, no DsClient methods should be called on this object except for bindDsService.
     * This method must be called when an application no-longer needs to communicate with the DS Service
     * or make any further DsClient method calls.
     * Service will continue to run in the background.
     *
     * @param context The object unbinds from the DS service.
     */
    public void unBindDsService(Context context)
    {
        DsLog.log1(TAG, "unBindDsService()");

        if (ds_ != null)
        {
            try
            {
                ds_.iUnregisterCallback(connection_.hashCode(), callbacks_, DsCommon.DS_CLIENT_VER_ONE);
                ds_.iUnregisterDeathHandler(this.hashCode(), deathHandler_);
            }
            catch (RemoteException e)
            {
                Log.e(TAG, "Remote Exception in unBindFromRemoteRunningService");
            }
            // Detach our existing connection.
            context.unbindService(connection_);
            ds_ = null;
        }
    }

    /* Following are the methods not implemented in DS2 */
    public void setNonPersistentMode(boolean on) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
           UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in setNonPersistentMode");
    }
    public int getBandCount() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
           UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in getBandCount");
    }
    public int[] getBandFrequencies() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in getBandFrequencies");
    }
    public void setProfileName(int profile, String name) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in setProfileName");
    }
    public boolean isMonoSpeaker() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in isMonoSpeaker");
    }
    public void setIeqPreset(int profile, int preset) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in setIeqPreset");
    }
    public int getIeqPreset(int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in getIeqPreset");
    }
    public boolean isProfileModified (int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in isProfileModified");
    }
    public boolean isProfileNameModified (int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in isProfileNameModified");
    }
    public void setGeq(int profile, int preset, float[] geqBandGains) throws IllegalArgumentException, DeadObjectException,
           IllegalStateException, UnsupportedOperationException, RemoteException, NullPointerException,
           RuntimeException
    {
        throw new RuntimeException("Exception in setGeq");
    }
    public float[] getGeq(int profile, int preset) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in getGeq");
    }
    public void registerDsApParamEvents(IDsApParamEvents listener) throws RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in registerDsApParamEvents");
    }
    public void unregisterDsApParamEvents() throws RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in unregisterDsApParamEvents");
    }
    public void registerVisualizer(IDsVisualizerEvents listener) throws RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in registerVisualizer");
    }
    public void unregisterVisualizer() throws RemoteException, NullPointerException, RuntimeException
    {
        throw new RuntimeException("Exception in unregisterVisualizer");
    }
    static public float getGeqBandGainLowerBound()
    {
        throw new RuntimeException("Exception in getGeqBandGainLowerBound");
    }
    static public float getGeqBandGainUpperBound()
    {
        throw new RuntimeException("Exception in getGeqBandGainUpperBound");
    }

    /**
     * The connection to control the service.
     *
     *  @internal
     */
    private ServiceConnection connection_ = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            DsLog.log1(TAG, "ServiceConnection.onServiceConnected()");

            ds_ = IDs.Stub.asInterface(service);

            try
            {
                ds_.iRegisterDeathHandler(this.hashCode(), deathHandler_);
                ds_.iRegisterCallback(this.hashCode(), callbacks_, DsCommon.DS_CLIENT_VER_ONE);
                DsLog.log1(TAG, "ServiceConnection.onServiceConnected()");
            }
            catch (RemoteException e)
            {
                Log.e(TAG, "onServiceConnected failed");
            }

            if (activityListener_ != null)
            {
                activityListener_.onClientConnected();
            }

            DsLog.log3(TAG, "CONNECTED: DSService");
        }

        public void onServiceDisconnected(ComponentName className)
        {
            DsLog.log1(TAG, "ServiceConnection.onServiceDisconnected()");

            if (activityListener_ != null)
            {
                activityListener_.onClientDisconnected();
            }

            ds_ = null;

            DsLog.log3(TAG, "/ServiceConnection.onServiceDisconnected()");
        }
    };

    /**
     * The events which will trigger the callbacks.
     *
     *  @internal
     */
    private IDsCallbacks callbacks_ = new IDsCallbacks.Stub() {
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

        public void onProfileSelected(int profile)
        {
            DsLog.log2(TAG, "event onProfileSelected()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_SELECTED_MSG, profile, 0));
        }

        public void onDsSuspended(boolean isSuspended)
        {
            // not implement
        }

        public void onProfileSettingsChanged(int profile)
        {
            // not implement
        }

        public void onVisualizerUpdated(float[] gains, float[] excitations)
        {
            // not implement
        }

        public void onVisualizerSuspended(boolean isSuspended)
        {
            // not implement
        }

        public void onAccessForceReleased(String app, int type)
        {
            // not implement
        }

        public void onAccessAvailable()
        {
            // not implement
        }

        public boolean onAccessRequested(String app, int type)
        {
            // not implement
            return true;
        }
        
        public void onProfileNameChanged(int profile, String name)
        {
            //not implement
        }
    };

    /**
     * The deathHandler_ to make DsService listen the death of DS client.
     *
     *  @internal
     */
    private IDsDeathHandler deathHandler_ = new IDsDeathHandler.Stub()
    {
        public void onClientDied()
        {
            // not implement
        } 
    };

    /**
     * The handler to update the GUI defined by Activities.
     *
     *  @internal
     */
    private Handler handler_ = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DsCommon.DS_STATUS_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(DS_STATUS_CHANGED_MSG): isOn = " + msg.arg1);
                    boolean isOn = (msg.arg1 == 0) ? false : true;

                    // we can directly call the cbk in the main thread
                    if (activityListener_ != null)
                    {
                        activityListener_.onDsOn(isOn);
                    }
                    break;

                case DsCommon.PROFILE_SELECTED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_SELECTED_MSG): profile = " + msg.arg1);

                    if (activityListener_ != null)
                    {
                        activityListener_.onProfileSelected(msg.arg1);
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

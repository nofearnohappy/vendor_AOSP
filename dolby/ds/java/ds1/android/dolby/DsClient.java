/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
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
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.lang.UnsupportedOperationException;

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
     * The listener for the visualizer data.
     *
     *  @internal
     */
    private IDsVisualizerEvents visualizerListener_ = null;

    /**
     * The listener for the audio processing parameter change.
     * @internal
     */
    private IDsApParamEvents dsApParamChangeListener_ = null;

    /**
     * The object used for synchronizing among different instances.
     *
     *  @internal
     */
    private static Object lock_ = new Object();

    /**
     * Band count of visualizer data.
     *
     *  @internal
     */
    private int bandCount_ = 0;

    /**
     * Gains of visualizer data.
     *
     *  @internal
     */
    private float[] gains_ = null;

    /**
     * Excitations of visualzier data.
     *
     *  @internal
     */
    private float[] excitations_ = null;

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
            case DsCommon.DS_INVALID_STATE:
                throw new IllegalStateException();
            case DsCommon.DS_OPERATION_NOT_PERMITTED:
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
                    int error = ds_.setDsOn(connection_.hashCode(), on);
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
                    error = ds_.setDsOn(connection_.hashCode(), on);
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
                boolean[] paramBoolean = new boolean[1];
                int error = ds_.getDsOn(paramBoolean);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = paramBoolean[0];
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
        return value;
    }

    /**
     * This method is for internal use.
     *
     * @param on Internal use.
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
    public void setNonPersistentMode(boolean on) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int error = 0;

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    error = ds_.setNonPersistentMode(on);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setNonPersistentMode");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setNonPersistentMode");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setNonPersistentMode");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setNonPersistentMode");
                }
            }
        }
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
                int error = ds_.getProfileCount(paramInt);
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
        return value;
    }

    /**
     * Get the names of all DS profiles.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get profile names failed.
     *
     * @return The array of profile names.
     */
    public String[] getProfileNames() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        String[] names = null;

        if (ds_ != null)
        {
            try
            {
                names = new String[getProfileCount()];
                int error = ds_.getProfileNames(names);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getProfileNames");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getProfileNames");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getProfileNames");
                e.printStackTrace();
                throw new RuntimeException("Exception in getProfileNames");
            }
        }
        return names;
    }

    /**
     * Retrieve the number of frequency bands.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if getting Geq band count failed.
     *
     * @return The number of frequency bands.
     */
    public int getBandCount() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int value = 0;
        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.getBandCount(paramInt);
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
                Log.e(TAG, "RemoteException in getBandCount");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getBandCount");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getBandCount");
                e.printStackTrace();
                throw new RuntimeException("Exception in getBandCount");
            }
        }
        return value;
    }

    /**
     * Retrieve the center frequencies of all frequency bands.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if getting Geq band frequencies failed.
     *
     * @return The center frequencies of all frequency bands.
     */
    public int[] getBandFrequencies() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int[] bandFrequencies = null;
        if (ds_ != null)
        {
            try
            {
                bandFrequencies = new int[getBandCount()];
                int error = ds_.getBandFrequencies(bandFrequencies);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getBandFrequencies");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getBandFrequencies");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getBandFrequencies");
                e.printStackTrace();
                throw new RuntimeException("Exception in getBandFrequencies");
            }
        }
        return bandFrequencies;
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
                    int error = ds_.setSelectedProfile(connection_.hashCode(), profile);
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
                int error = ds_.getSelectedProfile(paramInt);
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
        return value;
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
                    int error = ds_.setProfileSettings(connection_.hashCode(), profile, settings);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
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

        if (ds_ != null)
        {
            try
            {
                DsClientSettings[] paramSettings = new DsClientSettings[1];
                int error = ds_.getProfileSettings(profile, paramSettings);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                settings = paramSettings[0];
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
                    int error = ds_.resetProfile(connection_.hashCode(), profile);
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
    }

    /**
     * Set a new name for the specified DS profile.
     * Only user profiles may have their name changed.
     * The names of the built-in profiles (Movie, Music, Game and Voice) cannot be changed.
     * No event will be called on the calling client.
     * The caller may update the UI display immediately with the specified profile name.
     *
     * @param profile The index of the profile whose name is being changed.
     * @param name The new name of the specified profile
     *
     * @throws IllegalArgumentException if profile is not in the range or name is null.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set profile name failed.
     */
    public void setProfileName(int profile, String name) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }
        if (name == null)
        {
            throw new IllegalArgumentException("invalid name");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.setProfileName(connection_.hashCode(), profile, name);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setProfileName");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setProfileName");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setProfileName");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setProfileName");
                }
            }
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
                int error = ds_.getDsApVersion(paramString);
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
                int error = ds_.getDsVersion(paramString);
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
        return version;
    }

    /**
     * Queries whether the internal speaker is mono.
     *
     * @throws IllegalArgumentException if the argument is invalid.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get mono speaker state fail.
     *
     * @return True if the device uses a mono speaker, false otherwise.
     */
    public boolean isMonoSpeaker() throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        boolean ret_val = false;

        if (ds_ != null)
        {
            try
            {
                boolean[] paramBoolean = new boolean[1];
                int error = ds_.getMonoSpeaker(paramBoolean);

                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    ret_val = paramBoolean[0];
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in isMonoSpeaker");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException isMonoSpeaker");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in isMonoSpeaker");
                e.printStackTrace();
                throw new RuntimeException("Exception in isMonoSpeaker");
            }
        }
        return ret_val;

    }

    /**
     * Set a new intelligent equalizer preset for the specified DS profile.
     * The onEqSettingsChanged() event will be called on other clients.
     * No event will be called on the calling client.
     * The caller should retrieve the new settings by calling getProfileSettings()
     *      and getIeqPreset() methods in order to update the UI display.
     *
     * @param profile The index of the profile.
     * @param preset The new preset of intelligent equalizer.
     *
     * @throws IllegalArgumentException if profile and preset are not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set preset failed.
     */
    public void setIeqPreset(int profile, int preset) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }
        if (preset < DsConstants.IEQ_PRESET_INDEX_MIN || preset > DsConstants.IEQ_PRESET_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid preset");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.setIeqPreset(connection_.hashCode(), profile, preset);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setIeqPreset");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setIeqPreset");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setIeqPreset");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setIeqPreset");
                }
            }
        }
    }

    /**
     * Get the active intelligent equalizer preset.
     *
     * @param profile The index of the specified profile.
     *
     * @throws IllegalArgumentException if profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get preset failed.
     *
     * @return The index of active intelligent equalizer preset.
     */
    public int getIeqPreset(int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        int value = 0;

        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.getIeqPreset(profile, paramInt);
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
                Log.e(TAG, "RemoteException in getIeqPreset");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getIeqPreset");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getIeqPreset");
                e.printStackTrace();
                throw new RuntimeException("Exception in getIeqPreset");
            }
        }
        return value;
    }

    /**
     * Query whether the specified profile settings have been modified from the factory default settings.
     *
     * @param profile The index of the profile to be queried.
     *
     * @throws IllegalArgumentException if profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get profile modified status failed.
     *
     * @return True if the profile settings have been modified from factory defaults, false otherwise.
     */
    public boolean isProfileModified (int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        boolean value = false;

        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.getProfileModified(profile, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = ((paramInt[0] & DsCommon.DS_PROFILE_SETTINGS_MODIFIED) == DsCommon.DS_PROFILE_SETTINGS_MODIFIED);
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in isProfileModified");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException isProfileModified");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in isProfileModified");
                e.printStackTrace();
                throw new RuntimeException("Exception in isProfileModified");
            }
        }
        return value;
    }

    /**
     * Query whether the specified profile name has been modified from the factory default settings.
     *
     * @param profile The index of the profile to be queried.
     *
     * @throws IllegalArgumentException if profile is not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get profile modified status failed.
     *
     * @return True if the profile name has been modified from factory defaults, false otherwise.
     */
    public boolean isProfileNameModified (int profile) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        boolean value = false;

        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }

        if (ds_ != null)
        {
            try
            {
                int[] paramInt = new int[1];
                int error = ds_.getProfileModified(profile, paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
                else
                {
                    value = ((paramInt[0] & DsCommon.DS_PROFILE_NAME_MODIFIED) == DsCommon.DS_PROFILE_NAME_MODIFIED);
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in isProfileNameModified");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException isProfileNameModified");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in isProfileNameModified");
                e.printStackTrace();
                throw new RuntimeException("Exception in isProfileNameModified");
            }
        }
        return value;
    }

    /**
     * Sets the graphic equalizer band gains for the specified IEQ preset within the specified profile.
     * The onEqSettingsChanged() event will be called on other clients.
     * The number of bands that need to be provided can be determined by calling getBandCount().
     * No event will be called on the calling client.
     *
     * @param profile The index of the profile.
     * @param preset The preset of intelligent equalizer.
     * @param geqBandGains The new graphic equalizer band gains in dB.
     *
     * @throws IllegalArgumentException if profile and preset are not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if set band gains failed.
     */
    public void setGeq(int profile, int preset, float[] geqBandGains) throws IllegalArgumentException, DeadObjectException,
           IllegalStateException, UnsupportedOperationException, RemoteException, NullPointerException,
           RuntimeException
    {
        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }
        if (preset < DsConstants.IEQ_PRESET_INDEX_MIN || preset > DsConstants.IEQ_PRESET_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid preset");
        }

        if (ds_ != null)
        {
            synchronized(lock_)
            {
                try
                {
                    int error = ds_.setGeq(connection_.hashCode(), profile, preset, geqBandGains);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
                    }
                }
                catch(RemoteException e)
                {
                    Log.e(TAG, "RemoteException in setGeq");
                    throw e;
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NullPointerException in setGeq");
                    e.printStackTrace();
                    throw e;
                }
                catch(Exception e)
                {
                    Log.e(TAG, e.toString() + " in setGeq");
                    e.printStackTrace();
                    throw new RuntimeException("Exception in setGeq");
                }
            }
        }
    }

    /**
     * Gets the graphic equalizer band gains for the specified IEQ preset within the specified profile.
     *
     * @param profile The index of the specified profile.
     * @param preset The preset of intelligent equalizer.
     *
     * @throws IllegalArgumentException if profile and preset are not in the range.
     * @throws DeadObjectException if an error has occurred in the service,
     *         which usually indicates that DS is not available.
     * @throws IllegalStateException if DS is in invalid state.
     * @throws UnsupportedOperationException if the operation is not allowed.
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if get band gains failed.
     *
     * @return The band gains, in dB, for specified Ieq preset of a DS profile.
     */
    public float[] getGeq(int profile, int preset) throws IllegalArgumentException, DeadObjectException, IllegalStateException,
            UnsupportedOperationException, RemoteException, NullPointerException, RuntimeException
    {
        float[] value = null;

        if (profile < DsConstants.PROFILE_INDEX_MIN || profile > DsConstants.PROFILE_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid profile");
        }
        if (preset < DsConstants.IEQ_PRESET_INDEX_MIN || preset > DsConstants.IEQ_PRESET_INDEX_MAX)
        {
            throw new IllegalArgumentException("invalid preset");
        }

        if (ds_ != null)
        {
            try
            {
                value = new float[getBandCount()];
                int error = ds_.getGeq(profile, preset, value);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
                }
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in getGeq");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in getGeq");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in getGeq");
                e.printStackTrace();
                throw new RuntimeException("Exception in getGeq");
            }
        }
        return value;
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
                int error = ds_.setDsApParam(connection_.hashCode(), param, values);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    translateErrorCodeToExceptions(error);
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
                int error = ds_.getDsApParamLength(param, paramInt);
                if (error == DsCommon.DS_NO_ERROR)
                {
                    values = new int[paramInt[0]];
                    error = ds_.getDsApParam(param, values);
                    if (error != DsCommon.DS_NO_ERROR)
                    {
                        translateErrorCodeToExceptions(error);
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
        return values;
    }

    /**
     * This method is for internal use.
     *
     * @param listener Internal use.
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
    public void registerDsApParamEvents(IDsApParamEvents listener) throws RemoteException, NullPointerException, RuntimeException
    {
       	if (ds_ != null)
        {
            try
            {
                ds_.registerDsApParamEvents(connection_.hashCode());
                dsApParamChangeListener_ = listener;
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in registerDsApParamEvents");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in registerDsApParamEvents");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in registerDsApParamEvents");
                e.printStackTrace();
                throw new RuntimeException("Exception in registerDsApParamEvents");
            }
        }
        else
        {
            throw new RuntimeException("registerDsApParamEvents failed");
        }
    }

    /**
     * This method is for internal use.
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
    public void unregisterDsApParamEvents() throws RemoteException, NullPointerException, RuntimeException
    {
        if (ds_ != null)
        {
            try
            {
                ds_.unregisterDsApParamEvents(connection_.hashCode());
                dsApParamChangeListener_ = null;
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in unregisterDsApParamEvents");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in unregisterDsApParamEvents");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in unregisterDsApParamEvents");
                e.printStackTrace();
                throw new RuntimeException("Exception in unregisterDsApParamEvents");
            }
        }
        else
        {
            throw new RuntimeException("unregisterDsApParamEvents failed");
        }
    }

    /**
     * Register for the visualizer function.
     * The first client calling this method will turn on the visualizer automatically.
     *
     * @param listener The object interests in the event.
     *
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if register visualizer failed.
     */
    public void registerVisualizer(IDsVisualizerEvents listener) throws RemoteException, NullPointerException, RuntimeException
    {
       	if (ds_ != null)
        {
            try
            {
                if (bandCount_ == 0)
                {
                    Log.e(TAG, "graphic equalizer band count NOT initialized yet.");
                    throw new RuntimeException("Exception in registerVisualizer");
                }
                else
                {
                    if (gains_ == null)
                        gains_ = new float[bandCount_];
                    if (excitations_ == null)
                        excitations_ = new float[bandCount_];
                }
                ds_.registerVisualizerData(connection_.hashCode());
                visualizerListener_ = listener;
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in registerVisualizer");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in registerVisualizer");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in registerVisualizer");
                e.printStackTrace();
                throw new RuntimeException("Exception in registerVisualizer");
            }
        }
        else
        {
            throw new RuntimeException("registerVisualizer failed");
        }
    }

    /**
     * Unregister from the visualizer function.
     * The last client calling this method will turn off the visualizer automatically.
     *
     * @throws RemoteException if binder remote-invocation errors.
     * @throws NullPointerException if null pointer exception has occurred between the service and client.
     * @throws RuntimeException if unregister visualizer failed.
     */
    public void unregisterVisualizer() throws RemoteException, NullPointerException, RuntimeException
    {
        if (ds_ != null)
        {
            try
            {
                ds_.unregisterVisualizerData(connection_.hashCode());
                visualizerListener_ = null;
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "RemoteException in unregisterVisualizer");
                throw e;
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "NullPointerException in unregisterVisualizer");
                e.printStackTrace();
                throw e;
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + " in unregisterVisualizer");
                e.printStackTrace();
                throw new RuntimeException("Exception in unregisterVisualizer");
            }
        }
        else
        {
            throw new RuntimeException("unregisterVisualizer failed");
        }
    }

    /**
     * Get the lower bound of the geq band gains.
     *
     * @return The lower bound of the geq band gains in dB.
     */
    static public float getGeqBandGainLowerBound()
    {
        return DsConstants.GEQ_BAND_GAIN_RANGE[0];
    }

    /**
     * Get the uppper bound of the geq band gains.
     *
     * @return The upper bound of the geq band gains in dB.
     */
    static public float getGeqBandGainUpperBound()
    {
        return DsConstants.GEQ_BAND_GAIN_RANGE[1];
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
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(bindIntent, 0);

        if (resolveInfos == null) {
            DsLog.log1(TAG, "bindDsService() resolveInfos=null");
            return false;
        }

        // Is somebody else trying to intercept our call?
        if (resolveInfos.size() != 1) {
            DsLog.log1(TAG, "bindDsService() resolveInfos.size() = " + resolveInfos.size());
            return false;
        }

        ResolveInfo serviceInfo = resolveInfos.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        bindIntent.setComponent(component);

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

        try
        {
            // Avoid missing calling unregister in the context.
            // If unregistering is done, nothing will happen.
            ds_.unregisterVisualizerData(connection_.hashCode());
            visualizerListener_ = null;
            ds_.unregisterDsApParamEvents(connection_.hashCode());
            dsApParamChangeListener_ = null;

            ds_.unregisterCallback(callbacks_);
        }
        catch (RemoteException e)
        {
             Log.e(TAG, "Remote Exception in unBindFromRemoteRunningService");
        }
        // Detach our existing connection.
        context.unbindService(connection_);
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
                ds_.registerCallback(callbacks_, this.hashCode());
                Log.i(TAG, "hash code of the connect is " + this.hashCode());
                int[] paramInt = new int[1];
                int error = ds_.getBandCount(paramInt);
                if (error != DsCommon.DS_NO_ERROR)
                {
                    Log.e(TAG, "Internal error in onServiceConnected");
                }
                else
                {
                    bandCount_ = paramInt[0];
                }
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
    private IDsServiceCallbacks callbacks_ = new IDsServiceCallbacks.Stub() {
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

        public void onProfileSettingsChanged(int profile)
        {
            DsLog.log2(TAG, "event onProfileSettingsChanged()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_SETTINGS_CHANGED_MSG, profile, 0));
        }

        public void onProfileNameChanged(int profile, String name)
        {
            DsLog.log2(TAG, "event onProfileNameChanged()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.PROFILE_NAME_CHANGED_MSG, profile, 0, name));
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

        public void onEqSettingsChanged(int profile, int preset)
        {
            DsLog.log2(TAG, "event onEqSettingsChanged()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.EQ_SETTINGS_CHANGED_MSG, profile, preset));
        }

        public void onDsApParamChange(int profile, String paramName)
        {
            DsLog.log2(TAG, "event onDsApParamChange()");
            handler_.sendMessage(handler_.obtainMessage(DsCommon.DS_PARAM_CHANGED_MSG, profile, 0, paramName));
        }
    };

    /**
     * The handler to update the GUI defined by Activities.
     *
     *  @internal
     */
    private Handler handler_ = new Handler()
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

                case DsCommon.PROFILE_SETTINGS_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_SETTINGS_CHANGED_MSG): profile = " + msg.arg1);

                    if (activityListener_ != null)
                    {
                        activityListener_.onProfileSettingsChanged(msg.arg1);
                    }
                    break;

                case DsCommon.PROFILE_NAME_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(PROFILE_NAME_CHANGED_MSG): profile = " + msg.arg1 + " name =" + msg.obj);

                    if (activityListener_ != null)
                    {
                        activityListener_.onProfileNameChanged(msg.arg1, (String)msg.obj);
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

                case DsCommon.EQ_SETTINGS_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(EQ_SETTINGS_CHANGED_MSG): profile = " + msg.arg1 + " preset =" + msg.arg2);

                    if (activityListener_ != null)
                    {
                        activityListener_.onEqSettingsChanged(msg.arg1, msg.arg2);
                    }
                    break;

                case DsCommon.DS_PARAM_CHANGED_MSG:
                    DsLog.log1(TAG, "handleMessage(DS_PARAM_CHANGED_MSG): profile " + msg.arg1 + ", parameter = " + msg.obj);

                    if (dsApParamChangeListener_ != null)
                    {
                        dsApParamChangeListener_.onDsApParamChange(msg.arg1, (String)msg.obj);
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

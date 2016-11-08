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

package android.dolby.ds;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.util.Log;

import android.os.DeadObjectException;

import android.media.audiofx.AudioEffect;
import android.dolby.DsClientSettings;
import android.dolby.DsConstants;
import android.dolby.DsLog;
import android.dolby.DsCommon;

public class Ds
{
    private static final String TAG = "Ds";

    private static final String DS_VERSION_EXTERNAL = DsVersion.DS_VERSION;
    private static final String DS_VERSION_INTERNAL = DsVersion.DS_VERSION + " [Build " + DsVersion.DS_VERSION_BUILD + "]";

    // Remove Ds from global effect chain when Ds is off
    private static final boolean useEffectReleaseForDsOff = true;

     // useOffProfileForDsOff must be false when useEffectReleaseForDsOff = true
    private static final boolean useOffProfileForDsOff = false;

    /** The current settings array for all the profiles.
     * @internal
     */
    private DsProfileSettings[] currentProfiles_ = new DsProfileSettings[DsConstants.PROFILES_NUMBER];

    /** The default settings array for all the profiles.
     * @internal
     */
    private DsProfileSettings[] defaultProfiles_ = new DsProfileSettings[DsConstants.PROFILES_NUMBER];

    /** The off settings for switching off the Ds effect.
     * @internal
     */
    private DsProfileSettings offProfile_;

    /** The profile index that is currently selected.
     * @internal
     */
    private int selectedProfile_ = 0;

    /** The current Ds effect on/off state.
     * @internal
     */
    private boolean isDsOn_ = true;

    /** The Ds effect instance.
     * @internal
     */
    private DsEffect dsEffect_ = null;

    /**
     * The audio session id this effect is applied to.
     * @internal
     */
    private int audioSessionId_;

    /**
     * The file name to store the current settings.
     */
    public static final String DS_CURRENT_FILENAME = "ds1-current.xml";

    /**
     * The file name to store the current state.
     */
    public static final String DS_STATE_FILENAME = "ds1-state.xml";

    /**
    * Creates the Ds Audio Effect for a specified session ID.
    *
    * @throws InstantiationException
    * @throws IllegalStateException
    */
    public Ds(int audioSessionId)  throws InstantiationException, IllegalStateException
    {
        Log.i(TAG, "Creating Ds effect on audioSessionId = " + audioSessionId);

        if (audioSessionId < 0)
        {
            Log.e(TAG, "Ds effect with specified session Id (" + audioSessionId + ") is less than zero");
        }
        else
        {
            // Get the default profile settings each Profile
            defaultProfiles_ = DsPresetsConfiguration.getDefaultSettings();

            // Get the current profile settings each Profile
            currentProfiles_ = DsPresetsConfiguration.getCurrentSettings();

            if (useOffProfileForDsOff)
            {
                // Get the default profile settings off Profile
                offProfile_ = DsPresetsConfiguration.getOffProfileSettings();
            }

            if (DsAkSettings.isConstantAkParamsDefined())
            {
                audioSessionId_ = audioSessionId;
                if (!useEffectReleaseForDsOff)
                {
                    dsEffect_ = new DsEffect(audioSessionId_);
                    setInitStatus(false);
                }
            }
            else
            {
                throw new InstantiationException("Constant AK parameters NOT defined yet.");
            }
        }
    }

    /**
     * Populate the DS settings and the store path for the latest DS settings and DS state.
     *
     * @param defaultInStream The input stream of the XML file containing the default settings.
     * @param dir The location to store the DS current settings and DS state XML files.
     * @return true if the settings are successfully populated, and false otherwise.
     */
    static public boolean populateSettings(InputStream defaultInStream, String dir)
    {
        boolean ret = true;
        DsLog.log2(TAG, "populateSettings");

        String dsCurSettingsPath = dir + "/" + DS_CURRENT_FILENAME;
        String dsStatePath = dir + "/" + DS_STATE_FILENAME;
        DsStoreUtil.storeDsPath(dsCurSettingsPath, dsStatePath);

        try
        {
            FileInputStream currentInStream = new FileInputStream(dsCurSettingsPath);
            ret = DsPresetsConfiguration.xmlConfigParsing(currentInStream, defaultInStream, useOffProfileForDsOff);
            currentInStream.close();
            defaultInStream.close();
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Cannot find DS config XML file " + dsCurSettingsPath);
            e.printStackTrace();
            ret = false;
        }
        catch (Exception e)
        {
            Log.e(TAG, "populateSettings(): Exception loading " + dsCurSettingsPath + " or parsing the file");
            e.printStackTrace();
            ret = false;
        }

        if (ret)
        {
            ret = DsPresetsConfiguration.createProfileSettings(useOffProfileForDsOff);
            if (ret)
            {
                // Check if any error happens when parsing the default.xml file
                ret = DsPresetsConfiguration.getParserStatusFlag();
            }
        }

        return ret;
    }

    /**
     * Restore current profiles settings.
     *
     * Re-initilizes current profiles from DS Preset Configuration and sends
     * current profile setting to DS1 Audio Processing layer.
     */
    public void restoreCurrentProfiles()
    {
        // Get the current profile settings for each Profile
        DsLog.log1(TAG, "Ds resetCurrentProfiles");
        currentProfiles_ = DsPresetsConfiguration.getCurrentSettings();
        DsLog.log1(TAG, "current profile settings " + currentProfiles_[selectedProfile_].getCurrentProfileSettings());
        setInitStatus(false);
    }

    /**
     * Save the current DS state and settings.
     *
     */
    public void saveDsStateAndSettings()
    {
        DsLog.log2(TAG, "saveDsStateAndSettings");
        DsStoreUtil.saveDsState(isDsOn_ ? "1" : "0", String.valueOf(selectedProfile_));
        DsStoreUtil.saveDsProfileSettings(currentProfiles_);
    }

    /**
     * Set the on/off state of the Ds effect.
     *
     * @param on The new on/off state of the Ds effect.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public void setDsOn(boolean on) throws DeadObjectException
    {
        DsLog.log2(TAG, "setDsOn: \"" + on + "\"");

        if (!useEffectReleaseForDsOff)
        {
            if (!validateDsEffect())
            {
                throw new DeadObjectException();
            }
        }

        isDsOn_ = on;

        if (useEffectReleaseForDsOff)
        {
            if (!on)
            {
                if (dsEffect_ != null)
                {
                    dsEffect_.release();
                    dsEffect_ = null;
                }
            }
            else
            {
                DsLog.log1(TAG, "Ds on/off setEnabled(" + on + ")");
                dsEffect_ = new DsEffect(audioSessionId_);
                // We don't want to restore the previous state, it should just be on
                setInitStatus(true);
                dsEffect_.setAllProfileSettings(currentProfiles_[selectedProfile_]);
            }
        }
        else
        {
            if (useOffProfileForDsOff)
            {
                if (on)
                {
                    DsLog.log1(TAG, "Ds set to selected profile " + selectedProfile_);
                    dsEffect_.setAllProfileSettings(currentProfiles_[selectedProfile_]);
                }
                else
                {
                    DsLog.log1(TAG, "Ds set to OFF profile");
                    dsEffect_.setAllProfileSettings(offProfile_);
                }
            }
            else
            {
                DsLog.log1(TAG, "Ds on/off setEnabled(" + on + ")");
                dsEffect_.setEnabled(on);
            }
        }
    }

    /**
     * Get the current on/off state of the Ds effect.
     *
     * @return The on/off state of the Ds effect. true for on state, and false for off state.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public boolean getDsOn() throws DeadObjectException
    {
        DsLog.log2(TAG, "getDsOn");

        if (!useEffectReleaseForDsOff)
        {
            if (!validateDsEffect())
            {
                throw new DeadObjectException();
            }
        }

        boolean effectEnabled = false;

        if (!useEffectReleaseForDsOff)
        {
            if (useOffProfileForDsOff)
            {
                effectEnabled = (isDsOn_ && dsEffect_.getEnabled());
            }
            else
            {
                effectEnabled = dsEffect_.getEnabled();
            }
        }
        else
        {
            if (dsEffect_ != null)
            {
                effectEnabled = dsEffect_.getEnabled();
            }
        }

        return effectEnabled;
    }

    /**
     * Get the number of profiles available for selection.
     *
     * @return The number of profiles.
     */
    public int getProfileCount()
    {
        DsLog.log2(TAG, "getProfileCount");
        return DsConstants.PROFILES_NUMBER;
    }

    /**
     * Get the array of strings representing the names of all profiles.
     *
     * @return The names array for all the profiles.
     */
    public String[] getProfileNames()
    {
        String[] profileNames = new String[DsConstants.PROFILES_NUMBER];

        DsLog.log2(TAG, "getProfileNames");
        for (int i = 0; i < DsConstants.PROFILES_NUMBER; i++)
        {
            profileNames[i] = currentProfiles_[i].getDisplayName();
        }

        return profileNames;
    }

    /**
     * Apply the settings of the selected profile.
     *
     * @param profile The profile index to be selected.
     *
     * @return True if the specified profile was set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws IllegalArgumentException if the profile input is invalid.
     */
    public boolean setSelectedProfile(int profile) throws DeadObjectException, IllegalArgumentException
    {
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            DsLog.log2(TAG, "setSelectedProfile: \"" + currentProfiles_[profile].getDisplayName() + "\"");

            int iRet = dsEffect_.setAllProfileSettings(currentProfiles_[profile]);
            if (iRet == AudioEffect.SUCCESS)
            {
                selectedProfile_ = profile;
                ret = true;
            }
        }
        else
        {
            Log.e(TAG, "setSelectedProfile: Invalid profile input");
            throw new IllegalArgumentException();
        }

        return ret;
    }

    /**
     * Get the profile currently selected.
     *
     * @return The profile index currently selected.
     */
    public int getSelectedProfile()
    {
        DsLog.log2(TAG, "getSelectedProfile");
        return selectedProfile_;
    }

    /**
     * Request to update the profile with the specified DsClientSettings.
     *
     * @param profile The profile index whose settings are to be updated.
     * @param clientSettings The new DsClientSettings settings coming from the Client.
     *
     * @return True if the specified profile settings were set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws IllegalArgumentException if the profile input is invalid.
     */
    public boolean setProfileSettings(int profile, DsClientSettings clientSettings) throws DeadObjectException, IllegalArgumentException
    {
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;

        DsLog.log2(TAG, "setProfileSettings: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            int iRet = AudioEffect.SUCCESS;
            ArrayList paramsChanged = currentProfiles_[profile].updateFromClientSettings(clientSettings);
            // TODO: Here we have settings for only 1 endpoint. We need to re-work on this part if the customer raises
            //       a requirement that the different endpoints must keep different AK settings.
            DsAkSettings akSettings = currentProfiles_[profile].getAllSettings();
            Iterator iter = paramsChanged.iterator();
            while (iter.hasNext())
            {
                String param = (String)iter.next();
                int len = DsAkSettings.getParamArrayLength(param);
                int[] values = new int[len];
                for (int i = 0; i < len; i++)
                    values[i] = akSettings.get(param, i);
                DsLog.log1(TAG, "Updating parameter " + param + " with new value/values");
                if (selectedProfile_ == profile)
                {
                    iRet = dsEffect_.setSingleSetting(param, values);
                    if (iRet != AudioEffect.SUCCESS)
                        break;
                }
            }
            if (iRet == AudioEffect.SUCCESS)
            {
                ret = true;
            }
        }
        else
        {
            Log.e(TAG, "setProfileSettings: Invalid profile input");
            throw new IllegalArgumentException();
        }
        return ret;
    }

    /**
     * Get the DsClientSettings instance representing the current client-configurable settings.
     *
     * @param profile The profile index whose settings are to be retrieved.
     * @return The DsClientSettings instance exposed to the Client.
     *
     * @throws IllegalArgumentException if the profile input is invalid.
     */
    public DsClientSettings getProfileSettings(int profile) throws IllegalArgumentException
    {
        DsLog.log2(TAG, "getProfileSettings: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            return currentProfiles_[profile].extractClientSettings();
        }
        else
        {
            Log.e(TAG, "getProfileSettings: Invalid profile input");
            throw new IllegalArgumentException();
        }
    }

    /**
     * Reset the settings of the specified profile index to defaults.
     *
     * @param profile The profile index whose settings will be reset.
     *
     * @return True if the specified profile was reset successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws IllegalArgumentException if the profile input is invalid.
     * @throws UnsupportedOperationException if error is found when reset the profile.
     */
    public boolean resetProfile(int profile) throws DeadObjectException, UnsupportedOperationException, IllegalArgumentException
    {
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;

        DsLog.log2(TAG, "resetProfile: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            String displayName = defaultProfiles_[profile].getDisplayName();
            String description = defaultProfiles_[profile].getDescription();
            DsAkSettings akSettings = defaultProfiles_[profile].getAllSettings();
            boolean custom = defaultProfiles_[profile].isCustom();
            DsProfileSettings.Category category = defaultProfiles_[profile].getCategory();
            int ieqPreset = defaultProfiles_[profile].getIeqPreset();

            try
            {
                HashSet<String> savedParams = new HashSet<String>(DsClientSettings.basicProfileParams);
                currentProfiles_[profile] = new DsProfileSettings(displayName, description, akSettings, custom, category, ieqPreset, null, savedParams);
                if (profile == selectedProfile_)
                {
                    dsEffect_.setAllProfileSettings(currentProfiles_[profile]);
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, e.toString());
                throw new UnsupportedOperationException();
            }
            ret = true;
        }
        else
        {
            Log.e(TAG, "resetProfile: Invalid profile input");
            throw new IllegalArgumentException();
        }
        return ret;
    }

    /**
     * Set the name for the profile at the specified index.
     *
     * @param profile The profile index to which the new name is set.
     * @param name The new name for the profile.
     *
     * @return True if profile name was set successfully, false otherwise.
     *
     * @throws IllegalArgumentException if the profile input is invalid.
     * @throws UnsupportedOperationException if Name of this Profile is not settable.
     */
    public boolean setProfileName(int profile, String name) throws UnsupportedOperationException, IllegalArgumentException
    {
        DsLog.log2(TAG, "setProfileNames: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        //TBD: test invalid name?
        boolean ret = false;

        if (name != null)
        {
            if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
            {
                if (profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
                {
                    currentProfiles_[profile].setDisplayName(name);

                    ret = true;
                }
                else
                {
                    DsLog.log1(TAG, "setProfileName: Name of this Profile is not settable");
                    throw new UnsupportedOperationException();
                }
            }
            else
            {
                Log.e(TAG, "setProfileName: Invalid profile input");
                throw new IllegalArgumentException();
            }
        }
        return ret;
    }

    /**
     * Enable or disable the visualizer.
     * This enables or disables the visualizer in the underlying Ds library.
     * When disabled, visualizer data will not be updated and it therefore should not be retrieved.
     *
     * @param enable The new state of the visualizer.
     * @return AudioEffect.SUCCESS in case of success, AudioEffect.ERROR_BAD_VALUE,
     *         AudioEffect.ERROR_NO_MEMORY, AudioEffect.ERROR_INVALID_OPERATION or
     *         AudioEffect.ERROR_DEAD_OBJECT in case of failure.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public int setVisualizerOn(boolean enable) throws DeadObjectException
    {
        DsLog.log2(TAG, "setVisualizerOn: \"" + enable + "\"");
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return -1; // AUDIOEFFECT_ERROR
        }
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }
        return dsEffect_.setVisualizerOn(enable);
    }

    /**
     * Get the enabled state of the visualizer.
     *
     * @return true if the visualizer is enabled. false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public boolean getVisualizerOn() throws DeadObjectException
    {
        DsLog.log2(TAG, "getVisualizerOn");
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        return dsEffect_.getVisualizerOn();
    }

    /**
     * Get the visualizer data.
     *
     * @param gains The array to store visualizer band gains retrieved.
     * @param excitations The array to store visualizer band excitations retrieved.
     * @return The number of retrieved values.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public int getVisualizerData(float[] gains, float[] excitations) throws DeadObjectException
    {
        DsLog.log2(TAG, "getVisualizerData");
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return 0;
        }

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        int[] visualizerData = dsEffect_.getVisualizerData();
        if (visualizerData == null)
        {
            return 0;
        }

        float scale = (float)DsProfileSettings.DB_SCALING_FACTOR;
        int maxLen = DsAkSettings.getParamArrayLength("vcbg");
        int numGains = (gains.length < maxLen) ? gains.length : maxLen;
        for (int i = 0; i < numGains; i++)
        {
            gains[i] = (float)visualizerData[i] / scale;
        }
        int numExcitations = (excitations.length < maxLen) ? excitations.length : maxLen;
        int index = 0;
        for (int i = 0; i < numExcitations; i++)
        {
            index = i + maxLen;
            excitations[i] = (float)visualizerData[index] / scale;
        }

        return (numGains + numExcitations);
    }

    /**
     * Get the version of the underlying Ds audio processing library.
     *
     * @return The Ds audio processing library version in string format.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public String getDsApVersion() throws DeadObjectException
    {
        DsLog.log2(TAG, "getDsApVersion");
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return "Unknown";
        }

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        String value = dsEffect_.getVersion();
        StringBuilder version = new StringBuilder("DAP version : ");
        version.append(value);

        return version.toString();
    }

    /**
     * Get the version of the Ds Android integration.
     *
     * @return The Ds Android integration version in string format.
     */
    public String getDsVersion()
    {
        DsLog.log2(TAG, "getDsVersion");
        return DS_VERSION_EXTERNAL;
    }

    /**
     * Set a new Ieq preset for the specified profile.
     *
     * @param profile The index of the specified profile.
     * @param preset The index of the new preset.
     *
     * @return True if the preset was set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws IllegalArgumentException if the profile input is invalid.
     */
    public boolean setIeqPreset(int profile, int preset) throws DeadObjectException, IllegalArgumentException
    {
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;
        DsLog.log2(TAG, "setIeqPreset: \"" + preset + "\"");

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            if ((preset >= 0) && (preset < DsConstants.IEQ_PRESETS_NUMBER))
            {
                currentProfiles_[profile].setIeqPreset(preset);
                int iRet = dsEffect_.setAllProfileSettings(currentProfiles_[profile]);
                if (iRet == AudioEffect.SUCCESS)
                {
                    ret = true;
                }
            }
            else
            {
                Log.e(TAG, "setIeqPreset: Invalid profile input");
                throw new IllegalArgumentException();
            }
        }
        return ret;
    }

    /**
     * Get the active preset of the specified profile.
     *
     * @param profile The index of the specified profile.
     *
     * @return The active preset of the specified profile, -1 means failed.
     *
     * @throws IllegalArgumentException if the profile input is invalid.
     */
    public int getIeqPreset(int profile) throws IllegalArgumentException
    {
        int ret = -1;
        DsLog.log2(TAG, "getIeqPreset"); 

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            ret = currentProfiles_[profile].getIeqPreset();
        }
        else
        {
            Log.e(TAG, "getIeqPrest: Invalid profile");
            throw new IllegalArgumentException();
        }
        return ret;
    }

    /**
     * Get the specified profile settings modified value if it is different from
     * the factory default settings.
     *
     * @param profile The index of the profile to be queried.
     *
     * @return Value of the modified profile setting.
     */
    public int getProfileModified(int profile)
    {

        DsLog.log1(TAG, "getProfileModified"); 
        int modifiedValue = DsCommon.DS_PROFILE_NOT_MODIFIED;

        // check if the profile name has been changed.
        String defaultName = defaultProfiles_[profile].getDisplayName();
        String currentName = currentProfiles_[profile].getDisplayName();
        if (!defaultName.equals(currentName))
        {
            modifiedValue |= DsCommon.DS_PROFILE_NAME_MODIFIED;
        }

        // check if the UI controllable profile settings have been changed.
        DsAkSettings akDefaultSettings = defaultProfiles_[profile].getAllSettings();
        DsAkSettings akCurrentSettings = currentProfiles_[profile].getAllSettings();
        Object[] params = currentProfiles_[profile].getParamsSaved();
        if (params != null)
        {
            for (int i = 0; i < params.length; i++)
            {
                String param = (String)params[i];
                int paramLen = DsAkSettings.getParamArrayLength(param);
                for (int j = 0; j < paramLen; j++)
                {
                    if (akCurrentSettings.get(param, j) != akDefaultSettings.get(param, j))
                    {
                        modifiedValue |= DsCommon.DS_PROFILE_SETTINGS_MODIFIED;
                        break;
                    }
                }
            }
        }

        if ((modifiedValue & DsCommon.DS_PROFILE_SETTINGS_MODIFIED) != DsCommon.DS_PROFILE_SETTINGS_MODIFIED)
        {
            // check if the IEQ settting has been changed.
            int ieqDefaultIndex = defaultProfiles_[profile].getIeqPreset();
            int ieqCurrentIndex = currentProfiles_[profile].getIeqPreset();
            if (ieqCurrentIndex != ieqDefaultIndex)
            {
                modifiedValue |= DsCommon.DS_PROFILE_SETTINGS_MODIFIED;
            }
        }

        if ((modifiedValue & DsCommon.DS_PROFILE_SETTINGS_MODIFIED) != DsCommon.DS_PROFILE_SETTINGS_MODIFIED)
        {
            // check if the GEQ settting has been changed.
            int[][] geqDefaultSettings = defaultProfiles_[profile].getGeqGainArray();
            int[][] geqCurrentSettings = currentProfiles_[profile].getGeqGainArray();
            int gebfLen = DsAkSettings.getParamArrayLength("gebf");
            for (int i = 0; i < DsConstants.IEQ_PRESETS_NUMBER; i++)
            {
                for (int j = 0; j < gebfLen; j++)
                {
                    if (geqCurrentSettings[i][j] != geqDefaultSettings[i][j])
                    {
                        modifiedValue |= DsCommon.DS_PROFILE_SETTINGS_MODIFIED;
                        break;
                    }
                }
            }
        }

        return modifiedValue;
    }

    /**
     * Tell whether the specified parameter is part of the basic parameter list that is always saved when saving the current profiles.
     *
     * @param parameter The parameter name.
     * @return true on yes, false otherwise.
     */
    public boolean isBasicProfileSettings(String parameter)
    {
        return DsClientSettings.basicProfileParams.contains(parameter);
    }

    /**
     * Request to update the Geq band gains for the specified profile and Ieq preset.
     *
     * @param profile The profile index.
     * @param preset The Ieq preset index whose settings are to be updated.
     * @param geqBandGains The new graphic equalizer band gains in dB.
     *
     * @return True if the specified profile settings were set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws IllegalArgumentException if the profile input or the preset input is invalid.
     */
    public boolean setGeq(int profile, int preset, float[] geqBandGains) throws DeadObjectException, IllegalArgumentException
    {
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;

        DsLog.log2(TAG, "setGeq: \"profile name = " + currentProfiles_[profile].getDisplayName() + " preset " + preset +"\"");
        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            if ((preset >= 0) && (preset < DsConstants.IEQ_PRESETS_NUMBER))
            {
                int iRet = AudioEffect.SUCCESS;
                int[] values = currentProfiles_[profile].setGeq(preset, geqBandGains);
                if (selectedProfile_ == profile)
                {
                    // Set new Geq band gains to dsEffect
                    iRet = dsEffect_.setSingleSetting("gebg", values);
                }
                if (iRet == AudioEffect.SUCCESS)
                {
                    ret = true;
                }
            }
            else
            {
                Log.e(TAG, "setGeq: Invalid Ieq preset input");
                throw new IllegalArgumentException();
            }
        }
        else
        {
            Log.e(TAG, "setGeq: Invalid profile input");
            throw new IllegalArgumentException();
        }
        return ret;
    }

    /**
     * Get the Geq band gains of the specified profile and Ieq preset.
     *
     * @param profile The profile index.
     * @param preset The preset index whose settings are to be retrieved.
     *
     * @return The Geq band gains, in dB, of specified profile and Ieq preset.
     *
     * @throws IllegalArgumentException if the profile input or the preset input is invalid.
     */
    public float[] getGeq(int profile, int preset) throws IllegalArgumentException
    {
        DsLog.log2(TAG, "getGeq: \"profile name = " + currentProfiles_[profile].getDisplayName() + " preset " + preset +"\"");

        float[] values = null;

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            if ((preset >= 0) && (preset < DsConstants.IEQ_PRESETS_NUMBER))
            {
                values = currentProfiles_[profile].getGeq(preset);
            }
            else
            {
                Log.e(TAG, "getGeq: Invalid preset input");
                throw new IllegalArgumentException();
            }
        }
        else
        {
            Log.e(TAG, "getGeq: Invalid profile input");
            throw new IllegalArgumentException();
        }
        return values;
    }

    /**
     * Set a ds audio processing parameter directly, and the new setting therefore applies to the current profile.
     *
     * @param parameter The parameter name.
     * @param values The parameter values.
     * @return true if the Ds audio processing parameter was set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws UnsupportedOperationException if the set operation fails.
     */
    public boolean setDsApParam(String parameter, int[] values) throws DeadObjectException, UnsupportedOperationException
    {
        boolean ret = false;

        DsLog.log2(TAG, "setDsApParam");
        if (useEffectReleaseForDsOff && dsEffect_ == null)
        {
            return false;
        }
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }
        if (parameter.equals("iebt"))
        {
            Log.e(TAG, "iebt is NOT allowed to be set");
            throw new UnsupportedOperationException("Fail to set the parameter");
        }
        if (parameter.equals("gebg"))
        {
            Log.e(TAG, "gebg is NOT allowed to be set by setDsApParam, please use setGeq instead");
            throw new UnsupportedOperationException("Fail to set the parameter");
        }

        int[] settings = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            settings[i] =  values[i];
        }
        try
        {
            currentProfiles_[selectedProfile_].setDsApParam(parameter, settings);
            int iRet = dsEffect_.setSingleSetting(parameter, settings);
            if (iRet == AudioEffect.SUCCESS)
            {
                ret = true;
            }
            if (!isBasicProfileSettings(parameter))
            {
                // Check whether the new setting differ from the default setting or not.
                boolean paramModified = false;
                DsAkSettings defaultSettings = defaultProfiles_[selectedProfile_].getAllSettings();
                for (int i = 0; i < settings.length; i++)
                {
                    if (settings[i] != defaultSettings.get(parameter, i))
                    {
                        paramModified = true;
                        break;
                    }
                }
                // Add the parameter to the list that will be saved for the profile before shutdown
                if (paramModified)
                {
                    currentProfiles_[selectedProfile_].addParamSaved(parameter);
                }
                else
                {
                    currentProfiles_[selectedProfile_].removeParamSaved(parameter);
                }
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new UnsupportedOperationException("Fail to set the parameter");
        }

        return ret;
    }

    /**
     * Get a parameter value of the currently selected profile directly from the audio processing instance.
     *
     * @param parameter The parameter name.
     * @return The values currently adopted by the specified parameter, and null on failure.
     */
    public int[] getDsApParam(String parameter)
    {
        int[] values = null;
        DsLog.log2(TAG, "getDsApParam");
        try
        {
            values = currentProfiles_[selectedProfile_].getDsApParam(parameter);
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            values = null;
        }

        return values;
    }

    /**
     * Get the array length of an AK parameter.
     *
     * @param parameter The parameter name.
     * @return The length of the array of specified parameter.
     */
    public int getDsApParamLength(String parameter)
    {
        return DsAkSettings.getParamArrayLength(parameter);
    }

    /**
     * Recreate a new instance of DsEffect.
     * Since the DsEffect can be created in the constructor, we will not throw exceptions here.
     *
     * @internal
     */
    private boolean recreateDsEffect()
    {
        boolean ret = false;
        DsLog.log1(TAG, "recreateDsEffect");
        try
        {
            if (dsEffect_ != null)
            {
                dsEffect_.release();
            }
            dsEffect_ = new DsEffect(audioSessionId_);

            setInitStatus(true);
            ret = true;
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception in recreateDsEffect.");
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Initialize the status of the effect.
     *
     * @internal
     */
    private void setInitStatus(boolean useExistingState)
    {
        if (!useExistingState)
        {
            // Get Ds initial state
            String[] restoredState = DsStoreUtil.loadDsState();
            isDsOn_ = (restoredState[0].equals("1")) ? true : false;
            selectedProfile_ = Integer.parseInt(restoredState[1]);
        }

        DsLog.log1(TAG, "restore Ds=" + isDsOn_);
        DsLog.log1(TAG, "restore profile=" + selectedProfile_);

        try
        {
            // Ensure the DsEffect is enabled.
            if (dsEffect_ != null)
            {
                dsEffect_.setEnabled(true);

                // Set the initial status into the effect.
                setSelectedProfile(selectedProfile_);

                if (!useEffectReleaseForDsOff)
                {
                    setDsOn(isDsOn_);
                }
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception in setInitStatus");
            e.printStackTrace();
        }
    }

    /**
     * Check whether the effect is valid, if not, try to recreate.
     *
     * @return True if the Ds effect is valid, false otherwise.
     */
    public boolean validateDsEffect()
    {
        boolean ret = false;

        if (dsEffect_ != null) {
            ret = dsEffect_.hasControl();
            if (!ret)
            {
                Log.e(TAG, "Cannot control the DsEffect, trying to recreate...");
                ret = recreateDsEffect();
            }
        }

        return ret;
    }
}

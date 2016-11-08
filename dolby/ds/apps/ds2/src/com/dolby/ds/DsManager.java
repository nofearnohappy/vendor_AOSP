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

package com.dolby.ds;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.util.Log;

import android.os.DeadObjectException;

import android.media.audiofx.AudioEffect;
import com.dolby.api.DsConstants;
import com.dolby.api.DsLog;
import com.dolby.api.DsCommon;
import com.dolby.api.DsParams;
import com.dolby.api.DsProfileName;

public class DsManager
{
    private static final String TAG = "DsManager";

    private static final String DS_VERSION_EXTERNAL = DsVersion.DS_VERSION;
    private static final String DS_VERSION_INTERNAL = DsVersion.DS_VERSION + " [Build " + DsVersion.DS_VERSION_BUILD + "]";

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

    /**
     * The flag indicating whether DsEffect is suspended or not.
     * @internal
     */
    private boolean isDsSuspended_ = false;

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
     * Feature on state.
     * @internal
     */
    private static final String STATE_ON = "on";

    /**
     * Feature off state.
     * @internal
     */
    private static final String STATE_OFF = "off";

    /**
     * The directory to store DS current settings and DS state.
     */
    private static String DS_CURRENT_DIR = null;

    /**
     * The file name to store the current settings.
     */
    public static final String DS_CURRENT_FILENAME = "ds-current.xml";

    /**
     * The file name to store the current state.
     */
    public static final String DS_STATE_FILENAME = "ds-state.xml";

    /**
     * The constructor.
     *
     */
    public DsManager()
    {
        DsProperty.init(); 

        // Get the default profile settings each Profile
        defaultProfiles_ = DsPresetsConfiguration.getDefaultSettings();

        // Get the current profile settings each Profile
        currentProfiles_ = DsPresetsConfiguration.getCurrentSettings();

        // Get the default profile settings off Profile
        offProfile_ = DsPresetsConfiguration.getOffProfileSettings();
    }

    /**
     * Creates the Ds Audio Effect.
     */
    public void createDsEffect(int audioSessionId)  
    {
        Log.i(TAG, "Creating Ds effect on audioSessionId = " + audioSessionId);

        if (audioSessionId < 0)
        {
            Log.e(TAG, "Ds effect with specified session Id (" + audioSessionId + ") is less than zero");
        }
        else
        {
            if (DsAkSettings.isConstantAkParamsDefined())
            {
                try 
                {
                    audioSessionId_ = audioSessionId;
                    dsEffect_ = new DsEffect(audioSessionId_);
                    dsEffect_.defineProfile(DsConstants.PROFILES_NUMBER, offProfile_);
                    setInitStatus(false); 
                }
                catch (IllegalStateException ex)
                {
                    Log.e(TAG, "createDsEffect() FAILED! IllegalStateException");
                }
                catch (Exception ex)
                {
                    Log.e(TAG, "createDsEffect() FAILED! Exception");
                    ex.printStackTrace();
                }
            }
            else
            {
                Log.e(TAG, "createDsEffect() FAILED! Constant AK parameters NOT defined yet.");
            }
        }
    }

    /**
     * Load the DS settings.
     *
     * @param defaultInStream The input stream of the XML file containing the default settings.
     * @param dir The location to store the DS current settings and DS state XML files.
     * @return true if the settings are successfully populated, and false otherwise.
     */
    static public boolean loadSettings(InputStream defaultInStream, String dir)
    {
        boolean ret = true;
        DsLog.log2(TAG, "loadSettings");
        String dsCurSettingsPath;
        String dsStatePath;

        if (dir != null)
        {
            dsCurSettingsPath = dir + "/" + DS_CURRENT_FILENAME;
            dsStatePath = dir + "/" + DS_STATE_FILENAME;
            DsStoreUtil.storeDsPath(dsCurSettingsPath, dsStatePath);
            DS_CURRENT_DIR = dir;
        }
        else
        {
            Log.e(TAG, "No specified location to store the DS current settings and DS state!");
            return false;
        }

        try
        {
            FileInputStream currentInStream = new FileInputStream(dsCurSettingsPath);
            ret = DsPresetsConfiguration.xmlConfigParsing(currentInStream, defaultInStream);
            currentInStream.close();
            if (defaultInStream != null)
            {
                defaultInStream.close();
            }
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
            ret = DsPresetsConfiguration.createProfileSettings();
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
     * current profile setting to DS Audio Processing layer. Note that this
     * method must be called after the default settings have been loaded once
     * on startup.
     */
    public void restoreCurrentProfiles()
    {
        // Get the current profile settings for each Profile
        DsLog.log1(TAG, "Ds restoreCurrentProfiles");

        if (loadSettings(null, DS_CURRENT_DIR))
        {
            currentProfiles_ = DsPresetsConfiguration.getCurrentSettings();
            DsLog.log1(TAG, "current profile settings " + currentProfiles_[selectedProfile_].getCurrentProfileSettings());
            setInitStatus(false);
        }
        else
        {
            Log.e(TAG, "loadSettings FAILED! DS settings are NOT loaded successfully.");
        }
    }

    /**
     * Save the current DS state and settings.
     *
     */
    public void saveDsStateAndSettings()
    {
        DsLog.log2(TAG, "saveDsStateAndSettings");
        DsStoreUtil.saveDsState(isDsOn_ ? "1" : "0", String.valueOf(selectedProfile_), currentProfiles_[DsConstants.PROFLIE_CUSTOM_1].getDefaultName(), currentProfiles_[DsConstants.PROFLIE_CUSTOM_2].getDefaultName());
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
        DsLog.log1(TAG, "setDsOn: \"" + on + "\"");
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        isDsOn_ = on;

        DsLog.log1(TAG, "Ds on/off setEnabled(" + on + ")");
        dsEffect_.setEnabled(on);
    }

    /**
     * Set the suspended state of the Ds effect.
     *
     * @param on The new suspended state of the Ds effect.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public void setDsSuspended(boolean suspended) throws DeadObjectException
    {
        DsLog.log1(TAG, "setDsSuspended(" + suspended + ")");
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        isDsSuspended_ = suspended;
    }

    /**
     * Check whether DS effect is suspended or not.
     *
     * @return The current suspended state of DS effect.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public boolean getDsSuspended() throws DeadObjectException
    {
        DsLog.log2(TAG, "getDsSuspended()");
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        return isDsSuspended_;
    }

    /**
     * Get the current on/off/suspended state of the Ds effect.
     *
     * @return The state of the Ds effect.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public int getDsOn() throws DeadObjectException
    {
        DsLog.log2(TAG, "getDsOn");

        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }
        if (isDsSuspended_)
        {
            DsLog.log1(TAG, "DS effect is now suspended");
            return 0;
        }

        boolean effectEnabled;

        effectEnabled = dsEffect_.getEnabled();

        return effectEnabled ? 1 : 0;
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
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            DsLog.log1(TAG, "setSelectedProfile:" + profile);

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
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        boolean ret = false;
        String lastDefName = "";

        DsLog.log2(TAG, "resetProfile: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            String displayName = null;
            if(profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
            {
                lastDefName = currentProfiles_[profile].getDefaultName();
                displayName = "";
            }
            else
            {
                displayName = defaultProfiles_[profile].getDisplayName();
            }
            
            String description = defaultProfiles_[profile].getDescription();
            DsAkSettings akSettings = defaultProfiles_[profile].getAllSettings();
            boolean custom = defaultProfiles_[profile].isCustom();
            DsProfileSettings.Category category = defaultProfiles_[profile].getCategory();
            int ieqPreset = defaultProfiles_[profile].getIeqPreset();

            try
            {
                HashSet<String> savedParams = new HashSet<String>(DsProfileSettings.getBasicProfileParams());
                currentProfiles_[profile] = new DsProfileSettings(displayName, description, akSettings, custom, category, ieqPreset, null, savedParams);
                if(profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
                {
                    currentProfiles_[profile].setDefaultName(lastDefName);
                }
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
    public boolean setProfileName(int profile, DsProfileName name) throws UnsupportedOperationException, IllegalArgumentException
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
                    if (name.getDefaultName() != null)
                    {
                        currentProfiles_[profile].setDefaultName(name.getDefaultName());
                    }
                    if (name.getCurrentName() != null)
                    {
                        if (currentProfiles_[profile].getDefaultName().equals(name.getCurrentName()))
                        {
                            currentProfiles_[profile].setDisplayName("");
                        } 
                        else
                        {
                            currentProfiles_[profile].setDisplayName(name.getCurrentName());
                        }
                    }
                    
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
     * Get the name of the profile at the specified index.
     *
     * @param profile The profile index to which the new name is get.
     *
     * @return name The name of the specific profile.
     *
     * @throws IllegalArgumentException if the profile input is invalid.
     * @throws UnsupportedOperationException if Name of this Profile is not settable.
     */
    public DsProfileName getProfileName(int profile) throws UnsupportedOperationException, IllegalArgumentException
    {
        DsLog.log2(TAG, "getProfileName: \"" + currentProfiles_[profile].getDisplayName() + "\"");
        boolean ret = false;
        DsProfileName profileName = new DsProfileName();

        if ((profile >= 0) && (profile < DsConstants.PROFILES_NUMBER))
        {
            if (profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM)
            {
                String currentName = currentProfiles_[profile].getDisplayName();
                profileName.setCurrentName(currentName);
                String defaultName = currentProfiles_[profile].getDefaultName();
                profileName.setDefaultName(defaultName);
                ret = true;
            }
            else
            {
                DsLog.log1(TAG, "getProfileName: Name of this Profile is not gettable");
                throw new UnsupportedOperationException();
            }
        }
        else
        {
            Log.e(TAG, "getProfileName: Invalid profile input");
            throw new IllegalArgumentException();
        }
        return profileName;
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
        String currentName = currentProfiles_[profile].getDisplayName();
        if( (profile >= DsConstants.PROFILE_INDEX_FIRST_CUSTOM) && (profile < DsConstants.PROFILES_NUMBER))
        {
            String defaultName = currentProfiles_[profile].getDefaultName();
            if ((!currentName.equals("")) && (!currentName.equals(defaultName)))
            {
                modifiedValue |= DsCommon.DS_PROFILE_NAME_MODIFIED;
            }
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
    public static boolean isBasicProfileParam(String parameter)
    {
        return DsProfileSettings.isBasicProfileParam(parameter);
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
     * Set a ds audio processing parameter directly, and the new setting will be applied to the underlying effect
     * if the specified profile is the current selected one.
     *
     * @param profile The profile to which the parameter will be applied.
     * @param paramId The parameter id.
     * @param values The parameter values.
     * @return true if the Ds audio processing parameter was set successfully, false otherwise.
     *
     * @throws DeadObjectException if the DsEffect cannot be used.
     * @throws UnsupportedOperationException if the set operation fails.
     */
    public boolean setParameter(int profile, int paramId, int[] values) throws DeadObjectException, UnsupportedOperationException
    {
        boolean ret = true;

        DsLog.log2(TAG, "setParameter"); 
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        DsParams param = DsParams.FromInt(paramId);
        if(param == null)
        {
            return ret;
        }
        String parameter = param.toString();

        int[] settings = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            if(parameter.equals("vdhe") || parameter.equals("vspe"))
            {
                settings[i] = (values[i] == 1) ? DsAkSettings.AK_DS1_FEATURE_AUTO : DsAkSettings.AK_DS1_FEATURE_OFF;
            }
            else
            {
                settings[i] =  values[i];
            }
        }
        try
        {
            currentProfiles_[profile].setParameter(parameter, settings);
            // The parameter is applied to the underlying effect only if the specified profile
            // is the current selected one.
            if (profile == selectedProfile_)
            {
                int iRet = dsEffect_.setSingleSetting(parameter, settings);
                if (iRet != AudioEffect.SUCCESS)
                {
                    ret = false;
                }
            }
            if (!isBasicProfileParam(parameter) && ret)
            {
                // Check whether the new setting differ from the default setting or not.
                boolean paramModified = false;
                DsAkSettings defaultSettings = defaultProfiles_[profile].getAllSettings();
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
                    currentProfiles_[profile].addParamSaved(parameter);
                }
                else
                {
                    currentProfiles_[profile].removeParamSaved(parameter);
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
     * @param profile The profile from which the parameter will be retrieved.
     * @param paramId The parameter id.
     * @return The values currently adopted by the specified parameter, and null on failure.
     */
    public int[] getParameter(int profile, int paramId)
    {
        int[] values = null;
        DsLog.log2(TAG, "getParameter");

        DsParams param = DsParams.FromInt(paramId);
        if(param == null)
        {
            return values;
        }

        String parameter = param.toString();
        try
        {
            values = currentProfiles_[profile].getParameter(parameter);
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
     * @param paramId The parameter id.
     * @return The length of the array of specified parameter.
     */
    public int getParamLength(int paramId)
    {
        DsParams param = DsParams.FromInt(paramId);
        if(param == null)
        {
            return 0;
        }

        String parameter = param.toString();
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
            currentProfiles_[DsConstants.PROFLIE_CUSTOM_1].setDefaultName(restoredState[2]);
            currentProfiles_[DsConstants.PROFLIE_CUSTOM_2].setDefaultName(restoredState[3]);
        }

        DsLog.log1(TAG, "restore Ds=" + isDsOn_);
        DsLog.log1(TAG, "restore profile=" + selectedProfile_);

        try
        {
            // Set the initial status into the effect.
            setSelectedProfile(selectedProfile_);
            setDsOn(isDsOn_);
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
        boolean ret = dsEffect_.hasControl();

        if (!ret)
        {
            Log.e(TAG, "Cannot control the DsEffect, trying to recreate...");
            ret = recreateDsEffect();
        }

        return ret;
    }

    /**
     * Get the off type of DAP, off profile or bypass mode.
     *
     * @return The off type (either off profile or bypass) adopted by the effect.
     * @throws DeadObjectException if the DsEffect cannot be used.
     */
    public int getOffType() throws DeadObjectException
    {
        DsLog.log2(TAG, "getOffType");
        if (!validateDsEffect())
        {
            throw new DeadObjectException();
        }

        return dsEffect_.getOffType();
    }

    /**
     * Set the system properties related to profile.
     *
     */
    public void setProfileProperties (int profile)
    {
        DsProperty.setProfileNameProperty(DsCommon.PROFILE_NAMES[profile]);
        DsAkSettings settings = currentProfiles_[profile].getAllSettings();

        String state = (settings.get("deon", 0) != 0) ? STATE_ON : STATE_OFF;
        DsProperty.setDialogEnhancerProperty(state);

        state = (settings.get("vdhe", 0) != 0) ? STATE_ON : STATE_OFF;
        DsProperty.setHeadphoneVirtualizerProperty(state);

        state = (settings.get("vspe", 0) != 0) ? STATE_ON : STATE_OFF;
        DsProperty.setSpeakerVirtualizerProperty(state);

        state = (settings.get("dvle", 0) != 0) ? STATE_ON : STATE_OFF;
        DsProperty.setVolumeLevellerProperty(state);

        state = (settings.get("geon", 0) != 0) ? STATE_ON : STATE_OFF;
        DsProperty.setGeqStateProperty(state);

        int index = getIeqPreset(profile);
        state = (index != 0) ? STATE_ON : STATE_OFF; 
        DsProperty.setIeqStateProperty(state);
        DsProperty.setIeqPresetProperty(DsCommon.IEQ_PRESET_NAMES[index]);
    }
}

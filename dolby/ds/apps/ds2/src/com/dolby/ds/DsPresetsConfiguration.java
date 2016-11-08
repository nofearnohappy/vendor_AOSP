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

import java.util.*;
import java.io.InputStream;
import android.util.Log;

import com.dolby.api.DsCommon;
import com.dolby.api.DsLog;

public class DsPresetsConfiguration
{
    private static final String TAG = "DsPresetsConfiguration";

    private static DsConfigParser xmlParserDefault = null;
    private static DsConfigParser xmlParserCurrent = null;

    private static Vector<DsProfileSettings> defaultProfiles = new Vector<DsProfileSettings>();
    private static Vector<DsProfileSettings> currentProfiles = new Vector<DsProfileSettings>();
    private static DsProfileSettings offProfile_;
    private static boolean ieqSettingsAdded = false;

    /**
     * Parse the current ds settings and the default ds settings.
     *
     * @param currentSettings The input stream of the XML file containing the current settings to be parsed.
     * @param defaultSettings The input stream of the XML file containing the default settings to be parsed.
     * @return The status of parsing the XML file. true for success, and false for fail.
     */
    public static boolean xmlConfigParsing(InputStream currentSettings, InputStream defaultSettings)
    {
        boolean ret = true;

        // Parse the current settings.
        // Ignore the parsing errors for the current settings since they may be newly created.
        try
        {
            xmlParserCurrent = new DsConfigParser(currentSettings);
        }
        catch (IllegalArgumentException e)
        {
            DsLog.log1(TAG, "The current settings are invalid. The default settings will be used.");
        }

        if (defaultSettings != null)
        {
            // Parse the default settings.
            try
            {
                xmlParserDefault = new DsConfigParser(defaultSettings);
            }
            catch (IllegalArgumentException e)
            {
                Log.e(TAG, "Error in parsing the default settings.");
                ret = false;
            }
        }
        else if (xmlParserDefault == null)
        {
            DsLog.log1(TAG, "NULL input stream for the default settings.");
            ret = false;
        }

        return ret;
    }

    /**
     * Check if the XML file is in the required format or the data is valid or not.
     *
     * @return The status of parsing the XML file. true for success, and false for fail.
     */
    public static boolean getParserStatusFlag()
    {
        return xmlParserDefault.getParserStatusFlag();
    }

    /**
     * Create DsProfileSettings instances and initialize the static settings.
     *
     * @return true on success, and false otherwise.
     */
    static boolean createProfileSettings()
    {
        boolean ret = true;

        try
        {
            // Clear this static variables before adding new parameters.
            defaultProfiles.clear();
            currentProfiles.clear();
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[0], DsProfileSettings.Category.MOVIE);
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[1], DsProfileSettings.Category.MUSIC);
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[2], DsProfileSettings.Category.GAME);
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[3], DsProfileSettings.Category.VOICE);
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[4], DsProfileSettings.Category.CUSTOMIZED);
            addNewProfileSettings(DsCommon.PROFILE_NAMES_XML[5], DsProfileSettings.Category.CUSTOMIZED);
            addOffProfileSettings();
        }
        catch (Exception e)
        {
            Log.e(TAG, e.toString());
            ret = false;
            return ret;
        }

        return ret;
    }

    /**
     * Get default profile settings.
     *
     * @return The default profile settings.
     */
    public static DsProfileSettings[] getDefaultSettings()
    {
        return defaultProfiles.toArray(new DsProfileSettings[defaultProfiles.size()]);
    }

    /**
     * Get current profile settings.
     *
     * @return The current profile settings.
     */
    public static DsProfileSettings[] getCurrentSettings()
    {
        return currentProfiles.toArray(new DsProfileSettings[currentProfiles.size()]);
    }

    /**
     * Get off profile settings.
     *
     * @return The off profile settings.
     */
    public static DsProfileSettings getOffProfileSettings()
    {
        return offProfile_;
    }

    /**
     * Initialize the Ieq Settings.
     * @internal
     */
    private static void addIeqSettings()
    {
        int[][] ieqSettings = xmlParserDefault.getIeqSettingArray();
        int ieqIndex = 0;
        if (ieqSettings != null)
        {
            for (int[] settings : ieqSettings)
                DsProfileSettings.setIeqBandTargets(ieqIndex++, settings);
        }
    }

    /** Creates a new DsProfileSettings instance for the specified profile name parsed from the default and current configuration files
     * and adds to the default and current vectors
     * @internal
     *
     * @param name The string name of a profile
     * @param category The DsProfileSettings.Category of a profile
     * @throws UnsupportedOperationException
     */
    private static void addNewProfileSettings(String name, DsProfileSettings.Category category) throws UnsupportedOperationException
    {
        int[][] profileSettings;
        String profileName;
        int profileIeq;
        int[][] currentSettings;
        String currentName;
        int currentIeq;
        int[][] geqSettings;

        // extract default settings for the selected profile
        profileSettings = xmlParserDefault.getSettingArray(name, true);
        profileName = xmlParserDefault.getProfileSettingName(name);
        profileIeq = xmlParserDefault.getProfileSettingIeq(name);

        // Get GEQ band gain from default XML
        int[][] profileGebg = xmlParserDefault.getGeqSettingArray(name, null);

        if (profileSettings != null && DsAkSettings.isConstantAkParamsDefined())
        {
            try
            {
                if (!ieqSettingsAdded)
                {
                    addIeqSettings();
                    ieqSettingsAdded = true;
                }
                DsAkSettings allSettings = new DsAkSettings(profileSettings);
                defaultProfiles.add(new DsProfileSettings((profileName != null) ? profileName : name, ("The preset loaded for" + name),
                                    allSettings, true, category, (profileIeq != -1) ? profileIeq : 0, profileGebg, null));

                // extract current settings for the selected profile
                currentSettings = xmlParserCurrent.getSettingArray(name, false);
                HashSet<String> savedParams = xmlParserCurrent.getSavedParams();
                currentName = xmlParserCurrent.getProfileSettingName(name);
                currentIeq = xmlParserCurrent.getProfileSettingIeq(name);
                geqSettings = xmlParserCurrent.getGeqSettingArray(name, profileGebg);

                // generate the actual settings based on default settings and current settings
                profileSettings = combineSettings(profileSettings, currentSettings);
                profileIeq = resolveIeqPreset(profileIeq, currentIeq);
                if(DsProfileSettings.Category.CUSTOMIZED == category)
                {
                    profileName = (null != currentName)? currentName : "";
                }
                else
                {
                    profileName = resolveName(profileName, currentName);
                }

                allSettings = new DsAkSettings(profileSettings);
                currentProfiles.add(new DsProfileSettings((profileName != null) ? profileName : name, ("The current settings loaded for" + name),
                                    allSettings, true, category, (profileIeq != -1) ? profileIeq : 0, geqSettings, savedParams));
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString());
                throw new UnsupportedOperationException("Exception in creating profile settings");
            }
        }
        else
        {
            Log.e(TAG, "Constant AK parameters NOT defined, or profile settings NULL.");
            throw new UnsupportedOperationException("Settings are NOT ready yet.");
        }
    }

    /**
     * Creates a new DsProfileSettings instance for off settings parsed from the default configuration files.
     * @internal
     *
     * @throws UnsupportedOperationException
     */
    private static void addOffProfileSettings() throws UnsupportedOperationException
    {
        int[][] profileSettings;
        String profileName;
        int profileIeq;

        // extract default settings for off profile
        profileSettings = xmlParserDefault.getSettingArray("off", true);
        profileName = xmlParserDefault.getProfileSettingName("off");
        profileIeq = xmlParserDefault.getProfileSettingIeq("off");
        if (profileSettings != null  && DsAkSettings.isConstantAkParamsDefined())
        {
            try
            {
                DsAkSettings allSettings = new DsAkSettings(profileSettings);
                offProfile_ = new DsProfileSettings((profileName != null) ? profileName : "off", "The setting used for switching off Ds effect.",
                                                    allSettings, false, DsProfileSettings.Category.CUSTOMIZED, (profileIeq != -1) ? profileIeq : 0, null, null);
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString());
                throw new UnsupportedOperationException("Exception in creating off profile settings");
            }
        }
        else
        {
            Log.e(TAG, "Constant AK parameters NOT defined, or profile settings NULL.");
            throw new UnsupportedOperationException("Settings are NOT ready yet.");
        }
    }

    /** Get the combined two-dimensional array of settings from the specified default and custom settings arrays.
     *
     * @param defaultSetting The default two-dimension integer array for setting a certain profile.
     * @param currentSetting The current two-dimension integer array for setting a certain profile.
     *
     * @return The combined two-dimension integer array for setting a certain profile.
     *
     * @internal
     */
    private static int[][] combineSettings(int[][] defaultSettings, int[][] currentSettings)
    {
        int defaultLength = (defaultSettings == null) ? 0 : defaultSettings.length;
        int currentLength = (currentSettings == null) ? 0 : currentSettings.length;
        int settingLength = defaultLength + currentLength;

        if (settingLength == 0)
            return null;

        int[][] settingArray = new int[settingLength][];

        if (defaultLength != 0)
            System.arraycopy(defaultSettings, 0, settingArray, 0, defaultLength);
        if (currentLength != 0)
            System.arraycopy(currentSettings, 0, settingArray, defaultLength, currentLength);

        return settingArray;
    }

    /** Get the actual profile name given the default and current profile names.
     *
     * @param defaultSetting The default shown names for a certain profile.
     * @param currentSetting The current shown names for a certain profile.
     *
     * @return The actual shown names for a certain profile.
     *
     * @internal
     */
    private static String resolveName(String defaultName, String currentName)
    {
        return ((currentName != null) ? currentName : defaultName);
    }

    /** Get the actual IEQ index given the default and current IEQ indices.
     *
     * @param defaultSetting The default IEQ index for a certain profile.
     * @param currentSetting The current IEQ index for a certain profile.
     *
     * @return The actual IEQ index for a certain profile.
     *
     * @internal
     */
    private static int resolveIeqPreset(int defaultIeq, int currentIeq)
    {
        return ((currentIeq != -1) ? currentIeq : defaultIeq);
    }
}

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

import android.app.Activity;
import android.os.Bundle;
import java.util.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.util.Log;

import android.dolby.DsClientSettings;
import android.dolby.DsConstants;
import android.dolby.DsCommon;
import android.dolby.DsLog;

public class DsConfigParser
{
    private static final String TAG = "DsConfigParser";

    private static final int ASCII_TAB_LF = 10;
    private static final int ASCII_TAB_CR = 13;
    private static final int ASCII_TAB_SPACE = ' ';
    private static final int ASCII_TAB_EQUAL = '=';
    private static final int ASCII_TAB_COMMA = ',';
    private static final int ASCII_TAB_LEFT_BRACKET = '[';
    private static final int ASCII_TAB_RIGHT_BRACKET = ']';

    private static final int TUNING_MAX_OFFSET = 329;

    private static final int NO_ERROR = 0;
    // minor problems in xml file and DS will be enabled.
    private static final int ERROR_REDUNDANT_PROFILE = 0x1000;
    private static final int ERROR_REDUNDANT_IEQ = 0x2000;
    private static final int ERROR_REDUNDANT_OFF = 0x4000;
    // serious problems in xml file and DS will be disabled.
    private static final int ERROR_MISSING_PROFILE = 0x01;
    private static final int ERROR_MISSING_IEQ = 0x02;
    private static final int ERROR_MISSING_OFF = 0x04;
    private static final int ERROR_MISSING_PARAM = 0x08;
    private static final int ERROR_INVALID_PARAM_NAME = 0x10;
    private static final int ERROR_INVALID_PARAM_VALUE = 0x20;
    private static final int ERROR_INVALID_PARAM_LEN = 0x40;

    // hashmap used to map from string name to integer index
    private static LinkedHashMap<String, Integer> profileDefinitions = new LinkedHashMap<String, Integer>();
    private static LinkedHashMap<String, Integer> ieqDefinitions = new LinkedHashMap<String, Integer>();

    /** Hashmap represents whether the settable parameters are found in xml file or not.
     * @internal
     */
    private LinkedHashMap<String, Boolean> akParamsFound_ = new LinkedHashMap<String, Boolean>();

    static
    {
        for (int i = 0; i < DsCommon.PROFILE_NAMES_XML.length; i++)
            profileDefinitions.put(DsCommon.PROFILE_NAMES_XML[i], i);

//        Log.i(TAG, "DsCommon.IEQ_PRESET_NAMES_XML.length: " + DsCommon.IEQ_PRESET_NAMES_XML.length);
        for (int i = 0; i < DsCommon.IEQ_PRESET_NAMES_XML.length; i++)
        {
            ieqDefinitions.put(DsCommon.IEQ_PRESET_NAMES_XML[i], i);
            for (int j = 0; j < DsCommon.PROFILE_NAMES_XML.length; j++)
                ieqDefinitions.put(DsCommon.GEQ_NAMES_XML[j][i], i);
        }
    }

    private int parserErrorFlag = NO_ERROR;

    private String tagName = null;
    private String parameterType = null;
    private String parameterId = null;
    private String parameterName = null;
    private String parameterPreset = null;
    private String parameterDev = null;
    private String tunedRate = null;
    private String parameterValue = null;

    // class to store profile setting TAG parsed from XML
    private class ProfileSettings
    {
        String displayName;
        String ieqId;
        String device;
        String settingStr;
    }
    private HashMap<String, ProfileSettings> mapProfile = new HashMap<String, ProfileSettings>();
    // class to store IEQ setting TAG parsed from XML
    private class EqualizerSettings
    {
        String device;
        String settingStr;
    }
    private HashMap<String, EqualizerSettings> mapEqualizer = new HashMap<String, EqualizerSettings>();

    // string to store tuning and constant setting TAG parsed from XML
    private class DeviceSettings
    {
        String device;
        String settingStr;
    }
    private HashMap<String, DeviceSettings> mapDevice = new HashMap<String, DeviceSettings>();

    // default GEQ band gain
    private int[] defaultGeqBandGain_;

    /**
     * Constructor to parse the input stream of the passed XML file by pull parsing.
     *
     * @param is The input stream of the XML file containing the ds settings to be parsed.
     * @param useOffProfileForDsOff Indicates that the 'off' profile is being used when DS is turned off.
     * @throws IllegalArgumentException on parsing errors.
     */
    public DsConfigParser(InputStream is, boolean useOffProfileForDsOff)
    {
        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(is, "UTF-8");

            boolean tagFlag = false;
            int eventType = xpp.getEventType();
            int paraCount = 0;

            try
            {
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    switch (eventType)
                    {
                        case XmlPullParser.END_TAG:
                            tagFlag = false;
                            break;
                        case XmlPullParser.TEXT:
                            if (tagFlag)
                            {
                                if (tagName.equals("data"))
                                    parameterValue = xpp.getText();

                                if (parameterValue != null)
                                    parseParameters();
                             }
                            break;
                        case XmlPullParser.START_TAG:
                            tagFlag = true;
                            tagName = xpp.getName();

                            if ((tagName.equals("preset")) || (tagName.equals("profile")) ||
                                (tagName.equals("tuning")) || (tagName.equals("constant")))
                            {
                                String nameAttri, valueAttri;
                                int count = xpp.getAttributeCount();

                                parameterType = tagName;
                                parameterId = null;
                                parameterName = null;
                                parameterDev = null;
                                parameterPreset = null;
                                parameterValue = null;
                                for (int i = 0; i < count; i++)
                                {
                                    nameAttri = xpp.getAttributeName(i);
                                    valueAttri = xpp.getAttributeValue(i);
//                                    Log.i(TAG, "Name: " + nameAttri);
//                                    Log.i(TAG, "Value: " + valueAttri);

                                    if (nameAttri.equals("id"))
                                        parameterId = valueAttri;
                                    if (nameAttri.equals("name"))
                                        parameterName = valueAttri;
                                    if (nameAttri.equals("endpoint"))
                                        parameterDev = valueAttri;
                                    if (nameAttri.equals("tuned_rate"))
                                        tunedRate = valueAttri;
                                }
                            }

                            if (tagName.equals("include"))
                            {
                                parameterPreset = xpp.getAttributeValue(0);
                                parseParameters();
                            }
                            break;
                        default:
                            break;
                    }
                    eventType = xpp.next();
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "xmlConfigParsing(): error occurred while parsing xml file");
                throw new IllegalArgumentException("Invalid ds settings");
            }
        }
        catch (XmlPullParserException e)
        {
            Log.e(TAG, "xmlConfigParsing(): error occurred while parsing xml file");
            throw new IllegalArgumentException("Invalid ds settings");
        }

        checkConfigValidity(useOffProfileForDsOff);
    }

    /**
     * To generate the tables for different settings(IEQ settings, profile setting and device settings).
     * @internal
     */
    private void parseParameters()
    {
        // Hashmap for IEQ settings
        EqualizerSettings currentSettings = new EqualizerSettings();
        String preIeq = (parameterId != null) ? parameterId.substring(0, 3) : null;
        if (("preset".equals(parameterType)) && (("ieq".equals(preIeq)) || ("geq".equals(preIeq))))
        {
            currentSettings.device = parameterDev;
            currentSettings.settingStr = parameterValue;
            mapEqualizer.put(parameterId, currentSettings);
//            Log.i(TAG, "IEQsize: " + mapEqualizer.size());
            return;
        }

        // Hashmap for profile settings
        ProfileSettings currentProfileSettings = new ProfileSettings();
        if (("profile".equals(parameterType)) &&
            ((DsCommon.PROFILE_NAMES_XML[0].equals(parameterId)) ||
            (DsCommon.PROFILE_NAMES_XML[1].equals(parameterId)) ||
            (DsCommon.PROFILE_NAMES_XML[2].equals(parameterId)) ||
            (DsCommon.PROFILE_NAMES_XML[3].equals(parameterId)) ||
            (DsCommon.PROFILE_NAMES_XML[4].equals(parameterId)) ||
            (DsCommon.PROFILE_NAMES_XML[5].equals(parameterId)) ||
            ("off".equals(parameterId))))
        {
            currentProfileSettings.displayName = parameterName ;
            currentProfileSettings.ieqId = parameterPreset;
            currentProfileSettings.device = parameterDev;
            currentProfileSettings.settingStr = parameterValue;
            mapProfile.put(parameterId, currentProfileSettings);
//            Log.i(TAG, "Profilesize: " + mapProfile.size());
            return;
        }

        // Hashmap for tunning parameters
        DeviceSettings currentDeviceSettings = new DeviceSettings();
        if (("tuning".equals(parameterType)) || ("constant".equals(parameterType)))
        {
            currentDeviceSettings.device = parameterDev;
            currentDeviceSettings.settingStr = parameterValue;
            mapDevice.put(parameterType, currentDeviceSettings);
//            Log.i(TAG, "Devicesize: " + mapDevice.size());
        }

    }

    /**
     * Check if the XML file is in the required format or the data is valid or not.
     *
     * @return The status of parsing the XML file. true for success, and false for fail.
     */
    public boolean getParserStatusFlag()
    {
        boolean ret = true;

        DsLog.log1(TAG, "The parsing result of the configuration file shows below:");

        if (parserErrorFlag == NO_ERROR)
        {
            DsLog.log1(TAG, "No errors were found when parsing configuration file.");
        }
        else
        {
            if ((parserErrorFlag & ERROR_REDUNDANT_PROFILE) != 0)
                Log.w(TAG, "More profiles were specified in configuration file than expected.");
            if ((parserErrorFlag & ERROR_REDUNDANT_IEQ) != 0)
                Log.w(TAG, "More IEQ presets were specified in configuration file than expected.");
            if ((parserErrorFlag & ERROR_REDUNDANT_OFF) != 0)
                Log.w(TAG, "Off profile was specified in configuration file but is not expected.");
            if ((parserErrorFlag & ERROR_MISSING_PROFILE) != 0)
            {
                Log.e(TAG, "Not all expected profiles were specified in configuration file");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_MISSING_IEQ) != 0)
            {
                Log.e(TAG, "Not all expected IEQ presets were specified in configuration file");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_MISSING_OFF) != 0)
            {
                Log.e(TAG, "Off profile was expected but NOT specified in configuration file");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_MISSING_PARAM) != 0)
            {
                Log.e(TAG, "Some AK parameters were missing in configuration file");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_INVALID_PARAM_NAME) != 0)
            {
                Log.e(TAG, "Parameter name parsed from configuration file was not valid or in the required format");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_INVALID_PARAM_VALUE) != 0)
            {
                Log.e(TAG, "Parameter value parsed from configuration file was not valid or in the required format");
                ret = false;
            }
            if ((parserErrorFlag & ERROR_INVALID_PARAM_LEN) != 0)
            {
                Log.e(TAG, "The length of data specified for the AK parameter is inconsistent"
                        + " with the related AK parameter that determines the expected length.");
                ret = false;
            }
        }

        if (!ret)
            Log.e(TAG, "Parsing has failed, DS will be disabled! Please correct the errors in configuration file");

        return ret;
    }

    /**
     * To check if the settings in the default.xml are valid or not.
     * @param useOffProfileForDsOff Indicates that the 'off' profile is being used when DS is turned off.
     * @internal
     */
    private void checkConfigValidity(boolean useOffProfileForDsOff)
    {
        int i = 0;
        int requiredProfileNum = DsConstants.PROFILE_INDEX_MAX;
        int requiredIeqNum = DsConstants.IEQ_PRESET_INDEX_MAX;

        for (i = 0; i <= requiredProfileNum; i++)
        {
            if (mapProfile.get(DsCommon.PROFILE_NAMES_XML[i]) == null)
            {
                // the required profile setting is missing
                parserErrorFlag |= ERROR_MISSING_PROFILE;
            }
        }

        for (i = 1; i <= requiredIeqNum; i++)
        {
            if (mapEqualizer.get(DsCommon.IEQ_PRESET_NAMES_XML[i]) == null)
            {
                // the required IEQ preset is missing
                parserErrorFlag |= ERROR_MISSING_IEQ;
            }
        }

        if (useOffProfileForDsOff)
        {
            requiredProfileNum++;
            if (mapProfile.get("off") == null)
            {
                // "off" profile setting is needed in this mode
                parserErrorFlag |= ERROR_MISSING_OFF;
            }
        }
        else
        {
            // minor problem, "off" profile setting is not needed in this mode
            if (mapProfile.get("off") != null)
            {
                parserErrorFlag |= ERROR_REDUNDANT_OFF;
            }
        }

        if (mapProfile.size() > (requiredProfileNum + 1))
        {
            // minor problem, more profiles are available
            parserErrorFlag |= ERROR_REDUNDANT_PROFILE;
        }

        if (mapEqualizer.size() > requiredIeqNum)
        {
            // minor problem, more IEQ presets are available
            parserErrorFlag |= ERROR_REDUNDANT_IEQ;
        }
    }

    /**
     * To get the two-dimension integer array for setting a certain profile.
     * @param profile The profile name(music, movie, voice, game, user1 and user2).
     * @return The two-dimension integer array used for profile setting.
     * @internal
     */
    private int[][] getProfileSettingArray(String profile)
    {
        Vector<int[]> settingList = new Vector<int[]>();
        ProfileSettings currentProfileSettings = new ProfileSettings();
        currentProfileSettings = mapProfile.get(profile);

        if (currentProfileSettings == null)
            return null;

        DsLog.log1(TAG, "profile settingStr: " + currentProfileSettings.settingStr);

        settingList = parseSettingGroup(currentProfileSettings.settingStr);
        if (settingList == null)
            return null;

        DsLog.log1(TAG, "profile setting list size: " + settingList.size());
        return settingList.toArray(new int[settingList.size()][]);
    }

    /**
     * To get the displayed name of a certain profile.
     * @param profile The profile name(music, movie, voice, game, user1 and user2).
     * @return The displayed name of the input profile.
     */
    public String getProfileSettingName(String profile)
    {
        ProfileSettings currentProfileSettings = new ProfileSettings();
        currentProfileSettings = mapProfile.get(profile);

        if (currentProfileSettings == null)
            return null;

        DsLog.log1(TAG, "displayName: " + currentProfileSettings.displayName);
        return  currentProfileSettings.displayName;

    }

    /**
     * To get the IEQ setting of a certain profile.
     * @param profile The profile name(music, movie, voice, game, user1 and user2)
     * @return The index number corresponding to the DsCommon.IEQ_PRESET_NAMES_XML[] table.
     */
    public int getProfileSettingIeq(String profile)
    {
        ProfileSettings currentProfileSettings = new ProfileSettings();
        currentProfileSettings = mapProfile.get(profile);

        if (currentProfileSettings == null)
            return -1;

        DsLog.log1(TAG, "ieqId: " + currentProfileSettings.ieqId);
        Integer index = ieqDefinitions.get(currentProfileSettings.ieqId);

        return index == null ? -1 : index.intValue();
    }

    /**
     * To get the two-dimension integer array for tuning setting.
     * @return The two-dimension integer array used for tuning setting.
     * @internal
     */
    private int[][] getTuningSettingArray()
    {
        Vector<int[]> settingList = new Vector<int[]>();
        String settingStr;
        DeviceSettings deviceTuningSettings;
        DeviceSettings deviceConstantSettings;
        deviceTuningSettings = mapDevice.get("tuning");
        deviceConstantSettings = mapDevice.get("constant");

        if (deviceTuningSettings == null)
        {
            settingStr = (deviceConstantSettings == null) ? null : deviceConstantSettings.settingStr;
        }
        else
        {
            settingStr = (deviceConstantSettings == null) ? deviceTuningSettings.settingStr : (deviceTuningSettings.settingStr + deviceConstantSettings.settingStr);
        }

        if (settingStr == null)
            return null;

        DsLog.log1(TAG, "tuning settingStr: " + settingStr);

        settingList = parseSettingGroup(settingStr);
        if (settingList == null)
            return null;

        DsLog.log1(TAG, "device setting list size: " + settingList.size());
        return settingList.toArray(new int[settingList.size()][]);
    }

    /**
     * To get the two-dimension integer array for setting a certain profile(combine the profile setting and the tuning setting).
     *
     * @param profile          The profile name(music, movie, voice, game, user1 and user2).
     * @param requireAllParams Indicates whether all AK parameters must be specified in the settings.
     * @return The two-dimension integer array used for setting.
     */
    public int[][] getSettingArray(String profile, boolean requireAllParams)
    {
        // Before parsing the settings, reset the found flag to false for all the settable parameters.
        // Note that all the settable AK parameters are defined in DsAkSettings class,
        // and their names defined by "akSettableParamDefinitions"
        Object[] settableParamNames = DsAkSettings.akSettableParamDefinitions.toArray();
        for (int i = 0; i < settableParamNames.length; i++)
        {
            akParamsFound_.put((String)settableParamNames[i], false);
        }

        int[][] tuningArray = getTuningSettingArray();
        int[][] profileArray = getProfileSettingArray(profile);

        int profileLength = (profileArray == null) ? 0 : profileArray.length;
        int tuningLength = (tuningArray == null) ? 0 : tuningArray.length;
        int settingLength = profileLength + tuningLength;

        if (settingLength == 0)
            return null;

        if (requireAllParams)
        {
            // Normally requireAllParams is true on parsing ds1-default.xml, which require all parameters to be specified.
            // And it is false on parsing ds1-current.xml, which does not require all parameters to be specified.
            for (String paramName : akParamsFound_.keySet())
            {
                // iebt is parsed separately, so ignore the iebt parameter.
                // ignore the missing of lcmf paramter since it is NOT necessary.
                if (!akParamsFound_.get(paramName).booleanValue() && !paramName.equals("lcmf") && !paramName.equals("iebt"))
                {
                    Log.e(TAG, "AK parameter " + paramName + " missing in xml file!");
                    parserErrorFlag |= ERROR_MISSING_PARAM;
                }
            }
        }

        int[][] settingArray = new int[settingLength][];

        if (profileLength != 0)
            System.arraycopy(profileArray, 0, settingArray, 0, profileLength);
        if (tuningLength != 0)
            System.arraycopy(tuningArray, 0, settingArray, profileLength, tuningLength);

        DsLog.log1(TAG, "total setting list size: " + settingArray.length);

        return settingArray;
    }

    /**
     * To get the IEQ setting.
     * @return The array for initiating the IEQ settings.
     */
    public int[][] getIeqSettingArray()
    {
        int len = DsAkSettings.getParamArrayLength("iebt");
        return equalizerSettingArray(DsCommon.IEQ_PRESET_NAMES_XML, len, null);
    }

    /**
     * To get the GEQ setting of a certain profile.
     * @param profile The profile name(music, movie, voice, game, user1 and user2).
     * @param defaultGebg User input default geq setings.
     * @return The array for initiating the GEQ settings.
     */
    public int[][] getGeqSettingArray(String profile, int[][] defaultGebg)
    {
        int len = DsAkSettings.getParamArrayLength("gebg");
        return equalizerSettingArray(DsCommon.GEQ_NAMES_XML[profileDefinitions.get(profile)], len, defaultGebg);
    }

    /**
     * To get the two-dimension integer array for equalizer(geq/ieq) setting.
     * @internal
     *
     * @param paramNames The names array of equalizer(geq/ieq) setting.
     * @param length     The array length of equalizer(geq/ieq) setting.
     * @param userDefaultGebg User input default geq setings.
     * @return The two-dimension integer array used for equalizer(geq/ieq) setting.
     */
    private int[][] equalizerSettingArray(String[] paramNames, int length, int[][] userDefaultGebg)
    {
        Vector<int[]> eqList = new Vector<int[]>();
        EqualizerSettings currentSettings = new EqualizerSettings();

        for (int i = 0; i < DsConstants.IEQ_PRESETS_NUMBER; i++)
        {
            currentSettings = mapEqualizer.get(paramNames[i]);
            if (currentSettings == null)
            {
                // 1. Use the user input default GEQ band gain
                // 2. Use local[(parseSettingGroup()] stored default GEQ band gain
                // 3. Use the all zero GEQ band gain
                if (paramNames[i].substring(0,3).equalsIgnoreCase("geq"))
                {
                    if (userDefaultGebg != null)
                    {
                        eqList.add(userDefaultGebg[i]);
                    }
                    else if (defaultGeqBandGain_ != null)
                    {
                        eqList.add(defaultGeqBandGain_);
                    }
                    else
                    {
                        eqList.add(new int[length]);
                    }
                }
                else
                {
                    eqList.add(new int[length]);
                }
            }
            else
            {
                String settingGroup = currentSettings.settingStr;
                String parameter = null;
                String value = null;
                int[] actualSettings;
                int start = 0, end = 0;
                int spaceCount = 0;
                int arrayLength = settingGroup.length();

//                Log.i(TAG, "string: " + settingGroup);

                // skip "line ending" and "space" character
                while ((settingGroup.charAt(end) == ASCII_TAB_LF) || (settingGroup.charAt(end) == ASCII_TAB_CR) || (settingGroup.charAt(end) == ASCII_TAB_SPACE))
                {
                    end++;
                }
                start = end;
                while (end < arrayLength)
                {
                    boolean isParamFound = false;
                    spaceCount = 0;
                    while (settingGroup.charAt(end) != ASCII_TAB_EQUAL)
                    {
                        if (settingGroup.charAt(end) == ASCII_TAB_SPACE)
                            spaceCount++;
                        end++;
                    }
                    parameter = settingGroup.substring(start, end - spaceCount);

                    if (!parameter.equals("iebt") && !parameter.equals("gebg"))
                    {
                        // the parameter name parsed from the xml file is not a valid one
                        Log.e(TAG, "Unexpected parameter name " + parameter + " for equalizer settings");
                        parserErrorFlag |= ERROR_INVALID_PARAM_NAME;
                    }
                    else
                    {
                        isParamFound = true;
                    }

                    while (settingGroup.charAt(end) != ASCII_TAB_LEFT_BRACKET)
                    {
                        end++;
                    }
                    start = end ;
                    while (settingGroup.charAt(end) != ASCII_TAB_RIGHT_BRACKET)
                    {
                        end++;
                    }
                    end++;

                    if (isParamFound)
                    {
                        value = settingGroup.substring(start, end);
                        actualSettings = convertStringArray(value);
                        if (actualSettings != null)
                        {
                            if (!DsAkSettings.isAkParamLengthValid(parameter, actualSettings.length))
                            {
                                parserErrorFlag |= ERROR_INVALID_PARAM_LEN;
                            }
                            eqList.add(actualSettings);
                        }
                        else
                        {
                            // Skip this parameter containing invalid values and continue to parse the subsequent AK parameter
                            Log.e(TAG, "The values for AK parameter " + parameter + " are invalid");
                        }
                    }

                    // skip "line ending" and "space" character
                    if (end != arrayLength)
                    {
                        while ((settingGroup.charAt(end) == ASCII_TAB_LF) || (settingGroup.charAt(end) == ASCII_TAB_CR) || (settingGroup.charAt(end) == ASCII_TAB_SPACE))
                        {
                            end++;
                            if (end == arrayLength)
                                break;
                        }
                        start = end;
                    }
                }
            }
        }

//        Log.i(TAG, "IEQ list size: " + eqList.size());
        return eqList.toArray(new int[eqList.size()][]);
    }

    /**
     * To convert the formatted string to an integer array.
     * @param valueStr Convert the formatted string(eg. [10, 22, 35, 8, ..., 27, 12]) to an integer array
     * @return The converted integer array.
     * @internal
     */
    private int[] convertStringArray(String valueStr)
    {
        int start = 1, end = 1;
        int[] value = new int[TUNING_MAX_OFFSET];
        int count = 0;
        int spaceCount = 0;
        int arrayLength = valueStr.length();

        while (valueStr.charAt(end) == ASCII_TAB_SPACE)
        {
            end++;
        }
        start = end;
        while (end < arrayLength)
        {
            spaceCount = 0;
            while ((valueStr.charAt(end) != ASCII_TAB_COMMA) && (valueStr.charAt(end) != ASCII_TAB_RIGHT_BRACKET))
            {
                if (valueStr.charAt(end) == ASCII_TAB_SPACE)
                    spaceCount++;
                end++;
            }

            try
            {
                value[count] = Integer.parseInt(valueStr.substring(start, end - spaceCount));
            }
            catch (Exception ex)
            {
                // the parameter value parsed from the xml file is not a valid one
                parserErrorFlag |= ERROR_INVALID_PARAM_VALUE;
                return null;
            }
            count++;

            end++;
            // skip "space" character between each element
            if (end != arrayLength)
            {
                while (valueStr.charAt(end) == ASCII_TAB_SPACE)
                {
                    end++;
                }
                start = end;
            }
        }

        int[] settingValue = new int[count];
        System.arraycopy(value, 0, settingValue, 0, count);

        return settingValue;
    }

    /**
     * To parse each setting element from the XML tag string group
     * @param valueStr Convert the formatted string(eg. [10, 22, 35, 8, ..., 27, 12]) to an integer array
     * @return The setting array.
     * @internal
     */
    private Vector<int[]> parseSettingGroup(String settingGroup)
    {
        if (settingGroup == null)
            return null;
        Vector<int[]> settingList = new Vector<int[]>();

        int parameter = 0, offset = 0, value = 0;
        int start = 0, end = 0;
        String paraName, paraValue;
        int spaceCount = 0;
        int arrayLength = settingGroup.length();
        int[] actualSettings;

        // skip "line ending" and "space" character
        while ((settingGroup.charAt(end) == ASCII_TAB_LF) || (settingGroup.charAt(end) == ASCII_TAB_CR) || (settingGroup.charAt(end) == ASCII_TAB_SPACE))
        {
            end++;
        }
        start = end;
        while (end < arrayLength)
        {
            boolean isParamFound = false;
            spaceCount = 0;
            while (settingGroup.charAt(end) != ASCII_TAB_EQUAL)
            {
                if (settingGroup.charAt(end) == ASCII_TAB_SPACE)
                    spaceCount++;
                end++;
            }
            paraName = settingGroup.substring(start, end - spaceCount);

            if (DsAkSettings.akSettableParamDefinitions.contains(paraName))
            {
                parameter = DsAkSettings.getAkParamIndex(paraName);
                akParamsFound_.put(paraName, true);
                isParamFound = true;
            }
            else
            {
                // the parameter name parsed from the xml file is an unexpected one.
                Log.e(TAG, "Unexpected AK parameter name " + paraName);
                parserErrorFlag |= ERROR_INVALID_PARAM_NAME;
            }

            while (settingGroup.charAt(end) != ASCII_TAB_LEFT_BRACKET)
            {
                end++;
            }
            start = end ;
            while (settingGroup.charAt(end) != ASCII_TAB_RIGHT_BRACKET)
            {
                end++;
            }
            end++;

            if (isParamFound)
            {
                paraValue = settingGroup.substring(start, end);
                actualSettings = convertStringArray(paraValue);
                if (actualSettings != null)
                {
                    if (!DsAkSettings.isConstantAkParamsDefined() &&
                        (paraName.equalsIgnoreCase("genb") ||
                         paraName.equalsIgnoreCase("aonb") ||
                         paraName.equalsIgnoreCase("ienb") ||
                         paraName.equalsIgnoreCase("gebf")) )
                    {
                        DsAkSettings.setConstantAkParam(paraName, actualSettings);
                    }
                    else if (!DsAkSettings.isAkParamLengthValid(paraName, actualSettings.length))
                    {
                        parserErrorFlag |= ERROR_INVALID_PARAM_LEN;
                    }

                    for (int i = 0; i < actualSettings.length; i++)
                        settingList.add(new int[] {parameter, i, actualSettings[i]});

                    // Stored default GEQ band gain to local
                    if (paraName.equalsIgnoreCase("gebg"))
                    {
                        defaultGeqBandGain_ = actualSettings;
                    }
                }
                else
                {
                    // Skip this parameter containing invalid values and continue to parse the subsequent AK parameter.
                    Log.e(TAG, "The values for AK parameter " + paraName + " are invalid");
                }
            }

            // skip "line ending" and "space" character
            if (end != arrayLength)
            {
                while ((settingGroup.charAt(end) == ASCII_TAB_LF) || (settingGroup.charAt(end) == ASCII_TAB_CR) || (settingGroup.charAt(end) == ASCII_TAB_SPACE))
                {
                    end++;
                    if (end == arrayLength)
                        break;
                }
                start = end;
            }
        }

        return settingList;
    }

    /**
     * Get all the parameters that are saved in the xml file.
     * This is expected to be called right after all the settings array is got for parsing ds1-current.xml.
     *
     * @return The parameters that are saved in the xml file.
     */
    public HashSet<String> getSavedParams()
    {
        HashSet<String> savedParams = new HashSet<String>(DsClientSettings.basicProfileParams);

        // Take down all the parameters that are saved in ds1-current.xml.
        for (String paramName : akParamsFound_.keySet())
        {
            // gebg must be excluded here because a redundant gebg exists in the <profile> tag of ds1-current.xml, which
            // is already defined in <preset> tag. The redundant gebg values must not be used (they default to all 0s).
            if (akParamsFound_.get(paramName).booleanValue() && !paramName.equals("gebg"))
            {
                savedParams.add(paramName);
            }
        }
        return savedParams;
    }
}

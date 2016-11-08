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

import java.util.*;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.dolby.DsConstants;
import android.dolby.DsCommon;
import android.dolby.DsLog;

public class DsStoreUtil
{

    private static final String TAG = "DsStoreUtil";

    /**
     * Define the profile used if no previous state has been stored.
     * DsConstants.PROFILE_MOVIE should be used for both tablet-form-factor and phone-form-factor devices.
     */
    private static final int DEFAULT_PROFILE = DsConstants.PROFILE_MOVIE;

    // Tag names used for saving DS state
    private static final String TAG_DS_STATE = "DsState";
    private static final String SUBTAG_ONOFF = "DsOn";
    private static final String SUBTAG_PROFILEID = "CurrentProfile";

    // Tag names used for saving profile settings
    private static final String TAG_DS_CURRENT="currentdata";
    private static final String SUBTAG_PRESET = "preset";
    private static final String SUBTAG_PROFILE = "profile";
    private static final String SUBTAG_TUNING = "tuning";
    private static final String SUBTAG_CONSTANT = "constant";
    private static final String SUBTAG_DATA = "data";
    private static final String SUBTAG_INCLUDE = "include";
    private static final String ATTRIBUTE_ID="id";
    private static final String ATTRIBUTE_NAME="name";
    private static final String ATTRIBUTE_DEV="dev";
    private static final String ATTRIBUTE_PRESET="preset";


    private static String DS_CURRENT_SETTINGS_PATH = null;
    private static String DS_STATE_PATH = null;

    /**
     * Store the file paths that save the current DS settings and DS state.
     *
     * @param dsCurSettingsPath The path to save the current DS settings.
     * @param dsStatePath The path to save the current DS state.
     */
    public static void storeDsPath(String dsCurSettingsPath, String dsStatePath)
    {
        DS_CURRENT_SETTINGS_PATH = dsCurSettingsPath;
        DS_STATE_PATH = dsStatePath;
    }

    /**
     * Load DS state from xml file.
     *
     * @return String array used to do the initialization.
     */
    public static String[] loadDsState()
    {
        // first element is for on/off status, the second element is for profile index.
        String[] currentState = {"0", Integer.toString(DEFAULT_PROFILE)}; //disabled dolby as default

        FileInputStream fileis = null;
        try
        {
            fileis = new FileInputStream(DS_STATE_PATH);
        }
        catch (FileNotFoundException e)
        {
            DsLog.log1(TAG, "Cannot find DS state file " + DS_STATE_PATH +", using default value.");
            return currentState;
        }

        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(fileis, "UTF-8");

            int eventType = xpp.getEventType();
            String parameterName = null;
            String parameterValue = null;
            boolean tagFlag = false;

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
                            parameterValue = xpp.getText();
                            DsLog.log2(TAG, "Text: " + parameterValue);
                            if (tagFlag)
                            {
                                if (parameterName.equals(SUBTAG_ONOFF))
                                    currentState[0] = parameterValue;
                                if (parameterName.equals(SUBTAG_PROFILEID))
                                    currentState[1] = parameterValue;
                            }

                            break;
                        case XmlPullParser.START_TAG:
                            tagFlag = true;
                            parameterName = xpp.getName();
                            DsLog.log2(TAG, "Name: " + parameterName);
                            break;
                        default:
                            break;
                    }
                    eventType = xpp.next();
                }
                fileis.close();
            }
            catch (IOException e)
            {
                DsLog.log1(TAG, "Error occurred when parsing" + DS_STATE_PATH +", using default value.");
                return currentState;
            }
        }
        catch (XmlPullParserException e)
        {
            DsLog.log1(TAG, "Erro occurred when parsing " + DS_STATE_PATH +", using default value.");
            try 
            {
                fileis.close();
            }
            catch (IOException e1)
            {
                DsLog.log1(TAG, "Cannot close DS state file " + DS_STATE_PATH);
            }
            return currentState;
        }

        return currentState;
    }

    /**
     * Save DS state to xml file.
     *
     * @param dsState The current Ds effect on/off state.
     * @param currentProfile The profile index that is currently selected.
     */
    public static void saveDsState(String dsState, String currentProfile)
    {
        FileOutputStream fileos = null;
        try
        {
            fileos = new FileOutputStream(DS_STATE_PATH);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Failed to find or load " + DS_STATE_PATH + ", and the file could not be created");
            return;
        }
        XmlSerializer serializer = Xml.newSerializer();
        try
        {
            serializer.setOutput(fileos,"utf-8");
            serializer.text("\n");
            serializer.startTag(null, TAG_DS_STATE);
            serializer.text("\n");
            serializer.comment("Ds on/off state");
            serializer.text("\n");
            serializer.startTag(null, SUBTAG_ONOFF);
            serializer.text(dsState);
            serializer.endTag(null, SUBTAG_ONOFF);
            serializer.text("\n");
            serializer.comment("Profile index");
            serializer.text("\n");
            serializer.startTag(null, SUBTAG_PROFILEID);
            serializer.text(currentProfile);
            serializer.endTag(null, SUBTAG_PROFILEID);
            serializer.text("\n");
            serializer.endTag(null, TAG_DS_STATE);
            serializer.endDocument();
            serializer.flush();
        }
        catch (Exception e)
        {
            Log.e(TAG, "saveDsState(): error occurred while creating xml file");
        }

        try
        {
            if (fileos != null) 
            {
                fileos.close();
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to close file : " + DS_STATE_PATH);
        }

    }

    /**
     * Conver integer array to the String array.
     * @internal
	 *
     * @param integerArray The GEQ integer settings which will be converted to the String.
     * @return The GEQ setting String for the XML file.
     */
    private static String[] convertArray(int[][] integerArray)
    {
        int[] oneIntegerSetting;
        String[] stringArray = new String[integerArray.length];
        int gebfLen = DsAkSettings.getParamArrayLength("gebf");

        for (int i = 0; i < integerArray.length; i++)
        {
            StringBuffer oneStringArray = new StringBuffer();
            oneIntegerSetting = integerArray[i];
            oneStringArray.append("gebg=[");
            for (int j = 0; j < gebfLen - 1; j++)
                oneStringArray.append(String.valueOf(oneIntegerSetting[j]) + ", ");
            oneStringArray.append(String.valueOf(oneIntegerSetting[gebfLen - 1]) + "]");

            stringArray[i] = oneStringArray.toString();
        }

        return stringArray;
    }

    /**
     * Save DS profile settings to xml file.
     *
     * @param currentProfiles The current settings array for all the profiles.
     */
    public static void saveDsProfileSettings(DsProfileSettings[] currentProfiles)
    {
        String geqName[][] = DsCommon.GEQ_NAMES_XML;
        String profileIdName[] = DsCommon.PROFILE_NAMES_XML;
        FileOutputStream fileos = null;
        try
        {
            fileos = new FileOutputStream(DS_CURRENT_SETTINGS_PATH);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Failed to find or load " + DS_CURRENT_SETTINGS_PATH + ", and the file could not be created");
            return;
        }
        XmlSerializer serializer = Xml.newSerializer();
        try
        {
            serializer.setOutput(fileos,"utf-8");
            serializer.text("\n");
            serializer.startTag(null, TAG_DS_CURRENT);
            serializer.text("\n\n");

            for (int profile = 0; profile <= DsConstants.PROFILE_INDEX_MAX; profile++)
            {
                String DsCurrentSettings = currentProfiles[profile].getCurrentProfileSettings();
                String DsCurrentProfileNames =  currentProfiles[profile].getDisplayName();
                int DsCurrentIeqPresets = currentProfiles[profile].getIeqPreset();
                int[][] DsCurrentGeqSettings = currentProfiles[profile].getGeqGainArray();

                if (DsCurrentGeqSettings != null)
                {
                    String[] settingStr = convertArray(DsCurrentGeqSettings);
                    serializer.comment("gebg settings for " + profileIdName[profile] + " profile");
                    serializer.text("\n");
                    for (int index = 0; index <= DsConstants.IEQ_PRESET_INDEX_MAX; index++)
                    {
                        serializer.startTag(null, SUBTAG_PRESET);
                        serializer.attribute(null, ATTRIBUTE_ID, geqName[profile][index]);
                        serializer.text("\n    ");

                        serializer.startTag(null, SUBTAG_DATA);
                        serializer.text(settingStr[index]);
                        serializer.endTag(null, SUBTAG_DATA);
                        serializer.text("\n");

                        serializer.endTag(null, SUBTAG_PRESET);
                        serializer.text("\n");
                    }
                }

                if (DsCurrentSettings != null)
                {
                    serializer.comment("profile settings for " + profileIdName[profile] + " profile");
                    serializer.text("\n");
                    serializer.startTag(null, SUBTAG_PROFILE);
                    serializer.attribute(null, ATTRIBUTE_ID, profileIdName[profile]);
                    serializer.attribute(null, ATTRIBUTE_NAME, DsCurrentProfileNames);
                    serializer.text("\n    ");

                    serializer.startTag(null, SUBTAG_DATA);
                    serializer.text(DsCurrentSettings);
                    serializer.endTag(null, SUBTAG_DATA);
                    serializer.text("\n    ");

                    serializer.startTag(null, SUBTAG_INCLUDE);
                    serializer.attribute(null, ATTRIBUTE_PRESET, geqName[profile][DsCurrentIeqPresets]);
                    serializer.endTag(null, SUBTAG_INCLUDE);
                    serializer.text("\n");

                    serializer.endTag(null, SUBTAG_PROFILE);
                    serializer.text("\n\n");
                }
            }
            serializer.endTag(null, TAG_DS_CURRENT);
            serializer.endDocument();
            serializer.flush();
            fileos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, "saveDsProfileSettings(): error occurred while saving the current DS profile settings");
        }
    }
}

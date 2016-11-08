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
 * DsAkSettings.java
 *
 * Internal class that manages groups of Ds AK settings for use
 * with Ds instance.
 *
 * This class defines what Ds AK settings will be available for
 * configuration by the Ds class presets.
 */

package com.dolby.ds;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.lang.String;
import android.util.Log;

import com.dolby.api.DsLog;

public class DsAkSettings
{
    /**
     * Helper class to encapsulate a valid parameter.
     */
    static public class ParameterDefn
    {
        public ParameterDefn (String name, int len, int lowerBound, int upperBound)
        {
            this.paramName  = name;
            this.len        = len;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
        public String          paramName;   // The parameter name
        public int             len;         // The array length of the parameter
        public int             lowerBound;  // The lower bound of the parameter value
        public int             upperBound;  // The upper bound of the parameter value
        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof ParameterDefn))
                return false;
            return ( ((ParameterDefn)other).paramName.equalsIgnoreCase(paramName) && ((ParameterDefn)other).len == len &&
                     ((ParameterDefn)other).lowerBound == lowerBound && ((ParameterDefn)other).upperBound == upperBound );
        }

        @Override
        public int hashCode()
        {
            return akAllParamDefinitions_.get(paramName).intValue();
        }
    }

    /**
     * Helper class to encapsulate an allowed parameter-offset combination.
     */
    static protected class SettingDefn
    {
        public SettingDefn (String parameter, int offset)
        {
            this.parameter = parameter;
            this.offset    = offset;
        }
        public String parameter;
        public int offset;
        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof SettingDefn))
                return false;
            return ((SettingDefn)other).parameter.equals(parameter) && ((SettingDefn)other).offset == offset;
        }

        @Override
        public int hashCode()
        {
            return (getAkParamIndex(parameter) * akParams_.length) + offset;
        }
    }

    private static final String LOG_TAG = "DsAkSettings";

    /**
     * AK parameters. All the AK parameters are hard-coded here, including their names, lengths, upper/lower bounds.
     * They are classified into 5 groups: Profile, Configuration, Tuning, Internal, Read-only.
     *   The parameters in Profile group are settable from client GUI.
     *   The parameters in Configuration group can be re-configured.
     *   The parameters in Tuning group are for tuning only.
     *   The parameters in Internal group are managed internally.
     *   The parameters in Read-only group can only be retrieved, but not settable.
     */
    private static final ParameterDefn akParams_[] = {
        new ParameterDefn("bver", 5,   -32768, 32767),
        new ParameterDefn("bndl", 2,   -32768, 32767),
        new ParameterDefn("ocf" , 1,   0, 5),
        new ParameterDefn("preg", 1,   -2080, 480),
        new ParameterDefn("vdhe", 1,   0, 2),
        new ParameterDefn("vspe", 1,   0, 2),
        new ParameterDefn("dssf", 1,   20, 20000),
        new ParameterDefn("dvli", 1,   -640, 0),
        new ParameterDefn("dvlo", 1,   -640, 0),
        new ParameterDefn("dvle", 1,   0, 1),
        new ParameterDefn("dvmc", 1,   -320, 320),
        new ParameterDefn("dvme", 1,   0, 1),
        new ParameterDefn("ienb", 1,   1, 40),
        new ParameterDefn("iebf", 20,  20, 20000),
        new ParameterDefn("ieon", 1,   0, 1),
        new ParameterDefn("deon", 1,   0, 1),
        new ParameterDefn("ngon", 1,   0, 2),
        new ParameterDefn("geon", 1,   0, 1),
        new ParameterDefn("genb", 1,   1, 40),
        new ParameterDefn("gebf", 20,  20, 20000),
        new ParameterDefn("aonb", 1,   1, 40),
        new ParameterDefn("aobf", 40,  20, 20000),
        new ParameterDefn("aobg", 329, -480, 480),
        new ParameterDefn("aoon", 1,   0, 2),
        new ParameterDefn("arnb", 1,   1, 40),
        new ParameterDefn("arbf", 40,  20, 20000),
        new ParameterDefn("plb" , 1,   0, 288),
        new ParameterDefn("plmd", 1,   0, 4),
        new ParameterDefn("ven" , 1,   0, 1),
        new ParameterDefn("vnnb", 1,   1, 20),
        new ParameterDefn("vnbf", 20,  -32768, 32767),
        new ParameterDefn("vnbg", 20,  -32768, 32767),
        new ParameterDefn("vnbe", 20,  -32768, 32767),
        new ParameterDefn("vcnb", 1,   1, 40),
        new ParameterDefn("vcbf", 20,  20, 20000),
        new ParameterDefn("vcbg", 20,  -192, 576),
        new ParameterDefn("vcbe", 20,  -192, 576),
        new ParameterDefn("ver" , 4,   -32768, 32767),
        new ParameterDefn("pstg", 1,   -2080, 480),
        new ParameterDefn("dhsb", 1,   0, 96),
        new ParameterDefn("dhrg", 1,   -2080, 96),
        new ParameterDefn("dssb", 1,   0, 96),
        new ParameterDefn("dssa", 1,   5, 30),
        new ParameterDefn("dvla", 1,   0, 10),
        new ParameterDefn("iebt", 20,  -480, 480),
        new ParameterDefn("iea" , 1,   0, 16),
        new ParameterDefn("dea" , 1,   0, 16),
        new ParameterDefn("ded" , 1,   0, 16),
        new ParameterDefn("gebg", 20,  -576, 576),
        new ParameterDefn("aocc", 1,   0, 8),
        new ParameterDefn("arbi", 40,  0, 1),
        new ParameterDefn("arbl", 40,  -2080, 0),
        new ParameterDefn("arbh", 40,  -2080, 0),
        new ParameterDefn("arod", 1,   0, 192),
        new ParameterDefn("artp", 1,   0, 16),
        new ParameterDefn("endp", 1,   0, 6),
        new ParameterDefn("mxou", 1,   1, 8),
        new ParameterDefn("vol",  1,   -2048, 480),
        new ParameterDefn("vmon", 1,   0, 2),
        new ParameterDefn("vmb",  1,   0, 240),
        new ParameterDefn("lcmf", 2,   -32768, 32767),
        new ParameterDefn("lcvd", 2,   -32768, 32767),
        new ParameterDefn("lcsz", 1,   1, 32767),
        new ParameterDefn("lcpt", 168, -128, 127)
    };

    /**
     * Ds feature on/off states.
     */
    public static final int
        AK_DS1_FEATURE_OFF = 0,
        AK_DS1_FEATURE_ON = 1,
        AK_DS1_FEATURE_AUTO = 2;

    /**
     * All the settable AK parameter names that are expected to be defined in xml file.
     */
    public static HashSet<String> akSettableParamDefinitions = new HashSet<String>();

    /**
     * The linked hashmap between all the defined AK parameter names and their corresponding indices.
     */
    private static LinkedHashMap<String, Integer> akAllParamDefinitions_ = new LinkedHashMap<String, Integer>();

    /** The linked hashmap between all the defined settable settings and their corresponding indices.
     * @internal
     */
    private static LinkedHashMap<SettingDefn, Integer> settingsDefinitions_ = new LinkedHashMap<SettingDefn, Integer>();

    /** The flag indicating whether the DS settings is defined or not.
     * @internal
     */
    private static boolean constantAkParamsDefined_ = false;

    /** The values of constant AK parameters.
     * @internal
     */
    private static int akParam_genb_ = -1;
    private static int akParam_ienb_ = -1;
    private static int akParam_aonb_ = -1;
    private static int[] akParam_gebf_ = null;
    // aocc value is hardcoded to be 2 which means stereo format.
    private static final int AKPARAM_AOCC = 2;

    /** The default values array for all defined settable AK parameter-offset combinations.
     * @internal
     */
    private static int[] settingsDefaults_;

    /** The current values array for all defined settable AK parameter-offset combinations.
     * @internal
     */
    private int[] values_;

    /**
     * Pre-define the constant AK parameter values, along with the array lengths of related parameters.
     *
     * @param parameter The AK parameter name.
     * @param values    The parameter value/values.
     */
    public static void setConstantAkParam(String parameter, int[] values)
    {
        int i = 0;
        if (parameter.equalsIgnoreCase("genb"))
        {
            DsLog.log1(LOG_TAG, "The number of GEq bands is " + values[0]);
            i = getAkParamIndex("gebf");
            akParams_[i].len = values[0];
            i = getAkParamIndex("gebg");
            akParams_[i].len = values[0];
            i = getAkParamIndex("vcbf");
            akParams_[i].len = values[0];
            i = getAkParamIndex("vcbg");
            akParams_[i].len = values[0];
            i = getAkParamIndex("vcbe");
            akParams_[i].len = values[0];
            akParam_genb_ = values[0];
        }
        else if (parameter.equalsIgnoreCase("ienb"))
        {
            DsLog.log1(LOG_TAG, "The number of IEq bands is " + values[0]);
            i = getAkParamIndex("iebf");
            akParams_[i].len = values[0];
            i = getAkParamIndex("iebt");
            akParams_[i].len = values[0];
            akParam_ienb_ = values[0];
        }
        else if (parameter.equalsIgnoreCase("aonb"))
        {
            DsLog.log1(LOG_TAG, "The number of Audio Optimizer bands is " + values[0]);
            i = getAkParamIndex("aobf");
            akParams_[i].len = values[0];
            i = getAkParamIndex("aobg");
            akParams_[i].len = (values[0] + 1) * AKPARAM_AOCC;
            i = getAkParamIndex("arbf");
            akParams_[i].len = values[0];
            i = getAkParamIndex("arbi");
            akParams_[i].len = values[0];
            i = getAkParamIndex("arbl");
            akParams_[i].len = values[0];
            i = getAkParamIndex("arbh");
            akParams_[i].len = values[0];
            akParam_aonb_ = values[0];
        }
        else if (parameter.equalsIgnoreCase("gebf"))
        {
            DsLog.log1(LOG_TAG, "Initializing the graphic equalizer band center frequencies");
            akParam_gebf_ = values;
        }

        // Define all the allowed settings once all the key parameters are settled down.
        if (!constantAkParamsDefined_ && akParam_genb_ != -1 && akParam_ienb_ != -1 && akParam_aonb_ != -1 && akParam_gebf_ != null)
        {
            defineSettings();
            constantAkParamsDefined_ = true;
        }
    }

    /**
     * Get the band number of graphic equalizer.
     *
     * @return The current graphic equalizer band number.
     */
    public static int getGeqBandCount()
    {
        int bandCount = (int)akParam_genb_;
        return bandCount;
    }

    /**
     * Get the center frequencies of graphic equalizer bands.
     *
     * @return The current graphic equalizer band number in an array.
     */
    public static int[] getGeqBandFrequencies()
    {
        return akParam_gebf_;
    }

    /**
     * Get the number of elements contained in the specified AK parameter.
     *
     * @param parameter The AK parameter name.
     * @return The array length, that is, the number of elements contained in the AK parameter,
     *         and -1 indicates the parameter is not valid.
     */
    public static int getParamArrayLength(String parameter)
    {
        int i = getAkParamIndex(parameter);
        return (i == -1 ? i : akParams_[i].len);
    }

    /**
     * Validate the AK parameter value.
     *
     * @param index The AK parameter index.
     * @param value The AK parameter value to be validated.
     * @return true on a valid value, and false on an invalid one.
     */
    public static boolean isValidParamValue(int index, int value)
    {
        return (value >= akParams_[index].lowerBound && value <= akParams_[index].upperBound);
    }

    /**
     * Check whether the AK parameter is settable at JAVA layer or not.
     * @internal
     *
     * @param parameter The AK parameter name.
     * @return true if the parameter is settable, and false otherwise.
     */
    private static boolean isParamSettable(String parameter)
    {
        if (parameter.equalsIgnoreCase("vdhe") ||
            parameter.equalsIgnoreCase("vspe") ||
            parameter.equalsIgnoreCase("dvle") ||
            parameter.equalsIgnoreCase("dvme") ||
            parameter.equalsIgnoreCase("ngon") ||
            parameter.equalsIgnoreCase("ieon") ||
            parameter.equalsIgnoreCase("deon") ||
            parameter.equalsIgnoreCase("geon") ||
            parameter.equalsIgnoreCase("dhsb") ||
            parameter.equalsIgnoreCase("dhrg") ||
            parameter.equalsIgnoreCase("dssb") ||
            parameter.equalsIgnoreCase("dssa") ||
            parameter.equalsIgnoreCase("dssf") ||
            parameter.equalsIgnoreCase("dvla") ||
            parameter.equalsIgnoreCase("iebt") ||
            parameter.equalsIgnoreCase("iea") ||
            parameter.equalsIgnoreCase("dea") ||
            parameter.equalsIgnoreCase("ded") ||
            parameter.equalsIgnoreCase("gebg") ||
            parameter.equalsIgnoreCase("aoon") ||
            parameter.equalsIgnoreCase("plb") ||
            parameter.equalsIgnoreCase("plmd") ||
            parameter.equalsIgnoreCase("vmon") ||
            parameter.equalsIgnoreCase("vmb") ||
            parameter.equalsIgnoreCase("dvli") ||
            parameter.equalsIgnoreCase("dvlo") ||
            parameter.equalsIgnoreCase("dvmc") ||
            parameter.equalsIgnoreCase("ienb") ||
            parameter.equalsIgnoreCase("iebf") ||
            parameter.equalsIgnoreCase("genb") ||
            parameter.equalsIgnoreCase("gebf") ||
            parameter.equalsIgnoreCase("aonb") ||
            parameter.equalsIgnoreCase("aobf") ||
            parameter.equalsIgnoreCase("aobg") ||
            parameter.equalsIgnoreCase("arnb") ||
            parameter.equalsIgnoreCase("arbf") ||
            parameter.equalsIgnoreCase("aocc") ||
            parameter.equalsIgnoreCase("arbi") ||
            parameter.equalsIgnoreCase("arbl") ||
            parameter.equalsIgnoreCase("arbh") ||
            parameter.equalsIgnoreCase("arod") ||
            parameter.equalsIgnoreCase("artp") )
            return true;
        else
            return false;
    }

    /**
     * Get the index of the specified AK parameter.
     *
     * @param parameter The AK parameter name to be queried.
     * @return The index of the AK parameter in the parameter list, and -1 indicates
     *         the parameter is not valid.
     */
    public static int getAkParamIndex(String parameter)
    {
        Integer i = akAllParamDefinitions_.get(parameter);
        if (i == null)
            Log.e(LOG_TAG, "getAkParamIndex: parameter " + parameter + " not found!");
        return i == null ? -1 : i.intValue();
    }

    /**
     * Get the unique ID of the specified AK parameter.
     *
     * @param parameter The AK parameter name to be queried.
     * @return The unique ID of the AK parameter in the parameter list,
     *          and -1 indicates the parameter is not valid.
     */
    public static int getAkParamId(String parameter)
    {
        Integer i = akAllParamDefinitions_.get(parameter);
        if (i == null)
        {
            Log.e(LOG_TAG, "parameter " + parameter + " unidentified!");
            return -1;
        }
        int paramId = ((int)parameter.charAt(0) << (0 * 8)) +
                      ((int)parameter.charAt(1) << (1 * 8)) +
                      ((int)parameter.charAt(2) << (2 * 8));
        if (parameter.length() == 4)
        {
            paramId += (int)parameter.charAt(3) << (3 * 8);
        }
        return paramId;
    }

    /**
     * Get the index of the specified AK parameter-offset combination.
     *
     * @param parameter The AK parameter name to be queried.
     * @param offset The element index (offset) to be queried.
     * @return The index into the list of AK settings defined by this class corresponding to
     *         the specified parameter and offset combination, and -1 indicates
     *         the parameter-offset combination is not allowed.
     */
    public static int getAkSettingIndex(String parameter, int offset)
    {
        Integer i = settingsDefinitions_.get(new SettingDefn(parameter, offset));
        return i == null ? -1 : i.intValue();
    }

    /**
     * Check whether the settable settings have been defined.
     *
     * @return true on yes, and false on no.
     */
    public static boolean isConstantAkParamsDefined()
    {
        return constantAkParamsDefined_;
    }

    /**
     * Check whether the parameter values length is compatible to the predefined one.
     *
     * @param parameter The AK parameter name.
     * @param length    The parameter values length in the configuration file.
     * @return true on yes, and false on no.
     */
    public static boolean isAkParamLengthValid(String parameter, int length)
    {
        boolean ret = true;
        if (isConstantAkParamsDefined())
        {
            int i = getAkParamIndex(parameter);
            if (length != akParams_[i].len)
            {
                ret = false;
                Log.e(LOG_TAG, "In configuration file, the AK parameter " + parameter + " values length "
                         + length + " is NOT compatible to the defined length " + akParams_[i].len);
            }
        }
        return ret;
    }

    /**
     * Define all the settable settings.
     * @internal
     */
    static private void defineSettings()
    {
        //
        // Define the AK settings (parameters) that will be allowed for setting by the Ds effect.
        // Default values are also set here which will be used to initialize new DsAkSettings objects.
        // Settings other than those defined here will not be able to be set in the Ds effect.
        //
        int paramIndex = 0, elemIndex = 0, nElemPerParam = 0, elemLen = 0;

        //
        // Get the AK parameter array's total length.
        //
        for (paramIndex = 0; paramIndex < akParams_.length; ++paramIndex)
        {
            if (isParamSettable(akParams_[paramIndex].paramName))
                elemLen += akParams_[paramIndex].len;
        }
        settingsDefaults_ = new int[elemLen];

        //
        // Initialise internal collections.
        //
        for (paramIndex = 0; paramIndex < akParams_.length; ++paramIndex)
        {
            if (isParamSettable(akParams_[paramIndex].paramName))
            {
                nElemPerParam = akParams_[paramIndex].len;
                if (nElemPerParam == 1)
                {
                    settingsDefinitions_.put(new SettingDefn(akParams_[paramIndex].paramName, 0), elemIndex);
                    settingsDefaults_[elemIndex] = 0;
                    ++elemIndex;
                }
                else
                {
                    for (int i = 0; i < nElemPerParam; ++i)
                    {
                        settingsDefinitions_.put(new SettingDefn(akParams_[paramIndex].paramName, i), elemIndex);
                        settingsDefaults_[elemIndex] = 0;
                        ++elemIndex;
                    }
                }
            }
        }
    }

    static
    {
        //
        // Define the AK settings (parameters) that will be allowed for setting by the Ds effect.
        //
        for (int paramIndex = 0; paramIndex < akParams_.length; ++paramIndex)
        {
            akAllParamDefinitions_.put(akParams_[paramIndex].paramName, paramIndex);
            if (isParamSettable(akParams_[paramIndex].paramName))
            {
                akSettableParamDefinitions.add(akParams_[paramIndex].paramName);
            }
        }
    }

    /**
     * Create a new DsAkSettings instance with default values defined within the class.
     */
    public DsAkSettings()
    {
        values_ = Arrays.copyOf(settingsDefaults_, settingsDefaults_.length);
    }

    /**
     * Create a new DsAkSettings object by copying an existing object.
     *
     * @param c An existing DsAkSettings object to copy.
     */
    public DsAkSettings(DsAkSettings c)
    {
        values_ = Arrays.copyOf(c.getValues(), c.getValues().length);
    }

    /**
     * Create a new DsAkSettings object and initialize with the specified settings.
     * Settings not initialised with the specified settings will take on the class-defined default values.
     *
     * @param settings Array of triplets, one for each setting being set. The each triplet is the AK parameter, offset and value.
     */
    public DsAkSettings(int[][] settings)
    {
        this();
        if(settings != null)
        {
            set(settings);
        }
        else
        {
            Log.wtf(LOG_TAG, "Input settings array does not exist!");
        }
    }

    /**
     * This method is for internal use and is defined package-private.
     * @internal
     *
     * @return The internal array of values, indexed by the definition of
     *         allowed parameter-offset settings.
     */
    int[] getValues()
    {
        return values_;
    }

    /**
     * Indicate whether the parameter value conflicts with the pre-defined values.
     * @internal
     *
     * @param paramIndex The AK parameter index.
     * @param offset The element index (offset) of the parameter.
     * @param value The value for the specified parameter and offset.
     * @return true if there's a conflict, and false otherwise.
     */
    private boolean isParamValueConflicted(int paramIndex, int offset, int value)
    {
        boolean ret = false;

        if (constantAkParamsDefined_)
        {
            if (paramIndex == getAkParamIndex("genb") && value != akParam_genb_)
            {
                Log.e(LOG_TAG, "genb = " + value + " conflicts with the predefined value " + akParam_genb_);
                ret = true;
            }
            else if (paramIndex == getAkParamIndex("ienb") && value != akParam_ienb_)
            {
                Log.e(LOG_TAG, "ienb = " + value + " conflicts with the predefined value " + akParam_ienb_);
                ret = true;
            }
            else if (paramIndex == getAkParamIndex("aonb") && value != akParam_aonb_)
            {
                Log.e(LOG_TAG, "aonb = " + value + " conflicts with the predefined value " + akParam_aonb_);
                ret = true;
            }
            else if (paramIndex == getAkParamIndex("arnb") && value != akParam_aonb_)
            {
                Log.e(LOG_TAG, "arnb = " + value + " conflicts with the predefined value " + akParam_aonb_);
                ret = true;
            }
            else if (paramIndex == getAkParamIndex("aocc") && value != AKPARAM_AOCC)
            {
                Log.e(LOG_TAG, "aocc = " + value + " conflicts with the predefined value " + AKPARAM_AOCC);
                ret = true;
            }
        }
        else
        {
            Log.e(LOG_TAG, "Settable settings not defined yet");
            ret = true;
        }

        return ret;
    }

    /**
     * Set AK settings in a batch.
     *
     * @param settings Array of triplets, one for each setting being set. The each triplet is the AK parameter, offset and value.
     */
    public void set(int[][] settings) throws IllegalArgumentException
    {
        for(int[] fpv : settings)
        {
            if (fpv.length != 3)
                throw new IllegalArgumentException("Each setting must contain an array of 3 ints declared as int[3]");
            int i = fpv[0];
            fpv[2] = set(akParams_[i].paramName, fpv[1], fpv[2]);
        }
    }

    /**
     * Set the values for the AK parameter that contains multiple elements.
     *
     * @param parameter The AK parameter name to be set.
     * @param values The values array for the specified parameter.
     * @throws IllegalArgumentException If the parameter has not been defined as allowed by this module.
     */
    public void set(String parameter, int[] values) throws IllegalArgumentException
    {
        int paramIndex = getAkParamIndex(parameter);
        int i = getAkSettingIndex(parameter, 0);
        if (i == -1)
            throw new IllegalArgumentException("The parameter " + parameter + " is not settable");

        int paramLen = akParams_[paramIndex].len;
        int len = (values.length < paramLen) ? values.length : paramLen;
        for (int j = 0; j < len; ++j)
        {
            values[j] = (values[j] < akParams_[paramIndex].lowerBound) ? akParams_[paramIndex].lowerBound : values[j];
            values[j] = (values[j] > akParams_[paramIndex].upperBound) ? akParams_[paramIndex].upperBound : values[j];
            values_[i+j] = values[j];
        }
        DsLog.log1(LOG_TAG, "set: (parameter:" + parameter + " values:" + values + ")");
    }

    /**
     * Set the values for the specified AK parameter-offset combination.
     *
     * @param parameter The AK parameter name to be set.
     * @param offset The element index (offset) of the parameter.
     * @param value The value for the specified parameter and offset.
     * @return The value clamped with the upper and lower bounds.
     * @throws IllegalArgumentException If the parameter and offset combination
     * has not been defined as allowed by this module.
     */
    public int set(String parameter, int offset, int value) throws IllegalArgumentException
    {
        int paramIndex = getAkParamIndex(parameter);
        if (isParamValueConflicted(paramIndex, offset, value))
            throw new IllegalArgumentException("The parameter value conflicts with the pre-defined value");
        int i = getAkSettingIndex(parameter, offset);
        if (i == -1)
            throw new IllegalArgumentException("The parameter " + parameter + " and offset " + offset + " combination is not settable");

        if (!isValidParamValue(paramIndex, value))
        {
            DsLog.log1(LOG_TAG, "value " + value + " for parameter " + parameter + " is out of valid range");
            value = (value < akParams_[paramIndex].lowerBound) ? akParams_[paramIndex].lowerBound : value;
            value = (value > akParams_[paramIndex].upperBound) ? akParams_[paramIndex].upperBound : value;
            DsLog.log1(LOG_TAG, "Clamp the value to the upper/lower bound " + value);
        }

        values_[i] = value;
        DsLog.log2(LOG_TAG, "set: (parameter:" + parameter + " offset:" + offset + " value:" + value + ")");
        return value;
    }

    /**
     * Get the parameter values for the AK parameter that contains multiple elements.
     *
     * @param parameter The AK parameter name to get.
     * @return The value array in the settings for the specified parameter.
     * @throws IllegalArgumentException If the parameter has not been defined
     * as allowed by this module.
     */
    public int[] get(String parameter) throws IllegalArgumentException
    {
        int paramIndex = getAkParamIndex(parameter);
        int i = getAkSettingIndex(parameter, 0);
        if (i == -1)
            throw new IllegalArgumentException("The parameter " + parameter + " is not gettable");

        int length = akParams_[paramIndex].len;
        int[] values = new int[length];
        for (int j = 0; j < length; ++j)
            values[j] = values_[i+j];

        return values;
    }

    /**
     * Get the value of the specified AK parameter-offset combination.
     *
     * @param parameter The AK parameter name.
     * @param offset The element index (offset) of the parameter.
     * @return The value in the settings for the specified parameter and offset.
     * @throws IllegalArgumentException If the parameter and offset combination
     * has not been defined as allowed by this module.
     */
    public int get(String parameter, int offset)
    {
        int i = getAkSettingIndex(parameter, offset);
        if (i == -1)
            throw new IllegalArgumentException("The parameter " + parameter + " and offset " + offset + " combination is not gettable");

        return values_[i];
    }
}

/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013 - 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * DsProperty.java
 *
 * The helper class which is used for setting/getting the system properties by DS Audio Effect.
 * 
 */
package com.dolby.ds;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.*;
import android.util.Log;

public class DsProperty
{
    private static final String TAG = "DsProperty";

    /**
     * The strings defined for setting system properties.
     *
     */
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

    /**
     * The lock for protecting the DsProperty context to be thread safe.
     *
     * @internal
     */
    private static final Object lock_ = new Object();

    /**
     * The SystemProperties class.
     *
     * @internal
     */
    private static Class sysProp_ = null;

    /**
     * The SystemProperties class method to set system properties.
     *
     * @internal
     */
    private static Method systemPropertySet_ = null;

    /**
     * The SystemProperties class method to get system properties.
     *
     * @internal
     */
    private static Method systemPropertyGet_ = null;

    /**
     * The init method.
     *
     */
    public static void init()
    {
        synchronized (lock_)
        {
            try
            {
                sysProp_ = Class.forName("android.os.SystemProperties");
                systemPropertySet_ = sysProp_.getMethod("set", new Class[] {String.class, String.class});
                systemPropertyGet_ = sysProp_.getMethod("get", new Class[] {String.class, String.class});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in DsPropertyUtil.init");
            }
        }
    }

    /**
     * The method to get DS state system property.
     *
     */
    public static String getStateProperty()
    {
        String ret = null;
        synchronized (lock_)
        {
            try
            {
                ret = (String)systemPropertyGet_.invoke(sysProp_, new Object[]{PROP_DS_STATE, "false"});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in getStateProperty");
            }
        }
        return ret;
    }

    /**
     * The method to set DS state system property.
     *
     */
    public static void setStateProperty(String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setStateProperty");
            }
        }
    }

    /**
     * The method to set profile name system property.
     *
     */
    public static void setProfileNameProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_PROFILE_NAME, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setProfileNameProperty");
            }
        }
    }

    /**
     * The method to set Dialog Enhancer system property.
     *
     */
    public static void setDialogEnhancerProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_DIALOGENHANCER_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setDialogEnhancerProperty");
            }
        }
    }

    /**
     * The method to set Headphone Virtualizer system property.
     *
     */
    public static void setHeadphoneVirtualizerProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_HEADPHONE_VIRTUALIZER_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setHeadphoneVirtualizerProperty");
            }
        }
    }

    /**
     * The method to set Speaker Virtualizer system property.
     *
     */
    public static void setSpeakerVirtualizerProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_SPEAKER_VIRTUALIZER_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setSpeakerVirtualizerProperty");
            }
        }
    }

    /**
     * The method to set Volume Leveller system property.
     *
     */
    public static void setVolumeLevellerProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_VOLUMELEVELER_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setVolumeLevellerProperty");
            }
        }
    }

    /**
     * The method to set GEQ state system property.
     *
     */
    public static void setGeqStateProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_GEQ_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setGeqStateProperty");
            }
        }
    }

    /**
     * The method to set IEQ state system property.
     *
     */
    public static void setIeqStateProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_IEQ_STATE, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setIeqStateProperty");
            }
        }
    }

    /**
     * The method to set IEQ preset name system property.
     *
     */
    public static void setIeqPresetProperty (String value)
    {
        synchronized (lock_)
        {
            try
            {
                systemPropertySet_.invoke(sysProp_, new Object[]{PROP_DS_IEQ_PRESET, value});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in setIeqPresetProperty");
            }
        }
    }
    /**
     * The method to get Mono speaker state system property.
     *
     */
    public static String getMonoSpeakerProperty()
    {
        String ret = null;
        synchronized (lock_)
        {
            try
            {
                ret = (String)systemPropertyGet_.invoke(sysProp_, new Object[]{PROP_MONO_SPEAKER, "false"});
            }
            catch(Exception e)
            {
                Log.e(TAG, "Exception in getMonoSpeakerProperty");
            }
        }
        return ret;
    }

}

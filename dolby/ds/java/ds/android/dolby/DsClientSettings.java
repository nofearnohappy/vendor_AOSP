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
 * @defgroup    dsclientsettings DS Client Settings
 * @details     This class encapsulates the settings that are configurable
 *              by a client application using the client API for a DS profile.
 * @{
 */

package android.dolby;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.HashSet;
import android.util.Log;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * This class encapsulates the settings that are configurable
 * by a client application for a DS profile.
 */
public class DsClientSettings implements Parcelable
{
    private static final String TAG = "DsClientSettings";

    /**
     * The state of the graphic equalizer activation.
     * @internal
     */
    private boolean isGeqOn;
    /**
     * The state of the dialog enhancer activation.
     * @internal
     */
    private boolean isDialogEnhancerOn;
    /**
     * The state of the volume leveller activation.
     * @internal
     */
    private boolean isVolumeLevellerOn;
    /**
     * The state of the headphone virtualizer activation.
     * @internal
     */
    private boolean isHeadphoneVirtualizerOn;
    /**
     * The state of the speaker virtualizer activation.
     * @internal
     */
    private boolean isSpeakerVirtualizerOn;

    /**
     * The basic profile parameters that are always saved when saving the current profiles.
     */
    public static final HashSet<String> basicProfileParams;

    /**
     * Add the basic profile parameters to the saved parameter list.
     */
    static
    {
        basicProfileParams = new HashSet<String>();
        basicProfileParams.add("geon");
        basicProfileParams.add("deon");
        basicProfileParams.add("dvle");
        basicProfileParams.add("vdhe");
        basicProfileParams.add("vspe");
    }

    /**
     * Interface must be implemented and provided as a public CREATOR field that generates
     * instances of the Parcelable class from a Parcel.
     * This method is for internal use only.
     */
    public static final Parcelable.Creator<DsClientSettings> CREATOR = new Parcelable.Creator<DsClientSettings>()
    {
        public DsClientSettings createFromParcel(Parcel source)
        {
            return new DsClientSettings(source);
        }

        public DsClientSettings[] newArray(int size)
        {
            return new DsClientSettings[size];
        }
    };

    /**
     * Must be implemented, always return 0.
     * This method is for internal use only.
     */
    public int describeContents()
    {
        return 0;
    }

    /**
     * Create a default DsClientSettings.
     * This constructor is not intended for use by a client application. A client application
     * can obtain default settings for a profile by resetting the profile using the
     * DsClient.resetProfile method.
     */
    public DsClientSettings()
    {
        isGeqOn = false;
        isDialogEnhancerOn = false;
        isVolumeLevellerOn = false;
        isHeadphoneVirtualizerOn = false;
        isSpeakerVirtualizerOn = false;
    }

    /**
     * Constructor when re-constructing this object from a parcel.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object will be constructed.
     */
    public DsClientSettings(Parcel src)
    {
        readFromParcel(src);
    }

    /**
     * Write the data sequence into the parcel in order to be accessed remotely.
     * This method is for internal use only.
     *
     * @param dest  The data parcel in which the object should be written.
     * @param flags The flags that define how the object should be written.
     */
    public void writeToParcel(Parcel dest, int flags)
    {
        boolean[] settings = new boolean[5];

        settings[0] = isGeqOn;
        settings[1] = isDialogEnhancerOn;
        settings[2] = isVolumeLevellerOn;
        settings[3] = isHeadphoneVirtualizerOn;
        settings[4] = isSpeakerVirtualizerOn;

        dest.writeBooleanArray(settings);
    }

    /**
     * Restore the data sequence from the parcel sent remotely.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object is initialized.
     */
    public void readFromParcel(Parcel src)
    {
        boolean[] settings = new boolean[5];

        src.readBooleanArray(settings);

        isGeqOn = settings[0];
        isDialogEnhancerOn = settings[1];
        isVolumeLevellerOn = settings[2];
        isHeadphoneVirtualizerOn = settings[3];
        isSpeakerVirtualizerOn = settings[4];
    }

    /**
     * Set the state of the graphic equalizer activation.
     *
     * @param enable The new state of graphic equalizer (enabled/disabled).
     */
    public void setGeqOn(boolean enable)
    {
        isGeqOn = enable;
    }

    /**
     * Get the state of the graphic equalizer activation.
     *
     * @return The state of graphic equalizer.
     */
    public boolean getGeqOn()
    {
        return isGeqOn;
    }

    /**
     * Set the state of the dialog enhancer activation.
     *
     * @param enable The new state of the dialog enhancer (enabled/disabled).
     */
    public void setDialogEnhancerOn(boolean enable)
    {
        isDialogEnhancerOn = enable;
    }

    /**
     * Get the state of the dialog enhancer activation.
     *
     * @return The state of the dialog enhancer.
     */
    public boolean getDialogEnhancerOn()
    {
        return isDialogEnhancerOn;
    }

    /**
     * Set the state of the volume leveller activation.
     *
     * @param enable The new state of the volume leveller (enabled/disabled).
     */
    public void setVolumeLevellerOn(boolean enable)
    {
        isVolumeLevellerOn = enable;
    }

    /**
     * Get the state of the volume leveller activation.
     *
     * @return The state of the volume leveller.
     */
    public boolean getVolumeLevellerOn()
    {
        return isVolumeLevellerOn;
    }

    /**
     * Set the state of the headphone virtualizer activation.
     *
     * @param enable The new state of the headphone virtualizer (enabled/disabled).
     */
    public void setHeadphoneVirtualizerOn(boolean enable)
    {
        isHeadphoneVirtualizerOn = enable;
    }

    /**
     * Get the state of the headphone virtualizer activation.
     *
     * @return The state of the headphone virtualizer.
     */
    public boolean getHeadphoneVirtualizerOn()
    {
        return isHeadphoneVirtualizerOn;
    }

    /**
     * Set the state of the speaker virtualizer activation.
     *
     * @param enable The new state of the speaker virtualizer (enabled/disabled).
     */
    public void setSpeakerVirtualizerOn(boolean enable)
    {
        isSpeakerVirtualizerOn = enable;
    }

    /**
     * Get the state of the speaker virtualizer activation.
     *
     * @return The state of the speaker virtualizer.
     */
    public boolean getSpeakerVirtualizerOn()
    {
        return isSpeakerVirtualizerOn;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param anObject The object to compare this against.
     *
     * @return True if the given object represents a equivalent to this object, false otherwise.
     */
    public boolean equals(Object anObject)
    {
        if (this == anObject)
        {
            return true;
        }
        if (anObject instanceof DsClientSettings) 
        {
            DsClientSettings anotherObject = (DsClientSettings)anObject;
            
            if (this.isGeqOn == anotherObject.isGeqOn &&
                this.isDialogEnhancerOn == anotherObject.isDialogEnhancerOn &&
                this.isVolumeLevellerOn == anotherObject.isVolumeLevellerOn &&
                this.isHeadphoneVirtualizerOn == anotherObject.isHeadphoneVirtualizerOn &&
                this.isSpeakerVirtualizerOn == anotherObject.isSpeakerVirtualizerOn)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return The hash code.
     */
    public int hashCode()
    {
        int result = 0;
        
        result += (this.isGeqOn ? 2 : 0);
        result += (this.isDialogEnhancerOn ? 4 : 0);
        result += (this.isVolumeLevellerOn ? 8 : 0);
        result += (this.isHeadphoneVirtualizerOn ? 16 : 0);
        result += (this.isSpeakerVirtualizerOn ? 32 : 0);
        
        return result;
    }
}

/**
 * @}
 */

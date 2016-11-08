/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dsprofileName DS Profile Name Information
 * @details     This class encapsulates the special information for the profile display name.
 * @{
 */

package com.dolby.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * DsProfileName class provides special information for the profile display name.
 * This calss can only be used by Dolby apps.
 */
public class DsProfileName implements Parcelable
{
    private static final String TAG = "DsProfileName";

    /**
     * The current display name of the profile.
     * @internal
     */
    private String currentName_;
    /**
     * The default display name of the profile.
     * @internal
     */
    private String defaultName_;

    /**
     * Interface must be implemented and provided as a public CREATOR field that generates
     * instances of the Parcelable class from a Parcel.
     * This method is for internal use only.
     */
    public static final Parcelable.Creator<DsProfileName> CREATOR = new Parcelable.Creator<DsProfileName>()
    {
        public DsProfileName createFromParcel(Parcel source)
        {
            return new DsProfileName(source);
        }

        public DsProfileName[] newArray(int size)
        {
            return new DsProfileName[size];
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
     *  Create an uninitialized instance of DsProfileName.
     */
    public DsProfileName()
    {
    }

    /**
     * Constructor when re-constructing this object from a parcel.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object will be constructed.
     */
    public DsProfileName(Parcel src)
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
        dest.writeString(currentName_);
		dest.writeString(defaultName_);
    }

    /**
     * Restore the data sequence from the parcel sent remotely.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object is initialized.
     */
    public void readFromParcel(Parcel src)
    {
        currentName_ = src.readString();
		defaultName_ = src.readString();
    }

    /**
     * Set the current display name of the specific profile.
     *
     * @param name The new name for the specified profile.
     */
    public void setCurrentName(String name)
    {
        currentName_ = name;
    }
        
    /**
     * Get the current display name of the specific profile.
     *
     * @return The current display name of the specific profile.
     */
    public String getCurrentName()
    {
        return currentName_;
    }

    /**
     * Set the default name of the specific profile.
     *
     * @param name The default name for the specified profile.
     */
    public void setDefaultName(String name)
    {
        defaultName_ = name;
    }

     /**
     * Get the default name of the specific profile.
     *
     * @return The default name of the specific profile.
     */
    public String getDefaultName()
    {
        return defaultName_;
    }
}

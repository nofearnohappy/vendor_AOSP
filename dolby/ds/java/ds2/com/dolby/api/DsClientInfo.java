/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dsclientinfo DS Client Information
 * @details     This class encapsulates the information about the context of Ds client application.
 * @{
 */

package com.dolby.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class encapsulates the information about the context of Ds client application.
 */
public class DsClientInfo implements Parcelable
{
    private static final String TAG = "DsClientInfo";

    /**
     * The name of the Ds client application's package.
     
     * @internal
     */
    private String mPackageName;

	/**
     * The Ds API through which an application is connected.
     *
     * @internal
     */
    private int mConnectionBridge;


    /**
     * Interface must be implemented and provided as a public CREATOR field that generates
     * instances of the Parcelable class from a Parcel.
     * This method is for internal use only.
     */
    public static final Parcelable.Creator<DsClientInfo> CREATOR = new Parcelable.Creator<DsClientInfo>()
    {
        public DsClientInfo createFromParcel(Parcel source)
        {
            return new DsClientInfo(source);
        }

        public DsClientInfo[] newArray(int size)
        {
            return new DsClientInfo[size];
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
     * Create an uninitialized instance of DsClientInfo.
     */
    public DsClientInfo()
    {
    }

    /**
     * Constructor when re-constructing this object from a parcel.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object will be constructed.
     */
    public DsClientInfo(Parcel src)
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
        dest.writeString(mPackageName);
		dest.writeInt(mConnectionBridge);
    }

    /**
     * Restore the data sequence from the parcel sent remotely.
     * This method is for internal use only.
     *
     * @param src The data parcel with which the object is initialized.
     */
    public void readFromParcel(Parcel src)
    {
        mPackageName = src.readString();
		mConnectionBridge = src.readInt();
    }

    /**
     *  Set the name of the Ds client application's package.
     *
     * @param name The name of the Ds client application's package.
     */
    public void setPackageName(String name)
    {
        mPackageName = name;
    }

    /**
     * Get the name of the Ds client application's package.
     *
     * @return  The name of the Ds client application's package.
     */
    public String getPackageName()
    {
        return mPackageName;
    }

	/**
     *  Set the Ds API through which an application is connected.
     *
     * @param connection The Bridge through which an application is connected.
     */
    public void setConnectionBridge(int connection)
    {
        mConnectionBridge = connection;
    }

    /**
     * Get the Ds API through which an application is connected.
     *
     * @return  The Bridge through which an application is connected.
     */
    public int getConnectionBridge()
    {
        return mConnectionBridge;
    }
}

/**
 * @}
 */


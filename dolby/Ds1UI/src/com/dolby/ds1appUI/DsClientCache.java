/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.os.DeadObjectException;
import android.os.RemoteException;

/**
 * Cache of results from {@link DsClient}.
 */
public class DsClientCache {

    /**
     * Singleton instance.
     */
    public static final DsClientCache INSTANCE = new DsClientCache();

    private int mSelectedProfile = -1;

    private DsClientSettings[] mProfiles;

    private boolean mDsOn;

    private DsClientCache() {
    }

    public synchronized void reset() {
        mProfiles = null;
        mSelectedProfile = -1;
    }

    public synchronized DsClientSettings getSelectedProfileSettings(DsClient ds)
            throws DeadObjectException, RemoteException,
            UnsupportedOperationException {
        return getProfileSettings(ds, getSelectedProfile(ds));
    }

    public synchronized DsClientSettings getProfileSettings(DsClient ds,
            int profile) throws DeadObjectException, RemoteException,
            UnsupportedOperationException {
        if (mProfiles == null) {
            int profnumber=ds.getProfileCount();
            if (profnumber == 0) {
                throw new DeadObjectException();
            }
            mProfiles = new DsClientSettings[profnumber];
        }
        if (mProfiles[profile] == null) {
            mProfiles[profile] = ds.getProfileSettings(profile);
        }
        return mProfiles[profile];
    }

    public synchronized void setProfileSettings(DsClient ds, int profile,
            DsClientSettings settings) throws DeadObjectException,
            IllegalArgumentException, RemoteException,
            UnsupportedOperationException {
        ds.setProfileSettings(profile, settings);
        cacheProfileSettings(ds, profile, settings);
    }

    public synchronized void cacheProfileSettings(DsClient ds, int profile,
            DsClientSettings settings) throws DeadObjectException,
            RemoteException, UnsupportedOperationException {
        if (mProfiles == null) {
            mProfiles = new DsClientSettings[ds.getProfileCount()];
        }
        mProfiles[profile] = settings;
    }

    public synchronized int getSelectedProfile(DsClient ds)
            throws DeadObjectException, RemoteException,
            UnsupportedOperationException {
        if (mSelectedProfile == -1) {
            int profnumber = ds.getProfileCount();
            if (profnumber > 0) {
                mSelectedProfile = ds.getSelectedProfile();
            }
        }
        return mSelectedProfile;
    }

    public synchronized void cacheSelectedProfile(int selectedProfile) {
        this.mSelectedProfile = selectedProfile;
    }

    public synchronized void setSelectedProfile(DsClient ds, int profile)
            throws DeadObjectException, IllegalArgumentException,
            RemoteException, UnsupportedOperationException {
        ds.setSelectedProfile(profile);
        mProfiles[profile] = ds.getProfileSettings(profile);
        this.mSelectedProfile = profile;
    }

    public synchronized void cacheDsOn(boolean on) {
        this.mDsOn = on;
    }

    public boolean isDsOn() {
        return mDsOn;
    }

}

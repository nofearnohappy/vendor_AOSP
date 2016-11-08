/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * IDs.aidl
 *
 * Interface definition for the remote Service running in another process
 */

package android.dolby;

import android.dolby.DsClientSettings;
import android.dolby.IDsServiceCallbacks;

interface IDs
{
    int setDsOn(int handle, boolean on);

    int getDsOn(out boolean[] on);

    int setNonPersistentMode(boolean on);

    int getProfileCount(out int[] count);

    int getProfileNames(out String[] names);

    int getBandCount(out int[] count);

    int getBandFrequencies(out int[] frequencies);

    int setSelectedProfile(int handle, int profile);

    int getSelectedProfile(out int[] profile);

    int setProfileSettings(int handle, int profile, in DsClientSettings settings);

    int getProfileSettings(int profile, out DsClientSettings[] settings);

    int resetProfile(int handle, int profile);

    int setProfileName(int handle, int profile, String name);

    int getDsApVersion(out String[] version);

    int getMonoSpeaker(out boolean[] isMonoSpeaker);

    int getDsVersion(out String[] version);

    int setIeqPreset(int handle, int profile, int preset);

    int getIeqPreset(int profile, out int[] preset);

    int getProfileModified(int profile, out int[] modifiedValue);

    int setGeq (int handle, int profile, int preset, in float[] geqBandGains);

    int getGeq (int profile, int preset, out float[] geqBandGains);

    int setDsApParam(int handle, String param, in int[] values);

    int getDsApParam(String param, out int[] values);

    int getDsApParamLength(String param, out int[] len);

    void registerDsApParamEvents(int handle);

    void unregisterDsApParamEvents(int handle);

    void registerCallback(IDsServiceCallbacks cb, int handle);

    void unregisterCallback(IDsServiceCallbacks cb);

    void registerVisualizerData(int handle);

    void unregisterVisualizerData(int handle);
}

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
 * IDs.aidl
 *
 * Interface definition for the remote Service running in another process
 */

package com.dolby.api;

import com.dolby.api.DsClientInfo;
import com.dolby.api.DsProfileName;
import com.dolby.api.IDsCallbacks;
import com.dolby.api.IDsDeathHandler;

interface IDs
{
    void iRegisterVisualizerData(int handle);

    void iUnregisterVisualizerData(int handle);
    
    void iRegisterDeathHandler(int handle, IDsDeathHandler dh);

    void iUnregisterDeathHandler(int handle, IDsDeathHandler dh);

    void iRegisterDsAccess(int handle, in DsClientInfo info);

    void iUnregisterDsAccess(int handle);

    void iRegisterCallback(int handle, IDsCallbacks cb, int version);

    void iUnregisterCallback(int handle, IDsCallbacks cb, int version);

    int iSetState(int handle, int Device, boolean on);

    int iGetState(int Device, out int[] on);

    int iGetOffType(out int[] offType);

    int iGetDsServiceVersion(out String[] version);

    int iGetDapLibraryVersion(out String[] version);

    int iGetUdcLibraryVersion(out String[] version);

    int iSetParameter(int handle, int device, int profile, int paramId, in int[] values);

    int iGetParameter(int device, int profile, int paramId, out int[] values);

    int iSetIeqPreset(int handle, int device, int preset);

    int iGetIeqPreset(int device, out int[] preset);

    int iGetIeqPresetCount(int device, out int[] count);

    int iSetProfile(int handle, int device, int profile);

    int iGetProfile(int device, out int[] profile);

    int iResetProfile(int handle, int device, int profile);

    int iGetProfileModified(int device, int profile, out boolean[] flag);

    int iGetProfileCount(int device, out int[] count);

    int iRequestAccessRight(int handle, int type);

    int iAbandonAccessRight(int handle, int type);

    int iCheckAccessRight(int handle, int type, out int[] state);
    
    int iGetParamLength(int paramId, out int[] len);

    int iGetMonoSpeaker(out boolean[] mono);
    
    int iActivateTuning(int handle, int endpointPort, String productId);

    int iDeactivateTuning(int handle, int endpointPort, String productId);
    
    int iSetProfileName(int handle, int profile, in DsProfileName name);
    
    int iGetProfileName(int handle, int profile, out DsProfileName[] name);
}

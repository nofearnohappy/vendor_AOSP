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

#ifndef DOLBY_ENDPOINT_PARAM_CACHE_H_
#define DOLBY_ENDPOINT_PARAM_CACHE_H_

#include <system/audio.h>
#include <utils/Errors.h>
#include "IDlbProcess.h"
#include "DapParamCache.h"

namespace dolby {

using namespace android;

class EndpointParamCache
{
public:
    EndpointParamCache(IDlbProcess *dap);
    ~EndpointParamCache();

    status_t init();
    status_t setDevice(audio_devices_t device);
    audio_devices_t getDevice() { return mCurrentDevice; };
    status_t setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length);
    status_t getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length);
    status_t commitChangedParams(audio_devices_t device);
    status_t commitAllParams(audio_devices_t device);

protected:
    DapParamCache *getCache(audio_devices_t device);
    status_t commit(audio_devices_t device, bool modifiedOnly);
    status_t doCommit(DapParamCache *params, bool modifiedOnly);
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE
#ifdef DOLBY_DAP2_BACKWARD_COMPATIBLE
    status_t commitDap1Param(DapParameterId param, DapParamCache *params);
#endif
// @@DOLBY_DAP2_BACKWARD_COMPATIBLE_END

    IDlbProcess *mDap;
    audio_devices_t mCurrentDevice;
    KeyedVector<audio_devices_t, DapParamCache*> mCache;
};

} // namespace dolby
#endif//DOLBY_ENDPOINT_PARAM_CACHE_H_

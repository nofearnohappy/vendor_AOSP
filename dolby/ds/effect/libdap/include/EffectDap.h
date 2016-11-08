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

#ifndef DOLBY_EFFECT_DAP_H_
#define DOLBY_EFFECT_DAP_H_

#include "IEffectDap.h"
#include "EndpointParamCache.h"

#ifdef DOLBY_AUDIO_DUMP
#include "DapPcmDump.h"
#endif

namespace dolby {

using namespace android;

class EffectDap : public IEffectDap
{
public:
    EffectDap(IDlbProcess *dap);
    virtual ~EffectDap();

    virtual status_t init();
    virtual bool isOffload() const
    { return false; }
    virtual status_t process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer);
    virtual void setBypass(bool bypass);
    virtual status_t setEnabled(bool enabled);
    virtual status_t setDevice(audio_devices_t device);
    virtual status_t setConfig(const buffer_config_t &inCfg, const buffer_config_t &outCfg);
    virtual status_t setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length);
    virtual status_t getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t commitChangedParams(audio_devices_t device);
    virtual status_t commitAllParams(audio_devices_t device);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);

protected:
    void updateProcessEnabled();

    bool mEnabled;
    bool mBypassed;
    bool mProcessEnabled;
    IDlbProcess* mProc;
    EndpointParamCache mParamCache;
    AudioBufferProvider mInBuffer, mOutBuffer;
#ifdef DOLBY_AUDIO_DUMP
    DapPcmDump mPcmDump;
#endif
};

} // namespace dolby
#endif//DOLBY_EFFECT_DAP_H_

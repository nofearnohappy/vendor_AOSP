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
#ifndef DOLBY_EFFECT_QDSP_DAP_H_
#define DOLBY_EFFECT_QDSP_DAP_H_

#include "IEffectDap.h"
#include "ProfileParamParser.h"
#ifdef DOLBY_DAP_HW_QDSP_HAL_API
#include "QdspHalDriver.h"
#else
#include "QdspAlsaDriver.h"
#endif

namespace dolby {

using namespace android;

class EffectQdspDap : public IEffectDap
{
public:
    EffectQdspDap();

    virtual status_t init();
    virtual void setBypass(bool bypass);
    virtual status_t setEnabled(bool enabled);

    virtual bool isOffload() const
    { return true; }
    virtual status_t process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
    { return NOT_ENOUGH_DATA; }
    virtual status_t setConfig(const buffer_config_t &inCfg, const buffer_config_t &outCfg)
    { return NO_ERROR; }

    virtual status_t setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length);
    virtual status_t getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length);
    virtual status_t commitChangedParams(audio_devices_t device);
    virtual status_t commitAllParams(audio_devices_t device);
    virtual status_t getVisualizer(dap_param_value_t *data, int *bands);
    virtual status_t setDevice(audio_devices_t device);
    virtual status_t defineOffProfile(ProfileParamParser ppp);
    virtual int32_t  getOffType();

protected:
    status_t updateEnabled();

    bool mBypassed, mEnabled;
    QdspDriver mQdsp;
};

} // namespace dolby

#endif//DOLBY_EFFECT_QDSP_DAP_H_

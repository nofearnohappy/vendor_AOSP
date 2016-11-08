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
#ifndef DOLBY_EFFECT_CONTEXT_H_
#define DOLBY_EFFECT_CONTEXT_H_

#include <utils/Errors.h>
#include <hardware/audio_effect.h>

#include "IEffectDap.h"

namespace dolby {

using namespace android;

/**
    The class contains the code for common behavior in all effect libraries.

    The purpose of this class is to provide uniform behavior for all effect
    libraries. This includes following functionality:
        - Parse set parameter & get parameter commands
        - Extract configuration information and only update if changed
        - Implement offload specific behavior
        - Transform pregain & postgain values
*/
class EffectContext
{
public:
    EffectContext();
    ~EffectContext();

    status_t init();
    status_t setConfig(const effect_config_t *pConfig);
    void reset();
    status_t enable();
    status_t disable();
    status_t setParam(int paramId, uint32_t length, void *pValues);
    status_t getParam(int paramId, uint32_t *pLength, void *pValues);
    void setDevice(uint32_t device);
    void setVolume(int numChannels, const uint32_t *volumes);
    void setAudioMode(audio_mode_t mode);
    status_t getConfig(effect_config_t *pConfig);
    status_t offload(bool isOffload, int ioHandle);
    status_t process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer);

protected:
    status_t defineProfile(void *pValues, uint32_t length);
    status_t setParamValues(void *pValues, uint32_t length);
    status_t setVisualizerEnable(void *pValues, uint32_t length);
    status_t setPregain(void *pValues, uint32_t length);
    status_t setPostgain(void *pValues, uint32_t length);
    status_t setBypass(void *pValues, uint32_t length);
    status_t getVersion(dap_param_value_t *pValues, int *length);
    status_t getVisualizerEnable(dap_param_value_t *pValues, int *length);
    status_t getVisualizerData(dap_param_value_t *pValues, int *length);
    status_t getOffType(dap_param_value_t *pValues, int *length);

    bool isEffectActive();
    bool isConfigured();

private:
    static bool hasProcessConfigChanged(const buffer_config_t &oldConfig, const buffer_config_t &newConfig);
    static dap_param_value_t transformExternalGain(uint32_t volume);

protected:
    IEffectDap *mEffect;
    bool mOffloadEnabled;
    effect_config_t mConfig;
    static const dap_param_value_t kNumVisualizerBands;
};

} // namespace dolby

#endif//DOLBY_EFFECT_CONTEXT_H_

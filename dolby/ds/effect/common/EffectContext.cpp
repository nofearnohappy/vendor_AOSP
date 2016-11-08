/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "DlbEffectContext"

#include <math.h>
#include "utils/String8.h"
#include "utils/Errors.h"
#include "DlbLog.h"
#include "ds_config.h"
#include "EffectContext.h"
#include "EffectParamParser.h"
#include "ProfileParamParser.h"

namespace dolby {

using namespace android;

const dap_param_value_t EffectContext::kNumVisualizerBands = 20;

/**
    Initialize a new instance of EffectContext class

    This function creates a new instance of effect implementation using
    IEffectDap::EffectDapFactory() factory function defined by
    the effect implementation.
*/
EffectContext::EffectContext()
    : mEffect(IEffectDap::EffectDapFactory())
{
    ALOGI("%s()", __FUNCTION__);

    // Clear effect configuration.
    memset(&mConfig, 0, sizeof(effect_config_t));
    // Offloading is disabled by default.
    mOffloadEnabled = false;
}

EffectContext::~EffectContext()
{
    delete mEffect;
}

/**
    Call IEffectDap::init() to initialize the effect instance.
*/
status_t EffectContext::init()
{
    ALOGV("%s()", __FUNCTION__);
    return mEffect->init();
}

/**
    Returns true if the current effect should be processing audio data.
*/
bool EffectContext::isEffectActive()
{
    // Return true if effect should receive processing commands.
    return (mOffloadEnabled == mEffect->isOffload());
}

/**
    returns true if the effect is configured.
*/
bool EffectContext::isConfigured()
{
    // Return true if some configuration is provided for input and output.
    return (mConfig.inputCfg.mask && mConfig.outputCfg.mask);
}

/**
    Returns true if any of sample rate, number of channels or format are different.
*/
bool EffectContext::hasProcessConfigChanged(const buffer_config_t &oldConfig, const buffer_config_t &newConfig)
{
    return ((newConfig.mask & EFFECT_CONFIG_SMP_RATE) && (oldConfig.samplingRate != newConfig.samplingRate))
        || ((newConfig.mask & EFFECT_CONFIG_CHANNELS) && (oldConfig.channels != newConfig.channels))
        || ((newConfig.mask & EFFECT_CONFIG_FORMAT) && (oldConfig.format != newConfig.format));
}

/**
    Calls IEffectDap::setConfig() if the configuration has changed.

    The function IEffectDap::setConfig() should retain the old configuration
    if new configuration can not be applied.
*/
status_t EffectContext::setConfig(const effect_config_t *pConfig)
{
    ALOGV("%s()", __FUNCTION__);

    // We only care about changes to process config for sending to IEffectDap
    if (hasProcessConfigChanged(mConfig.inputCfg, pConfig->inputCfg) ||
        hasProcessConfigChanged(mConfig.outputCfg, pConfig->outputCfg))
    {
        ALOGV("%s() calling IEffectDap:setConfig() with changed configuration.", __FUNCTION__);
        status_t status = mEffect->setConfig(pConfig->inputCfg, pConfig->outputCfg);
        if (status != NO_ERROR)
        {
            ALOGE("%s() Setting configuration failed with error %d", __FUNCTION__, status);
            return status;
        }
    }
    // Do not try to merge new config into old by looking at mask values
    // Since we only care about the process config.
    mConfig = *pConfig;
    return NO_ERROR;
}

/**
    Resets the effect by reapplying existing configuration.
*/
void EffectContext::reset()
{
    ALOGV("%s()", __FUNCTION__);
    if (isConfigured())
    {
        ALOGV("%s() resetting configuration", __FUNCTION__);
        mEffect->setConfig(mConfig.inputCfg, mConfig.outputCfg);
    }
}

status_t EffectContext::enable()
{
    ALOGV("%s()", __FUNCTION__);
    return mEffect->setEnabled(true);
}

status_t EffectContext::disable()
{
    ALOGV("%s()", __FUNCTION__);
    return mEffect->setEnabled(false);
}

/**
    Process set parameter command and extract data for individual parameter.
*/
status_t EffectContext::setParam(int paramId, uint32_t length, void *pValues)
{
    ALOGV("%s()", __FUNCTION__);
    status_t status;

    switch (paramId)
    {
    case EFFECT_PARAM_SET_VALUES:
        ALOGV("%s() received parameter EFFECT_PARAM_SET_VALUES", __FUNCTION__);
        status = setParamValues(pValues, length);
        break;
    case EFFECT_PARAM_VISUALIZER_ENABLE:
        ALOGV("%s() received parameter EFFECT_PARAM_VISUALIZER_ENABLE", __FUNCTION__);
        status = setVisualizerEnable(pValues, length);
        break;
    case EFFECT_PARAM_SET_PREGAIN:
        ALOGV("%s() received parameter EFFECT_PARAM_SET_PREGAIN", __FUNCTION__);
        status = setPregain(pValues, length);
        break;
    case EFFECT_PARAM_SET_BYPASS:
        ALOGV("%s() received parameter EFFECT_PARAM_SET_BYPASS", __FUNCTION__);
        status = setBypass(pValues, length);
        break;
    case EFFECT_PARAM_DEFINE_PROFILE:
        ALOGV("%s() received parameter EFFECT_PARAM_DEFINE_PROFILE", __FUNCTION__);
        status = defineProfile(pValues, length);
        break;
    default:
        ALOGE("%s() received unknown parameter %d", __FUNCTION__, paramId);
        status = NAME_NOT_FOUND;
    }
    ALOGV("%s() returns %i", __FUNCTION__, status);
    return status;
}

/**
    Process set parameter command and pack data for individual parameter.
*/
status_t EffectContext::getParam(int paramId, uint32_t *pLength, void *pValues)
{
    ALOGVV("%s()", __FUNCTION__);
    // Ignore getter command when offload is disabled
    if (!isEffectActive())
    {
        ALOGV("%s() ignoring call since effect is not active.", __FUNCTION__);
        return INVALID_OPERATION;
    }

    status_t status = NO_ERROR;
    int length = *pLength / sizeof(dap_param_value_t);
    dap_param_value_t* values = reinterpret_cast<dap_param_value_t*>(pValues);

    switch (paramId)
    {
    case EFFECT_PARAM_VERSION:
        ALOGV("%s() requested parameter EFFECT_PARAM_VERSION", __FUNCTION__);
        status = getVersion(values, &length);
        break;
    case EFFECT_PARAM_VISUALIZER_ENABLE:
        ALOGV("%s() requested parameter EFFECT_PARAM_VISUALIZER_ENABLE", __FUNCTION__);
        status = getVisualizerEnable(values, &length);
        break;
    case EFFECT_PARAM_VISUALIZER_DATA:
        ALOGVV("%s() requested parameter EFFECT_PARAM_VISUALIZER_DATA", __FUNCTION__);
        status = getVisualizerData(values, &length);
        break;
    case EFFECT_PARAM_OFF_TYPE:
        ALOGV("%s() requested parameter EFFECT_PARAM_OFF_TYPE", __FUNCTION__);
        status = getOffType(values, &length);
        break;
    default:
        ALOGE("%s() called with invalid parameter id %d", __FUNCTION__, paramId);
        status = NAME_NOT_FOUND;
    }

    if (status == NO_ERROR)
    {
        *pLength = sizeof(dap_param_value_t) * length;
    }
    ALOGV("%s() returns %i", __FUNCTION__, status);
    return status;
}

/**
 * Send DAP parameter values to effect instance.
 */
status_t EffectContext::defineProfile(void *pValues, uint32_t length)
{
    ALOGD("%s()", __FUNCTION__);
    status_t status = NO_ERROR;
    ProfileParamParser ppp(pValues, length);
    // Verify that the parameters are packed correctly
    if (!ppp.validate())
    {
        ALOGE("%s() Invalid data for defining profile.", __FUNCTION__);
        return BAD_VALUE;
    }
    if (ppp.profileId() == PROFILE_OFF && mEffect->isOffload())
    {
        status = mEffect->defineOffProfile(ppp);
        if (status != NO_ERROR)
        {
            ALOGE("%s() Failure to set up the off profile.", __FUNCTION__);
            return status;
        }
    }
    return status;
}

/**
    Send DAP parameter values to effect instance.
*/
status_t EffectContext::setParamValues(void *pValues, uint32_t length)
{
    ALOGV("%s()", __FUNCTION__);
    EffectParamParser epp(pValues, length);
    // Verify that the parameters are packed correctly
    if (!epp.validate())
    {
        ALOGE("%s() Invalid data for setting parameter values.", __FUNCTION__);
        return BAD_VALUE;
    }
    // Extract parameters and add each parameter to cache
    epp.begin();
    while (epp.next())
    {
        mEffect->setParam(epp.deviceId(), epp.paramId(), epp.values(), epp.length());
        DapParameterId overrideParam;
        switch(epp.paramId())
        {
        case DAP1_PARAM_GENB:
            overrideParam = DAP_PARAM_VCNB;
            LOG_FATAL_IF(epp.length() != 1, "%s() Expected one value for GENB received %d",
                __FUNCTION__, epp.length());
            LOG_FATAL_IF(*epp.values() > kNumVisualizerBands,
                "%s() Expected less than %d visualizer bands received %hd",
                __FUNCTION__, kNumVisualizerBands, *epp.values());
            break;
        case DAP1_PARAM_GEBF:
            overrideParam = DAP_PARAM_VCBF;
            break;
        default:
            overrideParam = DAP_PARAM_INVALID;
        }
        if (overrideParam != DAP_PARAM_INVALID)
        {
            ALOGD("%s() Overriding %s with %s", __FUNCTION__, dapParamName(overrideParam).string(),
                dapParamNameValue(epp.paramId(), epp.values(), epp.length()).string());
            mEffect->setParam(epp.deviceId(), overrideParam, epp.values(), epp.length());
        }
    }
    // Commit the added parameters to DAP
    return mEffect->commitChangedParams(epp.activeDevice());
}

/**
    Enable or disable visualizer in the effect instance.
*/
status_t EffectContext::setVisualizerEnable(void *pValues, uint32_t length)
{
    ALOGD("%s()", __FUNCTION__);
    int enable = 0;
    ParserBuffer pb(pValues, length);
    if (!pb.extract(&enable))
    {
        ALOGE("%s() Invalid data for parameter EFFECT_PARAM_VISUALIZER_ENABLE", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGD("%s() Setting virtualizer enable = %d", __FUNCTION__, enable);
#ifdef DOLBY_DAP2
    return (enable ? NO_ERROR : INVALID_OPERATION);
#else
    mEffect->setParam(AUDIO_DEVICE_OUT_DEFAULT, DAP1_PARAM_VEN, static_cast<dap_param_value_t>(enable));
    return mEffect->commitChangedParams(AUDIO_DEVICE_OUT_DEFAULT);
#endif
}

/**
 *  Set Dolby effect pregain.
 */
status_t EffectContext::setPregain(void *pValues, uint32_t length)
{
    uint32_t volume;
    ParserBuffer pb(pValues, length);
    if (!pb.extract(&volume))
    {
        ALOGE("%s() Invalid data for parameter EFFECT_PARAM_SET_PREGAIN", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGD("%s(volume=%d)", __FUNCTION__, volume);
    dap_param_value_t gain = transformExternalGain(volume);
    mEffect->setParam(AUDIO_DEVICE_OUT_DEFAULT, DAP_PARAM_PREG, &gain, 1);
    return mEffect->commitChangedParams(AUDIO_DEVICE_OUT_DEFAULT);
}

/**
 *  Set Dolby effect postgain.
 */
status_t EffectContext::setPostgain(void *pValues, uint32_t length)
{
    uint32_t volume;
    ParserBuffer pb(pValues, length);
    if (!pb.extract(&volume))
    {
        ALOGE("%s() Invalid data for parameter EFFECT_PARAM_SET_POSTGAIN", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGD("%s(volume=%d)", __FUNCTION__, volume);
    dap_param_value_t gain = transformExternalGain(volume);
    mEffect->setParam(AUDIO_DEVICE_OUT_DEFAULT, DAP_PARAM_PSTG, &gain, 1);
    return mEffect->commitChangedParams(AUDIO_DEVICE_OUT_DEFAULT);
}

/**
    Enable or disable bypassing Dolby audio processing.
*/
status_t EffectContext::setBypass(void *pValues, uint32_t length)
{
    int32_t bypass;
    ParserBuffer pb(pValues, length);
    if (!pb.extract(&bypass))
    {
        ALOGE("%s() Invalid data for parameter EFFECT_PARAM_SET_BYPASS", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGD("%s(bypass=%d)", __FUNCTION__, bypass);
    mEffect->setBypass(bypass);
    return NO_ERROR;
}

/**
    Retrieve version of DAP used by effect instance.
*/
status_t EffectContext::getVersion(dap_param_value_t *values, int *length)
{
    ALOGD("%s()", __FUNCTION__);
    return mEffect->getParam(AUDIO_DEVICE_OUT_DEFAULT, DAP_PARAM_VER, values, length);
}

/**
    Retrieve state of visualizer in the effect instance.
*/
status_t EffectContext::getVisualizerEnable(dap_param_value_t *values, int *length)
{
    ALOGD("%s()", __FUNCTION__);
    int32_t *enable;
    int buf_size = (*length) * sizeof(dap_param_value_t);
    ParserBuffer pb(values, buf_size);
    if (!pb.consume(&enable))
    {
        ALOGE("%s() received buffer of %d bytes for EFFECT_PARAM_VISUALIZER_ENABLE expected 4 bytes.",
            __FUNCTION__, buf_size);
        return NOT_ENOUGH_DATA;
    }
    status_t ret = NO_ERROR;
#ifdef DOLBY_DAP2
    *enable = 1;
#else
    *enable = mEffect->getParam(AUDIO_DEVICE_OUT_DEFAULT, DAP1_PARAM_VEN, &ret);
#endif
    return ret;
}

/**
    This function converts visualizer gain into a character for logging.

    This function maps visualizer gain to a character between '0' to '9'.
    This is used for logging visualizer data received  from DAP.
*/
static char scaleVisualizerGain(dap_param_value_t gain)
{
    const char kScaleMin = '0';
    const char kScaleMax = '9';
    const dap_param_value_t kGainMin = -192;
    const dap_param_value_t kGainMax = 576;

    // Clip to min gain
    if (gain <= kGainMin)
    {
        return kScaleMin;
    }

    // Clip to max gain
    if (gain >= kGainMax)
    {
        return kScaleMax;
    }

    // Scale intermediate gain values using on linear interpolation.
    return kScaleMin + static_cast<char>(((kScaleMax - kScaleMin) * (gain - kGainMin)) / (kGainMax - kGainMin));
}

/**
    Retrieve visualizer data from effect instance.

    This function retrieves visualizer band gains and excitations from the
    effect instance by querying individually for parameters VCBG and VCBE.
*/
status_t EffectContext::getVisualizerData(dap_param_value_t *values, int *length)
{
    ALOGVV("%s()", __FUNCTION__);
    // Retrieve visualizer data
    int bands = kNumVisualizerBands;
    status_t ret = mEffect->getVisualizer(values, &bands);
    if (ret == NO_INIT)
    {
        *length = 0;
        return NO_ERROR;
    }
    else if (ret != NO_ERROR)
    {
        ALOGE_IF(ret != NO_INIT,
            "%s() failed to get visualizer data with error %d", __FUNCTION__, ret);
        return ret;
    }
    // Update length with number of retrieved values
    *length = 2 * bands;
    // Log the visualizer data.
    IF_ALOGD()
    {
        static int skip_logs = 0;
        if (skip_logs == 0)
        {
            skip_logs = 20;

            char vcbg[kNumVisualizerBands+1], vcbe[kNumVisualizerBands+1];
            for (int i = 0; i < bands; ++i)
            {
                vcbg[i] = scaleVisualizerGain(values[i]);
                vcbe[i] = scaleVisualizerGain(values[bands+i]);
            }
            vcbg[bands] = '\0';
            vcbe[bands] = '\0';

            ALOGD("%s() Visualizer data (skipped %d) [%s] [%s]", __FUNCTION__, skip_logs, vcbg, vcbe);
        }
        else
        {
            --skip_logs;
        }
    }
    return NO_ERROR;
}

/**
 * Retrieve the effect off type in the effect instance.
 */
status_t EffectContext::getOffType(dap_param_value_t *values, int *length)
{
    ALOGD("%s()", __FUNCTION__);
    int32_t *type;
    int buf_size = (*length) * sizeof(dap_param_value_t);
    ParserBuffer pb(values, buf_size);
    if (!pb.consume(&type))
    {
        ALOGE("%s() received buffer of %d bytes for EFFECT_PARAM_OFF_TYPE expected 4 bytes.",
            __FUNCTION__, buf_size);
        return NOT_ENOUGH_DATA;
    }
    status_t ret = NO_ERROR;
    *type = mEffect->getOffType();
    return ret;
}

void EffectContext::setDevice(uint32_t device)
{
    ALOGV("%s()", __FUNCTION__);
    mEffect->setDevice(device);
}

void EffectContext::setVolume(int numChannels, const uint32_t *volumes)
{
    ALOGV("%s()", __FUNCTION__);
}

void EffectContext::setAudioMode(audio_mode_t mode)
{
    ALOGV("%s()", __FUNCTION__);
}

status_t EffectContext::getConfig(effect_config_t *pConfig)
{
    ALOGV("%s()", __FUNCTION__);
    // Don't return configuration if we do not have any or
    // if the effect is not active due to offload setting.
    if (!isEffectActive() || !isConfigured())
    {
        return INVALID_OPERATION;
    }
    *pConfig = mConfig;
    return NO_ERROR;
}

status_t EffectContext::offload(bool isOffload, int ioHandle)
{
    ALOGV("%s()", __FUNCTION__);
    mOffloadEnabled = isOffload;
    return NO_ERROR;
}

/**
    Convert volume to log scale gain value.
*/
dap_param_value_t EffectContext::transformExternalGain(uint32_t volume)
{
    const uint32_t kVolumeMax = 1 << 24; // Value 1 in 8.24 fixed-point.
    const dap_param_value_t kDbScaleFactor  = 16;  // The dB scale factor to convert dB value to 'preg' value.

    const float fOne24 = (float)(kVolumeMax);
    float fVol = volume / fOne24;

    const dap_param_value_t EXT_DB_MIN = -130;        // The minimum dB value (maximum attenuation) for pregain and postgain.
    const dap_param_value_t EXT_DB_MAX = 0;           // The maximum dB value (minimum attenuation) for pregain and postgain.
    const float F_VOL_MIN  = 0.00000031623f; // Android volume below this value corresponds to the minimum dB value, that is,
                                             // -130dB. It is calculated by pow(10.0f, PSTG_DB_MIN/20).
    const float F_VOL_MAX  = 1.0f;           // Android volume above this value corresponds to the maximum dB value, that is,
                                             // 0dB. It is calculated by pow(10.0f, PSTG_DB_MAX/20).
    //
    // Calculate the pregain/postgain that needs to be applied. This calculation assumes that the Android floating point volume that ranges
    // from -0.0f to 1.0f maps logarithmically to the pregain/postgain parameter, ranging from -2080 (-130dB) to 0 (0dB) respectively.
    //
    dap_param_value_t extVal = 0;
    if (fVol <= F_VOL_MIN)
    {
        extVal = EXT_DB_MIN * kDbScaleFactor;
    }
    else if (fVol >= F_VOL_MAX)
    {
        extVal = EXT_DB_MAX * kDbScaleFactor;
    }
    else
    {
        float dbVal = 20.0f * log10(fVol);
        extVal = (dap_param_value_t)(dbVal * kDbScaleFactor);
    }
    ALOGD("%s() fVol:%f extVal:%hi", __FUNCTION__, fVol, extVal);

    return extVal;
}

status_t EffectContext::process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    ALOGVV("%s()", __FUNCTION__);
    if (!isConfigured())
    {
        ALOGE("%s() called when effect is not configured.", __FUNCTION__);
        return INVALID_OPERATION;
    }
    return mEffect->process(inBuffer, outBuffer);
}

} // namespace dolby

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

#define LOG_TAG "DlbEffectQdspDap"

#include "DlbLog.h"
#include "DlbEffect.h"
#include "QdspParams.h"
#include "EffectQdspDap.h"
#include "ds_config.h"

DEFINE_DOLBY_EFFECT_LIBRARY_INFO("Effect QDSP DAP Library");
DEFINE_DOLBY_EFFECT_DESCRIPTOR("QDSP DAP", true);

namespace dolby {

using namespace android;

IEffectDap *IEffectDap::EffectDapFactory()
{
    ALOGI("%s()", __FUNCTION__);
    return new EffectQdspDap();
};

EffectQdspDap::EffectQdspDap()
{
    ALOGD("%s()", __FUNCTION__);
    mBypassed = false;
    mEnabled = false;
}

status_t EffectQdspDap::init()
{
    ALOGD("%s()", __FUNCTION__);
    return mQdsp.init();
}

status_t EffectQdspDap::updateEnabled()
{
    ALOGV("%s()", __FUNCTION__);
    bool enable = mEnabled && (!mBypassed);
    return mQdsp.setEnabled(enable);
}

void EffectQdspDap::setBypass(bool bypass)
{
    ALOGV("%s()", __FUNCTION__);
    if (mBypassed != bypass)
    {
        mBypassed = bypass;
        updateEnabled();
    }
}

status_t EffectQdspDap::setEnabled(bool enabled)
{
    ALOGV("%s()", __FUNCTION__);
    if (mEnabled != enabled)
    {
        mEnabled = enabled;
        return updateEnabled();
    }
    return NO_ERROR;
}

status_t EffectQdspDap::setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s()", __FUNCTION__);
    QdspParameterId qparam = qdspParamIdForParam(param);
    if (qparam == QDSP_INVALID_PARAM)
    {
        return NAME_NOT_FOUND;
    }
    return mQdsp.setParam(device, qparam, values, length);
}

status_t EffectQdspDap::getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGVV("%s()", __FUNCTION__);
    QdspParameterId qparam = qdspParamIdForParam(param);
    if (qparam == QDSP_INVALID_PARAM)
    {
        return NAME_NOT_FOUND;
    }
    return mQdsp.getParam(device, qparam, values, length);
}

status_t EffectQdspDap::commitChangedParams(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    return mQdsp.commitChangedParams(device);
}

status_t EffectQdspDap::commitAllParams(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    return mQdsp.commitAllParams(device);
}

status_t EffectQdspDap::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);
    return mQdsp.getVisualizer(data, bands);
}

status_t EffectQdspDap::setDevice(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    return mQdsp.setDevice(device);
}

status_t EffectQdspDap::defineOffProfile(ProfileParamParser ppp)
{
    ALOGV("%s()", __FUNCTION__);
    return mQdsp.defineOffProfile(ppp);
}

int32_t EffectQdspDap::getOffType()
{
    ALOGV("%s()", __FUNCTION__);
    return mQdsp.getOffType();
}

} // namespace dolby

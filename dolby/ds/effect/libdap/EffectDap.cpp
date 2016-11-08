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
#define LOG_TAG "DlbEffectDap"

#include "DlbLog.h"
#include "DlbEffect.h"
#include "EffectDap.h"
#ifdef DOLBY_DAP2
#include "Dap2Process.h"
#else
#include "Dap1Process.h"
#endif
#include "DapBufferAdapter.h"
#include "CrossfadeProcess.h"
#include "ds_config.h"

DEFINE_DOLBY_EFFECT_LIBRARY_INFO("Effect DAP Library");
DEFINE_DOLBY_EFFECT_DESCRIPTOR("DAP", false);

namespace dolby {

using namespace android;

IEffectDap *IEffectDap::EffectDapFactory()
{
    ALOGI("%s()", __FUNCTION__);
#ifdef DOLBY_DAP2
    IDlbProcess *dap = new Dap2Process();
#else
    IDlbProcess *dap = new Dap1Process();
#endif
    IDlbProcess *buf = new DapBufferAdapter(dap);
    IDlbProcess *proc = new CrossfadeProcess(buf);
    return new EffectDap(proc);
};

EffectDap::EffectDap(IDlbProcess *dap) : mProc(dap), mParamCache(dap)
{
    ALOGD("%s()", __FUNCTION__);
    mBypassed = false;
    mEnabled = false;
    mProcessEnabled = false;
}

EffectDap::~EffectDap()
{
    ALOGD("%s()", __FUNCTION__);
    delete mProc;
}

status_t EffectDap::init()
{
    ALOGV("%s()", __FUNCTION__);
    status_t status = mProc->init();
    if (status != NO_ERROR)
    {
        return status;
    }
    return mParamCache.init();
}

status_t EffectDap::process(audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    ALOGVV("%s()", __FUNCTION__);

    mInBuffer.set(inBuffer);
    mOutBuffer.set(outBuffer);

#ifdef DOLBY_AUDIO_DUMP
    mPcmDump.dumpInput(mInBuffer);
#endif

    status_t status = NO_ERROR;

    if (mProcessEnabled)
    {
        status = mProc->process(&mInBuffer, &mOutBuffer);
        if (status != NO_ERROR)
        {
            mProcessEnabled = false;
            if (mBypassed)
            {
                status = NO_ERROR;
            }
        }
    }
    else if (mBypassed)
    {
        status = NO_ERROR;
    }
    else
    {
        status = INVALID_OPERATION;
    }

#ifdef DOLBY_AUDIO_DUMP
    mPcmDump.dumpOutput(mOutBuffer);
#endif

    return status;
}

void EffectDap::setBypass(bool bypass)
{
    ALOGV("%s(bypass=%d)", __FUNCTION__, bypass);
    if (mBypassed != bypass)
    {
        mBypassed = bypass;
        updateProcessEnabled();
    }
}

status_t EffectDap::setEnabled(bool enabled)
{
    ALOGV("%s(enabled=%d)", __FUNCTION__, enabled);
    if (mEnabled != enabled)
    {
        mEnabled = enabled;
        updateProcessEnabled();
    }
    return NO_ERROR;
}

void EffectDap::updateProcessEnabled()
{
    ALOGV("%s(enabled=%d, bypassed=%d)", __FUNCTION__, mEnabled, mBypassed);
    bool enabled = mEnabled && !mBypassed;
    mProc->setEnabled(enabled);
    if (enabled)
    {
        mProcessEnabled = true;
    }
}

status_t EffectDap::setDevice(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    if (device != mParamCache.getDevice())
    {
        mProc->deviceChanged();
    }
    return mParamCache.setDevice(device);
}

status_t EffectDap::setConfig(const buffer_config_t &inCfg, const buffer_config_t &outCfg)
{
    ALOGV("%s()", __FUNCTION__);

    if (inCfg.samplingRate != outCfg.samplingRate)
    {
        ALOGE("Sample rate conversion from %d to %d is not supported.", inCfg.samplingRate, outCfg.samplingRate);
        return BAD_VALUE;
    }

    if (inCfg.format != outCfg.format)
    {
        ALOGE("Audio format conversion from %d to %d is not supported.", inCfg.format, outCfg.format);
        return BAD_VALUE;
    }

    audio_format_t format = static_cast<audio_format_t>(inCfg.format);
    audio_channel_mask_t inChannels = static_cast<audio_channel_mask_t>(inCfg.channels);
    audio_channel_mask_t outChannels = static_cast<audio_channel_mask_t>(outCfg.channels);

    status_t status = mProc->configure(static_cast<int>(inCfg.buffer.frameCount),
        inCfg.samplingRate, format, inChannels, outChannels);
    if (status == NO_INIT)
    {
        // Sample rate change leads to DAP re-initialization, re-apply all the DAP parameters.
        commitAllParams(AUDIO_DEVICE_OUT_DEFAULT);
        status = NO_ERROR;
    }

    if (status == NO_ERROR)
    {
        mInBuffer.configure(format, inChannels);
        mOutBuffer.configure(format, outChannels);
    }
    return status;
}

status_t EffectDap::setParam(audio_devices_t device, DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s()", __FUNCTION__);
    return mParamCache.setParam(device, param, values, length);
}

status_t EffectDap::getParam(audio_devices_t device, DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGV("%s()", __FUNCTION__);
    return mParamCache.getParam(device, param, values, length);
}

status_t EffectDap::commitChangedParams(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    return mParamCache.commitChangedParams(device);
}

status_t EffectDap::commitAllParams(audio_devices_t device)
{
    ALOGV("%s()", __FUNCTION__);
    return mParamCache.commitAllParams(device);
}

status_t EffectDap::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);
    return mProc->getVisualizer(data, bands);
}

} // namespace dolby

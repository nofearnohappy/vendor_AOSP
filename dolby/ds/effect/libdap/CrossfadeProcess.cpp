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
#define LOG_TAG "DlbCrossfadeProcess"

#include "DlbLog.h"
#include "CrossfadeProcess.h"

#define CROSSFADE_SAMPLES   5120
#define DAP_PREROLL_SAMPLES 2048

namespace dolby {

using namespace android;

static const char* kCrossfadeStateNames[] =
    {
        "PREROLL_ACTIVE",
        "FADE_TO_ACTIVE",
        "DAP_ACTIVE",
        "FADE_TO_BYPASS",
        "DAP_BYPASSED",
    };

CrossfadeProcess::CrossfadeProcess(IDlbProcess *dap) : mDap(dap),
    mCrossfadeCounter(CROSSFADE_SAMPLES), mDapPrerollCounter(DAP_PREROLL_SAMPLES)
{
    ALOGD("%s()", __FUNCTION__);
    mState = DAP_BYPASSED;
}

CrossfadeProcess::~CrossfadeProcess()
{
    ALOGD("%s()", __FUNCTION__);
    delete mDap;
}

status_t CrossfadeProcess::init()
{
    ALOGD("%s()", __FUNCTION__);
    return mDap->init();
}

status_t CrossfadeProcess::setParam(DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s()", __FUNCTION__);
    return mDap->setParam(param, values, length);
}

status_t CrossfadeProcess::getParam(DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGV("%s()", __FUNCTION__);
    return mDap->getParam(param, values, length);
}

status_t CrossfadeProcess::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);
    return mDap->getVisualizer(data, bands);
}

status_t CrossfadeProcess::configure(int bufferSize, int sampleRate, audio_format_t format, audio_channel_mask_t inChannels, audio_channel_mask_t outChannels)
{
    ALOGV("%s()", __FUNCTION__);

    if ((format != AUDIO_FORMAT_PCM_16_BIT)
        && (format != AUDIO_FORMAT_PCM_32_BIT)
        && (format != AUDIO_FORMAT_PCM_8_24_BIT))
    {
        ALOGE("%s() Crossfade does not support audio format %d", __FUNCTION__, format);
        return BAD_TYPE;
    }

    if (inChannels != outChannels)
    {
        ALOGE("%s() Crossfade does not support different number of input & output channels", __FUNCTION__);
        return BAD_TYPE;
    }

    status_t status = mDap->configure(bufferSize, sampleRate, format, inChannels, outChannels);
    if (status == NO_ERROR || status == NO_INIT)
    {
        mCrossfadeBuffer.configure(bufferSize, format, inChannels);
    }
    return status;
}

void CrossfadeProcess::updateDapEnabled()
{
    bool enabled = (mState != DAP_BYPASSED);
    ALOGD("%s(enabled=%d)", __FUNCTION__, enabled);
    mDap->setEnabled(enabled);
}

void CrossfadeProcess::setEnabled(bool enable)
{
    ALOGV("%s(enable=%d)", __FUNCTION__, enable);
    CrossfadeState nextState = mState;
    switch (mState)
    {
    case PREROLL_ACTIVE:
        if (!enable)
        {
            nextState = DAP_BYPASSED;
        }
        break;
    case FADE_TO_ACTIVE:
        if (!enable)
        {
            nextState = FADE_TO_BYPASS;
        }
        break;
    case DAP_ACTIVE:
        if (!enable)
        {
            nextState = FADE_TO_BYPASS;
            mCrossfadeCounter.preset();
        }
        break;
    case FADE_TO_BYPASS:
        if (enable)
        {
            nextState = FADE_TO_ACTIVE;
        }
        break;
    case DAP_BYPASSED:
        if (enable)
        {
            nextState = PREROLL_ACTIVE;
            mDapPrerollCounter.reset();
        }
        break;
    }
    if (mState != nextState)
    {
        ALOGD("%s transitioning from %s state to %s", __FUNCTION__,
            kCrossfadeStateNames[mState], kCrossfadeStateNames[nextState]);
        mState = nextState;
        updateDapEnabled();
    }
}

status_t CrossfadeProcess::deviceChanged()
{
    if (mState == FADE_TO_BYPASS)
    {
        mCrossfadeCounter.reset();
        mState = DAP_BYPASSED;
    }
    return NO_ERROR;
}

status_t CrossfadeProcess::process(BufferProvider *inBuffer, BufferProvider *outBuffer)
{
    ALOGVV("%s()", __FUNCTION__);
    CrossfadeState nextState = mState;
    status_t ret = NO_ERROR;

    switch (mState)
    {
    case PREROLL_ACTIVE:
        ret = mDap->process(inBuffer, &mCrossfadeBuffer);
        mDapPrerollCounter.increment(inBuffer->capacity());
        if (mDapPrerollCounter.atEnd())
        {
            nextState = FADE_TO_ACTIVE;
            mCrossfadeCounter.reset();
        }
        break;
    case DAP_ACTIVE:
        ret = mDap->process(inBuffer, outBuffer);
        break;
    case FADE_TO_ACTIVE:
        ret = mDap->process(inBuffer, &mCrossfadeBuffer);
        crossfade(inBuffer, outBuffer, true);
        if (mCrossfadeCounter.atEnd())
        {
            mCrossfadeCounter.preset();
            nextState = DAP_ACTIVE;
        }
        break;
    case FADE_TO_BYPASS:
        ret = mDap->process(inBuffer, &mCrossfadeBuffer);
        crossfade(inBuffer, outBuffer, false);
        if (mCrossfadeCounter.atStart())
        {
            mCrossfadeCounter.reset();
            nextState = DAP_BYPASSED;
        }
        break;
    case DAP_BYPASSED:
        return INVALID_OPERATION;
    }

    if (ret != NO_ERROR)
    {
        nextState = DAP_BYPASSED;
    }

    if (mState != nextState)
    {
        ALOGD("%s transitioning from %s state to %s", __FUNCTION__,
            kCrossfadeStateNames[mState], kCrossfadeStateNames[nextState]);
        mState = nextState;
        updateDapEnabled();
    }

    return ret;
}

void CrossfadeProcess::crossfade(BufferProvider *inBuffer, BufferProvider *outBuffer, bool increment)
{
    ALOGVV("%s()", __FUNCTION__);

    int numSamples = inBuffer->capacity();
    uint8_t *in_buf = inBuffer->raw();
    uint8_t *out_buf = outBuffer->raw();
    uint8_t *dap_buf = mCrossfadeBuffer.raw();
    int stride = inBuffer->stride();
    int channels = inBuffer->channels();
    bool pcm16bit = (inBuffer->buffer()->data_type == DLB_BUFFER_SHORT_16);

    for (int i = 0; i < numSamples; ++i)
    {
        float dap_coeff = mCrossfadeCounter.ratio();
        float in_coeff = 1.0 - dap_coeff;

        if (pcm16bit)
        {
            accumulate(reinterpret_cast<int16_t*>(out_buf),
                reinterpret_cast<int16_t*>(in_buf),
                reinterpret_cast<int16_t*>(dap_buf),
                channels, in_coeff, dap_coeff);
        }
        else
        {
            accumulate(reinterpret_cast<int32_t*>(out_buf),
                reinterpret_cast<int32_t*>(in_buf),
                reinterpret_cast<int32_t*>(dap_buf),
                channels, in_coeff, dap_coeff);
        }

        in_buf += stride;
        out_buf += stride;
        dap_buf += stride;

        if (increment)
        {
            mCrossfadeCounter.increment(1);
        }
        else
        {
            mCrossfadeCounter.decrement(1);
        }
    }
}

/// @internal
/// Clamps the sample to a valid 16-bit audio PCM sample range.
///
/// @note This inline function should be optimised into CPU-specific assembly instructions
///  e.g.  asm ("ssat %[Rd], 16, %[Rm]" : [Rd] "=r" (sample) :[Rm] "r" (sample));
///
/// @param sample Sample to clamp to valid 16-bit PCM range.
/// @return The sample value, clamped to 16-bit range.
///
static inline int16_t clamp(int32_t sample)
{
    if ((sample>>15) ^ (sample>>31))
    {
        sample = 0x7FFF ^ (sample>>31);
    }
    return sample;
}

void CrossfadeProcess::accumulate(int16_t *out_buf, int16_t *in_buf, int16_t *dap_buf, int num_samples, float in_coeff, float dap_coeff)
{
    for (int i = 0; i < num_samples; ++i)
    {
        out_buf[i] = clamp(static_cast<int32_t>((dap_buf[i] * dap_coeff) + (in_buf[i] * in_coeff)));
    }
}

/// @internal
/// Clamps the sample to a valid 32-bit audio PCM sample range.
///
/// @param sample Sample to clamp to valid 32-bit PCM range.
/// @return The sample value, clamped to 32-bit range.
///
static inline int32_t clamp(int64_t sample)
{
    if ((sample>>31) ^ (sample>>63))
    {
        sample = 0x7FFFFFFF ^ (sample>>63);
    }
    return sample;
}

void CrossfadeProcess::accumulate(int32_t *out_buf, int32_t *in_buf, int32_t *dap_buf, int num_samples, float in_coeff, float dap_coeff)
{
    ALOGVV("%s32()", __FUNCTION__);
    for (int i = 0; i < num_samples; ++i)
    {
        out_buf[i] = clamp(static_cast<int64_t>((dap_buf[i] * dap_coeff) + (in_buf[i] * in_coeff)));
    }
}

} // namespace dolby

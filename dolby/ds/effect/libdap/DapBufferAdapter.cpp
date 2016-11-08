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
#define LOG_TAG "DlbDapBufferAdapter"

#include "DlbLog.h"
#include "DapBufferAdapter.h"

namespace dolby {

using namespace android;

DapBufferAdapter::DapBufferAdapter(IDlbProcess *dap) : mDap(dap),
    mInDlbSink(mInDlbBuf), mOutDlbSource(mOutDlbBuf)
{
    ALOGD("%s()", __FUNCTION__);
}

DapBufferAdapter::~DapBufferAdapter()
{
    delete mDap;
}

status_t DapBufferAdapter::init()
{
    ALOGD("%s()", __FUNCTION__);
    return mDap->init();
}

void DapBufferAdapter::setEnabled(bool enabled)
{
    ALOGV("%s(enabled=%d)", __FUNCTION__, enabled);
    mDap->setEnabled(enabled);
}

status_t DapBufferAdapter::getParam(DapParameterId param, dap_param_value_t* values, int* length)
{
    ALOGV("%s()", __FUNCTION__);
    return mDap->getParam(param, values, length);
}

status_t DapBufferAdapter::setParam(DapParameterId param, const dap_param_value_t* values, int length)
{
    ALOGV("%s()", __FUNCTION__);
    return mDap->setParam(param, values, length);
}

status_t DapBufferAdapter::getVisualizer(dap_param_value_t *data, int *bands)
{
    ALOGVV("%s()", __FUNCTION__);
    return mDap->getVisualizer(data, bands);
}

status_t DapBufferAdapter::configure(int bufferSize, int sampleRate, audio_format_t format, audio_channel_mask_t inChannels, audio_channel_mask_t outChannels)
{
    ALOGV("%s()", __FUNCTION__);
    status_t status = mDap->configure(bufferSize, sampleRate, format, inChannels, outChannels);
    if (status == NO_ERROR || status == NO_INIT)
    {
        mInDlbBuf.configure(NUM_PCM_SAMPLES_PER_BLOCK, format, inChannels);
        mOutDlbBuf.configure(NUM_PCM_SAMPLES_PER_BLOCK, format, outChannels);
        mInDlbSink.reset();
        mOutDlbSource.fillWithSilence();
    }
    return status;
}

status_t DapBufferAdapter::process(BufferProvider *inBuffer, BufferProvider *outBuffer)
{
    ALOGVV("%s()", __FUNCTION__);

    // Set audio buffers for input & output
    BufferSource inAudioSource(*inBuffer);
    BufferSink outAudioSink(*outBuffer);

    inAudioSource.reset();
    outAudioSink.reset();

    // Process all data in input buffer
    ALOGVV("%s() Received audio buffer with %d samples", __FUNCTION__, inAudioSource.size());
    status_t status = NO_ERROR;
    while (status == NO_ERROR && !inAudioSource.empty())
    {
        // Add NUM_PCM_SAMPLES_PER_BLOCK samples into input dlb_buffer and
        // remove the same number of samples from output buffer.
        int consumedSamples = mInDlbSink.consume(inAudioSource, inAudioSource.size());
        outAudioSink.consume(mOutDlbSource, consumedSamples);
        ALOGVV("%s() Copied %d samples from input audio buffer (%d remaining)",
            __FUNCTION__, consumedSamples, inAudioSource.size());
        // When input dlb_buffer is full, give it to DAP for processing
        if (mInDlbSink.full())
        {
            ALOGVV("%s() Calling DAP process() with %d samples", __FUNCTION__, mInDlbSink.size());
            status = mDap->process(&mInDlbBuf, &mOutDlbBuf);
            // All samples in input buffer are processed into output buffer so reset both sinks/sources.
            mInDlbSink.reset();
            mOutDlbSource.reset();
        }
    }
    return status;
}

} // namespace dolby

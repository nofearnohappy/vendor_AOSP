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

#define LOG_TAG "DlbDlbBufferProvider"

#include "DlbLog.h"
#include "DlbBufferProvider.h"

namespace dolby {

using namespace android;

BufferProvider::BufferProvider()
{
    ALOGD("%s()", __FUNCTION__);
    mBuffer.ppdata = 0;
    mBuffer.data_type = 0;
    mBuffer.nchannel = 0;
    mBuffer.nstride = 0;
    mCapacity = 0;
    mStride = 0;
}

BufferProvider::~BufferProvider()
{
    ALOGD("%s()", __FUNCTION__);
    delete[] mBuffer.ppdata;
}

void BufferProvider::configure(audio_format_t format, audio_channel_mask_t channels)
{
    ALOGV("%s()", __FUNCTION__);
    int dlb_data_type = 0, dlb_data_type_size = 0;
    switch(format)
    {
    case AUDIO_FORMAT_PCM_16_BIT:
        dlb_data_type = DLB_BUFFER_SHORT_16;
        dlb_data_type_size = sizeof(int16_t);
        break;
    case AUDIO_FORMAT_PCM_32_BIT:
    case AUDIO_FORMAT_PCM_8_24_BIT:
        dlb_data_type = DLB_BUFFER_LONG_32;
        dlb_data_type_size = sizeof(int32_t);
        break;
    default:
        LOG_FATAL("Audio format %d is not supported.", format);
    };

    unsigned dlb_channels = 0;
    switch (channels)
    {
    case AUDIO_CHANNEL_OUT_STEREO:
        dlb_channels = 2;
        break;
    case AUDIO_CHANNEL_OUT_5POINT1:
        dlb_channels = 6;
        break;
    case AUDIO_CHANNEL_OUT_7POINT1:
        dlb_channels = 8;
        break;
    default:
        LOG_FATAL("Audio channel configuration %d is not supported.", channels);
    }

    if (mBuffer.data_type != dlb_data_type || mBuffer.nchannel != dlb_channels)
    {
        ALOGD("%s() Updating buffer configuration.", __FUNCTION__);
        delete[] mBuffer.ppdata;
        mBuffer.data_type = dlb_data_type;
        mBuffer.nchannel = dlb_channels;
        mBuffer.nstride = dlb_channels;
        mBuffer.ppdata = new void*[dlb_channels];
        for (unsigned i = 0; i < dlb_channels; ++i)
        {
            mBuffer.ppdata[i] = NULL;
        }
        mStride = dlb_data_type_size * dlb_channels;
    }
}

void BufferProvider::setBuffer(void *buffer)
{
    ALOGVV("%s()", __FUNCTION__);
    uint8_t *buf = reinterpret_cast<uint8_t*>(buffer);
    int channel_stride = mStride / mBuffer.nchannel;
    for (unsigned i = 0; i < mBuffer.nchannel; ++i)
    {
        mBuffer.ppdata[i] = buf;
        buf += channel_stride;
    }
}

void AudioBufferProvider::set(audio_buffer_t *buffer)
{
    ALOGVV("%s()", __FUNCTION__);
    setBuffer(buffer->raw);
    mCapacity = buffer->frameCount;
}

DlbBufferProvider::DlbBufferProvider()
{
    ALOGD("%s()", __FUNCTION__);
    mData = NULL;
}

DlbBufferProvider::~DlbBufferProvider()
{
    ALOGD("%s()", __FUNCTION__);
    delete[] mData;
}

void DlbBufferProvider::configure(int capacity, audio_format_t format, audio_channel_mask_t channels)
{
    ALOGV("%s()", __FUNCTION__);
    int old_capacity_in_bytes = mCapacity * mStride;
    BufferProvider::configure(format, channels);
    int new_capacity_in_bytes = capacity * mStride;
    mCapacity = capacity;
    // Only allocate a new buffer if more memory is required
    if (old_capacity_in_bytes < new_capacity_in_bytes)
    {
        ALOGD("%s() Allocating new memory for the buffer.", __FUNCTION__);
        delete[] mData;
        mData = new uint8_t[new_capacity_in_bytes];
    }
    // Update the ppdata member with buffer details
    setBuffer(mData);
}

void BufferSource::fillWithSilence()
{
    ALOGV("%s()", __FUNCTION__);
    reset();
    int bytesToFill = mBuffer.stride() * mBuffer.capacity();
    memset(mCurrent, 0, bytesToFill);
}

int BufferSink::consume(BufferSource &source, int numSamples)
{
    ALOGVV("%s()", __FUNCTION__);
    int maxSamplesToCopy = min(source.mSize, (mBuffer.capacity() - mSize));
    int samplesToCopy = min(numSamples, maxSamplesToCopy);

    int bytesToCopy = samplesToCopy * mBuffer.stride();
    memcpy(mCurrent, source.mCurrent, bytesToCopy);

    mCurrent += bytesToCopy;
    mSize += samplesToCopy;

    source.mCurrent += bytesToCopy;
    source.mSize -= samplesToCopy;

    return samplesToCopy;
}

}

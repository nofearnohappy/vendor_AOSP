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

#ifndef DOLBY_DLB_BUFFER_PROVIDER_H_
#define DOLBY_DLB_BUFFER_PROVIDER_H_

#include <utils/Errors.h>
#include <hardware/audio_effect.h>
extern "C" {
#include "dlb_buffer.h"
}
#include <gtest/gtest_prod.h>

#define min(x, y) (((x) < (y)) ? (x) : (y))
#define max(x, y) (((x) > (y)) ? (x) : (y))

namespace dolby {

using namespace android;

class BufferProvider
{
public:
    BufferProvider();
    virtual     ~BufferProvider();

    dlb_buffer *buffer()
    { return &mBuffer; }

    uint8_t *raw()
    { return reinterpret_cast<uint8_t*>(*mBuffer.ppdata); }

    const uint8_t *raw() const
    { return reinterpret_cast<uint8_t*>(*mBuffer.ppdata); }

    int capacity() const
    { return mCapacity; }

    int stride() const
    { return mStride; }

    int channels() const
    { return static_cast<int>(mBuffer.nchannel); }

    void configure(audio_format_t format, audio_channel_mask_t channels);

protected:
    FRIEND_TEST(ConfigureTest, BufferProvider_setBuffer);
    FRIEND_TEST(BufferProviderTest, setBuffer_reset_high);
    FRIEND_TEST(BufferProviderTest, setBuffer_reset_low);

    void setBuffer(void *buffer);

    dlb_buffer mBuffer;
    int mCapacity;
    int mStride;
};

class AudioBufferProvider : public BufferProvider
{
public:
    void set(audio_buffer_t *audioBuf);
};

class DlbBufferProvider : public BufferProvider
{
public:
    DlbBufferProvider();
    ~DlbBufferProvider();

    void configure(int capacity, audio_format_t format, audio_channel_mask_t channels);

protected:
    uint8_t *mData;
};

class BufferSource
{
public:
    BufferSource(BufferProvider &buf) : mBuffer(buf)
    { mCurrent = NULL; mSize = 0; }

    void reset()
    { mCurrent = mBuffer.raw(); mSize = mBuffer.capacity(); }

    int size() const
    { return mSize; }

    bool full() const
    { return mSize >= mBuffer.capacity(); }

    bool empty() const
    { return mSize <= 0; }

    void fillWithSilence();

private:
    friend class BufferSink;
    BufferProvider &mBuffer;
    uint8_t *mCurrent;
    int mSize;
};

class BufferSink
{
public:
    BufferSink(BufferProvider &buf) : mBuffer(buf)
    { mCurrent = NULL; mSize = 0; }

    void reset()
    { mCurrent = mBuffer.raw(); mSize = 0; }

    int size() const
    { return mSize; }

    bool full() const
    { return mSize >= mBuffer.capacity(); }

    bool empty() const
    { return mSize <= 0; }

    int consume(BufferSource &source, int numSamples);

private:
    BufferProvider &mBuffer;
    uint8_t *mCurrent;
    int mSize;
};

} // namespace dolby
#endif//DOLBY_DLB_BUFFER_PROVIDER_H_

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

#include "gtest/gtest.h"
#include "DlbBufferProvider.h"

namespace dolby {

using namespace testing;
using namespace android;

#define NUM_ELEMS(arr) (sizeof(arr)/sizeof(arr[0]))

// Struct for supported and unsupported audio formats
struct ConfigureTestFormatData
{
    audio_format_t format;
    int data_type;
    int size;
};

// Struct for supported and unsupported channel configurations
struct ConfigureTestChannelData
{
    audio_channel_mask_t channels;
    unsigned nchannel;
    ptrdiff_t nstride;
};

// Class to provide access to channel & format test data.
class ConfigureTest : public TestWithParam< std::tr1::tuple<ConfigureTestFormatData, ConfigureTestChannelData> >
{
protected:
    virtual void SetUp()
    {
        fmt = std::tr1::get<0>(GetParam());
        chan = std::tr1::get<1>(GetParam());
    }

    ConfigureTestFormatData fmt;
    ConfigureTestChannelData chan;
};

// Verify that number of channels & format are mapped correctly to dlb_buffer
TEST_P(ConfigureTest, BufferProvider_configure)
{
    BufferProvider bp;

    bp.configure(fmt.format, chan.channels);
    ASSERT_EQ(fmt.data_type, bp.buffer()->data_type);
    ASSERT_EQ(chan.nchannel, bp.buffer()->nchannel);
    ASSERT_EQ(chan.nstride, bp.buffer()->nstride);

    for (unsigned c = 0; c < chan.nchannel; ++c)
    {
        ASSERT_EQ(NULL, bp.buffer()->ppdata[c]);
    }
}

// Verify that setBuffer correctly populates ppdata values
TEST_P(ConfigureTest, BufferProvider_setBuffer)
{
    uint8_t buf[] = { 0 };
    BufferProvider bp;

    bp.configure(fmt.format, chan.channels);
    bp.setBuffer(buf);

    for (unsigned c = 0; c < chan.nchannel; ++c)
    {
        ASSERT_EQ((buf + (c * fmt.size)), bp.buffer()->ppdata[c]);
    }
}

// Verify that setBuffer correctly populates ppdata values on change in number of channels
TEST(BufferProviderTest, setBuffer_reset_high)
{
    uint8_t buf[] = { 0 };
    BufferProvider bp;

    // Set configuration to 16 bit stereo
    bp.configure(AUDIO_FORMAT_PCM_16_BIT, AUDIO_CHANNEL_OUT_STEREO);
    bp.setBuffer(buf);

    // Change to 32 bit 5.1 and verify
    bp.configure(AUDIO_FORMAT_PCM_32_BIT, AUDIO_CHANNEL_OUT_5POINT1);
    bp.setBuffer(buf);

    for (unsigned c = 0; c < 6; ++c)
    {
        ASSERT_EQ((buf + (c * sizeof(int32_t))), bp.buffer()->ppdata[c]);
    }
}

// Verify that setBuffer correctly populates ppdata values on change in number of channels
TEST(BufferProviderTest, setBuffer_reset_low)
{
    uint8_t buf[] = { 0 };
    BufferProvider bp;

    // Set configuration to 32 bit 5.1
    bp.configure(AUDIO_FORMAT_PCM_32_BIT, AUDIO_CHANNEL_OUT_5POINT1);
    bp.setBuffer(buf);

    // Change to 16 bit stereo and verify
    bp.configure(AUDIO_FORMAT_PCM_16_BIT, AUDIO_CHANNEL_OUT_STEREO);
    bp.setBuffer(buf);

    for (unsigned c = 0; c < 2; ++c)
    {
        ASSERT_EQ((buf + (c * sizeof(int16_t))), bp.buffer()->ppdata[c]);
    }
}

// Verify that audio buffer is correctly represented as dlb_buffer
TEST_P(ConfigureTest, AudioBufferProvider_set)
{
    int kNumFrames = 42;
    uint8_t buf[] = { 0 };

    // Create dummy audio buf
    audio_buffer_t ab;
    ab.frameCount = kNumFrames;
    ab.u8 = buf;

    AudioBufferProvider bp;

    bp.configure(fmt.format, chan.channels);
    bp.set(&ab);

    ASSERT_EQ((int)ab.frameCount, bp.capacity());
    ASSERT_EQ(buf, bp.buffer()->ppdata[0]);
}

// Verify that dlb_buffer is correctly allocated after configuration
TEST_P(ConfigureTest, DlbBufferProvider_configure)
{
    const int kTestCapacity = 231;
    DlbBufferProvider bp;

    bp.configure(kTestCapacity, fmt.format, chan.channels);

    ASSERT_EQ(fmt.data_type, bp.buffer()->data_type);
    ASSERT_EQ(chan.nchannel, bp.buffer()->nchannel);
    ASSERT_EQ(chan.nstride, bp.buffer()->nstride);
    ASSERT_TRUE(bp.raw());

    // Fill the allocated buffer with data to trigger a valgrind error
    int buf_size = kTestCapacity * fmt.size * chan.nchannel;
    for (int k = 0; k < buf_size; ++k)
    {
        bp.raw()[k] = 0xaa;
    }
}

// Verify that dlb_buffer is correctly allocated after changing configuration
TEST(DlbBufferProvider, configure_reset_high)
{
    const int kTestCapacity = 231;

    DlbBufferProvider bp;

    // Set configuration to 16 bit stereo
    bp.configure(kTestCapacity, AUDIO_FORMAT_PCM_16_BIT, AUDIO_CHANNEL_OUT_STEREO);

    // Change to 32 bit 5.1 and verify
    bp.configure(kTestCapacity, AUDIO_FORMAT_PCM_32_BIT, AUDIO_CHANNEL_OUT_5POINT1);

    // Fill the allocated buffer with data to trigger a valgrind error
    int buf_size = kTestCapacity * sizeof(int32_t) * 6;
    for (int k = 0; k < buf_size; ++k)
    {
        bp.raw()[k] = 0xaa;
    }
}

// Verify that dlb_buffer is correctly allocated after changing configuration
TEST(DlbBufferProvider, configure_reset_low)
{
    const int kTestCapacity = 231;

    DlbBufferProvider bp;

    // Set configuration to 32 bit 5.1
    bp.configure(kTestCapacity, AUDIO_FORMAT_PCM_32_BIT, AUDIO_CHANNEL_OUT_5POINT1);

    // Change to 16 bit stereo and verify
    bp.configure(kTestCapacity, AUDIO_FORMAT_PCM_16_BIT, AUDIO_CHANNEL_OUT_STEREO);

    // Fill the allocated buffer with data to trigger a valgrind error
    int buf_size = kTestCapacity * sizeof(int16_t) * 2;
    for (int k = 0; k < buf_size; ++k)
    {
        bp.raw()[k] = 0xaa;
    }
}

// Create test cases for all combinations of channel & format configurations

static ConfigureTestFormatData mConfigureTestFormatData[] = {
    { AUDIO_FORMAT_PCM_16_BIT, DLB_BUFFER_SHORT_16, sizeof(int16_t) },
    { AUDIO_FORMAT_PCM_32_BIT, DLB_BUFFER_LONG_32, sizeof(int32_t) },
    { AUDIO_FORMAT_PCM_8_24_BIT, DLB_BUFFER_LONG_32, sizeof(int32_t) },
};

static ConfigureTestChannelData mConfigureTestChannelData[] = {
    { AUDIO_CHANNEL_OUT_STEREO, 2, 2 },
    { AUDIO_CHANNEL_OUT_5POINT1, 6, 6 },
    { AUDIO_CHANNEL_OUT_7POINT1, 8, 8 },
};

INSTANTIATE_TEST_CASE_P(DlbBufferProviderConfigureTests, ConfigureTest,
    Combine(ValuesIn(mConfigureTestFormatData), ValuesIn(mConfigureTestChannelData)));

}

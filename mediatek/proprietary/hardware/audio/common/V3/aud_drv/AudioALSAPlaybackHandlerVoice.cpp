#include "AudioALSAPlaybackHandlerVoice.h"

#include "SpeechDriverFactory.h"
#include "SpeechBGSPlayer.h"


#define LOG_TAG "AudioALSAPlaybackHandlerVoice"

namespace android
{

AudioALSAPlaybackHandlerVoice::AudioALSAPlaybackHandlerVoice(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerBase(stream_attribute_source),
    mBGSPlayer(BGSPlayer::GetInstance())
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_VOICE;
}


AudioALSAPlaybackHandlerVoice::~AudioALSAPlaybackHandlerVoice()
{
    ALOGD("%s()", __FUNCTION__);
}

status_t AudioALSAPlaybackHandlerVoice::open()
{
    ALOGD("+%s(), audio_mode = %d, u8BGSUlGain = %d, u8BGSDlGain = %d", __FUNCTION__, mStreamAttributeSource->audio_mode, mStreamAttributeSource->u8BGSUlGain, mStreamAttributeSource->u8BGSDlGain);

    ALOGD("%s() mStreamAttributeSource->audio_format =%d", __FUNCTION__,mStreamAttributeSource->audio_format);

    // debug pcm dump
    OpenPCMDump(LOG_TAG);


    // HW attribute config // TODO(Harvey): query this
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeTarget.audio_channel_mask = mStreamAttributeSource->audio_channel_mask; // same as source stream
    mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);
    mStreamAttributeTarget.sample_rate = ChooseTargetSampleRate(mStreamAttributeSource->sample_rate); // same as source stream
    ALOGD("mStreamAttributeTarget sample_rate = %d mStreamAttributeSource sample_rate = %d",mStreamAttributeTarget.sample_rate,mStreamAttributeSource->sample_rate);
    mStreamAttributeTarget.u8BGSDlGain = mStreamAttributeSource->u8BGSDlGain;
    mStreamAttributeTarget.u8BGSUlGain = mStreamAttributeSource->u8BGSUlGain;

    // bit conversion
    initBitConverter();


    // open background sound
    mBGSPlayer->mBGSMutex.lock();

    if (mStreamAttributeTarget.num_channels > 2)
    {
        mBGSPlayBuffer = mBGSPlayer->CreateBGSPlayBuffer(
            mStreamAttributeTarget.sample_rate,
            2,
            mStreamAttributeTarget.audio_format);

    }
    else
    {
        mBGSPlayBuffer = mBGSPlayer->CreateBGSPlayBuffer(
            mStreamAttributeTarget.sample_rate,
            mStreamAttributeTarget.num_channels,
            mStreamAttributeTarget.audio_format);
    }
    mBGSPlayer->Open(SpeechDriverFactory::GetInstance()->GetSpeechDriver(), mStreamAttributeTarget.u8BGSUlGain, mStreamAttributeTarget.u8BGSDlGain);

    mBGSPlayer->mBGSMutex.unlock();

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerVoice::close()
{
    ALOGD("+%s()", __FUNCTION__);

    // close background sound
    mBGSPlayer->mBGSMutex.lock();

    mBGSPlayer->Close();

    mBGSPlayer->DestroyBGSPlayBuffer(mBGSPlayBuffer);

    mBGSPlayer->mBGSMutex.unlock();


    // bit conversion
    deinitBitConverter();


    // debug pcm dump
    ClosePCMDump();


    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerVoice::routing(const audio_devices_t output_devices)
{
    return INVALID_OPERATION;
}

uint32_t AudioALSAPlaybackHandlerVoice::ChooseTargetSampleRate(uint32_t SampleRate)
{
    ALOGD("ChooseTargetSampleRate SampleRate = %d ",SampleRate);
    uint32_t TargetSampleRate = 44100;
    if( (SampleRate% 8000) == 0) // 8K base
    {
        TargetSampleRate = 48000;
    }
    return TargetSampleRate;
}
status_t AudioALSAPlaybackHandlerVoice::pause()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerVoice::resume()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerVoice::flush()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerVoice::setVolume(uint32_t vol)
{
    return INVALID_OPERATION;
}

int AudioALSAPlaybackHandlerVoice::drain(audio_drain_type_t type)
{
    return 0;
}

ssize_t AudioALSAPlaybackHandlerVoice::write(const void *buffer, size_t bytes)
{
    ALOGV("%s()", __FUNCTION__);

    void *newbuffer[96 * 1024] = {0};
    unsigned char *aaa;
    unsigned char *bbb;
    int i = 0;
    int j = 0;
	int retval = 0;
    // const -> to non const
    void *pBuffer = const_cast<void *>(buffer);
    ASSERT(pBuffer != NULL);

    aaa = (unsigned char *)newbuffer;
    bbb = (unsigned char *)buffer;


    if (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_16_BIT)
    {
        if (mStreamAttributeTarget.num_channels == 8)
        {
            for (i = 0 ; j < bytes; i += 4)
            {
                memcpy(aaa + i, bbb + j, 4);
                j += 16;
            }
            bytes = (bytes >> 2);
        }
        else if (mStreamAttributeTarget.num_channels == 6)
        {
            for (i = 0 ; j < bytes; i += 4)
            {
                memcpy(aaa + i, bbb + j, 4);
                j += 12;
            }
            bytes = (bytes/3);
        }
        else
        {
            memcpy(aaa, bbb , bytes);
        }
    }
    else
    {
        if (mStreamAttributeTarget.num_channels == 8)
        {
            for (i = 0 ; j < bytes; i += 8)
            {
                memcpy(aaa + i, bbb + j, 8);
                j += 32;
            }
            bytes = (bytes >> 2);
        }
        else if (mStreamAttributeTarget.num_channels == 6)
        {
            for (i = 0 ; j < bytes; i += 8)
            {
                memcpy(aaa + i, bbb + j, 8);
                j += 24;
            }
            bytes = (bytes/3);
        }
        else
        {
            memcpy(aaa, bbb , bytes);
        }

    }

    // bit conversion
    void *pBufferAfterBitConvertion = NULL;
    uint32_t bytesAfterBitConvertion = 0;
    doBitConversion(newbuffer, bytes, &pBufferAfterBitConvertion, &bytesAfterBitConvertion);


    // write data to background sound
    WritePcmDumpData(pBufferAfterBitConvertion, bytesAfterBitConvertion);
    uint32_t u4WrittenBytes = BGSPlayer::GetInstance()->Write(mBGSPlayBuffer, pBufferAfterBitConvertion, bytesAfterBitConvertion);
    if (u4WrittenBytes != bytesAfterBitConvertion) // TODO: 16/32
    {
        ALOGE("%s(), BGSPlayer::GetInstance()->Write() error, u4WrittenBytes(%u) != bytesAfterBitConvertion(%u)", __FUNCTION__, u4WrittenBytes, bytesAfterBitConvertion);
    }

    return bytes;
}


} // end of namespace android

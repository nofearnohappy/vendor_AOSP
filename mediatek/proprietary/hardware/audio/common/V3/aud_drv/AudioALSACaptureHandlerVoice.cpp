#include "AudioALSACaptureHandlerVoice.h"
#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderVoice.h"
#include "AudioALSACaptureDataProviderVoiceDL.h"
#include "AudioALSACaptureDataProviderVoiceUL.h"
#include "AudioALSACaptureDataProviderVoiceMix.h"
#include "AudioType.h"
#include "audio_custom_exp.h"

#define LOG_TAG "AudioALSACaptureHandlerVoice"

namespace android
{

AudioALSACaptureHandlerVoice::AudioALSACaptureHandlerVoice(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);

    init();
}


AudioALSACaptureHandlerVoice::~AudioALSACaptureHandlerVoice()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerVoice::init()
{
    ALOGD("%s()", __FUNCTION__);

    return NO_ERROR;
}


status_t AudioALSACaptureHandlerVoice::open()
{
    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source);

    ASSERT(mCaptureDataClient == NULL);

#ifndef LEGACY_VOICE_RECORD
    switch (mStreamAttributeTarget->input_source)
    {
#ifdef INCALL_DL_RECORD_DISABLED
        default:
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderVoiceUL::getInstance(), mStreamAttributeTarget);
            break;
        }
#else
        case AUDIO_SOURCE_VOICE_DOWNLINK:
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderVoiceDL::getInstance(), mStreamAttributeTarget);
            break;
        }
        case AUDIO_SOURCE_VOICE_UPLINK:
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderVoiceUL::getInstance(), mStreamAttributeTarget);
            break;
        }
        default:
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderVoiceMix::getInstance(), mStreamAttributeTarget);
            break;
        }
#endif
    }
#else
    // legacy voice record
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderVoice::getInstance(), mStreamAttributeTarget);
#endif

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerVoice::close()
{
    ALOGD("+%s()", __FUNCTION__);

    ASSERT(mCaptureDataClient != NULL);
    delete mCaptureDataClient;

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerVoice::routing(const audio_devices_t input_device)
{
    WARNING("Not support!!"); // TODO(Harvey): check it
    return INVALID_OPERATION;
}


ssize_t AudioALSACaptureHandlerVoice::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s()", __FUNCTION__);

    return mCaptureDataClient->read(buffer, bytes);
}

} // end of namespace android

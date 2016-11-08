#include "AudioALSACaptureHandlerANC.h"

#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderANC.h"
#include "AudioALSAStreamManager.h"



#define LOG_TAG "AudioALSACaptureHandlerANC"

namespace android
{

AudioALSACaptureHandlerANC::AudioALSACaptureHandlerANC(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);

    init();
}


AudioALSACaptureHandlerANC::~AudioALSACaptureHandlerANC()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerANC::init()
{
    ALOGD("%s()", __FUNCTION__);


    return NO_ERROR;
}


status_t AudioALSACaptureHandlerANC::open()
{
    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source);
    if (mSupportConcurrencyInCall == false && AudioALSAStreamManager::getInstance()->isModeInPhoneCall() == true)
    {
        mCaptureDataClient = NULL;
        ALOGD("-%s() don't support ANC Record at incall mode", __FUNCTION__);
        return NO_ERROR;
    }
    // TODO: check ANC is already opened?

    ASSERT(mCaptureDataClient == NULL);
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderANC::getInstance(), mStreamAttributeTarget);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerANC::close()
{
    ALOGD("+%s()", __FUNCTION__);

    if (mCaptureDataClient != NULL)
    {
        //ASSERT(mCaptureDataClient != NULL);
        delete mCaptureDataClient;
    }

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerANC::routing(const audio_devices_t input_device)
{
    WARNING("Not support!!");
    return INVALID_OPERATION;
}


ssize_t AudioALSACaptureHandlerANC::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s()", __FUNCTION__);
    if (mCaptureDataClient == NULL)
    {
        memset(buffer, 0, bytes);
        size_t wordSize = 0;
        switch (mStreamAttributeTarget->audio_format)
        {
            case AUDIO_FORMAT_PCM_8_BIT:
            {
                wordSize = sizeof(int8_t);
                break;
            }
            case AUDIO_FORMAT_PCM_16_BIT:
            {
                wordSize = sizeof(int16_t);
                break;
            }
            case AUDIO_FORMAT_PCM_8_24_BIT:
            case AUDIO_FORMAT_PCM_32_BIT:
            {
                wordSize = sizeof(int32_t);
                break;
            }
            default:
            {
                ALOGW("%s(), wrong format(0x%x), default use wordSize = %d", __FUNCTION__, mStreamAttributeTarget->audio_format, sizeof(int16_t));
                wordSize = sizeof(int16_t);
                break;
            }
        }
        int sleepus = ((bytes * 1000) / ((mStreamAttributeTarget->sample_rate / 1000) * mStreamAttributeTarget->num_channels * wordSize));
        ALOGD("%s(), sleepus = %d", __FUNCTION__, sleepus);
        usleep(sleepus);
        return bytes;
    }
    return mCaptureDataClient->read(buffer, bytes);
}

} // end of namespace android

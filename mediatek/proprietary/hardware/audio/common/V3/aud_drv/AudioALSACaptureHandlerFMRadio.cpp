#include "AudioALSACaptureHandlerFMRadio.h"

#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderFMRadio.h"
#include "AudioALSAStreamManager.h"



#define LOG_TAG "AudioALSACaptureHandlerFMRadio"

namespace android
{

AudioALSACaptureHandlerFMRadio::AudioALSACaptureHandlerFMRadio(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);
    mSupportConcurrencyInCall = false;
    init();
}


AudioALSACaptureHandlerFMRadio::~AudioALSACaptureHandlerFMRadio()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerFMRadio::init()
{
    ALOGD("%s()", __FUNCTION__);


    return NO_ERROR;
}


status_t AudioALSACaptureHandlerFMRadio::open()
{
    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source);

    if (mSupportConcurrencyInCall == false && AudioALSAStreamManager::getInstance()->isModeInPhoneCall() == true)//For Google FM, app keeps recording when entering into InCall Mode
    {
        mCaptureDataClient = NULL;
        ALOGD("-%s() don't support FM Record at incall mode", __FUNCTION__);
        return NO_ERROR;
    }
    else if (AudioALSAStreamManager::getInstance()->getFmEnable() == false)//For Google FM, app keeps recording when entering into Normal Mode from InCall
    {
        ALOGW("StreamIn resume FM enable (App keep reading,howerver HAL disable FM for InCall)");
        AudioALSAStreamManager::getInstance()->setFmEnable(true,true,false);
    }
    // TODO(Harvey): check FM is already opened?

    ASSERT(mCaptureDataClient == NULL);
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderFMRadio::getInstance(), mStreamAttributeTarget);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerFMRadio::close()
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


status_t AudioALSACaptureHandlerFMRadio::routing(const audio_devices_t input_device)
{
    WARNING("Not support!!");
    return INVALID_OPERATION;
}


ssize_t AudioALSACaptureHandlerFMRadio::read(void *buffer, ssize_t bytes)
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

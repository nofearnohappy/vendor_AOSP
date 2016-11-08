
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)

#include "AudioALSACaptureHandlerSpkFeed.h"

#include "AudioALSAHardwareResourceManager.h"
#include "AudioVolumeFactory.h"

#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderSpkFeed.h"
#define LOG_TAG "AudioALSACaptureHandlerSpkFeed"

namespace android
{

static FILE *pOutFile = NULL;

AudioALSACaptureHandlerSpkFeed::AudioALSACaptureHandlerSpkFeed(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);
    init();
}


AudioALSACaptureHandlerSpkFeed::~AudioALSACaptureHandlerSpkFeed()
{
    ALOGD("%s()+", __FUNCTION__);
}


status_t AudioALSACaptureHandlerSpkFeed::init()
{
    ALOGD("%s()", __FUNCTION__);

    mAudioALSAVolumeController = NULL;

    mAudioALSAVolumeController = AudioVolumeFactory::CreateAudioVolumeController();
    if (!mAudioALSAVolumeController)
    {
        ALOGE("get mAudioALSAVolumeController FAIL");
        ASSERT(mAudioALSAVolumeController != NULL);
    }

    return NO_ERROR;
}


status_t AudioALSACaptureHandlerSpkFeed::open()
{
    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source);

    ASSERT(mCaptureDataClient == NULL);
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderSpkFeed::getInstance(), mStreamAttributeTarget);

    //pOutFile = fopen("/sdcard/mtklog/SpkMonitor.pcm", "wb");

    mHardwareResourceManager->startInputDevice(AUDIO_DEVICE_IN_SPK_FEED);

    SetCaptureGain();

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerSpkFeed::close()
{
    ALOGD("+%s()", __FUNCTION__);

    mHardwareResourceManager->stopInputDevice(AUDIO_DEVICE_IN_SPK_FEED);

    ASSERT(mCaptureDataClient != NULL);
    delete mCaptureDataClient;
    if (pOutFile != NULL)
    {
        fclose(pOutFile);
        pOutFile = NULL;
    }

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerSpkFeed::routing(const audio_devices_t input_device)
{
    //mHardwareResourceManager->changeInputDevice(input_device);
    //Do not routing. It should be fixed.
    return NO_ERROR;
}


ssize_t AudioALSACaptureHandlerSpkFeed::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s()", __FUNCTION__);

    mCaptureDataClient->read(buffer, bytes);
    if (pOutFile != NULL)
    {
        fwrite(buffer, sizeof(char), bytes, pOutFile);
    }
    return bytes;
}

int AudioALSACaptureHandlerSpkFeed::SetCaptureGain(void)
{

    if (mAudioALSAVolumeController != NULL)
    {
        mAudioALSAVolumeController->SetCaptureGain(mStreamAttributeTarget->audio_mode, mStreamAttributeTarget->input_source,
                                                   mStreamAttributeTarget->input_device, mStreamAttributeTarget->output_devices);
    }
    return 0;
}

} // end of namespace android
#endif //end of defined(MTK_SPEAKER_MONITOR_SUPPORT)

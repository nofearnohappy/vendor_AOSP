#include "AudioALSACaptureHandlerNormal.h"

#include "AudioALSAHardwareResourceManager.h"

#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderNormal.h"

#include "AudioVUnlockDL.h"
#define LOG_TAG "AudioALSACaptureHandlerNormal"

namespace android
{

//static FILE *pOutFile = NULL;

AudioALSACaptureHandlerNormal::AudioALSACaptureHandlerNormal(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);

    init();
}


AudioALSACaptureHandlerNormal::~AudioALSACaptureHandlerNormal()
{
    ALOGD("+%s()", __FUNCTION__);

    ALOGD("%-s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerNormal::init()
{
    ALOGD("%s()", __FUNCTION__);

    return NO_ERROR;
}


status_t AudioALSACaptureHandlerNormal::open()
{
    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x, sample_rate=%d, num_channels=%d",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source, mStreamAttributeTarget->sample_rate,
          mStreamAttributeTarget->num_channels);

    ASSERT(mCaptureDataClient == NULL);
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderNormal::getInstance(), mStreamAttributeTarget);

#if 0
    pOutFile = fopen("/sdcard/mtklog/RecRaw.pcm", "wb");
    if (pOutFile == NULL)
    {
        ALOGD("%s(), fopen fail", __FUNCTION__);
    }
#endif

    mHardwareResourceManager->startInputDevice(mStreamAttributeTarget->input_device);

    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        struct timespec systemtime;
        VUnlockhdl->SetUplinkStartTime(systemtime, 1);
    }
    //===========================================

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerNormal::close()
{
    ALOGD("+%s()", __FUNCTION__);

    mHardwareResourceManager->stopInputDevice(mHardwareResourceManager->getInputDevice());

    ASSERT(mCaptureDataClient != NULL);
    delete mCaptureDataClient;

#if 0
    if (pOutFile != NULL)
    {
        fclose(pOutFile);
    }
#endif
    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        struct timespec systemtime;
        VUnlockhdl->SetUplinkStartTime(systemtime, 1);
    }
    //===========================================

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerNormal::routing(const audio_devices_t input_device)
{
    mHardwareResourceManager->changeInputDevice(input_device);
    return NO_ERROR;
}


ssize_t AudioALSACaptureHandlerNormal::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s()", __FUNCTION__);

    mCaptureDataClient->read(buffer, bytes);
#if 0   //remove dump here which might cause process too long due to SD performance
    if (pOutFile != NULL)
    {
        fwrite(buffer, sizeof(char), bytes, pOutFile);
    }
#endif
    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        struct timespec systemtime;
        VUnlockhdl->SetUplinkStartTime(systemtime, 0);
    }
    //===========================================

    return bytes;
}

} // end of namespace android

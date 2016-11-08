#include "AudioALSACaptureHandlerAEC.h"

#include "AudioALSAHardwareResourceManager.h"

#include "AudioALSACaptureDataClient.h"
#include "AudioALSACaptureDataProviderNormal.h"
#include "AudioALSACaptureDataProviderBTSCO.h"
#include "AudioALSACaptureDataProviderBTCVSD.h"
#include "AudioALSACaptureDataProviderEchoRef.h"
#include "AudioALSACaptureDataProviderEchoRefBTSCO.h"
#include "AudioALSACaptureDataProviderEchoRefExt.h"

#include "WCNChipController.h"


#define LOG_TAG "AudioALSACaptureHandlerAEC"

namespace android
{

//static FILE *pOutFile = NULL;

AudioALSACaptureHandlerAEC::AudioALSACaptureHandlerAEC(stream_attribute_t *stream_attribute_target) :
    AudioALSACaptureHandlerBase(stream_attribute_target)
{
    ALOGD("%s()", __FUNCTION__);

    init();
}


AudioALSACaptureHandlerAEC::~AudioALSACaptureHandlerAEC()
{
    ALOGD("+%s()", __FUNCTION__);

    ALOGD("%-s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerAEC::init()
{
    ALOGD("%s()", __FUNCTION__);
    memset(&mStreamAttributeTargetEchoRef, 0, sizeof(mStreamAttributeTargetEchoRef));
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerAEC::open()
{
    if ( (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO) ||
 		 (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET) ||
 		 (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT) )
		mStreamAttributeTarget->input_device = AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET;

    ALOGD("+%s(), input_device = 0x%x, input_source = 0x%x, sample_rate=%d, num_channels=%d, output_devices=0x%x",
          __FUNCTION__, mStreamAttributeTarget->input_device, mStreamAttributeTarget->input_source, mStreamAttributeTarget->sample_rate,
          mStreamAttributeTarget->num_channels, mStreamAttributeTarget->output_devices);

    ASSERT(mCaptureDataClient == NULL);
#if 0   //enable for check EchoRef data is correct
    //mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderNormal::getInstance(), mStreamAttributeTarget);
    //mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderEchoRef::getInstance(), mStreamAttributeTarget);
    mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderEchoRefExt::getInstance(), mStreamAttributeTarget);
#else
    if (mStreamAttributeTarget->input_device == AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET)
    {
        // open BT  data provider
        if (WCNChipController::GetInstance()->IsBTMergeInterfaceSupported() == true)
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderBTSCO::getInstance(), mStreamAttributeTarget);
        }
        else
        {
            mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderBTCVSD::getInstance(), mStreamAttributeTarget);
        }
    }
    else
    {
        mCaptureDataClient = new AudioALSACaptureDataClient(AudioALSACaptureDataProviderNormal::getInstance(), mStreamAttributeTarget);
    }

    //open echoref data provider
    if (mCaptureDataClient != NULL)
    {
        memcpy(&mStreamAttributeTargetEchoRef, mStreamAttributeTarget, sizeof(stream_attribute_t));
        if (mStreamAttributeTarget->input_device == AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET)
        {
            //open BT  echoref data provider
            mCaptureDataClient->AddEchoRefDataProvider(AudioALSACaptureDataProviderEchoRefBTSCO::getInstance(), &mStreamAttributeTargetEchoRef);
        }
        else
        {
#if defined(NXP_SMARTPA_SUPPORT) && defined(EXTCODEC_ECHO_REFERENCE_SUPPORT)
            if (mStreamAttributeTarget->output_devices == AUDIO_DEVICE_OUT_SPEAKER)
            {
                mCaptureDataClient->AddEchoRefDataProvider(AudioALSACaptureDataProviderEchoRefExt::getInstance(), &mStreamAttributeTargetEchoRef);
            }
            else
#endif
            {
                mCaptureDataClient->AddEchoRefDataProvider(AudioALSACaptureDataProviderEchoRef::getInstance(), &mStreamAttributeTargetEchoRef);
            }
        }
    }
#endif

#if 0
    pOutFile = fopen("/sdcard/mtklog/RecRaw.pcm", "wb");
    if (pOutFile == NULL)
    {
        ALOGD("%s(), open file fail ", __FUNCTION__);
    }
#endif

    if (mStreamAttributeTarget->input_device != AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET)
    {
        //no need to config analog part while BT case
        mHardwareResourceManager->startInputDevice(mStreamAttributeTarget->input_device);
    }

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerAEC::close()
{
    ALOGD("+%s()", __FUNCTION__);

    if (mStreamAttributeTarget->input_device != AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET)
    {
        //no need to config analog part while BT case
        mHardwareResourceManager->stopInputDevice(mHardwareResourceManager->getInputDevice());
    }

    ASSERT(mCaptureDataClient != NULL);
    delete mCaptureDataClient;
    mCaptureDataClient = NULL;

#if 0
    if (pOutFile != NULL)
    {
        fclose(pOutFile);
    }
#endif

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSACaptureHandlerAEC::routing(const audio_devices_t input_device)
{
    if ( (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO) ||
 		 (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET) ||
 		 (mStreamAttributeTarget->output_devices & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT) )
		mStreamAttributeTarget->input_device = AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET;

    mHardwareResourceManager->changeInputDevice(input_device);
    return NO_ERROR;
}


ssize_t AudioALSACaptureHandlerAEC::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s()", __FUNCTION__);

    mCaptureDataClient->read(buffer, bytes);
#if 0
    if (pOutFile != NULL)
    {
        fwrite(buffer, sizeof(char), bytes, pOutFile);
    }
#endif

    return bytes;
}

status_t AudioALSACaptureHandlerAEC::UpdateBesRecParam()
{
    ALOGD("+%s()", __FUNCTION__);
    if (mCaptureDataClient != NULL)
    {
#if 0   //do it in alsastreamin.
        //#if defined(NXP_SMARTPA_SUPPORT)    //for echoref data provider switch
        ALOGD("%s(), need reopen the data provider", __FUNCTION__);
        close();
        open();
#else
        mCaptureDataClient->UpdateBesRecParam();
#endif
    }
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

} // end of namespace android

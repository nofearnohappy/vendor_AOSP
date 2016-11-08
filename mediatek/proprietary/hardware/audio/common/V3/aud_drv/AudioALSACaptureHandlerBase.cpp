#include "AudioALSACaptureHandlerBase.h"

#include "AudioALSADataProcessor.h"

#include "AudioALSAHardwareResourceManager.h"
//#include "AudioALSACaptureDataProvider.h"


#define LOG_TAG "AudioALSACaptureHandlerBase"

namespace android
{

AudioALSACaptureHandlerBase::AudioALSACaptureHandlerBase(stream_attribute_t *stream_attribute_target) :
    mIdentity(0xFFFFFFFF),
    mDataProcessor(AudioALSADataProcessor::getInstance()),
    mHardwareResourceManager(AudioALSAHardwareResourceManager::getInstance()),
    mCaptureDataClient(NULL),
    mStreamAttributeTarget(stream_attribute_target),
    mSupportConcurrencyInCall(false)
{
    ALOGD("%s()", __FUNCTION__);

    init();
}


AudioALSACaptureHandlerBase::~AudioALSACaptureHandlerBase()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureHandlerBase::init()
{
    ALOGD("%s()", __FUNCTION__);

    return NO_ERROR;
}

status_t AudioALSACaptureHandlerBase::UpdateBesRecParam()
{
    ALOGD("%s()", __FUNCTION__);

    return NO_ERROR;
}

bool AudioALSACaptureHandlerBase::isSupportConcurrencyInCall()
{
    ALOGD("%s()", __FUNCTION__);

    return mSupportConcurrencyInCall;
}

} // end of namespace android


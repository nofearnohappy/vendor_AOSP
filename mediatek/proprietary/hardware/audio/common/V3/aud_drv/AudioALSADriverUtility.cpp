#include "AudioALSADriverUtility.h"
#include <cutils/properties.h>
#include "AudioLock.h"
#include <stdint.h>
#include <sys/types.h>
#include <stdlib.h>
#include <AudioALSADeviceParser.h>

#define LOG_TAG "AudioALSADriverUtility"

namespace android
{

AudioALSADriverUtility *AudioALSADriverUtility::mAudioALSADriverUtility = NULL;
AudioALSADriverUtility *AudioALSADriverUtility::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSADriverUtility == NULL)
    {
        mAudioALSADriverUtility = new AudioALSADriverUtility();
    }
    ASSERT(mAudioALSADriverUtility != NULL);
    return mAudioALSADriverUtility;
}

int AudioALSADriverUtility::GetPropertyValue(const char* ProPerty_Key)
{
    int result;
    char value[PROPERTY_VALUE_MAX];
    property_get(ProPerty_Key, value, "0");
    result = atoi(value);
    ALOGD("GetPropertyValue key = %s value = %d",ProPerty_Key,result);
    return result;
}

AudioALSADriverUtility::AudioALSADriverUtility() :
    mMixer(NULL)
{
    ALOGD("%s()", __FUNCTION__);

    mMixer = mixer_open(AudioALSADeviceParser::getInstance()->GetCardIndex());
    ALOGD("mMixer = %p", mMixer);
    ASSERT(mMixer != NULL);
}


AudioALSADriverUtility::~AudioALSADriverUtility()
{
    ALOGD("%s()", __FUNCTION__);

    mixer_close(mMixer);
    mMixer = NULL;
}


} // end of namespace android

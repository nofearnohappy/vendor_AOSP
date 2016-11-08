#include "AudioALSADataProcessor.h"

#include <utils/threads.h>

#include "AudioLock.h"

#define LOG_TAG "AudioALSADataProcessor"

namespace android
{

AudioALSADataProcessor *AudioALSADataProcessor::mAudioALSADataProcessor = NULL;
AudioALSADataProcessor *AudioALSADataProcessor::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSADataProcessor == NULL)
    {
        mAudioALSADataProcessor = new AudioALSADataProcessor();
    }
    ASSERT(mAudioALSADataProcessor != NULL);
    return mAudioALSADataProcessor;
}

AudioALSADataProcessor::AudioALSADataProcessor()
{
    ALOGD("%s()", __FUNCTION__);
}

AudioALSADataProcessor::~AudioALSADataProcessor()
{
    ALOGD("%s()", __FUNCTION__);
}

} // end of namespace android


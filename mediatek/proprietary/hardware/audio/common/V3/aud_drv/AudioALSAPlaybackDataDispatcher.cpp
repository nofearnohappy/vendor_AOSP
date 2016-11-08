#include "AudioALSAPlaybackDataDispatcher.h"

#include <utils/threads.h>

#include "AudioType.h"
#include "AudioLock.h"

#define LOG_TAG "AudioALSAPlaybackDataDispatcher"

namespace android
{

AudioALSAPlaybackDataDispatcher *AudioALSAPlaybackDataDispatcher::mAudioALSAPlaybackDataDispatcher = NULL;
AudioALSAPlaybackDataDispatcher *AudioALSAPlaybackDataDispatcher::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSAPlaybackDataDispatcher == NULL)
    {
        mAudioALSAPlaybackDataDispatcher = new AudioALSAPlaybackDataDispatcher();
    }
    ASSERT(mAudioALSAPlaybackDataDispatcher != NULL);
    return mAudioALSAPlaybackDataDispatcher;
}

AudioALSAPlaybackDataDispatcher::AudioALSAPlaybackDataDispatcher()
{
    ALOGD("%s()", __FUNCTION__);
}

AudioALSAPlaybackDataDispatcher::~AudioALSAPlaybackDataDispatcher()
{
    ALOGD("%s()", __FUNCTION__);
}

} // end of namespace android


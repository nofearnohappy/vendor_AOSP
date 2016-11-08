#include "AudioALSACodecDeviceOutSpeakerPMIC.h"

#include "AudioLock.h"

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSASpeakerMonitor.h"
#endif


#define LOG_TAG "AudioALSACodecDeviceOutSpeakerPMIC"

namespace android
{

AudioALSACodecDeviceOutSpeakerPMIC *AudioALSACodecDeviceOutSpeakerPMIC::mAudioALSACodecDeviceOutSpeakerPMIC = NULL;
AudioALSACodecDeviceOutSpeakerPMIC *AudioALSACodecDeviceOutSpeakerPMIC::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACodecDeviceOutSpeakerPMIC == NULL)
    {
        mAudioALSACodecDeviceOutSpeakerPMIC = new AudioALSACodecDeviceOutSpeakerPMIC();
    }
    ASSERT(mAudioALSACodecDeviceOutSpeakerPMIC != NULL);
    return mAudioALSACodecDeviceOutSpeakerPMIC;
}


AudioALSACodecDeviceOutSpeakerPMIC::AudioALSACodecDeviceOutSpeakerPMIC()
{
    ALOGD("%s()", __FUNCTION__);
}


AudioALSACodecDeviceOutSpeakerPMIC::~AudioALSACodecDeviceOutSpeakerPMIC()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACodecDeviceOutSpeakerPMIC::open()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Speaker_Amp_Switch"), "On"))
        {
            ALOGE("Error: Speaker_Amp_Switch invalid value");
        }
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
        AudioALSASpeakerMonitor::getInstance()->Activate();
        //AudioALSASpeakerMonitor::getInstance()->EnableSpeakerMonitorThread(true);
#endif
    }

    mClientCount++;

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


status_t AudioALSACodecDeviceOutSpeakerPMIC::close()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    mClientCount--;

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Speaker_Amp_Switch"), "Off"))
        {
            ALOGE("Error: Speaker_Amp_Switch invalid value");
        }
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
        //AudioALSASpeakerMonitor::getInstance()->EnableSpeakerMonitorThread(false);
        AudioALSASpeakerMonitor::getInstance()->Deactivate();
#endif
    }

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


} // end of namespace android

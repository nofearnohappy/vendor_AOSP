#include "AudioALSACodecDeviceOutSpeakerEarphonePMIC.h"

#include "AudioLock.h"


#define LOG_TAG "AudioALSACodecDeviceOutSpeakerEarphonePMIC"

namespace android
{

AudioALSACodecDeviceOutSpeakerEarphonePMIC *AudioALSACodecDeviceOutSpeakerEarphonePMIC::mAudioALSACodecDeviceOutSpeakerEarphonePMIC = NULL;
AudioALSACodecDeviceOutSpeakerEarphonePMIC *AudioALSACodecDeviceOutSpeakerEarphonePMIC::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACodecDeviceOutSpeakerEarphonePMIC == NULL)
    {
        mAudioALSACodecDeviceOutSpeakerEarphonePMIC = new AudioALSACodecDeviceOutSpeakerEarphonePMIC();
    }
    ASSERT(mAudioALSACodecDeviceOutSpeakerEarphonePMIC != NULL);
    return mAudioALSACodecDeviceOutSpeakerEarphonePMIC;
}


AudioALSACodecDeviceOutSpeakerEarphonePMIC::AudioALSACodecDeviceOutSpeakerEarphonePMIC()
{
    ALOGD("%s()", __FUNCTION__);
}


AudioALSACodecDeviceOutSpeakerEarphonePMIC::~AudioALSACodecDeviceOutSpeakerEarphonePMIC()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACodecDeviceOutSpeakerEarphonePMIC::open()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Headset_Speaker_Amp_Switch"), "On"))
        {
            ALOGE("Error: Headset_Speaker_Amp_Switch invalid value");
        }
    }

    mClientCount++;

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


status_t AudioALSACodecDeviceOutSpeakerEarphonePMIC::close()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    mClientCount--;

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Headset_Speaker_Amp_Switch"), "Off"))
        {
            ALOGE("Error: Headset_Speaker_Amp_Switch invalid value");
        }
    }

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


} // end of namespace android

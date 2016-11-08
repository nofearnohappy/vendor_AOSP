#include "AudioALSACodecDeviceBase.h"

#include "AudioLock.h"

#include "AudioALSADriverUtility.h"

#include "AudioALSACodecDeviceOutEarphonePMIC.h"
#include "AudioALSACodecDeviceOutReceiverPMIC.h"
#include "AudioALSACodecDeviceOutSpeakerNXP.h"
#include "AudioALSACodecDeviceOutSpeakerPMIC.h"
#include "AudioALSACodecDeviceOutSpeakerEarphonePMIC.h"
#include "AudioALSADeviceConfigManager.h"

#define LOG_TAG "AudioALSACodecDeviceBase"

namespace android
{
struct mixer *AudioALSACodecDeviceBase::mMixer = NULL;

AudioALSACodecDeviceBase *AudioALSACodecDeviceBase::createCodecOutputDevices(const audio_devices_t output_devices)
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    return AudioALSACodecDeviceOutSpeakerPMIC::getInstance();
}

AudioALSACodecDeviceBase::AudioALSACodecDeviceBase() :
    mClientCount(0)
{
    ALOGD("%s()", __FUNCTION__);

    if (mMixer == NULL)
    {
        mMixer = AudioALSADriverUtility::getInstance()->getMixer();
        ASSERT(mMixer != NULL);
    }
}

AudioALSACodecDeviceBase::~AudioALSACodecDeviceBase()
{
    ALOGD("%s()", __FUNCTION__);

    if (mMixer != NULL)
    {
        mixer_close(mMixer);
        mMixer == NULL;
    }
}

} // end of namespace android

#include "AudioALSACodecDeviceOutSpeakerNXP.h"

#include "AudioLock.h"
#include "audio_custom_exp.h"

#include <mtk_tfa98xx_interface.h>

#if defined(MTK_SPEAKER_MONITOR_SUPPORT) // test only
#include "AudioALSASpeakerMonitor.h"
#endif
#define APLL_ON //Low jitter Mode Set

#define LOG_TAG "AudioALSACodecDeviceOutSpeakerNXP"

namespace android
{

AudioALSACodecDeviceOutSpeakerNXP *AudioALSACodecDeviceOutSpeakerNXP::mAudioALSACodecDeviceOutSpeakerNXP = NULL;
AudioALSACodecDeviceOutSpeakerNXP *AudioALSACodecDeviceOutSpeakerNXP::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACodecDeviceOutSpeakerNXP == NULL)
    {
        mAudioALSACodecDeviceOutSpeakerNXP = new AudioALSACodecDeviceOutSpeakerNXP();
    }
    ASSERT(mAudioALSACodecDeviceOutSpeakerNXP != NULL);
    return mAudioALSACodecDeviceOutSpeakerNXP;
}


AudioALSACodecDeviceOutSpeakerNXP::AudioALSACodecDeviceOutSpeakerNXP()
{
    ALOGD("%s()", __FUNCTION__);
    // use default samplerate to load setting.
    open();
    close();
}


AudioALSACodecDeviceOutSpeakerNXP::~AudioALSACodecDeviceOutSpeakerNXP()
{
    ALOGD("%s()", __FUNCTION__);

    ALOGD("MTK_Tfa98xx_Deinit");
    MTK_Tfa98xx_Deinit();
}


status_t AudioALSACodecDeviceOutSpeakerNXP::open()
{
#ifndef EXTCODEC_ECHO_REFERENCE_SUPPORT
    MTK_Tfa98xx_SetBypassDspIncall(1);
#endif

    return open(44100);
}

status_t AudioALSACodecDeviceOutSpeakerNXP::open(const uint32_t SampleRate)
{
    ALOGD("+%s(), mClientCount = %d, SampleRate = %d", __FUNCTION__, mClientCount, SampleRate);

    if (mClientCount == 0)
    {
#ifdef APLL_ON
        ALOGD("+%s(), Audio_i2s0_hd_Switch on", __FUNCTION__);
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_hd_Switch"), "On"))
        {
            ALOGE("Error: Audio_i2s0_hd_Switch invalid value");
        }
#endif
        if (SampleRate == 48000)
        {
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "On48000"))
            {
                ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
            }
        }
        else if (SampleRate == 44100)
        {
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "On44100"))
            {
                ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
            }
        }
        else if (SampleRate == 32000)
        {
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "On32000"))
            {
                ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
            }
        }
        else if (SampleRate == 16000)
        {
#ifdef EXTCODEC_ECHO_REFERENCE_SUPPORT
            ALOGD("%s(), Audio_ExtCodec_EchoRef_Switch on", __FUNCTION__);
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ExtCodec_EchoRef_Switch"), "On"))
            {
                ALOGE("Error: Audio_ExtCodec_EchoRef_Switch invalid value");
            }
#endif
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "On16000"))
            {
                ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
            }
        }
        else if (SampleRate == 8000)
        {
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "On8000"))
            {
                ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
            }
        }
        MTK_Tfa98xx_SetSampleRate(SampleRate);
        MTK_Tfa98xx_SpeakerOn();
#ifdef EXTCODEC_ECHO_REFERENCE_SUPPORT
        //Echo Reference configure will be set on all sample rate
        MTK_Tfa98xx_EchoReferenceConfigure(1);
#endif

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
        //AudioALSASpeakerMonitor::getInstance()->Activate();
        //AudioALSASpeakerMonitor::getInstance()->EnableSpeakerMonitorThread(true);
#endif
    }

    mClientCount++;

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


status_t AudioALSACodecDeviceOutSpeakerNXP::close()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    mClientCount--;

    if (mClientCount == 0)
    {
        MTK_Tfa98xx_SpeakerOff();
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_SideGen_Switch"), "Off"))
        {
            ALOGE("Error: Audio_i2s0_SideGen_Switch invalid value");
        }
#ifdef EXTCODEC_ECHO_REFERENCE_SUPPORT
        ALOGD("%s(), Audio_ExtCodec_EchoRef_Switch off", __FUNCTION__);
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ExtCodec_EchoRef_Switch"), "Off"))
        {
            ALOGE("Error: Audio_ExtCodec_EchoRef_Switch invalid value");
        }
#endif
#ifdef APLL_ON
        ALOGD("+%s(), Audio_i2s0_hd_Switch off", __FUNCTION__);
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_i2s0_hd_Switch"), "Off"))
        {
            ALOGE("Error: Audio_i2s0_hd_Switch invalid value");
        }
#endif

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
        //AudioALSASpeakerMonitor::getInstance()->EnableSpeakerMonitorThread(false);
        //AudioALSASpeakerMonitor::getInstance()->Deactivate();
#endif
    }

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


} // end of namespace android

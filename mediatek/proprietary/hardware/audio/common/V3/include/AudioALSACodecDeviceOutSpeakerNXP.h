#ifndef ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_SPEAKER_NXP_H
#define ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_SPEAKER_NXP_H

#include "AudioType.h"

#include "AudioALSACodecDeviceBase.h"


namespace android
{

class AudioALSACodecDeviceOutSpeakerNXP : public AudioALSACodecDeviceBase
{
    public:
        virtual ~AudioALSACodecDeviceOutSpeakerNXP();

        static AudioALSACodecDeviceOutSpeakerNXP *getInstance();


        /**
         * open/close codec driver
         */
        status_t open();
        status_t open(const uint32_t SampleRate);
        status_t close();



    protected:
        AudioALSACodecDeviceOutSpeakerNXP();



    private:
        /**
         * singleton pattern
         */
        static AudioALSACodecDeviceOutSpeakerNXP *mAudioALSACodecDeviceOutSpeakerNXP;

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_SPEAKER_NXP_H

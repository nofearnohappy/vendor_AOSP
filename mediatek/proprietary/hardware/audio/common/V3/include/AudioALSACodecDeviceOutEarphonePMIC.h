#ifndef ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_EARPHONE_PMIC_H
#define ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_EARPHONE_PMIC_H

#include "AudioType.h"
#include "AudioALSACodecDeviceBase.h"
#include "AudioCustParam.h"


namespace android
{

class AudioALSACodecDeviceOutEarphonePMIC : public AudioALSACodecDeviceBase
{
    public:
        virtual ~AudioALSACodecDeviceOutEarphonePMIC();

        static AudioALSACodecDeviceOutEarphonePMIC *getInstance();


        /**
         * open/close codec driver
         */
        status_t open();
        status_t close();



    protected:
        AudioALSACodecDeviceOutEarphonePMIC();
        status_t DeviceDoDcCalibrate();

    private:
        /**
         * singleton pattern
         */
        static AudioALSACodecDeviceOutEarphonePMIC *mAudioALSACodecDeviceOutEarphonePMIC;
        AUDIO_BUFFER_DC_CALIBRATION_STRUCT mAudioBufferDcCalibrate;

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CODEC_DEVICE_OUT_EARPHONE_PMIC_H

#ifndef ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_MODEM_DAI_H
#define ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_MODEM_DAI_H

#include "AudioALSACaptureDataProviderBase.h"

namespace android
{

class AudioALSACaptureDataProviderModemDai : public AudioALSACaptureDataProviderBase
{
    public:
        virtual ~AudioALSACaptureDataProviderModemDai();

        static AudioALSACaptureDataProviderModemDai *getInstance();

        /**
         * open/close pcm interface when 1st attach & the last detach
         */
        status_t open();
        status_t close();



    protected:
        AudioALSACaptureDataProviderModemDai();



    private:
        /**
         * singleton pattern
         */
        static AudioALSACaptureDataProviderModemDai *mAudioALSACaptureDataProviderModemDai;


        /**
         * pcm read thread
         */
        static void *readThread(void *arg);
        pthread_t hReadThread;

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_MODEM_DAI_H
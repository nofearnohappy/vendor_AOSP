#ifndef ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_NORMAL_H
#define ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_NORMAL_H

#include "AudioALSACaptureDataProviderBase.h"

namespace android
{

class AudioALSACaptureDataProviderSpkFeed : public AudioALSACaptureDataProviderBase
{
    public:
        virtual ~AudioALSACaptureDataProviderSpkFeed();

        static AudioALSACaptureDataProviderSpkFeed *getInstance();

        /**
         * open/close pcm interface when 1st attach & the last detach
         */
        status_t open();
        status_t close();



    protected:
        AudioALSACaptureDataProviderSpkFeed();



    private:
        /**
         * singleton pattern
         */
        static AudioALSACaptureDataProviderSpkFeed *mAudioALSACaptureDataProviderSpkFeed;


        /**
         * pcm read thread
         */
        static void *readThread(void *arg);
        pthread_t hReadThread;

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_DATA_PROVIDER_NORMAL_H
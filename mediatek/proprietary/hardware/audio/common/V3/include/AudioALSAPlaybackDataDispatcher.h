#ifndef ANDROID_AUDIO_ALSA_PLAYBACK_DATA_DISPATCHER_H
#define ANDROID_AUDIO_ALSA_PLAYBACK_DATA_DISPATCHER_H

#include <tinyalsa/asoundlib.h>

namespace android
{

class AudioALSAPlaybackDataDispatcher
{
    public:
        virtual ~AudioALSAPlaybackDataDispatcher();

        static AudioALSAPlaybackDataDispatcher *getInstance();

    protected:
        AudioALSAPlaybackDataDispatcher();

    private:
        /**
         * singleton pattern
         */
        static AudioALSAPlaybackDataDispatcher *mAudioALSAPlaybackDataDispatcher;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_PLAYBACK_DATA_DISPATCHER_H

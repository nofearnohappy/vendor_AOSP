#ifndef ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_SPK_FEED_H
#define ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_SPK_FEED_H

#include "AudioALSACaptureHandlerBase.h"
//#include "AudioALSAVolumeController.h"
#include "AudioVolumeInterface.h"

namespace android
{

class AudioALSACaptureHandlerSpkFeed : public AudioALSACaptureHandlerBase
{
    public:
        AudioALSACaptureHandlerSpkFeed(stream_attribute_t *stream_attribute_target);
        virtual ~AudioALSACaptureHandlerSpkFeed();

        /**
         * open/close audio hardware
         */
        virtual status_t open();
        virtual status_t close();
        virtual status_t routing(const audio_devices_t input_device);


        /**
         * read data from audio hardware
         */
        virtual ssize_t  read(void *buffer, ssize_t bytes);

    protected:
        /**
         * init audio hardware
         */
        virtual status_t init();
    private:
        AudioVolumeInterface *mAudioALSAVolumeController;
        int SetCaptureGain(void);
        bool test;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_SPK_FEED_H

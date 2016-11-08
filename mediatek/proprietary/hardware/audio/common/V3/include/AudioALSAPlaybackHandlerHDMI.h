#ifndef ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_HDMI_H
#define ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_HDMI_H

#include "AudioALSAPlaybackHandlerBase.h"

namespace android
{

class AudioALSAPlaybackHandlerHDMI : public AudioALSAPlaybackHandlerBase
{
    public:
        AudioALSAPlaybackHandlerHDMI(const stream_attribute_t *stream_attribute_source);
        virtual ~AudioALSAPlaybackHandlerHDMI();

        virtual status_t SetMHLChipParameter(int channels, int bits, int samplerate);

        virtual status_t SetMHLChipEnable(int enable);
        
        /**
         * open/close audio hardware
         */
        virtual status_t open();
        virtual status_t close();
		virtual status_t pause();
		virtual status_t resume();
        virtual status_t flush();
        virtual int drain(audio_drain_type_t type);		
        virtual status_t routing(const audio_devices_t output_devices);
		virtual status_t setVolume(uint32_t vol);


        /**
         * write data to audio hardware
         */
        virtual ssize_t  write(const void *buffer, size_t bytes);

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_NORMAL_H

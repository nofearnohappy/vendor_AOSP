#ifndef ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_FAST_H
#define ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_FAST_H

#include "AudioALSAPlaybackHandlerBase.h"

namespace android
{

class AudioALSAPlaybackHandlerFast : public AudioALSAPlaybackHandlerBase
{
    public:
        AudioALSAPlaybackHandlerFast(const stream_attribute_t *stream_attribute_source);
        virtual ~AudioALSAPlaybackHandlerFast();


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

        virtual status_t setFilterMng(AudioMTKFilterManager *pFilterMng);


        /**
         * low latency
         */
        virtual status_t setLowLatencyMode(bool mode, size_t buffer_size, size_t reduceInterruptSize, bool bforce = false);

    private:
        struct timespec mNewtime, mOldtime;
        struct mixer *mMixer;
        bool SetLowJitterMode(bool bEnable , uint32_t SampleRate);
        uint32_t ChooseTargetSampleRate(uint32_t SampleRate,audio_devices_t outputdevice);
        bool DeviceSupportHifi(audio_devices_t outputdevice);
        uint32_t GetLowJitterModeSampleRate(void);
        double latencyTime[3];
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_FAST_H

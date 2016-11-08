#ifndef ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_BASE_H
#define ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_BASE_H

#include "AudioType.h"

namespace android
{

class AudioALSADataProcessor;

class AudioALSAHardwareResourceManager;
class AudioALSACaptureDataClient;

class AudioALSACaptureHandlerBase
{
    public:
        AudioALSACaptureHandlerBase(stream_attribute_t *stream_attribute_target);
        virtual ~AudioALSACaptureHandlerBase();


        /**
         * set handler index
         */
        inline void         setIdentity(const uint32_t identity) { mIdentity = identity; }
        inline uint32_t     getIdentity() const { return mIdentity; }


        /**
         * open/close audio hardware
         */
        virtual status_t open() = 0;
        virtual status_t close() = 0;
        virtual status_t routing(const audio_devices_t input_device) = 0;


        /**
         * read data from audio hardware
         */
        virtual ssize_t  read(void *buffer, ssize_t bytes) = 0;

        /**
         * Update BesRecord Parameters
         */
        virtual status_t UpdateBesRecParam();

        /**
         * Query if the capture handler can run in Call Mode
         */
        virtual bool isSupportConcurrencyInCall();

    protected:
        /**
         * init audio hardware
         */
        virtual status_t init();

        AudioALSAHardwareResourceManager *mHardwareResourceManager;
        AudioALSACaptureDataClient       *mCaptureDataClient;

        stream_attribute_t *mStreamAttributeTarget; // to stream in
        bool mSupportConcurrencyInCall;

    private:
        AudioALSADataProcessor *mDataProcessor;

        uint32_t mIdentity; // key for mCaptureHandlerVector
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_CAPTURE_HANDLER_BASE_H

#ifndef ANDROID_AUDIO_ALSA_STREAM_IN_H
#define ANDROID_AUDIO_ALSA_STREAM_IN_H

#include <utils/Mutex.h>

//#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware_legacy/AudioMTKHardwareInterface.h>


#include "AudioType.h"
#include "AudioLock.h"
#include "AudioSpeechEnhanceInfo.h"


namespace android
{

class AudioALSAStreamManager;
class AudioALSACaptureHandlerBase;

class AudioALSAStreamIn : public android_audio_legacy::AudioStreamIn
{
    public:
        AudioALSAStreamIn();
        virtual ~AudioALSAStreamIn();

        virtual status_t    set(uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate,
                                status_t *status, android_audio_legacy::AudioSystem::audio_in_acoustics acoustics, uint32_t flags=0);

        /** return audio sampling rate in hz - eg. 44100 */
        virtual uint32_t    sampleRate() const;

        /** return the input buffer size allowed by audio driver */
        virtual size_t      bufferSize() const;

        /** return input channel mask */
        virtual uint32_t    channels() const;

        /**
         * return audio format in 8bit or 16bit PCM format -
         * eg. AudioSystem:PCM_16_BIT
         */
        virtual int         format() const;

        /** set the input gain for the audio driver. This method is for
         *  for future use */
        virtual status_t    setGain(float gain);

        /** read audio buffer in from audio driver */
        virtual ssize_t     read(void *buffer, ssize_t bytes);

        /** dump the state of the audio input device */
        virtual status_t    dump(int fd, const Vector<String16> &args);

        /**
         * Put the audio hardware input into standby mode. Returns
         * status based on include/utils/Errors.h
         */
        virtual status_t    standby();

        // set/get audio input parameters. The function accepts a list of parameters
        // key value pairs in the form: key1=value1;key2=value2;...
        // Some keys are reserved for standard parameters (See AudioParameter class).
        // If the implementation does not accept a parameter change while the output is
        // active but the parameter is acceptable otherwise, it must return INVALID_OPERATION.
        // The audio flinger will put the input in standby and then change the parameter value.
        virtual status_t    setParameters(const String8 &keyValuePairs);
        virtual String8     getParameters(const String8 &keys);


        // Return the amount of input frames lost in the audio driver since the last call of this function.
        // Audio driver is expected to reset the value to 0 and restart counting upon returning the current value by this function call.
        // Such loss typically occurs when the user space process is blocked longer than the capacity of audio driver buffers.
        // Unit: the number of input audio frames
        virtual unsigned int getInputFramesLost() const;

        virtual status_t    addAudioEffect(effect_handle_t effect);
        virtual status_t    removeAudioEffect(effect_handle_t effect);


        /**
         * set stream in index
         */
        inline void         setIdentity(const uint32_t identity) { mIdentity = identity; }
        inline uint32_t     getIdentity() const { return mIdentity; }


        /**
         * open/close stream in related audio hardware
         */
        virtual status_t    open();
        virtual status_t    close();


        /**
         * routing
         */
        status_t routing(audio_devices_t input_device);


        /**
         * suspend/resume
         */
        status_t setSuspend(const bool suspend_on);


        /**
         * get stream attribute
         */
        virtual const stream_attribute_t *getStreamAttribute() const { return &mStreamAttributeTarget; }


        /**
         * update output routing
         */
        status_t updateOutputDeviceInfoForInputStream(audio_devices_t output_devices);

        /**
         * Set Input mute
         */
        void SetInputMute(bool bEnable);

        /**
         * Update Dynamic function mask
         */
        void UpdateDynamicFunctionMask(void);

        /**
         * Query if the stream in can run in Call Mode
         */
        bool isSupportConcurrencyInCall(void);
    protected:
        AudioALSAStreamManager         *mStreamManager;
        AudioALSACaptureHandlerBase    *mCaptureHandler;


        /**
         * check open input stream parameters
         */
        virtual bool checkOpenStreamFormat(int *format);
        virtual bool checkOpenStreamChannels(uint32_t *channels);
        virtual bool checkOpenStreamSampleRate(const audio_devices_t devices, uint32_t *sampleRate);



    private:
        AudioLock           mLock;

        uint32_t            mIdentity; // key for mStreamInVector

        uint32_t     mSuspendCount;
        bool                mStandby;
        stream_attribute_t  mStreamAttributeTarget;

        effect_handle_t mPreProcessEffectBackup[MAX_PREPROCESSORS];
        int mPreProcessEffectBackupCount;

        /**
         * Speech EnhanceInfo Instance
         */
        AudioSpeechEnhanceInfo *mAudioSpeechEnhanceInfoInstance;

        void CheckBesRecordInfo();

        /**
         * for debug PCM dump
         */
        void  OpenPCMDump(void);
        void  ClosePCMDump(void);
        void  WritePcmDumpData(void *buffer, ssize_t bytes);
        String8 mDumpFileName;
        FILE *mPCMDumpFile;
        static int mDumpFileNum;

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_STREAM_IN_H

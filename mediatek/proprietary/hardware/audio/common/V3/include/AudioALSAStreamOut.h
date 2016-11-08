#ifndef ANDROID_AUDIO_ALSA_STREAM_OUT_H
#define ANDROID_AUDIO_ALSA_STREAM_OUT_H

#include <utils/Mutex.h>

//#include <hardware_legacy/AudioHardwareInterface.h>
#include <hardware_legacy/AudioMTKHardwareInterface.h>
#include <hardware/audio.h>


#include "AudioType.h"
#include "AudioLock.h"

namespace android
{

class AudioALSAStreamManager;
class AudioALSAPlaybackHandlerBase;


enum stream_out_type
{
    STREAM_OUT_PRIMARY = 0,
    STREAM_OUT_HDMI_STEREO = 1,
    STREAM_OUT_HDMI_MULTI_CHANNEL = 2,
    NUM_STREAM_OUT_TYPE
};

class AudioALSAStreamOut : public android_audio_legacy::AudioMTKStreamOutInterface
{
    public:
        AudioALSAStreamOut();
        virtual ~AudioALSAStreamOut();

        virtual status_t set(uint32_t devices,
                             int *format,
                             uint32_t *channels,
                             uint32_t *sampleRate,
                             status_t *status,
                             uint32_t flags);

        /** return audio sampling rate in hz - eg. 44100 */
        virtual uint32_t    sampleRate() const;

        /** returns size of output buffer - eg. 4800 */
        virtual size_t      bufferSize() const;

        /** returns the output channel mask */
        virtual uint32_t    channels() const;

        /**
         * return audio format in 8bit or 16bit PCM format -
         * eg. AudioSystem:PCM_16_BIT
         */
        virtual int         format() const;

        /**
         * return the audio hardware driver latency in milli seconds.
         */
        virtual uint32_t    latency() const;

        /**
         * Use this method in situations where audio mixing is done in the
         * hardware. This method serves as a direct interface with hardware,
         * allowing you to directly set the volume as apposed to via the framework.
         * This method might produce multiple PCM outputs or hardware accelerated
         * codecs, such as MP3 or AAC.
         */
        virtual status_t    setVolume(float left, float right);

        /** write audio buffer to driver. Returns number of bytes written */
        virtual ssize_t     write(const void *buffer, size_t bytes);
        virtual int drain(audio_drain_type_t type);
        /**
         * Put the audio hardware output into standby mode. Returns
         * status based on include/utils/Errors.h
         */
        virtual status_t    standby();

        /** dump the state of the audio output device */
        virtual status_t dump(int fd, const Vector<String16> &args);

        // set/get audio output parameters. The function accepts a list of parameters
        // key value pairs in the form: key1=value1;key2=value2;...
        // Some keys are reserved for standard parameters (See AudioParameter class).
        // If the implementation does not accept a parameter change while the output is
        // active but the parameter is acceptable otherwise, it must return INVALID_OPERATION.
        // The audio flinger will put the output in standby and then change the parameter value.
        virtual status_t    setParameters(const String8 &keyValuePairs);
        virtual String8     getParameters(const String8 &keys);

        // return the number of audio frames written by the audio dsp to DAC since
        // the output has exited standby
        virtual status_t    getRenderPosition(uint32_t *dspFrames);


        /**
         * set stream out index
         */
        inline void         setIdentity(const uint32_t identity) { mStreamAttributeSource.mStreamOutIndex = mIdentity = identity; }
        inline uint32_t     getIdentity() const { return mIdentity; }


        /**
         * open/close stream out related audio hardware
         */
        virtual status_t    open();
        virtual status_t    close();
        virtual status_t    pause();
        virtual status_t    resume();
        virtual status_t    flush();
		
        bool                isOutPutStreamActive() { return (mStandby == false) ? true : false; }


        /**
         * routing
         */
        status_t routing(audio_devices_t output_devices);


        /**
         * suspend/resume
         */
        status_t setSuspend(const bool suspend_on);
        static status_t setSuspendStreamOutHDMIStereo(const bool suspend_on);


        /**
         * get stream attribute
         */
        virtual const stream_attribute_t *getStreamAttribute() const { return &mStreamAttributeSource; }


        /**
         * low latency
         */
        status_t setLowLatencyMode(bool mode);

	/**
	 *
	 */
        status_t UpdateSampleRate(int samplerate);

	/**
	 *
	 */
        bool  SampleRateSupport(uint32_t samplerate);

        virtual status_t    getPresentationPosition(uint64_t *frames, struct timespec *timestamp);//Add for KK

         virtual status_t    setCallBack(stream_callback_t callback, void *cookie);
        //Force below two functions to pure virtual
        virtual status_t    getNextWriteTimestamp(int64_t *timestamp);

    protected:
        AudioALSAStreamManager         *mStreamManager;
        AudioALSAPlaybackHandlerBase   *mPlaybackHandler;



        /**
         * for debug PCM dump
         */
        void  OpenPCMDump(const char *class_name);
        void  ClosePCMDump(void);
        void  WritePcmDumpData(const void *buffer, ssize_t bytes);
        FILE *mPCMDumpFile;
        static uint32_t mDumpFileNum;



    private:
        AudioLock           mLock;
        volatile int32_t    mLockCount;

        uint32_t            mIdentity; // key for mStreamOutVector

        uint32_t     mSuspendCount;
        static uint32_t     mSuspendStreamOutHDMIStereoCount;

        bool                mStandby;
        stream_attribute_t  mStreamAttributeSource;

        stream_out_type     mStreamOutType;
        static AudioALSAStreamOut *mStreamOutHDMIStereo;
        uint64_t mPresentedBytes;
        timespec mPresentedTime;
        uint64_t mPresentFrames;

        bool                mLowLatencyMode;
        bool                mOffload;
        bool                mPaused;
        stream_callback_t   mStreamCbk;
        void                *mCbkCookie;
        uint32_t            mOffloadVol;

        void setBufferSize();
};

} // end namespace android

#endif // end of ANDROID_SPEECH_DRIVER_INTERFACE_H

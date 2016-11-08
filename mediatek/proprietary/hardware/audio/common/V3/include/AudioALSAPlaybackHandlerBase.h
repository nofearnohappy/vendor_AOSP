#ifndef ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_BASE_H
#define ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_BASE_H

#include <tinyalsa/asoundlib.h> // TODO(Harvey): move it
#include "sound/compress_params.h"
#include <tinycompress/tinycompress.h>
#include <sound/asound.h>
#include "sound/compress_offload.h"

#include "AudioType.h"
#include "AudioALSADeviceParser.h"
#include <hardware/audio.h>


namespace android
{

class AudioALSADataProcessor;

class AudioALSAHardwareResourceManager;

class AudioMTKFilterManager;

class MtkAudioSrc;
class MtkAudioBitConverter;

class AudioALSAPlaybackHandlerBase
{
    public:
        virtual ~AudioALSAPlaybackHandlerBase();


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
		virtual status_t pause() = 0;
        virtual status_t resume() = 0;
		virtual status_t flush() = 0;
		virtual int drain(audio_drain_type_t type) = 0;
        virtual status_t routing(const audio_devices_t output_devices) = 0;
        virtual status_t setVolume(uint32_t vol) = 0;


        /**
         * write data to audio hardware
         */
        virtual ssize_t  write(const void *buffer, size_t bytes) = 0;


        /**
         * Post processing
         */
        virtual status_t setFilterMng(AudioMTKFilterManager *pFilterMng);


        /**
         * low latency
         */
        virtual status_t setLowLatencyMode(bool mode, size_t buffer_size, size_t reduceInterruptSize, bool bforce = false);


        /**
         * get hardware buffer info (framecount)
         */
        virtual status_t getHardwareBufferInfo(time_info_struct_t *HWBuffer_Time_Info);

        virtual status_t get_timeStamp(unsigned long *frames, unsigned int *samplerate);

		playback_handler_t getPlaybackHandlerType();

        /**
         * get stream attribute for kernel buffer
         */
        const stream_attribute_t *getStreamAttributeTarget() { return &mStreamAttributeTarget; }

		status_t         setComprCallback(stream_callback_t StreamCbk, void *CbkCookie);

        /**
         * set first write
         */
        virtual void setFirstDataWriteFlag(bool bFirstDataWrite) { mFirstDataWrite = bFirstDataWrite; }
        
    protected:
        AudioALSAPlaybackHandlerBase(const stream_attribute_t *stream_attribute_source);


        /**
         * pcm driver open/close
         */
        status_t         openPcmDriver(const unsigned int device); // TODO(Harvey): Query device by string
        status_t         openComprDriver(const unsigned int device);
        status_t         closePcmDriver();
        status_t         closeComprDriver();

        /**
         * pcm driver list
         */
        status_t         ListPcmDriver(const unsigned int card, const unsigned int device);

        /**
         * stereo to mono for speaker
         */
        status_t doStereoToMonoConversionIfNeed(void *buffer, size_t bytes);


        /**
         * Post processing
         */
        status_t         initPostProcessing();
        status_t         deinitPostProcessing();
        status_t         doPostProcessing(void *pInBuffer, uint32_t inBytes, void **ppOutBuffer, uint32_t *pOutBytes);


        /**
         * Bli SRC
         */
        status_t         initBliSrc();
        status_t         deinitBliSrc();
        status_t         doBliSrc(void *pInBuffer, uint32_t inBytes, void **ppOutBuffer, uint32_t *pOutBytes);


        /**
         * Bit Converter
         */
        pcm_format       transferAudioFormatToPcmFormat(const audio_format_t audio_format) const;
        status_t         initBitConverter();
        status_t         deinitBitConverter();
        status_t         doBitConversion(void *pInBuffer, uint32_t inBytes, void **ppOutBuffer, uint32_t *pOutBytes);

        status_t         initDataPending();
        status_t         DeinitDataPending();
        status_t         dodataPending(void *pInBuffer, uint32_t inBytes, void **ppOutBuffer, uint32_t *pOutBytes);



        playback_handler_t mPlaybackHandlerType;

        AudioALSAHardwareResourceManager *mHardwareResourceManager;

        const stream_attribute_t *mStreamAttributeSource; // from stream out
        stream_attribute_t        mStreamAttributeTarget; // to audio hw

        struct pcm_config mConfig; // TODO(Harvey): move it to AudioALSAHardwareResourceManager later
        struct pcm *mPcm; // TODO(Harvey): move it to AudioALSAHardwareResourceManager & AudioALSAPlaybackDataDispatcher later

        struct compr_config mComprConfig;
        struct compress *mComprStream;
        stream_callback_t mStreamCbk; 
        void *mCbkCookie;

        /**
         * Post processing
         */
        AudioMTKFilterManager *mAudioFilterManagerHandler;
        char                  *mPostProcessingOutputBuffer;
        uint32_t               mPostProcessingOutputBufferSize;
        bool mFirstDataWrite;


        /**
         * Bli SRC
         */
        MtkAudioSrc *mBliSrc;
        char        *mBliSrcOutputBuffer;


        /**
         * Bit Converter
         */
        MtkAudioBitConverter *mBitConverter;
        char                 *mBitConverterOutputBuffer;

        /**
         * data Pending
         */
        char                  *mdataPendingOutputBuffer;
        char                  *mdataPendingTempBuffer;
        uint32_t             mdataPendingOutputBufferSize;
        uint32_t             mdataPendingRemindBufferSize;
        static const uint32_t dataAlignedSize = 64;

        /**
         * for debug PCM dump
         */
        void  OpenPCMDump(const char *class_name);
        void  ClosePCMDump(void);
        void  WritePcmDumpData(const void *buffer, ssize_t bytes);
        FILE *mPCMDumpFile;
        static uint32_t mDumpFileNum;


    private:
        AudioALSADataProcessor *mDataProcessor;
        struct mixer *mMixer;

        uint32_t mIdentity; // key for mPlaybackHandlerVector

};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_BASE_H

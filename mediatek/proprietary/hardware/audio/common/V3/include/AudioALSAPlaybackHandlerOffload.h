#include "AudioALSAPlaybackHandlerBase.h"
#include "AudioMTKDec.h"
#include "sound/compress_params.h"
#include <tinycompress/tinycompress.h>
#include <sound/asound.h>
#include "sound/compress_offload.h"
#include <pthread.h>
#include <cutils/list.h>


#ifndef ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_OFFLOAD_H
#define ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_OFFLOAD_H


namespace android
{

//below is control message
#define OFFLOAD_IOC_MAGIC    'a' 

#define OFFLOADSERVICE_WRITEBLOCK   _IO(OFFLOAD_IOC_MAGIC, 0x01)
#define OFFLOADSERVICE_SETGAIN      _IO(OFFLOAD_IOC_MAGIC, 0x02)
#define OFFLOADSERVICE_SETMODE      _IO(OFFLOAD_IOC_MAGIC, 0x03)


enum {
    OFFLOAD_STATE_IDLE,
    OFFLOAD_STATE_PLAYING,
    OFFLOAD_STATE_PAUSED,
};

enum {
    OFFLOAD_WRITE_EMPTY,
    OFFLOAD_WRITE_REMAIN,
};


enum {
	OFFLOAD_CMD_WRITE,
    OFFLOAD_CMD_DRAIN,
    OFFLOAD_CMD_PAUSE,
    OFFLOAD_CMD_RESUME,
    OFFLOAD_CMD_CLOSE,
    OFFLOAD_CMD_FLUSH,
};


enum {
	OFFLOAD_MODE_GDMA = 0,
    OFFLOAD_MODE_SW,
    OFFLOAD_MODE_DSP,
};


struct offload_cmd {
    struct listnode node;
    int cmd;
};


struct offload_stream_property {
    int                         offload_state;
    bool                        remain_write;
    pthread_mutex_t             offload_mutex;
    pthread_cond_t              offload_cond;
    struct listnode             offload_cmd_list;
	struct compr_gapless_mdata  offload_mdata;
    pthread_t                   offload_pthread;
	void                        *tmpBuffer;
    int                         offload_gain;
};


class AudioALSAPlaybackHandlerOffload : public AudioALSAPlaybackHandlerBase
{
    public:
        AudioALSAPlaybackHandlerOffload(const stream_attribute_t *stream_attribute_source);
        virtual ~AudioALSAPlaybackHandlerOffload();

        /**
         * open/close audio hardware
         */
        virtual status_t open();
        virtual status_t close();
		virtual status_t pause();
        virtual status_t resume();
        virtual status_t flush();
        virtual status_t routing(const audio_devices_t output_devices);
		virtual status_t setVolume(uint32_t vol);

        /**
         * write data to audio hardware
         */
        virtual ssize_t  write(const void *buffer, size_t bytes);

		virtual int drain(audio_drain_type_t type);
        
        virtual status_t setFilterMng(AudioMTKFilterManager *pFilterMng);

        int process_write();

        void offload_callback(stream_callback_event_t event);
		
        void offload_initialize();
		
    protected:

	private:
		uint32_t ChooseTargetSampleRate(uint32_t SampleRate);
		AudioMTKDecHandlerBase *AudioDecHandlerCreate();
		bool SetLowJitterMode(bool bEnable,uint32_t SampleRate);
		uint32_t GetLowJitterModeSampleRate();

		struct mixer *mMixer;
        AudioMTKDecHandlerBase *mDecHandler;
        audio_format_t  mFormat;
        int8_t    *mDecBsbuf;
        int8_t    *mDecPcmbuf;
        uint32_t   mDecBsbufSize;
        uint32_t   mDecPcmbufSize;
        uint32_t   mDecPcmbufRemain;
        uint32_t   mDecBsbufRemain;
        bool       mDecHeaderParsed;
        bool       mReady;
        void       *mBsbuffer;
        uint32_t   mWritebytes;
        bool       mDrain;
};

} // end namespace android

#endif // end of ANDROID_AUDIO_ALSA_PLAYBACK_HANDLER_NORMAL_H

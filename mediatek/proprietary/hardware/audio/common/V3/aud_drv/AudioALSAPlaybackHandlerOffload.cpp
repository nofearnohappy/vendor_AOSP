#include "AudioALSAPlaybackHandlerOffload.h"
#include "AudioALSAHardwareResourceManager.h"
#include "AudioVolumeFactory.h"

#include "AudioMTKFilter.h"
#include "AudioVUnlockDL.h"
#include "AudioALSADeviceParser.h"
#include "AudioALSADriverUtility.h"
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSASpeakerMonitor.h"
#endif
#include <tinycompress/tinycompress.h>

#include <pthread.h>
#include <linux/rtpm_prio.h>
#include <sys/prctl.h>


#define LOG_TAG "AudioALSAPlaybackHandlerOffload"

 



namespace android
{
static void *writeThreadOffload(void *arg);
static bool threadExit = true;
static   const char PROPERTY_KEY_EXTDAC[PROPERTY_KEY_MAX]  = "af.resouce.extdac_support";
static char const *const kOffloadDeviceName = "/dev/offloadservice";
static int mFd = -1;

struct offload_stream_property offload_stream; 

static void *offload_threadloop(void *arg)
{
    // force to set priority
    int command;
    struct offload_cmd *cmd;
    bool callback, exit, drain;
    stream_callback_event_t event;
    struct listnode *item;
    struct sched_param sched_p;

	pthread_mutex_lock(&offload_stream.offload_mutex);
	
	AudioALSAPlaybackHandlerOffload *pOffloadHandler = (AudioALSAPlaybackHandlerOffload *)arg;
	ALOGD("%s()+", __FUNCTION__);
	
    sched_getparam(0, &sched_p);
    sched_p.sched_priority = RTPM_PRIO_AUDIO_PLAYBACK;
    if (0 != sched_setscheduler(0, SCHED_RR, &sched_p)){
        ALOGE("[%s] failed, errno: %d", __FUNCTION__, errno);
    }
    else {
        sched_p.sched_priority = RTPM_PRIO_AUDIO_CCCI_THREAD;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());

    mFd = ::open(kOffloadDeviceName, O_RDWR);
	if (mFd < 0)
        ALOGE("%s() fail to open %s", __FUNCTION__, kOffloadDeviceName);

    offload_stream.offload_state = OFFLOAD_STATE_IDLE;
    offload_stream.remain_write = false;
    exit = false;
    drain = false;
    ::ioctl(mFd, OFFLOADSERVICE_SETMODE, OFFLOAD_MODE_SW);
	
    for(;;)
    {
        cmd     = NULL;
        command = -1;
        callback = false;
		
        if (list_empty(&offload_stream.offload_cmd_list)) {
            ALOGV("%s(),list_empty, state:%x, remain:%x", __FUNCTION__, offload_stream.offload_state, offload_stream.remain_write);
            if(drain && offload_stream.offload_state == OFFLOAD_STATE_PLAYING)
                command = OFFLOAD_CMD_DRAIN;
            else if(offload_stream.remain_write && offload_stream.offload_state == OFFLOAD_STATE_PLAYING)
                command = OFFLOAD_CMD_WRITE;
        }
        else {
            ALOGV("%s(),list not empty", __FUNCTION__);
            item = list_head(&offload_stream.offload_cmd_list);
            cmd = node_to_item(item, struct offload_cmd, node);
            command = cmd->cmd;
            list_remove(item);
            free(cmd);
        }

        if(command== -1) {
            ALOGV("%s(),waitevent+", __FUNCTION__);
            pthread_cond_wait(&offload_stream.offload_cond, &offload_stream.offload_mutex);
            ALOGV("%s(),waitevent-", __FUNCTION__);
            continue;
        }
        pthread_mutex_unlock(&offload_stream.offload_mutex); 
        ALOGV("%s()command:%x", __FUNCTION__, command);

        switch(command)
        {
            case OFFLOAD_CMD_WRITE:
                if( pOffloadHandler->process_write() == OFFLOAD_WRITE_REMAIN ) {
                    offload_stream.remain_write = true;
                }
                else {
                    offload_stream.remain_write = false;
                    event = STREAM_CBK_EVENT_WRITE_READY;
                    callback = true;
                }
                ::ioctl(mFd, OFFLOADSERVICE_WRITEBLOCK, 0);
                break;
            case OFFLOAD_CMD_DRAIN:
                if(offload_stream.offload_state == OFFLOAD_STATE_PLAYING)
                    ::ioctl(mFd, OFFLOADSERVICE_WRITEBLOCK, 1);
                if(list_empty(&offload_stream.offload_cmd_list) )
                {
                    event = STREAM_CBK_EVENT_DRAIN_READY;
                    callback = true;
                    ALOGV("%s() drain callback notify", __FUNCTION__);
                }
                break;
            case OFFLOAD_CMD_PAUSE:
                if(offload_stream.offload_state == OFFLOAD_STATE_PLAYING)
                    offload_stream.offload_state = OFFLOAD_STATE_PAUSED;
                break;
            case OFFLOAD_CMD_RESUME:
                if(offload_stream.offload_state == OFFLOAD_STATE_PAUSED)
                    offload_stream.offload_state = OFFLOAD_STATE_PLAYING;
                break;
            case OFFLOAD_CMD_FLUSH:
                if(offload_stream.offload_state == OFFLOAD_STATE_PLAYING || offload_stream.offload_state == OFFLOAD_STATE_PAUSED)
                {
                    offload_stream.offload_state = OFFLOAD_STATE_IDLE;
                    pOffloadHandler->offload_initialize();
                    offload_stream.remain_write = false;
                }
                break;
            case OFFLOAD_CMD_CLOSE:
                exit = true;
                break;
            default:
                ALOGE("%s(),Invalid Command", __FUNCTION__);
                break;
        }

        if(callback)
            pOffloadHandler->offload_callback(event);

		pthread_mutex_lock(&offload_stream.offload_mutex);

        if(exit)
        {
            pOffloadHandler->offload_callback(STREAM_CBK_EVENT_WRITE_READY);
            pOffloadHandler->offload_callback(STREAM_CBK_EVENT_DRAIN_READY);
            break;
        }

    }
    pthread_mutex_unlock(&offload_stream.offload_mutex);
    ALOGD("%s()-", __FUNCTION__);
    ::close(mFd);
    threadExit = true;

	return NULL;
}

static int send_offload_cmd(int command)
{
    struct offload_cmd *cmd = (struct offload_cmd *)calloc(1, sizeof(struct offload_cmd));

    ALOGD("%s %d", __FUNCTION__, command);

    cmd->cmd = command;
    list_add_tail(&offload_stream.offload_cmd_list, &cmd->node);
    pthread_mutex_lock(&offload_stream.offload_mutex);
    pthread_cond_signal(&offload_stream.offload_cond);
    pthread_mutex_unlock(&offload_stream.offload_mutex);
    return 0;
}

AudioALSAPlaybackHandlerOffload::AudioALSAPlaybackHandlerOffload(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerBase(stream_attribute_source),
    mDecBsbuf(NULL),
    mDecPcmbuf(NULL),
    mDecBsbufSize(0),
    mDecPcmbufSize(0),
    mDecPcmbufRemain(0),
    mDecBsbufRemain(0),
    mDecHeaderParsed(false),
    mReady(false),
    mDrain(false)
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_OFFLOAD;
    mFormat = stream_attribute_source->audio_format;
    memset(&mComprConfig, 0, sizeof(mComprConfig));
    //ASSERT(false);
    mMixer = AudioALSADriverUtility::getInstance()->getMixer();   //DOUG TO CHECK
}


AudioALSAPlaybackHandlerOffload::~AudioALSAPlaybackHandlerOffload()
{
    ALOGD("%s()", __FUNCTION__);
}

void AudioALSAPlaybackHandlerOffload::offload_initialize()
{
    mReady = false;
    mDecPcmbufRemain = 0;
    mDecBsbufRemain  = 0;
}


status_t AudioALSAPlaybackHandlerOffload::setFilterMng(AudioMTKFilterManager *pFilterMng)
{
    ALOGD("+%s() mAudioFilterManagerHandler [0x%x]", __FUNCTION__, pFilterMng);
    mAudioFilterManagerHandler = pFilterMng;
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

void AudioALSAPlaybackHandlerOffload::offload_callback(stream_callback_event_t event)
{
    if(mCbkCookie != NULL)
        mStreamCbk(event, 0, mCbkCookie);
}


uint32_t AudioALSAPlaybackHandlerOffload::GetLowJitterModeSampleRate()
{
    return 48000;
}


bool AudioALSAPlaybackHandlerOffload::SetLowJitterMode(bool bEnable,uint32_t SampleRate)
{
    ALOGD("%s() bEanble = %d SampleRate = %u", __FUNCTION__, bEnable,SampleRate);

    enum mixer_ctl_type type;
    struct mixer_ctl *ctl;
    int retval = 0;

    // check need open low jitter mode
    if(SampleRate <= GetLowJitterModeSampleRate() && (AudioALSADriverUtility::getInstance()->GetPropertyValue(PROPERTY_KEY_EXTDAC)) == false)
    {
        ALOGD("%s(), bEanble = %d", __FUNCTION__, bEnable);
        return false;
    }

    ctl = mixer_get_ctl_by_name(mMixer, "Audio_I2S0dl1_hd_Switch");

    if (ctl == NULL)
    {
        ALOGE("Audio_I2S0dl1_hd_Switch not support");
        return false;
    }

    if (bEnable == true)
    {
        retval = mixer_ctl_set_enum_by_string(ctl, "On");
        ASSERT(retval == 0);
    }
    else
    {
        retval = mixer_ctl_set_enum_by_string(ctl, "Off");
        ASSERT(retval == 0);
    }
    return true;
}


status_t AudioALSAPlaybackHandlerOffload::open()
{
    ALOGD("+%s(), mDevice = 0x%x", __FUNCTION__, mStreamAttributeSource->output_devices);
    ALOGD("%s(), mStreamAttributeSource: format = %d",__FUNCTION__, mStreamAttributeSource->audio_format);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    // debug pcm dump
    OpenPCMDump(LOG_TAG);
    // acquire pmic clk
    mHardwareResourceManager->EnableAudBufClk(true);
    //doug to check
    
    //HpImpeDanceDetect();  //doug to check
#if 1
    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmDl1Meida);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmDl1Meida);

    ALOGD("AudioALSAPlaybackHandlerOffload::open() pcmindex = %d", pcmindex);
    ListPcmDriver(cardindex, pcmindex);

    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    mStreamAttributeTarget.buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    ALOGD("%s buffersizemax = %d", __FUNCTION__, mStreamAttributeTarget.buffer_size);
    pcm_params_free(params);
#endif
    //mStreamAttributeTarget.buffer_size = 32768;

//#ifdef PLAYBACK_USE_24BITS_ONLY
        //mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_32_BIT;
        mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_8_24_BIT;
        //mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_16_BIT;
//#else
	//	mStreamAttributeTarget.audio_format = (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_32_BIT) ? AUDIO_FORMAT_PCM_8_24_BIT : AUDIO_FORMAT_PCM_16_BIT;
//#endif


    mStreamAttributeTarget.audio_channel_mask = AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);
    mStreamAttributeTarget.sample_rate = ChooseTargetSampleRate(mStreamAttributeSource->sample_rate);
    // HW pcm config
    memset(&mConfig, 0, sizeof(mConfig));    
    mConfig.channels = mStreamAttributeTarget.num_channels;
    mConfig.rate = mStreamAttributeTarget.sample_rate; 
    
    // Buffer size: 1536(period_size) * 2(ch) * 4(byte) * 2(period_count) = 24 kb

    mConfig.period_count = 2;
    mConfig.period_size = (mStreamAttributeTarget.buffer_size / (mConfig.channels * mConfig.period_count)) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);

    mConfig.format = transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d",
          __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format);

    mComprConfig.codec = (struct snd_codec*)malloc(sizeof(struct snd_codec));
    if(mComprConfig.codec == NULL)
        ALOGE("%s(), allocate mComprConfig.codec fail");
    mComprConfig.fragments = 1024;
    mComprConfig.fragment_size = 8192;
    //mComprConfig.fragment_size = mStreamAttributeTarget.buffer_size;
    mComprConfig.codec->sample_rate = mStreamAttributeTarget.sample_rate;
    mComprConfig.codec->reserved[0] = mConfig.period_size;
    mComprConfig.codec->reserved[1] = mComprConfig.fragments*mComprConfig.fragment_size;
    if(mConfig.format == PCM_FORMAT_S16_LE)
        mComprConfig.codec->format = SNDRV_PCM_FORMAT_S16_LE;
    else
        mComprConfig.codec->format = SNDRV_PCM_FORMAT_S32_LE;
    mComprConfig.codec->id = SND_AUDIOCODEC_MP3;
    mComprConfig.codec->ch_in = 2;
    mComprConfig.codec->ch_out = 2;
	

    //init decoder
    mDecHandler = AudioDecHandlerCreate();
    if(mDecHandler == NULL)
    {
        ALOGE("+%s(), DecHandler create fail", __FUNCTION__);
        ASSERT(false);
        return -ENOSYS;
    }
    if(!mDecHandler->InitAudioDecoder())
    {
        ALOGE("+%s(), Decoder IP init fail", __FUNCTION__);
        ASSERT(false);
        return -ENOSYS;
    }
    
    // post processing
    initPostProcessing();
       // SRC
    initBliSrc();

    // bit conversion
    initBitConverter();

    initDataPending();

    // disable lowjitter mode   //doug to check
    SetLowJitterMode(true, mStreamAttributeTarget.sample_rate);

    openComprDriver(23);

	if( compress_set_gapless_metadata(mComprStream, &offload_stream.offload_mdata) != 0)
        ALOGE("%s(), compress_set_gapless_metadata() error= %s", __FUNCTION__, compress_get_error(mComprStream));

    mHardwareResourceManager->startOutputDevice(mStreamAttributeSource->output_devices, mStreamAttributeTarget.sample_rate);

    offload_stream.tmpBuffer = (void*)malloc(mComprConfig.fragment_size);
	mWritebytes = mComprConfig.fragment_size;

	list_init(&offload_stream.offload_cmd_list);

    int ret = pthread_mutex_init(&offload_stream.offload_mutex, NULL);
    if (ret != 0)
    {
        ALOGE("%s, Failed to initialize Mutex!", __FUNCTION__);
        ASSERT(false);
        return -ENOSYS;
    }
	
    ret = pthread_cond_init(&offload_stream.offload_cond, NULL);
    if (ret != 0)
    {
        ALOGE("%s, Failed to initialize Cond!", __FUNCTION__);
        ASSERT(false);
        return -ENOSYS;
    }

	threadExit = false;
    ret = pthread_create(&offload_stream.offload_pthread, NULL, &offload_threadloop, this);

    if (ret != 0)
    {
        ALOGE("%s() create thread OffloadWrite fail!!", __FUNCTION__);
        ASSERT(false);
        return -ENOSYS;
    }

    usleep(1 * 1000);
	
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
 
}

status_t AudioALSAPlaybackHandlerOffload::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

	//close compress device driver
	send_offload_cmd(OFFLOAD_CMD_CLOSE);
    while(!threadExit)
    {
        usleep(1 * 1000);
    }
    pthread_join(offload_stream.offload_pthread, (void **) NULL);
    pthread_cond_destroy(&offload_stream.offload_cond);
    pthread_mutex_destroy(&offload_stream.offload_mutex);
    closeComprDriver();

    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        VUnlockhdl->SetInputStandBy(true);
    }
    //===========================================


    // close codec driver
    mHardwareResourceManager->stopOutputDevice();

    // close pcm driver
    //closePcmDriver();

    // disable lowjitter mode  //doug to check
    SetLowJitterMode(false, mStreamAttributeTarget.sample_rate);

    DeinitDataPending();

    // bit conversion
    deinitBitConverter();

    // SRC
    deinitBliSrc();

    // post processing
    deinitPostProcessing();

    // debug pcm dump
    ClosePCMDump();

    //release pmic clk
    mHardwareResourceManager->EnableAudBufClk(false);
    //SetMHLChipEnable(false);   //doug to check

    //close decoder
    mDecHandler->DeinitAudioDecoder();
	
    //free codec params
    if(mDecBsbuf != NULL)
    {
        free(mDecBsbuf);
        mDecBsbuf = NULL;
    }
	
    if(mDecPcmbuf != NULL)
    {
        free(mDecPcmbuf);
		mDecPcmbuf = NULL;
    }

    ALOGD("%s(), mComprConfig.codec:%p", mComprConfig.codec);

	if(mComprConfig.codec)
    {
        free((void*)mComprConfig.codec);
        mComprConfig.codec = NULL;
    }

    memset(&mComprConfig,0, sizeof(mComprConfig));

//    offload_callback(STREAM_CBK_EVENT_WRITE_READY);
 //   usleep(1 * 1000);
   // offload_callback(STREAM_CBK_EVENT_DRAIN_READY);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerOffload::routing(const audio_devices_t output_devices)
{
    mHardwareResourceManager->changeOutputDevice(output_devices);
    if (mAudioFilterManagerHandler) { mAudioFilterManagerHandler->setDevice(output_devices); }
    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerOffload::pause()
{
    ALOGD("%s() state:%x", __FUNCTION__, offload_stream.offload_state);
    if(compress_pause(mComprStream) != 0 )
        ALOGE("%s() error:%s", __FUNCTION__, compress_get_error(mComprStream) );

    send_offload_cmd(OFFLOAD_CMD_PAUSE);
    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerOffload::resume()
{
    ALOGD("%s() state:%x", __FUNCTION__, offload_stream.offload_state);
    if( compress_resume(mComprStream) != 0 )
        ALOGE("%s() error:%s", __FUNCTION__, compress_get_error(mComprStream) );

    send_offload_cmd(OFFLOAD_CMD_RESUME);
    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerOffload::setVolume(uint32_t vol)
{
    ALOGD("%s() VOL:0x%x handle:%p", __FUNCTION__, vol, mComprStream);
    offload_stream.offload_gain = vol;
    if(mComprStream != NULL)
        ::ioctl(mFd, OFFLOADSERVICE_SETGAIN, vol);
    return NO_ERROR;
}

int AudioALSAPlaybackHandlerOffload::drain(audio_drain_type_t type)
{
    ALOGD("%s()", __FUNCTION__);
    mDrain = true;
    send_offload_cmd(OFFLOAD_CMD_DRAIN);
    //if( compress_drain(mComprStream) != 0 )
        //ALOGE("%s() error:%s", __FUNCTION__, compress_get_error(mComprStream) );

    return 0;
}

status_t AudioALSAPlaybackHandlerOffload::flush()
{
    ALOGD("%s() state:%x", __FUNCTION__, offload_stream.offload_state);
    if( compress_stop(mComprStream) != 0 )
        ALOGE("%s() error:%s", __FUNCTION__, compress_get_error(mComprStream) );

    send_offload_cmd(OFFLOAD_CMD_FLUSH);
    return NO_ERROR;
}


ssize_t AudioALSAPlaybackHandlerOffload::write(const void *buffer, size_t bytes)
{
    // const -> to non const
    void *pBuffer = const_cast<void *>(buffer);
	
    ASSERT(pBuffer != NULL);

    if(mDecBsbuf == NULL)
    {
        mDecBsbufSize = mWritebytes + mDecHandler->BsbufferSize();
        mDecBsbuf = (int8_t*)malloc(mDecBsbufSize);
        ALOGD("%s(), alloc Bitstream buffer:%p", __FUNCTION__, mDecBsbuf);
        if(mDecBsbuf == NULL)
        {
            ALOGE("%s(), alloc Bs buffer fail", __FUNCTION__);
            ASSERT(false);
            return -ENOMEM;
        }
    }

    if(mDecPcmbuf == NULL)
    {
        if(mWritebytes >= mDecHandler->PcmbufferSize())
           mDecPcmbufSize = mWritebytes<<1;
        else
           mDecPcmbufSize = mDecHandler->PcmbufferSize()<<1;
        mDecPcmbuf = (int8_t*)malloc(mDecPcmbufSize);
        ALOGD("%s(), alloc Pcm buffer:%p", __FUNCTION__, mDecPcmbuf);
        if(mDecPcmbuf == NULL)
        {
            ALOGE("%s(), alloc Pcm buffer fail", __FUNCTION__);
            ASSERT(false);
            return -ENOMEM;
        }
    }

    memcpy(mDecBsbuf + mDecBsbufRemain, buffer, bytes);
    mDecBsbufRemain += bytes;
    ALOGD("%s(), send command ", __FUNCTION__);

    send_offload_cmd(OFFLOAD_CMD_WRITE);

    return bytes;

}


uint32_t AudioALSAPlaybackHandlerOffload::ChooseTargetSampleRate(uint32_t SampleRate)
{
    uint32_t TargetSampleRate = 44100;
    if(SampleRate <=  192000 && SampleRate > 96000)
    {
        TargetSampleRate = 192000;
    }
    else if(SampleRate <=96000 && SampleRate > 48000)
    {
        TargetSampleRate = 96000;
    }
    else
    {
        TargetSampleRate = SampleRate;
    }
    return TargetSampleRate;
}


AudioMTKDecHandlerBase *AudioALSAPlaybackHandlerOffload::AudioDecHandlerCreate()
{
    AudioMTKDecHandlerBase *pDecHandler = NULL;
    switch(mFormat)
    {
        case AUDIO_FORMAT_MP3:
            pDecHandler = new AudioMTKDecHandlerMP3();
            break;
        default:
            break;
    }

    if (NULL == pDecHandler)
    {
        ALOGD("-%s(), CreateFail", __FUNCTION__);
			return NULL;
    }

    return pDecHandler;
}


int AudioALSAPlaybackHandlerOffload::process_write()
{
    while(1)
    {
        int32_t consumed;
        
        if(mDecBsbufRemain < mDecHandler->BsbufferSize() && mDecPcmbufRemain < mWritebytes)
            return OFFLOAD_WRITE_EMPTY; 

        if(mDecPcmbufRemain >= mWritebytes)
        {
            memcpy(offload_stream.tmpBuffer, mDecPcmbuf, mWritebytes);
            mDecPcmbufRemain -= mWritebytes;
            memmove(mDecPcmbuf, mDecPcmbuf+mWritebytes, mDecPcmbufRemain);
	          
            // stereo to mono for speaker
            if (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_16_BIT) // AudioMixer will perform stereo to mono when 32-bit
            {
                doStereoToMonoConversionIfNeed(offload_stream.tmpBuffer, mWritebytes);
            }
		     
            // post processing (can handle both Q1P16 and Q1P31 by audio_format_t)
            void *pBufferAfterPostProcessing = NULL;
            uint32_t bytesAfterPostProcessing = 0;
            doPostProcessing(offload_stream.tmpBuffer, mWritebytes, &pBufferAfterPostProcessing, &bytesAfterPostProcessing);
	      
            // SRC
            void *pBufferAfterBliSrc = NULL;
            uint32_t bytesAfterBliSrc = 0;
            doBliSrc(pBufferAfterPostProcessing, bytesAfterPostProcessing, &pBufferAfterBliSrc, &bytesAfterBliSrc);
		     
            // bit conversion
            void *pBufferAfterBitConvertion = NULL;
            uint32_t bytesAfterBitConvertion = 0;
            doBitConversion(pBufferAfterBliSrc, bytesAfterBliSrc, &pBufferAfterBitConvertion, &bytesAfterBitConvertion);
		    int *ptr32;
			int ptr32_temp, ptr32_cnt;
			ptr32 = (int*)pBufferAfterBitConvertion;
	#if 0
			for(ptr32_cnt = 0; ptr32_cnt<(bytesAfterBitConvertion>>2); ptr32_cnt++)
			{
			    ptr32_temp = *ptr32;
			    *ptr32 = ptr32_temp<<8;
				*ptr32++;
			}
	#endif
            // data pending
            void *pBufferAfterPending = NULL;
            uint32_t bytesAfterpending = 0;
            dodataPending(pBufferAfterBitConvertion, bytesAfterBitConvertion, &pBufferAfterPending, &bytesAfterpending);
		     
            // pcm dump
            WritePcmDumpData(pBufferAfterPending, bytesAfterpending);
		     
            //int retval = compress_write(mComprStream, offload_stream.tmpBuffer, mWritebytes);
            int retval = compress_write(mComprStream, pBufferAfterPending, bytesAfterpending);
            
            if (retval < 0)
            {
                ALOGE("%s(), compress_write() error, retval = %d, %s", __FUNCTION__, retval, compress_get_error(mComprStream));
            }
            else
            {
                if(!mReady)
                {
                    mReady = true;
                    if( offload_stream.offload_state == OFFLOAD_STATE_IDLE)
                        offload_stream.offload_state = OFFLOAD_STATE_PLAYING;
                    compress_nonblock(mComprStream, 1);
                    compress_start(mComprStream);
                }
            }
        #if 0
            if( compress_blockwrite(mComprStream) != 0)
                ALOGE("%s(), compress_blockwrite() error= %s", __FUNCTION__, compress_get_error(mComprStream));
        #endif

			
            return OFFLOAD_WRITE_REMAIN;
        }

        ALOGV("%s(),Decode+ %x, %x ", __FUNCTION__, mDecBsbufRemain, mDecPcmbufRemain);
        if(mDecBsbufRemain >= mDecHandler->BsbufferSize())
            consumed = mDecHandler->DecodeAudio( mDecBsbuf, mDecPcmbuf+mDecPcmbufRemain, mDecBsbufRemain);

        if(consumed < 0)
        {
            ALOGD("%s(), Decoder return error:%x", __FUNCTION__, consumed);
        }
        else
        {
            mDecBsbufRemain -= consumed;
            memmove(mDecBsbuf, mDecBsbuf + consumed, mDecBsbufRemain);
            mDecPcmbufRemain += mDecHandler->PcmbufferSize();
        }
        ALOGD("%s(),Decode- %x, %x", __FUNCTION__, mDecBsbufRemain, mDecPcmbufRemain);
    }

}



} // end of namespace android



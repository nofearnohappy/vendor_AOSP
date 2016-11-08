#include "AudioALSAPlaybackHandlerBTSCO.h"

#include "AudioALSADriverUtility.h"
#include "AudioALSAHardwareResourceManager.h"
#include "AudioLock.h"

#include "WCNChipController.h"


#define LOG_TAG "AudioALSAPlaybackHandlerBTSCO"


#if defined(CONFIG_MT_ENG_BUILD)
#define DEBUG_LATENCY
#endif

#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)

namespace android
{

AudioALSAPlaybackHandlerBTSCO::AudioALSAPlaybackHandlerBTSCO(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerBase(stream_attribute_source),
    mWCNChipController(WCNChipController::GetInstance())
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_BT_SCO;
}


AudioALSAPlaybackHandlerBTSCO::~AudioALSAPlaybackHandlerBTSCO()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSAPlaybackHandlerBTSCO::open()
{
    ALOGD("+%s(), mDevice = 0x%x", __FUNCTION__, mStreamAttributeSource->output_devices);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // debug pcm dump
    OpenPCMDump(LOG_TAG);

    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVOIPCallBTPlayback);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmVOIPCallBTPlayback);

    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
#ifdef PLAYBACK_USE_24BITS_ONLY
    const uint32_t bt_max_buffer_size = 8192; // 8k
#else
    const uint32_t bt_max_buffer_size = 4096; // 4k
#endif
    const uint32_t kernel_max_buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    mStreamAttributeTarget.buffer_size = (kernel_max_buffer_size < bt_max_buffer_size) ? kernel_max_buffer_size : bt_max_buffer_size;
    ALOGD("buffersizemax = %d", mStreamAttributeTarget.buffer_size);
    pcm_params_free(params);

    // HW attribute config // TODO(Harvey): query this
#ifdef PLAYBACK_USE_24BITS_ONLY
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_8_24_BIT;
#else
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_16_BIT;
#endif
    mStreamAttributeTarget.audio_channel_mask = AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);
    mStreamAttributeTarget.sample_rate = mWCNChipController->GetBTCurrentSamplingRateNumber();


    // HW pcm config
    mConfig.channels = mStreamAttributeTarget.num_channels;
    mConfig.rate = mStreamAttributeTarget.sample_rate;

    mConfig.period_count = 2;
    mConfig.period_size = (mStreamAttributeTarget.buffer_size / (mConfig.channels * mConfig.period_count)) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);

    mConfig.format = transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d",
          __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format);

    // SRC
    initBliSrc();

    // bit conversion
    initBitConverter();

    initDataPending();

    // open pcm driver
    openPcmDriver(pcmindex);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerBTSCO::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // close pcm driver
    closePcmDriver();

    DeinitDataPending();


    // bit conversion
    deinitBitConverter();


    // SRC
    deinitBliSrc();


    // debug pcm dump
    ClosePCMDump();


    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerBTSCO::routing(const audio_devices_t output_devices)
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerBTSCO::pause()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerBTSCO::resume()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerBTSCO::flush()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerBTSCO::setVolume(uint32_t vol)
{
    return INVALID_OPERATION;
}

int AudioALSAPlaybackHandlerBTSCO::drain(audio_drain_type_t type)
{
    return 0;
}

ssize_t AudioALSAPlaybackHandlerBTSCO::write(const void *buffer, size_t bytes)
{
    ALOGV("%s(), buffer = %p, bytes = %d", __FUNCTION__, buffer, bytes);

    if (mPcm == NULL)
    {
        ALOGE("%s(), mPcm == NULL, return", __FUNCTION__);
        return bytes;
    }

    // const -> to non const
    void *pBuffer = const_cast<void *>(buffer);
    ASSERT(pBuffer != NULL);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[0] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

    // SRC
    void *pBufferAfterBliSrc = NULL;
    uint32_t bytesAfterBliSrc = 0;
    doBliSrc(pBuffer, bytes, &pBufferAfterBliSrc, &bytesAfterBliSrc);


    // bit conversion
    void *pBufferAfterBitConvertion = NULL;
    uint32_t bytesAfterBitConvertion = 0;
    doBitConversion(pBufferAfterBliSrc, bytesAfterBliSrc, &pBufferAfterBitConvertion, &bytesAfterBitConvertion);


    // data pending
    void *pBufferAfterPending = NULL;
    uint32_t bytesAfterpending = 0;
    dodataPending(pBufferAfterBitConvertion, bytesAfterBitConvertion, &pBufferAfterPending, &bytesAfterpending);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[1] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

    // write data to pcm driver
    WritePcmDumpData(pBufferAfterPending, bytesAfterpending);
    int retval = pcm_write(mPcm, pBufferAfterPending, bytesAfterpending);


#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[2] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif


    if (retval != 0)
    {
        ALOGE("%s(), pcm_write() error, retval = %d", __FUNCTION__, retval);
    }

#ifdef DEBUG_LATENCY
    ALOGD("AudioALSAPlaybackHandlerNormal::write (-) latency_in_us,%1.6lf,%1.6lf,%1.6lf", latencyTime[0], latencyTime[1], latencyTime[2]);
#endif


    return bytes;
}


} // end of namespace android

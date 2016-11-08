#include "AudioALSAPlaybackHandlerSphDL.h"

#include "AudioALSAHardwareResourceManager.h"

#include "AudioMTKFilter.h"
#include "AudioALSADeviceParser.h"
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSASpeakerMonitor.h"
#endif
#include "AudioALSADriverUtility.h"
#ifdef MTK_DYNAMIC_CHANGE_HAL_BUFFER_SIZE
#include "AudioDef.h"
#endif


#define LOG_TAG "AudioALSAPlaybackHandlerSphDL"

//#define DEBUG_LATENCY
#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)

namespace android
{

AudioALSAPlaybackHandlerSphDL::AudioALSAPlaybackHandlerSphDL(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerNormal(stream_attribute_source)
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_NORMAL;
    mMixer = AudioALSADriverUtility::getInstance()->getMixer();
}


AudioALSAPlaybackHandlerSphDL::~AudioALSAPlaybackHandlerSphDL()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSAPlaybackHandlerSphDL::open()
{
    ALOGD("+%s(), mDevice = 0x%x", __FUNCTION__, mStreamAttributeSource->output_devices);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // debug pcm dump
    OpenPCMDump(LOG_TAG);
    //Echo reference path
    if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Dl1_MD_Echo_Ref_Switch"), "On"))
    {
        ALOGE("Error: Audio_Dl1_MD_Echo_Ref_Switch invalid value");
    }
    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmI2S0Dl1Playback);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmI2S0Dl1Playback);
    ALOGD("AudioALSAPlaybackHandlerSphDL::open() pcmindex = %d", pcmindex);
    ListPcmDriver(cardindex, pcmindex);

    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    mStreamAttributeTarget.buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    ALOGD("buffersizemax = %d", mStreamAttributeTarget.buffer_size);
    pcm_params_free(params);


    // HW attribute config // TODO(Harvey): query this
#ifdef PLAYBACK_USE_24BITS_ONLY
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_8_24_BIT;
#else
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_16_BIT;
#endif
    mStreamAttributeTarget.audio_channel_mask = AUDIO_CHANNEL_IN_MONO;
    mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);
    mStreamAttributeTarget.sample_rate = mStreamAttributeSource->sample_rate; // same as source stream


    // HW pcm config
    memset(&mConfig, 0, sizeof(mConfig));    
    mConfig.channels = mStreamAttributeTarget.num_channels;
    mConfig.rate = mStreamAttributeTarget.sample_rate;

    // Buffer size: 1536(period_size) * 2(ch) * 4(byte) * 2(period_count) = 24 kb

    mConfig.period_count = 4;
    mConfig.period_size = 512;

    mConfig.format = transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d",
          __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format);


    // post processing
    initPostProcessing();

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    unsigned int fc, bw;
    int th;
    if (mAudioFilterManagerHandler)
    {
        AudioALSASpeakerMonitor::getInstance()->GetFilterParam(&fc, &bw, &th);
        ALOGD("%s(), fc %d bw %d, th %d", __FUNCTION__, fc, bw, th);
        mAudioFilterManagerHandler->setSpkFilterParam(fc, bw, th);
    }
#endif

    // SRC
    initBliSrc();


    // bit conversion
    initBitConverter();


    // open codec driver
    mHardwareResourceManager->startOutputDevice(mStreamAttributeSource->output_devices, mStreamAttributeTarget.sample_rate);

    // open pcm driver
    openPcmDriver(pcmindex);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerSphDL::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // close codec driver
    mHardwareResourceManager->stopOutputDevice();

    //Echo reference path
    if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Dl1_MD_Echo_Ref_Switch"), "Off"))
    {
        ALOGE("Error: Audio_Dl1_MD_Echo_Ref_Switch invalid value");
    }

    // close pcm driver
    closePcmDriver();


    // bit conversion
    deinitBitConverter();


    // SRC
    deinitBliSrc();


    // post processing
    deinitPostProcessing();


    // debug pcm dump
    ClosePCMDump();


    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerSphDL::routing(const audio_devices_t output_devices)
{
    mHardwareResourceManager->changeOutputDevice(output_devices);
    if (mAudioFilterManagerHandler) { mAudioFilterManagerHandler->setDevice(output_devices); }
    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerSphDL::pause()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerSphDL::resume()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerSphDL::flush()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerSphDL::setVolume(uint32_t vol)
{
    return INVALID_OPERATION;
}

int AudioALSAPlaybackHandlerSphDL::drain(audio_drain_type_t type)
{
    return 0;
}


status_t AudioALSAPlaybackHandlerSphDL::setLowLatencyMode(bool mode, size_t buffer_size, size_t reduceInterruptSize, bool bforce)
{
    return NO_ERROR;
}

ssize_t AudioALSAPlaybackHandlerSphDL::write(const void *buffer, size_t bytes)
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
#if 0
    // stereo to mono for speaker, but for sphDL it's already mono, so ignore.
    if (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_16_BIT) // AudioMixer will perform stereo to mono when 32-bit
    {
        doStereoToMonoConversionIfNeed(pBuffer, bytes);
    }
#endif

    // post processing (can handle both Q1P16 and Q1P31 by audio_format_t)
    void *pBufferAfterPostProcessing = NULL;
    uint32_t bytesAfterPostProcessing = 0;
    doPostProcessing(pBuffer, bytes, &pBufferAfterPostProcessing, &bytesAfterPostProcessing);


    // SRC
    void *pBufferAfterBliSrc = NULL;
    uint32_t bytesAfterBliSrc = 0;
    doBliSrc(pBufferAfterPostProcessing, bytesAfterPostProcessing, &pBufferAfterBliSrc, &bytesAfterBliSrc);


    // bit conversion
    void *pBufferAfterBitConvertion = NULL;
    uint32_t bytesAfterBitConvertion = 0;
    doBitConversion(pBufferAfterBliSrc, bytesAfterBliSrc, &pBufferAfterBitConvertion, &bytesAfterBitConvertion);


    // pcm dump
    WritePcmDumpData(pBufferAfterBitConvertion, bytesAfterBitConvertion);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[1] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

    // write data to pcm driver
    int retval = pcm_write(mPcm, pBufferAfterBitConvertion, bytesAfterBitConvertion);

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
    ALOGD("AudioALSAPlaybackHandlerSphDL::write (-) latency_in_us,%1.6lf,%1.6lf,%1.6lf", latencyTime[0], latencyTime[1], latencyTime[2]);
#endif

    return bytes;
}


status_t AudioALSAPlaybackHandlerSphDL::setFilterMng(AudioMTKFilterManager *pFilterMng)
{
    ALOGD("+%s() mAudioFilterManagerHandler [0x%x]", __FUNCTION__, pFilterMng);
    mAudioFilterManagerHandler = pFilterMng;
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


} // end of namespace android

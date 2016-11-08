#include "AudioALSAPlaybackHandlerFast.h"

#include "AudioALSAHardwareResourceManager.h"
//#include "AudioALSAVolumeController.h"
//#include "AudioVolumeInterface.h"
#include "AudioVolumeFactory.h"
#include "AudioALSASampleRateController.h"

#include "AudioMTKFilter.h"
#include "AudioVUnlockDL.h"
#include "AudioALSADeviceParser.h"
#include "AudioALSADriverUtility.h"

#if !defined(MTK_BASIC_PACKAGE)
#include <audio_utils/pulse.h>
#endif

#define LOG_TAG "AudioALSAPlaybackHandlerFast"

// Latency Detect
//#define DEBUG_LATENCY
#define THRESHOLD_FRAMEWORK   0.002
#define THRESHOLD_HAL         0.002
#define THRESHOLD_KERNEL      0.002

#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)
static   const char PROPERTY_KEY_EXTDAC[PROPERTY_KEY_MAX]  = "af.resouce.extdac_support";

namespace android
{

AudioALSAPlaybackHandlerFast::AudioALSAPlaybackHandlerFast(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerBase(stream_attribute_source)
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_FAST;
    mMixer = AudioALSADriverUtility::getInstance()->getMixer();
}


AudioALSAPlaybackHandlerFast::~AudioALSAPlaybackHandlerFast()
{
    ALOGD("%s()", __FUNCTION__);
}

uint32_t AudioALSAPlaybackHandlerFast::GetLowJitterModeSampleRate()
{
    return 48000;
}

bool AudioALSAPlaybackHandlerFast::SetLowJitterMode(bool bEnable,uint32_t SampleRate)
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

bool AudioALSAPlaybackHandlerFast::DeviceSupportHifi(audio_devices_t outputdevice)
{
    // modify this to let output device support hifi audio
    if(outputdevice == AUDIO_DEVICE_OUT_WIRED_HEADSET || outputdevice == AUDIO_DEVICE_OUT_WIRED_HEADPHONE || outputdevice == AUDIO_DEVICE_OUT_SPEAKER)
    {
        return true;
    }
    return false;
}


uint32_t AudioALSAPlaybackHandlerFast::ChooseTargetSampleRate(uint32_t SampleRate,audio_devices_t outputdevice)
{
    ALOGD("ChooseTargetSampleRate SampleRate = %d outputdevice = %d",SampleRate,outputdevice);
    uint32_t TargetSampleRate = 44100;
    if(SampleRate <=  192000 && SampleRate > 96000 && DeviceSupportHifi(outputdevice))
    {
        TargetSampleRate = 192000;
    }
    else if(SampleRate <=96000 && SampleRate > 48000&& DeviceSupportHifi(outputdevice))
    {
        TargetSampleRate = 96000;
    }
    else if(SampleRate <= 48000 && SampleRate >= 32000)
    {
        TargetSampleRate = SampleRate;
    }
    return TargetSampleRate;
}

status_t AudioALSAPlaybackHandlerFast::open()
{
    ALOGD("+%s(), mDevice = 0x%x", __FUNCTION__, mStreamAttributeSource->output_devices);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // debug pcm dump
    OpenPCMDump(LOG_TAG);
    // acquire pmic clk
    mHardwareResourceManager->EnableAudBufClk(true);
    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmDl2Meida);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmDl2Meida);

#ifdef MTK_MAXIM_SPEAKER_SUPPORT
    if (mStreamAttributeSource->output_devices == AUDIO_DEVICE_OUT_SPEAKER)
    {
        pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmDl2MeidaSpk);
        cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmDl2MeidaSpk);
    }
#endif

    ALOGD("AudioALSAPlaybackHandlerNormal::open() pcmindex = %d", pcmindex);
    ListPcmDriver(cardindex, pcmindex);

    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }

    // audio low latency param - playback - hw buffer size
    mStreamAttributeTarget.buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    pcm_params_free(params);

    ALOGD("AudioALSAPlaybackHandlerNormal::open, mStreamAttributeTarget.buffer_size %d", mStreamAttributeTarget.buffer_size);

    // HW attribute config // TODO(Harvey): query this
#ifdef PLAYBACK_USE_24BITS_ONLY
    mStreamAttributeTarget.audio_format = AUDIO_FORMAT_PCM_8_24_BIT;
#else
    mStreamAttributeTarget.audio_format = (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_32_BIT) ? AUDIO_FORMAT_PCM_8_24_BIT : AUDIO_FORMAT_PCM_16_BIT;
#endif
    mStreamAttributeTarget.audio_channel_mask = AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);

    mStreamAttributeTarget.sample_rate = ChooseTargetSampleRate(AudioALSASampleRateController::getInstance()->getPrimaryStreamOutSampleRate(),
                                                                mStreamAttributeSource->output_devices);

    // HW pcm config
    memset(&mConfig, 0, sizeof(mConfig));
    mConfig.channels = mStreamAttributeTarget.num_channels;
    mConfig.rate = mStreamAttributeTarget.sample_rate;

    // Buffer size: 1536(period_size) * 2(ch) * 4(byte) * 2(period_count) = 24 kb

    mConfig.period_count = 2;
    // audio low latency param - playback - interrupt rate
    mConfig.period_size = (mStreamAttributeSource->buffer_size / mConfig.channels) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);

    mConfig.format = transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d, buffer size %d %d",
          __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format,
          mStreamAttributeTarget.buffer_size, mStreamAttributeSource->buffer_size);

    mStreamAttributeTarget.mInterrupt = (mConfig.period_size+0.0) / mStreamAttributeTarget.sample_rate;

    // post processing
    initPostProcessing();

    // SRC
    initBliSrc();

    // bit conversion
    initBitConverter();

    initDataPending();

    // disable lowjitter mode
    SetLowJitterMode(true, mStreamAttributeTarget.sample_rate);

    // open pcm driver
    openPcmDriver(pcmindex);

    // open codec driver
    mHardwareResourceManager->startOutputDevice(mStreamAttributeSource->output_devices, mStreamAttributeTarget.sample_rate);


    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        VUnlockhdl->SetInputStandBy(false);
        VUnlockhdl-> GetSRCInputParameter(mStreamAttributeTarget.sample_rate, mStreamAttributeTarget.num_channels, mStreamAttributeTarget.audio_format);
        VUnlockhdl->GetFirstDLTime();
    }
    //===========================================


    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerFast::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

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
    closePcmDriver();

    // disable lowjitter mode
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


    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerFast::routing(const audio_devices_t output_devices)
{
    mHardwareResourceManager->changeOutputDevice(output_devices);
    if (mAudioFilterManagerHandler) { mAudioFilterManagerHandler->setDevice(output_devices); }
    return NO_ERROR;
}
status_t AudioALSAPlaybackHandlerFast::pause()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerFast::resume()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerFast::flush()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerFast::setVolume(uint32_t vol)
{
    return INVALID_OPERATION;
}


int AudioALSAPlaybackHandlerFast::drain(audio_drain_type_t type)
{
    return 0;
}


status_t AudioALSAPlaybackHandlerFast::setLowLatencyMode(bool mode, size_t buffer_size, size_t reduceInterruptSize, bool bforce)
{
    return NO_ERROR;
}

ssize_t AudioALSAPlaybackHandlerFast::write(const void *buffer, size_t bytes)
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

    // stereo to mono for speaker
    if (mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_16_BIT) // AudioMixer will perform stereo to mono when 32-bit
    {
        doStereoToMonoConversionIfNeed(pBuffer, bytes);
    }


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

    // data pending
    void *pBufferAfterPending = NULL;
    uint32_t bytesAfterpending = 0;
    dodataPending(pBufferAfterBitConvertion, bytesAfterBitConvertion, &pBufferAfterPending, &bytesAfterpending);

    // pcm dump
    WritePcmDumpData(pBufferAfterPending, bytesAfterpending);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[1] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

#ifdef MTK_LATENCY_DETECT_PULSE
    detectPulse(5, 800, 0, (void *)pBufferAfterPending, bytesAfterpending/mStreamAttributeTarget.num_channels/((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4),
                 mStreamAttributeTarget.audio_format, mStreamAttributeTarget.num_channels, mStreamAttributeTarget.sample_rate);
#endif

    // write data to pcm driver
    int retval = pcm_write(mPcm, pBufferAfterPending, bytesAfterpending);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[2] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

#ifndef DOWNLINK_LOW_LATENCY
    // TODO(Harvey, Wendy), temporary disable Voice Unlock until 24bit ready
    //============Voice UI&Unlock REFERECE=============
    AudioVUnlockDL *VUnlockhdl = AudioVUnlockDL::getInstance();
    if (VUnlockhdl != NULL)
    {
        // get remain time
        //VUnlockhdl->SetDownlinkStartTime(ret_ms);
        VUnlockhdl->GetFirstDLTime();

        //VUnlockhdl->SetInputStandBy(false);
        if (mStreamAttributeSource->output_devices & AUDIO_DEVICE_OUT_WIRED_HEADSET ||
            mStreamAttributeSource->output_devices & AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
        {
            memset((void *)pBufferAfterBitConvertion, 0, bytesAfterBitConvertion);
        }
        VUnlockhdl->WriteStreamOutToRing(pBufferAfterBitConvertion, bytesAfterBitConvertion);
    }
    //===========================================
#endif


    if (retval != 0)
    {
        ALOGE("%s(), pcm_write() error, retval = %d", __FUNCTION__, retval);
    }

#ifdef DEBUG_LATENCY
    if(latencyTime[0]>THRESHOLD_FRAMEWORK || latencyTime[1]>THRESHOLD_HAL || latencyTime[2]>(mStreamAttributeTarget.mInterrupt-latencyTime[0]-latencyTime[1]+THRESHOLD_KERNEL))
    {
        ALOGD("latency_in_s,%1.3lf,%1.3lf,%1.3lf, interrupt,%1.3lf", latencyTime[0], latencyTime[1], latencyTime[2], mStreamAttributeTarget.mInterrupt);
    }
#endif

    return bytes;
}


status_t AudioALSAPlaybackHandlerFast::setFilterMng(AudioMTKFilterManager *pFilterMng)
{
    ALOGD("+%s() mAudioFilterManagerHandler [0x%x]", __FUNCTION__, pFilterMng);
    mAudioFilterManagerHandler = pFilterMng;
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


} // end of namespace android

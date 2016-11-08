#include "AudioALSAPlaybackHandlerNormal.h"

#include "AudioALSAHardwareResourceManager.h"
//#include "AudioALSAVolumeController.h"
//#include "AudioVolumeInterface.h"
#include "AudioVolumeFactory.h"
#include "AudioALSASampleRateController.h"

#include "AudioMTKFilter.h"
#include "AudioVUnlockDL.h"
#include "AudioALSADeviceParser.h"
#include "AudioALSADriverUtility.h"
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSASpeakerMonitor.h"
#endif

#undef MTK_HDMI_SUPPORT

#if defined(MTK_HDMI_SUPPORT)
#include "AudioExtDisp.h"
#endif

#define LOG_TAG "AudioALSAPlaybackHandlerNormal"

#ifdef DEBUG_LATENCY
// Latency Detect
//#define DEBUG_LATENCY
#define THRESHOLD_FRAMEWORK   0.010
#define THRESHOLD_HAL         0.010
#define THRESHOLD_KERNEL      0.010
#endif

#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)
static   const char PROPERTY_KEY_EXTDAC[PROPERTY_KEY_MAX]  = "af.resouce.extdac_support";

namespace android
{

AudioALSAPlaybackHandlerNormal::AudioALSAPlaybackHandlerNormal(const stream_attribute_t *stream_attribute_source) :
    AudioALSAPlaybackHandlerBase(stream_attribute_source),
    mCurMuteBytes(0),
    mForceMute(false)
{
    ALOGD("%s()", __FUNCTION__);
    mPlaybackHandlerType = PLAYBACK_HANDLER_NORMAL;
    mMixer = AudioALSADriverUtility::getInstance()->getMixer();
}


AudioALSAPlaybackHandlerNormal::~AudioALSAPlaybackHandlerNormal()
{
    ALOGD("%s()", __FUNCTION__);
}

uint32_t AudioALSAPlaybackHandlerNormal::GetLowJitterModeSampleRate()
{
    return 48000;
}

bool AudioALSAPlaybackHandlerNormal::SetLowJitterMode(bool bEnable,uint32_t SampleRate)
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


void AudioALSAPlaybackHandlerNormal::OpenHpImpeDancePcm(void)
{
    ALOGD("OpenHpImpeDancePcm");
    // Don't lock Sram/Dram lock here, it would be locked by caller function

    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmHpimpedancePlayback);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmHpimpedancePlayback);
    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    int buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    ALOGD("buffer_size = %d", buffer_size);
    pcm_params_free(params);
    // HW pcm config
    mHpImpedanceConfig.channels = 2;
    mHpImpedanceConfig.rate = mStreamAttributeSource->sample_rate;
    mHpImpedanceConfig.period_count = 2;
    mHpImpedanceConfig.period_size = (buffer_size / (mHpImpedanceConfig.channels * mHpImpedanceConfig.period_count)) / 2;
    mHpImpedanceConfig.format = PCM_FORMAT_S16_LE;
    mHpImpedanceConfig.start_threshold = 0;
    mHpImpedanceConfig.stop_threshold = 0;
    mHpImpedanceConfig.silence_threshold = 0;
    ALOGD("%s(), mHpImpedanceConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d",
          __FUNCTION__, mHpImpedanceConfig.channels, mHpImpedanceConfig.rate, mHpImpedanceConfig.period_size, mHpImpedanceConfig.period_count, mHpImpedanceConfig.format);
    // open pcm driver
    mHpImpeDancePcm = pcm_open(cardindex, pcmindex, PCM_OUT, &mHpImpedanceConfig);
    if (mHpImpeDancePcm == NULL)
    {
        ALOGE("%s(), mPcm == NULL!!", __FUNCTION__);
    }
    ALOGD("-%s(), mPcm = %p", __FUNCTION__, mHpImpeDancePcm);
    ASSERT(mHpImpeDancePcm != NULL);
    pcm_start(mHpImpeDancePcm);
}

void AudioALSAPlaybackHandlerNormal::CloseHpImpeDancePcm(void)
{
    ALOGD("CloseHpImpeDancePcm");
    // Don't lock Sram/Dram lock here, it would be locked by caller function

    if (mHpImpeDancePcm != NULL)
    {
        pcm_stop(mHpImpeDancePcm);
        pcm_close(mHpImpeDancePcm);
        mHpImpeDancePcm = NULL;
    }
}

void AudioALSAPlaybackHandlerNormal::HpImpeDanceDetect(void)
{
    ALOGD("+HpImpeDanceDetect outputdevice = %d", mStreamAttributeSource->output_devices);
#if defined(MTK_AUDIO_GAIN_TABLE) == defined(MTK_NEW_VOL_CONTROL)
    if (mHardwareResourceManager->getHeadPhoneChange() == false)
    {
        return;
    }
    else
    {
        mHardwareResourceManager->setHeadPhoneChange(false);
    }

    if ((mStreamAttributeSource->output_devices != AUDIO_DEVICE_OUT_WIRED_HEADSET) &&
        (mStreamAttributeSource->output_devices != AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
    {
        return;
    }

    OpenHpImpeDancePcm();
#ifdef MTK_NEW_VOL_CONTROL
    AudioMTKGainController::getInstance()->GetHeadPhoneImpedance();
#else
    AudioALSAVolumeController::getInstance()->GetHeadPhoneImpedance();
#endif
    CloseHpImpeDancePcm();
    usleep(20*1000); //sleep 20ms, to avoid mtk_pcm_I2S0dl1_open() before mtk_soc_pcm_hp_impedance_close() since hp impedance use dram and normal playback use sram, their AFE_DL1_BASE setting will conflict!
#endif
    ALOGD("-HpImpeDanceDetect outputdevice = %d", mStreamAttributeSource->output_devices);
}

bool AudioALSAPlaybackHandlerNormal::DeviceSupportHifi(audio_devices_t outputdevice)
{
    // modify this to let output device support hifi audio
    if(outputdevice == AUDIO_DEVICE_OUT_WIRED_HEADSET || outputdevice == AUDIO_DEVICE_OUT_WIRED_HEADPHONE || outputdevice == AUDIO_DEVICE_OUT_SPEAKER)
    {
        return true;
    }
    return false;
}


uint32_t AudioALSAPlaybackHandlerNormal::ChooseTargetSampleRate(uint32_t SampleRate,audio_devices_t outputdevice)
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
status_t SetMHLChipEnable(int enable)
{
#if 0
    ALOGD("+%s()", __FUNCTION__);

#if defined(MTK_HDMI_SUPPORT)
    // File descriptor
    int fd_audio = ::open(HDMI_DRV, O_RDWR);
    ALOGD("%s(), open(%s), fd_audio = %d", __FUNCTION__, HDMI_DRV, fd_audio);

    if (fd_audio >= 0)
    {
        ::ioctl(fd_audio, MTK_HDMI_AUDIO_ENABLE, enable);

        ALOGD("%s(), ioctl:MTK_HDMI_AUDIO_FORMAT =0x%x \n", __FUNCTION__, enable);

        ::close(fd_audio);
    }
    ALOGD("-%s(), fd_audio=%d", __FUNCTION__, fd_audio);
#endif
#endif

    return NO_ERROR;
}

status_t AudioALSAPlaybackHandlerNormal::open()
{
    ALOGD("+%s(), mDevice = 0x%x", __FUNCTION__, mStreamAttributeSource->output_devices);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    int pcmindex = 0;
    int cardindex = 0;

    // debug pcm dump
    OpenPCMDump(LOG_TAG);
    // acquire pmic clk
    mHardwareResourceManager->EnableAudBufClk(true);
    HpImpeDanceDetect();

    pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmI2S0Dl1Playback);
    cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmI2S0Dl1Playback);

    #ifdef MTK_MAXIM_SPEAKER_SUPPORT
    if (mStreamAttributeSource->output_devices == AUDIO_DEVICE_OUT_SPEAKER)
    {
        pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmExtSpkMeida);
        cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmExtSpkMeida);
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
    mStreamAttributeTarget.buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    pcm_params_free(params);

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
    mConfig.period_size = (mStreamAttributeTarget.buffer_size / (mConfig.channels * mConfig.period_count)) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);

    mConfig.format = transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d",
          __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format);

#ifdef MTK_AUDIO_SW_DRE
    mStartMuteBytes = mConfig.period_size *
                      mConfig.period_count *
                      mConfig.channels *
                      (pcm_format_to_bits(mConfig.format) / 8);

    mAllZeroBlock = new char[mStreamAttributeSource->buffer_size];
    memset(mAllZeroBlock, 0, mStreamAttributeSource->buffer_size);
#endif

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


status_t AudioALSAPlaybackHandlerNormal::close()
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

#ifdef MTK_AUDIO_SW_DRE
    delete [] mAllZeroBlock;
#endif
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAPlaybackHandlerNormal::routing(const audio_devices_t output_devices)
{
    mHardwareResourceManager->changeOutputDevice(output_devices);
    if (mAudioFilterManagerHandler) { mAudioFilterManagerHandler->setDevice(output_devices); }
    return NO_ERROR;
}
status_t AudioALSAPlaybackHandlerNormal::pause()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerNormal::resume()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerNormal::flush()
{
    return INVALID_OPERATION;
}

status_t AudioALSAPlaybackHandlerNormal::setVolume(uint32_t vol)
{
    return INVALID_OPERATION;
}


int AudioALSAPlaybackHandlerNormal::drain(audio_drain_type_t type)
{
    return 0;
}


status_t AudioALSAPlaybackHandlerNormal::setLowLatencyMode(bool mode, size_t buffer_size, size_t reduceInterruptSize, bool bforce)
{
    if(0 == buffer_size)
    {
        buffer_size = mStreamAttributeSource->buffer_size;
    }

    int rate = mode ? (buffer_size / mStreamAttributeSource->num_channels) / ((mStreamAttributeSource->audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4) :
                   ((mStreamAttributeTarget.buffer_size - reduceInterruptSize) / mConfig.channels) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);

    mStreamAttributeTarget.mInterrupt = (rate+0.0) / mStreamAttributeTarget.sample_rate;

    ALOGD("%s, rate %d %f, mode = %d , buffer_size = %d, channel %d, format%d", __FUNCTION__, rate, mStreamAttributeTarget.mInterrupt, mode, buffer_size, mConfig.channels, mStreamAttributeTarget.audio_format);

    mHardwareResourceManager->setInterruptRate(rate);
    return NO_ERROR;
}

ssize_t AudioALSAPlaybackHandlerNormal::write(const void *buffer, size_t bytes)
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

#if defined(MTK_AUDIO_SW_DRE) && defined(MTK_NEW_VOL_CONTROL)
    if (mStreamAttributeSource->output_devices == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
        mStreamAttributeSource->output_devices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
    {
        bool isAllMute = false;

        /* check if contents is mute */
        if (!memcmp(mAllZeroBlock, buffer, bytes))
            isAllMute = true;

        /* calculate delay and apply mute */
        ALOGV("%s(), isAllMute = %d, mForceMute = %d, mCurMuteBytes = %d, mStartMuteBytes = %d",
              __FUNCTION__,
              isAllMute,
              mForceMute,
              mCurMuteBytes,
              mStartMuteBytes);

        if (isAllMute)
        {
            if (!mForceMute) /* not mute yet */
            {
                mCurMuteBytes += bytes;
                if (mCurMuteBytes >= mStartMuteBytes)
                {
                    mForceMute = true;
                    ALOGD("%s(), SWDRE swdre mute", __FUNCTION__);
                    AudioMTKGainController::getInstance()->requestMute(getIdentity(), true);
                }
            }
        }
        else
        {
            mCurMuteBytes = 0;

            if (mForceMute)
            {
                mForceMute = false;
                ALOGD("%s(), SWDRE swdre unmute", __FUNCTION__);
                AudioMTKGainController::getInstance()->requestMute(getIdentity(), false);
            }
        }
    }
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


    // write data to pcm driver
    int retval = pcm_write(mPcm, pBufferAfterPending, bytesAfterpending);

#ifdef DEBUG_LATENCY
    clock_gettime(CLOCK_REALTIME, &mNewtime);
    latencyTime[2] = calc_time_diff(mNewtime, mOldtime);
    mOldtime = mNewtime;
#endif

#if 1 // TODO(Harvey, Wendy), temporary disable Voice Unlock until 24bit ready
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


status_t AudioALSAPlaybackHandlerNormal::setFilterMng(AudioMTKFilterManager *pFilterMng)
{
    ALOGD("+%s() mAudioFilterManagerHandler [0x%x]", __FUNCTION__, pFilterMng);
    mAudioFilterManagerHandler = pFilterMng;
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


} // end of namespace android

#include "AudioALSAStreamManager.h"

#include <cutils/properties.h>

#include "AudioALSAStreamOut.h"
#include "AudioALSAStreamIn.h"

#include "AudioALSAPlaybackHandlerBase.h"
#include "AudioALSAPlaybackHandlerNormal.h"
#include "AudioALSAPlaybackHandlerFast.h"
#include "AudioALSAPlaybackHandlerVoice.h"
#include "AudioALSAPlaybackHandlerBTSCO.h"
#include "AudioALSAPlaybackHandlerFMTransmitter.h"
#include "AudioALSAPlaybackHandlerHDMI.h"


#include "AudioALSACaptureHandlerBase.h"
#include "AudioALSACaptureHandlerNormal.h"
#include "AudioALSACaptureHandlerVoice.h"
#include "AudioALSACaptureHandlerFMRadio.h"
#include "AudioALSACaptureHandlerBT.h"
#include "AudioALSACaptureHandlerTDM.h"

#include "AudioALSACaptureHandlerANC.h"
#include "AudioALSACaptureHandlerAEC.h"

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSACaptureHandlerSpkFeed.h"
#endif

#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
#include "AudioALSASpeechStreamController.h"
#include "AudioALSACaptureHandlerModemDai.h"
#include "AudioALSAPlaybackHandlerSphDL.h"
#endif

#include "AudioALSASpeechPhoneCallController.h"
#include "AudioALSAFMController.h"

#include "AudioALSAVolumeController.h"

#include "AudioALSAVoiceWakeUpController.h"

#include "AudioALSAHardwareResourceManager.h" // TODO(Harvey): move it

#include "AudioCompFltCustParam.h"
#include "AudioCustParam.h"
#include "SpeechDriverInterface.h"
#include "SpeechDriverFactory.h"
#include "SpeechEnhancementController.h"
#include "SpeechVMRecorder.h"


#define LOG_TAG "AudioALSAStreamManager"

namespace android
{

/*==============================================================================
 *                     Property keys
 *============================================================================*/

const char PROPERTY_KEY_VOICE_WAKE_UP_NEED_ON[PROPERTY_KEY_MAX] = "persist.af.vw_need_on";


/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

AudioALSAStreamManager *AudioALSAStreamManager::mStreamManager = NULL;
AudioALSAStreamManager *AudioALSAStreamManager::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mStreamManager == NULL)
    {
        mStreamManager = new AudioALSAStreamManager();
    }
    ASSERT(mStreamManager != NULL);
    return mStreamManager;
}


/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

AudioALSAStreamManager::AudioALSAStreamManager() :
    mStreamOutIndex(0),
    mStreamInIndex(0),
    mPlaybackHandlerIndex(0),
    mCaptureHandlerIndex(0),
    mSpeechPhoneCallController(AudioALSASpeechPhoneCallController::getInstance()),
    mFMController(AudioALSAFMController::getInstance()),
    mAudioALSAVolumeController(AudioALSAVolumeController::getInstance()),
    mSpeechDriverFactory(SpeechDriverFactory::GetInstance()),
    mAudioSpeechEnhanceInfoInstance(AudioSpeechEnhanceInfo::getInstance()),
    mMicMute(false),
    mAudioMode(AUDIO_MODE_NORMAL),
    mFilterManagerNumber(0),
    mBesLoudnessStatus(false),
    mBesLoudnessControlCallback(NULL),
    mAudioALSAVoiceWakeUpController(AudioALSAVoiceWakeUpController::getInstance()),
    mVoiceWakeUpNeedOn(false),
    mForceDisableVoiceWakeUpForSetMode(false),
    mBypassPostProcessDL(false),
    mHeadsetChange(false),
    mBGSDlGain(0xFF),
    mBGSUlGain(0)
{
    ALOGD("%s()", __FUNCTION__);

    mStreamOutVector.clear();
    mStreamInVector.clear();

    mPlaybackHandlerVector.clear();
    mCaptureHandlerVector.clear();

    mFilterManagerVector.clear();

    // resume voice wake up need on
    char property_value[PROPERTY_VALUE_MAX];
    property_get(PROPERTY_KEY_VOICE_WAKE_UP_NEED_ON, property_value, "0"); //"0": default off
    const bool bVoiceWakeUpNeedOn = (property_value[0] == '0') ? false : true;
    if (bVoiceWakeUpNeedOn == true)
    {
        setVoiceWakeUpNeedOn(true);
    }
#ifdef MTK_BESLOUDNESS_SUPPORT
    unsigned int result = 0 ;
    AUDIO_AUDENH_CONTROL_OPTION_STRUCT audioParam;
    if (GetBesLoudnessControlOptionParamFromNV(&audioParam))
    {
        result = audioParam.u32EnableFlg;
    }
    mBesLoudnessStatus = (result ? true : false);
    ALOGD("AudioALSAStreamManager mBesLoudnessStatus [%d] (From NvRam) \n", mBesLoudnessStatus);
#else
    mBesLoudnessStatus = false;
    ALOGD("AudioALSAStreamManager mBesLoudnessStatus [%d] (Always) \n", mBesLoudnessStatus);
#endif
}


AudioALSAStreamManager::~AudioALSAStreamManager()
{
    ALOGD("%s()", __FUNCTION__);
}


/*==============================================================================
 *                     Implementations
 *============================================================================*/

android_audio_legacy::AudioStreamOut *AudioALSAStreamManager::openOutputStream(
    uint32_t devices,
    int *format,
    uint32_t *channels,
    uint32_t *sampleRate,
    status_t *status,
    uint32_t output_flag)
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);
    AudioAutoTimeoutLock _l(mLock);

    if (format == NULL || channels == NULL || sampleRate == NULL || status == NULL)
    {
        ALOGE("%s(), NULL pointer!! format = %p, channels = %p, sampleRate = %p, status = %p",
              __FUNCTION__, format, channels, sampleRate, status);
        if (status != NULL) { *status = INVALID_OPERATION; }
        return NULL;
    }

    ALOGD("%s(), devices = 0x%x, format = 0x%x, channels = 0x%x, sampleRate = %d, status = 0x%x",
          __FUNCTION__, devices, *format, *channels, *sampleRate, *status);

    // stream out flags
#if 1 // TODO(Harvey): why.........
    mStreamOutIndex = (uint32_t)(*status);
#endif
    //const uint32_t flags = 0; //(uint32_t)(*status);

    // create stream out
    AudioALSAStreamOut *pAudioALSAStreamOut = new AudioALSAStreamOut();
    pAudioALSAStreamOut->set(devices, format, channels, sampleRate, status, output_flag);
    if (*status != NO_ERROR)
    {
        ALOGE("-%s(), set fail, return NULL", __FUNCTION__);
        delete pAudioALSAStreamOut;
        pAudioALSAStreamOut = NULL;
        return NULL;
    }

    // save stream out object in vector
#if 0 // TODO(Harvey): why.........
    pAudioALSAStreamOut->setIdentity(mStreamOutIndex);
    mStreamOutVector.add(mStreamOutIndex, pAudioALSAStreamOut);
    mStreamOutIndex++;
#else
    pAudioALSAStreamOut->setIdentity(mStreamOutIndex);
    mStreamOutVector.add(mStreamOutIndex, pAudioALSAStreamOut);
#endif

    // setup Filter for ACF/HCF/AudEnh/VibSPK // TODO Check return status of pAudioALSAStreamOut->set
    AudioMTKFilterManager *pAudioFilterManagerHandler = new AudioMTKFilterManager(*sampleRate, android_audio_legacy::AudioSystem::popCount(*channels), *format, pAudioALSAStreamOut->bufferSize());
    mFilterManagerVector.add(mStreamOutIndex, pAudioFilterManagerHandler);
    //mFilterManagerNumber++;

    ALOGD("-%s(), out = %p, status = 0x%x, mStreamOutVector.size() = %d",
          __FUNCTION__, pAudioALSAStreamOut, *status, mStreamOutVector.size());


    return pAudioALSAStreamOut;
}

void AudioALSAStreamManager::closeOutputStream(android_audio_legacy::AudioStreamOut *out)
{
    ALOGD("+%s(), out = %p, mStreamOutVector.size() = %d", __FUNCTION__, out, mStreamOutVector.size());
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);
    AudioAutoTimeoutLock _l(mLock);

    if (out == NULL)
    {
        ALOGE("%s(), Cannot close null output stream!! return", __FUNCTION__);
        return;
    }

    AudioALSAStreamOut *pAudioALSAStreamOut = static_cast<AudioALSAStreamOut *>(out);
    ASSERT(pAudioALSAStreamOut != 0);

    uint32_t dFltMngindex = mFilterManagerVector.indexOfKey(pAudioALSAStreamOut->getIdentity());

    if (dFltMngindex < mFilterManagerVector.size())
    {
        AudioMTKFilterManager *pAudioFilterManagerHandler = static_cast<AudioMTKFilterManager *>(mFilterManagerVector[dFltMngindex]);
        ALOGD("%s, remove mFilterManagerVector Success [%d]/[%d] [%d], pAudioFilterManagerHandler=%p",
              __FUNCTION__, dFltMngindex, mFilterManagerVector.size(), pAudioALSAStreamOut->getIdentity(), pAudioFilterManagerHandler);
        ASSERT(pAudioFilterManagerHandler != 0);
        mFilterManagerVector.removeItem(pAudioALSAStreamOut->getIdentity());
        delete pAudioFilterManagerHandler;
    }
    else
    {
        ALOGD("%s, Remove mFilterManagerVector Error [%d]/[%d]", __FUNCTION__, dFltMngindex, mFilterManagerVector.size());
    }

    mStreamOutVector.removeItem(pAudioALSAStreamOut->getIdentity());
    delete pAudioALSAStreamOut;

    ALOGD("-%s(), mStreamOutVector.size() = %d", __FUNCTION__, mStreamOutVector.size());
}


android_audio_legacy::AudioStreamIn *AudioALSAStreamManager::openInputStream(
    uint32_t devices,
    int *format,
    uint32_t *channels,
    uint32_t *sampleRate,
    status_t *status,
    android_audio_legacy::AudioSystem::audio_in_acoustics acoustics,
    uint32_t input_flag)
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);
    AudioAutoTimeoutLock _l(mLock);

    if (format == NULL || channels == NULL || sampleRate == NULL || status == NULL)
    {
        ALOGE("%s(), NULL pointer!! format = %p, channels = %p, sampleRate = %p, status = %p",
              __FUNCTION__, format, channels, sampleRate, status);
        if (status != NULL) { *status = INVALID_OPERATION; }
        return NULL;
    }

    ALOGD("%s(), devices = 0x%x, format = 0x%x, channels = 0x%x, sampleRate = %d, status = %d, acoustics = 0x%x",
          __FUNCTION__, devices, *format, *channels, *sampleRate, *status, acoustics);

#if 1 // TODO(Harvey): why.........
    mStreamInIndex = (uint32_t)(*status);
#endif

    // create stream in
    AudioALSAStreamIn *pAudioALSAStreamIn = new AudioALSAStreamIn();
#ifdef UPLINK_LOW_LATENCY
    pAudioALSAStreamIn->set(devices, format, channels, sampleRate, status, acoustics, input_flag);
#else
    pAudioALSAStreamIn->set(devices, format, channels, sampleRate, status, acoustics);
#endif
    if (*status != NO_ERROR)
    {
        ALOGE("-%s(), set fail, return NULL", __FUNCTION__);
        delete pAudioALSAStreamIn;
        pAudioALSAStreamIn = NULL;
        return NULL;
    }

    // save stream in object in vector
#if 0 // TODO(Harvey): why.........
    pAudioALSAStreamIn->setIdentity(mStreamInIndex);
    mStreamInVector.add(mStreamInIndex, pAudioALSAStreamIn);
    mStreamInIndex++;
#else
    pAudioALSAStreamIn->setIdentity(mStreamInIndex);
    mStreamInVector.add(mStreamInIndex, pAudioALSAStreamIn);
#endif

    // when first stream in is ready to open
    if (mStreamInVector.size() == 1)
    {
        // make sure voice wake up is closed before any capture stream start (only 1st open need to check)
        if (mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == true)
        {
            ALOGD("%s(), force close voice wake up", __FUNCTION__);
            mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(false);
        }
    }

    ALOGD("-%s(), in = %p, status = 0x%x, mStreamInVector.size() = %d",
          __FUNCTION__, pAudioALSAStreamIn, *status, mStreamInVector.size());
    return pAudioALSAStreamIn;
}


void AudioALSAStreamManager::closeInputStream(android_audio_legacy::AudioStreamIn *in)
{
    ALOGD("+%s(), in = %p", __FUNCTION__, in);
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);
    AudioAutoTimeoutLock _l(mLock);

    if (in == NULL)
    {
        ALOGE("%s(), Cannot close null input stream!! return", __FUNCTION__);
        return;
    }

    AudioALSAStreamIn *pAudioALSAStreamIn = static_cast<AudioALSAStreamIn *>(in);
    ASSERT(pAudioALSAStreamIn != 0);

    mStreamInVector.removeItem(pAudioALSAStreamIn->getIdentity());
    delete pAudioALSAStreamIn;


    // make sure voice wake up is resume when all capture stream stop if need
    if (mVoiceWakeUpNeedOn == true &&
        mStreamInVector.size() == 0 &&
        mForceDisableVoiceWakeUpForSetMode == false)
    {
        ALOGD("%s(), resume voice wake up", __FUNCTION__);
        ASSERT(mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == false); // TODO(Harvey): double check, remove it later
        mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(true);
    }

    ALOGD("-%s(), mStreamInVector.size() = %d", __FUNCTION__, mStreamInVector.size());
}

AudioALSAPlaybackHandlerBase *AudioALSAStreamManager::createPlaybackHandler(
    stream_attribute_t *stream_attribute_source)
{
    ALOGD("+%s(), mAudioMode = %d, output_devices = 0x%x", __FUNCTION__, mAudioMode, stream_attribute_source->output_devices);
    AudioAutoTimeoutLock _l(mLock);

    // Init input stream attribute here
    stream_attribute_source->audio_mode = mAudioMode; // set mode to stream attribute for mic gain setting

    // just use what stream out is ask to use
    //stream_attribute_source->sample_rate = AudioALSASampleRateController::getInstance()->getPrimaryStreamOutSampleRate();

    //for DMNR tuning
    stream_attribute_source->BesRecord_Info.besrecord_dmnr_tuningEnable = mAudioSpeechEnhanceInfoInstance->IsAPDMNRTuningEnable();
    stream_attribute_source->bBypassPostProcessDL = mBypassPostProcessDL;
    stream_attribute_source->u8BGSDlGain = mBGSDlGain;
    stream_attribute_source->u8BGSUlGain = mBGSUlGain;


    // create
    AudioALSAPlaybackHandlerBase *pPlaybackHandler = NULL;
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
    if (stream_attribute_source->bModemDai_Input == true)
    {
        stream_attribute_source->sample_rate = 16000;
        pPlaybackHandler = new AudioALSAPlaybackHandlerSphDL(stream_attribute_source);
        uint32_t dFltMngindex = mFilterManagerVector.indexOfKey(stream_attribute_source->mStreamOutIndex);
        ALOGD("%s() ApplyFilter [%d]/[%d] Device [0x%x]", __FUNCTION__, dFltMngindex, mFilterManagerVector.size(), stream_attribute_source->output_devices);

        if (dFltMngindex < mFilterManagerVector.size())
        {
            pPlaybackHandler->setFilterMng(static_cast<AudioMTKFilterManager *>(mFilterManagerVector[dFltMngindex]));
            mFilterManagerVector[dFltMngindex]->setDevice(stream_attribute_source->output_devices);
        }
    }
    else
#endif
        if (isModeInPhoneCall() == true)
        {
            pPlaybackHandler = new AudioALSAPlaybackHandlerVoice(stream_attribute_source);
        }
        else
        {
            switch (stream_attribute_source->output_devices)
            {
                case AUDIO_DEVICE_OUT_EARPIECE:
                case AUDIO_DEVICE_OUT_SPEAKER:
                case AUDIO_DEVICE_OUT_WIRED_HEADSET:
                case AUDIO_DEVICE_OUT_WIRED_HEADPHONE:
                {
#ifdef DOWNLINK_LOW_LATENCY
                    if(AUDIO_OUTPUT_FLAG_FAST & stream_attribute_source->mAudioOutputFlags)
                    {
                        pPlaybackHandler = new AudioALSAPlaybackHandlerFast(stream_attribute_source);
                    }else
#endif
                        pPlaybackHandler = new AudioALSAPlaybackHandlerNormal(stream_attribute_source);
                    break;
                }
                case AUDIO_DEVICE_OUT_BLUETOOTH_SCO:
                case AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET:
                case AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT:
                {
                    pPlaybackHandler = new AudioALSAPlaybackHandlerBTSCO(stream_attribute_source);
                    break;
                }
                case AUDIO_DEVICE_OUT_AUX_DIGITAL:
                {
                    pPlaybackHandler = new AudioALSAPlaybackHandlerHDMI(stream_attribute_source);
                    break;
                }
                case AUDIO_DEVICE_OUT_FM:
                {
                    pPlaybackHandler = new AudioALSAPlaybackHandlerFMTransmitter(stream_attribute_source);
                    break;
                }
                default:
                {
                    ALOGE("%s(), No implement for such output_devices(0x%x)", __FUNCTION__, stream_attribute_source->output_devices);

#ifdef DOWNLINK_LOW_LATENCY
                    if(AUDIO_OUTPUT_FLAG_FAST & stream_attribute_source->mAudioOutputFlags)
                    {
                        pPlaybackHandler = new AudioALSAPlaybackHandlerFast(stream_attribute_source);
                    }
                    else
#endif
                        pPlaybackHandler = new AudioALSAPlaybackHandlerNormal(stream_attribute_source);
                    break;
                }
            }

            uint32_t dFltMngindex = mFilterManagerVector.indexOfKey(stream_attribute_source->mStreamOutIndex);
            ALOGD("%s() ApplyFilter [%d]/[%d] Device [0x%x]", __FUNCTION__, dFltMngindex, mFilterManagerVector.size(), stream_attribute_source->output_devices);

            if (dFltMngindex < mFilterManagerVector.size())
            {
                pPlaybackHandler->setFilterMng(static_cast<AudioMTKFilterManager *>(mFilterManagerVector[dFltMngindex]));
                mFilterManagerVector[dFltMngindex]->setDevice(stream_attribute_source->output_devices);
            }
        }

    // save playback handler object in vector
    ASSERT(pPlaybackHandler != NULL);
    pPlaybackHandler->setIdentity(mPlaybackHandlerIndex);

    mPlaybackHandlerVectorLock.lock();
    mPlaybackHandlerVector.add(mPlaybackHandlerIndex, pPlaybackHandler);
    mPlaybackHandlerVectorLock.unlock();

    mPlaybackHandlerIndex++;

    ALOGD("-%s(), mPlaybackHandlerVector.size() = %d", __FUNCTION__, mPlaybackHandlerVector.size());
    return pPlaybackHandler;
}


status_t AudioALSAStreamManager::destroyPlaybackHandler(AudioALSAPlaybackHandlerBase *pPlaybackHandler)
{
    ALOGD("+%s(), mode = %d, pPlaybackHandler = %p", __FUNCTION__, mAudioMode, pPlaybackHandler);
    //AudioAutoTimeoutLock _l(mLock); // TODO(Harvey): setparam -> routing -> close -> destroy deadlock

    status_t status = NO_ERROR;

    mPlaybackHandlerVectorLock.lock();
    mPlaybackHandlerVector.removeItem(pPlaybackHandler->getIdentity());
    mPlaybackHandlerVectorLock.unlock();

    delete pPlaybackHandler;

    ALOGD("-%s(), mPlaybackHandlerVector.size() = %d", __FUNCTION__, mPlaybackHandlerVector.size());
    return status;
}


AudioALSACaptureHandlerBase *AudioALSAStreamManager::createCaptureHandler(
    stream_attribute_t *stream_attribute_target)
{
    ALOGD("+%s(), mAudioMode = %d, input_source = %d, input_device = 0x%x",
          __FUNCTION__, mAudioMode, stream_attribute_target->input_source, stream_attribute_target->input_device);
    AudioAutoTimeoutLock _l(mLock);

    // use primary stream out device
    const audio_devices_t current_output_devices = (mStreamOutVector.size() > 0)
                                                   ? mStreamOutVector[0]->getStreamAttribute()->output_devices
                                                   : AUDIO_DEVICE_NONE;


    // Init input stream attribute here
    stream_attribute_target->audio_mode = mAudioMode; // set mode to stream attribute for mic gain setting
    stream_attribute_target->output_devices = current_output_devices; // set output devices to stream attribute for mic gain setting and BesRecord parameter

    // BesRecordInfo
    stream_attribute_target->BesRecord_Info.besrecord_enable = false; // default set besrecord off


    // create
    AudioALSACaptureHandlerBase *pCaptureHandler = NULL;
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    if (stream_attribute_target->input_device == AUDIO_DEVICE_IN_SPK_FEED)
    {
        pCaptureHandler = new AudioALSACaptureHandlerSpkFeed(stream_attribute_target);
    }
    else
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
        if ((stream_attribute_target->bModemDai_Input == true))
        {
            pCaptureHandler = new AudioALSACaptureHandlerModemDai(stream_attribute_target);
        }
        else
#endif
#endif
            if (stream_attribute_target->input_source == AUDIO_SOURCE_FM_TUNER)
            {
                pCaptureHandler = new AudioALSACaptureHandlerFMRadio(stream_attribute_target);
            }
            else if (stream_attribute_target->input_source == AUDIO_SOURCE_ANC)
            {
                pCaptureHandler = new AudioALSACaptureHandlerANC(stream_attribute_target);
            }
            else if (isModeInPhoneCall() == true)
            {
                pCaptureHandler = new AudioALSACaptureHandlerVoice(stream_attribute_target);
            }
            else if ((isModeInVoipCall() == true) || (stream_attribute_target->NativePreprocess_Info.PreProcessEffect_AECOn == true)
                     || (stream_attribute_target->input_source == AUDIO_SOURCE_VOICE_COMMUNICATION)
                     || (stream_attribute_target->input_source == AUDIO_SOURCE_CUSTOMIZATION1) //MagiASR enable AEC
                     || (stream_attribute_target->input_source == AUDIO_SOURCE_CUSTOMIZATION2)) //Normal REC with AEC
            {
#if 0   //def UPLINK_LOW_LATENCY
                if (stream_attribute_target->mAudioInputFlags & AUDIO_INPUT_FLAG_FAST)
                    stream_attribute_target->BesRecord_Info.besrecord_enable = false;
                else
                    stream_attribute_target->BesRecord_Info.besrecord_enable = EnableBesRecord();
#else
                    stream_attribute_target->BesRecord_Info.besrecord_enable = EnableBesRecord();
#endif
                stream_attribute_target->micmute = mMicMute;

                if (isModeInVoipCall() == true || (stream_attribute_target->input_source == AUDIO_SOURCE_VOICE_COMMUNICATION))
                {
                    stream_attribute_target->BesRecord_Info.besrecord_voip_enable = true;

                    if (current_output_devices == AUDIO_DEVICE_OUT_SPEAKER)
                    {
                        if (stream_attribute_target->input_device == AUDIO_DEVICE_IN_BUILTIN_MIC)
                        {
                            if (USE_REFMIC_IN_LOUDSPK == 1)
                            {
                                ALOGD("%s(), routing changed!! input_device: 0x%x => 0x%x",
                                      __FUNCTION__, stream_attribute_target->input_device, AUDIO_DEVICE_IN_BACK_MIC);
                                stream_attribute_target->input_device = AUDIO_DEVICE_IN_BACK_MIC;
                            }
                        }
                    }
                }

                switch (stream_attribute_target->input_device)
                {
                    case AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET:
                    {
#if 0   //not enable BT AEC
                        ALOGD("%s(), BT still use nonAEC handle for temp", __FUNCTION__);
                        stream_attribute_target->BesRecord_Info.besrecord_voip_enable = false;
                        pCaptureHandler = new AudioALSACaptureHandlerBT(stream_attribute_target);
#else   //enable BT AEC
                        ALOGD("%s(), BT use AEC handle", __FUNCTION__);
                        pCaptureHandler = new AudioALSACaptureHandlerAEC(stream_attribute_target);
#endif
                        break;
                    }
                    default:
                    {
                        pCaptureHandler = new AudioALSACaptureHandlerAEC(stream_attribute_target);
                        break;
                    }
                }
            }
            else
            {
                //enable BesRecord if not these input sources
                if ((stream_attribute_target->input_source != AUDIO_SOURCE_VOICE_UNLOCK) &&
                    (stream_attribute_target->input_source != AUDIO_SOURCE_FM_TUNER) && // TODO(Harvey, Yu-Hung): never go through here?
                    (stream_attribute_target->input_source != AUDIO_SOURCE_MATV) &&
                    (stream_attribute_target->input_source != AUDIO_SOURCE_ANC))
                {
#if 0   //def UPLINK_LOW_LATENCY
                    if (stream_attribute_target->mAudioInputFlags & AUDIO_INPUT_FLAG_FAST)
                        stream_attribute_target->BesRecord_Info.besrecord_enable = false;
                    else
                        stream_attribute_target->BesRecord_Info.besrecord_enable = EnableBesRecord();
#else
                        stream_attribute_target->BesRecord_Info.besrecord_enable = EnableBesRecord();
#endif
                }

                switch (stream_attribute_target->input_device)
                {
                    case AUDIO_DEVICE_IN_BUILTIN_MIC:
                    case AUDIO_DEVICE_IN_BACK_MIC:
                    case AUDIO_DEVICE_IN_WIRED_HEADSET:
                    {
                        pCaptureHandler = new AudioALSACaptureHandlerNormal(stream_attribute_target);
                        break;
                    }
                    case AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET:
                    {
                        pCaptureHandler = new AudioALSACaptureHandlerBT(stream_attribute_target);
                        break;
                    }
                    case AUDIO_DEVICE_IN_TDM:
                    {
                        pCaptureHandler = new AudioALSACaptureHandlerTDM(stream_attribute_target);
                        break;
                    }
                    default:
                    {
                        ALOGE("%s(), No implement for such input_device(0x%x)", __FUNCTION__, stream_attribute_target->input_device);
                        pCaptureHandler = new AudioALSACaptureHandlerNormal(stream_attribute_target);
                        break;
                    }
                }
            }

    // save capture handler object in vector
    ASSERT(pCaptureHandler != NULL);
    pCaptureHandler->setIdentity(mCaptureHandlerIndex);
    mCaptureHandlerVector.add(mCaptureHandlerIndex, pCaptureHandler);
    mCaptureHandlerIndex++;

    ALOGD("-%s(), mCaptureHandlerVector.size() = %d", __FUNCTION__, mCaptureHandlerVector.size());
    return pCaptureHandler;
}


status_t AudioALSAStreamManager::destroyCaptureHandler(AudioALSACaptureHandlerBase *pCaptureHandler)
{
    ALOGD("+%s(), mode = %d, pCaptureHandler = %p", __FUNCTION__, mAudioMode, pCaptureHandler);
    //AudioAutoTimeoutLock _l(mLock); // TODO(Harvey): setparam -> routing -> close -> destroy deadlock

    status_t status = NO_ERROR;

    mCaptureHandlerVector.removeItem(pCaptureHandler->getIdentity());
    delete pCaptureHandler;

    ALOGD("-%s(), mCaptureHandlerVector.size() = %d", __FUNCTION__, mCaptureHandlerVector.size());
    return status;
}


status_t AudioALSAStreamManager::setVoiceVolume(float volume)
{
    ALOGD("%s(), volume = %f", __FUNCTION__, volume);

    if (volume < 0.0 || volume > 1.0)
    {
        ALOGE("-%s(), strange volume level %f, something wrong!!", __FUNCTION__, volume);
        return BAD_VALUE;
    }

    AudioAutoTimeoutLock _l(mLock);

    if (mAudioALSAVolumeController)
    {
        // use primary stream out device
        const audio_devices_t current_output_devices = (mStreamOutVector.size() > 0)
                                                       ? mStreamOutVector[0]->getStreamAttribute()->output_devices
                                                       : AUDIO_DEVICE_NONE;
        mAudioALSAVolumeController->setVoiceVolume(volume, mAudioMode , current_output_devices);
    }

    return NO_ERROR;
}

float AudioALSAStreamManager::getMasterVolume(void)
{
    return mAudioALSAVolumeController->getMasterVolume();
}

status_t AudioALSAStreamManager::setMasterVolume(float volume)
{
    ALOGD("%s(), volume = %f", __FUNCTION__, volume);

    if (volume < 0.0 || volume > 1.0)
    {
        ALOGE("-%s(), strange volume level %f, something wrong!!", __FUNCTION__, volume);
        return BAD_VALUE;
    }

    AudioAutoTimeoutLock _l(mLock);
    if (mAudioALSAVolumeController)
    {
        // use primary stream out device
        const audio_devices_t current_output_devices = (mStreamOutVector.size() > 0)
                                                       ? mStreamOutVector[0]->getStreamAttribute()->output_devices
                                                       : AUDIO_DEVICE_NONE;
        mAudioALSAVolumeController->setMasterVolume(volume, mAudioMode , current_output_devices);
    }

    return NO_ERROR;
}


status_t AudioALSAStreamManager::setFmVolume(float volume)
{
    ALOGV("+%s(), volume = %f", __FUNCTION__, volume);

    if (volume < 0.0 || volume > 1.0)
    {
        ALOGE("-%s(), strange volume level %f, something wrong!!", __FUNCTION__, volume);
        return BAD_VALUE;
    }

    AudioAutoTimeoutLock _l(mLock);
    mFMController->setFmVolume(volume);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setMicMute(bool state)
{
    ALOGD("%s(), mMicMute: %d => %d", __FUNCTION__, mMicMute, state);
    AudioAutoTimeoutLock _l(mLock);
    if (isModeInPhoneCall() == true)
    {
        AudioALSASpeechPhoneCallController::getInstance()->setMicMute(state);
    }
    else
    {
        SetInputMute(state);
    }
    mMicMute = state;
    return NO_ERROR;
}


bool AudioALSAStreamManager::getMicMute()
{
    ALOGD("%s(), mMicMute = %d", __FUNCTION__, mMicMute);
    //AudioAutoTimeoutLock _l(mLock);

    return mMicMute;
}

void AudioALSAStreamManager::SetInputMute(bool bEnable)
{
    ALOGD("+%s(), %d", __FUNCTION__, bEnable);
    if (mStreamInVector.size() > 0)
    {
        for (size_t i = 0; i < mStreamInVector.size(); i++) // TODO(Harvey): Mic+FM !?
        {
            mStreamInVector[i]->SetInputMute(bEnable);
        }
    }
    ALOGD("-%s(), %d", __FUNCTION__);
}

status_t AudioALSAStreamManager::setVtNeedOn(const bool vt_on)
{
    ALOGD("%s(), setVtNeedOn: %d", __FUNCTION__, vt_on);
    AudioALSASpeechPhoneCallController::getInstance()->setVtNeedOn(vt_on);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setBGSDlMute(const bool mute_on)
{
    if (mute_on)
    {
        mBGSDlGain = 0;
    }
    else
    {
        mBGSDlGain = 0xFF;
    }
    ALOGD("%s(), mute_on: %d, mBGSDlGain=0x%x", __FUNCTION__, mute_on, mBGSDlGain);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setBGSUlMute(const bool mute_on)
{
    if (mute_on)
    {
        mBGSUlGain = 0;
    }
    else
    {
        mBGSUlGain = 0xFF;
    }
    ALOGD("%s(), mute_on: %d, mBGSUlGain=0x%x", __FUNCTION__, mute_on, mBGSUlGain);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setMode(audio_mode_t new_mode)
{
    ALOGD("+%s(), mAudioMode: %d => %d", __FUNCTION__, mAudioMode, new_mode);
    bool isNeedResumeAllStreams = false;
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);

    // check value
    if ((new_mode < AUDIO_MODE_NORMAL) || (new_mode > AUDIO_MODE_MAX))
    {
        return BAD_VALUE;
    }

    // TODO(Harvey): modem 1 / modem 2 check

    if (new_mode == mAudioMode)
    {
        ALOGW("-%s(), mAudioMode: %d == %d, return", __FUNCTION__, mAudioMode, new_mode);
        return NO_ERROR;
    }

    // make sure voice wake up is closed before leaving normal mode
    if (new_mode != AUDIO_MODE_NORMAL)
    {
        mForceDisableVoiceWakeUpForSetMode = true;
        if (mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == true)
        {
            ALOGD("%s(), force close voice wake up", __FUNCTION__);
            mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(false);
        }
    }

    // suspend and standby if needed
    if (isModeInPhoneCall(new_mode) == true || isModeInPhoneCall(mAudioMode) == true ||
        isModeInVoipCall(new_mode)  == true || isModeInVoipCall(mAudioMode) == true)
    {
        setAllStreamsSuspend(true, true);
        standbyAllStreams(true);
    }

    // close FM when mode swiching
    if (mFMController->getFmEnable() == true)
    {
        setFmEnable(false);
    }

    // TODO(Harvey): // close mATV when mode swiching

    {
        AudioAutoTimeoutLock _l(mLock);

        // use primary stream out device // TODO(Harvey): add a function? get from hardware?
        const audio_devices_t current_output_devices = (mStreamOutVector.size() > 0)
                                                       ? mStreamOutVector[0]->getStreamAttribute()->output_devices
                                                       : AUDIO_DEVICE_NONE;


        // close previous call if needed
        if (isModeInPhoneCall(mAudioMode) == true)
        {
            mSpeechPhoneCallController->close();
        }

        // open next call if needed
        if (isModeInPhoneCall(new_mode) == true)
        {
            mSpeechPhoneCallController->open(
                new_mode,
                current_output_devices,
                mSpeechPhoneCallController->getInputDeviceForPhoneCall(current_output_devices));
        }


        // resume if needed
        if (isModeInPhoneCall(new_mode) == true || isModeInPhoneCall(mAudioMode) == true ||
            isModeInVoipCall(new_mode)  == true || isModeInVoipCall(mAudioMode) == true)
        {
            isNeedResumeAllStreams = true;
        }

        mAudioMode = new_mode;

        if (isModeInPhoneCall() == true)
        {
            mAudioALSAVolumeController->setVoiceVolume(mAudioALSAVolumeController->getVoiceVolume(), mAudioMode , current_output_devices);
        }
        else
        {
            mAudioALSAVolumeController->setMasterVolume(mAudioALSAVolumeController->getMasterVolume(), mAudioMode , current_output_devices);
        }

        // make sure voice wake up is resume when go back to normal mode
        if (mAudioMode == AUDIO_MODE_NORMAL)
        {
            mForceDisableVoiceWakeUpForSetMode = false;
            if (mVoiceWakeUpNeedOn == true &&
                mStreamInVector.size() == 0)
            {
                ALOGD("%s(), resume voice wake up", __FUNCTION__);
                ASSERT(mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == false); // TODO(Harvey): double check, remove it later
                mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(true);
            }
        }
    }

    if (isNeedResumeAllStreams == true)
    {
        setAllStreamsSuspend(false, true);
    }
    ALOGD("-%s(), mAudioMode = %d", __FUNCTION__, mAudioMode);
    return NO_ERROR;
}


status_t AudioALSAStreamManager::routingOutputDevice(AudioALSAStreamOut *pAudioALSAStreamOut, const audio_devices_t current_output_devices, audio_devices_t output_devices)
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mLock);

    status_t status = NO_ERROR;
    audio_devices_t streamOutDevice = pAudioALSAStreamOut->getStreamAttribute()->output_devices;

    ALOGD("%s(), output_devices: 0x%x => 0x%x", __FUNCTION__, streamOutDevice, output_devices);

    // TODO(Harvey, Hochi): Sometimes AUDIO_DEVICE_NONE might need to transferred to other device?

    // set original routing device to TTY
    mSpeechPhoneCallController->setRoutingForTty((audio_devices_t)output_devices);

    // update the output device info for voice wakeup (even when "routing=0")
    mAudioALSAVoiceWakeUpController->updateDeviceInfoForVoiceWakeUp();

    // update if headset change
    mHeadsetChange = CheckHeadsetChange(streamOutDevice, output_devices);
    if ((mHeadsetChange == true) && (mFMController->getFmEnable() ==  false))
    {
        AudioALSAHardwareResourceManager::getInstance()->setHeadPhoneChange(mHeadsetChange);
    }
    ALOGD("mHeadsetChange = %d", mHeadsetChange);

    // When FM + (WFD, A2DP, SCO(44.1K -> 8/16K), ...), Policy will routing to AUDIO_DEVICE_NONE
    // Hence, use other device like AUDIO_DEVICE_OUT_REMOTE_SUBMIX instead to achieve FM routing.
    if (output_devices == AUDIO_DEVICE_NONE && mFMController->getFmEnable() == true)
    {
        ALOGD("%s(), Replace AUDIO_DEVICE_NONE with AUDIO_DEVICE_OUT_REMOTE_SUBMIX for AP-path FM routing", __FUNCTION__);
        output_devices = AUDIO_DEVICE_OUT_REMOTE_SUBMIX;
    }


    if (output_devices == AUDIO_DEVICE_NONE)
    {
        ALOGW("-%s(), output_devices == AUDIO_DEVICE_NONE(0x%x), return", __FUNCTION__, AUDIO_DEVICE_NONE);
        return NO_ERROR;
    }
    else if (output_devices == streamOutDevice)
    {
        ALOGW("-%s(), output_devices == current_output_devices(0x%x), return", __FUNCTION__, streamOutDevice);
        return NO_ERROR;
    }


    // do routing
    if (isModeInPhoneCall())
    {
        mSpeechPhoneCallController->routing(
            output_devices,
            mSpeechPhoneCallController->getInputDeviceForPhoneCall(output_devices));
    }

    Vector<AudioALSAStreamOut *> streamOutToRoute;
    AudioALSAHardwareResourceManager *hwResMng = AudioALSAHardwareResourceManager::getInstance();
    bool toSharedOut = hwResMng->isSharedOutDevice(output_devices);

    for (size_t i = 0; i < mStreamOutVector.size(); i++)
    {
        audio_devices_t curOutDevice = mStreamOutVector[i]->getStreamAttribute()->output_devices;
        bool curSharedOut = hwResMng->isSharedOutDevice(curOutDevice);

        // check if need routing
        if (curOutDevice != output_devices &&
            (pAudioALSAStreamOut == mStreamOutVector[i] ||   // route ourself
            (toSharedOut && curSharedOut))) // route shared output device streamout
        {
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
            if (pAudioALSAStreamOut->getStreamAttribute()->bModemDai_Input != true)
#endif
            {
                // suspend streamout
                mStreamOutVector[i]->setSuspend(true);
                streamOutToRoute.add(mStreamOutVector[i]);
            }
        }
    }

    for (size_t i = 0; i < streamOutToRoute.size(); i++)
    {
        // stream out routing
        status = streamOutToRoute[i]->routing(output_devices);
        ASSERT(status == NO_ERROR);
    }

    for (size_t i = 0; i < streamOutToRoute.size(); i++)
    {
        // resume streamout
        streamOutToRoute[i]->setSuspend(false);
    }

    if (!isModeInPhoneCall())
    {
        if (mStreamInVector.size() > 0)
        {
            // update the output device info for input stream (ex:for BesRecord parameters update or mic device change)
            ALOGD("%s(), mStreamInVector.size() = %d", __FUNCTION__, mStreamInVector.size());
            for (size_t i = 0; i < mStreamInVector.size(); i++)
            {
                status = mStreamInVector[i]->updateOutputDeviceInfoForInputStream(output_devices);
                ASSERT(status == NO_ERROR);
            }
        }

        if (mFMController->getFmEnable() == true)
        {
            if(android_audio_legacy::AudioSystem::popCount(streamOutDevice)!= android_audio_legacy::AudioSystem::popCount(output_devices))
                mFMController->routing(streamOutDevice, output_devices);//switch between SPK+HP and HP (ringtone)
            else
                mFMController->setFmEnable(false,output_devices,false,false,true);
        }
    }

    // volume control
    if (isModeInPhoneCall() == true)
    {
        mAudioALSAVolumeController->setVoiceVolume(mAudioALSAVolumeController->getVoiceVolume(), mAudioMode , output_devices);
    }
    else
    {
        mAudioALSAVolumeController->setMasterVolume(mAudioALSAVolumeController->getMasterVolume(), mAudioMode , output_devices);
    }

    ALOGD("-%s(), output_devices = 0x%x", __FUNCTION__, output_devices);
    return status;
}


status_t AudioALSAStreamManager::routingInputDevice(const audio_devices_t current_input_device, audio_devices_t input_device)
{
    ALOGD("+%s(), input_device: 0x%x => 0x%x", __FUNCTION__, current_input_device, input_device);
    AudioAutoTimeoutLock _l(mLock);

    status_t status = NO_ERROR;

    if (input_device == AUDIO_DEVICE_NONE)
    {
        ALOGW("-%s(), input_device == AUDIO_DEVICE_NONE(0x%x), return", __FUNCTION__, AUDIO_DEVICE_NONE);
        return NO_ERROR;
    }
    else if (input_device == current_input_device)
    {
        ALOGW("-%s(), input_device == current_input_device(0x%x), return", __FUNCTION__, current_input_device);
        return NO_ERROR;
    }


    if (isModeInPhoneCall() == true)
    {
        ALOGW("-%s(), not route during phone call, return", __FUNCTION__);
        return INVALID_OPERATION;
    }
    else if (mStreamInVector.size() > 0)
    {
        for (size_t i = 0; i < mStreamInVector.size(); i++)
        {
            if (mStreamInVector[i]->getStreamAttribute()->input_device == current_input_device
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
                && mStreamInVector[i]->getStreamAttribute()->bFixedRouting == false
#endif
               ) // TODO(Harvey): or add group?
            {
                status = mStreamInVector[i]->routing(input_device);
                ASSERT(status == NO_ERROR);
            }
        }
    }

    return status;
}

// check if headset has changed
bool AudioALSAStreamManager::CheckHeadsetChange(const audio_devices_t current_output_devices, audio_devices_t output_device)
{
    ALOGD("+%s(), current_output_devices = %d output_device = %d ", __FUNCTION__, current_output_devices, output_device);
    if (current_output_devices == output_device)
    {
        return false;
    }
    if (current_output_devices == AUDIO_DEVICE_NONE || output_device == AUDIO_DEVICE_NONE)
    {
        return true;
    }
    if (current_output_devices == AUDIO_DEVICE_OUT_WIRED_HEADSET || current_output_devices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE
        || output_device == AUDIO_DEVICE_OUT_WIRED_HEADSET || output_device == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
    {
        return true;
    }
    return false;
}

status_t AudioALSAStreamManager::setFmEnable(const bool enable, bool bForceControl, bool bForce2DirectConn) // TODO(Harvey)
{
    ALOGD("+%s(), enable = %d", __FUNCTION__, enable);
    AudioAutoTimeoutLock _l(mLock);

    // Check Audio Mode is Normal
    if (mAudioMode != AUDIO_MODE_NORMAL)
    {
        ALOGW("-%s(), mAudioMode(%d) is not AUDIO_MODE_NORMAL(%d), return.", __FUNCTION__, mAudioMode, AUDIO_MODE_NORMAL);
        return INVALID_OPERATION;
    }

    // use primary stream out device // TODO(Harvey): add a function? get from hardware?
    const audio_devices_t current_output_devices = (mStreamOutVector.size() > 0)
                                                   ? mStreamOutVector[0]->getStreamAttribute()->output_devices
                                                   : AUDIO_DEVICE_NONE;

    mFMController->setFmEnable(enable, current_output_devices, bForceControl, bForce2DirectConn);

    ALOGD("-%s(), enable = %d", __FUNCTION__, enable);
    return NO_ERROR;
}


bool AudioALSAStreamManager::getFmEnable()
{
    AudioAutoTimeoutLock _l(mLock);
    return mFMController->getFmEnable();
}

status_t AudioALSAStreamManager::setAllOutputStreamsSuspend(const bool suspend_on, const bool setModeRequest)
{
    AudioAutoTimeoutLock _l(mLock);

    for (size_t i = 0; i < mStreamOutVector.size(); i++)
    {
        ASSERT(mStreamOutVector[i]->setSuspend(suspend_on) == NO_ERROR);
    }

    return NO_ERROR;
}


status_t AudioALSAStreamManager::setAllInputStreamsSuspend(const bool suspend_on, const bool setModeRequest)
{
    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    AudioALSAStreamIn *pAudioALSAStreamIn = NULL;

    for (size_t i = 0; i < mStreamInVector.size(); i++)
    {
        pAudioALSAStreamIn = mStreamInVector[i];
        if (setModeRequest == true)
        {
            if (pAudioALSAStreamIn->isSupportConcurrencyInCall())
            {
                ALOGD("%s(), mStreamInVector[%d] support concurrency!!", __FUNCTION__, i);
                continue;
            }
        }
        status = pAudioALSAStreamIn->setSuspend(suspend_on);
        if (status != NO_ERROR)
        {
            ALOGE("%s(), mStreamInVector[%d] setSuspend() fail!!", __FUNCTION__, i);
        }
    }

    return status;
}


status_t AudioALSAStreamManager::setAllStreamsSuspend(const bool suspend_on, const bool setModeRequest)
{
    ALOGD("%s(), suspend_on = %d", __FUNCTION__, suspend_on);

    status_t status = NO_ERROR;

    status = setAllOutputStreamsSuspend(suspend_on, setModeRequest);
    status = setAllInputStreamsSuspend(suspend_on, setModeRequest);

    return status;
}


status_t AudioALSAStreamManager::standbyAllOutputStreams(const bool setModeRequest)
{
    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    AudioALSAStreamOut *pAudioALSAStreamOut = NULL;

    for (size_t i = 0; i < mStreamOutVector.size(); i++)
    {
        pAudioALSAStreamOut = mStreamOutVector[i];
        status = pAudioALSAStreamOut->standby();
        if (status != NO_ERROR)
        {
            ALOGE("%s(), mStreamOutVector[%d] standby() fail!!", __FUNCTION__, i);
        }
    }

    return status;
}


status_t AudioALSAStreamManager::standbyAllInputStreams(const bool setModeRequest)
{
    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    AudioALSAStreamIn *pAudioALSAStreamIn = NULL;

    for (size_t i = 0; i < mStreamInVector.size(); i++)
    {
        pAudioALSAStreamIn = mStreamInVector[i];
        if (setModeRequest == true)
        {
            if (pAudioALSAStreamIn->isSupportConcurrencyInCall())
            {
                ALOGD("%s(), mStreamInVector[%d] support concurrency!!", __FUNCTION__, i);
                continue;
            }
        }
        status = pAudioALSAStreamIn->standby();
        if (status != NO_ERROR)
        {
            ALOGE("%s(), mStreamInVector[%d] standby() fail!!", __FUNCTION__, i);
        }
    }

    return status;
}


status_t AudioALSAStreamManager::standbyAllStreams(const bool setModeRequest)
{
    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    status = standbyAllOutputStreams(setModeRequest);
    status = standbyAllInputStreams(setModeRequest);

    return status;
}


size_t AudioALSAStreamManager::getInputBufferSize(uint32_t sampleRate, audio_format_t format, uint32_t channelCount)
{
    size_t wordSize = 0;
    switch (format)
    {
        case AUDIO_FORMAT_PCM_8_BIT:
        {
            wordSize = sizeof(int8_t);
            break;
        }
        case AUDIO_FORMAT_PCM_16_BIT:
        {
            wordSize = sizeof(int16_t);
            break;
        }
        case AUDIO_FORMAT_PCM_8_24_BIT:
        case AUDIO_FORMAT_PCM_32_BIT:
        {
            wordSize = sizeof(int32_t);
            break;
        }
        default:
        {
            ALOGW("%s(), wrong format(0x%x), default use wordSize = %d", __FUNCTION__, format, sizeof(int16_t));
            wordSize = sizeof(int16_t);
            break;
        }
    }

    size_t bufferSize = ((sampleRate * channelCount * wordSize) * 20) / 1000; // TODO (Harvey): why 20 ms here?

    ALOGD("%s(), sampleRate = %u, format = 0x%x, channelCount = %d, bufferSize = %d",
          __FUNCTION__, sampleRate, format, channelCount, bufferSize);
    return bufferSize;
}


// set musicplus to streamout
status_t AudioALSAStreamManager::SetMusicPlusStatus(bool bEnable)
{

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        pTempFilter->setParamFixed(bEnable ? true : false);
    }

    return NO_ERROR;
}

bool AudioALSAStreamManager::GetMusicPlusStatus()
{

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        bool musicplus_status = pTempFilter->isParamFixed();
        if (musicplus_status)
        {
            return true;
        }
    }

    return false;
}

status_t AudioALSAStreamManager::SetBesLoudnessStatus(bool bEnable)
{
#ifdef MTK_BESLOUDNESS_SUPPORT
    ALOGD("mBesLoudnessStatus() flag %d", bEnable);
    mBesLoudnessStatus = bEnable;
    AUDIO_AUDENH_CONTROL_OPTION_STRUCT audioParam;
    audioParam.u32EnableFlg = bEnable ? 1 : 0;
    SetBesLoudnessControlOptionParamToNV(&audioParam);
    if (mBesLoudnessControlCallback != NULL)
    {
        mBesLoudnessControlCallback((void *)mBesLoudnessStatus);
    }
#else
    ALOGD("Unsupport set mBesLoudnessStatus()");
#endif
    return NO_ERROR;
}

bool AudioALSAStreamManager::GetBesLoudnessStatus()
{
    return mBesLoudnessStatus;
}

status_t AudioALSAStreamManager::SetBesLoudnessControlCallback(const BESLOUDNESS_CONTROL_CALLBACK_STRUCT *callback_data)
{
    if (callback_data == NULL)
    {
        mBesLoudnessControlCallback = NULL;
    }
    else
    {
        mBesLoudnessControlCallback = callback_data->callback;
        ASSERT(mBesLoudnessControlCallback != NULL);
        mBesLoudnessControlCallback((void *)mBesLoudnessStatus);
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateACFHCF(int value)
{
    ALOGD("%s()", __FUNCTION__);

    AUDIO_ACF_CUSTOM_PARAM_STRUCT sACFHCFParam;

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        if (value == 0)
        {
            ALOGD("setParameters Update ACF Parames");
            GetAudioCompFltCustParamFromNV(AUDIO_COMP_FLT_AUDIO, &sACFHCFParam);
            pTempFilter->setParameter(AUDIO_COMP_FLT_AUDIO, &sACFHCFParam);

        }
        else if (value == 1)
        {
            ALOGD("setParameters Update HCF Parames");
            GetAudioCompFltCustParamFromNV(AUDIO_COMP_FLT_HEADPHONE, &sACFHCFParam);
            pTempFilter->setParameter(AUDIO_COMP_FLT_HEADPHONE, &sACFHCFParam);

        }
        else if (value == 2)
        {
            ALOGD("setParameters Update ACFSub Parames");
            GetAudioCompFltCustParamFromNV(AUDIO_COMP_FLT_AUDIO_SUB, &sACFHCFParam);
            pTempFilter->setParameter(AUDIO_COMP_FLT_AUDIO_SUB, &sACFHCFParam);

        }
    }
    return NO_ERROR;
}

// ACF Preview parameter
status_t AudioALSAStreamManager::SetACFPreviewParameter(void *ptr , int len)
{
    ALOGD("%s()", __FUNCTION__);

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        pTempFilter->setParameter(AUDIO_COMP_FLT_AUDIO, (AUDIO_ACF_CUSTOM_PARAM_STRUCT *)ptr);
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::SetHCFPreviewParameter(void *ptr , int len)
{
    ALOGD("%s()", __FUNCTION__);

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        pTempFilter->setParameter(AUDIO_COMP_FLT_HEADPHONE, (AUDIO_ACF_CUSTOM_PARAM_STRUCT *)ptr);
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setSpkOutputGain(int32_t gain, uint32_t ramp_sample_cnt)
{
    ALOGD("%s(), gain = %d, ramp_sample_cnt = %u", __FUNCTION__, gain, ramp_sample_cnt);

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        pTempFilter->setSpkOutputGain(gain, ramp_sample_cnt);
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setSpkFilterParam(uint32_t fc, uint32_t bw, int32_t th)
{
    ALOGD("%s(), fc %d, bw %d, th %d", __FUNCTION__, fc, bw, th);

    for (size_t i = 0; i < mFilterManagerVector.size() ; i++)
    {
        AudioMTKFilterManager  *pTempFilter = mFilterManagerVector[i];
        pTempFilter->setSpkFilterParam(fc, bw, th);
    }

    return NO_ERROR;
}
status_t AudioALSAStreamManager::SetSpeechVmEnable(const int Type_VM)
{
    ALOGD("%s(), Type_VM=%d, only Normal VM", __FUNCTION__, Type_VM);

    AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
    GetNBSpeechParamFromNVRam(&eSphParamNB);
    if (Type_VM == 0) // normal VM
    {
        eSphParamNB.debug_info[0] = 0;
    }
    else // EPL
    {
        eSphParamNB.debug_info[0] = 3;
        if (eSphParamNB.speech_common_para[0] == 0) // if not assign EPL debug type yet, set a default one
        {
            eSphParamNB.speech_common_para[0] = 6;
        }
    }

    SetNBSpeechParamToNVRam(&eSphParamNB);
    SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::SetEMParameter(AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
    ALOGD("%s()", __FUNCTION__);

    SetNBSpeechParamToNVRam(pSphParamNB);
    SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(pSphParamNB);
    // Speech Enhancement, VM, Speech Driver
    // update VM/EPL/TTY record capability & enable if needed
    SpeechVMRecorder::GetInstance()->SetVMRecordCapability(pSphParamNB);

#if 0 // TODO(Tina):TTY
    SpeechDriverInterface *pSpeechDriver = mSpeechDriverFactory->GetSpeechDriver();
    if (pSpeechDriver->GetApSideModemStatus(TTY_STATUS_MASK) == true)
    {
        pSpeechDriver->TtyCtmDebugOn(SpeechVMRecorder::GetInstance()->GetVMRecordCapabilityForCTM4Way());
    }
#endif

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateSpeechParams(const int speech_band)
{
    ALOGD("%s()", __FUNCTION__);

//speech_band: 0:Narrow Band, 1: Wide Band, 2: Super Wideband, ..., 8: All 
    if (speech_band == 0)//Narrow Band
    {
        AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
        GetNBSpeechParamFromNVRam(&eSphParamNB);
        SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);
    }
#if defined(MTK_WB_SPEECH_SUPPORT)
    else if (speech_band == 1)//Wide Band
    {
        AUDIO_CUSTOM_WB_PARAM_STRUCT eSphParamWB;
        GetWBSpeechParamFromNVRam(&eSphParamWB);
        SpeechEnhancementController::GetInstance()->SetWBSpeechParametersToAllModem(&eSphParamWB);
    }
#endif
    else if (speech_band == 8)//set all mode parameters
    {
        AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
        AUDIO_CUSTOM_WB_PARAM_STRUCT eSphParamWB;
        GetNBSpeechParamFromNVRam(&eSphParamNB);
        SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);
#if defined(MTK_WB_SPEECH_SUPPORT)
        GetWBSpeechParamFromNVRam(&eSphParamWB);
        SpeechEnhancementController::GetInstance()->SetWBSpeechParametersToAllModem(&eSphParamWB);
#endif
    }

    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateSpeechLpbkParams()
{
    ALOGD("%s()", __FUNCTION__);
    AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
    AUDIO_CUSTOM_SPEECH_LPBK_PARAM_STRUCT  eSphParamNBLpbk;
    GetNBSpeechParamFromNVRam(&eSphParamNB);
    GetNBSpeechLpbkParamFromNVRam(&eSphParamNBLpbk);
    SpeechEnhancementController::GetInstance()->SetNBSpeechLpbkParametersToAllModem(&eSphParamNB, &eSphParamNBLpbk);
    //no need to set speech mode, only for loopback parameters update

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateMagiConParams()
{
    ALOGD("%s()", __FUNCTION__);
#if defined(MTK_MAGICONFERENCE_SUPPORT) && defined(MTK_DUAL_MIC_SUPPORT)
    AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT eSphParamMagiCon;
    GetMagiConSpeechParamFromNVRam(&eSphParamMagiCon);
    SpeechEnhancementController::GetInstance()->SetMagiConSpeechParametersToAllModem(&eSphParamMagiCon);

    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;
#else
    ALOGW("-%s(), MagiConference Not Support", __FUNCTION__);
    return INVALID_OPERATION;

#endif
}

status_t AudioALSAStreamManager::UpdateHACParams()
{
    ALOGD("%s()", __FUNCTION__);
#if defined(MTK_HAC_SUPPORT)
    AUDIO_CUSTOM_HAC_PARAM_STRUCT eSphParamHAC;
    GetHACSpeechParamFromNVRam(&eSphParamHAC);
    SpeechEnhancementController::GetInstance()->SetHACSpeechParametersToAllModem(&eSphParamHAC);

    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;
#else
    ALOGW("-%s(), HAC Not Support", __FUNCTION__);
    return INVALID_OPERATION;

#endif
}

status_t AudioALSAStreamManager::UpdateDualMicParams()
{
    ALOGD("%s()", __FUNCTION__);
    AUDIO_CUSTOM_EXTRA_PARAM_STRUCT eSphParamDualMic;
    GetDualMicSpeechParamFromNVRam(&eSphParamDualMic);
#if defined(MTK_DUAL_MIC_SUPPORT)
    SpeechEnhancementController::GetInstance()->SetDualMicSpeechParametersToAllModem(&eSphParamDualMic);
#endif

    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateSpeechMode()
{
    ALOGD("%s()", __FUNCTION__);
    //tina todo
    const audio_devices_t output_device = (audio_devices_t)AudioALSAHardwareResourceManager::getInstance()->getOutputDevice();
    const audio_devices_t input_device  = (audio_devices_t)AudioALSAHardwareResourceManager::getInstance()->getInputDevice();
    mSpeechDriverFactory->GetSpeechDriver()->SetSpeechMode(input_device, output_device);

    return NO_ERROR;
}

status_t AudioALSAStreamManager::UpdateSpeechVolume()
{
    ALOGD("%s()", __FUNCTION__);
    mAudioALSAVolumeController->initVolumeController();

    if (isModeInPhoneCall() == true)
    {
        //TINA TODO GET DEVICE
        int32_t outputDevice = (audio_devices_t)AudioALSAHardwareResourceManager::getInstance()->getOutputDevice();
        AudioALSASpeechPhoneCallController *pSpeechPhoneCallController = AudioALSASpeechPhoneCallController::getInstance();
#ifndef MTK_AUDIO_GAIN_TABLE
        mAudioALSAVolumeController->setVoiceVolume(mAudioALSAVolumeController->getVoiceVolume(), mAudioMode, (uint32)outputDevice);
#endif
        switch (outputDevice)
        {
            case AUDIO_DEVICE_OUT_WIRED_HEADSET :
            {
#ifdef  MTK_TTY_SUPPORT
                if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_VCO)
                {
                    mAudioALSAVolumeController->ApplyMicGain(Normal_Mic, mAudioMode);
                }
                else if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_HCO || pSpeechPhoneCallController->getTtyMode() == AUD_TTY_FULL)
                {
                    mAudioALSAVolumeController->ApplyMicGain(TTY_CTM_Mic, mAudioMode);
                }
                else
                {
                    mAudioALSAVolumeController->ApplyMicGain(Headset_Mic, mAudioMode);
                }
#else
                mAudioALSAVolumeController->ApplyMicGain(Headset_Mic, mAudioMode);
#endif
                break;
            }
            case AUDIO_DEVICE_OUT_WIRED_HEADPHONE :
            {
#ifdef  MTK_TTY_SUPPORT
                if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_VCO)
                {
                    mAudioALSAVolumeController->ApplyMicGain(Normal_Mic, mAudioMode);
                }
                else if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_HCO || pSpeechPhoneCallController->getTtyMode() == AUD_TTY_FULL)
                {
                    mAudioALSAVolumeController->ApplyMicGain(TTY_CTM_Mic, mAudioMode);
                }
                else
                {
                    mAudioALSAVolumeController->ApplyMicGain(Headset_Mic, mAudioMode);
                }
#else
                mAudioALSAVolumeController->ApplyMicGain(Headset_Mic, mAudioMode);
#endif
                break;
            }
            case AUDIO_DEVICE_OUT_SPEAKER:
            {
#ifdef  MTK_TTY_SUPPORT
                if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_VCO)
                {
                    mAudioALSAVolumeController->ApplyMicGain(Normal_Mic, mAudioMode);
                }
                else if (pSpeechPhoneCallController->getTtyMode() == AUD_TTY_HCO || pSpeechPhoneCallController->getTtyMode() == AUD_TTY_FULL)
                {
                    mAudioALSAVolumeController->ApplyMicGain(TTY_CTM_Mic, mAudioMode);
                }
                else
                {
                    mAudioALSAVolumeController->ApplyMicGain(Handfree_Mic, mAudioMode);
                }
#else
                mAudioALSAVolumeController->ApplyMicGain(Handfree_Mic, mAudioMode);
#endif
                break;
            }
            case AUDIO_DEVICE_OUT_EARPIECE:
            {
                mAudioALSAVolumeController->ApplyMicGain(Normal_Mic, mAudioMode);
                break;
            }
            default:
            {
                break;
            }
        }
    }
    else
    {
        setMasterVolume(mAudioALSAVolumeController->getMasterVolume());
    }
    return NO_ERROR;

}

status_t AudioALSAStreamManager::SetVCEEnable(bool bEnable)
{
    ALOGD("%s()", __FUNCTION__);
    SpeechEnhancementController::GetInstance()->SetDynamicMaskOnToAllModem(SPH_ENH_DYNAMIC_MASK_VCE, bEnable);
    return NO_ERROR;

}

status_t AudioALSAStreamManager::SetMagiConCallEnable(bool bEnable)
{
    ALOGD("%s(), bEnable=%d", __FUNCTION__, bEnable);

    // enable/disable flag
    SpeechEnhancementController::GetInstance()->SetMagicConferenceCallOn(bEnable);
    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;

}

bool AudioALSAStreamManager::GetMagiConCallEnable(void)
{
    bool bEnable = SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn();
    ALOGD("-%s(), bEnable=%d", __FUNCTION__, bEnable);

    return bEnable;
}

status_t AudioALSAStreamManager::SetHACEnable(bool bEnable)
{
    ALOGD("%s(), bEnable=%d", __FUNCTION__, bEnable);

    // enable/disable flag
    SpeechEnhancementController::GetInstance()->SetHACOn(bEnable);
    if (isModeInPhoneCall() == true) // get output device for in_call, and set speech mode
    {
        UpdateSpeechMode();
    }

    return NO_ERROR;

}

bool AudioALSAStreamManager::GetHACEnable(void)
{
    bool bEnable = SpeechEnhancementController::GetInstance()->GetHACOn();
    ALOGD("-%s(), bEnable=%d", __FUNCTION__, bEnable);

    return bEnable;
}

status_t AudioALSAStreamManager::SetVMLogConfig(unsigned short mVMConfig)
{
    ALOGD("+%s(), mVMConfig=%d", __FUNCTION__, mVMConfig);

#ifdef MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT
    if (GetVMLogConfig() == mVMConfig)
    {
        ALOGD("%s(), mVMConfig(%d) the same, retrun directly.", __FUNCTION__, mVMConfig);
        return NO_ERROR;
    }
    // Speech Enhancement, VM, Speech Driver
    // update VM/EPL/TTY record capability & enable if needed
    SpeechVMRecorder::GetInstance()->SetVMRecordCapability(mVMConfig);
#endif

    return NO_ERROR;

}

unsigned short AudioALSAStreamManager::GetVMLogConfig(void)
{
    unsigned short mVMConfig;

    AUDIO_CUSTOM_AUDIO_FUNC_SWITCH_PARAM_STRUCT eParaAudioFuncSwitch;
    GetAudioFuncSwitchParamFromNV(&eParaAudioFuncSwitch);
    mVMConfig = eParaAudioFuncSwitch.vmlog_dump_config;

    ALOGD("-%s(), mVMConfig=%d", __FUNCTION__, mVMConfig);

    return mVMConfig;
}

status_t AudioALSAStreamManager::SetCustXmlEnable(unsigned short enable)
{
    ALOGD("+%s(), enable = %d", __FUNCTION__, enable);

#ifdef MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT
    AUDIO_CUSTOM_AUDIO_FUNC_SWITCH_PARAM_STRUCT eParaAudioFuncSwitch;
    GetAudioFuncSwitchParamFromNV(&eParaAudioFuncSwitch);
    if (eParaAudioFuncSwitch.cust_xml_enable == enable)
    {
        ALOGD("%s(), enable(%d) the same, retrun directly.", __FUNCTION__, enable);
        return NO_ERROR;
    } else {
        eParaAudioFuncSwitch.cust_xml_enable = enable;
        SetAudioFuncSwitchParamToNV(&eParaAudioFuncSwitch);
        ALOGD("%s(), set CustXmlEnabl = %d\n", __FUNCTION__, enable);
    }
#endif

    return NO_ERROR;
}

unsigned short AudioALSAStreamManager::GetCustXmlEnable(void)
{
    unsigned short enable;

    AUDIO_CUSTOM_AUDIO_FUNC_SWITCH_PARAM_STRUCT eParaAudioFuncSwitch;
    GetAudioFuncSwitchParamFromNV(&eParaAudioFuncSwitch);
    enable = eParaAudioFuncSwitch.cust_xml_enable;

    ALOGD("-%s(), enable = %d", __FUNCTION__, enable);

    return enable;
}

status_t AudioALSAStreamManager::SetBtHeadsetNrec(bool bEnable)
{
    ALOGD("%s(), bEnable=%d", __FUNCTION__, bEnable);

    // enable/disable flag
    if (SpeechEnhancementController::GetInstance()->GetBtHeadsetNrecStatus() != bEnable)
    {
        SpeechEnhancementController::GetInstance()->SetBtHeadsetNrecOnToAllModem(bEnable);
    }

#if defined(MTK_AUDIO_BT_NREC_WO_ENH_MODE)
    bool mBtHeadsetNrecSwitchNeed = SpeechEnhancementController::GetInstance()->GetBtNrecSwitchNeed();
    if (isModeInPhoneCall() == true && mBtHeadsetNrecSwitchNeed) // get output device for in_call, and set speech mode
    {
        SpeechEnhancementController::GetInstance()->SetBtNrecSwitchNeed(false);
        UpdateSpeechMode();
    }
#endif

    return NO_ERROR;

}

bool AudioALSAStreamManager::GetBtHeadsetNrecStatus(void)
{
    bool bEnable = SpeechEnhancementController::GetInstance()->GetBtHeadsetNrecStatus();
    ALOGD("-%s(), bEnable=%d", __FUNCTION__, bEnable);

    return bEnable;
}

status_t AudioALSAStreamManager::Enable_DualMicSettng(sph_enh_dynamic_mask_t sphMask, bool bEnable)
{
    ALOGD("%s(), bEnable=%d", __FUNCTION__, bEnable);

    SpeechEnhancementController::GetInstance()->SetDynamicMaskOnToAllModem(sphMask, bEnable);
    return NO_ERROR;

}

status_t AudioALSAStreamManager::Set_LSPK_DlMNR_Enable(sph_enh_dynamic_mask_t sphMask, bool bEnable)
{
    ALOGD("%s(), bEnable=%d", __FUNCTION__, bEnable);

    Enable_DualMicSettng(sphMask, bEnable);

    if (SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn() == true &&
        SpeechEnhancementController::GetInstance()->GetDynamicMask(sphMask) == true)
    {
        ALOGE("Cannot open MagicConCall & LoudSpeaker DMNR at the same time!!");
    }
    return NO_ERROR;

}


bool AudioALSAStreamManager::getVoiceWakeUpNeedOn()
{
    AudioAutoTimeoutLock _l(mLock);
    return mVoiceWakeUpNeedOn;
}

status_t AudioALSAStreamManager::setVoiceWakeUpNeedOn(const bool enable)
{
    ALOGD("+%s(), mVoiceWakeUpNeedOn: %d => %d ", __FUNCTION__, mVoiceWakeUpNeedOn, enable);
    AudioAutoTimeoutLock _l(mLock);

    if (enable == mVoiceWakeUpNeedOn)
    {
        ALOGW("-%s(), enable(%d) == mVoiceWakeUpNeedOn(%d), return", __FUNCTION__, enable, mVoiceWakeUpNeedOn);
        return INVALID_OPERATION;
    }

    if (enable == true)
    {
        if (mStreamInVector.size() != 0 || mForceDisableVoiceWakeUpForSetMode == true)
        {
            ALOGD("-%s(), mStreamInVector.size() = %d, mForceDisableVoiceWakeUpForSetMode = %d, return", __FUNCTION__, mStreamInVector.size(), mForceDisableVoiceWakeUpForSetMode);
        }
        else
        {
            if (mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == false)
            {
                mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(true);
            }
        }
    }
    else
    {
        if (mAudioALSAVoiceWakeUpController->getVoiceWakeUpEnable() == true)
        {
            mAudioALSAVoiceWakeUpController->setVoiceWakeUpEnable(false);
        }
    }


    property_set(PROPERTY_KEY_VOICE_WAKE_UP_NEED_ON, (enable == false) ? "0" : "1");
    mVoiceWakeUpNeedOn = enable;

    ALOGD("-%s(), mVoiceWakeUpNeedOn: %d", __FUNCTION__, mVoiceWakeUpNeedOn);
    return NO_ERROR;
}

void AudioALSAStreamManager::UpdateDynamicFunctionMask(void)
{
    ALOGD("+%s()", __FUNCTION__);
    if (mStreamInVector.size() > 0)
    {
        for (size_t i = 0; i < mStreamInVector.size(); i++)
        {
            mStreamInVector[i]->UpdateDynamicFunctionMask();
        }
    }
    ALOGD("-%s()", __FUNCTION__);
}

bool AudioALSAStreamManager::EnableBesRecord(void)
{
    bool bRet = false;
    if ((QueryFeatureSupportInfo()& SUPPORT_HD_RECORD) > 0)
    {
        bRet = true;
    }
    ALOGD("-%s(), %x", __FUNCTION__, bRet);

    return bRet;
}

status_t AudioALSAStreamManager::setLowLatencyMode(bool mode)
{
    AudioAutoTimeoutLock _l(mLock);
    AudioALSAStreamOut *pAudioALSAStreamOut = NULL;

    for (size_t i = 0; i < mStreamOutVector.size(); i++)
    {
        pAudioALSAStreamOut = mStreamOutVector[i];
        pAudioALSAStreamOut->setLowLatencyMode(mode);
    }

    return NO_ERROR;
}

status_t AudioALSAStreamManager::setBypassDLProcess(bool flag)
{
    AudioAutoTimeoutLock _l(mLock);
    AudioALSAStreamOut *pAudioALSAStreamOut = NULL;

    mBypassPostProcessDL = flag;

    return NO_ERROR;
}
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
bool AudioALSAStreamManager::IsSphStrmSupport(void)
{
    char property_value[PROPERTY_VALUE_MAX];
    property_get("streamout.speech_stream.enable", property_value, "1");
    int speech_stream = atoi(property_value);
    ALOGD("%s = %d", __FUNCTION__, speech_stream);
    return ((speech_stream == 0) ? false : true);
}
status_t AudioALSAStreamManager::EnableSphStrm(audio_devices_t output_devices)
{
    AudioALSASpeechStreamController::getInstance()->SetStreamOutputDevice(output_devices);
    if (isModeInPhoneCall(mAudioMode) == true)
    {
        if ((output_devices & AUDIO_DEVICE_OUT_SPEAKER) != 0)
        {
            AudioALSASpeechStreamController::getInstance()->EnableSpeechStreamThread(true);
        }
    }
    return NO_ERROR;
}

status_t AudioALSAStreamManager::DisableSphStrm(audio_devices_t output_devices)
{
    AudioALSASpeechStreamController::getInstance()->SetStreamOutputDevice(output_devices);
    if (isModeInPhoneCallSupportEchoRef(mAudioMode) == true)
    {
        if (AudioALSASpeechStreamController::getInstance()->IsSpeechStreamThreadEnable() == true)
        {
            AudioALSASpeechStreamController::getInstance()->EnableSpeechStreamThread(false);
        }
    }
    return NO_ERROR;
}

status_t AudioALSAStreamManager::EnableSphStrm(audio_mode_t new_mode)
{
    if ((new_mode < AUDIO_MODE_NORMAL) || (new_mode > AUDIO_MODE_MAX))
    {
        return BAD_VALUE;
    }

    if (isModeInPhoneCall(new_mode) == true)
    {
        if ((AudioALSASpeechStreamController::getInstance()->GetStreamOutputDevice() & AUDIO_DEVICE_OUT_SPEAKER) != 0 &&
            (AudioALSASpeechStreamController::getInstance()->IsSpeechStreamThreadEnable() == false))
        {
            AudioALSASpeechStreamController::getInstance()->EnableSpeechStreamThread(true);
        }
    }
    return NO_ERROR;
}

status_t AudioALSAStreamManager::DisableSphStrm(audio_mode_t new_mode)
{
    if ((new_mode < AUDIO_MODE_NORMAL) || (new_mode > AUDIO_MODE_MAX))
    {
        return BAD_VALUE;
    }
    if (new_mode == mAudioMode)
    {
        ALOGW("-%s(), mAudioMode: %d == %d, return", __FUNCTION__, mAudioMode, new_mode);
        return BAD_VALUE;
    }

    if (isModeInPhoneCallSupportEchoRef(mAudioMode) == true)
    {
        if (AudioALSASpeechStreamController::getInstance()->IsSpeechStreamThreadEnable() == true)
        {
            AudioALSASpeechStreamController::getInstance()->EnableSpeechStreamThread(false);
        }
    }
    return NO_ERROR;
}

bool AudioALSAStreamManager::isModeInPhoneCallSupportEchoRef(const audio_mode_t audio_mode)
{
    if (audio_mode == AUDIO_MODE_IN_CALL)
    {
        return true;
    }
    else if (audio_mode == AUDIO_MODE_IN_CALL_EXTERNAL)
    {
        // get DSDA proposal type, only proposal2 support
        char property_value[PROPERTY_VALUE_MAX];
        property_get("persist.af.dsda_proposal_type", property_value, "2");
        int dsda_proposal_type = atoi(property_value);
        ALOGD("dsda_proposal_type = %d", dsda_proposal_type);
        if (dsda_proposal_type == 2)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    else
    {
        return false;
    }
}

status_t AudioALSAStreamManager::UpdateStreamOutFilter(android_audio_legacy::AudioStreamOut *out, int format, uint32_t channels, uint32_t sampleRate)
{
    AudioAutoTimeoutLock streamVectorAutoTimeoutLock(mStreamVectorLock);
    AudioAutoTimeoutLock _l(mLock);
    if (out == NULL)
    {
        ALOGE("%s(), Cannot close null output stream!! return", __FUNCTION__);
        return INVALID_OPERATION;
    }

    AudioALSAStreamOut *pAudioALSAStreamOut = static_cast<AudioALSAStreamOut *>(out);
    uint32_t dFltMngindex = mFilterManagerVector.indexOfKey(pAudioALSAStreamOut->getIdentity());

    if (dFltMngindex < mFilterManagerVector.size())
    {
        AudioMTKFilterManager *pAudioFilterManagerHandler = static_cast<AudioMTKFilterManager *>(mFilterManagerVector[dFltMngindex]);
        ALOGD("%s, remove mFilterManagerVector Success [%d]/[%d] [%d], pAudioFilterManagerHandler=%p",
              __FUNCTION__, dFltMngindex, mFilterManagerVector.size(), pAudioALSAStreamOut->getIdentity(), pAudioFilterManagerHandler);
        ASSERT(pAudioFilterManagerHandler != 0);
        mFilterManagerVector.removeItem(pAudioALSAStreamOut->getIdentity());
        delete pAudioFilterManagerHandler;
    }

    AudioMTKFilterManager *pAudioFilterManagerHandler = new AudioMTKFilterManager(sampleRate, android_audio_legacy::AudioSystem::popCount(channels), format, pAudioALSAStreamOut->bufferSize());
    mFilterManagerVector.add(pAudioALSAStreamOut->getIdentity(), pAudioFilterManagerHandler);
    return NO_ERROR;
}
#endif


status_t AudioALSAStreamManager::setParametersToStreamOut(const String8 &keyValuePairs) // TODO(Harvey
{
    if (mStreamOutVector.size() == 0)
    {
        return INVALID_OPERATION;
    }

    AudioALSAStreamOut *pAudioALSAStreamOut = NULL;
    for (size_t i = 0; i < mStreamOutVector.size() ; i++)
    {
        pAudioALSAStreamOut = mStreamOutVector[i];
        pAudioALSAStreamOut->setParameters(keyValuePairs);
    }

    return NO_ERROR;
}


status_t AudioALSAStreamManager::setParameters(const String8 &keyValuePairs, int IOport) // TODO(Harvey)
{
    status_t status = PERMISSION_DENIED;
    ssize_t index = -1;

    ALOGD("+%s(), IOport = %d, keyValuePairs = %s", __FUNCTION__, IOport, keyValuePairs.string());

    index = mStreamOutVector.indexOfKey(IOport);
    if (index >= 0)
    {
        ALOGD("Send to mStreamOutVector[%zu]", index);
        AudioALSAStreamOut *pAudioALSAStreamOut = mStreamOutVector.valueAt(index);
        status = pAudioALSAStreamOut->setParameters(keyValuePairs);
        ALOGD("-%s()", __FUNCTION__);
        return status;
    }

    index = mStreamInVector.indexOfKey(IOport);
    if (index >= 0)
    {
        ALOGD("Send to mStreamInVector [%zu]", index);
        AudioALSAStreamIn *pAudioALSAStreamIn = mStreamInVector.valueAt(index);
        status = pAudioALSAStreamIn->setParameters(keyValuePairs);
        ALOGD("-%s()", __FUNCTION__);
        return status;
    }

    ALOGE("-%s(), do nothing, return", __FUNCTION__);
    return status;
}


} // end of namespace android

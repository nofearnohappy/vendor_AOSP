#include "AudioALSAStreamIn.h"

#include "AudioALSAStreamManager.h"
#include "AudioALSACaptureHandlerBase.h"
#include "AudioUtility.h"

#include "AudioALSAFMController.h"

#include <audio_effects/effect_aec.h>

#define LOG_TAG "AudioALSAStreamIn"

#define NORMAL_BUFFER_TIME_MS 20 //ms
#define LOW_LATENCY_BUFFER_TIME_MS 5 //ms

namespace android
{

int AudioALSAStreamIn::mDumpFileNum = 0;

// TODO(Harvey): Query this
static const audio_format_t       kDefaultInputSourceFormat      = AUDIO_FORMAT_PCM_16_BIT;
static const audio_channel_mask_t kDefaultInputSourceChannelMask = AUDIO_CHANNEL_IN_STEREO;
static const audio_channel_mask_t kDefaultVoiceInputSourceChannelMask = AUDIO_CHANNEL_IN_VOICE_UPLINK | AUDIO_CHANNEL_IN_VOICE_DNLINK;
static const uint32_t             kDefaultInputSourceSampleRate  = 48000;


//uint32_t AudioALSAStreamIn::mSuspendCount = 0;

AudioALSAStreamIn::AudioALSAStreamIn() :
    mStreamManager(AudioALSAStreamManager::getInstance()),
    mCaptureHandler(NULL),
    mIdentity(0xFFFFFFFF),
    mAudioSpeechEnhanceInfoInstance(AudioSpeechEnhanceInfo::getInstance()),
    mStandby(true),
    mSuspendCount(0)
{
    ALOGD("%s()", __FUNCTION__);

    memset(&mStreamAttributeTarget, 0, sizeof(mStreamAttributeTarget));

    for (int i = 0; i < MAX_PREPROCESSORS; i++)
    {
        mPreProcessEffectBackup[i] = {0};
    }
    mPreProcessEffectBackupCount = 0;
    mPCMDumpFile = NULL;
}


AudioALSAStreamIn::~AudioALSAStreamIn()
{
    ALOGD("%s()", __FUNCTION__);

    ASSERT(mStandby == true && mCaptureHandler == NULL);
}


bool AudioALSAStreamIn::checkOpenStreamFormat(int *format)
{
    if (*format != kDefaultInputSourceFormat)
    {
        ALOGE("%s(), wrong format 0x%x, use 0x%x instead.", __FUNCTION__, *format, kDefaultInputSourceFormat);
        *format = kDefaultInputSourceFormat;
        return false;
    }
    else
    {
        return true;
    }
}

bool AudioALSAStreamIn::checkOpenStreamChannels(uint32_t *channels)
{
    if (*channels != kDefaultInputSourceChannelMask)
    {
        // Check channel mask for voice recording
        if (*channels & kDefaultVoiceInputSourceChannelMask && !(*channels & ~kDefaultVoiceInputSourceChannelMask))
        {
            return true;
        }

        if(*channels & AUDIO_CHANNEL_IN_MONO)
        {
            return true;
        }

        ALOGE("%s(), wrong channels 0x%x, use 0x%x instead.", __FUNCTION__, *channels, kDefaultInputSourceChannelMask);
        *channels = kDefaultInputSourceChannelMask;
        return false;
    }
    else
    {
        return true;
    }

}


bool AudioALSAStreamIn::checkOpenStreamSampleRate(const audio_devices_t devices, uint32_t *sampleRate)
{
    if (devices == AUDIO_DEVICE_IN_FM_TUNER) // FM
    {
        const uint32_t fm_uplink_sampling_rate = AudioALSAFMController::getInstance()->getFmUplinkSamplingRate();
        if (*sampleRate != fm_uplink_sampling_rate)
        {
            ALOGE("%s(), AUDIO_DEVICE_IN_FM_TUNER, wrong sampleRate %d, use %d instead.", __FUNCTION__, *sampleRate, fm_uplink_sampling_rate);
            *sampleRate = fm_uplink_sampling_rate;
            return false;
        }
        else
        {
            return true;
        }
    }
    else if (devices == AUDIO_DEVICE_IN_MATV) // MATV
    {
        if (*sampleRate != 32000) // TODO(Harvey): AudioMATVController::GetInstance()->GetMatvUplinkSamplingRate()
        {
            ALOGE("%s(), AUDIO_DEVICE_IN_MATV, wrong sampleRate %d, use %d instead.", __FUNCTION__, *sampleRate, 32000);
            *sampleRate = 32000;
            return false;
        }
        else
        {
            return true;
        }
    }
    else if (devices == AUDIO_DEVICE_IN_TDM) // TDM
    {
        if (*sampleRate != 44100) // TODO(Harvey): AudioMATVController::GetInstance()->GetMatvUplinkSamplingRate()
        {
            ALOGE("%s(), AUDIO_DEVICE_IN_MATV, wrong sampleRate %d, use %d instead.", __FUNCTION__, *sampleRate, 44100);
            *sampleRate = 44100;
            return false;
        }
        else
        {
            return true;
        }
    }
    else // Normal record
    {
        if (*sampleRate != kDefaultInputSourceSampleRate)
        {
            ALOGD("%s(), origin sampleRate %d, kDefaultInputSourceSampleRate %d.", __FUNCTION__, *sampleRate, kDefaultInputSourceSampleRate);

            if (mStreamAttributeTarget.BesRecord_Info.besrecord_tuningEnable || mStreamAttributeTarget.BesRecord_Info.besrecord_dmnr_tuningEnable)
            {
                if (*sampleRate == 16000)
                {
                    ALOGE("%s(), BesRecord 16K tuning", __FUNCTION__);
                    mStreamAttributeTarget.BesRecord_Info.besrecord_tuning16K = true;
                    *sampleRate = 48000;
                    return true;
                }
            }

            return true;
        }
        else
        {
            if (mStreamAttributeTarget.BesRecord_Info.besrecord_tuningEnable || mStreamAttributeTarget.BesRecord_Info.besrecord_dmnr_tuningEnable)
            {
                mStreamAttributeTarget.BesRecord_Info.besrecord_tuning16K = false;
            }

            return true;
        }
    }

}

status_t AudioALSAStreamIn::set(
    uint32_t devices,
    int *format,
    uint32_t *channels,
    uint32_t *sampleRate,
    status_t *status,
    android_audio_legacy::AudioSystem::audio_in_acoustics acoustics, uint32_t flags)
{
    ALOGD("%s(), devices = 0x%x, format = 0x%x, channels = 0x%x, sampleRate = %d, acoustics = 0x%x, flags = %d",
          __FUNCTION__, devices, *format, *channels, *sampleRate, acoustics, flags);

    AudioAutoTimeoutLock _l(mLock);

    *status = NO_ERROR;

    CheckBesRecordInfo();

    // check format
    if (checkOpenStreamFormat(format) == false)
    {
        *status = BAD_VALUE;
    }

    // check channel mask
    if (checkOpenStreamChannels(channels) == false)
    {
        *status = BAD_VALUE;
    }

    // check sample rate
    if (checkOpenStreamSampleRate(devices, sampleRate) == false)
    {
        *status = BAD_VALUE;
    }

    // config stream attribute
    if (*status == NO_ERROR)
    {
        // format
        mStreamAttributeTarget.audio_format = static_cast<audio_format_t>(*format);

        // channel
        mStreamAttributeTarget.audio_channel_mask = *channels;
        mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(*channels);

        // sample rate
        mStreamAttributeTarget.sample_rate = *sampleRate;

        // devices
        mStreamAttributeTarget.input_device = static_cast<audio_devices_t>(devices);

        // acoustics flags
        mStreamAttributeTarget.acoustics_mask = static_cast<audio_in_acoustics_t>(acoustics);

        // set buffer size // TODO(Harvey): Check
#ifdef UPLINK_LOW_LATENCY
        //fast record flags
        mStreamAttributeTarget.mAudioInputFlags = static_cast<audio_input_flags_t>(flags);

        size_t wordSize = 0;
        switch (mStreamAttributeTarget.audio_format)
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
                ALOGW("%s(), wrong format(0x%x), default use wordSize = %d", __FUNCTION__, mStreamAttributeTarget.audio_format, sizeof(int16_t));
                wordSize = sizeof(int16_t);
                break;
            }
        }

        if (mStreamAttributeTarget.mAudioInputFlags & AUDIO_INPUT_FLAG_FAST)
        {
            mStreamAttributeTarget.buffer_size = (mStreamAttributeTarget.sample_rate/1000) * LOW_LATENCY_BUFFER_TIME_MS * mStreamAttributeTarget.num_channels * wordSize;
        }
        else
        {
            mStreamAttributeTarget.buffer_size = (mStreamAttributeTarget.sample_rate/1000) * NORMAL_BUFFER_TIME_MS * mStreamAttributeTarget.num_channels * wordSize;
        }
#else
        // Buffer size: 2048(period_size) * 8(period_count) * 2(ch) * 2(byte) = 64 kb
        mStreamAttributeTarget.buffer_size = 65536; /*mStreamManager->getInputBufferSize(mStreamAttributeTarget.sample_rate,
                                                                                mStreamAttributeTarget.audio_format,
                                                                                mStreamAttributeTarget.num_channels);*/
#endif
        ALOGD("%s() done, sampleRate = %d, num_channels = %d, buffer_size=%d",
          __FUNCTION__, mStreamAttributeTarget.sample_rate,mStreamAttributeTarget.num_channels,
          mStreamAttributeTarget.buffer_size);

    }

    return *status;
}


uint32_t AudioALSAStreamIn::sampleRate() const
{
    ALOGV("%s(), return %d", __FUNCTION__, mStreamAttributeTarget.sample_rate);
    return mStreamAttributeTarget.sample_rate;
}


size_t AudioALSAStreamIn::bufferSize() const
{
    ALOGV("%s(), return 0x%x", __FUNCTION__, mStreamAttributeTarget.buffer_size);
    return mStreamAttributeTarget.buffer_size;
}

uint32_t AudioALSAStreamIn::channels() const
{
    ALOGV("%s(), return 0x%x", __FUNCTION__, mStreamAttributeTarget.audio_channel_mask);
    return mStreamAttributeTarget.audio_channel_mask;
}

int AudioALSAStreamIn::format() const
{
    ALOGV("%s(), return 0x%x", __FUNCTION__, mStreamAttributeTarget.audio_format);
    return mStreamAttributeTarget.audio_format;
}

status_t AudioALSAStreamIn::setGain(float gain)
{
    ALOGD("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}

void AudioALSAStreamIn::SetInputMute(bool bEnable)
{
    ALOGD("%s(), %d", __FUNCTION__, bEnable);
    //AudioAutoTimeoutLock _l(mLock);
    mStreamAttributeTarget.micmute = bEnable;
    ALOGD("-%s()", __FUNCTION__);
}

ssize_t AudioALSAStreamIn::read(void *buffer, ssize_t bytes)
{
    ALOGV("%s(), bytes= %d", __FUNCTION__, bytes);
    ssize_t ret_size = bytes;

    if (mSuspendCount > 0)
    {
        // here to sleep a buffer size latency and return.
        ALOGD("%s(), mSuspendCount = %u", __FUNCTION__, mSuspendCount);
        memset(buffer, 0, bytes);
        size_t wordSize = 0;
        switch (mStreamAttributeTarget.audio_format)
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
                ALOGW("%s(), wrong format(0x%x), default use wordSize = %d", __FUNCTION__, mStreamAttributeTarget.audio_format, sizeof(int16_t));
                wordSize = sizeof(int16_t);
                break;
            }
        }
        int sleepus = ((bytes * 1000) / ((mStreamAttributeTarget.sample_rate / 1000) * mStreamAttributeTarget.num_channels * wordSize));
        ALOGD("%s(), sleepus = %d", __FUNCTION__, sleepus);
        usleep(sleepus);
        return bytes;
    }

    AudioAutoTimeoutLock _l(mLock);

    status_t status = NO_ERROR;

    /// check open
    if (mStandby == true)
    {
        status = open();
    }

    /// write pcm data
    ASSERT(mCaptureHandler != NULL);

    ret_size = mCaptureHandler->read(buffer, bytes);

    WritePcmDumpData(buffer, bytes);

    return ret_size;
}


status_t AudioALSAStreamIn::dump(int fd, const Vector<String16> &args)
{
    ALOGD("%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSAStreamIn::standby()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mLock);

    status_t status = NO_ERROR;

    /// check close
    if (mStandby == false)
    {
        status = close();
    }

    ALOGD("-%s()", __FUNCTION__);
    return status;
}


status_t AudioALSAStreamIn::setParameters(const String8 &keyValuePairs)
{
    ALOGD("+%s(): %s", __FUNCTION__, keyValuePairs.string());
    AudioParameter param = AudioParameter(keyValuePairs);

    /// keys
    const String8 keyInputSource = String8(AudioParameter::keyInputSource);
    const String8 keyRouting     = String8(AudioParameter::keyRouting);

    /// parse key value pairs
    status_t status = NO_ERROR;
    int value = 0;

    /// intput source
    if (param.getInt(keyInputSource, value) == NO_ERROR)
    {
        param.remove(keyInputSource);
        // TODO(Harvey): input source
        AudioAutoTimeoutLock _l(mLock);
        ALOGD("%s() InputSource = %d", __FUNCTION__, value);
        mStreamAttributeTarget.input_source = static_cast<audio_source_t>(value);

        if (mStreamAttributeTarget.BesRecord_Info.besrecord_ForceMagiASREnable == true)
        {
            ALOGD("%s() force input source to AUDIO_SOURCE_CUSTOMIZATION1", __FUNCTION__);
            mStreamAttributeTarget.input_source = AUDIO_SOURCE_CUSTOMIZATION1;
        }

        if (mStreamAttributeTarget.BesRecord_Info.besrecord_ForceAECRecEnable == true)
        {
            ALOGD("%s() force input source to AUDIO_SOURCE_CUSTOMIZATION2", __FUNCTION__);
            mStreamAttributeTarget.input_source = AUDIO_SOURCE_CUSTOMIZATION2;
        }
    }

    /// routing
    if (param.getInt(keyRouting, value) == NO_ERROR)
    {
        param.remove(keyRouting);

        AudioAutoTimeoutLock _l(mLock);

        audio_devices_t inputdevice = static_cast<audio_devices_t>(value);
        //only need to modify the device while VoIP
        if (mStreamAttributeTarget.BesRecord_Info.besrecord_voip_enable == true)
        {
            if (mStreamAttributeTarget.output_devices == AUDIO_DEVICE_OUT_SPEAKER)
            {
                if (inputdevice == AUDIO_DEVICE_IN_BUILTIN_MIC)
                {
                    if (USE_REFMIC_IN_LOUDSPK == 1)
                    {
                        inputdevice = AUDIO_DEVICE_IN_BACK_MIC;
                        ALOGD("%s() force change to back mic", __FUNCTION__);
                    }
                }
            }
        }
        status = mStreamManager->routingInputDevice(mStreamAttributeTarget.input_device, inputdevice);
    }
#if defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
    if (param.getInt(String8("MOD_DAI_INPUT"), value) == NO_ERROR)
    {
        param.remove(String8("MOD_DAI_INPUT"));
        ALOGD("%s() set stream to MOD_DAI", __FUNCTION__);
        // channel
        mStreamAttributeTarget.audio_channel_mask = AUDIO_CHANNEL_IN_MONO;
        mStreamAttributeTarget.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeTarget.audio_channel_mask);

        // sample rate
        mStreamAttributeTarget.sample_rate = 16000;
        mStreamAttributeTarget.bFixedRouting = true;
        mStreamAttributeTarget.bModemDai_Input = true;
    }
#endif
    if (param.size())
    {
        ALOGW("%s(), still have param.size() = %d, remain param = \"%s\"",
              __FUNCTION__, param.size(), param.toString().string());
        status = BAD_VALUE;
    }

    ALOGD("-%s(): %s ", __FUNCTION__, keyValuePairs.string());
    return status;
}

String8 AudioALSAStreamIn::getParameters(const String8 &keys)
{
    ALOGD("%s()", __FUNCTION__);
    AudioParameter param = AudioParameter(keys);
    return param.toString();
}

unsigned int AudioALSAStreamIn::getInputFramesLost() const
{
    return 0;
}

status_t AudioALSAStreamIn::addAudioEffect(effect_handle_t effect)
{
    ALOGD("%s(), %p", __FUNCTION__, effect);

    int status;
    effect_descriptor_t desc;

    //record the effect which need enabled and set to capture handle later (setup it while capture handle created)
    status = (*effect)->get_descriptor(effect, &desc);
    ALOGD("%s(), effect name:%s, BackupCount=%d", __FUNCTION__, desc.name, mPreProcessEffectBackupCount);

    if (mPreProcessEffectBackupCount >= MAX_PREPROCESSORS)
    {
        ALOGD("%s(), exceed the uplimit", __FUNCTION__);
        return NO_ERROR;
    }

    if (status != 0)
    {
        ALOGD("%s(), no corresponding effect", __FUNCTION__);
        return NO_ERROR;
    }
    else
    {
        AudioAutoTimeoutLock _l(mLock);

        for (int i = 0; i < mPreProcessEffectBackupCount; i++)
        {
            if (mPreProcessEffectBackup[i] == effect)
            {
                ALOGD("%s() already found %s at index %d", __FUNCTION__, desc.name, i);
                return NO_ERROR;
            }
        }

        //echo reference
        if (memcmp(&desc.type, FX_IID_AEC, sizeof(effect_uuid_t)) == 0)
        {
            ALOGD("%s(), AECOn, need reopen the capture handle", __FUNCTION__);
            if (mStandby == false)
            {
                close();
            }
            mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_AECOn = true;
        }

        mPreProcessEffectBackup[mPreProcessEffectBackupCount] = effect;
        mPreProcessEffectBackupCount++;

        mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Record[mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Count] = effect;
        mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Count++;
        mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Update = true;
    }

    ALOGD("%s()-", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSAStreamIn::removeAudioEffect(effect_handle_t effect)
{
    ALOGD("%s(), %p", __FUNCTION__, effect);

    int i;
    int status;
    status_t RetStatus = -EINVAL;
    effect_descriptor_t desc;

    if (mPreProcessEffectBackupCount <= 0)
    {
        ALOGD("%s(), mPreProcessEffectBackupCount wrong", __FUNCTION__);
        return NO_ERROR;
    }


    status = (*effect)->get_descriptor(effect, &desc);
    ALOGD("%s(), effect name:%s, BackupCount=%d", __FUNCTION__, desc.name, mPreProcessEffectBackupCount);
    if (status != 0)
    {
        ALOGD("%s(), no corresponding effect", __FUNCTION__);
        return NO_ERROR;
    }

    AudioAutoTimeoutLock _l(mLock);

    for (i = 0; i < mPreProcessEffectBackupCount; i++)
    {
        if (RetStatus == 0)   /* status == 0 means an effect was removed from a previous slot */
        {
            mPreProcessEffectBackup[i - 1] = mPreProcessEffectBackup[i];
            mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Record[i - 1] = mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Record[i];
            ALOGD("%s() moving fx from %d to %d", __FUNCTION__, i, i - 1);
            continue;
        }
        if (mPreProcessEffectBackup[i] == effect)
        {
            ALOGD("%s() found fx at index %d, %p", __FUNCTION__, i, mPreProcessEffectBackup[i]);
            //            free(preprocessors[i].channel_configs);
            RetStatus = 0;
        }
    }

    if (RetStatus != 0)
    {
        ALOGD("%s() no effect found in backup queue", __FUNCTION__);
        return NO_ERROR;
    }

    //echo reference
    if (memcmp(&desc.type, FX_IID_AEC, sizeof(effect_uuid_t)) == 0)
    {
        if (mStandby == false)
        {
            close();
        }
        mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_AECOn = false;
    }

    mPreProcessEffectBackupCount--;
    /* if we remove one effect, at least the last preproc should be reset */
    mPreProcessEffectBackup[mPreProcessEffectBackupCount] = NULL;

    mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Count--;
    /* if we remove one effect, at least the last preproc should be reset */
    mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Record[mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Count] = NULL;
    mStreamAttributeTarget.NativePreprocess_Info.PreProcessEffect_Update = true;

    ALOGD("%s()-", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAStreamIn::open()
{
    // call open() only when mLock is locked.
    ASSERT(mLock.tryLock() != 0);

    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    if (mStandby == true)
    {
        mStandby = false;

        // create capture handler
        ASSERT(mCaptureHandler == NULL);
        mCaptureHandler = mStreamManager->createCaptureHandler(&mStreamAttributeTarget);

        // open audio hardware
        status = mCaptureHandler->open();
        ASSERT(status == NO_ERROR);

        OpenPCMDump();
    }

    return status;
}


status_t AudioALSAStreamIn::close()
{
    // call close() only when mLock is locked.
    ASSERT(mLock.tryLock() != 0);

    ALOGD("%s()", __FUNCTION__);

    status_t status = NO_ERROR;

    if (mStandby == false)
    {
        mStandby = true;

        ASSERT(mCaptureHandler != NULL);

        // close audio hardware
        status = mCaptureHandler->close();
        if (status != NO_ERROR)
        {
            ALOGE("%s(), close() fail!!", __FUNCTION__);
        }

        ClosePCMDump();
        // destroy playback handler
        mStreamManager->destroyCaptureHandler(mCaptureHandler);
        mCaptureHandler = NULL;
    }

    ASSERT(mCaptureHandler == NULL);
    return status;
}


status_t AudioALSAStreamIn::routing(audio_devices_t input_device)
{
    bool is_lock_in_this_function = false;
    if (mLock.tryLock() == 0) // from another stream in setParameter routing
    {
        ALOGD("%s(), is_lock_in_this_function = true", __FUNCTION__);
        is_lock_in_this_function = true;
    }

    ALOGD("+%s(), input_device = 0x%x", __FUNCTION__, input_device);

    status_t status = NO_ERROR;

    if (input_device == mStreamAttributeTarget.input_device)
    {
        ALOGW("%s(), input_device = 0x%x, already the same input device as current using", __FUNCTION__, input_device);

        if (is_lock_in_this_function == true)
        {
            mLock.unlock();
        }

        return status;
    }

    if (mStandby == false)
    {
        ASSERT(mStreamManager->isModeInPhoneCall() == false); // TODO(Harvey): routing & setMode simontaneously? move routing control to StreamManager?

#if 1 // ver1.
        status = close();
#else // ver2.
        ASSERT(mCaptureHandler != NULL);
        status = mCaptureHandler->routing(input_device);
#endif
    }

    mStreamAttributeTarget.input_device = input_device;

    if (is_lock_in_this_function == true)
    {
        mLock.unlock();
    }

    ALOGD("-%s()", __FUNCTION__);
    return status;
}

status_t AudioALSAStreamIn::updateOutputDeviceInfoForInputStream(audio_devices_t output_devices)
{

    ALOGD("+%s(), output_devices: 0x%x => 0x%x", __FUNCTION__, mStreamAttributeTarget.output_devices, output_devices);

    status_t status = NO_ERROR;
    bool bBesRecUpdate = false;
    audio_devices_t inputdevice = mStreamAttributeTarget.input_device;

    if (output_devices != mStreamAttributeTarget.output_devices)
    {
        //only need to modify the input device under VoIP
        if (mStreamAttributeTarget.BesRecord_Info.besrecord_voip_enable == true)
        {
            //receiver -> speaker
            if (output_devices == AUDIO_DEVICE_OUT_SPEAKER)
            {
                if (inputdevice == AUDIO_DEVICE_IN_BUILTIN_MIC)
                {
                    if (USE_REFMIC_IN_LOUDSPK == 1)
                    {
                        inputdevice = AUDIO_DEVICE_IN_BACK_MIC;
                        ALOGD("%s(), force using back mic", __FUNCTION__);
                    }
                }
            }
            else if (output_devices == AUDIO_DEVICE_OUT_EARPIECE)   //speaker -> receiver
            {
                if (inputdevice == AUDIO_DEVICE_IN_BACK_MIC)
                {
                    if (USE_REFMIC_IN_LOUDSPK == 1) //only use refmic in LOUDSPK
                    {
                        inputdevice = AUDIO_DEVICE_IN_BUILTIN_MIC;
                        ALOGD("%s(), force using main mic", __FUNCTION__);
                    }
                }
            }
        }

        if (inputdevice != mStreamAttributeTarget.input_device)
        {
            //update output devices to input stream info
            ALOGD("%s(), input_device: 0x%x => 0x%x", __FUNCTION__, mStreamAttributeTarget.input_device, inputdevice);
            AudioAutoTimeoutLock _l(mLock);
            mStreamAttributeTarget.output_devices = output_devices;
            routing(inputdevice);
        }
        else    //if no input device update
        {
            //speaker/receiver(headphone) switch no input path change, but should use receiver params
            if (((output_devices == AUDIO_DEVICE_OUT_SPEAKER) && ((mStreamAttributeTarget.output_devices == AUDIO_DEVICE_OUT_EARPIECE) ||
                                                                  (mStreamAttributeTarget.output_devices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)))
                || (((output_devices == AUDIO_DEVICE_OUT_EARPIECE) || (output_devices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)) && (mStreamAttributeTarget.output_devices == AUDIO_DEVICE_OUT_SPEAKER)))
            {
                ALOGD("%s(), BesRecord parameters update", __FUNCTION__);
                bBesRecUpdate = true;
            }
            //update output devices to input stream info
            mStreamAttributeTarget.output_devices = output_devices;

            if (bBesRecUpdate)
            {
                //update VoIP parameters config, only streamin has VoIP process
                if ((mStreamAttributeTarget.BesRecord_Info.besrecord_enable == true) && (mStreamAttributeTarget.BesRecord_Info.besrecord_voip_enable == true))
                {
#if 1
                    ALOGD("%s(), going to check UpdateBesRecParam", __FUNCTION__);
                    AudioAutoTimeoutLock _l(mLock);
                    if (mStandby == false)
                    {
                        ASSERT(mStreamManager->isModeInPhoneCall() == false);
                        ALOGD("%s(), close handler and reopen it", __FUNCTION__);
                        status = close();
                    }
#else
                    // [FIXME] Cannot trigger AEC resync successfully
                    ALOGD("%s(), going to UpdateBesRecParam", __FUNCTION__);
                    AudioAutoTimeoutLock _l(mLock);
                    if (mCaptureHandler != NULL)
                    {
                        mCaptureHandler->UpdateBesRecParam();
                    }
                    else
                    {
                        ALOGD("%s(), mCaptureHandler is destroyed, no need to update", __FUNCTION__);
                    }
#endif
                }
            }
        }

    }
    ALOGD("-%s()", __FUNCTION__);
    return status;

}

status_t AudioALSAStreamIn::setSuspend(const bool suspend_on)
{
    ALOGD("%s(), mSuspendCount = %u, suspend_on = %d", __FUNCTION__, mSuspendCount, suspend_on);

    if (suspend_on == true)
    {
        mSuspendCount++;
    }
    else if (suspend_on == false)
    {
        ASSERT(mSuspendCount > 0);
        mSuspendCount--;
    }

    ALOGD("-%s(), mSuspendCount = %u", __FUNCTION__, mSuspendCount);
    return NO_ERROR;
}


void AudioALSAStreamIn::CheckBesRecordInfo()
{
    ALOGD("+%s()", __FUNCTION__);

    if (mAudioSpeechEnhanceInfoInstance != NULL)
    {
        mStreamAttributeTarget.BesRecord_Info.besrecord_scene = mAudioSpeechEnhanceInfoInstance->GetBesRecScene();
        //for besrecord tuning
        mStreamAttributeTarget.BesRecord_Info.besrecord_tuningEnable = mAudioSpeechEnhanceInfoInstance->IsBesRecTuningEnable();

        //for DMNR tuning
        mStreamAttributeTarget.BesRecord_Info.besrecord_dmnr_tuningEnable = mAudioSpeechEnhanceInfoInstance->IsAPDMNRTuningEnable();

        memset(mStreamAttributeTarget.BesRecord_Info.besrecord_VMFileName, 0, VM_FILE_NAME_LEN_MAX);
        mAudioSpeechEnhanceInfoInstance->GetBesRecVMFileName(mStreamAttributeTarget.BesRecord_Info.besrecord_VMFileName);

        //for engineer mode
        if (mAudioSpeechEnhanceInfoInstance->GetForceMagiASRState() > 0)
        {
            mStreamAttributeTarget.BesRecord_Info.besrecord_ForceMagiASREnable = true;
        }
        if (mAudioSpeechEnhanceInfoInstance->GetForceAECRecState())
        {
            mStreamAttributeTarget.BesRecord_Info.besrecord_ForceAECRecEnable = true;
        }

        //for dynamic mask
        mStreamAttributeTarget.BesRecord_Info.besrecord_dynamic_mask = mAudioSpeechEnhanceInfoInstance->GetDynamicVoIPSpeechEnhancementMask();
    }
    ALOGD("-%s()", __FUNCTION__);
}

void AudioALSAStreamIn::UpdateDynamicFunctionMask(void)
{
    ALOGD("+%s()", __FUNCTION__);

    if (mAudioSpeechEnhanceInfoInstance != NULL)
    {
        //for dynamic mask
        mStreamAttributeTarget.BesRecord_Info.besrecord_dynamic_mask = mAudioSpeechEnhanceInfoInstance->GetDynamicVoIPSpeechEnhancementMask();
    }
    ALOGD("-%s()", __FUNCTION__);
}

bool AudioALSAStreamIn::isSupportConcurrencyInCall(void)
{
    ALOGD("+%s()", __FUNCTION__);
    bool bIsSupport = false;
    AudioAutoTimeoutLock _l(mLock);

    if (mCaptureHandler != NULL)
    {
        bIsSupport = mCaptureHandler->isSupportConcurrencyInCall();
    }
    else
    {
        ALOGW("mCaptureHandler is NULL");
        bIsSupport = false;
    }
    ALOGD("-%s() bIsSupport = %d", __FUNCTION__, bIsSupport);
    return bIsSupport;
}

void AudioALSAStreamIn::OpenPCMDump()
{
    ALOGV("%s()", __FUNCTION__);
    char Buf[10];
    snprintf(Buf, sizeof(Buf),"%d.pcm", mDumpFileNum);

    mDumpFileName = String8(streamin);
    mDumpFileName.append((const char *)Buf);

    mPCMDumpFile = NULL;
    mPCMDumpFile = AudioOpendumpPCMFile(mDumpFileName, streamin_propty);

    if (mPCMDumpFile != NULL)
    {
        ALOGD("%s DumpFileName = %s", __FUNCTION__, mDumpFileName.string());
    }

    mDumpFileNum++;
    mDumpFileNum %= MAX_DUMP_NUM;

}

void AudioALSAStreamIn::ClosePCMDump()
{
    ALOGV("%s()", __FUNCTION__);
    if (mPCMDumpFile)
    {
        AudioCloseDumpPCMFile(mPCMDumpFile);
        ALOGD("%s(), close it", __FUNCTION__);
    }
}

void  AudioALSAStreamIn::WritePcmDumpData(void *buffer, ssize_t bytes)
{
    if (mPCMDumpFile)
    {
        //ALOGD("%s()", __FUNCTION__);
        AudioDumpPCMData((void *)buffer , bytes, mPCMDumpFile);
    }
}


}
